package net.sf.gratia.services;

import net.sf.gratia.storage.Duration;
import net.sf.gratia.storage.Duration.*;
import net.sf.gratia.util.Execute;
import net.sf.gratia.util.Logging;
import net.sf.gratia.util.LogLevel;

import java.lang.Thread.*;
import java.rmi.*;
import java.security.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.TimeZone;
import javax.servlet.*;
//import javax.management.MBeanServerConnection;
//import javax.management.ObjectName;
//import javax.management.remote.JMXConnector;
//import javax.management.remote.JMXConnectorFactory;
//import javax.management.remote.JMXServiceURL;
//import net.sf.gratia.services.DatabaseMaintenance;

import org.hibernate.Session;
import org.hibernate.Transaction;


public class CollectorService implements ServletContextListener {
   static String fName;
   
   String rmibind;
   String rmilookup;
   String service;
   private JMSProxyImpl theProxy;
   Properties p;
   
   static final long threadStopWaitTime = 60 * 1000; // One minute, in milliseconds
   static final long safeStartCheckInterval = 30 * 1000; // 30s in ms.
   Boolean opsEnabled = false;
   Boolean m_servletEnabled = false;
   private Boolean housekeepingDisabled = false;
   private int fSecurityLevel = 0;
   
   private boolean needConnectionTracking() {
      return 1 == (fSecurityLevel & 1);
   }
   
   //
   // various threads
   //
   
   class RecordProcessorThreads {
      private RecordProcessor processors[] = null;
      
      public RecordProcessorThreads(int nthreads) {
         QueueManager.setupQueues(nthreads);
      }

      public synchronized int GetCount() { 
         if (processors == null) return 0;
         else return processors.length;
      }
      
      public synchronized boolean IsAlive() {
         // Return true if any of the processor thread is active.
         if (processors == null) return false;
         for (int i = 0; i < processors.length; i++) {
            if ((processors[i] != null) && processors[i].isAlive()) return true;
         }
         return false;
         
      }
      
      public synchronized void Start(int nthreads) {
         if (IsAlive()) {
            Logging.info("CollectorService: record processor threads cannot be started -- already active");
            return;
         }
         QueueManager.setupQueues(nthreads);
         processors = new RecordProcessor[nthreads];
         for (int i = 0; i < nthreads; ++i) {
            processors[i] = new RecordProcessor("RecordProcessor: " + i, QueueManager.getQueue(i), lock, global, CollectorService.this);
            processors[i].setPriority(Thread.MAX_PRIORITY);
            processors[i].setDaemon(true);
         }
         for (int i = 0; i < nthreads; i++) {
            processors[i].start();
         }
      }
      
      public synchronized int Stop() {
         // Attempt to stop the threads, return the number of unfinished
         // threads.
         
         int i;
         for (i = 0; i < processors.length; ++i)
         {
            if (processors[i] != null) {
               processors[i].stopRequest();
               if (processors[i].getState() == Thread.State.TIMED_WAITING) {
                  processors[i].interrupt();
               }
            }
         }
         int unfinished = 0;
         try {
            for (i = 0; i < processors.length; i++) {
               if (processors[i] != null) processors[i].join(threadStopWaitTime);                  
            }
         }
         catch (Exception ignore) {
         }
         for (i = 0; i < processors.length; ++i) {
            if ((processors[i] != null) && processors[i].isAlive()) {
               // Timeout occurred; thread has not finished
               unfinished = unfinished + 1;
            } else {
               // Finished
            }
         }
         return unfinished;
      }
   }
   
   RecordProcessorThreads recordProcessors;
   ReplicationService replicationService;
   RMIService rmiservice;
   QSizeMonitor qsizeMonitor;
   TableStatisticsManager tableStatisticsManager;
   BacklogStatisticsManager backlogStatisticsManager;
   MonitorRecordProcessor monitorRecordProcessor;
   DataHousekeepingService housekeepingService;
   
   public String configurationPath;
   
   //
   // various globals
   //
   
   Object lock = new Object();
   Hashtable global = new Hashtable();
   DatabaseMaintenance checker = null;
   
   public CollectorService() {
      net.sf.gratia.storage.Connection.setDefaultCollectorName(getName());
   }
   
   public void contextInitialized(ServletContextEvent sce) {
      int i = 0;
      
      //
      // initialize logging
      //
      p = net.sf.gratia.util.Configuration.getProperties();

      Logging.initialize("service");
      
      Enumeration iter = System.getProperties().propertyNames();
      Logging.log(LogLevel.CONFIG, "System properties:");
      while (iter.hasMoreElements()) {
         String key = (String)iter.nextElement();
         if (key.endsWith(".password")) continue;
         if (key.endsWith(".rootpassword")) continue;
         String value = (String)System.getProperty(key);
         Logging.log(LogLevel.CONFIG,
                     "Key: " + key + " value: " + value);
      }
      
      
      Logging.log(LogLevel.CONFIG, "Service properties:");
      iter = p.propertyNames();
      while (iter.hasMoreElements()) {
         String key = (String)iter.nextElement();
         if (key.endsWith(".password")) continue;
         if (key.endsWith(".rootpassword")) continue;
         String value = (String)p.getProperty(key);
         Logging.log(LogLevel.CONFIG, "Key: " + key + " value: " + value);
      }
      
      configurationPath = net.sf.gratia.util.Configuration.getConfigurationPath();
      
      String url = p.getProperty("service.open.connection");
      if ( url != null ) {
         fName = "collector:" + url;
      } else {
         try {
            fName = "collector:" + java.net.InetAddress.getLocalHost().toString();
         } catch (java.net.UnknownHostException e) {
            fName = "collector:localhost";
         }
      }
      
      fSecurityLevel = Integer.parseInt(p.getProperty("service.security.level", "0"));
      Logging.info("Certificate security level: " + fSecurityLevel);
      
      if (fSecurityLevel >= 2) {
         try {
            Logging.info("Initializing HTTPS Support");
            //
            // setup configuration path/https system parameters
            //
            System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            
            System.setProperty("javax.net.ssl.trustStore", configurationPath + "/truststore");
            System.setProperty("javax.net.ssl.trustStorePassword", "server");
            
            System.setProperty("javax.net.ssl.keyStore", configurationPath + "/keystore");
            System.setProperty("javax.net.ssl.keyStorePassword", "server");
            
            com.sun.net.ssl.HostnameVerifier hv = new com.sun.net.ssl.HostnameVerifier() {
               public boolean verify(String urlHostname, String certHostname) {
                  Logging.info("url host name: " + urlHostname);
                  Logging.info("cert host name: " + certHostname);
                  Logging.info("WARNING: Hostname is not matched for cert.");
                  return true;
               }
            };
            
            com.sun.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(hv);
         }
         catch (Exception e) {
            Logging.warning("CollectorService: contextInitialized() caught exception ", e);
         }
      }
      
      try {
         //
         // get configuration properties
         //
         
         rmilookup = p.getProperty("service.rmi.rmilookup");
         rmibind = p.getProperty("service.rmi.rmibind");
         service = p.getProperty("service.rmi.service");
         
         //
         // set default timezone
         //
         // HK: removed after we put the same line in Logging.java
         //TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
         
         //
         // start rmi
         //
         
         rmiservice = new RMIService();
         rmiservice.setDaemon(true);
         rmiservice.start();
         Thread.sleep(10);
         Logging.info("CollectorService: RMI Service Started");
         
         checker = new DatabaseMaintenance(p);
         
         //
         // Check the database version, before starting
         // hibernate, if the version number is too high
         // we should not touch it. 
         //
         
         if (checker.IsDbNewer()) {
            // The database is newer than the currently running code
            // we should abort since we might enter incorrect data.
            Logging.log(LogLevel.SEVERE,
                        "CollectorService: The database schema is newer " +
                        "than what is expected by this code.  You need " +
                        "to upgrade this code to a newer version " +
                        "of the Gratia Collector");
            Logging.log(LogLevel.SEVERE,
                        "CollectorService: The service is NOT started");
            return;
         }
         
         Logging.info("CollectorService: doing initial cleanup");
         checker.InitialCleanup();
         
         //
         // start database
         //
         
         Logging.info("CollectorService: starting Hibernate");
         try {
            HibernateWrapper.startMaster();
         }
         catch (Exception e) {
            Logging.log(LogLevel.SEVERE, "CollectorService: error starting Hibernate.");
            Logging.log(LogLevel.SEVERE, "CollectorService: manual correction required");
            Logging.debug("Exception details:", e);
            return;
         }
         
         // Verify defaults.
         Logging.info("CollectorService: checking defaults in DB");
         checker.AddDefaults();
         
         // Verify indexes.
         try {
            Logging.info("CollectorService: checking indexes on DB tables");
            checker.CheckIndices();
         }
         catch (Exception e) {
            Logging.log(LogLevel.SEVERE, "CollectorService: error while checking indexes.");
            Logging.log(LogLevel.SEVERE, "CollectorService: manual correction required");
            Logging.debug("Exception detail: ", e);
            if ((Exception) e.getCause() != null) {
               Logging.debug("Causing exception detail: ", (Exception) e.getCause());
            }
            return;
         }
         
         //
         // Upgrade the database
         // 
         Logging.info("CollectorService: check / upgrade schema to current version");
         if (!checker.Upgrade()) {
            // The database has not been upgraded correctly.
            Logging.log(LogLevel.SEVERE, "CollectorService: The database schema was not upgraded properly.");
            Logging.log(LogLevel.SEVERE, "CollectorService: Manual correction required.");
            return;
         }
         
         //
         // zap database
         //
         
         Logging.info("CollectorService: refreshing views");
         checker.AddViews();
         
         
         //
         // Setup record processor manager.
         // 
         int maxthreads = Integer.parseInt(p.getProperty("service.recordProcessor.threads"));
         recordProcessors = new RecordProcessorThreads(maxthreads);
         
         Logging.info("CollectorService: JMS Server Started");
         
         //
         // poke in rmi
         //
         theProxy = new JMSProxyImpl(this);
         Naming.rebind(rmibind + service, theProxy);
         Logging.info("JMSProxy started");
         
         // Determine whether we can start normal operations.
         Boolean safeStart = false;
         try {
            safeStart = 0 <
            Integer.parseInt(p.getProperty("gratia.service.safeStart"));
         } catch (Exception ignore) {
         }
         
         synchronized (this) { // Set current status
            opsEnabled = !safeStart;
            m_servletEnabled = opsEnabled;
            if (safeStart) {
               Logging.info("CollectorService: starting in safe mode -- no operational services started yet ...");
            } else {
               startAllOperations();
            } 
         }
      }
      catch (Exception e) {
         Logging.log(LogLevel.SEVERE, "CollectorService: contextInitialized() caught exception ", e);
      }
      
   }
   
   // Return the 'name' of this collector.
   public static String getName() {
      if (fName == null) {
         try {
            fName = "collector:" + java.net.InetAddress.getLocalHost().toString();
         } catch (java.net.UnknownHostException e) {
            fName = "collector:localhost";
         }
      }
      return fName;
   }
   
   
   private void startAllOperations() {
      int i = 0; // Loop variable
      int maxthreads = Integer.parseInt(p.getProperty("service.recordProcessor.threads"));
      
      try {
         Logging.info("CollectorService: beginning normal operational startup.");
         
         //
         // Start the servlet that receives records from probes.
         //
         
         if (p.getProperty("service.initial.servlets", "ON").equalsIgnoreCase("ON")) {
            Logging.info("CollectorService: enabling servlets to receive records.");
            enableServlet();
         } else {
            Logging.info("CollectorService: initial requested state of servlets is OFF.");
            disableServlet();
         }
         
         //
         // Start a thread to periodically clear expired data
         //
         String initialHousekeeping = p.getProperty("service.initial.housekeeping", "ON");
         if (initialHousekeeping.equalsIgnoreCase("ON")) {
            startHousekeepingService();
         } else if (initialHousekeeping.equalsIgnoreCase("RUN_NOW")) {
            startHousekeepingService(false);
         } else if (initialHousekeeping.matches("(?i)DISABLE.*")) {
            Logging.info("CollectorService: initial requested state of housekeeping is DISABLED.");
            housekeepingDisabled = true;
         }
         
         //
         // start a thread to recheck history directories every 6 hours
         //
         HistoryMonitor historyMonitor = new HistoryMonitor(p);
         historyMonitor.start();
         
         //
         // start msg recordProcessor
         //
         if (p.getProperty("service.initial.recordProcessor", "ON").equalsIgnoreCase("ON")) {
            Logging.info("CollectorService: starting recordProcessor.");
            recordProcessors.Start(maxthreads);
         } else {
            Logging.info("CollectorService: initial requested state of recordProcessor is OFF.");
         }
         
         //
         // if requested - start thread to monitor recordProcessor activity
         //
         if ((p.getProperty("monitor.recordProcessor.threads") != null) &&
             p.getProperty("monitor.recordProcessor.threads").equals("true")) {
            monitorRecordProcessor = new MonitorRecordProcessor(global);
            monitorRecordProcessor.start();
            Logging.info("CollectorService: Started MonitorRecordProcessor");
         }
         
         //
         // if requested - start service to monitor input queue sizes
         //
         if (p.getProperty("monitor.q.size").equals("1")) {
            Logging.log("CollectorService: Starting QSizeMonitor");
            qsizeMonitor = new QSizeMonitor(this);
            qsizeMonitor.start();
            Logging.info("CollectorService: QSizeMonitor started");
         }
         
         //
         // if requested - start service to keep an history of the table statistics.
         //
         if (p.getProperty("monitor.table.history").equals("1")) {
            Logging.log("CollectorService: Starting TableStatisticsManager");
            tableStatisticsManager = new TableStatisticsManager(this);
            tableStatisticsManager.start();
            Logging.info("CollectorService: TableStatisticsManager started");
         }

         //
         // if requested - start service to keep an history of the backlog statistics.
         //
         if (p.getProperty("monitor.backlog.history").equals("1")) {
            Logging.log("CollectorService: Starting BacklogStatisticsManager");
            backlogStatisticsManager = new BacklogStatisticsManager(this);
            backlogStatisticsManager.start();
            Logging.info("CollectorService: BacklogStatisticsManager started");
         }
      }
      catch (Exception e) {
         Logging.log(LogLevel.SEVERE, "CollectorService: contextInitialized() caught exception ", e);
      }
      
      //
      // add a server cert if one isn't there
      //
      if (p.getProperty("service.security.level").equals("1")) {
         if ((p.getProperty("service.use.selfgenerated.certs") != null) &&
             (p.getProperty("service.use.selfgenerated.certs").equals("1"))) {
            loadSelfGeneratedCerts();
         } else {
            loadVDTCerts();
         }
      }
      
      //
      // start replication service
      //
      if (p.getProperty("service.initial.replication", "ON").equalsIgnoreCase("ON")) {
         startReplicationService();
      } else {
         Logging.info("CollectorService: initial requested state of replication service is OFF.");
      }
      
   }
   
   public synchronized Boolean reaperActive() {
      return HistoryReaper.inProgress();
   }
   
   public synchronized void runReaper() {
      HistoryReaper reaper = new HistoryReaper();
      new Thread(reaper).start();
   }
   
   public synchronized Boolean operationsDisabled() {
      return !opsEnabled;
   }
   
   public synchronized void enableOperations() {
      if (opsEnabled) {
         return;
      } else {
         startAllOperations();
         opsEnabled = true;
      }
   }
   
   public synchronized Boolean servletEnabled() {
      return m_servletEnabled;
   }
   
   public synchronized void enableServlet() {
      if (!m_servletEnabled) {
         Logging.info("CollectorService: telling servlet to resume receiving records");
         m_servletEnabled = true;
      } else {
         Logging.info("CollectorService: servlet is already receiving records.");
      }
   }
   
   public synchronized void disableServlet() {
      if (m_servletEnabled) {
         Logging.info("CollectorService: telling servlet to stop receiving records");
         m_servletEnabled = false;
      } else {
         Logging.info("CollectorService: servlet is already set to reject incoming records");
      }
   }
   
   public synchronized void startHousekeepingService() {
      startHousekeepingService(true);
   }
   
   private synchronized void startHousekeepingService(Boolean initialDelay) {
      if (housekeepingDisabled ||
          housekeepingService == null ||
          !housekeepingService.isAlive()) 
      {
         Logging.info("CollectorService: Starting data housekeeping service");
         housekeepingDisabled = false;
         housekeepingService = new DataHousekeepingService(this,
                                                           DataHousekeepingService.HousekeepingAction.ALL,
                                                           initialDelay);
         housekeepingService.start();
      } 
      else if (housekeepingService.housekeepingStatus().equals("PAUSED"))
      {
         Logging.info("CollectorService: Waking up data housekeeping service");
         housekeepingService.wakeUp();
      }
   }
   
   public synchronized void pauseHousekeepingService() {
      if (housekeepingService == null || !housekeepingService.isAlive()) {
         Logging.info("CollectorService: housekeeping service cannot be paused -- not started!");
         return;
      }
      Logging.info("CollectorService: Pausing housekeeping service.");
      housekeepingService.requestPause();
   }
   
   public synchronized void stopHousekeepingService() {
      if (housekeepingService == null || !housekeepingService.isAlive()) {
         Logging.info("CollectorService: housekeeping service cannot be stopped -- not started!");
         return;
      }
      Logging.info("CollectorService: Stopping housekeeping service.");
      housekeepingService.requestStop();
      if (housekeepingService.getState() == Thread.State.TIMED_WAITING) {
         housekeepingService.interrupt();
      }
      try {
         housekeepingService.join(threadStopWaitTime); // Wait up to one minute for thread exit.
      }
      catch (InterruptedException e) { // Ignore
      }
      if (housekeepingService.isAlive()) { // Still working
         Logging.warning("CollectorService: housekeeping service has not stopped after " +
                         (long) (threadStopWaitTime / 1000) + "s");
      }
   }
   
   public synchronized Boolean housekeepingServiceDisabled() {
      return housekeepingDisabled;
   }
   
   public synchronized String housekeepingServiceStatus() {
      if (housekeepingDisabled) {
         return "DISABLED";
      } else if (housekeepingService == null) {
         return "STOPPED";
      } else {
         return housekeepingService.housekeepingStatus();
      }
   }

   public java.util.List<Object []> getHousekeepingTableStatus()
   {
      if (housekeepingService != null) {
         return housekeepingService.GetTableStatus();
      }
      return null;
   }
   
   public synchronized boolean housekeepingRunning() {
      if (housekeepingDisabled || housekeepingService == null) {
         return false;
      } else {
         return housekeepingService.isRunning();
      }
   }
   
   public synchronized void disableHousekeepingService() {
      stopHousekeepingService();
      housekeepingDisabled = true;
   }
   
   public synchronized Boolean startHousekeepingActionNow() {
      String status = housekeepingServiceStatus();
      if (status.equalsIgnoreCase("SLEEPING")) {
         housekeepingService.interrupt();
         return true;
      } else if (housekeepingDisabled ||
                 status.equals("STOPPED")) {
         startHousekeepingService(false); // No initial delay.
         return true;
      } else if (status.equals("PAUSED")) {
         Logging.info("CollectorService: Waking up data housekeeping service");
         housekeepingService.wakeUp();
         return true;
      } else {
         return false; // No action.
      }
   }
   
   public synchronized void startReplicationService() {
      if (replicationService == null || !replicationService.isAlive()) {
         Logging.info("CollectorService: Starting replication service");
         replicationService = new ReplicationService();
         replicationService.start();
      } else if (replicationService.isSleeping()) {
         replicationService.interrupt();
      }
   }
   
   public synchronized void stopReplicationService() {
      if (replicationService == null || !replicationService.isAlive()) {
         Logging.info("CollectorService: replication service cannot be stopped -- not started!");
         return;
      }
      Logging.info("CollectorService: Stopping replication service.");
      replicationService.requestStop();
      if (replicationService.getState() == Thread.State.TIMED_WAITING) {
         replicationService.interrupt();
      }
      try {
         replicationService.join(threadStopWaitTime); // Wait up to one minute for thread exit.
      }
      catch (InterruptedException e) { // Ignore
      }
      if (replicationService.isAlive()) { // Still working
         Logging.warning("CollectorService: replication service has not stopped after " +
                         (long) (threadStopWaitTime / 1000) + "s");
      }
      return;
   }
   
   public synchronized Boolean replicationServiceActive() {
      return replicationService != null && replicationService.isAlive();
   }
   
   public void stopDatabaseUpdateThreads() {
      if (!databaseUpdateThreadsActive()) {
         Logging.info("CollectorService: DB update threads cannot be stopped -- not started!");
         return;
      }
      Logging.info("CollectorService: stopping DB update threads");
      int total = recordProcessors.GetCount();
      int unfinished = recordProcessors.Stop();
      
      if (unfinished != 0) {
         Logging.warning("CollectorService: Some threads ("+unfinished+" out of "+total+") have not finished after " +
                         (long) (threadStopWaitTime / 1000) + "s");
      }
   }
   
   public void startDatabaseUpdateThreads() {
      if (databaseUpdateThreadsActive()) {
         Logging.info("CollectorService: DB update threads cannot be started -- already active");
         return;
      }
      int i;
      int maxthreads = Integer.parseInt(p.getProperty("service.recordProcessor.threads"));
      recordProcessors.Start(maxthreads);
   }
   
   public boolean databaseUpdateThreadsActive() {
      return recordProcessors.IsAlive();      
   }
   
   public void runQSizeMonitor() {
      if (qsizeMonitor != null) {
         qsizeMonitor.check();
      } else {
         Logging.info("CollectorService: DB QSizeMonitor.check requested but no monitor is running");         
      }
   }
   
   public void takeBacklogSnapshot() {
      if (backlogStatisticsManager != null) {
         backlogStatisticsManager.updateLocalBacklog();
         backlogStatisticsManager.takeSnapshot();
      } else {
         Logging.info("CollectorService: backlogStatisticsManager.takeSnapshot requested but no monitor is running");         
      }
   }

   public void updateBacklog(String name, long nrecords, long xmlfiles, long tarfiles, long backlog, long maxpendingfiles, long bundlesize)
   {
      if (backlogStatisticsManager != null) {
         backlogStatisticsManager.updateBacklog(name,nrecords,xmlfiles,tarfiles,backlog,maxpendingfiles,bundlesize);
      }
   }

   public void loadSelfGeneratedCerts() {
      //String keystore = System.getProperty("catalina.home") + "/gratia/keystore";
      String keystore =  "/var/lib/gratia-service/keystore";
      keystore = keystore.replaceAll("\\\\", "/");
      String command1[] =
      {"keytool",
         "-genkey",
         "-dname",
         "cn=server, ou=Fermi-GridAccounting, o=Fermi, c=US",
         "-alias",
         "server",
         "-keystore",
         keystore,
         "-keypass",
         "server",
         "-storepass",
      "server"};
      
      int exitValue1 = Execute.execute(command1);
      
      String command2[] =
      {"keytool",
         "-selfcert",
         "-alias",
         "server",
         "-keypass",
         "server",
         "-keystore",
         keystore,
         "-storepass",
      "server"};
      
      if (exitValue1 == 0)
         Execute.execute(command2);
      //      FlipSSL.flip();
   }
   
   public void loadVDTCerts() {
      //String keystore = System.getProperty("catalina.home") + "/gratia/keystore";
      String keystore = "/var/lib/gratia-service/keystore";
      //String configurationPath = System.getProperty("catalina.home") + "/gratia/";
      String configurationPath =  "/var/lib/gratia-service/";
      keystore = keystore.replaceAll("\\\\", "/");
      String command1[] =
      {
         "openssl",
         "pkcs12",
         "-export",
         "-out",
         configurationPath + "server.pkcs12",
         "-inkey",
         p.getProperty("service.vdt.key.file"),
         "-in",
         p.getProperty("service.vdt.cert.file"),
         "-passin",
         "pass:server",
         "-passout",
         "pass:server"
      };
      
      int exitValue1 = Execute.execute(command1);
      
      if (exitValue1 == 0) {
         PKCS12Load load = new PKCS12Load();
         try {
            load.load(configurationPath + "server.pkcs12",
                      keystore);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      FlipSSL.flip();
   }

   public void connectionResetAndLock()
   {
      // Reset and take a 'write' lock on the Connection cache table.
      
      net.sf.gratia.storage.Connection.collectionResetAndLock();
   }
   
   public void connectionResetUnLock()
   {
      // Release the write lock on the Connection cache table.

      net.sf.gratia.storage.Connection.collectionResetUnLock();
   }
   
   public void readLockCaches() {
      // Take a read lock on __all__ the object caches.

      net.sf.gratia.storage.Connection.collectionReadLock();
      net.sf.gratia.storage.Collector.collectionReadLock();
      net.sf.gratia.storage.Software.collectionReadLock();
   }

   public void readUnLockCaches() {
      // Release a read lock on __all__ the object caches.

      net.sf.gratia.storage.Connection.collectionReadUnLock();
      net.sf.gratia.storage.Collector.collectionReadUnLock();
      net.sf.gratia.storage.Software.collectionReadUnLock();
   }
   
   
   public String checkConnection(String certpem, String senderHost, String sender) 
   throws RemoteException, AccessException {
      
      net.sf.gratia.storage.Certificate c  = new net.sf.gratia.storage.Certificate( certpem);
      
      java.security.cert.X509Certificate certs[] = new java.security.cert.X509Certificate[1];
      
      try { 
         certs[0] = c.getCert();
      } catch (java.security.cert.CertificateException e) {
         Logging.info("Failed to create cert from pem: "+certpem);
         throw new AccessException("Invalid Certificate.");
      }
      
      return checkConnection(certs,senderHost,sender);
   }
   
   
   public String checkConnection(java.security.cert.X509Certificate certs[], String senderHost, String sender) 
   throws RemoteException, AccessException {
      final String command = "from Certificate where pem = ?";
      
      String result = "";
      net.sf.gratia.storage.Origin from = new net.sf.gratia.storage.Origin(new java.util.Date());
      
      if (certs == null || fSecurityLevel==1) {
         if (fSecurityLevel >= 4) {
            Logging.warning("checkCertificate: No certificate");
            return "";
         } else if (fSecurityLevel >= 2) {
            Logging.log("checkCertificate: No certificate");
         }
         if ( needConnectionTracking() ) {
            // Connection Tracking has been requested
            net.sf.gratia.storage.Connection gr_conn = new net.sf.gratia.storage.Connection(senderHost,sender,null);
            gr_conn = this.trackConnection(gr_conn);
            if (gr_conn.isValid()) {
               from.setConnection(gr_conn);
               result = from.asXml(0);
            } else {
               throw new AccessException("Invalid Gratia Connection.");
            }
         }
      } else {
         
         Session session = null;
         Transaction tx = null;
         
         for(int i=0; i< certs.length; ++i) {
            if (session == null) {
               session = HibernateWrapper.getSession();
            }
            String pem = null;
            net.sf.gratia.storage.Certificate localcert = null;
            try {
               pem =  net.sf.gratia.storage.Certificate.GeneratePem(certs[i]);
               tx = session.beginTransaction();
               localcert = (net.sf.gratia.storage.Certificate)session.createQuery(command).setString(0,pem).uniqueResult();
               tx.commit();
            } catch (java.security.cert.CertificateEncodingException e) {
               Logging.warning("checkCertificate exception: " + e);
               Logging.debug("Exception details: ", e);
               break;
            }
            if (localcert == null) {
               // Not registered yet.
               try {
                  localcert = new net.sf.gratia.storage.Certificate( certs[i] );
               } catch (java.security.cert.CertificateException e) {
                  Logging.warning("checkCertificate: Error when creating certificate object: " + e);
                  Logging.debug("Exception details: ", e);
                  break;
               }
               Integer nTries = 0;
               Boolean keepTrying = true;
               while (keepTrying) {
                  ++nTries;
                  if (session == null) {
                     session = HibernateWrapper.getSession();
                  }
                  try {
                     tx = session.beginTransaction();
                     session.saveOrUpdate( localcert );
                     session.flush();
                     tx.commit();
                     keepTrying = false;
                     session.close();
                     Logging.info("checkCertificate has created an entry for subject " +
                                  certs[i].getSubjectX500Principal().getName() +
                                  ", issuer: " +
                                  certs[i].getIssuerX500Principal().getName() +
                                  " valid from " +
                                  certs[i].getNotBefore() +
                                  " to " +
                                  certs[i].getNotAfter()
                                  );
                     Logging.log(LogLevel.FINER,"certificate details: " + certs[i].toString());
                  } catch (Exception e) {
                     HibernateWrapper.closeSession(session);
                     session = null;
                     if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "checkCertificate")) {
                        keepTrying = false;
                        Logging.warning("checkCertificate: error when storing certificate object: " + e.getMessage());
                        Logging.debug("checkCertificate: exception details:", e);
                     }
                  }
               }
            }
            if (! localcert.isValid() ) {
               
               throw new AccessException("Invalid Certificate.");
               
            } else if ( needConnectionTracking() ) {
               // Connection Tracking has been requested
               net.sf.gratia.storage.Connection gr_conn = new net.sf.gratia.storage.Connection(senderHost,sender,localcert);
               gr_conn = this.trackConnection(gr_conn);
               from.setConnection(gr_conn);
               if (gr_conn.isValid()) {
                  result = from.asXml(0);
               } else {
                  throw new AccessException("Invalid Gratia Connection.");
               }
            } else {
               result = from.asXml(0);
            }
            
         }
         HibernateWrapper.closeSession(session);
      }
      if (result.length()==0) {
         throw new AccessException("Failure during the check of the Certificate or the Gratia Connection.");
      }
      return result;
   }
   
   
   private net.sf.gratia.storage.Connection trackConnection(net.sf.gratia.storage.Connection gr_conn) throws AccessException {
      Session session = null;
      Boolean keepTrying  = true;
      Integer nTries = 0;
      
      while (keepTrying) {
         session = HibernateWrapper.getSession();
         Transaction tx = null;
         try {
            tx = session.beginTransaction();
            if (++nTries > 1) {
               Thread.sleep(300);
            }
            gr_conn = gr_conn.attach( session );
            session.flush();
            tx.commit();
            keepTrying = false;
            session.close();
         } catch (Exception e) {
            HibernateWrapper.closeSession(session);
            if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "checkCertificate")) {
               Logging.warning("checkCertificate: error when storing or retrieving connection object: " + e.getMessage());
               Logging.debug("checkCertificate: exception details:", e);
               keepTrying = false;
               
               // If we can not read the connection, let's assume it is set to invalid.
               // Apriori if this is wrong, the probe will try again later.
               throw new AccessException("Failure during the check of the the Gratia Connection.");
            }
         }
      }
      return gr_conn;
   }     
   
   
   public void contextDestroyed(ServletContextEvent sce) {
      Logging.info("CollectorService: Context Destroy Event");
      if (databaseUpdateThreadsActive()) {
         stopDatabaseUpdateThreads();
      }
      if ((housekeepingService != null) && housekeepingService.isAlive()) {
         stopHousekeepingService();
      }
      if ((replicationService != null) && replicationService.isAlive()) {
         stopReplicationService();
      }
      Logging.info("CollectorService: Context Destroyed");
      System.exit(0);
   }
   
   
   public class ReportSetup extends Thread {
      public ReportSetup() {
      }
      
      public void run() {
         try {
            Thread.sleep(30 * 1000);
         }
         catch (Exception ignore) {
         }
      }
   }
   
   public class HistoryMonitor extends Thread {
      
      private Duration checkInterval;
      private static final int defaultCheckIntervalHours = 6;
      
      private HistoryReaper reaper;
      
      public HistoryMonitor(Properties p) {
         reaper = new HistoryReaper();
         try {
            checkInterval =
            new Duration(p.getProperty("maintain.history.checkInterval",
                                       defaultCheckIntervalHours +
                                       " h"), DurationUnit.HOUR);
         }
         catch (DurationParseException e) {
            Logging.warning("HistoryMonitor: caught exception " +
                            "parsing maintain.history.checkInterval property", e);
            checkInterval = new Duration(defaultCheckIntervalHours, DurationUnit.HOUR);
         }
         
      }
      
      public void run() {
         while (true) {
            try {
               long checkInterval = this.checkInterval.msFromDate(new java.util.Date());
               Logging.debug("HistoryMonitor: going to sleep for " +
                             checkInterval + "ms.");
               Thread.sleep(checkInterval);
               new Thread(reaper).start();
            }
            catch (Exception ignore) {
            }
         }
      }
   }
   
   //
   // testing
   //
   
   static public void main(String args[]) {
      CollectorService service = new CollectorService();
      service.contextInitialized(null);
   }
}

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

    ListenerThread threads[];
    PerformanceThread pthreads[];
    ReplicationService replicationService;
    RMIService rmiservice;
    QSizeMonitor qsizeMonitor;
    MonitorListenerThread monitorListenerThread;
    DataHousekeepingService housekeepingService;

    public String configurationPath;

    //
    // various globals
    //

    String queues[] = null;
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
            String value = (String)System.getProperty(key);
            Logging.log(LogLevel.CONFIG,
                        "Key: " + key + " value: " + value);
        }


        Logging.log(LogLevel.CONFIG, "Service properties:");
        iter = p.propertyNames();
        while (iter.hasMoreElements()) {
            String key = (String)iter.nextElement();
            if (key.endsWith(".password")) continue;
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

            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

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
            // setup queues for message handling
            //

            int maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));
            queues = new String[maxthreads];

            Execute.execute("mkdir -p " + configurationPath + "/data");
            for (i = 0; i < maxthreads; i++) {
                Execute.execute("mkdir -p " + configurationPath + "/data/thread" + i);
                queues[i] = configurationPath + "/data/thread" + i;
                Logging.log("Created Q: " + queues[i]);
            }

            Logging.info("CollectorService: JMS Server Started");

            //
            // poke in rmi
            //
            JMSProxyImpl proxy = new JMSProxyImpl(this);
            Naming.rebind(rmibind + service, proxy);
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
        int maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));
        
        try {
            Logging.info("CollectorService: beginning normal operational startup.");

            //
            // Start the servlet that receives records from probes.
            //

            Logging.info("CollectorService: enabling servlet to receive records.");
            enableServlet();

            //
            // Check whether we need to start a checksum upgrade thread
            //

            Logging.log("CollectorService: Checking for unique index on md5v2");

            //
            // Start a thread to periodically clear expired data
            //
            startHousekeepingService();

            //
            // start a thread to recheck history directories every 6 hours
            //

            HistoryMonitor historyMonitor = new HistoryMonitor(p);
            historyMonitor.start();

            //
            // start msg listener
            //

            if (p.getProperty("performance.test") != null) {
                if (p.getProperty("performance.test").equals("false")) {
                    threads = new ListenerThread[maxthreads];
                    for (i = 0; i < maxthreads; i++) {
                        threads[i] = new ListenerThread("ListenerThread: " + i, queues[i], lock, global, this);
                        threads[i].setPriority(Thread.MAX_PRIORITY);
                        threads[i].setDaemon(true);
                    }
                    for (i = 0; i < maxthreads; i++) {
                        threads[i].start();
                    }
                } else {
                    pthreads = new PerformanceThread[maxthreads];
                    for (i = 0; i < maxthreads; i++) {
                        pthreads[i] = new PerformanceThread("PerformanceThread: " + i, queues[i], lock, global);
                        pthreads[i].setPriority(Thread.MAX_PRIORITY);
                        pthreads[i].setDaemon(true);
                    }
                    for (i = 0; i < maxthreads; i++) {
                        pthreads[i].start();
                    }
                }
            } else {
                threads = new ListenerThread[maxthreads];
                for (i = 0; i < maxthreads; i++) {
                    threads[i] = new ListenerThread("ListenerThread: " + i, queues[i], lock, global, this);
                    threads[i].setPriority(Thread.MAX_PRIORITY);
                    threads[i].setDaemon(true);
                }
                for (i = 0; i < maxthreads; i++) {
                    threads[i].start();
                }
            }

            //
            // if requested - start thread to monitor listener activity
            //
            if ((p.getProperty("monitor.listener.threads") != null) &&
                p.getProperty("monitor.listener.threads").equals("true")) {
                monitorListenerThread = new MonitorListenerThread(global);
                monitorListenerThread.start();
                Logging.info("CollectorService: Started MonitorListenerThread");
            }
            //
            // if requested - start service to monitor input queue sizes
            //

            if (p.getProperty("monitor.q.size").equals("1")) {
                Logging.log("CollectorService: Starting QSizeMonitor");
                qsizeMonitor = new QSizeMonitor();
                qsizeMonitor.start();
                Logging.info("CollectorService: QSizeMonitor started");
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

        startReplicationService();
       
        // Let's restart the reporting service to make sure that BIRT is getting the timezone
        // specified in its web.xml. (During the regular tomcat start this seems to be over-ridden by something else)
        String reporting_service_name = "Catalina:j2eeType=WebModule,name=//localhost/gratia-reporting,J2EEApplication=none,J2EEServer=none";
        Logging.info("CollectorService: recycling reporting service to ensure correct date display in reports");
        Boolean result = 
            QSizeMonitor.servletCommand(reporting_service_name,"stop",true);
        if (result) {
            result =
                QSizeMonitor.servletCommand(reporting_service_name,"start",true);
        }
        if (result) {
            Logging.info("CollectorService: reporting service recycled successfully.");
        } else {
            Logging.warning("CollectorService: Unable to recyle reporting service; reports could display inaccurate dates.");
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
            !housekeepingService.isAlive()) {
            Logging.info("CollectorService: Starting data housekeeping service");
            housekeepingDisabled = false;
            housekeepingService =
                new DataHousekeepingService(this,
                                            DataHousekeepingService.HousekeepingAction.ALL,
                                            initialDelay);
            housekeepingService.start();
        }
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
        if (housekeepingService == null) {
            return "STOPPED";
        } else if (housekeepingDisabled) {
            return "DISABLED";
        } else {
            return housekeepingService.housekeepingStatus();
        }
    }

    public synchronized void disableHousekeepingService() {
        stopHousekeepingService();
        housekeepingDisabled = true;
    }

    public synchronized Boolean startHousekeepingActionNow() {
        if (housekeepingServiceStatus().equalsIgnoreCase("SLEEPING")) {
            housekeepingService.interrupt();
            return true;
        } else if (housekeepingDisabled ||
                   housekeepingServiceStatus().equals("STOPPED")) {
            startHousekeepingService(false); // No initial delay.
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

    public synchronized void stopDatabaseUpdateThreads() {
        if (!databaseUpdateThreadsActive()) {
            Logging.info("CollectorService: DB update threads cannot be stopped -- not started!");
            return;
        }
        Logging.info("CollectorService: stopping DB update threads");
        int i;
        int maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));
        for (i = 0; i < maxthreads; i++)
            {
                if (threads[i] != null) {
                    threads[i].stopRequest();
                    if (threads[i].getState() == Thread.State.TIMED_WAITING) {
                        threads[i].interrupt();
                    }
                }
            }
        int unfinished = 0;
        try {
            for (i = 0; i < maxthreads; i++) {
                if (threads[i] != null) threads[i].join(threadStopWaitTime);                  
            }
        }
        catch (Exception ignore) {
        }
        for (i = 0; i < maxthreads; i++) {
            if ((threads[i] != null) && threads[i].isAlive()) {
                // Timeout occurred; thread has not finished
                unfinished = unfinished + 1;
            } else {
                // Finished
            }
        }
        if (unfinished != 0) {
            Logging.warning("CollectorService: Some threads ("+unfinished+") have not finished after " +
                            (long) (threadStopWaitTime / 1000) + "s");
        }
    }

    public synchronized void startDatabaseUpdateThreads() {
        if (databaseUpdateThreadsActive()) {
            Logging.info("CollectorService: DB update threads cannot be started -- already active");
            return;
        }
        int i;
        int maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));
        threads = new ListenerThread[maxthreads];
        for (i = 0; i < maxthreads; i++) {
            threads[i] = new ListenerThread("ListenerThread: " + i, queues[i], lock, global, this);
            threads[i].setPriority(Thread.MAX_PRIORITY);
            threads[i].setDaemon(true);
        }
        for (i = 0; i < maxthreads; i++) {
            threads[i].start();
        }

    }

    public synchronized boolean databaseUpdateThreadsActive() {
        int i;
        int maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));

        if (threads == null) return false;

        for (i = 0; i < maxthreads; i++) {
            if ((threads[i] != null) && threads[i].isAlive()) return true;
        }
        return false;
    }

    public void loadSelfGeneratedCerts() {
        String keystore = System.getProperty("catalina.home") + "/gratia/keystore";
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
        String keystore = System.getProperty("catalina.home") + "/gratia/keystore";
        String configurationPath = System.getProperty("catalina.home") + "/gratia/";
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
   
   public void setConnectionCaching(boolean enable)
   {
      net.sf.gratia.storage.Connection.setCaching(enable);
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

            Session session = null;
            session = HibernateWrapper.getSession();
  
            net.sf.gratia.storage.Connection gr_conn = new net.sf.gratia.storage.Connection(senderHost,sender,null);
            Transaction tx = session.beginTransaction();
            try {
               gr_conn = gr_conn.attach( session );
               session.flush();
               tx.commit();
            } catch (org.hibernate.exception.LockAcquisitionException e) {
               Logging.warning("checkCertificate: Lock acquisition exception.  Trying a second time.");
               try {
                  // Try a second time after a little sleep.
                  Thread.sleep(300);
                  
                  session.flush();
                  tx.commit();
                
               } catch (Exception sub_e) {
                  
                  Logging.warning("checkCertificate: error during 2nd attempt at storing or retrieving connection object in: ",sub_e);
                  sub_e.printStackTrace();
                  
                  tx.rollback();
               } 
            } catch (Exception e) {

               Logging.warning("checkCertificate: error when storing or retrieving connection object in: ",e);
               e.printStackTrace();

               tx.rollback();
            }

            session.close();
            
            from.setConnection(gr_conn);
            if (gr_conn.isValid()) {
               result = from.asXml(0);
            }
         }
         
      } else {
         
         Session session = null;

         
         for(int i=0; i< certs.length; ++i) {
            if (session == null) {
               session = HibernateWrapper.getSession();
            }
            try {
               String pem =  net.sf.gratia.storage.Certificate.GeneratePem(certs[i]);
            
               net.sf.gratia.storage.Certificate localcert = (net.sf.gratia.storage.Certificate)session.createQuery(command).setString(0,pem).uniqueResult();
            
               if (localcert == null) {
                  // Not registered yet.
                  
                  Transaction tx = session.beginTransaction();
                  try {
                     localcert = new net.sf.gratia.storage.Certificate( certs[i] );
                     session.saveOrUpdate( localcert );
                     session.flush();
                     tx.commit();
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
                  } catch (java.security.cert.CertificateException e) {
                     Logging.warning("checkCertificate: Error when creating certificate object: ",e);
                  } catch (Exception e) {
                     // Must close session!
                     tx.rollback();
                     session.close();
                     session = null;

                     Logging.warning("checkCertificate: error when storing certificate object: ",e);
                  }
               }
               if (! localcert.isValid() ) {

                  throw new AccessException("Invalid Certificate.");

               } else if ( needConnectionTracking() ) {
                  // Connection Tracking of has been requested
                  
                  if (session == null) {
                     session = HibernateWrapper.getSession();
                  }
                  net.sf.gratia.storage.Connection gr_conn = new net.sf.gratia.storage.Connection(senderHost,sender,localcert);
                  Transaction tx = session.beginTransaction();
                  try {
                     gr_conn = gr_conn.attach( session );
                     session.flush();
                     tx.commit();
                  } catch (Exception e) {
                     // Must close session!
                     tx.rollback();
                     session.close();
                     session = null;
                     
                     Logging.warning("checkCertificate: error when storing or retrieving connection object: ",e);
                  }
                  from.setConnection(gr_conn);
                  if (gr_conn.isValid()) {
                     result = from.asXml(0);
                  } else {
                     throw new AccessException("Invalid Gratia Connection.");
                  }
               } else {
                  result = from.asXml(0);
               }
               
            } catch (java.security.cert.CertificateEncodingException e) {
               Logging.warning("exception in checkCertificate: "+e);
            }            
         }
         if (session != null) {
            session.close();
         }
      }
      if (result.length()==0) {
         throw new AccessException("Failure during the check of the Certificate or the Gratia Connection.");
      }
      return result;
   }
   

    public void contextDestroyed(ServletContextEvent sce) {
        if (databaseUpdateThreadsActive()) {
            stopDatabaseUpdateThreads();
        }
        if ((housekeepingService != null) && housekeepingService.isAlive()) {
            stopHousekeepingService();
        }
        if ((replicationService != null) && replicationService.isAlive()) {
            stopReplicationService();
        }
        Logging.info("Context Destroy Event");
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

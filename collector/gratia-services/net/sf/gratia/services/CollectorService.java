package net.sf.gratia.services;

import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Execute;
import net.sf.gratia.util.Logging;

import java.lang.Thread.*;
import java.math.BigInteger;
import java.rmi.*;
import java.security.*;
import java.sql.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import javax.servlet.*;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import net.sf.gratia.storage.DatabaseMaintenance;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.exception.*;

public class CollectorService implements ServletContextListener {
    String rmibind;
    String rmilookup;
    String service;

    Properties p;

    static final long threadStopWaitTime = 60 * 1000; // One minute, in milliseconds
    static final long safeStartCheckInterval = 30 * 1000; // 30s in ms.
    Boolean opsEnabled = false;
    Boolean m_servletEnabled = false;
    private Boolean housekeepingDisabled = false;
    private Boolean checksumUpgradeDisabled = false;

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
    ChecksumUpgrader checksumUpgrader;

    public String configurationPath;

    //
    // various globals
    //

    String queues[] = null;
    Object lock = new Object();
    Hashtable global = new Hashtable();
    DatabaseMaintenance checker = null;

    public void contextInitialized(ServletContextEvent sce) {
        int i = 0;

        //
        // initialize logging
        //
        p = net.sf.gratia.util.Configuration.getProperties();
        
        Logging.initialize(p.getProperty("service.service.logfile"),
                           p.getProperty("service.service.maxlog"),
                           p.getProperty("service.service.console"),
                           p.getProperty("service.service.level"),
                           p.getProperty("service.service.numLogs"));

        //         Logging.info("CollectorService: classpath = " +
        //                      java.lang.System.getProperty("java.class.path", "."));

        Enumeration iter = System.getProperties().propertyNames();
        Logging.log("");
        while (iter.hasMoreElements()) {
            String key = (String)iter.nextElement();
            if (key.endsWith(".password")) continue;
            String value = (String)System.getProperty(key);
            Logging.log("Key: " + key + " value: " + value);
        }
        Logging.log("");

        Logging.log("");
        Logging.log("service properties:");
        Logging.log("");
        iter = p.propertyNames();
        while (iter.hasMoreElements()) {
            String key = (String)iter.nextElement();
            if (key.endsWith(".password")) continue;
            String value = (String)p.getProperty(key);
            Logging.log("Key: " + key + " value: " + value);
        }
        Logging.log("");
        Logging.log("service.security.level: " + p.getProperty("service.security.level"));

        configurationPath = net.sf.gratia.util.Configuration.getConfigurationPath();

        if (p.getProperty("service.security.level").equals("1")) {
            try {
                Logging.log("");
                Logging.log("Initializing HTTPS Support");
                Logging.log("");
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
                            Logging.log("url host name: " + urlHostname);
                            Logging.log("cert host name: " + certHostname);
                            Logging.log("WARNING: Hostname is not matched for cert.");
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
            Logging.log("");
            Logging.log("CollectorService: RMI Service Started");
            Logging.log("");

            checker = new DatabaseMaintenance(p);

            //
            // Check the database version, before starting
            // hibernate, if the version number is too high
            // we should not touch it. 
            //
         
            if (checker.IsDbNewer()) {
                // The database is newer than the currently running code
                // we should abort since we might enter incorrect data.
                Logging.warning("CollectorService: The database schema is newer than what is expected by this code.  You need to upgrade this code to a newer version of the Gratia Collector");
                Logging.warning("CollectorService: The service is NOT started");
                return;
            }
         
         
            checker.InitialCleanup();
         
            //
            // start database
            //

            HibernateWrapper.start();

            //
            // Upgrade the database
            // 

            checker.CheckIndices();
            if (!checker.Upgrade()) {
                // The database has not been upgraded correctly.
                Logging.warning("CollectorService: The database schema was not upgraded properly.");
                Logging.warning("CollectorService: Manual correction required.");
                return;
            }

            //
            // zap database
            //

            checker.AddDefaults();
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

            Logging.log("");
            Logging.log("CollectorService: JMS Server Started");
            Logging.log("");

            //
            // poke in rmi
            //

            JMSProxyImpl proxy = new JMSProxyImpl(this);
            Naming.rebind(rmibind + service, proxy);
            Logging.log("JMSProxy Started");

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
            Logging.warning("CollectorService: contextInitialized() caught exception ", e);
        }

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

            Logging.debug("CollectorService: Checking for unique index on md5v2");

            Boolean require_checksum_upgrade = false;
            try {
                require_checksum_upgrade = !checker.checkMd5v2Unique();
            }
            catch (Exception e) {
                Logging.warning("CollectorService: unable to ascertain md5v2 index status: not starting upgrade thread", e);
                checksumUpgradeDisabled = true;
                housekeepingDisabled = true; // Also don't start housekeeping.
            }

            if (!checksumUpgradeDisabled) {
                checksumUpgradeDisabled =
                    0 < checker.readIntegerDBProperty("gratia.database.disableChecksumUpgrade");
            }
            
            if (require_checksum_upgrade && !checksumUpgradeDisabled) {
                Logging.info("CollectorService: starting checksum upgrade thread.");
                checksumUpgrader = new ChecksumUpgrader(this);
                checksumUpgrader.start();
                Logging.log("CollectorService: ChecksumUpgrader started");
            }

            //
            // Start a thread to periodically clear expired data
            //
            startHousekeepingService();

            //
            // start a thread to recheck history directories every 6 hours
            //

            HistoryMonitor historyMonitor = new HistoryMonitor();
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
                Logging.log("CollectorService: Started MonitorListenerThread");
            }
            //
            // if requested - start service to monitor input queue sizes
            //

            if (p.getProperty("monitor.q.size").equals("1")) {
                Logging.log("CollectorService: Starting QSizeMonitor");
                qsizeMonitor = new QSizeMonitor();
                qsizeMonitor.start();
            }
        }
        catch (Exception e) {
            Logging.warning("CollectorService: contextInitialized() caught exception ", e);
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

        //
        // wait 1 minute to create new report config for birt (giving tomcat time to deploy the war)
        //
        //      
        // PENELOPE: no need to setup the reports
        //      (new ReportSetup()).start();

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

    public synchronized String checksumUpgradeStatus() {
        if (checksumUpgradeDisabled) {
            if (0 < checker.readIntegerDBProperty("gratia.database.disableChecksumUpgrade")) {
                return "DISABLED (MANUAL)";
            } else {
                return "DISABLED (AUTO-SAFETY)";
            }
        } else if (checksumUpgrader  != null) {
            return checksumUpgrader.checksumUpgradeStatus();
        } else {
            return "OFF";
        }
    }

    public synchronized void stopDatabaseUpdateThreads() {
        if (!databaseUpdateThreadsActive()) {
            Logging.info("CollectorService: DB update threads cannot be stopped -- not started!");
            return;
        }
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

    public Boolean checkMd5v2Unique() throws Exception {
        return checker.checkMd5v2Unique();
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

    public void contextDestroyed(ServletContextEvent sce) {
        Logging.info("");
        Logging.info("Context Destroy Event");
        Logging.info("");
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
        public HistoryMonitor() {
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(6 * 60 * 60 * 1000);
                    new HistoryReaper();
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

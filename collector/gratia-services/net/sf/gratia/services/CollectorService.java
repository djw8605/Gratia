package net.sf.gratia.services;

import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Execute;
import net.sf.gratia.util.Logging;
import net.sf.gratia.util.XP;

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

import net.sf.gratia.storage.DatabaseMaintenance;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.exception.*;

public class CollectorService implements ServletContextListener {
    public String rmibind;
    public String rmilookup;
    public String service;

    public Properties p;

    //
    // various threads
    //

    ListenerThread threads[];
    PerformanceThread pthreads[];
    StatusListenerThread statusListenerThread;
    ReplicationService replicationService;
    RMIService rmiservice;
    QSizeMonitor qsizeMonitor;;
    MonitorListenerThread monitorListenerThread;

    XP xp = new XP();

    public String configurationPath;

    //
    // various globals
    //

    String queues[] = null;
    Object lock = new Object();
    Hashtable global = new Hashtable();

    public void contextInitialized(ServletContextEvent sce)
    {
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
        while (iter.hasMoreElements())
            {
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
        while (iter.hasMoreElements())
            {
                String key = (String)iter.nextElement();
                if (key.endsWith(".password")) continue;
                String value = (String)p.getProperty(key);
                Logging.log("Key: " + key + " value: " + value);
            }
        Logging.log("");
        Logging.log("service.security.level: " + p.getProperty("service.security.level"));

        configurationPath = net.sf.gratia.util.Configuration.getConfigurationPath();

        if (p.getProperty("service.security.level").equals("1"))
            try
                {
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

                    com.sun.net.ssl.HostnameVerifier hv = new com.sun.net.ssl.HostnameVerifier()
                        {
                            public boolean verify(String urlHostname, String certHostname)
                            {
                                Logging.log("url host name: " + urlHostname);
                                Logging.log("cert host name: " + certHostname);
                                Logging.log("WARNING: Hostname is not matched for cert.");
                                return true;
                            }
                        };

                    com.sun.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(hv);
                }
            catch (Exception e)
                {
                    e.printStackTrace();
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

            DatabaseMaintenance checker = new DatabaseMaintenance(p);

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
            for (i = 0; i < maxthreads; i++)
                {
                    Execute.execute("mkdir -p " + configurationPath + "/data/thread" + i);
                    queues[i] = configurationPath + "/data/thread" + i;
                    Logging.log("Created Q: " + queues[i]);
                }

            Logging.log("");
            Logging.log("CollectorService: JMS Server Started");
            Logging.log("");

            //
            // Check whether we need to start a checksum upgrade thread
            //

            Logging.debug("CollectorService: Checking for unique index on md5v2");

            String checksum_check = "select non_unique from " +
                "information_schema.statistics " +
                "where table_schema = database() " +
                " and table_name = 'JobUsageRecord_Meta'" +
                " and column_name = 'md5v2'" +
                " and index_name != 'md5v2'";
            Session session = HibernateWrapper.getSession();
            Boolean require_checksum_upgrade = true;
            try {
                SQLQuery q = session.createSQLQuery(checksum_check);
                List results_list = q.list();
                if (! results_list.isEmpty()) {
                    BigInteger non_unique = (BigInteger) results_list.get(0);
                    Logging.debug("CollectorService: received answer: " + non_unique);
                    if ((non_unique != null) && (non_unique.intValue() == 0)) {
                        Logging.debug("CollectorService: found unique index on md5v2 in JobUsageRecord_Meta: no upgrade necessary.");
                        require_checksum_upgrade = false;
                    } else {
                        Logging.debug("CollectorService: found non-unique index on md5v2 in JobUsageRecord_Meta.");
                    }
                } else {
                    Logging.info("CollectorService: No index found on column md5v2 in JobUsageRecord_Meta: not attempting upgrade.");
                    require_checksum_upgrade = false;
                }
                session.close();
            }
            catch (Exception e) {
                Logging.debug("CollectorService: Attempt to check for index on md5v2 in JobUsageRecord_Meta failed!");
                if (session.isOpen()) session.close();
                throw e;
            }

            if (require_checksum_upgrade) {
                Logging.info("CollectorService: starting checksum upgrade thread.");
                ChecksumUpgrader CU = new ChecksumUpgrader(this);
                CU.start();
                Logging.log("CollectorService: ChecksumUpgrader started");
            }

            //
            // poke in rmi
            //

            JMSProxyImpl proxy = new JMSProxyImpl(this);
            Naming.rebind(rmibind + service, proxy);
            Logging.log("JMSProxy Started");

            //
            // start a thread to recheck history directories every 6 hours
            //

            HistoryMonitor historyMonitor = new HistoryMonitor();
            historyMonitor.start();

            //
            // start msg listener
            //

            if (p.getProperty("performance.test") != null)
                {
                    if (p.getProperty("performance.test").equals("false"))
                        {
                            threads = new ListenerThread[maxthreads];
                            for (i = 0; i < maxthreads; i++)
                                {
                                    threads[i] = new ListenerThread("ListenerThread: " + i, queues[i], lock, global);
                                    threads[i].setPriority(Thread.MAX_PRIORITY);
                                    threads[i].setDaemon(true);
                                }
                            for (i = 0; i < maxthreads; i++)
                                threads[i].start();
                        }
                    else
                        {
                            pthreads = new PerformanceThread[maxthreads];
                            for (i = 0; i < maxthreads; i++)
                                {
                                    pthreads[i] = new PerformanceThread("PerformanceThread: " + i, queues[i], lock, global);
                                    pthreads[i].setPriority(Thread.MAX_PRIORITY);
                                    pthreads[i].setDaemon(true);
                                }
                            for (i = 0; i < maxthreads; i++)
                                pthreads[i].start();
                        }
                }
            else
                {
                    threads = new ListenerThread[maxthreads];
                    for (i = 0; i < maxthreads; i++)
                        {
                            threads[i] = new ListenerThread("ListenerThread: " + i, queues[i], lock, global);
                            threads[i].setPriority(Thread.MAX_PRIORITY);
                            threads[i].setDaemon(true);
                        }
                    for (i = 0; i < maxthreads; i++)
                        threads[i].start();
                }

            //
            // if requested - start thread to monitor listener activity
            //
            if (p.getProperty("monitor.listener.threads") != null)
                if (p.getProperty("monitor.listener.threads").equals("true"))
                    {
                        monitorListenerThread = new MonitorListenerThread(global);
                        monitorListenerThread.start();
                        Logging.log("CollectorService: Started MonitorListenerThread");
                    }
            //
            // if requested - start service to monitor input queue sizes
            //

            if (p.getProperty("monitor.q.size").equals("1"))
                {
                    Logging.log("CollectorService: Starting QSizeMonitor");
                    qsizeMonitor = new QSizeMonitor();
                    qsizeMonitor.start();
                }

            /*
              statusListenerThread = new StatusListenerThread();
              statusListenerThread.setDaemon(true);
              statusListenerThread.start();
            */

        }
        catch (Exception e)
            {
                e.printStackTrace();
            }

        //
        // add a server cert if one isn't there
        //

        if (p.getProperty("service.security.level").equals("1"))
            {
                if ((p.getProperty("service.use.selfgenerated.certs") != null) &&
                    (p.getProperty("service.use.selfgenerated.certs").equals("1")))
                    loadSelfGeneratedCerts();
                else
                    loadVDTCerts();
            }

        //
        // start replication service
        //

        replicationService = new ReplicationService();
        replicationService.start();

        //
        // wait 1 minute to create new report config for birt (giving tomcat time to deploy the war)
        //
        //      
        // PENELOPE: no need to setup the reports
        //      (new ReportSetup()).start();

    }


    public synchronized void stopDatabaseUpdateThreads()
    {
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
        try
            {
                long delayMillis = 60*1000; // 60 seconds
                for (i = 0; i < maxthreads; i++) {
                    if (threads[i] != null) threads[i].join(delayMillis);                  
                }
            }
        catch (Exception ignore)
            {
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
            Logging.warning("CollectorService: Some threads ("+unfinished+") have not finished after 60 seconds");
        }
    }

    public synchronized void startDatabaseUpdateThreads()
    {
        if (databaseUpdateThreadsActive()) {
            Logging.info("CollectorService: DB update threads cannot be started -- already active");
            return;
        }
        int i;
        int maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));
        threads = new ListenerThread[maxthreads];
        for (i = 0; i < maxthreads; i++)
            {
                threads[i] = new ListenerThread("ListenerThread: " + i, queues[i], lock, global);
                threads[i].setPriority(Thread.MAX_PRIORITY);
                threads[i].setDaemon(true);
            }
        for (i = 0; i < maxthreads; i++) {
            threads[i].start();
        }

    }

    public synchronized boolean databaseUpdateThreadsActive()
    {
        int i;
        int maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));

        if (threads == null) return false;

        for (i = 0; i < maxthreads; i++) {
            if ((threads[i] != null) && threads[i].isAlive()) return true;
        }
        return false;
    }

    public void loadSelfGeneratedCerts()
    {
        String keystore = System.getProperty("catalina.home") + "/gratia/keystore";
        keystore = xp.replaceAll(keystore, "\\", "/");
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

    public void loadVDTCerts()
    {
        String keystore = System.getProperty("catalina.home") + "/gratia/keystore";
        String configurationPath = System.getProperty("catalina.home") + "/gratia/";
        keystore = xp.replaceAll(keystore, "\\", "/");
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

        if (exitValue1 == 0)
            {
                PKCS12Load load = new PKCS12Load();
                try
                    {
                        load.load(
                                  configurationPath + "server.pkcs12",
                                  keystore
                                  );
                    }
                catch (Exception e)
                    {
                        e.printStackTrace();
                    }
            }
        FlipSSL.flip();
    }

    public void contextDestroyed(ServletContextEvent sce)
    {
        Logging.info("");
        Logging.info("Context Destroy Event");
        Logging.info("");
        System.exit(0);
    }


    public class ReportSetup extends Thread
    {
        public ReportSetup()
        {
        }

        public void run()
        {
            try
                {
                    Thread.sleep(30 * 1000);
                }
            catch (Exception ignore)
                {
                }

            //
            // create a dummy ReportingConfig.xml for birt
            //
            /* PENELOPE: no need to create the file
               String dq = "\"";
               XP xp = new XP();
               StringBuffer xml = new StringBuffer();
               String catalinaHome = System.getProperty("catalina.home");
               catalinaHome = xp.replaceAll(catalinaHome, "\\", "/");

               xml.append("<ReportingConfig>" + "\n");
               xml.append("<DataSourceConfig" + "\n");
               xml.append("url=" + dq + p.getProperty("service.mysql.url") + dq + "\n");
               xml.append("user=" + dq + p.getProperty("service.birt.user") + dq + "\n");
               xml.append("password=" + dq + p.getProperty("service.birt.password") + dq + "\n");
               xml.append("/>" + "\n");
               xml.append("<PathConfig" + "\n");
               xml.append("reportsFolder=" + dq + catalinaHome +
               "/webapps/" + p.getProperty("service.birt.reports.folder") + "/" + dq + "\n");
               xml.append("engineHome=" + dq + catalinaHome +
               "/webapps/" + p.getProperty("service.birt.engine.home") + "/" + dq + "\n");
               xml.append("webappHome=" + dq + catalinaHome +
               "/webapps/" + p.getProperty("service.birt.webapp.home") + "/" + dq + "\n");
               xml.append("/>" + "\n");
               xml.append("</ReportingConfig>" + "\n");
               xp.save(catalinaHome + "/webapps/gratia-report-configuration/ReportingConfig.xml",
               xml.toString());
               Logging.log("ReportConfig updated");
            */
        }
    }

    public class HistoryMonitor extends Thread
    {
        public HistoryMonitor()
        {
        }

        public void run()
        {
            while (true)
                {
                    try
                        {
                            Thread.sleep(6 * 60 * 60 * 1000);
                            new HistoryReaper();
                        }
                    catch (Exception ignore)
                        {
                        }
                }
        }
    }


    //
    // testing
    //

    static public void main(String args[])
    {
        CollectorService service = new CollectorService();
        service.contextInitialized(null);
    }
}

package net.sf.gratia.services;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.util.*;
import java.io.*;

import net.sf.gratia.storage.*;

public class HibernateWrapper {
    static Properties p = null;

    static org.hibernate.cfg.Configuration hibernateConfiguration;
    static org.hibernate.SessionFactory hibernateFactory;

    public static boolean databaseDown = true;

    public static synchronized org.hibernate.cfg.Configuration getHibernateConfiguration() {
        return hibernateConfiguration;
    }

    public static synchronized void start() throws Exception {
        Properties ep = new Properties();
        ep.setProperty("hibernate.hbm2ddl.auto", "validate");
        start(ep);
    }

    public static synchronized void startMaster() throws Exception {
        Properties ep = new Properties();
        ep.setProperty("hibernate.hbm2ddl.auto", "update");
        start(ep);
    }

    private static synchronized void start(Properties ep) throws Exception {
        if (p != null) return; // Already done

        p = net.sf.gratia.util.Configuration.getProperties();

        // String configurationPath = net.sf.gratia.util.Configuration.getConfigurationPath();

        try {
            hibernateFactory.close();
        }
        catch (Exception ignore) {
        }

        try {
            hibernateConfiguration = new org.hibernate.cfg.Configuration();
            hibernateConfiguration.addDirectory(new File(net.sf.gratia.util.Configuration.getConfigurationPath()));
            //          hibernateConfiguration.addFile(new File(net.sf.gratia.util.Configuration.getGratiaHbmPath()));
            //          hibernateConfiguration.addFile(new File(net.sf.gratia.util.Configuration.getJobUsagePath()));
            //          hibernateConfiguration.addFile(new File(net.sf.gratia.util.Configuration.getMetricRecordPath()));
            //          hibernateConfiguration.addFile(new File(net.sf.gratia.util.Configuration.getJobUsageSummaryPath()));
            //          hibernateConfiguration.addFile(new File(net.sf.gratia.util.Configuration.getTracePath()));
            //          hibernateConfiguration.addFile(new File(net.sf.gratia.util.Configuration.getNodeSummaryPath()));

            hibernateConfiguration.configure(new File(net.sf.gratia.util.Configuration.getHibernatePath()));

            hibernateConfiguration.addProperties(ep); // Provided by init.

            Properties hp = new Properties();
            hp.setProperty("hibernate.connection.driver_class", p.getProperty("service.mysql.driver"));
            hp.setProperty("hibernate.connection.url", p.getProperty("service.mysql.url"));
            hp.setProperty("hibernate.connection.username", p.getProperty("service.mysql.user"));
            hp.setProperty("hibernate.connection.password", p.getProperty("service.mysql.password"));
                
            hibernateConfiguration.addProperties(hp);

            hibernateFactory = hibernateConfiguration.buildSessionFactory();

            Logging.info("HibernateWrapper: Hibernate Services Started");

            databaseDown = false;
        }
        catch (Exception databaseError) {
            Logging.warning("HibernateWrapper: Error Starting Hibernate", databaseError);
            databaseDown = true;
            throw databaseError; // Rethrow
        }
    }

    public static boolean systemDatabaseUp() {
        int i = 0;

        String command = "select dbid from JobUsageRecord JUR where JUR.dbid = 1";
        try {
            org.hibernate.Session session = hibernateFactory.openSession();
            List result = session.createQuery(command).list();
            for (i = 0; i < result.size(); i++) {
                JobUsageRecord record = (JobUsageRecord)result.get(i);
            }
            session.close();
            databaseDown = false;
            return true;
        }
        catch (Exception e) {
            databaseDown = true;
            return false;
        }
    }

    public static synchronized boolean databaseUp() {
        int i = 0;

        String command = "from JobUsageRecord J where J.RecordId = 1";
        try {
            Logging.log("HibernateWrapper: Database Check");
            org.hibernate.Session session = hibernateFactory.openSession();
            List result = session.createQuery(command).list();
            for (i = 0; i < result.size(); i++) {
                JobUsageRecord record = (JobUsageRecord)result.get(i);
            }
            session.close();
            databaseDown = false;
            Logging.log("HibernateWrapper: Database Check. Database Up.");
            return true;
        }
        catch (Exception e) {
            databaseDown = true;
            Logging.log("HibernateWrapper: Database Check: Database Down");
            e.printStackTrace();
            return false;
        }
    }

    public static synchronized org.hibernate.Session getSession() {
        try {
            org.hibernate.Session session = hibernateFactory.openSession();
            return session;
        }
        catch (Exception e) {
            databaseDown = true;
            Logging.log("HibernateWrapper: Get Session: Database Down");
            return null;
        }
    }
}

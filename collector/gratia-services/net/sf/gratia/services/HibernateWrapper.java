package net.sf.gratia.services;

// import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Logging;

import java.util.*;
import java.io.*;

public class HibernateWrapper {
    private static class ConnectionChecker implements org.hibernate.jdbc.Work {
        private boolean fConnectionStatus = false;

        ConnectionChecker() {
        }

        public synchronized boolean isFullyConnected(org.hibernate.Session session)
            throws java.sql.SQLException {
            if (session.connection() == null) {
                Logging.debug("ConnectionChecker.isFullyConnected: session.connection() is null.");
                fConnectionStatus = false;
            } else {
                session.doWork(this);
            }
            Logging.debug("ConnectionChecker.isFullyConnected returning " + fConnectionStatus);
            return fConnectionStatus;
        }

        public synchronized void execute(java.sql.Connection conn)
            throws java.sql.SQLException { // Required method 
            // Set connection status.
            Logging.debug("ConnectionChecker.execute called");
            if (conn == null) {
                Logging.fine("ConnectionChecker.execute: Connection is null!");
            }
            fConnectionStatus = (conn!=null) && (!conn.isClosed());
        }
    }

    static org.hibernate.cfg.Configuration hibernateConfiguration;
    static org.hibernate.SessionFactory hibernateFactory;

    static private Boolean hibernateInitialized = false;
    static private Properties initProperties = null;
    static private ConnectionChecker cChecker = new ConnectionChecker();

    public static synchronized org.hibernate.cfg.Configuration getHibernateConfiguration() {
        return hibernateConfiguration;
    }

    public static synchronized void start() throws Exception {
        startImpl();
    }

    public static synchronized void startMaster() throws Exception {
        initProperties = new Properties();
        initProperties.setProperty("hibernate.hbm2ddl.auto", "update");
        startImpl();
    }

    private static synchronized void startImpl() throws Exception {
        if (hibernateInitialized) {
            databaseUp();
            return;
        }

        try {
            if (hibernateFactory != null) {
                hibernateFactory.close();
            }
        } catch  (Exception ignore) {
        }

        try {
            hibernateConfiguration = new org.hibernate.cfg.Configuration();
            hibernateConfiguration.addDirectory(new File(net.sf.gratia.util.Configuration.getHibernateConfigurationPath()));

            hibernateConfiguration.configure(new File(net.sf.gratia.util.Configuration.getHibernatePath()));

            if (initProperties != null) {
                hibernateConfiguration.addProperties(initProperties); // Provided by init.
            }

            Properties p = net.sf.gratia.util.Configuration.getProperties();

            Properties hp = new Properties();
            hp.setProperty("hibernate.connection.driver_class", p.getProperty("service.mysql.driver"));
            hp.setProperty("hibernate.connection.url", p.getProperty("service.mysql.url"));
            hp.setProperty("hibernate.connection.username", p.getProperty("service.mysql.user"));
            hp.setProperty("hibernate.connection.password", p.getProperty("service.mysql.password"));
                
            hibernateConfiguration.addProperties(hp);

            hibernateFactory = hibernateConfiguration.buildSessionFactory();

            Logging.info("HibernateWrapper: Hibernate Services Started");
        }
        catch (Exception databaseError) {
            Logging.warning("HibernateWrapper: Error Starting Hibernate", databaseError);
            throw databaseError; // Rethrow
        }
        hibernateInitialized = true;
    }

    public static synchronized boolean databaseUp() {
        try {
            if (!hibernateInitialized) {
                startImpl();
            }
            Logging.log("HibernateWrapper: Database Check");
            org.hibernate.Session session = hibernateFactory.openSession();
            Boolean connected = isFullyConnected(session);
            session.close();
            if (connected) {
                Logging.log("HibernateWrapper: Database Check. Database Up.");
            } else {
                Logging.info("HibernateWrapper: Database Check: Database Down");
            }
            return connected;
        }
        catch (Exception e) {
            Logging.info("HibernateWrapper: Database Check: Database Down");
            Logging.debug("Exception details: ", e);
            return false;
        }
    }

    public static synchronized org.hibernate.Session getSession() { // Fast
        try {
            if (!hibernateInitialized) {
                startImpl();
            }
            org.hibernate.Session session = hibernateFactory.openSession();
            return session;
        }
        catch (Exception e) {
            Logging.info("HibernateWrapper: Get Session: Database Down");
            return null;
        }
    }
   
    public static synchronized org.hibernate.Session getCheckedSession() { // Slower
        try {
            if (!hibernateInitialized) {
                startImpl();
            }
            org.hibernate.Session session = hibernateFactory.openSession();
            if (!isFullyConnected(session)) {
                // Try one more time.
                Logging.log("HibernateWrapper: Get Session: initial attempt to get session failed; trying one more time");
                session = hibernateFactory.openSession();
                if (!isFullyConnected(session)) {
                    // Failed.
                    Logging.info("HibernateWrapper: Get Session: Database Down");
                    return null;
                }
                Logging.log("HibernateWrapper: Get Session: retry succeeded.");
            }
            return session;
        }
        catch (Exception e) {
            Logging.info("HibernateWrapper: Get Session: Database Down");
            return null;
        }
    }
   
   public static boolean isFullyConnected(org.hibernate.Session session) {
       // Return true if the session is fully useable (i.e rolling back a
       // transaction won't throw because something is 'already closed').

       if (session != null) {
           if (session.isOpen() && session.isConnected()) {
               try { 
                   return cChecker.isFullyConnected(session);
               } catch (java.sql.SQLException e) {
                   Logging.debug("HibernateWrapper::isFullyConnected caught SQLException: " + e.getMessage());
                   return false;
               } catch (org.hibernate.exception.JDBCConnectionException e) {
                   Logging.debug("HibernateWrapper::isFullyConnected caught JDBCConnectionException: " + e.getMessage());
                   return false;
               } catch (Exception e) {
                   Logging.info("HibernateWrapper::isFullyConnected unexcepted exception: "+e.getMessage());
                   Logging.debug("Exception details: ", e);
               }
           }
       }
       return false;
   }

}

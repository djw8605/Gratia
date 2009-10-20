package net.sf.gratia.services;

// import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Logging;

import java.util.*;
import java.io.*;

public class HibernateWrapper {
    static org.hibernate.cfg.Configuration hibernateConfiguration;
    static org.hibernate.SessionFactory hibernateFactory;

    static private Boolean hibernateInitialized = false;
    static private Properties initProperties = null;

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
            systemDatabaseUp();
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

    public static boolean systemDatabaseUp() {
        try {
            if (!hibernateInitialized) {
                startImpl();
            }
            org.hibernate.Session session = hibernateFactory.openSession();
            Boolean connected = isFullyConnected(session);
            session.close();
            return connected;
        }
        catch (Exception e) {
            return false;
        }
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

    public static synchronized org.hibernate.Session getSession() {
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
   
   public static boolean isFullyConnected(org.hibernate.Session session) {
      // Return true if the session is fully useable (i.e rolling back a
      // transaction won't throw because something is 'already closed').
      
      if (session != null) {
         if (session.isOpen() && session.isConnected()) {
            java.sql.Connection conn = session.connection();
            try { 
               return conn!=null && !conn.isClosed();
            } catch (java.sql.SQLException e) {
               return false;
            } catch (org.hibernate.exception.JDBCConnectionException e) {
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

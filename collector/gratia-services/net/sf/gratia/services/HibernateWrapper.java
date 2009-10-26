package net.sf.gratia.services;

// import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Logging;

import java.util.*;
import java.io.*;

public class HibernateWrapper {
   private static class ConnectionChecker implements org.hibernate.jdbc.Work {
      private final boolean fExtraCheck;
      
      private static class ConnectionException extends java.sql.SQLException {
         public ConnectionException(String msg) {
            super(msg);
         }
      }
      
      ConnectionChecker() {
         fExtraCheck = false;
      }
      
      ConnectionChecker(boolean extra) {
         fExtraCheck = extra;
      }
      
      public synchronized boolean isFullyConnected(org.hibernate.Session session)
      {
         // Check if the connection is valid or not.
         boolean connectionStatus = false;
         try {
            session.doWork(this);
            connectionStatus = true;
            Logging.debug("ConnectionChecker.isFullyConnected returning without exception.");
         } catch (org.hibernate.HibernateException e) {
            java.lang.Throwable next = e.getCause();
            String inside = "";
            boolean expected = false;
            if (next != null) {
               expected = (next instanceof ConnectionException || next instanceof com.mysql.jdbc.CommunicationsException);
               inside = " (" + next.getMessage() + ")";
            }
            if (!expected) {
               Logging.fine("ConnectionChecker.isFullyConnected caught an unexpected exception: " + e.getMessage() + inside);
               Logging.debug("ConnectionChecker.isFullyConnected caught an unexpected exception details: ",e);
            } else {
               Logging.debug("ConnectionChecker.isFullyConnected caught an exception: " + e.getMessage() + inside);
            }
         }
         return connectionStatus;
      }
      
      public synchronized void execute(java.sql.Connection conn) throws java.sql.SQLException { // Required method 
         // Set connection status.
         Logging.debug("ConnectionChecker.execute called");
         if (conn == null) {
            Logging.fine("ConnectionChecker.execute: Connection is null!");
            throw new ConnectionException("No JDBC connection");
         } else if (!conn.isClosed()) {
            if (fExtraCheck) {
               // No try catch here; we really want the exception to pass
               // through in case of error.
               Logging.fine("ConnectionChecker.execute: Testing connection validity.");
               java.sql.PreparedStatement statement = conn.prepareStatement("select 1");
               statement.execute();
               statement.close();
            }
         } else {
            // Throws an exception to prevent 'doWork' from 
            // putting the broken connection back in Hibernate's 
            // ConnectionManager's connection pool.
            Logging.fine("ConnectionChecker.execute: Connection is already closed!");
            throw new ConnectionException("JDBC connection is already closed");
         }
      }
   }
   
   static org.hibernate.cfg.Configuration hibernateConfiguration;
   static org.hibernate.SessionFactory hibernateFactory;
   
   static private Boolean hibernateInitialized = false;
   static private Properties initProperties = null;
   static private ConnectionChecker cChecker = new ConnectionChecker();
   static private ConnectionChecker cTester = new ConnectionChecker(true);
   
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
         if (connected) {
            // Only close the session if we are fully connected.
            // so that we do not recirculate the 'broken' connection.
            session.close();
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
      return getSession(null);
   }
   
   public static synchronized org.hibernate.Session getSession(org.hibernate.CacheMode mode) { // Fast
      try {
         if (!hibernateInitialized) {
            startImpl();
         }
         org.hibernate.Session session = hibernateFactory.openSession();
         if (mode != null) {
            session.setCacheMode(mode);
         }
         return session;
      }
      catch (Exception e) {
         Logging.info("HibernateWrapper: Get Session: Database Down");
         return null;
      }
   }
   
   public static synchronized org.hibernate.Session getCheckedSession() { // Slower
      return getCheckedSession(null);
   }
   
   public static synchronized org.hibernate.Session getCheckedSession(org.hibernate.CacheMode mode) { // Slower
      try {
         if (!hibernateInitialized) {
            startImpl();
         }
         org.hibernate.Session session = hibernateFactory.openSession();
         if (!isFullyConnected(session,true)) {
            // Try one more time.
            Logging.log("HibernateWrapper: Get Session: initial attempt to get session failed; trying one more time");
            session = hibernateFactory.openSession();
            if (!isFullyConnected(session,true)) {
               // Failed.
               Logging.info("HibernateWrapper: Get Session: Database Down");
               return null;
            }
            Logging.log("HibernateWrapper: Get Session: retry succeeded.");
         }
         if (mode != null) {
            session.setCacheMode(mode);
         }
         return session;
      }
      catch (Exception e) {
         Logging.info("HibernateWrapper: Get Session: Database Down");
         return null;
      }
   }
   
   public static boolean isFullyConnected(org.hibernate.Session session, boolean testconnection) {
      // Return true if the session is fully useable (i.e rolling back a
      // transaction won't throw because something is 'already closed').
      
      if (session != null) {
         if (session.isOpen() && session.isConnected()) {
            try { 
               if (testconnection) {
                  return cTester.isFullyConnected(session);
               } else {
                  return cChecker.isFullyConnected(session);
               }
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
   
   public static boolean isFullyConnected(org.hibernate.Session session) {
      // Return true if the session is fully useable (i.e rolling back a
      // transaction won't throw because something is 'already closed').
      
      return isFullyConnected(session,false);
   }
   
   public static void closeSession(org.hibernate.Session session) {
      // Rollback the current transaction (if any) and close the session
      // (unless the connection is no longer valid in which case we must 
      // not close the session to prevent the connection from being re-used).
      
      if (isFullyConnected(session)) {
         org.hibernate.Transaction tx = session.getTransaction();
         if (tx != null && tx.isActive()) tx.rollback();
         session.close();
      }
   }      
}

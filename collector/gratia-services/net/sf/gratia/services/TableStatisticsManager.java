package net.sf.gratia.services;

import net.sf.gratia.storage.ExpirationDateCalculator;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;
import net.sf.gratia.util.LogLevel;

import java.util.*;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Query;

public class TableStatisticsManager extends Thread {
   
   CollectorService fService;
   long fWait;
   ExpirationDateCalculator fExpCalc = new ExpirationDateCalculator();
   Date fLastHourly = new Date(0);  // Time we last did the hourly check.  We initiliaze it to now, to delay until one hour after the start
   Date fLastDaily = new Date(0);   // Time we last did the daily check.  We initiliaze it to now, to delay until one dayt after the start
   Date fLastCleanup = new Date(0); // Time we last did the cleanup.  We initiliaze it to now, to delay until a while after the start

   static final String fgTakeSnapShotCommand = "insert into TableStatisticsSnapshots (ServerDate, ValueType, RecordType, Qualifier, nRecords) " +
                                                  "select :currentTime, ValueType, RecordType, Qualifier, nRecords from TableStatistics";
   static final String fgUpdateHourlyCommand = "call table_statictics_hourly_summary(:from,:upto)";
   static final String fgUpdateDailyCommand  = "call table_statictics_daily_summary(:from,:upto)";

   private long getPropertiesValue(Properties p, String valuename, long defaultValue)
   {
      try {
         String value = p.getProperty(valuename);
         if (value == null) {
            return defaultValue;
         }
         return Long.parseLong(value);
      }
      catch (java.lang.NumberFormatException e) {
         Logging.warning("TableStatisticsManager: Unable to read configuration item "+valuename,e);
      }
      return defaultValue;
   }

   public TableStatisticsManager(CollectorService service) {

      fService = service;

      Properties p = Configuration.getProperties();

      fWait = getPropertiesValue(p,"tableStatistics.snapshots.wait",1) * 60 * 1000;      
      Logging.info("TableStatisticsManager: using wait of "+fWait / 1000 + " seconds.");
   }
   
   public void run() {
      Logging.fine("TableStatisticsManager: Started");
      while (true) {
         takeSnapshot();

         Date now = new Date();
         if ( (now.getTime() - fLastHourly.getTime()) > 3600*1000 / 60 ) {
            checkHourly();
         }
         if ( (now.getTime() - fLastDaily.getTime()) > 24*3600*1000 ) {
            checkDaily();
         }
         if ( (now.getTime() - fLastCleanup.getTime()) > 24*3600*1000 ) {
            cleanupOldRecords();
         }

         try {
            Thread.sleep(fWait);
         }
         catch (Exception e) {
         }
      }
   }
   
   public void takeSnapshot() 
   {
      Logging.log("TableStatisticsManager: takeSnapshot");

      Boolean keepTrying  = true;
      Integer nTries = 0;
      Session session = HibernateWrapper.getSession();
      try {
         Transaction tx = session.beginTransaction();
         if (++nTries > 1) {
            Thread.sleep(300);
         }
         
         org.hibernate.SQLQuery query = session.createSQLQuery( fgTakeSnapShotCommand );
         query.setTimestamp( "currentTime",new Date());

         Logging.info("takeSnapshot: About to execute " + query.getQueryString());
         long updated = query.executeUpdate();
         
         session.flush();
         tx.commit();
         keepTrying = false;
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "registerInput")) {
            Logging.warning("takeSnapshot: error when updating TableStatisticSnapshot table",e);
            keepTrying = false;
         }
      }
      
   }

   public void checkHourly() 
   {
      Logging.fine("TableStatisticsManager: checkHourly");

      Boolean keepTrying  = true;
      Integer nTries = 0;
      Session session = HibernateWrapper.getSession();
      try {
         Transaction tx = session.beginTransaction();
         if (++nTries > 1) {
            Thread.sleep(300);
         }
         
         // Figure out what we need.
         final String fromcmd = "select max(EventDate) from TableStatisticsHourly";
         Date from = null;
         try {
            Logging.log("Executing: " + fromcmd);
            Query rq = session.createSQLQuery(fromcmd);
            List res = rq.list();
            if (res.isEmpty()) {
               from = new Date(0); // Intentionally very very old date.
            } else {
               from = (Date)res.get(0);
               if (from == null) {
                  from = new Date(0); // Intentionally very very old date.
               }
            }
         } catch (Exception e) {
            HibernateWrapper.closeSession(session);
            Logging.warning("Command: Error: " + fromcmd + " : ",e);
            return;
         }

         final String uptocmd = "select max(ServerDate) from TableStatisticsSnapshots";
         Date upto = null;
         try {
            Logging.log("Executing: " + uptocmd);
            Query rq = session.createSQLQuery(uptocmd);
            upto = (Date)rq.uniqueResult();
         } catch (Exception e) {
            HibernateWrapper.closeSession(session);
            Logging.warning("Command: Error: " + uptocmd + " : ",e);
            return;
         }
         if (upto == null) {
            HibernateWrapper.closeSession(session);
            return;            
         }
         // Round down to the previous hour.
         upto.setMinutes(00);
         upto.setSeconds(00);
            
         org.hibernate.SQLQuery query = session.createSQLQuery( fgUpdateHourlyCommand );
         query.setParameter( "from", from );
         query.setParameter( "upto", upto );
         
         Logging.info("checkHourly: About to execute from : " + from + " to " + upto);
         Logging.info("checkHourly: About to execute " + query.getQueryString());
         long updated = query.executeUpdate();
         
         session.flush();
         tx.commit();
         keepTrying = false;
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "registerInput")) {
            Logging.warning("takeSnapshot: error when updating TableStatisticSnapshot table.", e);
            keepTrying = false;
         }
         return;
      }

      // fLastHourly = new Date();
   }

   public void checkDaily() 
   {
      Logging.fine("TableStatisticsManager: checkDaily");
      
      Boolean keepTrying  = true;
      Integer nTries = 0;
      Session session = HibernateWrapper.getSession();
      try {
         Transaction tx = session.beginTransaction();
         if (++nTries > 1) {
            Thread.sleep(300);
         }
         
         // Figure out what we need.
         final String fromcmd = "select max(EventDate) from TableStatisticsDaily";
         Date from = null;
         try {
            Logging.log("Executing: " + fromcmd);
            Query rq = session.createSQLQuery(fromcmd);
            List res = rq.list();
            if (res.isEmpty()) {
               from = new Date(0); // Intentionally very very old date.
            } else {
               from = (Date)res.get(0);
               if (from == null) {
                  from = new Date(0); // Intentionally very very old date.
               }
            }
         } catch (Exception e) {
            HibernateWrapper.closeSession(session);
            Logging.warning("Command: Error: " + fromcmd + " : ",e);
            return;
         }
         
         final String uptocmd = "select max(ServerDate) from TableStatisticsHourly";
         Date upto = null;
         try {
            Logging.log("Executing: " + uptocmd);
            Query rq = session.createSQLQuery(uptocmd);
            upto = (Date)rq.uniqueResult();
         } catch (Exception e) {
            HibernateWrapper.closeSession(session);
            Logging.warning("Command: Error: " + uptocmd + " : ",e);
            return;
         }
         if (upto == null) {
            HibernateWrapper.closeSession(session);
            return;            
         }
         // Round down to the previous day.
         upto.setHours(00);
         upto.setMinutes(00);
         upto.setSeconds(00);
         
         org.hibernate.SQLQuery query = session.createSQLQuery( fgUpdateDailyCommand );
         query.setParameter( "from", from );
         query.setParameter( "upto", upto );
         
         Logging.info("checkDaily: About to execute from : " + from + " to " + upto);
         Logging.info("checkDaily: About to execute " + query.getQueryString());
         long updated = query.executeUpdate();
         
         session.flush();
         tx.commit();
         keepTrying = false;
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "registerInput")) {
            Logging.warning("checkDaily: error when updating TableStatisticSnapshot table.", e);
            keepTrying = false;
         }
         return;
      }
      
      fLastDaily = new Date();
   }

   
   private long deleteRecords(Session session, String table, String datefield)
   {
      String limit = fExpCalc.expirationDateAsSQLString(new Date(), table);
      long nrecords = 0;
      if (limit.length() > 0) {
         String cmd = "delete from " + table + " where :dateLimit > " + datefield;
         org.hibernate.SQLQuery query = session.createSQLQuery( cmd );
         query.setString("dateLimit", limit);
         
         Logging.debug("takeSnapshot: About to delete with " + query.getQueryString());
         nrecords = query.executeUpdate();
         Logging.info("TableStatisticsManager: deleted " + nrecords + " " + table + " records.");
      }
      return nrecords;
   }
   
   public void cleanupOldRecords()
   {
      Logging.log("TableStatisticsManager: cleanupOldRecords");
      
      Session session = HibernateWrapper.getSession();
      try {
         Transaction tx = session.beginTransaction();
         
         deleteRecords(session,"TableStatisticsSnapshots","ServerDate");
         deleteRecords(session,"TableStatisticsHourly","EventDate");
         deleteRecords(session,"TableStatisticsDaily","EventDate");
      
         session.flush();
         tx.commit();
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         Logging.warning("takeSnapshot: error when cleaning TableStatistics tables.", e);
         return;
      }

      fLastCleanup = new Date();
   }
}




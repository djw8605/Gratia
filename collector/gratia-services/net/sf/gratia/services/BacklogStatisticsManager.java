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

public class BacklogStatisticsManager extends Thread {
   
   CollectorService fService;
   long fWait;
   long fBundleSize = 0;
   long fQueueSize = 0;
   ExpirationDateCalculator fExpCalc = new ExpirationDateCalculator();
    // For debugging we keep those at 'beggining of time' rather than now.
   Date fLastHourly = new Date(0);  // Time we last did the hourly check.  We initialize it to now, to delay until one hour after the start
   Date fLastDaily = new Date(0);   // Time we last did the daily check.  We initialize it to now, to delay until one dayt after the start
   Date fLastCleanup = new Date(0); // Time we last did the cleanup.  We initialize it to now, to delay until a while after the start

   static final String fgUpdateBacklogCommand = "insert into BacklogStatistics " + 
                                                   "(ServerDate, EntityType, Name, nRecords, xmlFiles, tarFiles, serviceBacklog, maxPendingFiles, bundleSize ) " +
                                                   "values(UTC_TIMESTAMP(), ?, ?, ?,?,?,?,?,?)" +
                                                   " on duplicate key update prevServerDate = ServerDate, prevRecords = nRecords, prevServiceBacklog = serviceBacklog, " +
                                                   " ServerDate = UTC_TIMESTAMP(), nRecords = ?, xmlFiles = ?, tarFiles = ?, serviceBacklog = ?, maxPendingFiles = ?, bundleSize = ?";
//                                                   "values(UTC_TIMESTAMP(), :entityType, :name, :nrecords, :xmlfiles, :tarfiles, :serviceBacklog, :maxPendingFiles, :bundleSize)";
   static final String fgGetTakeSnapshotLimit = "select max(ServerDate) from BacklogStatisticsSnapshots";
   static final String fgTakeSnapShotCommand = "insert into BacklogStatisticsSnapshots " + 
                                                  "(ServerDate, EventDate, EntityType, Name, nRecords, xmlFiles, tarFiles, serviceBacklog, maxPendingFiles, bundleSize ) " +
                                                  "select :currentTime, ServerDate, EntityType, Name, nRecords, xmlFiles, tarFiles, serviceBacklog, maxPendingFiles, bundleSize "+ 
                                                  "from BacklogStatistics where ServerDate >= :datelimit";
   static final String fgUpdateHourlyCommand = "call backlog_statictics_hourly_summary(:from,:upto)";
   static final String fgUpdateDailyCommand  = "call backlog_statictics_daily_summary(:from,:upto)";

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
         Logging.warning("BacklogStatisticsManager: Unable to read configuration item "+valuename,e);
      }
      return defaultValue;
   }

   public BacklogStatisticsManager(CollectorService service) {

      fService = service;

      Properties p = Configuration.getProperties();

      fWait = getPropertiesValue(p,"backlogStatistics.snapshots.wait",1) * 60 * 1000;      
      fBundleSize = getPropertiesValue(p,"service.datapump.bundlesize",10);      
      fQueueSize = getPropertiesValue(p,"max.q.size",100000);
      Logging.info("BacklogStatisticsManager: using wait of "+fWait / 1000 + " seconds.");
   }
   
   public void run() {
      Logging.fine("BacklogStatisticsManager: Started");
      while (true) {
         takeSnapshot();

         Date now = new Date();
         if ( (now.getTime() - fLastHourly.getTime()) > 3600*1000 ) {
            checkHourly();
         }
         if ( (now.getTime() - fLastDaily.getTime()) > 24*3600*1000 ) {
            checkDaily();
         }
         if ( (now.getTime() - fLastCleanup.getTime()) > 24*3600*1000 ) {
            cleanupOldRecords();
         }
         updateLocalBacklog();

         try {
            Thread.sleep(fWait);
         }
         catch (Exception e) {
         }
      }
   }
   
   public void takeSnapshot() 
   {
      Logging.log("BacklogStatisticsManager: takeSnapshot");

      Boolean keepTrying  = true;
      Integer nTries = 0;
      Session session = HibernateWrapper.getSession();
      try {
         Transaction tx = session.beginTransaction();
         if (++nTries > 1) {
            Thread.sleep(300);
         }
         
         org.hibernate.SQLQuery query = session.createSQLQuery( fgGetTakeSnapshotLimit );
         List res = query.list();
         Date from = null;
         if (res.isEmpty()) {
            from = new Date(0); // Intentionally very very old date.
         } else {
            from = (Date)res.get(0);
            if (from == null) {
               from = new Date(0); // Intentionally very very old date.
            }
         }

         query = session.createSQLQuery( fgTakeSnapShotCommand );
         query.setTimestamp( "currentTime", new Date() );
         query.setTimestamp( "datelimit", from );

         Logging.info("takeSnapshot: About to execute " + query.getQueryString() + "; with dateLimit = " + from);
         long updated = query.executeUpdate();
         
         session.flush();
         tx.commit();
         keepTrying = false;
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "takeSnapshot")) {
            Logging.warning("takeSnapshot: error when updating BacklogStatisticSnapshot table",e);
            keepTrying = false;
         }
      }
      
   }

   public void checkHourly() 
   {
      Logging.fine("BacklogStatisticsManager: checkHourly");

      Boolean keepTrying  = true;
      Integer nTries = 0;
      Session session = HibernateWrapper.getSession();
      try {
         Transaction tx = session.beginTransaction();
         if (++nTries > 1) {
            Thread.sleep(300);
         }
         
         // Figure out what we need.
         final String fromcmd = "select max(EventDate) from BacklogStatisticsHourly";
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

         final String uptocmd = "select max(ServerDate) from BacklogStatisticsSnapshots";
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
         
         Logging.info("BacklogStatisticsManager checkHourly: About to execute from : " + from + " to " + upto);
         Logging.info("BacklogStatisticsManager checkHourly: About to execute " + query.getQueryString());
         long updated = query.executeUpdate();
         
         session.flush();
         tx.commit();
         keepTrying = false;
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "checkHourly")) {
            Logging.warning("BacklogStatisticsManager checkHourly: error when updating BacklogStatisticSnapshot table.", e);
            keepTrying = false;
         }
         return;
      }

      fLastHourly = new Date();
   }

   public void checkDaily() 
   {
      Logging.fine("BacklogStatisticsManager: checkDaily");
      
      Boolean keepTrying  = true;
      Integer nTries = 0;
      Session session = HibernateWrapper.getSession();
      try {
         Transaction tx = session.beginTransaction();
         if (++nTries > 1) {
            Thread.sleep(300);
         }
         
         // Figure out what we need.
         final String fromcmd = "select max(EventDate) from BacklogStatisticsDaily";
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
         
         final String uptocmd = "select max(ServerDate) from BacklogStatisticsHourly";
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
         
         Logging.info("BacklogStatisticsManager checkDaily: About to execute from : " + from + " to " + upto);
         Logging.info("BacklogStatisticsManager checkDaily: About to execute " + query.getQueryString());
         long updated = query.executeUpdate();
         
         session.flush();
         tx.commit();
         keepTrying = false;
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "checkDaily")) {
            Logging.warning("checkDaily: error when updating BacklogStatisticSnapshot table.", e);
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
         Logging.info("BacklogStatisticsManager: deleted " + nrecords + " " + table + " records.");
      }
      return nrecords;
   }
   
   public void cleanupOldRecords()
   {
      Logging.log("BacklogStatisticsManager: cleanupOldRecords");
      
      Session session = HibernateWrapper.getSession();
      try {
         Transaction tx = session.beginTransaction();
         
         deleteRecords(session,"BacklogStatisticsSnapshots","ServerDate");
         deleteRecords(session,"BacklogStatisticsHourly","EventDate");
         deleteRecords(session,"BacklogStatisticsDaily","EventDate");
      
         session.flush();
         tx.commit();
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         Logging.warning("takeSnapshot: error when cleaning BacklogStatistics tables.", e);
         return;
      }

      fLastCleanup = new Date();
   }
   
   private void updateBacklogImp(String entityType, String name, long nrecords, long xmlfiles, long tarfiles, long backlog, long maxpendingfiles, long bundlesize)
   {
      Boolean keepTrying  = true;
      Integer nTries = 0;
      Session session = HibernateWrapper.getSession();
      try {
         Transaction tx = session.beginTransaction();
         if (++nTries > 1) {
            Thread.sleep(300);
         }
         org.hibernate.SQLQuery query = session.createSQLQuery( fgUpdateBacklogCommand );
         query.setString(0,entityType);
         query.setString(1,name);
         query.setLong(2,nrecords);
         query.setLong(3,xmlfiles);
         query.setLong(4,tarfiles);
         query.setLong(5,backlog);
         query.setLong(6,maxpendingfiles);
         query.setLong(7,bundlesize);
         
         query.setLong(8,nrecords);
         query.setLong(9,xmlfiles);
         query.setLong(10,tarfiles);
         query.setLong(11,backlog);
         query.setLong(12,maxpendingfiles);
         query.setLong(13,bundlesize);
         
         // Logging.info("updateBacklog: About to execute " + query.getQueryString());
         long updated = query.executeUpdate();
         
         session.flush();
         tx.commit();
         keepTrying = false;
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "updateBacklog")) {
            Logging.warning("updateBacklog: error when updating BacklogStatistics table.", e);
            keepTrying = false;
         }
         return;
      }
   }
   
   static final String fgRemoteBacklog = "select sum(nRecords)+sum(serviceBacklog) from BacklogStatistics where EntityType != 'Local' and Name != '"+CollectorService.getName()+"'";
   public long getServiceBacklog() 
   {
      // Logging.log("BacklogStatisticsManager: getServiceBacklog");
      
      long serviceBacklog = 0;
      Boolean keepTrying  = true;
      Integer nTries = 0;
      Session session = HibernateWrapper.getSession();
      try {
         Transaction tx = session.beginTransaction();
         if (++nTries > 1) {
            Thread.sleep(300);
         }
         
         org.hibernate.SQLQuery query = session.createSQLQuery( fgRemoteBacklog );
         Logging.info("getServiceBacklog: About to execute " + query.getQueryString());
         List res = query.list();
         if (!res.isEmpty()) {
            Object value = res.get(0);
            if (value != null) {
               serviceBacklog = ((java.math.BigDecimal)value).longValue();
            }
         }
         session.flush();
         tx.commit();
         keepTrying = false;
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "getServiceBacklog")) {
            Logging.warning("getServiceBacklog: error when doing getServiceBacklog table",e);
            keepTrying = false;
         }
      }
      return serviceBacklog;
   }
   
   protected void updateLocalBacklog()
   {
      // Gather the info

      long backlog = 0;
      int nQueues = QueueManager.getNumberOfQueues();
      long nRecords = 0;
      long xmlFiles = 0;
      long serviceBacklog = getServiceBacklog();
      for (int i = 0; i < nQueues; i++) {
         QueueManager.Queue q = QueueManager.getQueue(i);
         nRecords += q.getNRecords();
         xmlFiles += q.getNFiles();
      }
      updateBacklogImp("local",CollectorService.getName(),nRecords,xmlFiles,0,serviceBacklog,fQueueSize,fBundleSize);
   }
   
   public void updateBacklog(String name, long nrecords, long xmlfiles, long tarfiles, long backlog, long maxpendingfiles, long bundlesize)
   {
      // Register the current backlog status of the incoming probe or collector.
      if (name.startsWith("collector:")) {
         updateBacklogImp("collector",name,nrecords,xmlfiles,tarfiles,backlog,maxpendingfiles,bundlesize);
      } else {
         updateBacklogImp("probe",name,nrecords,xmlfiles,tarfiles,backlog,maxpendingfiles,bundlesize);
      }         
   }
   
}




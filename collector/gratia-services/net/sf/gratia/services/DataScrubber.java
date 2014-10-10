package net.sf.gratia.services;

import net.sf.gratia.storage.ExpirationDateCalculator;
import net.sf.gratia.util.Logging;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.*;
import java.math.BigInteger;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class DataScrubber {
   // This class is used to delete expired data as set by the configuration items:
   // service.lifetime.JobUsageRecord = 3 months
   // service.lifetime.JobUsageRecord.RawXML = 1 month
   // service.lifetime.ComputeElement = 5 years
   // service.lifetime.StorageElement = 5 years
   // service.lifetime.ComputeElementRecord = 5 years
   // service.lifetime.StorageElementRecord = 5 years
   // service.lifetime.Subcluster = 5 years
   // service.lifetime.MasterServiceSummary = UNLIMITED
   // service.lifetime.MasterServiceSummaryHourly = 5 days
   // service.lifetime.MetricRecord = 3 months
   // service.lifetime.MetricRecord.RawXML = 1 month
   // service.lifetime.DupRecord.Duplicates = 1 month
   // service.lifetime.DupRecord.<error-type>  = <as specified>
   // service.lifetime.DupRecord = UNLIMITED
   // service.lifetime.Trace.<procName> = <as specified>
   // service.lifetime.Trace.<procName> = 1 month
   //
   
   int fBatchSize    =  1000;
   long fXmlBatchSize = 100000;
   
   private boolean fStopRequested = false;
   private boolean fPauseRequested = false;
   private DataHousekeepingService fService = null;
   
   ExpirationDateCalculator fExpCalc = new ExpirationDateCalculator();
   
   public DataScrubber(DataHousekeepingService service) {
      fService = service;
      try {
         // Get batch size from properties if set.
         fBatchSize = Integer.valueOf(fExpCalc.lifetimeProperties().
                                      getProperty("service.lifetimeManagement.fBatchSize"));
      }
      catch (Exception ignore) {
      }
      try {
         // Get batch size from properties if set.
         fBatchSize = Integer.valueOf(fExpCalc.lifetimeProperties().
                                      getProperty("service.lifetimeManagement.BatchSize"));
      }
      catch (Exception ignore) {
      }
      try {
         // Get batch size from properties if set.
         fXmlBatchSize = Long.valueOf(fExpCalc.lifetimeProperties().
                                      getProperty("service.lifetimeManagement.XmlBatchSize"));
      }
      catch (Exception ignore) {
      }
   }
   
   public void requestStop() {
      fStopRequested = true;
   }
   
   public void requestPause() {
      fPauseRequested = true;
   }
   
   public void cancelPause() {
      fPauseRequested = false;
   }
   
   public void pause() 
   {
      fService.pause();
      fPauseRequested = false;
   }
   
   protected long deleteRawXml( String type, String selection, String limit, String msg ) {
      long deletedEntities = 0;
      long deletedThisIteration = 0;
      Integer nTries = 0;
      Boolean keepTrying = true;
      
      long maxdbid = 0;
      long mindbid = 0;
      
      do {
         ++nTries;
         Session session = null;
         Transaction tx = null;
         try {
            session = HibernateWrapper.getSession();
            tx = session.beginTransaction();
            
            // First get the max dbid where we might want to delet something.
            // In order to speed up the query let first assume a dense set of record (i.e.
            // at least one record per day):

            org.hibernate.SQLQuery query = session.createSQLQuery("select max(dbid) from "+type+"_Meta where ServerDate between :dateLimit and date_add(:dateLimit,interval 1 day)");
            query.setString( "dateLimit", limit );
            
            Logging.debug("DataScrubber: About to execute " + query.getQueryString());
            Long result = null;
            BigInteger resultBig = (BigInteger)query.uniqueResult();
            if(resultBig != null)
                result = (Long) (resultBig.longValue());
            
            if ( result == null ) {
               query = session.createSQLQuery("select max(dbid) from "+type+"_Meta where ServerDate < :dateLimit");
               query.setString( "dateLimit", limit );
               
               Logging.debug("DataScrubber: About to execute " + query.getQueryString());
               resultBig = (BigInteger)query.uniqueResult();
               if(resultBig != null)
                   result = (Long) (resultBig.longValue());
            }
            if ( result == null ) {
               
               Logging.debug("DataScrubber: found no max(dbid) in "+type+"_Xml");
               // Nothing to do.
               return 0;
            }
            maxdbid = result;
            
            // Now get the minimum plausible dbid.
            // The natural query would be:
            //     select min(dbid) from JobUsageRecord_Xml where ExtraXml = "";
            // however in our case this is much slower than this alternative:
            query = session.createSQLQuery("select dbid from "+type+"_Xml X where ExtraXml = \"\" order by dbid limit 1");
 
            Logging.debug("DataScrubber: About to execute " + query.getQueryString());
            resultBig = (BigInteger)query.uniqueResult();
            if(resultBig != null)
                result = (Long) (resultBig.longValue());
            if ( result == null ) {
               Logging.debug("DataScrubber: found no min(dbid) in "+type+"_Xml");
               // Nothing to do.
               return 0;
            }
            
            mindbid = result;

            nTries = 0;
            keepTrying = false;
            session.close();
         } catch (Exception e) {
            HibernateWrapper.closeSession(session);
            if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "DataScrubber")) {
               Logging.warning("DataScrubber: error in acquiring min/max dbid for deleting " + msg + "!", e);
               keepTrying = false;
            }
            
         }
      } while (keepTrying && (!fStopRequested));
      
      // Now iterate through the dbids.
      String deletecmd = "delete from X using "+type+"_Xml X inner join "+type+"_Meta M  where " +
         " X.dbid = M.dbid "+selection+" and ExtraXml = \"\" and ServerDate < :dateLimit and :mindbid <= X.dbid and X.dbid < :maxdbid";

      Logging.debug("DataScrubber: To delete rawxml will loop on dbid "+mindbid+" to "+maxdbid);
      long cursor = mindbid;
      keepTrying = true;
      while ( keepTrying && cursor < maxdbid && !fStopRequested ) 
      {
         if (fPauseRequested) {
            pause();
            if (fStopRequested) {
               break;
            }
         }
         keepTrying = true;
         ++nTries;
         Session session = null;
         Transaction tx = null;
         try {
            session = HibernateWrapper.getSession();
            tx = session.beginTransaction();

            org.hibernate.SQLQuery query = session.createSQLQuery( deletecmd );
            query.setString( "dateLimit", limit );
            query.setLong( "mindbid", cursor );
            query.setLong( "maxdbid", cursor + fXmlBatchSize);
            Logging.debug("DataScrubber: About to execute " + query.getQueryString() + " with dateLimit = " + limit + " and mindbid = " + cursor + " and maxdbid = " + (cursor + fXmlBatchSize) );
            deletedThisIteration = query.executeUpdate();
            Logging.debug("DataScrubber: Deleted " + deletedThisIteration + " "+type+"_Xml records" );

            tx.commit();
            nTries = 0;
            keepTrying = false;  // If the session.close() fails we do not try again.
            
            session.close();
            
            keepTrying = true;
            cursor = cursor + fXmlBatchSize;

         } catch (Exception e) {
            HibernateWrapper.closeSession(session);
            deletedThisIteration = 0;
            if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "DataScrubber")) {
               Logging.warning("DataScrubber: error in deleting " + msg + "!", e);
               keepTrying = false;
            }
         }
         deletedEntities += deletedThisIteration;
      }
      return deletedEntities;
   }
   
   protected long ExecuteSQL( String deletecmd, String limit, String msg ) {
      long deletedEntities = 0;
      long deletedThisIteration = 0;
      Integer nTries = 0;
      Boolean keepTrying = true;
      do {
         if (fPauseRequested) {
            pause();
            if (fStopRequested) {
               break;
            }
         }
         ++nTries;
         Session session = null;
         Transaction tx = null;
         try {
            session = HibernateWrapper.getSession();
            tx = session.beginTransaction();
            org.hibernate.SQLQuery query = session.createSQLQuery( deletecmd );
            Logging.debug("DataScrubber: About to query " + query.getQueryString());
            
            query.setString( "dateLimit", limit );
            
            deletedThisIteration = query.executeUpdate();
            tx.commit();
            nTries = 0;
            keepTrying = false;
            session.close();
         }
         catch (Exception e) {
            HibernateWrapper.closeSession(session);
            deletedThisIteration = 0;
            if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "DataScrubber")) {
               Logging.warning("DataScrubber: error in deleting " + msg + "!", e);
               keepTrying = false;
            }
         }
         deletedEntities += deletedThisIteration;
      } while (((deletedThisIteration == fBatchSize) ||
                keepTrying) &&
               (!fStopRequested));
      return deletedEntities;
   }
   
   protected long deleteHibernateRecords(String className,
                                         String idAttribute,
                                         String whereClause,
                                         String limit, String msg) {
      long deletedEntities = 0;
      long deletedThisIteration = 0;
      Integer nTries = 0;
      Boolean keepTrying = true;
      Transaction tx = null;
      String deleteCmd = "delete " + className + " record where record." + idAttribute + " in ( :ids )";
      do {
         if (fPauseRequested) {
            pause();
            if (fStopRequested) {
               break;
            }
         }
         ++nTries;
         String selectCmd = ( "select record.id from " + className +
                             " record where " + whereClause );
         List ids = GetList(selectCmd, limit, msg);
         if ((ids == null) || (ids.size() == 0)) return 0;
         Session session = null;
         try {
            session = HibernateWrapper.getSession();
            tx = session.beginTransaction();
            org.hibernate.Query query = session.createQuery( deleteCmd );
            Logging.debug("DataScrubber: About to " + query.getQueryString());
            query.setParameterList("ids", ids);
            deletedThisIteration = query.executeUpdate();
            tx.commit();
            nTries = 0;
            keepTrying = false;
            session.close();
         }
         catch (Exception e) {
            HibernateWrapper.closeSession(session);
            deletedThisIteration = 0;
            if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "DataScrubber")) {
               Logging.warning("DataScrubber: error in deleting " + msg + ":" + selectCmd + "\n" + deleteCmd);
               Logging.warning("DataScrubber: error in deleting " + msg + "!", e);
               keepTrying = false;
            } else {
               Logging.warning("DataScrubber: Lock failure detected in deleting " + msg + ":" + selectCmd + "\n" + deleteCmd);  
            }
         }
         deletedEntities += deletedThisIteration;
      } while (((deletedThisIteration == fBatchSize) ||
                keepTrying) &&
               (!fStopRequested));
      return deletedEntities;
   }
   
   protected long Execute( String deletecmd, String limit, String msg ) {
      long deletedEntities = 0;
      long deletedThisIteration = 0;
      Integer nTries = 0;
      Boolean keepTrying = true;
      Transaction tx = null;
      do {
         if (fPauseRequested) {
            pause();
            if (fStopRequested) {
               break;
            }
         }
         ++nTries;
         Session session = null;
         try {
            session = HibernateWrapper.getSession();
            tx = session.beginTransaction();
            org.hibernate.Query query = session.createQuery( deletecmd );
            Logging.debug("DataScrubber: About to query " + query.getQueryString());
            
            query.setString( "dateLimit", limit );
            query.setMaxResults(fBatchSize);
            
            deletedThisIteration = query.executeUpdate();
            tx.commit();
            nTries = 0;
            keepTrying = false;
            session.close();
         }
         catch (Exception e) {
            HibernateWrapper.closeSession(session);
            deletedThisIteration = 0;
            if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "DataScrubber")) {
               Logging.warning("DataScrubber: error in deleting " + msg + "!", e);
               keepTrying = false;
            }
         }
         deletedEntities += deletedThisIteration;
      } while (((deletedThisIteration == fBatchSize) ||
                keepTrying) &&
               (!fStopRequested));
      return deletedEntities;
   }
   
   protected List GetList( String listcmd, String datelimit, String msg )
   {
      List result = null;
      Integer nTries = 0;
      Boolean keepTrying = true;
      Transaction tx = null;
      while (keepTrying) {
         ++nTries;
         Session session = null;
         try {
            session =  HibernateWrapper.getSession();
            tx = session.beginTransaction();
            org.hibernate.Query query = session.createQuery( listcmd );
            Logging.debug("DataScrubber: About to query " + query.getQueryString());
            query.setString( "dateLimit", datelimit );
            query.setMaxResults( fBatchSize );
            result = query.list();
            tx.commit();
            keepTrying = false;
            session.close();
         }
         catch (Exception e) {
            HibernateWrapper.closeSession(session);
            result = null;
            if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "DataScrubber")) {
               Logging.warning("DataScrubber: error in deleting " + msg + "!", e);
               keepTrying = false;
            }
         }
      }
      return result;
   }
   
   private long DeleteRows(String delcmd, Session session, List ids) {
      Logging.debug("DataScrubber: About to " + delcmd);
      
      org.hibernate.SQLQuery query = session.createSQLQuery( delcmd );
      query.setParameterList( "ids", ids );
      
      return query.executeUpdate();
   }
   
   protected long DeleteRows(Session session, String tablename, List ids) {
      return DeleteRows("delete from " + tablename + " where dbid in ( :ids )",
                        session, ids);
   }
   
   public long MetricRawXml() {
      // Execute: delete from tableName_Xml where EndTime < cutoffdate and ExtraXml == null
      String limit = fExpCalc.expirationDateAsSQLString(new Date(), "MetricRecord", "RawXML");
      
      if (!(limit.length() > 0)) return 0;
      Logging.fine("DataScrubber: Remove all MetricRecord RawXML records older than: " + limit);
      
      long nrecords = deleteRawXml("MetricRecord", "", limit, "MetricRecord RawXML" );
      
      Logging.info("DataScrubber: Removed " + nrecords +
                   " MetricRecord RawXML records older than: " + limit);
      return nrecords;
   }
   
   public long JobUsageRawXml() {
      // Execute: delete from tableName_Xml where EndTime < cutoffdate and ExtraXml == null
      String limit = fExpCalc.expirationDateAsSQLString(new Date(), "JobUsageRecord", "RawXML");
      
      if (!(limit.length() > 0)) return 0;
      Logging.fine("DataScrubber: Remove all JobUsage RawXML records older than: " + limit);
      
      long nrecords = deleteRawXml("JobUsageRecord", "", limit, "JobUsageRecord RawXML" );
      
      Logging.info("DataScrubber: Removed " + nrecords +
                   " JobUsage RawXML records older than: " + limit);
      return nrecords;
   }
   
   public long IndividualJobUsageRecords() {
      // Execute: delete from tableName set where EndTime < cutoffdate
      SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
      Date now = new Date();
      String limit = fExpCalc.expirationDateAsSQLString(now, "JobUsageRecord");

      // Calculate 6 months from now
      GregorianCalendar cal = new
            GregorianCalendar(TimeZone.getTimeZone("GMT"));
      cal.setTime(now);
      cal.add(Calendar.MONTH, 6);
      String endTimeLimit = dateFormatter.format(cal.getTime());
      
      // We handle the case where
      //   a) EndTime is null
      //   b) EndTime is incorrect (usually 1970-01-01), including in the future.
      //   c) Normal case
      List ids = null;
      long nrecords = 0;
      if (limit.length() > 0) {
         Logging.fine("DataScrubber: Remove all JobUsage records older than: " + limit);
         
         String hqlList = "select RecordId from JobUsageRecord where " +
         "((EndTime.Value is null) or " +
         "(EndTime.Value < :dateLimit) or (EndTime.Value > '" + endTimeLimit + "')) and ServerDate < :dateLimit";
         boolean done = false;
         Integer nTries = 0;
         Transaction tx = null;
         while (!done) {
            if (fPauseRequested) {
               pause();
               if (fStopRequested) {
                  break;
               }
            }
            ++nTries;
            ids = GetList(hqlList, limit, "JobUsageRecord records");
            Logging.debug("DataScrubber: deleting " + ids);
            
            // Here we decide whether to loop or not after each 'batch'
            done = (ids==null) || (ids.size() < fBatchSize);
            
            if ((ids != null) && (!ids.isEmpty())) {
               
               Session session = null;
               try {
                  long res = 0;
                  // Ideally Hibernate would put a cascading foreign key
                  // on the JobUsageRecord, it does not work now.  So
                  // do the delete from each table by hand.
                  // Ideally we would extract this list of tables from
                  // the hibernate configuration
                  String [] tables = {
                     "Disk",
                     "Memory",
                     "Swap",
                     "Network",
                     "TimeDuration",
                     "TimeInstant",
                     "ServiceLevel",
                     "PhaseResource",
                     "VolumeResource",
                     "ConsumableResource",
                     "Resource",
                     "JobUsageRecord_Xml",
                     "JobUsageRecord_Meta",
                     "JobUsageRecord_Origin"
                  };
                  
                  long n = 0;
                  // Special case multi-table delete since
                  // TransferDetails isn't keyed on dbid.
                  session = HibernateWrapper.getSession();
                  tx = session.beginTransaction();
                  DeleteRows("delete TD, T from TransferDetails TD, " +
                             "TDCorr T where T.TransferDetailsId = " +
                             "TD.TransferDetailsId and T.dbid in " +
                             "( :ids )",
                             session, ids);
                  for (int r=0; r < tables.length; ++r) {
                     DeleteRows(session, tables[r], ids);
                  }
                  // JobUsageRecord should be last, so do it
                  // explicitly so we never forget:
                  n = DeleteRows(session, "JobUsageRecord", ids);
                  tx.commit();
                  nTries = 0;
                  nrecords = nrecords + n;
                  session.close();
               }
               catch (Exception e) {
                  HibernateWrapper.closeSession(session);
                  if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "DataScrubber")) {
                     Logging.warning("DataScrubber: error in deleting JobUsageRecord!", e);
                     return nrecords; // Intentionally return now to exit the loop.
                  }
               }
               if (fStopRequested) done = true; // Truncate after this loop if we're asked.
            }
         }
         Logging.info("DataScrubber: " + nrecords +
                      " JobUsageRecord have been deleted.");
      }
      return nrecords;
   }

   public Object[] GetTableStatus(Date ref, String type, String qualifier) 
   {
      // Execute: delete from tableName set where EndTime < cutoffdate
      Date limit = fExpCalc.expirationRange(ref, type, qualifier).fExpirationDate;
      Object [] array = new Object[2];
      array[0] = type;
      array[1] = limit;
      return array;

   }
   
   public List<Object []> GetTableStatus() 
   {
      List<Object []> status = new java.util.ArrayList<Object []>();
      Date now = new Date();
      status.add(GetTableStatus(now,"JobUsageRecord",""));
      status.add(GetTableStatus(now,"JobUsageRecord","RawXML"));
      status.add(GetTableStatus(now,"MetricRecord",""));
      status.add(GetTableStatus(now,"MetricRecord","RawXML"));
      status.add(GetTableStatus(now,"ServiceSummary",""));
      status.add(GetTableStatus(now,"ServiceSummaryHourly",""));
      status.add(GetTableStatus(now,"ComputeElement",""));
      status.add(GetTableStatus(now,"StorageElement",""));
      status.add(GetTableStatus(now,"ComputeElementRecord",""));
      status.add(GetTableStatus(now,"StorageElementRecord",""));
      status.add(GetTableStatus(now,"Subcluster",""));
      status.add(GetTableStatus(now,"MetricRecord",""));
      
      // Ignoring Trace and DupRecord for now.

      return status;
   }
   
   public long IndividualGenericRecords(String type, String name) {
      
      // Execute: delete from tableName set where EndTime < cutoffdate
      String limit = fExpCalc.expirationDateAsSQLString(new Date(), type);
      
      // We need to handle the case where
      //   a) EndTime is null
      //   b) EndTime is incorrect (usually 1970-01-01)
      //   c) Normal case
      
      //   c) Normal case
      long nrecords = 0;
      if (limit.length() > 0) {
         Logging.fine("DataScrubber: Remove all "+name+" older than: " + limit);
         
         String sqlDelete = ("delete from O using "+type+"_Origin as O " +
                             " inner join "+type+" as R inner join "+type+"_Meta as M " +
                             " where O.dbid = R.dbid and R.dbid = M.dbid and " + 
                             " R.Timestamp < :dateLimit and M.ServerDate < :dateLimit" );
         
         nrecords = ExecuteSQL(sqlDelete, limit, "Origin of "+name+" records");
         
         // With the 'stock' version of hibernate we can not use deleteHibernateRecords,
         // because there is no until way to specify to hibernate that the key field should 
         // be referred in raw SQL to as, "XXXRecord.dbid" and not, "dbid" which
         // causes an ambiguity with XXXRecord_Meta and XXXRecord_Xml. This means that we're constrained to
         // deleting all out-of-date XXXRecords at once.
         
         // String hqlDelete = "delete "+type+" where Timestamp.Value < :dateLimit and ServerDate < :dateLimit";
         // nrecords = Execute(hqlDelete, limit, type);
         
         // The follwing works with the version of a hibernate that is patched, see details
         // in the file gratia/common/lib/hibernate3.README.
         
         nrecords = deleteHibernateRecords( type, "RecordId", "Timestamp.Value < :dateLimit and ServerDate < :dateLimit", limit, type );
         
         Logging.info("DataScrubber: deleted " + nrecords + " " + name + " records.");
      }
      
      return nrecords;
   }
   
   public long IndividualMetricRecords() {
      return IndividualGenericRecords("MetricRecord","Metric");
   }
   
   public long IndividualComputeElement() {
      return IndividualGenericRecords("ComputeElement","ComputeElement");
   }
   
   public long IndividualStorageElement() {
      return IndividualGenericRecords("StorageElement","StorageElement");
   }
   
   public long IndividualComputeElementRecord() {
      return IndividualGenericRecords("ComputeElementRecord","ComputeElementRecord");
   }
   
   public long IndividualStorageElementRecord() {
      return IndividualGenericRecords("StorageElementRecord","StorageElementRecord");
   }
   
   public long IndividualSubclusterRecord() {
      return IndividualGenericRecords("Subcluster","Subcluster");
   }
   
   public long GenericSummary(String table, String name) {
      // Execute: delete from tableName set where EndTime < cutoffdate
      String limit = fExpCalc.expirationDateAsSQLString(new Date(), table);
      
      long nrecords = 0;
      if (limit.length() > 0) {
         Logging.fine("DataScrubber: Remove all "+name+" records older than: " + limit);
         
         String hqlDelete = "delete "+table+" where Timestamp.Value < :dateLimit";
         nrecords = Execute(hqlDelete, limit, name+" records");
         
         Logging.info("DataScrubber: deleted " + nrecords + " " + name + " records.");
      }
      
      return nrecords;
   }
   
   public long MasterServiceSummary() {
      return GenericSummary("ServiceSummary","service summary");
   }
   
   public long MasterServiceSummaryHourly() {
      return GenericSummary("ServiceSummaryHourly","service summary hourly");
   }
   
   public long Trace() {
      return tableCleanupHelper("Trace", "traceId", "procName", "eventtime");
   }
   
   public long DupRecord() {
      return tableCleanupHelper("DupRecord", "dupid", "error", "eventdate");
   }
   
   public long Origin() {
      final String [] types = {
         "JobUsageRecord",
         "MetricRecord",
         "ComputeElement",
         "StorageElement",
         "ComputeElementRecord",
         "StorageElementRecord",
         "Subcluster",
         "ProbeDetails"
      };
      
      // Build the command string
      String sqlDelete = "delete from Origin using Origin ";
      String whereClause = "";
      for (int r=0; r < types.length; ++r) {
         sqlDelete = sqlDelete + "left outer join "+types[r]+"_Origin on "+types[r]+"_Origin.originid = Origin.originid ";
         if (r==0) {
            whereClause = types[r]+"_Origin.dbid is null ";
         } else {
            whereClause = whereClause + "and "+types[r]+"_Origin.dbid is null ";
         }
      }
      sqlDelete = sqlDelete + " where " + whereClause;
      
      Session session = null;
      long result = 0;
      try {
         session = HibernateWrapper.getSession();
         Transaction tx = session.beginTransaction();
         Logging.debug("DataScrubber: About to " + sqlDelete);
         org.hibernate.SQLQuery query = session.createSQLQuery( sqlDelete );
         
         result = query.executeUpdate();
         Logging.debug("DataScrubber: deleted " + result);
         tx.commit();
         session.close();
      }
      catch (Exception e) {
         HibernateWrapper.closeSession(session);
         Logging.warning("DataScrubber: error in deleting Origins!", e);
      }
      return result;
      
   }
   
   protected long tableCleanupHelper(String tableName,
                                     String idAttribute,
                                     String qualifierColumn,
                                     String dateColumn) {
      Properties p = fExpCalc.lifetimeProperties(); // Everybody's on the same page
      Enumeration properties = p.keys();
      Pattern propertyPattern = // Want lifetimes with table and qualifiers
      Pattern.compile("service\\.lifetime\\.(?i)" + tableName + "\\.(.+)");
      Date refDate = new Date();
      long count = 0;
      String extraWhereClause = "";
      String qualifierList = "";
      while (properties.hasMoreElements()) { // Loop over all specified properties.
         String key = (String) properties.nextElement();
         Matcher m = propertyPattern.matcher(key);
         if (m.lookingAt()) { // Match to property
            String qualifier = m.group(1);
            if ((qualifier != null) && (qualifier.length() > 0)) {
               extraWhereClause = qualifierColumn + " = '" + qualifier + "' and ";
               qualifierList += ((qualifierList.length() > 0)?", ":"") +
               "'" + qualifier + "'";
               count += tableCleanupHelper(refDate, qualifier,
                                           tableName, idAttribute,
                                           qualifierColumn, dateColumn,
                                           extraWhereClause);
            }
         }
      }
      if (qualifierList.length() > 0) {
         // Lifetime without error specifier is a default, not an
         // override.
         extraWhereClause = qualifierColumn + " NOT IN (" + qualifierList + ") and ";
      }
      count += tableCleanupHelper(refDate, "",
                                  tableName, idAttribute,
                                  qualifierColumn, dateColumn,
                                  extraWhereClause); // Catch-all
      return count;
   }
   
   protected long tableCleanupHelper(Date refDate, String qualifier,
                                     String tableName, String idAttribute,
                                     String qualifierColumn, String dateColumn,
                                     String extraWhereClause) {
      String limit = fExpCalc.expirationDateAsSQLString(refDate, tableName, qualifier);
      long count = 0;
      String extra_message = (((qualifier != null) && (qualifier.length() > 0))
                              ?(" with " + qualifierColumn + " " + qualifier)
                              :"");
      if (limit.length() > 0) {
         Logging.debug("DataScrubber: Remove all " + tableName + " entries " +
                       extra_message +
                       " older than: " + limit);
         count = deleteHibernateRecords(tableName,
                                        idAttribute,
                                        extraWhereClause +
                                        dateColumn + " < :dateLimit",
                                        limit,
                                        " entries " + extra_message);
         Logging.info("DataScrubber: deleted " + count + "  entries from " +
                      tableName + extra_message);
      }
      return count;
   }
 
   protected long testLooping()
   {
      // This routine is modeled after the other routines and is used to artificially test
      // that the pausing mechanism works.
      
      final long maxloops = 30;
      long nloops = 0;
      boolean done = false;
      while (!done) {
         if (fPauseRequested) {
            pause();
            if (fStopRequested) {
               break;
            }
         }

         try {
            Logging.warning("DataHousekeepingService: testing routine going to sleep for " +
                            1000 + "ms.");
            Thread.sleep(1000); // one second.
         }
         catch (Exception e) {
            // Ignore
         }

         nloops = nloops + 1;         
         done = (nloops >= maxloops);

         if (fStopRequested) done = true; // Truncate after this loop if we're asked.
      }
      return 0;
   }
}

package net.sf.gratia.storage;

import net.sf.gratia.services.HibernateWrapper;
import net.sf.gratia.services.ExpirationDateCalculator;
import net.sf.gratia.util.Logging;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.Hashtable;
import java.util.List;
import java.util.Date;
import java.util.Enumeration;
import java.util.regex.*;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class DataScrubber {
    // This class is used to delete expired data as set by the configuration items:
    //
    // service.lifetime.JobUsageRecord = 3 months
    // service.lifetime.JobUsageRecord.RawXML = 1 month
    // service.lifetime.MetricRecord = 3 months
    // service.lifetime.MetricRecord.RawXML = 1 month
    // service.lifetime.DupRecord.Duplicates = 1 month
    // service.lifetime.DupRecord.<error-type>  = <as specified>
    // service.lifetime.DupRecord = UNLIMITED
    // service.lifetime.Trace.<pname> = <as specified>
    // service.lifetime.Trace.<pname> = 1 month
    //

    int bunchSize = 10000; // Should be set to 10000 in production.

    ExpirationDateCalculator eCalc = new ExpirationDateCalculator();

//     protected static int ParseLimit( String timelimit ) {
//         // For now hardcoded!

//         timelimit = timelimit.trim();
//         if (timelimit == null || timelimit.equalsIgnoreCase("unlimited")) {
//             return 0;
//         } else {
//             timelimit = timelimit.toLowerCase();
//             // Assume 'month'
//             int index = timelimit.indexOf('m');
//             return Integer.parseInt(timelimit.substring(0,index).trim());
//         }
//     }

    static protected long ExecuteSQL( String deletecmd, String limit, String msg ) 
    {
        long deletedEntities = 0;

        Session session =  HibernateWrapper.getSession();
        Transaction tx = session.beginTransaction();
        try {

            org.hibernate.SQLQuery query = session.createSQLQuery( deletecmd );
            Logging.debug("DataScrubber: About to query " + query.getQueryString());

            query.setString( "dateLimit", limit );

            deletedEntities = query.executeUpdate();
            tx.commit();
        }
        catch (Exception e) {
            tx.rollback();
            Logging.warning("DataScrubber: error in deleting " + msg + "!", e);
            deletedEntities = 0;
        }
        session.close();
        return deletedEntities;
    }

    static protected long Execute( String deletecmd, String limit, String msg ) 
    {
        long deletedEntities = 0;

        Session session =  HibernateWrapper.getSession();
        Transaction tx = session.beginTransaction();
        try {

            org.hibernate.Query query = session.createQuery( deletecmd );
            Logging.debug("DataScrubber: About to query " + query.getQueryString());

            query.setString( "dateLimit", limit );

            deletedEntities = query.executeUpdate();
            tx.commit();
        }
        catch (Exception e) {
            tx.rollback();
            Logging.warning("DataScrubber: error in deleting " + msg + "!", e);
            deletedEntities = 0;
        }
        session.close();
        return deletedEntities;
    }

    protected List GetList( String listcmd, String datelimit, String msg ) 
    {
        List result = null;

        Session session =  HibernateWrapper.getSession();
        Transaction tx = session.beginTransaction();
        try {

            org.hibernate.Query query = session.createQuery( listcmd );
            Logging.debug("DataScrubber: About to query " + query.getQueryString());

            query.setString( "dateLimit", datelimit );
            query.setMaxResults( bunchSize );

            result = query.list();
            tx.commit();
        }
        catch (Exception e) {
            tx.rollback();
            Logging.warning("DataScrubber: error in deleting " + msg + "!", e);
            result = null;
        }
        if (session!=null) session.close();
        return result;
    }

    protected long DeleteRows(Session session, String tablename, List ids) 
    {
        String delcmd = "delete from " + tablename + " where dbid in ( :ids )";

        Logging.debug("DataScrubber: About to " + delcmd);

        org.hibernate.SQLQuery query = session.createSQLQuery( delcmd );
        query.setParameterList( "ids", ids );

        return query.executeUpdate();
    }


    public long MetricRawXml() {
        // Execute: delete from tableName_Xml where EndTime < cutoffdate and ExtraXml == null
        Properties p = eCalc.lifetimeProperties(); // Everybody's on the same page
        String limit = eCalc.expirationDateAsSQLString(new Date(), "MetricRecord", "RawXML");

        if (!(limit.length() > 0)) return 0;
        Logging.log("DataScrubber: Remove all MetricRecord RawXML records older than: " + limit);

        String delquery = "delete X from MetricRecord_Xml as X, MetricRecord_Meta as M, MetricRecord as J " +
            " where X.dbid = M.dbid and X.dbid = J.dbid and ExtraXml = \"\" and (Timestamp is null || Timestamp < :dateLimit) and ServerDate < :dateLimit";
        long nrecords = ExecuteSQL( delquery, limit, "MetricRecord RawXML");

        Logging.info("DataScrubber: Removed " + nrecords +
                     " MetricRecord RawXML records older than: " + limit);
        return nrecords;
    }

    public long JobUsageRawXml() {
        // Execute: delete from tableName_Xml where EndTime < cutoffdate and ExtraXml == null
        Properties p = eCalc.lifetimeProperties(); // Everybody's on the same page
        String limit = eCalc.expirationDateAsSQLString(new Date(), "JobUsageRecord", "RawXML");

        if (!(limit.length() > 0)) return 0;
        Logging.log("DataScrubber: Remove all JobUsage RawXML records older than: " + limit);
 
        String delquery = "delete X from JobUsageRecord_Xml X, JobUsageRecord_Meta as M, JobUsageRecord as J " + 
            " where M.dbid = X.dbid and J.dbid = X.dbid and ExtraXml = \"\" and (EndTime is null || EndTime < :dateLimit) and ServerDate < :dateLimit";
        long nrecords = ExecuteSQL(delquery, limit, "JobUsageRecrod RawXML");

        Logging.info("DataScrubber: Removed " + nrecords +
                     " JobUsage RawXML records older than: " + limit);
        return nrecords;
    }

    public long IndividualJobUsageRecords() {
        // Execute: delete from tableName set where EndTime < cutoffdate
        Properties p = eCalc.lifetimeProperties(); // Everybody's on the same page
        String limit = eCalc.expirationDateAsSQLString(new Date(), "JobUsageRecord");

        // We need to handle the case where
        //   a) EndTime is null
        //   b) EndTime is incorrect (usually 1970-01-01)
        //   c) Normal case

        //   c) Normal case
        List ids = null;
        long nrecords = 0;
        if (limit.length() > 0) {
            Logging.log("DataScrubber: Remove all JobUsage records older than: " + limit);

            String hqlList = "select RecordId from JobUsageRecord where EndTime.Value < :dateLimit and ServerDate < :dateLimit";
            boolean done = false;

            while (!done) {
                ids = GetList(hqlList, limit, "JobUsageRecord records");
                Logging.info("DataScrubber: deleting " + ids);
                
                // Here we decide whether to loop or not after each 'bunch'
                done = (ids==null) || (ids.size() < bunchSize);

                if ((ids != null) && (!ids.isEmpty())) {
                
                    Session session =  HibernateWrapper.getSession();
                    Transaction tx = session.beginTransaction();
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
                            "JobUsageRecord"
                        };
                        
                        long n = 0;
                        for (int r=0; r < tables.length; ++r) {
                            n = DeleteRows(session,tables[r],ids);
                        }                             
                        tx.commit();                   
                        nrecords = nrecords + n;
                    }
                    catch (Exception e) {
                        tx.rollback();
                        Logging.warning("DataScrubber: error in deleting JobUsageRecord!", e);
                        if (session!=null) session.close();
                        return nrecords; // Intentionally return now to exit the loop.
                    }
                    if (session!=null) session.close();
                }
            }
            Logging.info("DataScrubber: " + nrecords +
                         " JobUsageRecord have been deleted.");
        }
        return nrecords;
    }

    public long IndividualMetricRecords() {
        // Execute: delete from tableName set where EndTime < cutoffdate
        Properties p = eCalc.lifetimeProperties(); // Everybody's on the same page
        String limit = eCalc.expirationDateAsSQLString(new Date(), "MetricRecord");

        // We need to handle the case where
        //   a) EndTime is null
        //   b) EndTime is incorrect (usually 1970-01-01)
        //   c) Normal case

        //   c) Normal case
        long nrecords = 0;
        if (limit.length() > 0) {
            Logging.log("DataScrubber: Remove all Metric records older than: " + limit); 

            String hqlDelete = "delete MetricRecord where Timestamp.Value < :dateLimit and ServerDate < :dateLimit";
            nrecords = Execute(hqlDelete, limit, "Metric records");

            Logging.info("DataScrubber: deleted " + nrecords + " Metric records ");
        }

        return nrecords;
    }

    public long Trace() {
        return tableCleanupHelper("Trace", "pname", "eventtime");
    }

    public long DupRecord() {
        return tableCleanupHelper("DupRecord", "error", "eventdate");
    }

    private long tableCleanupHelper(String tableName, String qualifierColumn,
                                    String dateColumn) {
        Properties p = eCalc.lifetimeProperties(); // Everybody's on the same page
        Enumeration properties = p.keys();
        Pattern propertyPattern = // Want lifetimes with table and qualifiers
            Pattern.compile("service\\.lifetime\\." + tableName + "\\.([^\\.]+)");
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
                    qualifierList += (qualifierList.length() > 0)?", ":"" +
                        "'" + qualifier + "'";
                    count += tableCleanupHelper(refDate, qualifier,
                                                tableName, qualifierColumn, dateColumn,
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
                                    tableName, qualifierColumn, dateColumn,
                                    extraWhereClause); // Catch-all
        return count;
    }

    private long tableCleanupHelper(Date refDate, String qualifier,
                                    String tableName, String qualifierColumn, String dateColumn,
                                    String extraWhereClause) {
        String limit = eCalc.expirationDateAsSQLString(refDate, tableName, qualifier);
        long count = 0;
        String extra_message =
            ((qualifier != null) && (qualifier.length() > 0))?
            ("with " + qualifierColumn + " " + qualifier):
            "";
        if (limit.length() > 0) {
            Logging.log("DataScrubber: Remove all " + tableName + " entries " +
                        extra_message +
                        " older than: " + limit);
            String hqlDelete = "delete " + tableName + " where " +
                extraWhereClause +
                dateColumn + " < :dateLimit";
            count = Execute(  hqlDelete, limit, " entries " + extra_message);
            Logging.info("DataScrubber: deleted " + count + "  entries from " +
                         tableName + extra_message);
        }
        return count;
    }

}

package net.sf.gratia.storage;

import net.sf.gratia.services.HibernateWrapper;
import net.sf.gratia.util.Logging;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.List;

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
    // service.lifetime.DupRecord = UNLIMITED
    //

    int DupRecordDuplicateLimit = 0; // Limit expressed in month
    int DupRecordLimit = 0;          // Limit expressed in month
    int JobUsageRecordLimit = 0;
    int JobUsageRecordXmlLimit = 0;
    int MetricRecordLimit = 0;
    int MetricRecordXmlLimit = 0;

    int bunchSize = 2; // Should be set to 10000 in production.

    protected static int ParseLimit( String date ) {
        // For now hardcoded!

        if (date == null || date.equalsIgnoreCase("unlimited")) {
            return 0;
        } else {
            return 2;
        }
    }

    protected static String WhatDate( int months )
    {
        // Return the date 'months' months ago.

        GregorianCalendar jcal = new GregorianCalendar( TimeZone.getTimeZone("GMT") );
        jcal.add( Calendar.MONTH, -1 * months );
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(jcal.getTime());
    }

    public DataScrubber() 
    {
        Properties p = net.sf.gratia.util.Configuration.getProperties();

        DupRecordDuplicateLimit = ParseLimit(p.getProperty( "service.lifetime.DupRecord.Duplicates"));
        DupRecordLimit          = ParseLimit(p.getProperty( "service.lifetime.DupRecord"));

        JobUsageRecordLimit     = ParseLimit(p.getProperty( "service.lifetime.JobUsageRecord"));
        JobUsageRecordXmlLimit  = ParseLimit(p.getProperty( "service.lifetime.JobUsageRecord.RawXML"));
        MetricRecordLimit       = ParseLimit(p.getProperty( "service.lifetime.MetricRecord"));
        MetricRecordXmlLimit    = ParseLimit(p.getProperty( "service.lifetime.MetricRecord.RawXML"));
    }

    static protected int ExecuteSQL( String deletecmd, String limit, String msg ) 
    {
        int deletedEntities = 0;

        Session session =  HibernateWrapper.getSession();
        Transaction tx = session.beginTransaction();
        try {

            org.hibernate.SQLQuery query = session.createSQLQuery( deletecmd );
            Logging.warning("DataScrubber: About to query " + query.getQueryString());

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

    static protected int Execute( String deletecmd, String limit, String msg ) 
    {
        int deletedEntities = 0;

        Session session =  HibernateWrapper.getSession();
        Transaction tx = session.beginTransaction();
        try {

            org.hibernate.Query query = session.createQuery( deletecmd );
            Logging.warning("DataScrubber: About to query " + query.getQueryString());

            query.setString( "dateLimit", limit );

            Logging.warning("DataScrubber: About to query " + query.getQueryString());

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
            Logging.warning("DataScrubber: About to query " + query.getQueryString());

            query.setString( "dateLimit", datelimit );
            query.setMaxResults( bunchSize );

            Logging.warning("DataScrubber: About to query " + query.getQueryString());

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

    protected int DeleteRows(Session session, String tablename, List ids) 
    {
        String delcmd = "delete from " + tablename + " where dbid in ( :ids )";

        Logging.warning("DataScrubber: About to " + delcmd);

        //statement = connection.createStatement();
        //statement.executeUpdate(cmd);

        org.hibernate.SQLQuery query = session.createSQLQuery( delcmd );
        query.setParameterList( "ids", ids );

        return query.executeUpdate();
    }


    public int MetricRawXml()
    {
        // Execute: delete from tableName_Xml where EndTime < cutoffdate and ExtraXml == null

        if (MetricRecordXmlLimit==0) return 0;

        String limit = WhatDate( JobUsageRecordXmlLimit );
        Logging.debug("DataScrubber: Remove all MetricRecord RawXML records older than: "+limit);

        String delquery = "delete X from MetricRecord_Xml as X, MetricRecord_Meta as M, MetricRecord as J where X.dbid = M.dbid and X.dbid = J.dbid and (Timestamp is null || Timestamp < :dateLimit) and ServerDate < :dateLimit";
        int nrecords = ExecuteSQL( delquery, limit, "MetricRecord RawXML");

        Logging.debug("DataScrubber: Removed "+nrecords+" MetricRecord RawXML records older than: "+limit);
        return nrecords;
    }

    public int JobUsageRawXml()
    {
        // Execute: delete from tableName_Xml where EndTime < cutoffdate and ExtraXml == null

        if (JobUsageRecordXmlLimit==0) return 0;

        String limit = WhatDate( JobUsageRecordXmlLimit );
        Logging.debug("DataScrubber: Remove all JobUsage RawXML records older than: "+limit);
 
        String delquery = "delete X from JobUsageRecord_Xml X, JobUsageRecord_Meta as M, JobUsageRecord as J " + 
            " where M.dbid = X.dbid and J.dbid = X.dbid and (EndTime is null || EndTime < :dateLimit) and ServerDate < :dateLimit";
        int nrecords = ExecuteSQL( delquery, limit, "JobUsageRecrod RawXML");

        Logging.debug("DataScrubber: Removed "+nrecords+" JobUsage RawXML records older than: "+limit);
        return nrecords;
    }

    public int IndividualJobUsageRecords()
    {
        // Execute: delete from tableName set where EndTime < cutoffdata

        // We need to handle the case where
        //   a) EndTime is null
        //   b) EndTime is incorrect (usually 1970-01-01)
        //   c) Normal case

        //   c) Normal case
        List  ids = null;
        int nrecords = 0;
        if (JobUsageRecordLimit != 0) {

            String limit = WhatDate( JobUsageRecordLimit );
            Logging.debug("DataScrubber: Remove all JobUsage records older than: "+limit);

            String hqlList = "select RecordId from JobUsageRecord where EndTime.Value < :dateLimit and ServerDate < :dateLimit";
            boolean done = false;

            while (!done) {
                ids = GetList( hqlList, limit, "JobUsageRecord records" );
                Logging.debug("DataScrubber: deleting "+ids);
                
                // Here we decide whether to loop or not after each 'bunch'
                done = (ids==null) || (ids.size() < bunchSize);

                if (ids!=null && !ids.isEmpty()) {
                
                    Session session =  HibernateWrapper.getSession();
                    Transaction tx = session.beginTransaction();
                    try {
                        int res = 0;
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
                        
                        int n = 0;
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
            Logging.warning("DataScrubber: "+nrecords+" JobUsageRecord have been deleted.");
        }
        return nrecords;
    }

    public int IndividualMetricRecords()
    {
        // Execute: delete from tableName set where EndTime < cutoffdata

        // We need to handle the case where
        //   a) EndTime is null
        //   b) EndTime is incorrect (usually 1970-01-01)
        //   c) Normal case

        //   c) Normal case
        Logging.warning("DataScrubber: Check for Metric record to be removed");
        int nrecords = 0;
        if (MetricRecordLimit != 0) {
            String limit = WhatDate( MetricRecordLimit );
            Logging.debug("DataScrubber: Would have remove all Metric records older than: "+limit);

            String hqlDelete = "delete MetricRecord where Timestamp.Value < :dateLimit";
            nrecords = Execute( hqlDelete, limit, "Metric records" );
            Logging.debug("DataScrubber: deleted "+nrecords+" Metric records ");
        }

        return nrecords;
    }

    public int Duplicate() 
    {
        // Execute: delete from DupRecord where eventtime < cutoffdate && RecordType == "Duplicate"
        // Returns the number of objects deleted from the database.

        Logging.warning("DataScrubber: Check for Duplicates to be removed");

        int n = 0;
        int ndup = 0;
        if (DupRecordLimit != 0 ) {
            String limit = WhatDate( DupRecordLimit );
            Logging.debug("DataScrubber: Will remove all error record older than: "+limit);

            String hqlDelete = "delete DupRecord where eventtime < :dateLimit";
            n = Execute(  hqlDelete, limit, " error records " );
            Logging.debug("DataScrubber: deleted "+n+" error records ");
        }

        if (DupRecordDuplicateLimit != 0 ) {
            String limit = WhatDate( DupRecordDuplicateLimit );

            Logging.debug("DataScrubber: Will remove all duplicates record older than: "+limit);
            String hqlDelete = "delete DupRecord where error = 'Duplicate' and eventdate < :dateLimit";
            ndup = Execute(  hqlDelete, limit, " duplicate records " );
            Logging.debug("DataScrubber: deleted "+ndup+" duplicate records ");
        }
        return n + ndup;
    }

}

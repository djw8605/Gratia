package net.sf.gratia.storage;

import net.sf.gratia.services.HibernateWrapper;
import net.sf.gratia.util.Logging;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;

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

    public static int ParseLimit( String date ) {
        // For now hardcoded!

        
        if (date == null || date.equalsIgnoreCase("unlimited")) {
            return 0;
        } else {
            return 2;
        }
    }

    public static String WhatDate( int months )
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

        DupRecordDuplicateLimit = ParseLimit(p.getProperty( "service.lifetime.DupRecord.Duplicates" ));
        DupRecordLimit = ParseLimit(p.getProperty( "service.lifetime.DupRecord" ));
    }

    public int RawXml( String tableName ) 
    {
        // Execute: delete from tableName_Xml where EndTime < cutoffdate and ExtraXml == null

        return 0;
    }

    public int IndividualRecords( String tableName ) 
    {
        // Execute: delete from tableName set where EndTime < cutoffdata

        // We need to handle the case where
        //   a) EndTime is null
        //   b) EndTime is incorrect (usually 1970-01-01)
        //   c) Normal case

        return 0;
    }

    static int Execute( String deletecmd, String limit, String msg ) 
    {
        int deletedEntities = 0;

        Session session =  HibernateWrapper.getSession();
        Transaction tx = session.beginTransaction();
        try {
            deletedEntities = session.createQuery( deletecmd )
                .setString( "dateLimit", limit )
                .executeUpdate();
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

    public int Duplicate() 
    {
        // Execute: delete from DupRecord where eventtime < cutoffdate && RecordType == "Duplicate"
        // Returns the number of objects deleted from the database.

        Logging.warning("DataScrubber: Check for Duplicates to be remove");

        int n = 0;
        int ndup = 0;
        if (DupRecordLimit != 0 ) {
            String limit = WhatDate( DupRecordLimit );
            Logging.debug("DataScrubber: Would have remove all error record older than: "+limit);

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

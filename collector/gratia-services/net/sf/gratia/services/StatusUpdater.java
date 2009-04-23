package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import net.sf.gratia.storage.*;

import java.util.*;
import java.sql.*;
import java.text.*;

public class StatusUpdater
{

   private final static String dq = "\"";

   Properties p;
   XP xp = new XP();
   SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

   String driver = null;
   String url = null;
   String user = null;
   String password = null;

   public StatusUpdater()
   {
      p = Configuration.getProperties();

   }

    public Site getSite(org.hibernate.Session session, String sitename) throws Exception
    {
        Site site = null;
        String siteCommand = "from Site where SiteName = ?";
        
        site = (Site)session.createQuery(siteCommand).setString(0,sitename).uniqueResult();
        
        if (site == null) {
            site = new Site(sitename);
            
            session.save(site);
        }
        return site;
   }


   private static final String command = "from Probe where probename = ?";
   
   public Probe update(org.hibernate.Session session, Record record, String rawxml) throws Exception
   {
      String probeName = record.getProbeName().getValue();
      
      Probe probe = null;
      Site site = null;
      {
          // org.hibernate.Session session =  HibernateWrapper.getSession();
          probe = (Probe)session.createQuery(command).setString(0,probeName).uniqueResult();

          int record_idx = 0;
          if (probe == null) {
              // We need a new Probe object
              probe = new Probe(probeName);
              String siteName;
              try {
                  siteName = record.getSiteName().getValue();
              }
              catch (NullPointerException e) {
                  siteName = "Unknown";
              }
              site = getSite(session, siteName);
              probe.setsite(site);
          }
          probe.setactive(1);

          DateElement date = new DateElement();
          date.setValue( new java.util.Date() );

          probe.setcurrenttime(date);
          probe.setstatus("alive");
          // probe.setnRecords( probe.getnRecords() + 1 );

          Logging.log("StatusUpdater: will save/update "+probe.getprobename()+" n="+probe.getnRecords());
          session.saveOrUpdate(probe);
      }
      return probe;
   }
}

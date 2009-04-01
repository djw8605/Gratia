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
   java.sql.Connection connection;
   Statement statement;
   SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

   String driver = null;
   String url = null;
   String user = null;
   String password = null;

   public StatusUpdater()
   {
      p = Configuration.getProperties();

   }

   public void openConnection()
   {
      try
      {
         driver = p.getProperty("service.mysql.driver");
         url = p.getProperty("service.mysql.url");
         user = p.getProperty("service.mysql.user");
         password = p.getProperty("service.mysql.password");
         Class.forName(driver);
         connection = null;
         connection = DriverManager.getConnection(url, user, password);
      }
      catch (Exception e)
      {
         Logging.log("StatusUpdater: Error During Init: No Connection");
      }
   }

    public Site getSite(org.hibernate.Session session, String sitename) throws Exception
    {
        Site site = null;
        String command = "from Site where SiteName = ?";
        
        List result = session.createQuery(command).setString(0,sitename).list();
        
        if (result.size() == 0) {
            site = new Site(sitename);
            
            session.save(site);

        } else if (result.size() == 1) {
            site = (Site) result.get(0);

        } else {
            // Humm there is more than one probe with the same name!
            // We have a problem.
            throw new Exception("getSite got more than one sites ("+result.size()+" with the name "+sitename);
        }

        return site;
   }

   public Probe update(org.hibernate.Session session, Record record, String rawxml) throws Exception
   {
      if (connection == null)
         openConnection();
      if (connection == null)
         throw new Exception("StatusUpdater: No Connection: CommunicationsException");

      String probeName = record.getProbeName().getValue();
      String comma = ",";

      String command = "from Probe where probename = ?"; 
      
      Probe probe = null;
      Site site = null;
      {
          // org.hibernate.Session session =  HibernateWrapper.getSession();
          List result = session.createQuery(command).setString(0,probeName).list();

          int record_idx = 0;
          if (result.size() == 0) {
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
              probe.setactive(1);
          } else if (result.size() == 1) {
              probe = (Probe)result.get(0);
              probe.setactive(1);
          } else {
              // Humm there is more than one probe with the same name!
              // We have a problem.
              throw new Exception("We got more than one probe ("+result.size()+" with the name "+probeName);
          }
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

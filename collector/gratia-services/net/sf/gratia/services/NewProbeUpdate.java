package net.sf.gratia.services;

import java.util.Properties;
import java.sql.*;
import net.sf.gratia.storage.*;

public class NewProbeUpdate
{
   public Properties p;

   Connection connection = null;
   Statement statement = null;
   ResultSet resultSet = null;
   XP xp = new XP();

   String driver = null;
   String url = null;
   String user = null;
   String password = null;

   public NewProbeUpdate()
   {
      p = Configuration.getProperties();

      driver = p.getProperty("service.mysql.driver");
      url = p.getProperty("service.mysql.url");
      user = p.getProperty("service.mysql.user");
      password = p.getProperty("service.mysql.password");
      openConnection();
   }

   public void openConnection()
   {
      try
      {
         Class.forName(driver);
         connection = null;
         connection = (java.sql.Connection)DriverManager.getConnection(url, user, password);
      }
      catch (Exception e)
      {
      }
   }

   public void check(Record record) throws Exception
   {
      if (connection == null)
         openConnection();
      if (connection == null)
         throw new Exception("NewProbeUpdate: No Connection");

      StringElement site = record.getSiteName();
      StringElement probe = record.getProbeName();
      String sitename = "Unknown";
      String probename = probe.getValue();
      String dq = "\"";
      String comma = ",";
      int icount = 0;
      int facilityid = -1;

      if (site != null)
         sitename = site.getValue();
      //
      // see if the probe exists
      //
      String command = "select count(*) from CEProbes where probename = " + dq + probename + dq;
      try
      {
         statement = connection.prepareStatement(command);
         resultSet = statement.executeQuery(command);
         while (resultSet.next())
         {
            icount = resultSet.getInt(1);
         }
         resultSet.close();
         statement.close();
         //
         // already there - just exit
         //
         if (icount > 0)
            return;
         //
         // otherwise get facilityid for sitename
         //

         command = "select facility_id from Site where facility_name = " + dq + sitename + dq;
         statement = connection.prepareStatement(command);
         resultSet = statement.executeQuery(command);
         while (resultSet.next())
         {
            facilityid = resultSet.getInt(1);
         }
         resultSet.close();
         statement.close();
         //
         // if facilityid == -1 it doesn't exist - add it to Site table 
         //
         if (facilityid == -1)
         {
            command = "insert into Site(facility_name) values(" + dq + sitename + dq + ")";
            statement = connection.createStatement();
            statement.executeUpdate(command);
            statement.close();
            command = "select facility_id from Site where facility_name = " + dq + sitename + dq;
            statement = connection.prepareStatement(command);
            resultSet = statement.executeQuery(command);
            while (resultSet.next())
            {
               facilityid = resultSet.getInt(1);
            }
            resultSet.close();
            statement.close();
         }
         //
         // now add a new entry to ceprobes with default values
         //
         command =
               "insert into CEProbes (facility_id,probename,active,reporthh,reportmm) values(" +
               facilityid + comma + dq + probename + dq + comma + "1" + comma + "24" + comma + "00" + ")";
         statement = connection.createStatement();
         statement.executeUpdate(command);
         statement.close();
      }
      catch (Exception e)
      {
         //
         // communications error
         //
         if (HibernateWrapper.databaseUp())
         {
            Logging.log("NewProbeUpdate: Processing Error");
            Logging.log("NewProbeUpdate: command: " + command);
            Logging.log("NewProbeUpdate: e: " + e);
         }
         connection = null;
         throw new Exception("NewProbeUpdate: No Connection");
      }
   }

}

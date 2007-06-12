package net.sf.gratia.services;

import java.util.Properties;
import java.sql.*;
import net.sf.gratia.storage.*;

public class NewVOUpdate
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

   public NewVOUpdate()
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

   public void check(Record rec) throws Exception
   {
      if (rec.getClass() != JobUsageRecord.class) return;
      JobUsageRecord record = (JobUsageRecord)rec;
      
      if (connection == null)
         openConnection();
      if (connection == null)
         throw new Exception("NewVOUpdate: No Connection");

      String dq = "\"";
      String comma = ",";

      StringElement site = record.getSiteName();
      String voname = record.getUserIdentity().getVOName();
      String reportablevoname = record.getUserIdentity().getVOName();
      if (reportablevoname == null) reportablevoname = "null";
      else reportablevoname = dq + reportablevoname + dq;
      
      int icount = 0;
      int VOid = -1;

      //
      // see if the probe exists
      //
      String command = "select count(*) from VONameCorrection where voname = binary " + dq + voname + dq + 
                  " and binary reportablevoname = " + reportablevoname;
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

         command = "select VOid from VO where VOName = " + dq + voname + dq; // Case INsensitive match.
         statement = connection.prepareStatement(command);
         resultSet = statement.executeQuery(command);
         while (resultSet.next())
         {
            VOid = resultSet.getInt(1);
         }
         resultSet.close();
         statement.close();
         //
         // if facilityid == -1 it doesn't exist - add it to Site table 
         //
         if (VOid == -1)
         {
            command = "insert into VO(VOName) values(" + dq + voname + dq + ")";
            statement = connection.createStatement();
            statement.executeUpdate(command);
            statement.close();
            command = "select VOid from VO where VOName = " + dq + voname + dq; // Case INsensitive match.
            statement = connection.prepareStatement(command);
            resultSet = statement.executeQuery(command);
            while (resultSet.next())
            {
               VOid = resultSet.getInt(1);
            }
            resultSet.close();
            statement.close();
         }
         //
         // now add a new entry to ceprobes with default values
         //
         command =
               "insert into VONameCorrection(VOid,VOName,ReportableVOName) values(" +
               VOid + comma + dq + voname + dq + comma + reportablevoname + ")";
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
            Logging.log("NewVOUpdate: Processing Error");
            Logging.log("NewVOUpdate: command: " + command);
            Logging.log("NewVOUpdate: e: " + e);
         }
         connection = null;
         throw new Exception("NewVOUpdate: No Connection");
      }
   }

}

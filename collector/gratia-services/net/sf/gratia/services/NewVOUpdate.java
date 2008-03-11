package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

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
      String reportablevoname = record.getUserIdentity().getReportableVOName();
      String comparisonString;
      if (reportablevoname == null) {
          comparisonString = "is null";
      } else {
          comparisonString = "= " + dq + reportablevoname + dq;
      }
      int icount = 0;
      int VOid = -1;

      //
      // see if the probe exists
      //
      String command = "select count(*) from VONameCorrection where" +
          " binary voname = binary " + dq + voname + dq + 
          " and binary reportablevoname " + comparisonString;
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
         // Otherwise check for an existing entry for this VO to put in a default mapping
         //
         String votablekey;
         if (voname.startsWith("/")) { // FQAN -- use ReportableVOName for default instead
             votablekey = reportablevoname;
         } else {
             votablekey = voname;
         }

         command = "select VOid from VO where VOName = " + dq + votablekey + dq;
         statement = connection.prepareStatement(command);
         resultSet = statement.executeQuery(command);
         while (resultSet.next())
         {
            VOid = resultSet.getInt(1);
         }
         resultSet.close();
         statement.close();
         //
         // if VOid == -1 it doesn't exist - add it to VO table 
         //
         if (VOid == -1)
         {
            command = "insert into VO(VOName) values(" + dq + votablekey + dq + ")";
            statement = connection.createStatement();
            statement.executeUpdate(command);
            statement.close();
            command = "select VOid from VO where VOName = " + dq + votablekey + dq;
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
         // now add a new entry to VONameCorrection with default values
         //
         command =
               "insert into VONameCorrection(VOid,VOName,ReportableVOName) values(" +
               VOid + comma + dq + voname + dq + comma + dq + reportablevoname + dq + ")";
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

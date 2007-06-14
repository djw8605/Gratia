package net.sf.gratia.services;

import net.sf.gratia.storage.*;

import java.util.*;
import java.sql.*;
import java.text.*;

public class StatusUpdater
{
   Properties p;
   XP xp = new XP();
   Connection connection;
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

   public void update(Record record, String rawxml) throws Exception
   {
      if (connection == null)
         openConnection();
      if (connection == null)
         throw new Exception("StatusUpdater: No Connection: CommunicationsException");

      String probeName = record.getProbeName().getValue();
      String dq = "\"";
      String comma = ",";

      String command = "update Probe set" +
            " currenttime = " + dq + format.format(new java.util.Date()) + dq + comma +
            " status = " + dq + "alive" + dq + comma +
            " nRecords = nRecords + 1" +
            " where probename = " + dq + probeName + dq;

      try
      {
         statement = connection.createStatement();
         statement.execute(command);
         statement.close();
      }
      catch (Exception e)
      {
         try
         {
            connection.close();
         }
         catch (Exception ignore)
         {
         }
         connection = null;
         throw new Exception("StatusUpdater: No Connection: CommunicationsException");
      }
   }
}

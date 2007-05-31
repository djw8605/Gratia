package net.sf.gratia.services;

import java.util.*;
import java.sql.*;

import net.sf.gratia.storage.*;

public class ReplicationDataPump extends Thread
{
   private String driver;
   private String url;
   private String user;
   private String password;

   long dbid;
   Post post;
   java.sql.Connection connection;
   Statement statement;
   ResultSet resultSet;
   String command;

   int id;
   String rawxml;
   String xml;
   String extraxml;

   int irecords = 0;

   org.hibernate.Session session;

   XP xp = new XP();

   Properties p;
   String replicationid;

   public boolean exitflag = false;

   String dq = "\"";
   String cr = "\n";
   String comma = ",";

   boolean databaseDown = false;

   public ReplicationDataPump(String replicationid)
   {
      this.replicationid = replicationid;
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
         Class.forName(driver).newInstance();
         connection = DriverManager.getConnection(url, user, password);
         Logging.log("ReplicationDataPump: Database Connection Opened: " + replicationid);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         return;
      }
   }

   public void run()
   {
      Logging.log("ReplicationDataPump: Started: " + replicationid);
      if (!HibernateWrapper.databaseUp())
      {
         HibernateWrapper.start();
         if (!HibernateWrapper.databaseUp())
         {
            Logging.log("ReplicationDataPump: Hibernate Down - Exiting: " + replicationid);
            return;
         }
      }

      while (true)
      {
         loop();
         if (exitflag)
         {
            cleanup();
            Logging.log("ReplicationDataPump: Stopping/Exiting: " + replicationid);
            return;
         }
      }
   }

   public void cleanup()
   {
      try
      {
         resultSet.close();
      }
      catch (Exception ignore)
      {
      }
      try
      {
         statement.close();
      }
      catch (Exception ignore)
      {
      }
      try
      {
         connection.close();
         Logging.log("ReplicationDataPump: Connection Closed: " + replicationid);
      }
      catch (Exception ignore)
      {
      }
      try
      {
         session.close();
      }
      catch (Exception ignore)
      {
      }
   }

   public void exit()
   {
      exitflag = true;
      Logging.log("ReplicationDataPump: Exit Requested: " + replicationid);
   }

   public void loop()
   {
      if (exitflag)
         return;

      if (!HibernateWrapper.databaseUp())
      {
         Logging.log("ReplicationDataPump: " + replicationid + " :Hibernate Down - Exiting");
         exitflag = true;
         return;
      }

      command = "select * from Replication where replicationid = " + replicationid;

      String openconnection = "";
      String secureconnection = "";
      String running = "";
      String security = "";
      String dbid = "";
      String probename = "";
      String frequency = "";
      String table = "";

      try
      {
         Logging.log("ReplicationDataPump: " + replicationid + " Executing Command: " + command);
         statement = connection.prepareStatement(command);
         resultSet = statement.executeQuery(command);

         while (resultSet.next())
         {
            openconnection = resultSet.getString("openconnection");
            secureconnection = resultSet.getString("secureconnection");
            running = resultSet.getString("running");
            security = resultSet.getString("security");
            dbid = resultSet.getString("dbid");
            probename = resultSet.getString("probename");
            frequency = resultSet.getString("frequency");
            table = resultSet.getString("table");
            if (table == null) table = "JobUsageRecord";
         }
         resultSet.close();
         statement.close();
      }
      catch (Exception e)
      {
         if (!HibernateWrapper.databaseUp())
         {
            cleanup();
            exitflag = true;
            return;
         }
         Logging.log("command: " + command);
         e.printStackTrace();
         cleanup();
         exitflag = true;
         return;
      }

      if (running.equals("0"))
      {
         exitflag = true;
         return;
      }

      //
      // create base retrieval
      //

      command = "select count(*) from "+table+" , "+table+"_Meta"+ cr +
            "where dbid > " + dbid;
      if (probename.startsWith("Probe:"))
      {
         probename = xp.replace(probename, "Probe:", "");
         command = command + cr;
         command = command + " and ProbeName = " + dq + probename + dq + cr;
      }
      if (probename.startsWith("VO:"))
      {
         probename = xp.replace(probename, "VO:", "");
         command = command + cr;
         command = command + " and VOName = " + dq + probename + dq + cr;
      }

      if (!probename.equals("All"))
      {
      }
      int count = 0;

      try
      {
         statement = connection.prepareStatement(command);
         resultSet = statement.executeQuery(command);
         while (resultSet.next())
            count = resultSet.getInt(1);
         resultSet.close();
         statement.close();
      }
      catch (Exception e)
      {
         if (!HibernateWrapper.databaseUp())
         {
            cleanup();
            exitflag = true;
            return;
         }
         Logging.log("Error During Replication");
         Logging.log("Command: " + command);
         e.printStackTrace();
         cleanup();
         exitflag = true;
         return;
      }

      Logging.log("ReplicationDataPump: " + replicationid + " Executed Command: " + command);
      Logging.log("ReplicationDataPump: " + replicationid + " Records: " + count);

      command = "select dbid from "+table + "_Meta" + cr +
            "where dbid > " + dbid;
      if (!probename.equals("All"))
      {
         command = command + cr;
         command = command + " and probename = " + dq + probename + dq + cr;
      }

      //
      // start replication
      //

      Post post = null;

      try
      {
         statement = connection.prepareStatement(command, java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
         resultSet = statement.executeQuery(command);
         while (resultSet.next())
         {
            if (exitflag)
            {
               cleanup();
               return;
            }
            dbid = resultSet.getString("dbid");
            String xml = getXML(dbid,table);
            if (xml.length() == 0)
            {
               Logging.log("Received Null XML: dbid: " + dbid);
               continue;
            }
            if (p.getProperty("service.datapump.trace") != null)
               if (p.getProperty("service.datapump.trace").equals("1"))
               {
                  Logging.log("");
                  Logging.log("dbid: " + dbid);
                  Logging.log("xml: " + xml);
                  Logging.log("");
               }
            if (security.equals("0"))
               post = new Post(openconnection + "/gratia-servlets/rmi", "update", xml);
            else
               post = new Post(secureconnection + "/gratia-servlets/rmi", "update", xml);
            Logging.log("ReplicationDataPump: Sending: " + replicationid + ":" + dbid);
            String response = post.send();
            String[] results = split(response, ":");
            if (!results[0].equals("OK"))
            {
               Logging.log("Error During Post: " + response);
               cleanup();
               exitflag = true;
               return;
            }
            //
            // update replicationtable
            //
            updateReplicationTable(dbid);
         }
         resultSet.close();
         statement.close();
      }
      catch (Exception e)
      {
         if (!HibernateWrapper.databaseUp())
         {
            Logging.log("ReplicationDataPump: " + replicationid + " :Database Connection Error");
            cleanup();
            exitflag = true;
            return;
         }
         Logging.log("ReplicationDataPump: Error During Replication");
         e.printStackTrace();
         cleanup();
         exitflag = true;
         return;
      }

      //
      // now wait frequency minutes
      //

      long wait = Integer.parseInt(frequency);
      wait = wait * 60 * 1000;
      try
      {
         Thread.sleep(wait);
      }
      catch (Exception ignore)
      {
      }
   }

   public String getXML(String dbid,String table) throws Exception
   {
      StringBuffer buffer = new StringBuffer();

      int i = 0;

      Logging.log("ReplicationDataPump: getXML: dbid: " + dbid);

      session = HibernateWrapper.getSession();
      String command = "from "+table+" where "+table+".dbid = " + dbid;
      List result = session.createQuery(command).list();
      for (i = 0; i < result.size(); i++)
      {
         Record record = (Record)result.get(i);
         //DurationElement duration = getCpuSystemDuration(dbid,table);
         //if (duration != null)
         //   record.setCpuSystemDuration(duration);
         //if (record.getCpuSystemDuration() == null)
         //   Logging.log("dbid: " + dbid + " null cpu system duration");
         buffer.append("replication" + "|");
         buffer.append(record.asXML() + "|");
         buffer.append(record.getRawXml() + "|");
         buffer.append(record.getExtraXml());
      }
      session.close();
      return buffer.toString();
   }

   //public DurationElement getCpuSystemDuration(String dbid, String table) throws Exception
   //{
   //   String command = "select CpuSystemDuration from "+table+" where dbid = " + dbid;
   //   Double value = null;

   //   Statement statement = connection.prepareStatement(command);
   //   ResultSet resultSet = statement.executeQuery(command);
   //   while (resultSet.next())
   //   {
   //      value = resultSet.getDouble(1);
   //   }
   //   resultSet.close();
   //   statement.close();

   //   if (value == null)
   //      return null;

   //   DurationElement duration = new DurationElement();
   //   duration.setValue(value);
   //   duration.setType("system");
   //   return duration;
   //}

   public void updateReplicationTable(String dbid) throws Exception
   {
      String command =
            "update Replication" + cr +
            " set dbid = " + dbid + comma + cr +
            " rowcount = rowcount + 1" + cr +
            " where replicationid = " + replicationid;

      Statement statement = connection.createStatement();
      statement.executeUpdate(command);
      statement.close();
   }

   public String[] split(String input, String sep)
   {
      Vector vector = new Vector();
      StringTokenizer st = new StringTokenizer(input, sep);
      while (st.hasMoreTokens())
         vector.add(st.nextToken());
      String[] results = new String[vector.size()];
      for (int i = 0; i < vector.size(); i++)
         results[i] = (String)vector.elementAt(i);
      return results;
   }
}

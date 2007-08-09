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
    private boolean trace;
    private long chunksize;

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

    public void replicationLog(String log) {
        Logging.log("ReplicationDataPump ID #" + replicationid + ": " + log);
    }

    public ReplicationDataPump(String replicationid)
    {
        this.replicationid = replicationid;
        p = Configuration.getProperties();
        driver = p.getProperty("service.mysql.driver");
        url = p.getProperty("service.mysql.url");
        user = p.getProperty("service.mysql.user");
        password = p.getProperty("service.mysql.password");
        openConnection();
      
        String tmp = p.getProperty("service.datapump.trace");
        trace = tmp != null && tmp.equals("1");
        chunksize = 32000;
        tmp = p.getProperty("service.datapump.chunksize");
        if (tmp != null) {
            try {
                chunksize = Long.parseLong(tmp); 
            } catch (Exception e) {
                replicationLog("Parsing error (" + e +
                               ") when loading property" +
                               " service.datapump.chunksize with value: " +
                               tmp + " using default value (" +
                               chunksize + ")");
            }
        }
    }

    public void openConnection()
    {
        try
            {
                Class.forName(driver).newInstance();
                connection = DriverManager.getConnection(url, user, password);
                replicationLog("Database Connection Opened");
            }
        catch (Exception e)
            {
                e.printStackTrace();
                return;
            }
    }

    public void run()
    {
        replicationLog("Started run");
        if (!HibernateWrapper.databaseUp())
            {
                HibernateWrapper.start();
                if (!HibernateWrapper.databaseUp())
                    {
                        replicationLog("Hibernate Down - Exiting");
                        return;
                    }
            }

        while (true)
            {
                loop();
                if (exitflag)
                    {
                        cleanup();
                        replicationLog("Stopping/Exiting");
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
                replicationLog("Connection Closed");
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
        replicationLog("Exit Requested");
    }

    public void loop()
    {
        if (exitflag)
            return;

        if (!HibernateWrapper.databaseUp())
            { 
                replicationLog("Hibernate Down - Exiting");
                exitflag = true;
                return;
            }

        command = "SELECT * FROM Replication WHERE replicationid = " + replicationid;

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
                replicationLog("Executing Command: " + command);
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
                        table = resultSet.getString("recordtable");
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
                replicationLog("Error executing command: " + command);
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
        String tables = table + "_Meta M";
        String where = "M.dbid > " + dbid;
        if (probename.startsWith("VO:")) {
            probename = xp.replace(probename, "VO:", "");
            tables += ", " + table + " T, VO V, VONameCorrection C";
            where += " AND V.VOName = BINARY " + dq + probename + dq +
                " AND M.dbid = T.dbid AND" + cr +
                "        T.VOName = BINARY C.VOName AND" + cr +
                "        ((BINARY T.ReportableVOName = BINARY C.ReportableVOName)" + cr +
                "         OR ((T.ReportableVOName IS NULL) AND" + cr +
                "             (C.ReportableVOName IS NULL))) AND" + cr +
                "        C.void = V.void";
        } else if (probename.startsWith("Probe:")) {
            probename = xp.replace(probename, "Probe:", "");
            where += " AND M.ProbeName = " + dq + probename + dq;
        } else if (probename.startsWith("Grid:")) {
            probename = xp.replace(probename, "Grid:", "");
            where += " AND M.Grid";
            if (probename.equals("<null>")) {
                where += " IS NULL";
            } else {
                where += " = " + dq + probename + dq;
            }
        }

        command = "SELECT count(*) FROM " + tables + cr + "  WHERE " + where;

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
                replicationLog("Error during replication executing command: " + command);
                e.printStackTrace();
                cleanup();
                exitflag = true;
                return;
            }

        replicationLog("Executed Command: " + command);
        replicationLog("Records: " + count);
        replicationLog("ChunkSize: " + chunksize);

        command = "SELECT M.dbid FROM " + tables + cr + "  WHERE " + where +
            cr + "  LIMIT " + chunksize;

        //
        // start replication
        //

        Post post = null;

        try
            {
                statement =
                    connection.prepareStatement(command,
                                                java.sql.ResultSet.TYPE_FORWARD_ONLY,
                                                java.sql.ResultSet.CONCUR_READ_ONLY);
                resultSet = statement.executeQuery(command);
                command = "";
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
                                replicationLog("Received Null XML: dbid: " + dbid);
                                continue;
                            }
                        if (trace) {
                            replicationLog("TRACE dbid: " + dbid);
                            replicationLog("TRACE xml: " + xml);
                        }
                        if (security.equals("0"))
                            post = new Post(openconnection + "/gratia-servlets/rmi", "update", xml);
                        else
                            post = new Post(secureconnection + "/gratia-servlets/rmi", "update", xml);
                        replicationLog("Sending: " + dbid);
                        String response = post.send();
                        String[] results = split(response, ":");
                        if (!results[0].equals("OK"))
                            {
                                replicationLog("Error During Post: " + response);
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
                replicationLog("Run Complete");
            }
        catch (Exception e)
            {
                if (!HibernateWrapper.databaseUp())
                    {
                        replicationLog("Database Connection Error");
                        cleanup();
                        exitflag = true;
                        return;
                    }
                replicationLog("Error During Replication");
                if (command.length() > 0) {
                    replicationLog("Active command was " + command);
                }
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

        replicationLog("getXML: dbid: " + dbid);

        session = HibernateWrapper.getSession();
        String command = "from "+table+" t where t.RecordId = " + dbid;
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

package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Execute;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

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
    java.sql.Connection dbconnection;
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

    public void replicationLog(String log, Exception ex) {
        Logging.log("ReplicationDataPump ID #" + replicationid + ": " + log, ex);
    }

    public ReplicationDataPump(String replicationid)
    {
        this.replicationid = replicationid;
        p = Configuration.getProperties();
        driver = p.getProperty("service.mysql.driver");
        url = p.getProperty("service.mysql.url");
        user = p.getProperty("service.mysql.user");
        password = p.getProperty("service.mysql.password");

        if (!openDatabaseConnection()) {
            exitflag = true;
            return;
        }
      
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

    public boolean openDatabaseConnection()
    {
        try
            {
                Class.forName(driver).newInstance();
                dbconnection = DriverManager.getConnection(url, user, password);
                replicationLog("Database Connection Opened");
                return true;
            }
        catch (Exception e)
            {
                replicationLog("Database Connection failed to open",e);
                return false;
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
                dbconnection.close();
                replicationLog("Database Connection Closed");
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
        String target = "";

        try
            {
                replicationLog("Executing Command: " + command);
                statement = dbconnection.prepareStatement(command);
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
                        if (table == null || table == "") table = "JobUsageRecord";
                    }
                resultSet.close();
                statement.close();
            }
        catch (Exception e)
            {
                if (!HibernateWrapper.databaseUp())
                    {
                        exitflag = true;
                        return;
                    }
                replicationLog("Error executing command: " + command, e);
                // e.printStackTrace();
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
                statement = dbconnection.prepareStatement(command);
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
                        exitflag = true;
                        return;
                    }
                replicationLog("Error during replication executing command: " + command, e);
                // e.printStackTrace();
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

        if (security.equals("0"))
            target = openconnection + "/gratia-servlets/rmi";
        else
            target = secureconnection + "/gratia-servlets/rmi";

        try
            {
                statement =
                    dbconnection.prepareStatement(command,
                                                  java.sql.ResultSet.TYPE_FORWARD_ONLY,
                                                  java.sql.ResultSet.CONCUR_READ_ONLY);
                resultSet = statement.executeQuery(command);
                command = "";

                int bundle_size = 10;
                int bundle_count = 0;
                String lowdbid = "0";
                StringBuilder xml_msg = new StringBuilder();

                while (resultSet.next())
                    {
                        if (exitflag)
                            {
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

                        xml_msg.append(xml);
                        if (bundle_count == 0) lowdbid = dbid;
                        bundle_count = bundle_count + 1;
                        if (bundle_count == bundle_size) {
                            
                            uploadXML(target,xml_msg.toString(),lowdbid, dbid, bundle_count);

                            if (exitflag) return;

                            bundle_count = 0;
                            xml_msg = new StringBuilder();
                        }
                    }
                if (bundle_count != 0) {
                    uploadXML(target, xml_msg.toString(), lowdbid, dbid, bundle_count);
                    if (exitflag) return;
                }
                resultSet.close();
                statement.close();
                replicationLog("Run Complete");
            }
        catch (Exception e)
            {
                if (!HibernateWrapper.databaseUp())
                    {
                        replicationLog("Database Database Connection Error", e);
                        exitflag = true;
                        return;
                    }
                if (command.length() > 0) {
                    replicationLog("Error During Replication\n Active command was " + command, e);
                } else {
                    replicationLog("Error During Replication",e);
                }                    
                // e.printStackTrace();
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
        StringBuilder buffer = new StringBuilder();

        int i = 0;

        replicationLog("getXML: dbid: " + dbid);

        session = HibernateWrapper.getSession();
        String command = "from "+table+" t where t.RecordId = " + dbid;
        List result = session.createQuery(command).list();
        for (i = 0; i < result.size(); i++)
            {
                Record record = (Record)result.get(i);
                buffer.append("replication|");
                buffer.append(record.asXML() + "|");
                buffer.append(record.getRawXml() + "|");
                buffer.append(record.getExtraXml() + "|");
            }
        session.close();
        return buffer.toString();
    }

    public void uploadXML(String target, String xml, String low, String high, int nrows) throws Exception
    {
        Post post = new Post(target, "update", xml);

        replicationLog("Sending: " + low + " to " + high);

        String response = post.send();
        
        if (post.success && response != null) {
            String[] results = split(response, ":");
            if (!results[0].equals("OK"))
                {
                    replicationLog("Error During Post: " + response);
                    exitflag = true;
                    return;
                }
            //
            // update replicationtable
            //
            updateReplicationTable(high, nrows);
        } else {
            replicationLog("Error during Replication.",post.exception);
            exitflag = true;
            return;
        }
    }

    public void updateReplicationTable(String dbid, int nrows) throws Exception
    {
        String command =
            "update Replication" + cr +
            " set dbid = " + dbid + comma + cr +
            " rowcount = rowcount + " + nrows + cr +
            " where replicationid = " + replicationid;

        Statement statement = dbconnection.createStatement();
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

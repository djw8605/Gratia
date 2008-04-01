package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.util.*;
import java.sql.*;

public class ReplicationService extends Thread
{
    public String driver;
    public String url;
    public String user;
    public String password;
    
    Connection dbconnection;
    Statement statement;
    ResultSet resultSet;
    
    String command;
    Hashtable table = new Hashtable();
    
    Properties p;
    
    XP xp = new XP();
    
    public ReplicationService()
    {
        p = net.sf.gratia.util.Configuration.getProperties();
        driver = p.getProperty("service.mysql.driver");
        url = p.getProperty("service.mysql.url");
        user = p.getProperty("service.mysql.user");
        password = p.getProperty("service.mysql.password");
        openDatabaseConnection();
    }
    
    public void openDatabaseConnection()
    {
        try
            {
                Class.forName(driver).newInstance();
                dbconnection = null;
                dbconnection = DriverManager.getConnection(url,user,password);
            }
        catch (Exception e)
            {
                dbconnection = null;
            }
    }
    
    public void run()
    {
        Logging.log("ReplicationService Started");
        while (true)
            loop();
    }
    
    public void loop()
    {
        if (dbconnection == null)
            openDatabaseConnection();
        if (dbconnection == null)
            {
                Logging.log("ReplicationService: No Database Connection: Sleeping");
                try
                    {
                        Thread.sleep(60 * 1000);
                    }
                catch (Exception ignore)
                    {
                    }
                return;
            }
        
        command = "select * from Replication";
        ReplicationDataPump pump = null;
        
        try
            {
                Vector stopped = new Vector();
                
                statement = dbconnection.prepareStatement(command);
                resultSet = statement.executeQuery(command);
                while (resultSet.next())
                    {
                        String replicationid = resultSet.getString("replicationid");
                        String running = resultSet.getString("running");
			
                        if (running.equals("0"))
                            stopped.add(replicationid);
                        
                        if (table.get(replicationid) != null)
                            {
                                pump = (ReplicationDataPump) table.get(replicationid);
                                if ((! pump.isAlive()) && (running.equals("1")))
                                    {
                                        Logging.log("ReplicationService: Starting DataPump: " + replicationid);
                                        pump = new ReplicationDataPump(replicationid);
                                        table.put(replicationid,pump);
                                        pump.start();
                                    }
                            }
                        else
                            {
                                if (running.equals("0"))
                                    continue;
                                pump = (ReplicationDataPump) table.get(replicationid);
                                Logging.log("ReplicationService: Starting DataPump: " + replicationid);
                                pump = new ReplicationDataPump(replicationid);
                                table.put(replicationid,pump);
                                pump.start();
                            }
                    }
                resultSet.close();
                statement.close();
                //
                // now - loop thru stopped and stop any threads that might still be running
                //
                for (Enumeration x = stopped.elements(); x.hasMoreElements();)
                    {
                        String key = (String) x.nextElement();
                        pump = (ReplicationDataPump) table.get(key);
                        if ((pump != null) && (pump.isAlive()))
                            {
                                Logging.log("ReplicationService: Stopping DataPump: " + key);
                                pump.exit();
                            }
                    }
            }
        catch (Exception e)
            {
                try
                    {
                        dbconnection.close();
                    }
                catch (Exception ignore)
                    {
                    }
                dbconnection = null;
            }
        try
            {
                Logging.log("ReplicationService: Sleeping");
                long wait = Integer.parseInt(p.getProperty("service.replication.wait"));
                wait = wait * 60 * 1000;
                Thread.sleep(wait);
            }
        catch (Exception ignore)
            {
            }
    }
}

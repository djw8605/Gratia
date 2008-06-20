package net.sf.gratia.services;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.util.*;
import java.sql.*;

public class ReplicationService extends Thread {
    String driver;
    String url;
    String user;
    String password;
    
    Connection dbconnection;
    Statement statement;
    ResultSet resultSet;
    
    String command;
    Hashtable pumpStore = new Hashtable();

    Properties p;

    Boolean stopRequested = false;
    
    public ReplicationService() {
        p = net.sf.gratia.util.Configuration.getProperties();
        driver = p.getProperty("service.mysql.driver");
        url = p.getProperty("service.mysql.url");
        user = p.getProperty("service.mysql.user");
        password = p.getProperty("service.mysql.password");
        openDatabaseConnection();
    }
    
    void openDatabaseConnection() {
        try {
            Class.forName(driver).newInstance();
            dbconnection = null;
            dbconnection = DriverManager.getConnection(url,user,password);
        }
        catch (Exception e) {
            dbconnection = null;
        }
    }
    
    public void run() {
        Logging.info("ReplicationService Started");
        while (!stopRequested) {
            loop();
        }
        Logging.info("ReplicationService: Stop requested");
        // Stop all pumps.
        Enumeration x = pumpStore.elements();
        while (x.hasMoreElements()) {
            String key = (String) x.nextElement();
            // Need to get the pump and shut it down
            ReplicationDataPump pump = (ReplicationDataPump) pumpStore.get(key);
            if ((pump != null) && (pump.isAlive())) {
                Logging.log("ReplicationService: Stopping DataPump: " + key);
                pump.exit();
            }
        }
        Logging.info("ReplicationService: All pumps sent exit() directive");
        Logging.info("ReplicationService: Exiting");
    }


    public void requestStop() {
        stopRequested = true;
    }

    public void loop() {
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
        
        try {
            Hashtable checkedPumps = new Hashtable();
                
            statement = dbconnection.prepareStatement(command);
            resultSet = statement.executeQuery(command);
            while (resultSet.next()) {
                String replicationid = resultSet.getString("replicationid");
                String running = resultSet.getString("running");
			
                checkedPumps.put(replicationid, running);

                if (pumpStore.get(replicationid) != null) {
                    pump = (ReplicationDataPump) pumpStore.get(replicationid);
                    if ((! pump.isAlive()) && (running.equals("1"))) {
                        Logging.log("ReplicationService: Starting DataPump: " + replicationid);
                        pump = new ReplicationDataPump(replicationid);
                        pumpStore.put(replicationid,pump);
                        pump.start();
                    }
                }
                else {
                    if (running.equals("0")) {
                        continue;
                    }
                    pump = (ReplicationDataPump) pumpStore.get(replicationid);
                    Logging.log("ReplicationService: Starting DataPump: " + replicationid);
                    pump = new ReplicationDataPump(replicationid);
                    pumpStore.put(replicationid,pump);
                    pump.start();
                }
            }
            resultSet.close();
            statement.close();
            //
            // now - loop thru stopped and stop any threads that might still be running
            //
            for (Enumeration x = pumpStore.elements(); x.hasMoreElements();) {
                String key = (String) x.nextElement();
                String running = (String) checkedPumps.get(key);
                if (running == null || running.equals("0")) {
                    // Need to get the pump and shut it down
                    pump = (ReplicationDataPump) pumpStore.get(key);
                    if ((pump != null) && (pump.isAlive())) {
                        Logging.log("ReplicationService: Stopping DataPump: " + key);
                        pump.exit();
                    }
                }
            }
        }
        catch (Exception e) {
            try {
                dbconnection.close();
            }
            catch (Exception ignore) {
            }
            dbconnection = null;
        }
        try {
            Logging.log("ReplicationService: Sleeping");
            long wait = Integer.parseInt(p.getProperty("service.replication.wait"));
            wait = wait * 60 * 1000;
            Thread.sleep(wait);
        }
        catch (Exception ignore) {
        }
    }
}

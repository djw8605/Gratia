package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.util.*;

public class AccountingMonitor extends Thread {
    public String rmilookup;

    public String rmibind;

    public String service;

    public String from;

    public String externalrmi;

    public Hashtable accountingTable;

    public Properties p;

    public XP xp = new XP();

    public String tableLocation;

    public long monitorWait;

    public AccountingMonitor() {
        p = Configuration.getProperties();
        tableLocation = Configuration.getAccountingTablePath();

        rmilookup = p.getProperty("service.rmi.rmilookup");
        rmibind = p.getProperty("service.rmi.rmibind");
        service = p.getProperty("service.rmi.service");
        from = p.getProperty("service.external.http");
        externalrmi = p.getProperty("service.external.rmi");

        monitorWait = Long.parseLong(p.getProperty("service.monitor.wait"));
        monitorWait = monitorWait * (1000 * 60);

        //
        // load the accounting table
        //

        accountingTable = Configuration.getAccountingTable();

        //
        // now - initialize the server entries in it
        //
        Enumeration iter;
        Hashtable tempTable = new Hashtable();
        for (int i = 0; i < 500; i++) {
            String server = p.getProperty("service.monitor.server." + i);
            if (server != null) {
                tempTable.put(server, server);
                if (accountingTable.get(server) == null) {
                    Logging.info("AccountingMonitor: Adding: " + server);
                    accountingTable.put(server, new Long(0));
                }
            }
        }
        //
        // clean out old entries
        //
        iter = accountingTable.keys();
        while (iter.hasMoreElements()) {
            String key = (String) iter.nextElement();
            if (tempTable.get(key) == null) {
                Logging.info("AccountingMonitor: Dropping Server: " + key);
                accountingTable.remove(key);
            }
        }
        //
        // print out current results
        //
        iter = accountingTable.keys();
        while (iter.hasMoreElements()) {
            String key = (String) iter.nextElement();
            Long value = (Long) accountingTable.get(key);
            Logging.info("MonitorService: Monitoring Server: " + key
                    + " Current DBID: " + value);
        }
        //
        // save current contents
        //
        Configuration.saveAccountingTable(accountingTable);
    }

    public void run() {
        String to;
        Long value;
        Logging.info("AccountingMonitor: Started");
        Hashtable accountingTable = null;

        while (true) {
            try {
                Logging.info("AccountingMonitor: Sleeping");
                sleep(monitorWait);
            } catch (Exception ignore) {
            }

            accountingTable = Configuration.getAccountingTable();

            Enumeration iter = accountingTable.keys();
            while (iter.hasMoreElements()) {
                to = (String) iter.nextElement();
                value = (Long) accountingTable.get(to);
                try {
                    Post post = new Post(to, "remoteRequest", from, to, ""
                            + value.longValue());
                    post.add("from", from);
                    post.add("to", to);
                    post.add("rmi", externalrmi);
                    post.send();
                    if (post.success) {
                        Logging.info("AccountingMonitor: Sent Request To: " + to
                                     + " DBID: " + value);
                    } else {
                        Logging
                            .info("AccountingMonitor: Unable To Send Request To: "
                                  + to);
                    }   
                } catch (Exception e) {
                    Logging
                            .info("AccountingMonitor: Unable To Send Request To: "
                                    + to);
                    Logging.info(xp.parseException(e));
                    continue;
                }
            }
        }
    }
}

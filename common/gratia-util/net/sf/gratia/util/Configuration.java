package net.sf.gratia.util;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Logging;

import java.util.*;
import java.io.*;

public class Configuration {

    public static String getCatalinaHome()
    {
        return System.getProperty("catalina.home");
    }

    public static String getConfigurationPath()
    {
        String path = System.getProperty("catalina.home");
        if (path == null) {
           path = ".";
        }
        path = path + "/gratia";
        return path;
    }

    public static String getHibernateConfigurationPath()
    {
        String path = System.getProperty("catalina.home");
        if (path == null) {
           path = ".";
        }
        path = path + "/gratia/hibernate";
        return path;
    }

    public static String getHibernatePath()
    {
        return getHibernateConfigurationPath() + "/" + "hibernate.cfg.xml";
    }

    public static String getJMSPath()
    {
        return getConfigurationPath() + "/" + "a3servers.xml";
    }

    public static Properties getProperties()
    {
        Properties p = new Properties();
        try
            {
                p.load(new FileInputStream(new File(getConfigurationPath() + "/" + "service-configuration.properties")));
            }
        catch (Exception e)
            {
                Logging.log("Error Loading: " + getConfigurationPath() + "/" + "service-configuration.properties");
                e.printStackTrace();
            }
        if ((!p.containsKey("service.recordProcessor.threads")) &&
            p.containsKey("service.listener.threads")) {
            p.setProperty("service.recordProcessor.threads",
                          p.getProperty("service.listener.threads"));
            p.remove("service.listener.threads");
        }
        if ((!p.containsKey("monitor.recordProcessor.threads")) &&
            p.containsKey("monitor.listener.threads")) {
            p.setProperty("monitor.recordProcessor.threads",
                          p.getProperty("monitor.listener.threads"));
            p.remove("monitor.listener.threads");
        }
        if ((!p.containsKey("monitor.recordProcessor.wait")) &&
            p.containsKey("monitor.listener.wait")) {
            p.setProperty("monitor.recordProcessor.wait",
                          p.getProperty("monitor.listener.wait"));
            p.remove("monitor.listener.wait");
        }
        return p;
    }

    public static String getAccountingTablePath()
    {
        return getConfigurationPath() + "/" + "accounting-table";
    }

    public static synchronized Hashtable getAccountingTable()
    {
        Hashtable accountingTable = (Hashtable) XP.getObject(getAccountingTablePath());
        if (accountingTable == null)
            {
                accountingTable = new Hashtable();
                saveAccountingTable(accountingTable);
            }
        return accountingTable;
    }

    public static synchronized void saveAccountingTable(Hashtable table)
    {
        XP.putObject(getAccountingTablePath(),table);
    }

}

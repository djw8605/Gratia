package net.sf.gratia.util;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Logging;

import java.util.*;
import java.io.*;

public class Configuration {

    public static String getConfigurationPath()
    {
        String path = "/etc/gratia/collector";
        return path;
    }

    public static String getHibernateConfigurationPath()
    {
        String path = "/usr/share/gratia/hibernate";
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

        // Database users/passwords are maintained in there own config file
        // If they exist in the old one, we will igonre them.
        Properties auth = new Properties();
        try
            {
                auth.load(new FileInputStream(new File(getConfigurationPath() + "/" + "service-authorization.properties")));
            }
        catch (Exception e)
            {
                Logging.log("Error Loading: " + getConfigurationPath() + "/" + "service-authorization.properties");
                e.printStackTrace();
            }

        String attribute = "";
        attribute = "service.mysql.rootpassword";
        if (p.containsKey(attribute))    { p.remove(attribute); }
        if ( auth.containsKey(attribute)) { p.setProperty(attribute, auth.getProperty(attribute)); }

        attribute = "service.mysql.user";
        if (p.containsKey(attribute))    { p.remove(attribute); };
        if ( auth.containsKey(attribute)) { p.setProperty(attribute, auth.getProperty(attribute)); }

        attribute = "service.mysql.password";
        if (p.containsKey(attribute))    { p.remove(attribute); };
        if ( auth.containsKey(attribute)) { p.setProperty(attribute, auth.getProperty(attribute)); }

        attribute = "service.reporting.user";
        if (p.containsKey(attribute))    { p.remove(attribute); };
        if ( auth.containsKey(attribute)) { p.setProperty(attribute, auth.getProperty(attribute)); }

        attribute = "service.reporting.password";
        if (p.containsKey(attribute))    { p.remove(attribute); }
        if ( auth.containsKey(attribute)) { p.setProperty(attribute, auth.getProperty(attribute)); }

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

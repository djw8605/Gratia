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
        path = path + "/gratia";
        return path;
    }

    public static String getHibernatePath()
    {
        return getConfigurationPath() + "/" + "hibernate.cfg.xml";
    }

    public static String getGratiaHbmPath()
    {
        return getConfigurationPath() + "/" + "Gratia.hbm.xml";
    }

    public static String getJobUsagePath()
    {
        return getConfigurationPath() + "/" + "JobUsage.hbm.xml";
    }

    public static String getJobUsageSummaryPath()
    {
        return getConfigurationPath() + "/" + "JobUsageSummary.hbm.xml";
    }

    public static String getNodeSummaryPath()
    {
        return getConfigurationPath() + "/" + "NodeSummary.hbm.xml";
    }

    public static String getMetricRecordPath()
    {
        return getConfigurationPath() + "/" + "MetricRecord.hbm.xml";
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

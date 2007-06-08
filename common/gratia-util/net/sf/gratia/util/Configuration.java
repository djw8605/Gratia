package net.sf.gratia.util;

import java.util.*;
import java.io.*;

public class Configuration
{

	public static XP xp = new XP();

	public static String getCatalinaHome()
	{
		return System.getProperty("catalina.home");
	}

	public static String getConfigurationPath()
	{
		String path = System.getProperty("catalina.home");
		path = path + File.separator + "gratia";
		return path;
	}

	public static String getHibernatePath()
	{
		return getConfigurationPath() + File.separator + "hibernate.cfg.xml";
	}

	public static String getGratiaHbmPath()
	{
		return getConfigurationPath() + File.separator + "Gratia.hbm.xml";
	}

	public static String getJobUsagePath()
	{
		return getConfigurationPath() + File.separator + "JobUsage.hbm.xml";
	}

	public static String getMetricRecordPath()
	{
		return getConfigurationPath() + File.separator + "MetricRecord.hbm.xml";
	}

	public static String getJMSPath()
	{
		return getConfigurationPath() + File.separator + "a3servers.xml";
	}

	public static Properties getProperties()
	{
		Properties p = new Properties();
		try
		{
			p.load(new FileInputStream(new File(getConfigurationPath() + File.separator + "service-configuration.properties")));
		}
		catch (Exception e)
		{
			Logging.log("Error Loading: " + getConfigurationPath() + File.separator + "service-configuration.properties");
			e.printStackTrace();
		}
		
		return p;
	}

	public static String getAccountingTablePath()
	{
		return getConfigurationPath() + File.separator + "accounting-table";
	}

	public static synchronized Hashtable getAccountingTable()
	{
		Hashtable accountingTable = (Hashtable) xp.getObject(getAccountingTablePath());
		if (accountingTable == null)
		{
			accountingTable = new Hashtable();
			saveAccountingTable(accountingTable);
		}
		return accountingTable;
	}

	public static synchronized void saveAccountingTable(Hashtable table)
	{
		xp.putObject(getAccountingTablePath(),table);
	}

}

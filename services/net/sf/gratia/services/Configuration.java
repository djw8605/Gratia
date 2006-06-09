package net.sf.gratia.services;

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
				path = path + "/gratia";
				return path;
		}

		public static String getHibernatePath()
		{
				return getConfigurationPath() + "/" + "hibernate.cfg.xml";
		}

		public static String getJobUsagePath()
		{
				return getConfigurationPath() + "/" + "JobUsage.hbm.xml";
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
								System.out.println("Error Loading: " + getConfigurationPath() + "/" + "service-configuration.properties");
								e.printStackTrace();
						}
				return p;
		}

		public static Properties getCEProbeProperties()
		{
				Properties p = new Properties();
				try
						{
								p.load(new FileInputStream(new File(getConfigurationPath() + "/" + "ceprobe-configuration.properties")));
						}
				catch (Exception e)
						{
								System.out.println("Error Loading: " + getConfigurationPath() + "/" + "ceprobe-configuration.properties");
								e.printStackTrace();
						}
				return p;
		}

		public static void saveCEProbeProperties(String string)
		{
				xp.save(getConfigurationPath() + "/" + "ceprobe-configuration.properties",string);
		}

		public static String getAccountingTablePath()
		{
				return getConfigurationPath() + "/" + "accounting-table";
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

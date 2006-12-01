package net.sf.gratia.services;

import java.util.*;
import java.sql.*;
import java.io.*;
import java.text.*;

public class HistoryReaper
{
		public String driver;
		public String url;
		public String user;
		public String password;

		Connection connection;
		Statement statement;
		ResultSet resultSet;

		String command;

		Properties p;
		
		XP xp = new XP();

		String filenames[] = new String[0];

		public HistoryReaper()
		{
				Logging.log("HistoryReaper: Starting");

				p = net.sf.gratia.services.Configuration.getProperties();
				driver = p.getProperty("service.mysql.driver");
				url = p.getProperty("service.mysql.url");
				user = p.getProperty("service.mysql.user");
				password = p.getProperty("service.mysql.password");
				openConnection();
				getDirectories();
		}

		public void openConnection()
		{
				try
						{
								Class.forName(driver).newInstance();
								connection = null;
								connection = DriverManager.getConnection(url,user,password);
						}
				catch (Exception e)
						{
						}
		}

		public void deleteDirectory(String path)
		{
				String files[] = xp.getFileList(path);

				for (int i = 0; i < files.length; i++)
						{
								try
										{
												File file = new File(files[i]);
												file.delete();
										}
								catch (Exception ignore)
										{
										}
						}
				try
						{
								File file = new File(path);
								file.delete();
								Logging.log("HistoryReaper: Deleted Directory: " + path + ":Files: " + files.length);
						}
				catch (Exception ignore)
						{
						}
		}

		public void getDirectories()
		{
				int i = 0;
				Vector vector = new Vector();
				String path = System.getProperties().getProperty("catalina.home") + "/gratia/data";
				path = xp.replaceAll(path,"\\","/");
				Logging.log("HistoryReaper: Path: " + path);
				String temp[] = xp.getDirectoryList(path);
				for (i = 0; i < temp.length; i++)
						{
								String directory = temp[i];
								if ((directory.indexOf("history") > -1) || (directory.indexOf("old") > -1))
										vector.add(temp[i]);
						}
				Logging.log("HistoryReaper: Directories To Process: " + vector.size());
				//
				// figure out which directories to delete
				//
				java.util.Date now = new java.util.Date();
				long nowmilli = now.getTime();
				long historymilli = Long.parseLong(p.getProperty("maintain.history.log"));
				historymilli = historymilli * 24 * 60 * 60 * 1000;
				java.util.Date oldest = new java.util.Date(nowmilli - historymilli);
				Calendar beginning = Calendar.getInstance();
				beginning.setTime(oldest);
				beginning.set(Calendar.SECOND,0);
				beginning.set(Calendar.MINUTE,0);
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhh");
				String beginningHistory = path + "/history" + format.format(beginning.getTime());
				Logging.log("HistoryReaper: First Directory To Process: " + beginningHistory);
				//
				// now - screen/delete older directories
				//
				for (i = 0; i < vector.size(); i++)
						{
								String directory = (String) vector.elementAt(i);
								if (directory.compareTo(beginningHistory) < 0)
										{
												Logging.log("HistoryReaper: Deleting Directory: " + directory);
												deleteDirectory(directory);
										}
						}
		}

}

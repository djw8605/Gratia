package net.sf.gratia.services;

import java.util.*;
import java.sql.*;
import java.io.*;
import java.text.*;

public class RecoveryService extends Thread
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

		Vector history = new Vector();
		String filenames[] = new String[0];
		java.util.Date databaseDate = null;

		int irecords = 0;

		public RecoveryService()
		{
				System.out.println("RecoveryService: Starting");

				p = net.sf.gratia.services.Configuration.getProperties();
				driver = p.getProperty("service.mysql.driver");
				url = p.getProperty("service.mysql.url");
				user = p.getProperty("service.mysql.user");
				password = p.getProperty("service.mysql.password");
				openConnection();
				getDirectories();
				getDatabaseDate();
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
								System.out.println("RecoveryService: Deleted Directory: " + path + ":Files: " + files.length);
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
				System.out.println("RecorveryService: Path: " + path);
				String temp[] = xp.getDirectoryList(path);
				for (i = 0; i < temp.length; i++)
						if (temp[i].indexOf("history") > -1)
								vector.add(temp[i]);
				System.out.println("RecoveryService: Directories To Process: " + vector.size());
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
				System.out.println("RecoveryService: First Directory To Process: " + beginningHistory);
				//
				// now - screen/delete older directories
				//
				if (p.getProperty("replay.all.history").equals("0"))
						for (i = 0; i < vector.size(); i++)
								{
										String directory = (String) vector.elementAt(i);
										if (directory.compareTo(beginningHistory) < 0)
												{
														System.out.println("RecoveryService: Deleting Directory: " + directory);
														deleteDirectory(directory);
												}
										else
												history.add(directory);
								}
		}

		public void getDatabaseDate()
		{
				long days = Long.parseLong(p.getProperty("maintain.history.log"));
				long now = (new java.util.Date()).getTime();
				databaseDate = new java.util.Date(now - (days * 24 * 60 * 1000));

				command = "select max(ServerDate) from JobUsageRecord";
				try
						{
								statement = connection.prepareStatement(command);
								resultSet = statement.executeQuery(command);
								while(resultSet.next())
										{
												Timestamp timestamp = resultSet.getTimestamp(1);
												if (timestamp != null)
														databaseDate = new java.util.Date(timestamp.getTime());
										}
								resultSet.close();
								statement.close();
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}

				long temp = databaseDate.getTime() - (5 * 60 * 1000);
				databaseDate = new java.util.Date(temp);
				System.out.println("RecoveryService: Recovering From: " + databaseDate);
		}

		public void run()
		{
				String directory = "";
				System.out.println("RecoveryService: Started");
				for (int i = 0; i < history.size(); i++)
						{
								directory = (String) history.elementAt(i);
								System.out.println("RecoveryService: Processing Directory: " + directory);
								recover(directory);
						}
				System.out.println("RecoveryService: Exiting");
		}

		public void recover(String directory)
		{
				int i = 0;
				String connection = p.getProperty("service.open.connection");
				Post post = null;
				String timestamp;
				java.util.Date recordDate;

				filenames = xp.getFileList(directory);
				for (i = 0; i < filenames.length; i++)
						{
								String blob = xp.get(filenames[i]);
								try
										{
												StringTokenizer st = new StringTokenizer(blob,"|");
												st.nextToken();
												timestamp = st.nextToken();
												recordDate = new java.util.Date(Long.parseLong(timestamp));
										}
								catch (Exception e)
										{
												System.out.println("RecoveryService: Error:Processing File: " + filenames[i]);
												System.out.println("RecoveryService: Blob: " + blob);
												try
														{
																File temp = new File(filenames[i]);
																temp.delete();
														}
												catch (Exception ignore)
														{
														}
												continue;
										}
								if (recordDate.after(databaseDate) && p.getProperty("replay.all.history").equals("0"))
										{
												post = new Post(connection + "/gratia-servlets/rmi","update",blob);
												try
														{
																irecords++;
																post.send();
																System.out.println("RecoveryService: Sent: " + irecords + ":" + filenames[i] + " :Timestamp: " + recordDate);
														}
												catch (Exception e)
														{
																System.out.println("RecoveryService: Error Sending: " + filenames[i] + " Error: " + e);
																return;
														}
										}
								else if (p.getProperty("replay.all.history").equals("1"))
										{
												post = new Post(connection + "/gratia-servlets/rmi","update",blob);
												try
														{
																irecords++;
																post.send();
																System.out.println("RecoveryService: Sent: " + irecords + ":" + filenames[i] + " :Timestamp: " + recordDate);
														}
												catch (Exception e)
														{
																System.out.println("RecoveryService: Error Sending: " + filenames[i] + " Error: " + e);
																return;
														}
										}
								else

										{
												System.out.println("RecoveryService: Skipping: " + filenames[i] + " :Timestamp: " + recordDate);
										}
						}
		}
}

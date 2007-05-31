package net.sf.gratia.administration;

import net.sf.gratia.services.*;

import java.io.*;
import java.net.*;

import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;

import java.util.regex.*;
import java.text.*;

import java.rmi.*;

public class SystemAdministration extends HttpServlet 
{
		XP xp = new XP();
		//
		// database related
		//
		String driver = "";
		String url = "";
		String user = "";
		String password = "";
		Connection connection;
		Statement statement;
		ResultSet resultSet;
		//
		// processing related
		//
		String html = "";
		String row = "";
		StringBuffer buffer = new StringBuffer();
		//
		// globals
		//
		HttpServletRequest request;
		HttpServletResponse response;
		boolean initialized = false;
		Properties props;
		Properties p;
		String message = null;
		//
		// support
		//
		String dq = "\"";
		String comma = ",";
		String cr = "\n";

		public JMSProxy proxy = null;

		//
		// statics for recovery thread
		//

		public static RecoveryService recoveryService = null;
		public static String status = "";
		public static long skipped = 0;
		public static long processed = 0;
		public static long errors = 0;
		public static boolean replayall = false;

    public void initialize()
		{
				p = net.sf.gratia.services.Configuration.getProperties();
				try
						{
								proxy = (JMSProxy) Naming.lookup(p.getProperty("service.rmi.rmilookup") +
																								 p.getProperty("service.rmi.service"));
						}
				catch (Exception e)
						{
								Logging.warning(xp.parseException(e));
						}
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
		{
				initialize();
				this.request = request;
				this.response = response;
				if (request.getParameter("action") != null)
						{
								if (request.getParameter("action").equals("replay"))
										replay();
								else if (request.getParameter("action").equals("replayAll"))
										replayAll();
								else if (request.getParameter("action").equals("stopDatabaseUpdateThreads"))
										stopDatabaseUpdateThreads();
								else if (request.getParameter("action").equals("startDatabaseUpdateThreads"))
										startDatabaseUpdateThreads();
						}
				setup();
				process();
				response.setContentType("text/html");
				response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
				response.setHeader("Pragma", "no-cache"); // HTTP 1.0
				PrintWriter writer = response.getWriter();
				writer.write(html);
				writer.flush();
				writer.close();
		}

		public void setup()
		{
				html = xp.get(request.getRealPath("/") + "systemadministration.html");
		}

		public void process()
		{
				String status = "Active";

				html = xp.replaceAll(html,"#status#",SystemAdministration.status);
				html = xp.replaceAll(html,"#processed#","" + SystemAdministration.processed);
				html = xp.replaceAll(html,"#skipped#","" + SystemAdministration.skipped);

				try
						{
								boolean flag = proxy.databaseUpdateThreadsActive();
								if (flag)
										status = "Alive";
								else
										status = "Stopped";
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
				html = xp.replaceAll(html,"#threadstatus#",status);
		}

		public void replay()
		{
				if (SystemAdministration.recoveryService != null)
						if (SystemAdministration.recoveryService.isAlive())
								return;
				SystemAdministration.status = "Starting";
				SystemAdministration.skipped = 0;
				SystemAdministration.processed = 0;
				SystemAdministration.replayall = false;
				SystemAdministration.recoveryService = new RecoveryService();
				SystemAdministration.recoveryService.start();
		}

		public void replayAll()
		{
				if (SystemAdministration.recoveryService != null)
						if (SystemAdministration.recoveryService.isAlive())
								return;
				SystemAdministration.status = "Starting";
				SystemAdministration.skipped = 0;
				SystemAdministration.processed = 0;
				SystemAdministration.replayall = true;
				SystemAdministration.recoveryService = new RecoveryService();
				SystemAdministration.recoveryService.start();
		}

		public void stopDatabaseUpdateThreads()
		{
				try
						{
								proxy.stopDatabaseUpdateThreads();
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}

		public void startDatabaseUpdateThreads()
		{
				try
						{
								proxy.startDatabaseUpdateThreads();
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}

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
						SystemAdministration.status = "RecoveryService: Starting";

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

				public void getDirectories()
				{
						int i = 0;
						Vector vector = new Vector();
						String path = System.getProperties().getProperty("catalina.home") + "/gratia/data";
						path = xp.replaceAll(path,"\\","/");
						System.out.println("RecoveryService: Path: " + path);
						String temp[] = xp.getDirectoryList(path);
						for (i = 0; i < temp.length; i++)
								if (temp[i].indexOf("history") > -1)
										history.add(temp[i]);
						System.out.println("RecoveryService: Directories To Process: " + history.size());
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
						SystemAdministration.status = "Finished";
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
										if (SystemAdministration.replayall)
												{
														post = new Post(connection + "/gratia-servlets/rmi","update",blob);
														try
																{
																		irecords++;
																		post.send();
																		SystemAdministration.processed++;
																		System.out.println("RecoveryService: Sent: " + irecords + ":" + filenames[i] + " :Timestamp: " + recordDate);
																		SystemAdministration.status = "RecoveryService: Sent: " + irecords + ":" + filenames[i] + " :Timestamp: " + recordDate;
																}
														catch (Exception e)
																{
																		System.out.println("RecoveryService: Error Sending: " + filenames[i] + " Error: " + e);
																		return;
																}
												}
										else if (recordDate.after(databaseDate))
												{
														post = new Post(connection + "/gratia-servlets/rmi","update",blob);
														try
																{
																		irecords++;
																		post.send();
																		System.out.println("RecoveryService: Sent: " + irecords + ":" + filenames[i] + " :Timestamp: " + recordDate);
																		SystemAdministration.processed++;
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
														SystemAdministration.skipped++;
														System.out.println("RecoveryService: Skipping: " + filenames[i] + " :Timestamp: " + recordDate);
														SystemAdministration.status = "RecoveryService: Skipping: " + filenames[i] + " :Timestamp: " + recordDate;
												}
								}
				}
		}
}

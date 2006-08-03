package net.sf.gratia.services;

import java.util.*;
import java.sql.*;
import java.io.*;

import org.hibernate.SessionFactory;

public class ReplicationService extends Thread
{
		public String driver;
		public String url;
		public String user;
		public String password;

		Connection connection;
		Statement statement;
		ResultSet resultSet;

		String command;
		Hashtable table = new Hashtable();

		Properties p;
		
		XP xp = new XP();

		SessionFactory factory;

		public ReplicationService(SessionFactory factory)
		{
				this.factory = factory;
				p = Configuration.getProperties();
				driver = p.getProperty("service.mysql.driver");
				url = p.getProperty("service.mysql.url");
				user = p.getProperty("service.mysql.user");
				password = p.getProperty("service.mysql.password");
				openConnection();
		}

		public void openConnection()
		{
				try
						{
								Class.forName(driver).newInstance();
								connection = DriverManager.getConnection(url,user,password);
								Logging.log("ReplicationService: Database Connection Opened");
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}

		public void run()
		{
				System.out.println("ReplicationService Started");
				while (true)
						loop();
		}

		public void loop()
		{
				command = "select * from Replication";
				ReplicationDataPump pump = null;

				try
						{
								Vector stopped = new Vector();

								statement = connection.prepareStatement(command);
								resultSet = statement.executeQuery(command);
								while (resultSet.next())
										{
												String replicationid = resultSet.getString("replicationid");
												String running = resultSet.getString("running");
												
												if (running.equals("0"))
														stopped.add(replicationid);

												if (table.get(replicationid) != null)
														{
																pump = (ReplicationDataPump) table.get(replicationid);
																if ((! pump.isAlive()) && (running.equals("1")))
																		{
																				System.out.println("ReplicationService: Starting DataPump: " + replicationid);
																				pump = new ReplicationDataPump(replicationid,factory);
																				table.put(replicationid,pump);
																				pump.start();
																		}
														}
												else
														{
																if (running.equals("0"))
																		continue;
																pump = (ReplicationDataPump) table.get(replicationid);
																System.out.println("ReplicationService: Starting DataPump: " + replicationid);
																pump = new ReplicationDataPump(replicationid,factory);
																table.put(replicationid,pump);
																pump.start();
														}
										}
								resultSet.close();
								statement.close();
								//
								// now - loop thru stopped and stop any threads that might still be running
								//
								for (Enumeration x = stopped.elements(); x.hasMoreElements();)
										{
												String key = (String) x.nextElement();
												pump = (ReplicationDataPump) table.get(key);
												if ((pump != null) && (pump.isAlive()))
														{
																System.out.println("ReplicationService: Stopping DataPump: " + key);
																pump.exit();
														}
										}
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
				try
						{
								System.out.println("ReplicationService: Sleeping");
								long wait = Integer.parseInt(p.getProperty("service.replication.wait"));
								wait = wait * 60 * 1000;
								Thread.sleep(wait);
						}
				catch (Exception ignore)
						{
						}
		}
}

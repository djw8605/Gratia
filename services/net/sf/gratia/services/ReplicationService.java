package net.sf.gratia.services;

import java.util.*;
import java.sql.*;
import java.io.*;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

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
		org.hibernate.cfg.Configuration hibernateConfiguration;

		public ReplicationService(org.hibernate.cfg.Configuration hibernateConfiguration,SessionFactory factory)
		{
				this.hibernateConfiguration = hibernateConfiguration;
				this.factory = factory;
				p = net.sf.gratia.services.Configuration.getProperties();
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
								connection = null;
								connection = DriverManager.getConnection(url,user,password);
						}
				catch (Exception e)
						{
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
				if (connection == null)
						openConnection();
				if (connection == null)
						{
								System.out.println("ReplicationService: No Connection: Sleeping");
								try
										{
												Thread.sleep(60 * 1000);
										}
								catch (Exception ignore)
										{
										}
								return;
						}

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
																				pump = new ReplicationDataPump(replicationid,hibernateConfiguration,factory);
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
																pump = new ReplicationDataPump(replicationid,hibernateConfiguration,factory);
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
								try
										{
												connection.close();
										}
								catch (Exception ignore)
										{
										}
								connection = null;
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

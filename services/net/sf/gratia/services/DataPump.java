package net.sf.gratia.services;

import java.rmi.*;
import java.rmi.server.*;
import javax.jms.*;
import java.util.*;
import java.sql.*;
import java.io.*;

import org.hibernate.*;
import net.sf.gratia.storage.*;

public class DataPump extends Thread
{
		private String rmilookup;
		private String service;
		private String driver;
		private String url;
		private String user;
		private String password;

		String from;
		String to;
		String rmi;
		long dbid;
		Post post;
		java.sql.Connection connection;
		Statement statement;
		ResultSet resultSet;
		String command;

		int id;
		String rawxml;
		String xml;
		String extraxml;

		int irecords = 0;

		SessionFactory factory;
		org.hibernate.Session session;

		XP xp = new XP();

		public DataPump(String from,String to,String rmi,long dbid,SessionFactory factory)
		{
				this.from = from;
				this.to = to;
				this.rmi = rmi;
				this.dbid = dbid;
				this.factory = factory;
				loadProperties();
		}

		public void loadProperties()
		{
				try
						{
								Properties p = Configuration.getProperties();
								rmilookup = p.getProperty("service.rmi.rmilookup");
								service = p.getProperty("service.rmi.service");
								driver = p.getProperty("service.mysql.driver");
								url = p.getProperty("service.mysql.url");
								user = p.getProperty("service.mysql.user");
								password = p.getProperty("service.mysql.password");
						}
				catch (Exception ignore)
						{
						}
		}

		public JMSProxy getProxy()
		{
				JMSProxy proxy = null;
				try
						{
								proxy = (JMSProxy) Naming.lookup(rmi);
						}
				catch (Exception ignore)
						{
						}
				return proxy;
		}

		public void run()
		{
				Logging.info("DataPump: Starting");
				JMSProxy proxy = getProxy();
				if (proxy == null)
						Logging.info("DataPump: Remote RMI Unavailable: Will Use Post");
				try
						{
								Class.forName(driver).newInstance();
								connection = DriverManager.getConnection(url,user,password);
								Logging.log("DataPump: Database Connection Opened");
						}
				catch (Exception e)
						{
								e.printStackTrace();
								return;
						}

				command = "select dbid,RawXml,ExtraXml from JobUsageRecord" +
						" where dbid > " + dbid +
						" order by dbid";

				try
						{
								statement = connection.prepareStatement(command,java.sql.ResultSet.TYPE_FORWARD_ONLY,java.sql.ResultSet.CONCUR_READ_ONLY);
								statement.setFetchSize(Integer.MIN_VALUE);
								resultSet = statement.executeQuery(command);
								while(resultSet.next())
										{
												id = resultSet.getInt(1);
												rawxml = resultSet.getString(2);
												extraxml = resultSet.getString(3);
												if (extraxml == null)
														extraxml = "";
												xml = getXML(id);
												if (proxy != null)
														{
																boolean status = proxy.remoteUpdate(to,id,xml,rawxml,extraxml);
																Logging.log("DataPump: RMI: Sent: To: " + from  + " From: " + to + " DBID: " + id + " Status: " + status);
																if (! status)
																		{
																				Logging.log("DataPump: RMI: Error Sending DBID: " + id);
																				shutdown();
																				Logging.log("DataPump: RMI: Exiting: Records Sent: " + irecords);
																				return;
																		}
														}
												else
														{
																Logging.log("DataPump: Sending: From: " + from + " To: " + to + " DBID: " + id);
																post = new Post(from,"remoteUpdate","" + id,xml,rawxml,extraxml);
																post.add("from",to);
																post.add("to",from);
																String status = post.send();
																Logging.log("DataPump: Sent: To: " + from  + " From: " + to + " DBID: " + id + " Status: " + status);
																if (status == null)
																		{
																				Logging.log("DataPump: Error Sending DBID: " + id);
																				shutdown();
																				Logging.log("DataPump: Exiting: Records Sent: " + irecords);
																				return;
																		}
														}
												irecords++;
										}
						}
				catch (Exception e)
						{
								Logging.warning("DataPump: Error Sending DBID: " + id);
								e.printStackTrace();
						}
						
				shutdown();
				Logging.info("DataPump: Exiting: Records Sent: " + irecords);
		}

		public String getXML(int dbid)
		{
				StringBuffer buffer = new StringBuffer();
				int i = 0;

				try
						{
								session = factory.openSession();
								String command = "from JobUsageRecord where dbid = " + dbid;
								List result = session.createQuery(command).list();
								for(i = 0; i < result.size(); i++)
										{
												JobUsageRecord record = (JobUsageRecord) result.get(i);
												buffer.append(record.asXML());
										}
						}
				catch (Exception e)
						{
								Logging.warning(xp.parseException(e));
						}
				finally
						{
								session.close();
						}
				return buffer.toString();
		}

		public void shutdown()
		{
				try
						{
								resultSet.close();
								statement.close();
								connection.close();
						}
				catch (Exception ignore)
						{
						}
		}
}

package net.sf.gratia.services;

import java.util.Properties;
import java.util.Vector;
import java.util.StringTokenizer;

import java.sql.*;

public class StatusListenerThread extends Thread
{

		java.sql.Connection sqlconnection;
		Statement statement;

		//
		// database parameters
		//

		Properties p;
		XP xp = new XP();

		public StatusListenerThread()
		{
				p = Configuration.getProperties();
		}


		public void run()
		{
				try
						{
								String driver = p.getProperty("service.mysql.driver");
								String url = p.getProperty("service.mysql.url");
								String user = p.getProperty("service.mysql.user");
								String password = p.getProperty("service.mysql.password");
								Class.forName(driver);
								sqlconnection = DriverManager.getConnection(url,user,password);
								Logging.info("StatusListenerThread: Database Opened");
						}
				catch (Exception e)
						{
								Logging.warning("StatusListenerThread: Error Opening Database: " + e);
								Logging.warning(xp.parseException(e));
								Logging.warning("StatusListenerThread: Exiting");
								return;
						}

        Object lock = new Object();
				
				try
						{
								synchronized (lock) 
										{
												lock.wait();
										}
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}

		public void onMessage()
		{
				/*
				String dq = "\"";
				String comma = ",";
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

				Logging.info("StatusListenerThread: Received Message");

				String input = "";

				try
						{
								input = message.getStringProperty("xml");
						}
				catch (Exception e)
						{
								Logging.warning("StatusListenerThread: Error: " + e);
								Logging.warning(xp.parseException(e));
						}

				String tokens[] = split(input,":");
				Logging.info("StatusListerThread: Received: " + input);

				String probename = tokens[0];
				String status = tokens[1];

				try
						{
								statement = sqlconnection.createStatement();
								String command = "update Probe set" +
										" currenttime = timestamp(" + dq + format.format(new java.util.Date()) + dq + ")" + comma +
										" status = " + dq + status + dq +
										" where probename = " + dq + probename + dq;
								statement.execute(command);
								statement.close();
								if (tokens.length == 3)
										if (tokens[1].equals("lost"))
												{
														command = "insert into CEProbeStatus (currenttime,probename,probestatus,jobs,lostjobs) values(" +
																"timestamp(" + dq + format.format(new java.util.Date()) + dq + ")" + comma +
																dq + probename + dq + comma +
																dq + "lost" + dq + comma +
																"0" + comma + tokens[2] + ")";
														statement = sqlconnection.prepareStatement(command);
														statement.execute(command);
														statement.close();
												}
						}
				catch (Exception e)
						{
								Logging.warning("StatusListenerThread: Error: " + e);
								Logging.warning(xp.parseException(e));
						}
				*/
		}

		public String[] split(String input,String sep)
		{
				Vector vector = new Vector();
				StringTokenizer st = new StringTokenizer(input,sep);
				while(st.hasMoreElements())
						vector.add(st.nextElement());
				String results[] = new String[vector.size()];
				for (int i = 0; i < vector.size(); i++)
						results[i] = (String) vector.elementAt(i);
				return results;
		}
				

}




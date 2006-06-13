package net.sf.gratia.services;

import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Message;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.QueueReceiver;
import javax.jms.Queue;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import java.util.StringTokenizer;

import java.text.*;

import java.sql.*;

import net.sf.gratia.storage.*;

public class StatusListenerThread extends Thread
{
		//
		// jms things
		//

		QueueConnectionFactory qcf;
		Queue statusQueue;

		//
		// database parameters
		//

		Properties p;
		XP xp = new XP();

		public StatusListenerThread(QueueConnectionFactory qcf,Queue q)
		{
				this.qcf = qcf;
				this.statusQueue = q;
				p = Configuration.getProperties();
		}



		public void run()
		{
				QueueConnection qc = null;
				QueueSession qs = null;
				QueueReceiver qrec = null;
				Message msg;

				Connection connection;
				Statement statement;

				String dq = "\"";
				String comma = ",";
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

				Logging.info("StatusListenerThread: Starting JMS");
				try
						{
								qc = qcf.createQueueConnection();
								qs = qc.createQueueSession(false,javax.jms.Session.AUTO_ACKNOWLEDGE);
								qrec = qs.createReceiver(statusQueue);
								qc.start();
								Logging.info("StatusListenerThread: JMS Started");
						}
				catch (Exception e)
						{
								Logging.warning("StatusListenerThread: Error Starting JMS: " + e);
								Logging.warning(xp.parseException(e));
								Logging.warning("StatusListenerThread: Exiting");
								return;
						}

				Logging.info("StatusListenerThread: Opening Database");
				try
						{
								String driver = p.getProperty("service.mysql.driver");
								String url = p.getProperty("service.mysql.url");
								String user = p.getProperty("service.mysql.user");
								String password = p.getProperty("service.mysql.password");
								Class.forName(driver);
								connection = DriverManager.getConnection(url,user,password);
								Logging.info("StatusListenerThread: Database Opened");
						}
				catch (Exception e)
						{
								Logging.warning("StatusListenerThread: Error Opening Database: " + e);
								Logging.warning(xp.parseException(e));
								Logging.warning("StatusListenerThread: Exiting");
								return;
						}

				Logging.info("StatusListenerThread: Running");

				while(true)
						{
								try
										{
												msg = qrec.receive();
										}
								catch (Exception shutdown)
										{
												Logging.info("StatusListenerThread: Exiting");
												return;
										}
								Logging.info("StatusListenerThread: Received Message");

								String input = "";

								try
										{
												input = msg.getStringProperty("xml");
										}
								catch (Exception e)
										{
												Logging.warning("StatusListenerThread: Error: " + e);
												Logging.warning(xp.parseException(e));
												continue;
										}

								String tokens[] = split(input,":");
								Logging.info("StatusListerThread: Received: " + input);

								String probename = tokens[0];
								String status = tokens[1];

								try
										{
												statement = connection.createStatement();
												String command = "update CEProbes set" +
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
																		statement = connection.prepareStatement(command);
																		statement.execute(command);
																		statement.close();
																}
										}
								catch (Exception e)
										{
												Logging.warning("StatusListenerThread: Error: " + e);
												Logging.warning(xp.parseException(e));
										}
						}
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




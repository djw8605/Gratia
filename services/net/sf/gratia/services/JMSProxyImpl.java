package net.sf.gratia.services;

import java.rmi.*;
import java.rmi.server.*;
import javax.jms.*;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Date;
import java.io.*;
import java.text.*;

public class JMSProxyImpl extends UnicastRemoteObject implements JMSProxy
{
		private String rmilookup;
		private String service;
		private String driver;
		private String url;
		private String user;
		private String password;

		Hashtable pumps = new Hashtable();

		Properties p;

		String queues[] = null;
		int iq = 0;
		XP xp = new XP();

		int irecords = 0;


	public JMSProxyImpl() throws RemoteException
		{
				super();
				loadProperties();

				int maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));
				queues = new String[maxthreads];
				for (int i = 0; i < maxthreads; i++)
						queues[i] = System.getProperties().getProperty("catalina.home") + "/gratia/data/thread" + i;
		}

		public void loadProperties()
		{
				try
						{
								p = Configuration.getProperties();
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


		public boolean update(String xml) throws RemoteException
		{
				irecords++;

				if ((irecords % 100) == 0)
						{
								iq++;
								if (iq > (queues.length - 1))
										iq = 0;

								try
										{
												Thread.yield();
										}
								catch (Exception ignore)
										{
										}
						}

				try
						{
								File file = File.createTempFile("job","xml",new File(queues[iq]));
								String filename = file.getPath();
								xp.save(filename,xml);
								return true;
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
				return false;
		}


		public boolean statusUpdate(String xml) throws RemoteException
		{
				/*
				try
						{
								TextMessage message = jmssession.createTextMessage();
								message.setStringProperty("xml",xml);
								producer.send(message);
								jmssession.commit();
								// checkQueue();
								return true;
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
				*/

				return false;
		}
}

package net.sf.gratia.services;

import java.util.Properties;

import java.rmi.*;
import java.io.*;
import java.util.TimeZone;
import java.util.Enumeration;

import org.objectweb.joram.client.jms.admin.*;
import org.objectweb.joram.client.jms.*;
import org.objectweb.joram.client.jms.tcp.*;

import fr.dyade.aaa.agent.AgentServer;

import javax.servlet.*;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class CollectorService implements ServletContextListener
{
		public String rmibind;
		public String rmilookup;
		public String service;

		public Properties p;

		public org.hibernate.cfg.Configuration hibernateConfiguration;
		public org.hibernate.SessionFactory hibernateFactory;

		//
		// various threads
		//

		ListenerThread thread1;
		ListenerThread thread2;
		ListenerThread thread3;
		StatusListenerThread statusListenerThread;

		ProbeMonitorThread probeMonitorThread;
		RMIService rmiservice;
		Master master;

		public void contextInitialized(ServletContextEvent sce)
		{
				XP xp = new XP();
				String catalinaHome = "";

				Enumeration iter = System.getProperties().propertyNames();
				System.out.println("");
				while(iter.hasMoreElements())
						{
								String key = (String) iter.nextElement();
								String value = (String) System.getProperty(key);
								System.out.println("Key: " + key + " value: " + value);
						}
				System.out.println("");

				try
						{
								//
								// get configuration properties
								//

								p = net.sf.gratia.services.Configuration.getProperties();
								rmilookup = p.getProperty("service.rmi.rmilookup");
								rmibind = p.getProperty("service.rmi.rmibind");
								service = p.getProperty("service.rmi.service");
								
								//
								// create a dummy ReportingConfig.xml for birt
								//
								
								String dq = "\"";
								xp = new XP();
								StringBuffer xml = new StringBuffer();
								catalinaHome = System.getProperty("catalina.home");
								catalinaHome = xp.replaceAll(catalinaHome,"\\","/");

								xml.append("<ReportingConfig>" + "\n");
								xml.append("<DataSourceConfig" + "\n");
								xml.append("url=" + dq + p.getProperty("service.mysql.url") + dq + "\n");
								xml.append("user=" + dq + p.getProperty("service.mysql.user") + dq + "\n");
								xml.append("password=" + dq + p.getProperty("service.mysql.password") + dq + "\n");
								xml.append("/>" + "\n");
								xml.append("<PathConfig" + "\n");
								xml.append("reportsFolder=" + dq + catalinaHome + "/webapps/GratiaReports/" + dq + "\n");
								xml.append("engineHome=" + dq + catalinaHome + "/webapps/Birt/" + dq + "\n");
								xml.append("webappHome=" + dq + catalinaHome + "/webapps/GratiaReporting/" + dq + "\n");
								xml.append("/>" + "\n");
								xml.append("</ReportingConfig>" + "\n");
								xp.save(catalinaHome + "/webapps/GratiaReportConfiguration/ReportingConfig.xml",
												xml.toString());

								//
								// set default timezone
								//

								TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

								//
								// initialize logging
								//

								Logging.initialize(p.getProperty("service.service.logfile"),
																	 p.getProperty("service.service.maxlog"),
																	 p.getProperty("service.service.console"),
																	 p.getProperty("service.service.level"));
								//
								// start jms
								//

								File lock = new File(net.sf.gratia.services.Configuration.getConfigurationPath() + "/s1/lock");
								if (lock.exists())
										{
												Logging.info("CollectorService: Deleting Lock File");
												lock.delete();
										}

								System.setProperty(AgentServer.CFG_DIR_PROPERTY,net.sf.gratia.services.Configuration.getConfigurationPath());
								AgentServer.init((short) 0,net.sf.gratia.services.Configuration.getConfigurationPath() + "/s1",null);
								AgentServer.start();
								Thread.sleep(10);
								Logging.info("CollectorService: JMS Server Started");

								//
								// start rmi
								//

								rmiservice = new RMIService();
								rmiservice.setDaemon(true);

								rmiservice.start();
								Thread.sleep(10);

								//
								// setup topics/q's
								//

								AdminModule.collocatedConnect("root","root");

								Queue queue1 = (Queue) Queue.create("xml-accounting-queue");
								Queue queue2 = (Queue) Queue.create("status-queue");

								User user = User.create("anonymous", "anonymous");

								queue1.setFreeReading();
								queue1.setFreeWriting();

								queue2.setFreeReading();
								queue2.setFreeWriting();

								javax.jms.ConnectionFactory cf = TcpConnectionFactory.create("localhost", 16010);
								javax.jms.QueueConnectionFactory qcf = QueueTcpConnectionFactory.create("localhost", 16010);
								
								//
								// start database
								//

								try
										{
												hibernateConfiguration = new org.hibernate.cfg.Configuration();
												hibernateConfiguration.addFile(new File(net.sf.gratia.services.Configuration.getJobUsagePath()));
												hibernateConfiguration.configure(new File(net.sf.gratia.services.Configuration.getHibernatePath()));

												Properties hp = new Properties();
												hp.setProperty("hibernate.connection.driver_class",p.getProperty("service.mysql.driver"));
												hp.setProperty("hibernate.connection.url",p.getProperty("service.mysql.url"));
												hp.setProperty("hibernate.connection.username",p.getProperty("service.mysql.user"));
												hp.setProperty("hibernate.connection.password",p.getProperty("service.mysql.password"));
												hibernateConfiguration.addProperties(hp);

												hibernateFactory = hibernateConfiguration.buildSessionFactory();
												Logging.info("Database Opened");
										}
								catch (Exception databaseError)
										{
												databaseError.printStackTrace();
										}

								//
								// poke in rmi
								//

								JMSProxyImpl proxy = new JMSProxyImpl(qcf,queue1,queue2,hibernateFactory);
								Naming.rebind(rmibind + service,proxy);
								
								//
								// start msg listener
								//

								javax.jms.Connection connection = (javax.jms.Connection) cf.createConnection();
								connection.start();
								AdminModule.disconnect();

								thread1 = new ListenerThread("Listener 1",hibernateFactory,qcf,queue1);
								thread1.setPriority(Thread.MAX_PRIORITY);
								thread1.setDaemon(true);
								thread1.start();

								thread2 = new ListenerThread("Listener 2",hibernateFactory,qcf,queue1);
								thread2.setPriority(Thread.MAX_PRIORITY);
								thread2.setDaemon(true);
								thread2.start();

								thread3 = new ListenerThread("Listener 3",hibernateFactory,qcf,queue1);
								thread3.setPriority(Thread.MAX_PRIORITY);
								thread3.setDaemon(true);
								thread3.start();
								
								statusListenerThread = new StatusListenerThread(qcf,queue2);
								statusListenerThread.setDaemon(true);
								statusListenerThread.start();

								//
								// start probe monitor
								//

								probeMonitorThread = new ProbeMonitorThread(hibernateFactory);
								probeMonitorThread.setDaemon(true);
								probeMonitorThread.start();

								//
								// start monitoring service
								//

								if (p.getProperty("service.monitor.start").equals("1"))
										{
												AccountingMonitor monitor = new AccountingMonitor();
												monitor.start();
										}
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}

				master = new Master();
				master.setDaemon(true);
				master.start();
		}

		public void contextDestroyed(ServletContextEvent sce)
		{
				Logging.info("");
				Logging.info("Context Destroy Event");
				Logging.info("");
				System.exit(0);
		}

		//
		// testing
		//

		static public void main(String args[])
		{
				CollectorService service = new CollectorService();
				service.contextInitialized(null);
		}
}

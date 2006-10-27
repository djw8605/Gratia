package net.sf.gratia.services;

import java.util.Properties;

import java.rmi.*;
import java.io.*;
import java.net.*;
import java.util.TimeZone;
import java.util.Enumeration;

import javax.servlet.*;

import java.sql.*;

import java.security.*;

public class CollectorService implements ServletContextListener
{
		public String rmibind;
		public String rmilookup;
		public String service;

		public Properties p;

		//
		// various threads
		//

		ListenerThread threads[];
		StatusListenerThread statusListenerThread;
		ReplicationService replicationService;
		ProbeMonitorService probeMonitorService;
		RMIService rmiservice;
		QSizeMonitor qsizeMonitor;;

		XP xp = new XP();

		public String configurationPath;

		String queues[] = null;

		Object lock = new Object();

		public void contextInitialized(ServletContextEvent sce)
		{
				String catalinaHome = "";
				int i = 0;

				Enumeration iter = System.getProperties().propertyNames();
				System.out.println("");
				while(iter.hasMoreElements())
						{
								String key = (String) iter.nextElement();
								String value = (String) System.getProperty(key);
								System.out.println("Key: " + key + " value: " + value);
						}
				System.out.println("");

				p = net.sf.gratia.services.Configuration.getProperties();

				System.out.println("");
				System.out.println("service properties:");
				System.out.println("");
				iter = p.propertyNames();
				while(iter.hasMoreElements())
						{
								String key = (String) iter.nextElement();
								String value = (String) p.getProperty(key);
								System.out.println("Key: " + key + " value: " + value);
						}
				System.out.println("");
				System.out.println("service.security.level: " + p.getProperty("service.security.level"));

				configurationPath = net.sf.gratia.services.Configuration.getConfigurationPath();

				if (p.getProperty("service.security.level").equals("1"))
						try
								{
										System.out.println("");
										System.out.println("Initializing HTTPS Support");
										System.out.println("");
										//
										// setup configuration path/https system parameters
										//
										System.setProperty("java.protocol.handler.pkgs","com.sun.net.ssl.internal.www.protocol");
										Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

										System.setProperty("javax.net.ssl.trustStore",configurationPath + "/truststore");
										System.setProperty("javax.net.ssl.trustStorePassword","server");

										System.setProperty("javax.net.ssl.keyStore",configurationPath + "/keystore");
										System.setProperty("javax.net.ssl.keyStorePassword","server");

										com.sun.net.ssl.HostnameVerifier hv=new com.sun.net.ssl.HostnameVerifier() 
												{
														public boolean verify(String urlHostname, String certHostname) 
														{
																System.out.println("url host name: " + urlHostname);
																System.out.println("cert host name: " + certHostname);
																System.out.println("WARNING: Hostname is not matched for cert.");
																return true;
														}
												};

										com.sun.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(hv);
								}
						catch (Exception e)
								{
										e.printStackTrace();
								}

				try
						{
								//
								// get configuration properties
								//

								rmilookup = p.getProperty("service.rmi.rmilookup");
								rmibind = p.getProperty("service.rmi.rmibind");
								service = p.getProperty("service.rmi.service");
								
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
								// start rmi
								//

								rmiservice = new RMIService();
								rmiservice.setDaemon(true);
								rmiservice.start();
								Thread.sleep(10);
								System.out.println("");
								System.out.println("CollectorService: RMI Service Started");
								System.out.println("");

								//
								// start database
								//

								HibernateWrapper.start();

								//
								// zap database
								//

								zapDatabase();

								//
								// setup queues for message handling
								//

								int maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));
								queues = new String[maxthreads];

								Execute.execute("mkdir " + configurationPath + "/data");
								for (i = 0; i < maxthreads; i++)
										{
												Execute.execute("mkdir " + configurationPath + "/data/thread" + i);
												queues[i] = configurationPath + "/data/thread" + i;
												System.out.println("Created Q: " + queues[i]);
										}

								System.out.println("");
								System.out.println("CollectorService: JMS Server Started");
								System.out.println("");

								//
								// poke in rmi
								//

								JMSProxyImpl proxy = new JMSProxyImpl(this);
								Naming.rebind(rmibind + service,proxy);
								System.out.println("JMSProxy Started");

								//
								// whack old history
								//

								new HistoryReaper();

								//
								// start a thread to recheck history directories every 6 hours
								//

								HistoryMonitor historyMonitor = new HistoryMonitor();
								historyMonitor.start();

								//
								// start msg listener
								//

								threads = new ListenerThread[maxthreads];
								for (i = 0; i < maxthreads; i++)
										{
												threads[i] = new ListenerThread("ListenerThread: " + i,queues[i],lock);
												threads[i].setPriority(Thread.MAX_PRIORITY);
												threads[i].setDaemon(true);
										}
								for (i = 0; i < maxthreads; i++)
										threads[i].start();

								//
								// if requested - start service to monitor input queue sizes
								//

								if (p.getProperty("monitor.q.size").equals("1"))
										{
												System.out.println("CollectorService: Starting QSizeMonitor");
												qsizeMonitor = new QSizeMonitor();
												qsizeMonitor.start();
										}

								/*
									statusListenerThread = new StatusListenerThread();
									statusListenerThread.setDaemon(true);
									statusListenerThread.start();
								*/

								//
								// start probe monitor
								//

								probeMonitorService = new ProbeMonitorService();
								probeMonitorService.setDaemon(true);
								probeMonitorService.start();
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}

				//
				// add a server cert if one isn't there
				//

				if (p.getProperty("service.security.level").equals("1"))
						{
								if ((p.getProperty("service.use.selfgenerated.certs") != null) &&
										(p.getProperty("service.use.selfgenerated.certs").equals("1")))
										loadSelfGeneratedCerts();
								else
										loadVDTCerts();
						}

				//
				// start replication service
				//

				replicationService = new ReplicationService();
				replicationService.start();

				//
				// wait 1 minute to create new report config for birt (giving tomcat time to deploy the war)
				//

				(new ReportSetup()).start();
		}


		public void stopDatabaseUpdateThreads()
		{
				int i;
				int maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));
				for (i = 0; i < maxthreads; i++)
						{
								threads[i].stopRequest();
						}
				try
						{
								Thread.sleep(60 * 1000);
						}
				catch (Exception ignore)
						{
						}
		}

		public void startDatabaseUpdateThreads()
		{
				int i;
				int maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));
				threads = new ListenerThread[maxthreads];
				for (i = 0; i < maxthreads; i++)
						{
								threads[i] = new ListenerThread("ListenerThread: " + i,queues[i],lock);
								threads[i].setPriority(Thread.MAX_PRIORITY);
								threads[i].setDaemon(true);
						}
				for (i = 0; i < maxthreads; i++)
						threads[i].start();

		}

		public boolean databaseUpdateThreadsActive()
		{
				int i;
				int maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));

				for (i = 0; i < maxthreads; i++)
						if (threads[i].isAlive())
								return true;
				return false;
		}

		public void loadSelfGeneratedCerts()
		{
				String dq = "\"";
				String keystore = System.getProperty("catalina.home") + "/gratia/keystore";
				keystore = xp.replaceAll(keystore,"\\","/");
				String command1[] =
						{"keytool",
						 "-genkey",
						 "-dname",
						 "cn=server, ou=Fermi-GridAccounting, o=Fermi, c=US",
						 "-alias",
						 "server",
						 "-keystore",
						 keystore,
						 "-keypass",
						 "server",
						 "-storepass",
						 "server"};

				int exitValue1 = Execute.execute(command1);

				String command2[] =
						{"keytool",
						 "-selfcert",
						 "-alias",
						 "server",
						 "-keypass",
						 "server",
						 "-keystore",
						 keystore,
						 "-storepass",
						 "server"};

				if (exitValue1 == 0)
						Execute.execute(command2);
				FlipSSL.flip();
		}

		public void loadVDTCerts()
		{
				String dq = "\"";
				String keystore = System.getProperty("catalina.home") + "/gratia/keystore";
				String configurationPath = System.getProperty("catalina.home") + "/gratia/";
				keystore = xp.replaceAll(keystore,"\\","/");
				String command1[] =
						{
								"openssl",
								"pkcs12",
								"-export",
								"-out",
								configurationPath + "server.pkcs12",
								"-inkey",
								p.getProperty("service.vdt.key.file"),
								"-in",
								p.getProperty("service.vdt.cert.file"),
								"-passin",
								"pass:server",
								"-passout",
								"pass:server"
						};

				int exitValue1 = Execute.execute(command1);

				if (exitValue1 == 0)
						{
								PKCS12Load load = new PKCS12Load();
								try
										{
												load.load(
																	configurationPath + "server.pkcs12",
																	keystore
																	);
										}
								catch (Exception e)
										{
												e.printStackTrace();
										}
						}
				FlipSSL.flip();
		}

		public void zapDatabase()
		{
				String dq = "\"";
				String comma = ",";
				XP xp = new XP();
				int i = 0;

				Properties p = net.sf.gratia.services.Configuration.getProperties();

				String driver = p.getProperty("service.mysql.driver");
				String url = p.getProperty("service.mysql.url");
				String user = p.getProperty("service.mysql.user");
				String password = p.getProperty("service.mysql.password");
	
				java.sql.Connection connection;
				Statement statement;
				ResultSet resultSet;

				String gratiaVersion = p.getProperty("gratia.version");
				String gratiaDatabaseVersion = p.getProperty("gratia.database.version");

				try
						{
								Class.forName(driver);
								connection = (java.sql.Connection) DriverManager.getConnection(url,user,password);
						}
				catch (Exception e)
						{
								Logging.warning("CollectorService: Error During zapDatabase: " + e);
								Logging.warning(xp.parseException(e));
								return;
						}

				String commands1[] = 
						{
								"alter table CETable add unique index index02(facility_name)",
								"alter table CEProbes add unique index index02(probename)",
								"insert into CETable(facility_name) values(" + dq + "Unknown" + dq + ")",
								"alter table JobUsageRecord add index index02(EndTime)",
								"alter table JobUsageRecord add index index03(ProbeName)",
								"alter table JobUsageRecord add index index04(HostDescription)",
								"alter table JobUsageRecord add index index05(StartTime)",
								"alter table JobUsageRecord add index index06(GlobalJobid)",
								"alter table JobUsageRecord add index index07(LocalJobid)",
								"alter table JobUsageRecord add index index08(Host(255))",
								"alter table JobUsageRecord drop index index09",
								"alter table JobUsageRecord drop index index10",
								"alter table JobUsageRecord add index index11(ServerDate)",
								"alter table JobUsageRecord add unique index index12(md5)",
								"alter table JobUsageRecord add index index13(ServerDate)",
								"alter table Security add unique index index02(alias)",
								"alter table CPUInfo change column NodeName HostDescription varchar(255)",
								//
								// place older to initialize SystemProplist
								//
								"delete from SystemProplist",
								"insert into SystemProplist(car,cdr) values(" + dq + "gratia.version" + dq + comma + dq + gratiaDatabaseVersion + dq + ")",
								"insert into SystemProplist(car,cdr) values(" + dq + "gratia.database.version" + dq + comma + dq + gratiaDatabaseVersion + dq + ")",
								"delete from CPUMetricTypes",
								"insert into CPUMetricTypes(CPUMetricType) values(" + dq + "wallclock" + dq + ")",
								"insert into CPUMetricTypes(CPUMetricType) values(" + dq + "process" + dq + ")"

						};

				for (i = 0; i < commands1.length; i++)
						try
								{
										System.out.println("Executing: " + commands1[i]);
										statement = connection.createStatement();
										statement.executeUpdate(commands1[i]);
										System.out.println("Command: OK: " + commands1[i]);
								}
						catch (Exception e)
								{
										System.out.println("Command: Error: " + commands1[i] + " : " + e);
								}

		}

		public void contextDestroyed(ServletContextEvent sce)
		{
				Logging.info("");
				Logging.info("Context Destroy Event");
				Logging.info("");
				System.exit(0);
		}


		public class ReportSetup extends Thread
		{
				public ReportSetup()
				{
				}

				public void run()
				{
						try
								{
										Thread.sleep(30 * 1000);
								}
						catch (Exception ignore)
								{
								}

						//
						// create a dummy ReportingConfig.xml for birt
						//
				
						String dq = "\"";
						XP xp = new XP();
						StringBuffer xml = new StringBuffer();
						String catalinaHome = System.getProperty("catalina.home");
						catalinaHome = xp.replaceAll(catalinaHome,"\\","/");

						xml.append("<ReportingConfig>" + "\n");
						xml.append("<DataSourceConfig" + "\n");
						xml.append("url=" + dq + p.getProperty("service.mysql.url") + dq + "\n");
						xml.append("user=" + dq + p.getProperty("service.birt.user") + dq + "\n");
						xml.append("password=" + dq + p.getProperty("service.birt.password") + dq + "\n");
						xml.append("/>" + "\n");
						xml.append("<PathConfig" + "\n");
						xml.append("reportsFolder=" + dq + catalinaHome + 
											 "/webapps/" + p.getProperty("service.birt.reports.folder") + "/" + dq + "\n");
						xml.append("engineHome=" + dq + catalinaHome + 
											 "/webapps/" + p.getProperty("service.birt.engine.home") + "/" + dq + "\n");
						xml.append("webappHome=" + dq + catalinaHome + 
											 "/webapps/" + p.getProperty("service.birt.webapp.home") + "/" + dq + "\n");
						xml.append("/>" + "\n");
						xml.append("</ReportingConfig>" + "\n");
						xp.save(catalinaHome + "/webapps/gratia-report-configuration/ReportingConfig.xml",
										xml.toString());
						System.out.println("ReportConfig updated");
				}
		}

		public class HistoryMonitor extends Thread
		{
				public HistoryMonitor()
				{
				}

				public void run()
				{
						while(true)
								{
										try
												{
														Thread.sleep(6 * 60 * 60 * 1000);
														new HistoryReaper();
												}
										catch (Exception ignore)
												{
												}
								}
				}
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

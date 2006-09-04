package net.sf.gratia.services;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.Date;
import java.util.Properties;
import java.text.*;

import java.io.*;

import net.sf.gratia.storage.*;

import org.dom4j.*;
import org.dom4j.io.*;

import org.hibernate.*;
import org.hibernate.cfg.*;

import java.sql.*;

public class ListenerThread extends Thread
{
		String ident = null;
		String directory = null;

		//
		// database parameters
		//

		org.hibernate.cfg.Configuration hibernateConfiguration;
		SessionFactory factory;
		org.hibernate.Session session;
		Transaction tx;
		JobRecUpdaterManager updater = new JobRecUpdaterManager();
		int itotal = 0;
		boolean duplicateCheck = false;
		Properties p;

		XP xp = new XP();

		StatusUpdater statusUpdater = new StatusUpdater();
		NewProbeUpdate newProbeUpdate = new NewProbeUpdate();

		volatile boolean databaseDown = false;

		public ListenerThread(String ident,String directory,org.hibernate.cfg.Configuration hibernateConfiguration,SessionFactory factory)
		{
				this.ident = ident;
				this.directory = directory;
				this.hibernateConfiguration = hibernateConfiguration;
				this.factory = factory;
				loadProperties();
				try
						{
								String url = p.getProperty("service.jms.url");
								System.out.println("");
								System.out.println("ListenerThread: " + ident + ":" + directory + ": Started");
								System.out.println("");
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}

		public void loadProperties()
		{
				p = Configuration.getProperties();
				String temp = p.getProperty("service.duplicate.check");
				if (temp.equals("1"))
						duplicateCheck = true;
				else
						duplicateCheck = false;
				Logging.log("ListenerThread: " + ident + ":Duplicate Check: " + duplicateCheck);
		}

    public boolean gotDuplicate(JobUsageRecord record) throws Exception
    {
				boolean status = false;
    	
				if (duplicateCheck == false)
						return false;

				JobIdentity jobIdentity = record.getJobIdentity();
				DateElement startTimeElement = record.getStartTime();
				
				if ((jobIdentity == null) || (startTimeElement == null))
						{
								System.out.println("ListenerThread: " + ident + ":Invalid Record:" + "\n" + jobIdentity + "\n");
								return false;
						}
				if ((jobIdentity.getGlobalJobId() == null) || (startTimeElement.getValue() == null))
						{
								System.out.println("ListenerThread: " + ident + ":Invalid Record:" + "\n" + jobIdentity + "\n");
								return false;
						}

				String globalJobid = jobIdentity.getGlobalJobId();
				String localJobid = jobIdentity.getLocalJobId();
				String host = record.getHost().getValue();

				Date startTime = startTimeElement.getValue();
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					
				String sql = "SELECT dbid " + 
						"FROM JobUsageRecord " +
						"WHERE GlobalJobId = '" + globalJobid + "' " +
						" AND StartTime = '" + format.format(startTime) + "'" +
						" AND LocalJobid = '" + localJobid + "'" +
						" AND Host = '" + host + "'";
	    	
				org.hibernate.Session session2 = factory.openSession();
				
				try
						{
								if(session2.createSQLQuery(sql).list().size() > 0)
										status = true;
						}
				catch (Exception e)
						{
								// System.out.println("ListenerThread: " + ident + ":Error During Dup Check");
								throw e;
						}
				finally
						{
								try
										{
												session2.close();
										}
								catch (Exception ignore)
										{
										}
						}

				return status;
    }

    public boolean communicationsError(Exception exception)
    {
				String source = exception.toString();
				if (source.length() > 255)
						source = source.substring(0,255);

				System.out.println("ListenerThread: " + ident + ":Communications Error Test:" + source);

				if (source.indexOf("hibernate") > 0)
						return true;
				if (source.indexOf("StatusUpdater") > 0)
						return true;

				try
						{
								Thread.sleep(30 * 1000);
						}
				catch (Exception ignore)
						{
						}
				try
						{
								String driver = p.getProperty("service.mysql.driver");
								String url = p.getProperty("service.mysql.url");
								String user = p.getProperty("service.mysql.user");
								String password = p.getProperty("service.mysql.password");
								Class.forName(driver);
								java.sql.Connection connection = null;
								connection = DriverManager.getConnection(url,user,password);
								connection.close();
								System.out.println("ListenerThread: " + ident + ":No Communications Error");
								return false;
						}
				catch (Exception e)
						{
								System.out.println("ListenerThread: " + ident + ":Detected Communications Error");
								return true;
						}
		}

		public void run()
		{
				while (true)
						{
								if (databaseDown)
										{
												restartDatabase();
										}
								loop();
								try
										{
												Thread.sleep(30 * 1000);
										}
								catch (Exception ignore)
										{
										}
						}
		}
				
		public void restartDatabase()
		{
				try
						{
								factory = hibernateConfiguration.buildSessionFactory();
								session = factory.openSession();
								databaseDown = false;
								statusUpdater = new StatusUpdater();
								newProbeUpdate = new NewProbeUpdate();
								System.out.println("ListenerThread: " + ident + ":Restarting");
						}
				catch (Exception ignore)
						{
						}
				try
						{
								Thread.sleep(30 * 1000);
						}
				catch (Exception ignore)
						{
						}
		}

		public void shutdown()
		{
				try
						{
								try
										{
												session.close();
										}
								catch (Exception ignore1)
										{
										}
								factory.close();
								databaseDown = true;
								System.out.println("ListenerThread: " + ident + ":Shutting Down");
						}
				catch (Exception ignore2)
						{
						}
				try
						{
								Thread.sleep(30 * 1000);
						}
				catch (Exception ignore)
						{
						}
		}

		public void loop()
		{
				if (databaseDown)
						return;

				String files[] = xp.getFileList(directory);
				
				if (files.length == 0)
						return;

				for (int i = 0; i < files.length; i++)
						{
								String file = files[i];
								String xml = xp.get(files[i]);
								JobUsageRecord current = null;

								System.out.println("ListenerThread: " + ident + ":Processing: " + file);

								try
										{
												
												ArrayList records = convert(xml);
												
												for(int j = 0; j < records.size(); j++)
														{
																// System.out.println("ListenerThread: " + ident + ":Before Begin Transaction");
																session = factory.openSession();
																tx = session.beginTransaction();
																// System.out.println("ListenerThread: " + ident + ":After Begin Transaction");

																current = (JobUsageRecord) records.get(j);
																statusUpdater.update(current,xml);

																// System.out.println("ListenerThread: " + ident + ":Before Duplicate Check");
																boolean gotdup = gotDuplicate(current);
																// System.out.println("ListenerThread: " + ident + ":After Duplicate Check");

																if (gotdup)
																		{
																				// System.out.println("ListenerThread: " + ident + ":Before Save Duplicate");
																				saveDuplicate(current);
																				// System.out.println("ListenerThread: " + ident + ":After Save Duplicate");
																		}
																else
																		{
																				// System.out.println("ListenerThread: " + ident + ":Before New Probe Update");
																				newProbeUpdate.check(current);
																				// System.out.println("ListenerThread: " + ident + ":After New Probe Update");
																				updater.Update(current);
																				if (xml != null)
																						current.setRawXml(xml);
																				// System.out.println("ListenerThread: " + ident + ":Before Hibernate Save");
																				session.save(current);
																				// System.out.println("ListenerThread: " + ident + ":After Hibernate Save");
																		}
																// System.out.println("ListenerThread: " + ident + ":Before Transaction Commit");
																tx.commit();
																session.close();
														}
										}
								catch (Exception exception)
										{
												if (communicationsError(exception))
														{
																shutdown();
																return;
														}
												System.out.println("");
												System.out.println("ListenerThread: " + ident + ":Error In Process: " + exception);
												System.out.println("ListenerThread: " + ident + ":Current: " + current);
										}
								// System.out.println("ListenerThread: " + ident + ":After Transaction Commit");
								try
										{
												File temp = new File(file);
												temp.delete();
										}
								catch (Exception ignore)
										{
										}
								itotal++;
								System.out.println("ListenerThread: " + ident + ":Total Records: " + itotal);
						}
		}


		public ArrayList convert(String xml) throws Exception 
		{
				ArrayList usageRecords = new ArrayList();    	
				SAXReader saxReader = new SAXReader();        
				Document doc = null;
				Element eroot = null;

				// Read the XML into a document for parsing

				try
						{
								doc = saxReader.read(new StringReader(xml));  
						}
				catch (Exception e)
						{
								Logging.warning(xp.parseException(e));
								Logging.warning("XML:" + "\n\n" + xml + "\n\n");
						}
				try 
						{
								eroot = doc.getRootElement();

								JobUsageRecord job = null;
								UsageRecordLoader load = new UsageRecordLoader();

								if (eroot.getName()=="JobUsageRecord"
										|| eroot.getName()=="UsageRecord"
										|| eroot.getName()=="Usage"
										|| eroot.getName()=="UsageRecordType") 
										{
												// The current element is a job usage record node.  Use it to populate a JobUsageRecord object            	
												job = load.ReadUsageRecord(eroot);

												// Add this populated job usage record to the usage records array list
												usageRecords.add(job);                
										} 
								else if (eroot.getName()!="UsageRecords") 
										{            	
												// Unexpected root element
												throw new Exception("In the xml usage record, the expected root nodes are " + 
																						"JobUsageRecords, JobUsageRecord, Usage, UsageRecord " + 
																						"and UsageRecordType.\nHowever we got "+eroot.getName());
										} 
								else 
										{
												// This is a usage records node
												// which should contain one to many job usage record nodes so start a loop through its children
												for (Iterator i = eroot.elementIterator(); i.hasNext(); ) 
														{
																Element element = (Element) i.next();
																if (element.getName() == "JobUsageRecord") 
																		{
																				//The current element is a job usage record node.  Use it to populate a JobUsageRecord object
																				job = load.ReadUsageRecord(element);
																				usageRecords.add(job);
																		} 
																else 
																		{
																				// Unexpected element
																				throw new Exception("Unexpected element: "+element.getName()
																														+"\n"+element);
																		}
														}
										}
						} 
				catch(Exception e) 
						{
								Utils.GratiaDebug("Parse error:  " + e.getMessage());
								throw new Exception("loadURXmlFile saw an error at 2:"+ e);        	
						}
				finally
						{
								// Cleanup object instantiations
								saxReader = null;
								doc = null;   
								eroot = null;
						}

				// The usage records array list is now populated with all the job usage records found in the given XML file
				//  return it to the caller.
				return usageRecords;
		}   

		public void saveDuplicate(JobUsageRecord current)
		{
				DupRecord record = new DupRecord();

				record.seteventdate(new java.util.Date());
				record.setrawxml(current.asXML());
				try
						{
								org.hibernate.Session session2 = factory.openSession();
								Transaction tx2 = session2.beginTransaction();
								session2.save(record);
								tx2.commit();
								session2.close();
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}

}

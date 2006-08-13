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
import java.text.*;

import java.io.*;

import net.sf.gratia.storage.*;

import org.dom4j.*;
import org.dom4j.io.*;

import org.hibernate.*;
import org.hibernate.cfg.*;

public class ListenerThread extends Thread
{
		String ident = null;
		
		//
		// jms things
		//

		QueueConnectionFactory qcf;
		Queue q;

		//
		// database parameters
		//

		org.hibernate.cfg.Configuration cfg;
		SessionFactory factory;
		Session session;
		Transaction tx;
		JobRecUpdaterManager updater = new JobRecUpdaterManager();
		int itotal = 0;
		boolean duplicateCheck = false;
		Properties p;

		XP xp = new XP();

		StatusUpdater statusUpdater = new StatusUpdater();

		public ListenerThread(String ident,SessionFactory factory,QueueConnectionFactory qcf,Queue q)
		{
				this.ident = ident;
				this.factory = factory;
				this.qcf = qcf;
				this.q = q;
				loadProperties();
		}

		public void loadProperties()
		{
				p = Configuration.getProperties();
				String temp = p.getProperty("service.duplicate.check");
				if (temp.equals("1"))
						duplicateCheck = true;
				else
						duplicateCheck = false;
				Logging.log("CollectorMsgListener: Duplicate Check: " + duplicateCheck);
		}

    public boolean gotDuplicate(JobIdentity jobIdentity, DateElement startTimeElement, Session session)
    {
				boolean status = false;
    	
				if (duplicateCheck == false)
						return false;

				Logging.log("jobIdentity; " + jobIdentity);
				Logging.log("startTimeElement: " + startTimeElement);
				if ((jobIdentity == null) || (startTimeElement == null))
						{
								Logging.log("Invalid Record:" + "\n" + jobIdentity + "\n");
								return false;
						}
				Logging.log("globalJobId: " + jobIdentity.getGlobalJobId());
				Logging.log("startTimElement.value: " + startTimeElement.getValue());
				if ((jobIdentity.getGlobalJobId() == null) || (startTimeElement.getValue() == null))
						{
								Logging.log("Invalid Record: " + "\n" + jobIdentity + "\n");
								return false;
						}

				String globalJobId = jobIdentity.getGlobalJobId();
				Date startTime = startTimeElement.getValue();
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					
				String sql = "SELECT dbid " + 
						"FROM JobUsageRecord " +
						"WHERE GlobalJobId = '" + globalJobId + "' " +
						"AND StartTime = '" + format.format(startTime) + "'";
	    	
				try
						{
								if(session.createSQLQuery(sql).list().size() > 0)
										status = true;
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
				return status;
    }

		public void run()
		{
				String rawxml = null;
				String extraxml = null;
				QueueConnection qc = null;
				QueueSession qs = null;
				QueueReceiver qrec = null;
				Message msg;
				NewProbeUpdate newProbeUpdate = new NewProbeUpdate();

				Logging.info("ListenerThread: Starting JMS: " + ident);
				try
						{
								qc = qcf.createQueueConnection();
								qs = qc.createQueueSession(false,javax.jms.Session.AUTO_ACKNOWLEDGE);
								qrec = qs.createReceiver(q);
								qc.start();
								Logging.info("ListenerThread: JMS Started: " + ident);
						}
				catch (Exception e)
						{
								Logging.warning("ListenerThread: Error Starting JMS: " + e);
								Logging.warning(xp.parseException(e));
								Logging.warning("ListenerThread: Exiting");
								return;
						}

				Logging.info("ListenerThread: Running: " + ident);

				while(true)
						{
								try 
										{
												try
														{
																msg = qrec.receive();
														}
												catch (Exception shutdown)
														{
																Logging.info("ListenerThread: Exiting: " + ident);
																return;
														}
												Logging.info("ListenerThread: Received Message: " + ident);
												session = factory.openSession();	
												tx = session.beginTransaction();
												if (msg instanceof TextMessage) 
														{
																String xml = ((TextMessage) msg).getText();
																rawxml = msg.getStringProperty("rawxml");
																extraxml = msg.getStringProperty("extraxml");
																String dbid = msg.getStringProperty("dbid");

																// Logging.debug("ListenerThread: dbid: " + "\n" + dbid + "\n");
																// Logging.debug("ListenerThread: xml: " + "\n" + xml + "\n");
																// Logging.debug("ListenerThread: rawxml: " + "\n" + rawxml + "\n");

																ArrayList records = convert(xml);
												
																for(int i=0; i < records.size(); i++)
																		{
																				JobUsageRecord current = (JobUsageRecord) records.get(i);
																				statusUpdater.update(current,rawxml);
																				if (gotDuplicate(current.getJobIdentity(), current.getStartTime(), session) == true)
																						{
																								Logging.warning("Duplicate: " + current);
																								saveDuplicate(current,extraxml);
																						}
																				else
																						{
																								newProbeUpdate.check(current);
																								updater.Update(current);
																								if (rawxml != null)
																										current.setRawXml(rawxml);
																								if (extraxml != null)
																										current.setExtraXml(extraxml);
																								try
																										{
																												session.save(current);
																										}
																								catch (Exception duplicate)
																										{
																												saveDuplicate(current,extraxml);
																										}
																								itotal++;
																						}
																		}
																Logging.log("Total Records: " + itotal);
														}
										}
								catch (Exception e) 
										{
												e.printStackTrace();
										}
								finally
										{
												try
														{
																tx.commit();
														}
												catch (Exception ignore)
														{
														}
												try
														{
																session.close();
														}
												catch (Exception ignore)
														{
														}
										}
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

		public void saveDuplicate(JobUsageRecord current,String extraxml)
		{
				DupRecord record = new DupRecord();

				record.seteventdate(new java.util.Date());
				record.setrawxml(current.asXML());
				record.setextraxml(extraxml);
				try
						{
								session.save(record);
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}

}

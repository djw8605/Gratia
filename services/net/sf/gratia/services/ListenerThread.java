package net.sf.gratia.services;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.List;

import java.text.*;

import java.io.*;
import java.net.*;

import net.sf.gratia.storage.*;

import org.dom4j.*;
import org.dom4j.io.*;

import org.hibernate.*;

import java.sql.*;

import java.security.*;

public class ListenerThread extends Thread
{
		String ident = null;
		String directory = null;

		//
		// database parameters
		//

		org.hibernate.Session session;
		Transaction tx;
		JobRecUpdaterManager updater = new JobRecUpdaterManager();
		int itotal = 0;
		boolean duplicateCheck = false;
		Properties p;

		XP xp = new XP();

		StatusUpdater statusUpdater = new StatusUpdater();
		NewProbeUpdate newProbeUpdate = new NewProbeUpdate();

		Object lock;

		int dupdbid = 0;

		String historypath = "";

		public ListenerThread(String ident,
													String directory,
													Object lock)
		{
				this.ident = ident;
				this.directory = directory;
				this.lock = lock;
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
				historypath = System.getProperties().getProperty("catalina.home") + "/gratia/data/";
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

		public String md5key(String input) throws Exception
		{
				MessageDigest md = MessageDigest.getInstance("MD5");
				md.update(input.getBytes());
				return HexString.bufferToHex(md.digest());
		}

    public boolean gotDuplicate(JobUsageRecord record) throws Exception
    {
				boolean status = false;
				String dq = "'";

				if (duplicateCheck == false)
						return false;

				RecordIdentity temp = record.getRecordIdentity();
				record.setRecordIdentity(null);
				String md5key = md5key(record.asXML());
				record.setmd5(md5key);
				record.setRecordIdentity(temp);

				String sql = "SELECT dbid from JobUsageRecord where md5 = " + dq + md5key + dq;
	    	
				org.hibernate.Session session2 = HibernateWrapper.getSession();
				dupdbid = 0;

				try
						{
								List list = session2.createSQLQuery(sql).list();
								if (list.size() > 0)
										{
												status = true;
												Integer value = (Integer) list.get(0);
												dupdbid = value.intValue();
										}
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


		public void run()
		{
				while (true)
						{
								if (! HibernateWrapper.databaseUp())
										{
												HibernateWrapper.start();
												if (HibernateWrapper.databaseDown)
														{
																System.out.println("ListenerThread: " + ident + ":Hibernate Down: Sleeping");
																try
																		{
																				Thread.sleep(30 * 1000);
																		}
																catch (Exception ignore)
																		{
																		}
																continue;
														}
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
				
		public void loop()
		{
				if (! HibernateWrapper.databaseUp())
						return;

				String files[] = xp.getFileList(directory);
				
				if (files.length == 0)
						return;

				for (int i = 0; i < files.length; i++)
						{
								String file = files[i];
								String blob = xp.get(files[i]);
								String xml = null;
								String rawxml = null;
								String extraxml = null;
								JobUsageRecord current = null;

								//
								// see if we got a normal update or a replicated one
								//
								
								boolean gotreplication = false;
								boolean gothistory = false;
								String historydate = null;

								try
										{
												if (blob.startsWith("replication"))
														{
																StringTokenizer st = new StringTokenizer(blob,"|");
																if (st.hasMoreTokens())
																		st.nextToken();
																if (st.hasMoreTokens())
																		xml = st.nextToken();
																if (st.hasMoreTokens())
																		rawxml = st.nextToken();
																if (st.hasMoreTokens())
																		extraxml = st.nextToken();
																gotreplication = true;
														}
												else if (blob.startsWith("history"))
														{
																StringTokenizer st = new StringTokenizer(blob,"|");
																if (st.hasMoreTokens())
																		st.nextToken();
																if (st.hasMoreTokens())
																		historydate = st.nextToken();
																if (st.hasMoreTokens())
																		xml = st.nextToken();
																if (st.hasMoreTokens())
																		rawxml = st.nextToken();
																if (st.hasMoreTokens())
																		extraxml = st.nextToken();
																gothistory = true;
														}
												else
														xml = blob;
										}
								catch (Exception e)
										{
												System.out.println("ListenerThread: " + ident + ":Error:Processing File: " + file);
												System.out.println("ListenerThread: " + ident + ":Blob: " + blob);
												try
														{
																File temp = new File(file);
																temp.delete();
														}
												catch (Exception ignore)
														{
														}
												continue;
										}

								if (xml == null)
										{
												System.out.println("ListenerThread: " + ident + ":Error:No Data Processing: " + file);
												try
														{
																File temp = new File(file);
																temp.delete();
														}
												catch (Exception ignore)
														{
														}
												continue;
										}

								System.out.println("ListenerThread: " + ident + ":Processing: " + file);

								try
										{
												ArrayList records = new ArrayList();
												
												try
														{
																records = convert(xml);
														}
												catch (Exception e)
														{
																if (gotreplication)
																		saveParse("Replication","Parse",xml);
																else if (gothistory)
																		saveParse("History","Parse",xml);
																else
																		saveParse("Probe","Parse",xml);
														}
												
												for(int j = 0; j < records.size(); j++)
														{
																// System.out.println("ListenerThread: " + ident + ":Before Begin Transaction");
																session = HibernateWrapper.getSession();
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
																				if (gotreplication)
																						saveDuplicate("Replication","Duplicate",dupdbid,current);
																				else if (gothistory)
																						;
																				else
																						saveDuplicate("Probe","Duplicate",dupdbid,current);
																				// System.out.println("ListenerThread: " + ident + ":After Save Duplicate");
																		}
																else
																		{
																				// System.out.println("ListenerThread: " + ident + ":Before New Probe Update");
																				synchronized(lock)
																						{
																								newProbeUpdate.check(current);
																						}
																				// System.out.println("ListenerThread: " + ident + ":After New Probe Update");
																				updater.Update(current);
																				if (rawxml != null)
																						current.setRawXml(rawxml);
																				if (extraxml != null)
																						current.setExtraXml(extraxml);
																				// System.out.println("ListenerThread: " + ident + ":Before Hibernate Save");
																				try
																						{
																								if (gothistory)
																										{
																												Date serverDate = new Date(Long.parseLong(historydate));
																												current.setServerDate(serverDate);
																										}
																								session.save(current);
																								//
																								// now - save history
																								//
																								if (! gothistory)
																										{
																												Date serverDate = current.getServerDate();
																												synchronized(lock)
																														{
																																Date now = new Date();
																																SimpleDateFormat format = new SimpleDateFormat("yyyyMMddkk");
																																String path = historypath + "history" + format.format(now);
																																File directory = new File(path);
																																if (! directory.exists())
																																		{
																																				directory.mkdir();
																																		}
																																File historyfile = File.createTempFile("history","xml",new File(path));
																																String filename = historyfile.getPath();
																																if (gotreplication && (extraxml != null))
																																		xp.save(filename,"history" + "|" + serverDate.getTime() + 
																																						"|" + xml + "|" + rawxml + "|" + extraxml);
																																else if (gotreplication)
																																		xp.save(filename,"history" + "|" + serverDate.getTime() + "|" + xml + "|" + rawxml);
																																else
																																		xp.save(filename,"history" + "|" + serverDate.getTime() + "|" + xml);
																														}
																										}
																						}
																				catch (Exception e)
																						{
																								if (HibernateWrapper.databaseUp())
																										{
																												if (gotreplication)
																														saveSQL("Replication","SQLError",current);
																												else
																														saveSQL("Probe","SQLError",current);
																										}
																								else
																										throw e;
																						}

																				// System.out.println("ListenerThread: " + ident + ":After Hibernate Save");
																		}
																// System.out.println("ListenerThread: " + ident + ":Before Transaction Commit");
																tx.commit();
																session.close();
																// System.out.println("ListenerThread: " + ident + ":After Transaction Commit");
														}
										}
								catch (Exception exception)
										{
												if (! HibernateWrapper.databaseUp())
														{
																System.out.println("ListenerThread: " + ident + ":Communications Error:Shutting Down");
																return;
														}
												System.out.println("");
												System.out.println("ListenerThread: " + ident + ":Error In Process: " + exception);
												System.out.println("ListenerThread: " + ident + ":Current: " + current);
										}
								// System.out.println("ListenerThread: " + ident + ":Before File Delete: " + file);
								try
										{
												File temp = new File(file);
												temp.delete();
										}
								catch (Exception ignore)
										{
												// System.out.println("ListenerThread: " + ident + ":File Delete Failed: " + file + " Error: " + ignore);
										}
								// System.out.println("ListenerThread: " + ident + ":After File Delete: " + file);
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
								throw new Exception();
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
								System.out.println("ListenerThread: " + ident + ":Parse error:  " + e.getMessage());
								System.out.println("ListenerThread: " + ident + ":XML:  " + "\n" + xml);
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

		public void saveDuplicate(String source,String error,int dupdbid,JobUsageRecord current)
		{
				DupRecord record = new DupRecord();

				record.seteventdate(new java.util.Date());
				record.setrawxml(current.asXML());
				record.setsource(source);
				record.seterror(error);
				record.setdbid(dupdbid);

				try
						{
								org.hibernate.Session session2 = HibernateWrapper.getSession();
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

		public void saveParse(String source,String error,String xml)
		{
				DupRecord record = new DupRecord();

				record.seteventdate(new java.util.Date());
				record.setrawxml(xml);
				record.setsource(source);
				record.seterror(error);

				try
						{
								org.hibernate.Session session2 = HibernateWrapper.getSession();
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

		public void saveSQL(String source,String error,JobUsageRecord current)
		{
				DupRecord record = new DupRecord();

				record.seteventdate(new java.util.Date());
				record.setrawxml(current.asXML());
				record.setsource(source);
				record.seterror(error);

				try
						{
								org.hibernate.Session session2 = HibernateWrapper.getSession();
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

package net.sf.gratia.services;

import net.sf.gratia.storage.*;

import java.util.*;
import java.sql.*;
import java.text.*;

import org.hibernate.*;

public class ProbeMonitorThread extends Thread
{
		//
		// database parameters
		//

		Properties p;
		XP xp = new XP();

		SessionFactory factory;
		Session session;
		Transaction tx;

		Hashtable jobtable = new Hashtable();

		public ProbeMonitorThread(SessionFactory factory)
		{
				this.factory = factory;
				loadProperties();
				deleteTable();
				loadTable();
		}

		public void loadProperties()
		{
				p = Configuration.getCEProbeProperties();
		}

    public void deleteTable()
    {
				Properties p = Configuration.getProperties();

				try
						{
								String driver = p.getProperty("service.mysql.driver");
								String url = p.getProperty("service.mysql.url");
								String user = p.getProperty("service.mysql.user");
								String password = p.getProperty("service.mysql.password");
								Class.forName(driver);
								Connection connection = DriverManager.getConnection(url,user,password);
								String sql = "delete from ceprobes";
								Statement statement = connection.createStatement();
								statement.execute(sql);
								statement.close();
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
    }

		public void loadTable()
		{
				int i = 0;
				CEProbes record;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

				for (i = 0; i < 500; i++)
						{
								String probename = p.getProperty("probe.probename." + i);
								if (probename == null)
										continue;
								int probeid = Integer.parseInt(p.getProperty("probe.probeid." + i));
								int facilityid = Integer.parseInt(p.getProperty("probe.facilityid." + i));
								int active = Integer.parseInt(p.getProperty("probe.active." + i));
								int reporthh = Integer.parseInt(p.getProperty("probe.reporthh." + i));
								int reportmm = Integer.parseInt(p.getProperty("probe.reportmm." + i));
								int jobs = 0;
								if (p.getProperty("probe.jobs." + i) != null)
										jobs = Integer.parseInt(p.getProperty("probe.jobs." + i));
								record = new CEProbes();
								record.setprobename(probename);
								record.setprobeid(probeid);
								record.set_facility_id(facilityid);
								record.setactive(active);
								record.setreporthh(reporthh);
								record.setreportmm(reportmm);
								record.setstatus("");
								record.setjobs(jobs);
								DateElement current = new DateElement();
								java.util.Date time = new java.util.Date();
								current.setValue(time);
								record.setcurrenttime(current);
								
								jobtable.put(probename,new Integer(jobs));

								try
										{
												session = factory.openSession();
												tx = session.beginTransaction();
												session.save(record);
												tx.commit();
												session.close();
												Logging.info("ProbeMonitorThread: Added: " + record);
										}
								catch (Exception e)
										{
												Logging.warning("ProbeMonitorThread: Error Adding: " + record);
												Logging.warning(xp.parseException(e));
										}
						}
		}

		public void run()
		{
				Properties p = Configuration.getProperties();

				Logging.info("ProbeMonitorThread: Starting");
				long wait = Long.parseLong(p.getProperty("service.probe.monitor.wait"));
				wait = wait * 1000 * 60;
				while(true)
						{
								try
										{
												sleep(wait);
										}
								catch (Exception ignore)
										{
										}
								Logging.info("ProbeMonitorThread: Scanning: " + new java.util.Date());
								scan();
						}
		}

		public void scan()
		{
				Properties p = Configuration.getProperties();

				String driver = p.getProperty("service.mysql.driver");
				String url = p.getProperty("service.mysql.url");
				String user = p.getProperty("service.mysql.user");
				String password = p.getProperty("service.mysql.password");
	
				Connection connection;
				Statement statement;
				ResultSet resultSet;

				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String dq = "\"";
				String comma = ",";

				int irecords = 0;
				StringBuffer buffer = new StringBuffer();

				try
						{
								Class.forName(driver);
								connection = DriverManager.getConnection(url,user,password);
						}
				catch (Exception e)
						{
								Logging.warning("ProbeMonitorThread: Error During Scan: " + e);
								Logging.warning(xp.parseException(e));
								return;
						}

				String command = "select * from ceprobes";

				try
						{
								statement = connection.prepareStatement(command);
								resultSet  = statement.executeQuery(command);
								while(resultSet.next())
										{
												String probename = resultSet.getString("probename");
												int reporthh = resultSet.getInt("reporthh");
												int reportmm = resultSet.getInt("reportmm");
												int jobs = resultSet.getInt("jobs");
												String status = resultSet.getString("status");
												long current = resultSet.getTimestamp("currenttime").getTime();

												int active = resultSet.getInt("active");

												buffer.append("probe.probeid." + irecords + "="  + resultSet.getInt("probeid") + "\n");
												buffer.append("probe.facilityid." + irecords + "=" + resultSet.getInt("facility_id") + "\n");
												buffer.append("probe.probename." + irecords + "=" + probename + "\n");
												buffer.append("probe.active." + irecords + "=" + resultSet.getInt("active") + "\n");
												buffer.append("probe.reporthh." + irecords + "=" + resultSet.getInt("reporthh") + "\n");
												buffer.append("probe.reportmm." + irecords + "=" + resultSet.getInt("reportmm") + "\n");
												buffer.append("probe.jobs." + irecords + "=" + resultSet.getInt("jobs") + "\n");
												buffer.append("\n");

												irecords++;

												if (active == 0)
														continue;

												long monitor = reporthh * 24 * 60 * 1000;
												monitor = monitor + (reportmm * 60 * 1000);

												java.util.Date now1 = new java.util.Date();
												long now2 = now1.getTime();

												//
												// here we go - if there is already a record out there for this probe don't update
												//
												
												java.util.Date previous = new java.util.Date(now2 - monitor);

												String command2 =
														"select count(*) from ceprobestatus where" +
														" probename = " + dq + probename + dq +
														" and currenttime >= " + "timestamp(" + dq + format.format(previous) + dq + ")";

												Statement statement2 = connection.prepareStatement(command2);
												ResultSet resultSet2 = statement2.executeQuery(command2);
												int count = 0;
												while(resultSet2.next())
														count = resultSet2.getInt(1);
												resultSet2.close();
												statement2.close();
												/*
												Logging.info("Command: " + command2 + "\n" +
																		 "Count: " + count + "\n" +
																		 "Previous: " + format.format(previous) + "\n" +
																		 "Current: " + format.format(new java.util.Date(current)));
												Logging.info("State: " + ((current + monitor) < now2));
												*/
												if (count > 0)
														continue;

												//
												// if (current + monitor) < now then we have a dead probe
												//

												if ((current + monitor) < now2)
														{
																
																command2 =
																		"insert into ceprobestatus (currenttime,probename,probestatus,jobs,lostjobs) values(" +
																		"timestamp(" + dq + format.format(now1) + dq + ")" + comma +
																		dq + probename + dq + comma +
																		dq + "dead" + dq + comma +
																		"0" + comma + "0" + ")";
																statement2 = connection.prepareStatement(command2);
																statement2.execute(command2);
																statement2.close();
														}
												//
												// else a normal status
												//
												else
														{
																Integer previousJobs = (Integer) jobtable.get(probename);
																int temp = previousJobs.intValue();
																int jobdelta = jobs - temp;
																jobtable.put(probename,new Integer(jobs));

																command2 =
																		"insert into ceprobestatus (currenttime,probename,probestatus,jobs,lostjobs) values(" +
																		"timestamp(" + dq + format.format(now1) + dq + ")" + comma +
																		dq + probename + dq + comma +
																		dq + "alive" + dq + comma +
																		jobdelta + comma + "0" + ")";
																statement2 = connection.prepareStatement(command2);
																statement2.execute(command2);
																statement2.close();
														}
										}
								resultSet.close();
								statement.close();
								connection.close();
						}
				catch (Exception e)
						{
								Logging.warning("ProbeMonitorThread: Error During Scan");
								Logging.warning(xp.parseException(e));
						}
				Configuration.saveCEProbeProperties(buffer.toString());
		}

}

package net.sf.gratia.services;

import java.util.Properties;
import java.sql.*;
import net.sf.gratia.storage.*;

public class NewProbeUpdate
{
		public Properties p;

		Connection connection = null;
		Statement statement = null;
		ResultSet resultSet = null;
		XP xp = new XP();

		public NewProbeUpdate()
		{
				p = Configuration.getProperties();

				String driver = p.getProperty("service.mysql.driver");
				String url = p.getProperty("service.mysql.url");
				String user = p.getProperty("service.mysql.user");
				String password = p.getProperty("service.mysql.password");

				try
						{
								Class.forName(driver);
								connection = (java.sql.Connection) DriverManager.getConnection(url,user,password);
						}
				catch (Exception e)
						{
								Logging.warning("NewProbeUpdate: Error During init: " + e);
								Logging.warning(xp.parseException(e));
								return;
						}
		}

		public void check(JobUsageRecord record)
		{
				StringElement site = record.getSiteName();
				StringElement probe = record.getProbeName();
				String sitename = "Unknown";
				String probename = probe.getValue();
				String dq = "\"";
				String comma = ",";
				int icount = 0;
				int facilityid = -1;

				if (site != null)
						sitename = site.getValue();
				//
				// see if the probe exists
				//
				String command = "select count(*) from CEProbes where probename = " + dq + probename + dq;
				try
						{
								statement = connection.prepareStatement(command);
								resultSet  = statement.executeQuery(command);
								while(resultSet.next())
										{
												icount = resultSet.getInt(1);
										}
								resultSet.close();
								statement.close();
								//
								// already there - just exit
								//
								if (icount > 0)
										return;
								//
								// otherwise get facilityid for sitename
								//
								command = "select facility_id from CETable where facility_name = " + dq + sitename + dq;
								statement = connection.prepareStatement(command);
								resultSet  = statement.executeQuery(command);
								while(resultSet.next())
										{
												facilityid = resultSet.getInt(1);
										}
								resultSet.close();
								statement.close();
								//
								// if facilityid == -1 it doesn't exist - add it to cetable
								//
								if (facilityid == -1)
										{
												command = "insert into CETable(facility_name) values(" + dq + sitename + dq + ")";
												statement = connection.createStatement();
												statement.executeUpdate(command);
												statement.close();
												command = "select facility_id from CETable where facility_name = " + dq + sitename + dq;
												statement = connection.prepareStatement(command);
												resultSet  = statement.executeQuery(command);
												while(resultSet.next())
														{
																facilityid = resultSet.getInt(1);
														}
												resultSet.close();
												statement.close();
										}
								//
								// now add a new entry to ceprobes with default values
								//
								command = 
										"insert into CEProbes (facility_id,probename,active,reporthh,reportmm) values(" + 
										facilityid + comma + dq + probename + dq + comma + "1" + comma + "24" + comma + "00" + ")";
								statement = connection.createStatement();
								statement.executeUpdate(command);
								statement.close();
						}
				catch (Exception e)
						{
								Logging.warning("NewProbeUpdate: Error During init: " + e);
								Logging.warning(xp.parseException(e));
						}

		}
}

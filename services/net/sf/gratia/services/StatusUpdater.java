package net.sf.gratia.services;

import net.sf.gratia.storage.*;

import java.util.*;
import java.sql.*;
import java.text.*;

public class StatusUpdater
{
		Properties p;
		XP xp = new XP();
		Connection connection;
		Statement statement;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		public StatusUpdater()
		{
				p = Configuration.getProperties();

				try
						{
								String driver = p.getProperty("service.mysql.driver");
								String url = p.getProperty("service.mysql.url");
								String user = p.getProperty("service.mysql.user");
								String password = p.getProperty("service.mysql.password");
								Class.forName(driver);
								connection = DriverManager.getConnection(url,user,password);
						}
				catch (Exception e)
						{
								Logging.warning("StatusUpdater: Error During Init");
								Logging.warning(xp.parseException(e));
						}
		}

		public void update(JobUsageRecord record,String rawxml)
		{
				String probeName = record.getProbeName().getValue();
				String dq = "\"";
				String comma = ",";

				String command = "update CEProbes set" +
						" currenttime = " + dq + format.format(new java.util.Date()) + dq + comma +
						" status = " + dq + "alive" + dq + comma +
						" jobs = jobs + 1" +
						" where probename = " + dq + probeName + dq;

				try
						{
								statement = connection.createStatement();
								statement.execute(command);
								statement.close();
						}
				catch (Exception e)
						{
								Logging.warning("StatusUpdater: Error During Update");
								Logging.warning(xp.parseException(e));
								Logging.warning("StatusUpdater: xml: " + "\n" + rawxml + "\n");
						}
		}
}

package net.sf.gratia.services;

import net.sf.gratia.storage.*;

import java.util.*;
import java.sql.*;
import java.text.*;

public class ProbeStatusUpdate
{
		Properties p;
		XP xp = new XP();
		Connection connection;
		Statement statement;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

		String driver = null;
		String url = null;
		String user = null;
		String password = null;

		public ProbeStatusUpdate()
		{
				p = Configuration.getProperties();

		}

		public void openConnection()
		{
				try
						{
								driver = p.getProperty("service.mysql.driver");
								url = p.getProperty("service.mysql.url");
								user = p.getProperty("service.mysql.user");
								password = p.getProperty("service.mysql.password");
								Class.forName(driver);
								connection = null;
								connection = DriverManager.getConnection(url,user,password);
						}
				catch (Exception e)
						{
								Logging.log("StatusUpdater: Error During Init: No Connection");
						}
		}

		public void closeConnection()
		{
				try
						{
								connection.close();
						}
				catch (Exception ignore)
						{
						}
		}
		

		public void update(String tokenString)
		{
				String dq = "\"";
				String comma = ",";

				openConnection();
				if (connection == null)
						return;
				
				Hashtable table = new Hashtable();
				StringTokenizer st1 = new StringTokenizer(tokenString,"|");
				while (st1.hasMoreTokens())
						{
								String keyvalue = st1.nextToken();
								StringTokenizer st2 = new StringTokenizer(keyvalue,"=");
								String key = st2.nextToken();
								String value = st2.nextToken();
								table.put(key,value);
						}
				String probename = (String) table.get("probename");
				for (Enumeration x = table.keys();x.hasMoreElements();)
						{
								String key = (String) x.nextElement();
								String value = (String) table.get(key);
								
								String command =
										"insert into ProbeStatus(ServerDate,ProbeName,KeyValue,ValueValue) " +
										" values (" +
										dq + format.format(new java.util.Date()) + dq + comma +
										dq + probename + dq + comma +
										dq + key + dq + comma +
										dq + value + dq + ")";

								try
										{
												statement = connection.createStatement();
												statement.execute(command);
												statement.close();
										}
								catch (Exception e)
										{
												Logging.log("ProbeStatusUpdate: Error: " + e);
												Logging.log("ProbeStatusUpdate: Command: " + command);
										}
						}
				closeConnection();
		}
}

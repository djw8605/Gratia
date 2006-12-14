package net.sf.gratia.reporting;

import net.sf.gratia.services.*;
import java.util.*;
import java.sql.*;

public class GenerateSQL
{
		XP xp = new XP();
		//
		// database related
		//
		String driver = "";
		String url = "";
		String user = "";
		String password = "";
		Connection connection;
		Statement statement;
		ResultSet resultSet;
		//
		// processing related
		//
		String key = null;
		String role = null;
		String policy = "Unknown";
		String sql = null;
		String p1 = null;
		String p2 = null;
		String p3 = null;
		String p4 = null;
		String p5 = null;
		String p6 = null;
		String p7 = null;
		String p8 = null;
		String p9 = null;
		String data = null;
		
		String dq = "'";
    
		public GenerateSQL(String sql,String key)
		{
				this.sql = sql;
				this.key = key;
		}

		public String generate()
		{
				openConnection();

				String command = "select * from trace where userkey = " + dq + key + dq;
				try
						{
								statement = connection.prepareStatement(command);
								resultSet = statement.executeQuery(command);
								while(resultSet.next())
										{
												data = resultSet.getString("data");
												p1 = resultSet.getString("p1");
												p2 = resultSet.getString("p2");
												p3 = resultSet.getString("p3");
												p4 = resultSet.getString("p4");
												p5 = resultSet.getString("p5");
												p6 = resultSet.getString("p6");
												p7 = resultSet.getString("p7");
												p8 = resultSet.getString("p8");
												p9 = resultSet.getString("p9");
												role = resultSet.getString("role");
										}
								resultSet.close();
								statement.close();
						}
				catch (Exception e)
						{
								e.printStackTrace();
								closeConnection();
								return sql;
						}
				command = "select whereclause from RolesTable where role = " + dq + role + dq;
				try
						{
								statement = connection.prepareStatement(command);
								resultSet = statement.executeQuery(command);
								while(resultSet.next())
										{
												policy = resultSet.getString("whereclause");
										}
								resultSet.close();
								statement.close();
						}
				catch (Exception e)
						{
								e.printStackTrace();
								closeConnection();
								return sql;
						}
				closeConnection();
				//
				// only regenerate for role GratiaGlobalAdmin or policy Everything
				//
				if ((! role.equals("GratiaGlobalAdmin")) && (! policy.equals("Everything")))
						return sql;
				//
				// otherwise walk the parameter list and return the reconstituted sql
				//
				if (data.indexOf("?") > 0)
						data = xp.replace(data,"?",dq + p1 + dq);
				if (data.indexOf("?") > 0)
						data = xp.replace(data,"?",dq + p2 + dq);
				if (data.indexOf("?") > 0)
						data = xp.replace(data,"?",dq + p3 + dq);
				if (data.indexOf("?") > 0)
						data = xp.replace(data,"?",dq + p4 + dq);
				if (data.indexOf("?") > 0)
						data = xp.replace(data,"?",dq + p5 + dq);
				if (data.indexOf("?") > 0)
						data = xp.replace(data,"?",dq + p6 + dq);
				if (data.indexOf("?") > 0)
						data = xp.replace(data,"?",dq + p7 + dq);
				if (data.indexOf("?") > 0)
						data = xp.replace(data,"?",dq + p8 + dq);
				if (data.indexOf("?") > 0)
						data = xp.replace(data,"?",dq + p9 + dq);
				return data;
		}

		public void openConnection()
		{
				try
						{
								Properties p = Configuration.getProperties();
								driver = p.getProperty("service.mysql.driver");
								url = p.getProperty("service.mysql.url");
								user = p.getProperty("service.mysql.user");
								password = p.getProperty("service.mysql.password");
						}
				catch (Exception ignore)
						{
						}
				try
						{
								Class.forName(driver).newInstance();
								connection = DriverManager.getConnection(url,user,password);
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}

		public void closeConnection()
		{
				try
						{
								connection.close();
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}
}

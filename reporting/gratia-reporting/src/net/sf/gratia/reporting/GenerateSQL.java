package net.sf.gratia.reporting;

import net.sf.gratia.util.*;
import java.util.*;
import java.sql.*;

public class GenerateSQL
{
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
	String dq = "'";
	String data = null;
	String sql = null;
	String key = null;

	public GenerateSQL(String sql, String key)
      {
            this.sql = sql;
            this.key = key;
      }

	public String generate()
	{
		data = "";
		openConnection();

		String command = "select * from trace where userKey = " + dq + key + dq;
		//System.err.println("command: "+command);
		try
		{
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);
			while(resultSet.next())
			{
				data = resultSet.getString("sqlQuery");
			}
			resultSet.close();
			statement.close();
			closeConnection();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			closeConnection();
			sql = "";
		}
		return data;
	}

	public void openConnection()
	{
		try
		{
			Properties p = Configuration.getProperties();
			driver = p.getProperty("service.mysql.driver");
			url = p.getProperty("service.mysql.url");
			user = p.getProperty("service.reporting.user");
			password = p.getProperty("service.reporting.password");
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

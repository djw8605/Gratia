package net.sf.gratia.administration;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.services.*;

import java.io.*;
import java.net.*;

import java.util.*;

import java.sql.*;

import java.util.regex.*;

public class SQLHandler
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

	String scriptfile = "";
	boolean errorflag = false;
	public boolean testmode = false;
	boolean errorChecking = true;

	public SQLHandler(String scriptfile)
	{
		this.scriptfile = scriptfile;
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
		if (testmode)
		{
			driver = "com.mysql.jdbc.Driver";
			url = "jdbc:mysql://localhost:3306/test";
			user = "root";
			password = "lisp01";
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

	public boolean run()
	{
		openConnection();

		String script = xp.get(scriptfile);
		StringTokenizer st = new StringTokenizer(script,"\n\r");
		StringBuffer buffer = new StringBuffer();

		while (st.hasMoreTokens())
		{
			String line = st.nextToken();
			if (line.toLowerCase().equals("go"))
			{
				execute(buffer.toString());
				buffer = new StringBuffer();
			}
			else if (line.toLowerCase().equals("errorcheckingoff"))
			{
				errorChecking = false;
			}
			else if (line.toLowerCase().equals("errorcheckingon"))
			{
				errorChecking = true;
			}
			else
				buffer.append(line + "\n");
		}
		closeConnection();

		if (errorflag)
			return false;
		else
			return true;
	}

	public void execute(String command)
	{
		try
		{
			System.out.println("SQLHandler: Executing: " + command);
			statement = connection.createStatement();
			statement.executeUpdate(command);
			System.out.println("SQLHandler: Command: OK: " + command);
		}
		catch (Exception e)
		{
			System.out.println("SQLHandler: Command: Error: " + command + " : " + e);
			if (errorChecking)
				errorflag = true;
		}

	}

	//
	// for testing
	//

	public static void main(String[] args)
	{
		SQLHandler handler = new SQLHandler("/eclipse/workspace/gratia/configuration/build-summary-tables.sql");
		handler.testmode = true;
		boolean status = handler.run();
		System.out.println("SQLHandler: Returned: " + status);
	}
}

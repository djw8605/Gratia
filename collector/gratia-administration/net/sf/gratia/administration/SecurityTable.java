package net.sf.gratia.administration;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Execute;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.services.*;

import java.io.*;
import java.net.*;

import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;

import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;

import java.util.regex.*;

public class SecurityTable extends HttpServlet 
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
	String html = "";
	String row = "";
	Pattern p = Pattern.compile("<tr>.*?</tr>",Pattern.MULTILINE + Pattern.DOTALL);
	Matcher m = null;
	StringBuffer buffer = new StringBuffer();
	//
	// globals
	//
	HttpServletRequest request;
	HttpServletResponse response;
	boolean initialized = false;
	//
	// support
	//
	String dq = "\"";
	String comma = ",";
	String cr = "\n";
	Hashtable table = new Hashtable();
	Properties props;

	public void init(ServletConfig config) throws ServletException 
	{
	}

	public void openConnection()
	{
		try
		{
			props = Configuration.getProperties();
			driver = props.getProperty("service.mysql.driver");
			url = props.getProperty("service.mysql.url");
			user = props.getProperty("service.mysql.user");
			password = props.getProperty("service.mysql.password");
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

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		String fqan = (String) request.getSession().getAttribute("FQAN");
		boolean login = true;
		if (fqan == null)
			login = false;
		else if (fqan.indexOf("NoPriveleges") > -1)
			login = false;

		if (!login)
		{
			String uriPart = request.getRequestURI();
			int slash2 = uriPart.substring(1).indexOf("/") + 1;
			uriPart = uriPart.substring(slash2);
			String queryPart = request.getQueryString();
			if (queryPart == null)
				queryPart = "";
			else
				queryPart = "?" + queryPart;

			request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);
			Properties p = Configuration.getProperties();
			String loginLink = p.getProperty("service.secure.connection") + request.getContextPath() + "/gratia-login.jsp";
			html = "<br><center><h3>Please <a href='" + loginLink + "'>login</a> to access the information</h3></center>";
			response.setContentType("text/html");
			response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
			response.setHeader("Pragma", "no-cache"); // HTTP 1.0
			PrintWriter writer = response.getWriter();
			writer.write(html);
			writer.flush();
			writer.close();
		}
		else
		{
			openConnection();
			System.out.println("SecurityTable: doGet");
			if (request.getParameter("action") != null)
				doPost(request,response);

			this.request = request;
			this.response = response;
			table = new Hashtable();
			setup();
			process();
			response.setContentType("text/html");
			response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
			response.setHeader("Pragma", "no-cache"); // HTTP 1.0
			request.getSession().setAttribute("table",table);
			PrintWriter writer = response.getWriter();
			writer.write(html);
			writer.flush();
			writer.close();
			closeConnection();
		}
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		openConnection();
		System.out.println("SecurityTable: doPost");
		this.request = request;
		this.response = response;
		table = (Hashtable) request.getSession().getAttribute("table");
		update();
		closeConnection();
		response.sendRedirect("securitytable.html");
	}

	public void setup()
	{
		html = xp.get(request.getRealPath("/") + "securitytable.html");
		m = p.matcher(html);
		while (m.find())
		{
			String temp = m.group();
			if (temp.indexOf("#index#") > 0)
			{
				row = temp;
				break;
			}
		}
	}

	public void process()
	{
		int index = 0;
		String command = "select securityid,source,alias,state from Security order by alias";
		buffer = new StringBuffer();

		try
		{
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			while(resultSet.next())
			{
				String newrow = new String(row);
				newrow = xp.replaceAll(newrow,"#index#","" + index);
				newrow = xp.replaceAll(newrow,"#dbid#","" + resultSet.getInt(1));
				newrow = xp.replaceAll(newrow,"#type#",resultSet.getString(2));
				newrow = xp.replaceAll(newrow,"#alias#",resultSet.getString(3));
				newrow = xp.replaceAll(newrow,"#state#",resultSet.getString(4));
				table.put("index:" + index,"" + index);
				table.put("dbid:" + index,resultSet.getString(1));
				index++;
				buffer.append(newrow);
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		html = xp.replace(html,row,buffer.toString());
	}

	public void update()
	{
		String action = request.getParameter("action");
		String dbid = request.getParameter("dbid");
		String alias = request.getParameter("alias");

		System.out.println("action: " + action + " dbid: " + dbid + " alias: " + alias);
		if (action.equals("delete"))
			delete(dbid,alias);
		else if (action.equals("deploy"))
			deploy(dbid,alias);
		else if (action.equals("activate"))
			activate(dbid,alias);
		else if (action.equals("deactivate"))
			deactivate(dbid,alias);
		else if (action.equals("deleteall"))
			deleteAll();
		else if (action.equals("deployall"))
			deployAll();
		else if (action.equals("activateall"))
			activateAll();
		else if (action.equals("deactiveall"))
			deactivateAll();
	}

	public void setState(String dbid,String state)
	{
		Statement statement = null;

		try
		{
			String command = "update Security set state = " + dq + state + dq +
			" where securityid = " + dbid;
			statement = connection.createStatement();
			statement.executeUpdate(command);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (Exception ignore)
			{
			}
		}
	}

	public void delete(String dbid,String alias)
	{
		String command = "delete from Security where securityid = " + dbid;
		try
		{
			statement = connection.createStatement();
			statement.executeUpdate(command);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (Exception ignore)
			{
			}
		}
		String keystore = Configuration.getConfigurationPath() + "/truststore";
		command = "keytool -delete -alias " + alias + " -keystore " + keystore + " -storepass server";
		String command1[] =
		{"keytool",
				"-delete",
				"-alias",
				alias,
				"-keystore",
				keystore,
				"-storepass",
		"server"};
		Execute.execute(command1);
		// FlipSSL.flip();
	}

	public void deploy(String dbid,String alias)
	{
		String command = "select hostpem from Security where securityid = " + dbid;
		String configurationPath = Configuration.getConfigurationPath();
		String pempath = configurationPath + "/pem";
		String pem = "";

		try
		{
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			while(resultSet.next())
			{
				pem = resultSet.getString(1);
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		xp.save(pempath,pem);

		String keystore = Configuration.getConfigurationPath() + "/truststore";
		command = "keytool -delete -alias " + alias + " -keystore " + keystore + " -storepass server";
		String command1[] =
		{"keytool",
				"-delete",
				"-alias",
				alias,
				"-keystore",
				keystore,
				"-storepass",
		"server"};
		Execute.execute(command1);

		command = "keytool -import -alias " + alias + " -file pem -keystore " + keystore + 
		" -storepass server -keypass server -noprompt";
		String command2[] =
		{"keytool",
				"-import",
				"-alias",
				alias,
				"-file",
				pempath,
				"-keystore",
				keystore,
				"-storepass",
				"server",
				"-keypass",
				"server",
		"-noprompt"};
		Execute.execute(command2);
		FlipSSL.flip();
	}

	public void activate(String dbid,String alias)
	{
		String command = "select hostpem from Security where securityid = " + dbid;
		String configurationPath = Configuration.getConfigurationPath();
		String pempath = configurationPath + "/pem";
		String pem = "";

		try
		{
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			while(resultSet.next())
			{
				pem = resultSet.getString(1);
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		xp.save("pem",pem);

		String keystore = Configuration.getConfigurationPath() + "/truststore";
		command = "keytool -delete -alias " + alias + " -keystore " + keystore + " -storepass server";
		String command1[] =
		{"keytool",
				"-delete",
				"-alias",
				alias,
				"-keystore",
				keystore,
				"-storepass",
		"server"};
		Execute.execute(command1);

		command = "keytool -import -alias " + alias + " -file pem -keystore " + keystore + 
		" -storepass server -keypass server -noprompt";
		String command2[] =
		{"keytool",
				"-import",
				"-alias",
				alias,
				"-file",
				pempath,
				"-keystore",
				keystore,
				"-storepass",
				"server",
				"-keypass",
				"server",
		"-noprompt"};
		Execute.execute(command2);
		setState(dbid,"Active");
		FlipSSL.flip();
	}

	public void deactivate(String dbid,String alias)
	{
		String keystore = Configuration.getConfigurationPath() + "/truststore";
		String command = "keytool -delete -alias " + alias + " -keystore " + keystore + " -storepass server";
		Execute.execute(command);
		setState(dbid,"Inactive");
		FlipSSL.flip();
	}

	public void deleteAll()
	{
		String command = "select securityid,alias from Security order by alias";
		Statement statement = null;
		ResultSet resultSet = null;

		try
		{
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			while(resultSet.next())
			{
				String dbid = resultSet.getString(1);
				String alias = resultSet.getString(2);
				delete(dbid,alias);
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void deployAll()
	{
		String command = "select securityid,alias from Security order by alias";
		Statement statement = null;
		ResultSet resultSet = null;

		try
		{
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			while(resultSet.next())
			{
				String dbid = resultSet.getString(1);
				String alias = resultSet.getString(2);
				deploy(dbid,alias);
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void activateAll()
	{
		String command = "select securityid,alias from Security order by alias";
		Statement statement = null;
		ResultSet resultSet = null;

		try
		{
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			while(resultSet.next())
			{
				String dbid = resultSet.getString(1);
				String alias = resultSet.getString(2);
				activate(dbid,alias);
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void deactivateAll()
	{
		String command = "select securityid,alias from Security order by alias";
		Statement statement = null;
		ResultSet resultSet = null;

		try
		{
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			while(resultSet.next())
			{
				String dbid = resultSet.getString(1);
				String alias = resultSet.getString(2);
				deactivate(dbid,alias);
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}

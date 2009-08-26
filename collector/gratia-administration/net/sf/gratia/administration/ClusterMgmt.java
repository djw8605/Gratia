package net.sf.gratia.administration;

import net.sf.gratia.util.XP;

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

public class ClusterMgmt extends HttpServlet 
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
	String newname = "<New Cluster Name>";

	public void init(ServletConfig config) throws ServletException 
	{
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

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		String fqan = (String) request.getSession().getAttribute("FQAN");
		boolean login = true;
		if (fqan == null)
			login = false;
		else if (fqan.indexOf("NoPrivileges") > -1)
			login = false;
		
		String uriPart = request.getRequestURI();
		int slash2 = uriPart.substring(1).indexOf("/") + 1;
		uriPart = uriPart.substring(slash2);
		String queryPart = request.getQueryString();
		if (queryPart == null)
			queryPart = "";
		else
			queryPart = "?" + queryPart;

		request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);

		if (!login)
            	{
               		Properties p = Configuration.getProperties();
                	String loginLink = p.getProperty("service.secure.connection") + request.getContextPath() + "/gratia-login.jsp";
			String redirectLocation = response.encodeRedirectURL(loginLink);
			response.sendRedirect(redirectLocation);
			request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);
            	}
		else
		{
			openConnection();
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
		this.request = request;
		this.response = response;
		table = (Hashtable) request.getSession().getAttribute("table");
		update();
		closeConnection();
		response.sendRedirect("vo.html");
	}

	public void setup()
	{
		html = xp.get(request.getRealPath("/") + "cluster.html");
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
		String command = "select clusterid, name from Cluster order by name";
		buffer = new StringBuffer();

		try
		{
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			while(resultSet.next())
			{
				String newrow = new String(row);
				newrow = xp.replaceAll(newrow,"#index#","" + index);
				newrow = xp.replace(newrow,"#clusterid#","" + resultSet.getInt(1));
				newrow = xp.replace(newrow,"#name#",resultSet.getString(2));
				table.put("index:" + index,"" + index);
				table.put("clusterid:" + index,resultSet.getString(1));
				table.put("name:" + index,resultSet.getString(2));
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
		for (int j = 0; j < 5; j++)
		{
			String newrow = new String(row);
			newrow = xp.replaceAll(newrow,"#index#","" + index);
			newrow = xp.replace(newrow,"#name#",newname);
			table.put("index:" + index,"" + index);
			table.put("name:" + index,newname);
			index++;
			buffer.append(newrow);
		}
		html = xp.replace(html,row,buffer.toString());
	}

	public void update()
	{
		int index;
		String key = "";
		String oldvalue = "";
		String newvalue = "";

		for (index = 0; index < 1000; index++)
		{
			key = "index:" + index;
			oldvalue = (String) table.get(key);
			newvalue = (String) request.getParameter(key);
			if (oldvalue == null)
				break;
			key = "name:" + index;
			oldvalue = (String) table.get(key);
			newvalue = (String) request.getParameter(key);
			if (oldvalue.equals(newvalue))
				continue;
			if (oldvalue.equals(newname))
				insert(index);
			else
				update(index);
		}
	}

	public void update(int index)
	{
		String command = 
			"update VO set" + cr +
			" VOName = " + dq + (String) request.getParameter("voname:" + index) + dq + cr +
			" where VOid = " + request.getParameter("void:" + index);
		try
		{
			statement = connection.createStatement();
			statement.executeUpdate(command);
			// connection.commit();
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

	public void insert(int index)
	{
		String command = 
			"insert into VO (VOName) values(" + 
			dq + (String) request.getParameter("voname:" + index) + dq + ")";
		try
		{
			statement = connection.createStatement();
			statement.executeUpdate(command);
			// connection.commit();
		}
		catch (Exception e)
		{
			System.out.println("command: " + command);
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
}

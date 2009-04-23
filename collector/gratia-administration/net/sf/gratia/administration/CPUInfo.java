package net.sf.gratia.administration;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import java.io.*;

import java.util.Properties;
import java.util.Hashtable;

import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;

import java.util.regex.*;

public class CPUInfo extends HttpServlet
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
	Hashtable sitebyid = new Hashtable();
	Hashtable sitebyname = new Hashtable();
	String newname = "<New Probe Name>";

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
			if (request.getParameter("action") != null)
			{
				if (request.getParameter("action").equals("delete"))
					delete();
			}
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
		response.sendRedirect("cpuinfo.html");
	}

	public void setup()
	{
		html = xp.get(request.getRealPath("/") + "cpuinfo.html");
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
		String command = "";
		buffer = new StringBuffer();
		sitebyid = new Hashtable();
		sitebyname = new Hashtable();

		try
		{
			command = "select * from CPUInfo";
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			while(resultSet.next())
			{
				String newrow = new String(row);
				newrow = xp.replaceAll(newrow,"#index#","" + index);
				table.put("index:" + index,"" + index);

				newrow = xp.replaceAll(newrow,"#hostid#",resultSet.getString("HostId"));
				table.put("hostid:" + index,resultSet.getString("HostId"));

				String HostDescription = resultSet.getString("HostDescription");
				if (HostDescription == null)
					HostDescription = "No Data";

				newrow = xp.replaceAll(newrow,"#hostdescription#",HostDescription);
				table.put("HostDescription:" + index,HostDescription);

				String benchmarkscore = resultSet.getString("BenchmarkScore");
				if (benchmarkscore == null)
					benchmarkscore = "No Data";
				newrow = xp.replaceAll(newrow,"#benchmarkscore#",benchmarkscore);
				table.put("benchmarkscore:" + index,benchmarkscore);

				String cpucount = resultSet.getString("CPUCount");
				if (cpucount == null)
					cpucount = "No Data";
				newrow = xp.replaceAll(newrow,"#cpucount#",cpucount);
				table.put("cpucount:" + index,cpucount);

				String os = resultSet.getString("OS");
				if (os == null)
					os = "No Data";
				newrow = xp.replaceAll(newrow,"#os#",os);
				table.put("os:" + index,os);

				String osversion = resultSet.getString("OSVersion");
				if (osversion == null)
					osversion = "No Data";
				newrow = xp.replaceAll(newrow,"#osversion#",osversion);
				table.put("osversion:" + index,osversion);

				String cputype = resultSet.getString("CPUType");
				if (cputype == null)
					cputype = "No Data";
				newrow = xp.replaceAll(newrow,"#cputype#",cputype);
				table.put("cputype:" + index,cputype);

				buffer.append(newrow);
				index++;
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		String newrow = new String(row);
		newrow = xp.replaceAll(newrow,"#index#","" + index);
		newrow = xp.replace(newrow,"#hostdescription#",newname);
		newrow = xp.replace(newrow,"#benchmarkscore#","");
		newrow = xp.replace(newrow,"#cpucount#","");
		newrow = xp.replace(newrow,"#os#","");
		newrow = xp.replace(newrow,"#osversion#","");
		newrow = xp.replace(newrow,"#cputype#","");
		table.put("index:" + index,"" + index);
		table.put("hostdescription:" + index,newname);

		html = xp.replace(html,row,buffer.toString());
	}


	public void update()
	{
		int index;
		String key = "";
		String oldvalue = "";
		String newvalue = "";

		/*
		Enumeration x = request.getParameterNames();
		while(x.hasMoreElements())
		{
			key = (String) x.nextElement();
			String value = (String) request.getParameter(key);
			System.out.println("key: " + key + " value: " + value);
		}
		 */

		for (index = 0; index < 1000; index++)
		{
			key = "index:" + index;
			oldvalue = (String) table.get(key);
			newvalue = (String) request.getParameter(key);
			if (oldvalue == null)
				break;

			key = "hostdescription:" + index;
			oldvalue = (String) table.get(key);
			newvalue = (String) request.getParameter(key);

			if ((oldvalue != null) && (oldvalue.equals(newname)) && (! oldvalue.equals(newvalue)))
			{
				insert(index);
				continue;
			}
			if ((oldvalue != null) && (oldvalue.equals(newvalue)))
				break;

			key = "benchmarkscore:" + index;
			oldvalue = (String) table.get(key);
			newvalue = (String) request.getParameter(key);
			if (! oldvalue.equals(newvalue))
			{
				update(index);
				continue;
			}

			key = "cpucount:" + index;
			oldvalue = (String) table.get(key);
			newvalue = (String) request.getParameter(key);
			if (! oldvalue.equals(newvalue))
			{
				update(index);
				continue;
			}

			key = "os:" + index;
			oldvalue = (String) table.get(key);
			newvalue = (String) request.getParameter(key);
			if (! oldvalue.equals(newvalue))
			{
				update(index);
				continue;
			}

			key = "osversion:" + index;
			oldvalue = (String) table.get(key);
			newvalue = (String) request.getParameter(key);
			if (! oldvalue.equals(newvalue))
			{
				update(index);
				continue;
			}

			key = "cputype:" + index;
			oldvalue = (String) table.get(key);
			newvalue = (String) request.getParameter(key);
			if (! oldvalue.equals(newvalue))
			{
				update(index);
				continue;
			}

		}
	}

	public void update(int index)
	{
		String hostid = (String) request.getParameter("hostid:" + index);
		String HostDescription = (String) request.getParameter("hostdescription:" + index);
		String benchmarkscore = (String) request.getParameter("benchmarkscore:" + index);
		String cpucount = (String) request.getParameter("cpucount:" + index);
		String os = (String) request.getParameter("os:" + index);
		String osversion = (String) request.getParameter("osversion:" + index);
		String cputype = (String) request.getParameter("cputype:" + index);

		String command =
			"update CPUInfo set" + cr +
			" HostDescription = " + dq + HostDescription + dq + comma + cr +
			" BenchmarkScore = " + dq + benchmarkscore + dq + comma + cr +
			" CPUCount = " + dq + cpucount + dq + comma + cr +
			" OS = " + dq + os + dq + comma + cr +
			" OSVersion = " + dq + osversion + dq + comma + cr +
			" CPUType = " + dq + cputype + dq + cr +
			" where hostid = " + hostid;
		try
		{
			statement = connection.createStatement();
			statement.executeUpdate(command);
			statement.close();
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
		String HostDescription = (String) request.getParameter("hostdescription:" + index);
		String benchmarkscore = (String) request.getParameter("benchmarkscore:" + index);
		String cpucount = (String) request.getParameter("cpucount:" + index);
		String os = (String) request.getParameter("os:" + index);
		String osversion = (String) request.getParameter("osversion:" + index);
		String cputype = (String) request.getParameter("cputype:" + index);

		String command =
			"insert into CPUInfo" +
			"(HostDescription,BenchmarkScore,CPUCount,OS,OSVersion,CPUType)" + cr +
			"values(" + cr +
			dq + HostDescription + dq + comma + cr +
			dq + benchmarkscore + dq + comma + cr +
			dq + cpucount + dq + comma + cr +
			dq + os + dq + comma + cr +
			dq + osversion + dq + comma + cr +
			dq + cputype + dq + ")";

		try
		{
			statement = connection.createStatement();
			statement.executeUpdate(command);
			statement.close();
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

	void delete()
	{
		String command = "";

		try
		{
			command = "delete from CPUInfo where hostid = " + request.getParameter("hostid");
			Statement statement = connection.createStatement();
			statement.executeUpdate(command);
			statement.close();
		}
		catch (Exception e)
		{
			System.out.println("command: " + command);
			e.printStackTrace();
		}
	}

}

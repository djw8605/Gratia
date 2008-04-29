package net.sf.gratia.administration;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.services.*;

import java.io.*;
import java.net.*;

import java.util.TreeSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;

import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;
import java.text.*;
import java.util.regex.*;

public class VONameCorrection extends HttpServlet 
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
	Hashtable vobyid = new Hashtable();
	Hashtable vobyname = new Hashtable();
	String newname = "<New VOName>";

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
		response.sendRedirect("vonamecorrection.html");
	}

	public void setup()
	{
		html = xp.get(request.getRealPath("/") + "vonamecorrection.html");
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
		vobyid = new Hashtable();
		vobyname = new Hashtable();

		try
		{
			command = "select VOid,VOName from VO";
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			while(resultSet.next())
			{
				String id = resultSet.getString(1);
				String name = resultSet.getString(2);
				vobyid.put(id,name);
				vobyname.put(name,id);
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		try
		{
			command = "select corrid,VOName,ReportableVOName,VOid from VONameCorrection order by VOName";
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			while(resultSet.next())
			{
				String newrow = new String(row);
				String voname = resultSet.getString(2);
				String reportablevoname = resultSet.getString(3);

				newrow = xp.replaceAll(newrow,"#index#","" + index);
				table.put("index:" + index,"" + index);

				newrow = xp.replaceAll(newrow,"#corrid#",resultSet.getString(1));
				table.put("corrid:" + index,resultSet.getString(1));

				newrow = xp.replaceAll(newrow,"#voname#",voname);
				if (reportablevoname != null)
					newrow = xp.replaceAll(newrow,"#reportablevoname#",reportablevoname);
				else
					newrow = xp.replaceAll(newrow,"#reportablevoname#","");

				String actualname = (String) vobyid.get(resultSet.getString(4));
				newrow = volist(index,newrow,actualname);

				boolean usered = true;

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

		for (int j = 0; j < 1; j++)
		{
			String newrow = new String(row);
			newrow = xp.replaceAll(newrow,"#index#","" + index);
			newrow = xp.replace(newrow,"#voname#",newname);
			newrow = xp.replace(newrow,"#reportablevoname#","");
			newrow = volist(index,newrow,"xxx");
			table.put("index:" + index,"" + index);
			table.put("voname:" + index,newname);
			index++;
			buffer.append(newrow);
		}
		html = xp.replace(html,row,buffer.toString());
	}

	public String volist(int index,String input,String current)
	{
		Pattern p = Pattern.compile("<sel.*#actualname#.*?</select>",Pattern.MULTILINE + Pattern.DOTALL);
		Matcher m = p.matcher(input);
		m.find();
		String row = m.group();

		p = Pattern.compile("<option.*</option>");
		m = p.matcher(input);
		m.find();
		String option = m.group();
		StringBuffer buffer = new StringBuffer();

		TreeSet names = new TreeSet(vobyname.keySet());
		for (Iterator x = names.iterator(); x.hasNext();)
		{
			String newoption = new String(option);
			String name = (String) x.next();
			newoption = xp.replaceAll(newoption,"#actualname#",name);
			if (name.equals(current))
			{
				newoption = xp.replace(newoption,"#selected#","selected='selected'");
				table.put("actualname:" + index,current);
			}
			else
				newoption = xp.replace(newoption,"#selected#","");
			buffer.append(newoption);
		}

		String temp = xp.replace(row,option,buffer.toString());
		String output = xp.replace(input,row,temp);
		return output;
	}

	public String activelist(int index,String input,String current)
	{
		Pattern p = Pattern.compile("<select name=\"active:.*?</select>",Pattern.MULTILINE + Pattern.DOTALL);
		Matcher m = p.matcher(input);
		m.find();
		String row = m.group();
		String r = "";

		table.put("active:" + index,current);
		if (current.equals("Yes"))
		{
			r = "<select name='active:" + index + "' id='active:" + index + "'>" + cr;
			r = r + "<option value='Yes' selected='selected'>Yes</option>" + cr;
			r = r + "<option value='No'>No</option>" + cr;
			r = r + "</select>" + cr;
		}
		else
		{
			r = "<select name='active:" + index + "' id='active:" + index +"'>" + cr;
			r = r + "<option value='No' selected='selected'>No</option>" + cr;
			r = r + "<option value='Yes'>Yes</option>" + cr;
			r = r + "</select>" + cr;
		}

		String output = xp.replace(input,row,r);
		return output;
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

			key = "voname:" + index;
			oldvalue = (String) table.get(key);
			newvalue = (String) request.getParameter(key);

			if ((oldvalue != null) && (oldvalue.equals(newname)) && (! oldvalue.equals(newvalue)))
			{
				insert(index);
				continue;
			}
			if ((oldvalue != null) && (oldvalue.equals(newvalue)))
				break;

			key = "actualname:" + index;
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
		String corrid = (String) request.getParameter("corrid:" + index);
		String actualname = (String) request.getParameter("actualname:" + index);
		String VOid = (String) vobyname.get(actualname);

		String command = 
			"update VONameCorrection set" + cr +
			" VOid = " + VOid + cr +
			" where corrid = " + corrid;
		try
		{
			statement = connection.createStatement();
			statement.executeUpdate(command);
			statement.close();
			// connection.commit();
		}
		catch (Exception e)
		{
			System.err.println("Command = "+command);
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
		String actualname = (String) request.getParameter("actualname:" + index);
		String VOid = (String) vobyname.get(actualname);
		String voname = (String) request.getParameter("voname:" + index);
		String reportablevoname = (String) request.getParameter("reportablevoname:" + index);
		if (reportablevoname == null || reportablevoname.length()==0) {
			reportablevoname = "null";
		} else {
			reportablevoname = dq + reportablevoname + dq;
		}

		String command = 
			"insert into VONameCorrection (VOid,VOName,ReportableVOName) values(" + 
			VOid + comma + dq + voname + dq + comma + reportablevoname + ")";
		try
		{
			statement = connection.createStatement();
			statement.executeUpdate(command);
			statement.close();
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

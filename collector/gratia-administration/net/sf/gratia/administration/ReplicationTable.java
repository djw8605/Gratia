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
import java.util.Vector;

import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;

import java.util.regex.*;

public class ReplicationTable extends HttpServlet
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

	Pattern p = Pattern.compile("<tr id=\"datarow.*?>.*#index#.*?</table>.*?</tr>", Pattern.MULTILINE + Pattern.DOTALL);
	Matcher m = null;
	StringBuffer buffer = new StringBuffer();
	//
	// globals
	//
	HttpServletRequest request;
	HttpServletResponse response;
	boolean initialized = false;
	Properties props;
	String message = null;
	//
	// support
	//
	String dq = "\"";
	String comma = ",";
	String cr = "\n";
	Hashtable table = new Hashtable();
	String newname = "<New Entry>";

	// Which Servlet/web page is this
	String Name;

	// Which Records are we replicating
	String RecordTable = "JobUsageRecord";

	public void init(ServletConfig config) throws ServletException
	{
		// javax.servlet.ServletConfig.getInitParameter() 
		String what = config.getInitParameter("RecordType");
		if (what != null)
		{
			RecordTable = what;
		}
		Name = config.getServletName();
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
			connection = DriverManager.getConnection(url, user, password);
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

			this.request = request;
			this.response = response;
			table = new Hashtable();
			setup();
			if (request.getParameter("action") != null)
			{
				if (request.getParameter("action").equals("register"))
					register();
				else if (request.getParameter("action").equals("activate"))
					activate();
				else if (request.getParameter("action").equals("deactivate"))
					deactivate();
				else if (request.getParameter("action").equals("reset"))
					reset();
				else if (request.getParameter("action").equals("delete"))
					delete();
				else if (request.getParameter("action").equals("test"))
					test();
			}
			process();
			response.setContentType("text/html");
			response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
			response.setHeader("Pragma", "no-cache"); // HTTP 1.0
			request.getSession().setAttribute("table", table);
			PrintWriter writer = response.getWriter();
			//
			// cleanup message
			//
			if (message != null)
				html = xp.replace(html, "#message#", message);
			else
				html = xp.replace(html, "#message#", "");
			message = null;

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
		table = (Hashtable)request.getSession().getAttribute("table");
		setup();
		update();
		closeConnection();
		response.sendRedirect(Name+".html?action=showmessage");
	}

	public void setup()
	{
		html = xp.get(request.getRealPath("/") + "replicationtable.html");
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

		Vector vector = new Vector();

		try
		{
			command = "select distinct ProbeName from " + RecordTable + "_Meta order by ProbeName";
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			vector.add("All");

			while (resultSet.next())
				vector.add("Probe:" + resultSet.getString(1));

			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		if (RecordTable.equals("JobUsageRecord")) {
			try
			{
				command = "select distinct(V.VOName) from VO V, VONameCorrection C " +
				"where V.void = C.void order by V.VOName";
				statement = connection.prepareStatement(command);
				resultSet = statement.executeQuery(command);

				while (resultSet.next())
					vector.add("VO:" + resultSet.getString(1));

				resultSet.close();
				statement.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
// 		try {
// 			command = "select distinct(Grid) from " + RecordTable + "_Meta order by Grid";
// 			statement = connection.prepareStatement(command);
// 			resultSet = statement.executeQuery(command);

// 			while (resultSet.next()) {
// 				String grid = resultSet.getString(1);
// 				if (resultSet.wasNull()) {
// 					grid = "<null>";
// 				}
// 				vector.add("Grid:" + grid);
// 			}

// 			resultSet.close();
// 			statement.close();
// 		}
// 		catch (Exception e) {
// 			e.printStackTrace();
// 		}
		try
		{
			command =
				"select replicationid,registered,running,security,openconnection,secureconnection," +
				"frequency,dbid,rowcount,probename,recordtable from Replication " +
				" where recordtable = " + dq + RecordTable + dq +
				" order by replicationid";
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);

			while (resultSet.next())
			{
				String newrow = new String(row);

				newrow = xp.replaceAll(newrow, "#index#", "" + index);
				table.put("index:" + index, "" + index);

				Pattern p1 = Pattern.compile("<input\\s*id=\"openconnection.*?/>", Pattern.MULTILINE + Pattern.DOTALL);
				Matcher m1 = p1.matcher(newrow);
				m1.find();
				String temp = m1.group();
				newrow = xp.replace(newrow, temp, "<label>" + resultSet.getString("openconnection") + "</label>");
				table.put("openconnection:" + index, resultSet.getString("openconnection"));

				if (resultSet.getString("registered").equals("0"))
					newrow = xp.replaceAll(newrow, "#registered#", "N");
				else
					newrow = xp.replaceAll(newrow, "#registered#", "Y");

				if (resultSet.getString("running").equals("0"))
					newrow = xp.replaceAll(newrow, "#running#", "N");
				else
					newrow = xp.replaceAll(newrow, "#running#", "Y");


				newrow = xp.replaceAll(newrow, "#replicationid#", resultSet.getString("replicationid"));
				table.put("replicationid:" + index, resultSet.getString("replicationid"));

				newrow = xp.replaceAll(newrow, "#webpagename#", Name);

				newrow = xp.replaceAll(newrow, "#frequency#", resultSet.getString("frequency"));
				table.put("frequency:" + index, resultSet.getString("frequency"));

				newrow = xp.replaceAll(newrow, "#dbid#", resultSet.getString("dbid"));
				newrow = xp.replaceAll(newrow, "#rowcount#", resultSet.getString("rowcount"));

				String usesecurity = "No";
				if (resultSet.getString("security").equals("1"))
					usesecurity = "Yes";
				newrow = securitylist(index, newrow, usesecurity);

				newrow = probelist(index, newrow, resultSet.getString("probename"), vector);

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
		{
			String newrow = new String(row);

			Pattern p1 = Pattern.compile("<table id=\"optiontable.*?</table>", Pattern.MULTILINE + Pattern.DOTALL);
			Matcher m1 = p1.matcher(newrow);
			m1.find();
			String zap = m1.group();
			newrow = xp.replace(newrow, zap, "");

			newrow = xp.replaceAll(newrow, "#index#", "" + index);
			newrow = xp.replace(newrow, "#openconnection#", newname);

			newrow = xp.replace(newrow, "#frequency#", "1");
			newrow = xp.replace(newrow, "#rowcount#", "0");
			newrow = xp.replace(newrow, "#dbid#", "0");
			newrow = xp.replace(newrow, "#registered#", "N");
			newrow = xp.replace(newrow, "#running#", "N");
			newrow = probelist(index, newrow, "xxx", vector);
			newrow = securitylist(index, newrow, "No");
			table.put("index:" + index, "" + index);
			table.put("openconnection:" + index, newname);
			index++;
			buffer.append(newrow);
		}

		html = xp.replace(html,"#recordtable#",RecordTable);
		html = xp.replace(html, row, buffer.toString());
	}

	public String probelist(int index, String input, String current, Vector names)
	{
		Pattern p1 = Pattern.compile("<select id=\"probename.*probename:.*?</select>", Pattern.MULTILINE + Pattern.DOTALL);
		Matcher m1 = p1.matcher(input);
		m1.find();
		String row = m1.group();

		Pattern p2 = Pattern.compile("<option.*</option>");
		Matcher m2 = p2.matcher(row);
		m2.find();
		String option = m2.group();

		StringBuffer buffer = new StringBuffer();

		for (Enumeration x = names.elements(); x.hasMoreElements(); )
		{
			String name = (String)x.nextElement();
			String printableName = name.replaceAll("<", "&lt;");
			printableName = printableName.replaceAll(">", "&gt;");
			if (name.equals(current))
			{
				buffer.append("<option selected=" + dq + "selected" + dq + ">" + printableName + "</option>" + cr);
				table.put("probename:" + index, current);
			}
			else
				buffer.append("<option>" + printableName + "</option>" + cr);
		}

		String temp = xp.replace(row, option, buffer.toString());
		String output = xp.replace(input, row, temp);
		return output;
	}

	public String securitylist(int index, String input, String current)
	{
		Pattern p = Pattern.compile("<select id=\"security.*security:.*?</select>", Pattern.MULTILINE + Pattern.DOTALL);
		Matcher m = p.matcher(input);
		m.find();
		String row = m.group();
		StringBuffer r = new StringBuffer();

		if (current.equals("Yes"))
		{
			r.append("<select name=" + dq + "security:" + index + dq + ">" + cr);
			r.append("<option value=" + dq + "Yes" + dq + " selected=" + dq + "selected" + dq + ">Yes</option>" + cr);
			r.append("<option value=" + dq + "No" + dq + ">No</option>" + cr);
			r.append("</select>" + cr);
			table.put("security:" + index, "Yes");

		}
		else
		{
			r.append("<select name=" + dq + "security:" + index + dq + ">" + cr);
			r.append("<option value=" + dq + "Yes" + dq + ">Yes</option>" + cr);
			r.append("<option value=" + dq + "No" + dq + " selected=" + dq + "selected" + dq + ">No</option>" + cr);
			r.append("</select>" + cr);
			table.put("security:" + index, "No");
		}

		String output = xp.replace(input, row, r.toString());
		return output;
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
			oldvalue = (String)table.get(key);
			newvalue = (String)request.getParameter(key);
			if (oldvalue == null)
				break;

			key = "openconnection:" + index;
			oldvalue = (String)table.get(key);
			newvalue = (String)request.getParameter(key);

			if ((oldvalue != null) && (oldvalue.equals(newname)) && (!oldvalue.equals(newvalue)))
			{
				insert(index);
				continue;
			}
			if ((oldvalue != null) && (oldvalue.equals(newvalue)))
				break;

			key = "security:" + index;
			oldvalue = (String)table.get(key);
			newvalue = (String)request.getParameter(key);
			if (!oldvalue.equals(newvalue))
			{
				update(index);
				continue;
			}

			key = "frequency:" + index;
			oldvalue = (String)table.get(key);
			newvalue = (String)request.getParameter(key);
			if (!oldvalue.equals(newvalue))
			{
				update(index);
				continue;
			}

			key = "probename:" + index;
			oldvalue = (String)table.get(key);
			newvalue = (String)request.getParameter(key);
			if (!oldvalue.equals(newvalue))
			{
				update(index, true); // Reset dbid and rowcount
				continue;
			}

		}
		table.put("message", "Entry added/updated");
	}

	public void insert(int index)
	{
		String command = "";
		int sflag = 0;
		int rflag = 0;

		if (request.getParameter("security:" + index).equals("Yes"))
			sflag = 1;
		if (allreadyRegistered(request.getParameter("openconnection:" + index)))
			rflag = 1;

		try
		{
			command =
				"insert into Replication(openconnection,registered,running,security,probename," +
				"frequency,dbid,rowcount,recordtable) values(" + cr +
				dq + request.getParameter("openconnection:" + index) + dq + comma + cr +
				rflag + comma + cr +
				"0" + comma + cr +
				sflag + comma + cr +
				dq + request.getParameter("probename:" + index) + dq + comma + cr +
				request.getParameter("frequency:" + index) + comma + cr +
				"0" + comma + cr +
				"0" + comma + cr +
				dq + RecordTable + dq +
				")";
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

	public boolean allreadyRegistered(String openconnection)
	{
		String command = "select count(*) from Replication where registered = 1 and openconnection = " +
		dq + openconnection + dq;
		int count = 0;

		try
		{
			Statement statement = connection.prepareStatement(command);
			ResultSet resultSet = statement.executeQuery(command);
			while (resultSet.next())
			{
				count = resultSet.getInt(1);
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			System.out.println("command: " + command);
			e.printStackTrace();
		}
		if (count > 0)
			return true;
		return false;
	}

	public void update(int index) {
		update(index, false);
	}

	public void update(int index, boolean reset)
	{
		String command = "";
		int sflag = 0;

		if (request.getParameter("security:" + index).equals("Yes"))
			sflag = 1;

		try
		{
			command =
				"update Replication set" + cr +
				"security = " + sflag + comma + cr +
				"probename = " + dq + request.getParameter("probename:" + index) + dq + comma + cr +
				"frequency = " + request.getParameter("frequency:" + index);
			if (reset) {
				command += comma + cr + "dbid = 0" + comma + cr + "rowcount = 0";
			}
			command += cr + "where replicationid = " + request.getParameter("replicationid:" + index);
			System.out.println("command: " + command);
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

	void register()
	{
		String security = null;
		String openconnection = null;
		String command = "";
		String mypem = "";
		String remotepem = "";
		String secureconnection = "";
		Post post;
		String response;
		String[] results;

		try
		{
			command =
				"select security,openconnection from Replication" +
				" where replicationid = " + request.getParameter("replicationid");
			Statement statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);
			while (resultSet.next())
			{
				security = resultSet.getString("security");
				openconnection = resultSet.getString("openconnection");
			}
			resultSet.close();
			statement.close();
			if (security == null)
				return;
			if (security.equals("0"))
				return;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
		//
		// get my pem
		//
		post = new Post(props.getProperty("service.open.connection") + "/gratia-security/security", "get");
		response = post.send(true);
		if (!post.success) {
                    message = post.errorMsg;
                    return;
		}
		results = split(response, ":");
		mypem = results[1];
		//
		// register with host
		//
		post = new Post(openconnection + "/gratia-security/security", "put");
		post.add("arg1", "Client:" + props.getProperty("service.open.connection"));
		post.add("arg2", "Replication");
		post.add("arg3", mypem);
		response = post.send(true);
		if (!post.success) {
                    message = post.errorMsg;
                    return;
		}
		results = split(response, ":");
		if (!results[0].equals("ok"))
		{
			message = "Error Registering With Remote Host: " + response;
			System.out.println(message);
		}
		//
		// get the hosts pem
		//
		post = new Post(openconnection + "/gratia-security/security", "get");
		response = post.send(true);
		if (!post.success) {
                    message = post.errorMsg;
                    return;
		}
		results = split(response, ":");
		remotepem = results[1];
		secureconnection = results[2];
		secureconnection = URLDecoder.decode(secureconnection);
		//
		// and register with myself
		//
		post = new Post(props.getProperty("service.open.connection") + "/gratia-security/security", "put");
		post.add("arg1", "Server:" + secureconnection);
		post.add("arg2", "Replication");
		post.add("arg3", remotepem);
		response = post.send(true);
		if (!post.success) {
                    message = post.errorMsg;
                    return;
		}
		results = split(response, ":");
		if (!results[0].equals("ok"))
		{
			message = ("Error Registering With Myself: " + response);
			System.out.println(message);	 
		}

		//
		// now - update database with remote secure connection
		//
		try
		{
			command =
				"update Replication set secureconnection = " + dq + secureconnection + dq + comma + cr +
				" registered = 1" + cr +
				" where replicationid = " + request.getParameter("replicationid");
			Statement statement = connection.createStatement();
			statement.executeUpdate(command);
			statement.close();
		}
		catch (Exception e)
		{
			System.out.println("command: " + command);
			e.printStackTrace();
			message = "Error registering "+openconnection+" : " + cr + e.toString();
		}
	}

	void activate()
	{
		String command = "";

		try
		{
			command = "update Replication set running = 1" +
			" where replicationid = " + request.getParameter("replicationid");
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

	void reset() {
		String command = "";

		try
		{
			command = "update Replication set dbid = 0, rowcount = 0" +
			" where replicationid = " + request.getParameter("replicationid");
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

	void deactivate()
	{
		String command = "";

		try
		{
			command = "update Replication set running = 0" +
			" where replicationid = " + request.getParameter("replicationid");
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

	void delete()
	{
		String command = "";

		try
		{
			command = "delete from Replication where replicationid = " + request.getParameter("replicationid");
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

	public void test()
	{
		String command = "select * from Replication where replicationid = " + request.getParameter("replicationid");
		String target = "";
		String security = "";
		String response = "";

		try
		{
                    Statement statement = connection.prepareStatement(command);
                    ResultSet resultSet = statement.executeQuery(command);
                    while (resultSet.next())
			{
                            security = resultSet.getString("security");
                            if (security.equals("0"))
                                target = resultSet.getString("openconnection");
                            else 
                                target = resultSet.getString("secureconnection");
			}
                    resultSet.close();
                    statement.close();
                    
                    statement.close();
		}
		catch (Exception e)
                    {
			System.out.println("command: " + command);
			e.printStackTrace();
                    }
		Post post = new Post(target + "/gratia-servlets/rmi", "update", "xxx");
		try
                    {
			response = post.send(true);
                    }
		catch (Exception e)
                    {
                        message = "Error for "+target+" : " + cr + xp.parseException(e);
                        return;
                    }
		if (!post.success) {
                    message = "Error for "+target+" : " + post.errorMsg;
                    return;
		}
		try
                    {
			String[] results = split(response, ":");
			if (!results[0].equals("OK"))
                            {
				message = "Error for "+target+" : " + response;
				return;
                            }
                    } 
		catch (Exception e)
                    {
			message = "Error for "+target+" : " + cr + xp.parseException(e);
			return;
                    }
		message = "Test Succeeded !!";
	}
    
	public String[] split(String input, String sep)
	{
            Vector vector = new Vector();
            StringTokenizer st = new StringTokenizer(input, sep);
            while (st.hasMoreTokens())
                vector.add(st.nextToken());
            String[] results = new String[vector.size()];
            for (int i = 0; i < vector.size(); i++)
                results[i] = (String)vector.elementAt(i);
            return results;
	}
}

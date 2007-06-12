package net.sf.gratia.administration;

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
import java.text.*;

public class Status extends HttpServlet 
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
    Pattern p = Pattern.compile("<tr class=qsize>.*?</tr>",Pattern.MULTILINE + Pattern.DOTALL);
    Matcher m = null;

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
	openConnection();

	this.request = request;
	this.response = response;
	setup();
	process();
	response.setContentType("text/html");
	response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
	response.setHeader("Pragma", "no-cache"); // HTTP 1.0
	PrintWriter writer = response.getWriter();
	writer.write(html);
	writer.flush();
	writer.close();
	closeConnection();
    }

    public static String DisplayInt(Integer value) 
    {
	if (value == null)
	    return "n/a";
	else 
	    return value.toString();
    }

    public void setup()
    {
	html = xp.get(request.getRealPath("/") + "status.html");
    }

    public void process()
    {
	int index = 0;
	String command = "";
	buffer = new StringBuffer();
	String dq = "'";

	Integer count1 = null;
	Integer error1 = null;

	Integer count2 = null;
	Integer error2 = null;

	Integer count3 = null;
	Integer error3 = null;

	Integer count4 = null;
	Integer error4 = null;

	Integer count5 = null;
	Integer error5 = null;

	Integer count6 = null;
	Integer error6 = null;

	Integer count24 = null;
	Integer error24 = null;

	Integer count7 = null;
	Integer error7 = null;
				
	Integer totalcount = null;
	Integer totalerror = null;

	java.util.Date now = new java.util.Date();
	long decrement = 60 * 60 * 1000;
	java.util.Date to = new java.util.Date();
	java.util.Date from = new java.util.Date(to.getTime() - decrement);

	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	try
	    {
		//
		// previous hour
		//

		command = "select count(*) from JobUsageRecord_Meta where ServerDate > " + dq + format.format(from) + dq +
		    " and ServerDate <= " + dq + format.format(to) + dq;
		System.out.println("command: " + command);
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    count1 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		command = "select count(*) from DupRecord where EventDate > " + dq + format.format(from) + dq +
		    " and EventDate <= " + dq + format.format(to) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    error1 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		//
		// hour - 2
		//

		decrement = 60 * 60 * 1000;
		to = from;
		from = new java.util.Date(to.getTime() - decrement);

		command = "select count(*) from JobUsageRecord_Meta where ServerDate > " + dq + format.format(from) + dq +
		    " and ServerDate <= " + dq + format.format(to) + dq;
		System.out.println("command: " + command);
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    count2 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		command = "select count(*) from DupRecord where EventDate > " + dq + format.format(from) + dq +
		    " and EventDate <= " + dq + format.format(to) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    error2 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		//
		// hour - 3
		//

		decrement = 60 * 60 * 1000;
		to = from;
		from = new java.util.Date(to.getTime() - decrement);

		command = "select count(*) from JobUsageRecord_Meta where ServerDate > " + dq + format.format(from) + dq +
		    " and ServerDate <= " + dq + format.format(to) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    count3 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		command = "select count(*) from DupRecord where EventDate > " + dq + format.format(from) + dq +
		    " and EventDate <= " + dq + format.format(to) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    error3 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		//
		// previous - 4
		//

		decrement = 60 * 60 * 1000;
		to = from;
		from = new java.util.Date(to.getTime() - decrement);

		command = "select count(*) from JobUsageRecord_Meta where ServerDate > " + dq + format.format(from) + dq +
		    " and ServerDate <= " + dq + format.format(to) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    count4 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		command = "select count(*) from DupRecord where EventDate > " + dq + format.format(from) + dq +
		    " and EventDate <= " + dq + format.format(to) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    error4 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		//
		// previous - 5
		//

		to = from;
		decrement = 60 * 60 * 1000;
		from = new java.util.Date(to.getTime() - decrement);

		command = "select count(*) from JobUsageRecord_Meta where ServerDate > " + dq + format.format(from) + dq +
		    " and ServerDate <= " + dq + format.format(to) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    count5 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		command = "select count(*) from DupRecord where EventDate > " + dq + format.format(from) + dq +
		    " and EventDate <= " + dq + format.format(to) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    error5 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		//
		// previous - 6
		//

		decrement = 60 * 60 * 1000;
		to = from;
		from = new java.util.Date(to.getTime() - decrement);

		command = "select count(*) from JobUsageRecord_Meta where ServerDate > " + dq + format.format(from) + dq +
		    " and ServerDate <= " + dq + format.format(to) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    count6 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		command = "select count(*) from DupRecord where EventDate > " + dq + format.format(from) + dq +
		    " and EventDate <= " + dq + format.format(to) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    error6 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		//
		// previous day
		//

		decrement = 24 * 60 * 60 * 1000;
		java.util.Date date = new java.util.Date(now.getTime() - decrement);

		command = "select count(*) from JobUsageRecord_Meta where ServerDate > " + dq + format.format(date) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    count24 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		command = "select count(*) from DupRecord where EventDate > " + dq + format.format(date) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    error24 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		//
		// previous 7 days
		//

		decrement = 7 * 24 * 60 * 60 * 1000;
		date = new java.util.Date(now.getTime() - decrement);

		command = "select count(*) from JobUsageRecord_Meta where ServerDate > " + dq + format.format(date) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    count7 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		command = "select count(*) from DupRecord where EventDate > " + dq + format.format(date) + dq;
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    error7 = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		//
		// total
		//

		command = "select count(*) from JobUsageRecord";
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    totalcount = resultSet.getInt(1);
		resultSet.close();
		statement.close();

		command = "select count(*) from DupRecord";
		statement = connection.prepareStatement(command);
		resultSet = statement.executeQuery(command);
		while(resultSet.next())
		    totalerror = resultSet.getInt(1);
		resultSet.close();
		statement.close();
	    }
	catch (Exception e)
	    {
		e.printStackTrace();
	    }

	html = xp.replaceAll(html,"#count1#","" + DisplayInt(count1));
	html = xp.replaceAll(html,"#error1#","" + DisplayInt(error1));

	html = xp.replaceAll(html,"#count2#","" + DisplayInt(count2));
	html = xp.replaceAll(html,"#error2#","" + DisplayInt(error2));

	html = xp.replaceAll(html,"#count3#","" + DisplayInt(count3));
	html = xp.replaceAll(html,"#error3#","" + DisplayInt(error3));

	html = xp.replaceAll(html,"#count4#","" + DisplayInt(count4));
	html = xp.replaceAll(html,"#error4#","" + DisplayInt(error4));

	html = xp.replaceAll(html,"#count5#","" + DisplayInt(count5));
	html = xp.replaceAll(html,"#error5#","" + DisplayInt(error5));

	html = xp.replaceAll(html,"#count6#","" + DisplayInt(count6));
	html = xp.replaceAll(html,"#error6#","" + DisplayInt(error6));

	html = xp.replaceAll(html,"#count24#","" + DisplayInt(count24));
	html = xp.replaceAll(html,"#error24#","" + DisplayInt(error24));

	html = xp.replaceAll(html,"#count7#","" + DisplayInt(count7));
	html = xp.replaceAll(html,"#error7#","" + DisplayInt(error7));

	html = xp.replaceAll(html,"#totalcount#","" + DisplayInt(totalcount));
	html = xp.replaceAll(html,"#totalerror#","" + DisplayInt(totalerror));

	int maxthreads = Integer.parseInt(props.getProperty("service.listener.threads"));
	String path = System.getProperties().getProperty("catalina.home");
	path = xp.replaceAll(path,"\\","/");

	m = p.matcher(html);
	m.find();
	String row = m.group();
	StringBuffer buffer = new StringBuffer();

	for (int i = 0; i < maxthreads; i++)
	    {
		String newrow = new String(row);
		String xpath = path + "/gratia/data/thread" + i;
		String filelist[] = xp.getFileList(xpath);
		newrow = xp.replaceAll(newrow,"#queue#","Q" + i);
		newrow = xp.replaceAll(newrow,"#queuesize#","" + filelist.length);
		buffer.append(newrow);
	    }
	html = xp.replaceAll(html,row,buffer.toString());
    }

}

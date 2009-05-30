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
import java.text.*;

public class MonitorStatus extends HttpServlet 
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
	String cr = "\n\r";

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
		String probename = null;
		String sitename = null;
		String host = null;
		String allsites = null;
		openConnection();
		
		String uriPart = request.getRequestURI();
		int slash2 = uriPart.substring(1).indexOf("/") + 1;
		uriPart = uriPart.substring(slash2);
		String queryPart = request.getQueryString();
		if (queryPart == null)
			queryPart = "";
		else
			queryPart = "?" + queryPart;

		request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);

		this.request = request;
		this.response = response;
		probename = request.getParameter("probename");
		sitename = request.getParameter("sitename");
		allsites = request.getParameter("allsites");
		host = request.getParameter("host");
      boolean xml = false;
      String xmlreq = request.getParameter("xml");
      if (xmlreq != null && xmlreq.equals("yes")) {
         xml = true;
      }
		buffer = new StringBuffer();
		if (allsites != null)
			processAllSites(xml);
		else if (probename != null)
			processProbe(probename,xml);
		else if (sitename != null)
			processSite(sitename,xml);
		else if (host != null)
			processHost(host,xml);
		else
			process(xml);
		response.setContentType("text/plain");
		response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0
		PrintWriter writer = response.getWriter();
		writer.write(buffer.toString());
		writer.flush();
		writer.close();
		closeConnection();
	}
   
   private static void append(StringBuffer buffer, String what, int thread, long value, boolean xml) 
   {
      if (xml) {
         buffer.append("<"+what+" thread="+thread+">");
         buffer.append(value);
         buffer.append("</"+what+">");
      } else {
         buffer.append(what+thread+"=");
         buffer.append(value);
         buffer.append("|");
      }
   }
   
   private static void append(StringBuffer buffer, String what, long value, boolean xml) 
   {
      if (xml) {
         buffer.append("<"+what+">");
         buffer.append(value);
         buffer.append("</"+what+">");
      } else {
         buffer.append(what+"=");
         buffer.append(value);
         buffer.append("|");
      }
   }
   
   private static void append(StringBuffer buffer, String what, String value, boolean xml) 
   {
      if (xml) {
         buffer.append("<"+what+">");
         buffer.append(value);
         buffer.append("</"+what+">");
      } else {
         buffer.append(what+"=");
         buffer.append(value);
         buffer.append("|");
      }
   }
   
   public void processProbe(String probename, boolean xml)
	{
		String command = "";
		String dq = "'";
		java.util.Date date = null;

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try
		{
			//
			// return time stamp of last probe contact
			//
			command = "select currenttime from Probe where probename = " + dq + probename + dq;
			System.out.println("command: " + command);
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);
			while(resultSet.next())
				date = resultSet.getTimestamp(1);
			resultSet.close();
			statement.close();
			if (date == null)
				append(buffer,"last-contact","never",xml);
			else
				append(buffer,"last-contact",format.format(date),xml);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void processSite(String sitename, boolean xml)
	{
		String command = "";
		String dq = "'";
		java.util.Date date = null;
		String probename = null;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try
		{
			//
			// return time stamp of last site contact
			//
			command = "select P.currenttime, P.probename from Probe P, Site T where P.active = 1 and T.SiteName = " + dq + sitename + dq + " and T.siteid = P.siteid order by currenttime desc";
			System.out.println("command: " + command);
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);
			while(resultSet.next()) {
				date = resultSet.getTimestamp(1);
				probename = resultSet.getString(2);
				if (date == null) {
					if (probename == null) {
						append(buffer,"last-contact","never",xml);
					} else {
                  append(buffer,"probename",probename,xml);
						append(buffer,"last-contact","never",xml);
					}
				} else {
					if (probename == null)
						probename = "Unknown probe";
               append(buffer,"probename",probename,xml);
               append(buffer,"last-contact",format.format(date),xml);
				}
            buffer.append("\n");
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void processAllSites(boolean xml)
	{
		String command = "";
		String site = "";
		java.util.Date date = null;
		String probename = null;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try
		{
			//
			// return time stamp of last site contact
			//
			command = "select T.SiteName, P.currenttime, P.probename from Probe P, Site T where P.active = 1 and T.siteid = P.siteid order by T.SiteName,P.currenttime desc";
			System.out.println("command: " + command);
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);
			while(resultSet.next()) {
				site = resultSet.getString(1);
				date = resultSet.getTimestamp(2);
				probename = resultSet.getString(3);
				if (probename == null)
					probename = "Unknown probe";
            append(buffer,"site",site,xml);
            append(buffer,"probename",probename,xml);
				if (date == null) {
					append(buffer,"last-contact","never",xml);
				} else {
					append(buffer,"last-contact",format.format(date),xml);
				}
            buffer.append("\n");
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	public void processHost(String host, boolean xml)
	{
		String command = "";
		String dq = "'";
		java.util.Date date = null;

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try
		{
			//
			// return time stamp of last host contact
			//
			command = "select max(ServerDate) from JobUsageRecord,JobUsageRecord_Meta where JobUsageRecord.dbid = JobUsageRecord_Meta.dbid and Host = " + dq + host + dq;
			System.out.println("command: " + command);
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);
			while(resultSet.next())
				date = resultSet.getTimestamp(1);
			resultSet.close();
			statement.close();
			if (date == null)
				append(buffer,"last-contact","never",xml);
			else
				append(buffer,"last-contact",format.format(date),xml);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void process(boolean xml)
	{
		int index = 0;
		String command = "";
		String dq = "'";

		Integer count1 = null;
		Integer error1 = null;

		Integer count24 = null;
		Integer error24 = null;

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
         
         String tables [] = { "JobUsageRecord", "MetricRecord", "ComputeElement", "StorageElement", "ComputeElementRecord", "StorageElementRecord", "Subcluster" };
         
         count1 = new Integer(0);
         
         for(int i = 0; i < tables.length; ++i) {

            command = "select count(*) from " + tables[i] + "_Meta where ServerDate > " + dq + format.format(from) + dq +
                      " and ServerDate <= " + dq + format.format(to) + dq;
            System.out.println("command: " + command);
            statement = connection.prepareStatement(command);
            resultSet = statement.executeQuery(command);
            while(resultSet.next()) {
               count1 = count1 + resultSet.getInt(1);
            }
            resultSet.close();
            statement.close();
         }
         
			command = "select count(*) from DupRecord where EventDate > " + dq + format.format(from) + dq +
			" and EventDate <= " + dq + format.format(to) + dq;
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);
			while(resultSet.next())
				error1 = resultSet.getInt(1);
			resultSet.close();
			statement.close();

			//
			// previous day
			//

			decrement = 24 * 60 * 60 * 1000;
			java.util.Date date = new java.util.Date(now.getTime() - decrement);

         count24 = new Integer(0);
         for(int i = 0; i < tables.length; ++i) {
            command = "select count(*) from " + tables[i] + "_Meta where ServerDate > " + dq + format.format(date) + dq;
            statement = connection.prepareStatement(command);
            resultSet = statement.executeQuery(command);
            while(resultSet.next())
               count24 = count24 + resultSet.getInt(1);
            resultSet.close();
            statement.close();
         }

			command = "select count(*) from DupRecord where EventDate > " + dq + format.format(date) + dq;
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);
			while(resultSet.next())
				error24 = resultSet.getInt(1);
			resultSet.close();
			statement.close();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		append(buffer,"record-count-hour",count1,xml);
		append(buffer,"record-count-24hour",count24,xml);

		int maxthreads = Integer.parseInt(props.getProperty("service.listener.threads"));
		String path = System.getProperties().getProperty("catalina.home");
		path = xp.replaceAll(path,"\\","/");
      path = path + "/gratia/data/thread";
      
		for (int i = 0; i < maxthreads; i++)
		{
			append(buffer,"queuesize",i,XP.getFileNumber(path + i),xml);
		}
	}
}

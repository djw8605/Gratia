package net.sf.gratia.administration;

import net.sf.gratia.util.XP;
import net.sf.gratia.util.Logging;
import net.sf.gratia.services.*;

import java.io.*;
import java.util.Properties;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import java.rmi.*;
import org.apache.tools.bzip2.*;
import com.ice.tar.*;

public class StopDatabase extends HttpServlet 
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
	Properties p;
	String message = null;
	//
	// support
	//
	String dq = "\"";
	String comma = ",";
	String cr = "\n";

	public JMSProxy proxy = null;

	//
	// statics for recovery thread
	//

	public static String status = "";
	public static long skipped = 0;
	public static long processed = 0;
	public static long errors = 0;
	public static boolean replayall = false;

	public void initialize()
	{
		p = net.sf.gratia.util.Configuration.getProperties();
		try
		{
			proxy = (JMSProxy) Naming.lookup(p.getProperty("service.rmi.rmilookup") +
					p.getProperty("service.rmi.service"));
		}
		catch (Exception e)
		{
			Logging.warning(xp.parseException(e));
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		initialize();
		this.request = request;
		this.response = response;

		
		String uriPart = request.getRequestURI();
		int slash2 = uriPart.substring(1).indexOf("/") + 1;
		uriPart = uriPart.substring(slash2);
		String queryPart = request.getQueryString();
		if (queryPart == null)
			queryPart = "";
		else
			queryPart = "?" + queryPart;

		request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);

		if (request.getParameter("action") != null)
		{
			if (request.getParameter("action").equals("stopDatabaseUpdateThreads"))
				stopDatabaseUpdateThreads();
			else if (request.getParameter("action").equals("startDatabaseUpdateThreads"))
				startDatabaseUpdateThreads();
		}
		setup();
		process();
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0
		PrintWriter writer = response.getWriter();
		writer.write(html);
		writer.flush();
		writer.close();
	}

	public void setup()
	{
		html = xp.get(request.getRealPath("/") + "stopGratiaDatabaseUpdateThreads.html");
	}

	public void process()
	{
		String status = "Active";

		html = xp.replaceAll(html,"#status#", StopDatabase.status);
		html = xp.replaceAll(html,"#processed#","" + StopDatabase.processed);
		html = xp.replaceAll(html,"#skipped#","" + StopDatabase.skipped);

		try
		{
			boolean flag = proxy.databaseUpdateThreadsActive();
			if (flag)
				status = "Alive";
			else
				status = "Stopped";
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		html = xp.replaceAll(html,"#threadstatus#",status);
	}

	public void stopDatabaseUpdateThreads()
	{
		try
		{
			proxy.stopDatabaseUpdateThreads();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void startDatabaseUpdateThreads()
	{
		try
		{
			proxy.startDatabaseUpdateThreads();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

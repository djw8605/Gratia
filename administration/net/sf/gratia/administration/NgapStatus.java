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

public class NgapStatus extends HttpServlet 
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
				openConnection();

				this.request = request;
				this.response = response;
				buffer = new StringBuffer();
				process();
				response.setContentType("text/html");
				response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
				response.setHeader("Pragma", "no-cache"); // HTTP 1.0
				PrintWriter writer = response.getWriter();
				writer.write(buffer.toString());
				writer.flush();
				writer.close();
				closeConnection();
		}

		public void process()
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

				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

				try
						{
								//
								// previous hour
								//

								command = "select count(*) from JobUsageRecord where ServerDate > " + dq + format.format(from) + dq +
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
								// previous day
								//

								decrement = 24 * 60 * 60 * 1000;
								java.util.Date date = new java.util.Date(now.getTime() - decrement);

								command = "select count(*) from JobUsageRecord where ServerDate > " + dq + format.format(date) + dq;
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

						}
				catch (Exception e)
						{
								e.printStackTrace();
						}

				buffer.append("record-count-hour=" + count1 + "|");
				buffer.append("record-count-24hour=" + count24 + "|");
				
				int maxthreads = Integer.parseInt(props.getProperty("service.listener.threads"));
				String path = System.getProperties().getProperty("catalina.home");
				path = xp.replaceAll(path,"\\","/");

				for (int i = 0; i < maxthreads; i++)
						{
								String xpath = path + "/gratia/data/thread" + i;
								String filelist[] = xp.getFileList(xpath);
								buffer.append("queuesize" + i + "=" + filelist.length + "|");
						}
		}

}

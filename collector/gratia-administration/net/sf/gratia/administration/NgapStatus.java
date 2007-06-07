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
				String probename = null;
				String sitename = null;
				String host = null;
				String allsites = null;
				openConnection();

				this.request = request;
				this.response = response;
				probename = request.getParameter("probename");
				sitename = request.getParameter("sitename");
				allsites = request.getParameter("allsites");
				host = request.getParameter("host");
				buffer = new StringBuffer();
				if (allsites != null)
						processAllSites();
				else if (probename != null)
						processProbe(probename);
				else if (sitename != null)
						processSite(sitename);
				else if (host != null)
						processHost(host);
				else
						process();
				response.setContentType("text/plain");
				response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
				response.setHeader("Pragma", "no-cache"); // HTTP 1.0
				PrintWriter writer = response.getWriter();
				writer.write(buffer.toString());
				writer.flush();
				writer.close();
				closeConnection();
		}

		public void processProbe(String probename)
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
										buffer.append("last-contact=never\n");
								else
										buffer.append("last-contact=" + format.format(date) + "\n");
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}

		public void processSite(String sitename)
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
														buffer.append("last-contact=never\n");
												} else {
														buffer.append(probename + ": last-contact=never\n");
												}
										} else {
												if (probename == null)
														probename = "Unknown probe";
												buffer.append(probename + ": last-contact=" + format.format(date) + "\n");
										}
								}
								resultSet.close();
								statement.close();
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}

		public void processAllSites()
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
										buffer.append(site + "|" + probename + "|");
										if (date == null) {
												buffer.append("never\n");
										} else {
												buffer.append(format.format(date) + "\n");
										}
								}
								resultSet.close();
								statement.close();
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
				
		}

		public void processHost(String host)
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
								command = "select max(ServerDate) from JobUsageRecord where Host = " + dq + host + dq;
								System.out.println("command: " + command);
								statement = connection.prepareStatement(command);
								resultSet = statement.executeQuery(command);
								while(resultSet.next())
										date = resultSet.getTimestamp(1);
								resultSet.close();
								statement.close();
								if (date == null)
										buffer.append("last-contact=never\n");
								else
										buffer.append("last-contact=" + format.format(date) + "\n");
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
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

				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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

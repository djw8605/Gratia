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
								// hour - 2
								//

								decrement = 60 * 60 * 1000;
								to = from;
								from = new java.util.Date(to.getTime() - decrement);

								command = "select count(*) from JobUsageRecord where ServerDate > " + dq + format.format(from) + dq +
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

								command = "select count(*) from JobUsageRecord where ServerDate > " + dq + format.format(from) + dq +
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

								command = "select count(*) from JobUsageRecord where ServerDate > " + dq + format.format(from) + dq +
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

								command = "select count(*) from JobUsageRecord where ServerDate > " + dq + format.format(from) + dq +
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

								command = "select count(*) from JobUsageRecord where ServerDate > " + dq + format.format(from) + dq +
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

								//
								// previous 7 days
								//

								decrement = 7 * 24 * 60 * 60 * 1000;
								date = new java.util.Date(now.getTime() - decrement);

								command = "select count(*) from JobUsageRecord where ServerDate > " + dq + format.format(date) + dq;
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
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}

				html = xp.replaceAll(html,"#count1#","" + count1.intValue());
				html = xp.replaceAll(html,"#error1#","" + error1.intValue());

				html = xp.replaceAll(html,"#count2#","" + count2.intValue());
				html = xp.replaceAll(html,"#error2#","" + error2.intValue());

				html = xp.replaceAll(html,"#count3#","" + count3.intValue());
				html = xp.replaceAll(html,"#error3#","" + error3.intValue());

				html = xp.replaceAll(html,"#count4#","" + count4.intValue());
				html = xp.replaceAll(html,"#error4#","" + error4.intValue());

				html = xp.replaceAll(html,"#count5#","" + count5.intValue());
				html = xp.replaceAll(html,"#error5#","" + error5.intValue());

				html = xp.replaceAll(html,"#count6#","" + count6.intValue());
				html = xp.replaceAll(html,"#error6#","" + error6.intValue());

				html = xp.replaceAll(html,"#count24#","" + count24.intValue());
				html = xp.replaceAll(html,"#error24#","" + error24.intValue());

				html = xp.replaceAll(html,"#count7#","" + count7.intValue());
				html = xp.replaceAll(html,"#error7#","" + error7.intValue());

				String path = System.getProperties().getProperty("catalina.home");
				path = xp.replaceAll(path,"\\","/");

				String xpath = path + "/gratia/data/thread0";
				String filelist[] = xp.getFileList(xpath);
				html = xp.replaceAll(html,"#q0#","" + filelist.length);

				xpath = path + "/gratia/data/thread1";
				filelist = xp.getFileList(xpath);
				html = xp.replaceAll(html,"#q1#","" + filelist.length);

				xpath = path + "/gratia/data/thread2";
				filelist = xp.getFileList(xpath);
				html = xp.replaceAll(html,"#q2#","" + filelist.length);
		}

}

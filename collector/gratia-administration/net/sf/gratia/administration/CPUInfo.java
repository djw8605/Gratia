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

// HK-New                                                                                                                                                                                                                                                           
import java.util.HashMap;
import java.util.Map;

//public class CPUInfo extends HttpServlet {
public class CPUInfo extends GrAdminHttpServlet {
    // moved up   
    //XP xp = new XP();
    //
    // database related
    //
    //String driver = "";
    //String url = "";
    //String user = "";
    //String password = "";
    //Connection connection;

    Statement statement;
    // moved up   
    //
    // processing related
    //
    //String html = "";
    //String row = "";
    //Pattern p = Pattern.compile("<tr>.*?</tr>",Pattern.MULTILINE + Pattern.DOTALL);
    //Matcher m = null;

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
    String newname = "<New Probe Name>";


    // moved up   
    //Hashtable table = new Hashtable();

    Hashtable sitebyid = new Hashtable();
    Hashtable sitebyname = new Hashtable();

    

    public String getPagename() {
        return "cpuinfo.html";
    }


    /*
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
    */


   public void doGet(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException {
      
      if (LoginChecker.checkLogin(request, response)) {
         openConnection();
         
         //this.request = request;
         //this.response = response;
         table = new Hashtable();
         if (request.getParameter("action") != null)
         {
            if (request.getParameter("action").equals("delete"))
		//delete();
               delete(request);
         }
         //setup();
         setup( request );
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
    /*   
   public void doPost(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException	{
      if (LoginChecker.checkLogin(request, response)) {
         openConnection();
         this.request = request;
         this.response = response;
         table = (Hashtable) request.getSession().getAttribute("table");
         update();
         closeConnection();
         response.sendRedirect("cpuinfo.html");
      }
   }
   
   public void setup() throws IOException
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
    */

    String getAndReplace(String newrow, int index, String pattern, String column, ResultSet resultSet) throws java.sql.SQLException
    {
	String value = resultSet.getString(column);
	if (value == null)
	    value = "No Data";
	else {
	    value = value.replaceAll("\"","&quot;");
	}
	newrow = newrow.replaceAll(pattern,value);
	table.put(column+":" + index,value);
	return newrow;
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
		ResultSet resultSet = statement.executeQuery(command);
		
		while(resultSet.next())
		    {
			String newrow = new String(row);
			newrow = xp.replaceAll(newrow,"#index#", "" + index);
			table.put("index:" + index,              "" + index);
			
			newrow = xp.replaceAll(newrow,"#hostid#", resultSet.getString("HostId"));
			table.put("hostid:" + index,              resultSet.getString("HostId"));
			
			newrow = getAndReplace(newrow, index,"#hostdescription#","HostDescription", resultSet);
			newrow = getAndReplace(newrow, index,"#benchmarkscore#", "BenchmarkScore",  resultSet);
			newrow = getAndReplace(newrow, index,"#cpucount#",       "CPUCount",        resultSet);
			newrow = getAndReplace(newrow, index,"#os#",             "OS",              resultSet);
			newrow = getAndReplace(newrow, index,"#osversion#",      "OSVersion",       resultSet);
			newrow = getAndReplace(newrow, index,"#cputype#",        "CPUType",         resultSet);
			
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
	/* HK Debug back to original
	newrow = xp.replace(newrow,"#benchmarkscore#", "null");
	newrow = xp.replace(newrow,"#cpucount#",       "null");
	newrow = xp.replace(newrow,"#os#",             "null");
	newrow = xp.replace(newrow,"#osversion#",      "null");
	newrow = xp.replace(newrow,"#cputype#",        "null");
	*/

	table.put("index:" + index,"" + index);
	table.put("hostdescription:" + index,newname);
      
	html = xp.replace(html,row,buffer.toString());
    }
   
    //public void update()
    public void update(HttpServletRequest request)
    {
	int index;
	String key = "";
	String oldvalue = "";
	String newvalue = "";
	
	for (index = 0; index < 1000; index++)
	    {
		key = "index:" + index;
		oldvalue = (String) table.get(key);
		//newvalue = (String) request.getParameter(key);
		newvalue = (String) nggetquerystring(request, key);
		if (oldvalue == null)
		    break;
		
		key = "hostdescription:" + index;
		oldvalue = (String) table.get(key);
		//newvalue = (String) request.getParameter(key);
		newvalue = (String) nggetquerystring(request, key);
		
		if ((oldvalue != null) && (oldvalue.equals(newname)) && (! oldvalue.equals(newvalue)))
		    {
			//insert(index);
			insert(index, request);
			continue;
		    }
		if ((oldvalue != null) && (oldvalue.equals(newvalue)))
		    break;
		
		key = "benchmarkscore:" + index;
		oldvalue = (String) table.get(key);
		//newvalue = (String) request.getParameter(key);
		newvalue = (String) nggetquerystring(request, key);
		if (! oldvalue.equals(newvalue))
		    {
			//update(index);
			update(index, request);
			continue;
		    }
		
		key = "cpucount:" + index;
		oldvalue = (String) table.get(key);
		//newvalue = (String) request.getParameter(key);
		newvalue = (String) nggetquerystring(request, key);
		if (! oldvalue.equals(newvalue))
		    {
			//update(index);
			update(index, request);
			continue;
		    }
		
		key = "os:" + index;
		oldvalue = (String) table.get(key);
		//newvalue = (String) request.getParameter(key);
		newvalue = (String) nggetquerystring(request, key);
		if (! oldvalue.equals(newvalue))
		    {
			//update(index);
			update(index, request);
			continue;
		    }
		
		key = "osversion:" + index;
		oldvalue = (String) table.get(key);
		//newvalue = (String) request.getParameter(key);
		newvalue = (String) nggetquerystring(request, key);
		if (! oldvalue.equals(newvalue))
		    {
			//update(index);
			update(index, request);
			continue;
		    }
		
		key = "cputype:" + index;
		oldvalue = (String) table.get(key);
		//newvalue = (String) request.getParameter(key);
		newvalue = (String) nggetquerystring(request, key);
		if (! oldvalue.equals(newvalue))
		    {
			//update(index);
			update(index, request);
			continue;
		    }
		
	    }
    }
    
    //public void update(int index)
    public void update(int index, HttpServletRequest request)
    {
	/*
	String hostid          = (String) request.getParameter("hostid:" + index);
	String HostDescription = (String) request.getParameter("hostdescription:" + index);
	String benchmarkscore  = (String) request.getParameter("benchmarkscore:" + index);
	String cpucount        = (String) request.getParameter("cpucount:" + index);
	String os              = (String) request.getParameter("os:" + index);
	String osversion       = (String) request.getParameter("osversion:" + index);
	String cputype         = (String) request.getParameter("cputype:" + index);
	*/
	String hostid          = (String) nggetquerystring(request, "hostid:" + index);
	String HostDescription = (String) nggetquerystring(request, "hostdescription:" + index);
	String benchmarkscore  = (String) nggetquerystring(request, "benchmarkscore:" + index);
	String cpucount        = (String) nggetquerystring(request, "cpucount:" + index);
	String os              = (String) nggetquerystring(request, "os:" + index);
	String osversion       = (String) nggetquerystring(request, "osversion:" + index);
	String cputype         = (String) nggetquerystring(request, "cputype:" + index);

	String command = "update CPUInfo set HostDescription = ?, BenchmarkScore = ?, CPUCount = ?," +
	    " OS = ?, OSVersion = ?, CPUType = ? where hostid = ?;";
	
	PreparedStatement statement = null;
	try
	    {
		statement = connection.prepareStatement(command);
		statement.setString(1, HostDescription);
		statement.setString(2, benchmarkscore);
		statement.setString(3, cpucount);
		statement.setString(4, os);
		statement.setString(5, osversion);
		statement.setString(6, cputype);
		statement.setString(7, hostid);
		statement.executeUpdate();
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
    
    //public void insert(int index)
    public void insert(int index, HttpServletRequest request)
    {
	/*
	String HostDescription = (String) request.getParameter("hostdescription:" + index);
	String benchmarkscore  = (String) request.getParameter("benchmarkscore:" + index);
	String cpucount        = (String) request.getParameter("cpucount:" + index);
	String os              = (String) request.getParameter("os:" + index);
	String osversion       = (String) request.getParameter("osversion:" + index);
	String cputype         = (String) request.getParameter("cputype:" + index);
	*/

	String HostDescription = (String) nggetquerystring(request, "hostdescription:" + index);
	String benchmarkscore  = (String) nggetquerystring(request, "benchmarkscore:" + index);
	String cpucount        = (String) nggetquerystring(request, "cpucount:" + index);
	String os              = (String) nggetquerystring(request, "os:" + index);
	String osversion       = (String) nggetquerystring(request, "osversion:" + index);
	String cputype         = (String) nggetquerystring(request, "cputype:" + index);

	String command = "insert into CPUInfo(HostDescription,BenchmarkScore,CPUCount,OS,OSVersion,CPUType)" +
	    " values(?,?,?,?,?,?);";
      
	PreparedStatement statement = null;
	try
	    {
		statement = connection.prepareStatement(command);
		statement.setString(1, HostDescription);
		statement.setString(2, benchmarkscore);
		statement.setString(3, cpucount);
		statement.setString(4, os);
		statement.setString(5, osversion);
		statement.setString(6, cputype);
		statement.executeUpdate();
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
   
    //void delete()
    void delete( HttpServletRequest request )
    {
	String command = "";
      
	try
	    {
		command = "delete from CPUInfo where hostid = ?";
		PreparedStatement statement = connection.prepareStatement(command);
		//statement.setInt(1, Integer.parseInt(request.getParameter("hostid")));
		statement.setInt( 1, Integer.parseInt( nggetquerystring(request, "hostid") ) );
		statement.executeUpdate();
		statement.close();
	    }
	catch (Exception e)
	    {
		System.out.println("command: " + command);
		e.printStackTrace();
	    }
    }

   
}

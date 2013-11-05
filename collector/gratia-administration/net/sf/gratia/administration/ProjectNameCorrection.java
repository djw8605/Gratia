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

import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;

import java.util.regex.*;

// HK-New                                                                                                                                                                                            
import java.util.HashMap;
import java.util.Map;

public class ProjectNameCorrection extends GrAdminHttpServlet {

    // moved up
    //XP xp = new XP();
    //
    // database related
    //
    // moved up
    //String driver = "";
    //String url = "";
    //String user = "";
    //String password = "";
    //Connection connection;

    Statement statement;
    ResultSet resultSet;
    //
    // processing related
    //
    // moved up
    //String html = "";
    //String row = "";
    //Pattern p = Pattern.compile("<tr>.*?</tr>",Pattern.MULTILINE + Pattern.DOTALL);
    //Matcher m = null;

    StringBuffer buffer = new StringBuffer();
    //
    // globals
    //
    boolean initialized = false;
    //
    // support
    //
    String dq = "\"";
    String comma = ",";
    String cr = "\n";

    // moved up
    //Hashtable table = new Hashtable();
    String newname = "<New VO Name>";

    public String getPagename() {
        return "projectnamecorrection.html";
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

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        if (LoginChecker.checkLogin(request, response)) {
            openConnection();
            table = new Hashtable();
            setup(request);
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
        if (LoginChecker.checkLogin(request, response)) {
            openConnection();
            table = (Hashtable) request.getSession().getAttribute("table");
            update(request);
            closeConnection();
            response.sendRedirect("projectnamecorrection.html");
        }
    }

    public void setup(HttpServletRequest request) throws IOException
    {
        html = xp.get(request.getRealPath("/") + "projectnamecorrection.html");
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

    public void process()
    {
        int index = 0;
        String command = "select ProjectNameCorrid, ProjectName, ReportableProjectName from ProjectNameCorrection order by ProjectName";
        buffer = new StringBuffer();

        try
            {
                statement = connection.prepareStatement(command);
                resultSet = statement.executeQuery(command);

                while(resultSet.next())
                    {
                        String newrow = new String(row);
                        newrow = xp.replaceAll(newrow,"#index#","" + index);
                        newrow = xp.replace(newrow,"#corrid#","" + resultSet.getInt(1));
			String pn=resultSet.getString(2);
			if (pn != null) {
			    newrow = xp.replace(newrow,"#projectname#",resultSet.getString(2));
			} else {
			    // HK hack, empty string causes a problem with my new hash table
			    //newrow = xp.replaceAll(newrow,"#projectname#","");
			    //newrow = xp.replaceAll(newrow,"#projectname#","null");
			    newrow = xp.replaceAll(newrow,"#projectname#",""); // HK back to the original
			    // I believe this is based on the assumption or fact that getParameter will return "" for this parameter which has "" assigned
			    pn="";
			}
                        newrow = xp.replace(newrow,"#reportableprojectname#",resultSet.getString(3));
                        table.put("index:" + index,"" + index);
                        table.put("corrid:" + index,resultSet.getString(1));
                        table.put("projectname:" + index,pn);
                        table.put("reportableprojectname:" + index,resultSet.getString(3));
                        index++;
                        buffer.append(newrow);
                    }
                resultSet.close();
                statement.close();
            }
        catch (Exception e)
            {
                e.printStackTrace();
            }
 
        html = xp.replace(html,row,buffer.toString());
    }

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
                key = "reportableprojectname:" + index;
                oldvalue = (String) table.get(key);
                //newvalue = (String) request.getParameter(key);
                newvalue = (String) nggetquerystring(request, key);
                if (oldvalue.equals(newvalue))
                    continue;
                update(index, request);
            }
    }

    public void update(int index, HttpServletRequest request)	{
        String command = "update ProjectNameCorrection set ReportableProjectName = ? where ProjectNameCorrid = ?;";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(command);
            //statement.setString(1, request.getParameter("reportableprojectname:" + index));
            //statement.setInt(2, Integer.parseInt(request.getParameter("corrid:" + index)));

            statement.setString(1,                nggetquerystring(request, "reportableprojectname:" + index)  );
            statement.setInt(2, Integer.parseInt( nggetquerystring(request, "corrid:" + index)   ));


            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                statement.close();
            } catch (Exception ignore) {
            }
        }
    }

}

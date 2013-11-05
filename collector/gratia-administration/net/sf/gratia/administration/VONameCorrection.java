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


// HK-New
import java.util.HashMap;
import java.util.Map;


public class VONameCorrection extends GrAdminHttpServlet {

    // moved up
    // XP xp = new XP();

    //
    // database related

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
    Hashtable vobyid = new Hashtable();
    Hashtable vobyname = new Hashtable();
    String newname = "<New VOName>";

    public String getPagename() {
	return "vonamecorrection.html";
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

    /*
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
            response.sendRedirect("vonamecorrection.html");
        }
    }

    public void setup(HttpServletRequest request) throws IOException
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
    */

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
                        if (reportablevoname != null) {
                            newrow = xp.replaceAll(newrow,"#reportablevoname#",reportablevoname);
                            table.put("reportablevoname:" + index, reportablevoname);
                        } else {
			    // HK debug May 31 2013
                            newrow = xp.replaceAll(newrow,"#reportablevoname#","");
                            table.put("reportablevoname:" + index, "");
			    // HK debug June 29 2013 back to original because GrAdminHttpServlet can now handle empty strings
                            //newrow = xp.replaceAll(newrow,"#reportablevoname#","null");
                            //table.put("reportablevoname:" + index, "null");
                        }
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
		// HK debug June 29 2013 back to original because GrAdminHttpServlet can now handle empty strings
                //newrow = xp.replace(newrow,"#reportablevoname#",newname);
                newrow = volist(index,newrow,"xxx");
                table.put("index:" + index,"" + index);
                table.put("voname:" + index,newname);
                table.put("reportablevoname:" + index, newname);
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

    public void update(HttpServletRequest request)
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
                newvalue = (String) nggetquerystring(request, key); //newvalue = (String) request.getParameter(key); //HK
                if (oldvalue == null)
                    break;

                key = "voname:" + index;
                oldvalue = (String) table.get(key);
                newvalue = (String) nggetquerystring(request, key); //newvalue = (String) request.getParameter(key); // HK

                if ((oldvalue != null) && (oldvalue.equals(newname)) && (! oldvalue.equals(newvalue)))
                    {
                        insert(index, request);
                        continue;
                    }
                if ((oldvalue != null) && (oldvalue.equals(newvalue)))
                    break;

                key = "actualname:" + index;
                oldvalue = (String) table.get(key);
                newvalue = (String) nggetquerystring(request, key); //newvalue = (String) request.getParameter(key); // HK
                if (! oldvalue.equals(newvalue))//if ( (newvalue != null) && (! oldvalue.equals(newvalue)) )
                    {
                        update(index, request);
                        continue;
                    }

                key = "reportablevoname:" + index;
                oldvalue = (String) table.get(key);
                newvalue = (String) nggetquerystring(request, key); //newvalue = (String) request.getParameter(key); // HK
                if (! oldvalue.equals(newvalue))//if ( (newvalue != null) && (! oldvalue.equals(newvalue)) )
                    {
                        update(index, request);
                        continue;
                    }

            }
    }

    public void update(int index, HttpServletRequest request)
    {
        //int corrid = Integer.parseInt(request.getParameter("corrid:" + index));
        int corrid = Integer.parseInt(nggetquerystring(request, "corrid:" + index));

        //String actualname =  request.getParameter("actualname:" + index);
        String actualname =  nggetquerystring(request, "actualname:" + index);

        int VOid = Integer.parseInt((String) vobyname.get(actualname));

        //String voname = (String) request.getParameter("voname:" + index);
        String voname = (String) nggetquerystring(request, "voname:" + index);

        //String reportablevoname = (String) request.getParameter("reportablevoname:" + index);
        String reportablevoname = (String) nggetquerystring(request, "reportablevoname:" + index);

        String command = "update VONameCorrection set VOid = ?, VOName = ?, ReportableVOName = ? where corrid = ?;";
        PreparedStatement statement = null;
        try
            {
                statement = connection.prepareStatement(command);
                statement.setInt(1, VOid);
                statement.setString(2, voname);
                if (reportablevoname == null || reportablevoname.length()==0) {
                    statement.setNull(3, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(3, reportablevoname);
                }
                statement.setInt(4, corrid);
                statement.executeUpdate();
                statement.close();
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

    public void insert(int index, HttpServletRequest request)
    {
        //String actualname = (String) request.getParameter("actualname:" + index);
        String actualname =  nggetquerystring(request, "actualname:" + index);
        int VOid = Integer.parseInt((String) vobyname.get(actualname));
        //String voname = (String) request.getParameter("voname:" + index);
        String voname = (String) nggetquerystring(request, "voname:" + index);
        //String reportablevoname = (String) request.getParameter("reportablevoname:" + index);
        String reportablevoname = (String) nggetquerystring(request, "reportablevoname:" + index);

        String command = 
            "insert into VONameCorrection (VOid,VOName,ReportableVOName) values(?,?,?);";
        PreparedStatement statement = null;
        try
            {
                statement = connection.prepareStatement(command);
                statement.setInt(1, VOid);
                statement.setString(2, voname);
                if (reportablevoname == null || reportablevoname.length()==0) {
                    statement.setNull(3, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(3, reportablevoname);
                }
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
}

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

public class Roles extends HttpServlet {
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
    Pattern p = Pattern.compile("<tr>.*?</tr>",Pattern.MULTILINE + Pattern.DOTALL);
    Matcher m = null;
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
    Hashtable table = new Hashtable();
    Hashtable sitebyid = new Hashtable();
    Hashtable sitebyname = new Hashtable();
    String newname = "<New Probe Name>";

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
            if (request.getParameter("action") != null)
                {
                    if (request.getParameter("action").equals("delete"))
                        delete(request);
                }
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
            response.sendRedirect("roles.html");
        }
    }

    public void setup(HttpServletRequest request) throws IOException
    {
        html = xp.get(request.getRealPath("/") + "roles.html");
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

        try
            {
                command = "select * from Role";
                statement = connection.prepareStatement(command);
                resultSet = statement.executeQuery(command);

                while(resultSet.next())
                    {
                        String newrow = new String(row);

                        newrow = xp.replaceAll(newrow,"#index#","" + index);
                        table.put("index:" + index,"" + index);

                        newrow = xp.replaceAll(newrow,"#roleid#",resultSet.getString("roleid"));
                        table.put("roleid:" + index,resultSet.getString("roleid"));

                        String role = resultSet.getString("role");
                        newrow = xp.replaceAll(newrow,"#role#",role);
                        table.put("role:" + index,role);

                        String subtitle = resultSet.getString("subtitle");
                        newrow = xp.replaceAll(newrow,"#subtitle#",subtitle);
                        table.put("subtitle:" + index,subtitle);

                        String where = resultSet.getString("whereclause");
                        newrow = xp.replaceAll(newrow,"#where#",where);
                        table.put("where:" + index,where);

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
        newrow = xp.replace(newrow,"#role#","New Role");
        newrow = xp.replace(newrow,"#subtitle#","");
        newrow = xp.replace(newrow,"#where#","");
        table.put("index:" + index,"" + index);
        table.put("role:" + index,"New Role");
        buffer.append(newrow);
        html = xp.replace(html,row,buffer.toString());
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
                newvalue = (String) request.getParameter(key);
                if (oldvalue == null)
                    {
                        break;
                    }

                key = "role:" + index;
                oldvalue = (String) table.get(key);
                newvalue = (String) request.getParameter(key);

                if ((oldvalue != null) && (oldvalue.equals("New Role")) &&
                    (! oldvalue.equals(newvalue))) {
                    insert(index, request);
                    continue;
                } else if ((oldvalue != null) && (oldvalue.equals("New Role"))) {
                    break;
                }

                key = "subtitle:" + index;
                oldvalue = (String) table.get(key);
                newvalue = (String) request.getParameter(key);
                if (! oldvalue.equals(newvalue))
                    {
                        update(index, request);
                        continue;
                    }

                key = "where:" + index;
                oldvalue = (String) table.get(key);
                newvalue = (String) request.getParameter(key);
                if (! oldvalue.equals(newvalue))
                    {
                        update(index, request);
                        continue;
                    }

            }
    }

    public void update(int index, HttpServletRequest request)
    {
        int roleid = Integer.parseInt(request.getParameter("roleid:" + index));
        String role = request.getParameter("role:" + index);
        String subtitle = request.getParameter("subtitle:" + index);
        String where = request.getParameter("where:" + index);

        String command = "update Role set role = ?, subtitle = ?, whereclause = ? where roleid = ?;";
        PreparedStatement statement = null;
        try
            {
                statement = connection.prepareStatement(command);
                statement.setString(1, role);
                statement.setString(2, subtitle);
                statement.setString(3, where);
                statement.setInt(4, roleid);
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

    public void insert(int index, HttpServletRequest request)
    {
        String role = request.getParameter("role:" + index);
        String subtitle = request.getParameter("subtitle:" + index);
        String where = request.getParameter("where:" + index);

        String command = "insert into Role(role,subtitle,whereclause) values(?, ?, ?);";
        PreparedStatement statement = null;
        try
            {
                statement = connection.prepareStatement(command);
                statement.setString(1, role);
                statement.setString(2, subtitle);
                statement.setString(3, where);
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

    void delete(HttpServletRequest request)
    {
        String command = "";

        try
            {
                int roleid = Integer.parseInt(request.getParameter("roleid"));
                command = "delete from Role where roleid = ?;";
                PreparedStatement statement = connection.prepareStatement(command);
                statement.setInt(1, roleid);
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

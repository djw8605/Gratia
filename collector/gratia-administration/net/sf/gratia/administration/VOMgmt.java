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

public class VOMgmt extends GrAdminHttpServlet {
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
    String newname = "<New VO Name>";

    // moved up
    //Hashtable table = new Hashtable();

    public String getPagename() {
	return "vo.html";
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
            response.sendRedirect("vo.html");
        }
    }

    public void setup(HttpServletRequest request) throws IOException
    {
        html = xp.get(request.getRealPath("/") + "vo.html");
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
        String command = "select * from VO order by VOName";
        buffer = new StringBuffer();

        try
            {
                statement = connection.prepareStatement(command);
                resultSet = statement.executeQuery(command);

                while(resultSet.next())
                    {
                        String newrow = new String(row);
                        newrow = xp.replaceAll(newrow,"#index#","" + index);
                        newrow = xp.replace(newrow,"#void#","" + resultSet.getInt(1));
                        newrow = xp.replace(newrow,"#voname#",resultSet.getString(2));
                        table.put("index:" + index,"" + index);
                        table.put("void:" + index,resultSet.getString(1));
                        table.put("voname:" + index,resultSet.getString(2));
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
        for (int j = 0; j < 5; j++)
            {
                String newrow = new String(row);
                newrow = xp.replaceAll(newrow,"#index#","" + index);
                newrow = xp.replace(newrow,"#voname#",newname);
                table.put("index:" + index,"" + index);
                table.put("voname:" + index,newname);
                index++;
                buffer.append(newrow);
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
                key = "voname:" + index;
                oldvalue = (String) table.get(key);
                //newvalue = (String) request.getParameter(key);
                newvalue = (String) nggetquerystring(request, key);

                if (oldvalue.equals(newvalue))
                    continue;
                if (oldvalue.equals(newname))
                    insert(index, request);
                else
                    update(index, request);
            }
    }

    public void update(int index, HttpServletRequest request)	{
        String command = "update VO set VOName = ? where VOid = ?;";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(command);
            //statement.setString(1, request.getParameter("voname:" + index));
	    statement.setString(1, nggetquerystring(request, "voname:" + index )  );
            //statement.setInt(2, Integer.parseInt(request.getParameter("void:" + index)));
	    statement.setInt(2,   Integer.parseInt( nggetquerystring(request, "void:" + index )       )  );
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

    public void insert(int index, HttpServletRequest request) {
        String command = "insert into VO (VOName) values(?);";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(command);
            //statement.setString(1, request.getParameter("voname:" + index));
	    statement.setString(1, nggetquerystring(request, "voname:" + index )  );
            statement.executeUpdate();
            // connection.commit();
        } catch (Exception e) {
            System.out.println("command: " + command);
            e.printStackTrace();
        } finally {
            try {
                statement.close();
            } catch (Exception ignore) {
            }
        }
    }
}

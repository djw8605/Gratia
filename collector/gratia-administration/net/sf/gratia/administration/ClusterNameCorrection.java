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

public class ClusterNameCorrection extends HttpServlet 
{

  //
  // database related
  //
  String driver = "";
  String url = "";
  String user = "";
  String password = "";
  Connection connection;
  PreparedStatement statement;
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
  String comma = ",";
  String cr = "\n";
  Hashtable table = new Hashtable();
  Hashtable clusterbyid = new Hashtable();
  Hashtable clusterbyname = new Hashtable();
  String newname = "<New VOName>";

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
          response.sendRedirect("clusternamecorrection.html");
      }
  }

  public void setup(HttpServletRequest request)
  {
    html = XP.get(request.getRealPath("/") + "clusternamecorrection.html");
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
    clusterbyid = new Hashtable();
    clusterbyname = new Hashtable();

    try
    {
      command = "select clusterid,name from Cluster";
      statement = connection.prepareStatement(command);
      resultSet = statement.executeQuery(command);

      while(resultSet.next())
      {
        String id = resultSet.getString(1);
        String name = resultSet.getString(2);
        clusterbyid.put(id,name);
        clusterbyname.put(name,id);
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
      command = "select corrid,ClusterName,type,clusterid from ClusterNameCorrection order by ClusterName";
      statement = connection.prepareStatement(command);
      resultSet = statement.executeQuery(command);

      while(resultSet.next())
      {
        String newrow = new String(row);
        String clustername = resultSet.getString(2);
        Integer type = resultSet.getInt(3);

        newrow = XP.replaceAll(newrow,"#index#","" + index);
        table.put("index:" + index,"" + index);

        newrow = XP.replaceAll(newrow,"#corrid#",resultSet.getString(1));
        table.put("corrid:" + index,resultSet.getString(1));

        newrow = XP.replaceAll(newrow,"#name#", clustername);
     
        String actualname = (String) clusterbyid.get(resultSet.getString(4));
        newrow = clusterlist(index,newrow,actualname);

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
      newrow = XP.replaceAll(newrow,"#index#","" + index);
      newrow = XP.replace(newrow,"#voname#",newname);
      newrow = clusterlist(index,newrow,"xxx");
      table.put("index:" + index,"" + index);
      table.put("voname:" + index,newname);
      index++;
      buffer.append(newrow);
    }
    html = XP.replace(html,row,buffer.toString());
  }

  public String clusterlist(int index,String input,String current)
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

    TreeSet names = new TreeSet(clusterbyname.keySet());
    for (Iterator x = names.iterator(); x.hasNext();)
    {
      String newoption = new String(option);
      String name = (String) x.next();
      newoption = XP.replaceAll(newoption,"#actualname#",name);
      if (name.equals(current))
      {
        newoption = XP.replace(newoption,"#selected#","selected='selected'");
        table.put("actualname:" + index,current);
      }
      else
        newoption = XP.replace(newoption,"#selected#","");
      buffer.append(newoption);
    }

    String temp = XP.replace(row,option,buffer.toString());
    String output = XP.replace(input,row,temp);
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

    String output = XP.replace(input,row,r);
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
      newvalue = (String) request.getParameter(key);
      if (oldvalue == null)
        break;

      key = "voname:" + index;
      oldvalue = (String) table.get(key);
      newvalue = (String) request.getParameter(key);

      if ((oldvalue != null) && (oldvalue.equals(newname)) && (! oldvalue.equals(newvalue)))
      {
          insert(index, request);
        continue;
      }
      if ((oldvalue != null) && (oldvalue.equals(newvalue)))
        break;

      key = "actualname:" + index;
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
    String corrid = (String) request.getParameter("corrid:" + index);
    String actualname = (String) request.getParameter("actualname:" + index);
    String clusterid = (String) clusterbyname.get(actualname);

    String command = 
      "update ClusterNameCorrection set" +
      " clusterid = ? where corrid=?";
    try
    {
      statement = connection.prepareStatement(command);
      statement.executeUpdate(command);
      statement.close();
      // connection.commit();
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
    String actualname = (String) request.getParameter("actualname:" + index);
    String clusterid = (String) clusterbyname.get(actualname);
    String name = (String) request.getParameter("clustername:" + index);
    String type = (String) request.getParameter("aggtype:" + index);

    String command = 
      "insert into ClusterNameCorrection (clusterid,ClusterName,type) values(?,?,?)";
      try
      {
        statement = connection.prepareStatement(command);
        statement.setString(1, clusterid);
        statement.setString(2, name);
        statement.setString(3, type);
        statement.executeUpdate();
        statement.close();
        // connection.commit();
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

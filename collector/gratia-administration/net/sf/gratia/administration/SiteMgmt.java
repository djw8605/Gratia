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

public class SiteMgmt extends GrAdminHttpServlet {
    // moved up
    //XP xp = new XP();

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
    String newname = "<New CE Name>";

    // moved up
    //Hashtable table = new Hashtable();

    public String getPagename() {
	return "site.html";
    }

    public void process()
    {
        int index = 0;
        String command = "select * from Site order by SiteName";
        buffer = new StringBuffer();

        try
            {
                statement = connection.prepareStatement(command);
                resultSet = statement.executeQuery(command);

                while(resultSet.next())
                    {
                        String newrow = new String(row);
			// HK Comments Begin
			// Following part constructs a form.html from template string(setup) by replacing "#dbid#" with real values
			// HK Comments End
                        newrow = xp.replaceAll( newrow, "#index#",  "" + index);
                        newrow = xp.replace(    newrow, "#dbid#",   "" + resultSet.getInt(1));
                        newrow = xp.replace(    newrow, "#cename#",      resultSet.getString(2));

			// HK Comments Begin
			// The following part constructs an internal HashTable which logs the current contents of the DB
			// and this Table will be compared with Query String of the POST method in doPost function
			// HK Comments End
                        table.put("index:"  + index, "" + index);
                        table.put("dbid:"   + index, resultSet.getString(1));
                        table.put("cename:" + index, resultSet.getString(2));
                        index++;
                        buffer.append(newrow);
                    } // end of while
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
		// HK Comments Begin
		// In addition, form.html and the Table will always have 5 more rows
		// Here form.html and Table still have #dbid# not replaced
		// HK Comments End
                newrow = xp.replaceAll( newrow, "#index#",  "" + index);
                newrow = xp.replace(    newrow, "#cename#", newname);

                table.put("index:"  + index,  "" + index);
                table.put("cename:" + index,  newname);

                index++;
                buffer.append(newrow);
            }

	// HK this actually constructs a response to GET method of HTTP
        html = xp.replace(html, row, buffer.toString() );
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

		// HK
                //newvalue = (String) request.getParameter(key);
		// HK, the first invocation of nggetquerystring must always invoke getInputStream to fetch the Query String
		// but if doPost does not reset hkcount, nggetquerystring will always look at the very first output of getInputStream
                newvalue = (String) nggetquerystring(request, key);
                if (oldvalue == null)                    
		    break;
                key = "cename:" + index;
                oldvalue = (String) table.get(key);
		// HK
                //newvalue = (String) request.getParameter(key);
                newvalue = (String) nggetquerystring(request, key);
		//System.out.println("hk index = " + index + " hk old bef =  " + oldvalue + " new bef = " + newvalue);
                if (oldvalue.equals(newvalue))
                    continue;
		//System.out.println("hk index = " + index + " hk old aft =  " + oldvalue + " new aft = " + newvalue);
		// HK at this point, oldvale is not equal to newvalue
                if (oldvalue.equals(newname))                    
		    insert(index, request); // HK user filled the last 5 boxes
                else                                             
		    update(index, request); // HK user modified the existing box
            }
    }

    public void update(int index, HttpServletRequest request)
    {
        String command = "update Site set SiteName = ? where siteid = ?;";
        PreparedStatement statement = null;
        try
            {
                statement = connection.prepareStatement(command);
		// HK
                //statement.setString(1, request.getParameter("cename:" + index));
                statement.setString(1, nggetquerystring(request, "cename:" + index )  );
                //statement.setInt(2, Integer.parseInt(  request.getParameter("dbid:" + index)  )  );
                statement.setInt(2,   Integer.parseInt( nggetquerystring(request, "dbid:" + index )       )  );

                statement.executeUpdate();
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
        String command = "insert into Site (SiteName) values(?);";
        PreparedStatement statement = null;
        try
            {
                statement = connection.prepareStatement(command);
		// HK
                //statement.setString(1, request.getParameter("cename:" + index));
                statement.setString(1, nggetquerystring(request, "cename:" + index )  );
                System.out.println(request.getParameter("cename:" + index));
                statement.executeUpdate();
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


    // HK called by doGet() 
    // HK This function simply constructs a template string that looks like
    //<tr><td><label>
    //<input name="cename:#index#" type="text" id="cename:#index#" value="#cename#" />
    //<input name="index:#index#" type="hidden" id="index:#index#" value="#index#" />
    //<input name="dbid:#index#" type="hidden" id="dbid:#index#" value="#dbid#" />
    //</label></td></tr>
    // public void setup(HttpServletRequest request) throws IOException


} // end of class

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

public class SiteMgmt extends HttpServlet {
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
    String newname = "<New CE Name>";

    // NG is our new prefix that stands for New Gratia
    // HK start  //private static long hkcount = 0;

    private static Map<String, String> ngmap = null;

    private String nggetquerystring (HttpServletRequest request, String key) {

        String hkanswer = "";

	if ( ngmap == null ) {

            try {
		ngmap = new HashMap<String, String>();

		String HK1 = convertStreamToString( request.getInputStream() );
		//System.out.println("HK String : " + HK1 );
		String[] HKa = HK1.split("&");
		for (String hk : HKa) {
		    String [] hktemp = hk.split("=");
		    ngmap.put( hktemp[0], hktemp[1] );
		}
            }
            catch (IOException ex){
                ex.printStackTrace();
            }
        } // end of if

        hkanswer = ngmap.get ( key );
        return hkanswer;
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return URLDecoder.decode( s.hasNext() ? s.next() : "" );
        //      return s.hasNext() ? s.next() : "";
    }
    // HK end




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
            request.getSession().setAttribute("table", table);
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
	    // HK this is critical, without this, nggetquerystring will Never again invoke getInputStream(and update the ngmap) after the very first time.
	    //hkcount = 0;
	    ngmap = null;
            update(request);
            closeConnection();
            response.sendRedirect("site.html");
        }
    }

    // HK called by doGet()
    // HK This function simply constructs a template string that looks like
    //<tr><td><label>
    //<input name="cename:#index#" type="text" id="cename:#index#" value="#cename#" />
    //<input name="index:#index#" type="hidden" id="index:#index#" value="#index#" />
    //<input name="dbid:#index#" type="hidden" id="dbid:#index#" value="#dbid#" />
    //</label></td></tr>
    public void setup(HttpServletRequest request) throws IOException
    {
        html = xp.get(request.getRealPath("/") + "site.html");
        m = p.matcher(html);
        while (m.find())
            {
                String temp = m.group();
                if (temp.indexOf("#index#") > 0)
                    {
			//System.out.println("hk row =  " + temp);
                        row = temp;     // HK String row is initialized to "";
                        break;
                    }
            }
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
			// The following part constructs a form.html from the template string(setup) by replacing "#dbid#" with real values.
			// HK Comments End

			// HK Comments Begin
			//
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

                if (oldvalue == null)                    break;


                key = "cename:" + index;
                oldvalue = (String) table.get(key);

		// HK
                //newvalue = (String) request.getParameter(key);
                newvalue = (String) nggetquerystring(request, key);

		System.out.println("hk index = " + index + " hk old bef =  " + oldvalue + " new bef = " + newvalue);

                if (oldvalue.equals(newvalue))
                    continue;

		System.out.println("hk index = " + index + " hk old aft =  " + oldvalue + " new aft = " + newvalue);

		// HK at this point, oldvale is not equal to newvalue
                if (oldvalue.equals(newname))                    insert(index, request); // HK user filled the last 5 boxes
                else                                             update(index, request); // HK user modified the existing box
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
}

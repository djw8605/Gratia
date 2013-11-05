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


public abstract class GrAdminHttpServlet extends HttpServlet {

    XP xp = new XP();

    // used by setup
    String html = "";
    String row = "";
    Pattern p = Pattern.compile("<tr>.*?</tr>",Pattern.MULTILINE + Pattern.DOTALL);
    Matcher m = null;

    // database related
    String driver = ""; // needed
    String url = "";
    String user = "";
    String password = "";

    Connection connection;

    Hashtable table = new Hashtable();


    // This is a placeholder for the name of the site page name
    // the Sub Classes such as SiteMgmt.java should implement that returns the actual name of the page
    public abstract String getPagename();


    // ngmap used by doPost
    /* ============================= */
    protected static Map<String, String> ngmap = null; // HK start  //private static long hkcount = 0;

    // used by update and insert
    /* ============================= */
    protected String nggetquerystring (HttpServletRequest request, String key) {
        String hkanswer = ""; // local
	if ( ngmap == null ) {
            try {
		ngmap = new HashMap<String, String>();

		//System.out.println("HK Query String : " + request.getQueryString()  );
		//System.out.println("HK Content type : " + request.getContentType()  );
		//System.out.println("HK Content Length : " + request.getContentLength()  );

		String hkendsuffix = "=";
		String HK1 = convertStreamToString( request.getInputStream() );
		//System.out.println("HK String : " + HK1 );
		String[] HKa = HK1.split("&");



		for (String hk : HKa) {
		    //System.out.println("HK Split String1 : " + hk );

		    if ( hk.endsWith( hkendsuffix ) ) {

			int hkpos = hk.indexOf( hkendsuffix );
			String hksubstring = hk.substring( 0, hkpos );
			ngmap.put( hksubstring, "" );

		    } else {
			String [] hktemp = hk.split( "=", 2 );

			// HK the following line, I tried, but turns out not a good idea, so I do not use
			// if ( hktemp[1] == null ) hktemp[1] = "null"; not working, in this case split does not seem to return an array but a single String
			// we have to try not to fill the table or HTML with empty value, i.e. at least should put something..

			//System.out.println("HK Split String2 : " + hktemp[0] + " and " + hktemp[1] );
			ngmap.put( hktemp[0], hktemp[1] );
		    }



		} // end of for
            }
            catch (IOException ex){
                ex.printStackTrace();
            }
        } // end of if
        hkanswer = ngmap.get ( key );
        return hkanswer;
    } // end of nggetquerystring function


    // Purely Internal
    /* ============================= */
    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return URLDecoder.decode( s.hasNext() ? s.next() : "" );   // return s.hasNext() ? s.next() : "";
    }

    /* ============================= */
    public void init(ServletConfig config) throws ServletException 
    {
    }
    /* ============================= */
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
    /* ============================= */
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


    public abstract void process();
    public abstract void update(HttpServletRequest request);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (LoginChecker.checkLogin(request, response)) {
            openConnection();
            table = new Hashtable();
            setup(request);
            process();
            response.setContentType("text/html");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Pragma", "no-cache");
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
            ngmap = null; // HK this is critical, without this, nggetquerystring will Never again invoke getInputStream(and update the ngmap) after the very first time. //hkcount = 0;
            update(request);
            closeConnection();
            //response.sendRedirect("site.html");
            response.sendRedirect( getPagename() );
        }
    }

    public void setup(HttpServletRequest request) throws IOException
    {
	// html = xp.get(request.getRealPath("/") + "site.html");
        html = xp.get(request.getRealPath("/") + getPagename() );
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



}

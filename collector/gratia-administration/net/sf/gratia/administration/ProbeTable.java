package net.sf.gratia.administration;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import net.sf.gratia.services.*;

import java.io.*;
import java.net.*;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;

import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;
import java.text.*;
import java.util.regex.*;

//public class ProbeTable extends HttpServlet {
public class ProbeTable extends GrAdminHttpServlet {
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
    //
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
    String newname = "<New Probe Name>";

    //Hashtable table = new Hashtable();
    Hashtable sitebyid   = new Hashtable();
    Hashtable sitebyname = new Hashtable();
    TreeSet   siteList   = new TreeSet();

    Boolean hibernateInitialized = false;

    public String getPagename() {
        return "probetable.html";
    }

    void initHibernate() {
        if (hibernateInitialized) return;
        Properties p = Configuration.getProperties();
	
        while (true) {
            // Wait until JMS service is up
            try { 
                JMSProxy proxy = (JMSProxy)
                    java.rmi.Naming.lookup(p.getProperty("service.rmi.rmilookup") +
                                           p.getProperty("service.rmi.service"));
            } catch (Exception e) {
                try {
                    Thread.sleep(5000);
                } catch (Exception ignore) {
                }
            }
            break;
        }
        try {
            HibernateWrapper.start();
        }
        catch (Exception e) {
            Logging.warning("Caught exception during hibernate init", e);
            return;
        }
        hibernateInitialized = true;
    }



    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Logging.initialize("administration"); // initialize logging
    }

    /*
    public void openConnection() {
        try {
            Properties p = Configuration.getProperties();
            driver = p.getProperty("service.mysql.driver");
            url = p.getProperty("service.mysql.url");
            user = p.getProperty("service.mysql.user");
            password = p.getProperty("service.mysql.password");
        }
        catch (Exception ignore) {
        }
        try {
            Class.forName(driver).newInstance();
            connection = DriverManager.getConnection(url,user,password);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            connection.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    */

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        if (LoginChecker.checkLogin(request, response)) {
            initHibernate();
            String activeFilter = null;
            openConnection();
            table = new Hashtable();
            setup(request);
            process(request);
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

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (LoginChecker.checkLogin(request, response)) {
            openConnection();
            table = (Hashtable) request.getSession().getAttribute("table");

	    ngmap = null;

            //String activeFilter = (String) request.getParameter("activeFilter");
	    String activeFilter = (String) nggetquerystring(request, "activeFilter");

            if (activeFilter == null) {
                activeFilter = "none";
            }
            update(request);
            closeConnection();
            response.sendRedirect("probetable.html?activeFilter=" + activeFilter);
        }
    }

    /*
    public void setup(HttpServletRequest request) throws IOException {
        html = xp.get(request.getRealPath("/") + "probetable.html");
        m = p.matcher(html);
        while (m.find()) {
            String temp = m.group();
            if (temp.indexOf("#index#") > 0) {
                row = temp;
                break;
            }
        }
    }
    */


    public void process() {
    }

    public void process(HttpServletRequest request) {
        int index = 0;
        String command = "";
        buffer     = new StringBuffer();
        sitebyid   = new Hashtable();
        sitebyname = new Hashtable();
        int activeFlag = 0;

        String activeFilter = (String) request.getParameter("activeFilter");
        if (activeFilter == null) {
            activeFilter = "none";
        }
        else {
            if (activeFilter.equals("active")) {
                html = html.replaceFirst("(Probe Table Administration)[^<]*(</h3>)",
                                         "$1 (active probes)$2");
                activeFlag = 1;
            } 
            else if (activeFilter.equals("inactive")) {
                html = html.replaceFirst("(Probe Table Administration)[^<]*(</h3>)",
                                         "$1 (inactive probes)$2");
                activeFlag = -1;
            }
        }
        html = html.replaceFirst("#activeFilter#", activeFilter);
        try {
            command = "select siteid,SiteName from Site order by SiteName";
            statement = connection.createStatement();
            resultSet = statement.executeQuery(command);

            while(resultSet.next()) {
                String id   = resultSet.getString(1);
                String name = resultSet.getString(2);
                sitebyid.put(id,name);
                sitebyname.put(name,id);
                siteList.add(name);
            }
            resultSet.close();
            statement.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            command = "select probeid,siteid,probename,active,reporthh,reportmm,nRecords,currenttime from Probe";
            if (activeFlag == 1) {
                command += " where active = 1";
            } 
            else if (activeFlag == -1) {
                command += " where active = 0";
            }
            command += " order by active desc, probename";
            statement = connection.createStatement();
            resultSet = statement.executeQuery(command);

            while(resultSet.next()) {
                String newrow    = new String(row);
                String probename = resultSet.getString(3);
                String siteid    = resultSet.getString(2);
                String nRecords  = resultSet.getString(7);
                if (nRecords == null) {
                    nRecords = "0";
                }
                Timestamp timestamp = resultSet.getTimestamp(8);

                newrow = xp.replaceAll(newrow,"#index#","" + index);
                table.put("index:" + index, "" + index);
                newrow = xp.replaceAll(newrow,"#dbid#",resultSet.getString(1));
                table.put("dbid:" + index, resultSet.getString(1));

                newrow = xp.replaceAll(newrow,"#probename#",probename);
                newrow = xp.replaceAll(newrow, "#nRecords#", nRecords);

                /*
                  newrow = xp.replaceAll(newrow,"#reporthh#",resultSet.getString(5));
                  table.put("reporthh:" + index,resultSet.getString(5));
                  newrow = xp.replaceAll(newrow,"#reportmm#",resultSet.getString(6));
                  table.put("reportmm:" + index,resultSet.getString(6));
                */

                String cename = (String) sitebyid.get(siteid);
                boolean updateDB = false;
                if (cename == null) {
                    Logging.warning("Probe " + probename + " has siteid " + siteid +
                                    ", which is missing from the Site table -- redirecting to Unknown");
                    cename = "Unknown";
                    updateDB = true;
                }
                newrow = celist(index,newrow,cename);

                String yesorno = "Yes";
                if (resultSet.getString(4).equals("0"))
                    yesorno = "No";

                newrow = activelist(index,newrow,yesorno);

                boolean usered = true;

                long now = (new java.util.Date()).getTime();
                long delta = 3 * 24 * 60 * 60 * 1000;
                long previous = 0;
                if (timestamp != null) {
                    previous = timestamp.getTime();
                    if ((previous + delta) > now)
                        usered = false;
                    if (yesorno.equals("No"))
                        usered = false;
                }
                if (timestamp != null) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    newrow = xp.replaceAll(newrow,"#lastcontact#",format.format(timestamp));
                }
                else {
                    newrow = xp.replaceAll(newrow,"#lastcontact#","Never");
                }
                if (usered == false)
                    newrow = xp.replaceAll(newrow,"class=\"red\"","class=\"black\"");

                buffer.append(newrow);
                if (updateDB) {
                    command =
                        "update Probe set siteid = ?, active = ? where probeid = ?;";
                    PreparedStatement statement = null;
                    try {
                        statement = connection.prepareStatement(command);
                        statement.setInt(1, Integer.parseInt((String) sitebyname.get(cename)));
                        statement.setInt(2, yesorno.equals("Yes")?1:0);
                        statement.setInt(3, Integer.parseInt(resultSet.getString(1)));
                        statement.executeUpdate();
                        statement.close();
                        // connection.commit();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        try {
                            statement.close();
                        }
                        catch (Exception ignore) {
                        }
                    } 
                } // end of if
                ++index;
            } // end of while
            resultSet.close();
            statement.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        for (int j = 0; j < 5; j++) {
            String newrow = new String(row);
            newrow = xp.replaceAll(newrow, "#lastcontact#", "");
            newrow = xp.replaceAll(newrow, "#index#",       "" + index);
            newrow = xp.replace(newrow,    "#probename#",   newname);
            newrow = xp.replace(newrow,    "#reporthh#",    "24");
            newrow = xp.replace(newrow,    "#reportmm#",    "0");
            newrow = xp.replaceAll(newrow, "#nRecords#",    "0");
            newrow = celist(index,newrow,"xxx");
            table.put("index:" + index,     "" + index);
            table.put("probename:" + index, newname);
            ++index;
            buffer.append(newrow);
        }
        html = xp.replace(html,row,buffer.toString());
	//System.out.println("HK New HTMP : " + html );
    }

    // called by process
    public String celist(int index,String input,String current) {
        Pattern p = Pattern.compile("<sel.*#cename#.*?</select>",Pattern.MULTILINE + Pattern.DOTALL);
        Matcher m = p.matcher(input);
        m.find();
        String row = m.group();

        p = Pattern.compile("<option.*</option>");
        m = p.matcher(input);
        m.find();
        String option = m.group();
        StringBuffer buffer = new StringBuffer();

        for (Iterator x = siteList.iterator(); x.hasNext();) {
            String newoption = new String(option);
            String name = (String) x.next();
            newoption = xp.replaceAll(newoption,"#cename#",name);
            if (name.equals(current)) {
                newoption = xp.replace(newoption,"#selected#","selected='selected'");
                table.put("cename:" + index,current);
            }
            else
                newoption = xp.replace(newoption,"#selected#","");
            buffer.append(newoption);
        }

        String temp = xp.replace(row,option,buffer.toString());
        String output = xp.replace(input,row,temp);
        return output;
    }

    // called by process
    public String activelist(int index,String input,String current) {
        Pattern p = Pattern.compile("<select name=\"active:.*?</select>",Pattern.MULTILINE + Pattern.DOTALL);
        Matcher m = p.matcher(input);
        m.find();
        String row = m.group();
        String r = "";

        table.put("active:" + index,current);
        if (current.equals("Yes")) {
            r = "<select name='active:" + index + "' id='active:" + index + "'>" + cr;
            r = r + "<option value='Yes' selected='selected'>Yes</option>" + cr;
            r = r + "<option value='No'>No</option>" + cr;
            r = r + "</select>" + cr;
        }
        else {
            r = "<select name='active:" + index + "' id='active:" + index + "'>" + cr;
            r = r + "<option value='No' selected='selected'>No</option>" + cr;
            r = r + "<option value='Yes'>Yes</option>" + cr;
            r = r + "</select>" + cr;
        }

        String output = xp.replace(input,row,r);
        return output;
    }

    public void update(HttpServletRequest request) {
        int index;
        String key = "";
        String oldvalue = "";
        String newvalue = "";

        for (index = 0; index < 1000; index++) {
            key = "index:" + index;
            oldvalue = (String) table.get(key);
            //newvalue = (String) request.getParameter(key); //HKMOD
	    newvalue = (String) nggetquerystring(request, key);
            if (oldvalue == null)
                break;

            key = "probename:" + index;
            oldvalue = (String) table.get(key);
            //newvalue = (String) request.getParameter(key); //HKMOD
	    newvalue = (String) nggetquerystring(request, key);
	    // HK Comment : It only allows a new name to be inserted
	    // It does not allow an existing name to be modified.
            if ((oldvalue != null) && (oldvalue.equals(newname)) && (! oldvalue.equals(newvalue))) {
                insert(index, request);
                continue;
            }
            if ((oldvalue != null) && (oldvalue.equals(newvalue)))
                break;

            key = "cename:" + index;
            oldvalue = (String) table.get(key);
            //newvalue = (String) request.getParameter(key); // HKMOD
	    newvalue = (String) nggetquerystring(request, key);
            if (! oldvalue.equals(newvalue)) {
                update(index, request);
                continue;
            }

            key = "active:" + index;
            oldvalue = (String) table.get(key);
	    //newvalue = (String) request.getParameter(key); // HKMOD
	    newvalue = (String) nggetquerystring(request, key);
            if (! oldvalue.equals(newvalue)) {
                update(index, request);
                continue;
            }

        }
    }

    public void update(int index, HttpServletRequest request) {
        //int dbid            = Integer.parseInt(request.getParameter("dbid:" + index));
        //String cename       = request.getParameter("cename:" + index);
        //String activeString = request.getParameter("active:" + index);

        int dbid            = Integer.parseInt( nggetquerystring(request,"dbid:" + index) );
        String cename       = nggetquerystring(request, "cename:" + index);
        String activeString = nggetquerystring(request, "active:" + index);

        int ceid            = Integer.parseInt((String) sitebyname.get(cename));

        int active = 0;
        if (activeString.equals("Yes"))
            active = 1;
        
        String command =  "update Probe set siteid = ?, active = ? where probeid = ?;";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(command);
            statement.setInt(1, ceid);
            statement.setInt(2, active);
            statement.setInt(3, dbid);
            statement.executeUpdate();
            statement.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                statement.close();
            }
            catch (Exception ignore) {
            }
        }
    }

    public void insert(int index, HttpServletRequest request) {
        //String cename       = (String) request.getParameter("cename:" + index);
        //String activeString = (String) request.getParameter("active:" + index);

        String cename       = nggetquerystring(request, "cename:" + index);
        String activeString = nggetquerystring(request, "active:" + index);

        int ceid            = Integer.parseInt( (String) sitebyname.get(cename) );

        int active = 0;
        if (activeString.equals("Yes"))
            active = 1;

	// HKMOD
        String probename = (String) nggetquerystring(request, "probename:" + index);

        String command = "insert into Probe (siteid,probename,active) values(?, ?, ?);";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(command);
            statement.setInt(1, ceid);
            statement.setString(2, probename);
            statement.setInt(3, active);
            statement.executeUpdate();
            statement.close();
            // connection.commit();
        }
        catch (Exception e) {
            System.out.println("command: " + command);
            e.printStackTrace();
        }
        finally {
            try {
                statement.close();
            }
            catch (Exception ignore) {
            }
        }
    }



}

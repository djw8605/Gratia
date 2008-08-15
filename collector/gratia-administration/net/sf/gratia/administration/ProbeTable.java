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

public class ProbeTable extends HttpServlet {
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
    HttpServletRequest request;
    HttpServletResponse response;
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
    TreeSet siteList = new TreeSet();

    Boolean hibernateInitialized = false;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // initialize logging
        Logging.initialize("administration");
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

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        initHibernate();
        String fqan = (String) request.getSession().getAttribute("FQAN");
        boolean login = true;
        if (fqan == null)
            login = false;
        else if (fqan.indexOf("NoPrivileges") > -1)
            login = false;

        String uriPart = request.getRequestURI();
        int slash2 = uriPart.substring(1).indexOf("/") + 1;
        uriPart = uriPart.substring(slash2);
        String queryPart = request.getQueryString();
        if (queryPart == null)
            queryPart = "";
        else
            queryPart = "?" + queryPart;

        request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);

        if (!login) {
            Properties p = Configuration.getProperties();
            String loginLink = p.getProperty("service.secure.connection") + request.getContextPath() + "/gratia-login.jsp";
            String redirectLocation = response.encodeRedirectURL(loginLink);
            response.sendRedirect(redirectLocation);
            request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);
        }
        else {
            String activeFilter = null;
            openConnection();

            this.request = request;
            this.response = response;
            table = new Hashtable();
            setup();
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

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        openConnection();
        this.request = request;
        this.response = response;
        table = (Hashtable) request.getSession().getAttribute("table");
        String activeFilter = (String) request.getParameter("activeFilter");
        if (activeFilter == null) {
            activeFilter = "none";
        }
        update();
        closeConnection();
        response.sendRedirect("probetable.html?activeFilter=" + activeFilter);
    }

    public void setup() {
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

    public void process() {
        int index = 0;
        String command = "";
        buffer = new StringBuffer();
        sitebyid = new Hashtable();
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
            statement = connection.prepareStatement(command);
            resultSet = statement.executeQuery(command);

            while(resultSet.next()) {
                String id = resultSet.getString(1);
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
            statement = connection.prepareStatement(command);
            resultSet = statement.executeQuery(command);

            while(resultSet.next()) {
                String newrow = new String(row);
                String probename = resultSet.getString(3);
                String siteid = resultSet.getString(2);
                String nRecords = resultSet.getString(7);
                if (nRecords == null) {
                    nRecords = "0";
                }
                Timestamp timestamp = resultSet.getTimestamp(8);

                newrow = xp.replaceAll(newrow,"#index#","" + index);
                table.put("index:" + index,"" + index);
                newrow = xp.replaceAll(newrow,"#dbid#",resultSet.getString(1));
                table.put("dbid:" + index,resultSet.getString(1));

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
                        "update Probe set" + cr +
                        " siteid = " + sitebyname.get(cename) + comma + cr +
                        " active = " + resultSet.getString(4) + cr +
                        " where probeid = " + resultSet.getString(1);

                    try {
                        statement = connection.createStatement();
                        statement.executeUpdate(command);
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
                }
                ++index;
            }
            resultSet.close();
            statement.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        for (int j = 0; j < 5; j++) {
            String newrow = new String(row);
            newrow = xp.replaceAll(newrow,"#lastcontact#","");
            newrow = xp.replaceAll(newrow,"#index#","" + index);
            newrow = xp.replace(newrow,"#probename#",newname);
            newrow = xp.replace(newrow,"#reporthh#","24");
            newrow = xp.replace(newrow,"#reportmm#","0");
            newrow = xp.replaceAll(newrow, "#nRecords#", "0");
            newrow = celist(index,newrow,"xxx");
            table.put("index:" + index,"" + index);
            table.put("probename:" + index,newname);
            index++;
            buffer.append(newrow);
        }
        html = xp.replace(html,row,buffer.toString());
    }

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

    public void update() {
        int index;
        String key = "";
        String oldvalue = "";
        String newvalue = "";

        /*
          Enumeration x = request.getParameterNames();
          while(x.hasMoreElements()) {
          key = (String) x.nextElement();
          String value = (String) request.getParameter(key);
          System.out.println("key: " + key + " value: " + value);
          }
        */

        for (index = 0; index < 1000; index++) {
            key = "index:" + index;
            oldvalue = (String) table.get(key);
            newvalue = (String) request.getParameter(key);
            if (oldvalue == null)
                break;

            key = "probename:" + index;
            oldvalue = (String) table.get(key);
            newvalue = (String) request.getParameter(key);

            if ((oldvalue != null) && (oldvalue.equals(newname)) && (! oldvalue.equals(newvalue))) {
                insert(index);
                continue;
            }
            if ((oldvalue != null) && (oldvalue.equals(newvalue)))
                break;

            key = "cename:" + index;
            oldvalue = (String) table.get(key);
            newvalue = (String) request.getParameter(key);
            if (! oldvalue.equals(newvalue)) {
                update(index);
                continue;
            }

            key = "active:" + index;
            oldvalue = (String) table.get(key);
            newvalue = (String) request.getParameter(key);
            if (! oldvalue.equals(newvalue)) {
                update(index);
                continue;
            }

            /*
              key = "reporthh:" + index;
              oldvalue = (String) table.get(key);
              newvalue = (String) request.getParameter(key);
              if (! oldvalue.equals(newvalue)) {
              update(index);
              continue;
              }

              key = "reportmm:" + index;
              oldvalue = (String) table.get(key);
              newvalue = (String) request.getParameter(key);
              if (! oldvalue.equals(newvalue)) {
              update(index);
              continue;
              }
            */
        }
    }

    public void update(int index) {
        String dbid = (String) request.getParameter("dbid:" + index);
        String cename = (String) request.getParameter("cename:" + index);
        String ceid = (String) sitebyname.get(cename);
        String active = (String) request.getParameter("active:" + index);
        if (active.equals("Yes"))
            active = "1";
        else
            active = "0";
        String reporthh = (String) request.getParameter("reporthh:" + index);
        String reportmm = (String) request.getParameter("reportmm:" + index);

        String command = 
            "update Probe set" + cr +
            " siteid = " + ceid + comma + cr +
            " active = " + active + cr +
            " where probeid = " + dbid;

        try {
            statement = connection.createStatement();
            statement.executeUpdate(command);
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
    }

    public void insert(int index) {
        String cename = (String) request.getParameter("cename:" + index);
        String ceid = (String) sitebyname.get(cename);
        String active = (String) request.getParameter("active:" + index);
        if (active.equals("Yes"))
            active = "1";
        else
            active = "0";
        String probename = (String) request.getParameter("probename:" + index);

        /*
          String reporthh = (String) request.getParameter("reporthh:" + index);
          String reportmm = (String) request.getParameter("reportmm:" + index);
        */

        /*
          String command = 
          "insert into Probe (siteid,probename,active,reporthh,reportmm) values(" + 
          ceid + comma + dq + probename + dq + comma + active + comma + reporthh + comma + reportmm + ")";
        */

        String command = 
            "insert into Probe (siteid,probename,active) values(" + 
            ceid + comma + dq + probename + dq + comma + active  + ")";
        try {
            statement = connection.createStatement();
            statement.executeUpdate(command);
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

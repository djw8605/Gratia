package net.sf.gratia.administration;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.sql.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;

import net.sf.gratia.services.*;
import net.sf.gratia.storage.Replication;
import net.sf.gratia.util.XP;
import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Logging;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.*;

public class ReplicationTable extends HttpServlet {
    //
    // database related
    //
    Statement statement;
    ResultSet resultSet;

    //
    // processing related
    //
    String html = "";
    String row = "";

    Pattern datarowPattern =
        Pattern.compile("<tr id=\"datarow.*?>.*#replicationid#.*?</table>.*?</tr>",
                        Pattern.MULTILINE + Pattern.DOTALL);
    Pattern updateButtonPattern =
        Pattern.compile("update:(\\d+)");
    Pattern cancelButtonPattern =
        Pattern.compile("cancel:(\\d+)");
    Matcher matcher = null;
    StringBuffer buffer = new StringBuffer();

    //
    // globals
    //
    HttpServletRequest request;
    HttpServletResponse response;
    Properties props;
    String message = null;
    Boolean modify = false;
    Boolean initialized = false;
    Hashtable<Integer,Replication> repTable = null;

    //
    // support
    //
    String sq = "'";
    String dq = "\"";
    String comma = ",";
    String cr = "\n";
    //    Hashtable table = new Hashtable();
    String newname = "<New Entry>";

    // Which Servlet/web page is this
    String Name;

    // Which Records are we replicating
    String RecordTable = "JobUsageRecord";

    public void init(ServletConfig config) throws ServletException {
        // javax.servlet.ServletConfig.getInitParameter() 
        String what = config.getInitParameter("RecordType");
        if ((what != null) && (what.length() > 0)) {
            RecordTable = what;
        }
        Name = config.getServletName();
    }

    void initialize() {
        if (initialized) return;
        while (true) {
            // Wait until JMS service is up
            try { 
                JMSProxy proxy = (JMSProxy)
                    Naming.lookup(props.getProperty("service.rmi.rmilookup") +
                                  props.getProperty("service.rmi.service"));
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
            Logging.warning("SystemAdministration: Caught exception during hibernate init", e);
        }
        props = Configuration.getProperties();
        initialized = true;
    }

    private Boolean checkNeedLogin(HttpServletRequest request,
                                   HttpServletResponse response)
        throws ServletException, IOException {
        // Check whether we need to log in.
        String fqan = (String) request.getSession().getAttribute("FQAN");
        Boolean needLogin = false;
        if ((fqan == null) || (fqan.indexOf("NoPrivileges") > -1)) {
            needLogin = true;
        }

        String uriPart = request.getRequestURI();
        int slash2 = uriPart.substring(1).indexOf("/") + 1;
        uriPart = uriPart.substring(slash2);
        String queryPart = request.getQueryString();
        if (queryPart == null)
            queryPart = "";
        else
            queryPart = "?" + queryPart;

        request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);

        if (needLogin) {
            Properties p = Configuration.getProperties();
            String loginLink = p.getProperty("service.secure.connection") + request.getContextPath() + "/gratia-login.jsp";
            String redirectLocation = response.encodeRedirectURL(loginLink);
            response.sendRedirect(redirectLocation);
        }
        return needLogin;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        if (checkNeedLogin(request, response)) return; // Redirected, nothing to do.
        setup(request, response);
        Integer selectedReplicationId = null;
        try {
            selectedReplicationId =
                new Integer(request.getParameter("replicationid"));
        }
        catch (Exception ignore) {
        }                
        Replication repEntry;
        if ((selectedReplicationId != null) &&
            ((repEntry = repTable.get(selectedReplicationId)) != null) &&
            (request.getParameter("action") != null)) {
            if (modify) {
                message = "In modify mode: update or cancel before attempting another action.";
            } else if (request.getParameter("action").equals("register")) {
                register(repEntry);
            } else if (request.getParameter("action").equals("activate")) {
                activate(repEntry);
            } else if (request.getParameter("action").equals("deactivate")) {
                deactivate(repEntry);
            } else if (request.getParameter("action").equals("reset")) {
                reset(repEntry);
            } else if (request.getParameter("action").equals("delete")) {
                delete(repEntry);
            } else if (request.getParameter("action").equals("test")) {
                test(repEntry);
            } else if (request.getParameter("action").equals("modify")) {
                modify = true;
            }
        }
        loadRepTable(); // In case of changes.
        process(selectedReplicationId);
        compileResponse();
    }

    private void compileResponse() throws IOException {
        response.setContentType("text/html");
        response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        //        request.getSession().setAttribute("table", table);
        PrintWriter writer = response.getWriter();
        //
        // cleanup message
        //
        if (message != null) {
            html = html.replace("#message#", message);
        } else {
            html = html.replace("#message#", "");
        }
        message = null;
        writer.write(html);
        writer.flush();
        writer.close();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (checkNeedLogin(request, response)) return; // Redirected, nothing to do.
        String newPar = request.getParameter("new");
        String refreshPar = request.getParameter("refresh");
        Enumeration pars = request.getParameterNames();
        Integer repEntryId = null;
        while (pars.hasMoreElements()) {
            String par = (String) pars.nextElement();
            Logging.debug("ReplicationTable: Paramater " + par + ": " +
                          request.getParameter(par));
        }
        setup(request, response);
        pars = request.getParameterNames();
        while (pars.hasMoreElements()) {
            String par = (String) pars.nextElement();
            matcher = cancelButtonPattern.matcher(par);
            if (matcher.matches()) {
                // Cancel
                modify = false;
            }
            matcher = updateButtonPattern.matcher(par);
            if (matcher.matches()) { // Want update
                try {
                    repEntryId = new Integer(matcher.group(1));
                } catch (Exception e) {
                    Logging.warning("ReplicationTable: caught exception while preparing for update",
                                    e);
                }
                Replication repEntry;

                if (repEntryId == 0) { // New entry
                    repEntry = new Replication(RecordTable);
                    repTable.put(repEntry.getreplicationid(), repEntry);
                } else {
                    repEntry = repTable.get(repEntryId);
                    if (repEntry == null) {
                        Logging.warning("Unable to find replication entry ID " +
                                        repEntryId + " in table for update");
                    }
                }
                if (repEntry != null) {
                    Logging.debug("ReplicationTable: matched for update: replicationid " +
                                  repEntryId);
                    modify = false;
                    update(repEntry);
                    loadRepTable(); // To see change
                }
            }
        }
        if ((newPar != null) && (newPar.length()>0)) {
            // New Entry
            Replication repEntry = new Replication(RecordTable);
            // Place in repTable
            repTable.put(repEntry.getreplicationid(), repEntry);
            // Set up to modify this new line;
            modify = true;
        }
        if (repEntryId == null) {
            process(0);
        } else {
            process(repEntryId);
        }
        compileResponse();
    }

    void setup(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        // Once-only init
        initialize();

        // For other routines to see.
        this.request = request;
        this.response = response;

        // Slurp page template
        html = XP.get(request.getRealPath("/") + "replicationtable.html");
        matcher = datarowPattern.matcher(html);
        while (matcher.find()) {
            String temp = matcher.group();
            if (temp.indexOf("#replicationid#") > 0) {
                row = temp;
                break;
            }
        }

        // Load up with replication entries from the DB.
        loadRepTable();
    }

    void loadRepTable() {
        // Load replication entries from DB
        Session session;
        session = HibernateWrapper.getSession();
        Query rq = session.createQuery("select record from Replication record " +
                                       "where record.recordtable = " +
                                       sq + RecordTable + sq +
                                       " order by record.replicationid");
        List records = rq.list();
        session.close();

        // Load hash table with entries
        repTable = new Hashtable<Integer, Replication>();
        for ( Object listEntry : records ) {
            Replication repEntry = (Replication) listEntry;
            Logging.debug("Replication: loaded entry " + repEntry.getreplicationid());
            repTable.put(new Integer(repEntry.getreplicationid()),
                         repEntry);
        }
    }

    void process(Integer selectedReplicationId) {
        Logging.debug("ReplicationTable: received selectedReplicationId " +
                      selectedReplicationId);
        Logging.debug("ReplicationTable: modify = " + modify);
        buffer = new StringBuffer();
        // Loop through replication table entries.
        Enumeration<Replication> repEntries = repTable.elements();
        Vector<Replication> vec = new Vector<Replication>();
        while (repEntries.hasMoreElements()) {
            vec.add((Replication) repEntries.nextElement());
        }
        Collections.sort(vec); // Sort according to Replication.CompareTo().
        for ( Replication repEntry : vec ) {
            Boolean modifyThisEntry = false;
            if ((modify == true) &&
                (selectedReplicationId != null) &&
                (repEntry.getreplicationid() == selectedReplicationId.intValue())) {
                Logging.debug("ReplicationTable: request to modify replication Id "
                              + selectedReplicationId);
                if (repEntry.getrunning() > 0) {
                    message = "Cowardly refusing to modify a running replication entry: stop first.";
                    modify = false;
                } else {
                    modifyThisEntry = true;
                }
            }            
            if (repEntry.getreplicationid() != 0) { // Skip new entry
                buffer.append(process(repEntry, modifyThisEntry));
            }
        }
        Replication newEntry;
        if ((newEntry = repTable.get(0)) != null) { // Deal with new entry
            buffer.append(process(newEntry, modify));
        }
        html = html.replace(row, buffer.toString());
        html = html.replace("#recordtable#", RecordTable);
    }

    String process(Replication repEntry, Boolean modifyThisEntry) {
        // Start updating the row.
        String newrow = new String(row);

        // Simple or editable row?
        if (modifyThisEntry) {
            Logging.debug("ReplicationTable: current object state:\n" +
                          "  openconnection: "  + repEntry.getopenconnection() + "\n" + 
                          "  secureconnection: "  + repEntry.getsecureconnection() + "\n" + 
                          "  registered: "  + repEntry.getregistered() + "\n" + 
                          "  running: "  + repEntry.getrunning() + "\n" + 
                          "  security: "  + repEntry.getsecurity() + "\n" + 
                          "  probename: "  + repEntry.getprobename() + "\n" + 
                          "  frequency: "  + repEntry.getfrequency() + "\n" + 
                          "  dbid: "  + repEntry.getdbid() + "\n" + 
                          "  rowcount: "  + repEntry.getrowcount() + "\n" + 
                          "  bundleSize: "  + repEntry.getbundleSize());
            // This line should be editable;
            newrow = newrow
                .replaceAll("#openconnection#",
                            "<input id=\"openconnection:#replicationid#\" " +
                            "name=\"openconnection:#replicationid#\" " +
                            "type=\"text\" value=\"" +
                            repEntry.getopenconnection() +
                            "\" size=\"35\" maxlength=\"120\" />")
                .replaceAll("#registered#",
                            (repEntry.getregistered() == 0)?"No":"Yes")
                .replaceAll("#running#",
                            (repEntry.getrunning() == 0)?"No":"Yes")
                .replaceAll("#security#",
                            "<select id=\"security:#replicationid#\" name=\"security:#replicationid#\">\n" +
                            ((repEntry.getsecurity() == 0)?
                             ("  <option>Yes</option>\n" +
                              "  <option selected=\"selected\">No</option>"):
                             ("  <option selected=\"selected\">Yes</option>\n" +
                              "  <option>No</option>")) +
                            "</select>")
                .replaceAll("#probename#", probeList(repEntry.getprobename()))
                .replaceAll("#frequency#", 
                            "<input name=\"frequency:#replicationid#\" " +
                            "type=\"text\" value=\"" +
                            Integer.toString(repEntry.getfrequency()) +
                            "\" size=\"3\" maxlength=\"4\" />")
                .replaceAll("#dbid#", 
                            "<input name=\"dbid:#replicationid#\" " +
                            "type=\"text\" value=\"" +
                            Integer.toString(repEntry.getdbid()) +
                            "\" size=\"8\" maxlength=\"10\" />")
                .replaceAll("#rowcount#", Integer.toString(repEntry.getrowcount()))
                .replaceAll("#bundleSize#", 
                            "<input name=\"bundleSize:#replicationid#\" " +
                            "type=\"text\" value=\"" +
                            Integer.toString(repEntry.getbundleSize()) +
                            "\" size=\"3\" maxlength=\"4\" />")
                .replaceAll("#modifyupdate#",
                            "<td style=\"text-align: center\">" +
                            "<input type=\"submit\" name=\"update:#replicationid#\" value=\"Update\"/>" +
                            "</td>" +
                            "<td style=\"text-align: center\">" +
                            "<input type=\"submit\" name=\"cancel:#replicationid#\" value=\"Cancel\"/>" +
                            "</td>")
                .replaceAll("#webpagename#", Name)
                .replaceAll("#replicationid#",
                            Integer.toString(repEntry.getreplicationid()));
        } else { // Simple
            newrow = newrow
                .replaceAll("#openconnection#",
                            repEntry.getopenconnection())
                .replaceAll("#registered#",
                            (repEntry.getregistered() == 0)?"No":"Yes")
                .replaceAll("#running#",
                            (repEntry.getrunning() == 0)?"No":"Yes")
                .replaceAll("#frequency#",
                            Integer.toString(repEntry.getfrequency()))
                .replaceAll("#dbid#",
                            Integer.toString(repEntry.getdbid()))
                .replaceAll("#rowcount#",
                            Integer.toString(repEntry.getrowcount()))
                .replaceAll("#security#",
                            (repEntry.getsecurity() == 0)?"No":"Yes")
                .replaceAll("#bundleSize#",
                            Integer.toString(repEntry.getbundleSize()))
                .replaceAll("#probename#", repEntry.getprobename())
                .replaceAll("#modifyupdate#",
                            "<td colspan=\"2\" " +
                            "style=\"text-align: center\">" +
                            "<a href=\"#webpagename#.html?" +
                            "action=modify&amp;replicationid=" +
                            "#replicationid#\">Modify</a></td>")
                .replaceAll("#webpagename#", Name)
                .replaceAll("#replicationid#",
                            Integer.toString(repEntry.getreplicationid()));
        }
        return newrow;
    }

    String probeList(String currentProbe) {
        StringBuffer buffer = new StringBuffer();
        Vector<String> probelist = new Vector<String>();
        probelist.add("All"); // Must have this.

        Session session = HibernateWrapper.getSession();
        
        Query q =
            session.createSQLQuery("select distinct ProbeName from " +
                                   RecordTable + "_Meta order by ProbeName");
        for (Object probeName : q.list()) {
            probelist.add("Probe:" + (String) probeName);
        }
        if (RecordTable.equals("JobUsageRecord")) {
            q = session.createSQLQuery("select distinct(VO.VOName) from VO " + 
                                       "join VONameCorrection VC on " +
                                       "(VO.void = VC.void) order by VO.VOName");
            for (Object voName : q.list()) {
                probelist.add("VO:" + (String) voName);
            }
        }
        session.close();

        buffer.append("<select name=\"probename:#replicationid#\">\n");
        for (String probe : probelist) {
            buffer.append("  <option");
            if (probe.equals(currentProbe)) {
                buffer.append(" selected=\"selected\"");
            }
            buffer.append(">" + probe + "</option>\n");
        }
        buffer.append("</select>\n");
        return buffer.toString();
    }

    void register(Replication repEntry) {
        if (repEntry.getsecurity() == 0) return;
        // Retrieve PEM
        Post post = new Post(props.getProperty("service.open.connection")
                        + "/gratia-security/security", "get");
        String response = post.send(true);
        if (!post.success) {
            message = post.errorMsg;
            return;
        }
        String[] results = split(response, ":");
        String myPEM = results[1];

        // Register with host
        post = new Post(repEntry.getopenconnection() +
                        "/gratia-security/security", "put");
        post.add("arg1", "Client:" + 
                 props.getProperty("service.open.connection"));
        post.add("arg2", "Replication");
        post.add("arg3", myPEM);
        response = post.send(true);
        if (!post.success) {
            message = post.errorMsg;
            return;
        }
        results = split(response, ":");
        if (!results[0].equals("ok")) {
            message = "ERROR registering with remote host: " + response;
            Logging.warning("ReplicationTable: " + message);
        }

        // Get host's PEM
        post = new Post(repEntry.getopenconnection() +
                        "/gratia-security/security", "get");
        response = post.send(true);
        if (!post.success) {
            message = post.errorMsg;
            return;
        }
        results = split(response, ":");
        String remotePEM = results[1];
        String secureconnection = URLDecoder.decode(results[2]);

        // Register with ourselves
        post = new Post(props.getProperty("service.open.connection") + "/gratia-security/security", "put");
        post.add("arg1", "Server:" + secureconnection);
        post.add("arg2", "Replication");
        post.add("arg3", remotePEM);
        response = post.send(true);
        if (!post.success) {
            message = post.errorMsg;
            return;
        }
        results = split(response, ":");
        if (!results[0].equals("ok")) {
            message = "ERROR registering with ourselves: " + response;
            Logging.warning("ReplicationTable: " + message);
        }

        repEntry.setsecureconnection(secureconnection);
        repEntry.setregistered(1);
        commitUpdate(repEntry);
    }

    void activate(Replication repEntry) {
        repEntry.setrunning(1);
        commitUpdate(repEntry);
    }

    void deactivate(Replication repEntry) {
        repEntry.setrunning(0);
        commitUpdate(repEntry);
    }

    void reset(Replication repEntry) {
        repEntry.setdbid(0);
        repEntry.setrowcount(0);
        commitUpdate(repEntry);
    }

    void delete(Replication repEntry) {
        Session session = null;
        try {
            session = HibernateWrapper.getSession();
            Transaction tx = session.beginTransaction();
            session.delete(repEntry);
            tx.commit();
        }
        catch (Exception e) {
            if ((session != null) && (session.isOpen())) {
                Transaction tx = session.getTransaction();
                if (tx != null) tx.rollback();
                session.close();
            }
            message = "ERROR deleting replication entry " +
                repEntry.getreplicationid() +
                " from DB";
            Logging.warning("ReplicationTable: " + message);
            Logging.debug("ReplicationTable: exception details:", e);
        }
    }

    void test(Replication repEntry) {
        String target =
            ((repEntry.getsecurity() == 1)?
             repEntry.getsecureconnection():
             repEntry.getopenconnection()) +
            "/gratia-servlets/rmi";
        String response = "";
        Post post = new Post(target, "update", "xxx");
        try {
            response = post.send(true);
        }
        catch (Exception e) {
            message = "Error for " + target + " : " + e;
            return;
        }
        if (!post.success) {
            message = "Error for " + target + " : " + post.errorMsg;
            return;
        }
        try {
            String[] results = split(response, ":");
            if (!results[0].equals("OK")) {
                message = "Error for " + target + " : " + response;
                return;
            }
        } 
        catch (Exception e) {
            message = "Error for " + target + " : " + e;
            return;
        }
        message = "Test Succeeded !!";
    }

    void update(Replication repEntry) {
        int repEntryId = repEntry.getreplicationid();
        Logging.debug("ReplicationTable: updating replicationId " +
                      repEntryId);
        if (repEntryId == 0) {
            Logging.debug("Replication table: new entry!");
        }

        // openconnection
        String key = "openconnection:" + repEntryId;
        String newValue = request.getParameter(key);
        if (!repEntry.getopenconnection().equals(newValue)) {
            repEntry.setopenconnection(newValue);
        }

        // security
        int newValueInt = 0;
        Boolean successfulConversion = false;
        key = "security:" + repEntryId;
        try {
            newValueInt = Integer.parseInt(request.getParameter(key));
            successfulConversion = true;
        } catch (Exception e) {
            successfulConversion = false;
        }
        if (successfulConversion && (repEntry.getsecurity() != newValueInt)) {
            repEntry.setsecurity(newValueInt);
        }

        // probename
        key = "probename:" + repEntryId;
        newValue = request.getParameter(key);
        if (!repEntry.getprobename().equals(newValue)) {
            repEntry.setprobename(newValue);
        }

        // frequency
        key = "frequency:" + repEntryId;
        try {
            newValueInt = Integer.parseInt(request.getParameter(key));
            successfulConversion = true;
        } catch (Exception e) {
            successfulConversion = false;
        }
        if (successfulConversion && (repEntry.getfrequency() != newValueInt)) {
            repEntry.setfrequency(newValueInt);
        }

        // dbid
        key = "dbid:" + repEntryId;
        try {
            newValueInt = Integer.parseInt(request.getParameter(key));
            successfulConversion = true;
        } catch (Exception e) {
            successfulConversion = false;
        }
        if (successfulConversion && (repEntry.getdbid() != newValueInt)) {
            repEntry.setdbid(newValueInt);
        }

        // bundleSize
        key = "bundleSize:" + repEntryId;
        try {
            newValueInt = Integer.parseInt(request.getParameter(key));
            successfulConversion = true;
        } catch (Exception e) {
            successfulConversion = false;
        }
        if (successfulConversion && (repEntry.getbundleSize() != newValueInt)) {
            repEntry.setbundleSize(newValueInt);
        }

        commitUpdate(repEntry);
    }

    void commitUpdate(Replication repEntry) {
        // Update record in DB
        Session session = null;
        try {
            session = HibernateWrapper.getSession();
            Transaction tx = session.beginTransaction();
            session.saveOrUpdate(repEntry);
            tx.commit();
        }
        catch (Exception e) {
            if ((session != null) && (session.isOpen())) {
                Transaction tx = session.getTransaction();
                if (tx != null) tx.rollback();
                session.close();
            }
            message = "ERROR updating replication entry " +
                repEntry.getreplicationid() +
                " in DB";
            Logging.warning("ReplicationTable: " + message);
            Logging.debug("ReplicationTable: exception details:", e);
        }
    }

    public String[] split(String input, String sep) {
        Vector vector = new Vector();
        StringTokenizer st = new StringTokenizer(input, sep);
        while (st.hasMoreTokens())
            vector.add(st.nextToken());
        String[] results = new String[vector.size()];
        for (int i = 0; i < vector.size(); i++)
            results[i] = (String)vector.elementAt(i);
        return results;
    }
}

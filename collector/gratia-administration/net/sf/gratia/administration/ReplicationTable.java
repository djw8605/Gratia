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
    // processing related
    //
    String fHtml = "";
    String fRow = "";
   
    Pattern fDatarowPattern =
        Pattern.compile("<tr id=\"datarow.*?>.*#replicationid#.*?</table>.*?</tr>",
                        Pattern.MULTILINE + Pattern.DOTALL);
    Pattern fUpdateButtonPattern = Pattern.compile("update:(\\d+)");
    Pattern fCancelButtonPattern = Pattern.compile("cancel:(\\d+)");
    Matcher fMatcher = null;
   
    //
    // globals
    //
    Properties fProperties;
    String fMessage = null;
    Boolean fModify = false;
    Boolean fInitialized = false;
    Hashtable<Integer,Replication> fRepTable = null;
    Boolean fDBOK = true;

    // Configuration
    String fCollectorPem;

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
    String fApplicationURL = "replicationtable.html";
   
    public void init(ServletConfig config) throws ServletException {
        // javax.servlet.ServletConfig.getInitParameter() 
        String what = config.getInitParameter("RecordType");
        if ((what != null) && (what.length() > 0)) {
            RecordTable = what;
            fApplicationURL = RecordTable.toLowerCase() + "replicationtable.html";
        }
        Name = config.getServletName();
    }
   
    void initialize() throws IOException {
        if (fInitialized) return;
        fProperties = Configuration.getProperties();
        while (true) {
            // Wait until JMS service is up
            try { 
                JMSProxy proxy = (JMSProxy)
                    Naming.lookup(fProperties.getProperty("service.rmi.rmilookup") +
                                  fProperties.getProperty("service.rmi.service"));
            } catch (Exception e) {
                try {
                    Thread.sleep(5000);
                } catch (Exception ignore) {
                }
            }
            break;
        }
        String certfile = fProperties.getProperty("service.vdt.cert.file");
        fCollectorPem = XP.get(certfile);

        fInitialized = true;
    }
   
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        if (LoginChecker.checkLogin(request, response)) {
            setup(request);
            Integer selectedReplicationId = null;
            try {
                selectedReplicationId = new Integer(request.getParameter("replicationid"));
            }
            catch (Exception ignore) {
            }                
            Replication repEntry;
            if ((selectedReplicationId != null) &&
                (fRepTable != null) &&
                ((repEntry = fRepTable.get(selectedReplicationId)) != null) &&
                (request.getParameter("action") != null)) {
                if (fModify) {
                    fMessage = "In modify mode: update or cancel before attempting another action.";
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
                    fModify = true;
                }
            }
            if (fRepTable != null) {
                loadRepTable(); // In case of changes.
                process(selectedReplicationId);
            }
            compileResponse(response);
        }
    }
   
    private void compileResponse(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        //        request.getSession().setAttribute("table", table);
        PrintWriter writer = response.getWriter();
        //
        // cleanup message
        //
        if (fMessage != null) {
            fHtml = fHtml.replace("#message#", fMessage);
        } else {
            fHtml = fHtml.replace("#message#", "");
        }

        fMessage = null;
        writer.write(fHtml);
        writer.flush();
        writer.close();
    }
   
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        if (LoginChecker.checkLogin(request, response)) {
            Enumeration pars = request.getParameterNames();
            while (pars.hasMoreElements()) {
                String par = (String) pars.nextElement();
                Logging.debug("ReplicationTable: Post Parameter " + par + " : " + request.getParameter(par));
            }
                      

            setup(request);

            String action = request.getParameter("action");
      
            if (action==null || action.equals("Refresh") || action.equals("Cancel")) {
                fModify = false;
                loadRepTable(); // In case of changes.
                process(0);
                compileResponse(response);
            } else {
                Integer selectedReplicationId = null;
                Replication repEntry = null;
                try {
                    selectedReplicationId = new Integer(request.getParameter("replicationid"));
                    repEntry = fRepTable.get(selectedReplicationId);
                }
                catch (Exception ignore) {
                }
                if (repEntry != null) {
                    if (action.equals("Update")) {
                        Logging.debug("ReplicationTable: matched for update: replicationid " + selectedReplicationId);
                        if (selectedReplicationId == 0) { // New entry
                            repEntry = new Replication(RecordTable);
                            fRepTable.put(repEntry.getreplicationid(), repEntry);
                        }
                        fModify = false;
                        update(repEntry, request);
                    } else if (fModify) {
                        fMessage = "In modify mode: update or cancel before attempting another action.";
                    } else if (action.equals("Register")) {
                        register(repEntry);
                    } else if (action.equals("Start")) {
                        activate(repEntry);
                    } else if (action.equals("Stop")) {
                        deactivate(repEntry);
                    } else if (action.equals("Reset")) {
                        reset(repEntry);
                    } else if (action.equals("Delete")) {
                        delete(repEntry);
                    } else if (action.equals("Test")) {
                        test(repEntry);
                    } else if (action.equals("Modify")) {
                        fModify = true;
                    } else {
                        // Unknown action
                        fMessage = "Error: unknown action ("+action+") has been requested.";
                    }
                    loadRepTable(); // In case of changes.
                    process(selectedReplicationId);
                    compileResponse(response);
                } else {
                    if (action.equals("New Entry")) {
                        repEntry = new Replication(RecordTable);
                        // Place in fRepTable
                        fRepTable.put(repEntry.getreplicationid(), repEntry);
                        // Set up to modify this new line;
                        fModify = true;
               
                        process(0);
                        compileResponse(response);            
                    } else if (action.equals("Update")) {
                        Logging.debug("ReplicationTable: matched for update: replicationid " + selectedReplicationId);
                        if (selectedReplicationId == null || selectedReplicationId !=0) {
                            Logging.warning("Unable to find replication entry ID " +
                                            selectedReplicationId + " in table for update");
                        } else {
                            repEntry = new Replication(RecordTable);
                            fRepTable.put(repEntry.getreplicationid(), repEntry);                  
                            update(repEntry, request);
                            loadRepTable(); // To see change
                        }
                        fModify = false;
                        process(0);
                        compileResponse(response);            
                    } else {
                        // Unknown action
                        fMessage = "Error: Unexpected action ("+action+") has been requested with a correct replication record ("+selectedReplicationId+").";
                    }
            
                }
            }
        }
    }
   
    void setup(HttpServletRequest request)
        throws ServletException, IOException {
        // Once-only init
        initialize();
        fDBOK = true; // Default state
        // Slurp page template
        fHtml = XP.get(request.getRealPath("/") + "replicationtable.html");
        fMatcher = fDatarowPattern.matcher(fHtml);
        while (fMatcher.find()) {
            String temp = fMatcher.group();
            if (temp.indexOf("#replicationid#") > 0) {
                fRow = temp;
                break;
            }
        }
        try {
            HibernateWrapper.start();
        }
        catch (Exception e) {
            Logging.warning("SystemAdministration: Caught exception during hibernate init" + e.getMessage());
            Logging.debug("Exception details: ", e);
        }      
        // Load up with replication entries from the DB.
        loadRepTable();
    }
   
    void loadRepTable() {
        if (!fDBOK) return; // Don't try.
        // Load replication entries from DB
        Session session = null;
        List records;
        try {
            session = HibernateWrapper.getCheckedSession();
            Query rq = session.createQuery("select record from Replication record " +
                                           "where record.recordtable = " +
                                           sq + RecordTable + sq +
                                           " order by record.replicationid");
            records = rq.list();
            session.close();
        } catch (Exception e) {
            if (HibernateWrapper.isFullyConnected(session)) {
                session.close();
            }
            fMessage = "Failed to load replication information from DB. Try reload.";
            fDBOK = false;
            return;
        }
        // Load hash table with entries
        fRepTable = new Hashtable<Integer, Replication>();
        for ( Object listEntry : records ) {
            Replication repEntry = (Replication) listEntry;
            Logging.debug("Replication: loaded entry " + repEntry.getreplicationid());
            fRepTable.put(new Integer(repEntry.getreplicationid()),
                          repEntry);
        }
    }
   
    String Disabled(boolean needed) {
        if (needed) {
            return " disabled=\"disabled\" ";
        } else {
            return "";
        }
    }
   
    void process(Integer selectedReplicationId) {
        Logging.debug("ReplicationTable: received selectedReplicationId " +
                      selectedReplicationId);
        Logging.debug("ReplicationTable: modify = " + fModify);
        StringBuffer buffer = new StringBuffer();
        // Loop through replication table entries.
        Enumeration<Replication> repEntries = fRepTable.elements();
        Vector<Replication> vec = new Vector<Replication>();
        while (repEntries.hasMoreElements()) {
            vec.add((Replication) repEntries.nextElement());
        }
        Collections.sort(vec); // Sort according to Replication.CompareTo().
        boolean modifyingEntry = false;
      
        for ( Replication repEntry : vec ) {
            Boolean modifyThisEntry = false;
            if ((fModify == true) &&
                (selectedReplicationId != null) &&
                (repEntry.getreplicationid() == selectedReplicationId.intValue())) {
                Logging.debug("ReplicationTable: request to modify replication Id "
                              + selectedReplicationId);
                if (repEntry.getrunning() > 0) {
                    fMessage = "Cowardly refusing to modify a running replication entry: stop first.";
                    fModify = false;
                } else {
                    modifyThisEntry = true;
                    modifyingEntry = true;
                }
            }            
            if (repEntry.getreplicationid() != 0) { // Skip new entry
                buffer.append(process(repEntry, modifyThisEntry));
            }
        }
        Replication newEntry;
        if ((newEntry = fRepTable.get(0)) != null) { // Deal with new entry
            buffer.append(process(newEntry, fModify));
        }
        if (modifyingEntry) {
            // Disable the refresh and new entry button
            fHtml = fHtml.replace("value=\"Refresh\"", "value=\"Refresh\" disabled=\"disabled\"");
            fHtml = fHtml.replace("value=\"New Entry\"", "value=\"Refresh\" disabled=\"disabled\"");
        }
        fHtml = fHtml.replace(fRow, buffer.toString());
        fHtml = fHtml.replace("#recordtable#", RecordTable)
            .replaceAll("#applicationurl#",
                        fApplicationURL);
    }
   
    String process(Replication repEntry, Boolean modifyThisEntry) {
        // Start updating the row.
        String newrow = new String(fRow);
      
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
                .replaceAll("#actionblock#",
                            "<tr><td style=\"text-align: center\">" +
                            "<input type=\"submit\" name=\"action\" value=\"Update\"/>" +
                            "</td>" +
                            "<td style=\"text-align: center\">" +
                            "<input type=\"submit\" name=\"action\" value=\"Cancel\"/>" +
                            "</td></tr>")
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
                .replaceAll("#probename#", repEntry.getprobename());
            if (fModify) {
                newrow = newrow.replaceAll("#actionblock#",
                                           "          <tr>\n" +
                                           "            <td style=\"text-align: center\"><input type=\"submit\" "+Disabled(true)+" name=\"action\" value=\"Register\"/>\n" +
                                           "            <td style=\"text-align: center\"><input type=\"submit\" "+Disabled(true)+" name=\"action\" value=\"Test\"/>\n" +
                                           "          </tr>\n" +
                                           "          <tr>\n" +
                                           "            <td style=\"text-align: center\"><input type=\"submit\" "+Disabled(true)+" name=\"action\" value=\"Start\"/>\n" +
                                           "            <td style=\"text-align: center\"><input type=\"submit\" "+Disabled(true)+" name=\"action\" value=\"Stop\"/>\n" +
                                           "          </tr>\n" +
                                           "          <tr>\n" +
                                           "            <td style=\"text-align: center\"><input type=\"submit\" "+Disabled(true)+" name=\"action\" value=\"Reset\"/>\n" +
                                           "            <td style=\"text-align: center\"><input type=\"submit\" "+Disabled(true)+" name=\"action\" value=\"Delete\"/>\n" +
                                           "          </tr>\n" +
                                           "          <tr><td colspan=\"2\" style=\"text-align: center\">" +
                                           "              <input type=\"submit\" "+Disabled(true)+"name=\"action\" value=\"Modify\"/></td></tr>");
            } else {
                newrow = newrow.replaceAll("#actionblock#",
                                           "          <tr>\n" +
                                           "            <td style=\"text-align: center\"><input type=\"submit\" "+Disabled(repEntry.getsecurity() == 0)+" name=\"action\" value=\"Register\"/>\n" +
                                           "            <td style=\"text-align: center\"><input type=\"submit\" "+Disabled(repEntry.getsecurity() == 1 && repEntry.getsecureconnection().length() == 0)+" name=\"action\" value=\"Test\"/>\n" +
                                           "          </tr>\n" +
                                           "          <tr>\n" +
                                           "            <td style=\"text-align: center\"><input type=\"submit\" "+Disabled(repEntry.getsecurity() == 1 && repEntry.getsecureconnection().length() == 0)+" name=\"action\" value=\"Start\"/>\n" +
                                           "            <td style=\"text-align: center\"><input type=\"submit\" "+Disabled(repEntry.getsecurity() == 1 && repEntry.getsecureconnection().length() == 0)+" name=\"action\" value=\"Stop\"/>\n" +
                                           "          </tr>\n" +
                                           "          <tr>\n" +
                                           "            <td style=\"text-align: center\"><input type=\"submit\" name=\"action\" value=\"Reset\"/>\n" +
                                           "            <td style=\"text-align: center\"><input type=\"submit\" name=\"action\" value=\"Delete\"/>\n" +
                                           "          </tr>\n" +
                                           "          <tr><td colspan=\"2\" style=\"text-align: center\">" +
                                           "              <input type=\"submit\" name=\"action\" value=\"Modify\"/></td></tr>");
            }
            newrow = newrow.replaceAll("#webpagename#", Name)
                .replaceAll("#replicationid#", Integer.toString(repEntry.getreplicationid()));
        }
        return newrow;
    }
   
    String probeList(String currentProbe) {
        StringBuffer buffer = new StringBuffer();
        Vector<String> probelist = new Vector<String>();
        probelist.add("All"); // Must have this.
      
        Session session = HibernateWrapper.getCheckedSession();
      
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
        if (repEntry.getsecurity() == 0) {
            fMessage = "Security is not enable for this replication entry.  No registration needed.";
            return;
        }
      
        // Get our own certificate.
      
        // Register with host and get secure url
        Post post = new Post(repEntry.getopenconnection() +
                             "/gratia-registration/register", "getsecureurl");
        post.add("arg1", fCollectorPem);
        String response = post.send(true);
        if (!post.success) {
            fMessage = post.errorMsg;
            return;
        }
        String[] results = split(response, ":");
        if (results[0].equals("secureconnection")) {

            String secureconnection = URLDecoder.decode(results[1]);
            repEntry.setsecureconnection(secureconnection);
            repEntry.setregistered(1);
            commitUpdate(repEntry);

        } else if (results[0].equals("Error")) {
            fMessage = "ERROR registering with remote host: " + response;
            Logging.warning("ReplicationTable: " + fMessage);
        } else {
            fMessage = "ERROR registering with remote host: " + response;
            Logging.warning("ReplicationTable got an expected message format: " + fMessage);
        }
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
            session = HibernateWrapper.getCheckedSession();
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
            fMessage = "ERROR deleting replication entry " +
                repEntry.getreplicationid() +
                " from DB";
            Logging.warning("ReplicationTable: " + fMessage);
            Logging.debug("ReplicationTable: exception details:", e);
        }
    }
   
    void test(Replication repEntry) 
    {
        // Test whether the connection is working.
      
        String target = ((repEntry.getsecurity() == 1) ? repEntry.getsecureconnection() :
                         repEntry.getopenconnection()) + "/gratia-servlets/rmi";
      
        if (target.startsWith("file:")) {
            target = ((repEntry.getsecurity() == 1) ? repEntry.getsecureconnection() :
                      repEntry.getopenconnection());
         
            String path = target.substring(5); // Skip the prefix.
         
            File dir = new File(path);
            if (!dir.isDirectory()) {
                fMessage = "Test Failed! " + dir + " is not a directory.";
            } else if (!dir.canWrite()) {
                fMessage = "Test Failed! " + dir + " is not writeable.";
            } else {
                fMessage = "Test Succeeded!!";
            }

        } else {
            String response = "";
            Post post = new Post(target, "update", "xxx");
            try {            
                response = post.send(true);
            }
            catch (Exception e) {
                Logging.warning("Error for target:"+target,e);
                fMessage = "Error for " + target + " : " + e;
                return;
            }
            if (!post.success) {
                fMessage = "Error for " + target + " : " + post.errorMsg;
                return;
            }
            try {
                String[] results = split(response, ":");
                if (!results[0].equals("OK")) {
                    fMessage = "Error for " + target + " : " + response;
                    return;
                }
            } 
            catch (Exception e) {
                fMessage = "Error for " + target + " : " + e;
                return;
            }
            fMessage = "Test Succeeded !!";
        }
    }
   
    void update(Replication repEntry, HttpServletRequest request) {
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
        newValue = request.getParameter(key);
        newValueInt = newValue.equals("Yes") ? 1 : 0;
        if (repEntry.getsecurity() != newValueInt) {
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
            session = HibernateWrapper.getCheckedSession();
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
            fMessage = "ERROR updating replication entry " +
                repEntry.getreplicationid() +
                " in DB";
            Logging.warning("ReplicationTable: " + fMessage);
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

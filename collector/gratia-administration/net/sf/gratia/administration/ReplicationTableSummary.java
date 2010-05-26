package net.sf.gratia.administration;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.sql.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
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

public class ReplicationTableSummary extends HttpServlet {
   
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
   Pattern fTableFinder = Pattern.compile("(?:(?:Compute|Storage)Element(?:Record)?|Subcluster)");
   Matcher fMatcher = null;
   
   //
   // globals
   //
   Properties fProperties;
   String fMessage = null;
   Boolean fModify = false;
   Boolean fInitialized = false;
   HashMap<String, List<Replication> > fRepTable = null;
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
   String fApplicationURL = "replicationtablesummary.html";

   private void done() {
      fRepTable = null;
   }

   static String fgPreamble = 
   "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
   "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
   "<head>\n" +
   "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\" />\n" +
   "<title>Gratia Accounting</title>\n" +
   "<link href=\"stylesheet.css\" type=\"text/css\" rel=\"stylesheet\" />\n" +
   "<link href=\"docstyle.css\" type=\"text/css\" rel=\"stylesheet\" />\n" +
   "</head>\n" +
   "<body>\n" +
   "<h1 align=\"center\" class=\"osgcolor\">&nbsp;&nbsp;&nbsp;&nbsp;Gratia Administration&nbsp;&nbsp;&nbsp;&nbsp;</h1>\n" +
   "<h3 align=\"center\">Replication Summary </h3>\n";
   
   public void init(ServletConfig config) throws ServletException {
      // javax.servlet.ServletConfig.getInitParameter() 
      Logging.debug("ReplicationTableSummary.init()");
      Name = config.getServletName();
   }
   
   void initialize() throws IOException {
      Logging.debug("ReplicationTableSummary.initialize()");
      if (fInitialized) return;
      Logging.debug("ReplicationTableSummary.initialize() continue");
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
      throws ServletException, IOException 
   {
      Logging.debug("ReplicationTableSummary.doGet()");
      setup(request);
      
      process();

      compileResponse(response);
      done();
   }
   
   private void compileResponse(HttpServletResponse response) throws IOException {
      Logging.debug("ReplicationTableSummary.compileResponse()");
      response.setContentType("text/html");
      response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
      response.setHeader("Pragma", "no-cache"); // HTTP 1.0
      //        request.getSession().setAttribute("table", table);
      PrintWriter writer = response.getWriter();
      //
      // cleanup message
      //
      writer.write(fgPreamble);
      if (fMessage != null) {
         writer.write("\n<pre id=\"message\" class=\"msg\">");
         writer.write(fMessage);
         writer.write("</pre>\n");
      } else {
         writer.write(fHtml);
      }
      writer.write("</body>\n      </html>\n");

      fMessage = null;
      writer.flush();
      writer.close();
   }
   
   public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException 
   {
      Logging.debug("ReplicationTableSummary.doPost()");

      Enumeration pars = request.getParameterNames();
      while (pars.hasMoreElements()) {
         String par = (String) pars.nextElement();
         Logging.debug("ReplicationTableSummary: Post Parameter " + par + " : " + request.getParameter(par));
      }
      setup(request);
      process();
      compileResponse(response);      
      done();
   }
   
   void setup(HttpServletRequest request)
      throws ServletException, IOException {
      Logging.debug("ReplicationTableSummary.setup()");

      // Once-only init
      initialize();
      fDBOK = true; // Default state

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
      // Load replication entries from DB

      Logging.debug("ReplicationTableSummary.loadRepTable()");
      if (!fDBOK) return; // Don't try.
      
      Session session = null;
      List records;
      try {
         session = HibernateWrapper.getCheckedSession();
         Transaction tx = session.beginTransaction();
         Query rq = session.createQuery("select record from Replication record order by record.recordtable, record.replicationid");
         records = rq.list();
         tx.commit();
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         fMessage = "Failed to load replication information from DB. Try reload.";
         fDBOK = false;
         return;
      }

      // Load hash table with entries      
      fRepTable = new java.util.LinkedHashMap<String, List<Replication> >();
      List<Replication> current;
      for ( Object listEntry : records ) {
         Replication repEntry = (Replication) listEntry;
         Logging.debug("Replication: loaded entry " +
                       repEntry.getreplicationid() + ": " +
                       repEntry.getopenconnection() + ", " +
                       repEntry.getregistered() + ", " +
                       repEntry.getrunning() + ", " + 
                       repEntry.getsecurity() + ", " +
                       repEntry.getfrequency() + ", " +
                       repEntry.getdbid() + ", " +
                       repEntry.getrowcount() + ", " +
                       repEntry.getbundleSize() + "."
                       );
         current = fRepTable.get( repEntry.getrecordtable() );
         if (current == null) {
            current = new java.util.LinkedList<Replication>();
            fRepTable.put( repEntry.getrecordtable(), current );
         }
         current.add( repEntry );
      }
   }
   
   private void process() {
      if (fRepTable == null) return;
      Logging.debug("ReplicationTableSummary.process()");

      StringBuffer buffer = new StringBuffer();

      // Loop through replication table entries.
      for (java.util.Map.Entry<String, List<Replication> > entry : fRepTable.entrySet()) {
         buffer.append("<h4>").append(entry.getKey()).append("</h4>");
         buffer.append("<table width=\"100%\" border=\"1\" cellpadding=\"10\">\n");
         buffer.append("<tr><th bgcolor=\"#999999\" scope=\"col\">ID</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Remote Host</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Registered</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Running</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Security</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Probe Name</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Check Interval (m)</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">DBID</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Row Count</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Bundle Size</th>\n");
         buffer.append("</tr>\n");
         
         
         List<Replication> vec = entry.getValue();
         Collections.sort(vec); // Sort according to Replication.CompareTo().
      
         for ( Replication repEntry : vec ) {
            Logging.debug("ReplicationTableSummary: current object state:\n" +
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
            buffer.append("<tr>\n");
            buffer.append("<td>").append(repEntry.getreplicationid()).append("</td>");
            buffer.append("<td>").append(repEntry.getopenconnection()).append("</td>");
            buffer.append("<td style=\"text-align: center\">").append(repEntry.getregistered()).append("</td>\n");
            buffer.append("<td style=\"text-align: center\">").append(repEntry.getrunning()).append("</td>\n");
            buffer.append("<td style=\"text-align: center\">").append(repEntry.getsecurity()).append("</td>\n");
            buffer.append("<td style=\"width: 200px; text-align:left\">").append(repEntry.getprobename()).append("</td>\n");
            buffer.append("<td style=\"text-align: right\">").append(repEntry.getfrequency()).append("</td>\n");
            buffer.append("<td style=\"text-align: right\">").append(repEntry.getdbid()).append("</td>\n");
            buffer.append("<td style=\"text-align: right\">").append(repEntry.getrowcount()).append("</td>\n");
            buffer.append("<td style=\"text-align: right\">").append(repEntry.getbundleSize()).append("</td>\n");
            buffer.append("</tr>\n");
         }
         buffer.append("</table>\n");
      }
      fHtml = buffer.toString();
   }
   
}

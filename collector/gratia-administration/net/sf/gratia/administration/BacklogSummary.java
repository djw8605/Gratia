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
import net.sf.gratia.storage.Backlog;
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

public class BacklogSummary extends HttpServlet {
   
   static final Pattern yesMatcher = Pattern.compile("^[YyTt1]");

   //
   // globals
   //
   String fErrorMessage = null;
   Boolean fInitialized = false;
   Boolean fDBOK = true;
   JMSProxy fProxy = null;

   //
   // support
   //
   
   // Which Servlet/web page is this
   String Name;
   static final String fApplicationURL = "Backlog.html";

   static final String fgPreamble = 
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
   "<h3 align=\"center\">Backlog Summary </h3>\n";
   
   public void init(ServletConfig config) throws ServletException {
      // javax.servlet.ServletConfig.getInitParameter() 
      Logging.debug("BacklogSummary.init()");
      Name = config.getServletName();
   }
   
   void initialize() throws IOException {
      Logging.debug("BacklogSummary.initialize()");
      if (fInitialized) return;
      Logging.debug("BacklogSummary.initialize() continue");
      Properties properties = Configuration.getProperties();
      while (true) {
         // Wait until JMS service is up
         try { 
            fProxy = (JMSProxy)
               Naming.lookup(properties.getProperty("service.rmi.rmilookup") +
                             properties.getProperty("service.rmi.service"));
         } catch (Exception e) {
            try {
               Thread.sleep(5000);
            } catch (Exception ignore) {
            }
         }
         break;
      }

      fInitialized = true;
   }
   
   private boolean wantDetails(HttpServletRequest request) 
   {
      String wantDetails = request.getParameter("wantDetails");      
      if (wantDetails != null) {
         if (yesMatcher.matcher(wantDetails).matches()) {
            return true;
         } else {
            return false;
         }
      }
      return false;
   }   
                          
   public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException 
   {
      Logging.debug("BacklogSummary.doGet()");
      setup(request);
      
      boolean details = wantDetails(request);
      String html = process(details);

      compileResponse(response,html,details);
   }
   
   public void doPost(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException 
   {
      Logging.debug("BacklogSummary.doPost()");
      
      Enumeration pars = request.getParameterNames();
      while (pars.hasMoreElements()) {
         String par = (String) pars.nextElement();
         Logging.debug("BacklogSummary: Post Parameter " + par + " : " + request.getParameter(par));
      }
      setup(request);
      boolean details = wantDetails(request);
      String html = process(details);
      compileResponse(response, html, details);
   }

   private void compileResponse(HttpServletResponse response, String html, boolean details) throws IOException {
      Logging.debug("BacklogSummary.compileResponse()");
      response.setContentType("text/html");
      response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
      response.setHeader("Pragma", "no-cache"); // HTTP 1.0
      //        request.getSession().setAttribute("table", table);
      PrintWriter writer = response.getWriter();
      //
      // cleanup message
      //
      writer.write(fgPreamble);
      writer.write("<h4 align=\"center\">");
      writer.write(fProxy.getName());
      writer.write("</h4>\n");
      if (details) {
         writer.write("<h4 align=\"center\"><a href=\"backlog.html?wantDetails=0\" target=\"adminContent\">See less details</a></h4>\n");
      } else {
         writer.write("<h4 align=\"center\"><a href=\"backlog.html?wantDetails=1\" target=\"adminContent\">See more details</a></h4>\n");
      }         

      if (fErrorMessage != null) {
         writer.write("\n<pre id=\"message\" class=\"msg\">");
         writer.write(fErrorMessage);
         writer.write("</pre>\n");
      } else {
         writer.write(html);
      }
      writer.write("</body>\n      </html>\n");

      fErrorMessage = null;
      writer.flush();
      writer.close();
   }
   
   
   void setup(HttpServletRequest request)
      throws ServletException, IOException {
      Logging.debug("BacklogSummary.setup()");

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

   }

   private void addHeader(StringBuffer buffer, boolean details) 
   {
      buffer.append("<table width=\"100%\" border=\"1\" cellpadding=\"10\">\n");
      buffer.append("<tr><th width=\"30%\" bgcolor=\"#999999\" scope=\"col\">Provider</th>\n");
      buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Records to be uploaded</th>\n");
      if (details) {
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Xml files in queue</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Tar files in queue</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Max pending files</th>\n");
      }         
      buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Estimated service backlog</th>\n");
      buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Bundle size</th>\n");
      buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Last update</th>\n");
      buffer.append("</tr>\n");      
   }
   
   private void addData(StringBuffer buffer, List<Backlog> list, boolean details)
   {
      // Collections.sort(list); // Sort according to Backlog.CompareTo().
      for ( Backlog backlog : list ) {
         Logging.debug("BacklogSummary: current object state:\n" +
                       "  ServerDate: "  + backlog.getServerDate() + "\n" + 
                       "  EntityType: "  + backlog.getEntityType() + "\n" + 
                       "  Name: "  + backlog.getName() + "\n" + 
                       "  Files: "  + backlog.getFiles() + "\n" + 
                       "  Records: "  + backlog.getRecords() + "\n" + 
                       "  TarFiles: "  + backlog.getTarFiles() + "\n" + 
                       "  ServiceBacklog: "  + backlog.getServiceBacklog() + "\n" + 
                       "  MaxPendingFiles: "  + backlog.getMaxPendingFiles() + "\n" + 
                       "  BundleSize: "  + backlog.getBundleSize() + "\n" + 
                       "  PrevServerDate: "  + backlog.getPrevServerDate() + "\n" + 
                       "  PrevRecords: "  + backlog.getPrevRecords() + "\n" + 
                       "  PrevServiceBacklog: "  + backlog.getPrevServiceBacklog());
         buffer.append("<tr>\n");
         //buffer.append("<td>").append(backlog.getEntityType()).append("</td>");
         if (backlog.getEntityType().equals("collector")) {
            buffer.append("<td style=\"width: 200px; text-align:left\">");
            String name = backlog.getName();
            if (name.startsWith("collector:")) {
               name = name.substring(10);
            }
            buffer.append("<a href=\"").append(name).append("/gratia-administration/backlog.html\" target=\"adminContent\">");
            buffer.append(backlog.getName()).append("</a>");
            buffer.append("</td>\n");
         } else {
            buffer.append("<td style=\"width: 200px; text-align:left\">").append(backlog.getName()).append("</td>\n");
         }
         buffer.append("<td style=\"text-align: center\">").append(backlog.getRecords()).append("</td>\n");
         if (details) {
            buffer.append("<td style=\"text-align: center\">").append(backlog.getFiles()).append("</td>\n");
            buffer.append("<td style=\"text-align: center\">").append(backlog.getTarFiles()).append("</td>\n");
            buffer.append("<td style=\"text-align: center\">").append(backlog.getMaxPendingFiles()).append("</td>\n");
         }
         buffer.append("<td style=\"text-align: center\">").append(backlog.getServiceBacklog()).append("</td>\n");
         buffer.append("<td style=\"text-align: center\">").append(backlog.getBundleSize()).append("</td>\n");
         buffer.append("<td>").append(backlog.getServerDate()).append("</td>");
         //buffer.append("<td style=\"text-align: center\">").append(backlog.getPrevServerDate()).append("</td>\n");
         //buffer.append("<td style=\"text-align: center\">").append(backlog.getPrevRecords()).append("</td>\n");
         //buffer.append("<td style=\"text-align: center\">").append(backlog.getPrevServiceBacklog()).append("</td>\n");
         buffer.append("</tr>\n");
      }      
   }
   
   private void addFooter(StringBuffer buffer)
   {
      buffer.append("</table>\n");
   }
   
   private String process(boolean details) {
      // Load up with backlog information
      
      Logging.debug("BacklogSummary.process()");

      if (!fDBOK) return "<br>No access to the database.</br>";
      
      Session session = null;
      List<Backlog> collectors = null;
      List<Backlog> probes = null;
      try {
         session = HibernateWrapper.getCheckedSession();
         Transaction tx = session.beginTransaction();
         collectors = Backlog.getList(session, " where EntityType = 'collector' order by Name, ServerDate "); 
         probes = Backlog.getList(session, " where EntityType != 'collector' and EntityType != 'local' order by Name, ServerDate ");
         tx.commit();
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         Logging.warning("Failed to load backlog information from DB. Try reload.",e);
         fErrorMessage = "Failed to load backlog information from DB. Try reload.";
         fDBOK = false;
         return "<br>Unable to find backlog information.<br>";
      }
                                 
      if (collectors == null && probes == null) {
         return "<br>Unable to find backlog information.<br>";
      }

      StringBuffer buffer = new StringBuffer();

      if (collectors != null && !collectors.isEmpty()) {
         buffer.append("<h4>Collectors</h4>");
         addHeader(buffer,details);
         addData(buffer,collectors,details);
         addFooter(buffer);
      }
      
      if (collectors != null && !collectors.isEmpty()) {
         buffer.append("<h4>Probes</h4>");
         addHeader(buffer,details);
         addData(buffer,probes,details);
         addFooter(buffer);
      }
      return buffer.toString();
   }
   
}

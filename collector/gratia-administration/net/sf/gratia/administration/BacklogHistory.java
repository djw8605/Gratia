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

import net.sf.gratia.services.JMSProxy;
import net.sf.gratia.services.HibernateWrapper;
import net.sf.gratia.storage.Backlog;
import net.sf.gratia.storage.BacklogSummary;
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

import org.apache.commons.lang.StringEscapeUtils;

public class BacklogHistory extends HttpServlet {
   
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
   static final String fApplicationURL = "backlog-history.html";

   static final String fgPreamble = 
   "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
   "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
   "<head>\n" +
   "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\" />\n" +
   "<title>Gratia Accounting</title>\n" +
   "<link href=\"stylesheet.css\" type=\"text/css\" rel=\"stylesheet\" />\n" +
   "<link href=\"docstyle.css\" type=\"text/css\" rel=\"stylesheet\" />\n" +
   "<style type=\"text/css\">\n" +
   ".problem {color: #FF0000; font-weight:bold;}\n" +
   ".good {color: green; font-weight:bold;}\n" +
   ".improving {color: #000000}" +
   "</style>" +
   "</head>\n" +
   "<body>\n" +
   "<h1 align=\"center\" class=\"osgcolor\">&nbsp;&nbsp;&nbsp;&nbsp;Gratia Administration&nbsp;&nbsp;&nbsp;&nbsp;</h1>\n" +
   "<h3 align=\"center\">Backlog History </h3>\n";
   
   public void init(ServletConfig config) throws ServletException {
      // javax.servlet.ServletConfig.getInitParameter() 
      Logging.debug("BacklogHistory.init()");
      Name = config.getServletName();
   }
   
   void initialize() throws IOException {
      Logging.debug("BacklogHistory.initialize()");
      if (fInitialized) return;
      Logging.debug("BacklogHistory.initialize() continue");
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
      Logging.debug("BacklogHistory.doGet()");
      setup(request);
      
      boolean details = wantDetails(request);
      String html = process(request.getParameter("name"),details);

      compileResponse(response,request.getParameter("name"),html,details);
   }
   
   public void doPost(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException 
   {
      Logging.debug("BacklogHistory.doPost()");
      
      Enumeration pars = request.getParameterNames();
      while (pars.hasMoreElements()) {
         String par = (String) pars.nextElement();
         Logging.debug("BacklogHistory: Post Parameter " + par + " : " + request.getParameter(par));
      }
      setup(request);
      boolean details = wantDetails(request);
      String html = process(request.getParameter("name"),details);
      compileResponse(response, request.getParameter("name"), html, details);
   }

   private void compileResponse(HttpServletResponse response, String name, String html, boolean details) throws IOException {
      Logging.debug("BacklogHistory.compileResponse()");
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
      writer.write(StringEscapeUtils.escapeXml(name));
      writer.write(" on ");      
      writer.write(fProxy.getName());
      writer.write("</h4>\n");
//      if (details) {
//         writer.write("<h4 align=\"center\"><a href=\"backlog.html?wantDetails=0\" target=\"adminContent\">See less details</a></h4>\n");
//      } else {
//         writer.write("<h4 align=\"center\"><a href=\"backlog.html?wantDetails=1\" target=\"adminContent\">See more details</a></h4>\n");
//      }         

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
      Logging.debug("BacklogHistory.setup()");

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
      buffer.append("<tr><th width=\"30%\" bgcolor=\"#999999\" scope=\"col\">Date/Time</th>\n");
      buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Average Records</th>\n");
      if (details) {
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Average Xml files in queue</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Average Tar files in queue</th>\n");
         buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Average Max pending files</th>\n");
      }         
      buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Average service backlog</th>\n");
      buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Bundle size</th>\n");
      buffer.append("    <th width=\"200\" colspan=2 bgcolor=\"#999999\" scope=\"col\">Status</th>\n");
//      buffer.append("    <th width=\"100\" bgcolor=\"#999999\" scope=\"col\"></th>\n");
//      buffer.append("    <th bgcolor=\"#999999\" scope=\"col\">Last update</th>\n");
      buffer.append("</tr>\n");      
   }
   
   private void addData(StringBuffer buffer, List<BacklogSummary> list, boolean details)
   {
      // Collections.sort(list); // Sort according to Backlog.CompareTo().
      BacklogSummary backlog = null;
      for ( BacklogSummary nextlog : list ) {
         
         if (backlog == null) {
            backlog = nextlog;
         } else {
            Logging.debug("BacklogHistory: current object state:\n" +
                          "  EventDate: "  + backlog.getEventDate() + "\n" + 
                          "  EntityType: "  + backlog.getEntityType() + "\n" + 
                          "  Name: "  + backlog.getName() + "\n" + 
                          "  StartTime: "  + backlog.getStartTime() + "\n" + 
                          "  AvgEndTime: "  + backlog.getEndTime() + "\n" + 
                          "  AvgFiles: "  + backlog.getAvgFiles() + "\n" + 
                          "  AvgRecords: "  + backlog.getAvgRecords() + "\n" + 
                          "  AvgTarFiles: "  + backlog.getAvgTarFiles() + "\n" + 
                          "  AvgServiceBacklog: "  + backlog.getAvgServiceBacklog() + "\n" + 
                          "  AvgMaxPendingFiles: "  + backlog.getAvgMaxPendingFiles() + "\n" + 
                          "  AvgBundleSize: "  + backlog.getAvgBundleSize() + "\n");
            buffer.append("<tr>\n");
            buffer.append("<td>").append(backlog.getServerDate()).append("</td>");
            buffer.append("<td style=\"text-align: center\">").append(backlog.getAvgRecords()).append("</td>\n");
            if (details) {
               buffer.append("<td style=\"text-align: center\">").append(backlog.getAvgFiles()).append("</td>\n");
               buffer.append("<td style=\"text-align: center\">").append(backlog.getAvgTarFiles()).append("</td>\n");
               buffer.append("<td style=\"text-align: center\">").append(backlog.getAvgMaxPendingFiles()).append("</td>\n");
            }
            buffer.append("<td style=\"text-align: center\">").append(backlog.getAvgServiceBacklog()).append("</td>\n");
            buffer.append("<td style=\"text-align: center\">").append(backlog.getAvgBundleSize()).append("</td>\n");
            
            long backlogValue = backlog.getAvgRecords()+backlog.getAvgServiceBacklog();
            long backlogDecrease = (nextlog.getAvgRecords()+nextlog.getAvgServiceBacklog()) - backlogValue;
            long msSpan = backlog.getEventDate().getTime() - nextlog.getEventDate().getTime();
            boolean progress;
            if (backlogDecrease > 0) {
               // Real decrease
               progress = true;
            } else {
               progress = false;
               backlogDecrease = -backlogDecrease;
            }
            if (backlogValue == 0 || (progress && backlogDecrease > backlogValue) ) {
               buffer.append("<td style=\"text-align: center\" class=\"good\" colspan=2>Up to date</td>\n");
            } else if (backlogDecrease < 10) {
               buffer.append("<td style=\"text-align: center\" class=\"improving\">Stable</td>\n");
               buffer.append("<td style=\"text-align: center\">See ");
               buffer.append("<a href=\"").append("backlog-history.html?name=");
               buffer.append(backlog.getName()).append("\" target=\"adminContent\">");            
               buffer.append("history</a> for additional details</td>");               
            } else {
               if (progress) {
                  buffer.append("<td style=\"text-align: center\" class=\"improving\">Catching up</td>\n");
               } else {
                  buffer.append("<td style=\"text-align: center\" class=\"problem\">Losing ground</td>\n");
               } 
               java.text.DecimalFormat decForm = new java.text.DecimalFormat();
               decForm.applyPattern("0.0");
               java.text.FieldPosition pos1 = new java.text.FieldPosition(java.text.NumberFormat.FRACTION_FIELD);
               
               double catchupTime = (double)msSpan * backlogValue / backlogDecrease / (1000*60); // in minutes.
               if (msSpan == 0) {
                  buffer.append("<td style=\"text-align: center\">See ");
                  buffer.append("<a href=\"").append("backlog-history.html?name=");
                  buffer.append(backlog.getName()).append("\" target=\"adminContent\">");            
                  buffer.append("history</a> for additional details</td>");               
               } else {
                  if (catchupTime < 1) {
                     buffer.append("<td style=\"text-align: center\">less than one minutes</td>");
                  } else if ( catchupTime < 100 ) {                  
                     buffer.append("<td style=\"text-align: center\">");
                     buffer = decForm.format(catchupTime, buffer, pos1);
                     buffer.append(" minutes</td>\n");
                  } else {
                     catchupTime = catchupTime / 60; // hours.
                     if ( catchupTime < 48 ) {
                        buffer.append("<td style=\"text-align: center\">");
                        buffer = decForm.format(catchupTime, buffer, pos1);
                        buffer.append(" hours</td>\n");
                     } else {
                        catchupTime = catchupTime / 24 ; // days
                        buffer.append("<td style=\"text-align: center\">");
                        buffer = decForm.format(catchupTime, buffer, pos1);
                        buffer.append(" days</td>\n");
                     }
                  }
               }
            }
            //buffer.append("<td style=\"text-align: center\">").append(backlog.getPrevServerDate()).append("</td>\n");
            //buffer.append("<td style=\"text-align: center\">").append(backlog.getPrevRecords()).append("</td>\n");
            //buffer.append("<td style=\"text-align: center\">").append(backlog.getPrevServiceBacklog()).append("</td>\n");
            buffer.append("</tr>\n");
            backlog = nextlog;
         }
      }
   }
   
   private void addFooter(StringBuffer buffer)
   {
      buffer.append("</table>\n");
   }
   
   private String process(String name, boolean details) {
      // Load up with backlog information
      
      Logging.debug("BacklogHistory.process()");

      if (!fDBOK) return "<br>No access to the database.</br>";
      
      Session session = null;
      List<Backlog> summary = null;
      List<BacklogSummary> hourly = null;
      List<BacklogSummary> daily = null;
      try {
         session = HibernateWrapper.getCheckedSession();
         Transaction tx = session.beginTransaction();
         summary = Backlog.getList(session, " where Name = '" + name + "' order by ServerDate ");
         hourly = BacklogSummary.getList(session,"Hourly", " where Name = '" + name + "' order by EventDate desc");
         daily = BacklogSummary.getList(session,"Daily", " where Name = '" + name + "' order by EventDate desc");
         tx.commit();
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         Logging.warning("Failed to load backlog information from DB. Try reload.",e);
         fErrorMessage = "Failed to load backlog information from DB. Try reload.";
         fDBOK = false;
         return "<br>Unable to find backlog information.<br>";
      }

      if (summary == null && hourly == null && daily == null) {
         return "<br>Unable to find backlog information.<br>";
      }
      if (summary.isEmpty() && hourly.isEmpty() && daily.isEmpty()) {
         return "<br>There is no backlog information.<br>";
      }
      
      StringBuffer buffer = new StringBuffer();

      if (summary != null && ! summary.isEmpty()) {
         BacklogStatus.addHeader(buffer,details);
         BacklogStatus.addData(buffer,summary,details);
         BacklogStatus.addFooter(buffer);
      }
      
      buffer.append("<h4>Hourly</h4>");
      addHeader(buffer,details);
      addData(buffer,hourly,details);
      addFooter(buffer);

      buffer.append("<h4>Daily</h4>");
      addHeader(buffer,details);
      addData(buffer,daily,details);
      addFooter(buffer);
      
      return buffer.toString();
   }
   
}

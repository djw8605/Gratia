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

public class BacklogByProbes extends HttpServlet {
   
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
   "<style type=\"text/css\">\n" +
   ".problem {color: #FF0000; font-weight:bold;}\n" +
   ".good {color: green; font-weight:bold;}\n" +
   ".improving {color: #000000}\n" +
   ".ancient {color: purple; font-style:italic; font-weight:bold;}\n" +
   "</style>" +
   "</head>\n" +
   "<body>\n" +
   "<h1 align=\"center\" class=\"osgcolor\">&nbsp;&nbsp;&nbsp;&nbsp;Gratia Administration&nbsp;&nbsp;&nbsp;&nbsp;</h1>\n" +
   "<h3 align=\"center\">Local Backlog By Probes</h3>\n";
   
   public void init(ServletConfig config) throws ServletException {
      // javax.servlet.ServletConfig.getInitParameter() 
      Logging.debug("BacklogByProbes.init()");
      Name = config.getServletName();
   }
   
   void initialize() throws IOException {
      Logging.debug("BacklogByProbes.initialize()");
      if (fInitialized) return;
      Logging.debug("BacklogByProbes.initialize() continue");
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
      Logging.debug("BacklogByProbes.doGet()");
      setup(request);
      
      boolean details = wantDetails(request);
      String html = process(details);

      compileResponse(response,html,details);
   }
   
   public void doPost(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException 
   {
      Logging.debug("BacklogByProbes.doPost()");
      
      Enumeration pars = request.getParameterNames();
      while (pars.hasMoreElements()) {
         String par = (String) pars.nextElement();
         Logging.debug("BacklogByProbes: Post Parameter " + par + " : " + request.getParameter(par));
      }
      setup(request);
      boolean details = wantDetails(request);
      String html = process(details);
      compileResponse(response, html, details);
   }

   private void compileResponse(HttpServletResponse response, String html, boolean details) throws IOException {
      Logging.debug("BacklogByProbes.compileResponse()");
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
         //writer.write("<h4 align=\"center\"><a href=\"backlog.html?wantDetails=0\" target=\"adminContent\">See less details</a></h4>\n");
      } else {
         //writer.write("<h4 align=\"center\"><a href=\"backlog.html?wantDetails=1\" target=\"adminContent\">See more details</a></h4>\n");
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
      Logging.debug("BacklogByProbes.setup()");

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
   
   public Connection openConnection()
   {
      Properties properties = Configuration.getProperties();
      if (fProxy == null) {
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
      }
      
      //
      // database related
      //
      String driver = "";
      String url = "";
      String user = "";
      String password = "";
      try {
         driver = properties.getProperty("service.mysql.driver");
         url = properties.getProperty("service.mysql.url");
         user = properties.getProperty("service.mysql.user");
         password = properties.getProperty("service.mysql.password");
      }
      catch (Exception ignore) {
      }
      try {
         Class.forName(driver).newInstance();
         return DriverManager.getConnection(url,user,password);
      }
      catch (Exception e) {
         Logging.warning("BacklogByProbes: Failed to open connection.",e);
      }
      return null;
   }
   
   public void closeConnection(Connection connection) {
      if (connection != null) {
         try {
            connection.close();
         }
         catch (Exception e) {
            Logging.warning("BacklogByProbes: Failed to open connection.",e);
         }
      }
   }

   private String process(boolean details) {
      // Load up with backlog information
      
      Logging.debug("BacklogByProbes.process()");

      if (!fDBOK) return "<br>No access to the database.</br>";
      
      // Execute command
      final String fgProcessQueueLengthQuery = "select Queue from CollectorStatus order by Queue";
      final String topdir = "/var/lib";

      StringBuffer buffer = new StringBuffer();
      
      try
      {
         Connection connection = openConnection();
         PreparedStatement statement = connection.prepareStatement(fgProcessQueueLengthQuery);
         ResultSet resultSet = statement.executeQuery();
         while(resultSet.next()) {
            int q = resultSet.getInt(1);
            String command = topdir+"/gratia/collector-backlog-byprobes "+topdir+"/gratia/data/thread"+q;
            Process child = Runtime.getRuntime().exec(command);
            
            // Get the input stream and read from it
            InputStream in = child.getInputStream();
            int c;
            buffer.append("<pre>\n");
            while ((c = in.read()) != -1) {
               buffer.append((char)c);
            }
            buffer.append("</pre>\n");
            in.close();
            
         }
         resultSet.close();
         statement.close();
         closeConnection(connection);
      }
      catch (Exception e)
      {
         buffer.append("Error: Failed to get the breakdown by probes ("+e.getMessage()+")");
         Logging.warning("BacklogByProbes: Failed to get the breakdown by probes.",e);
      }         
      
      return buffer.toString();
   }
   
}

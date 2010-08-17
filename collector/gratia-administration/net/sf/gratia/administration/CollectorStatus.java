package net.sf.gratia.administration;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.Properties;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.text.DateFormat;
import java.util.Date;
import java.io.PrintWriter;

import java.rmi.Naming;

import net.sf.gratia.util.Logging;
import net.sf.gratia.services.JMSProxy;

/**
 * <p>Title: Record </p>
 *
 * <p>Description: Present via the web interface the current state of the collector sub-component.</p>
 *
 * <p>Copyright: Copyright (c) 2010</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 */

public class CollectorStatus extends HttpServlet 
{
   private class System {
      public String fName;
      public String fPrettyName;
      public System(String name, String pname) { 
         fName = name;
         fPrettyName = pname;
      }
   }
   
   private enum Output {
      xml,
      html,
      text;
   }
   
   private JMSProxy fProxy = null;
   
   static private final String gHtmlPrologue = 
   "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"+
   "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"+
   "<head>\n"+
   "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\" />\n"+
   "<title>Gratia Accounting</title>\n"+
   "<link href=\"stylesheet.css\" type=\"text/css\" rel=\"stylesheet\" />\n"+
   "<link href=\"docstyle.css\" type=\"text/css\" rel=\"stylesheet\" />\n"+
   "</head>\n"+
   "<body>\n"+
   "<h1 align=\"center\" class=\"osgcolor\">&nbsp;&nbsp;&nbsp;&nbsp;Gratia Administration&nbsp;&nbsp;&nbsp;&nbsp;</h1>\n"+
   "<h3>Sub-System Status</h3>\n";

   private void print(StringBuffer buffer, System subsystem, String status, Output outputformat)
   {
      status = status.toLowerCase();
      if (outputformat == Output.html) {
         buffer.append("<tr><td><strong>");
         buffer.append(subsystem.fPrettyName);
         buffer.append("</strong></td><td><div align=\"center\">");
         if (status.equals("disabled")) {
            buffer.append("<font color=\"fuchsia\"><strong>Disabled</strong></font>");
         } else if (status.equals("safe")) {
            buffer.append("<font color=\"fuchsia\"><strong>Safe</strong></font>");
         } else if (status.equals("sleeping")) {
            buffer.append("<font color=\"green\"><strong>Sleeping</strong></font>");
         } else if (status.equals("active")) {
            buffer.append("<font color=\"green\"><strong>Active</strong></font>");
         } else if (status.equals("enabled")) {
            buffer.append("<font color=\"green\"><strong>Enabled</strong></font>");
         } else if (status.equals("running")) {
            buffer.append("<font color=\"green\"><strong>Running</strong></font>");
         } else if (status.equals("paused")) {
            buffer.append("<font color=\"red\"><strong>Paused</strong></font>");
         } else if (status.equals("stopped")) {
            buffer.append("<font color=\"red\"><strong>Stopped</strong></font>");
         } else if (status.equals("unknown")) {
            buffer.append("<font color=\"red\"><strong>Unknown</strong></font>");
         } else {
            buffer.append(status);
         }
         buffer.append("</div></td></tr>\n");

      } else if (outputformat == Output.xml) {
         buffer.append("<"+subsystem.fName+">");
         buffer.append(status);
         buffer.append("</"+subsystem.fName+">\n");
      } else {
         buffer.append(subsystem.fName);
         buffer.append("=");
         buffer.append(status);
         buffer.append("|");
      }
   }
   
   public void initialize() {
      Properties p = net.sf.gratia.util.Configuration.getProperties();
      try {
         fProxy = (JMSProxy) Naming.lookup(p.getProperty("service.rmi.rmilookup") +
                                           p.getProperty("service.rmi.service"));
      }
      catch (Exception e) {
         Logging.warning("SystemAdministration: Caught exception during RMI lookup", e);
      }
   }

	public void init(ServletConfig config) throws ServletException 
	{
	}
   
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException
	{
		String uriPart = request.getRequestURI();
		int slash2 = uriPart.substring(1).indexOf("/") + 1;
		uriPart = uriPart.substring(slash2);
		String queryPart = request.getQueryString();
		if (queryPart == null)
			queryPart = "";
		else
			queryPart = "?" + queryPart;
		request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);
      
		String outputformatreq = request.getParameter("out").toLowerCase();
      Output outputformat = Output.text;
      if (outputformatreq.equals("xml")) {
         outputformat = Output.xml;
      } else if (outputformatreq.equals("html")) {
         outputformat = Output.html;
      }
      String subsystem = request.getParameter("subsystem");
      if (subsystem != null) {
         subsystem = subsystem.toLowerCase();
      }
      
      List<System> sortedSystems = new LinkedList();
      sortedSystems.add(new System("operations","Global Operational Status"));
      sortedSystems.add(new System("collector","Collector"));
      sortedSystems.add(new System("databaseupdate","Database updates"));
      sortedSystems.add(new System("replication","Replication"));
      sortedSystems.add(new System("datahousekeeping","DataHousekeeping"));
      sortedSystems.add(new System("history","History clean-up"));
      
      HashMap<String, String> status = new HashMap<String, String>();
      for(System item : sortedSystems) {
         status.put(item.fName,"unknown");
      }
      if (fProxy == null) {
         initialize();
      }
      
      if (fProxy != null ) {
         try {
            Boolean flag = fProxy.operationsDisabled();
            if (flag) {
               status.put("operations","SAFE");
               status.put("collector","SAFE");
               status.put("databaseupdate","SAFE");
               status.put("replication","SAFE");
               status.put("datahousekeeping","SAFE");
               status.put("history","SAFE");
            } else {
               status.put("operations","Active");
               flag = fProxy.servletEnabled();
               if (flag) {
                  status.put("collector","Active");
               } else {
                  status.put("collector","Stopped");
               }
               flag = fProxy.databaseUpdateThreadsActive();
               if (flag) {
                  status.put("databaseupdate","Active");
               } else {
                  status.put("databaseupdate","Stopped");
               }
               flag = fProxy.replicationServiceActive();
               if (flag) {
                  status.put("replication","Active");
               } else {
                  status.put("replication","Stopped");
               }
               status.put("datahousekeeping",fProxy.housekeepingServiceStatus());
               flag = fProxy.reaperActive();
               if (flag) {
                  status.put("history","RUNNING");
               } else {
                  status.put("history","SLEEPING");
               }
               status.put("queuemanager",fProxy.queueManagerStatus());
            }
         }
         catch (Exception e) {
            Logging.warning("CollectorStaus.doGet: Caught exception assessing operational status via proxy", e);
         }
      } 
      
      StringBuffer buffer = new StringBuffer();
      if (outputformat == Output.html) {
         response.setContentType("text/html");
         buffer.append(gHtmlPrologue);
         buffer.append("<p>Status last updated at: <strong>");
         buffer.append(DateFormat.getDateTimeInstance().format(new Date()));
         buffer.append(" UTC</strong>\n");
         buffer.append("<table width=\"40%\"></p>\n");
         buffer.append("<tr>\n");
         buffer.append("<th width=\"30%\" scope=\"col\"><strong><div align=\"center\">Item</div></strong></th>\n");
         buffer.append("<th width=\"10%\" scope=\"col\"><strong><div align=\"center\">Status</div></strong></th>\n");
         buffer.append("</tr>\n");
      } else {
         response.setContentType("text/plain");
      }
      
      if (subsystem == null) {
         for (System s : sortedSystems) {
            print(buffer,s,status.get(s.fName),outputformat);
         }
      } else {
         for (System s : sortedSystems) {
            if (s.fName.equals(subsystem)) {
               print(buffer,s,status.get(subsystem),outputformat);
            }
         }
      }
      if (outputformat == Output.html) {
         buffer.append("</table>\n");
         buffer.append("</body>\n");
         buffer.append("</html>\n");
      }

		response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0
		PrintWriter writer = response.getWriter();
		writer.write(buffer.toString());
		writer.flush();
		writer.close();
	}
   
}
package net.sf.gratia.testbacklog;

import net.sf.gratia.util.Logging;
import net.sf.gratia.util.LogLevel;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

public class RMIHandlerServlet extends net.sf.gratia.servlets.RMIHandlerServlet {
   
   public void init(ServletConfig config)
   throws ServletException {
      super.init(config);         
   }
   
   public void doPost(HttpServletRequest req, HttpServletResponse res)
   throws ServletException, IOException, IllegalStateException, NoClassDefFoundError 
   {
      
      boolean result;
      try {
         result = lookupProxy() && fCollectorProxy.servletEnabled();
      }
      catch (java.rmi.NoSuchObjectException e) {
         result = false;
         Logging.log(LogLevel.SEVERE,
                     "RMIHandlerServlet encountered RMI lookup error: expected object has gone away!");
         throw e;
      }
      
      if (!result) {
         PrintWriter writer = res.getWriter();
         writer.write("Error: service not ready.");
         writer.flush();
         return;
      }
      super.doPost(req,res);
      // And now for a snapshot of the backlog information
      // fCollectorProxy.BacklogSnapshot()
      return;
   }
}

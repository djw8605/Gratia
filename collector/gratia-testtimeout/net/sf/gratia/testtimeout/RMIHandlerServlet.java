package net.sf.gratia.testtimeout;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

public class RMIHandlerServlet extends HttpServlet {

   public void init(ServletConfig config)
        throws ServletException {
        super.init(config);         
    }
      
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException, IllegalStateException, NoClassDefFoundError 
   {
         
      PrintWriter writer = res.getWriter();
           writer.write("This connection will last 600s.");
      try {
         Thread.sleep(600000);
      } catch (Exception ex) {
         writer.write("Early exit.");
      }
      writer.flush();
      return;
   }
         
}

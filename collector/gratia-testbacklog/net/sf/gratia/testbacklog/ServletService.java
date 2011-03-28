package net.sf.gratia.testbacklog;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.TimeZone;

import net.sf.gratia.util.Logging;

// Perform necessary initialization on service start
public class ServletService implements ServletContextListener {

   public ServletService() {
      //
      // initialize logging
      //
         
      Logging.initialize("testbacklog");

      // Set default timezone.
      Logging.info("ServletService: setting default time zone to GMT.");
      TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
   }

   public void contextInitialized(ServletContextEvent sce) {
      Logging.info("ServletService: Context Initialize Event");
   }

   public void contextDestroyed(ServletContextEvent sce) {
      Logging.info("ServletService: Context Destroy Event");
   }
}

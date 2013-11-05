package net.sf.gratia.reporting;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.TimeZone;

import net.sf.gratia.util.Logging;

// Perform necessary initialization on service start
public class ReportingService implements ServletContextListener {

   public ReportingService() {
      Logging.initialize("reporting");

      // Set default timezone.
      // HK: Commented out as we now do the TimeZone setting direclty in Logging.java
      //Logging.info("ReportingService: setting default time zone to GMT.");
      //TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
   }

   public void contextInitialized(ServletContextEvent sce) {
      Logging.info("ReportingService: Context Initialize Event");
   }

   public void contextDestroyed(ServletContextEvent sce) {
      Logging.info("ReportingService: Context Destroy Event");
   }
}

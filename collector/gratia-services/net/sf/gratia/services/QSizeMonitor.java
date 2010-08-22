package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;
import net.sf.gratia.util.LogLevel;

import java.util.*;

import java.lang.management.ManagementFactory; 
import javax.management.*; 
import javax.management.remote.*;

//
// notes and asides - this class will check each of the defined q's and stop the input service if the qsize > max.q.size
//
// in order to work the following options must be passed to the jvm prior to starting tomcat
//
// -Dcom.sun.management.jmxremote -> turns on jmx management
// -Dcom.sun.management.jmxremote.port=8004 -> where I can connect - must be unique per tomcat instance
// -Dcom.sun.management.jmxremote.authenticate=false -> no headaches
// -Dcom.sun.management.jmxremote.ssl=false -> no headahces
// -Dssl.port=8443 -> the ssl port to manage - must match that in server.xml
//
// note that this will only work with the 5.x series of tomcat
//

public class QSizeMonitor extends Thread {
   
   boolean fStoppedServlets = false;
   boolean fPausedHousekeeping = false;
   
   long maxFilesForInput = 0;
   long minFilesForInput = 0;
   long maxRecordsForHouseKeeping = 0;
   long minRecordsForHouseKeeping = 0;
   CollectorService fService;

   private long getPropertiesValue(Properties p, String valuename)
   {
      try {
         String value = p.getProperty(valuename);
         if (value == null) {
            return 0;
         }
         return Long.parseLong(value);
      }
      catch (java.lang.NumberFormatException e) {
         Logging.warning("QSizeMonitor: caught exception " + valuename +
                         " property", e);
      }
      return 0;
   }

   public QSizeMonitor(CollectorService service) {
      final long restartThreshold = 80;
      final long restartThresholdForHouseKeeping = 10;

      fService = service;

      Properties p = Configuration.getProperties();

      maxFilesForInput = getPropertiesValue(p,"max.q.size");
      minFilesForInput = (long)restartThreshold * maxFilesForInput / 100;
      
      maxRecordsForHouseKeeping = getPropertiesValue(p,"max.housekeeping.nrecords");
      minRecordsForHouseKeeping = getPropertiesValue(p,"min.housekeeping.nrecords");
      if (maxRecordsForHouseKeeping > 0) {
         if (minRecordsForHouseKeeping >= maxRecordsForHouseKeeping) {
            Logging.warning("QSizeMonitor: restart threshold for housekeeping ("+minRecordsForHouseKeeping+")"+
                            " is less than the max to stop ("+maxRecordsForHouseKeeping+") using "+
                            maxRecordsForHouseKeeping*restartThresholdForHouseKeeping+" instead.");
            minRecordsForHouseKeeping = maxRecordsForHouseKeeping * restartThresholdForHouseKeeping / 100;
         } else if (minRecordsForHouseKeeping == 0) {
            // Zero is unlikely to happen when the QSizeMonitor runs.
            minRecordsForHouseKeeping = maxRecordsForHouseKeeping * restartThresholdForHouseKeeping / 100;
         }
      }
   }
   
   public void run() {
      Logging.fine("QSizeMonitor: Started");
      while (true) {
         try {
            Thread.sleep(60 * 1000);
         }
         catch (Exception e) {
         }
         check();
      }
   }
   
   public void check() {
      
      Logging.log("QSizeMonitor: Checking");
      
      long maxFiles = 0;
      long maxRecords = 0;
      for (int i = 0; i < QueueManager.getNumberOfQueues(); ++i) {
         QueueManager.Queue queue = QueueManager.getQueue(i);
         long nfiles = queue.getNFiles();
         long nrecords = queue.getNRecords();
         
         if (nfiles > maxFiles)
            maxFiles = nfiles;         
         if (nrecords > maxRecords)
            maxRecords = nrecords;
      }
      if (maxFilesForInput > 0) {
         if (fService.servletEnabled()) {
            if ( maxFiles > maxFilesForInput ) {
               Logging.info("QSizeMonitor: Queue Size Exceeded: " + maxFiles);
               Logging.info("QSizeMonitor: Shutting Down Input");
               fService.disableServlet();
               fStoppedServlets = true;
            }
         } else if (fStoppedServlets) {
            // If the QSizeMonitor stopped the servlets, it is allowed to
            // restart them
            if (maxFiles < minFilesForInput) {
               Logging.info("QSizeMonitor: Restarting Input: " + maxFiles);
               fService.enableServlet();
               fStoppedServlets = false;
            }
         }
      }
      if (maxRecordsForHouseKeeping > 0) {
         if (fService.housekeepingRunning()) {
            if ( maxRecords > maxRecordsForHouseKeeping ) {
               Logging.info("QSizeMonitor: HouseKeeping max number of records in queue exceeded: " + maxRecords);
               Logging.info("QSizeMonitor: Pausing Housekeeping.");
               fService.pauseHousekeepingService();
               fPausedHousekeeping = true;
            }
         } else if (fPausedHousekeeping) {
            // If the QSizeMonitor stopped the servlets, it is allowed to
            // restart them
            if (maxRecords < (minRecordsForHouseKeeping)) {
               Logging.info("QSizeMonitor: Restarting Housekeeping with " + maxRecords + " records in queue");
               if (fService.housekeepingServiceStatus().equals("PAUSED") ) {
                  fService.startHousekeepingActionNow();
               }
               fPausedHousekeeping = false;
            }
         }
      }
   }
   
   final String bean_servlets = "Catalina:j2eeType=WebModule," + "name=//localhost/gratia-servlets,J2EEApplication=none,J2EEServer=none";
   
   void startServletService() {
      servletCommand(bean_servlets,"start",false);
   }
   
   void stopServletService() {
      servletCommand(bean_servlets,"stop",false);
   }
   
   static Boolean servletCommand(String bean_name, String cmd, boolean ignore_missing) {
      Boolean result = false;
      
      try {
         MBeanServerConnection mbsc = null;
         if (System.getProperty("com.sun.management.jmxremote") == null) {
            Logging.log(LogLevel.SEVERE, "CollectorService: internal servlet " +
                        "control is not available." +
                        " Please ensure that the system property " +
                        "com.sun.management.jmxremote" +
                        " is set to allow required control of " +
                        "servlets and reporting service.");
            return result; // No point in continuing
         }
         Logging.log("CollectorService: attempting to obtain local MBeanServer");
         mbsc = ManagementFactory.getPlatformMBeanServer();
         
         ObjectName objectName = new ObjectName(bean_name);
         
         mbsc.invoke(objectName, cmd, null, null);
         Logging.log("CollectorService: successfully executed MBean control command " +
                     cmd + " on MBean " + bean_name);
         result = true;
      }
      catch (javax.management.InstanceNotFoundException missing) {
         // We might want to ignore this error.
         if (ignore_missing) {
            result = true; // Didn't care whether it was there or not.
         } else {
            Logging.warning("CollectorService: ServletCommand(\"" +
                            cmd + "\") caught exception " + missing);
            Logging.debug("Exception details: ", missing);
         }
      }
      catch (Exception e) {
         Logging.warning("CollectorService: ServletCommand(\"" +
                         cmd + "\") caught exception " + e);
         Logging.debug("Exception details: ", e);
      }
      return result;
   }
}




package net.sf.gratia.services;

import net.sf.gratia.util.Logging;
import net.sf.gratia.services.DataScrubber;
import net.sf.gratia.storage.Duration;
import net.sf.gratia.storage.Duration.*;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class DataHousekeepingService extends Thread {
   
   private enum Status {
      STOPPED,
      INITIALIZING,
      RUNNING,
      PAUSING,
      PAUSED,
      SLEEPING,
      STOPPING;
   }
   
   private CollectorService collectorService = null;
   private boolean pauseRequested = false;
   private boolean stopRequested = false;
   private boolean sleepEnabled = true;
   private Duration fCheckInterval;
   private boolean fIsTesting = false;
   
   private static final int defaultCheckIntervalDays = 2;
   
   private DataScrubber housekeeper = new DataScrubber(this);
   
   public enum HousekeepingAction { 
      ALL, DUPRECORD, TRACE, JOBUSAGEXML, METRICXML, METRICRECORD,
      JOBUSAGERECORD, CE, SE, CERECORD, SERECORD, SUBCLUSTER,
      SERVICESUMMARY, SERVICESUMMARYHOURLY, ORIGIN, TESTING, NONE;
      private Lock l = new ReentrantLock();
      public Boolean tryLock() {
         return l.tryLock();
      }
      public void lock() {
         l.lock();
      }
      public void unlock() {
         l.unlock();
      }
   }
   
   private HousekeepingAction defaultAction = HousekeepingAction.ALL;
   private HousekeepingAction currentAction = HousekeepingAction.NONE;
   private Status currentStatus  = Status.STOPPED;
   private Status previousStatus = Status.STOPPED;
   
   public DataHousekeepingService(CollectorService cS) {
      initialize(cS);
   }
   
   public DataHousekeepingService(CollectorService cS,
                                  HousekeepingAction action) {
      initialize(cS);
      defaultAction = action;
   }
   
   public DataHousekeepingService(CollectorService cS,
                                  HousekeepingAction action,
                                  Boolean iD) {
      initialize(cS);
      defaultAction = action;
      sleepEnabled = iD;
   }
   
   private void initialize(CollectorService cS) {
      currentStatus = Status.INITIALIZING;
      collectorService = cS;
      Properties p = net.sf.gratia.util.Configuration.getProperties();
      String checkIntervalDays = p.getProperty("service.lifetimeManagement.checkIntervalDays");
      if ((checkIntervalDays != null) && (checkIntervalDays.length() > 0)) {
         Logging.info("DataHousekeepingService: found obsolete property " +
                      "service.lifetimeManagement.checkIntervalDays\n" +
                      "Please use service.lifetimeManagement.checkInterval instead " +
                      "with value <num> [hdwmy] (default [d]ays)");
         try {
            fCheckInterval = new Duration(checkIntervalDays, DurationUnit.DAY);
         } catch (DurationParseException e) {
            Logging.warning("DataHouseKeepingService: caught exception " +
                            "parsing service.lifetimeManagement.checkIntervalDays property", e);
            fCheckInterval = new Duration(defaultCheckIntervalDays, DurationUnit.DAY);
         }
      } else { // Preferred control
         try {
            fCheckInterval =
            new Duration(p.getProperty("service.lifetimeManagement.checkInterval",
                                       defaultCheckIntervalDays +
                                       " d"), DurationUnit.DAY);
         }
         catch (DurationParseException e) {
            Logging.warning("DataHouseKeepingService: caught exception " +
                            "parsing service.lifetimeManagement.checkInterval property", e);
            fCheckInterval = new Duration(defaultCheckIntervalDays, DurationUnit.DAY);
         }
      }
      // Detect if we are running the test
      String secure = p.getProperty("service.secure.connection");
      if (secure.equals(p.getProperty("service.open.connection"))) {
         // Since the open and secure connection are the same we can safely assume
         // that we are running runPurge.sh and thus we should spend a minute or
         // two sleeping if there is no work in order to be able to test the 
         // pausing of this services
         fIsTesting = true;
      }
   }
   
   public boolean isRunning() {
      // Return true if the housekeeping is currently in the process of deleting data.
      
      return (isAlive() && currentStatus == Status.RUNNING);
   }

   public String housekeepingStatus() {
      // Reset status if neccessary
      if (!isAlive()) {
         currentStatus = Status.STOPPED;
      }
      return ((currentStatus == Status.RUNNING)?currentAction.toString():currentStatus.toString());
   }
   
   public void requestStop() {
      // for the housekeeping service to stop.
      // This supersedes any request to pause.
      currentStatus = Status.STOPPING;
      housekeeper.requestStop();
      stopRequested = true;
      pauseRequested = false;
   }
   
   public void requestPause() 
   {
      // Request for the housekeeping service to take a pause.
      // The request is ignored if we already have a request to stop.
      if (!stopRequested) {
         previousStatus = currentStatus;
         currentStatus = Status.PAUSING;
         pauseRequested = true;
         housekeeper.requestPause();
      }
   }
   
   public void pause() 
   {
      while (pauseRequested) {
         try {
            long checkInterval = this.fCheckInterval.msFromDate(new Date());
            Logging.info("DataHousekeepingService paused going to sleep for " +
                         checkInterval + "ms. or until woke up");
            currentStatus = Status.PAUSED;
            Thread.sleep(checkInterval);
            Logging.info("DataHousekeepingService woke up after pause");
         } catch (InterruptedException x) {
            Logging.debug("DataHousekeepingService pause was interrupted");
         }
      }
      if (stopRequested) {
         Logging.info("DataHousekeepingService woke up with a request to stop.");         
      } else {
         Logging.info("DataHousekeepingService woke up, continuing where we left off.");
         currentStatus = previousStatus;
      }
   }

   public void wakeUp() {
      pauseRequested = false;
      housekeeper.cancelPause();
      interrupt();
   }

   @Override
   public void run() {
      Logging.info("DataHousekeepingService started");
      while (!stopRequested) {
         if (sleepEnabled) {
            currentStatus = Status.SLEEPING;
            try {
               long checkInterval = this.fCheckInterval.msFromDate(new Date());
               Logging.debug("DataHousekeepingService: going to sleep for " +
                             checkInterval + "ms.");
               Thread.sleep(checkInterval);
            }
            catch (Exception e) {
               // Ignore
            }
         } else {
            // Re-enable sleep every time we skip one.
            // I.e. this let's us skip the first sleep
            // or the code below could request an immediate re-run.
            sleepEnabled = true;
         }
         if (!stopRequested) {
            currentStatus = Status.RUNNING;
            executeHousekeeping(defaultAction); 	 
         }
      }
      Logging.info("DataHousekeepingService exiting");        
   }
   
   private long executeHousekeeping(HousekeepingAction action) {
      long result = 0;
      if (action == null) return result;
      currentAction = action;
      Logging.info("DataHousekeepingService: " + action + ".");
      switch(action) {
         case ALL:
            for (HousekeepingAction a : HousekeepingAction.values()) {
               if ((a == HousekeepingAction.ALL) || (a == HousekeepingAction.NONE)) {
                  continue;
               } 
               if (stopRequested) {
                  currentStatus = Status.STOPPING;
                  break;
               } else {
                  if (pauseRequested) {
                     pause();
                     if (stopRequested) {
                        currentStatus = Status.STOPPING;
                        break;
                     }
                  }
                  if (a == HousekeepingAction.TESTING) {
                     if (fIsTesting && result == 0) {
                        result = result + executeHousekeeping(a);
                     }
                  } else {
                     result = result + executeHousekeeping(a);
                  }
               }
            }
            break;
         case TESTING:
            // Let's allow for testing pausability.
            // This code should be a representation of the way the sub-routine
            // checks for stop/pause.
            if (action.tryLock()) {
               try {
                  result = housekeeper.testLooping();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case JOBUSAGEXML:
            if (action.tryLock()) {
               try {
                  result = housekeeper.JobUsageRawXml();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case METRICXML:
            if (action.tryLock()) {
               try {
                  result = housekeeper.MetricRawXml();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case METRICRECORD:
            if (action.tryLock()) {
               try {
                  result = housekeeper.IndividualMetricRecords();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case JOBUSAGERECORD:
            if (action.tryLock()) {
               try {
                  result = housekeeper.IndividualJobUsageRecords();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case CE:
            if (action.tryLock()) {
               try {
                  result = housekeeper.IndividualComputeElement();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case SE:
            if (action.tryLock()) {
               try {
                  result = housekeeper.IndividualStorageElement();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case SERECORD:
            if (action.tryLock()) {
               try {
                  result = housekeeper.IndividualStorageElementRecord();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case CERECORD:
            if (action.tryLock()) {
               try {
                  result = housekeeper.IndividualComputeElementRecord();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case SUBCLUSTER:
            if (action.tryLock()) {
               try {
                  result = housekeeper.IndividualSubclusterRecord();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case SERVICESUMMARY:
            if (action.tryLock()) {
               try {
                  result = housekeeper.MasterServiceSummary();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case SERVICESUMMARYHOURLY:
            if (action.tryLock()) {
               try {
                  result = housekeeper.MasterServiceSummaryHourly();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case DUPRECORD:
            if (action.tryLock()) {
               try {
                  result = housekeeper.DupRecord();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case TRACE:
            if (action.tryLock()) {
               try {
                  result = housekeeper.Trace();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         case ORIGIN:
            if (action.tryLock()) {
               try {
                  result = housekeeper.Origin();
               }
               finally {
                  action.unlock();
               }
            }
            break;
         default:
      }
      Logging.info("DataHousekeepingService: "+action+" has deleted "+result+" rows.");
      Logging.info("DataHousekeepingService: " + action + " complete.");
      return result;
   }
   
   java.util.List<Object[]> GetTableStatus()
   {
      return housekeeper.GetTableStatus();
   }
}

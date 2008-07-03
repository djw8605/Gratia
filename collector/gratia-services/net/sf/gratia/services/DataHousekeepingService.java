package net.sf.gratia.services;

import net.sf.gratia.util.Logging;
import net.sf.gratia.storage.DataScrubber;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataHousekeepingService extends Thread {

    private enum Status {
        STOPPED,
            INITIALIZING,
            RUNNING,
            SLEEPING,
            STOPPING;
    }

    private CollectorService collectorService = null;
    private Boolean stopRequested = false;
    private Boolean sleepEnabled = true;
    private Date lastCompletionDate;

    private DataScrubber housekeeper = new DataScrubber();
    private long checkInterval;
    public enum HousekeepingAction { 
        ALL, JOBUSAGEXML, METRICXML, METRICRECORD, JOBUSAGERECORD, DUPRECORD, NONE;
        private Lock l = new ReentrantLock();
        public Boolean tryLock() {
            return l.tryLock();
        }
        public void Lock() {
            l.lock();
        }
        public void unlock() {
            l.unlock();
        }
    }

    private HousekeepingAction defaultAction = HousekeepingAction.ALL;
    private HousekeepingAction currentAction = HousekeepingAction.NONE;
    private Status currentStatus = Status.STOPPED;

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
        checkInterval = 24 * 3600 * 1000 *
            Long.valueOf(p.getProperty("service.lifetimeManagement.checkIntervalDays",
                                       "2"));
    }

    public String housekeepingStatus() {
        // Reset status if neccessary
        if (!isAlive()) {
            currentStatus = Status.STOPPED;
        }
        return ((currentStatus == Status.RUNNING)?currentAction.toString():currentStatus.toString());
    }

    public Date lastCompletionDate() {
        return lastCompletionDate;
    }

    public void requestStop() {
        currentStatus = Status.STOPPING;
        stopRequested = true;
    }

    public void run() {
        Logging.info("DataHousekeepingService started");
        while (!stopRequested) {
            if (sleepEnabled) {
                currentStatus = Status.SLEEPING;
                try {
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
                Boolean md5v2Status = false;
                try {
                    md5v2Status = collectorService.checkMd5v2Unique();
                } catch (Exception e) {
                    // Ignore
                }
                if (md5v2Status) {
                    executeHousekeeping(defaultAction);
                    lastCompletionDate = new Date();
                } else {
                    Logging.info("DataHousekeepingService: checksums are not " + 
                                 "upgraded yet -- going back to sleep");
                }
            }
        }
        Logging.info("DataHousekeepingService exiting");        
    }

    private Boolean executeHousekeeping(HousekeepingAction action) {
        Boolean result = false;
        currentAction = action;
        switch(action) {
        case ALL:
            for (HousekeepingAction a : HousekeepingAction.values()) {
                if (a == HousekeepingAction.ALL) {
                    continue;
                }
                if (stopRequested) {
                    currentStatus = Status.STOPPING;
                    break;
                } else {
                    executeHousekeeping(a);
                }
            }
            result = true; // Don't care how many we actually executed.
            break;
        case JOBUSAGEXML:
            if (action.tryLock()) {
                try {
                    housekeeper.JobUsageRawXml();
                    result = true; // OK
                }
                finally {
                    action.unlock();
                }
            }
            break;
        case METRICXML:
            if (action.tryLock()) {
                try {
                    housekeeper.MetricRawXml();
                    result = true; // OK
                }
                finally {
                    action.unlock();
                }
            }
            break;
        case METRICRECORD:
            if (action.tryLock()) {
                try {
                    housekeeper.IndividualMetricRecords();
                    result = true; // OK
                }
                finally {
                    action.unlock();
                }
            }
            break;
        case JOBUSAGERECORD:
            if (action.tryLock()) {
                try {
                    housekeeper.IndividualJobUsageRecords();
                    result = true; // OK
                }
                finally {
                    action.unlock();
                }
            }
            break;
        case DUPRECORD:
            if (action.tryLock()) {
                try {
                    housekeeper.DupRecord();
                    result = true; // OK
                }
                finally {
                    action.unlock();
                }
            }
            break;
        default:
        }
        return result;
    }
}

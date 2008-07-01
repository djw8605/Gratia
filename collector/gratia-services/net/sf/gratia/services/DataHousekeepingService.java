package net.sf.gratia.services;

import net.sf.gratia.util.Logging;
import net.sf.gratia.storage.DataScrubber;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataHousekeepingService extends Thread {

    private CollectorService collectorService = null;
    private Boolean stopRequested = false;
    private Boolean initialDelay = true;
    private Date lastCompletionDate;

    private DataScrubber housekeeper = new DataScrubber();
    private long checkInterval;
    public enum HousekeepingAction { 
        ALL, JOBUSAGEXML, METRICXML, METRICRECORD, JOBUSAGERECORD, DUPLICATE;
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
        initialDelay = iD;
    }

    private void initialize(CollectorService cS) {
        collectorService = cS;
        Properties p = net.sf.gratia.util.Configuration.getProperties();
        checkInterval = 24 * 3600 * 1000 *
            Long.valueOf(p.getProperty("service.lifetime.checkIntervalDays",
                                       "2"));
    }

    public Date lastCompletionDate() {
        return lastCompletionDate;
    }

    public void requestStop() {
        stopRequested = true;
    }

    public void run() {
        Logging.info("DataHousekeepingService started");
        while (!stopRequested) {
            try {
                Thread.sleep(checkInterval);
            }
            catch (Exception e) {
                // Ignore
            }
            if (!stopRequested) {
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
        switch(action) {
        case ALL:
            for (HousekeepingAction a : HousekeepingAction.values()) {
                if (a == HousekeepingAction.ALL) {
                    continue;
                }
                if (stopRequested) {
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
        case DUPLICATE:
            if (action.tryLock()) {
                try {
                    housekeeper.Duplicate();
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

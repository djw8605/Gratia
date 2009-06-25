package net.sf.gratia.services;

import net.sf.gratia.util.Logging;

import java.util.*;

import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.Session;

import net.sf.gratia.storage.Replication;

public class ReplicationService extends Thread {

    Hashtable<Integer, ReplicationDataPump> pumpStore =
        new Hashtable<Integer, ReplicationDataPump>();
    Properties p;
    Boolean stopRequested = false;
    
    public ReplicationService() {
        p = net.sf.gratia.util.Configuration.getProperties();
    }
    
    public void run() {
        Logging.info("ReplicationService Started");
        while (!stopRequested) {
            loop();
        }
        Logging.info("ReplicationService: Stop requested");
        // Stop all pumps.
        Enumeration<Integer> x = pumpStore.keys();
        while (x.hasMoreElements()) {
            Integer key = x.nextElement();
            // Need to get the pump and shut it down
            ReplicationDataPump pump = pumpStore.get(key);
            if ((pump != null) && (pump.isAlive())) {
                Logging.log("ReplicationService: Stopping DataPump: " + key);
                pump.exit();
            }
        }
        Logging.info("ReplicationService: Exiting");
    }


    public void requestStop() {
        stopRequested = true;
    }

    public void loop() {
        ReplicationDataPump pump = null;
        Session session = null;
        Hashtable<Integer, Integer> checkedPumps = new Hashtable<Integer, Integer>();
        try {
            session = HibernateWrapper.getSession();
            Query rq =
                session.createQuery("select replicationEntry from " +
                                    "Replication replicationEntry ")
                .setCacheMode(CacheMode.IGNORE);
            Iterator rIter = rq.iterate();
            while (rIter.hasNext()) {
                Replication replicationEntry = (Replication) rIter.next();
                // Logging.debug("Entity name of replication entry: " +
                //               session.getEntityName(replicationEntry));
                Integer replicationid = replicationEntry.getreplicationid();
                Integer running = replicationEntry.getrunning();
                checkedPumps.put(replicationid, running);

                pump = pumpStore.get(replicationid);
                if ((running == 1) && ((pump == null) || (!pump.isAlive()))) {
                    Logging.log("ReplicationService: Starting DataPump: " + replicationid);
                    pump = new ReplicationDataPump(replicationid);
                    pumpStore.put(replicationid,pump);
                    pump.start();
                }
            }
            session.close();
        }
        catch (Exception e) {
            if ((session != null) && session.isOpen()) session.close();
            Logging.warning("ReplicationService: caught exception " +
                            e + " trying to check and/or start new replication data pumps.");
            Logging.debug("ReplicationService: exception detail follows: ", e);
        }
        //
        // now - loop through running threads, find out if they're still wanted
        // and stop them if not.
        //
        for (Enumeration<Integer> x = pumpStore.keys(); x.hasMoreElements();) {
            Integer replicationId = x.nextElement();
            try {
                Integer running = checkedPumps.get(replicationId);
                if (running == null || running.intValue() == 0) {
                    // Need to get the pump and shut it down
                    pump = pumpStore.get(replicationId);
                    if ((pump != null) && (pump.isAlive())) {
                        Logging.log("ReplicationService: Stopping DataPump: " + replicationId);
                        pump.exit();
                    }
                }
            }
            catch (Exception e) {
                Logging.warning("ReplicationService: caught exception " +
                                e + " trying to check and/or shut down " +
                                "replication pump ID " + replicationId);
                Logging.debug("ReplicationService: exception detail follows: ", e);
            }
        }
        try {
            Logging.log("ReplicationService: Sleeping");
            long wait = Integer.parseInt(p.getProperty("service.replication.wait"));
            wait = wait * 60 * 1000;
            Thread.sleep(wait);
        }
        catch (Exception ignore) {
        }
    }
}

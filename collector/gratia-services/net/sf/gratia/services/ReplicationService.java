package net.sf.gratia.services;

import net.sf.gratia.util.Logging;

import java.util.*;

import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.Session;

import net.sf.gratia.storage.Replication;

public class ReplicationService extends Thread {
   
   boolean isSleeping = false;
   Hashtable<Long, ReplicationDataPump> pumpStore =
   new Hashtable<Long, ReplicationDataPump>();
   Properties p;
   Boolean stopRequested = false;
   
   public ReplicationService() {
      p = net.sf.gratia.util.Configuration.getProperties();
   }
   
   public synchronized boolean isSleeping() {
      return isSleeping;
   }

   public void run() {
      Logging.info("ReplicationService Started");
      while (!stopRequested) {
         loop();
      }
      Logging.info("ReplicationService: Stop requested");
      // Stop all pumps.
      Enumeration<Long> x = pumpStore.keys();
      while (x.hasMoreElements()) {
         Long key = x.nextElement();
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
      Hashtable<Long, Integer> checkedPumps = new Hashtable<Long, Integer>();
      try {
         session = HibernateWrapper.getSession();
         org.hibernate.Transaction tx = session.beginTransaction();
         Query rq = session.createQuery("select replicationEntry from " +
                                        "Replication replicationEntry ")
            .setCacheMode(CacheMode.IGNORE);
         Iterator rIter = rq.iterate();
         while (rIter.hasNext()) {
            Replication replicationEntry = (Replication) rIter.next();
            // Logging.debug("Entity name of replication entry: " +
            //               session.getEntityName(replicationEntry));
            Long replicationid = replicationEntry.getreplicationid();
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
         tx.commit();
         session.close();
      }
      catch (Exception e) {
         HibernateWrapper.closeSession(session);
         Logging.warning("ReplicationService: caught exception " +
                         e + " trying to check and/or start new replication data pumps.");
         Logging.warning("ReplicationService: exception detail follows: ", e);
      }
      //
      // now - loop through running threads, find out if they're still wanted
      // and stop them if not.
      //
      for (Enumeration<Long> x = pumpStore.keys(); x.hasMoreElements();) {
         Long replicationId = x.nextElement();
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
         isSleeping = true;
         Thread.sleep(wait);
         isSleeping = false;
      }
      catch (Exception ignore) {
      }
   }
}

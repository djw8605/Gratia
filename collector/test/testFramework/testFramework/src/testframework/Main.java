/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package testframework;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.gratia.util.*;

import net.sf.gratia.storage.*;
import net.sf.gratia.services.*;

/**
 *
 * @author pcanal
 */
public class Main {

   static void testingParsing() {
      String[] filenames = XP.getFileList("test-data", "xml");
      System.out.println("Files: " + filenames.length);
      RecordUpdaterManager updater = new RecordUpdaterManager();
      JobUsageRecordUpdater.AddDefaults(updater);
      RecordConverter converter = new RecordConverter();

      Logging.initialize("test.bsh"); // "logfile","5000","1","OFF","1");

      int record_idx = 0;
      for (int i = 0; i < filenames.length; i++) {
         System.out.println("Processing: " + filenames[i]);
         String xml = XP.get(filenames[i]);

         ArrayList records;
         try {
            records = converter.convert(xml);
         } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            continue;
         }
         for (int j = 0; j < records.size(); j++) {
            Record current = (Record) records.get(j);
            System.err.println("Seen record " + j + "/" + records.size());
         }
      }
   }

   static boolean startupHibernate() {

      try {
         HibernateWrapper.start();
      } catch (Exception ex) {
         Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
      }
      if (!HibernateWrapper.databaseUp()) {
         try {
            HibernateWrapper.start();
         } catch (Exception e) { // Ignore
         }
         if (HibernateWrapper.databaseDown) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null);
            return false;
         }
      }
      return true;
   }

   static void testingListener() {
      Logging.initialize("testFramework");

      CollectorService collector = new CollectorService();
      Object lock = new Object();
      Hashtable global = new Hashtable();

      if (! startupHibernate() ) {
         return;
      }
      ListenerThread thread = new ListenerThread("testing", "thread0", lock, global, collector);
      // thread.run();


      int nfiles = 0;
      do {
         nfiles = thread.loop();
      } while (nfiles != 0);
   }

   static void testingReplication() {
      Logging.initialize("testFramework");

      if (! startupHibernate() ) {
         return;
      }
      ReplicationService replicationService = new ReplicationService();
      replicationService.start();
   }
   /**
    * @param args the command line arguments
    */
   public static void main(String[] args) {
      //testingParsing();
      testingListener();
      //testingReplication();
   }
}
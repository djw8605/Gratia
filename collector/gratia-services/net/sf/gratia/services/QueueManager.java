package net.sf.gratia.services;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.XP;
import net.sf.gratia.util.Logging;
import net.sf.gratia.util.LogLevel;
import net.sf.gratia.util.Execute;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;

import java.io.File;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Title: Queue </p>
 *
 * <p>Description: Object in charge on creating and deleting file in the queue directory and maintaining the CollectorStatus table</p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Chris Green
 * @version 1.0
 */
public class QueueManager 
{
   //   private static final Pattern fgRecordIdPattern = Pattern.compile("RecordIdentity");
   //   private static final Pattern fgReplicationPattern = Pattern.compile("replication\|");
   private static final Pattern fgRecordPattern = Pattern.compile("(RecordIdentity)|(replication\\|)");
   private static final Pattern fgBundleSizePattern = Pattern.compile("bundleSize=\\s*([0-9]*)");
   private static final Pattern fgSizeInFileName = Pattern.compile("\\.([0-9]*)\\.xml");
   
   private static final String fgUpdateStatus = "insert into CollectorStatus values ( :queue, now(), :nfiles, :nrecords) "
                                                + " on duplicate key update UpdateDate=now(), Records=Records+:nrecords, Files=Files+:nfiles";
   private static final String fgClearStatus = "delete from CollectorStatus where Name = :queue";
   

   private final static Lock fgFileLock = new ReentrantLock();
   static Queue   fgQueues[] = null;
   static File    fgStageDir = null;
   
   static Date fgLastReset = null;
   
   public static class Queue 
   {
      // Describe and offer handle to a Collector queue.
      
      String fQueueName;
      File   fDirectory;
      
      public Queue(String path)
      {
         // Create a queue object ; also insure the underlying directory exist.
         
         fQueueName = path;
         fDirectory = new File(path);
         fDirectory.mkdirs();
      }
      
      public void clearStatusTable() 
      {
         // Clear the status row for one queue.
         
         Boolean keepTrying  = true;
         Integer nTries = 0;
         Session session = HibernateWrapper.getSession();
         try {
            Transaction tx = session.beginTransaction();
            if (++nTries > 1) {
               Thread.sleep(300);
            }
            
            org.hibernate.SQLQuery query = session.createSQLQuery( fgClearStatus );
            query.setString( "queue", fQueueName );
            
            Logging.debug("Queue::clearStatusTable: About to execute " + query.getQueryString() + " with queue = " + fQueueName );
            long updated = query.executeUpdate();
            
            session.flush();
            tx.commit();
            keepTrying = false;
            session.close();
         } catch (Exception e) {
            HibernateWrapper.closeSession(session);
            if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "registerInput")) {
               Logging.warning("Queue::clearStatusTable: error when updating CollectorStatus table: " + e.getMessage());
               Logging.debug("Queue::clearStatusTable: exception details:", e);
               keepTrying = false;
               
               boolean status_out_of_date = true;
            }
         }
      }
      
      void deleteFile(String filename, String from, long nrecords)
      {
         // Delete one of the file in the queue and update the CollectorStatus accordingly.
         
         try {
            File temp = new File(filename);
            synchronized(this) {
               temp.delete();
            }
            updateStatus(from,-nrecords,-1);
         } catch (Exception ignore) {
            // Logging.log(ident + ": File Delete Failed: " + file +
            // " Error: " + ignore);
         }         
      }
      
      public String [] getFileList() 
      {
         // Return the list of all the files in the queue.
         return XP.getFileList(fDirectory.toString());
      }
      
      public String getShortName()
      {
         // Return the basename of the directory in which the queue keeps its data.
         // To be used for history recording, etc.
         
         return fDirectory.getName();
      }

      public String toString() 
      {
         // Return the string representation of the queue
         return fDirectory.toString();
      }
      
      public void updateStatus(String from, long nrecords, long nfiles)
      {
         // Update the status table regarding this queue.
         QueueManager.updateStatus(toString(),from,nrecords,nfiles);
      }
      
      public boolean save(File stagedFile, String from, String suffix, long nrecords)
      {
         // Move the stagedFile to the queue.
         
         try {
            boolean result = false;
            File file = File.createTempFile("job", suffix, fDirectory);
            synchronized(this) {
               result = stagedFile.renameTo(file);
            }
            updateStatus(from,nrecords,1);
            return result;
         } catch (Exception e) {
            Logging.warning("queue::save: Failed to add the record to the queue: "+toString(),e);
            return false;
         }
      }
   }

   public static synchronized void setupQueues(int nthreads) 
   {
      //
      // setup queues for message handling
      //
      String configurationPath = System.getProperty("catalina.home") + "/gratia";
      if (fgQueues == null || fgQueues.length != nthreads) {
         fgQueues = new Queue[nthreads];
         Execute.execute("mkdir -p " + configurationPath + "/data");
         for (int i = 0; i < nthreads; i++) {
            fgQueues[i] = new Queue(configurationPath + "/data/thread" + i);
            Logging.log("Created Q: " + fgQueues[i]);
         }
      }
      resetStatus();
   }

   public static synchronized void initialize()  
   {
      if (fgQueues == null) {
         
         Properties p = null;
         try {
            p = Configuration.getProperties();
         } catch (Exception ignore) {
            return;
         }
         
         int maxthreads = Integer.parseInt(p.getProperty("service.recordProcessor.threads"));
         setupQueues(maxthreads);
         
         fgStageDir = new File(System.getProperties().getProperty("catalina.home")
                             + "/gratia/data/stage");
         fgStageDir.mkdirs();
      }
   }

   public static int getNumberOfQueues()
   {
      // Return the number of active queues.
      return fgQueues.length;
   }
   
   public static Queue getQueue(int which)
   {
      // Return the input directory name for a specific queue
      return fgQueues[which];
   }
   
   public static File getStageDir()
   {
      // Return the directory object where the servlets should stage their input.
      return fgStageDir;
   }
      
   public static boolean update(int queueNumber, File tmpFile, String from, String xml) 
   {
      
      // Acquire the number of records in the xml file (i.e. bundle size).
      String bundleSize;
      long nrecords = 0;
      
      Matcher bundleMatcher = fgBundleSizePattern.matcher(xml);
      if (bundleMatcher.find()) {
         bundleSize = bundleMatcher.group(1);
         nrecords = Long.parseLong(bundleSize);
         // Logging.log(LogLevel.SEVERE, "QueueManager::update: matched bundles size with:"+bundleMatcher.group()+" and "+bundleMatcher.group(1));               
      } else {
         Matcher recordMatcher = fgRecordPattern.matcher(xml);
         
         while (recordMatcher.find()) {
            // Logging.log(LogLevel.SEVERE, "QueueManager::update: matched record pattern with:"+recordMatcher.group()+" and "+recordMatcher.group(1)+" and "+recordMatcher.group(2));               
            if (recordMatcher.group(1).length()>0) {
               nrecords = nrecords + 1;
            } else if (recordMatcher.group(2).length()>0) {
               nrecords = nrecords + 1;
            } else {
               // Logging.log(LogLevel.SEVERE, "QueueManager::update: internal error in the pattern matching, we did not understand the match:"+recordMatcher.group());               
            }
         }
         bundleSize = Long.toString(nrecords);
      }
      
      // Determine the suffix/sub-location of the file.
      String suffix = "."+bundleSize + ".xml";
      if (from != null && from.length() != 0) {
         String from_filename = from.replaceAll("[:/\\.]","_");
         suffix = "." + from_filename + suffix;
      }
      
      Logging.log(LogLevel.DEBUG, "QueueManager::update using suffix: "+suffix);
      return fgQueues[queueNumber].save(tmpFile,from,suffix,nrecords);
   }

   public static void resetStatus()
   {
      // Reset the status counter to synchronize them with what is currently on disk.

      Logging.info("ManagerQueue.resetStatus: started");
      for (Queue queue : fgQueues) {
         // Prevent any updates to the table or the disk
         String files [];
         synchronized (queue) {            
            // Clear the CollectorStatus table
            queue.clearStatusTable();
            
            // Get the list of files
            files = queue.getFileList();
            
            fgLastReset = new Date();
         }
         // Unlock the updates to the table and disk

         // Add up the information from the list of files.
         // The filename (see JMSProxyImpl::update) are of the form:
         //    job###.cleaned_up_from.nrecords.xml
         long nrecords = 0;
         long nfiles = files.length;
         for(String file : files) 
         {
            Matcher sizeMatcher = fgBundleSizePattern.matcher(file);
            if (sizeMatcher.find()) {
               String bundleSize = sizeMatcher.group(1);
               nrecords = Long.parseLong(bundleSize);
            } else {
               // The number of records is not encodded in the filename, let's look
               // at the data...
               String xml;
               try {
                  xml = XP.get(file);
               } catch(java.io.IOException e) {
                  Logging.debug("QueueManager.resetStatus: Unable to open input file "+file,e);
                  continue;
               }
               Matcher bundleMatcher = fgBundleSizePattern.matcher(xml);
               if (bundleMatcher.find()) {
                  String bundleSize = bundleMatcher.group(1);
                  nrecords = Long.parseLong(bundleSize);
                  // Logging.log(LogLevel.SEVERE, "QueueManager::reset: matched bundles size with:"+bundleMatcher.group()+" and "+bundleMatcher.group(1));               
               } else {
                  Matcher recordMatcher = fgRecordPattern.matcher(xml);
                  
                  while (recordMatcher.find()) {
                     // Logging.log(LogLevel.SEVERE, "QueueManager::reset: matched record pattern with:"+recordMatcher.group()+" and "+recordMatcher.group(1)+" and "+recordMatcher.group(2));               
                     if (recordMatcher.group(1).length()>0) {
                        nrecords = nrecords + 1;
                     } else if (recordMatcher.group(2).length()>0) {
                        nrecords = nrecords + 1;
                     } else {
                        // Logging.log(LogLevel.SEVERE, "QueueManager::reset: internal error in the pattern matching, we did not understand the match:"+recordMatcher.group());               
                     }
                  }
               }
            }
         } // For each file 
         queue.updateStatus("",nrecords,nfiles);  // Note: We can not determine the origin accurately.
      } // For each queue
      Logging.info("ManagerQueue.resetStatus: done");
   }
   
   public static void updateStatus(String queue, String from, long nrecords, long nfiles) 
   {
      // Update the status information to note that there is new files (nfiles)
      // containing new records (nrecords) coming from the probe 'from'.
      
      // This routines does a direct SQL update (rather than using Java object) in order
      // to be able to use the column = column + value idiom.

      Boolean keepTrying  = true;
      Integer nTries = 0;
      Session session = HibernateWrapper.getSession();
      try {
         Transaction tx = session.beginTransaction();
         if (++nTries > 1) {
            Thread.sleep(300);
         }
         
         org.hibernate.SQLQuery query = session.createSQLQuery( fgUpdateStatus );
         query.setString( "queue", queue );
         query.setLong( "nrecords", nrecords );
         query.setLong( "nfiles", nfiles);
         
         Logging.debug("updateStatus: About to execute " + query.getQueryString() + " with queue = " + queue + " and nrecords = " + nrecords + " and nfiles = " + nfiles );
         long updated = query.executeUpdate();
         
         session.flush();
         tx.commit();
         keepTrying = false;
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "registerInput")) {
            Logging.warning("updateStatus: error when updating CollectorStatus table: " + e.getMessage());
            Logging.debug("updateStatus: exception details:", e);
            keepTrying = false;
            
            boolean status_out_of_date = true;
         }
      }
   }
   
}
   

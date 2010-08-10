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
   
   private static final String fgUpdateStatus = "insert into CollectorStatus values ( :queue, :number, now(), :nfiles, :nrecords) "
                                                + " on duplicate key update UpdateDate=now(), Queue=:number, Records=Records+:nrecords, Files=Files+:nfiles";
   private static final String fgClearStatus = "delete from CollectorStatus where Name = :queue";

   private static String fgHostName = "localhost";
   
   private final static Lock fgFileLock = new ReentrantLock();
   static Queue              fgQueues[] = null;
   static java.io.File       fgStageDir = null;
   static Date               fgLastReset = null;
   
   public static class File
   {
      // Describe a file held in a queue.
      // In particular holds on to the number of records and the origin
      
      String fPath = null;
      String fFrom = null;
      long fNRecords = -1;
      
      public File(String path) 
      {
         fPath = path;
      }
      
      public void setFrom(String from)
      {
         // Set the name of the entity that sent us the file/records.
         // It can be a probe or a collector.
         
         fFrom = from;
      }
      
      public String getFrom() 
      {
         // Return the name of the entity that sent us the file/records.
         // It can be a probe or a collector.
         
         /*
          if (fFrom == null) {
             // Attempt to get the origina from the file name
          }
         */
         return fFrom;
      }
      
      public void setNRecords(long nrecords) 
      {
         // Set the number of record in this file.

         fNRecords = nrecords;
      }
      
      public long getNRecords() throws java.io.IOException, java.io.FileNotFoundException
      {
         // Return the number of records in the file
         // If the number has not been set explicitly, we look
         // at the file name and if we do not find it there, we 
         // opened up the file.
         // If we are unable to determinte the number of records
         // we throw an exception.

         if (fNRecords == -1) {
            if (fPath != null) {
               fNRecords = extractNRecordsFromFile(fPath);
            } else {
               throw new java.io.FileNotFoundException();
            }
         }
         return fNRecords;
      }
      
      public long getAge()
      {
         // Return the age of the file in milliseconds.
         
         java.io.File checkFile = new java.io.File(fPath);
         return (new Date().getTime() - checkFile.lastModified());
      }
      

      public String getData() throws java.io.IOException, java.io.FileNotFoundException
      {
         // Return the content of the file
         
         return XP.get(fPath);
      }
      
      public String getPath()
      {
         // Return the location of the file
         
         return fPath;
      }

      public void reset(String path)
      {
         // Re-use this File object for a new path.
         fPath = path;
         fFrom = null;
         fNRecords = -1;
      }
      
      public static long extractNRecordsFromFile(String path) throws java.io.IOException
      {
         // Return the number of records in the file
         
         long nrecords = 0;
         Matcher sizeMatcher = fgSizeInFileName.matcher(path);
         if (sizeMatcher.find()) {
            String bundleSize = sizeMatcher.group(1);
            nrecords = Long.parseLong(bundleSize);
            // Logging.log(LogLevel.SEVERE, "Queue::extractNRecordsFromFile: matched bundles size in file name with:"+sizeMatcher.group()+" and "+sizeMatcher.group(1));               
         } else {
            // The number of records is not encodded in the filename, let's look
            // at the data...
            String xml;
            try {
               xml = XP.get(path);
            } catch(java.io.IOException e) {
               Logging.debug("Queue::extractNRecordsFromFile: Unable to open input file "+path,e);
               throw e;
            }
            Matcher bundleMatcher = fgBundleSizePattern.matcher(xml);
            if (bundleMatcher.find()) {
               String bundleSize = bundleMatcher.group(1);
               nrecords = Long.parseLong(bundleSize);
               // Logging.log(LogLevel.SEVERE, "Queue::extractNRecordsFromFile: matched bundles size with:"+bundleMatcher.group()+" and "+bundleMatcher.group(1));               
            } else {
               Matcher recordMatcher = fgRecordPattern.matcher(xml);
               
               while (recordMatcher.find()) {
                  // Logging.log(LogLevel.SEVERE, "Queue::extractNRecordsFromFile: matched record pattern with:"+recordMatcher.group()+" and "+recordMatcher.group(1)+" and "+recordMatcher.group(2));               
                  if (recordMatcher.group(1).length()>0) {
                     nrecords = nrecords + 1;
                  } else if (recordMatcher.group(2).length()>0) {
                     nrecords = nrecords + 1;
                  } else {
                     // Logging.log(LogLevel.SEVERE, "Queue::extractNRecordsFromFile: internal error in the pattern matching, we did not understand the match:"+recordMatcher.group());               
                  }
               }
            }
         }
         return nrecords;
      }
      
   }
   
   public static class Queue 
   {
      // Describe and offer handle to a Collector queue.
      
      String       fQueueName;
      int          fQueueNumber;
      java.io.File fDirectory;
      boolean      fOutOfDate = false;
      
      public Queue(String path, int index)
      {
         // Create a queue object ; also insure the underlying directory exist.
         
         fDirectory = new java.io.File(path);
         fDirectory.mkdirs();
         fQueueName = fgHostName + ":" + fDirectory.getName();
         fQueueNumber = index;
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
               
               fOutOfDate = true;
            }
         }
      }
      
      void deleteFile(QueueManager.File file)
      {
         // Delete one of the file in the queue and update the CollectorStatus accordingly.
         
         try {
            java.io.File temp = new java.io.File(file.fPath);
            synchronized(this) {
               temp.delete();
            }
            updateStatus(file.getFrom(),-file.getNRecords(),-1);
         } catch (Exception ignore) {
            Logging.debug(fQueueName + ": File Delete Failed: " + file.fPath + " Error: ",ignore);
            fOutOfDate = true;
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
      
      public void renameTo(QueueManager.File oldfile, java.io.File newfile)
      {
         // Move one of the file out of the queue and update the CollectorStatus accordingly.

         java.io.File old = new java.io.File(oldfile.getPath());
         synchronized(this) {
            old.renameTo(newfile);
            updateStatus(oldfile);
         }
      }
                           
      public void refreshStatus()
      {
         // Reset the status counter to synchronize them with what is currently on disk.

         // Prevent any updates to the table or the disk
         String files [];
         synchronized (this) {            
            // Clear the CollectorStatus table
            clearStatusTable();
            
            // Get the list of files
            files = getFileList();
            
            // Need to set it here to avoid infinite recursion.
            fOutOfDate = false;
         }
         // Unlock the updates to the table and disk
         
         // Add up the information from the list of files.
         // The filename (see JMSProxyImpl::update) are of the form:
         //    job###.from.nrecords.xml
         long nrecords = 0;
         long nfiles = files.length;
         try {
            for(String file : files) 
            {
               nrecords = nrecords + File.extractNRecordsFromFile(file);
            } // For each file 
            updateStatus("",nrecords,nfiles);  // Note: We can not determine the origin accurately.
         } catch (java.io.IOException e) {
            // Problem during the status update
            Logging.warning("Queue::refreshStatus: Problem during the update (we will attempt to refresh the status at the next update):",e);
            fOutOfDate = true;
         }
      }
      
      public void updateStatus(QueueManager.File file)
      {
         // Update the status information to note that there is new files (nfiles)
         // containing new records (nrecords) coming from the probe 'from'.
         
         try {
            updateStatus(file.getFrom(), file.getNRecords(), -1);
         } catch (java.io.IOException e) {
            Logging.warning("Queue::updateStatus: Problem during the update (we will attempt to refresh the status at the next update):",e);
            fOutOfDate = true;
         }
         
      }
      
      public void updateStatus(String from, long nrecords, long nfiles)
      {
         // Update the status information to note that there is new files (nfiles)
         // containing new records (nrecords) coming from the probe 'from'.
            
         // This routines does a direct SQL update (rather than using Java object) in order
         // to be able to use the column = column + value idiom.
         
         if (fOutOfDate) {
            // There has been a problem has some point during the table updating,
            // let's recalculate from the actual files.
            refreshStatus();
         }
         
         Boolean keepTrying  = true;
         Integer nTries = 0;
         Session session = HibernateWrapper.getSession();
         try {
            Transaction tx = session.beginTransaction();
            if (++nTries > 1) {
               Thread.sleep(300);
            }
            
            org.hibernate.SQLQuery query = session.createSQLQuery( fgUpdateStatus );
            query.setString( "queue", fQueueName );
            query.setLong( "number", fQueueNumber );
            query.setLong( "nrecords", nrecords );
            query.setLong( "nfiles", nfiles);
            
            Logging.debug("updateStatus: About to execute " + query.getQueryString() + " with queue = " + fQueueName + " and nrecords = " + nrecords + " and nfiles = " + nfiles );
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
               
               fOutOfDate = true;
            }
         }
      }
      
      public boolean save(java.io.File stagedFile, String from, String suffix, long nrecords)
      {
         // Move the stagedFile to the queue.
         
         try {
            boolean result = false;
            java.io.File file = java.io.File.createTempFile("job", suffix, fDirectory);
            synchronized(this) {
               result = stagedFile.renameTo(file);
            }
            updateStatus(from,nrecords,1);
            return result;
         } catch (Exception e) {
            Logging.warning("queue::save: Failed to add the record to the queue : "+toString(),e);
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
         for (int i = 0; i < nthreads; i++) {
            fgQueues[i] = new Queue(configurationPath + "/data/thread" + i, i);
            Logging.log("Created Q: " + fgQueues[i]);
         }
      }
      refreshStatus();
   }

   public static synchronized void initialize()  
   {
      if (fgQueues == null) {
         
         Properties p = null;
         try {
            p = Configuration.getProperties();
            fgHostName = java.net.InetAddress.getLocalHost().toString(); 
         } catch (Exception ignore) {
            return;
         }
         
         int maxthreads = Integer.parseInt(p.getProperty("service.recordProcessor.threads"));
         setupQueues(maxthreads);
         
         fgStageDir = new java.io.File(System.getProperties().getProperty("catalina.home")
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
   
   public static java.io.File getStageDir()
   {
      // Return the directory object where the servlets should stage their input.
      return fgStageDir;
   }
   
   public static String getStatus() 
   {
      // Return the status of the QueueManager in regard to keeping track of the queue size
      
      int nOutOfDate = 0;
      for (Queue queue : fgQueues) {
         if (queue.fOutOfDate) {
            nOutOfDate += 1;
         }
      }
      if (nOutOfDate == 0) {
         return "UpToDate";
      } else if (nOutOfDate == fgQueues.length) {
         return "OutOfDate";
      } else {
         return "PartialOutOfDate";
      }
   }
      
      
   public static boolean update(int queueNumber, java.io.File tmpFile, String from, String xml) 
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

   public static void refreshStatus()
   {
      // Reset the status counter to synchronize them with what is currently on disk.

      Logging.info("ManagerQueue.refreshStatus: started");
      for (Queue queue : fgQueues) {
         queue.refreshStatus();
         fgLastReset = new Date();
      } // For each queue
      Logging.info("ManagerQueue.refreshStatus: done");
   }
}
   

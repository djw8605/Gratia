package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Execute;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;
import net.sf.gratia.util.LogLevel;
import java.util.regex.Pattern;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.*;
import java.sql.*;
import java.io.*;
import java.util.Date;
import java.text.*;

import net.sf.gratia.storage.*;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.apache.log4j.Level;

public class ReplicationDataPump extends Thread {
   private static final char dq = '\'';
   private static final char cr = '\n';
   private static final int default_bundle_size = 10;
   private static final double maxBundleDataSize = 0.9*2*1000*1000;
   private static Pattern tableFinder =
      Pattern.compile("(?:(?:Compute|Storage)Element(?:Record)?|Subcluster)");
   // Class attributes
   private Properties p;
   private long replicationId;
   private boolean trace;
   private int chunksize;

   // Class state
   private Replication replicationEntry = null;
   private String currentDestination = null;
   private String currentProbename = null;
   private boolean exitflag = false;
   private int nSentThisLoop = 0;
   private int nSentThisRun = 0;
   private Boolean firstLoopThisRun = true;
   private String replicatePath = "";

   public ReplicationDataPump(long replicationId) {
      this.replicationId = replicationId;
      p = Configuration.getProperties();
      
      String tmp = p.getProperty("service.datapump.trace");
      trace = tmp != null && tmp.equals("1");
      chunksize = 32000;
      tmp = p.getProperty("service.datapump.chunksize");
      if (tmp != null) {
         try {
            chunksize = Integer.parseInt(tmp); 
         } catch (Exception e) {
            replicationLog(LogLevel.WARNING,
                           "Parsing error (" + e +
                           ") when loading property" +
                           " service.datapump.chunksize with value: " +
                           tmp + " using default value (" +
                           chunksize + ")");
         }
      }
      replicatePath = "/var/lib/gratia-service/data/replicate";
      File dir = new File(replicatePath);
      if (!dir.exists()) {
         dir.mkdir();
      }
   }

   @Override
      public void run() {
      replicationLog(LogLevel.FINER, "Started run");
      if (!HibernateWrapper.databaseUp()) {
         try {
            HibernateWrapper.start();
         }
         catch (Exception ignore) {
         }
         if (!HibernateWrapper.databaseUp()) {
            replicationLog(LogLevel.INFO, "Hibernate Down - Exiting");
            return;
         }
      }

      while (!exitflag) {
         loop();
         nSentThisRun += nSentThisLoop;
         if (exitflag) break; // Skip wait
         LogLevel messageLevel;
         if (firstLoopThisRun || nSentThisLoop > 0) {
            messageLevel = LogLevel.FINE;
         } else {
            messageLevel = LogLevel.FINER;
         }
         if (firstLoopThisRun) firstLoopThisRun = false;
         replicationLog(messageLevel,
                        " waiting for more records (" + nSentThisLoop +
                        " records sent, " + nSentThisRun + " this run)");
         //
         // now wait frequency minutes
         //
         long wait = 0;
         if (replicationEntry != null) {
            wait = replicationEntry.getfrequency();
         }
         if (wait == 0) wait = 1;
         wait = wait * 60 * 1000;
         try {
            Thread.sleep(wait);
         }
         catch (Exception ignore) {
         }
      }

      replicationLog(LogLevel.FINER, "Stopping/Exiting");
      if (nSentThisRun > 0) {
         replicationLog(LogLevel.FINE,
                        nSentThisRun + " records sent during this run.");
      }
      return;
   }

   public void exit() {
      exitflag = true;
      replicationLog(LogLevel.FINE, "Exit Requested");
   }

   private void loop() {
      nSentThisLoop = 0;
      if (exitflag) return;
        
      if (!HibernateWrapper.databaseUp()) { 
         replicationLog(LogLevel.INFO, "Hibernate Down - Exiting");
         exitflag = true;
         return;
      }

      Session session = null;
      List dbidList = null;
      long maxdbid = 0;
      long backlog = 0;
      try {
         session = HibernateWrapper.getSession();
         Transaction tx = session.beginTransaction();
         replicationEntry =
            (Replication) session.get("net.sf.gratia.storage.Replication", replicationId);

         if (replicationEntry != null) {
            currentDestination = replicationEntry.getDestination();
            currentProbename = replicationEntry.getprobename();
         }

         if ((replicationEntry == null) ||
             (replicationEntry.getrunning() == 0)) {
            // Entry has been turned off or removed -- exit.
            replicationLog(LogLevel.FINE,
                           "replication entry " +
                           replicationId +
                           " has been removed or turned off.");
            exitflag = true;
            tx.commit();
            session.close();
            return;
         }

         //
         // create base retrieval
         //
         String table = replicationEntry.getrecordtable();
         String tables;
         if (tableFinder.matcher(table).matches()) {
            tables = table + " M";
         } else {
            tables = table + "_Meta M";
         }
         String where = "M.dbid > " + replicationEntry.getdbid();
         String probename = replicationEntry.getprobename();
         if (probename.startsWith("VO:")) {
            probename = probename.replace("VO:", "");
            tables += ", " + table + " T, VO V, VONameCorrection C";
            where += " AND V.VOName = BINARY " + dq + probename + dq +
               " AND M.dbid = T.dbid AND" + cr +
               "        T.VOName = BINARY C.VOName AND" + cr +
               "        ((BINARY T.ReportableVOName = BINARY C.ReportableVOName)" + cr +
               "         OR ((T.ReportableVOName IS NULL) AND" + cr +
               "             (C.ReportableVOName IS NULL))) AND" + cr +
               "        C.void = V.void";
         } else if (probename.startsWith("Probe:")) {
            probename = probename.replace("Probe:", "");
            where += " AND M.ProbeName like " + dq + probename + dq;
         } else if (probename.startsWith("Grid:")) {
            probename = probename.replace("Grid:", "");
            where += " AND M.Grid";
            if (probename.equals("<null>")) {
               where += " IS NULL";
            } else {
               where += " = " + dq + probename + dq;
            }
         }

         String command = "SELECT M.dbid FROM " + tables + "  WHERE " + where;
         String maxcommand = "select max(M.dbid) from " + tables + "  WHERE " + where;
         String remoteBacklogcommand = "select sum(nRecords)+sum(serviceBacklog) from BacklogStatistics where Name != '"+CollectorService.getName()+"' and EntityType != 'local'";

         SQLQuery sq = session.createSQLQuery(maxcommand);
         Object res = sq.uniqueResult();
         if (res != null) {
            maxdbid = ((BigInteger)res).longValue();
         } else {
            maxdbid = replicationEntry.getdbid();
         }
         
         sq = session.createSQLQuery(remoteBacklogcommand);
         res = sq.uniqueResult();
         if (res != null) {
            backlog = ((BigDecimal)res).longValue();
         }

         replicationLog(LogLevel.FINEST,
                        "Getting record information based on " + command);
         sq = session.createSQLQuery(command);
         sq.setMaxResults(chunksize);
         dbidList = sq.list();
         int lSize = dbidList.size();
         tx.commit();
         session.close();
         if (lSize == 0) {
            // Make sure to tell the remote collector that we are update to date.
            sendBacklogUpdate(replicationEntry.getDestination(),replicationEntry.getbundleSize(),backlog);
            replicationLog(LogLevel.FINEST,
                           "No records found");
            return;
         }
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         replicationLog(LogLevel.WARNING,
                        "Problem encountered obtaining list of records to replicate",e);
	 // removed by HK
         //exitflag = true;
         //return;
      }
      
      //
      // start replication
      //
      String replicationTarget = replicationEntry.getDestination();

      Iterator dIter = dbidList.iterator();
      int bundle_size = replicationEntry.getbundleSize(); // Read table entry
      if (bundle_size == 0) { // Use default
         bundle_size = default_bundle_size; // Default default
         String tmp =  p.getProperty("service.datapump.bundlesize");
         if (tmp != null) {
            try {
               bundle_size = Integer.parseInt(tmp);
            }
            catch (Exception e) {
               replicationLog(LogLevel.WARNING, "Parsing error (" + e +
                              ") when loading property service.datapump.bundlesize with value: " +
                              tmp + " -- using default value " + bundle_size);
            }
         }
      }
      int bundle_count = 0;
      long lowdbid = 0;
      StringBuilder xml_msg = new StringBuilder();
      long dbid = 0;
      try {
         session = HibernateWrapper.getSession();
         while (dIter.hasNext()) { // For each dbid in this batch
            if (exitflag) {
               session.close();
               return; // Abandon unsent bundle
            }
            long prevdbid = dbid;
            dbid = ((BigInteger) dIter.next()).longValue();
            String xml = getXML(dbid, replicationEntry.getrecordtable(), session);
            if (xml.length() == 0) {
               replicationLog(LogLevel.INFO,
                              "Received Null XML: dbid: " + dbid);
               continue;
            }
            if ( (xml.length() + xml_msg.length()) > maxBundleDataSize ) {
               // This xml would lead to the total message length to be
               // to large for the receiving Collector.  So we need to
               // close-out and send the bundle.
               
               uploadBundle(session, lowdbid, prevdbid, maxdbid, backlog, replicationTarget, xml_msg.toString(), bundle_count);
               bundle_count = 0;
               xml_msg = new StringBuilder();
            }
            if (trace) {
               replicationLog(LogLevel.FINEST, "TRACE dbid: " + dbid);
               replicationLog(LogLevel.FINEST, "TRACE xml: " + xml);
            }
            xml_msg.append(xml);
            if (bundle_count == 0) lowdbid = dbid;
            bundle_count = bundle_count + 1;
            if (bundle_count == bundle_size) {
               uploadBundle(session, lowdbid, dbid, maxdbid, backlog, replicationTarget, xml_msg.toString(), bundle_count);
               if (exitflag)  {
                  session.close();
                  return;
               }
               bundle_count = 0;
               xml_msg = new StringBuilder();
            }
         } // End dbid loop
         if (bundle_count != 0) { // Send tag-end records.
            uploadBundle(session, lowdbid, dbid, maxdbid, backlog, replicationTarget, xml_msg.toString(), bundle_count);
         }
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession(session);
         replicationLog(LogLevel.WARNING,
                        "Problem encountered duplicating records",e);
      }
   }

   public String getXML(long dbid, String table, Session session)
      throws Exception {
      StringBuilder buffer = new StringBuilder();

      replicationLog(LogLevel.FINEST, "getXML: dbid: " + dbid);

      Record record = (Record) session.get("net.sf.gratia.storage." + table, dbid);

      // Record has disappeared -- nothing to replicate
      if (record == null) return "";

      buffer.append("replication|");
      buffer.append(record.asXML() + "|");
      buffer.append(record.getRawXml() + "|");
      buffer.append(record.getExtraXml() + "|");
 
      return buffer.toString();
   }
   
   public Boolean uploadBundle(Session session, long lowdbid, long dbid, long maxdbid, long backlog, String replicationTarget, String xml, int bundle_count) 
   throws java.lang.Exception 
   {
      // Upload a bundle
      
      replicationLog(LogLevel.FINE, "Sending: " + lowdbid + " to " + dbid);
      long left = maxdbid - dbid;
      if (left < 0) { left = 0; }
      if (uploadXML(replicationTarget, xml, bundle_count, left, backlog)) {
         // Successful -- update replication table entry
         session.refresh(replicationEntry);
         Transaction tx = session.beginTransaction();
         replicationEntry.setdbid(dbid);
         replicationEntry.setrowcount(replicationEntry.getrowcount() + bundle_count);
         session.flush();
         tx.commit();
         if (firstLoopThisRun && (nSentThisLoop == 0)) { // Message for first send
            replicationLog(LogLevel.FINE, " active");
         }
         nSentThisLoop += bundle_count;                  
         return true;
      }
      return false;
      
   }

   public Boolean sendBacklogUpdate(String replicationTarget, int bundle_count, long backlog) throws Exception 
   {
      Boolean result = false;
      if (!replicationTarget.startsWith("file:")) {

         long nRecords = 0;
         int nQueues = QueueManager.getNumberOfQueues();
         for (int i = 0; i < nQueues; i++) {
            QueueManager.Queue q = QueueManager.getQueue(i);
            nRecords += q.getNRecords();
         }
         
         Post post = new Post(replicationTarget + "/gratia-servlets/rmi", "update", "xxx");
         post.add("xmlfiles", String.valueOf(0));
         post.add("tarfiles", String.valueOf(0)); 
         post.add("maxpendingfiles", p.getProperty("max.q.size"));
         post.add("backlog", String.valueOf(backlog+nRecords));
         post.add("bundlesize", String.valueOf(bundle_count));
         
         String response = post.send();        
        
         if (post.success && response != null) {
            String[] results = split(response, ":");
            if (results[0].equals("OK")) {
               result = true;
            } else {
               replicationLog(LogLevel.WARNING, "Error during post: " + response);
               exitflag = true;
            }
         } else {
            String errorMessage;
            if (post.exception != null) {
               errorMessage = post.exception.toString();
            } else if (response != null) {
               errorMessage = response;
            } else {
               errorMessage = "UNKNOWN";
            }
            replicationLog(LogLevel.INFO,
                           "Error during replication: " +
                           errorMessage +
                           "; will retry later.");
            if (post.exception != null) {
               replicationLog(LogLevel.DEBUG,
                              "Exception details:", post.exception);
            }
            exitflag = true;
         }
      }
      return result;
   }

   public Boolean uploadXML(String replicationTarget, String xml, int bundle_count, long record_left, long backlog)
      throws Exception {

      Boolean result = false;
         
      long nRecords = 0;
      int nQueues = QueueManager.getNumberOfQueues();
      for (int i = 0; i < nQueues; i++) {
         QueueManager.Queue q = QueueManager.getQueue(i);
         nRecords += q.getNRecords();
      }
         
      if (!replicationTarget.startsWith("file:")) {
         Post post = new Post(replicationTarget + "/gratia-servlets/rmi", "update", xml);

         post.add("xmlfiles", String.valueOf(record_left));
         post.add("tarfiles", String.valueOf(0)); 
         post.add("maxpendingfiles", p.getProperty("max.q.size"));
         post.add("backlog", String.valueOf(backlog+nRecords));
         post.add("bundlesize", String.valueOf(bundle_count));

         String response = post.send();        
         if (post.success && response != null) {
            String[] results = split(response, ":");
            if (results[0].equals("OK")) {
               result = true;
            } else {
               replicationLog(LogLevel.WARNING, "Error during post: " + response);
               exitflag = true;
            }
         } else {
            String errorMessage;
            if (post.exception != null) {
               errorMessage = post.exception.toString();
            } else if (response != null) {
               errorMessage = response;
            } else {
               errorMessage = "UNKNOWN";
            }
            replicationLog(LogLevel.INFO,
                           "Error during replication: " +
                           errorMessage +
                           "; will retry later.");
            if (post.exception != null) {
               replicationLog(LogLevel.DEBUG,
                              "Exception details:", post.exception);
            }
            exitflag = true;
         }
      } else {
         // Request to save locally instead of sending
         String dir = replicationTarget.substring(5); // Skip the prefix.
         // Create the subdir if needed.

         Date now = new Date();
         SimpleDateFormat format = new SimpleDateFormat("yyyyMMddkk");
         String path = replicatePath + "/" + dir + "/replicate" + "-" + format.format(now);
         File directory = new File(path);
         if (!directory.exists()) {
            directory.mkdirs();
         }
         if (nSentThisLoop == 0) { // Message for first send
            replicationLog(LogLevel.FINE, " writing to disk in " + replicatePath + "/" + dir);
         }           
         long part = 0; //nrecords / recordsPerDirectory;
           
         String directory_part = "rep" + replicationId;
         File subdir = new File(directory,directory_part + "-" + part);
           
         if (!subdir.exists()) {
            subdir.mkdir();
         }
           
         // Save the xml
         File repfile = File.createTempFile("bundle-", ".xml", subdir);
         String filename = repfile.getPath();
         XP.save(filename,xml);
         result = true;
      }
      return result;
   }


   public String[] split(String input, String sep) {
      Vector vector = new Vector();
      StringTokenizer st = new StringTokenizer(input, sep);
      while (st.hasMoreTokens())
         vector.add(st.nextToken());
      String[] results = new String[vector.size()];
      for (int i = 0; i < vector.size(); i++)
         results[i] = (String)vector.elementAt(i);
      return results;
   }

   private String logPreamble() {
      String preamble = "ReplicationDataPump #" + replicationId;
      if (currentDestination != null) {
         preamble += " (" + currentProbename + " to " + currentDestination + ")";
      }
      preamble += ": ";
      return preamble;
   }
            

   public void replicationLog(Level level, String log) {
      Logging.log(level, logPreamble() + log);
   }

   public void replicationLog(Level level, String log, Exception ex) {
      Logging.log(level, logPreamble() + log, ex);
   }

   public void replicationLog(String log) {
      Logging.log(logPreamble() + log);
   }

   public void replicationLog(String log, Exception ex) {
      Logging.log(logPreamble() + log, ex);
   }


}

package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.util.ArrayList;

import java.util.Date;
import java.util.Properties;
import java.util.Hashtable;
import java.util.regex.Pattern;

import java.text.*;

import java.io.*;

import net.sf.gratia.storage.*;

import org.apache.commons.lang.exception.ExceptionUtils;

import org.hibernate.*;
import org.hibernate.exception.ConstraintViolationException;

public class RecordProcessor extends Thread {

   final static long    fgFileOldTime = 1000 * 60 * 60 * 24; // Empty files should be deleted after 1 day.
   final static String  fgReplicationMarker = "replication";
   final static String  fgOriginMarker = "Origin";
   final static Pattern fgDuplicateExceptionFinder = Pattern.compile("\\b[Dd]uplicate\\b");
   final static Pattern fgMetaFinder = Pattern.compile("_Meta ");
   final static Pattern fgOriginFinder = Pattern.compile("Origin ");
   
   Properties   fProperties;

   final Object fLock;
   String       fIdentity = null;

   // Location of the data files.
   String             fHistoryPath = "";
   File               fQuarantineDir = null;
   QueueManager.Queue fQueue = null;    // Location of the incoming messages.
   long               fRecordsPerDirectory = 10000;   // Maximum number of records per directory.

   // Main service object.
   CollectorService fCollectorService;

   // Helper and configuration objects.
   RecordUpdaterManager fUpdater = new RecordUpdaterManager();
   RecordConverter      fConverter = new RecordConverter();
   StatusUpdater        fStatusUpdater = null;
   NewVOUpdate          fNewVOUpdate = null;
   NewProjectNameUpdate fNewProjectNameUpdate = null;
   NewClusterUpdate     fNewClusterUpdate = null;
   ErrorRecorder        fErrorRecorder = new ErrorRecorder();

   // Message passing and caching collections.
   Hashtable              fGlobal;
   Hashtable<String,Long> fProbeDetails = new Hashtable<String,Long>();

   //
   // various things used in the update loop
   //
   boolean stopflag = false;
   long fNFiles = 0;                    // Number of files processed;
   long fNRecords = 0;                  // Number of records processed;

   private class DuplicateOriginHandler {

      public static final int HANDLED = 1;
      public static final int NOT_RELEVANT = 0;
      public static final int TOO_MANY_DUPS = 5;
      private int fTries = 0;

      DuplicateOriginHandler() {
      }

      public void reset() {
         fTries = 0;
      }

      public int maybeHandleDuplicateOrigin(ConstraintViolationException e, String ident) {
	Logging.fine(fIdentity + ":getSQL " + e.getSQL());
         if (fgDuplicateExceptionFinder.matcher(e.getSQLException().getMessage()).find()) { 
         //if (fgDuplicateExceptionFinder.matcher(e.getSQLException().getMessage()).find() &&
         //    fgOriginFinder.matcher(e.getSQL()).find()) {
            if (++fTries < TOO_MANY_DUPS) {
               Logging.fine(fIdentity + ": detected " + fTries +
                            ((fTries > 1)?" consecutive ":"") +
                            " duplicate origin " + ((fTries > 1)?"entries":"entry") + " -- retry.");
               Logging.debug(fIdentity + ": exception details: ", e);
               return HANDLED;
            } else {
               Logging.warning(fIdentity + ": detected too many consecutive duplicate origin failures (" +
                               fTries + " while processing origin entry.)");
               Logging.debug(fIdentity + ": exception details: ", e);
               return TOO_MANY_DUPS;
            }
         } else {
            return NOT_RELEVANT;
         }
      }      
   }



   public RecordProcessor(String ident,
                          QueueManager.Queue queue,
                          Object lock,
                          Hashtable global,
                          CollectorService collectorService) {
      this.fIdentity = ident;
      this.fQueue = queue;
      this.fLock = lock;
      this.fGlobal = global;
      this.fCollectorService = collectorService;

      loadProperties();
      try {
         String url = fProperties.getProperty("service.jms.url");
         Logging.info(fIdentity + ": " + fQueue.toString() + ": Started");
      } catch (Exception e) {
         Logging.warning(fIdentity + ": ERROR! Serious problems starting recordProcessor");
         Logging.debug(fIdentity + "Exception detail: ", e);
      }
      fHistoryPath = "/var/lib/gratia-service/data/";

      fQuarantineDir = new File(fHistoryPath + 
                               "quarantine");
      fQuarantineDir.mkdirs();

      JobUsageRecordUpdater.AddDefaults(fUpdater);
   }

   public void loadProperties() {
      fProperties= Configuration.getProperties();

      try {
         long max_record =
            Long.parseLong(fProperties.getProperty("maintain.recordsPerDirectory"));
         fRecordsPerDirectory = max_record;
      } catch (Exception e) {
         // Only issue a warning here
         Logging.warning(fIdentity +
                         ": Failed to parse property " +
                         "maintain.recordsPerDirectory");
      }
   }

   public void stopRequest() {
      stopflag = true;
      Logging.fine(fIdentity + ": Stop Requested");
   }

   @Override
      public void run() {
      while (true) {
         if (stopflag) {
            Logging.info(fIdentity + ": Exiting");
            return;
         }

         if (!HibernateWrapper.databaseUp()) {
            Logging.log(fIdentity + ": Hibernate Down: Sleeping");
            try {
               Thread.sleep(30 * 1000);
            } catch (Exception ignore) {
            }
            continue;
         }
         if (stopflag) {
            Logging.info(fIdentity + ": Exiting");
            return;
         }
         int nfiles = loop();
         if (stopflag) {
            Logging.info(fIdentity + ": Exiting");
            return;
         }
         if (nfiles == 0) {
            // Sleep only if there is no file waiting.
            try {
               Thread.sleep(30 * 1000);
            } catch (Exception ignore) {
            }
         }
      }
   }

   public int loop() {
      QueueManager.File file = new QueueManager.File("");
      
      String blob = "";

      String xml = "";
      ArrayList rawxmllist = new ArrayList();
      ArrayList extraxmllist = new ArrayList();
      ArrayList md5list = new ArrayList();
      ArrayList historydatelist = new ArrayList();

      boolean gotreplication = false;
      boolean gothistory = false;

      // Return the number of files seen.
      // or 0 in the case of error.

      String files[] = fQueue.getFileList();

      int nfiles = 0;

      if (files.length == 0) {
         return 0;
      }

      fStatusUpdater = new StatusUpdater();
      fNewVOUpdate = new NewVOUpdate();
      fNewProjectNameUpdate = new NewProjectNameUpdate();
      fNewClusterUpdate = new NewClusterUpdate();

      NEXTFILE:
      for (int i = 0; i < files.length; ++i) { // Loop over files
         fGlobal.put("recordProcessor", new java.util.Date());
            
         if (stopflag) { // Stop requested
            break;
         }
         ++nfiles;
         file.reset( files[i] );
         
         //MPERF: Logging.fine(fIdentity + ": Start Processing: " + file);
         try {
            blob = file.getData();
         } catch (FileNotFoundException e) {
            Utils.GratiaError("RecordProcessor",
                              "XML file read",
                              fIdentity + ": Unable to find file " + file.getPath() + "; FS trouble or two collectors running?");
            continue; // Next file
         } catch (IOException e) {
            Utils.GratiaError("RecordProcessor",
                              "XML file read",
                              fIdentity + ": Error " + e.getMessage() + " while trying to read " + file.getPath());
            saveQuarantineFile(file, "Error reading file: ", e);
            continue; // Next file
         }
         if (blob.length() == 0) { // Empty file -- how old is it.
            if (file.getAge() > fgFileOldTime) {
               Logging.info(fIdentity + ": removing old empty file " + file.getPath());
               fQueue.deleteFile(file);               
            } else { // Skip file
               Logging.log(fIdentity + ": deferring read of recent empty file " + file.getPath());
               continue;
            }
         }
         xml = "";
         rawxmllist.clear();
         extraxmllist.clear();
         md5list.clear();
         historydatelist.clear();

         gotreplication = gothistory = false;
   
         try {
            saveIncoming(blob);
         } catch (IOException e) {
            Logging.warning(fIdentity +
                            ": ERROR! Loop failed to backup incoming " +
                            "message. \nError: " +
                            e.getMessage() + "\n");
         }

         Record current = null;
         Origin origin = null;

         //
         // see if trace requested
         //
         if (fProperties.getProperty("service.datapump.trace").equals("1")) {
            Logging.debug(fIdentity + ": XML Trace:" + "\n\n" + blob + "\n\n");
         }

         // See if we have an origin preceding the data.
         ReplicationTokenizer st = null;
         String nextpart = blob;

         if (blob.startsWith(fgOriginMarker)) {
            st = new ReplicationTokenizer(blob, "|");
            if (st.hasMoreTokens()) {
               // skip marker
               st.nextToken();
               if (st.hasMoreTokens()) {
                  String originStr = st.nextToken();
                  try {
                     origin = fConverter.convertOrigin(originStr);
                     if (origin != null && origin.getConnection() != null) {
                        file.setFrom( origin.getConnection().getSender() );
                     }
                  } catch (Exception e) {
                     Logging.warning(fIdentity + ": Origin parse error:  ", e);
                     Logging.warning(fIdentity + ": XML:  " + "\n" + originStr);
                  }
               }
            }
            if (st.hasMoreTokens()) {
               nextpart = st.nextToken();
            } else {
               nextpart = "";
            }
         }
         //
         // see if we got a normal update or a replicated one
         //
         try {
            int nseen = 0;
            if (nextpart.startsWith(fgReplicationMarker)) {
               if (st == null) {
                  // We could assert that nextpart == blob
                  st = new ReplicationTokenizer(blob, "|");
                  if (st.hasMoreTokens()) {
                     // Skip marker
                     st.nextToken();
                  }
               }

               gotreplication = true;

               while (st.hasMoreTokens()) {
                  String val = st.nextToken();
                  if (val.equals(fgReplicationMarker) && st.hasMoreTokens()) {
                     val = st.nextToken();
                  }
                  xml = xml.concat(val);
                  if (st.hasMoreTokens()) {
                     rawxmllist.add(st.nextToken());
                  } else {
                     rawxmllist.add(null);
                  }
                  if (st.hasMoreTokens()) {
                     // The extraxml can be directly followed by the word 'replication'
                     String extraxml = st.nextToken();
                     if (extraxml.endsWith(fgReplicationMarker)) {
                        extraxml =
                           extraxml.substring(0, extraxml.length() -
                                              fgReplicationMarker.length());
                     }
                     extraxmllist.add(extraxml);
                  } else {
                     extraxmllist.add(null);
                  }
                  nseen = nseen + 1;
               }
            } else if (nextpart.startsWith("history")) {
               gothistory = true;
               if (st == null) {
                  // We could assert that nextpart == blob
                  st = new ReplicationTokenizer(blob, "|");
                  if (st.hasMoreTokens()) {
                     // Skip marker
                     st.nextToken();
                  }
               }
               while (st.hasMoreTokens()) {
                  if (st.hasMoreTokens()) {
                     historydatelist.add(st.nextToken());
                  } else {
                     historydatelist.add(null);
                  }
                  if (st.hasMoreTokens()) {
                     xml = xml.concat(st.nextToken());
                  }
                  if (st.hasMoreTokens()) {
                     rawxmllist.add(st.nextToken());
                  } else {
                     rawxmllist.add(null);
                  }
                  if (st.hasMoreTokens()) {
                     extraxmllist.add(st.nextToken());
                  } else {
                     extraxmllist.add(null);
                  }
                  nseen = nseen + 1;
                  if (st.hasMoreTokens()) {
                     // Skip next marker
                     st.nextToken();
                  }
               }
            } else if (nextpart.startsWith("historymd5")) {
               gothistory = true;
               if (st == null) {
                  // We could assert that nextpart == blob
                  st = new ReplicationTokenizer(blob, "|");
                  if (st.hasMoreTokens()) {
                     // Skip marker
                     st.nextToken();
                  }
               }
               while (st.hasMoreTokens()) {
                  if (st.hasMoreTokens()) {
                     historydatelist.add(st.nextToken());
                  } else {
                     historydatelist.add(null);
                  }
                  if (st.hasMoreTokens()) {
                     xml = xml.concat(st.nextToken());
                  }
                  if (st.hasMoreTokens()) {
                     md5list.add(st.nextToken());
                  } else {
                     md5list.add(null);
                  }
                  nseen = nseen + 1;
                  if (st.hasMoreTokens()) {
                     // Skip next marker
                     st.nextToken();
                  }
               }
            } else {
               xml = nextpart;
               nseen = nseen + 1;
            }
            if (nseen > 1) {
               xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                  "<RecordEnvelope>" + xml + "</RecordEnvelope>";
            }
         } catch (Exception e) {
            Logging.warning(fIdentity + ": Error:Processing File: " + file);
            Logging.warning(fIdentity + ": Blob: " + blob);
            saveQuarantineFile(file, "Error parsing file: ", e);
            continue; // Next file.
         }

         if (xml == null) {
            Logging.warning(fIdentity + ": No data to process: " + file);
            saveQuarantineFile(file, "Unable to fIdentityify XML in file");
            continue; // Next file.
         }

         //MPERF: Logging.fine(fIdentity + ": Processing: " + file);
         Logging.log(fIdentity + ": Processing: " + file.getPath());

         ArrayList records = new ArrayList();

         try {
            if (gotreplication) {
               // First attempt to fix an error in data
               // replicated from old collectors.
               xml = xml.replaceAll("<LocalJobId ><undefined></LocalJobId>",
                                    "<LocalJobId >&lt;undefined&gt;" +
                                    "</LocalJobId>");
            }
            records = convert(xml);
            if (records.size() > 1) {
               Logging.log(fIdentity + ": Received envelope of " +
                           records.size() + " records");
            }
         } catch (Exception e) {
            try {
               if (gotreplication) {
                  Logging.fine(fIdentity + ": Received bad replication XML");
                  fErrorRecorder.saveParse("Replication", "Parse", xml);
               } else if (gothistory) {
                  Logging.fine(fIdentity + ": Received bad history XML");
                  fErrorRecorder.saveParse("History", "Parse", xml);
               } else {
                  Logging.fine(fIdentity + ": Received bad probe XML");
                  fErrorRecorder.saveParse("Probe", "Parse", xml);
               }
            } catch (Exception ignore) {
               // Even if there's a connection problem, ignore it, save
               // the XML in the quarantine area and move on.
            }
            saveQuarantineFile(file, "Problem parsing XML in file: ", e);
            continue; // Next file.
         }
         int rSize = records.size();
         file.setNRecords(rSize);

         //MPERF: Logging.fine(fIdentity+ ": converted " + rSize + " records");
         DuplicateOriginHandler dupOriginHandler = new DuplicateOriginHandler();
         if (origin != null) {
            // Save the origin first in its own transaction, so to 
            //   - make sure it is always stored properly even if we have a bundle
            //       and the first record is a duplicate.
            //   - reduce the risk that when we lookup the Origin, the previous
            //       storing is not yet completely flush in the db.
            String rId = ": ";
            if (gotreplication) {
               rId += "Replication ";
            } else if (gothistory) {
               rId += "History ";
            }
            Session or_session = null;
            int nTries = 0;
            boolean keepTrying = true;
            Transaction or_tx = null;
            while (keepTrying) {
               ++nTries;
               try {
                  fCollectorService.readLockCaches();
                  or_session = HibernateWrapper.getSession();
                  or_tx = or_session.beginTransaction();
                  origin = origin.attach(or_session);
                  or_session.flush();
                  or_tx.commit();
                  keepTrying = false;
                  or_session.close();
                  fCollectorService.readUnLockCaches();
               } catch (ConstraintViolationException e) {
                  fCollectorService.readUnLockCaches();
                  HibernateWrapper.closeSession( or_session );
                  switch (dupOriginHandler.maybeHandleDuplicateOrigin(e, fIdentity + rId)) {
                  case DuplicateOriginHandler.HANDLED: break;
                  case DuplicateOriginHandler.TOO_MANY_DUPS:
                     saveQuarantineFile(file, "Too many consecutive duplicate origin failures in origin initial storing (" +
                                    DuplicateOriginHandler.TOO_MANY_DUPS + "): ",
                                    e);
                     continue NEXTFILE; // Next file.                              
                  case DuplicateOriginHandler.NOT_RELEVANT:
                  default:
                     Logging.warning(fIdentity + rId +
                                     ": received unexpected constraint violation exception " +
                                     e.getMessage() + " while processing origin entry.");
                     Logging.debug(fIdentity + rId + ": exception details:", e);
                     saveQuarantineFile(file, "Problem processing origin entry for record file: ", e);
                     continue NEXTFILE; // Next file. 
                  }
               } catch (Exception e) {
                  fCollectorService.readUnLockCaches();
                  HibernateWrapper.closeSession( or_session );
                  if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, fIdentity)) {
                     Logging.warning(fIdentity + rId +
                                     ": received unexpected exception " +
                                     e.getMessage() + " while processing origin entry.");
                     Logging.debug(fIdentity + rId + ": exception details:", e);
                     saveQuarantineFile(file, "Problem processing origin entry for record file: ", e);
                     continue NEXTFILE; // Next file. 
                  }
               }
            }
            //MPERF: Logging.fine(fIdentity + rId + " saved Origin object.");
         }
         NEXTRECORD: for (int j = 0; j < rSize; ++j) { // Loop over records in file
            if (stopflag) { // Stop requested. Quit processing completely (don't delete this input file)
               break NEXTFILE;
            }
            current = (Record) records.get(j);

            // For information logging.
            String rId = ": ";
            if (gotreplication) {
               rId += "Replication ";
            } else if (gothistory) {
               rId += "History ";
            }
            String simpleName = current.getClass().getSimpleName();
            rId += simpleName;
            if (rSize > 1) {
               rId += " " + (j + 1) + " / " + rSize;
            }
            rId += " (" + current.getProbeName();
            if (simpleName.equals("ProbeDetails")) {
               rId += ", recordId=" +
                  ((ProbeDetails) current).getRecordIdentity();
            }
            rId += ")";

            Probe probe;
                
            //MPERF: Logging.fine(fIdentity + rId + " starting hibernate operations.");
            int nTries = 0;
            boolean keepTrying = true;
            Session pr_session = null;
            Transaction pr_tx = null;
            while (keepTrying) {
               ++nTries;
               try {
                  pr_session = HibernateWrapper.getSession();
                  pr_tx = pr_session.beginTransaction();

                  probe = fStatusUpdater.update(pr_session, current, xml);
                  pr_session.flush();
                  pr_tx.commit();
                  // Set the probe on the current object.
                  current.setProbe(probe);
                  keepTrying = false;
                  pr_session.close();
               } catch (Exception e) {
                  HibernateWrapper.closeSession( pr_session );
                  if (pr_session.isOpen()) {
                     Logging.warning(fIdentity + ": Communications error: " + "shutting down");
                     return 0; // Session could not close; DB problem
                  }
                  if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, fIdentity)) {
                     keepTrying = false;
                     if (handleUnexpectedException(rId, e, gotreplication, current)) {
                        continue NEXTRECORD; // Process next record.
                     } else {
                        Logging.warning(fIdentity + ": Communications error: " + "shutting down");
                        return 0; // DB access problem.
                     }
                  }
               }
            }
            //MPERF: Logging.fine(fIdentity + rId + " saved probe object.");


            // Fix up the record; in particular set EndTime if it is out of whack.
            try {
               fUpdater.Update(current);
            } catch (RecordUpdater.UpdateException e) {
               // One of the updater found the record to be improper for storage
               // For example a Job Usage Record might be missing EndTime and 
               // at least StartTime and WallDuration (i.e. we have no clue about
               // the EndTime).
               Logging.warning(fIdentity + rId + ": Error in record updating: " + e.getMessage());
               Logging.debug(fIdentity + rId + ": exception details: ", e);
               if ((e.getCause() instanceof ConnectionException) ||
                   (e.getCause() instanceof com.mysql.jdbc.CommunicationsException) ||
                   (!HibernateWrapper.databaseUp())) {
                  Logging.warning(fIdentity + ": Communications error: " + "shutting down");
                  return 0; // DB access trouble.
               } else {
                  try {
                     if (gotreplication) {
                        fErrorRecorder.saveSQL("Replication", "RecordUpdate", current);
                     } else {
                        fErrorRecorder.saveSQL("Probe", "RecordUpdate", current);
                     }
                  } catch (Exception e2) {
                     saveQuarantineFile(file, "Unexpected error while recording RecordUpdate error", e2);
                  }
                  continue NEXTRECORD; // Process next record.
               }
            } catch (Exception e) {
               // Humm an unexception exception.
               Logging.warning(fIdentity + rId + ": Error in record updating: " + e.getMessage());
               Logging.debug(fIdentity + rId + ": exception details: ", e);
               if ((e instanceof ConnectionException) ||
                   (e instanceof com.mysql.jdbc.CommunicationsException) ||
                   (e.getCause() instanceof com.mysql.jdbc.CommunicationsException) ||
                   (!HibernateWrapper.databaseUp())) {
                  Logging.warning(fIdentity + ": Communications error: " + "shutting down");
                  return 0; // DB access trouble.
               } else {
                  try {
                     if (gotreplication) {
                        fErrorRecorder.saveSQL("Replication", "RecordUpdateInternalError", current);
                     } else {
                        fErrorRecorder.saveSQL("Probe", "RecordUpdateInternalError", current);
                     }
                  } catch (Exception e2) {
                     saveQuarantineFile(file, "Unexpected error while recording RecordUpdate error", e2);
                  }
                  continue NEXTRECORD; // Process next record.
               }            
            }
            
            boolean acceptRecord = true;
            if (!fCollectorService.housekeepingServiceDisabled()) {
               Date date;
               try {
                  date = current.getDate();
               } catch (NullPointerException e) {
                  date = current.getServerDate();
               }
               ExpirationDateCalculator.Range expirationRange = current.getExpirationRange();
               if (date.before(expirationRange.fExpirationDate)) {
                  acceptRecord = false;
                  try {
                     if (gotreplication) {
                        Logging.fine(fIdentity + rId +
                                     ": Rejected record because " +
                                     "its data are too old (" +
                                     current.getDate() + " < " +
                                     expirationRange.fExpirationDate + ")");
                        fErrorRecorder.saveDuplicate("Replication",
                                                    "ExpirationDate",
                                                    0, current);
                     } else if (gothistory) {
                        Logging.fine(fIdentity + rId +
                                     ": Ignored history record " +
                                     "because data are is too " +
                                     "old (" + current.getDate() +
                                     " < " + expirationRange.fExpirationDate + ")");
                     } else {
                        Logging.fine(fIdentity + rId +
                                     ": Rejected record because " +
                                     "its data are too old (" +
                                     current.getDate() + " < " +
                                     expirationRange.fExpirationDate + ")");
                        fErrorRecorder.saveDuplicate("Probe",
                                                    "ExpirationDate",
                                                    0, current);
                     }
                  } catch (Exception e) {
                     if (handleUnexpectedException(rId, e, gotreplication, current)) {
                        saveQuarantineFile(file, "Unexpected error while recording expired record", e);
                        continue NEXTRECORD; // Process next record.
                     } else {
                        Logging.warning(fIdentity + ": Communications error: " + "shutting down");
                        return 0; // DB access problem.
                     }

                  }
               } else if(date.after(expirationRange.fCutoffDate)) {
                  acceptRecord = false;
                  try {
                     if (gotreplication) {
                        Logging.fine(fIdentity + rId +
                                     ": Rejected record because " +
                                     "its data are too far in the future (" +
                                     current.getDate() + " < " +
                                     expirationRange.fCutoffDate + ")");
                        fErrorRecorder.saveDuplicate("Replication",
                                                    "CutoffDate",
                                                    0, current);
                     } else if (gothistory) {
                        Logging.fine(fIdentity + rId +
                                     ": Ignored history record " +
                                     "because its data are too " +
                                     "far in the future (" + current.getDate() +
                                     " < " + expirationRange.fCutoffDate + ")");
                     } else {
                        Logging.fine(fIdentity + rId +
                                     ": Rejected record because " +
                                     "its data are too far in the future (" +
                                     current.getDate() + " > " +
                                     expirationRange.fCutoffDate + ")");
                        fErrorRecorder.saveDuplicate("Probe",
                                                    "CutoffDate",
                                                    0, current);
                     }
                  } catch (Exception e) {
                     if (handleUnexpectedException(rId, e, gotreplication, current)) {
                        saveQuarantineFile(file, "Unexpected error while recording expired record", e);
                        continue NEXTRECORD; // Process next record.
                     } else {
                        Logging.warning(fIdentity + ": Communications error: " + "shutting down");
                        return 0; // DB access problem.
                     }
                     
                  }
               }
            }

            if ((!gothistory) || (!(md5list.size() > j)) ||
                md5list.get(j) == null) {
               try {
                  if (current instanceof JobUsageRecord) {
                     String md5key = current.computemd5(DatabaseMaintenance.UseJobUsageSiteName());
                     current.setmd5(md5key);
                  } else {
                     String md5key = current.computemd5(false);
                     current.setmd5(md5key);
                  }
               } catch (Exception e) {
                  if (handleUnexpectedException(rId, e, gotreplication, current)) {
                     saveQuarantineFile(file, "Unexpected error while calculating checksum for record", e);
                     continue NEXTRECORD;
                  } else {
                     Logging.warning(fIdentity + ": Communications error: " + "shutting down");
                     return 0; // DB access problem.
                  }
               }
            } else {
               current.setmd5((String) md5list.get(j));
            }
            if (current.getTableName().equals("ProbeDetails")) {
               Long pd_dbid = fProbeDetails.get(current.getmd5());
               if (pd_dbid != null) {
                  // This is a duplicate.
                  Logging.fine(fIdentity + rId +
                               ": " + "(fast) Ignore duplicate of record " +
                               pd_dbid);
                  acceptRecord = false;
               }
            }
            
            if (acceptRecord) {
               current.setDuplicate(false);
               dupOriginHandler.reset(); // Start counting duplicate origins from 0.
               if (origin != null) {
                  //MPERF: Logging.fine(fIdentity + rId + " about to add origin objects.");
                  current.addOrigin(origin);
               }

               Session rec_session = null;
               Transaction rec_tx = null;
               String incomingxml;
               String rawxml;
               String extraxml;
               nTries = 0;
               keepTrying = true;
               while (keepTrying) {
                  ++nTries;
                  try {
                     fCollectorService.readLockCaches();
                     rec_session = HibernateWrapper.getSession();
                     rec_tx = rec_session.beginTransaction();
                     //MPERF: Logging.fine(fIdentity + rId + " attaching VO and other content.");
                     synchronized (fLock) {
                        // Synchronize on fLock so we're guaranteed only
                        // one run per collector, not one per thread if
                        // we were synchronizing on the objects
                        // themselves.
                        fNewVOUpdate.check(current, rec_session);
                        fNewProjectNameUpdate.check(current, rec_session);
                        fNewClusterUpdate.check(current, rec_session);
                        current.attachContent(rec_session);
                        // Reduce contention on the attached objects (in particular Connection)
                        // to avoid DB deadlock.
                        Logging.debug(fIdentity + rId + ": Before session flush");
                        rec_session.flush();
                     }
                     //MPERF: Logging.fine(fIdentity + rId + " managing RawXML.");
                     incomingxml = current.getRawXml();
                     rawxml = null;
                     extraxml = null;
                     if (rawxmllist.size() > j) {
                        rawxml = (String) rawxmllist.get(j);
                        if (rawxml != null) {
                           if (rawxml.equals("null")) {
                              // The info was scrubbed from the sender's database.
                              current.setRawXml(null);
                           } else {
                              current.setRawXml(rawxml);
                           }
                        }
                     }
                     if (extraxmllist.size()>j) {
                        extraxml = (String)extraxmllist.get(j);
                        if (extraxml != null && !extraxml.equals("null")) {
                           String oldExtraXml = current.getExtraXml();
                           if (oldExtraXml != null) {
                              extraxml = oldExtraXml + extraxml;
                           }
                           current.setExtraXml(extraxml);
                        }
                     }
                     Logging.debug(fIdentity + rId + ": Before Hibernate Save");
                     if (gothistory) {
                        Date serverDate = new Date(Long.parseLong((String) historydatelist.get(j)));
                        current.setServerDate(serverDate);
                     }
                     //MPERF: Logging.fine(fIdentity + rId + " saving object.");
                     rec_session.save(current);
                     //MPERF: Logging.fine(fIdentity + rId + " executing trigger.");
                     current.executeTrigger(rec_session);
                     //MPERF: Logging.fine(fIdentity + rId + " executing flush.");
                     rec_session.flush();
                     //MPERF: Logging.fine(fIdentity + rId + " executing comming.");
                     rec_tx.commit();
                     keepTrying = false;
                     rec_session.close();
                     //MPERF: Logging.fine(fIdentity + ": After Transaction Commit");
                     // Save history
                     if (!gothistory) {
                        saveHistory(current, incomingxml,
                                    rawxml, extraxml, gotreplication);
                     }
                     fNRecords = fNRecords + 1;
                     Logging.fine(fIdentity + rId + " saved.");
                     
                     fCollectorService.readUnLockCaches();
                  } catch (ConstraintViolationException e) {
                     fCollectorService.readUnLockCaches();
                     HibernateWrapper.closeSession(rec_session);
                     if (rec_session.isOpen()) {
                        Logging.warning(fIdentity + ": Communications error: " + "shutting down");
                        return 0; // Session could not close; DB problem
                     }
                     try {
                        if (maybeHandleDuplicateRecord(e, current, rId, gotreplication, gothistory)) {
                           keepTrying = false; // Don't retry
                        } else {
                           String message = "Received unexpected constraint violation";
                           switch (dupOriginHandler.maybeHandleDuplicateOrigin(e, fIdentity + rId)) {
                           case DuplicateOriginHandler.HANDLED: break; // Retry.
                           case DuplicateOriginHandler.TOO_MANY_DUPS: // Too many retries
                              message = "Received too many consecutive duplicate origin failures (main loop) (" +
                                 DuplicateOriginHandler.TOO_MANY_DUPS + ")";
                           case DuplicateOriginHandler.NOT_RELEVANT:
                           default:
                              if (handleUnexpectedException(rId, e, gotreplication, current,
                                                            message)) {
                                 continue NEXTRECORD; // Process next record.
                              } else {
                                 Logging.warning(fIdentity + ": Communications error: " + "shutting down");
                                 return 0; // DB access trouble.
                              }                                   
                           } // End switch.
                        } // End maybeHandleDuplicateReocrd()
                     } catch (Exception e2) {
                        String msg = new String("Received unexpected exception while trying to resolved constraint violation: ");
                        msg += e.getMessage();
                        if (handleUnexpectedException(rId, e2, gotreplication, current,msg)) {
                           continue NEXTRECORD; // Process next record.
                        } else {
                           Logging.warning(fIdentity + ": Communications error: " + "shutting down");
                           return 0; // DB access trouble.
                        }
                     }
                  } catch (Exception e) {
                     fCollectorService.readUnLockCaches();
                     HibernateWrapper.closeSession(rec_session);
                     if (rec_session.isOpen()) {
                        Logging.warning(fIdentity + ": Communications error: " + "shutting down");
                        return 0; // Session could not close; DB problem
                     }
                     if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, fIdentity)) {
                        keepTrying = false;
                        if (handleUnexpectedException(rId, e, gotreplication, current)) {
                           continue NEXTRECORD; // Process next record.
                        } else {
                           Logging.warning(fIdentity + ": Communications error: " + "shutting down");
                           return 0; // DB access trouble  .                                  
                        }
                     }
                  } // End general catch
               } // End while (keepTrying)
            } // End of handling accepted records.
         } // End of for each record loop
         // Logging.log(fIdentity + ": Before File Delete: " + file.getPath());
         fQueue.deleteFile(file);               
         // Logging.log(fIdentity + ": After File Delete: " + file.getPath());
         ++fNFiles;
      } // End loop over files
      Logging.fine(fIdentity + ": Input messages this run: " + fNFiles);
      Logging.fine(fIdentity + ": Records this run: " + fNRecords);
      return nfiles;
   }

   public File getDirectory(String what) {
      Date now = new Date();
      SimpleDateFormat format = new SimpleDateFormat("yyyyMMddkk");
      String path = fHistoryPath + what + "-" + format.format(now);
      File dirondisk = new File(path);
      if (!dirondisk.exists()) {
         dirondisk.mkdir();
      }

      long part = fNRecords / fRecordsPerDirectory;

      File subdir = new File(dirondisk, fQueue.getShortName() + "-" + part);

      if (!subdir.exists()) {
         subdir.mkdir();
      }
      return subdir;
   }

   public void saveIncoming(String data) throws java.io.IOException {
      File where = getDirectory("old");
      File errorfile = File.createTempFile("old-", ".xml", where);
      String filename = errorfile.getPath();
      XP.save(filename, data);
   }

   public void saveQuarantineRecord(Record current, String annot, Exception e) {
      saveQuarantineRecord(current, annot + " " + e.getMessage() + "\n" +
                           ExceptionUtils.getFullStackTrace(e));
   }

   public void saveQuarantineFile(QueueManager.File oldfile, String annot, Exception e) {
      saveQuarantineFile(oldfile,
                     annot + " " + e.getMessage() + "\n" +
                     ExceptionUtils.getFullStackTrace(e));
   }

   public void saveQuarantineRecord(Record current, String annot) {
      String xml = "";
      try {
         xml = current.getRawXml();
         if (xml == null || (xml.length() == 0) || xml.equals("null")) {
            xml = current.asXML();
         }
         if ((annot != null) && (! annot.endsWith("\n"))) {
            annot.concat("\n"); // End with a line feed.
         }
         File newxmlfile = File.createTempFile("quarantine-", ".xml", fQuarantineDir);
         XP.save(newxmlfile.getPath(), xml); // Save XML.
         XP.save(newxmlfile.getPath().replace(".xml", ".txt"), annot); // Save annotation
         Logging.warning(fIdentity + ": record XML quarantined as " +
                         newxmlfile.getPath() + " (" + annot + ")");
      } catch (Exception e) {
         Logging.warning(fIdentity + ": record could not be quarantined! XML follows as last-ditch preservation:");
         Logging.warning(fIdentity + xml);
         Logging.debug(fIdentity + ": exception details: ", e);
      }
   }

   public void saveQuarantineFile(QueueManager.File oldfile, String annot) {
      try {
         if ((annot != null) && (! annot.endsWith("\n"))) {
            annot.concat("\n"); // End with a line feed.
         }
         File newxmlfile = File.createTempFile("quarantine-", ".xml", fQuarantineDir);
         fQueue.renameTo(oldfile,newxmlfile);
         XP.save(newxmlfile.getPath().replace(".xml", ".txt"), annot); // Save annotation
         Logging.warning(fIdentity + ": file " + oldfile + " quarantined as " +
                         newxmlfile.getPath() + " (" + annot + ")");
      } catch (Exception e) {
         Logging.warning(fIdentity + ": file " + oldfile.getPath() + " could not be quarantined and remains in input queue.");
         Logging.debug(fIdentity + ": exception details: ", e);
      }
   }

   public void saveHistory(Record current, String xml, String rawxml,
                           String extraxml, boolean gotreplication)
      throws java.io.IOException {
      Date serverDate = current.getServerDate();
      File where = getDirectory("history");

      File historyfile = File.createTempFile("history-", ".xml", where);
      String filename = historyfile.getPath();

      StringBuffer data;
      if (gotreplication) {
         data = new StringBuffer("history" + "|" + serverDate.getTime() +
                                 "|" + xml + "|" + rawxml);
         if (extraxml != null) {
            data.append("|" + extraxml);
         }
      } else {
         data = new StringBuffer("historymd5" + "|" + serverDate.getTime() +
                                 "|" + xml + "|" + current.getmd5());
      }
      XP.save(filename, data.toString());
   }

   public ArrayList convert(String xml) throws Exception {
      ArrayList records = null;

      try {
         records = fConverter.convert(xml);
      } catch (Exception e) {
         Logging.info(fIdentity + ": Parse error:  " + e.getMessage());
         Logging.info(fIdentity + ": XML:  " + "\n" + xml);
         throw e;
      }

      // The usage records array list is now populated with all the
      // job usage records found in the given XML file return it to
      // the caller.
      return records;
   }

   private boolean handleUnexpectedException(String rId, Exception e,
                                             boolean gotreplication, Record current) {
      return handleUnexpectedException(rId, e, gotreplication, current,
                                       "Received unexpected exception");
   }

   private boolean handleUnexpectedException(String rId, Exception e,
                                             boolean gotreplication, Record current,
                                             String message) {
      Logging.warning(fIdentity + rId + ": " + message + " " + e.getMessage());
      Logging.debug(fIdentity + rId + ": exception details:", e);
      if ((e instanceof ConnectionException) ||
          (e instanceof com.mysql.jdbc.CommunicationsException) ||
          (e.getCause() instanceof com.mysql.jdbc.CommunicationsException) ||
          (!HibernateWrapper.databaseUp())) {
         Logging.warning(fIdentity + ": Communications error: " + "shutting down");
         return false;
      } else {
         try {
            if (gotreplication) {
               fErrorRecorder.saveSQL("Replication", "SQLError", current);
            } else {
               fErrorRecorder.saveSQL("Probe", "SQLError", current);
            }
         } catch (Exception e2) {
            saveQuarantineRecord(current, rId + ": unable to record error in table", e2); 
            if ((e2 instanceof ConnectionException) ||
                (e2 instanceof com.mysql.jdbc.CommunicationsException) ||
                (e2.getCause() instanceof com.mysql.jdbc.CommunicationsException) ||
                (!HibernateWrapper.databaseUp())) {
               Logging.warning(fIdentity + ": Error saving in DupRecord table: " + "shutting down");
               return false;
            }
         }
      }
      Logging.warning(fIdentity + ": Error in Process: ", e);
      Logging.warning(fIdentity + ": Current: " + current);
      return true;
   }

   private boolean maybeHandleDuplicateRecord(ConstraintViolationException e,
                                              Record current,
                                              String rId,
                                              boolean gotreplication,
                                              boolean gothistory) throws Exception {
      Logging.debug(fIdentity + rId + ": handling ConstraintViolationException caused by SQL: " +
                    e.getSQL());
      long dupdbid = 0;
      boolean needCurrentSaveDup = false;
      Transaction tx = null;
      if (fgDuplicateExceptionFinder.matcher(e.getSQLException().getMessage()).find()){ 
      //if (fgDuplicateExceptionFinder.matcher(e.getSQLException().getMessage()).find() &&
      //    fgMetaFinder.matcher(e.getSQL()).find()) { // Duplicate of an interesting table
         if (current.getTableName().equals("JobUsageRecord")) {
            UserIdentity newUserIdentity =
               ((JobUsageRecord) current).getUserIdentity();
            Session dup_session = HibernateWrapper.getSession();
            tx = dup_session.beginTransaction();
            Query dup_query = dup_session.createQuery("select record from " +
                                                      "JobUsageRecord " +
                                                      "record where " +
                                                      "record.md5 = " +
                                                      "'" +
                                                      current.getmd5() +
                                                      "'").setCacheMode(CacheMode.IGNORE);
            tx.commit();
            boolean savedCurrent = false;
            JobUsageRecord original_record = (JobUsageRecord) dup_query.uniqueResult();
            if (original_record == null) {
               return true;
            }
            dupdbid = original_record.getRecordId();
            UserIdentity originalUserIdentity = original_record.getUserIdentity();
            if (newUserIdentity == null) {
               return true;
            }
            boolean newerIsBetter = false;
            String replaceReason = null;
            if (originalUserIdentity == null) {
               newerIsBetter = true;
               replaceReason = "original UserIdentity block is null";
            } else if ((newUserIdentity.getVOName() !=
                        null) &&
                       (newUserIdentity.getVOName().
                        length() != 0) &&
                       (!newUserIdentity.getVOName().
                        equalsIgnoreCase("Unknown"))) {
               // Have something with which to replace it.
               replaceReason = "New VOName \"" + newUserIdentity.getVOName() + "\" is better";
               if (originalUserIdentity.getVOName() ==
                   null) {
                  newerIsBetter = true;
                  replaceReason +=
                     " -- original VOName is null";
               } else if (originalUserIdentity.getVOName().length() == 0) {
                  newerIsBetter = true;
                  replaceReason +=
                     " -- original VOName is empty";
               } else if (originalUserIdentity.getVOName().
                          equalsIgnoreCase("Unknown")) {
                  newerIsBetter = true;
                  replaceReason +=
                     " -- original VOName is " +
                     "\"Unknown\")";
               } else if ((newUserIdentity.getVOName().startsWith("/")) &&
                          (!originalUserIdentity.getVOName().startsWith("/"))) {
                  newerIsBetter = true;
                  replaceReason +=
                     " -- original VOName is not fully qualified";
               }
            } // End if (originalUserIdentity == null)
            if (!newerIsBetter) { // Still haven't decided
               if ((newUserIdentity.getCommonName() != null) &&
                   (!newUserIdentity.getCommonName().
                    startsWith("Generic"))) {
                  replaceReason =
                     "New CommonName is better";
                  if (originalUserIdentity.getCommonName() == null) {
                     newerIsBetter = true;
                     replaceReason +=
                        " -- original CommonName " +
                        "is null";
                  } else if (originalUserIdentity.getCommonName().length() == 0) {
                     newerIsBetter = true;
                     replaceReason +=
                        " -- original CommonName " +
                        "is empty";
                  } else if (originalUserIdentity.getCommonName().
                             startsWith("Generic")) {
                     newerIsBetter = true;
                     replaceReason +=
                        " -- original CommonName " +
                        "is generic";
                  }
               } else if ((newUserIdentity.getKeyInfo() != null) &&
                          (originalUserIdentity.getKeyInfo() == null)) {
                  newerIsBetter = true;
                  replaceReason =
                     "Original KeyInfo is null";
               }
            } // End if (!newerIsBetter)
            if (newerIsBetter) {
               int nTries = 0;
               boolean keepTrying = true;
               while (keepTrying) {
                  ++nTries;
                  try {
                     if (! HibernateWrapper.isFullyConnected(dup_session)) {
                        dup_session = HibernateWrapper.getSession();
                     }
                     tx = dup_session.beginTransaction();
                     // Keep the new one and ditch the old
                     Logging.fine(fIdentity + rId +
                                  ": Replacing record " +
                                  dupdbid +
                                  " with \"better\" " +
                                  "record (" +
                                  replaceReason + ").");
                     original_record.maybeRemoveFromSummary(dup_session);
                     // Delete the record.
                     String originalXml =
                        original_record.asXML();
                     String originalTableName =
                        original_record.getTableName();
                     dup_session.delete(original_record);
                     if (!savedCurrent) {
                        // If we haven't saved
                        // the current record
                        // yet, flush and commit
                        // the delete
                        // (important) and then
                        // save the current record.
                        dup_session.flush();
                        tx.commit();
                        tx = dup_session.beginTransaction();
                        dup_session.save(current);
                        current.executeTrigger(dup_session);
                        savedCurrent = true;
                     }
                     tx.commit();
                     keepTrying = false;
                     if (original_record.setDuplicate(true)) {
                        if (gotreplication) {
                           fErrorRecorder.saveDuplicate("Replication",
                                                       "Duplicate",
                                                       current.getRecordId(),
                                                       originalXml,
                                                       originalTableName);
                        } else if (gothistory) { // NOP
                        } else {
                           fErrorRecorder.saveDuplicate("Probe",
                                                       "Duplicate",
                                                       current.getRecordId(),
                                                       originalXml,
                                                       originalTableName);
                        }
                     }
                  } catch (Exception e2) {
                     HibernateWrapper.closeSession(dup_session);
                     if (dup_session.isOpen()) throw new ConnectionException(e2); // Session could not close; DB problem
                     if (!LockFailureDetector.detectAndReportLockFailure(e2, nTries, fIdentity)) {
                        throw e2; // Re-throw;
                     }
                  } // End try (resolve duplicate JobUsageRecord)
               } // End while (keepTrying)
            } // End if (newerIsBetter)
            if (!savedCurrent) {
               needCurrentSaveDup =
                  current.setDuplicate(true);
               if (!needCurrentSaveDup) { // Save probe object anyway
                  Probe localprobe = current.getProbe();
                  if (localprobe != null) {
                     int nTries = 0;
                     boolean keepTrying = true;
                     while (keepTrying) {
                        ++nTries;
                        try {
                           if (! HibernateWrapper.isFullyConnected(dup_session)) {
                              dup_session = HibernateWrapper.getSession();
                           }
                           tx = dup_session.beginTransaction();
                           dup_session.saveOrUpdate(localprobe);
                           dup_session.flush();
                           tx.commit();
                           dup_session.close();
                           keepTrying = false;
                        } catch (Exception e2) {
                           HibernateWrapper.closeSession(dup_session);
                           if (dup_session.isOpen()) throw new ConnectionException(e2); // Session could not close; DB problem
                           if (!LockFailureDetector.detectAndReportLockFailure(e2, nTries, fIdentity)) {
                              throw e2; // Re-throw
                           }
                        } // End try (save probe)
                     } // End while (keepTrying)
                  } // End if (localprobe != null)
               } // End if (!needCurrentSaveDup)
            } // End if (!savedCurrent)
            if (HibernateWrapper.isFullyConnected(dup_session)) {
               dup_session.close();
            }
         } else { // Not JobUsageRecord
            needCurrentSaveDup = current.setDuplicate(true);
            Session dup2_session = null;
            try {
               dup2_session = HibernateWrapper.getSession();
               String cmd = "select dbid from " +
                  current.getTableName() +
                  "_Meta record where " +
                  "record.md5 = " +
                  "'" +
                  current.getmd5() +
                  "'";
               tx = dup2_session.beginTransaction();
               //Integer dup_dbid = (Integer) (dup2_session.createSQLQuery(cmd).uniqueResult());
               Long dup_dbid = (Long) (dup2_session.createSQLQuery(cmd).uniqueResult());
               tx.commit();
               // Avoid infinite growth
               if (current instanceof ProbeDetails) {
                  if (fProbeDetails.size() > 500) {
                     fProbeDetails.clear();
                  }
                  fProbeDetails.put(current.getmd5(), dup_dbid);
               }
               dupdbid = dup_dbid;
               dup2_session.close();
            } catch (Exception sub_except) {
               // Ignore all exceptions, if we can get and store the
               // cached value, it will simply need to be redone later.
               HibernateWrapper.closeSession(dup2_session);
            }
            if (!needCurrentSaveDup) {
               Logging.fine(fIdentity + rId +
                            ": " + "Ignore duplicate of record " +
                            dupdbid);
            }
         } // End if (JobUsageRecord)
         if (needCurrentSaveDup) {
            try {
               Logging.fine(fIdentity + rId +
                            ": " + (gothistory ? "Ignore" : "Save") +
                            " duplicate of record " +
                            dupdbid);
               if (gotreplication) {
                  fErrorRecorder.saveDuplicate("Replication",
                                              "Duplicate",
                                              dupdbid,
                                              current);
               } else if (gothistory) { // NOP
               } else {
                  fErrorRecorder.saveDuplicate("Probe",
                                              "Duplicate",
                                              dupdbid,
                                              current);
               }
            } catch (Exception e2) {
               saveQuarantineRecord(current, rId + ": unable to save duplicate in DupRecord table", e2);
            }
         } // End if (needCurrentSaveDup)
      } else {
         // Constraint exception, but not a duplicate on a table we're expecting. 
         return false;
      } // End (constraint failure is an expected duplicate)
    
      return true;
   } // End handleConstrainViolationException()

} // End class RecordProcessor.

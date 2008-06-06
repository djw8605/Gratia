package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.util.ArrayList;

import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Hashtable;

import java.text.*;

import java.io.*;

import net.sf.gratia.storage.*;


import org.hibernate.*;
import org.hibernate.exception.ConstraintViolationException;

public class ListenerThread extends Thread
{
    static String replication = "replication";

    String ident = null;
    String directory = null;         // Location of the incoming messages.
    Hashtable global;
    long ninput = 0;                 // Number of input messages processed
    long nrecords = 0;                // Number of records processed;
    String directory_part = null;    // stemp for history and old subdirectory
    long recordsPerDirectory = 10000; // Maximum number of records per directory.

    //
    // database parameters
    //

    org.hibernate.Session session;
    Transaction tx;
    RecordUpdaterManager updater = new RecordUpdaterManager();
    RecordConverter converter = new RecordConverter();

    int itotal = 0;
    boolean duplicateCheck = false;
    Properties p;

    XP xp = new XP();

    StatusUpdater statusUpdater = null;
    NewVOUpdate newVOUpdate = null;
    ErrorRecorder errorRecorder = new ErrorRecorder();

    Object lock;

    String historypath = "";

    //
    // various things used in the update loop
    //

    boolean stopflag = false;

    public ListenerThread(String ident,
                          String directory,
                          Object lock,
                          Hashtable global)
    {      
        this.ident = ident;
        this.directory = directory;
        this.lock = lock;
        this.global = global;

        File tmp = new File(directory);
        this.directory_part = tmp.getName();

        loadProperties();
        try
            {
                String url = p.getProperty("service.jms.url");
                Logging.log("");
                Logging.log("ListenerThread: " + ident + ":" + directory + ": Started");
                Logging.log("");
            }
        catch (Exception e)
            {
                e.printStackTrace();
            }
        historypath = System.getProperties().getProperty("catalina.home") + "/gratia/data/";

        JobUsageRecordUpdater.AddDefaults(updater);
    }

    public void loadProperties()
    {
        p = Configuration.getProperties();
        String temp = p.getProperty("service.duplicate.check");
        if (temp.equals("1"))
            duplicateCheck = true;
        else
            duplicateCheck = false;
        Logging.log("ListenerThread: " + ident + ":Duplicate Check: " + duplicateCheck);

        try 
            {
                long max_record = Long.parseLong(p.getProperty("maintain.recordsPerDirectory"));
                recordsPerDirectory = max_record;
            }
        catch (Exception e) 
            {
                // Only issue a warning here
                Logging.log("ListernerThread: " + ident + " Failed to parse property maintain.recordsPerDirectory");
            }            
    }

    public void stopRequest()
    {
        stopflag = true;
        Logging.log("ListenerThread: " + ident + ":Stop Requested");
    }

    public void run()
    {
        while (true)
            {
                if (stopflag)
                    {
                        Logging.log("ListenerThread: " + ident + ":Exiting");
                        return;
                    }

                if (!HibernateWrapper.databaseUp())
                    {
                        HibernateWrapper.start();
                        if (HibernateWrapper.databaseDown)
                            {
                                Logging.log("ListenerThread: " + ident + ":Hibernate Down: Sleeping");
                                try
                                    {
                                        Thread.sleep(30 * 1000);
                                    }
                                catch (Exception ignore)
                                    {
                                    }
                                continue;
                            }
                    }
                if (stopflag)
                    {
                        Logging.log("ListenerThread: " + ident + ":Exiting");
                        return;
                    }
                int nfiles = loop();
                if (stopflag)
                    {
                        Logging.log("ListenerThread: " + ident + ":Exiting");
                        return;
                    }
                if (nfiles==0) {
                    // Sleep only if there is no file waiting.
                    try
                        {
                            Thread.sleep(30 * 1000);
                        }
                    catch (Exception ignore)
                        {
                        }
                }
            }
    }

    public int loop()
    {
        String file = "";
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

        if (!HibernateWrapper.databaseUp())
            return 0;

        String files[] = xp.getFileList(directory);

        int nfiles = files.length;

        if (nfiles == 0)
            return 0;

        statusUpdater = new StatusUpdater();
        newVOUpdate = new NewVOUpdate();

        for (int i = 0; i < files.length; i++)
            {
                global.put("listener", new java.util.Date());

                if (stopflag)
                    {
                        Logging.log("ListenerThread: " + ident + ":Exiting");
                        return nfiles;
                    }

                file = files[i];
                blob = xp.get(files[i]);

                xml = "";
                rawxmllist.clear();
                extraxmllist.clear();
                md5list.clear();
                historydatelist.clear();

                gotreplication = gothistory = false;

                ninput = ninput + 1;

                try
                    {
                        saveIncoming(blob);
                    }
                catch (Exception e) 
                    {
                        Logging.log("ListenerThread: " + ident + ":loop failed to backup incoming message. \nError: "+e.getMessage()+"\n");
                    }

                Record current = null;

                //
                // see if trace requested
                //

                if (p.getProperty("service.datapump.trace").equals("1"))
                    {
                        Logging.log("ListenerThread: " + ident + ":XML Trace:" + "\n\n" + blob + "\n\n");
                    }

                //
                // see if we got a normal update or a replicated one
                //

                try
                    {
                        int nseen = 0;
                        if (blob.startsWith(replication))
                            {
                                StringTokenizer st = new StringTokenizer(blob, "|");

                                gotreplication = true;

                                while(st.hasMoreTokens()) {
                                    String val = st.nextToken();
                                    if (val.equals(replication) && st.hasMoreTokens()) {
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
                                        if (extraxml.endsWith(replication)) {
                                            extraxml = extraxml.substring(0,extraxml.length()-replication.length());
                                        }
                                        extraxmllist.add(extraxml);
                                    } else {
                                        extraxmllist.add(null);
                                    }
                                    nseen = nseen + 1;
                                }
                            }
                                
                        else if (blob.startsWith("history"))
                            {
                                gothistory = true;
                                StringTokenizer st = new StringTokenizer(blob, "|");
                                while(st.hasMoreTokens()) {
                                    st.nextToken();
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
                                }

                            }
                        else if (blob.startsWith("historymd5"))
                            {
                                gothistory = true;
                                StringTokenizer st = new StringTokenizer(blob, "|");
                                while (st.hasMoreTokens()) {
                                    st.nextToken();
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
                                }
                            }
                        else {
                            xml = blob;
                            nseen = nseen + 1;
                        }
                        if (nseen>1) {
                            xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><RecordEnvelope>" + xml + "</RecordEnvelope>";
                        }
                    }
                catch (Exception e)
                    {
                        Logging.log("ListenerThread: " + ident + ":Error:Processing File: " + file);
                        Logging.log("ListenerThread: " + ident + ":Blob: " + blob);
                        try
                            {
                                File temp = new File(file);
                                temp.delete();
                            }
                        catch (Exception ignore)
                            {
                            }
                        continue;
                    }

                if (xml == null)
                    {
                        Logging.log("ListenerThread: " + ident + ":Error:No Data To Process: " + file);
                        try
                            {
                                File temp = new File(file);
                                temp.delete();
                            }
                        catch (Exception ignore)
                            {
                            }
                        continue;
                    }

                Logging.log("ListenerThread: " + ident + ":Processing: " + file);

                ArrayList records = new ArrayList();

                try {
                    records = convert(xml);
                }
                catch (Exception e) {
                    try {
                        if (gotreplication)
                            errorRecorder.saveParse("Replication", "Parse", xml);
                        else if (gothistory)
                            errorRecorder.saveParse("History", "Parse", xml);
                        else
                            errorRecorder.saveParse("Probe", "Parse", xml);
                    }
                    catch (Exception ignore) { }
                }

                for (int j = 0; j < records.size(); j++) {
                    // Logging.log("ListenerThread: " + ident + ":Before Begin Transaction");
                    session = HibernateWrapper.getSession();
                    tx = session.beginTransaction();
                    try {
                        // Logging.log("ListenerThread: " + ident + ":After Begin Transaction");

                        current = (Record)records.get(j);

                        Probe probe = statusUpdater.update(session, current, xml);
                        current.setProbe(probe);

                        // Logging.log("ListenerThread: " + ident + ":After New Probe Update");
                        updater.Update(current);

                        if ((!gothistory) || (!(md5list.size()>j)) || md5list.get(j) == null) {
                            String md5key = current.computemd5();
                            current.setmd5(md5key);
                            if (current.getTableName()
                                .equals("JobUsageRecord")) {
                                // Need to do this to keep number of
                                // duplicates making it into the DB
                                // under control during the upgrade
                                // procedure. This will be removed for a
                                // future upgrade as it is only really
                                // necessary for very large DBs.
                                Logging.debug("Calculating and saving " +
                                              "old-style checksum for " +
                                              "JobUsageRecord");
                                JobUsageRecord jRecord = (JobUsageRecord) current;
                                String oldMd5 = jRecord.computeOldMd5();
                                jRecord.setoldMd5(oldMd5);
                            }
                        } else {
                            current.setmd5((String)md5list.get(j));
                        }
                        current.setDuplicate(false);

                        synchronized (lock)
                            {
                                newVOUpdate.check(current, session);
                            }
                        synchronized (lock)
                            {
                                current.AttachContent(session);
                            }

                        String rawxml = null;
                        String extraxml = null;
                        if (rawxmllist.size()>j) {
                            rawxml = (String)rawxmllist.get(j);
                            if (rawxml != null)
                                current.setRawXml(rawxml);
                        }
                        if (extraxmllist.size()>j) {
                            extraxml = (String)extraxmllist.get(j);
                            if (extraxml != null)
                                current.setExtraXml(extraxml);
                        }
                        Logging.log("ListenerThread: " + ident + ":Before Hibernate Save");
                        if (gothistory) {
                            Date serverDate = new Date(Long.parseLong((String)historydatelist.get(j)));
                            current.setServerDate(serverDate);
                        }
                        session.save(current);
                        //
                        // now - save history
                        //
                        if (!gothistory) {
                            saveHistory(current,xml,rawxml,extraxml,gotreplication);
                        }
                        // Logging.log("ListenerThread: " + ident + ":After Hibernate Save");
                        // Logging.log("ListenerThread: " + ident + ":Before Transaction Commit");
                        session.flush();
                        tx.commit();
                        session.close();
                        // Logging.log("ListenerThread: " + ident + ":After Transaction Commit");
                        nrecords = nrecords + 1;
                    }
                    catch (ConstraintViolationException e) {
                        tx.rollback();
                        session.close();
                        int dupdbid = 0;
                        Boolean needCurrentSaveDup = false;
                        if (e.getSQLException().getMessage().matches(".*\\b[Dd]uplicate\\b.*")) {
                            if (current.getTableName().equals("JobUsageRecord")) {
                                UserIdentity newUserIdentity = ((JobUsageRecord) current).getUserIdentity();
                                session = HibernateWrapper.getSession();
                                Query q =
                                    session.createQuery("select record from " +
                                                        "JobUsageRecord " +
                                                        "record where " +
                                                        "record.md5 = " +
                                                        "'" +
                                                        current.getmd5() +
                                                        "'")
                                    .setCacheMode(CacheMode.IGNORE);
                                ScrollableResults dups = q.scroll(ScrollMode.FORWARD_ONLY);
                                Boolean savedCurrent = false;
                                try {
                                    if (dups.next()) {
                                        tx = session.beginTransaction();
                                        JobUsageRecord original_record = (JobUsageRecord) dups.get(0);
                                        dupdbid = original_record.getRecordId();
                                        UserIdentity originalUserIdentity = original_record.getUserIdentity();
                                        if (newUserIdentity == null) continue; // No replacement
                                        if ((originalUserIdentity == null) ||
                                            ((newUserIdentity.getVOName() != null) &&
                                             (newUserIdentity.getVOName().length() != 0) &&
                                             ((originalUserIdentity.getVOName() == null) ||
                                              (originalUserIdentity.getVOName().length() == 0) ||
                                              (newUserIdentity.getVOName().startsWith("/")) ||
                                              (originalUserIdentity.getVOName().equalsIgnoreCase("Unknown")) ||
                                              (originalUserIdentity.getCommonName().startsWith("Generic")) ||
                                              (originalUserIdentity.getKeyInfo() == null)))) {
                                            // Keep the new one and ditch the old
                                            Logging.info("ListenerThread: " + ident + ": replacing record " +
                                                         dupdbid + " with \"better\" record.");
                                            // Has to be in this order otherwise we'll get a duplicate error.
                                            if (original_record.setDuplicate(true)) {
                                                if (gotreplication) {
                                                    errorRecorder.saveDuplicate("Replication", "Duplicate",
                                                                                current.getRecordId(),
                                                                                original_record);
                                                } else if (gothistory) {
                                                    ;
                                                } else {
                                                    errorRecorder.saveDuplicate("Probe", "Duplicate",
                                                                                current.getRecordId(),
                                                                                original_record);
                                                }
                                            }
                                            SummaryUpdater.removeFromSummary(original_record.getRecordId(),
                                                                             session);
                                            session.delete(original_record);
                                            if (!savedCurrent) {
                                                session.flush();
                                                tx.commit();
                                                tx = session.beginTransaction();
                                                session.save(current);
                                                savedCurrent = true;
                                            }
                                        }
                                        session.flush();
                                        tx.commit();
                                    }
                                    if (!savedCurrent) {
                                        needCurrentSaveDup = current.setDuplicate(true);
                                    }
                                    session.close();
                                }
                                catch (Exception e2) {
                                    tx.rollback();
                                    session.close();
                                    Logging.warning("ListenerThread: " + ident +
                                                    ": caught exception resolving duplicates for record with md5 checksum" +
                                                    current.getmd5() +
                                                    " -- all duplicates of same will remain in DB", e2);
                                }
                            } else {
                                needCurrentSaveDup = current.setDuplicate(true);
                            }
                            if (needCurrentSaveDup) {
                                //Logging.log("ListenerThread: " + ident + ":Before Save Duplicate");
                                Logging.debug("ListenerThread: " + ident + ": save duplicate of record " +
                                              dupdbid);
                                try {
                                    if (gotreplication) {
                                        errorRecorder.saveDuplicate("Replication", "Duplicate", dupdbid, current);
                                    } else if (gothistory) {
                                        // If we are reprocessing the history date, we should not
                                        // be recording the possible duplicates.
                                        ;
                                    } else {
                                        errorRecorder.saveDuplicate("Probe", "Duplicate", dupdbid, current);
                                    }
                                    //Logging.log("ListenerThread: " + ident + ":After Save Duplicate");
                                }
                                catch (Exception ignore) { }
                            }
                        } else { // Constraint exception, but not a duplicate: oops!
                            if (HibernateWrapper.databaseUp()) {
                                try {
                                    if (gotreplication) {
                                        errorRecorder.saveSQL("Replication", "SQLError", current);
                                    } else {
                                        errorRecorder.saveSQL("Probe", "SQLError", current);
                                    }
                                }
                                catch (Exception ignore) { }
                            } else {
                                Logging.log("ListenerThread: " + ident + ":Communications Error:Shutting Down");
                                return 0; 
                            }
                            Logging.warning("ListenerThread: " + ident + ":Error In Process: ",e);
                            Logging.warning("ListenerThread: " + ident + ":Current: " + current);
                        }
                    }
                    catch (Exception e) {
                        // Must close session!
                        tx.rollback();
                        session.close();
                        if (HibernateWrapper.databaseUp()) {
                            try {
                                if (gotreplication) {
                                    errorRecorder.saveSQL("Replication", "SQLError", current);
                                } else {
                                    errorRecorder.saveSQL("Probe", "SQLError", current);
                                }
                            }
                            catch (Exception ignore) { }
                        } else {
                            Logging.log("ListenerThread: " + ident + ":Communications Error:Shutting Down");
                            return 0; 
                        }
                        Logging.warning("ListenerThread: " + ident + ":Error In Process: ",e);
                        Logging.warning("ListenerThread: " + ident + ":Current: " + current);
                    } // End general catch
                }
                // Logging.log("ListenerThread: " + ident + ":Before File Delete: " + file);
                try {
                    File temp = new File(file);
                    temp.delete();
                }
                catch (Exception ignore) {

                    // Logging.log("ListenerThread: " + ident + ":File Delete Failed: " + file + " Error: " + ignore);
                }
                // Logging.log("ListenerThread: " + ident + ":After File Delete: " + file);
                itotal++;
                Logging.log("ListenerThread: " + ident + ":Total Input Messages: " + itotal);
                Logging.log("ListenerThread: " + ident + ":Total Records: " + nrecords);
                
            }
        return nfiles; 
    }

    public File getDirectory(String what) 
    {
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddkk");
        String path = historypath + what + "-" + format.format(now);
        File directory = new File(path);
        if (!directory.exists())
            {
                directory.mkdir();
            }
        
        long part = nrecords / recordsPerDirectory;

        File subdir = new File(directory,directory_part + "-" + part);
        
        if (!subdir.exists())
            {
                subdir.mkdir();
            }
        return subdir;
    }


    public void saveIncoming(String data) throws java.io.IOException
    {
        File where = getDirectory("old");
        File errorfile = File.createTempFile("old-", ".xml", where);
        String filename = errorfile.getPath();
        xp.save(filename, data);
    }


    public void saveHistory(Record current, String xml, String rawxml, String extraxml, boolean gotreplication) throws java.io.IOException
    {
        Date serverDate = current.getServerDate();
        File where = getDirectory("history");

        File historyfile = File.createTempFile("history-", ".xml", where);
        String filename = historyfile.getPath();

        StringBuffer data;
        if (gotreplication) {
            data = new StringBuffer("history" + "|" + serverDate.getTime() + "|" + xml + "|" + rawxml);
            if (extraxml != null) {
                data.append("|" + extraxml);
            }
        } else {
            data = new StringBuffer("historymd5" + "|" + serverDate.getTime() + "|" + xml + "|" + current.getmd5());
        }
        xp.save(filename,data.toString());
    }
    
    public ArrayList convert(String xml) throws Exception
    {
        ArrayList records = null;

        try {
            records = converter.convert(xml);
        }
        catch (Exception e)
            {
                Logging.log("ListenerThread: " + ident + ":Parse error:  " + e.getMessage());
                Logging.log("ListenerThread: " + ident + ":XML:  " + "\n" + xml);
                throw e;
            }

        // The usage records array list is now populated with all the job usage records found in the given XML file
        //  return it to the caller.
        return records;
    }

}

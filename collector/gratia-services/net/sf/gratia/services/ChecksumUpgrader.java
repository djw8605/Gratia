package net.sf.gratia.services;

import net.sf.gratia.util.Logging;

import net.sf.gratia.storage.JobUsageRecord;
import net.sf.gratia.storage.UserIdentity;
import net.sf.gratia.storage.SummaryUpdater;

import java.math.BigInteger;
import java.sql.*;
import java.util.Iterator;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.*;

public class ChecksumUpgrader extends Thread {

    private ErrorRecorder errorRecorder = new ErrorRecorder();

    private CollectorService collectorService = null;

    private enum Status {
        UPDATING_CHECKSUMS, RESOLVING_DUPLICATES,
            BLOCKING_UPDATES,
            RESOLVING_DUPLICATES_BLOCKING,
            UPDATING_MD5V2_INDEX,
            DROPPING_OLD_MD5_INDEX,
            COMPLETE,
            STOPPED }

    private Status checksumUpgradeStatus = Status.STOPPED;

    public ChecksumUpgrader(CollectorService cS) {
        collectorService = cS;
    }

    public String checksumUpgradeStatus() {
        // Reset status if necessary
        if ((!isAlive()) && !(checksumUpgradeStatus == Status.COMPLETE)) {
            checksumUpgradeStatus = Status.STOPPED;
        }
        return checksumUpgradeStatus.toString();
    }

    public void run() {

        // Procedure is as follows:
        //
        // 1. Go through all records without a checksum and calculate
        // one. Since all new records get a new-style checksum, there is
        // no race condition here.
        //
        // 2. Once we are in the situation that all records have a new
        // checksum, go through the entire DB and fix all "duplicate"
        // records to have the best user / VO information.
        //
        // 3. Loop (2) until number of duplicates stops decreasing,
        // which will indicate that as many duplicates are coming in as
        // it takes to fix the last lot.
        //
        // 4. Lock the DB.
        //
        // 5. Fix the last lot of duplicates.
        //
        // 6. Drop the old non-unique index on md5v2and create a new, unique one.
        //
        // 7. Drop the index on md5.
        //
        // 8. Release the lock and complete.
        //
        // A failure at any stage will result in the process picking up
        // where it left off at the next restart.

        // 1.
        checksumUpgradeStatus = Status.UPDATING_CHECKSUMS;
        Logging.info("ChecksumUpgrader: updating all checksums in JobUsageRecord_Meta");
        try {
//             runGC();
//             Logging.debug("ChecksumUpgrader: memory report -- " + usedMemory());
            batchUpdateChecksums();
//             runGC();
//             Logging.debug("ChecksumUpgrader: memory report -- " + usedMemory());
        }
        catch (Exception e) {
            Logging.warning("ChecksumUpgrader: batch update of checksums failed!", e);
            return;
        }

        // 2., 3. Fix duplicates, iterating until we're as done as we
        // can be without stopping updates.
        checksumUpgradeStatus = Status.RESOLVING_DUPLICATES;
        Logging.info("ChecksumUpgrader: fixing all duplicates based on md5v2");
        try {
            fixAllDuplicates();
        }
        catch (Exception e) {
            Logging.warning("ChecksumUpgrader: looped fix of duplicates failed!", e);
            return;
        }

        try {
            checksumUpgradeStatus = Status.BLOCKING_UPDATES;
            Logging.info("ChecksumUpgrader: preventing external changes to listener thread state");
            // 4. Lock the DB.
            synchronized (collectorService) {
                Boolean activeStatus = collectorService.databaseUpdateThreadsActive();
            
                if (activeStatus) {
                    Logging.info("ChecksumUpgrader: deactivating listener threads");
                    collectorService.stopDatabaseUpdateThreads();
                }
                // 5. Final duplicate resolution pass.
                checksumUpgradeStatus = Status.RESOLVING_DUPLICATES_BLOCKING;
                Logging.info("ChecksumUpgrader: final pass on duplicates");
                fixDuplicatesOnce();

                // 6. Make index unique.
                checksumUpgradeStatus = Status.UPDATING_MD5V2_INDEX;
                Logging.info("ChecksumUpgrader: make index on md5v2 unique (could take some time)");
                Session session = HibernateWrapper.getSession();
                Transaction tx = session.beginTransaction();
                try {
                    Connection connection = session.connection();
                    Statement statement = connection.createStatement();
                    statement
                        .execute("alter table JobUsageRecord_Meta "
                                 + "drop index index17"
                                 + ", " 
                                 + "add unique index index17(md5v2)"
                                 );
                    tx.commit();
                }
                catch (Exception e) {
                    tx.rollback();
                    session.close();
                    Logging.warning("ChecksumUpgrader: update of index on md5v2 failed!", e);
                    return;
                }
                session.close();

                // 7. Drop the index on md5.
                checksumUpgradeStatus = Status.DROPPING_OLD_MD5_INDEX;
                Logging.info("ChecksumUpgrader: drop index on old md5 column");
                try {
                    DropIndexByColumn("JobUsageRecord_Meta", "md5");
                }
                catch (Exception e) {
                    Logging.warning("ChecksumUpgrader: removal of index(es) on md5 failed! \nNote: vestigial index(es) on old md5 column will be removed on collector restart", e);
                    return;
                }

                // 8. Reactivate undates.
                if (activeStatus) {
                    Logging.info("ChecksumUpgrader: reactivating listener threads");
                    collectorService.startDatabaseUpdateThreads();
                }
            }
        }
        catch (Exception e) {
            Logging.warning("ChecksumUpgrader: final stage fix and index recreation failed!", e);
            return;
        }
        checksumUpgradeStatus = Status.COMPLETE;
        Logging.info("ChecksumUpgrader: checksum upgrade operation complete");
    }

    private void fixAllDuplicates() {
        long duplicatesFixedLastIteration = 0;
        long duplicatesFixedThisIteration = 0;
        long totalDuplicatesFixed = 0;
        int maxIterations = 200;
        int iterations = 0;
        do {
            duplicatesFixedLastIteration = duplicatesFixedThisIteration;
            duplicatesFixedThisIteration = fixDuplicatesOnce();
            totalDuplicatesFixed += duplicatesFixedThisIteration;
        } while (
                 // Set maximum number to avoid infinite loop
                 (++iterations < maxIterations) &&
                 // Fixed less this time than last
                 (duplicatesFixedThisIteration < duplicatesFixedLastIteration) &&
                 // Fixed at least 100 dupes last time.
                 duplicatesFixedThisIteration > 100);
        if (iterations >= maxIterations) {
            Logging.info("ChecksumUpgrader: maximum of " +
                         maxIterations +
                         " exceeded for duplicate resolution exceeded -- " +
                         " final (locked) pass may take some time.");
        }
        Logging.info("ChecksumUpgrader: main duplicate resolution phase complete in " +
                     iterations + " iterations.");
        Logging.info("ChecksumUpgrader: resolved duplicates for a total of " +
                     totalDuplicatesFixed +
                     " duplicated checksums.");
    }

    private long fixDuplicatesOnce() {
        long nDupsFixed = 0;
        Logging.fine("fixDuplicatesOnce: starting duplicate resolution cycle");
        Boolean continueLooping = true;
        while (continueLooping) {
            Session session = HibernateWrapper.getSession();
            session.setFlushMode(FlushMode.COMMIT);
            Query q =
                session.createSQLQuery("select md5v2, count(*) as `Count` " +
                                       "from JobUsageRecord_Meta " +
                                       "where md5v2 is not null " +
                                       "group by md5v2 having Count > 1")
                .setCacheMode(CacheMode.IGNORE)
                .setMaxResults(10000); // Memory usage limiter
            List csList = q.list();
            Logging.log("fixDuplicatesOnce: this cycle detected " +
                          csList.size() + " duplicated checksums");
            Iterator csIter = csList.iterator();
            session.close();
            int checksumsReadThisLoop = 0;
            while (csIter.hasNext()) {
                ++checksumsReadThisLoop;
                session = HibernateWrapper.getSession();
                Transaction tx;
                tx = session.beginTransaction();
                String md5 = "";
                try {
                    // Multiple results.
                    Object[] csRow = (Object[]) csIter.next();
                    md5 = (String) csRow[0]; // MD5 checksum
                    // Number of entries with that checksum
                    int nDups = ((BigInteger) csRow[1]).intValue();
                    Logging.debug("fixDuplicatesOnce: resolving " + nDups +
                                  " duplicates with checksum " + md5);
                    Query rq =
                        session.createQuery("select record from " +
                                            "JobUsageRecord record " +
                                            " where record.md5 = '" + md5 +
                                            "' order by record.RecordId")
                        .setCacheMode(CacheMode.IGNORE);
                    Iterator rIter = rq.iterate();
                    if (!rIter.hasNext()) {
                        Logging.warning("fixDuplicatesOnce: no results for md5 = " +
                                        md5);
                        tx.rollback();
                        session.close();
                        continue;
                    }
                    JobUsageRecord base = (JobUsageRecord) rIter.next(); // First record in set
                    if (!rIter.hasNext()) {
                        Logging.warning("fixDuplicatesOnce: supposed duplicate md5 = " +
                                        md5 + " has only one matching record!");
                        tx.rollback();
                        session.close();
                        continue;
                    }
                    while (rIter.hasNext()) { // Compare subsequent records.
                        UserIdentity baseUserIdentity = base.getUserIdentity();
                        JobUsageRecord compare = (JobUsageRecord) rIter.next();
                        // Need to add comparison and salt if
                        // necessary. Next on the list TODO ...
                        UserIdentity compareUserIdentity = compare.getUserIdentity();
                        Boolean newerIsBetter = false;
                        String replaceReason = null;
                        if (baseUserIdentity == null) {
                            newerIsBetter = true;
                            replaceReason = "original UserIdentity block is null";
                        } else if ((compareUserIdentity.getVOName() != null) &&
                                   (compareUserIdentity.getVOName().length() != 0) &&
                                   (!compareUserIdentity.getVOName().equalsIgnoreCase("Unknown"))) {
                            // Have something with which to replace it.
                            replaceReason = "New VOName is better";
                            if (baseUserIdentity.getVOName() == null) {
                                newerIsBetter = true;
                                replaceReason += " -- original VOName is null";
                            } else if (baseUserIdentity.getVOName().length() == 0) {
                                newerIsBetter = true;
                                replaceReason += " -- original VOName is empty";
                            } else if (baseUserIdentity.getVOName().equalsIgnoreCase("Unknown")) {
                                newerIsBetter = true;
                                replaceReason += " -- original VOName is \"Unknown\")";
                            } else if ((compareUserIdentity.getVOName().startsWith("/")) &&
                                       (!baseUserIdentity.getVOName().startsWith("/"))) {
                                newerIsBetter = true;
                                replaceReason += " -- original VOName is not fully qualified";
                            }
                        }
                        if (!newerIsBetter) { // Still haven't decided
                            if ((compareUserIdentity.getCommonName() != null) &&
                                (!compareUserIdentity.getCommonName().startsWith("Generic"))) {
                                replaceReason = "New CommonName is better";
                                if (baseUserIdentity.getCommonName() == null) {
                                    newerIsBetter = true;
                                    replaceReason += " -- original CommonName is null";
                                } else if (baseUserIdentity.getCommonName().length() == 0) {
                                    newerIsBetter = true;
                                    replaceReason += " -- original CommonName is empty";
                                } else if (baseUserIdentity.getCommonName().startsWith("Generic")) {
                                    newerIsBetter = true;
                                    replaceReason += " -- original CommonName is generic";
                                }
                            } else if ((compareUserIdentity.getKeyInfo() != null) &&
                                       (baseUserIdentity.getKeyInfo() == null)) { 
                                newerIsBetter = true;
                                replaceReason = "Original KeyInfo is null";
                            }
                        }
                        if (newerIsBetter) {
                            Logging.debug("fixDuplicatesOnce: deleting record " +
                                          base.getRecordId() + " in favor of record " +
                                          compare.getRecordId() +
                                          " (" + replaceReason + ").");
                            errorRecorder.saveDuplicate("ChecksumUpgrader",
                                                        "Duplicate",
                                                        compare.getRecordId(),
                                                        base);
                            SummaryUpdater.removeFromSummary(base.getRecordId(), session);
                            session.delete(base); // Supersedes first record.
                            base = compare; // Use this for future comparisons.
                        } else { // This one is not better: delete it
                            Logging.debug("fixDuplicatesOnce: deleting record " +
                                          compare.getRecordId() + " as not better than earlier record " +
                                          base.getRecordId());
                            errorRecorder.saveDuplicate("ChecksumUpgrader",
                                                        "Duplicate",
                                                        base.getRecordId(),
                                                        compare);
                            SummaryUpdater.removeFromSummary(compare.getRecordId(), session);
                            session.delete(compare);
                        }
                            
                    }
                    session.flush();
                    tx.commit();
                    session.close();
                }
                catch (Exception e) {
                    Logging.warning("fixDuplicatesOnce: caught exception " +
                                    "resolving duplicates with checksum " + md5 + " with " +
                                    net.sf.gratia.services.DatabaseMaintenance.UseJobUsageSiteName(), e);
                    tx.rollback();
                    session.close();
                
                }
                ++nDupsFixed;
            }
            if (checksumsReadThisLoop == 0) continueLooping = false; // Done
        }
        Logging.fine("fixDuplicatesOnce: this cycle resolved " +
                     nDupsFixed + " duplicated checksums");
        return nDupsFixed;
    }

    private int getBatchCommitLimit() {
        int session_records_limit = 5000;
        org.hibernate.cfg.Configuration hC = HibernateWrapper.getHibernateConfiguration();
        String p = hC.getProperty("hibernate.jdbc.batch_size");
        Logging.debug("Got property value: " + p);
        int batch_commit_size = Integer.decode(p).intValue();
        if (batch_commit_size == 0) {
            batch_commit_size = 1; // Default if not set.
        } else if (batch_commit_size > session_records_limit) {
            batch_commit_size = session_records_limit;
        }
        return batch_commit_size;
    }

    private void batchUpdateChecksums() {
        // Need to be very careful to keep session integrity here.
        Logging.debug("batchUpdateChecksums: start.");
        long last_processed_dbid = 0;
        int result = 0; // Successful unless an exception makes it all the way out here.
        try {
            int batch_commit_size = getBatchCommitLimit();
            long old_last_processed_dbid;
            int loop_counter = 0;
            int loop_mod = 1000 / batch_commit_size;
            do {
                ++loop_counter;
                old_last_processed_dbid = last_processed_dbid;
                Logging.debug("batchUpdateChecksums: updating " +
                              "JobUsageRecords from " +
                              last_processed_dbid +
                              ", batch size " + batch_commit_size);
                last_processed_dbid = updateJobUsageRecords(last_processed_dbid,
                                                            batch_commit_size);
            } while (last_processed_dbid > old_last_processed_dbid);
        }
        catch (Exception e) {
            Logging.debug("batchUpdateChecksums: caught exception in outer loop: ", e);
        }
        return;
    }

    private long updateJobUsageRecords(long last_dbid) throws Exception {
        return updateJobUsageRecords(last_dbid, 1);
    }

    private long updateJobUsageRecords(long last_dbid, int nRecords) throws Exception {
        Logging.debug("UpdateJobUsageRecords(" + last_dbid + ", " + nRecords + ")");
        if (nRecords == 0) {
            Logging.debug("UpdateJobUsageRecords: nothing to do");
            return last_dbid; // NOP.
        }
        int records_processed = 0;
//         runGC();
//         Logging.debug("UpdateJobUsageRecords: memory report 1 -- " + usedMemory());
        Logging.debug("UpdateJobUsageRecords: get Session");
        Session session = HibernateWrapper.getSession();
        session.setCacheMode(CacheMode.IGNORE);
        session.setFlushMode(FlushMode.COMMIT);
        Logging.debug("UpdateJobUsageRecords: Get query for dbid > " +
                      last_dbid);
        Query q =
            session.createQuery("select record from JobUsageRecord " + 
                                "record where record.RecordId > " +
                                last_dbid +
                                " and record.md5 is null")
            .setCacheMode(CacheMode.IGNORE)
            .setMaxResults(nRecords);
        ScrollableResults records = q.scroll(ScrollMode.FORWARD_ONLY);
        try {
            Logging.debug("UpdateJobUsageRecords: new transaction for update");
            session.beginTransaction();
        }
        catch (Exception e) {
            Logging.warning("UpdateJobUsageRecords: Exception opening new transaction: ", e);
            // Major problem -- finish
            session.close();
            throw e;
        }
        long current_dbid = 0;
        while ((records_processed < nRecords) && records.next()) {
            ++records_processed;
            Logging.debug("UpdateJobUsageRecords: processing record " +
                          records_processed + " of " + nRecords);
            JobUsageRecord record = (JobUsageRecord) records.get(0);
            current_dbid = record.getRecordId();
            Logging.debug("UpdateJobUsageRecords: read record " + current_dbid);
            try {
                String new_md5 = record.computemd5(DatabaseMaintenance.UseJobUsageSiteName());
                // Set the new checksum.
                Logging.debug("UpdateJobUsageRecords: writing " +
                              "new checksum: " + new_md5);
                record.setmd5(new_md5);
                session.update(record);
            }
            catch (Exception e) {
                Logging.warning("UpdateJobUsageRecords: error computing " + 
                                "and updating md5 checksum for record " +
                                current_dbid + ": ", e);
                session.getTransaction().rollback();
                session.close();
                throw e;
            }
        }
        if (records_processed > 0) {
            try {
                Logging.debug("UpdateJobUsageRecords: flush and commit records " +
                              last_dbid + " through " + current_dbid);
                session.flush();
                session.getTransaction().commit();
                session.close();
                Logging.debug("UpdateJobUsageRecords: records " +
                              last_dbid + " through " + current_dbid + " committed");
                last_dbid = current_dbid;
            }
            catch (Exception e) {
                Logging.debug("UpdateJobUsageRecords: caught exception " +
                              "while committing session: ", e);
                Transaction tx = session.getTransaction();
                if ((tx != null) && tx.isActive()) {
                    tx.rollback();
                }
                if (session.isOpen()) session.close();
                throw e;
            }
        } else { // Clean up from whatever stage we've reached
            Transaction tx = session.getTransaction();
            if ((tx != null) && tx.isActive()) {
                tx.rollback();
            }
            if (session.isOpen()) session.close();
        }
//         runGC();
//         Logging.debug("UpdateJobUsageRecords: memory report 2 -- " + usedMemory());
        return last_dbid;
    }

    private static void runGC () throws Exception
    {
        // It helps to call Runtime.gc()
        // using several method calls:
        for (int r = 0; r < 4; ++ r) _runGC ();
    }

    private static void _runGC () throws Exception
    {
        long usedMem1 = usedMemory (), usedMem2 = Long.MAX_VALUE;
        for (int i = 0; (usedMem1 < usedMem2) && (i < 500); ++ i)
        {
            s_runtime.runFinalization ();
            s_runtime.gc ();
            Thread.currentThread ().yield ();
            
            usedMem2 = usedMem1;
            usedMem1 = usedMemory ();
        }
    }

    private static long usedMemory ()
    {
        return s_runtime.totalMemory () - s_runtime.freeMemory ();
    }
    
    private static final Runtime s_runtime = Runtime.getRuntime ();

    private void DropIndexByColumn(String table, String column) throws Exception {
        Statement statement;
        ResultSet resultSet;

        Session session = HibernateWrapper.getSession();
        Transaction tx = session.beginTransaction();
        Connection connection = session.connection();
        
        String check = "select index_name from information_schema.statistics where " +
            "table_schema = database() and table_name = '" + table
            + "' and column_name = '"
            + column + "'";
        try {
            
            Logging.debug("Executing: " + check);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(check);
            while (resultSet.next()) {
                String index_name  = resultSet.getString(1);
                String cmd = "alter table " + table + " drop index " + index_name;
                // Index still there
                try {
                    Logging.debug("Executing: " + cmd);
                    statement = connection.createStatement();
                    statement.executeUpdate(cmd);
                } catch (Exception e) {
                    Logging.debug("Command: Error: " + cmd + " : " + e);
                    throw e;
                }
                Logging.debug("Command: OK: " + cmd);
            }
            resultSet.close();
            statement.close();
        } catch (Exception e) {
            Logging.debug("Command: Error: " + check + " : " + e);
            throw e;
        }
        
    }

}

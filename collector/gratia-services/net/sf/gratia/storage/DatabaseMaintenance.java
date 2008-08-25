package net.sf.gratia.storage;

import net.sf.gratia.util.XP;
import net.sf.gratia.util.Execute;
import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Logging;
import net.sf.gratia.util.LogLevel;
import net.sf.gratia.services.HibernateWrapper;
import net.sf.gratia.storage.JobUsageRecord;
import net.sf.gratia.storage.UserIdentity;

import java.io.File;
import java.lang.Long;
import java.lang.System;
import java.math.BigInteger;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.*;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.JDBCException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.*;

public class DatabaseMaintenance {
    static final String dq = "\"";
    static final String comma = ",";
    static final int gratiaDatabaseVersion = 47;
    static final int latestDBVersionRequiringStoredProcedureLoad = gratiaDatabaseVersion;
    static final int latestDBVersionRequiringSummaryViewLoad = 37;
    static final int latestDBVersionRequiringSummaryTriggerLoad = 47;
    static final int latestDBVersionRequiringTableStatisticsRefresh = 38;

    static boolean dbUseJobUsageSiteName = false;

    java.sql.Connection connection;
    int liveVersion = 0;
    XP xp = new XP();
    boolean isInnoDB = false;

    public DatabaseMaintenance(Properties p) {
        
        String driver = p.getProperty("service.mysql.driver");
        String url = p.getProperty("service.mysql.url");
        String user = p.getProperty("service.mysql.user");
        String password = p.getProperty("service.mysql.password");

        try
            {
                Class.forName(driver);
                connection = (java.sql.Connection)DriverManager.getConnection(url, user, password);
            }
        catch (Exception e)
            {
                Logging.warning("DatabaseMaintenance: Error During connection: " + e);
                Logging.warning(xp.parseException(e));
                return;
            }

        RationalizePropsTable();

        ReadLiveVersion();

    }
    
    public boolean IsDbNewer()
    {
        return (liveVersion > gratiaDatabaseVersion);
    }
    
    static public boolean UseJobUsageSiteName() {
        return dbUseJobUsageSiteName; 
    }

    public boolean InitialCleanup() {
        // Do check and cleanup that must be done before Hibernate is started.
        
        if (liveVersion < 4) Execute("Drop view if exists Role;");
        if (liveVersion < 5) Execute("Drop view if exists Site;");
        if (liveVersion < 6) Execute("Drop view if exists Probe;");

        String hibernate_cfg = System.getProperty("catalina.home");
        hibernate_cfg = xp.replaceAll(hibernate_cfg, "\\", "" + File.separatorChar);
        hibernate_cfg = hibernate_cfg + File.separatorChar + "gratia" + File.separatorChar + "hibernate.cfg.xml";

        String grep_cmd[] = {"grep", "-e", "org\\.hibernate\\.dialect\\.MySQLInnoDBDialect", hibernate_cfg};
        
        int result = Execute.execute(grep_cmd);
        if (result == 0) {
            isInnoDB = true;
            return CheckAndConvertTables();
        }
        return true;
    }
    
    public void AddIndex(String table, Boolean unique, String name,
                         String content) throws Exception {

        AddIndex(table,unique,name,content,false);
    }

    public void AddIndex(String table, Boolean unique, String name,
                         String content, Boolean avoidDuplicateIndex) throws Exception {
        Statement statement;
        ResultSet resultSet;

        String check = "show index from " + table + " where Key_name = '"
            + name + "'";
        String checkcontent = "show index from " + table + " where Column_name = '"
            + content + "'";

        String cmd = "alter table " + table + " add ";
        if (unique) {
            cmd = cmd + "unique ";
        }
        cmd = cmd + "index " + name + "(" + content + ")";
        try {
            Logging.log("Executing: " + check);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(check);
            if (!resultSet.next()) { 
               resultSet.close();
               statement.close();
               Boolean exist = false;
               if (avoidDuplicateIndex) {
                  Logging.log("Executing: " + checkcontent);
                  statement = connection.createStatement();
                  resultSet = statement.executeQuery(checkcontent);

                  exist = resultSet.next();

                  resultSet.close();
                  statement.close();
               }
               // No index yet
               if (!exist) {
                  Logging.log("Executing: " + cmd);
                  statement = connection.createStatement();
                  statement.executeUpdate(cmd);
                  Logging.log("Command: OK: " + cmd);
               }
            }
            statement.close();

        } catch (Exception e) {
            Logging.log("Command: Error: " + cmd + " : " + e);
            throw e;
        }
    }

    public void DropIndex(String table, String name) throws Exception {
        Statement statement;
        ResultSet resultSet;

        String check = "show index from " + table + " where Key_name = '"
            + name + "'";
        String cmd = "alter table " + table + " drop index " + name;
        try {
            Logging.log("Executing: " + check);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(check);
            if (resultSet.next()) {
                // Index still there
                Logging.log("Executing: " + cmd);
                statement = connection.createStatement();
                statement.executeUpdate(cmd);
                Logging.log("Command: OK: " + cmd);
            }
            resultSet.close();
            statement.close();
        } catch (Exception e) {
            Logging.log("Command: Error: " + cmd + " : " + e);
            throw e;
        }
    }

    public void CheckIndices() throws Exception {
        Logging.info("DatabaseMaintenance: checking indexes on tables.");

        AddIndex("Site", true, "index02", "SiteName");
        AddIndex("Probe", true, "index02", "probename");

        Boolean md5v2_checksum_is_operational = false;

        try {
            md5v2_checksum_is_operational = checkMd5v2Unique();
        }
        catch (Exception e) {
            // Ignore
        }

        //
        // the following were added to get rid of unused indexes
        //
        DropIndex("JobUsageRecord", "index04");
        DropIndex("JobUsageRecord", "index06");
        DropIndex("JobUsageRecord", "index07");
        DropIndex("JobUsageRecord", "index09");
        DropIndex("JobUsageRecord", "index10");
        if (md5v2_checksum_is_operational) {
            // No longer necessary since we're using md5v2 instead
            DropIndex("JobUsageRecord_Meta", "index12");
            DropIndex("JobUsageRecord_Meta", "md5"); // Possible
        }
        //
        // original index structure
        //
        AddIndex("JobUsageRecord", false, "index02", "EndTime");
        AddIndex("JobUsageRecord_Meta", false, "index03", "ProbeName");
        // AddIndex("JobUsageRecord",false,"index04","HostDescription");
        AddIndex("JobUsageRecord", false, "index05", "StartTime");
        // AddIndex("JobUsageRecord",false,"index06","GlobalJobid");
        // AddIndex("JobUsageRecord",false,"index07","LocalJobid");
        AddIndex("JobUsageRecord", false, "index08", "Host(255)");
        if (!md5v2_checksum_is_operational) { // Still relying on old index, md5v2 not ready yet.
            AddIndex("JobUsageRecord_Meta", true, "index12", "md5", true);
        }
        AddIndex("JobUsageRecord_Meta", false, "index13", "ServerDate");

        AddIndex("MetricRecord_Meta", true, "index12", "md5", true);
        AddIndex("ProbeDetails_Meta", true, "index12", "md5", true);

        // 
        // Index on DupRecord
        //
        AddIndex("DupRecord",false,"index02","eventdate");
        if ((liveVersion == 0) || (liveVersion >= 24)) { // Only if we have the correct table format.
            AddIndex("DupRecord",false,"index03","RecordType");
            AddIndex("DupRecord",false,"index04","source");
            AddIndex("DupRecord",false,"index05","error");
        }

        //
        // new indexes for authentication
        //
        AddIndex("JobUsageRecord", false, "index14", "VOName");
        AddIndex("JobUsageRecord", false, "index15", "CommonName");

        //
        AddIndex("Security", true, "index02", "alias");

        //
        // New index for ResourceType
        //
        AddIndex("JobUsageRecord", false, "index16", "ResourceType");
        
        //
        // Indexes for VONameCorrection
        //
        AddIndex("VONameCorrection",false,"index01","VOName, ReportableVOName");

        //
        // Index for new md5 column
        //
        // NOTE: this routine creates a NON-UNIQUE index, even though it
        // will eventually become unique. The scenarios are as follows:
        //
        // 1. Table does not exist, or table exists but column does not:
        // Hibernate will create column and unique index. Index will be
        // removed and replaced with a non-unique one called something
        // different; and then upgrade will take place.
        //
        // 2. Table and column exists, index does not: line below will
        // create index.
        //
        // 3. Table, column and index already exist: NOP.
        //
        // This is of course exactly what we want: if there is the
        // chance of data already existing without the new checksum the
        // upgrade thread will kick in.

        // Remove hibernate's index, because it could be premature: replace it
        // with our own. This won't remove the, "final" one because we
        // called it something different. We'll need to calculate the
        // new checksums en bloc and *then* make the index unique.
        DropIndex("JobUsageRecord_Meta", "md5v2");
        if (liveVersion >= 31 ||
            (gratiaDatabaseVersion >= 31 && liveVersion == 0)) {
            // Note that we're adding a non-unique one, here.
            AddIndex("JobUsageRecord_Meta", false, "index17", "md5v2", true);
        }

        // Indexes for MasterSummaryData
        AddIndex("MasterSummaryData", false, "index01", "EndTime");
        AddIndex("MasterSummaryData", false, "index02", "VOcorrid");
        AddIndex("MasterSummaryData", false, "index03", "ProbeName");
        AddIndex("MasterSummaryData", false, "index04", "CommonName");
        AddIndex("MasterSummaryData", false, "index05", "ResourceType");
        AddIndex("MasterSummaryData", false, "index06", "HostDescription");
        AddIndex("MasterSummaryData", false, "index07", "ApplicationExitCode");
        AddIndex("MasterSummaryData", true, "index08",
                 "EndTime, VOcorrid, ProbeName, " +
                 "CommonName, ResourceType, " +
                 "HostDescription, ApplicationExitCode");

        // Indexes for MasterTransferSummary
        AddIndex("MasterTransferSummary", false, "index01", "StartTime");
        AddIndex("MasterTransferSummary", false, "index02", "VOcorrid");
        AddIndex("MasterTransferSummary", false, "index03", "ProbeName");
        AddIndex("MasterTransferSummary", false, "index04", "CommonName");
        AddIndex("MasterTransferSummary", false, "index05", "Protocol");
        AddIndex("MasterTransferSummary", false, "index06", "RemoteSite");
        AddIndex("MasterTransferSummary", false, "index07", "Status");
        AddIndex("MasterTransferSummary", false, "index08", "IsNew");
        AddIndex("MasterTransferSummary", false, "index09", "StorageUnit");
        AddIndex("MasterTransferSummary", true, "index10",
                 "StartTime, VOcorrid, ProbeName, " +
                 "CommonName, Protocol, RemoteSite, Status, IsNew, StorageUnit");

        if (readIntegerDBProperty("gratia.database.wantNodeSummary") > 0) {
            // Indexes for NodeSummary
            AddIndex("NodeSummary", false, "index01", "EndTime");
            AddIndex("NodeSummary", false, "index02", "Node");
            AddIndex("NodeSummary", false, "index03", "ProbeName");
            AddIndex("NodeSummary", false, "index04", "ResourceType");
            AddIndex("NodeSummary", true, "index05", "EndTime, Node, ProbeName, ResourceType");
        }
        if (readIntegerDBProperty("gratia.database.wantSummaryTrigger") == 0) {
            // OSG Daily.
            AddIndex("JobUsageRecord_Meta", false, "index18", "ReportedSiteName");
        }

        // Indexes for Trace table to facilitate housekeeping.
        AddIndex("trace", false, "index01", "eventtime");
        AddIndex("trace", false, "index02", "pname");

        // SystemProplist management (safety)
        AddIndex("SystemProplist", true, "index01", "car");

        Logging.info("DatabaseMaintenance: table index checking complete.");

    }

    private int CallPostInstall(String action) {
        Logging.fine("DatabaseMaintenance: calling post-install script for action \"" + action + "\"");
        String post_install = System.getProperty("catalina.home");
        post_install = xp.replaceAll(post_install, "\\", "" + File.separatorChar);
        post_install = post_install + File.separatorChar + "gratia" + File.separatorChar + "post-install.sh";
        String chmod_cmd[] = {"chmod", "700", post_install};
        Execute.execute(chmod_cmd); // Mark executable just in case.
        String post_cmd[] = {post_install, action};
        int result = Execute.execute(post_cmd);
        if (result == 0) {
            return result;
        } else {
            return -1;
        }
    }

    public int Execute(String cmd) {
        Statement statement;
        int result = 0;

        try {
            Logging.log("Executing: " + cmd);
            statement = connection.createStatement();
            result = statement.executeUpdate(cmd);
            Logging.log("Command: OK: " + cmd);
        } catch (Exception e) {
            Logging.warning("Command: Error: " + cmd + " : " + e);
            result = -1;
        }
        return result;
    }

    private int getCount(String cmd) {
        Statement statement;
        ResultSet resultSet;

        try {
            Logging.log("Executing: " + cmd);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(cmd);
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            Logging.log("Command: OK: " + cmd);
        } catch (Exception e) {
            Logging.warning("Command: Error: " + cmd + " : " + e);
        }
        return 0;
    }

    public void SiteDefaults() {
        Statement statement;
        ResultSet resultSet;

        String check = "select count(*) from Site where SiteName = 'Unknown'";
        String cmd = "insert into Site(SiteName) values(" + dq
            + "Unknown" + dq + ")";

        try {
            Logging.log("Executing: " + check);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(check);
            Boolean needupdate = true;
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                needupdate = (count != 1);
            }
            if (needupdate) {
                Logging.log("Executing: " + cmd);
                statement.executeUpdate(cmd);
            }
            Logging.log("Command: OK: " + cmd);
        } catch (Exception e) {
            Logging.warning("Command: Error: " + cmd + " : " + e);
        }
    }
    
    private String GetJobUsageRecordColumnsForReportView() {
        Statement statement;
        ResultSet resultSet;

        String cmd = "show columns from JobUsageRecord";
        String result = "";
        try {
            Logging.log("Executing: " + cmd);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(cmd);
            while (resultSet.next()) {
                String column = resultSet.getString(1);
                if (column.endsWith("Description") &&
                    ! ((column.startsWith("Host")) || (column.startsWith("Status")))) {
                    continue; // Skip these columns
                } else if (column.equals("SiteName")) {
                    column = "Site.SiteName";
                } else if (column.equals("Status")) {
                    column = "JobUsageRecord.Status";
                } else if (column.equals("dbid")) {
                    column = "JobUsageRecord_Meta.dbid";
                } else if (column.equals("VOName") || column.equals("ReportableVOName") || column.equals("VOcorrid")) {
                    continue; // Skip these columns
                }
                if (result.length()>0) result = result + ",";
                result = result + column;
            }
        } catch (Exception e) {
            Logging.warning("Command: Error: " + cmd + " : " + e);
        }
        return result;
    }
    

    public void CPUMetricDefaults() {
        Execute("delete from CPUMetricTypes");
        Execute("insert into CPUMetricTypes(CPUMetricType) values(" + dq
                + "wallclock" + dq + ")");
        Execute("insert into CPUMetricTypes(CPUMetricType) values(" + dq
                + "process" + dq + ")");
    }
    
    public void RoleDefaults() {
        if (getCount("select count(*) from Role where role='GratiaGlobalAdmin'")==0) {
            Execute("insert into Role(role,subtitle,whereclause) values('GratiaGlobalAdmin','GratiaGlobalAdmin','')");
        }
        if (getCount("select count(*) from Role where role='GratiaUser'")==0) {
            Execute("insert into Role(role,subtitle,whereclause) values('GratiaUser','GratiaUser','Everything')");
        }
    }

    public void AddDefaults() {
        SiteDefaults();
        CPUMetricDefaults();
        RoleDefaults();
        PropertyDefaults();
    }

    private void PropertyDefaults() {
        //
        // place holder to initialize SystemProplist
        //
        Properties p = net.sf.gratia.util.Configuration.getProperties();

        UpdateDbProperty("use.report.authentication",
                         p.getProperty("use.report.authentication", "0"));
        UpdateDbProperty("gratia.database.wantSummaryTrigger",
                         p.getProperty("gratia.database.wantSummaryTrigger", "1"));
        UpdateDbProperty("gratia.database.wantStoredProcedures",
                         p.getProperty("gratia.database.wantStoredProcedures", "1"));
        UpdateDbProperty("gratia.database.useJobUsageSiteName",
                         p.getProperty("gratia.database.useJobUsageSiteName", "0"));
        UpdateDbProperty("gratia.database.wantNodeSummary",
                         p.getProperty("gratia.database.wantNodeSummary", "0"));
        UpdateDbProperty("gratia.database.disableChecksumUpgrade",
                         p.getProperty("gratia.database.disableChecksumUpgrade", "0"));

        dbUseJobUsageSiteName = 0 != readIntegerDBProperty("gratia.database.useJobUsageSiteName");
    }
    
    public void AddViews() {
        Execute("DROP VIEW IF EXISTS CETable");
        Execute("CREATE VIEW CETable AS select Site.SiteId AS facility_id,Site.SiteName AS facility_name from Site");
        Execute("DROP VIEW IF EXISTS CEProbes");
        Execute("CREATE VIEW CEProbes AS select probeid,siteid as facility_id," +
                "probename,active,currenttime,CurrentTimeDescription,reporthh," +
                "reportmm,status," + FindRecordsColumn() + " as jobs from Probe");
        Execute("DROP VIEW IF EXISTS JobUsageRecord_Report");
        Execute("CREATE VIEW JobUsageRecord_Report as select " + GetJobUsageRecordColumnsForReportView() +
                ", JobUsageRecord_Meta.ProbeName, JobUsageRecord_Meta.ReportedSiteName, Site.SiteName, VO.VOName" +
                ", JobUsageRecord_Meta.ServerDate" +
                " from JobUsageRecord_Meta, Site, Probe, JobUsageRecord, VO, VONameCorrection" +
                " where " +
                " JobUsageRecord_Meta.probeid = Probe.probeid and Probe.siteid = Site.siteid" +
                " and JobUsageRecord_Meta.dbid = JobUsageRecord.dbid" +
                " and binary JobUsageRecord.VOName = binary VONameCorrection.VOName" + 
                " and ((binary JobUsageRecord.ReportableVOName = binary VONameCorrection.ReportableVOName) or" + 
                " ((JobUsageRecord.ReportableVOName is null) and (VONameCorrection.ReportableVOName is null)))" +
                " and VONameCorrection.void = VO.void" + 
                " and JobUsageRecord.VOName = VONameCorrection.VOName");
    }

    public int readIntegerDBProperty(String property) {
        Statement statement;
        ResultSet resultSet;

        String check = "select cdr from SystemProplist where car = " + dq
            + property + dq;

        int result = -1;

        try {
            Logging.log("Executing: " + check);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(check);
            if (resultSet.next()) {
                String vers = resultSet.getString(1);
                result = Integer.valueOf(vers).intValue();
            }           
            Logging.log("Command: OK: " + check);
        } catch (Exception e) {
            Logging.warning("Command: Error: " + check + " : " + e);
        }
        return result;
    }
    
    public void ReadLiveVersion() {

        Statement statement;
        ResultSet resultSet;

        if ( 1 != getCount("select count(*) from information_schema.tables where " +
                           "table_schema = Database() and table_name = " +
                           dq + "SystemProplist" + dq) ) {
            return;
        } // No SystemProplist table yet

        String check = "select cdr from SystemProplist where car = " + dq
            + "gratia.database.version" + dq;

        try {
            Logging.log("Executing: " + check);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(check);
            if (resultSet.next()) {
                String vers = resultSet.getString(1);
                if (vers.equals("1.0.0")) {
                    liveVersion = 1;
                } else {
                    try {
                        liveVersion = Integer.valueOf(vers).intValue();
                    } catch (Exception e) {
                        liveVersion = 1;
                    }
                }
            }
            Logging.log("Command: OK: " + check);
        } catch (Exception e) {
            Logging.warning("Command: Error: " + check + " : " + e);
        }        
    }

    private void UpdateDbProperty(String property, int value) {
        UpdateDbProperty(property, "" + value);
    }

    private void UpdateDbProperty(String property, String value,
                                  Boolean ignoreNull) {
        if (value == null) return;
        UpdateDbProperty(property, value);
    }
    
    private void UpdateDbProperty(String property, String value) {
        int nRows = getCount("select count(*) from SystemProplist where car = " +
                             dq + property + dq);
        if (nRows > 0) {
            Execute("update SystemProplist set cdr = " + dq + value + dq +
                    " where car = " + dq + property + dq);
        } else {
            Execute("insert into SystemProplist(car,cdr) values(" +
                    dq + property + dq + ", " +
                    dq + value + dq + ")");
        }
    }

    private void UpdateDbVersion(int newVersion) {
        UpdateDbProperty("gratia.database.version", newVersion);
        liveVersion = newVersion;        
    }

    private boolean checkAndUpgradeDbAuxiliaryItems() {
        if (readIntegerDBProperty("gratia.database.version") != gratiaDatabaseVersion) {
            Logging.log(LogLevel.SEVERE,
                        "INTERNAL ERROR: DatabaseMainentance::checkAndUpgradeDbAuxiliaryItems" +
                        " called with inconsistent DB version");
            return false;
        }

        // First check summary tables
        int ver = readIntegerDBProperty("gratia.database.summaryViewVersion");
        if (ver < latestDBVersionRequiringSummaryViewLoad) {
            int result = CallPostInstall("summary-view");
            if (result > -1) {
                UpdateDbProperty("gratia.database.summaryViewVersion", gratiaDatabaseVersion);
                Logging.log("Summary view updated successfully");
            } else {
                Logging.warning("FAIL: summary view NOT updated");
                return false;
            }
        }

        // Next check trigger
        int wanted = readIntegerDBProperty("gratia.database.wantSummaryTrigger");
        Logging.log("gratia.database.wantSummaryTrigger = " + wanted);
        if (1 == wanted) {
            ver = readIntegerDBProperty("gratia.database.summaryTriggerVersion");
            Logging.debug("Read gratia.database.summaryTriggerVersion " + ver);
            if (ver < latestDBVersionRequiringSummaryTriggerLoad) {
                Logging.debug("Calling post install for trigger load");
                int result = CallPostInstall("trigger");
                if (result > -1) {
                    UpdateDbProperty("gratia.database.summaryTriggerVersion", gratiaDatabaseVersion);
                    Logging.log("Summary trigger updated successfully");
                } else {
                    Logging.warning("FAIL: summary trigger NOT updated");
                    return false;
                }
            }
        }

        // Check stored procedures
        wanted = readIntegerDBProperty("gratia.database.wantStoredProcedures");
        Logging.log("gratia.database.wantStoredProcedures = " + wanted);
        if (1 == wanted) {
            ver = readIntegerDBProperty("gratia.database.storedProcedureVersion");
            if (ver < latestDBVersionRequiringStoredProcedureLoad) {
                int result = CallPostInstall("stored");
                if (result > -1) {
                    UpdateDbProperty("gratia.database.storedProcedureVersion",
                                     gratiaDatabaseVersion);
                    Logging.log("Stored procedures updated successfully");
                } else {
                    Logging.warning("FAIL: stored procedures NOT updated");
                    return false;
                }
            }
        }

        // Check TableStatistics
        {
            ver = readIntegerDBProperty("gratia.database.TableStatisticsVersion");
            if (ver < latestDBVersionRequiringTableStatisticsRefresh) {
                int result = RefreshTableStatistics();
                if (result > -1) {
                    UpdateDbProperty("gratia.database.TableStatisticsVersion",
                                     gratiaDatabaseVersion);
                    Logging.log("Table statistics updated successfully");
                } else {
                    Logging.warning("FAIL: table statistics NOT updated");
                    return false;
                }
            }
        }                
        return true;
    }

    public boolean Upgrade() {
        PropertyDefaults(); // Make sure we have the values in SystemProplist
        int oldvers = liveVersion;

        if (oldvers == 0) {
            Statement statement;
            ResultSet resultSet;
            
            String check = "select count(*),min(PropId) from SystemProplist where car = " + dq
                + "gratia.database.version" + dq;

            try {
                Logging.log("Executing: " + check);
                statement = connection.createStatement();
                resultSet = statement.executeQuery(check);
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    if (count > 0) { // Rare case where version is 0 but this entry exists
                        Execute("update SystemProplist set cdr = " + gratiaDatabaseVersion +
                                " where car = " + dq + "gratia.database.version" + dq);
                    } else { // Normal case: needs new entry
                        Execute("insert into SystemProplist(car,cdr) values(" + dq
                                + "gratia.database.version" + dq + comma + dq
                                + gratiaDatabaseVersion + dq + ")");
                    }
                }
                Logging.log("Command: OK: " + check);
            } catch (Exception e) {
                Logging.warning("Command: Error: " + check + " : " + e);
            }    

            Logging.info("Gratia database now at version "
                         + gratiaDatabaseVersion);

            return checkAndUpgradeDbAuxiliaryItems();
            
        } else {
            // Do the necessary upgrades if any

            Logging.info("Gratia database at version " + oldvers);

            
            int current = oldvers;

            if (current == 1) {
                Logging.fine("Gratia database upgraded from " + current
                            + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }
            if (current == 2) {
                // Upgrade to version 3;
                if (oldvers < 2) {
                    // Then MetricRecord_Meta never contained the Xml fields,
                    // nothing to do.
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    int result = Execute("insert into MetricRecord_Xml(dbid,RawXml,ExtraXml) select dbid,RawXml,ExtraXml from MetricRecord_Meta");
                    if (result > -1) {
                        result = Execute("alter table MetricRecord_Meta drop column RawXml, drop column ExtraXml");
                    }
                    if (result > -1) {
                        Logging.fine("Gratia database upgraded from " + current
                                    + " to " + (current + 1));
                        current = current + 1;
                        UpdateDbVersion(current);
                    } else {
                        Logging.warning("Gratia database FAILED to upgrade from "
                                    + current + " to " + (current + 1));
                    }
                }
            }
            if (current == 3) {
                int result = Execute("insert into JobUsageRecord_Xml(dbid,RawXml,ExtraXml) select dbid,RawXml,ExtraXml from JobUsageRecord;");
                if (result > -1) {
                    result = Execute("alter table JobUsageRecord drop column RawXml, drop column ExtraXml;");
                }
                if (result > -1) {
                    // move md5, ServerDate, SiteName, SiteNameDescription,
                    // ProbeName, ProbeNameDescription,
                    // recordId,CreateTime.CreateTimeDescription,RecordKeyInfoId,RecordKeyInfoContent
                    result = Execute("insert into JobUsageRecord_Meta(dbid,md5, ServerDate, SiteName, SiteNameDescription, ProbeName, ProbeNameDescription,recordId,CreateTime,CreateTimeDescription,RecordKeyInfoId,RecordKeyInfoContent) select dbid, md5, ServerDate, SiteName, SiteNameDescription, ProbeName, ProbeNameDescription,recordId,CreateTime,CreateTimeDescription,RecordKeyInfoId,RecordKeyInfoContent from JobUsageRecord;");
                }
                if (result > -1) {
                    result = Execute("alter table JobUsageRecord drop column md5, drop column ServerDate, drop column SiteName, drop column SiteNameDescription, drop column ProbeName, drop column ProbeNameDescription, drop column recordId, drop column CreateTime, drop column CreateTimeDescription, drop column RecordKeyInfoId, drop column RecordKeyInfoContent; ");
                }
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current + " to " + (current + 1));
                }
            }
            if (current == 4) {
                int result = Execute("insert into Role(roleid,role,subtitle,whereClause) select roleid,role,subtitle,whereclause from RolesTable");
                if (result > -1) {
                    result = Execute("drop table RolesTable;");
                }
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current + " to " + (current + 1));
                }
            }
            if (current == 5) {
                int result = Execute("insert into Site(siteid,SiteName) select facility_id,facility_name from CETable");
                if (result > -1) {
                    result = Execute("drop table CETable;");
                }
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current + " to " + (current + 1));
                }
            }
            if (current == 6) {
                String records_column = FindRecordsColumn();
                int result = Execute("insert into Probe(probeid,siteid,probename,active,currenttime,CurrentTimeDescription," +
                                     "reporthh,reportmm,status," + records_column + ") select " +
                                     "probeid,facility_id,probename,active,currenttime,CurrentTimeDescription,reporthh,reportmm,status,jobs " +
                                     " from CEProbes");
                if (result > -1) {
                    result = Execute("drop table CEProbes;");
                }
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current + " to " + (current + 1));
                }
            }
            if (current == 7) {
                int result = Execute("insert into VONameCorrection(VOName,ReportableVOName) select distinct binary VOName,binary ReportableVOName from JobUsageRecord");
                if (result > -1) {
                    result = Execute("insert into VO(VOName) select distinct binary VOName from VONameCorrection");
                }
                if (result > -1) {
                    result = Execute("update VONameCorrection,VO set VONameCorrection.VOid = VO.VOid where binary VONameCorrection.VOName = binary VO.VOName");
                }
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current + " to " + (current + 1));
                }                
            }
            if (current == 8 || current == 9) { // Can combine update command into one SQL statement for both versions
                int result = Execute("update (select ProbeName,count(*) as nRecords,max(ServerDate) as ServerDate from JobUsageRecord_Meta group by ProbeName) as sums,Probe set Probe.nRecords = sums.nRecords, Probe.currenttime = sums.ServerDate, Probe.status = 'alive' where Probe.ProbeName = sums.ProbeName;");
                if (result > -1 && current == 8) { // Only necessary for DB version 8
                    if (FindRecordsColumn() == "jobs") {
                        result = Execute("alter table Probe drop column jobs; ");
                    }
                }
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + 10);
                    current = 10;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current + " to " + 10);
                }
            }
            if (current == 10) {
                int result = Execute("ALTER TABLE MetricRecord MODIFY DetailsData TEXT");
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current + " to " + (current + 1));
                } 
            }
            if ((current > 10 ) && (current < 14)) { // Never saw the light of day and superseded.
                int new_version = 14;
                Logging.fine("Gratia database upgraded from " + current + " to " + new_version);
                current = new_version;
                UpdateDbVersion(current);
            }
            if (current == 14) {
                int result = Execute("delete from VONameCorrection");
                if (result > -1) {
                    result = Execute("delete from VO");
                }
                if (result > -1) {
                    result = Execute("alter table VONameCorrection modify column VOName varchar(255) binary");
                }
                if (result > -1) {
                    result = Execute("alter table VONameCorrection modify column ReportableVOName varchar(255) binary");
                }
                if (result > -1) {
                    result = Execute("insert into VONameCorrection(VOName,ReportableVOName) select distinct binary VOName,binary ReportableVOName from JobUsageRecord");
                }
                if (result > -1) {
                    result = Execute("alter table VO modify column VOName varchar(255) binary");
                }
                if (result > -1) {
                    result = Execute("insert into VO(VOName) select distinct binary VOName from VONameCorrection");
                }
                if (result > -1) {
                    result = Execute("update VONameCorrection,VO set VONameCorrection.VOid=VO.VOid where VONameCorrection.VOName = binary VO.VOName");
                }
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current +
                                " to " + (current + 1));
                }
            }
            if (current == 15) {
                int result = Execute("update JobUsageRecord_Meta set ReportedSiteName = SiteName where SiteName is not null");
                if (result > -1) {
                    result = Execute("update JobUsageRecord_Meta set ReportedSiteNameDescription = SiteNameDescription where SiteNameDescription is not null");
                }
                if (result > -1) {
                    result = Execute("alter table JobUsageRecord_Meta drop column SiteName, drop column SiteNameDescription");
                }
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current +
                                " to " + (current + 1));
                }
            }
            if (current == 16) {
                // NOP -- used to manipulate summary tables.
                Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }
            if (current == 17) {
                // Auxiliary DB item upgrades only.
                Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }
            if (current == 18) {
                // Correct problem with VONameCorrection
                int result = 0;
                Statement statement;
                ResultSet resultSet;
                String command =
                    "select min(corrid) as mc,VOName,ReportableVOName " +
                    "from VONameCorrection group by binary VOName, " +
                    "binary ReportableVOName order by mc";
                try {
                    Logging.log("Executing: " + command);
                    statement = connection.createStatement();
                    resultSet = statement.executeQuery(command);
                    while (result > -1 && resultSet.next()) {
                        int minCorrID = resultSet.getInt(1);
                        String VOName = resultSet.getString(2);
                        String ReportableVOName = resultSet.getString(3);
                        if (ReportableVOName == null) {
                            result =
                                Execute("delete from VONameCorrection where binary VOName = binary " +
                                        dq + VOName + dq + " and ReportableVOName is null " +
                                        "and corrid > " + minCorrID);
                        } else {
                            result =
                                Execute("delete from VONameCorrection where binary VOName = binary " +
                                        dq + VOName + dq + " and binary ReportableVOName = binary " +
                                        dq + ReportableVOName + dq + " and corrid > " + minCorrID);
                        }
                    }
                }
                catch (Exception e) {
                     Logging.log("Command: Error: " + command + " : " + e);
                     result = -1;
                }
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current +
                                " to " + (current + 1));
                }
            }
            if (current == 19) {
                // Auxiliary DB item upgrades only.
                Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }
            if (current == 20) {
                int result = Execute("update Probe set nConnections = 0, nDuplicates = 0");
                if (result > -1) {
                    result = Execute("update JobUsageRecord_Meta, Probe set JobUsageRecord_Meta.probeid = Probe.probeid where JobUsageRecord_Meta.probename = Probe.probename");
                }
                if (result > -1) {
                    result = Execute("update MetricRecord_Meta, Probe set MetricRecord_Meta.probeid = Probe.probeid where MetricRecord_Meta.probename = Probe.probename");
                }
                if (result > -1) {
                    result = Execute("update ProbeDetails_Meta, Probe set ProbeDetails_Meta.probeid = Probe.probeid where ProbeDetails_Meta.probename = Probe.probename");
                } 
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current +
                                " to " + (current + 1));
                }
            }
            if (current == 21) {
                // Auxiliary DB item upgrades only.
                Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }
            if (current == 22) {
                // Auxiliary DB item upgrades only.
                Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }
            if (current == 23) {
                int result = Execute("alter table DupRecord modify column RecordType varchar(255) binary");
                if (result > -1) {
                    try {
                        CheckIndices(); // Call again to update the DupRecord indexes.
                    }
                    catch (Exception e) {
                        Logging.warning("Gratia database FAILED to upgrade from " + current +
                                        " to " + (current + 1), e);
                    }
                    int tmp = current;
                    current = current + 1;
                    UpdateDbVersion(current);
                    Logging.fine("Gratia database upgraded from " + tmp + " to " + current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current +
                                " to " + (current + 1));
                }
            }
            if (current == 24 || current == 25 || current == 26 || current == 27 || current == 28) {
                // Auxiliary DB item upgrades only.
                try {
                    CheckIndices(); // Call again to update the DupRecord indexes.
                 }
                catch (Exception e) {
                    Logging.warning("Gratia database FAILED to upgrade from " + current +
                                    " to 29", e);
                }
                Logging.fine("Gratia database upgraded from " + current + " to 29");
                current = 29;
                UpdateDbVersion(current);
            }
            if (current == 29) {
                Logging.debug("DatabaseMaintenance: " +
                              "updating VONameCorrection table to contain null values");
                // Correct the VONameCorrection table -- summary tables need to be recalculated.
                int result = Execute("update VONameCorrection set ReportableVOName = null where ReportableVOName = 'null'");
                if (result > -1) {
                    Logging.debug("DatabaseMaintenance: " +
                                  "dropping existing NEWVONameCorrection table");
                    result = Execute("drop table if exists NEWVONameCorrection");
                }
                if (result > -1) {
                    Logging.debug("DatabaseMaintenance: " +
                                  "create new NEWVONameCorrection table");
                    result = Execute("create table NEWVONameCorrection like VONameCorrection;");
                }
                if (result > -1) {
                    Logging.debug("DatabaseMaintenance: " +
                                  "Filling NEWVONameCoorection table");
                    result = Execute("insert into NEWVONameCorrection(VOName,ReportableVOName) " +
                                     "select distinct VOName,ReportableVOName from VONameCorrection");
                }
                if (result > -1) {
                    Logging.debug("DatabaseMaintenance: " +
                                  "Filling in best VOid values");
                    result = Execute("update NEWVONameCorrection N, VONameCorrection V " +
                                     "set N.VOid = V.VOid " +
                                     "where ((N.VOName = V.VOName) and " +
                                     "(((N.ReportableVOName is null) and (V.ReportableVOName is null)) or " +
                                     "(N.ReportableVOName = V.ReportableVOName)))");
                }
                if (result > -1) {
                    Logging.debug("DatabaseMaintenance: " +
                                  "Removing old backup table");
                    result = Execute("DROP TABLE IF EXISTS BackupVONameCorrection");
                }
                if (result > -1) {
                    Logging.debug("DatabaseMaintenance: " +
                                  "replacing VONameCorrection table with new one");
                    result = Execute("rename table VONameCorrection to BackupVONameCorrection, " +
                                     "NEWVONameCorrection to VONameCorrection");
                }
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current +
                                " to " + (current + 1));
                }         
            }
            if (gratiaDatabaseVersion < 31) { // FQAN checksum upgrade at v31 May not be activated yet.
                // Done, one way or the other.
                return ((current == gratiaDatabaseVersion) && checkAndUpgradeDbAuxiliaryItems());
            }
            if (current == 30) {
                try {
                    CheckIndices(); // Call again to update the md5v2 index
                }
                catch (Exception e) {
                    Logging.warning("Gratia database FAILED to upgrade from " + current +
                                    " to " + (current + 1), e);
                }
                int tmp = current;
                current = current + 1;
                UpdateDbVersion(current);
                Logging.fine("Gratia database upgraded from " + tmp + " to " + current);
            }
            if (current == 31) {
                // Auxiliary DB item upgrades only.
                Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }                
            if (current == 32) {
                // Auxiliary DB item upgrades only (trigger code)
                Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }                
            if (current == 33) {
                // Auxiliary DB item upgrades only (trigger code)
                Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }                
            if (current == 34) {
                // Auxiliary DB item upgrades only (stored procedures and trigger code)
                Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }                
            if (current == 35) {
                // Auxiliary DB item upgrades only (summary tables and trigger code)
                Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }                
            if (current == 36) {
                // Auxiliary DB item upgrades only (summary views)
                Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }                
            if (current == 37) {
                // Auxiliary DB item upgrades only (TableStatistics)
                Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
                UpdateDbVersion(current);
            }                
            if (current == 38) {
                int result = Execute("alter table SystemProplist modify column car varchar(255) not null default ''");
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current +
                                " to " + (current + 1));
                }         
                // Also auxiliary DB item upgrades (trigger and friends)
            }
            if (current == 39 || current == 40) {
                // Auxiliary DB item upgrades only (trigger and friends)
                Logging.fine("Gratia database upgraded from " + current + " to 41");
                current = 41;
                UpdateDbVersion(current);
            }                
            if (current == 41) {
                int result = 0;
                Statement statement;
                ResultSet resultSet;
                String command = "select column_name, column_key, table_name, EXTRA from information_schema.COLUMNS where table_schema = DATABASE() and column_name like '%SummaryID';";
                // May need to add primary key manually to
                // MasterSummaryData and/or NodeSummary if not already
                // done by Hibernate (table already existed).
                try {
                    Logging.log("Executing: " + command);
                    statement = connection.createStatement();
                    resultSet = statement.executeQuery(command);
                    while (result > -1 && resultSet.next()) {
                        String columnName = resultSet.getString(1);
                        String columnKey = resultSet.getString(2);
                        String tableName = resultSet.getString(3);
                        String extra = resultSet.getString(4);
                        if (extra.contains("auto_increment")) continue; // No change required.
                        if (tableName.equals("MasterSummaryData")) {
                            Logging.debug("DatabaseMaintenance: adding auto_increment key to MasterSummaryData.");
                            result = Execute("alter table MasterSummaryData MODIFY COLUMN SummaryID INT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (SummaryID);");
                        } else if (tableName.equals("NodeSummary")) {
                            Logging.debug("DatabaseMaintenance: adding auto_increment key to NodeSummary.");
                            result = Execute("alter table NodeSummary MODIFY COLUMN NodeSummaryID INT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (NodeSummaryID);");
                        }
                    }
                }
                catch (Exception e) {
                    Logging.warning("Command: Error: " + command + " : ", e);
                    result = -1;
                }
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.warning("Gratia database FAILED to upgrade from " + current +
                                " to " + (current + 1));
                }         
                // Also auxiliary DB item upgrades (trigger and friends)
            }
            if (current == 42) {
                int result = 0;
                Session session = null;
                try {
                    session = HibernateWrapper.getSession();
                    Transaction tx = session.beginTransaction();                    
                    Query q =
                        session.createSQLQuery("UPDATE Replication SET " +
                                               "registered = 0 where registered IS NULL");
                    q.executeUpdate();
                    q = session.createSQLQuery("UPDATE Replication SET " +
                                               "running = 0 where running IS NULL"); 
                    q.executeUpdate();
                    q = session.createSQLQuery("UPDATE Replication SET " +
                                               "registered = 0 where registered IS NULL"); 
                    q.executeUpdate();
                    q = session.createSQLQuery("UPDATE Replication SET " +
                                               "security = 0 where security IS NULL"); 
                    q.executeUpdate();
                    q = session.createSQLQuery("UPDATE Replication SET " +
                                               "openconnection = '' where openconnection IS NULL"); 
                    q.executeUpdate();
                    q = session.createSQLQuery("UPDATE Replication SET " +
                                               "secureconnection = '' where secureconnection IS NULL"); 
                    q.executeUpdate();
                    q = session.createSQLQuery("UPDATE Replication SET " +
                                               "frequency = 0 where frequency IS NULL"); 
                    q.executeUpdate();
                    q = session.createSQLQuery("UPDATE Replication SET " +
                                               "dbid = 0 where dbid IS NULL"); 
                    q.executeUpdate();
                    q = session.createSQLQuery("UPDATE Replication SET " +
                                               "rowcount = 0 where rowcount IS NULL"); 
                    q.executeUpdate();
                    q = session.createSQLQuery("UPDATE Replication SET " +
                                               "probename = 'All' where probename IS NULL"); 
                    q.executeUpdate();
                    q = session.createSQLQuery("UPDATE Replication SET " +
                                               "recordtable = 'JobUsageRecord' where recordtable IS NULL"); 
                    q.executeUpdate();
                    q = session.createSQLQuery("ALTER TABLE Replication " +
                                               "MODIFY COLUMN registered INT NOT NULL DEFAULT '0', " +
                                               "MODIFY COLUMN running INT NOT NULL DEFAULT '0', " +
                                               "MODIFY COLUMN security INT NOT NULL DEFAULT '0', " +
                                               "MODIFY COLUMN openconnection VARCHAR(255) NOT NULL DEFAULT '', " +
                                               "MODIFY COLUMN secureconnection VARCHAR(255) NOT NULL DEFAULT '', " +
                                               "MODIFY COLUMN frequency INT NOT NULL DEFAULT '1', " +
                                               "MODIFY COLUMN dbid INT NOT NULL DEFAULT '0', " +
                                               "MODIFY COLUMN rowcount INT NOT NULL DEFAULT '0', " +
                                               "MODIFY COLUMN probename VARCHAR(255) NOT NULL DEFAULT 'All', " +
                                               "MODIFY COLUMN recordtable VARCHAR(255) NOT NULL DEFAULT '';");
                    q.executeUpdate();
                    tx.commit();
                } catch (Exception e) {
                    if ((session != null) && (session.isOpen())) {
                        Transaction tx = session.getTransaction();
                        if (tx != null) tx.rollback();
                        session.close();
                        Logging.debug("Exception detail: ", e);
                        Logging.warning("Gratia database FAILED to upgrade from " + current +
                                        " to " + (current + 1)); 
                    }
                    result = -1;
                }
                if (result > -1) {
                    Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                }         
            }
            if ((current >= 43) && (current <= 46)) {
                // Auxiliary DB item upgrades only (trigger and friends)
                Logging.fine("Gratia database upgraded from " + current + " to 47");
                current = 47;
                UpdateDbVersion(current);
            }                
            return ((current == gratiaDatabaseVersion) && checkAndUpgradeDbAuxiliaryItems());
        }
    }

    private String FindRecordsColumn() {
        Statement statement;
        ResultSet resultSet;

        String cmd = "show columns from Probe";
        try {
            Logging.log("Executing: " + cmd);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(cmd);
            while (resultSet.next()) {
                String column = resultSet.getString(1);
                if (column.equals("jobs") || column.equals("nRecords")) {
                    return column;
                }
            }
        } catch (Exception e) {
            Logging.warning("Command: Error: " + cmd + " : " + e);
        }   
        return "";
    }

    private void RationalizePropsTable() {
        Statement statement;
        ResultSet resultSet;

        if ( 1 != getCount("select count(*) from information_schema.tables where " +
                           "table_schema = Database() and table_name = " +
                           dq + "SystemProplist" + dq) ) {
            return;
        } // No SystemProplist table yet

        String check = "select count(*),min(PropId) from SystemProplist where car = " + dq
            + "gratia.database.version" + dq;

        try {
            Logging.log("Executing: " + check);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(check);
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count > 1) {
                    int propid = resultSet.getInt(2);
                    Execute("delete from SystemProplist where car = "
                            + dq  + "gratia.database.version" + dq + " and PropId > " + propid);
                }
            }
            Logging.log("Command: OK: " + check);
        } catch (Exception e) {
            Logging.warning("Command: Error: " + check + " : " + e);
        }        
    }

    private int RefreshTableStatistics() {
        int result = Execute("DROP TABLE IF EXISTS TableStatistics");
        if (result > -1) {
            String command = "CREATE TABLE TableStatistics(" +
                "RecordType VARCHAR(255) NOT NULL," +
                "nRecords INTEGER DEFAULT 0, Qualifier VARCHAR(255) NOT NULL DEFAULT '', " +
                "UNIQUE KEY index1 (RecordType, Qualifier))";

            if (isInnoDB) {
                command += " ENGINE = 'innodb'";
            }
                             
            result = Execute(command);
            //
            Statement statement;
            ResultSet resultSet;
            command = "select table_name from information_schema.tables " +
                "where table_schema = Database() and table_name like '%_Meta';";
            try {
                statement = connection.prepareStatement(command);
                resultSet = statement.executeQuery(command);
                HashSet tableList = new HashSet<String>();
                while (resultSet.next()) {
                    String table_name = resultSet.getString(1);
                    int end_index = table_name.lastIndexOf("_Meta");
                    String base_table = table_name.substring(0,end_index);
                    tableList.add(base_table);
                }
                resultSet.close();
                statement.close();

                // Put error type information into the TableStatistics record.
                //
                result = Execute("update DupRecord set RecordType = 'JobUsageRecord' where RecordType is null");
                result = Execute("insert into TableStatistics(RecordType,nRecords,Qualifier)" +
                                 " select RecordType, count(*), error from DupRecord group by RecordType, error;");
                // Put count(*) information in for all tables.
                tableList.add("DupRecord");
                for (Iterator x = tableList.iterator(); (result > -1) && x.hasNext();) {
                    String table_name = (String) x.next();
                    result = Execute("insert into TableStatistics(RecordType,nRecords)" +
                                     " select '" + table_name +
                                     "', count(*) from " + table_name);
                    if (result > -1) {
                        result = CallPostInstall("countTrigger" + table_name);
                    }
                }
            }
            catch (Exception e) {
                result = -1;
            }
        }
        return result;
    }

    private boolean CheckAndConvertTables() {
        // Obtain a list of all the tables and check their table type.
        Statement statement;
        ResultSet resultSet;

        String get_convert_list = "select table_name, data_length, index_length\n" +
            "  from information_schema.tables\n" +
            "  where table_schema = Database()\n" +
            "    and table_type = 'BASE TABLE'\n" +
            "    and engine = 'MyISAM'\n" +
            "  order by index_length, data_length";
        try {
            Logging.log("Executing: " + get_convert_list);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(get_convert_list);
            while (resultSet.next()) {
                String tableName = resultSet.getString(1);
                Long dataLength = resultSet.getLong(2);
                Long indexLength = resultSet.getLong(3);
                Logging.fine("Converting table " + tableName +
                            " (data_length = " + prettySize(dataLength) +
                            ", index_length = " + prettySize(indexLength) + ")");
                long startTime = System.currentTimeMillis();
                int result;
                try {
                    result = Execute("alter table `" + tableName + "` engine innodb");
                } catch (Exception e) {
                    result = -1;
                }
                long timeTaken = System.currentTimeMillis() - startTime;
                NumberFormat form = NumberFormat.getInstance();
                if (form instanceof DecimalFormat) {
                    ((DecimalFormat) form).setMinimumIntegerDigits(1);
                    ((DecimalFormat) form).setMinimumFractionDigits(2);
                    ((DecimalFormat) form).setMaximumFractionDigits(2);
                    ((DecimalFormat) form).setDecimalSeparatorAlwaysShown(true);
                }

                String tTString = Long.valueOf(timeTaken / 3600000).toString() + ":" +
                    Long.valueOf((timeTaken % 3600000)/ 60000).toString() + ":" +
                    form.format(Long.valueOf(timeTaken % 60000) / 1000.0);
                if (result > -1) {
                    Logging.fine("Table " + tableName +
                                " converted to INNODB in " + tTString);
                } else {
                    Logging.warning("Table " + tableName +
                                    " FAILED conversion to INNODB after " + tTString);
                    return false;
                }
            }
            resultSet.close();
            statement.close();
            Logging.info("Table conversion to INNODB complete");
        } catch (Exception e) {
            Logging.warning("Command: Error: " + get_convert_list + " : " + e);
            return false;
        }
        return true;
    }
    
    private String prettySize(Long number) {
        return prettySize(number, 1100);
    }

    private String prettySize(Long number, Integer threshold) {
        String suffices[] = {"B", "KiB", "MiB", "GiB", "TiB"};
        int suffix_counter = 0;
        Double size = new Double(number);
        NumberFormat form = NumberFormat.getInstance();
        if (form instanceof DecimalFormat) {
            ((DecimalFormat) form).setMinimumIntegerDigits(1);
            ((DecimalFormat) form).setMaximumFractionDigits(2);
        }
        while ((size > threshold) && (suffix_counter < suffices.length)) {
            size /= 1024;
            ++suffix_counter;
        }
        return form.format(size) + suffices[suffix_counter];
    }

    public Boolean checkMd5v2Unique() throws Exception {
        Logging.debug("DatabaseMaintenance: Checking for unique index on md5v2");

        String checksum_check = "select non_unique from " +
            "information_schema.statistics " +
            "where table_schema = database() " +
            " and table_name = 'JobUsageRecord_Meta'" +
            " and column_name = 'md5v2'" +
            " and index_name != 'md5v2'";
        Session session = HibernateWrapper.getSession();
        Boolean result = false;
        try {
            SQLQuery q = session.createSQLQuery(checksum_check);
            List results_list = q.list();
            if (! results_list.isEmpty()) {
                BigInteger non_unique = (BigInteger) results_list.get(0);
                Logging.debug("checkMd5v2Unique: received answer: " + non_unique);
                if ((non_unique != null) && (non_unique.intValue() == 0)) {
                    Logging.debug("checkMd5v2Unique: found unique index on md5v2 in JobUsageRecord_Meta.");
                    result = true;
                } else {
                    Logging.debug("checkMd5v2Unique: found non-unique index on md5v2 in JobUsageRecord_Meta.");
                }
            } else {
                Logging.debug("checkMd5v2Unique: no index found on column md5v2 in JobUsageRecord_Meta.");
                throw new Exception("No md5v2 index");
            }
            session.close();
        }
        catch (Exception e) {
            Logging.warning("checkMd5v2Unique: attempt to check for index on md5v2 in JobUsageRecord_Meta failed!");
            if (session != null && session.isOpen()) session.close();
            throw e;
        }
        return result;
    }

}

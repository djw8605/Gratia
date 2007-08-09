package net.sf.gratia.storage;

import java.sql.*;

import net.sf.gratia.services.Execute;
import net.sf.gratia.services.XP;
import net.sf.gratia.services.Logging;
import java.util.Properties;
import java.io.File;

public class DatabaseMaintenance {
    static final String dq = "\"";
    static final String comma = ",";
    static final int gratiaDatabaseVersion = 20;
    static final int latestDBVersionRequiringStoredProcedureLoad = gratiaDatabaseVersion;
    static final int latestDBVersionRequiringSummaryTableLoad = 19;
    static final int latestDBVersionRequiringSummaryTriggerLoad = 19;

    java.sql.Connection connection;
    int liveVersion = 0;
    XP xp = new XP();

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
    
    public boolean InitialCleanup() {
        // Do check and cleanup that must be done before Hibernate is started.
        
        if (liveVersion < 4) Execute("Drop view if exists Role;");
        if (liveVersion < 5) Execute("Drop view if exists Site;");
        if (liveVersion < 6) Execute("Drop view if exists Probe;");
        
        return true;
    }
    
    public void AddIndex(String table, Boolean unique, String name,
                         String content) {
        Statement statement;
        ResultSet resultSet;

        String check = "show index from " + table + " where Key_name = '"
            + name + "'";
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
                // No index yet
                Logging.log("Executing: " + cmd);
                statement = connection.createStatement();
                statement.executeUpdate(cmd);
                Logging.log("Command: OK: " + cmd);
            }
        } catch (Exception e) {
            Logging.log("Command: Error: " + cmd + " : " + e);
        }
    }

    public void DropIndex(String table, String name) {
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
        } catch (Exception e) {
            Logging.log("Command: Error: " + cmd + " : " + e);
        }
    }

    public void CheckIndices() {
        AddIndex("Site", true, "index02", "SiteName");
        AddIndex("Probe", true, "index02", "probename");

        //
        // the following were added to get rid of unused indexes
        //
        DropIndex("JobUsageRecord", "index04");
        DropIndex("JobUsageRecord", "index06");
        DropIndex("JobUsageRecord", "index07");
        DropIndex("JobUsageRecord", "index09");
        DropIndex("JobUsageRecord", "index10");
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
        AddIndex("JobUsageRecord_Meta", true, "index12", "md5");
        AddIndex("JobUsageRecord_Meta", false, "index13", "ServerDate");

        // 
        // Index on DupRecord
        //
        AddIndex("DupRecord",false,"index02","eventdate");
        
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
        // Indixes for VONameCorrection
        //
        AddIndex("VONameCorrection",false,"index01","VOName, ReportableVOName");

    }

    private int CallPostInstall(String action) {
        Logging.log("DatabaseMaintenance: calling post-install script for action \"" + action + "\"");
        String home = System.getProperty("catalina.home");
        home = xp.replaceAll(home, "\\", "" + File.separatorChar);
        home = home + File.separatorChar + "gratia" + File.separatorChar + "post-install.sh";
        String chmod_cmd[] = {"chmod", "700", home};
        Execute.execute(chmod_cmd); // Mark executable just in case.
        String post_cmd[] = {home, action};
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
            Logging.log("Command: Error: " + cmd + " : " + e);
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
            Logging.log("Command: Error: " + cmd + " : " + e);
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
            Logging.log("Command: Error: " + cmd + " : " + e);
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
            Logging.log("Command: Error: " + cmd + " : " + e);
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
        Properties p = net.sf.gratia.services.Configuration.getProperties();

        UpdateDbProperty("use.report.authentication", p.getProperty("use.report.authentication"));
        UpdateDbProperty("gratia.database.wantSummaryTable",
                         p.getProperty("gratia.database.wantSummaryTable"));
        UpdateDbProperty("gratia.database.wantSummaryTrigger",
                         p.getProperty("gratia.database.wantSummaryTrigger"));
        UpdateDbProperty("gratia.database.wantStoredProcedures",
                         p.getProperty("gratia.database.wantStoredProcedures"));
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
                " from JobUsageRecord_Meta, Site, Probe, JobUsageRecord, VO, VONameCorrection " +
                " where " +
                " JobUsageRecord_Meta.ProbeName = Probe.probename and Probe.siteid = Site.siteid" +
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
            Logging.log("Command: Error: " + check + " : " + e);
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
            Logging.log("Command: Error: " + check + " : " + e);
        }        
    }

    private void UpdateDbProperty(String property, int value) {
        UpdateDbProperty(property, "" + value);
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
            Logging.log("INTERNAL ERROR: DatabaseMainentance::checkAndUpgradeDbAuxiliaryItems" +
                        " called with inconsistent DB version");
            return false;
        }

        // First check summary tables
        int wanted = readIntegerDBProperty("gratia.database.wantSummaryTable");
        Logging.log("gratia.database.wantSummaryTable = " + wanted);
        if (1 == wanted) {
            int ver = readIntegerDBProperty("gratia.database.summaryTableVersion");
            if (ver < latestDBVersionRequiringSummaryTableLoad) {
                int result = CallPostInstall("summary");
                if (result > -1) {
                    UpdateDbProperty("gratia.database.summaryTableVersion", gratiaDatabaseVersion);
                    Logging.log("Summary tables updated successfully");
                } else {
                    Logging.log("FAIL: summary tables NOT updated");
                    return false;
                }
            }
        }

        // Next check trigger
        wanted = readIntegerDBProperty("gratia.database.wantSummaryTrigger");
        Logging.log("gratia.database.wantSummaryTrigger = " + wanted);
        if (1 == wanted) {
            int ver = readIntegerDBProperty("gratia.database.summaryTriggerVersion");
            if (ver < latestDBVersionRequiringSummaryTableLoad) {
                int result = CallPostInstall("trigger");
                if (result > -1) {
                    UpdateDbProperty("gratia.database.summaryTriggerVersion", gratiaDatabaseVersion);
                    Logging.log("Summary trigger updated successfully");
                } else {
                    Logging.log("FAIL: summary trigger NOT updated");
                    return false;
                }
            }
        }

        // Finally, check stored procedures
        wanted = readIntegerDBProperty("gratia.database.wantStoredProcedures");
        Logging.log("gratia.database.wantStoredProcedures = " + wanted);
        if (1 == wanted) {
            int ver = readIntegerDBProperty("gratia.database.storedProcedureVersion");
            if (ver < latestDBVersionRequiringSummaryTableLoad) {
                int result = CallPostInstall("stored");
                if (result > -1) {
                    UpdateDbProperty("gratia.database.storedProcedureVersion",
                                     gratiaDatabaseVersion);
                    Logging.log("Stored procedures updated successfully");
                } else {
                    Logging.log("FAIL: stored procedures NOT updated");
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
                Logging.log("Command: Error: " + check + " : " + e);
            }    

            Logging.log("Gratia database now at version "
                        + gratiaDatabaseVersion);

            return checkAndUpgradeDbAuxiliaryItems();
            
        } else {
            // Do the necessary upgrades if any

            Logging.log("Gratia database at version " + oldvers);

            
            int current = oldvers;

            if (current == 1) {
                Logging.log("Gratia database upgraded from " + current
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
                        Logging.log("Gratia database upgraded from " + current
                                    + " to " + (current + 1));
                        current = current + 1;
                        UpdateDbVersion(current);
                    } else {
                        Logging.log("Gratia database FAILED to upgrade from "
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
                    Logging.log("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.log("Gratia database FAILED to upgrade from " + current + " to " + (current + 1));
                }
            }
            if (current == 4) {
                int result = Execute("insert into Role(roleid,role,subtitle,whereClause) select roleid,role,subtitle,whereclause from RolesTable");
                if (result > -1) {
                    result = Execute("drop table RolesTable;");
                }
                if (result > -1) {
                    Logging.log("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.log("Gratia database FAILED to upgrade from " + current + " to " + (current + 1));
                }
            }
            if (current == 5) {
                int result = Execute("insert into Site(siteid,SiteName) select facility_id,facility_name from CETable");
                if (result > -1) {
                    result = Execute("drop table CETable;");
                }
                if (result > -1) {
                    Logging.log("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.log("Gratia database FAILED to upgrade from " + current + " to " + (current + 1));
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
                    Logging.log("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.log("Gratia database FAILED to upgrade from " + current + " to " + (current + 1));
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
                    Logging.log("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.log("Gratia database FAILED to upgrade from " + current + " to " + (current + 1));
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
                    Logging.log("Gratia database upgraded from " + current + " to " + 10);
                    current = 10;
                    UpdateDbVersion(current);
                } else {
                    Logging.log("Gratia database FAILED to upgrade from " + current + " to " + 10);
                }
            }
            if (current == 10) {
                int result = Execute("ALTER TABLE MetricRecord MODIFY DetailsData TEXT");
                if (result > -1) {
                    Logging.log("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.log("Gratia database FAILED to upgrade from " + current + " to " + (current + 1));
                } 
            }
            if ((current > 10 ) && (current < 14)) { // Never saw the light of day and superseded.
                int new_version = 14;
                Logging.log("Gratia database upgraded from " + current + " to " + new_version);
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
                    Logging.log("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.log("Gratia database FAILED to upgrade from " + current +
                                " to " + (current + 1));
                }
            }
            boolean haveProbeSummaryTable =
                (1 == getCount("select count(*) from information_schema.tables where " +
                               "table_schema = Database() and table_name = " +
                               dq + "ProbeSummary" + dq));
            if (current == 15) {
                int result = Execute("update JobUsageRecord_Meta set ReportedSiteName = SiteName where SiteName is not null");
                if (result > -1) {
                    result = Execute("update JobUsageRecord_Meta set ReportedSiteNameDescription = SiteNameDescription where SiteNameDescription is not null");
                }
                if (result > -1) {
                    result = Execute("alter table JobUsageRecord_Meta drop column SiteName, drop column SiteNameDescription");
                }
                if (result > -1) {
                    Logging.log("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.log("Gratia database FAILED to upgrade from " + current +
                                " to " + (current + 1));
                }
            }
            if (current == 16) {
                int result = 0;
                if (haveProbeSummaryTable &&
                    (1 == readIntegerDBProperty("gratia.database.wantSummaryTable")) &&
                    ! (readIntegerDBProperty("gratia.database.summaryTableVersion") <
                       latestDBVersionRequiringSummaryTableLoad)) {
                    // Only if we have summary tables but we're not
                    // going to upgrade them.
                    try {
                        // For a short time, DB conversion 15 -> 16 was
                        // changing the name of a column in ProbeSummary
                        // from SiteName to ReportedSiteName; since in
                        // version 17 we decided to drop it, we take the
                        // name change out of the v16 upgrade and delete
                        // whichever column we find.
                        Statement statement;
                        ResultSet resultSet;
                        statement = connection.createStatement();
                        String query = "select COLUMN_NAME from " +
                            "information_schema.COLUMNS where TABLE_SCHEMA = Database()" +
                            " and TABLE_NAME = " +
                            dq + "ProbeSummary" + dq + " and COLUMN_NAME like " +
                            dq + "%SiteName" + dq;
                        resultSet = statement.executeQuery(query);
                        String cmd = "alter table ProbeSummary";
                        int nresults = 0;
                        while (resultSet.next()) {
                            if (nresults > 0) {
                                cmd += ",";
                            }
                            cmd += " drop column " + resultSet.getString(1);
                            ++nresults;
                        }
                        result = Execute(cmd);
                    }
                    catch (Exception e) {
                        result = -1;
                    }
                }
                if (result > -1) {
                    Logging.log("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.log("Gratia database FAILED to upgrade from " + current +
                                " to " + (current + 1));
                }
            }
            if (current == 17) {
                // Auxiliary DB item upgrades only.
                Logging.log("Gratia database upgraded from " + current + " to " + (current + 1));
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
                    Logging.log("Gratia database upgraded from " + current + " to " + (current + 1));
                    current = current + 1;
                    UpdateDbVersion(current);
                } else {
                    Logging.log("Gratia database FAILED to upgrade from " + current +
                                " to " + (current + 1));
                }
            }
            if (current == 19) {
                // Auxiliary DB item upgrades only.
                Logging.log("Gratia database upgraded from " + current + " to " + (current + 1));
                current = current + 1;
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
            Logging.log("Command: Error: " + cmd + " : " + e);
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
            Logging.log("Command: Error: " + check + " : " + e);
        }        
    }
}

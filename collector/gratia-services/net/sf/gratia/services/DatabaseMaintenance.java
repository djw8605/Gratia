package net.sf.gratia.services;

import net.sf.gratia.util.XP;
import net.sf.gratia.util.Execute;
import net.sf.gratia.util.Logging;
import net.sf.gratia.util.LogLevel;
import net.sf.gratia.storage.ComputeElementRecord;
import net.sf.gratia.storage.JobUsageRecord;

import java.io.File;
import java.lang.Long;
import java.lang.System;
import java.math.BigInteger;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class DatabaseMaintenance {

   static final String dq = "\"";
   static final String comma = ",";
   static final int gratiaDatabaseVersion = 94;
   static final int latestDBVersionRequiringStoredProcedureLoad = 93;
   static final int latestDBVersionRequiringSummaryViewLoad = 92;
   static final int latestDBVersionRequiringSummaryTriggerLoad = 93;
   static final int latestDBVersionRequiringTableStatisticsRefresh = 87;
   static boolean dbUseJobUsageSiteName = false;
   java.sql.Connection connection;
   int liveVersion = 0;
   boolean isInnoDB = false;

   public DatabaseMaintenance(Properties p) {

      String driver = p.getProperty("service.mysql.driver");
      String url = p.getProperty("service.mysql.url");
      String user = p.getProperty("service.mysql.user");
      String password = p.getProperty("service.mysql.password");

      try {
         Logging.info("DatabaseMaintenance: initialized"); 
         Class.forName(driver);
         connection = (java.sql.Connection) DriverManager.getConnection(url, user, password);
      } catch (Exception e) {
         Logging.warning("DatabaseMaintenance: Error During connection: " + e);
         Logging.warning(XP.parseException(e));
         return;
      }

      RationalizePropsTable();

      ReadLiveVersion();
      Logging.info("DatabaseMaintenance: current database version(" + liveVersion + ") intended database version(" + gratiaDatabaseVersion + ")"); 

   }

   public boolean IsDbNewer() {
      return (liveVersion > gratiaDatabaseVersion);
   }

   static public boolean UseJobUsageSiteName() {
      return dbUseJobUsageSiteName;
   }

   public boolean InitialCleanup() {
      // Do check and cleanup that must be done before Hibernate is started.

      if (liveVersion < 4) {
         Execute("DROP VIEW IF EXISTS Role;");
      }
      if (liveVersion < 5) {
         Execute("DROP VIEW IF EXISTS Site;");
      }
      if (liveVersion < 6) {
         Execute("DROP VIEW IF EXISTS Probe;");
      }
      if (liveVersion < 65) {
         Execute("DROP TABLE IF EXISTS trace;");
      }

      String grep_cmd[] = {"grep", "-e", "org\\.hibernate\\.dialect\\.MySQLInnoDBDialect",
         net.sf.gratia.util.Configuration.getHibernatePath()};

      int result = Execute.execute(grep_cmd);
      if (result == 0) {
         isInnoDB = true;
         return CheckAndConvertTables();
      }
      return true;
   }

   public boolean TableExist(String table) 
   {
      // Return true if the table exist.
      
      String check = "show tables like \"" + table + "\"";
      
      try {
         Logging.log("Executing: " + check);
         Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery(check);
         boolean exist = (resultSet.next()); 
         resultSet.close();
         statement.close();
         return exist;
      }  catch (Exception e) {
         Logging.warning("Command: Error: " + check + " : " + e);
         return false;
      }      
   }
   
   public void AddIndex(String table, Boolean unique, String name,
         String content) throws Exception {

       AddIndex(table, unique, name, content, false, false, null);
   }

   public void AddIndex(String table, Boolean unique, String name,
         String content, Boolean avoidDuplicateIndex) throws Exception {

       AddIndex(table, unique, name, content, avoidDuplicateIndex, false, null);
   }

   public void AddIndex(String table, Boolean unique, String name,
         String content, Boolean avoidDuplicateIndex, Boolean ignore) throws Exception {

       AddIndex(table, unique, name, content, avoidDuplicateIndex, ignore, null);
   }

   public void AddIndex(String table, Boolean unique, String name,
                        String content, Boolean avoidDuplicateIndex, Boolean ignore,
                        String prefix) throws Exception {
      Statement statement;
      ResultSet resultSet;

      String check = "show index from " + table + " where Key_name = '" + name + "'";
      String checkcontent = "show index from " + table + " where Column_name = '" + content + "'";

      String cmd = "alter ";
      if (ignore) {
          cmd += "ignore ";
      }
      cmd += "table " + table + " add ";
      if (unique) {
          cmd += "unique ";
      }
      if (prefix != null) {
         content = content + "(" + prefix + ")";
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
         Logging.warning("Command: Error: " + cmd + " : " + e);
         throw e;
      }
   }

   public void DropIndex(String table, String name) throws Exception {
      Statement statement;
      ResultSet resultSet;

      String check = "show index from " + table + " where Key_name = '" + name + "'";
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
         Logging.warning("Command: Error: " + cmd + " : " + e);
         throw e;
      }
   }

    public void MaybeFixOriginAndAddIndex() throws Exception {
        Statement statement;
        ResultSet resultSet;

        String check = "show index from Origin where Key_name = 'o01'";

        statement = connection.createStatement();
        resultSet = statement.executeQuery(check);
        Boolean needIndex = false;
        if (!resultSet.next()) {
            needIndex = true;
        }
        statement.close();

        if (!needIndex) {
            return; // Nothing to do.
        }

        Logging.info("Checking for problems with Origin table prior to adding new unique composite index.");
        String dupFinder = "select ServerDate, cid" +
            " from Origin group by ServerDate, cid having count(*) > 1";
        statement = connection.createStatement();
        resultSet = statement.executeQuery(dupFinder);
        List<String>tableList = new ArrayList<String>();
        while (resultSet.next()) {
            String oidFinder = "select originid from Origin where ServerDate = ? and cid = ?";
            PreparedStatement s2 = connection.prepareStatement(oidFinder);
            s2.setTimestamp(1, resultSet.getTimestamp("ServerDate"));
            s2.setInt(2, resultSet.getInt("cid"));
            ResultSet r2 = s2.executeQuery();
            Integer originalOID = null;
            while (r2.next()) {
                if (originalOID == null) {
                    originalOID = r2.getInt("originid");
                } else {
                    Integer toReplace = r2.getInt("originid");
                    if (tableList.isEmpty()) { // Populate tableList
                        String listSelect = "select table_name " +
                            "from information_schema.tables " +
                            "where table_schema = database() " +
                            "and table_name like '%_Origin'";
                        Statement s3 = connection.createStatement();
                        ResultSet r3 = s3.executeQuery(listSelect);
                        while (r3.next()) {
                            tableList.add(r3.getString(1)); 
                        } // End loop over _Origin table names.
                        s3.close();
                    } // End if (tableList empty).
                    for (String tableName : tableList) { // Update _Origin tables to refer to correct originid
                        String updateCmd = "update " + tableName + " set originid = ? where originid = ?";
                        PreparedStatement s4 = connection.prepareStatement(updateCmd);
                        s4.setInt(1, originalOID);
                        s4.setInt(2, toReplace);
                        int nChanges = s4.executeUpdate();
                        if (nChanges > 0) {
                            Logging.fine("Corrected " + nChanges + " problematic entries in table " + tableName);
                        }
                    }
                    // Remove the now-unused duplicate entry from Origin.
                    String deleteCmd = "delete from Origin where originid = ?";
                    PreparedStatement s5 = connection.prepareStatement(deleteCmd);
                    s5.setInt(1, toReplace);
                    int nDeletes = s5.executeUpdate();
                    if (nDeletes == 1) {
                        Logging.fine("Deleted now-unused duplicate entry " + toReplace + " from Origin table.");
                    } else {
                        Logging.warning("Unable to delete entry with originid " + toReplace + " from Origin table.");
                    }
                } // End if (originalOID).
            } // End loop over originIds for a given duplicate set.
            s2.close();
        }
        Logging.log("Adding unique index to Origin table.");
        // Add the required index.
        AddIndex("Origin", true, "o01", "ServerDate,cid", false );
        Logging.info("Addition of unique index to Origin table complete.");
    }

    public void CheckIndices() throws Exception {
      Logging.info("DatabaseMaintenance: checking indexes on tables.");

      AddIndex("Site", true, "index02", "SiteName");
      AddIndex("Probe", true, "index02", "probename");

      // Indices for Connection and Certificate tracking
      AddIndex("Certificate", true, "pem01", "pem", true, false, "128");
      AddIndex("Origin", false, "s01", "ServerDate", false );
      MaybeFixOriginAndAddIndex();

      //
      // the following were added to get rid of unused indexes
      //
      DropIndex("JobUsageRecord", "index04");
      DropIndex("JobUsageRecord", "index06");
      DropIndex("JobUsageRecord", "index07");
      DropIndex("JobUsageRecord", "index09");
      DropIndex("JobUsageRecord", "index10");


      // No longer necessary since we're using md5v2 instead
      DropIndex("JobUsageRecord_Meta", "index12");
      DropIndex("JobUsageRecord_Meta", "md5"); // Possible

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

      AddIndex("JobUsageRecord_Meta", false, "index13", "ServerDate");
      AddIndex("JobUsageRecord_Xml", false, "NullExtraXml", "ExtraXml(1)");

      // ProbeDetails
      ensureUniqueMd5("ProbeDetails");
      AddIndex("ProbeDetails_Meta", false, "index03", "ProbeName");
      AddIndex("ProbeDetails_Meta", false, "index13", "ServerDate");
      AddIndex("ProbeDetails_Meta", false, "probeid", "probeid");
      AddIndex("ProbeDetails_Xml", false, "NullExtraXml", "ExtraXml(1)");

      // MetricRecord
      ensureUniqueMd5("MetricRecord");
      AddIndex("MetricRecord_Xml", false, "NullExtraXml", "ExtraXml(1)");
      AddIndex("MetricRecord_Meta", false, "index03", "ProbeName");
      AddIndex("MetricRecord_Meta", false, "index13", "ServerDate");
      AddIndex("MetricRecord_Meta", false, "probeid", "probeid");
      AddIndex("MetricRecord", false, "MetricName", "MetricName");
      AddIndex("MetricRecord", false, "MetricStatus", "MetricStatus");
      AddIndex("MetricRecord", false, "Timestamp", "Timestamp");
      AddIndex("MetricRecord", false, "ServiceType", "ServiceType");
      AddIndex("MetricRecord", false, "ServiceUri", "ServiceUri");
      AddIndex("MetricRecord", false, "GatheredAt", "GatheredAt");
      AddIndex("MetricRecord", false, "HostName", "HostName");
      AddIndex("MetricRecord", false, "MetricType", "MetricType");
      AddIndex("MetricRecord", false, "VoName", "VoName");

      // ComputeElement
      ensureUniqueMd5("ComputeElement");
      AddIndex("ComputeElement_Meta", false, "index13", "ServerDate");
      AddIndex("ComputeElement", false, "Timestamp", "Timestamp");
      AddIndex("ComputeElement", false, "index03", "ProbeName");
      AddIndex("ComputeElement", false, "probeid", "probeid");
      AddIndex("ComputeElement", false, "Cluster", "Cluster");
      AddIndex("ComputeElement", false, "UniqueID", "UniqueID");
      AddIndex("ComputeElement", true, "time_unique", "UniqueID, Timestamp");

      // StorageElement
      ensureUniqueMd5("StorageElement");
      AddIndex("StorageElement_Meta", false, "index13", "ServerDate");
      AddIndex("StorageElement", false, "Timestamp", "Timestamp");
      AddIndex("StorageElement", false, "index03", "ProbeName");
      AddIndex("StorageElement", false, "probeid", "probeid");
      AddIndex("StorageElement", false, "UniqueID", "UniqueID");
      AddIndex("StorageElement", false, "ParentID", "ParentID");
      AddIndex("StorageElement", false, "OwnerDN", "OwnerDN");
      AddIndex("StorageElement", false, "SE", "SE");

      // ComputeElementRecord
      ensureUniqueMd5("ComputeElementRecord");
      AddIndex("ComputeElementRecord_Meta", false, "index13", "ServerDate");
      AddIndex("ComputeElementRecord", false, "Timestamp", "Timestamp");
      AddIndex("ComputeElementRecord", false, "index03", "ProbeName");
      AddIndex("ComputeElementRecord", false, "UniqueID", "UniqueID");
      AddIndex("ComputeElementRecord", false, "probeid", "probeid");

      // StorageElementRecord
      ensureUniqueMd5("StorageElementRecord");
      AddIndex("StorageElementRecord_Meta", false, "index13", "ServerDate");
      AddIndex("StorageElementRecord", false, "Timestamp", "Timestamp");
      AddIndex("StorageElementRecord", false, "index03", "ProbeName");
      AddIndex("StorageElementRecord", false, "UniqueID", "UniqueID");
      
      // Subcluster
      ensureUniqueMd5("Subcluster");
      AddIndex("Subcluster_Meta", false, "index13", "ServerDate");
      AddIndex("Subcluster", false, "Timestamp", "Timestamp");
      AddIndex("Subcluster", false, "index03", "ProbeName");
      AddIndex("Subcluster", false, "UniqueID", "UniqueID");
      
      // Index on DupRecord
      AddIndex("DupRecord", false, "index02", "eventdate");
      if ((liveVersion == 0) || (liveVersion >= 24)) { // Only if we have the correct table format.
         AddIndex("DupRecord", false, "index03", "RecordType");
         AddIndex("DupRecord", false, "index04", "source");
         AddIndex("DupRecord", false, "index05", "error");
      }

      //
      // new indexes for authentication
      //
      AddIndex("JobUsageRecord", false, "index14", "VOName");
      AddIndex("JobUsageRecord", false, "index15", "CommonName");

      //
      // New index for ResourceType
      //
      AddIndex("JobUsageRecord", false, "index16", "ResourceType");

      //
      // Indexes for VONameCorrection
      //
      AddIndex("VONameCorrection", false, "index01", "VOName, ReportableVOName");

      // Remove hibernate's index, because it could be premature: replace it
      // with our own. This won't remove the, "final" one because we
      // called it something different. We'll need to calculate the
      // new checksums en bloc and *then* make the index unique.
      DropIndex("JobUsageRecord_Meta", "md5v2");
      AddIndex("JobUsageRecord_Meta", true, "index17", "md5v2", true);

      // Indexes for MasterSummaryData
      AddIndex("MasterSummaryData", false, "index01", "EndTime");
      AddIndex("MasterSummaryData", false, "index02", "VOcorrid");
      AddIndex("MasterSummaryData", false, "index03", "ProbeName");
      AddIndex("MasterSummaryData", false, "index04", "CommonName");
      AddIndex("MasterSummaryData", false, "index05", "ResourceType");
      AddIndex("MasterSummaryData", false, "index06", "HostDescription");
      AddIndex("MasterSummaryData", false, "index07", "ApplicationExitCode");
      DropIndex("MasterSummaryData",       "index08");
      DropIndex("MasterSummaryData",       "index09");
      DropIndex("MasterSummaryData",       "index10");
      AddIndex("MasterSummaryData", false, "index11", "DistinguishedName");
      DropIndex("MasterSummaryData",       "index12");
      AddIndex("MasterSummaryData", false, "index13", "Cores");
      AddIndex("MasterSummaryData", false, "index14", "Grid");
      AddIndex("MasterSummaryData", false, "index15", "ProjectNameCorrid");
      AddIndex("MasterSummaryData", true,  "index17",
            "EndTime, VOcorrid, ProbeName, " +
            "CommonName, DistinguishedName, ResourceType, " +
            "HostDescription, ApplicationExitCode, Grid, " +
            "Cores, ProjectNameCorrid");

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
      DropIndex("MasterTransferSummary",       "index10");
      AddIndex("MasterTransferSummary", false, "index11", "DistinguishedName");
      AddIndex("MasterTransferSummary", false, "index12", "Grid");
      AddIndex("MasterTransferSummary", false, "index13", "ProjectNameCorrid");
      AddIndex("MasterTransferSummary", true, "index14",
            "StartTime, VOcorrid, ProbeName, " +
            "CommonName, Protocol, RemoteSite, " +
            "Status, IsNew, StorageUnit, " +
            "Grid, DistinguishedName, ProjectNameCorrid");

      // Indexes for MasterServiceSummary
      AddIndex("MasterServiceSummary", false, "index01", "Timestamp");
      AddIndex("MasterServiceSummary", false, "index02", "CEUniqueID");
      AddIndex("MasterServiceSummary", false, "index03", "SiteName");
      AddIndex("MasterServiceSummary", false, "index04", "VOcorrid");

      // Indexes for MasterServiceSummaryHourly
      AddIndex("MasterServiceSummaryHourly", false, "index01", "Timestamp");
      AddIndex("MasterServiceSummaryHourly", false, "index02", "CEUniqueID");
      AddIndex("MasterServiceSummaryHourly", false, "index03", "SiteName");
      AddIndex("MasterServiceSummaryHourly", false, "index04", "VOcorrid");

      if (readIntegerDBProperty("gratia.database.wantNodeSummary") > 0) {
         // Indexes for NodeSummary
         AddIndex("NodeSummary", false, "index01", "EndTime");
         AddIndex("NodeSummary", false, "index02", "Node");
         AddIndex("NodeSummary", false, "index03", "ProbeName");
         AddIndex("NodeSummary", false, "index04", "ResourceType");
         AddIndex("NodeSummary", true, "index05", "EndTime, Node, ProbeName, ResourceType");
         AddIndex("NodeSummary", false, "index06", "HostDescription");
      }

      if (readIntegerDBProperty("gratia.database.wantSummaryTrigger") == 0) {
         // OSG Daily.
         AddIndex("JobUsageRecord_Meta", false, "index18", "ReportedSiteName");
      }

      // CPUInfo
      AddIndex("CPUInfo", false, "index01", "HostDescription");

      // Indexes for Trace table to facilitate housekeeping.
      AddIndex("trace", false, "index01", "eventtime");
      AddIndex("trace", false, "index02", "procName");

      // SystemProplist management (safety)
      AddIndex("SystemProplist", true, "index01", "car");

      // Replication (safety)
      AddIndex("Replication", true, "index01",
            "openconnection, secureconnection, probename, recordtable");

      Logging.info("DatabaseMaintenance: table index checking complete.");

   }

   private int CallPostInstall(String action) {
      Logging.info("DatabaseMaintenance: Calling post-install script for action \"" + action + "\"");
      String post_install = "/usr/share/gratia/post-install"; 
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

    private boolean tableHasColumn(String table, String column) {
        Statement statement;
        ResultSet resultSet;

        String cmd = "show columns from " + table + " where Field = '" + column + "'";

        try {
            Logging.log("Executing: " + cmd);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(cmd);
            if (resultSet.next()) {
                return true;
            }
            Logging.log("Command: OK: " + cmd);
        } catch (Exception e) {
            Logging.warning("Command: Error: " + cmd + " : " + e);
        }
        return false;
    }

   public void SiteDefaults() {
      Statement statement;
      ResultSet resultSet;

      String check = "select count(*) from Site where SiteName = 'Unknown'";
      String cmd = "insert into Site(SiteName) values(" + dq + "Unknown" + dq + ")";

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
                  !((column.startsWith("Host")) || (column.startsWith("Status")))) {
               continue; // Skip these columns
            } else if (column.equals("SiteName")) {
               column = "Site.SiteName";
            } else if (column.equals("Status")) {
               column = "JobUsageRecord.Status";
            } else if (column.equals("dbid")) {
               column = "JobUsageRecord_Meta.dbid";
            } else if (column.equals("VOName") || column.equals("ReportableVOName") || column.equals("VOcorrid")) {
               continue; // Skip these columns
            } else if (column.equals("ProjectName")) {
               continue; // Skip these columns
            }
            if (result.length() > 0) {
               result = result + ",";
            }
            result = result + column;
         }
      } catch (Exception e) {
         Logging.warning("Command: Error: " + cmd + " : " + e);
      }
      return result;
   }

   public void CPUMetricDefaults() {
      Execute("delete from CPUMetricTypes");
      Execute("insert into CPUMetricTypes(CPUMetricType) values(" + dq + "wallclock" + dq + ")");
      Execute("insert into CPUMetricTypes(CPUMetricType) values(" + dq + "process" + dq + ")");
   }

   public void RoleDefaults() {
      if (getCount("select count(*) from Role where role='GratiaGlobalAdmin'") == 0) {
         Execute("insert into Role(role,subtitle,whereclause) values('GratiaGlobalAdmin','GratiaGlobalAdmin','')");
      }
      if (getCount("select count(*) from Role where role='GratiaUser'") == 0) {
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
      int wantSummaryTrigger = 1;
      try {
         wantSummaryTrigger =
               Integer.parseInt(p.getProperty("gratia.database.wantSummaryTrigger",
               "1"));
      } catch (Exception ignore) {
      }
      UpdateDbProperty("gratia.database.wantSummaryTrigger",
            wantSummaryTrigger);
      JobUsageRecord.setwantSummary(wantSummaryTrigger == 1);
      ComputeElementRecord.setwantSummary(wantSummaryTrigger == 1);
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
            ", JobUsageRecord_Meta.ServerDate " +
            ", ProjectNameCorrection.ReportableProjectName " +
            " from JobUsageRecord_Meta, Site, Probe, JobUsageRecord, VO, VONameCorrection, ProjectNameCorrection " +
            " where " +
            " JobUsageRecord_Meta.probeid = Probe.probeid and Probe.siteid = Site.siteid" +
            " and JobUsageRecord_Meta.dbid = JobUsageRecord.dbid" +
            " and binary JobUsageRecord.VOName = binary VONameCorrection.VOName" +
            " and ((binary JobUsageRecord.ReportableVOName = binary VONameCorrection.ReportableVOName) or" +
            " ((JobUsageRecord.ReportableVOName is null) and (VONameCorrection.ReportableVOName is null)))" +
            " and VONameCorrection.void = VO.void" +
            " and JobUsageRecord.VOName = VONameCorrection.VOName" +
            " and ((binary JobUsageRecord.ProjectName = binary ProjectNameCorrection.ProjectName) or " +
            " ((JobUsageRecord.ProjectName is null) and (ProjectNameCorrection.ProjectName is null)))");
   }

   public int readIntegerDBProperty(String property) {
      Statement statement;
      ResultSet resultSet;

      String check = "select cdr from SystemProplist where car = " + dq + property + dq;

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

      if (1 != getCount("select count(*) from information_schema.tables where " +
            "table_schema = Database() and table_name = " +
            dq + "SystemProplist" + dq)) {
         return;
      } // No SystemProplist table yet

      String check = "select cdr from SystemProplist where car = " + dq + "gratia.database.version" + dq;

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
      if (value == null) {
         return;
      }
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
      int result = 1;
      int wanted = 1;
      int ver = 1;
      Logging.info("DatabaseMaintenance: checkAndUpgradeDbAuxiliaryItems started");
      if (readIntegerDBProperty("gratia.database.version") != gratiaDatabaseVersion) {
         Logging.log(LogLevel.SEVERE,
               "INTERNAL ERROR: DatabaseMainentance::checkAndUpgradeDbAuxiliaryItems" +
               " called with inconsistent DB version");
         return false;
      }

      // First check summary tables
      result = checkAndUpdateProperty("summary-view","gratia.database.summaryViewVersion",latestDBVersionRequiringSummaryViewLoad);
      if ( result == 0 ) {
          return false;
      } 

      // First trigger 
      wanted = readIntegerDBProperty("gratia.database.wantSummaryTrigger");
      Logging.info("DatabaseMainentance: gratia.database.wantSummaryTrigger = " + wanted);
      if (1 == wanted) {
          result  = checkAndUpdateProperty("trigger","gratia.database.summaryTriggerVersion",latestDBVersionRequiringSummaryTriggerLoad);
          if ( result == 0 ) {
             return false;
         } 
      }

      // Check stored procedures
      wanted = readIntegerDBProperty("gratia.database.wantStoredProcedures");
      Logging.info("DatabaseMainentance: gratia.database.wantStoredProcedures = " + wanted);
      if (1 == wanted) {
          result  = checkAndUpdateProperty("stored","gratia.database.storedProcedureVersion",latestDBVersionRequiringStoredProcedureLoad);
          if ( result == 0 ) {
             return false;
         } 
      }

      // Check TableStatistics
      {
         ver = readIntegerDBProperty("gratia.database.TableStatisticsVersion");
         Logging.info("DatabaseMainentance: gratia.database.TableStatisticsVersion = " + ver);
         if (ver < latestDBVersionRequiringTableStatisticsRefresh  || !TableExist("TableStatistics")) {
            result = RefreshTableStatistics();
            if (result > -1) {
               UpdateDbProperty("gratia.database.TableStatisticsVersion", gratiaDatabaseVersion);
               Logging.info("DatabaseMainentance: Table statistics updated successfully to version " + gratiaDatabaseVersion);
            } else {
               Logging.warning("DatabaseMainentance: FAILED, table statistics NOT updated");
               return false;
            }
         }
      }
      
      // Check TableStatistics History
      {
         ver = readIntegerDBProperty("gratia.database.TableStatisticsHistoryVersion");
         Logging.info("DatabaseMainentance: gratia.database.TableStatisticsHistoryVersion = " + ver);
         if (ver < 0 || !TableExist("TableStatisticsSnapshots")) {
            result = UpdateTableStatisticsHistory(ver);
            if (result > -1) {
               UpdateDbProperty("gratia.database.TableStatisticsHistoryVersion", gratiaDatabaseVersion);
               Logging.info("DatabaseMainentance: Table statistics history updated successfully to version " + gratiaDatabaseVersion);
            } else {
               Logging.warning("DatabaseMainentance: FAILED, table statistics history NOT updated properly");
               return false;
            }
         }
         
      }
      
      // Check BacklogStatistics History
      {
         ver = readIntegerDBProperty("gratia.database.BacklogStatisticsHistoryVersion");
         Logging.info("DatabaseMainentance: gratia.database.BacklogStatisticsHistoryVersion = " + ver);
         if (ver < 0 || !TableExist("BacklogStatistics") || !TableExist("BacklogStatisticsSnapshots")) {
            result = CreateBacklogTables(ver);
            if (result > -1) {
               UpdateDbProperty("gratia.database.BacklogStatisticsHistoryVersion", gratiaDatabaseVersion);
               Logging.info("DatabaseMainentance: Backlog statistics history updated successfully to version " + gratiaDatabaseVersion);
            } else {
               Logging.warning("DatabaseMainentance: FAILED backlog statistics history NOT updated properly");
               return false;
            }
         }
         
      }
      Logging.info("DatabaseMaintenance: checkAndUpgradeDbAuxiliaryItems complete");
      return true;
   }
   
   private int checkAndUpdateProperty(String type,String property,int latest) {
      int current = readIntegerDBProperty(property);
      Logging.info("DatabaseMaintenance: Checking " + type + " - current(" + current + ") latest(" + latest + ")");
      if ( current < latest ) {
         int result = CallPostInstall(type);
         if (result > -1) {
            UpdateDbProperty(property, latest);
            Logging.info("DatabaseMaintenance: " + type + " updated successfully to version " + latest);
         } else {
            Logging.warning("DatabaseMaintenance: update FAILED - " + type + " NOT updated");
            return 0;
         }
      }
      return 1;
   }  //end of checkAndUpdateProperty  
  

   public boolean Upgrade() {
      PropertyDefaults(); // Make sure we have the values in SystemProplist
      int oldvers = liveVersion;
      Logging.info("DatabaseMaintenance: starting Upgrade check - current version: " + liveVersion + " supported version: " + gratiaDatabaseVersion);

      // if schema versions are up-to-date, just check auxilary items.
      if ( liveVersion == gratiaDatabaseVersion ) {
          Logging.info("DatabaseMaintenance: no schema updates required.");
          return checkAndUpgradeDbAuxiliaryItems();
      }

      // check to see if this is an initial install
      if (oldvers == 0) {
         Statement statement;
         ResultSet resultSet;

         String check = "select count(*),min(PropId) from SystemProplist where car = " + dq + "gratia.database.version" + dq;

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
                  Execute("insert into SystemProplist(car,cdr) values(" + dq + "gratia.database.version" + dq + comma + dq + gratiaDatabaseVersion + dq + ")");
               }
            }
            Logging.log("Command: OK: " + check);
         } catch (Exception e) {
            Logging.warning("Command: Error: " + check + " : " + e);
         }

         Logging.info("DatabaseMaintenance: Gratia database now at version " + gratiaDatabaseVersion);
         return checkAndUpgradeDbAuxiliaryItems();
      } else {
         // Do the necessary upgrades if any

         int current = oldvers;

         if (current < 31) {
            Logging.warning("Gratia database schema has been created by v0.32 or older, please first upgrade to Gratia v1.02 and then upgrade to this current release");
            return false;
         }
         if (32 <= current && current <= 37) {
            // Auxiliary DB item upgrades only (trigger code)
            Logging.fine("Gratia database upgraded from " + current + " to " + (38));
            current = 38;
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
                  if (extra.contains("auto_increment")) {
                     continue; // No change required.
                  }
                  if (tableName.equals("MasterSummaryData")) {
                     Logging.debug("DatabaseMaintenance: adding auto_increment key to MasterSummaryData.");
                     result = Execute("alter table MasterSummaryData MODIFY COLUMN SummaryID INT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (SummaryID);");
                  } else if (tableName.equals("NodeSummary")) {
                     Logging.debug("DatabaseMaintenance: adding auto_increment key to NodeSummary.");
                     result = Execute("alter table NodeSummary MODIFY COLUMN NodeSummaryID INT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (NodeSummaryID);");
                  }
               }
            } catch (Exception e) {
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
               session.close();
            } catch (Exception e) {
               HibernateWrapper.closeSession( session );
               Logging.debug("Exception detail: ", e);
               if ((Exception) e.getCause() != null) {
                   Logging.debug("Causing exception detail: ", (Exception) e.getCause());
               }
               Logging.warning("Gratia database FAILED to upgrade from " + current +
                     " to " + (current + 1));
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
         if (current == 47) {
            // NOP (md5 index on MetricRecord_Meta handled with rest of indexes).
            Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
            ++current;
            UpdateDbVersion(current);
         }
         if ((current >= 48) && (current <= 55)) {
            // Auxiliary DB item upgrades only (trigger code and friends, stored procedures)
            Logging.fine("Gratia database upgraded from " + current + " to 56");
            current = 56;
            UpdateDbVersion(current);
         }
         if (current == 56 || current == 57) {
            int result = 0;
            Session session = null;
            try {
               session = HibernateWrapper.getSession();
               Transaction tx = session.beginTransaction();
               Query q = session.createSQLQuery("ALTER TABLE MasterSummaryData " +
                     "MODIFY COLUMN ApplicationExitCode VARCHAR(255) NOT NULL DEFAULT '0', " +
                     "MODIFY COLUMN VOcorrid BIGINT(20) NOT NULL DEFAULT 0," +
                     "MODIFY COLUMN Njobs BIGINT(20) NOT NULL DEFAULT 0;");
               q.executeUpdate();
               tx.commit();
               session.close();
            } catch (Exception e) {
               HibernateWrapper.closeSession( session );
               Logging.debug("Exception detail: ", e);
               Logging.warning("Gratia database FAILED to upgrade from " + current +
                     " to 58");
               result = -1;
            }
            if (result > -1) {
               Logging.fine("Gratia database upgraded from " + current + " to 58");
               current = 58;
               UpdateDbVersion(current);
            }
         }
         if (current == 58) {
            // Trigger procedures.
            Logging.fine("Gratia database upgraded from " + current + " to " + (current + 1));
            ++current;
            UpdateDbVersion(current);
         }
         if (current == 59) {
            int result = 0;
            Session session = null;
            try {
               session = HibernateWrapper.getSession();
               Transaction tx = session.beginTransaction();
               Query q =
                     session.createSQLQuery("ALTER TABLE NodeSummary " +
                     "MODIFY COLUMN CpuSystemTime DOUBLE," +
                     "MODIFY COLUMN CpuUserTime DOUBLE," +
                     "MODIFY COLUMN CpuCount BIGINT(20)," +
                     "MODIFY COLUMN BenchmarkScore BIGINT(20)," +
                     "MODIFY COLUMN DaysInMonth BIGINT(20);");
               q.executeUpdate();
               tx.commit();
               session.close();
            } catch (Exception e) {
               HibernateWrapper.closeSession( session );
               Logging.debug("Exception detail: ", e);
               Logging.warning("Gratia database FAILED to upgrade from " + current +
                     " to " + (current + 1));
               result = -1;
            }
            if (result > -1) {
               Logging.fine("Gratia database upgraded from " + current +
                     " to " + (current + 1));
               ++current;
               UpdateDbVersion(current);
            }
         }
         Integer schemaOnlyLowerBound = 60;
         Integer schemaOnlyUpperBound = 77;
         if ((current >= schemaOnlyLowerBound) && (current < schemaOnlyUpperBound)) {
            // Stored procedures, trigger procedures.
            Logging.fine("Gratia database upgraded from " + current + " to " + schemaOnlyUpperBound);
            current = schemaOnlyUpperBound;
            UpdateDbVersion(current);
         }
         if (current == 77) {
            // Update smaller XML tables to use MEDIUMTEXT (this is a
            // patch release). Defer Update of JobUsageRecord_Xml to a
            // major release.
            int result = 0;
            Session session = null;
            try {
               session = HibernateWrapper.getSession();
               Transaction tx = session.beginTransaction();
               Query q = session.createSQLQuery("ALTER TABLE DupRecord " +
                                                "MODIFY COLUMN extraxml MEDIUMTEXT," +
                                                "MODIFY COLUMN rawxml MEDIUMTEXT ;");
               q.executeUpdate();
               q = session.createSQLQuery("ALTER TABLE ProbeDetails_Xml " +
                                          "MODIFY COLUMN extraxml MEDIUMTEXT," +
                                          "MODIFY COLUMN rawxml MEDIUMTEXT ;");
               q.executeUpdate();
               q = session.createSQLQuery("ALTER TABLE MetricRecord_Xml " +
                                          "MODIFY COLUMN extraxml MEDIUMTEXT," +
                                          "MODIFY COLUMN rawxml MEDIUMTEXT ;");
               q.executeUpdate();
               tx.commit();
               session.close();
            } catch (Exception e) {
               HibernateWrapper.closeSession( session );
               Logging.debug("Exception detail: ", e);
               Logging.warning("Gratia database FAILED to upgrade from " + current +
                               " to " + (current + 1));
               result = -1;
            }
            if (result > -1) {
               Logging.fine("Gratia database upgraded from " + current +
                            " to " + (current + 1));
               ++current;
               UpdateDbVersion(current);
            }
         }
         schemaOnlyLowerBound = 78;
         schemaOnlyUpperBound = 80;
         if ((current >= schemaOnlyLowerBound) && (current < schemaOnlyUpperBound)) {
            // Stored procedures, trigger procedures.
            Logging.fine("Gratia database upgraded from " + current + " to " + schemaOnlyUpperBound);
            current = schemaOnlyUpperBound;
            UpdateDbVersion(current);
         }
         if (current == 80) {
             // Update JobUsageRecord_XML tables to use MEDIUMTEXT.
             int result = 0;
             Session session = null;
             try {
                 session = HibernateWrapper.getSession();
                 Transaction tx = session.beginTransaction();
                 Query q =
                     session.createSQLQuery("ALTER TABLE JobUsageRecord_Xml " +
                                            "MODIFY COLUMN extraxml MEDIUMTEXT," +
                                            "MODIFY COLUMN rawxml MEDIUMTEXT ;");
                 q.executeUpdate();
                 tx.commit();
                 session.close();
             } catch (Exception e) {
                 HibernateWrapper.closeSession( session );
                 Logging.debug("Exception detail: ", e);
                 Logging.warning("Gratia database FAILED to upgrade from " + current +
                                 " to " + (current + 1));
                 result = -1;
             }
             if (result > -1) {
                 Logging.fine("Gratia database upgraded from " + current +
                              " to " + (current + 1));
                 ++current;
                 UpdateDbVersion(current);
             }
         }
         schemaOnlyLowerBound = 81;
         schemaOnlyUpperBound = 84;
         if ((current >= schemaOnlyLowerBound) && (current < schemaOnlyUpperBound)) {
             // Stored procedures, trigger procedures.
             Logging.fine("Gratia database upgraded from " + current + " to " + schemaOnlyUpperBound);
             current = schemaOnlyUpperBound;
             UpdateDbVersion(current);
         }
         if (current == 84) {
             // Remove vestigial and obstructive ServerDate from some
             // non-Meta tables.
             int result = 0;
             Statement statement;
             ResultSet resultSet;
             String command = "SELECT T.TABLE_NAME " +
                 "FROM information_schema.COLUMNS C " +
                 "JOIN information_schema.TABLES T ON " +
                 "  (T.TABLE_TYPE = 'BASE TABLE' AND " +
                 "   T.TABLE_SCHEMA = DATABASE() AND " +
                 "   C.TABLE_SCHEMA = T.TABLE_SCHEMA AND " +
                 "   C.TABLE_NAME = T.TABLE_NAME " +
                 "   ) " +
                 "WHERE COLUMN_NAME = 'ServerDate' " +
                 "  AND C.TABLE_NAME NOT LIKE '%_Meta' " +
                 "  AND C.TABLE_NAME != 'Origin' " +
                 "ORDER by T.TABLE_SCHEMA, T.TABLE_NAME;";
             try {
                 Logging.log("Executing: " + command);
                 statement = connection.createStatement();
                 resultSet = statement.executeQuery(command);
                 while (result > -1 && resultSet.next()) {
                     String tableName = resultSet.getString(1);
                     Logging.debug("DatabaseMaintenance: removing vestigial ServerDate column from table " + tableName);
                     result = Execute("ALTER TABLE " + tableName + " DROP COLUMN ServerDate;");
                 }
             } catch (Exception e) {
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
         }
         if(current == 85) {
            Upgrade86 bigintUpgrader = new Upgrade86();

            //Upgrade from 85 to 86
            //This involves updating the data type of a bunch of fields from int to bigint 
            //All the details of the upgrade could be seen in Upgrade86.java 
            try
            {
               //Try doing the db upgrade from version 85 to version 86
               Logging.fine("Upgrade86 - Beginning database schema upgrade from version " + current + " to version " + (current + 1));
               bigintUpgrader.upgrade(connection);
               Logging.fine("Success!!! Upgrade86 - Finished database schema upgrade from version " + current + " to version " + (current + 1));

               //If and only if the upgrade finished OK, then we will reach this point in the code, where we update the gratia database version
               //If we didn't reach this point i.e. if an Exception was thrown and the code went into the catch clause, that means the value of 
               //'current' won't be updated and this will cause the Upgrade method to return false
               //By doing the version equality at the very end of the Upgrade method, if the upgrade failed, we can ensure that the code will exit CollectorService.java with a message about 
               //failed upgrade and the need for 'Manual intervention'. Refer to the CollectorService.java code for further details
               
               current = current + 1;
               UpdateDbVersion(current);
               
            }
            //If something goes wrong, catch the Exception and log it 
            catch(Exception e)
            {
               //Capture and log the full stack trace of the exception so that we have available all the details about what went wrong 
               Logging.warning("Error!!! Gratia database FAILED to upgrade from " +
                               current + " to " +
                               (current + 1) + ". Here is a stack trace of the Exception. \n" +
                               bigintUpgrader.getStackTrace(e));
                }
         }

         schemaOnlyLowerBound = 86;
         schemaOnlyUpperBound = 91;
         if ((current >= schemaOnlyLowerBound) && (current < schemaOnlyUpperBound)) {
             // Stored procedures, trigger procedures only.
            Logging.fine("DatabaseMaintenance: Gratia database upgraded from " + current + " to " + schemaOnlyUpperBound);
            current = schemaOnlyUpperBound;
            UpdateDbVersion(current);
         }

         //Upgrade from 91 to 92
         if (current == 91) {
           int result = 0;
           boolean updateResult;
           updateResult = SetAutoIncrement("ProjectNameCorrection", "ProjectNameCorrid"); 
           if ( updateResult = false) {
              Logging.log(LogLevel.SEVERE,"DatabaseMaintenance: FAILED to update database from " + current + " to " + (current + 1));
              return false;
           }
           // verify we don't already have an entry for an empty set of values
           if (getCount("select count(*) from ProjectNameCorrection where ProjectName is NULL") == 0) {
              Execute("insert into ProjectNameCorrection(ProjectNameCorrid,ProjectName,ReportableProjectName) values(NULL,NULL,'Unknown')");
           }
           Logging.log("DatabaseMaintenance: Gratia database upgraded from " + current + " to " + (current + 1));
           current = current + 1;
           UpdateDbVersion(current);
         }  

         schemaOnlyLowerBound = 92;
         schemaOnlyUpperBound = 93;
         if ((current >= schemaOnlyLowerBound) && (current < schemaOnlyUpperBound)) {
             // Stored procedures, trigger procedures only.
            Logging.fine("DatabaseMaintenance: Gratia database upgraded from " + current + " to " + schemaOnlyUpperBound);
            current = schemaOnlyUpperBound;
            UpdateDbVersion(current);
         }

          // update from 93 to 94
          if (current == 93) {
              if (!AddPKToAuxTables()) {
                  Logging.log(LogLevel.SEVERE,"DatabaseMaintenance: FAILED to update database from " + current + " to " + (current + 1));
                  return false;
              }
              Logging.log("DatabaseMaintenance: Gratia database upgraded from " + current + " to " + (current + 1));
              current = current + 1;
              UpdateDbVersion(current);
          }

         if ( liveVersion != gratiaDatabaseVersion  ) {
           Logging.log(LogLevel.SEVERE,"DatabaseMaintenance: FAILED to update database from " + current + " to " + gratiaDatabaseVersion);
           return false;
         }

         // Stored procedures, trigger procedures.
         Logging.info("DatabaseMaintenance: Gratia database now at version " + liveVersion);
         return checkAndUpgradeDbAuxiliaryItems();
      } // end of else
   } // end of Upgrade method

   // check to see if a column has auto_increment set.
   // if not, set it.
   private boolean SetAutoIncrement(String table_name, String column_name) {
     int result;
     Statement statement;
     ResultSet resultSet;
     String command = "SELECT COLUMN_NAME, COLUMN_KEY, TABLE_NAME, EXTRA FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + table_name + "' AND COLUMN_NAME = '" + column_name + "';";
     // May need to add primary key manually if not already
     // done by Hibernate (table already existed).
     try {
        Logging.log("Executing: " + command);
        statement = connection.createStatement();
        resultSet = statement.executeQuery(command);
       while (resultSet.next()) {
          String columnName = resultSet.getString(1);
          String columnKey  = resultSet.getString(2);
          String tableName  = resultSet.getString(3);
          String extra      = resultSet.getString(4);
          if (extra.contains("auto_increment")) {
             Logging.info("DatabaseMaintenance: auto_increment and primary key already set for TABLE(" + table_name + ") COLUMN(" + column_name + ")");
             continue; // No change required.
          }
          Logging.info("DatabaseMaintenance: adding auto_increment and primary key to TABLE(" + table_name + ") COLUMN(" + column_name + ")");
          command = "ALTER TABLE " + table_name + "MODIFY COLUMN " + column_name + " INT NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (" + column_name + ");";
          result = Execute(command);
          if ( result > -1 ) { 
            return false;
          }
        } // end of while
     } catch (Exception e) {
        Logging.warning("Command: Error: " + command + " : ", e);
        return false;
     } // end of try/catch
     return true;
   } // end SetAutoIncrement

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

      if (1 != getCount("select count(*) from information_schema.tables where " +
            "table_schema = Database() and table_name = " +
            dq + "SystemProplist" + dq)) {
         return;
      } // No SystemProplist table yet

      String check = "select count(*),min(PropId) from SystemProplist where car = " + dq + "gratia.database.version" + dq;

      try {
         Logging.log("Executing: " + check);
         statement = connection.createStatement();
         resultSet = statement.executeQuery(check);
         if (resultSet.next()) {
            int count = resultSet.getInt(1);
            if (count > 1) {
               int propid = resultSet.getInt(2);
               Execute("delete from SystemProplist where car = " + dq + "gratia.database.version" + dq + " and PropId > " + propid);
            }
         }
         Logging.log("Command: OK: " + check);
      } catch (Exception e) {
         Logging.warning("Command: Error: " + check + " : " + e);
      }
   }

   private int CreateBacklogTables(int oldversion) 
   {
      int result = Execute("DROP TABLE IF EXISTS BacklogStatistics");
      if (result > -1) {
         String command = net.sf.gratia.storage.Backlog.fgCreateTable;
         if (isInnoDB) {
            command += " ENGINE = 'innodb'";
         }
         result = Execute(command);
      }
      if (oldversion < 0 || !TableExist("BacklogStatisticsSnapshots"))
      {
         result = Execute("DROP TABLE IF EXISTS BacklogStatisticsSnapshots");
         if (result < 0) {
            return result;
         }
         String command = "CREATE TABLE BacklogStatisticsSnapshots(" +
         "ServerDate DATETIME NOT NULL, " + 
         "EventDate DATETIME NOT NULL, " + 
         "EntityType VARCHAR(255) NOT NULL, " +
         "Name VARCHAR(255) NOT NULL, " +
         "nRecords BIGINT DEFAULT 0, " +
         "xmlFiles BIGINT DEFAULT 0, " +
         "tarFiles BIGINT DEFAULT 0, " +
         "serviceBacklog BIGINT DEFAULT 0, " +
         "maxPendingFiles BIGINT DEFAULT 0, " +
         "bundleSize BIGINT DEFAULT 0, " +
         "UNIQUE KEY index1 (ServerDate, EntityType, Name))";
         if (isInnoDB) {
            command += " ENGINE = 'innodb'";
         }
         result = Execute(command);
         if (result < 0) {
            return result;
         }
      }
      if (oldversion < 0 || !TableExist("BacklogStatisticsHourly"))
      {
         result = Execute("DROP TABLE IF EXISTS BacklogStatisticsHourly");
         if (result < 0) {
            return result;
         }
         String command = net.sf.gratia.storage.BacklogSummary.fgCreateHourlyTable;
         if (isInnoDB) {
            command += " ENGINE = 'innodb'";
         }
         result = Execute(command);
         if (result < 0) {
            return result;
         }
      }
      if (oldversion < 0 || !TableExist("BacklogStatisticsDaily"))
      {
         result = Execute("DROP TABLE IF EXISTS BacklogStatisticsDaily");
         if (result < 0) {
            return result;
         }
         String command =  net.sf.gratia.storage.BacklogSummary.fgCreateDailyTable;
         if (isInnoDB) {
            command += " ENGINE = 'innodb'";
         }
         result = Execute(command);
         if (result < 0) {
            return result;
         }
      }
      return result;
      
   }
   
   private int RefreshTableStatistics() {
      int result = Execute("DROP TABLE IF EXISTS TableStatistics");
      if (result > -1) {
         String command = "CREATE TABLE TableStatistics(" +
               "ValueType VARCHAR(255) NOT NULL, " +
               "RecordType VARCHAR(255) NOT NULL, Qualifier VARCHAR(255) NOT NULL DEFAULT '', " +
               "nRecords BIGINT DEFAULT 0, " +
               "UNIQUE KEY index1 (ValueType, RecordType, Qualifier))";

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
               String base_table = table_name.substring(0, end_index);
               tableList.add(base_table);
            }
            resultSet.close();
            statement.close();

            // Put error type information into the TableStatistics record.
            //
            result = Execute("update DupRecord set RecordType = 'JobUsageRecord' where RecordType is null");
            result = Execute("insert into TableStatistics(ValueType, RecordType,nRecords,Qualifier)" +
                  " select 'current',RecordType, count(*), error from DupRecord group by RecordType, error;");
            // Put count(*) information in for all tables.
            tableList.add("DupRecord");
            for (Iterator x = tableList.iterator(); (result > -1) && x.hasNext();) {
               String table_name = (String) x.next();
               result = Execute("insert into TableStatistics(ValueType,RecordType,nRecords)" +
                                " select 'current','" + table_name +
                                "', count(*) from " + table_name);
               result = Execute("insert into TableStatistics(ValueType,RecordType,nRecords)" +
                                " select 'lifetime','" + table_name +
                                "', ifnull(max(dbid),0) from " + table_name);
               if (result > -1) {
                  result = CallPostInstall("countTrigger" + table_name);
               }
            }
         } catch (Exception e) {
            result = -1;
         }
      }
      return result;
   }
   
   private int UpdateTableStatisticsHistory(int oldversion) 
   {
      int result = 0;
      if (oldversion < 0 || !TableExist("TableStatisticsSnapshots"))
      {
         result = Execute("DROP TABLE IF EXISTS TableStatisticsSnapshots");
         if (result < 0) {
            return result;
         }
         String command = "CREATE TABLE TableStatisticsSnapshots(" +
         "ServerDate DATETIME NOT NULL, " + 
         "ValueType VARCHAR(255) NOT NULL, " +
         "RecordType VARCHAR(255) NOT NULL, Qualifier VARCHAR(255) NOT NULL DEFAULT ''," +
         "nRecords BIGINT DEFAULT 0, " +
         "UNIQUE KEY index1 (ServerDate, ValueType, RecordType, Qualifier))";
         if (isInnoDB) {
            command += " ENGINE = 'innodb'";
         }
         result = Execute(command);
         if (result < 0) {
            return result;
         }
      }
      if (oldversion < 0 || !TableExist("TableStatisticsHourly"))
      {
         result = Execute("DROP TABLE IF EXISTS TableStatisticsHourly");
         if (result < 0) {
            return result;
         }
         String command = "CREATE TABLE TableStatisticsHourly(" +
         "tshid BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
         "ServerDate DATETIME NOT NULL, " + 
         "EventDate DATETIME NOT NULL, " + 
         "ValueType VARCHAR(255) NOT NULL, " +
         "RecordType VARCHAR(255) NOT NULL, Qualifier VARCHAR(255) NOT NULL DEFAULT ''," +
         "StartTime DATETIME NOT NULL, " + 
         "EndTime DATETIME NOT NULL, " + 
         "avgRecords BIGINT DEFAULT 0, " +
         "maxRecords BIGINT DEFAULT 0, " +
         "minRecords BIGINT DEFAULT 0, " +
         "UNIQUE KEY index1 (EventDate, ValueType, RecordType, Qualifier))";
         if (isInnoDB) {
            command += " ENGINE = 'innodb'";
         }
         result = Execute(command);
         if (result < 0) {
            return result;
         }
      }
      if (oldversion < 0 || !TableExist("TableStatisticsDaily"))
      {
         result = Execute("DROP TABLE IF EXISTS TableStatisticsDaily");
         if (result < 0) {
            return result;
         }
         String command = "CREATE TABLE TableStatisticsDaily(" +
         "tsdid BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
         "ServerDate DATETIME NOT NULL, " + 
         "EventDate DATETIME NOT NULL, " + 
         "ValueType VARCHAR(255) NOT NULL, " +
         "RecordType VARCHAR(255) NOT NULL, Qualifier VARCHAR(255) NOT NULL DEFAULT ''," +
         "StartTime DATETIME NOT NULL, " + 
         "EndTime DATETIME NOT NULL, " + 
         "avgRecords BIGINT DEFAULT 0, " +
         "maxRecords BIGINT DEFAULT 0, " +
         "minRecords BIGINT DEFAULT 0, " +
         "UNIQUE KEY index1 (EventDate, ValueType, RecordType, Qualifier))";
         if (isInnoDB) {
            command += " ENGINE = 'innodb'";
         }
         result = Execute(command);
         if (result < 0) {
            return result;
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
                  Long.valueOf((timeTaken % 3600000) / 60000).toString() + ":" +
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

    private boolean AddPKToAuxTables() {
        // Add primary key to JobUsageRecord auxiliary tables
        String tables[] = {"Disk", "Memory", "Swap", "Network", "TimeDuration", "TimeInstant",
                "ServiceLevel", "PhaseResource", "VolumeResource", "ConsumableResource", "Resource"};
        try {
            for (String table : tables) {
                Logging.info("Adding primary key to " + table);
                // Create new table to modify
                String newtable = table + "_new";
                String command = "DROP TABLE IF EXISTS " + newtable;
                if (Execute(command) < 0) {
                    return false;
                }
                command = "CREATE TABLE " + newtable + " LIKE " + table;
                if (Execute(command) < 0) {
                    return false;
                }
                // see if table already contains pkid column, maybe added by hibernate. Remove if so.
                if (tableHasColumn(newtable, "pkid")) {
                    command = "ALTER TABLE " + newtable + " DROP COLUMN pkid";
                    if (Execute(command) < 0) {
                        return false;
                    }
                    command = "ALTER TABLE " + table + " DROP COLUMN pkid";
                    if (Execute(command) < 0) {
                        return false;
                    }
                }
                // add primary key column and foreign constraint
                command = "ALTER TABLE " + newtable + " ADD COLUMN pkid bigint(20) NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
                        "ADD CONSTRAINT FOREIGN KEY (dbid) REFERENCES JobUsageRecord (dbid) ON DELETE CASCADE";
                if (Execute(command) < 0) {
                    return false;
                }
                // copy data into new table, creating primary key
                command = "INSERT INTO " + newtable + " SELECT *,null FROM " + table;
                if (Execute(command) < 0) {
                    return false;
                }
                // rename tables, keeping old
                command = "RENAME TABLE " + table + " TO " + table + "_old, " + newtable + " TO " + table;
                if (Execute(command) < 0) {
                    return false;
                }
            }
        } catch (Exception e) {
            Logging.warning("Command: Error: " + e);
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

   private void ensureUniqueMd5(String table) throws Exception {
      Session session = null;
      try {
         session = HibernateWrapper.getSession();
         Transaction tx = session.beginTransaction();
         Query q =
               session.createSQLQuery("SELECT INDEX_NAME, NON_UNIQUE " +
               "FROM information_schema.STATISTICS " +
               "WHERE TABLE_SCHEMA = DATABASE() " +
               "  AND TABLE_NAME = '" + table + "_Meta'" +
               "  AND COLUMN_NAME = 'md5'");
         ScrollableResults records = q.scroll(ScrollMode.FORWARD_ONLY);
         Boolean hasUnique = false;
         ArrayList<String> indexesToRemove = new ArrayList<String>();
         while (records.next()) {
            Object[] results = records.get();
            if (((BigInteger) results[1]).intValue() == 0) { // Unique
               hasUnique = true;
            } else { // Remove non-unique index
               indexesToRemove.add((String) results[0]);
            }
         }
         String removeCommand = null;
         // Remove indexes
         for (String iName : indexesToRemove) {
            if (removeCommand == null) {
               removeCommand = "ALTER TABLE " + table + "_Meta ";
            } else {
               removeCommand += ", ";
            }
            removeCommand += "DROP INDEX " + iName;
         }
         if (removeCommand != null) {
            q = session.createSQLQuery(removeCommand);
            q.executeUpdate();
         }
         if (!hasUnique) {
            // Add unique index, dropping duplicates
            q = session.createSQLQuery("ALTER IGNORE TABLE " + table +
                  "_Meta ADD UNIQUE INDEX `index12` (md5)");
            q.executeUpdate();
            // Clear up other tables.
            if (1 == getCount("select count(*) from information_schema.tables where " +
                              "table_schema = Database() and table_name = " +
                              dq + table + "_Xml" + dq)) {
                // Table is optional, but clean it up if we have it.
                q = session.createSQLQuery("DELETE X FROM " + table + "_Xml " +
                                           "X LEFT JOIN " + table + "_Meta M " +
                                           "ON (X.dbid = M.dbid) " +
                                           "WHERE M.dbid IS NULL");
                q.executeUpdate();
            }
            q = session.createSQLQuery("DELETE X FROM " + table +
                  " X LEFT JOIN " + table + "_Meta M " +
                  "ON (X.dbid = M.dbid) " +
                  "WHERE M.dbid IS NULL");
            q.executeUpdate();
         }
         tx.commit();
         session.close();
      } catch (Exception e) {
         HibernateWrapper.closeSession( session );
         throw e;
      }
   } 
} 

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
    static final int gratiaDatabaseVersion = 11;

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
				}	else {
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
    
     public String GetJobUsageRecordColumns() {
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
                if (column.equals("SiteName")) {
                    column = "Site.SiteName";
                } else if (column.equals("Status")) {
                    column = "JobUsageRecord.Status";
                } else if (column.equals("dbid")) {
                    column = "JobUsageRecord_Meta.dbid";
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

        //
        // place holder to initialize SystemProplist
        //
        Properties p = net.sf.gratia.services.Configuration.getProperties();

        // String gratiaVersion = p.getProperty("gratia.version");
        // Deprecated String gratiaDatabaseVersion = p.getProperty("gratia.database.version");
        String useReportAuthentication = p.getProperty("use.report.authentication");

        if (0 == Execute("update SystemProplist set cdr = " + dq
                + useReportAuthentication + dq + " where car = " + dq
                + "use.report.authentication" + dq)) {
            Execute("insert into SystemProplist(car,cdr) values(" + dq
                    + "use.report.authentication" + dq + comma + dq
                    + useReportAuthentication + dq + ")");
        }
    }
    
    public void AddViews() {
        Execute("DROP VIEW IF EXISTS CETable;");
        Execute("CREATE VIEW CETable AS select Site.SiteId AS facility_id,Site.SiteName AS facility_name from Site;");
        Execute("DROP VIEW IF EXISTS CEProbes;");
        Execute("CREATE VIEW CEProbes AS select probeid,siteid as facility_id," +
                "probename,active,currenttime,CurrentTimeDescription,reporthh," +
                "reportmm,status," + FindRecordsColumn() + " as jobs from Probe;");
        Execute("DROP VIEW IF EXISTS JobUsageRecord_Report;");
        Execute("CREATE VIEW JobUsageRecord_Report as select "+GetJobUsageRecordColumns()+
		        " ,JobUsageRecord_Meta.ProbeName " +
                " from JobUsageRecord_Meta,Site,Probe,JobUsageRecord " +
                " where " +
                " JobUsageRecord_Meta.ProbeName = Probe.probename and Probe.siteid = Site.siteid" +
                " and JobUsageRecord_Meta.dbid = JobUsageRecord.dbid;");
    }
    
    public void ReadLiveVersion() {

        Statement statement;
        ResultSet resultSet;

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
    
    public void UpdateDbVersion(int newVersion) {
        Execute("update SystemProplist set cdr = " + dq + newVersion + dq
                + " where car = " + dq + "gratia.database.version" + dq);

        liveVersion = newVersion;        
    }

    public boolean Upgrade() {
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


						CallPostInstall("all"); // Need some DB root-user action
						
            Logging.log("Gratia database now at version "
												+ gratiaDatabaseVersion);

            return true;
            
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
										result = CallPostInstall("trigger");
										result = CallPostInstall("summary");
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
						if (current > 6) {
								int result = CallPostInstall("stored");
								if (result > -1) {
                    Logging.log("Gratia database refreshed stored procedures.");
								} else {
										Logging.log("Gratia database FAILED to refresh stored procedures.");
								}
						}
            if (current == 7) {
                int result = Execute("insert into VONameCorrection(VOName,ReportableVOName) select distinct binary VOName,ReportableVOName from JobUsageRecord");
                if (result > -1) {
                    result = Execute("insert into VO(VOName) select distinct VOName from VONameCorrection");
                }
                if (result > -1) {
                    result = Execute("update VONameCorrection,VO set VONameCorrection.VOid=VO.VOid where VONameCorrection.VOName = VO.VOName");
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
            return current == gratiaDatabaseVersion;
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
														+ dq	+ "gratia.database.version" + dq + " and PropId > " + propid);
								}
            }
            Logging.log("Command: OK: " + check);
        } catch (Exception e) {
            Logging.log("Command: Error: " + check + " : " + e);
        }        
    }
}

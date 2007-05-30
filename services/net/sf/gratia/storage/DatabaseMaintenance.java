package net.sf.gratia.storage;

import java.sql.*;
import net.sf.gratia.services.Logging;
import java.util.Properties;

public class DatabaseMaintenance 
{
    java.sql.Connection connection;
    static final String dq = "\"";
    static final String comma = ",";

    static final int gratiaDatabaseVersion = 4;

    public DatabaseMaintenance(java.sql.Connection con) 
    {
        connection = con;
    }

    public void AddIndex(String table, Boolean unique, String name, String content) 
    {
        Statement statement;
        ResultSet resultSet;

        String check = "show index from " + table + " where Key_name = '" + name + "'";
        String cmd = "alter table " + table + " add ";
        if (unique) {
          cmd = cmd + "unique ";
        }
        cmd = cmd + "index " + name + "(" + content + ")";
        try {
            Logging.log("Executing: " + check);
            statement =  connection.createStatement();
            resultSet = statement.executeQuery(check);
            if (!resultSet.next()) {
              // No index yet 
              Logging.log("Executing: " + cmd);
              statement = connection.createStatement();
              statement.executeUpdate(cmd);
              Logging.log("Command: OK: " + cmd);
            }
        }
        catch (Exception e)
        {
            Logging.log("Command: Error: " + cmd + " : " + e);
        }
    }

    public void DropIndex(String table, String name) 
    {
        Statement statement;
        ResultSet resultSet;

        String check = "show index from " + table + " where Key_name = '" + name + "'";
        String cmd = "alter table " + table + " drop index "+name;
        try {
            Logging.log("Executing: " + check);
            statement =  connection.createStatement();
            resultSet = statement.executeQuery(check);
            if (resultSet.next()) {
              // Index still there 
              Logging.log("Executing: " + cmd);
              statement = connection.createStatement();
              statement.executeUpdate(cmd);
              Logging.log("Command: OK: " + cmd);
            }
        }
        catch (Exception e)
        {
            Logging.log("Command: Error: " + cmd + " : " + e);
        }
    }

    public void CheckIndices() {
      AddIndex("CETable",true,"index02","facility_name");
      AddIndex("CEProbes",true,"index02","probename");

      //
      // the following were added to get rid of unused indexes
      //
      DropIndex("JobUsageRecord","index04");
      DropIndex("JobUsageRecord","index06");
      DropIndex("JobUsageRecord","index07");
      DropIndex("JobUsageRecord","index09");
      DropIndex("JobUsageRecord","index10");
      //
      // original index structure
      //
      AddIndex("JobUsageRecord",false,"index02","EndTime");
      AddIndex("JobUsageRecord",false,"index03","ProbeName");
      //AddIndex("JobUsageRecord",false,"index04","HostDescription");
      AddIndex("JobUsageRecord",false,"index05","StartTime");
      //AddIndex("JobUsageRecord",false,"index06","GlobalJobid");
      //AddIndex("JobUsageRecord",false,"index07","LocalJobid");
      AddIndex("JobUsageRecord",false,"index08","Host(255)");
      AddIndex("JobUsageRecord",false,"index11","ServerDate");
      AddIndex("JobUsageRecord",true,"index12","md5");
      AddIndex("JobUsageRecord",false,"index13","ServerDate");

        //
        // new indexes for authentication
        //
      AddIndex("JobUsageRecord",false,"index14","VOName");
      AddIndex("JobUsageRecord",false,"index15","CommonName");
        
        //
      AddIndex("Security",true,"index02","alias");

        //
        // New index for ResourceType
        //
      AddIndex("JobUsageRecord",false,"index16","ResourceType");
        
    }

    public int Execute(String cmd) 
    {
        Statement statement;
        int result = 0;

        try {
            Logging.log("Executing: " + cmd);
            statement = connection.createStatement();
            result = statement.executeUpdate(cmd);
            Logging.log("Command: OK: " + cmd);
        }
        catch (Exception e)
        {
            Logging.log("Command: Error: " + cmd + " : " + e);
	    result = -1;
        }
        return result;
    }
   
    public void SiteDefaults() 
    {
        Statement statement;
        ResultSet resultSet;

        String check = "select count(*) from CETable where facility_name = 'Unknown'";
        String cmd = "insert into CETable(facility_name) values(" + dq + "Unknown" + dq + ")";

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
        }
        catch (Exception e)
        {
            Logging.log("Command: Error: " + cmd + " : " + e);
        }
   }

   public void CPUMetricDefaults() 
   {
        Execute("delete from CPUMetricTypes");
        Execute("insert into CPUMetricTypes(CPUMetricType) values(" + dq + "wallclock" + dq + ")");
        Execute("insert into CPUMetricTypes(CPUMetricType) values(" + dq + "process" + dq + ")");
   }



    public void AddDefaults()
    {
        SiteDefaults();
        CPUMetricDefaults();

        //
        // place holder to initialize SystemProplist
        //
        Properties p = net.sf.gratia.services.Configuration.getProperties();

        String gratiaVersion = p.getProperty("gratia.version");
        String gratiaDatabaseVersion = p.getProperty("gratia.database.version");
        String useReportAuthentication = p.getProperty("use.report.authentication");

        if (0==Execute("update SystemProplist set cdr = " + dq + useReportAuthentication + dq + " where car = " +  dq + "use.report.authentication" + dq ) )
        {
            Execute("insert into SystemProplist(car,cdr) values(" + dq + "use.report.authentication" + dq + comma + dq + useReportAuthentication + dq + ")");
        }
    }

    public void Upgrade()
    {
        int oldvers = 0;

        Statement statement;
        ResultSet resultSet;

        String check = "select cdr from SystemProplist where car = " + dq + "gratia.database.version" + dq;

        try {
            Logging.log("Executing: " + check);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(check);
            if (resultSet.next()) {
                String vers = resultSet.getString(1);
                if (vers.equals("1.0.0")) 
                {
                    oldvers = 1;
                }
                else 
                {        
                    try {
			oldvers = Integer.valueOf( vers ).intValue();
		    } catch (Exception e) {
			oldvers = 1;
		    }
                }
            }
            Logging.log("Command: OK: " + check);
        }
        catch (Exception e)
        {
            Logging.log("Command: Error: " + check + " : " + e);
        }

        if (oldvers == 0) {

           Execute("insert into SystemProplist(car,cdr) values(" + dq + "gratia.database.version" + dq + comma + dq + gratiaDatabaseVersion + dq + ")");
           Logging.log("Gratia database now at version "+gratiaDatabaseVersion);

        } else {
            // Do the necessary upgrades if any
	    
	    Logging.log("Gratia database at version "+oldvers);
	    
	    int current = oldvers;
	    if (current == 1) {
		Logging.log("Gratia database upgraded from "+current+" to "+ (current + 1));
		current = current + 1;
	    }
            if (current == 2) {
		// Upgrade to version 3;
		if (oldvers < 2 ) {
		    // Then MetricRecord_Meta never contained the Xml fields, nothing to do.
		} else {
		    int result = Execute("insert into MetricRecord_Xml(dbid,RawXml,ExtraXml) select dbid,RawXml,ExtraXml from MetricRecord_Meta");
		    if (result>-1) {
			result = Execute("alter table MetricRecord_Meta drop column RawXml, drop column ExtraXml");
		    }
		    if (result>-1) {
			Logging.log("Gratia database upgraded from "+current+" to "+ (current + 1));
			current = current + 1;
		    } else {
			Logging.log("Gratia database FAILED to upgrade from "+current+" to "+ (current + 1));
		    }
		}
	    }
	    if (current == 3) {
                int result = Execute("insert into JobUsageRecord_Xml(dbid,RawXml,ExtraXml) select dbid,RawXml,ExtraXml from JobUsageRecord");
		if (result>-1) {
		    result = Execute("alter table JobUsageRecord drop column RawXml, drop column ExtraXml");
		}
		if (result>-1) {
		    // move md5, ServerDate, SiteName, SiteNameDescription, ProbeName, ProbeNameDescription,
		    // recordId,CreateTime.CreateTimeDescription,RecordKeyInfoId,RecordKeyInfoContent
		    result = Execute("insert into JobUsageRecord_Meta(dbid,md5, ServerDate, SiteName, SiteNameDescription, ProbeName, ProbeNameDescription,recordId,CreateTime,CreateTimeDescription,RecordKeyInfoId,RecordKeyInfoContent) select dbid, md5, ServerDate, SiteName, SiteNameDescription, ProbeName, ProbeNameDescription,recordId,CreateTime,CreateTimeDescription,RecordKeyInfoId,RecordKeyInfoContent from JobUsageRecord");
		}
		if (result>-1) {
		    result = Execute("alter table JobUsageRecord drop column md5, drop column ServerDate, drop column SiteName, drop column SiteNameDescription, drop column ProbeName, drop column ProbeNameDescription, drop column recordId, drop column CreateTime, drop column CreateTimeDescription, drop column RecordKeyInfoId, drop column RecordKeyInfoContent ");
		}
		if (result>-1) {
		    Logging.log("Gratia database upgraded from "+current+" to "+ (current + 1));
		    current = current + 1;
		} else {
		    Logging.log("Gratia database FAILED to upgrade from "+current+" to "+ (current + 1));
		}
	    }	    

            Execute("update SystemProplist set cdr = " + dq + current + dq + " where car = " +  dq + "gratia.database.version" + dq );
        }
    }
}


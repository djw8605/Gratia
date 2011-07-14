package net.sf.gratia.reporting;

//import java.io.*;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;

public class ReportingConfiguration
{
	HttpServletRequest request = null;
	Properties p = net.sf.gratia.util.Configuration.getProperties();
	private String _configLoaded = null;
	private String _databaseURL = null;
	private String _databaseUser = null;
	private String _databasePassword = null;
	private String _reportsFolder = null;
	private String _reportsMenuConfig = null;
	private String _csvHome = null;
	private String _reportingVersion = null;
	private String _staticFolder = null;
	private String _staticFolderPath = null;
	private String _statReportsConfig = null;
	private boolean _logging = false;

	public String getConfigLoaded() {
		return _configLoaded;
	}
	
	public String getDatabaseURL() {
		return _databaseURL;
	}

	public String getDatabaseUser() {
		return _databaseUser;
	}
	
	public String getDatabasePassword() {
		return _databasePassword;
	}

	public String getReportsFolder() {
		return _reportsFolder;
	}

	public String getReportsMenuConfig() {
		return _reportsMenuConfig;
	}

	public String getCsvHome() {
		return _csvHome;
	}

	public String getReportingVersion() {
		return _reportingVersion;
	}

	public String getStaticFolder() {
		return _staticFolder;
	}

	public String getStaticFolderPath() {
		return _staticFolderPath;
	}

	public String getStaticReportsConfig() {
		return _statReportsConfig;
	}

	public boolean getLogging() {
		return _logging;
	}

  //-----------------------------------------------
	public boolean doesPropertyExist(String property) {
	  boolean propertyExists = true;
    String value = null;
	  value = p.getProperty(property);
    if ( value == null ) {
      propertyExists = false;
    }
		return propertyExists;
	}
  //-----------------------------------------------
	public String getPropertyValue(String property) {
	  String value = p.getProperty(property);
    if ( value == null ) {
      value = "false";
    }
		return value;
	}

	public void loadReportingConfiguration(javax.servlet.http.HttpServletRequest request)
	{
		this.request = request;
		Properties p = net.sf.gratia.util.Configuration.getProperties();

		// The special key 'configLoaded' is set when the configuration has been already loaded
		if (_configLoaded == null)
		{
			try
			{
				String webappsHome =  System.getProperty("catalina.home") + "/" + "webapps" + "/";

				_reportsFolder     = (webappsHome + p.getProperty("service.reporting.reports.folder") + "/");
				_reportsMenuConfig = (webappsHome + p.getProperty("service.reporting.menuconfig"));
				_statReportsConfig = (webappsHome + p.getProperty("service.reporting.staticreports"));
				_staticFolderPath  = (webappsHome + p.getProperty("service.reporting.static.folder") + "/");
				_staticFolder      = p.getProperty("service.reporting.static.folder");

				_logging = false;
				String logging = p.getProperty("service.reporting.logging");
				if (logging != null)
				{
					if (logging.trim().equalsIgnoreCase("true"))
						_logging = true;
				}

				if (_staticFolder != null)
				{
					if (_staticFolder.charAt(_staticFolder.length()-1) != '/')
						_staticFolder = _staticFolder + "/";
					if (_staticFolder.charAt(0) != '/')
						_staticFolder = "/" + _staticFolder;
				}

				if (System.getProperty( "os.name" ).trim().equalsIgnoreCase("Windows"))
					_csvHome = "./";
				else
					_csvHome       = (webappsHome + "gratia_csv_temp" + "/");

				_databaseURL       = p.getProperty("service.mysql.url");
				_databaseUser      = p.getProperty("service.reporting.user");
				_databasePassword  = p.getProperty("service.reporting.password");
				_staticFolder      = p.getProperty("service.reporting.static.folder");

				_reportingVersion  = net.sf.gratia.reporting.Versions.GetPackageVersionString();

	 // Set a flag indicating the configuration has been loaded, so subsequent calls will not load again
				 _configLoaded = "1";

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

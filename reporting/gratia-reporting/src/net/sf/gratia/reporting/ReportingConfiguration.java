package net.sf.gratia.reporting;

import java.io.*;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;

public class ReportingConfiguration 
{
	HttpServletRequest request = null;
	Properties p = net.sf.gratia.util.Configuration.getProperties();
	private String _configLoaded = null;
	private String _reportsFolder = null;
	private String _databaseURL = null;
	private String _databaseUser = null;
	private String _databasePassword = null;
	private String _reportsMenuConfig = null;
	private String _csvHome = null;
	private String _reportingVersion = null;
	private String _staticFolder = null;
	private String _staticFolderPath = null;
	
	public String getDatabasePassword() {
		return _databasePassword;
	}

	public String getDatabaseURL() {
		return _databaseURL;
	}

	public String getDatabaseUser() {
		return _databaseUser;
	}

	public String getReportsFolder() {
		return _reportsFolder;
	}

	public String getReportsMenuConfig() {
		return _reportsMenuConfig;
	}
	
	public String getConfigLoaded() {
		return _configLoaded;
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
		
	public void loadReportingConfiguration(javax.servlet.http.HttpServletRequest request)
	{		
		this.request = request;
		Properties p = net.sf.gratia.util.Configuration.getProperties();

		// The special key 'configLoaded' is set when the configuration has been already loaded
		   if (_configLoaded == null)
		   {			
			try
			{
				String webappsHome =  System.getProperty("catalina.home") + File.separatorChar+ "webapps" + File.separatorChar;
				
				_reportsFolder     = (webappsHome + p.getProperty("service.reporting.reports.folder").replace("/", File.separator) + File.separatorChar);
				_reportsMenuConfig = (webappsHome + p.getProperty("service.reporting.menuconfig").replace("/", File.separator));
				_staticFolderPath  = (webappsHome + p.getProperty("service.reporting.static.folder") + File.separatorChar);				
				_staticFolder      = p.getProperty("service.reporting.static.folder");
				if (_staticFolder.substring(_staticFolder.length()-1, _staticFolder.length()) != "/")
					_staticFolder = _staticFolder + "/";
				if (_staticFolder.substring(0, 1) != "/")
					_staticFolder = "/" + _staticFolder;
				
				_csvHome           = (webappsHome + "birt_csv_temp" + File.separatorChar);
				
				_databaseURL       =  p.getProperty("service.mysql.url");
				_databaseUser      = p.getProperty("service.reporting.user");
				_databasePassword  = p.getProperty("service.reporting.password");				
				_staticFolder      = p.getProperty("service.reporting.static.folder");

				_reportingVersion  = p.getProperty("gratia.reporting.version");
		   	
	 // Set a flag indicating the configuration has been loaded, so subsequent calls will not load again
		   		 _configLoaded = "1";

		   	} catch (Exception e) {
				e.printStackTrace();
			}
		   }		
	}

}


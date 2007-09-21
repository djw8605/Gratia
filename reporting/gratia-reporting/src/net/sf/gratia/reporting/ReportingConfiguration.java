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
	private String _engineHome = null;
	private String _webappHome = null;
	private String _databaseURL = null;
	private String _databaseUser = null;
	private String _databasePassword = null;
	private String _reportsMenuConfig = null;
	private String _logsHome = null;
	private String _csvHome = null;
	
	public String getDatabasePassword() {
		return _databasePassword;
	}

	public String getDatabaseURL() {
		return _databaseURL;
	}

	public String getDatabaseUser() {
		return _databaseUser;
	}

	public String getEngineHome() {
		return _engineHome;
	}

	public String getReportsFolder() {
		return _reportsFolder;
	}

	public String getWebappHome() {
		return _webappHome;
	}

	public String getReportsMenuConfig() {
		return _reportsMenuConfig;
	}
	
	public String getConfigLoaded() {
		return _configLoaded;
	}
	
	public String getLogsHome() {
		return _logsHome;
	}
	
	public String getCsvHome() {
		return _csvHome;
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
				_engineHome        = (webappsHome + p.getProperty("service.reporting.engine.home") + File.separatorChar);
				_webappHome        = (webappsHome + p.getProperty("service.reporting.webapp.home") + File.separatorChar);
				_logsHome          = (webappsHome + "birt_logs" + File.separatorChar);	
				_csvHome           = (webappsHome + "birt_csv_temp" + File.separatorChar);
				
				_databaseURL =  p.getProperty("service.mysql.url");
				_databaseUser = p.getProperty("service.reporting.user");
				_databasePassword = p.getProperty("service.reporting.password");
		   	
	 // Set a flag indicating the configuration has been loaded, so subsequent calls will not load again
		   		 _configLoaded = "1";

		   	} catch (Exception e) {
				e.printStackTrace();
			}
		   }		
	}

	
}


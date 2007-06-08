package net.sf.gratia.reporting;

import java.io.*;
import java.util.Iterator;
import java.util.Properties;

//import java.util.logging.Level;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
//import org.eclipse.birt.report.engine.api.EngineConfig;
//import org.eclipse.birt.report.engine.api.ReportEngine;
//import org.eclipse.birt.report.model.api.DesignEngine;
//import org.eclipse.birt.report.model.api.SessionHandle;

import net.sf.gratia.reporting.exceptions.InvalidConfigurationException;

public class ReportingConfiguration 
{
	private String _configLoaded = null;
	private String _reportsFolder = null;
	private String _engineHome = null;
	private String _webappHome = null;
	private String _databaseURL = null;
	private String _databaseUser = null;
	private String _databasePassword = null;
	private String _reportsMenuConfig = null;


	//private static EngineConfig _reportEngineConfig = null;
	//private static ReportEngine _reportEngine = null;
	
	//private static SessionHandle _designSession = null;
	
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
	
	/*
	public static EngineConfig getReportEngineConfig() {
		if(_reportEngineConfig == null)
		{
			_reportEngineConfig = new EngineConfig();
			_reportEngineConfig.setEngineHome( getEngineHome() );
			_reportEngineConfig.setLogConfig("", Level.SEVERE);
		}
		
		return _reportEngineConfig;
	}	
	
	public static ReportEngine getReportEngine() {
		if(_reportEngine == null)
		{
			_reportEngine = new ReportEngine(getReportEngineConfig());		    
		}
		
		return _reportEngine;
	}	
	
	public static SessionHandle getDesignSession() {
		if(_designSession == null)
		{
			_designSession = DesignEngine.newSession( null );
		}
		
		return _designSession;
	}
	*/
	
	public void loadReportingConfiguration(javax.servlet.http.HttpServletRequest request)
	{		
		
		Properties p = net.sf.gratia.util.Configuration.getProperties();

		// The special key 'configLoaded' is set when the configuration has been already loaded
		   if (_configLoaded == null)
		   {			
			try
			{
				String catalinaHome = System.getProperty("catalina.home");
				
				_reportsFolder = (catalinaHome + File.separator + "webapps" + File.separator + p.getProperty("service.reporting.reports.folder") + File.separator);
				_engineHome = (catalinaHome + File.separator + "webapps" + File.separator + p.getProperty("service.reporting.engine.home") + File.separator);
				_webappHome = (catalinaHome + File.separator + "webapps" + File.separator + p.getProperty("service.reporting.webapp.home") + File.separator);		        	
			 
				_databaseURL =  p.getProperty("service.mysql.url");
				_databaseUser = p.getProperty("service.reporting.user");
				_databasePassword = p.getProperty("service.reporting.password");
				_reportsMenuConfig = p.getProperty("service.reporting.menuconfig");			        	       	
		   
			    				
	 // Set a flag indicating the configuration has been loaded, so subsequent calls will not load again
		   		 _configLoaded = "1";

		   	}

		   	catch(Exception ignore)
			{
			}
		   }		
	}
	
}

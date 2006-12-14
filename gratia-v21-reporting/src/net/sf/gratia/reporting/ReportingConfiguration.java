package net.sf.gratia.reporting;

import java.io.File;
import java.util.Iterator;
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
		throws InvalidConfigurationException
	{		
		// The special key 'configLoaded' is set when the configuration has been already loaded
		if(_configLoaded == null)
		{			
			SAXReader saxReader = new SAXReader();        
		    Document doc = null;    
		    File source = null;
		    
			try
			{
				// Open the config file
				source = new File(request.getRealPath("/") + "../gratia-report-configuration/ReportingConfig.xml");
								
				// Parse the configuration file
			    doc = saxReader.read(source); 
			   			    
			    // Loop through each element in the configuration file under the root element
			    for (Iterator i = doc.getRootElement().elementIterator(); i.hasNext(); ) 
			    {
			        Element element = (Element) i.next();
			        
			        // Set the appropriate values based on the name of this element
			        if(element.getName().equals("PathConfig"))
			        {
			        	_reportsFolder = getAttributeValue(element, "reportsFolder");
			        	_engineHome = getAttributeValue(element, "engineHome");
			        	_webappHome = getAttributeValue(element, "webappHome");		        	
			        }
			        else if(element.getName().equals("DataSourceConfig"))
			        {
			        	_databaseURL = getAttributeValue(element, "url");
			        	_databaseUser = getAttributeValue(element, "user");
			        	_databasePassword = getAttributeValue(element, "password");		        	       	
			        }		        
			        else
			        	throw new InvalidConfigurationException("Unknown config node named '" + element.getName() + "'");
			    } // Loop through each element in the configuration file under the root element
			    				
			    // Set a flag indicating the configuration has been loaded, so subsequent calls will not load again
			    _configLoaded = "1";
			}
			catch(DocumentException exDoc)
			{
				throw new InvalidConfigurationException("Unable to parse Gratia Reporting configuration"+exDoc.getMessage(), exDoc);
			}
			finally
			{				
				doc = null;
			    saxReader = null;
			    source = null;
			}
		}		
	}
	
	private String getAttributeValue(Element element, String attributeName)
		throws InvalidConfigurationException
	{
		if(element.attribute(attributeName) == null)
			throw new InvalidConfigurationException(attributeName + " is a required attribute of " + element.getName() + ", but it was not found.");
		
		String attributeValue = element.attribute(attributeName).getValue();
				
		return attributeValue;
	}
}

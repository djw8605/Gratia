package net.sf.gratia.reporting;

import java.io.*;
import java.lang.System;
import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import net.sf.gratia.reporting.exceptions.InvalidConfigurationException;

public class UserConfiguration 
{
	private String _configLoaded = null;
	private ArrayList _menuGroups = null;
	private ArrayList _dashboardRows = null;		
	
	public ArrayList getMenuGroups() {
		return _menuGroups;
	}
	
	public ArrayList getDashboardRows() {
		return _dashboardRows;
	}
	
	public void loadUserConfiguration(javax.servlet.http.HttpServletRequest request)
		throws InvalidConfigurationException
	{			
		ReportingConfiguration reportingConfiguration = (ReportingConfiguration)request.getSession().getAttribute("reportingConfiguration");
		
		// The special key 'configLoaded' is set when the configuration has been already loaded
		if(_configLoaded == null)
		{			
			SAXReader saxReader = new SAXReader();        
			Document doc = null;    
			File source = null;
			String menuConfig = null;

			try 
			{
				menuConfig = reportingConfiguration.getReportsMenuConfig();
			}
			catch (SecurityException exSec) 
			{
				// Continue if we get an exception here
			}

			if (menuConfig == null) 
			{
				String catalinaHome =  System.getProperty("catalina.home") + File.separatorChar;
				menuConfig = (catalinaHome + "webapps" + File.separatorChar + "gratia-reports" + "MenuConfig"+ File.separator + "UserConfig_osg.xml");
			}
			
			try 
			{	
					// Open the config file
				
				String reportsFolder = reportingConfiguration.getReportsFolder();

				source = new File(menuConfig);
							
					// Parse the configuration file
				doc = saxReader.read(source); 
			   	
					// Loop through each element in the configuration file under the root element
				for (Iterator i = doc.getRootElement().elementIterator(); i.hasNext(); ) 
				{
					Element element = (Element) i.next();
			        
						// Set the appropriate values based on the name of this element
					if(element.getName().equals("Menu"))
					{
						_menuGroups = new ArrayList();
			        		for (Iterator menuGroupIterator = element.elementIterator(); menuGroupIterator.hasNext();)
						{
							Element ndeMenuGroup = (Element) menuGroupIterator.next();
			        			MenuGroup newMenuGroup = new MenuGroup(getAttributeValue(ndeMenuGroup, "name"));
			        			for (Iterator menuItemIterator = ndeMenuGroup.elementIterator(); menuItemIterator.hasNext();)
							{
								Element ndeMenuItem = (Element) menuItemIterator.next();
								String link = getAttributeValue(ndeMenuItem, "link").replace("[ReportsFolder]", reportsFolder);	//replaceAll("\\[ReportsFolder\\]", reportsFolder) ));		        							
								newMenuGroup.getMenuItems().add(new MenuItem(getAttributeValue(ndeMenuItem, "name"), link)); 
							}
			        			
							_menuGroups.add(newMenuGroup);
						}
					}
					else if(element.getName().equals("Dashboard"))
					{
						_dashboardRows = new ArrayList();
			        		for (Iterator rowIterator = element.elementIterator(); rowIterator.hasNext();)
						{
							Element ndeRow = (Element) rowIterator.next();
			        			DashboardRow newRow = new DashboardRow();
			        			for (Iterator dashboardItemIterator = ndeRow.elementIterator(); dashboardItemIterator.hasNext();)
							{
								Element ndeDashboardItem = (Element) dashboardItemIterator.next();
								String link = getAttributeValue(ndeDashboardItem, "link").replace("[ReportsFolder]", reportsFolder);	//replaceAll("\\[ReportsFolder\\]", reportsFolder) ));		        							
			        			newRow.getDashboardItems().add(new DashboardItem(link)); //replaceAll("\\[ReportsFolder\\]", reportsFolder)));
							}
							
							_dashboardRows.add(newRow);
						}
					}
					else
						throw new InvalidConfigurationException("Unknown config node named '" + element.getName() + "'");
				} // Loop through each element in the configuration file under the root element
			    				
		// Set a flag indicating the configuration has been loaded, so subsequent calls will not load again
				_configLoaded = "1";

			} // try
			catch(DocumentException exDoc)
			{
				throw new InvalidConfigurationException("Unable to parse User configuration ", exDoc);
			}
			finally
			{				
				doc = null;
				saxReader = null;
				source = null;
			}
		} // if(_configLoaded == null)
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

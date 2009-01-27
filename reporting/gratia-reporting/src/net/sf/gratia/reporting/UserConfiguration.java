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
      
      public ArrayList getMenuGroups() {
         return _menuGroups;
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
               String catalinaHome =  System.getProperty("catalina.home") + "/";
               menuConfig = (catalinaHome + "webapps" + "/" + "gratia-reports" + "MenuConfig" + "/" + "UserConfig_osg.xml");
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
                        
                        // Determine if this is a conditional menu item based on a property in the configuration file
                        if ( (boolean) includeMenuItem(ndeMenuGroup, reportingConfiguration) == false ) {
                           continue; // skip this MenuGroup
                        }
                        
                        for (Iterator menuItemIterator = ndeMenuGroup.elementIterator(); menuItemIterator.hasNext();)
                        {
                           Element ndeMenuItem = (Element) menuItemIterator.next();
                           String name = getAttributeValue(ndeMenuItem, "name");
                           String display = getAttributeValue(ndeMenuItem, "display");
                           String link = "false";
                           String linkProperty = getAttributeValue(ndeMenuItem, "linkProperty");
                           if ( linkProperty.equals("false") ) {
                              link = getAttributeValue(ndeMenuItem, "link").replace("[ReportsFolder]", reportsFolder);
                           } else {
                              link = reportingConfiguration.getPropertyValue("service.open.connection") + "/" + reportingConfiguration.getPropertyValue(linkProperty) + "/index.html";
                           }
                           newMenuGroup.getMenuItems().add(new MenuItem(name, link, display));
                        }
                        
                        _menuGroups.add(newMenuGroup);
                     }
                  }
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
      // --------------------------------------------
      private String getAttributeValue(Element element, String attributeName)
		throws InvalidConfigurationException
      {
         String attributeValue = "";
         if(element.attribute(attributeName) == null)
            attributeValue = "false";
         else
            attributeValue = element.attribute(attributeName).getValue();
         
         return attributeValue;
      }
      // --------------------------------------------
      private boolean includeMenuItem(Element element, ReportingConfiguration reportingConfiguration)
		throws InvalidConfigurationException
      {
         boolean menuItem = true;
         String type = getAttributeValue(element, "type");
         if ( type.equals("conditional") ) {
            String property = getAttributeValue(element, "property");
            if ( property.equals("false") ) {
               throw new InvalidConfigurationException("Conditional menu item found with no property attribute");
            }
            boolean exists = reportingConfiguration.doesPropertyExist(property);
            if ( exists == false ) {
               menuItem = false;
            }
         }
         return menuItem;
      }
   }

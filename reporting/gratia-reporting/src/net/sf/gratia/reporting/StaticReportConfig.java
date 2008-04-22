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

public class StaticReportConfig
{
	private String _staticConfigLoaded = null;
	private ArrayList _statReportGroups = null;

	public ArrayList getStatReportGroups() {
		return _statReportGroups;
	}

	public void loadStaticReportConfig(javax.servlet.http.HttpServletRequest request)
		throws InvalidConfigurationException
	{
		ReportingConfiguration reportingConfiguration = (ReportingConfiguration)request.getSession().getAttribute("reportingConfiguration");

		// The special key 'configLoaded' is set when the configuration has been already loaded
		if(_staticConfigLoaded == null)
		{
			SAXReader saxReader = new SAXReader();
			Document doc = null;
			File source = null;
			String statReportConfig = null;

			try
			{
				statReportConfig = reportingConfiguration.getStaticReportsConfig();
			}
			catch (SecurityException exSec)
			{
				// Continue if we get an exception here
			}

			if (statReportConfig == null)
			{
				String catalinaHome =  System.getProperty("catalina.home") + "/";
				statReportConfig = (catalinaHome + "webapps" + "/" + "gratia-reports" + "/"  + "MenuConfig" + "/" + "StaticReports.xml");
			}

			try
			{
					// Open the config file

				String reportsFolder = reportingConfiguration.getReportsFolder();
				String staticFolder = reportingConfiguration.getStaticFolder();

				source = new File(statReportConfig);

					// Parse the configuration file
				doc = saxReader.read(source);

					// Loop through each element in the configuration file under the root element
				for (Iterator i = doc.getRootElement().elementIterator(); i.hasNext(); )
				{
					Element ndeStatReportGroup = (Element) i.next();

						// Set the appropriate values based on the name of this element
					if(ndeStatReportGroup.getName().equals("StaticReportGroup"))
					{
						_statReportGroups = new ArrayList();

						StaticReportGroup newStatReportGroup = new StaticReportGroup(getAttrValue(ndeStatReportGroup, "name"));
							
						for (Iterator statReportItemIterator = ndeStatReportGroup.elementIterator(); statReportItemIterator.hasNext();)
						{
							Element ndeStatReportItem = (Element) statReportItemIterator.next();
							String name = getAttrValue(ndeStatReportItem, "name");
							String link = getAttrValue(ndeStatReportItem, "link");
							String report = "";
							if (ndeStatReportItem.getName().equals("reportItem") )
							{
								report = link.substring(link.indexOf("]")+1, link.indexOf(".rptdesign"));
								link = link.replace("[ReportsFolder]", reportsFolder);
							}
							else if (ndeStatReportItem.getName().equals("csvItem") )
							{
								report = name.replaceAll(" ", "");
								link = staticFolder + "/" + report + ".csv";
							}

							newStatReportGroup.getStatItems().add(new StaticReportItem(name, report, link));
						}
						_statReportGroups.add(newStatReportGroup);
					}
				} // Loop through each element in the configuration file under the root element

					// Set a flag indicating the configuration has been loaded, so subsequent calls will not load again
				_staticConfigLoaded = "1";
			} // try
			catch(DocumentException exDoc)
			{
				throw new InvalidConfigurationException("Unable to parse Static Reports configuration ", exDoc);
			}
			catch (Exception ex){}
			finally
			{
				doc = null;
				saxReader = null;
				source = null;
			}
		} // if(_configLoaded == null)
	}

	private String getAttrValue(Element element, String attributeName)
		throws InvalidConfigurationException
	{
		String attributeValue = "";
		if(element.attribute(attributeName) == null)
			attributeValue = "";
		else
			attributeValue = element.attribute(attributeName).getValue();

		return attributeValue;
	}
}

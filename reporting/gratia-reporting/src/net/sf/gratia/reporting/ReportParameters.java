package net.sf.gratia.reporting;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import net.sf.gratia.reporting.exceptions.InvalidConfigurationException;

public class ReportParameters
{
	private ArrayList _paramGroups = null; 

	public ArrayList getParamGroups () { 
		return _paramGroups;
	}

	public void loadReportParameters(String report)
		throws InvalidConfigurationException
	{
		SAXReader saxReader = new SAXReader();
		Document doc = null;
		File source = null;


		if (report == null || report.length() == 0)
		{
			throw new InvalidConfigurationException("A Report name has not been specified");
		}

		try
		{
			// Open the Report file and read the report file
			source = new File(report);
			doc = saxReader.read(source);

			 // iterate through all child elements of root
			    for (Iterator i = doc.getRootElement().elementIterator(); i.hasNext(); )
			    {
				Element element = (Element) i.next();
			 	String elementName = element.getName();

			// Expand the "parameters" elements and gather the information to build the ArrayLists
			 	if (elementName == "parameters")
			 	{
			 		_paramGroups = new ArrayList(); // we have the parameters group
			 		for (Iterator pIterator = element.elementIterator(); pIterator.hasNext();)
					{
					   Element pType = (Element) pIterator.next();
					   String paramType = pType.getName().trim();

			// TO DO: if this is a parameter-group, then the parameters are in deeper levels. Ignore for the time being...
					   if (paramType == "parameter-group")
					   {
					      for (Iterator gIterator = pType.elementIterator(); gIterator.hasNext();)
					      {
					         Element groupType = (Element) gIterator.next();
					         for (Iterator ggIter = groupType.elementIterator(); ggIter.hasNext();)
					         {
					            Element ggroupType = (Element) ggIter.next();
					            String ggType = ggroupType.getName();
					            String ggName = ggroupType.getText();
					            String gparamName = ggroupType.attribute("name").getValue();

					            if  (ggType == "scalar-parameter")
					            {
					              // do the rest ....
					            }
					         }
					      }
					   }

					   String paramName = pType.attribute("name").getValue(); //PARAMETER NAME

					   if (paramType == "scalar-parameter")
					   {
			        	  ParameterGroup newParameterGroup = new ParameterGroup(paramName);

			 		      for (Iterator sIterator = pType.elementIterator(); sIterator.hasNext();)
			 		      {
			 				Element scalar = (Element) sIterator.next();
			 				String propertyName = scalar.attribute("name").getValue().trim();
			 				String propertyValue = scalar.getText().trim();

							newParameterGroup.getParameterProperties().add(new ParameterProperty(propertyName, propertyValue));
			 				if (propertyName.indexOf("selectionList") > -1 )
			 				{
			 		  		   for (Iterator structIter = scalar.elementIterator(); structIter.hasNext();)
			 		  		   {
								String value = "";
								String label = "";
								Element structure = (Element) structIter.next();
								for (Iterator listIter = structure.elementIterator(); listIter.hasNext();)
								{
			 		  	      	   Element listElement = (Element) listIter.next();
			 		  	      	   String listName = listElement.getName();
			 		  	      	   String listPropertyName = listElement.attribute("name").getValue().trim();
			 		  	      	   String listValue = listElement.getText();
			 		  	      	   if (listPropertyName.indexOf("value") > -1)
			 		  	      	   {
									   value = listValue;
							  	   }
							  	   else if (listPropertyName.indexOf("label") > -1)
							  	   {
										label = listValue;
					   		 	   }
			 		  	    	 }
								 newParameterGroup.getParameterListSelection().add(new ParameterListSelection(label, value));
			 		  		   }
			 		  	     }
			 		       }

		 				  _paramGroups.add(newParameterGroup);
					    } // "if (paramType == "scalar-parameter")"
				      } // "for (Iterator pIterator ..."
			 	} // "if (elementName == "parameters")"
			 } // "for (Iterator i = "
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
	}

}

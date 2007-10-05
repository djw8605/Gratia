package net.sf.gratia.reporting;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import net.sf.gratia.reporting.exceptions.InvalidConfigurationException;
//
// Does not Handle combo box parameters that use a query to obtain the values.
// It handles only "scalar-parameter" both in or outside parameter groups
//
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
			String reportFolder = source.getPath();
			reportFolder = reportFolder.substring(0, reportFolder.lastIndexOf(File.separatorChar)+1);
			
			doc = saxReader.read(source);

			// Iterate through all child elements of root
			// Expand only the "parameters" elements and gather the information to build the ArrayLists
			// The parameters are either in: an external library or  "parameter-group" or  "scalar-parameter"
			// 
			//    <parameters>
		    //		<parameter-group name="DatabaseAccess" id="84">
		    //      	<parameters>
		    //          	<scalar-parameter name="DatabaseURL" id="81">
			//						......
            //				</scalar-parameter>
	        //       	</parameters>
	        //      </parameter-group>
			//			......
		    //      <parameter-group name="DateRange" id="118" extends="gratia-lib1.DateRange"/>
		    //      <scalar-parameter name="TraceTableKey" id="119" extends="gratia-lib1.TraceTableKey"/>
		    //	  </parameters>
			//			
			//		
			 	
			    for (Iterator i = doc.getRootElement().elementIterator(); i.hasNext(); )
			    {
				Element element = (Element) i.next();
			 	String elementName = element.getName();
			 	
			 	if (elementName == "parameters")
			 	{
			 		_paramGroups = new ArrayList();       // we have a parameters grouping
			 		for (Iterator pIterator = element.elementIterator(); pIterator.hasNext();)
					{
					   Element pType = (Element) pIterator.next();
					   String paramType = pType.getName().trim();
				       String fromLibrary = pType.attributeValue("extends");
				       
				       if (fromLibrary != null)
				       {
				    	   LoadLibraryParameters(reportFolder, fromLibrary);
				       }else if (paramType == "parameter-group" && fromLibrary == null)
					   {
					      for (Iterator gIterator = pType.elementIterator(); gIterator.hasNext();)
					      {
					         Element groupInfo = (Element) gIterator.next();
					         for (Iterator ggIter = groupInfo.elementIterator(); ggIter.hasNext();)
					         {
					            Element groupType = (Element) ggIter.next();
					            String gType = groupType.getName();
					            String gName = groupType.getText();
					            
					            // Skip parameters in the Database and UserInfo groups - set by the EventHandler
					            if ((gName.indexOf("Database") > -1) && (gName.indexOf("UserInfo") > -1)) 
					            	break;
					            if  (gType == "scalar-parameter")
					            {
						        	  addParameterInfo(groupType);
					            } // "scalar-parameter"
					         } // "for (Iterator ggIter..."
					      } // "for (Iterator gIterator..."
					   } else if (paramType == "scalar-parameter")
					   {
			        	  addParameterInfo(pType);
					   } 
				      } // "for (Iterator pIterator ..."
			 	} // "if (elementName == "parameters")"
			 } // "for (Iterator i = "
		} // try
		catch(Exception exDoc)
		{
			throw new InvalidConfigurationException("Unable to parse the Report design file  ", exDoc);
		}
		finally
		{
			doc = null;
			saxReader = null;
			source = null;
		}
	}
	
	
	private void LoadLibraryParameters(String reportFolder, String library)
			throws InvalidConfigurationException
	{	
		// The library name is of the form: libraryName.elementName (gratia-lib1.DateRange)
		String lookForElement = library.substring(library.lastIndexOf(".") + 1 );
		String libraryFile = reportFolder + library.substring(0, library.lastIndexOf(".") +1) + "rptlibrary";
		
		SAXReader saxReader = new SAXReader();
		Document doc = null;
		File source = null;

		if (libraryFile == null || libraryFile.length() == 0)
		{
			throw new InvalidConfigurationException("A Report library name has not been specified");
		}

		try
		{
			// Open the Report file and read the report file
			source = new File(libraryFile);
			doc = saxReader.read(source);

			// iterate through all child elements of root
		    for (Iterator i = doc.getRootElement().elementIterator(); i.hasNext(); )
		    {
		    	Element element = (Element) i.next();
		    	String elementName = element.getName();
		 	
		// Expand the "parameters" elements and gather the information to build the ArrayLists
		    	if (elementName == "parameters")
		    	{
		    		for (Iterator pIterator = element.elementIterator(); pIterator.hasNext();)
		    		{
		    			Element pType = (Element) pIterator.next();
		    			String paramType = pType.getName().trim();
		    			String paramName = pType.attributeValue("name").trim();
		    			
		    	// Get the information only if this is the element we are looking for		
		    			if (paramName.indexOf(lookForElement) > -1)
		    			{
		    				if (paramType.indexOf("parameter-group") > -1 )
		    				{
		    					for (Iterator gIterator = pType.elementIterator(); gIterator.hasNext();)
		    					{
		    						Element groupInfo = (Element) gIterator.next();
		    						for (Iterator ggIter = groupInfo.elementIterator(); ggIter.hasNext();)
		    						{
		    							Element groupType = (Element) ggIter.next();
		    							String gType = groupType.getName();
		    							if  (gType.indexOf("scalar-parameter") > -1 )
		    							{
		    								addParameterInfo(groupType);
		    							} // "scalar-parameter"
		    						} // "for (Iterator ggIter..."
		    					} // "for (Iterator gIterator..."
		    				}else if (paramType.indexOf("scalar-parameter") > -1 )
		    				{
		    					addParameterInfo(pType);
		    				} // "if (paramType == "scalar-parameter")"
		    			} 
		    		 } // "for (Iterator pIterator ..."
		    	} // "if (elementName == "parameters")"
		    } // "for (Iterator i = "
		} // try
		catch(Exception exDoc)
		{
			throw new InvalidConfigurationException("Unable to parse the Report library design file  ", exDoc);
		}
		finally
		{
			doc = null;
			saxReader = null;
			source = null;
		}
	}

	private void addParameterInfo (Element scalarParam)
	{
		String paramName = scalarParam.attributeValue("name").trim();
	 	ParameterGroup newParameterGroup = new ParameterGroup(paramName);

	    for (Iterator sIterator = scalarParam.elementIterator(); sIterator.hasNext();)
	    {
			Element scalar = (Element) sIterator.next();
			String propertyName = scalar.attributeValue("name").trim();
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
	  				   String listPropertyName = listElement.attributeValue("name").trim();
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
	}
	
}

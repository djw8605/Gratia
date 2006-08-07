package net.sf.gratia.storage;

import net.sf.gratia.services.*;
import java.util.*;
import org.dom4j.*;
import org.dom4j.io.*;
import java.io.*;

public class JobUsageRecordConverter
{
  XP xp = new XP();

public ArrayList convert(String xml) throws Exception 
  {
    ArrayList usageRecords = new ArrayList();    	
    SAXReader saxReader = new SAXReader();        
    Document doc = null;
    Element eroot = null;

    // Read the XML into a document for parsing
    try
      {
        doc = saxReader.read(new StringReader(xml));  
      }
    catch (Exception e)
      {
        System.out.println(xp.parseException(e));
        System.out.println("XML:" + "\n\n" + xml + "\n\n");
      }
    try 
      {
        eroot = doc.getRootElement();

        JobUsageRecord job = null;
        UsageRecordLoader load = new UsageRecordLoader();

        if (eroot.getName()=="JobUsageRecord"
            || eroot.getName()=="UsageRecord"
            || eroot.getName()=="Usage"
            || eroot.getName()=="UsageRecordType") 
          {
            // The current element is a job usage record node.  Use it to populate a JobUsageRecord object            	
            job = load.ReadUsageRecord(eroot);

            // Add this populated job usage record to the usage records array list
            usageRecords.add(job);                
          } 
        else if (eroot.getName()!="UsageRecords") 
          {            	
            // Unexpected root element
            throw new Exception("In the xml usage record, the expected root nodes are " + 
                                "JobUsageRecords, JobUsageRecord, Usage, UsageRecord " + 
                                "and UsageRecordType.\nHowever we got "+eroot.getName());
          } 
        else 
          {
            // This is a usage records node
            // which should contain one to many job usage record nodes so start a loop through its children
            for (Iterator i = eroot.elementIterator(); i.hasNext(); ) 
              {
                Element element = (Element) i.next();
                if (element.getName() == "JobUsageRecord") 
                  {
                    //The current element is a job usage record node.  Use it to populate a JobUsageRecord object
                    job = load.ReadUsageRecord(element);
                    usageRecords.add(job);
                  } 
                else 
                  {
                    // Unexpected element
                    throw new Exception("Unexpected element: "+element.getName()
                                        +"\n"+element);
                  }
              }
          }
      } 
    catch(Exception e) 
      {
        System.out.println("Parse error:  " + e.getMessage());
        throw new Exception("loadURXmlFile saw an error at 2:"+ e);        	
      }
    finally
      {
        // Cleanup object instantiations
        saxReader = null;
        doc = null;   
        eroot = null;
      }

    // The usage records array list is now populated with all the job usage records found in the given XML file
    //  return it to the caller.
    return usageRecords;
  }   

}

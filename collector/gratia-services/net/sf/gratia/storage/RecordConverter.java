package net.sf.gratia.storage;

import net.sf.gratia.util.XP;

import net.sf.gratia.services.*;
import java.util.*;
import org.dom4j.*;
import org.dom4j.io.*;
import java.io.*;

public class RecordConverter
{
   XP xp = new XP();

   public ArrayList convert(String xml) throws Exception
   {
      ArrayList usageRecords = null; 
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
         Utils.GratiaError(e,"XML:" + "\n\n" + xml + "\n\n");
         throw new Exception("Badly formed xml file");
      }
      try
      {
         eroot = doc.getRootElement();

         UsageRecordLoader load = new UsageRecordLoader();
         MetricRecordLoader mload = new MetricRecordLoader();
         ProbeDetailsLoader hload = new ProbeDetailsLoader();

         usageRecords = load.ReadRecords(eroot);
         if (usageRecords == null)
         {
            usageRecords = mload.ReadRecords(eroot);
         }
         if (usageRecords == null)
         {
             usageRecords = hload.ReadRecords(eroot);
         }
         if (usageRecords == null)
         {
            // Unexpected root element
            throw new Exception("In the xml usage record, the expected root nodes are " +
                                "JobUsageRecords, JobUsageRecord, Usage, UsageRecord " +
                                "UsageRecordType and MetricRecord.\nHowever we got " + eroot.getName());
         }
      }
      catch (Exception e)
      {
         Utils.GratiaError(e);
         throw e;
         // throw new Exception("loadURXmlFile saw an error at 2:" + e);
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
      if (usageRecords == null) usageRecords = new ArrayList();
      return usageRecords;
   }

}

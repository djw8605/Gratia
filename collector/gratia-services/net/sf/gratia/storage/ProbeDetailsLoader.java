package net.sf.gratia.storage;

import java.util.Iterator;
import org.dom4j.Attribute;
import org.dom4j.Element;
import java.util.ArrayList;

import net.sf.gratia.storage.RecordIdentity;
import net.sf.gratia.storage.StringElement;

/**
 * <p>Title: ProbeDetailsLoader</p>
 *
 * <p>Description: Implement the parsing and transformation of the XML Usage Record 
 * (via a sax Element).</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 * 
 * Updated by Arvind Gopu, Indiana University (http://peart.ucs.indiana.edu)
 *
 */
public class ProbeDetailsLoader extends RecordLoader
{
   public ArrayList ReadRecords(Element eroot) throws Exception
   {
      ArrayList records = new ArrayList();

      if (eroot.getName() == "ProbeDetails")
      {
         // The current element is a ProbeDetails record node.  Use it to populate a ProbeDetails object
         records.add(ReadRecord(eroot));
         
      } else if (eroot.getName() == "RecordEnvelope") {
         for (Iterator i = eroot.elementIterator(); i.hasNext();) {
            Element element = (Element) i.next();
            if (element.getName() == "ProbeDetails") {
               //The current element is a job usage record node.  Use it to populate a JobUsageRecord object
               records.add(ReadRecord(element));
            } else {
               // Don't care
            }
         }
      }

      if (records.size() == 0)
      {
         return null;
      }
      return records;

   }

   public Record ReadRecord(Element element) throws Exception
   {
      ProbeDetails job = new ProbeDetails();
      job.addRawXml(element.asXML());

      for (Iterator i = element.attributeIterator(); i.hasNext(); )
      {
         // Attribute a = (Attribute)
         i.next();
         // Skip all attribute of ProbeDetails for now
      }

      for (Iterator i = element.elementIterator(); i.hasNext(); )
      {
         Element sub = (Element)i.next();
         // System.out.println("" + sub.GetName())
         try
         {
            if (sub.getName() == "ReporterLibrary")
            {
                AddSoftware(job, sub);
            }
            else if (sub.getName() == "Reporter")
            {
                AddSoftware(job, sub);
            }
            else if (sub.getName() == "Service")
            {
                AddSoftware(job, sub);
            }
            else
            {
               ReadCommonRecord(job,sub);
            }
         }
         catch (Exception e)
         {
            // Something went wrong in the parsing.  We do not die, we
            // continue to try to parse.  The next step in the processing
            // would need to see what's missing.
            job.addExtraXml(sub.asXML());
            //            Utils.GratiaInfo("Warning: error during the xml parsing of " + job.getRecordId() + " : " + e);
            e.printStackTrace();
         }
      }
      return job;
   }

    public static void AddSoftware(ProbeDetails job, Element element)
        throws Exception {

        Software soft = new Software();
        soft.setType( element.getName() );

        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("version")) {
                soft.setVersion(a.getValue());
            } else {
                // Add To ExtraXml
            }
        }
        soft.setName(element.getText());
        job.addSoftware(soft);
    }

    public ProbeDetailsLoader() {
    }
}

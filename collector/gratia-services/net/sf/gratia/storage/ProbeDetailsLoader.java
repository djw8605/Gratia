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
public class ProbeDetailsLoader implements RecordLoader
{
   public ArrayList ReadRecords(Element eroot) throws Exception
   {
      ArrayList records = new ArrayList();

      if (eroot.getName() == "ProbeDetails")
      {
         // The current element is a metric record node.  Use it to populate a ProbeDetails object
         records.add(ReadRecord(eroot));
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
            if (sub.getName().equalsIgnoreCase("RecordIdentity"))
            {
               SetRecordIdentity(job, sub);
            }
            else if (sub.getName() == "SiteName")
            {
               SetSiteName(job, sub);
            }
            else if (sub.getName() == "ProbeName")
            {
               SetProbeName(job, sub);
            }
            else if (sub.getName() == "Grid")
            {
               SetGrid(job, sub);
            }
            else if (sub.getName() == "ReporterLibrary")
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
               job.addExtraXml(sub.asXML());
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

   public static void SetRecordIdentity(ProbeDetails job, Element element)
            throws Exception
   {
      RecordIdentity id = job.getRecordIdentity();
      if (id != null /* record identity already set */)
      {
         Utils.GratiaError("SetRecordIdentity", "parsing",
                                    " found a second RecordIdentity field in the xml file",
                                    false);
         return;
      }
      for (Iterator i = element.attributeIterator(); i.hasNext(); )
      {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("recordId"))
         {
            if (id == null)
               id = new RecordIdentity();
            id.setRecordId(a.getValue());
         }
         else if (a.getName().equalsIgnoreCase("createTime"))
         {
            if (id == null)
               id = new RecordIdentity();
            DateElement createTime = new DateElement();
            createTime.setValue(a.getValue());
            id.setCreateTime(createTime);
         }
      }
      if (id != null)
         job.setRecordIdentity(id);
   }

   public static void SetSiteName(ProbeDetails job, Element element)
        throws Exception
   {
      StringElement el = job.getSiteName();
      if (el != null /* job identity already set */)
      {
         Utils.GratiaError("SetSiteName", "parsing",
             " found a second SiteName field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); )
      {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description"))
         {
            el.setDescription(a.getValue());
         }
      }
      el.setValue(element.getText());
      job.setSiteName(el);
   }

   public static void SetProbeName(ProbeDetails job, Element element) throws
            Exception
   {
      StringElement el = job.getProbeName();
      if (el == null)
      {
         el = new StringElement();
      }
      for (Iterator i = element.attributeIterator(); i.hasNext(); )
      {
         Attribute a = (Attribute)i.next();
         if (a.getName() == "description")
         {
            String desc = el.getDescription();
            if (desc == null) desc = "";
            else desc = desc + " ; ";
            desc = desc + a.getValue();
            el.setDescription(desc);
         }
      }
      String val = el.getValue();
      if (val == null) val = "";
      else val = val + " ; ";
      val = val + element.getText();
      el.setValue(val);
      job.setProbeName(el);
   }

    public static void SetGrid(ProbeDetails job, Element element)
            throws Exception {
        StringElement el = job.getGrid();
        if (el == null) {
            el = new StringElement();
        }
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName() == "description") {
                String desc = el.getDescription();
                if (desc == null)
                    desc = "";
                else
                    desc = desc + " ; ";
                desc = desc + a.getValue();
                el.setDescription(desc);
            }
        }
        String val = el.getValue();
        if (val == null)
            val = "";
        else
            val = val + " ; ";
        val = val + element.getText();
        el.setValue(val);
        job.setGrid(el);
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

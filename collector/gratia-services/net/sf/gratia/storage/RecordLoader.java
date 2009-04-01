package net.sf.gratia.storage;

import java.util.ArrayList;
import org.dom4j.Element;
import org.dom4j.Attribute;

/**
 * <p>Title: RecordLoader</p>
 *
 * <p>Description: Defines the interface for the parsing and transformation of the XML Usage Record 
 * (via a sax Element).</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 */
public abstract class RecordLoader
{
   // Interface Method
   public abstract ArrayList ReadRecords(Element eroot) throws Exception;
   public abstract Record ReadRecord(Element element) throws Exception;

   // Common implementations.
   static void ReadCommonRecord(Record job, Element sub) throws Exception
   {
      // This should be executed last.
      // If the element is not one of the supported element (Grid, ProbeName, SiteName, RecordIdentity, Origins)
      // it is added to the ExtraXml field.
    
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
      else if (sub.getName() == "Origin")
      {
         AddOrigin(job, sub);
      }
      else
      {
         job.addExtraXml(sub.asXML());
      }
      
   }
   
   static void AddOrigin(Record job, Element element) throws Exception
   {
      Origin from = OriginLoader.ReadElement(element, job);
   }
   
   static void SetGrid(Record job, Element element) throws Exception 
   {
      StringElement el = job.getGrid();
      if (el == null) {
         el = new StringElement();
      }
      for (Object iter : element.attributes() ) {
         Attribute a = (Attribute)iter;
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
   
   public static void SetProbeName(Record job, Element element) throws Exception {
      StringElement el = job.getProbeName();
      if (el == null) {
         el = new StringElement();
      }
      for (Object iter : element.attributes() ) {
         Attribute a = (Attribute)iter;
         if (a.getName() == "description") {
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
   
   public static void SetRecordIdentity(Record job, Element element) throws Exception
   {
      RecordIdentity id = job.getRecordIdentity();
      if (id != null /* record identity already set */)
      {
         Utils.GratiaError("SetRecordIdentity", "parsing",
                           " found a second RecordIdentity field in the xml file",
                           false);
         return;
      }
      for (Object iter : element.attributes() ) {
         Attribute a = (Attribute)iter;
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
   
   public static void SetSiteName(Record job, Element element) throws Exception
   {
      StringElement el = job.getSiteName();
      if (el != null /* job identity already set */)
      {
         Utils.GratiaError("SetSiteName", "parsing",
                           " found a second SiteName field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Object iter : element.attributes() ) {
         Attribute a = (Attribute)iter;
         if (a.getName().equalsIgnoreCase("description"))
         {
            el.setDescription(a.getValue());
         }
      }
      el.setValue(element.getText());
      job.setSiteName(el);
   }
}
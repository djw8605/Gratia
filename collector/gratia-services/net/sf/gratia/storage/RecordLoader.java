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

   public static String ConcatString(String old, String value) 
   {
      // Return the old and value string concatenated with ';'
      // (or just value if old is empty or null)./
      
      String res = old;
      if (res == null || res.length()==0)
         res = value;
      else
         res = res + " ; " + value;
      return res;
   }
   
   public static String LimitedTextField(Record rec, Element element, String value, int limit, String fieldname) 
   {
      // Return the text field limited to 'limit' charaters and print an warning message
      // to the log if we are truncating the field.
      
      if (value.length() >= limit) {
         Utils.GratiaInfo("Set"+fieldname+" found a value longer than "+limit+" characters (the value has been truncated).");
         rec.addExtraXml(element.asXML());
         value = value.substring(0,limit);
      }
      return value;
   }
   
   public static String SetLimitedTextField(StringElement el, Record rec, Element element, String value, int limit, String fieldname) 
   {
      // Return the text field limited to 'limit' charaters and print an warning message
      // to the log if we are truncating the field.
      
      value = LimitedTextField(rec, element, value, limit, fieldname);
      el.setValue( value );
      return value;
   }
   
   public static String SetLimitedTextField(StringElement el, Record rec, Element element, int limit, String fieldname) 
   {
      // Return the text field limited to 'limit' charaters and print an warning message
      // to the log if we are truncating the field.
      
      return SetLimitedTextField(el,rec,element,element.getText(),limit,fieldname);
   }

   public static String SetLimitedDescription(XmlElement el, Record rec, Element element, String value, int limit, String fieldname) 
   {
      // Return the text field limited to 'limit' charaters and print an warning message
      // to the log if we are truncating the field.
      
      String desc = ConcatString( el.getDescription(), value );
      
      desc = LimitedTextField(rec, element, desc, limit, fieldname + "'s description");
      el.setDescription( desc );
      return desc;
   }

   public static String SetLimitedDescription(XmlElement el, Record rec, Element element, Attribute a, int limit, String fieldname) 
   {
      // Return the text field limited to 'limit' charaters and print an warning message
      // to the log if we are truncating the field.
      
      return SetLimitedDescription(el,rec,element,a.getValue(),limit,fieldname);
   }
   
   
   
   // Common implementations.
   static void ReadCommonRecord(Record rec, Element sub) throws Exception
   {
      // This should be executed last.
      // If the element is not one of the supported element (Grid, ProbeName, SiteName, RecordIdentity, Origins)
      // it is added to the ExtraXml field.
    
      if (sub.getName().equalsIgnoreCase("RecordIdentity"))
      {
         SetRecordIdentity(rec, sub);
      }
      else if (sub.getName() == "SiteName")
      {
         SetSiteName(rec, sub);
      }
      else if (sub.getName() == "ProbeName")
      {
         SetProbeName(rec, sub);
      }
      else if (sub.getName() == "Grid")
      {
         SetGrid(rec, sub);
      }
      else if (sub.getName() == "Origin")
      {
         AddOrigin(rec, sub);
      }
      else
      {
         rec.addExtraXml(sub.asXML());
      }
      
   }
   
   static void AddOrigin(Record rec, Element element) throws Exception
   {
      Origin from = OriginLoader.ReadElement(element, rec);
   }
   
   static void SetGrid(Record rec, Element element) throws Exception 
   {
      StringElement el = rec.getGrid();
      if (el == null) {
         el = new StringElement();
      }
      for (Object iter : element.attributes() ) {
         Attribute a = (Attribute)iter;
         if (a.getName() == "description") {
            SetLimitedDescription(el, rec, element, a, 255, "Grid");
         }
      }
      String val = el.getValue();
      if (val == null)
         val = "";
      else
         val = val + " ; ";
      val = val + element.getText();
      el.setValue(val);
      rec.setGrid(el);
   }
   
   public static void SetProbeName(Record rec, Element element) throws Exception {
      StringElement el = rec.getProbeName();
      if (el == null) {
         el = new StringElement();
      }
      for (Object iter : element.attributes() ) {
         Attribute a = (Attribute)iter;
         if (a.getName() == "description") {
            SetLimitedDescription(el, rec, element, a, 255, "ProbeName");
         }
      }
      String val = el.getValue();
      if (val == null) val = "";
      else val = val + " ; ";
      val = val + element.getText();
      el.setValue(val);
      rec.setProbeName(el);
   }
   
   public static void SetRecordIdentity(Record rec, Element element) throws Exception
   {
      RecordIdentity id = rec.getRecordIdentity();
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
         rec.setRecordIdentity(id);
   }
   
   public static void SetSiteName(Record rec, Element element) throws Exception
   {
      StringElement el = rec.getSiteName();
      if (el != null /* rec identity already set */)
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
            SetLimitedDescription(el, rec, element, a, 255, "SiteName");
         }
      }
      el.setValue(element.getText());
      rec.setSiteName(el);
   }
}
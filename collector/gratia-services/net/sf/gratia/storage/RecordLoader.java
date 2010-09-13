package net.sf.gratia.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.apache.commons.lang.StringEscapeUtils;
import net.sf.gratia.util.Logging;
import java.util.regex.*;

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
   StringBuilder extraXmlAttributes = new StringBuilder();
   // Interface Method
   public abstract ArrayList ReadRecords(Element eroot) throws Exception;
   public abstract Record ReadRecord(Element element) throws Exception;

   public String ConcatString(String old, String value) 
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
   
   public String LimitedTextField(Record rec, Element element, String value, int limit, String fieldname) 
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
   
   public String SetLimitedTextField(StringElement el, Record rec, Element element, String value, int limit, String fieldname) 
   {
      // Return the text field limited to 'limit' charaters and print an warning message
      // to the log if we are truncating the field.
      
      value = LimitedTextField(rec, element, value, limit, fieldname);
      el.setValue( value );
      return value;
   }
   
   public String SetLimitedTextField(StringElement el, Record rec, Element element, int limit, String fieldname) 
   {
      // Return the text field limited to 'limit' charaters and print an warning message
      // to the log if we are truncating the field.
      
      return SetLimitedTextField(el,rec,element,element.getText(),limit,fieldname);
   }

   public String SetLimitedDescription(XmlElement el, Record rec, Element element, String value, int limit, String fieldname) 
   {
      // Return the text field limited to 'limit' charaters and print an warning message
      // to the log if we are truncating the field.
      
      String desc = ConcatString( el.getDescription(), value );
      
      desc = LimitedTextField(rec, element, desc, limit, fieldname + "'s description");
      el.setDescription( desc );
      return desc;
   }

   public String SetLimitedDescription(XmlElement el, Record rec, Element element, Attribute a, int limit, String fieldname) 
   {
      // Return the text field limited to 'limit' charaters and print an warning message
      // to the log if we are truncating the field.
      
      return SetLimitedDescription(el,rec,element,a.getValue(),limit,fieldname);
   }
   
   
   
   // Common implementations.
   void ReadCommonRecord(Record rec, Element sub) throws Exception
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
   
   void AddOrigin(Record rec, Element element) throws Exception
   {
      Origin from = OriginLoader.ReadElement(element, rec);
   }
   
   void SetGrid(Record rec, Element element) throws Exception 
   {
      StringElement el = rec.getGrid();
      if (el == null) {
         el = new StringElement();
      }
      for (Object iter : element.attributes() ) {
         Attribute a = (Attribute)iter;
         if (a.getName() == "description") {
            SetLimitedDescription(el, rec, element, a, 255, "Grid");
         } else { 
               extraXmlAttribute(element,a);

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
   
   public void SetProbeName(Record rec, Element element) throws Exception {
      StringElement el = rec.getProbeName();
      if (el == null) {
         el = new StringElement();
      }
      for (Object iter : element.attributes() ) {
         Attribute a = (Attribute)iter;
         if (a.getName() == "description") {
            SetLimitedDescription(el, rec, element, a, 255, "ProbeName");
         } else { 
               extraXmlAttribute(element,a);
            }
      }
      String val = el.getValue();
      if (val == null) val = "";
      else val = val + " ; ";
      val = val + element.getText();
      el.setValue(val);
      rec.setProbeName(el);
   }
   
   public void SetRecordIdentity(Record rec, Element element) throws Exception
   {
      RecordIdentity id = rec.getRecordIdentity();
      if (id != null) /* record identity already set */
      {
         Utils.GratiaError("SetRecordIdentity", "parsing",
                           " found a second RecordIdentity field in the xml file",
                           false);
         rec.addExtraXml(element.asXML());
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
         } else { 
               extraXmlAttribute(element,a);
            }
      }
      if (id != null)
         rec.setRecordIdentity(id);
   }
   
   public void SetSiteName(Record rec, Element element) throws Exception
   {
      StringElement el = rec.getSiteName();
      if (el != null) {
         /* site name already set */
         if (!el.getValue().equals(element.getText())) {
            Utils.GratiaError("SetSiteName", "parsing",
                              " found a second SiteName field in the xml file", false);
         }
         rec.addExtraXml(element.asXML());
         return;
      }
      el = new StringElement();
      for (Object iter : element.attributes() ) {
         Attribute a = (Attribute)iter;
         if (a.getName().equalsIgnoreCase("description"))
         {
            SetLimitedDescription(el, rec, element, a, 255, "SiteName");
         } else { 
               extraXmlAttribute(element,a);
            }
      }
      el.setValue(element.getText());
      rec.setSiteName(el);
   }

   public void extraXmlAttribute(Element el, Attribute attr)
   {
      if(el != null) {

          String tagName = StringEscapeUtils.escapeXml(el.getName());

          if(!Pattern.matches( ".*TAG_NAME_" + tagName + ".*", extraXmlAttributes )) {
              if(!extraXmlAttributes.toString().equals(""))
                 extraXmlAttributes.append("/>");
              extraXmlAttributes.append("<TAG_NAME_");
              extraXmlAttributes.append(tagName);
          }
          extraXmlAttributes.append(" ");
          extraXmlAttributes.append(StringEscapeUtils.escapeXml(attr.getName()));
          extraXmlAttributes.append("=\"");
          extraXmlAttributes.append(StringEscapeUtils.escapeXml(attr.getValue()));
          extraXmlAttributes.append("\"");
      }
   }

   public Record addExtraXmlAttributes(Record job)
   {
       if(extraXmlAttributes.toString().length() > 0)
       {
           job.addExtraXml("<ExtraAttribute xmlns=\"http://www.gridforum.org/2003/ur-wg\">");
           job.addExtraXml(extraXmlAttributes.toString().replaceAll("TAG_NAME_",""));
           job.addExtraXml("></ExtraAttribute>");
       }
       return job;
   }
}

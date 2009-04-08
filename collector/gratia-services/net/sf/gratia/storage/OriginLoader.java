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
 *
 */
public class OriginLoader
{
   public static Origin Read(Element eroot) throws Exception
   {
      Origin records = null;

      if (eroot.getName() == "Origin")
      {
         // The current element is a Origin record node.  Use it to populate a Origin object
         records = ReadElement(eroot, null);
      }

      return records;

   }

   public static Origin ReadElement(Element element, Record record) throws Exception
   {
      Origin origin = new Origin();
      int hopNumber = 0;

      for (Iterator i = element.attributeIterator(); i.hasNext(); )
      {
         Attribute a = (Attribute) i.next();
         if (a.getName().equalsIgnoreCase("hopNumber")) {
            hopNumber = Integer.parseInt(a.getValue());
         }  else {
            // Add To ExtraXml
         }
      }

      for (Iterator i = element.elementIterator(); i.hasNext(); )
      {
         Element sub = (Element)i.next();
         // System.out.println("" + sub.GetName())
         try
         {
            if (sub.getName().equalsIgnoreCase("ServerDate"))
            {
               SetServerDate(origin, sub, record);
            }
            else if (sub.getName() == "Connection")
            {
               SetConnection(origin, sub, record);
            }
            else 
            {
               if (record != null) {
                  record.addExtraXml(sub.asXML());
               }
            }
         }
         catch (Exception e)
         {
            // Something went wrong in the parsing.  We do not die, we
            // continue to try to parse.  The next step in the processing
            // would need to see what's missing.
            if (record != null) {
               record.addExtraXml(sub.asXML());
            }
            //            Utils.GratiaInfo("Warning: error during the xml parsing of " + job.getRecordId() + " : " + e);
            e.printStackTrace();
         }
      }
      
      if (record != null) {
         record.insertOrigin(origin,hopNumber);
      }
      
      return origin;
   }

   public static void SetServerDate(Origin origin, Element element, Record record)
   throws Exception {
      java.util.Date d = origin.getServerDate();
      if (d != null /* server date already set */) {
         Utils.GratiaError("SetServerDate", "parsing",
                           " found a second ServerDate field in the xml file", false);
         return;
      }
      DateElement el = new DateElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            el.setDescription(a.getValue());
         }
      }
      
      el.setValue(element.getText());
      origin.setServerDate(el.getValue());
   }
   
   public static void SetConnection(Origin origin, Element element, Record record)
   throws Exception {
      Connection grconn = origin.getConnection();
      if (grconn != null /* job identity already set */) {
         Utils.GratiaError("SetConnection", "parsing",
                           " found a second Connection field in the xml file", false);
         if (record != null) {
            record.addExtraXml(element.asXML());
         }
         return;
      }
      grconn = new Connection();
      boolean seen = false;
      // No known attributes.
      String extras = "";
      for (Iterator i = element.elementIterator(); i.hasNext();) {
         Element sub = (Element) i.next();
         if (sub.getName().equalsIgnoreCase("SenderHost")) {
            seen = true;
            grconn.setSenderHost(sub.getText());
         } else if (sub.getName().equalsIgnoreCase("Sender")) {
            seen = true;
            grconn.setSender(sub.getText());
         } else if (sub.getName().equalsIgnoreCase("Collector")) {
            seen = true;
            grconn.setCollectorName(sub.getText());
         } else if (sub.getName().equalsIgnoreCase("Certificate")) {
            seen = true;
            grconn.setCertificate(new Certificate(sub.getText()));
         } else {
            extras = extras + sub.asXML();
         }
      }
      if (seen)
         origin.setConnection(grconn);
      if (extras.length() > 0 && record != null) {
         extras = "<Connection>" + extras + "</Connection>";
         record.addExtraXml(extras);
      }
   }
   
   

    public OriginLoader() {
    }
}

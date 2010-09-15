package net.sf.gratia.storage;

import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Element;

import net.sf.gratia.storage.RecordIdentity;
import net.sf.gratia.storage.StringElement;

import net.sf.gratia.util.Logging;

/**
 * <p>Title: StorageElementRecordLoader</p>
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
 * Updated by Brian Bockelman, University of Nebraska-Lincoln (http://rcf.unl.edu)
 * 
 */

public class StorageElementRecordLoader extends RecordLoader {
   
   public ArrayList ReadRecords(Element eroot) throws Exception {
      ArrayList records = new ArrayList();
      if (eroot.getName() == "StorageElementRecord") {
         // The current element is a StorageElementRecord node.  Use it to populate a StorageElementRecord object            	
         records.add(ReadRecord(eroot));
      }  else if (eroot.getName() == "RecordEnvelope") {
         for (Iterator i = eroot.elementIterator(); i.hasNext();) {
            Element element = (Element) i.next();
            if (element.getName() == "StorageElementRecord") {
               //The current element is a SE state node.  Use it to populate a StorageElementRecord object
               records.add(ReadRecord(element));
            } else {
               // Don't care
            }
         }
      }
      if (records.size() == 0) {
         return null;
      }
      return records;
   }
   
   public Record ReadRecord(Element element) throws Exception {
      StorageElementRecord job = new StorageElementRecord();
      resetExtraXmlAttributes();

      job.addRawXml(element.asXML());
      
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         // Attribute a = (Attribute)
         i.next();
         // Skip all attribute of StorageElementRecord for now
      }
      
      for (Iterator i = element.elementIterator(); i.hasNext(); ) {
         Element sub = (Element)i.next();
         // System.out.println("" + sub.GetName())
         try {
            if (sub.getName().equalsIgnoreCase("UniqueID")) {
               SetUniqueID(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("MeasurementType")) {
               SetMeasurementType(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("StorageType")) {
               SetStorageType(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("Timestamp")) {
               SetTimestamp(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("TotalSpace")) {
               SetTotalSpace(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("FreeSpace")) {
               SetFreeSpace(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("UsedSpace")) {
               SetUsedSpace(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("FileCountLimit")) {
               SetFileCountLimit(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("FileCount")) {
               SetFileCount(job, sub);
            }
            else {
               ReadCommonRecord(job,sub);
            }
         }
         catch (Exception e) {
            // Something went wrong in the parsing.  We do not die, we
            // continue to try to parse.  The next step in the processing
            // would need to see what's missing.
            job.addExtraXml(sub.asXML());
            Logging.warning("Warning: error during the xml parsing of " +
                            job.getRecordId() + " : " + e +
                            "; offending XML: " + sub.asXML(), e);
         }
      }
      return addExtraXmlAttributes(job);
   }
   
   public void SetUniqueID(StorageElementRecord job, Element element)
   throws Exception {
      StringElement el = job.getUniqueID();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetUniqueID", "parsing",
                           " found a second UniqueID field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "UniqueID");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "UniqueID");
      job.setUniqueID(el);
   }
   
   
   public void SetMeasurementType(StorageElementRecord job, Element element)
   throws Exception {
      StringElement el = job.getMeasurementType();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetMeasurementType", "parsing",
                           " found a second MeasurementType field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "MeasurementType");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "MeasurementType");
      job.setMeasurementType(el);
   }
   
   public void SetStorageType(StorageElementRecord job, Element element)
   throws Exception {
      StringElement el = job.getStorageType();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetStorageType", "parsing",
                           " found a second StorageType field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "StorageType");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "StorageType");
      job.setStorageType(el);
   }
   
   public void SetTimestamp(StorageElementRecord job, Element element)
   throws Exception {
      DateElement el = job.getTimestamp();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetTimestamp", "parsing",
                           " found a second EndTime field in the xml file", false);
         return;
      }
      el = new DateElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "Timestamp");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      // Duration d = new Duration();
      
      el.setValue(element.getText());
      job.setTimestamp(el);
   }
   
   public void SetTotalSpace(StorageElementRecord job, Element element)
   throws Exception {
      IntegerElement el = job.getTotalSpace();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetTotalSpace", "parsing",
                           " found a second TotalSpace field in the xml file", false);
         return;
      }
      el = new IntegerElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "TotalSpace");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      el.setValue((new Long(element.getText())).longValue());
      job.setTotalSpace(el);
   }
   
   public void SetFreeSpace(StorageElementRecord job, Element element)
   throws Exception {
      IntegerElement el = job.getFreeSpace();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetFreeSpace", "parsing",
                           " found a second FreeSpace field in the xml file", false);
         return;
      }
      el = new IntegerElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "FreeSpace");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      el.setValue((new Long(element.getText())).longValue());
      job.setFreeSpace(el);
   }
   
   public void SetUsedSpace(StorageElementRecord job, Element element)
   throws Exception {
      IntegerElement el = job.getUsedSpace();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetUsedSpace", "parsing",
                           " found a second UsedSpace field in the xml file", false);
         return;
      }
      el = new IntegerElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "UsedSpace");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      el.setValue((new Long(element.getText())).longValue());
      job.setUsedSpace(el);
   }
   
   public void SetFileCountLimit(StorageElementRecord job, Element element)
   throws Exception {
      IntegerElement el = job.getFileCountLimit();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetFileCountLimit", "parsing",
                           " found a second FileCountLimit field in the xml file", false);
         return;
      }
      el = new IntegerElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "FileCountLimit");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      el.setValue((new Long(element.getText())).longValue());
      job.setFileCountLimit(el);
   }
   
   public void SetFileCount(StorageElementRecord job, Element element)
   throws Exception {
      IntegerElement el = job.getFileCount();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetFileCount", "parsing",
                           " found a second FileCount field in the xml file", false);
         return;
      }
      el = new IntegerElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "FileCount");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      el.setValue((new Long(element.getText())).longValue());
      job.setFileCount(el);
   }
   
   public StorageElementRecordLoader() {
   }
}

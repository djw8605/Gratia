package net.sf.gratia.storage;

import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Element;

import net.sf.gratia.storage.RecordIdentity;
import net.sf.gratia.storage.StringElement;

import net.sf.gratia.util.Logging;

/**
 * <p>Title: StorageElementLoader</p>
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

public class StorageElementLoader extends RecordLoader {

  public ArrayList ReadRecords(Element eroot) throws Exception {
    ArrayList records = new ArrayList();
    if (eroot.getName() == "StorageElement") {
      // The current element is a SE description node.  Use it to populate a StorageElement object            	
      records.add(ReadRecord(eroot));
    }  else if (eroot.getName() == "RecordEnvelope") {
      for (Iterator i = eroot.elementIterator(); i.hasNext();) {
        Element element = (Element) i.next();
        if (element.getName() == "StorageElement") {
          //The current element is a job usage record node.  Use it to populate a StorageElement object
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
    StorageElement job = new StorageElement();
    job.addRawXml(element.asXML());

    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      // Attribute a = (Attribute)
      i.next();
      // Skip all attribute of StorageElement for now
    }

    for (Iterator i = element.elementIterator(); i.hasNext(); ) {
      Element sub = (Element)i.next();
      // System.out.println("" + sub.GetName())
      try {
        if (sub.getName().equalsIgnoreCase("UniqueID")) {
          SetUniqueID(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("SE")) {
          SetSE(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Name")) {
          SetName(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("ParentID")) {
          SetParentID(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("VO")) {
          SetVO(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("OwnerDN")) {
          SetOwnerDN(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("SpaceType")) {
          SetSpaceType(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Timestamp")) {
          SetTimestamp(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Implementation")) {
          SetImplementation(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Version")) {
          SetVersion(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Status")) {
          SetStatus(job, sub);
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
    return job;
  }

  public static void SetUniqueID(StorageElement job, Element element)
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
        el.setDescription(a.getValue());
      }
    }
    el.setValue(element.getText());
    job.setUniqueID(el);
  }

  public static void SetSE(StorageElement job, Element element)
  throws Exception {
    StringElement el = job.getSE();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetSE", "parsing",
          " found a second SE field in the xml file", false);
      return;
    }
    el = new StringElement();
    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      Attribute a = (Attribute)i.next();
      if (a.getName().equalsIgnoreCase("description")) {
        el.setDescription(a.getValue());
      }
    }
    el.setValue(element.getText());
    job.setSE(el);
  }

  public static void SetName(StorageElement job, Element element)
  throws Exception {
    StringElement el = job.getName();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetName", "parsing",
          " found a second Name field in the xml file", false);
      return;
    }
    el = new StringElement();
    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      Attribute a = (Attribute)i.next();
      if (a.getName().equalsIgnoreCase("description")) {
        el.setDescription(a.getValue());
      }
    }
    el.setValue(element.getText());
    job.setName(el);
  }
  
  public static void SetParentID(StorageElement job, Element element)
  throws Exception {
    StringElement el = job.getParentID();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetParentID", "parsing",
          " found a second ParentID field in the xml file", false);
      return;
    }
    el = new StringElement();
    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      Attribute a = (Attribute)i.next();
      if (a.getName().equalsIgnoreCase("description")) {
        el.setDescription(a.getValue());
      }
    }
    el.setValue(element.getText());
    job.setParentID(el);
  }

  public static void SetVO(StorageElement job, Element element)
  throws Exception {
    StringElement el = job.getVO();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetVO", "parsing",
          " found a second VO field in the xml file", false);
      return;
    }
    el = new StringElement();
    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      Attribute a = (Attribute)i.next();
      if (a.getName().equalsIgnoreCase("description")) {
        el.setDescription(a.getValue());
      }
    }
    el.setValue(element.getText());
    job.setVO(el);
  }

  public static void SetOwnerDN(StorageElement job, Element element)
  throws Exception {
    StringElement el = job.getOwnerDN();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetOwnerDN", "parsing",
          " found a second OwnerDN field in the xml file", false);
      return;
    }
    el = new StringElement();
    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      Attribute a = (Attribute)i.next();
      if (a.getName().equalsIgnoreCase("description")) {
        el.setDescription(a.getValue());
      }
    }
    el.setValue(element.getText());
    job.setOwnerDN(el);
  }


  public static void SetSpaceType(StorageElement job, Element element)
  throws Exception {
    StringElement el = job.getSpaceType();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetSpaceType", "parsing",
          " found a second SpaceType field in the xml file", false);
      return;
    }
    el = new StringElement();
    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      Attribute a = (Attribute)i.next();
      if (a.getName().equalsIgnoreCase("description")) {
        el.setDescription(a.getValue());
      }
    }
    el.setValue(element.getText());
    job.setSpaceType(el);
  }

  public static void SetTimestamp(StorageElement job, Element element)
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
        el.setDescription(a.getValue());
      }
    }
    // Duration d = new Duration();

    el.setValue(element.getText());
    job.setTimestamp(el);
  }

  public static void SetImplementation(StorageElement job, Element element)
  throws Exception {
    StringElement el = job.getImplementation();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetImplementation", "parsing",
          " found a second Implementation field in the xml file", false);
      return;
    }
    el = new StringElement();
    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      Attribute a = (Attribute)i.next();
      if (a.getName().equalsIgnoreCase("description")) {
        el.setDescription(a.getValue());
      }
    }
    el.setValue(element.getText());
    job.setImplementation(el);
  }

  public static void SetVersion(StorageElement job, Element element)
  throws Exception {
    StringElement el = job.getVersion();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetVersion", "parsing",
          " found a second Version field in the xml file", false);
      return;
    }
    el = new StringElement();
    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      Attribute a = (Attribute)i.next();
      if (a.getName().equalsIgnoreCase("description")) {
        el.setDescription(a.getValue());
      }
    }
    el.setValue(element.getText());
    job.setVersion(el);
  }

  public static void SetStatus(StorageElement job, Element element)
  throws Exception {
    StringElement el = job.getStatus();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetStatus", "parsing",
          " found a second Status field in the xml file", false);
      return;
    }
    el = new StringElement();
    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      Attribute a = (Attribute)i.next();
      if (a.getName().equalsIgnoreCase("description")) {
        el.setDescription(a.getValue());
      }
    }
    el.setValue(element.getText());
    job.setStatus(el);
  }

  public StorageElementLoader() {
  }
}

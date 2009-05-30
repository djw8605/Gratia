package net.sf.gratia.storage;

import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Element;

import net.sf.gratia.storage.RecordIdentity;
import net.sf.gratia.storage.StringElement;

import net.sf.gratia.util.Logging;

/**
 * <p>Title: ComputeElementRecordLoader</p>
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

public class ComputeElementRecordLoader extends RecordLoader {

  public ArrayList ReadRecords(Element eroot) throws Exception {
    ArrayList records = new ArrayList();
    if (eroot.getName() == "ComputeElementRecord") {
      // The current element is a GlueVO record node.  Use it to populate a GlueRecord object            	
      records.add(ReadRecord(eroot));
    }  else if (eroot.getName() == "RecordEnvelope") {
      for (Iterator i = eroot.elementIterator(); i.hasNext();) {
        Element element = (Element) i.next();
        if (element.getName() == "ComputeElementRecord") {
          //The current element is a job usage record node.  Use it to populate a ComputeElementRecord object
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
    ComputeElementRecord job = new ComputeElementRecord();
    job.addRawXml(element.asXML());

    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      // Attribute a = (Attribute)
      i.next();
      // Skip all attribute of ComputeElement for now
    }

    for (Iterator i = element.elementIterator(); i.hasNext(); ) {
      Element sub = (Element)i.next();
      // System.out.println("" + sub.GetName())
      try {
        if (sub.getName().equalsIgnoreCase("UniqueID")) {
          SetUniqueID(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("VO")) {
          SetVO(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Timestamp")) {
          SetTimestamp(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("RunningJobs")) {
          SetRunningJobs(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("TotalJobs")) {
          SetTotalJobs(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("WaitingJobs")) {
          SetWaitingJobs(job, sub);
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

  public static void SetUniqueID(ComputeElementRecord job, Element element)
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

  public static void SetVO(ComputeElementRecord job, Element element)
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

  public static void SetTimestamp(ComputeElementRecord job, Element element)
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

  public static void SetRunningJobs(ComputeElementRecord job, Element element)
  throws Exception {
    IntegerElement el = job.getRunningJobs();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetRunningJobs", "parsing",
          " found a second RunningJobs field in the xml file", false);
      return;
    }
    el = new IntegerElement();
    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      Attribute a = (Attribute)i.next();
      if (a.getName().equalsIgnoreCase("description")) {
        el.setDescription(a.getValue());
      }
    }
    el.setValue((new Long(element.getText())).longValue());
    job.setRunningJobs(el);
  }

  public static void SetTotalJobs(ComputeElementRecord job, Element element)
  throws Exception {
    IntegerElement el = job.getTotalJobs();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetTotalJobs", "parsing",
          " found a second TotalJobs field in the xml file", false);
      return;
    }
    el = new IntegerElement();
    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      Attribute a = (Attribute)i.next();
      if (a.getName().equalsIgnoreCase("description")) {
        el.setDescription(a.getValue());
      }
    }
    el.setValue((new Long(element.getText())).longValue());
    job.setTotalJobs(el);
  }

  public static void SetWaitingJobs(ComputeElementRecord job, Element element)
  throws Exception {
    IntegerElement el = job.getWaitingJobs();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetWaitingJobs", "parsing",
          " found a second WaitingJobs field in the xml file", false);
      return;
    }
    el = new IntegerElement();
    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      Attribute a = (Attribute)i.next();
      if (a.getName().equalsIgnoreCase("description")) {
        el.setDescription(a.getValue());
      }
    }
    el.setValue((new Long(element.getText())).longValue());
    job.setWaitingJobs(el);
  }

  public ComputeElementRecordLoader() {
  }
}

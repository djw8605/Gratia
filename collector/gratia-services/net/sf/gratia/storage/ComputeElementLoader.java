package net.sf.gratia.storage;

import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Element;

import net.sf.gratia.storage.RecordIdentity;
import net.sf.gratia.storage.StringElement;

import net.sf.gratia.util.Logging;

/**
 * <p>Title: ComputeElementLoader</p>
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

public class ComputeElementLoader extends RecordLoader {

  public ArrayList ReadRecords(Element eroot) throws Exception {
    ArrayList records = new ArrayList();
    if (eroot.getName() == "ComputeElement") {
      // The current element is a GlueCe record node.  Use it to populate a GlueRecord object            	
      records.add(ReadRecord(eroot));
    }  else if (eroot.getName() == "RecordEnvelope") {
      for (Iterator i = eroot.elementIterator(); i.hasNext();) {
        Element element = (Element) i.next();
        if (element.getName() == "ComputeElement") {
          //The current element is a job usage record node.  Use it to populate a JobUsageRecord object
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
    ComputeElement job = new ComputeElement();
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
        else if (sub.getName().equalsIgnoreCase("CEName")) {
          SetCEName(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Cluster")) {
          SetCluster(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("HostName")) {
          SetHostName(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Timestamp")) {
          SetTimestamp(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("LrmsType")) {
          SetLrmsType(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("LrmsVersion")) {
          SetLrmsVersion(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("MaxRunningJobs")) {
          SetMaxRunningJobs(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("MaxTotalJobs")) {
          SetMaxTotalJobs(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("AssignedJobSlots")) {
          SetAssignedJobSlots(job, sub);
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

  public static void SetUniqueID(ComputeElement job, Element element)
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

  public static void SetCEName(ComputeElement job, Element element)
  throws Exception {
    StringElement el = job.getCEName();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetCEName", "parsing",
          " found a second CEName field in the xml file", false);
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
    job.setCEName(el);
  }

  public static void SetCluster(ComputeElement job, Element element)
  throws Exception {
    StringElement el = job.getCluster();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetCluster", "parsing",
          " found a second Cluster field in the xml file", false);
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
    job.setCluster(el);
  }

  public static void SetHostName(ComputeElement job, Element element)
  throws Exception {
    StringElement el = job.getHostName();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetHostName", "parsing",
          " found a second HostName field in the xml file", false);
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
    job.setHostName(el);
  }

  public static void SetTimestamp(ComputeElement job, Element element)
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

  public static void SetLrmsType(ComputeElement job, Element element)
  throws Exception {
    StringElement el = job.getLrmsType();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetLrmsType", "parsing",
          " found a second LrmsType field in the xml file", false);
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
    job.setLrmsType(el);
  }

  public static void SetLrmsVersion(ComputeElement job, Element element)
  throws Exception {
    StringElement el = job.getLrmsVersion();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetLrmsVersion", "parsing",
          " found a second LrmsVersion field in the xml file", false);
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
    job.setLrmsVersion(el);
  }

  public static void SetMaxRunningJobs(ComputeElement job, Element element)
  throws Exception {
    IntegerElement el = job.getMaxRunningJobs();
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
    job.setMaxRunningJobs(el);
  }

  public static void SetMaxTotalJobs(ComputeElement job, Element element)
  throws Exception {
    IntegerElement el = job.getMaxTotalJobs();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetMaxTotalJobs", "parsing",
          " found a second MaxTotalJobs field in the xml file", false);
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
    job.setMaxTotalJobs(el);
  }

  public static void SetAssignedJobSlots(ComputeElement job, Element element)
  throws Exception {
    IntegerElement el = job.getAssignedJobSlots();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetAssignedJobSlots", "parsing",
          " found a second AssignedJobSlots field in the xml file", false);
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
    job.setAssignedJobSlots(el);
  }

  public static void SetStatus(ComputeElement job, Element element)
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

  public ComputeElementLoader() {
  }
}

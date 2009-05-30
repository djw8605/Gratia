package net.sf.gratia.storage;

import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Element;

import net.sf.gratia.storage.StringElement;

import net.sf.gratia.util.Logging;

/**
 * <p>Title: SubclusterLoader</p>
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

public class SubclusterLoader extends RecordLoader {

  public ArrayList ReadRecords(Element eroot) throws Exception {
    ArrayList records = new ArrayList();
    if (eroot.getName() == "Subcluster") {
      // The current element is a Subcluster node.  Use it to populate a Subcluster object            	
      records.add(ReadRecord(eroot));
    }  else if (eroot.getName() == "RecordEnvelope") {
      for (Iterator i = eroot.elementIterator(); i.hasNext();) {
        Element element = (Element) i.next();
        if (element.getName() == "Subcluster") {
          //The current element is a subcluster node.  Use it to populate a Subcluster object
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
    Subcluster job = new Subcluster();
    job.addRawXml(element.asXML());

    for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
      // Attribute a = (Attribute)
      i.next();
      // Skip all attribute of Subcluster for now
    }

    for (Iterator i = element.elementIterator(); i.hasNext(); ) {
      Element sub = (Element)i.next();
      // System.out.println("" + sub.GetName())
      try {
        if (sub.getName().equalsIgnoreCase("UniqueID")) {
          SetUniqueID(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Name")) {
          SetName(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Cluster")) {
          SetCluster(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Platform")) {
          SetPlatform(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("OS")) {
          SetOS(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("OSVersion")) {
          SetOSVersion(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Timestamp")) {
          SetTimestamp(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Cores")) {
          SetCores(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Hosts")) {
          SetHosts(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Cpus")) {
          SetCpus(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("RAM")) {
          SetRAM(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("Processor")) {
          SetProcessor(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("BenchmarkName")) {
          SetBenchmarkName(job, sub);
        }
        else if (sub.getName().equalsIgnoreCase("BenchmarkValue")) {
          SetBenchmarkValue(job, sub);
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

  public static void SetUniqueID(Subcluster job, Element element)
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

  public static void SetName(Subcluster job, Element element)
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

  public static void SetCluster(Subcluster job, Element element)
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

  public static void SetPlatform(Subcluster job, Element element)
  throws Exception {
    StringElement el = job.getPlatform();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetPlatform", "parsing",
          " found a second Platform field in the xml file", false);
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
    job.setPlatform(el);
  }

  public static void SetOS(Subcluster job, Element element)
  throws Exception {
    StringElement el = job.getOS();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetOS", "parsing",
          " found a second OS field in the xml file", false);
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
    job.setOS(el);
  }

  public static void SetOSVersion(Subcluster job, Element element)
  throws Exception {
    StringElement el = job.getOSVersion();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetOSVersion", "parsing",
          " found a second OSVersion field in the xml file", false);
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
    job.setOSVersion(el);
  }

  public static void SetTimestamp(Subcluster job, Element element)
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

  public static void SetCores(Subcluster job, Element element)
  throws Exception {
    IntegerElement el = job.getCores();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetCores", "parsing",
          " found a second Cores field in the xml file", false);
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
    job.setCores(el);
  }

  public static void SetHosts(Subcluster job, Element element)
  throws Exception {
    IntegerElement el = job.getHosts();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetHosts", "parsing",
          " found a second Hosts field in the xml file", false);
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
    job.setHosts(el);
  }

  public static void SetCpus(Subcluster job, Element element)
  throws Exception {
    IntegerElement el = job.getCpus();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetCpus", "parsing",
          " found a second Cpus field in the xml file", false);
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
    job.setCpus(el);
  }

  public static void SetRAM(Subcluster job, Element element)
  throws Exception {
    IntegerElement el = job.getRAM();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetRAM", "parsing",
          " found a second RAM field in the xml file", false);
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
    job.setRAM(el);
  }

  public static void SetProcessor(Subcluster job, Element element)
  throws Exception {
    StringElement el = job.getProcessor();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetProcessor", "parsing",
          " found a second Processor field in the xml file", false);
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
    job.setProcessor(el);
  }

  public static void SetBenchmarkName(Subcluster job, Element element)
  throws Exception {
    StringElement el = job.getBenchmarkName();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetBenchmarkName", "parsing",
          " found a second BenchmarkName field in the xml file", false);
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
    job.setBenchmarkName(el);
  }

  public static void SetBenchmarkValue(Subcluster job, Element element)
  throws Exception {
    StringElement el = job.getBenchmarkValue();
    if (el != null /* job identity already set */) {
      Utils.GratiaError("SetBenchmarkValue", "parsing",
          " found a second BenchmarkValue field in the xml file", false);
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
    job.setBenchmarkValue(el);
  }

  public SubclusterLoader() {
  }
}

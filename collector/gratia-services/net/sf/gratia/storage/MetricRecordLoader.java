package net.sf.gratia.storage;

import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Element;

import net.sf.gratia.storage.RecordIdentity;
import net.sf.gratia.storage.StringElement;

import net.sf.gratia.util.Logging;

/**
 * <p>Title: MetricRecordLoader</p>
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
 * More Updates by Arvind Gopu 2007-10-19
 * 
 */

public class MetricRecordLoader extends RecordLoader {
   public ArrayList ReadRecords(Element eroot) throws Exception {
      ArrayList records = new ArrayList();
      if (eroot.getName() == "MetricRecord") {
         // The current element is a metric record node.  Use it to populate a MetricRecord object            	
         records.add(ReadRecord(eroot));
      }  else if (eroot.getName() == "RecordEnvelope") {
         for (Iterator i = eroot.elementIterator(); i.hasNext();) {
            Element element = (Element) i.next();
            if (element.getName() == "MetricRecord") {
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
      MetricRecord job = new MetricRecord();
      job.addRawXml(element.asXML());
      
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         // Attribute a = (Attribute)
         i.next();
         // Skip all attribute of MetricRecord for now
      }
      
      for (Iterator i = element.elementIterator(); i.hasNext(); ) {
         Element sub = (Element)i.next();
         // System.out.println("" + sub.GetName())
         try {
            if (sub.getName().equalsIgnoreCase("MetricName")) {
               SetMetricName(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("MetricType")) {
               SetMetricType(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("MetricStatus")) {
               SetMetricStatus(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("Timestamp")) {
               SetTimestamp(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("ServiceType")) {
               SetServiceType(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("ServiceUri")) {
               SetServiceUri(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("GatheredAt")) {
               SetGatheredAt(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("SummaryData")) {
               SetSummaryData(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("DetailsData")) {
               SetDetailsData(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("PerformanceData")) {
               SetPerformanceData(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("VoName")) {
               SetVoName(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("SamUploadFlag")) {
               SetSamUploadFlag(job, sub);
            }
            else if (sub.getName().equalsIgnoreCase("HostName")) {
               SetHostName(job, sub);
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
   
   public void SetMetricName(MetricRecord job, Element element)
   throws Exception {
      StringElement el = job.getMetricName();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetMetricName", "parsing",
                           " found a second JobName field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "MetricName");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "MetricName");
      job.setMetricName(el);
   }
   
   public void SetMetricType(MetricRecord job, Element element)
   throws Exception {
      StringElement el = job.getMetricType();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetMetricType", "parsing",
                           " found a second JobType field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "MetricType");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "MetricType");
      job.setMetricType(el);
   }
   
   public void SetMetricStatus(MetricRecord job, Element element)
   throws Exception {
      StringElement el = job.getMetricStatus();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetMetricStatus", "parsing",
                           " found a second Status field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "MetricStatus");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "MetricStatus");
      job.setMetricStatus(el);
   }
   
   public void SetTimestamp(MetricRecord job, Element element)
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
   
   public void SetServiceType(MetricRecord job, Element element)
   throws Exception {
      StringElement el = job.getServiceType();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetServiceType", "parsing",
                           " found a second JobName field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "ServiceType");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "ServiceType");
      job.setServiceType(el);
   }
   
   public void SetServiceUri(MetricRecord job, Element element)
   throws Exception {
      StringElement el = job.getServiceUri();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetServiceUri", "parsing",
                           " found a second JobName field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "ServiceUri");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "ServiceUri");
      job.setServiceUri(el);
   }
   
   public void SetGatheredAt(MetricRecord job, Element element)
   throws Exception {
      StringElement el = job.getGatheredAt();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetGatheredAt", "parsing",
                           " found a second JobName field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "GatheredAt");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "GatherAt");
      job.setGatheredAt(el);
   }
   
   public void SetSummaryData(MetricRecord job, Element element)
   throws Exception {
      StringElement el = job.getSummaryData();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetSummaryData", "parsing",
                           " found a second JobName field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "SummaryData");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "SummaryData");
      job.setSummaryData(el);
   }
   
   public void SetDetailsData(MetricRecord job, Element element)
   throws Exception {
      StringElement el = job.getDetailsData();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetDetailsData", "parsing",
                           " found a second JobName field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "DetailsData");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "DetailsData");
      job.setDetailsData(el);
   }
   
   public void SetPerformanceData(MetricRecord job, Element element)
   throws Exception {
      StringElement el = job.getPerformanceData();
      if (el != null /* Performance Data already set */) {
         Utils.GratiaError("SetPerformanceData", "parsing",
                           " found a second PerformanceData field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "PerformanceData");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "PerformanceData");
      job.setPerformanceData(el);
   }
   
   public void SetVoName(MetricRecord job, Element element)
   throws Exception {
      StringElement el = job.getVoName();
      if (el != null /* Vo name already set */) {
         Utils.GratiaError("SetVoName", "parsing",
                           " found a second VoName field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "VoName");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "VoName");
      job.setVoName(el);
   }
   
   public void SetSamUploadFlag(MetricRecord job, Element element)
   throws Exception {
      IntegerElement el = job.getSamUploadFlag();
      if (el != null /* SamUploadFlag already set */) {
         Utils.GratiaError("SetSamUploadFlag", "parsing",
                           " found a second SamUploadFlag field in the xml file", false);
         return;
      }
      el = new IntegerElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "SamUploadFlag");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      el.setValue((new Long(element.getText())).longValue()); 
      job.setSamUploadFlag(el);
   }
   
   public void SetHostName(MetricRecord job, Element element)
   throws Exception {
      StringElement el = job.getHostName();
      if (el != null /* job identity already set */) {
         Utils.GratiaError("SetHostName", "parsing",
                           " found a second JobName field in the xml file", false);
         return;
      }
      el = new StringElement();
      for (Iterator i = element.attributeIterator(); i.hasNext(); ) {
         Attribute a = (Attribute)i.next();
         if (a.getName().equalsIgnoreCase("description")) {
            SetLimitedDescription(el, job, element, a, 255, "HostName");
         } else {
               extraXmlAttribute(element,a);
            }
      }
      SetLimitedTextField(el, job, element, 255, "HostName");
      job.setHostName(el);
   }
   
   public MetricRecordLoader() {
   }
}

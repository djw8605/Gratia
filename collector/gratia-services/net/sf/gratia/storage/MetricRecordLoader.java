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

    public static void SetMetricName(MetricRecord job, Element element)
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
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setMetricName(el);
    }

    public static void SetMetricType(MetricRecord job, Element element)
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
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setMetricType(el);
    }

    public static void SetMetricStatus(MetricRecord job, Element element)
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
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setMetricStatus(el);
    }

    public static void SetTimestamp(MetricRecord job, Element element)
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

    public static void SetServiceType(MetricRecord job, Element element)
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
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setServiceType(el);
    }

    public static void SetServiceUri(MetricRecord job, Element element)
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
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setServiceUri(el);
    }

    public static void SetGatheredAt(MetricRecord job, Element element)
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
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setGatheredAt(el);
    }

    public static void SetSummaryData(MetricRecord job, Element element)
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
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setSummaryData(el);
    }

    public static void SetDetailsData(MetricRecord job, Element element)
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
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setDetailsData(el);
    }

    public static void SetPerformanceData(MetricRecord job, Element element)
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
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setPerformanceData(el);
    }

    public static void SetVoName(MetricRecord job, Element element)
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
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setVoName(el);
    }

    public static void SetSamUploadFlag(MetricRecord job, Element element)
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
                el.setDescription(a.getValue());
            }
        }
        el.setValue((new Long(element.getText())).longValue()); 
        job.setSamUploadFlag(el);
    }

    public static void SetHostName(MetricRecord job, Element element)
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
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setHostName(el);
    }

    public MetricRecordLoader() {
    }
}

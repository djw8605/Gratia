package net.sf.gratia.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Element;

import net.sf.gratia.storage.RecordIdentity;
import net.sf.gratia.storage.StringElement;

import net.sf.gratia.util.Logging;

/**
 * <p>Title: UsageRecordLoader</p>
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
 */
public class UsageRecordLoader extends RecordLoader {
    public ArrayList ReadRecords(Element eroot) throws Exception {
        ArrayList usageRecords = new ArrayList();

        if (eroot.getName() == "JobUsageRecord"
            || eroot.getName() == "UsageRecord"
            || eroot.getName() == "Usage"
            || eroot.getName() == "UsageRecordType") {
            // The current element is a job usage record node.  Use it to populate a JobUsageRecord object            	
            Record job = ReadRecord(eroot);

            // Add this populated job usage record to the usage records array list
            usageRecords.add(job);
        } else if (eroot.getName().equals("UsageRecords")
                   || eroot.getName().equals("RecordEnvelope")) {
            // This is a usage records node
            // which should contain one to many job usage record nodes so start a loop through its children
            for (Iterator i = eroot.elementIterator(); i.hasNext();) {
                Element element = (Element) i.next();
                if (element.getName().equals("JobUsageRecord") 
                    || element.getName().equals("UsageRecord")
                    || element.getName().equals("Usage")
                    || element.getName().equals("UsageRecordType")) {
                    //The current element is a job usage record node.  Use it to populate a JobUsageRecord object
                    Record job = ReadRecord(element);
                    usageRecords.add(job);
                } else if (eroot.getName().equals("UsageRecords")) { // Definitely expected JobUsageRecord here
                    // Unexpected element
                    throw new Exception("Unexpected element: "
                                        + element.getName() + "\n" + element);
                } else {
                    // Don't care
                }
            }
        }
        if (usageRecords.size() == 0) {
            return null;
        }
        return usageRecords;
    }

    public Record ReadRecord(Element element) throws Exception {
        JobUsageRecord job = new JobUsageRecord();
        job.addRawXml(element.asXML());

        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            // Attribute a = (Attribute)
            i.next();
            // Skip all attribute of JobUsageRecord for now
        }

        for (Iterator i = element.elementIterator(); i.hasNext();) {
            Element sub = (Element) i.next();
            // System.out.println("" + sub.GetName())
            try {
                if (sub.getName().equalsIgnoreCase("JobIdentity")) {
                    SetJobIdentity(job, sub);
                } else if (sub.getName().equalsIgnoreCase("UserIdentity")) {
                    SetUserIdentity(job, sub);
                } else if (sub.getName().equalsIgnoreCase("JobName")) {
                    SetJobName(job, sub);
                } else if (sub.getName().equalsIgnoreCase("Status")) {
                    SetStatus(job, sub);
                } else if (sub.getName().equalsIgnoreCase("WallDuration")) {
                    SetWallDuration(job, sub);
                } else if (sub.getName().equalsIgnoreCase("CpuDuration")) {
                    SetCpuDuration(job, sub);
                } else if (sub.getName().equalsIgnoreCase("EndTime")) {
                    SetEndTime(job, sub);
                } else if (sub.getName().equalsIgnoreCase("StartTime")) {
                    SetStartTime(job, sub);
                } else if (sub.getName().equalsIgnoreCase("TimeDuration")) {
                    SetTimeDuration(job, sub);
                } else if (sub.getName().equalsIgnoreCase("TimeInstant")) {
                    SetTimeInstant(job, sub);
                } else if (sub.getName().equalsIgnoreCase("MachineName")) {
                    SetMachineName(job, sub);
                } else if (sub.getName().equalsIgnoreCase("Host")) {
                    SetHost(job, sub);
                } else if (sub.getName().equalsIgnoreCase("SubmitHost")) {
                    SetSubmitHost(job, sub);
                } else if (sub.getName().equalsIgnoreCase("Queue")) {
                    SetQueue(job, sub);
                } else if (sub.getName().equalsIgnoreCase("ProjectName")) {
                    SetProjectName(job, sub);
                } else if (sub.getName().equalsIgnoreCase("Network")) {
                    SetNetwork(job, sub);
                } else if (sub.getName().equalsIgnoreCase("Disk")) {
                    SetDisk(job, sub);
                } else if (sub.getName().equalsIgnoreCase("Memory")) {
                    SetMemory(job, sub);
                } else if (sub.getName().equalsIgnoreCase("Swap")) {
                    SetSwap(job, sub);
                } else if (sub.getName().equalsIgnoreCase("NodeCount")) {
                    SetNodeCount(job, sub);
                } else if (sub.getName().equalsIgnoreCase("Njobs")) {
                    SetNjobs(job, sub);
                } else if (sub.getName().equalsIgnoreCase("Processors")) {
                    SetProcessors(job, sub);
                } else if (sub.getName().equalsIgnoreCase("ServiceLevel")) {
                    SetServiceLevel(job, sub);
                } else if (sub.getName().equalsIgnoreCase("Charge")) {
                    SetCharge(job, sub);
                } else if (sub.getName().equalsIgnoreCase("Resource")) {
                    AddResource(job, sub);
                } else if (sub.getName().equalsIgnoreCase("PhaseResource")) {
                    AddPhaseResource(job, sub);
                } else if (sub.getName().equalsIgnoreCase("VolumeResource")) {
                    AddVolumeResource(job, sub);
                } else if (sub.getName().equalsIgnoreCase("ConsumableResource")) {
                    AddConsumableResource(job, sub);
                } else {
                    ReadCommonRecord(job,sub);
                }
            } catch (Exception e) {
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

    public static void SetJobIdentity(JobUsageRecord job, Element element)
            throws Exception {
        JobIdentity id = job.getJobIdentity();
        if (id != null /* job identity already set */) {
            Utils.GratiaError("SetJobIdentity", "parsing",
                    " found a second JobIdentity field in the xml file", false);
            job.addExtraXml(element.asXML());
            return;
        }
        // No known attributes.
        String extras = "";
        for (Iterator i = element.elementIterator(); i.hasNext();) {
            Element sub = (Element) i.next();
            if (sub.getName().equalsIgnoreCase("GlobalJobId")) {
                if (id == null)
                    id = new JobIdentity();
                id.setGlobalJobId(sub.getText());
            } else if (sub.getName().equalsIgnoreCase("LocalJobId")) {
                if (id == null)
                    id = new JobIdentity();
                id.setLocalJobId(sub.getText());
            } else if (sub.getName().equalsIgnoreCase("ProcessId")) {
                if (id == null)
                    id = new JobIdentity();
                id.addProcessId(sub.getText());
            } else {
                extras = extras + sub.asXML();
            }
        }
        if (id != null)
            job.setJobIdentity(id);
        if (extras.length() > 0) {
            extras = "<JobIdentity>" + extras + "</JobIdentity>";
            job.addExtraXml(extras);
        }
    }

    public static KeyInfoType genKeyInfo(Element element) throws Exception {
        KeyInfoType info = new KeyInfoType();
        if (element.getName().equalsIgnoreCase("DN")) { // New DN element
            info.setId(null);
            info.setContent(element.getText());
        } else { // Real KeyInfo block
            for (Iterator i = element.attributeIterator(); i.hasNext();) {
                Attribute a = (Attribute) i.next();
                if (a.getName().equalsIgnoreCase("Id")) {
                    info.setId(a.getValue());
                }
            }
            String keyContent = "";
            for (Iterator i = element.elementIterator(); i.hasNext();) {
                Element sub = (Element) i.next();
                keyContent = keyContent + "\t" + sub.asXML();
            }
            info.setContent(keyContent);
        }
        return info;
    }

    public static void SetUserIdentity(JobUsageRecord job, Element element)
            throws Exception {
        UserIdentity id = job.getUserIdentity();
        if (false /* job identity already set */) {
            Utils.GratiaError(
                            "SetUserIdentity",
                            "parsing",
                            " found a second UserIdentity field in the xml file",
                            false);
        }
        // No known attributes.
        String extras = "";
        for (Iterator i = element.elementIterator(); i.hasNext();) {
            Element sub = (Element) i.next();
            if (sub.getName().equalsIgnoreCase("GlobalUsername")) {
                if (id == null)
                    id = new UserIdentity();
                id.setGlobalUsername(sub.getText());
            } else if (sub.getName().equalsIgnoreCase("LocalUserId")) {
                if (id == null)
                    id = new UserIdentity();
                id.setLocalUserId(sub.getText());
            } else if (sub.getName().equalsIgnoreCase("KeyInfo")) {
                if (id == null)
                    id = new UserIdentity();
                if (id.getKeyInfo() == null) // Subordinate to DN field
                    id.setKeyInfo(genKeyInfo(sub));
            } else if (sub.getName().equalsIgnoreCase("DN")) {
                if (id == null)
                    id = new UserIdentity();
                id.setKeyInfo(genKeyInfo(sub));
            } else if (sub.getName().equalsIgnoreCase("VOName")) {
                if (id == null)
                    id = new UserIdentity();
                id.setVOName(sub.getText());
            } else if (sub.getName().equalsIgnoreCase("ReportableVOName")) {
                if (id == null)
                    id = new UserIdentity();
                id.setReportableVOName(sub.getText());
            } else if (sub.getName().equalsIgnoreCase("CommonName")) {
                if (id == null)
                    id = new UserIdentity();
                id.setCommonName(sub.getText());
            } else {
                extras = extras + sub.asXML();
            }
        }
        if (id != null)
            job.setUserIdentity(id);
        if (extras.length() > 0) {
            extras = "<UserIdentity>" + extras + "</UserIdentity>";
            job.addExtraXml(extras);
        }
    }

    public static void SetJobName(JobUsageRecord job, Element element)
            throws Exception {
        StringElement el = job.getJobName();
        if (el != null /* job identity already set */) {
            Utils.GratiaError("SetJobName", "parsing",
                    " found a second JobName field in the xml file", false);
            job.addExtraXml(element.asXML());
            return;
        }
        el = new StringElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setJobName(el);
    }

    public static void SetStatus(JobUsageRecord job, Element element)
            throws Exception {
        StringElement el = job.getStatus();
        if (el != null /* job identity already set */) {
            Utils.GratiaError("SetStatus", "parsing",
                    " found a second Status field in the xml file", false);
            job.addExtraXml(element.asXML());
            return;
        }
        el = new StringElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setStatus(el);
    }

    public static void SetWallDuration(JobUsageRecord job, Element element)
            throws Exception {
        DurationElement el = job.getWallDuration();
        if (el != null /* job identity already set */) {
            Utils.GratiaError(
                            "SetWallDuration",
                            "parsing",
                            " found a second WallDuration field in the xml file",
                            false);
            job.addExtraXml(element.asXML());
            return;
        }
        el = new DurationElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            }
        }
        // Duration d = new Duration();

        el.setValue(element.getText());
        job.setWallDuration(el);
    }

    public static void SetCpuDuration(JobUsageRecord job, Element element)
            throws Exception {
        DurationElement el = job.getWallDuration();
        String usage = "user";
        el = new DurationElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("type")
                    || a.getName().equalsIgnoreCase("usageType")) {
                usage = a.getValue();
            }
        }
        el.setValue(element.getText());

        if (usage.equalsIgnoreCase("user")) {
            if (job.getCpuUserDuration() != null) {
                Utils.GratiaError(
                                "SetCpuDuration",
                                "parsing",
                                " found a second CpuUserDuration field in the xml file",
                                true);
                job.addExtraXml(element.asXML());
                return;
            }
            job.setCpuUserDuration(el);
        } else if (usage.equalsIgnoreCase("system")) {
            if (job.getCpuSystemDuration() != null) {
                Utils.GratiaError(
                                "SetCpuDuration",
                                "parsing",
                                " found a second CpuSystemDuration field in the xml file",
                                true);
                job.addExtraXml(element.asXML());
                return;
            }
            job.setCpuSystemDuration(el);
        } else {
            Utils.GratiaError("SetCpuDuration", "parsing",
                    " found an unknown usageType " + usage, true);

            job.addExtraXml(element.asXML());
        }
    }

    public static void SetEndTime(JobUsageRecord job, Element element)
            throws Exception {
        DateElement el = job.getEndTime();
        if (el != null /* job identity already set */) {
            Utils.GratiaError("SetEndTime", "parsing",
                    " found a second EndTime field in the xml file", false);
            job.addExtraXml(element.asXML());
            return;
        }
        el = new DateElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            }
        }
        // Duration d = new Duration();

        el.setValue(element.getText());
        job.setEndTime(el);
    }

    public static void SetStartTime(JobUsageRecord job, Element element)
            throws Exception {
        DateElement el = job.getStartTime();
        if (el != null /* job identity already set */) {
            Utils.GratiaError("SetStartTime", "parsing",
                    " found a second StartTime field in the xml file", false);
            job.addExtraXml(element.asXML());
            return;
        }
        el = new DateElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            }
        }

        el.setValue(element.getText());
        job.setStartTime(el);
    }

    public static void SetTimeDuration(JobUsageRecord job, Element element)
            throws Exception {
        DurationElement el = new DurationElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("type")) {
                el.setType(a.getValue());
            }
        }
        el.setValue(element.getText());

        List l = job.getTimeDuration();
        if (l == null)
            l = new java.util.LinkedList();
        l.add(el);
        job.setTimeDuration(l);
    }

    public static void SetTimeInstant(JobUsageRecord job, Element element)
            throws Exception {
        DateElement el = new DateElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("type")) {
                el.setType(a.getValue());
            }
        }
        el.setValue(element.getText());

        List l = job.getTimeInstant();
        if (l == null)
            l = new java.util.LinkedList();
        l.add(el);
        job.setTimeInstant(l);
    }

    public static void SetMachineName(JobUsageRecord job, Element element)
            throws Exception {
        StringElement el = job.getMachineName();
        if (el != null /* job identity already set */) {
            Utils.GratiaError("SetMachineName", "parsing",
                    " found a second MachineName field in the xml file", false);
            job.addExtraXml(element.asXML());
            return;
        }
        el = new StringElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setMachineName(el);
    }

    public static void SetHost(JobUsageRecord job, Element element)
            throws Exception {
        StringElement el = job.getHost();
        boolean primary = false;
        if (el == null) {
            el = new StringElement();
        }
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                String desc = el.getDescription();
                if (desc == null)
                    desc = "";
                else
                    desc = desc + " ; ";
                desc = desc + a.getValue();
                el.setDescription(desc);
            } else if (a.getName().equalsIgnoreCase("primary")) {
                primary = (a.getValue().equalsIgnoreCase("true"));
            }
        }
        String val = el.getValue();
        if (val == null)
            val = "";
        else
            val = val + " ; ";
        val = val + element.getText();
        if (primary)
            val = val + " (primary) ";
        el.setValue(val);
        job.setHost(el);
    }

    public static void SetSubmitHost(JobUsageRecord job, Element element)
            throws Exception {
        StringElement el = job.getSubmitHost();
        if (el != null /* job identity already set */) {
            Utils.GratiaError("SetSubmitHost", "parsing",
                    " found a second SubmitHost field in the xml file", false);
            job.addExtraXml(element.asXML());
            return;
        }
        el = new StringElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setSubmitHost(el);
    }

    public static void SetQueue(JobUsageRecord job, Element element)
            throws Exception {
        StringElement el = job.getQueue();
        if (el != null /* job identity already set */) {
            Utils.GratiaError("SetQueue", "parsing",
                    " found a second Queue field in the xml file", false);
            job.addExtraXml(element.asXML());
            return;
        }
        el = new StringElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            }
        }
        el.setValue(element.getText());
        job.setQueue(el);
    }

    public static void SetProjectName(JobUsageRecord job, Element element)
            throws Exception {
        StringElement el = job.getProjectName();
        if (el == null) {
            el = new StringElement();
        }
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                String desc = el.getDescription();
                if (desc == null)
                    desc = "";
                else
                    desc = desc + " ; ";
                desc = desc + a.getValue();
                el.setDescription(desc);
            }
        }
        String val = el.getValue();
        if (val == null)
            val = "";
        else
            val = val + " ; ";
        val = val + element.getText();
        el.setValue(val);
        job.setProjectName(el);
    }

    public static void SetNetwork(JobUsageRecord job, Element element)
            throws Exception {
        ResourceElement el = new ResourceElement();
        el.setMetrics("total");
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("metrics")) {
                el.setMetrics(a.getValue());
            } else if (a.getName().equalsIgnoreCase("phaseUnit")) {
                el.setPhaseUnit(a.getValue());
            } else if (a.getName().equalsIgnoreCase("storageUnit")) {
                el.setStorageUnit(a.getValue());
            }
        }
        el.setValue((new Double(element.getText())).doubleValue());
        List l = job.getNetwork();
        if (l == null)
            l = new java.util.LinkedList();
        l.add(el);
        job.setNetwork(l);
    }

    public static void SetDisk(JobUsageRecord job, Element element)
            throws Exception {
        ResourceElement el = new ResourceElement();
        el.setMetrics("total");
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("metrics")) {
                el.setMetrics(a.getValue());
            } else if (a.getName().equalsIgnoreCase("phaseUnit")) {
                el.setPhaseUnit(a.getValue());
            } else if (a.getName().equalsIgnoreCase("storageUnit")) {
                el.setStorageUnit(a.getValue());
            } else if (a.getName().equalsIgnoreCase("type")) {
                el.setType(a.getValue());
            }
        }
        el.setValue((new Double(element.getText())).doubleValue());
        List l = job.getDisk();
        if (l == null)
            l = new java.util.LinkedList();
        l.add(el);
        job.setDisk(l);
    }

    public static void SetMemory(JobUsageRecord job, Element element)
            throws Exception {
        ResourceElement el = new ResourceElement();
        el.setMetrics("total");
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("metrics")) {
                el.setMetrics(a.getValue());
            } else if (a.getName().equalsIgnoreCase("phaseUnit")) {
                el.setPhaseUnit(a.getValue());
            } else if (a.getName().equalsIgnoreCase("storageUnit")) {
                el.setStorageUnit(a.getValue());
            } else if (a.getName().equalsIgnoreCase("type")) {
                el.setType(a.getValue());
            }
        }
        el.setValue((new Double(element.getText())).doubleValue());
        List l = job.getMemory();
        if (l == null)
            l = new java.util.LinkedList();
        l.add(el);
        job.setMemory(l);
    }

    public static void SetSwap(JobUsageRecord job, Element element)
            throws Exception {
        ResourceElement el = new ResourceElement();
        el.setMetrics("total");
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("metrics")) {
                el.setMetrics(a.getValue());
            } else if (a.getName().equalsIgnoreCase("phaseUnit")) {
                el.setPhaseUnit(a.getValue());
            } else if (a.getName().equalsIgnoreCase("storageUnit")) {
                el.setStorageUnit(a.getValue());
            } else if (a.getName().equalsIgnoreCase("type")) {
                el.setType(a.getValue());
            }
        }
        el.setValue((new Double(element.getText())).doubleValue());
        List l = job.getSwap();
        if (l == null)
            l = new java.util.LinkedList();
        l.add(el);
        job.setSwap(l);
    }

    public static void SetNodeCount(JobUsageRecord job, Element element)
            throws Exception {
        IntegerElement el = job.getNodeCount();
        if (el != null && el.getValue() != 1) {
            Utils.GratiaError("SetNodeCount", "parsing",
                    " found a second NodeCount field in the xml file", false);
            job.addExtraXml(element.asXML());
            return;
        }
        el = new IntegerElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("metric")) {
                el.setMetric(a.getValue());
            }
        }
        el.setValue((new Long(element.getText())).longValue());
        job.setNodeCount(el);
    }

    public static void SetNjobs(JobUsageRecord job, Element element)
            throws Exception {
        IntegerElement el = job.getNjobs();
        if (el != null && el.getValue() != 1 /* job identity already set */) {
            Utils.GratiaError("SetNjobs", "parsing",
                    " found a second Njobs field in the xml file", false);
            job.addExtraXml(element.asXML());
            return;
        }
        el = new IntegerElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("metric")) {
                el.setMetric(a.getValue());
            }
        }
        el.setValue((new Long(element.getText())).longValue());
        job.setNjobs(el);
    }

    public static void SetProcessors(JobUsageRecord job, Element element)
            throws Exception {
        IntegerElement el = job.getProcessors();
        if (el != null /* job identity already set */) {
            Utils.GratiaError("SetProcessors", "parsing",
                    " found a second Processors field in the xml file", false);
            job.addExtraXml(element.asXML());
            return;
        }
        el = new IntegerElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("metric")) {
                el.setMetric(a.getValue());
            } else if (a.getName().equalsIgnoreCase("consumptionRate")) {
                el.setConsumptionRate((new Double(a.getValue())).doubleValue());
            }
        }
        el.setValue((new Long(element.getText())).longValue());
        job.setProcessors(el);
    }

    public static void SetServiceLevel(JobUsageRecord job, Element element)
            throws Exception {
        StringElement el = new StringElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("type")) {
                el.setType(a.getValue());
            }
        }
        el.setValue(element.getText());
        List l = job.getServiceLevel();
        if (l == null)
            l = new java.util.LinkedList();
        l.add(el);
        job.setServiceLevel(l);
    }

    public static void SetCharge(JobUsageRecord job, Element element)
            throws Exception {
        FloatElement el = job.getCharge();
        if (el != null /* job identity already set */) {
            Utils.GratiaError("SetCharge", "parsing",
                    " found a second Charge field in the xml file", false);
            return;
        }
        el = new FloatElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("unit")) {
                el.setUnit(a.getValue());
            } else if (a.getName().equalsIgnoreCase("formula")) {
                el.setFormula(a.getValue());
            }
        }
        el.setValue((new Double(element.getText())).doubleValue());
        job.setCharge(el);
    }

    public static void AddPhaseResource(JobUsageRecord job, Element element)
            throws Exception {
        ResourceElement el = new ResourceElement();
        el.setMetrics("total");
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("unit")) {
                el.setUnit(a.getValue());
            } else if (a.getName().equalsIgnoreCase("phaseUnit")) {
                el.setPhaseUnit(a.getValue());
            }
        }
        el.setValue((new Double(element.getText())).doubleValue());
        List l = job.getPhaseResource();
        if (l == null)
            l = new java.util.LinkedList();
        l.add(el);
        job.setPhaseResource(l);
    }

    public static void AddVolumeResource(JobUsageRecord job, Element element)
            throws Exception {
        ResourceElement el = new ResourceElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("unit")) {
                el.setUnit(a.getValue());
            } else if (a.getName().equalsIgnoreCase("storageUnit")) {
                el.setStorageUnit(a.getValue());
            }
        }
        el.setValue((new Double(element.getText())).doubleValue());
        List l = job.getVolumeResource();
        if (l == null)
            l = new java.util.LinkedList();
        l.add(el);
        job.setVolumeResource(l);
    }

    public static void AddConsumableResource(JobUsageRecord job, Element element)
            throws Exception {
        ResourceElement el = new ResourceElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equalsIgnoreCase("description")) {
                el.setDescription(a.getValue());
            } else if (a.getName().equalsIgnoreCase("unit")) {
                el.setUnit(a.getValue());
            }
        }
        el.setValue((new Double(element.getText())).doubleValue());
        List l = job.getConsumableResource();
        if (l == null)
            l = new java.util.LinkedList();
        l.add(el);
        job.setConsumableResource(l);
    }

    public static void AddResource(JobUsageRecord job, Element element)
            throws Exception {
        StringElement el = new StringElement();
        for (Iterator i = element.attributeIterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equals("description")) {
                el.setDescription(a.getValue());
                if (a.getValue().equalsIgnoreCase("GlobalUsername")) {
                    UserIdentity id = job.getUserIdentity();
                    if (id == null)
                        id = new UserIdentity();
                    id.setGlobalUsername(element.getText());
                    job.setUserIdentity(id);
                    return;
                } else if (a.getValue().equalsIgnoreCase("UserVOName")) {
                    UserIdentity id = job.getUserIdentity();
                    if (id == null)
                        id = new UserIdentity();
                    id.setVOName(element.getText().trim());
                    job.setUserIdentity(id);
                    return;
                } else if (a.getValue()
                        .equalsIgnoreCase("UserReportableVOName")) {
                    UserIdentity id = job.getUserIdentity();
                    if (id == null)
                        id = new UserIdentity();
                    id.setReportableVOName(element.getText().trim());
                    job.setUserIdentity(id);
                    return;
                } else if (a.getValue().equalsIgnoreCase("ResourceType")) {
                    String val = element.getText().trim();
                    if (val != null && val.length() > 0 ) {
                        StringElement rel = job.getResourceType();
                        if (rel == null) {
                            rel = new StringElement();
                        } /* else { maybe throw an exception! } */
                        rel.setValue(val);
                        job.setResourceType(rel);
                    }
                    return;
                }
            }
        }
        el.setValue(element.getText());
        List l = job.getResource();
        if (l == null)
            l = new java.util.LinkedList();
        l.add(el);
        job.setResource(l);
    }
    
    public UsageRecordLoader() {
    }
}

package net.sf.gratia.storage;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;

import net.sf.gratia.util.Logging;

/**
 * <p>Title: JobUsageRecord </p>
 *
 * <p>Description: JobUsageRecord is Gratia's in-memory representation of a GGF
 * Usage Record.  See http://www.gridforum.org/2003/ur-wg</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 */
public class JobUsageRecord extends Record {

   // Meta Information (not part of the xml file per se).
   // See Record class
   // Meta Information (from the xml file)
   // See Record class

   // Data Content
   private UserIdentity UserIdentity;  // This should be a list or set of UserIdentity
   private StringElement ResourceType;
   private JobIdentity JobIdentity;
   private StringElement JobName;
   private FloatElement Charge;
   private StringElement Status;
   private IntegerElement Njobs;
   private DurationElement WallDuration;
   private DurationElement CpuUserDuration;
   private DurationElement CpuSystemDuration;
   private IntegerElement NodeCount;
   private IntegerElement Processors;
   private DateElement StartTime;
   private DateElement EndTime;
   private StringElement MachineName;
   private StringElement SubmitHost;
   private StringElement Queue;
   private StringElement ProjectName;
   private StringElement Host;
   private List Disk;
   private List Memory;
   private List Swap;
   private List Network;
   private List TimeDuration;
   private List TimeInstant;
   private List ServiceLevel;
   private List PhaseResource;
   private List VolumeResource;
   private List ConsumableResource;
   private List Resource;
   private Set TDSet;
   private static Boolean wantSummary = true;

   public JobUsageRecord() {
      RecordIdentity = null; // new RecordIdentity();
      IntegerElement el = new IntegerElement();
      el.setValue(1);
      setNjobs(el);
      RawXml = "";
      ExtraXml = "";
      ServerDate = new Date();
      TDSet = null;
   }

   public String listToString(String name, List l) {
      String output = "";
      for (Iterator i = l.iterator(); i.hasNext();) {
         Object el = i.next();
         output = output + " " + name + ": " + el;
      }
      return output;
   }

   public void listAsXml(StringBuilder output, String name, List l) {
      for (Iterator i = l.iterator(); i.hasNext();) {
         XmlElement el = (XmlElement) i.next();
         output.append(el.asXml(name));
      }
   }

   @Override
   public String toString() {
      String output = "UsageRecord: Db Id: " + RecordId;
      if (ResourceType != null) {
         output = output + "RecordType: " + ResourceType;
      }
      if (ProbeName != null) {
         output = output + "ProbeName: " + ProbeName;
      }
      if (Grid != null) {
         output = output + "Grid: " + Grid;
      }
      if (RecordIdentity != null) {
         output = output + RecordIdentity;
      }
      if (JobIdentity != null) {
         output = output + JobIdentity;
      }
      if (UserIdentity != null) {
         output = output + " User: " + UserIdentity;
      }
      if (JobName != null) {
         output = output + " JobName: " + JobName;
      }
      if (Charge != null) {
         output = output + " Charge: " + Charge;
      }
      if (Status != null) {
         output = output + " Status: " + Status;
      }
      if (Njobs != null) {
         output = output + " Njobs: " + Njobs;
      }
      if (WallDuration != null) {
         output = output + " WallDuration: " + WallDuration;
      }
      if (CpuUserDuration != null) {
         output = output + " CpuUserDuration: " + CpuUserDuration;
      }
      if (CpuSystemDuration != null) {
         output = output + " CpuSystemDuration: " + CpuSystemDuration;
      }
      if (NodeCount != null) {
         output = output + " NodeCount: " + NodeCount;
      }
      if (Processors != null) {
         output = output + " Processors: " + Processors;
      }
      if (StartTime != null) {
         output = output + " StartTime: " + StartTime;
      }
      if (EndTime != null) {
         output = output + " EndTime: " + EndTime;
      }
      if (MachineName != null) {
         output = output + " MachineName: " + MachineName;
      }
      if (SiteName != null) {
         output = output + " SiteName: " + SiteName;
      }
      if (SubmitHost != null) {
         output = output + " SubmitHost: " + SubmitHost;
      }
      if (Queue != null) {
         output = output + " Queue: " + Queue;
      }
      if (ProjectName != null) {
         output = output + " ProjectName: " + ProjectName;
      }
      if (Host != null) {
         output = output + " Host: " + Host;
      }
      if (Disk != null) {
         output = output + listToString("Disk", Disk);
      }
      if (Memory != null) {
         output = output + listToString("Memory", Memory);
      }
      if (Swap != null) {
         output = output + listToString("Swap", Swap);
      }
      if (Network != null) {
         output = output + listToString("Network", Network);
      }
      if (TimeDuration != null) {
         output = output + listToString("TimeDuration", TimeDuration);
      }
      if (TimeInstant != null) {
         output = output + listToString("TimeInstant", TimeInstant);
      }
      if (ServiceLevel != null) {
         output = output + listToString("ServiceLevel", ServiceLevel);
      }
      if (PhaseResource != null) {
         output = output + listToString("PhaseResource", PhaseResource);
      }
      if (VolumeResource != null) {
         output = output + listToString("VolumeResource", VolumeResource);
      }
      if (ConsumableResource != null) {
         output = output + listToString("ConsumableResource", ConsumableResource);
      }
      if (Resource != null) {
         output = output + listToString("Resource", Resource);
      }
      if (Origins != null) {
         output = output + Origins.toString();
      }

      // if (RawXml != null) output = output + "\n" + RawXml;
      if (ExtraXml != null) {
         output = output + "\nExtraXml:\n" + ExtraXml;
      }
      return output;
   }

   public String asXML() {
      return asXML(false, false);
   }

   public String asXML(boolean formd5, boolean optional) {
      // If formd5 is true do not include
      //    RecordIdentity
      // in calculation.  If DatabaseMaintenance.UseJobUsageSiteName()
      // is also false, do not include
      //    UserIdentity
      //    SiteName
      //    Grid
      // in calculation.
      StringBuilder output = new StringBuilder(""); // ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      output.append("<JobUsageRecord xmlns=\"http://www.gridforum.org/2003/ur-wg\"\n");
      output.append("		xmlns:urwg=\"http://www.gridforum.org/2003/ur-wg\"\n");
      output.append("		xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
      // output.append("		xsi:schemaLocation=\"http://www.gridforum.org/2003/ur-wg file:/Users/bekah/Documents/GGF/URWG/urwg-schema.09.xsd\">\n");
      output.append("		xsi:schemaLocation=\"http://www.gridforum.org/2003/ur-wg file:///u:/OSG/urwg-schema.11.xsd\">\n");

      boolean formd5_optional = formd5 && optional /* == DatabaseMaintenance.UseJobUsageSiteName() */;

      // output.append("UsageRecord: Db Id: " + RecordId.asXml(output));
      if (!formd5) {
         if (RecordIdentity != null) {
            RecordIdentity.asXml(output);
         }
      }
      if (JobIdentity != null) {
         JobIdentity.asXml(output);
      }
      if (!formd5_optional) {
         if (UserIdentity != null) {
            UserIdentity.asXml(output);
         }
      }
      if (JobName != null) {
         JobName.asXml(output, "JobName");
      }
      if (Charge != null) {
         Charge.asXml(output, "Charge");
      }
      if (Status != null) {
         Status.asXml(output, "Status");
      }
      if (Njobs != null && Njobs.getValue() != 1) {
         Njobs.asXml(output, "Njobs");
      }
      if (WallDuration != null) {
         WallDuration.asXml(output, "WallDuration");
      }
      if (CpuUserDuration != null) {
         CpuUserDuration.asXml(output, "CpuDuration", "usageType", "user");
      }
      if (CpuSystemDuration != null) {
         CpuSystemDuration.asXml(output, "CpuDuration", "usageType", "system");
      }
      if (NodeCount != null) {
         NodeCount.asXml(output, "NodeCount");
      }
      if (Processors != null) {
         Processors.asXml(output, "Processors");
      }
      if (StartTime != null) {
         StartTime.asXml(output, "StartTime");
      }
      if (EndTime != null) {
         EndTime.asXml(output, "EndTime");
      }
      if (MachineName != null) {
         MachineName.asXml(output, "MachineName");
      }
      if (!formd5_optional) {
         if (SiteName != null) {
            SiteName.asXml(output, "SiteName");
         }
      }
      if (SubmitHost != null) {
         SubmitHost.asXml(output, "SubmitHost");
      }
      if (Queue != null) {
         Queue.asXml(output, "Queue");
      }
      if (ProjectName != null) {
         ProjectName.asXml(output, "ProjectName");
      }
      if (Host != null) {
         Host.asXml(output, "Host");
      }
      if (Disk != null) {
         listAsXml(output, "Disk", Disk);
      }
      if (Memory != null) {
         listAsXml(output, "Memory", Memory);
      }
      if (Swap != null) {
         listAsXml(output, "Swap", Swap);
      }
      if (Network != null) {
         listAsXml(output, "Network", Network);
      }
      if (TimeDuration != null) {
         listAsXml(output, "TimeDuration", TimeDuration);
      }
      if (TimeInstant != null) {
         listAsXml(output, "TimeInstant", TimeInstant);
      }
      if (ServiceLevel != null) {
         listAsXml(output, "ServiceLevel", ServiceLevel);
      }
      if (PhaseResource != null) {
         listAsXml(output, "PhaseResource", PhaseResource);
      }
      if (VolumeResource != null) {
         listAsXml(output, "VolumeResource", VolumeResource);
      }
      if (ConsumableResource != null) {
         listAsXml(output, "ConsumableResource", ConsumableResource);
      }
      if (Resource != null) {
         listAsXml(output, "Resource", Resource);
      }
      if (ProbeName != null) {
         ProbeName.asXml(output, "ProbeName");
      }
      if (!formd5_optional) {
         if (Grid != null) {
            Grid.asXml(output, "Grid");
         }
      }
      if (ResourceType != null) {
         ResourceType.asXml(output, "Resource");
      }
      if (ExtraXml != null) {
         output.append(StringEscapeUtils.escapeXml(ExtraXml));
      }
      if ((TDSet != null) && (TDSet.size() > 0)) {
         getTransferDetails().asXml(output);
      }
      if (!formd5) {
         if (Origins != null) {
            originsAsXml(output);
         }
      }
      output.append("</JobUsageRecord>" + "\n");
      return output.toString();
   }

   public void AttachContent(org.hibernate.Session session) throws Exception {
      AttachOrigins(session);
   }

   public String getTableName() {
      return "JobUsageRecord";
   }

   public void setUserIdentity(UserIdentity UserIdentity) {
      this.UserIdentity = UserIdentity;
   }

   public UserIdentity getUserIdentity() {
      return UserIdentity;
   }

   public void setJobIdentity(JobIdentity JobIdentity) {
      this.JobIdentity = JobIdentity;
   }

   public JobIdentity getJobIdentity() {
      return JobIdentity;
   }

   public void setJobName(StringElement JobName) {
      this.JobName = JobName;
   }

   public StringElement getJobName() {
      return JobName;
   }

   public void setCharge(FloatElement Charge) {
      this.Charge = Charge;
   }

   public FloatElement getCharge() {
      return Charge;
   }

   public void setNjobs(IntegerElement njobs) {
      this.Njobs = njobs;
   }

   public IntegerElement getNjobs() {
      return Njobs;
   }

   public void setStatus(StringElement Status) {
      this.Status = Status;
   }

   public StringElement getStatus() {
      return Status;
   }

   public void setWallDuration(DurationElement WallDuration) {
      this.WallDuration = WallDuration;
   }

   public DurationElement getWallDuration() {
      return WallDuration;
   }

   public void setCpuUserDuration(DurationElement CpuUserDuration) {
      this.CpuUserDuration = CpuUserDuration;
   }

   public DurationElement getCpuUserDuration() {
      return CpuUserDuration;
   }

   public void setCpuSystemDuration(DurationElement CpuSystemDuration) {
      this.CpuSystemDuration = CpuSystemDuration;
   }

   public DurationElement getCpuSystemDuration() {
      return CpuSystemDuration;
   }

   public void setNodeCount(IntegerElement NodeCount) {
      this.NodeCount = NodeCount;
   }

   public IntegerElement getNodeCount() {
      return NodeCount;
   }

   public void setProcessors(IntegerElement Processors) {
      this.Processors = Processors;
   }

   public IntegerElement getProcessors() {
      return Processors;
   }

   public void setStartTime(DateElement StartTime) {
      this.StartTime = StartTime;
   }

   public DateElement getStartTime() {
      return StartTime;
   }

   public void setEndTime(DateElement EndTime) {
      this.EndTime = EndTime;
   }

   public DateElement getEndTime() {
      return EndTime;
   }

   public void setMachineName(StringElement MachineName) {
      this.MachineName = MachineName;
   }

   public StringElement getMachineName() {
      return MachineName;
   }

   public void setSubmitHost(StringElement SubmitHost) {
      this.SubmitHost = SubmitHost;
   }

   public StringElement getSubmitHost() {
      return SubmitHost;
   }

   public void setQueue(StringElement Queue) {
      this.Queue = Queue;
   }

   public StringElement getQueue() {
      return Queue;
   }

   public void setProjectName(StringElement ProjectName) {
      this.ProjectName = ProjectName;
   }

   public StringElement getProjectName() {
      return ProjectName;
   }

   public void setHost(StringElement el) {
      this.Host = el;
   }

   public StringElement getHost() {
      return Host;
   }

   public void setDisk(List Disk) {
      this.Disk = Disk;
   }

   public List getDisk() {
      return Disk;
   }

   public void setMemory(List Memory) {
      this.Memory = Memory;
   }

   public List getMemory() {
      return Memory;
   }

   public void setSwap(List Swap) {
      this.Swap = Swap;
   }

   public List getSwap() {
      return Swap;
   }

   public void setNetwork(List Network) {
      this.Network = Network;
   }

   public List getNetwork() {
      return Network;
   }

   public void setTimeDuration(List TimeDuration) {
      this.TimeDuration = TimeDuration;
   }

   public List getTimeDuration() {
      return TimeDuration;
   }

   public void setTimeInstant(List TimeInstant) {
      this.TimeInstant = TimeInstant;
   }

   public List getTimeInstant() {
      return TimeInstant;
   }

   public void setServiceLevel(List field1) {
      this.ServiceLevel = field1;
   }

   public List getServiceLevel() {
      return ServiceLevel;
   }

   public void setPhaseResource(List PhaseResource) {
      this.PhaseResource = PhaseResource;
   }

   public List getPhaseResource() {
      return PhaseResource;
   }

   public void setVolumeResource(List VolumeResource) {
      this.VolumeResource = VolumeResource;
   }

   public List getVolumeResource() {
      return VolumeResource;
   }

   public void setConsumableResource(List ConsumableResource) {
      this.ConsumableResource = ConsumableResource;
   }

   public List getConsumableResource() {
      return ConsumableResource;
   }

   public void setResource(List Resource) {
      this.Resource = Resource;
   }

   public List getResource() {
      return Resource;
   }

   public boolean setDuplicate(boolean b) {
      // setDuplicate will increase the count (nRecords,nConnections,nDuplicates) for the probe
      // and will return true if the duplicate needs to be recorded as a potential error.
      if (b) {
         this.Probe.setnDuplicates(this.Probe.getnDuplicates() + 1);
      } else {
         this.Probe.setnRecords(this.Probe.getnRecords() + 1);
      }
      return b;
   }

   public void setResourceType(StringElement resourceType) {
      this.ResourceType = resourceType;
      if (this.ResourceType != null) {
         this.ResourceType.setDescription("ResourceType");
      }
   }

   public StringElement getResourceType() {
      return ResourceType;
   }

   public Date getDate() {
      // Returns the date this record is reporting about.
      if (EndTime != null) {
         return EndTime.getValue();
      } else if (StartTime != null) {
         return EndTime.getValue();
      }
      // If this doesn't work then exception should be thrown
      return ServerDate;
   }

   public String computemd5(boolean optional) throws Exception {
      // Calculate the checksum
      String md5key = Utils.md5key(asXML(true, optional));
//        Logging.debug("DEBUG: Calculated md5v2 value of " + md5key + " on following XML:\n" + asXML(true));
      return md5key;
   }

   // TDSet
   public void setTDSet(Set TDSet) {
      this.TDSet = TDSet;
   }

   public Set getTDSet() {
      return TDSet;
   }

   // Convenience methods
   public void setTransferDetails(TransferDetails details) {
      if (TDSet == null) {
         TDSet = new HashSet();
      } else {
         TDSet.clear();
      }
      TDSet.add(details);
   }

   public TransferDetails getTransferDetails() {
      if (TDSet == null) {
         return null;
      }
      if (TDSet.size() == 0) {
         return null;
      } else if (TDSet.size() > 1) {
         Logging.warning("TransferDetails: TDSet has multiple entries -- not returning any!");
         return null;
      } else {
         return (TransferDetails) TDSet.iterator().next();
      }
   }

   // Trigger called after session.save()
   @Override
   public void executeTrigger(org.hibernate.Session session) throws Exception {
      TransferDetails td = getTransferDetails();
      if ((td != null) &&
            (!session.contains(td))) { // No cascade
         session.save(td); // Save TransferDetails session.
      }
      if (wantSummary) {
         session.flush();
         org.hibernate.Query q =
               session.createSQLQuery("call add_JUR_to_summary(" +
               getRecordId() + ")");
         q.executeUpdate();
      }
   }

   public static void setwantSummary(Boolean wantSummary) {
      JobUsageRecord.wantSummary = wantSummary;
   }
}

package net.sf.gratia.storage;

import java.util.Iterator;
import java.util.List;
import java.util.Date;

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
public class JobUsageRecord implements Record
{
   // Calculated information (not directly in the xml file)
   private Probe Probe;

   private String RawXml;   // Complete Usage Record Xml
   private String ExtraXml; // Xml fragment not used for any of the data members/field


   private RecordIdentity RecordIdentity;
   private UserIdentity UserIdentity;  // This should be a list or set of UserIdentity
   private int RecordId;
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
   private StringElement SiteName;
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
   private StringElement Grid;
   private StringElement ProbeName;
   private Date ServerDate;
   private String md5;

   public JobUsageRecord()
   {
      RecordIdentity = null; // new RecordIdentity();
      IntegerElement el = new IntegerElement();
      el.setValue(1);
      setNjobs(el);
      RawXml = "";
      ExtraXml = "";
      ServerDate = new Date();
   }

   public String listToString(String name, List l)
   {
      String output = "";
      for (Iterator i = l.iterator(); i.hasNext(); )
      {
         Object el = i.next();
         output = output + " " + name + ": " + el;
      }
      return output;
   }

   public String listAsXml(String name, List l)
   {
      String output = "";
      for (Iterator i = l.iterator(); i.hasNext(); )
      {
         XmlElement el = (XmlElement)i.next();
         output = output + el.asXml(name);
      }
      return output;
   }

   public String toString()
   {
      String output = "UsageRecord: Db Id: " + RecordId;
      if (ResourceType != null) output = output + "RecordType: " + ResourceType;
      if (ProbeName != null) output = output + "ProbeName: " + ProbeName;
      if (Grid != null) output = output + "Grid: " + Grid;
      if (RecordIdentity != null) output = output + RecordIdentity;
      if (JobIdentity != null) output = output + JobIdentity;
      if (UserIdentity != null) output = output + " User: " + UserIdentity;
      if (JobName != null) output = output + " JobName: " + JobName;
      if (Charge != null) output = output + " Charge: " + Charge;
      if (Status != null) output = output + " Status: " + Status;
      if (Njobs != null) output = output + " Njobs: " + Njobs;
      if (WallDuration != null) output = output + " WallDuration: " + WallDuration;
      if (CpuUserDuration != null) output = output + " CpuUserDuration: " + CpuUserDuration;
      if (CpuSystemDuration != null) output = output + " CpuSystemDuration: " + CpuSystemDuration;
      if (NodeCount != null) output = output + " NodeCount: " + NodeCount;
      if (Processors != null) output = output + " Processors: " + Processors;
      if (StartTime != null) output = output + " StartTime: " + StartTime;
      if (EndTime != null) output = output + " EndTime: " + EndTime;
      if (MachineName != null) output = output + " MachineName: " + MachineName;
      if (SiteName != null) output = output + " SiteName: " + SiteName;
      if (SubmitHost != null) output = output + " SubmitHost: " + SubmitHost;
      if (Queue != null) output = output + " Queue: " + Queue;
      if (ProjectName != null) output = output + " ProjectName: " + ProjectName;
      if (Host != null) output = output + " Host: " + Host;
      if (Disk != null) output = output + listToString("Disk", Disk);
      if (Memory != null) output = output + listToString("Memory", Memory);
      if (Swap != null) output = output + listToString("Swap", Swap);
      if (Network != null) output = output + listToString("Network", Network);
      if (TimeDuration != null) output = output + listToString("TimeDuration", TimeDuration);
      if (TimeInstant != null) output = output + listToString("TimeInstant", TimeInstant);
      if (ServiceLevel != null) output = output + listToString("ServiceLevel", ServiceLevel);
      if (PhaseResource != null) output = output + listToString("PhaseResource", PhaseResource);
      if (VolumeResource != null) output = output + listToString("VolumeResource", VolumeResource);
      if (ConsumableResource != null) output = output + listToString("ConsumableResource", ConsumableResource);
      if (Resource != null) output = output + listToString("Resource", Resource);

      // if (RawXml != null) output = output + "\n" + RawXml;
      if (ExtraXml != null) output = output + "\nExtraXml:\n" + ExtraXml;
      return output;
   }

   public String asXML()
   {
      String output = ""; // ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      output = output + ("<JobUsageRecord xmlns=\"http://www.gridforum.org/2003/ur-wg\"\n");
      output = output + ("		xmlns:urwg=\"http://www.gridforum.org/2003/ur-wg\"\n");
      output = output + ("		xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
      // output = output + ("		xsi:schemaLocation=\"http://www.gridforum.org/2003/ur-wg file:/Users/bekah/Documents/GGF/URWG/urwg-schema.09.xsd\">\n");
      output = output + ("		xsi:schemaLocation=\"http://www.gridforum.org/2003/ur-wg file:///u:/OSG/urwg-schema.11.xsd\">\n");

      // output = output + "UsageRecord: Db Id: " + RecordId.asXml();
      if (RecordIdentity != null) output = output + RecordIdentity.asXml();
      if (JobIdentity != null) output = output + JobIdentity.asXml();
      if (UserIdentity != null) output = output + UserIdentity.asXml();
      if (JobName != null) output = output + JobName.asXml("JobName");
      if (Charge != null) output = output + Charge.asXml("Charge");
      if (Status != null) output = output + Status.asXml("Status");
      if (Njobs != null
          && Njobs.getValue() != 1) output = output + Njobs.asXml("Njobs");
      if (WallDuration != null) output = output + WallDuration.asXml("WallDuration");
      if (CpuUserDuration != null) output = output + CpuUserDuration.asXml("CpuDuration", "usageType", "user");
      if (CpuSystemDuration != null) output = output + CpuSystemDuration.asXml("CpuDuration", "usageType", "system");
      if (NodeCount != null) output = output + NodeCount.asXml("NodeCount");
      if (Processors != null) output = output + Processors.asXml("Processors");
      if (StartTime != null) output = output + StartTime.asXml("StartTime");
      if (EndTime != null) output = output + EndTime.asXml("EndTime");
      if (MachineName != null) output = output + MachineName.asXml("MachineName");
      if (SiteName != null) output = output + SiteName.asXml("SiteName");
      if (SubmitHost != null) output = output + SubmitHost.asXml("SubmitHost");
      if (Queue != null) output = output + Queue.asXml("Queue");
      if (ProjectName != null) output = output + ProjectName.asXml("ProjectName");
      if (Host != null) output = output + Host.asXml("Host");
      if (Disk != null) output = output + listAsXml("Disk", Disk);
      if (Memory != null) output = output + listAsXml("Memory", Memory);
      if (Swap != null) output = output + listAsXml("Swap", Swap);
      if (Network != null) output = output + listAsXml("Network", Network);
      if (TimeDuration != null) output = output + listAsXml("TimeDuration", TimeDuration);
      if (TimeInstant != null) output = output + listAsXml("TimeInstant", TimeInstant);
      if (ServiceLevel != null) output = output + listAsXml("ServiceLevel", ServiceLevel);
      if (PhaseResource != null) output = output + listAsXml("PhaseResource", PhaseResource);
      if (VolumeResource != null) output = output + listAsXml("VolumeResource", VolumeResource);
      if (ConsumableResource != null) output = output + listAsXml("ConsumableResource", ConsumableResource);
      if (Resource != null) output = output + listAsXml("Resource", Resource);
      if (ProbeName != null) output = output + ProbeName.asXml("ProbeName");
      if (Grid != null) output = output + Grid.asXml("Grid");
      if (ResourceType != null) output = output + ResourceType.asXml("Resource");
      if (ExtraXml != null) output = output + ExtraXml;
      output = output + "</JobUsageRecord>" + "\n";
      return output;
   }

   public void AttachContent( org.hibernate.Session session ) throws Exception
   {

   }
    
   public String getTableName()
   {
      return "JobUsageRecord";
   }

   public void setRecordIdentity(RecordIdentity n) { RecordIdentity = n; }
   public RecordIdentity getRecordIdentity()
   {
      return RecordIdentity;
   }

   public void setUserIdentity(UserIdentity UserIdentity)
   {
      this.UserIdentity = UserIdentity;
   }

   public UserIdentity getUserIdentity()
   {
      return UserIdentity;
   }

   public void setRecordId(int RecordId)
   {
      this.RecordId = RecordId;
   }

   public int getRecordId()
   {
      return RecordId;
   }

   public void setJobIdentity(JobIdentity JobIdentity)
   {
      this.JobIdentity = JobIdentity;
   }

   public JobIdentity getJobIdentity()
   {
      return JobIdentity;
   }

   public void addRawXml(String RawXml)
   {
      this.RawXml = this.RawXml + RawXml;
   }

   public void setRawXml(String RawXml)
   {
      this.RawXml = RawXml;
   }

   public String getRawXml()
   {
      return RawXml;
   }

   public void addExtraXml(String ExtraXml)
   {
      this.ExtraXml = this.ExtraXml + ExtraXml;
   }

   public void setExtraXml(String ExtraXml)
   {
      this.ExtraXml = ExtraXml;
   }

   public String getExtraXml()
   {
      return ExtraXml;
   }

   public void setJobName(StringElement JobName)
   {
      this.JobName = JobName;
   }

   public StringElement getJobName()
   {
      return JobName;
   }

   public void setCharge(FloatElement Charge)
   {
      this.Charge = Charge;
   }

   public FloatElement getCharge()
   {
      return Charge;
   }

   public void setNjobs(IntegerElement njobs)
   {
      this.Njobs = njobs;
   }

   public IntegerElement getNjobs()
   {
      return Njobs;
   }

   public void setStatus(StringElement Status)
   {
      this.Status = Status;
   }

   public StringElement getStatus()
   {
      return Status;
   }

   public void setWallDuration(DurationElement WallDuration)
   {
      this.WallDuration = WallDuration;
   }

   public DurationElement getWallDuration()
   {
      return WallDuration;
   }

   public void setCpuUserDuration(DurationElement CpuUserDuration)
   {
      this.CpuUserDuration = CpuUserDuration;
   }

   public DurationElement getCpuUserDuration()
   {
      return CpuUserDuration;
   }

   public void setCpuSystemDuration(DurationElement CpuSystemDuration)
   {
      this.CpuSystemDuration = CpuSystemDuration;
   }

   public DurationElement getCpuSystemDuration()
   {
      return CpuSystemDuration;
   }

   public void setNodeCount(IntegerElement NodeCount)
   {
      this.NodeCount = NodeCount;
   }

   public IntegerElement getNodeCount()
   {
      return NodeCount;
   }

   public void setProcessors(IntegerElement Processors)
   {
      this.Processors = Processors;
   }

   public IntegerElement getProcessors()
   {
      return Processors;
   }

   public void setStartTime(DateElement StartTime)
   {
      this.StartTime = StartTime;
   }

   public DateElement getStartTime()
   {
      return StartTime;
   }

   public void setEndTime(DateElement EndTime)
   {
      this.EndTime = EndTime;
   }

   public DateElement getEndTime()
   {
      return EndTime;
   }

   public void setMachineName(StringElement MachineName)
   {
      this.MachineName = MachineName;
   }

   public StringElement getMachineName()
   {
      return MachineName;
   }

   public void setSiteName(StringElement SiteName)
   {
      this.SiteName = SiteName;
   }

   public StringElement getSiteName()
   {
      return SiteName;
   }

   public void setSubmitHost(StringElement SubmitHost)
   {
      this.SubmitHost = SubmitHost;
   }

   public StringElement getSubmitHost()
   {
      return SubmitHost;
   }

   public void setQueue(StringElement Queue)
   {
      this.Queue = Queue;
   }

   public StringElement getQueue()
   {
      return Queue;
   }

   public void setProjectName(StringElement ProjectName)
   {
      this.ProjectName = ProjectName;
   }

   public StringElement getProjectName()
   {
      return ProjectName;
   }

   public void setHost(StringElement el)
   {
      this.Host = el;
   }

   public StringElement getHost()
   {
      return Host;
   }

   public void setDisk(List Disk)
   {
      this.Disk = Disk;
   }

   public List getDisk()
   {
      return Disk;
   }

   public void setMemory(List Memory)
   {
      this.Memory = Memory;
   }

   public List getMemory()
   {
      return Memory;
   }

   public void setSwap(List Swap)
   {
      this.Swap = Swap;
   }

   public List getSwap()
   {
      return Swap;
   }

   public void setNetwork(List Network)
   {
      this.Network = Network;
   }

   public List getNetwork()
   {
      return Network;
   }

   public void setTimeDuration(List TimeDuration)
   {
      this.TimeDuration = TimeDuration;
   }

   public List getTimeDuration()
   {
      return TimeDuration;
   }

   public void setTimeInstant(List TimeInstant)
   {
      this.TimeInstant = TimeInstant;
   }

   public List getTimeInstant()
   {
      return TimeInstant;
   }

   public void setServiceLevel(List field1)
   {
      this.ServiceLevel = field1;
   }

   public List getServiceLevel()
   {
      return ServiceLevel;
   }

   public void setPhaseResource(List PhaseResource)
   {
      this.PhaseResource = PhaseResource;
   }

   public List getPhaseResource()
   {
      return PhaseResource;
   }

   public void setVolumeResource(List VolumeResource)
   {
      this.VolumeResource = VolumeResource;
   }

   public List getVolumeResource()
   {
      return VolumeResource;
   }

   public void setConsumableResource(List ConsumableResource)
   {
      this.ConsumableResource = ConsumableResource;
   }

   public List getConsumableResource()
   {
      return ConsumableResource;
   }

   public void setResource(List Resource)
   {
      this.Resource = Resource;
   }

   public List getResource()
   {
      return Resource;
   }

   public Probe getProbe() { return Probe; }
   public void setProbe(Probe p) { this.Probe = p; }
   public boolean setDuplicate(boolean b) 
   {
       // setDuplicate will increase the count (nRecords,nConnections,nDuplicates) for the probe
       // and will return true if the duplicate needs to be recorded as a potential error.
       if (b) {
           this.Probe.setnDuplicates( this.Probe.getnDuplicates() + 1 );
       } else {
           this.Probe.setnRecords( this.Probe.getnRecords() + 1 );
       }
       return b;
   }

   public void setProbeName(StringElement ProbeName)
   {
      this.ProbeName = ProbeName;
   }
   public StringElement getProbeName()
   {
      return ProbeName;
   }

   public void setGrid(StringElement Grid)
   {
      this.Grid = Grid;
   }
   public StringElement getGrid()
   {
      return Grid;
   }

   public void setResourceType(StringElement resourceType)
   {
      this.ResourceType = resourceType;
      if (this.ResourceType != null)
      {
         this.ResourceType.setDescription("ResourceType");
      }
   }
   public StringElement getResourceType()
   {
      return ResourceType;
   }

   public Date getServerDate()
   {
      return ServerDate;
   }

   public void setServerDate(Date value)
   {
      ServerDate = value;
   }

   public String computemd5() throws Exception
   {
      RecordIdentity temp = getRecordIdentity();
      setRecordIdentity(null);
      String md5key = Utils.md5key(asXML());
      setRecordIdentity(temp);

      return md5key;
   }

   public String getmd5()
   {
      return md5;
   }

   public void setmd5(String value)
   {
      md5 = value;
   }
}

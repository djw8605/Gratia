package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.Date;

/**
 * <p>Title: ComputeElement </p>
 *
 * <p>Description: ComputeElement is Gratia's in-memory representation of a GlueCE datum 
 * See https://twiki.cern.ch/twiki/bin/view/LCG/GridMonitoringProbeStandard
 *
 * <p>Copyright: Copyright (c) 2009</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 *
 * Updated by Brian Bockelman, University of Nebraska-Lincoln (http://rcf.unl.edu)
 *
 */
public class ComputeElement extends Record
{
   // Meta Information (not part of the xml file per se).
   // See Record class

   // Meta Information (from the xml file)
   // See Record class

   // Data Content.
    
   private StringElement UniqueID;
   private StringElement CEName; 
   private StringElement Cluster; 
   private StringElement HostName; 

   private DateElement Timestamp;

   private StringElement LrmsType;   
   private StringElement LrmsVersion;

   private IntegerElement MaxRunningJobs;
   private IntegerElement MaxTotalJobs;
   private IntegerElement AssignedJobSlots;

   private StringElement Status;

   public ComputeElement()
   {
      RecordIdentity = null; // new RecordIdentity();
      RawXml = "";
      ExtraXml = "";
      ServerDate = new Date();
   }

   public String toString()
   {
      String output = "ComputeElement dbid: " + RecordId + "\n";
      if (RecordIdentity != null) output = output + RecordIdentity + "\n";
      if (SiteName != null) output = output + " SiteName: " + SiteName + "\n";
      if (ProbeName != null) output = output + "ProbeName: " + ProbeName + "\n";

      output = output + "GlueCEUniqueID: " + UniqueID + "\n";
      output = output + "GlueCEName: " + CEName + "\n";
      output = output + "GlueCEHostingCluster: " + Cluster + "\n";
      output = output + "GlueCEInfoHostName: " + HostName + "\n";
      output = output + "timestamp: " + Timestamp + "\n";
      output = output + "GlueCEInfoLRMSType: " + LrmsType + "\n";
      output = output + "GlueCEInfoLRMSVersion: " + LrmsVersion + "\n";
      output = output + "GlueCEPolicyMaxRunningJobs: " + MaxRunningJobs + "\n";
      output = output + "GlueCEPolicyMaxTotalJobs: " + MaxTotalJobs + "\n";
      output = output + "GlueCEPolicyAssignedJobSlots: " + AssignedJobSlots + "\n";
      output = output + "GlueCEStateStatus: " + Status + "\n";

      if (Origins != null) output = output + Origins.toString();
      return output;
   }

   public String asXML()
   {
      return asXML(false, false);
   }
   
   public String asXML(boolean formd5, boolean optional)
   {
      // If formd5 is true do not include
      //    RecordIdentity
      // in calculation.
     boolean formd5_optional = formd5 && optional;
     
      StringBuilder output = new StringBuilder(""); // ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      output.append("<ComputeElement xmlns:urwg=\"http://www.gridforum.org/2003/ur-wg\">\n");
      if (!formd5) { 
         if (RecordIdentity != null) RecordIdentity.asXml(output);
      }
      if (ProbeName != null) ProbeName.asXml(output,"ProbeName");
      if (SiteName != null) SiteName.asXml(output,"SiteName");

      if (UniqueID != null) UniqueID.asXml(output,"UniqueID");
      if (CEName != null) CEName.asXml(output,"CEName");
      if (Cluster != null) Cluster.asXml(output,"Cluster");
      if (HostName != null) HostName.asXml(output,"HostName");
      if (Timestamp != null && !formd5) Timestamp.asXml(output,"Timestamp");
      if (LrmsType != null) LrmsType.asXml(output,"LrmsType");
      if (LrmsVersion != null) LrmsVersion.asXml(output,"LrmsVersion");
      if (MaxRunningJobs != null) MaxRunningJobs.asXml(output,"MaxRunningJobs");
      if (MaxTotalJobs != null) MaxTotalJobs.asXml(output,"MaxTotalJobs");
      if (AssignedJobSlots != null) AssignedJobSlots.asXml(output,"AssignedJobSlots");
      if (Status != null) Status.asXml(output,"Status");
      if (ProbeName != null) {
         ProbeName.asXml(output, "ProbeName");
      }
      if (!formd5_optional) {
         if (SiteName != null) {
            SiteName.asXml(output, "SiteName");
         }
      }
      if (ExtraXml != null) {
         output.append(StringEscapeUtils.escapeXml(ExtraXml));
      }
      if (!formd5_optional) {
         if (Grid != null) {
            Grid.asXml(output, "Grid");
         }
      }
      if (!formd5_optional) {
         if (Origins != null) originsAsXml(output);
      }
      output.append("</ComputeElement>\n");
      return output.toString();
   }

   public void AttachContent( org.hibernate.Session session ) throws Exception
   {
      AttachOrigins( session );
   }

   public String getTableName()
   {
      return "ComputeElement";
   }

   public static Date expirationDate() {
      return new Date(0);
   }

   public Date getDate() 
   {
      // Returns the date this records is reporting about.
      return this.Timestamp.getValue();
   }

   public boolean setDuplicate(boolean b) 
   {
       // setDuplicate will increase the count (nRecords,nConnections,nDuplicates) for the probe
       // and will return true if the duplicate needs to be recorded as a potential error.
       this.Probe.setnConnections( Probe.getnConnections() + 1 );
       return false;
   }

   public String computemd5(boolean optional) throws Exception
   {
      RecordIdentity temp = getRecordIdentity();
      setRecordIdentity(null);
      String md5key = Utils.md5key(asXML(true, optional));
      setRecordIdentity(temp);

      return md5key;
   }

  public StringElement getUniqueID() {
    return UniqueID;
  }

  public void setUniqueID(StringElement uniqueID) {
    UniqueID = uniqueID;
  }

  public StringElement getCEName() {
    return CEName;
  }

  public void setCEName(StringElement name) {
    CEName = name;
  }

  public StringElement getCluster() {
    return Cluster;
  }

  public void setCluster(StringElement cluster) {
    Cluster = cluster;
  }

  public StringElement getHostName() {
    return HostName;
  }

  public void setHostName(StringElement hostName) {
    HostName = hostName;
  }

  public DateElement getTimestamp() {
    return Timestamp;
  }

  public void setTimestamp(DateElement timestamp) {
    Timestamp = timestamp;
  }

  public StringElement getLrmsType() {
    return LrmsType;
  }

  public void setLrmsType(StringElement lrmsType) {
    LrmsType = lrmsType;
  }

  public StringElement getLrmsVersion() {
    return LrmsVersion;
  }

  public void setLrmsVersion(StringElement lrmsVersion) {
    LrmsVersion = lrmsVersion;
  }

  public IntegerElement getMaxRunningJobs() {
    return MaxRunningJobs;
  }

  public void setMaxRunningJobs(IntegerElement maxRunningJobs) {
    MaxRunningJobs = maxRunningJobs;
  }

  public IntegerElement getMaxTotalJobs() {
    return MaxTotalJobs;
  }

  public void setMaxTotalJobs(IntegerElement maxTotalJobs) {
    MaxTotalJobs = maxTotalJobs;
  }

  public IntegerElement getAssignedJobSlots() {
    return AssignedJobSlots;
  }

  public void setAssignedJobSlots(IntegerElement assignedJobSlots) {
    AssignedJobSlots = assignedJobSlots;
  }

  public StringElement getStatus() {
    return Status;
  }

  public void setStatus(StringElement status) {
    Status = status;
  }
  
}

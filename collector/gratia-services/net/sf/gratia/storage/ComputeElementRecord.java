package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.Date;

/**
 * <p>Title: ComputeElementRecord </p>
 *
 * <p>Description: ComputeElement is Gratia's in-memory representation of a GlueVO datum 
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
public class ComputeElementRecord extends Record
{
   // Meta Information (not part of the xml file per se).
   // See Record class

   // Meta Information (from the xml file)
   // See Record class

   // Data Content.
    
   private StringElement UniqueID;
   private StringElement VO;

   private DateElement Timestamp;

   private IntegerElement RunningJobs;
   private IntegerElement TotalJobs;
   private IntegerElement WaitingJobs;

   private static boolean wantSummary = true;

   public ComputeElementRecord()
   {
      RecordIdentity = null; // new RecordIdentity();
      RawXml = "";
      ExtraXml = "";
      ServerDate = new Date();
   }

   public String toString()
   {
      String output = "ComputeElementRecord dbid: " + RecordId + "\n";
      if (RecordIdentity != null) output = output + RecordIdentity + "\n";
      if (SiteName != null) output = output + " SiteName: " + SiteName + "\n";
      if (ProbeName != null) output = output + "ProbeName: " + ProbeName + "\n";

      output = output + "GlueCEUniqueID: " + UniqueID + "\n";
      output = output + "GlueCEAccessControlBaseRule: " + VO + "\n";
      output = output + "timestamp: " + Timestamp + "\n";
      output = output + "GlueCEStateRunningJobs: " + RunningJobs + "\n";
      output = output + "GlueCEStateTotalJobs: " + TotalJobs + "\n";
      output = output + "GlueCEStateWaitingJobs: " + WaitingJobs + "\n";

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
      output.append("<ComputeElementRecord xmlns:urwg=\"http://www.gridforum.org/2003/ur-wg\">\n");
      if (!formd5) { 
         if (RecordIdentity != null) RecordIdentity.asXml(output);
      }
      if (ProbeName != null) ProbeName.asXml(output,"ProbeName");
      if (SiteName != null) SiteName.asXml(output,"SiteName");

      if (UniqueID != null) UniqueID.asXml(output,"UniqueID");
      if (VO != null) VO.asXml(output,"VO");
      if (Timestamp != null) Timestamp.asXml(output,"Timestamp");
      if (RunningJobs != null) RunningJobs.asXml(output,"RunningJobs");
      if (TotalJobs != null) TotalJobs.asXml(output,"TotalJobs");
      if (WaitingJobs != null) WaitingJobs.asXml(output,"WaitingJobs");
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
      output.append("</ComputeElementRecord>\n");
      return output.toString();
   }

   public void AttachContent( org.hibernate.Session session ) throws Exception
   {
      AttachOrigins( session );
   }

   public String getTableName()
   {
      return "ComputeElementRecord";
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
       if (b) {
           this.Probe.setnDuplicates( this.Probe.getnDuplicates() + 1 );
       } else {
           this.Probe.setnRecords( this.Probe.getnRecords() + 1 );
       }
       return b;
   }

   public String computemd5(boolean optional) throws Exception
   {
      RecordIdentity temp = getRecordIdentity();
      setRecordIdentity(null);
      String md5key = Utils.md5key(asXML());
      setRecordIdentity(temp);

      return md5key;
   }

  public StringElement getUniqueID() {
    return UniqueID;
  }

  public void setUniqueID(StringElement uniqueID) {
    UniqueID = uniqueID;
  }

  public StringElement getVO() {
    return VO;
  }

  public void setVO(StringElement vo) {
    VO = vo;
  }

  public DateElement getTimestamp() {
    return Timestamp;
  }

  public void setTimestamp(DateElement timestamp) {
    Timestamp = timestamp;
  }

  public IntegerElement getRunningJobs() {
    return RunningJobs;
  }

  public void setRunningJobs(IntegerElement runningJobs) {
    RunningJobs = runningJobs;
  }

  public IntegerElement getTotalJobs() {
    return TotalJobs;
  }

  public void setTotalJobs(IntegerElement totalJobs) {
    TotalJobs = totalJobs;
  }

  public IntegerElement getWaitingJobs() {
    return WaitingJobs;
  }

  public void setWaitingJobs(IntegerElement waitingJobs) {
    WaitingJobs = waitingJobs;
  }

  public static void setwantSummary(Boolean wantSummary) {
    ComputeElementRecord.wantSummary = wantSummary;
  }

  public void executeTrigger(org.hibernate.Session session) throws Exception {
    if (wantSummary) {
       session.flush();
       org.hibernate.Query q =
             session.createSQLQuery("call add_service_to_hourly_summary(" +
             getRecordId() + ")");
       q.executeUpdate();
       q =
         session.createSQLQuery("call add_service_to_daily_summary(" +
         getRecordId() + ")");
       q.executeUpdate();
    }
 }

}

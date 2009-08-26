package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.Date;

/**
 * <p>Title: StorageElement </p>
 *
 * <p>Description: StorageElement is Gratia's in-memory representation of a SE datum 
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
public class StorageElement extends Record
{
   // Meta Information (not part of the xml file per se).
   // See Record class

   // Meta Information (from the xml file)
   // See Record class

   // Data Content.
    
   private StringElement UniqueID;
   private StringElement SE; 
   private StringElement Name;
   private StringElement ParentID;
   private StringElement VO;
   private StringElement OwnerDN;
   private StringElement SpaceType; 

   private DateElement Timestamp;

   private StringElement Implementation;   
   private StringElement Version;
   private StringElement Status;

   public StorageElement()
   {
      RecordIdentity = null; // new RecordIdentity();
      RawXml = "";
      ExtraXml = "";
      ServerDate = new Date();
   }

   public String toString()
   {
      String output = "StorageElement dbid: " + RecordId + "\n";
      if (RecordIdentity != null) output = output + RecordIdentity + "\n";
      if (SiteName != null) output = output + " SiteName: " + SiteName + "\n";
      if (ProbeName != null) output = output + "ProbeName: " + ProbeName + "\n";

      output = output + "Storage Space UniqueID: " + UniqueID + "\n";
      output = output + "GlueSEName: " + SE + "\n";
      output = output + "Storage Space Name: " + Name + "\n";
      output += "Space ParentID: " + ParentID + "\n";
      output += "VO: " + VO + "\n";
      output += "OwnerDN: " + OwnerDN + "\n";
      output = output + "Storage SpaceType: " + SpaceType + "\n";
      output = output + "timestamp: " + Timestamp + "\n";
      output = output + "GlueSEImplementationName: " + Implementation + "\n";
      output = output + "GlueSEImplementationVersion: " + Version + "\n";
      output = output + "GlueSEStateStatus: " + Status + "\n";

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
      output.append("<StorageElement xmlns:urwg=\"http://www.gridforum.org/2003/ur-wg\">\n");
      if (!formd5) { 
         if (RecordIdentity != null) RecordIdentity.asXml(output);
      }
      if (ProbeName != null) ProbeName.asXml(output,"ProbeName");
      if (SiteName != null) SiteName.asXml(output,"SiteName");

      if (UniqueID != null) UniqueID.asXml(output,"UniqueID");
      if (SE != null) SE.asXml(output,"SE");
      if (Name != null) Name.asXml(output,"Name");
      if (SpaceType != null) SpaceType.asXml(output,"SpaceType");
      if (Timestamp != null && !formd5) Timestamp.asXml(output,"Timestamp");
      if (Implementation != null) Implementation.asXml(output,"Implementation");
      if (Version != null) Version.asXml(output,"Version");
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
      output.append("</StorageElement>\n");
      return output.toString();
   }

   public void AttachContent( org.hibernate.Session session ) throws Exception
   {
      AttachOrigins( session );
   }

   public String getTableName()
   {
      return "StorageElement";
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

  public DateElement getTimestamp() {
    return Timestamp;
  }

  public void setTimestamp(DateElement timestamp) {
    Timestamp = timestamp;
  }

  public StringElement getSE() {
    return SE;
  }

  public void setSE(StringElement se) {
    SE = se;
  }

  public StringElement getName() {
    return Name;
  }

  public void setName(StringElement name) {
    Name = name;
  }

  public StringElement getSpaceType() {
    return SpaceType;
  }

  public void setSpaceType(StringElement spaceType) {
    SpaceType = spaceType;
  }

  public StringElement getImplementation() {
    return Implementation;
  }

  public void setImplementation(StringElement implementation) {
    Implementation = implementation;
  }

  public StringElement getVersion() {
    return Version;
  }

  public void setVersion(StringElement version) {
    Version = version;
  }

  public StringElement getStatus() {
    return Status;
  }

  public void setStatus(StringElement status) {
    Status = status;
  }

  public StringElement getParentID() {
    return ParentID;
  }

  public void setParentID(StringElement parentID) {
    ParentID = parentID;
  }

  public StringElement getVO() {
    return VO;
  }

  public void setVO(StringElement vo) {
    VO = vo;
  }

  public StringElement getOwnerDN() {
    return OwnerDN;
  }

  public void setOwnerDN(StringElement ownerDN) {
    OwnerDN = ownerDN;
  }

}

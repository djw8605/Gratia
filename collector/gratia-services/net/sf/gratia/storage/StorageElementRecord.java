package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.Date;

/**
 * <p>Title: StorageElementRecord </p>
 *
 * <p>Description: StorageElementRecord is Gratia's in-memory representation of a Storage element datum 
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
public class StorageElementRecord extends Record
{
   // Meta Information (not part of the xml file per se).
   // See Record class

   // Meta Information (from the xml file)
   // See Record class

   // Data Content.
    
   private StringElement UniqueID;
   private StringElement MeasurementType;
   private StringElement StorageType;

   private DateElement Timestamp;

   private IntegerElement TotalSpace;
   private IntegerElement FreeSpace;
   private IntegerElement UsedSpace;
   private IntegerElement FileCount;
   private IntegerElement FileCountLimit;


   public IntegerElement getFileCount() {
    return FileCount;
  }

  public void setFileCount(IntegerElement fileCount) {
    FileCount = fileCount;
  }

  public IntegerElement getFileCountLimit() {
    return FileCountLimit;
  }

  public void setFileCountLimit(IntegerElement fileCountLimit) {
    FileCountLimit = fileCountLimit;
  }

  public StorageElementRecord()
   {
      RecordIdentity = null; // new RecordIdentity();
      RawXml = "";
      ExtraXml = "";
      ServerDate = new Date();
   }

   public String toString()
   {
      String output = "StorageElementRecord dbid: " + RecordId + "\n";
      if (RecordIdentity != null) output = output + RecordIdentity + "\n";
      if (SiteName != null) output = output + " SiteName: " + SiteName + "\n";
      if (ProbeName != null) output = output + "ProbeName: " + ProbeName + "\n";

      output = output + "Storage space UniqueID: " + UniqueID + "\n";
      output = output + "Measurement type: " + MeasurementType + "\n";
      output = output + "Space type: " + StorageType + "\n";
      output = output + "timestamp: " + Timestamp + "\n";
      output = output + "Total space (GB): " + TotalSpace + "\n";
      output = output + "Free space (GB): " + FreeSpace + "\n";
      output = output + "Used space (GB): " + UsedSpace + "\n";
      output = output + "File Count Limit: " + FileCountLimit + "\n";
      output = output + "File Count: " + FileCount + "\n";

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
      output.append("<StorageElementRecord xmlns:urwg=\"http://www.gridforum.org/2003/ur-wg\">\n");
      if (!formd5) { 
         if (RecordIdentity != null) RecordIdentity.asXml(output);
      }
      if (ProbeName != null) ProbeName.asXml(output,"ProbeName");
      if (SiteName != null) SiteName.asXml(output,"SiteName");

      if (UniqueID != null) UniqueID.asXml(output,"UniqueID");
      if (MeasurementType != null) MeasurementType.asXml(output,"MeasurementType");
      if (StorageType!= null) StorageType.asXml(output,"StorageType");
      if (Timestamp != null) Timestamp.asXml(output,"Timestamp");
      if (TotalSpace != null) TotalSpace.asXml(output,"TotalSpace");
      if (UsedSpace != null) UsedSpace.asXml(output,"UsedSpace");
      if (FreeSpace != null) FreeSpace.asXml(output,"FreeSpace");
      if (FileCountLimit != null) FileCountLimit.asXml(output,"FileCountLimit");
      if (FileCount != null) FileCount.asXml(output,"FileCount");
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
      output.append("</StorageElementRecord>\n");
      return output.toString();
   }

   public void AttachContent( org.hibernate.Session session ) throws Exception
   {
      AttachOrigins( session );
   }

   public String getTableName()
   {
      return "StorageElementRecord";
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

  public DateElement getTimestamp() {
    return Timestamp;
  }

  public void setTimestamp(DateElement timestamp) {
    Timestamp = timestamp;
  }

  public StringElement getMeasurementType() {
    return MeasurementType;
  }

  public void setMeasurementType(StringElement measurementType) {
    MeasurementType = measurementType;
  }

  public StringElement getStorageType() {
    return StorageType;
  }

  public void setStorageType(StringElement storageType) {
    StorageType = storageType;
  }

  public IntegerElement getTotalSpace() {
    return TotalSpace;
  }

  public void setTotalSpace(IntegerElement totalSpace) {
    TotalSpace = totalSpace;
  }

  public IntegerElement getFreeSpace() {
    return FreeSpace;
  }

  public void setFreeSpace(IntegerElement freeSpace) {
    FreeSpace = freeSpace;
  }

  public IntegerElement getUsedSpace() {
    return UsedSpace;
  }

  public void setUsedSpace(IntegerElement usedSpace) {
    UsedSpace = usedSpace;
  }

}

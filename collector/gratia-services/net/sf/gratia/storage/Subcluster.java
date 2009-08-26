package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.Date;

/**
 * <p>Title: Subcluster </p>
 *
 * <p>Description: Subcluster is Gratia's in-memory representation of a subcluster datum 
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
public class Subcluster extends Record
{
   // Meta Information (not part of the xml file per se).
   // See Record class

   // Meta Information (from the xml file)
   // See Record class

   // Data Content.
    
   private StringElement UniqueID;
   private StringElement Name; 
   private StringElement Cluster; 
   private StringElement Platform;
   private StringElement OS;
   private StringElement OSVersion;

   private DateElement Timestamp;

   private IntegerElement Cores;
   private IntegerElement Hosts;
   private IntegerElement Cpus;
   private IntegerElement RAM;
   
   private StringElement Processor;
   private StringElement BenchmarkName;
   private StringElement BenchmarkValue;



   public Subcluster()
   {
      RecordIdentity = null; // new RecordIdentity();
      RawXml = "";
      ExtraXml = "";
      ServerDate = new Date();
   }

   public String toString()
   {
      String output = "Subcluster dbid: " + RecordId + "\n";
      if (RecordIdentity != null) output = output + RecordIdentity + "\n";
      if (SiteName != null) output = output + " SiteName: " + SiteName + "\n";
      if (ProbeName != null) output = output + "ProbeName: " + ProbeName + "\n";

      output = output + "GlueSubClusterUniqueID: " + UniqueID + "\n";
      output = output + "GlueSubClusterName: " + Name + "\n";
      output = output + "GlueClusterName: " + Cluster + "\n";
      output = output + "Processor Platform: " + Platform + "\n";
      output = output + "GlueHostOperatingSystemName: " + OS + "\n";
      output = output + "GlueHostOperatingSystemRelease: " + OSVersion + "\n";
      output = output + "timestamp: " + Timestamp + "\n";
      output = output + "Cores: " + Cores + "\n";
      output = output + "Hosts: " + Hosts + "\n";
      output = output + "Cpus: " + Cpus + "\n";
      output += "RAM: " + RAM + "\n";
      output = output + "Processor: " + Processor + "\n";
      output = output + "Benchmark Name: " + BenchmarkName + "\n";
      output = output + "Benchmark Value: " + BenchmarkValue + "\n";

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
      output.append("<Subcluster xmlns:urwg=\"http://www.gridforum.org/2003/ur-wg\">\n");
      if (!formd5) { 
         if (RecordIdentity != null) RecordIdentity.asXml(output);
      }
      if (ProbeName != null) ProbeName.asXml(output,"ProbeName");
      if (SiteName != null) SiteName.asXml(output,"SiteName");

      if (UniqueID != null) UniqueID.asXml(output,"UniqueID");
      if (Name != null) Name.asXml(output,"Name");
      if (Cluster != null) Cluster.asXml(output,"Cluster");
      if (Platform != null) Platform.asXml(output,"Platform");
      if (OS != null) OS.asXml(output,"OS");
      if (OSVersion != null) OSVersion.asXml(output,"OSVersion");
      if (Timestamp != null && !formd5) Timestamp.asXml(output,"Timestamp");
      if (Cores != null) Cores.asXml(output,"Cores");
      if (Hosts != null) Hosts.asXml(output,"Hosts");
      if (Cpus != null) Cpus.asXml(output,"Cpus");
      if (RAM != null) RAM.asXml(output,"RAM");
      if (Processor != null) Processor.asXml(output,"Processor");
      if (BenchmarkName != null) BenchmarkName.asXml(output,"BenchmarkName");
      if (BenchmarkValue != null) BenchmarkValue.asXml(output,"BenchmarkValue");
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
      output.append("</Subcluster>\n");
      return output.toString();
   }

   public void AttachContent( org.hibernate.Session session ) throws Exception
   {
      AttachOrigins( session );
   }

   public String getTableName()
   {
      return "Subcluster";
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

  public StringElement getCluster() {
    return Cluster;
  }

  public void setCluster(StringElement cluster) {
    Cluster = cluster;
  }

  public DateElement getTimestamp() {
    return Timestamp;
  }

  public void setTimestamp(DateElement timestamp) {
    Timestamp = timestamp;
  }

  public StringElement getName() {
    return Name;
  }

  public void setName(StringElement name) {
    Name = name;
  }

  public StringElement getPlatform() {
    return Platform;
  }

  public void setPlatform(StringElement platform) {
    Platform = platform;
  }

  public StringElement getOS() {
    return OS;
  }

  public void setOS(StringElement os) {
    OS = os;
  }

  public StringElement getOSVersion() {
    return OSVersion;
  }

  public void setOSVersion(StringElement version) {
    OSVersion = version;
  }

  public IntegerElement getCores() {
    return Cores;
  }

  public void setCores(IntegerElement cores) {
    Cores = cores;
  }

  public IntegerElement getHosts() {
    return Hosts;
  }

  public void setHosts(IntegerElement hosts) {
    Hosts = hosts;
  }

  public IntegerElement getCpus() {
    return Cpus;
  }

  public void setCpus(IntegerElement cpus) {
    Cpus = cpus;
  }

  public StringElement getProcessor() {
    return Processor;
  }

  public void setProcessor(StringElement processor) {
    Processor = processor;
  }

  public StringElement getBenchmarkName() {
    return BenchmarkName;
  }

  public void setBenchmarkName(StringElement benchmarkName) {
    BenchmarkName = benchmarkName;
  }

  public StringElement getBenchmarkValue() {
    return BenchmarkValue;
  }

  public void setBenchmarkValue(StringElement benchmarkValue) {
    BenchmarkValue = benchmarkValue;
  }

  public IntegerElement getRAM() {
    return RAM;
  }

  public void setRAM(IntegerElement ram) {
    RAM = ram;
  }

}

package net.sf.gratia.storage;

import java.util.Iterator;
import java.util.List;
import java.util.Date;

/**
 * <p>Title: MetricRecord </p>
 *
 * <p>Description: MetrciRecord is Gratia's in-memory representation of a metric datum 
 * See https://twiki.cern.ch/twiki/bin/view/LCG/GridMonitoringProbeStandard
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 */
public class MetricRecord implements Record
{

   // Meta Information (from the xml file)
   private RecordIdentity RecordIdentity;
   private StringElement ProbeName;
   private StringElement SiteName;

   // Meta Information (not part of the xml file per se).
   private int RecordId;
   private String RawXml;   // Complete Usage Record Xml
   private String ExtraXml; // Xml fragment not used for any of the data members/field
   private Date ServerDate;
   private String md5;

   // Data Content.

   private StringElement MetricName;   // The name of the metric. this be of the format <SERVICE>-<METRIC>
   private StringElement MetricStatus; // A return status code (See https://twiki.cern.ch/twiki/bin/view/LCG/GridMonitoringProbeStandard#Status_and_Performance_Metrics)
   private DateElement Timestamp;      // The time the metric was gathered

   public MetricRecord()
   {
      RecordIdentity = null; // new RecordIdentity();
      RawXml = "";
      ExtraXml = "";
      ServerDate = new Date();
   }

   public String toString()
   {
      String output = "MetricRecord dbid: " + RecordId + "\n";
      if (RecordIdentity != null) output = output + RecordIdentity + "\n";
      if (SiteName != null) output = output + " SiteName: " + SiteName + "\n";
      if (ProbeName != null) output = output + "ProbeName: " + ProbeName + "\n";

      output = output + "metricName: " + MetricName + "\n";
      output = output + "metricType: " + MetricStatus + "\n";
      output = output + "timestamp: " + Timestamp + "\n";
      return output;
   }

   public String asXML()
   {
      String output = ""; // ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      output = output + ("<MetricRecord  xmlns:urwg=\"http://www.gridforum.org/2003/ur-wg\">\n");
      if (RecordIdentity != null) output = output + RecordIdentity.asXml();
      if (ProbeName != null) output = output + ProbeName.asXml("ProbeName");
      if (SiteName != null) output = output + SiteName.asXml("SiteName");

      if (MetricName != null) output = output + MetricName.asXml("MetricName");
      if (MetricStatus != null) output = output + MetricStatus.asXml("MetricStatus");
      if (Timestamp != null) output = output + Timestamp.asXml("Timestamp");

      output = output + ("</MetricRecord>\n");
      return output;
   }

   public void setRecordId(int RecordId)
   {
      this.RecordId = RecordId;
   }

   public int getRecordId()
   {
      return RecordId;
   }

   public void setRecordIdentity(RecordIdentity n) { RecordIdentity = n; }
   public RecordIdentity getRecordIdentity()
   {
      return RecordIdentity;
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

   public Date getServerDate()
   {
      return ServerDate;
   }

   public void setServerDate(Date value)
   {
      ServerDate = value;
   }

   public void setProbeName(StringElement ProbeName)
   {
      this.ProbeName = ProbeName;
   }
   public StringElement getProbeName()
   {
      return ProbeName;
   }

   public void setSiteName(StringElement SiteName)
   {
      this.SiteName = SiteName;
   }

   public StringElement getSiteName()
   {
      return SiteName;
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








   public void setMetricName(StringElement MetricName)
   {
      this.MetricName = MetricName;
   }

   public StringElement getMetricName()
   {
      return MetricName;
   }

   public void setMetricStatus(StringElement MetricStatus)
   {
      this.MetricStatus = MetricStatus;
   }

   public StringElement getMetricStatus()
   {
      return MetricStatus;
   }

   public void setTimestamp(DateElement Timestamp)
   {
      this.Timestamp = Timestamp;
   }

   public DateElement getTimestamp()
   {
      return Timestamp;
   }

}
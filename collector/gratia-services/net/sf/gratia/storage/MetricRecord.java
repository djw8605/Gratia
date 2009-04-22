package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.Date;

/**
 * <p>Title: MetricRecord </p>
 *
 * <p>Description: MetricRecord is Gratia's in-memory representation of a metric datum 
 * See https://twiki.cern.ch/twiki/bin/view/LCG/GridMonitoringProbeStandard
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 *
 * Updated by Arvind Gopu, Indiana University (http://peart.ucs.indiana.edu)
 * More Updates by Arvind Gopu 2007-10-19
 *
 */
public class MetricRecord extends Record
{
   // Meta Information (not part of the xml file per se).
   // See Record class

   // Meta Information (from the xml file)
   // See Record class

   // Data Content.
    
   // The name of the metric. this be of the format <SERVICE>-<METRIC>
   private StringElement MetricName;   
   private StringElement MetricType; // Type of metric:status/peformance

   // A return status code (See https://twiki.cern.ch/twiki/bin/view/LCG/
   //         GridMonitoringProbeSpecification#Status_and_Performance_Metrics)
   private StringElement MetricStatus; 

   private DateElement Timestamp;      // The time the metric was gathered
   private StringElement ServiceType;  // The service we are testing

   // For NON-LOCAL PROBES: Majority of the probes are in this category
   // The hostname/port/service we are testing
   private StringElement ServiceUri;   
   private StringElement GatheredAt;   // The Monitoring Host that runs probes

   private StringElement SummaryData;   // Summary of probe output

   private StringElement DetailsData;   // Details of Status probe output
   private StringElement PerformanceData;// Details of Performance probe output

   private StringElement VoName;     // Details of VO for the proxy 

   private IntegerElement SamUploadFlag;// Details of External (SAM) 
                                        //      Upload Status

   // FOR LOCAL PROBES: The Monitoring Host that runs probes AND tests itself. 
   //  When this is provided, GatheredAt and ServiceUri will NOT be provided
   private StringElement HostName;    


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
      output = output + "metricType: " + MetricType + "\n";
      output = output + "metricStatus: " + MetricStatus + "\n";
      output = output + "timestamp: " + Timestamp + "\n";
      output = output + "serviceType: " + ServiceType + "\n";
      output = output + "serviceUri: " + ServiceUri + "\n";
      output = output + "gatheredAt: " + GatheredAt + "\n";
      output = output + "summaryData: " + SummaryData + "\n";
      output = output + "detailsData: " + DetailsData + "\n";
      output = output + "performanceData: " + PerformanceData + "\n";
      output = output + "voName: " + VoName + "\n";
      output = output + "samUploadFlag: " + SamUploadFlag + "\n";
      output = output + "hostName: " + HostName + "\n";

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
      
      StringBuilder output = new StringBuilder(""); // ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      output.append("<MetricRecord  xmlns:urwg=\"http://www.gridforum.org/2003/ur-wg\">\n");
      if (!formd5) { 
         if (RecordIdentity != null) RecordIdentity.asXml(output);
      }
      if (ProbeName != null) ProbeName.asXml(output,"ProbeName");
      if (SiteName != null) SiteName.asXml(output,"SiteName");

      if (MetricName != null) MetricName.asXml(output,"MetricName");
      if (MetricType != null) MetricType.asXml(output,"MetricType");
      if (MetricStatus != null) MetricStatus.asXml(output,"MetricStatus");
      if (Timestamp != null) Timestamp.asXml(output,"Timestamp");
      if (ServiceType != null) ServiceType.asXml(output,"ServiceType");
      if (ServiceUri != null) ServiceUri.asXml(output,"ServiceUri");
      if (GatheredAt != null) GatheredAt.asXml(output,"GatheredAt");
      if (SummaryData != null) SummaryData.asXml(output,"SummaryData");
      if (DetailsData != null) DetailsData.asXml(output,"DetailsData");
      if (PerformanceData != null) PerformanceData.asXml(output,"PerformanceData");
      if (VoName != null) VoName.asXml(output,"VoName");
      if (SamUploadFlag != null) SamUploadFlag.asXml(output,"SamUploadFlag");
      if (HostName != null) HostName.asXml(output,"HostName");

      if (!formd5) {
         if (Origins != null) originsAsXml(output);
      }
      output.append("</MetricRecord>\n");
      return output.toString();
   }

   public void AttachContent( org.hibernate.Session session ) throws Exception
   {
      AttachOrigins( session );
   }

   public String getTableName()
   {
      return "MetricRecord";
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

   public void setMetricName(StringElement MetricName)
   {
      this.MetricName = MetricName;
   }

   public StringElement getMetricName()
   {
      return MetricName;
   }

   public void setMetricType(StringElement MetricType)
   {
      this.MetricType = MetricType;
   }

   public StringElement getMetricType()
   {
      return MetricType;
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

   public void setServiceType(StringElement ServiceType)
   {
      this.ServiceType = ServiceType;
   }

   public StringElement getServiceType()
   {
      return ServiceType;
   }

   public void setServiceUri(StringElement ServiceUri)
   {
      this.ServiceUri = ServiceUri;
   }

   public StringElement getServiceUri()
   {
      return ServiceUri;
   }

   public void setGatheredAt(StringElement GatheredAt)
   {
      this.GatheredAt = GatheredAt;
   }

   public StringElement getGatheredAt()
   {
      return GatheredAt;
   }

   public void setSummaryData(StringElement SummaryData)
   {
      this.SummaryData = SummaryData;
   }

   public StringElement getSummaryData()
   {
      return SummaryData;
   }

   public void setDetailsData(StringElement DetailsData)
   {
      this.DetailsData = DetailsData;
   }

   public StringElement getDetailsData()
   {
      return DetailsData;
   }

   public void setPerformanceData(StringElement PerformanceData)
   {
      this.PerformanceData = PerformanceData;
   }

   public StringElement getPerformanceData()
   {
      return PerformanceData;
   }

   public void setVoName(StringElement VoName)
   {
      this.VoName = VoName;
   }

   public StringElement getVoName()
   {
      return VoName;
   }

   public void setSamUploadFlag(IntegerElement SamUploadFlag)
   {
      this.SamUploadFlag = SamUploadFlag;
   }

   public IntegerElement getSamUploadFlag()
   {
      return SamUploadFlag;
   }

   public void setHostName(StringElement HostName)
   {
      this.HostName = HostName;
   }

   public StringElement getHostName()
   {
      return HostName;
   }
}

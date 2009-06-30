package net.sf.gratia.storage;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import net.sf.gratia.util.Logging;

import org.apache.commons.lang.StringEscapeUtils;

public class TransferDetails extends Record {
   
   // Link to associated class
   private Set JURSet;
   
   // Information directly reflecting DB contents
   private int TransferDetailsId;
   private String Protocol;
   private String Source;
   private String Destination;
   private Integer IsNew;
   
   public TransferDetails() {
      Logging.debug("TransferDetails: Creating new object");
      Protocol = null;
      Source = null;
      Destination = null;
      IsNew = null;
      JURSet = null;
   }
   
   ////////////////////////////////////////////////////////////////////
   // Methods required by interface Record
   //
   public void addRawXml(String RawXml) {
      // FIXME
   }
   
   public void setRawXml(String RawXml) {
      // FIXME
   }
   
   public String getRawXml() {
      // FIXME
      return "";
   }
   
   public void addExtraXml(String ExtraXml) {
      // FIXME
   }
   
   public void setExtraXml(String ExtraXml) {
      // FIXME
   }
   
   public String getExtraXml() {
      // FIXME
      return "";
   }
   
   public String asXML()
   {
      return asXML(false,false);
   }
   
   public String asXML(boolean formd5,boolean optional)
   {
      StringBuilder output = new StringBuilder();
      asXml(output);
      return output.toString();
   }
   
   public void asXml(StringBuilder output) {
      int start = output.length();
      if (Protocol != null) {
         resourceAsXml(output, "Protocol", Protocol);
      }
      if (Source != null) {
         resourceAsXml(output, "Source", Source);
      }
      if (Destination != null) {
         resourceAsXml(output, "Destination", Destination);
      }
      if (IsNew != null) {
         resourceAsXml(output, "IsNew", IsNew.toString());
      }
      Logging.debug("TransferDetails: writing XML: " + output.substring(start));
   }
   
   private void resourceAsXml(StringBuilder output, String name, String value) {
      output.append("<urwg:Resource description=\"");
      output.append(StringEscapeUtils.escapeXml(name));
      output.append("\">");
      output.append(StringEscapeUtils.escapeXml(value));
      output.append("</urwg:Resource>");
   }
   
   public StringElement getSiteName() {
      return getJobUsageRecord().getSiteName();
   }
   
   public StringElement getProbeName() {
      return getJobUsageRecord().getProbeName();
   }
   
   public Date getServerDate() {
      // FIXME
      return getJobUsageRecord().getServerDate();
   }
   
   public void setServerDate(Date value) {
      // FIXME
   }
   
   public String computemd5(boolean optional) throws Exception {
      // FIXME
      return "";
   }
   
   public String getmd5() {
      // FIXME
      return "";
   }
   
   public void setmd5(String md5set) {
      // FIXME
   }
   
   public int getRecordId() {
      return TransferDetailsId;
   }
   
   public void setRecordId(int RecordId) {
      this.TransferDetailsId = RecordId;
   }
   
   public Probe getProbe() {
      // FIXME
      return null;
   }
   
   public void setProbe(Probe p) {
      // FIXME
   }
   
   public boolean setDuplicate(boolean b) {
      // FIXME
      return b;
   }
   
   public String getTableName() {
      return "TransferDetails";
   }
   
   public Date getDate() {
      return getJobUsageRecord().getStartTime().getValue();
   }
   
   public void AttachContent( org.hibernate.Session session ) throws Exception {
   }
   
   ////////////////////////////////////////////////////////////////////
   // Local get / set methods
   //
   ////////////////////////////////////////////////////////////////////
   
   // Protocol
   public void setProtocol(String Protocol) {
      Logging.debug("TransferDetails: setting Protocol to " + Protocol);
      this.Protocol = Protocol;
   }
   
   public String getProtocol() {
      return Protocol;
   }
   
   // Source
   public void setSource(String Source) {
      Logging.debug("TransferDetails: setting Source to " + Source);
      this.Source = Source;
   }
   
   public String getSource() {
      return Source;
   }
   
   // Destination
   public void setDestination(String Destination) {
      Logging.debug("TransferDetails: setting Destination to " + Destination);
      this.Destination = Destination;
   }
   
   public String getDestination() {
      return Destination;
   }
   
   // IsNew
   public void setIsNew(Integer IsNew) {
      Logging.debug("TransferDetails: setting IsNew to " + IsNew);
      this.IsNew = IsNew;
   }
   
   public Integer getIsNew() {
      return IsNew;
   }
   
   // JURSet
   public void setJURSet(Set JURSet) {
      this.JURSet = JURSet;
   }
   
   public Set getJURSet() {
      return JURSet;
   }
   
   // Convenience methods
   public void setJobUsageRecord(JobUsageRecord record) {
      if (JURSet == null) {
         JURSet = new HashSet();
      } else {
         JURSet.clear();
      }
      JURSet.add(record);
   }
   
   public JobUsageRecord getJobUsageRecord() {
      if (JURSet == null) return null;
      if (JURSet.size() == 0) {
         return null;
      } else if (JURSet.size() > 1) {
         Logging.warning("TransferDetails: JURSet has multiple entries -- not returning any!");
         return null;
      } else {
         return (JobUsageRecord) JURSet.iterator().next();
      }
   }
   
}

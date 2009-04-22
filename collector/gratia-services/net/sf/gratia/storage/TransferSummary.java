package net.sf.gratia.storage;

import java.util.Date;

import net.sf.gratia.util.Logging;

import org.apache.commons.lang.StringEscapeUtils;

//
// Gratia's in-memory representation of a JobUsage summary record.
//

// 2008/06/27 Chris Green -- MINIMAL implementation to ensure table
// creation. When this starts to be used in earnest it will be necessary
// to implement this class fully.

public class TransferSummary extends Record {

    // Information directly reflecting DB contents
    private int RecordId;
    private DateElement StartTime;
    private IntegerElement VOcorrid;
    private StringElement ProbeName;
    private StringElement RemoteSite;
    private StringElement Protocol;
    private StringElement CommonName;
    private StringElement ResourceType;
    private StringElement HostDescription;
    private IntegerElement Status;
    private IntegerElement IsNew;
    private IntegerElement Njobs;
    private ResourceElement TransferStats;

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
      // FIXME
        return "";
    }

    public StringElement getSiteName() {
        // FIXME
        return new StringElement();
    }

    public StringElement getProbeName() {
        return ProbeName;
    }

    public Date getServerDate() {
        // FIXME
        return new Date();
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
        return RecordId;
    }
 
    public void setRecordId(int RecordId) {
        this.RecordId = RecordId;
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
        return "MasterTransferSummary";
    }

    public Date getDate() {
        return StartTime.getValue();
    }

    public void AttachContent( org.hibernate.Session session ) throws Exception {
    }

    ////////////////////////////////////////////////////////////////////
    // Local get / set methods
    //
    ////////////////////////////////////////////////////////////////////
    // StartTime
    public DateElement getStartTime() {
        return StartTime;
    }

    public void setStartTime(DateElement StartTime) {
        this.StartTime = StartTime;
    }

    // VOcorrid
    public IntegerElement getVOcorrid() {
        return VOcorrid;
    }

    public void setVOcorrid(IntegerElement VOcorrid) {
        this.VOcorrid = VOcorrid;
    }
    
    // ProbeName
    // getter method above (requirement from Record)
    public void setProbeName(StringElement ProbeName) {
        this.ProbeName = ProbeName;
    }

    // CommonName
    public StringElement getCommonName() {
        return CommonName;
    }

    public void setCommonName(StringElement CommonName) {
        this.CommonName = CommonName;
    }

    // RemoteSite
    public StringElement getRemoteSite() {
        return RemoteSite;
    }

    public void setRemoteSite(StringElement RemoteSite) {
        this.RemoteSite = RemoteSite;
    }

    // Protocol
    public StringElement getProtocol() {
        return Protocol;
    }

    public void setProtocol(StringElement Protocol) {
        this.Protocol = Protocol;
    }

    // Status
    public IntegerElement getStatus() {
        return Status;
    }

    public void setStatus(IntegerElement Status) {
        this.Status = Status;
    }

    // IsNew
    public IntegerElement getIsNew() {
        return IsNew;
    }

    public void setIsNew(IntegerElement IsNew) {
        this.IsNew = IsNew;
    }

    // Njobs
    public IntegerElement getNjobs() {
        return Njobs;
    }

    public void setNjobs(IntegerElement Njobs) {
        this.Njobs = Njobs;
    }

    // TransferStats
    public ResourceElement getTransferStats() {
        return TransferStats;
    }

    public void setTransferStats(ResourceElement TransferStats) {
        this.TransferStats = TransferStats;
    }

}

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
// 2009/07/18 Brian Bockelman -- MINIMAL adoptations from JobUsageSummary to make the class work.

public class ServiceSummary extends Record {

    // Information directly reflecting DB contents
    private int RecordId;
    private DateElement Timestamp;
    private StringElement ProbeName;
    private StringElement CEUniqueID;
    private IntegerElement VOcorrid;
    private StringElement CEName;
    private StringElement HostName;
    private IntegerElement Clustercorrid;
    private StringElement SiteName;
    private IntegerElement RecordCount;
    private IntegerElement RunningJobs;
    private IntegerElement WaitingJobs;
    private IntegerElement TotalJobs;

    ////////////////////////////////////////////////////////////////////
    // Methods required by interface Record
    //
    public String asXML()
    {
      return asXML(false,false);
    }
   
    public String asXML(boolean formd5, boolean optional)
    {
      // FIXME
        return "";
    }

    public StringElement getSiteName() {
        return SiteName;
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

    public int getRecordId() {
        return RecordId;
    }

    public void setRecordId(int RecordId) {
        this.RecordId = RecordId;
    }

    public boolean setDuplicate(boolean b) {
        // FIXME
        return b;
    }

    public String getTableName() {
        return "MasterServiceSummary";
    }

    public Date getDate() {
        return Timestamp.getValue();
    }

    public void AttachContent( org.hibernate.Session session ) throws Exception {
    }

    public DateElement getTimestamp() {
      return Timestamp;
    }

    public void setTimestamp(DateElement timestamp) {
      Timestamp = timestamp;
    }

    public StringElement getProbeName() {
      return ProbeName;
    }

    public void setProbeName(StringElement probeName) {
      ProbeName = probeName;
    }

    public StringElement getCEUniqueID() {
      return CEUniqueID;
    }

    public void setCEUniqueID(StringElement uniqueID) {
      CEUniqueID = uniqueID;
    }

    public IntegerElement getVOcorrid() {
      return VOcorrid;
    }

    public void setVOcorrid(IntegerElement ocorrid) {
      VOcorrid = ocorrid;
    }

    public StringElement getCEName() {
      return CEName;
    }

    public void setCEName(StringElement name) {
      CEName = name;
    }

    public StringElement getHostName() {
      return HostName;
    }

    public void setHostName(StringElement hostName) {
      HostName = hostName;
    }

    public IntegerElement getClustercorrid() {
      return Clustercorrid;
    }

    public void setClustercorrid(IntegerElement clusterCorrId) {
      Clustercorrid = clusterCorrId;
    }

    public IntegerElement getRecordCount() {
      return RecordCount;
    }

    public void setRecordCount(IntegerElement recordCount) {
      RecordCount = recordCount;
    }

    public IntegerElement getRunningJobs() {
      return RunningJobs;
    }

    public void setRunningJobs(IntegerElement runningJobs) {
      RunningJobs = runningJobs;
    }

    public IntegerElement getWaitingJobs() {
      return WaitingJobs;
    }

    public void setWaitingJobs(IntegerElement waitingJobs) {
      WaitingJobs = waitingJobs;
    }

    public IntegerElement getTotalJobs() {
      return TotalJobs;
    }

    public void setTotalJobs(IntegerElement totalJobs) {
      TotalJobs = totalJobs;
    }

    public void setSiteName(StringElement siteName) {
      SiteName = siteName;
    }

}

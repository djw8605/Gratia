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

public class JobUsageSummary extends Record {

    // Information directly reflecting DB contents
    private int RecordId;
    private DateElement EndTime;
    private IntegerElement VOcorrid;
    private StringElement CommonName;
    private StringElement ResourceType;
    private StringElement HostDescription;
    private StringElement ApplicationExitCode;
    private IntegerElement Njobs;
    private DurationElement WallDuration;
    private DurationElement CpuUserDuration;
    private DurationElement CpuSystemDuration;
    private IntegerElement Cores;

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
        // FIXME
        return new StringElement();
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
        return "MasterSummaryData";
    }

    public Date getDate() {
        return EndTime.getValue();
    }

    public void AttachContent( org.hibernate.Session session ) throws Exception {
    }

    ////////////////////////////////////////////////////////////////////
    // Local get / set methods
    //
    ////////////////////////////////////////////////////////////////////
    // EndTime
    public DateElement getEndTime() {
        return EndTime;
    }

    public void setEndTime(DateElement EndTime) {
        this.EndTime = EndTime;
    }

    // VOcorrid
    public IntegerElement getVOcorrid() {
        return VOcorrid;
    }

    public void setVOcorrid(IntegerElement VOcorrid) {
        this.VOcorrid = VOcorrid;
    }
    
    // CommonName
    public StringElement getCommonName() {
        return CommonName;
    }

    public void setCommonName(StringElement CommonName) {
        this.CommonName = CommonName;
    }

    // ResourceType
    public StringElement getResourceType() {
        return ResourceType;
    }

    public void setResourceType(StringElement ResourceType) {
        this.ResourceType = ResourceType;
    }

    // HostDescription
    public StringElement getHostDescription() {
        return HostDescription;
    }

    public void setHostDescription(StringElement HostDescription) {
        this.HostDescription = HostDescription;
    }

    // ApplicationExitCode
    public StringElement getApplicationExitCode() {
        return ApplicationExitCode;
    }

    public void setApplicationExitCode(StringElement ApplicationExitCode) {
        this.ApplicationExitCode = ApplicationExitCode;
    }

    // Njobs
    public IntegerElement getNjobs() {
        return Njobs;
    }

    public void setNjobs(IntegerElement Njobs) {
        this.Njobs = Njobs;
    }

    // WallDuration
    public DurationElement getWallDuration() {
        return WallDuration;
    }

    public void setWallDuration(DurationElement WallDuration) {
        this.WallDuration = WallDuration;
    }

    // CpuUserDuration
    public DurationElement getCpuUserDuration() {
        return CpuUserDuration;
    }

    public void setCpuUserDuration(DurationElement CpuUserDuration) {
        this.CpuUserDuration = CpuUserDuration;
    }

    // CpuSystemDuration
    public DurationElement getCpuSystemDuration() {
        return CpuSystemDuration;
    }

    public void setCpuSystemDuration(DurationElement CpuSystemDuration) {
        this.CpuSystemDuration = CpuSystemDuration;
    }

    // Cores
    public IntegerElement getCores() {
        return Cores;
    }

    public void setCores(IntegerElement Cores) {
        this.Cores = Cores;
    }
}

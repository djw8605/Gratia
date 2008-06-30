package net.sf.gratia.storage;

import java.util.Date;

import net.sf.gratia.util.Logging;

//
// Gratia's in-memory representation of a Node summary record.
//

// 2008/06/27 Chris Green -- MINIMAL implementation to ensure table
// creation. When this starts to be used in earnest it will be necessary
// to implement this class fully.

public class NodeSummary implements Record {

    // Information directly reflecting DB contents
    private int RecordId;
    private DateElement EndTime;
    private StringElement Node;
    private StringElement ProbeName;
    private StringElement ResourceType;
    private DurationElement CpuSystemDuration;
    private DurationElement CpuUserDuration;
    private IntegerElement CpuCount;
    private StringElement HostDescription;
    private IntegerElement BenchmarkScore;
    private IntegerElement DaysInMonth;

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

    public String asXML() {
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

    public String computemd5() throws Exception {
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

    public void setProbe(Probe p) {
        // FIXME
    }

    public boolean setDuplicate(boolean b) {
        // FIXME
        return b;
    }

    public String getTableName() {
        return "NodeSummary";
    }

    public Date getDate() {
        return EndTime.getValue();
    }

    public Date getExpirationDate() {
        return new Date(); // FIXME
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

    // Node
    public StringElement getNode() {
        return Node;
    }

    public void setNode(StringElement Node) {
        this.Node = Node;
    }

    // ProbeName
    // getter method above (requirement from Record)
    public void setProbeName(StringElement ProbeName) {
        this.ProbeName = ProbeName;
    }

    // ResourceType
    public StringElement getResourceType() {
        return ResourceType;
    }

    public void setResourceType(StringElement ResourceType) {
        this.ResourceType = ResourceType;
    }

    // CpuSystemDuration
    public DurationElement getCpuSystemDuration() {
        return CpuSystemDuration;
    }

    public void setCpuSystemDuration(DurationElement CpuSystemDuration) {
        this.CpuSystemDuration = CpuSystemDuration;
    }

    // CpuUserDuration
    public DurationElement getCpuUserDuration() {
        return CpuUserDuration;
    }

    public void setCpuUserDuration(DurationElement CpuUserDuration) {
        this.CpuUserDuration = CpuUserDuration;
    }

    // CpuCount
    public IntegerElement getCpuCount() {
        return CpuCount;
    }

    public void setCpuCount(IntegerElement CpuCount) {
        this.CpuCount = CpuCount;
    }
    
    // HostDescription
    public StringElement getHostDescription() {
        return HostDescription;
    }

    public void setHostDescription(StringElement HostDescription) {
        this.HostDescription = HostDescription;
    }

    // BenchmarkScore
    public IntegerElement getBenchmarkScore() {
        return BenchmarkScore;
    }

    public void setBenchmarkScore(IntegerElement BenchmarkScore) {
        this.BenchmarkScore = BenchmarkScore;
    }

    // DaysInMonth
    public IntegerElement getDaysInMonth() {
        return DaysInMonth;
    }

    public void setDaysInMonth(IntegerElement DaysInMonth) {
        this.DaysInMonth = DaysInMonth;
    }

}

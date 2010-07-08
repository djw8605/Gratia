/**
 * 
 */
package net.sf.gratia.storage;

/**
 * @author pcanal
 *
 */
public class VONameCorrection {
    private long corrid;
    private long VOid;
    private String VOName;
    private String ReportableVOName;
    
    public VONameCorrection()
    {
    }

    public long getVOid() {
            return VOid;
    }

    public void setVOid(long id) {
            this.VOid = id;
    }

    public long getcorrid() {
        return corrid;
    }

    public void setcorrid(long id) {
        this.corrid = id;
    }

    public String getVOName() {
            return VOName;
    }

    public void setVOName(String name) {
        VOName = name;
    }

    public String getReportableVOName() {
        return ReportableVOName;
    }

    public void setReportableVOName(String name) {
        ReportableVOName = name;
    }

    public String toString() {
        String output = "Site: " + "VOid: " + VOid + " VOName: " + VOName;
        output = output + " ReportableVOName: " + ReportableVOName;
        return output;
    }

}

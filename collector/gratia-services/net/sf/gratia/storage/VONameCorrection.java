/**
 * 
 */
package net.sf.gratia.storage;

/**
 * @author pcanal
 *
 */
public class VONameCorrection {
    private int corrid;
    private int VOid;
    private String VOName;
    private String ReportableVOName;
    
    public VONameCorrection()
    {
    }

    public int getVOid() {
            return VOid;
    }

    public void setVOid(int id) {
            this.VOid = id;
    }

    public int getcorrid() {
        return corrid;
    }

    public void setcorrid(int id) {
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

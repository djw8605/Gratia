/**
 * 
 */
package net.sf.gratia.storage;

/**
 * @author weigand
 *
 */
public class ProjectNameCorrection {
    private long ProjectNameCorrid;
    private String ProjectName;
    private String ReportableProjectName;
    
    public ProjectNameCorrection()
    {
    }

    public long getProjectNameCorrid() {
            return ProjectNameCorrid;
    }

    public void setProjectNameCorrid(long id) {
            this.ProjectNameCorrid = id;
    }

    public String getProjectName() {
            return ProjectName;
    }

    public void setProjectName(String name) {
        ProjectName = name;
    }

    public String getReportableProjectName() {
        return ReportableProjectName;
    }

    public void setReportableProjectName(String name) {
        ReportableProjectName = name;
    }

    public String toString() {
        String output = "Site: " + "ProjectNameCorrid: " + ProjectNameCorrid + " ProjectName: " + ProjectName;
        output = output + " ReportableProjectName: " + ReportableProjectName;
        return output;
    }

}

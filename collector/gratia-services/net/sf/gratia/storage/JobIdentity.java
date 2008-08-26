package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * <p>Title: JobIdentity</p>
 *
 * <p>Description: Contains all the known information about the job.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 */
public class JobIdentity {
    private String GlobalJobId;
    private String LocalJobId;
    private String ProcessIds;
    public JobIdentity() {
    }

    public void setGlobalJobId(String GlobalJobId) {
        this.GlobalJobId = GlobalJobId;
    }

    public String getGlobalJobId() {
        return GlobalJobId;
    }

    public void setLocalJobId(String LocalJobId) {
        this.LocalJobId = LocalJobId;
    }

    public String getLocalJobId() {
        return LocalJobId;
    }

    public void addProcessId(String ProcessId) {
        if (this.ProcessIds == null) this.ProcessIds = ProcessId;
        else this.ProcessIds = this.ProcessIds + " " + ProcessIds;
    }

    public void setProcessIds(String ProcessIds) {
        this.ProcessIds = ProcessIds;
    }

    public String getProcessIds() {
        return ProcessIds;
    }

    public String toString() {
        return " JobId: (Global: "+GlobalJobId+ " Local: "+LocalJobId+" ProcessIds: "+ProcessIds;
    }

    public String asXml() {
        String output = "<JobIdentity>\n";
        if (GlobalJobId != null) output = output + "<GlobalJobId >"+StringEscapeUtils.escapeXml(GlobalJobId)+"</GlobalJobId>\n";
        if (LocalJobId != null) output = output + "<LocalJobId >"+StringEscapeUtils.escapeXml(LocalJobId)+"</LocalJobId>\n";
        if (ProcessIds != null) output = output + "<ProcessId>"+StringEscapeUtils.escapeXml(ProcessIds)+"</ProcessId>\n"; // FIXME: need to split the ProcessIds
        output = output + "</JobIdentity>\n";
        return output;
    }
}

package net.sf.gratia.storage;

public class Probe
{
    private int probeid;

    private Site site;

    private String probename;
    private DateElement currenttime;
    private int active;
    private int reporthh;
    private int reportmm;
    private String status;
    private int nRecords;
    private int nConnections;
    private int nDuplicates;

    public Probe()
    {
        site = null;
        nRecords = 0;
        nConnections = 0;
        nDuplicates = 0;
    }

    public Probe(String name)
    {
        probename = name;
        site = null;
    }
    
    public int getsiteid() 
    {
        if (site != null) return site.getsiteid();
        else return 0;
    }
    
//     public void setsiteid(int siteid) 
//     {
//         this.siteid = siteid;
//     }

    public Site getsite() { return site; }
    public void setsite(Site s) { site = s; }
    
    public int getprobeid() 
    {
        return probeid;
    }
    
    public void setprobeid(int probeid) 
    {
        this.probeid = probeid;
    }
    
    public String getprobename() 
    {
        return probename;
    }
    
    public void setprobename(String name) 
    {
        probename = name;
    }
    
    public DateElement getcurrenttime()
    {
        return currenttime;
    }
    
    public void setcurrenttime(DateElement time)
    {
        this.currenttime = time;
    }
    
    public int getreporthh()
    {
        return reporthh;
    }
    
    public void setreporthh(int value)
    {
        this.reporthh = value;
    }
    
    public int getreportmm()
    {
        return reportmm;
    }
    
    public void setreportmm(int value)
    {
        this.reportmm = value;
    }
    
    public int getactive()
    {
        return active;
    }
    
    public void setactive(int value)
    {
        this.active = value;
    }
    
    public void setstatus(String value)
    {
        this.status = value;
    }
    
    public String getstatus()
    {
        return status;
    }
    
    public int getnRecords()
    {
        return nRecords;
    }
    
    public void setnRecords(int value)
    {
        this.nRecords = value;
    }
    
    public int getnConnections()
    {
        return nConnections;
    }
    
    public void setnConnections(int value)
    {
        this.nConnections = value;
    }
    
    public int getnDuplicates()
    {
        return nDuplicates;
    }
    
    public void setnDuplicates(int value)
    {
        this.nDuplicates = value;
    }
    
    public String toString() 
    {
        String output = 
            "Probe: " + 
            " siteid: " + getsiteid() +
            " probeid: " + probeid +
            " probename: " + probename +
            " currenttime: " + currenttime +
            " active: " + active +
            " reporthh: " + reporthh +
            " reportmm: " + reportmm +
            " status: " + status +
            " nRecords: " + nRecords +
            " nConnections: " + nConnections +
            " nDuplicates: " + nDuplicates;
        return output;
    }
}

package net.sf.gratia.storage;

public class Site
{
    private long siteid;
    private String siteName;
	
    public Site()
    {
    }

    public Site(String name)
    {
        siteName = name;
    }

    public long getsiteid() {
        return siteid;
    }
    
    public void setsiteid(long id) {
        this.siteid = id;
    }
    
    public String getsiteName() {
        return siteName;
    }
    
    public void setsiteName(String name) {
        siteName = name;
    }
    
    public String toString() {
        String output = "Site: " + "siteid: " + siteid + " SiteName: " + siteName;
        return output;
    }
}

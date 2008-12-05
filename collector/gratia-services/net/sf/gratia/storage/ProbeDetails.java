package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.Date;

/**
 * <p>Title: ProbeDetails </p>
 *
 * <p>Description: Use to hold the information send by the Probe during the handshake.
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 *
 */

public class ProbeDetails extends Record
{
    // Information regarding the probe itself
    private java.util.Map<String, Software> SoftwareMap;
    private Set Software;

    // Calculated information (not directly in the xml file)
    private Probe Probe;

    // Meta Information (from the xml file)
    private RecordIdentity RecordIdentity;
    private StringElement ProbeName;
    private StringElement SiteName;
    private StringElement Grid;

    // Meta Information (not part of the xml file per se).
    private int RecordId;
    private String RawXml;   // Complete Usage Record Xml
    private String ExtraXml; // Xml fragment not used for any of the data members/field
    private Date ServerDate;
    private String md5;
    
    public ProbeDetails()
    {
        RecordIdentity = null; // new RecordIdentity();
        RawXml = "";
        ExtraXml = "";
        ServerDate = new Date();
    }

    public void addSoftware(Software soft) 
    {
        if (this.Software == null) {
            this.Software = new java.util.HashSet();
            this.SoftwareMap =  new java.util.HashMap<String,Software>();
        }
        try {
            if (this.SoftwareMap.get( soft.getmd5() ) == null ) {
                this.SoftwareMap.put( soft.getmd5(), soft);
                this.Software.add( soft );
            }
        } catch (Exception e) {
            // ignore software if we can't get the md5
        }        
    }

    public void setSoftware(Set s) { 
        this.Software = s; 

        for (Iterator i = s.iterator(); i.hasNext(); )
         {
             Software soft = (Software)i.next();
             try {
                 if (this.SoftwareMap.get( soft.getmd5() ) == null ) {
                     this.SoftwareMap.put( soft.getmd5(), soft);
                 }
             } catch (Exception e) {
                 // ignore software if we can't get the md5
             }
         }
    }

    public Set getSoftware() { return this.Software; }

    public void setProbeName(StringElement ProbeName)
    {
	this.ProbeName = ProbeName;
    }
    
    public Probe getProbe() { return Probe; }
    public void setProbe(Probe p) { this.Probe = p; }
    public boolean setDuplicate(boolean b) 
    {
        // setDuplicate will increase the count (nRecords,nConnections,nDuplicates) for the probe
       // and will return true if the duplicate needs to be recorded as a potential error.
        this.Probe.setnConnections( Probe.getnConnections() + 1 );
        return false;
    }

    public StringElement getProbeName()
    {
	return ProbeName;
    }
    
    public void setSiteName(StringElement SiteName)
    {
        this.SiteName = SiteName;
    }
    
    public StringElement getSiteName()
    {
        return SiteName;
    }
    
    public void setGrid(StringElement Grid)
    {
        this.Grid = Grid;
    }
    public StringElement getGrid()
    {
        return Grid;
    }

    public void setRecordId(int RecordId)
    {
        this.RecordId = RecordId;
    }
    
    public int getRecordId()
    {
        return RecordId;
    }

    public void setRecordIdentity(RecordIdentity n) { RecordIdentity = n; }
    public RecordIdentity getRecordIdentity()
    {
        return RecordIdentity;
    }

    public void addRawXml(String RawXml)
    {
	this.RawXml = this.RawXml + RawXml;
    }
    
    public void setRawXml(String RawXml)
    {
	this.RawXml = RawXml;
    }
    
    public String getRawXml()
    {
	return RawXml;
    }
    
    public void addExtraXml(String ExtraXml)
    {
	this.ExtraXml = this.ExtraXml + ExtraXml;
    }
    
    public void setExtraXml(String ExtraXml)
    {
	this.ExtraXml = ExtraXml;
    }
    
    public String getExtraXml()
    {
	return ExtraXml;
    }

    public static Date expirationDate() {
        return new Date(0);
    }
    
    public Date getDate() 
    {
        // Returns the date this records is reporting about.
        return new Date(); // We don't know, so say it's about today!
    }
    
    public Date getServerDate()
    {
        return ServerDate;
    }
    
    public void setServerDate(Date value)
    {
        ServerDate = value;
    }
    
    public String setToString(String name, Set l)
    {
        String output = "";
        for (Iterator i = l.iterator(); i.hasNext(); )
            {
                Object el = i.next();
                output = output + " " + name + ": " + el;
            }
        return output;
    }
    
    public String mapToString(String name, java.util.Map<String, Software> l)
    {
        String output = "";
        for (Iterator i = l.values().iterator(); i.hasNext(); )
            {
                Object el = i.next();
                output = output + " " + name + ": " + el;
            }
        return output;
    }
    
    public String setAsXml(String name, Set l)
    {
        String output = "";
        for (Iterator i = l.iterator(); i.hasNext(); )
            {
                XmlElement el = (XmlElement)i.next();
                output = output + el.asXml(name);
            }
        return output;
    }
    
    public String mapAsXml(String name, java.util.Map<String,Software> l)
    {
        String output = "";
        for (Iterator i = l.values().iterator(); i.hasNext(); )
            {
         XmlElement el = (XmlElement)i.next();
         output = output + el.asXml(name);
            }
        return output;
    }
    
    public String toString()
    {
        String output = "";
        //      String output = "Details dbid: " + RecordId + "\n";
        if (RecordIdentity != null) output = output + RecordIdentity + "\n";
        if (SiteName != null) output = output + " SiteName: " + SiteName + "\n";
        if (ProbeName != null) output = output + "ProbeName: " + ProbeName + "\n";
        if (ProbeName != null) output = output + "Grid: " + Grid + "\n";
        if (SoftwareMap != null) output = output + mapToString("", SoftwareMap);

        return output;
    }
    
    public String asXML()
    {
        String output = ""; // ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        output = output + ("<ProbeDetails xmlns:urwg=\"http://www.gridforum.org/2003/ur-wg\">\n");
        if (RecordIdentity != null) output = output + RecordIdentity.asXml();
        if (ProbeName != null) output = output + ProbeName.asXml("ProbeName");
        if (SiteName != null) output = output + SiteName.asXml("SiteName");
        if (Grid != null) output = output + Grid.asXml("Grid");

        if (SoftwareMap != null) output = output + mapAsXml("", SoftwareMap);
        
        output = output + ("</ProbeDetails>\n");
        return output;
    }
    
    public void AttachContent( org.hibernate.Session session ) throws Exception
    {
       for (Iterator i = this.SoftwareMap.values().iterator(); i.hasNext(); )
           {
               Software s = (Software)i.next();
               
          s.Attach(session);
           }
    }
    
    public String computemd5() throws Exception
    {
        RecordIdentity temp = getRecordIdentity();
        setRecordIdentity(null);
        String md5key = Utils.md5key(asXML());
        setRecordIdentity(temp);
        
        return md5key;
    }
    
    public String getmd5()
    {
      return md5;
    }
    
    public void setmd5(String value)
    {
        md5 = value;
    }

    public String getTableName()
    {
        return "ProbeDetails";
    }
}

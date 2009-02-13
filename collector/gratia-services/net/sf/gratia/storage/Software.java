package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

import net.sf.gratia.util.Logging;

/**
 * <p>Title: Software </p>
 *
 * <p>Description: Use to hold the information send by the Probe during the handshake
 *    regarding which services, libraries and executable it is dealing with or using.
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 *
 */


public class Software implements XmlElement
{
    private int softid;
    private String Name;
    private String Version;
    private String Type;
    private String md5;

    public Software()
    {
        // Default Constructor
    }

    public void setsoftid(int id) { softid = id; }
    public int getsoftid() { return softid; }

    public void setName(String val) { Name = val; }
    public String getName() { return Name; }

    public void setVersion(String val) { Version = val; }
    public String getVersion() { return Version; }

    public void setType(String val) { Type = val; }
    public String getType() { return Type; }

   public String asXml(String elementName) {
      StringBuilder output = new StringBuilder();
      asXml(output,elementName);
      return output.toString();
   }
   
   public void asXml(StringBuilder output, String elementName)
    {
        output.append("<");
        output.append(Type + " ");
        if (Version != null) output.append("version = \"" +
                                           StringEscapeUtils.escapeXml(Version) + "\"");
        if (Name != null) {
            output.append(">" + StringEscapeUtils.escapeXml(Name) + "</" +
                          StringEscapeUtils.escapeXml(Type) + ">");
        } else {
            output.append("/>");
        }
    }
    
    public String toString() 
    {
        String output = "Software id#: " + softid + "\n";
        output = output + "type: " +Type + "\n";
        output = output + "name: " + Name + "\n";
        output = output + "version: " + Version + "\n";
        return output;
    }

    public String computemd5() throws Exception
    {
        String md5key = Utils.md5key(asXml(""));        
        return md5key;
    }
    
    public String getmd5() throws Exception
    {
        if (md5 == null) return computemd5();
        return md5;
    }
    
    public void setmd5(String value)
    {
        md5 = value;
    }

    private static java.util.Map<String, Software> fgSaved = new java.util.HashMap<String,Software>();

    public static Software getSoftware( Software check ) throws Exception
    {
        return fgSaved.get( check.getmd5() );
    }
    public static void setSoftware( Software soft ) throws Exception
    {
        fgSaved.put( soft.getmd5(), soft );
    }
        
        
    public static Software getSoftware( org.hibernate.Session session, Software check )  throws Exception
    {
        Software soft = null;
        String command = "from Software where md5 = ?";
        
        java.util.List result = session.createQuery(command).setString(0,check.getmd5()).list();
        
        if (result.size() == 0) {

            return null;

        } else if (result.size() >= 1) {
            
            for(int i=0; i<result.size(); ++i) {
                soft = (Software) result.get(i);
            
                // Should we cross check the md5?
                if ( check.asXml("").equals( soft.asXml("") ) ) {
                    return soft;
                }
            }
        }
        return null;
        
    }

    public void Attach( org.hibernate.Session session ) throws Exception
    {
        Logging.debug("Software::Attach: " + softid);

        if (softid != 0) return;

        // First check whether this Software is already in memory
        Software attached = Software.getSoftware( this );

        if (attached != null) 
        {

            this.setsoftid(  attached.getsoftid() ); 

        } else {           

            // Check whether this Software is already in the db
            attached = Software.getSoftware( session, this );
            if (attached != null) 
            {
                this.setsoftid(  attached.getsoftid() );
            
            } else {
            
                // Otherwise save it.
                session.save(this);
                
            }   
        }
    }

}

package net.sf.gratia.storage;

import net.sf.gratia.util.Logging;
import org.apache.commons.lang.StringEscapeUtils;

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

public class Software implements AttachableXmlElement
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
      
      // For AttachableXmlElement interface:
      
      public void setId(long id) { setsoftid((int)id); }
      public long getId() { return (long)getsoftid(); }
      
      public String getmd5() throws Exception
      {
         if (md5 == null) return computemd5();
         return md5;
      }
      
      public void setmd5(String value)
      {
         md5 = value;
      }
      
      public boolean equals(Object obj) 
      {
         if (this == obj) return true;
         if ( !(obj instanceof Software) ) return false;
         
         final Software soft = (Software) obj;
         
         if ( !soft.getName().equals( getName() ) ) return false;
         if ( !soft.getVersion().equals( getVersion() ) ) return false;
         if ( !soft.getType().equals( getType() ) ) return false;
          
         return true;
      }
      
      public int hashCode() {
         if (md5 != null) {
            return md5.hashCode();
         } else {
            int hash = 0;
            if (Name != null) hash = hash + Name.hashCode();
            if (Version != null) hash = hash + Version.hashCode();
            if (Type != null) hash = hash + Type.hashCode();
            return hash;
         }
      }

      
      // Software Specific interface.
      
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

      private static AttachableCollection<Software> fgSaved = new AttachableCollection<Software>();

      public static Software getSoftware( org.hibernate.Session session, Software check )  throws Exception
      {
         return fgSaved.getObject( session, check );         
      }
      
      public Software Attach( org.hibernate.Session session ) throws Exception
      {
         return fgSaved.attach( session, this );
      }
      
   }

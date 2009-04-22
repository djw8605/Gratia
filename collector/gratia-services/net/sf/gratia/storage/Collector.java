package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * <p>Title: Collector </p>
 *
 * <p>Description: Use to hold the information about (remove) Collectors
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 *
 */

public class Collector implements AttachableXmlElement
{
   private long collid;
   private int fState;
   private String md5;
   
   // XML portion.
   String fName;
   
   public Collector()
   {
      // Default Constructor
   }
   
   public Collector(String name)
   {
      fName = name;
   }
   
   // For AttachableXmlElement interface:
   
   public void setId(long id) { setcollid(id); }
   public long getId() { return getcollid(); }
   
   public String getmd5() throws Exception
   {
      if (md5 == null) return computemd5();
      return md5;
   }
   
   public void setmd5(String value)
   {
      md5 = value;
   }
   
   private boolean check(Object me, Object other) 
   {
      if ( me == null && other == null) return true;
      if ( me != null && other != null) return me.equals( other );
      return false;
   }
   
   public boolean equals(Object obj) 
   {
      if (this == obj) return true;
      if ( !(obj instanceof Collector) ) return false;
      
      final Collector conn = (Collector) obj;
      
      if ( !check( getName(), conn.getName()) ) return false;
      
      return true;
   }
   
   public int hashCode() {
      if (false && md5 != null) {
         return md5.hashCode();
      } else {
         int hash = 0;
         if (fName != null) hash = hash + fName.hashCode();
         return hash;
      }
   }
   
   public void setcollid(long id) { collid = id; }
   public long getcollid() { return collid; }
   
   public void setName(String val) { fName = val; }
   public String getName() { return fName; }
   
   
   public String asXml(String elementName) {
      StringBuilder output = new StringBuilder();
      asXml(output,elementName);
      return output.toString();
   }
   
   private void asXml(StringBuilder output, String elementName, String value) 
   {
      if (value != null) {
         output.append("<" + elementName + ">");
         output.append(StringEscapeUtils.escapeXml(value));
         output.append("</" + elementName + ">\n");
      }
   }
   
   public void asXml(StringBuilder output, String elementName)
   {
      output.append("<Collector>");
      asXml(output,"Name",fName);
      output.append("</Collector>\n");
      
   }
   
   public String toString() 
   {
      String output = "Collector id#: " + collid + "\n";
      output = output + "Name: " + fName + "\n";
      return output;
   }
   
   public String computemd5() throws Exception
   {
      String md5key = Utils.md5key(asXml(""));        
      return md5key;
   }
   
   private static AttachableCollection<Collector> fgSaved = new AttachableCollection<Collector>();
   
   public static Collector getCollector( org.hibernate.Session session, Collector check )  throws Exception
   {
      return fgSaved.getObject( session, check );         
   }
   
   public Collector attach( org.hibernate.Session session ) throws Exception
   {
      Collector coll = fgSaved.attach( session, this );
      // session.saveOrUpdate( coll );
      return coll;
   }
   
}
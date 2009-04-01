package net.sf.gratia.storage;

/**
 * <p>Title: AttachableXmlElement</p>
 *
 * <p>Description: Interface to allow maninuplation of 'sub' elements that persist beyhond a single record.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author Philippe Canal
 * @version 1.0
 */

public interface AttachableXmlElement extends XmlElement {
 
   // Unique indentifiers, usually the primary key
   public long getId();
   public void setId(long newvalue);
   
   // Calculated string 'almost' uniquely representing the object.
   public String getmd5() throws Exception;
   public void setmd5(String newvalue);
   
   // Indentity definitions.
   public boolean equals(Object obj);
   public int hashCode();
   
}

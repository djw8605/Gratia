package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;
import net.sf.gratia.util.Logging;

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
      // See Record class
      
      // Meta Information (from the xml file)
      // See Record class
      
      // Meta Information (not part of the xml file per se).
      // See Record class
      
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
         // Replace the current 'map' with a new empty one.
         this.SoftwareMap =  new java.util.HashMap<String,Software>();
         
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
      
      public boolean setDuplicate(boolean b) 
      {
         // setDuplicate will increase the count (nRecords,nConnections,nDuplicates) for the probe
         // and will return true if the duplicate needs to be recorded as a potential error.
         this.Probe.setnConnections( Probe.getnConnections() + 1 );
         return false;
      }
      
      public static Date expirationDate() {
         return new Date(0);
      }
      
      public Date getDate() 
      {
         // Returns the date this records is reporting about.
         return new Date(); // We don't know, so say it's about today!
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
      
      public void setAsXml(StringBuilder output, String name, Set<XmlElement> coll)
      {
         for (XmlElement el : coll) {
            el.asXml(output, name);
            output.append("\n");
         }
      }
      
      public void mapAsXml(StringBuilder output, String name, java.util.Map<String,Software> coll)
      {
         for (XmlElement el : coll.values() ) {
            el.asXml(output, name);
            output.append("\n");
         }
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
         
         if (Origins != null) output = output + Origins.toString();
         return output;
      }
      
      public String asXML()
      {
         return asXML(false,false);
      }
      
      public String asXML(boolean formd5, boolean optional)
      {
         // If formd5 is true do not include
         //    RecordIdentity
         // in calculation.
         
         StringBuilder output = new StringBuilder(""); // ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
         output.append("<ProbeDetails xmlns:urwg=\"http://www.gridforum.org/2003/ur-wg\">\n");
         if (!formd5) { 
            if (RecordIdentity != null) RecordIdentity.asXml(output);
         }
         if (ProbeName != null) ProbeName.asXml(output,"ProbeName");
         if (SiteName != null) SiteName.asXml(output,"SiteName");
         if (Grid != null) Grid.asXml(output,"Grid");
         
         if (SoftwareMap != null) mapAsXml(output,"", SoftwareMap);

         if (!formd5) {
            if (Origins != null) originsAsXml(output);
         }
         
         output.append("</ProbeDetails>\n");
         return output.toString();
      }
      
      public void AttachContent( org.hibernate.Session session ) throws Exception
      {
         AttachOrigins( session );
         
         if (this.SoftwareMap != null) {
            List oldlist = new java.util.ArrayList<Software>();
            List newlist = new java.util.ArrayList<Software>();
            Iterator i = this.SoftwareMap.values().iterator();
            while ( i.hasNext() )
            {
               Software s = (Software)i.next();
               
               Software attached = s.Attach(session);
               
               if (attached != s) {
                  // Case where the Software already existed in the cache.
                  oldlist.add( s );
                  newlist.add( attached );
               }
            }
            i = oldlist.iterator();
            while( i.hasNext() )
            {
               Software oldsoft = (Software)i.next();
               this.SoftwareMap.remove( oldsoft.getmd5() );
               this.Software.remove( oldsoft );
            }
            i = newlist.iterator();
            while( i.hasNext() )
            {
               Software newsoft = (Software)i.next();
               addSoftware( newsoft );
            }
         }
      }
      
      public String computemd5(boolean optional) throws Exception
      {
         String md5key = Utils.md5key(asXML(true, optional));
         
         return md5key;
      }
      
      public String getTableName()
      {
         return "ProbeDetails";
      }
   }

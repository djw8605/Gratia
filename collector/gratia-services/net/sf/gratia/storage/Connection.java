package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

import net.sf.gratia.util.Logging;
import java.util.Date;

/**
 * <p>Title: Connection </p>
 *
 * <p>Description: Use to hold the information about connection between Probe (or Collector)
 * and a collector.
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 *
 */


public class Connection implements AttachableXmlElement
   {
      private long cid;
      private int fState;
      private String md5;

      // XML portion.
      private String fClient;
      private String fSender;
      private Certificate fCertificate;
      private Connection fServer;
      private Date fFirstSeen; // Local to this collector
      private Date fLastSeen;  // Local to this collector

      final int kInvalid = 0;
      final int kValid = 1;
      
      public Connection()
      {
         // Default Constructor
         fState = kValid;
      }

      public Connection(String client, String sender, Certificate cert)
      {
         fClient = client;
         fSender = sender;
         fCertificate = cert;
         fState = kValid;
      }
      
      // For AttachableXmlElement interface:
      
      public void setId(long id) { setcid(id); }
      public long getId() { return getcid(); }
      
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
         if ( !(obj instanceof Connection) ) return false;
         
         final Connection conn = (Connection) obj;
         
         if ( !check( getClient(), conn.getClient()) ) return false;
         if ( !check( getSender(), conn.getSender()) ) return false;
         if ( !check( getCertificate(), conn.getCertificate()) ) return false;
         if ( !check( getServer(), conn.getServer()) ) return false;
         
         return true;
      }
      
      public int hashCode() {
         if (false && md5 != null) {
            return md5.hashCode();
         } else {
            int hash = 0;
            if (fSender != null) hash = hash + fSender.hashCode();
            if (fCertificate != null) hash = hash + fCertificate.hashCode();
            return hash;
         }
      }
      
      
      public void setcid(long id) { cid = id; }
      public long getcid() { return cid; }
      
      public void setClient(String val) { fClient = val; }
      public String getClient() { return fClient; }
      
      public void setSender(String val) { fSender = val; }
      public String getSender() { return fSender; }
      
      public void setCertificate(Certificate val) { fCertificate = val; }
      public Certificate getCertificate() { return fCertificate; }
      
      public void setServer(Connection val) { fServer = val; }
      public Connection getServer() { return fServer; }
      public String getServerName() {
         if (fServer != null) {
            return fServer.getSender();
         } else {
            return net.sf.gratia.services.CollectorService.getName();
         }
      }
      
      public void setFirstSeen(Date value) { fFirstSeen = value; }
      public Date getFirstSeen() { return fFirstSeen; }

      public void setLastSeen(Date value) { fLastSeen = value; }
      public Date getLastSeen() { return fLastSeen; }

		public boolean isValid() 
		{
         return fState == kValid;
		}
      
		public void setValid(boolean value) 
		{
         fState = value ? kValid : kInvalid;
		}
      
      public int getState() { return fState; }
      public void setState(int value) { fState = value; }
      
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
         output.append("<Connection>");
         asXml(output,"Client",fClient);
         asXml(output,"Sender",fSender);
         asXml(output,"Server",getServerName());
         if (fCertificate != null) fCertificate.asXml(output);
         output.append("</Connection>\n");

      }
      
      public String toString() 
      {
         String output = "Connection id#: " + cid + " and ref " + Integer.toHexString(System.identityHashCode(this)) + "\n";
         output = output + "Client: " + fClient + "\n";
         output = output + "Sender: " + fSender + "\n";
         output = output + "Server: " + getServerName() + "\n";
         output = output + "Certificate: " + fCertificate + "\n";
         return output;
      }
      
      public String computemd5() throws Exception
      {
         String md5key = Utils.md5key(asXml(""));        
         return md5key;
      }
            
      private static AttachableCollection<Connection> fgSaved = new AttachableCollection<Connection>();
      
      public static Connection getConnection( org.hibernate.Session session, Connection check )  throws Exception
      {
         return fgSaved.getObject( session, check );         
      }
      
      public Connection attach( org.hibernate.Session session ) throws Exception
      {
         Connection conn = fgSaved.attach( session, this );
         if (conn.fFirstSeen == null) {
            conn.fFirstSeen = new Date();
         }
         conn.fLastSeen = new Date();
         session.saveOrUpdate( conn );
         return conn;
      }
      
   }

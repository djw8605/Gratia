package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

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


public class Connection implements AttachableXmlElement, Comparable
   {
      private long cid;
      private int fState;
      private String md5;

      // XML portion.
      private String fSenderHost;  // Usually the senderHost ip address. 
      private String fSender;      // the probe name or collector name
      private Certificate fCertificate;
      private Collector fCollector;  // The name of the receiving collector.
      private Date fFirstSeen; // Local to this collector
      private Date fLastSeen;  // Local to this collector

      final int kInvalid = 0;
      final int kValid = 1;

      private static String gDefaultCollectorName = "Standalone";
      
      public Connection()
      {
         // Default Constructor
         fState = kValid;
      }

      public Connection(Connection conn)
      {
         cid = conn.cid;
         fSenderHost = conn.fSenderHost;
         fSender = conn.fSender;
         fCertificate = conn.fCertificate;
         fState = conn.fState;
         fCollector = conn.fCollector;
         fFirstSeen = conn.fFirstSeen;
         fLastSeen = conn.fLastSeen;
      }
      
      public Connection(String senderHost, String sender, Certificate cert)
      {
         fSenderHost = senderHost;
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
      
      @Override
      public boolean equals(Object obj) 
      {
         if (this == obj) return true;
         if ( !(obj instanceof Connection) ) return false;
         
         final Connection conn = (Connection) obj;
         
         if ( !check( getSenderHost(), conn.getSenderHost()) ) return false;
         if ( !check( getSender(), conn.getSender()) ) return false;
         if ( !check( getCertificate(), conn.getCertificate()) ) return false;
         if ( !check( getCollector(), conn.getCollector()) ) return false;
         
         return true;
      }
      
    @Override
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
      
      public int compareTo(Object obj) {
         try {
            Connection cmp = (Connection) obj;
            
            int result;
            if (getSender() != null && cmp.getSender() != null) {
               result = getSender().compareTo( cmp.getSender() );
            } else {
               result = getSenderHost().compareTo( cmp.getSenderHost() );
            }
            if (result == 0) {
               result =  getCollectorName().compareTo( cmp.getCollectorName() );
            }
            return result;
         }
         catch (Exception e) {
            return -1;
         }
      }
      
      
      public void setcid(long id) { cid = id; }
      public long getcid() { return cid; }
      
      public void setSenderHost(String val) { fSenderHost = val; }
      public String getSenderHost() { return fSenderHost; }
      
      public void setSender(String val) { fSender = val; }
      public String getSender() { return fSender; }
      
      public void setCertificate(Certificate val) { fCertificate = val; }
      public Certificate getCertificate() { return fCertificate; }
      
      public void setCollector(Collector val) { fCollector = val; }
      public Collector getCollector() { return fCollector; }
      public void setCollectorName(String name) {
         if (fCollector==null) {
            if (name != null) {
               fCollector = new Collector(name);
            }
         } else {
            fCollector.setName(name);
         }
      }
      public String getCollectorName() {
         if (fCollector != null) {
            return fCollector.getName();
         } else {
            return gDefaultCollectorName; // net.sf.gratia.services.CollectorService.getName();
         }
      }
      static public void setDefaultCollectorName(String name) {
         gDefaultCollectorName = name;
      }
      static public String getDefaultCollectorName() {
         return gDefaultCollectorName;
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
         asXml(output,"SenderHost",fSenderHost);
         asXml(output,"Sender",fSender);
         asXml(output,"Collector",getCollectorName());
         if (fCertificate != null) fCertificate.asXml(output);
         output.append("</Connection>\n");

      }
      
      public String toString() 
      {
         String output = "Connection id#: " + cid + " and ref " + Integer.toHexString(System.identityHashCode(this)) + "\n";
         output = output + "SenderHost: " + fSenderHost + "\n";
         output = output + "Sender: " + fSender + "\n";
         output = output + "Collector: " + getCollectorName() + "\n";
         output = output + "Certificate: " + fCertificate + "\n";
         output = output + "FirstSeen: " + fFirstSeen + "\n";
         output = output + "LastSeen: " + fLastSeen + "\n";
         output = output + "State: " + fState + "\n";
         return output;
      }
      
      public String computemd5() throws Exception
      {
         String md5key = Utils.md5key(asXml(""));        
         return md5key;
      }
            
      private static AttachableCollection<Connection> fgSaved = new AttachableCollection<Connection>();
      
      public static void setCaching(boolean enable) 
      {
         // Enable or disable the caching of connection.
         fgSaved.setCaching( enable );
      }
      
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

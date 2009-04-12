package net.sf.gratia.storage;

import net.sf.gratia.util.Logging;
import java.util.Date;

/**
 * <p>Title: Origin </p>
 *
 * <p>Description: Use to keep track of when the record was transfered between a Probe (or Collector)
 * and another collector.
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 *
 */

public class Origin
   {
      private long fOriginId;
      private DateElement fServerDate;
      private Connection fConnection;
      
      public Origin() 
      {
         fServerDate = new DateElement();
      }
      
      public Origin(java.util.Date when)
      {
         fServerDate = new DateElement();         
         fServerDate.setValue(when);
      }
      
      public Origin(Connection grconn, java.util.Date when)
      {
         fServerDate = new DateElement();         
         fServerDate.setValue(when);
         fConnection = grconn;
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
         if ( !(obj instanceof Origin) ) return false;
         
         final Origin other = (Origin) obj;
         
         if ( !getServerDate().equals( other.getServerDate()) ) return false;
         if ( !check( getConnection(), other.getConnection()) ) return false;
         
         return true;
      }
      
      public int hashCode() {
         int hash = fServerDate.getValue().hashCode();
         if (fConnection != null) hash = hash + fConnection.hashCode();
         return hash;
      }
      
      public long getOriginId() {
         return fOriginId;
      }
      
      public void setOriginId(long value) {
         fOriginId = value;
      }
      
      public void setServerDate(Date value) {
         fServerDate.setValue ( value );
      }
      
      public Date getServerDate() {
         return fServerDate.getValue();
      }
      
      public void setConnection(Connection grconn) 
      {
         fConnection = grconn;
      }
      
      public Connection getConnection() {
         return fConnection;
      }
      
      public String toString(int hopNumber) {
         StringBuilder output = new StringBuilder("");
         output.append("HopNumber: ");
         output.append(hopNumber);
         output.append("\n");
         output.append("ServerDate: ");
         output.append(fServerDate.toString());
         output.append("\n");
         output.append("Connection: ");
         if (fConnection==null) {
            output.append("missing!\n");
         } else {
            output.append(fConnection.toString());
         }
         return output.toString();
      }
      
      public String toString() {
         return toString(-1);
      }
      
      public String asXml(int hopNumber) {
         StringBuilder output = new StringBuilder();
         asXml(output,hopNumber);
         return output.toString();
      }
      
      public void asXml(StringBuilder output, int hopNumber) {
         output.append("<Origin hop=\""+hopNumber+"\" >");
         fServerDate.asXml(output,"ServerDate");
         if (fConnection!=null) {
            fConnection.asXml(output,"");
         }
         output.append("</Origin>");
      }
      
      public void AttachContent( org.hibernate.Session session ) throws Exception
      {
         if (fConnection != null) {
            fConnection = fConnection.attach( session );
         }
      }
      
      final static String selectCommand = "from Origin where ServerDate = ? and cid = ? ";
      
      public Origin attach( org.hibernate.Session session ) throws Exception
      {
         // If an identical origin already exist in the DB return it.
         // If not, persist this instance.
         
         AttachContent( session );

         if ( fOriginId != 0) {
            session.update( this );
            return this;
         }
         
         if (fConnection ==  null) {
            return this;
         }

         Origin indb = (Origin) session.createQuery(selectCommand).setTimestamp(0,fServerDate.getValue()).setLong(1,fConnection.getcid()).uniqueResult();
         
         if (indb != null) {            
            return indb;
         } else {
            session.saveOrUpdate(this);
            return this;
         }
      }

   }




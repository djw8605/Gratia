package net.sf.gratia.storage;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * <p>Title: Record </p>
 *
 * <p>Description: Base class of the Gratia Records</p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Chris Green
 * @version 1.0
 */
public abstract class Record implements RecordInterface {

   // Calculated information (not directly in the xml file)
   protected Probe Probe;
   protected List<Origin> Origins;

   // Meta Information (from the xml file)
   // See Record class
   protected RecordIdentity RecordIdentity;
   protected StringElement ProbeName;
   protected StringElement SiteName;
   protected StringElement Grid;
   
   // Meta Information (not part of the xml file per se).
   protected int RecordId;
   protected String RawXml;   // Complete Usage Record Xml
   protected String ExtraXml; // Xml fragment not used for any of the data members/field
   protected Date ServerDate;
   protected String md5;

   
   static ExpirationDateCalculator eCalc = new ExpirationDateCalculator();

   // Returns the date of the oldest raw records we keep
   public Date getExpirationDate() {
      return eCalc.expirationDate(new Date(), getTableName());
   }
   
   public void executeTrigger(org.hibernate.Session session) throws Exception {
      // NOP
   }
   
   public Probe getProbe() 
   { 
      return Probe; 
   }
   public void setProbe(Probe p) 
   {
      this.Probe = p; 
   }
   
   public void setProbeName(StringElement ProbeName)
   {
      this.ProbeName = ProbeName;
   }
   public StringElement getProbeName()
   {
      return ProbeName;
   }
   
   public void setGrid(StringElement Grid)
   {
      this.Grid = Grid;
   }
   public StringElement getGrid()
   {
      return Grid;
   }

   public void setSiteName(StringElement SiteName)
   {
      this.SiteName = SiteName;
   }   
   public StringElement getSiteName()
   {
      return SiteName;
   }
   
   public void setRecordIdentity(RecordIdentity n) 
   {
      RecordIdentity = n; 
   }
   public RecordIdentity getRecordIdentity()
   {
      return RecordIdentity;
   }
   
   public void setRecordId(int RecordId)
   {
      this.RecordId = RecordId;
   }
   public int getRecordId()
   {
      return RecordId;
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
  
   public String getmd5()
   {
      return md5;
   }   
   public void setmd5(String value)
   {
      md5 = value;
   }
   
   public List<Origin> getOrigins()
   {
      return Origins;
   }
   public void setOrigins(List<Origin> value)
   {
      Origins = value;
   }
   public void insertOrigin(Origin from, int hopNumber)
   {
      // Reinsert the origin into the slot number 'HopNumber' in the list of origins.
      
      if ( hopNumber == 0) {
         addOrigin(from);
      } else {
         if ( Origins == null) {
            Origins = new ArrayList<Origin>();
         }
         for( int i = Origins.size(); i < hopNumber; i++ ) {
            Origins.add(null);
         }
         Origins.set( hopNumber - 1, from );
      }
   }
   public void addOrigin(Origin from)
   {
      // Add the origin at the end of the list of origin
      // and update the origin's HopNumber.

      if ( Origins == null) {
         Origins = new ArrayList<Origin>();
      }
      Origins.add(from);
   }
   protected String originsToString() 
   {
      StringBuilder output = new StringBuilder("");
      int hop = 1;
      for( Origin from : Origins ) {
         output.append("Origin: \n");
         output.append(from.toString(hop));
         output.append("\n");
         hop++;
      }
      return output.toString();
   }
   protected void originsAsXml(StringBuilder output) 
   {
      int hop = 1;
      for( Origin from : Origins ) {
         from.asXml(output,hop);
         output.append("\n");
         hop++;
      }
   }
   public void AttachOrigins( org.hibernate.Session session ) throws Exception
   {
      if (Origins != null) {
         for(int i = 0; i < Origins.size(); i++) {
            Origin from = Origins.get( i );
            from = from.attach(session);
            Origins.set( i, from );
         }
      }
   }
   
   public Date getServerDate()
   {
      return ServerDate;
   }   
   public void setServerDate(Date value)
   {
      ServerDate = value;
   }
   
}

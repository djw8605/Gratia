package net.sf.gratia.storage;

import java.util.Date;

/**
 * <p>Title: Collector </p>
 *
 * <p>Description: Keeps track of the current status of the backlog on the local collector.</p>
 *
 * <p>Note: since this class represent counters, the updates are done directly in mysql, bypassing hibernate.</p>
 *
 * <p>Copyright: Copyright (c) 2010</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 */

public class CollectorStatus {
   private Date   fUpdateDate;
   private String fName;
   private long   fFiles;    // File in queue
   private long   fRecords;  // Record in queue

   public CollectorStatus() 
   {
      // Default Constructor
      fUpdateDate = null;
      fName = null;
      fFiles = 0;
      fRecords = 0;
   }

   public Date getUpdateDate()
   {
      return fUpdateDate;
   }   
   public void setUpdateDate(Date value)
   {
      fUpdateDate = value;
   }

   public void setName(String Name)
   {
      this.fName = Name;
   }
   public String getName()
   {
      return fName;
   }
   
   public void setFiles(long Files)
   {
      this.fFiles = Files;
   }
   public long getFiles()
   {
      return fFiles;
   }
   
   public void setRecords(long Records)
   {
      this.fRecords = Records;
   }
   public long getRecords()
   {
      return fRecords;
   }
   
}
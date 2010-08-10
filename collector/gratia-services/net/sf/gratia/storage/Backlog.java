package net.sf.gratia.storage;

import java.util.Date;

/**
 * <p>Title: Collector </p>
 *
 * <p>Description: Keeps track of the status of the backlog on any entity (probe,local collector,remote collecotr).</p>
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

public class Backlog extends CollectorStatus {
   private long   fBackId;          
   private String fType;            // Type of the entity (Probe,Collector,LocalCollector)
   private long   fServiceBacklog;  // Estimated number of records not yet processed in the underlying service.
   private Date   fServerDate;
   
   public Backlog() 
   {
      fServerDate = null;
   }
   
   public void setBackId(long id)
   {
      this.fBackId = id;
   }
   public long getBackId()
   {
      return fBackId;
   }
   
   public void setType(String Type)
   {
      this.fType = Type;
   }
   public String getType()
   {
      return fType;
   }

   public void setServiceBacklog(long records)
   {
      this.fServiceBacklog = records;
   }
   public long getServiceBacklog()
   {
      return fServiceBacklog;
   }

   public Date getServerDate()
   {
      return fServerDate;
   }   
   public void setServerDate(Date value)
   {
      fServerDate = value;
   }

}
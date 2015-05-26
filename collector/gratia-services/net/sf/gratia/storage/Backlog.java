package net.sf.gratia.storage;

import java.util.Date;
import java.util.List;
import java.math.BigInteger;
import java.util.Map;
import java.util.HashMap;

import org.hibernate.Session;
import org.hibernate.Query;

/**
 * <p>Title: Collector </p>
 *
 * <p>Description: Keeps track of the status of the backlog on any entity (probe,local collector,remote collector).</p>
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

public class Backlog  
{
   private Date   fServerDate;          
   private String fEntityType;         // Type of the entity (Probe,Collector,LocalCollector)
   private String fName;
   private long   fFiles;              // Files in queue
   private long   fRecords;            // Record in queue
   private long   fTarFiles;
   private long   fServiceBacklog;     // Estimated number of records not yet processed in the underlying service.
   private long   fMaxPendingFiles;    // Maximum number of files in 'queue' before it is compressed or incoming record are rejected.
   private long   fBundleSize;
   private Date   fPrevServerDate;          
   private long   fPrevRecords;        // Previous number of records in queue
   private long   fPrevServiceBacklog; // Previous estimated number of records not yet processed in the underlying service.
   
   public Backlog() 
   {
   }
   
   public void setServerDate(Date Value) {
      this.fServerDate = Value;
   }
   
   public Date getServerDate() {
      return fServerDate;
   }

   public void setPrevServerDate(Date Value) {
      this.fPrevServerDate = Value;
   }
   
   public Date getPrevServerDate() {
      return fPrevServerDate;
   }
   
   public void setEntityType(String Type)
   {
      this.fEntityType = Type;
   }
   public String getEntityType()
   {
      return fEntityType;
   }

   public void setName(String Name)
   {
      this.fName = Name;
   }
   public String getName()
   {
      return fName;
   }

   public void setFiles(long value)
   {
      this.fFiles = value;
   }
   public long getFiles()
   {
      return fFiles;
   }
   
   public void setRecords(long value)
   {
      this.fRecords = value;
   }
   public long getRecords()
   {
      return fRecords;
   }

   public void setTarFiles(long value)
   {
      this.fTarFiles = value;
   }
   public long getTarFiles()
   {
      return fTarFiles;
   }
   
   public void setMaxPendingFiles(long value)
   {
      this.fMaxPendingFiles = value;
   }
   public long getMaxPendingFiles()
   {
      return fMaxPendingFiles;
   }
   
   public void setBundleSize(long value)
   {
      this.fBundleSize = value;
   }
   public long getBundleSize()
   {
      return fBundleSize;
   }
   
   public void setPrevRecords(long value)
   {
      this.fPrevRecords = value;
   }
   public long getPrevRecords()
   {
      return fPrevRecords;
   }
   
   public void setServiceBacklog(long records)
   {
      this.fServiceBacklog = records;
   }
   public long getServiceBacklog()
   {
      return fServiceBacklog;
   }

   public void setPrevServiceBacklog(long records)
   {
      this.fPrevServiceBacklog = records;
   }
   public long getPrevServiceBacklog()
   {
      return fPrevServiceBacklog;
   }
   
   public static final String fgCreateTable = "CREATE TABLE BacklogStatistics(" +
   "ServerDate DATETIME NOT NULL, " +
   "EntityType VARCHAR(255) NOT NULL, " +
   "Name VARCHAR(255) NOT NULL, " +
   "nRecords BIGINT DEFAULT 0, " +
   "xmlFiles BIGINT DEFAULT 0, " +
   "tarFiles BIGINT DEFAULT 0, " +
   "serviceBacklog BIGINT DEFAULT 0, " +
   "maxPendingFiles BIGINT DEFAULT 0, " +
   "bundleSize BIGINT DEFAULT 0, " +
   "prevServerDate DATETIME, " +
   "prevRecords BIGINT DEFAULT 0, " +
   "prevServiceBacklog BIGINT default 0, " + 
   "UNIQUE KEY index1 (EntityType, Name))";
   private static final String fgSelectCommand = "select ServerDate,EntityType,Name,nRecords,xmlFiles,tarFiles,serviceBacklog,maxPendingFiles,bundleSize,prevServerDate,prevRecords,prevServiceBacklog from BacklogStatistics";

   public static List<Backlog> getList(Session session, String selection, Map<String,Object> parameters)
   {
      String command = fgSelectCommand + selection;
      Query rq = session.createSQLQuery(command);
      for (Map.Entry<String, Object> e : parameters.entrySet())
          rq.setParameter(e.getKey(), e.getValue());
      List<Backlog> result = new java.util.LinkedList<Backlog>();
      List<Object[]> data = rq.list();
      for ( Object[] values : data ) {
         Backlog backlog = new Backlog();
         backlog.setServerDate( (Date) values[0] );
         backlog.setEntityType( (String) values[1] );
         backlog.setName( (String) values[2] );
         backlog.setRecords( ((BigInteger)values[3]).longValue() );
         backlog.setFiles( ((BigInteger)values[4]).longValue() );
         backlog.setTarFiles( ((BigInteger)values[5]).longValue() );
         backlog.setServiceBacklog( ((BigInteger)values[6]).longValue() );
         backlog.setMaxPendingFiles( ((BigInteger)values[7]).longValue() );
         backlog.setBundleSize( ((BigInteger)values[8]).longValue() );

         backlog.setPrevServerDate( (Date) values[9] );
         backlog.setPrevRecords( ((BigInteger)values[10]).longValue() );
         backlog.setPrevServiceBacklog( ((BigInteger)values[11]).longValue() );
         result.add(backlog);
      }
      return result;
   }
   public static List<Backlog> getList(Session session, String selection)
    {
        Map<String,Object> emptyMap = new HashMap<String,Object>();
        return getList(session, selection, emptyMap);
    }
   
}

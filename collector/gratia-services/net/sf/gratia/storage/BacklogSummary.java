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

public class BacklogSummary  
{
   private long   fBshid;
   private Date   fServerDate;          
   private Date   fEventDate;          
   private String fEntityType;         // Type of the entity (Probe,Collector,LocalCollector)
   private String fName;
   private Date   fStartTime;
   private Date   fEndTime;
   private long   fAvgRecords;            // Record in queue
   private long   fMaxRecords;            // Record in queue
   private long   fMinRecords;            // Record in queue
   private long   fAvgFiles;              // Files in queue
   private long   fMaxFiles;              // Files in queue
   private long   fMinFiles;              // Files in queue
   private long   fAvgTarFiles;
   private long   fMaxTarFiles;
   private long   fMinTarFiles;
   private long   fAvgServiceBacklog;     // Estimated number of records not yet processed in the underlying service.
   private long   fMaxServiceBacklog;     // Estimated number of records not yet processed in the underlying service.
   private long   fMinServiceBacklog;     // Estimated number of records not yet processed in the underlying service.
   private long   fAvgMaxPendingFiles;    // Maximum number of files in 'queue' before it is compressed or incoming record are rejected.
   private long   fAvgBundleSize;
   
   public BacklogSummary() 
   {
   }
   
   public long getBshid() {
      return fBshid;
   }

   public void setServerDate(Date Value) {
      this.fServerDate = Value;
   }
   public Date getServerDate() {
      return fServerDate;
   }
   

   public void setEventDate(Date Value) {
      this.fEventDate = Value;
   }
   public Date getEventDate() {
      return fEventDate;
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

   public void setStartTime(Date Value) {
      this.fStartTime = Value;
   }
   public Date getStartTime() {
      return fStartTime;
   }
   
   public void setEndTime(Date Value) {
      this.fEndTime = Value;
   }
   public Date getEndTime() {
      return fEndTime;
   }
   
   public void setAvgFiles(long value)
   {
      this.fAvgFiles = value;
   }
   public long getAvgFiles()
   {
      return fAvgFiles;
   }
   
   public void setMinFiles(long value)
   {
      this.fMinFiles = value;
   }
   public long getMinFiles()
   {
      return fMinFiles;
   }
   
   public void setMaxFiles(long value)
   {
      this.fMaxFiles = value;
   }
   public long getMaxFiles()
   {
      return fMaxFiles;
   }
   
   public void setAvgRecords(long value)
   {
      this.fAvgRecords = value;
   }
   public long getAvgRecords()
   {
      return fAvgRecords;
   }

   public void setMaxRecords(long value)
   {
      this.fMaxRecords = value;
   }
   public long getMaxRecords()
   {
      return fMaxRecords;
   }

   public void setMinRecords(long value)
   {
      this.fMinRecords = value;
   }
   public long getMinRecords()
   {
      return fMinRecords;
   }

   public void setAvgTarFiles(long value)
   {
      this.fAvgTarFiles = value;
   }
   public long getAvgTarFiles()
   {
      return fAvgTarFiles;
   }
   
   public void setMaxTarFiles(long value)
   {
      this.fMaxTarFiles = value;
   }
   public long getMaxTarFiles()
   {
      return fMaxTarFiles;
   }

   public void setMinTarFiles(long value)
   {
      this.fMinTarFiles = value;
   }
   public long getMinTarFiles()
   {
      return fMinTarFiles;
   }

   public void setAvgServiceBacklog(long records)
   {
      this.fAvgServiceBacklog = records;
   }
   public long getAvgServiceBacklog()
   {
      return fAvgServiceBacklog;
   }
   
   public void setMaxServiceBacklog(long records)
   {
      this.fMaxServiceBacklog = records;
   }
   public long getMaxServiceBacklog()
   {
      return fMaxServiceBacklog;
   }
   
   public void setMinServiceBacklog(long records)
   {
      this.fMinServiceBacklog = records;
   }
   public long getMinServiceBacklog()
   {
      return fMinServiceBacklog;
   }
   
   
   public void setAvgMaxPendingFiles(long value)
   {
      this.fAvgMaxPendingFiles = value;
   }
   public long getAvgMaxPendingFiles()
   {
      return fAvgMaxPendingFiles;
   }
   
   public void setAvgBundleSize(long value)
   {
      this.fAvgBundleSize = value;
   }
   public long getAvgBundleSize()
   {
      return fAvgBundleSize;
   }

   private static final String fgCreateColumns = "ServerDate DATETIME NOT NULL, " + 
   "EventDate DATETIME NOT NULL, " + 
   "EntityType VARCHAR(255) NOT NULL, " +
   "Name VARCHAR(255) NOT NULL, " +
   "StartTime DATETIME NOT NULL, " + 
   "EndTime DATETIME NOT NULL, " + 
   "avgRecords BIGINT DEFAULT 0, " +
   "maxRecords BIGINT DEFAULT 0, " +
   "minRecords BIGINT DEFAULT 0, " +
   "avgXmlFiles BIGINT DEFAULT 0, " +
   "maxXmlFiles BIGINT DEFAULT 0, " +
   "minXmlFiles BIGINT DEFAULT 0, " +
   "avgTarFiles BIGINT DEFAULT 0, " +
   "maxTarFiles BIGINT DEFAULT 0, " +
   "minTarFiles BIGINT DEFAULT 0, " +
   "avgServiceBacklog BIGINT DEFAULT 0, " +
   "maxServiceBacklog BIGINT DEFAULT 0, " +
   "minServiceBacklog BIGINT DEFAULT 0, " +
   "avgMaxPendingFiles BIGINT DEFAULT 0, " +
   "avgBundleSize BIGINT DEFAULT 0, " +
   "UNIQUE KEY index1 (EventDate, EntityType, Name))";
   
   public static final String fgCreateHourlyTable = "CREATE TABLE BacklogStatisticsHourly(" + "bshid BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " + fgCreateColumns;
   public static final String fgCreateDailyTable = "CREATE TABLE BacklogStatisticsDaily(" + "bsdid BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +fgCreateColumns;
   
   private static final String fgSelectCommand = " ServerDate,EventDate,EntityType,Name,if(StartTime=0,FROM_UNIXTIME(0),StartTime),EndTime,"+
   " ifnull(avgRecords,0),ifnull(maxRecords,0),ifnull(minRecords,0),ifnull(avgXmlFiles,0),ifnull(maxXmlFiles,0),ifnull(minXmlFiles,0), " + 
   " ifnull(avgTarFiles,0),ifnull(maxTarFiles,0),ifnull(minTarFiles,0),ifnull(avgServiceBacklog,0),ifnull(maxServiceBacklog,0),ifnull(minServiceBacklog,0),ifnull(avgMaxPendingFiles,0),ifnull(avgBundleSize,0) " +
   " from BacklogStatistics";
   
   private static final String fgSelectHourlyCommand = "select bshid, " + fgSelectCommand + "Hourly";
   private static final String fgSelectDailyCommand = "select bsdid, " + fgSelectCommand + "Daily";

   public static List<BacklogSummary> getList(Session session, String what, String selection, Map<String,Object> parameters)
   {
      String command;
      if (what.equals("Hourly")) {
         command = fgSelectHourlyCommand + " " + selection;
      } else {
         command = fgSelectDailyCommand + " " + selection;
      }
      Query rq = session.createSQLQuery(command);
      for (Map.Entry<String, Object> e : parameters.entrySet())
          rq.setParameter(e.getKey(), e.getValue());
      List<BacklogSummary> result = new java.util.LinkedList<BacklogSummary>();
      List<Object[]> data = rq.list();
      for ( Object[] values : data ) {
         BacklogSummary backlog = new BacklogSummary();
         backlog.fBshid = ((BigInteger)values[0]).longValue();
         backlog.setServerDate( (Date) values[1] );
         backlog.setEventDate( (Date) values[2] );
         backlog.setEntityType( (String) values[3] );
         backlog.setName( (String) values[4] );
         backlog.setStartTime( (Date) values[5] );
         backlog.setEndTime( (Date) values[6] );
         backlog.setAvgRecords( ((BigInteger)values[7]).longValue() );
         backlog.setMaxRecords( ((BigInteger)values[8]).longValue() );
         backlog.setMinRecords( ((BigInteger)values[9]).longValue() );
         backlog.setAvgFiles( ((BigInteger)values[10]).longValue() );
         backlog.setMaxFiles( ((BigInteger)values[11]).longValue() );
         backlog.setMinFiles( ((BigInteger)values[12]).longValue() );
         backlog.setAvgTarFiles( ((BigInteger)values[13]).longValue() );
         backlog.setMaxTarFiles( ((BigInteger)values[14]).longValue() );
         backlog.setMinTarFiles( ((BigInteger)values[15]).longValue() );
         backlog.setAvgServiceBacklog( ((BigInteger)values[16]).longValue() );
         backlog.setMaxServiceBacklog( ((BigInteger)values[17]).longValue() );
         backlog.setMinServiceBacklog( ((BigInteger)values[18]).longValue() );
         backlog.setAvgMaxPendingFiles( ((BigInteger)values[19]).longValue() );
         backlog.setAvgBundleSize( ((BigInteger)values[20]).longValue() );

         result.add(backlog);
      }
      return result;
   }

   public static List<BacklogSummary> getList(Session session, String what, String selection)
   {
       Map<String,Object> emptyMap = new HashMap<String,Object>();
       return getList(session, what, selection, emptyMap);
   }
   
}

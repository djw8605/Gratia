DELIMITER ||

DROP PROCEDURE IF EXISTS backlog_statictics_hourly_summary
||
CREATE PROCEDURE backlog_statictics_hourly_summary(inputFrom DateTime, inputUpto DateTime)
SQL SECURITY INVOKER
DETERMINISTIC
TSHS:BEGIN
  -- Main

set @inputFromShifted = inputFrom - interval 1 hour;
set @nowDate = UTC_TIMESTAMP();

insert into BacklogStatisticsHourly
(
   ServerDate,
   EventDate,
   EntityType,
   Name,
   EndTime,
   StartTime,
   avgRecords,
   maxRecords,
   minRecords,
   avgXmlFiles,
   maxXmlFiles,
   minXmlFiles,
   avgTarFiles,
   maxTarFiles,
   minTarFiles,
   avgServiceBacklog,
   maxServiceBacklog,
   minServiceBacklog,
   avgMaxPendingFiles,
   avgBundleSize
)
select
@nowDate,
R.EventHour,
R.EntityType,
R.Name,
-- count(*),
max(R.ServerDate) as end,
min(L.ServerDate) as start,
-- sum(TIME_TO_SEC(timediff(R.ServerTime ,L.ServerTime))) as span,
sum( (R.nRecords + L.nRecords) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) 
   / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavg,
max(R.nRecords) as maxRecords,
min(L.nRecords) as minRecords,
sum( (R.xmlFiles + L.xmlFiles) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) 
   / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavgxmlFiles,
max(R.xmlFiles) as maxXmlFiles,
min(L.xmlFiles) as minXmlFiles,
sum( (R.tarFiles + L.tarFiles) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) 
   / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavgtarFiles,
max(R.tarFiles) as maxTarFiles,
min(L.tarFiles) as minTarFiles,
sum( (R.serviceBacklog + L.serviceBacklog) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) 
   / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavgServiceBacklog,
max(R.serviceBacklog) as maxServiceBacklog,
min(L.serviceBacklog) as minServiceBacklog,
sum( (R.maxPendingFiles + L.maxPendingFiles) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) 
   / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavgmaxPendingFiles,
sum( (R.bundleSize + L.bundleSize) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) 
   / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavgBundleSize
from
(
   select
   @serial := @serial+1 as serial,
   EntityType,
   Name,
   ServerDate,
   timestamp(ServerDate) as ServerTime,
   nRecords, xmlFiles, tarFiles, serviceBacklog, maxPendingFiles, bundleSize,
   timestamp(EventHour) as EventTime,
   addtime(timestamp(EventHour),10000) as EventTimePlusOne,
   EventHour
   from
   (
      select
      @serial := 0,
      EntityType,
      Name,
      ServerDate,
      nRecords,
      xmlFiles,
      tarFiles,
      serviceBacklog,
      maxPendingFiles,
      bundleSize,
      date_format(ServerDate,"%Y/%m/%d:%H:00:00") as EventHour
      from BacklogStatisticsSnapshots
      where ServerDate > @inputFromShifted
   )
   I1
   order by EntityType, Name, ServerDate
)
R
left join
(
   select
   @serial1 := @serial1+1 as serial,
   EntityType,
   Name,
   ServerDate,
   timestamp(ServerDate) as ServerTime,
   nRecords, xmlFiles, tarFiles, serviceBacklog, maxPendingFiles, bundleSize
   from
   (
      select
      @serial1 := 0, EntityType, Name, ServerDate, nRecords, xmlFiles, tarFiles, serviceBacklog, maxPendingFiles, bundleSize
      from BacklogStatisticsSnapshots
      where ServerDate > @inputFromShifted
   )
   I1
   order by EntityType, Name, ServerDate
)
L on R.serial = L.serial+1
     and L.EntityType = R.EntityType
     and L.Name = R.Name
where R.EventHour < inputUpto
      and R.EventHour > inputFrom
group by R.EntityType, R.Name, R.EventHour;

END;
||
DROP PROCEDURE IF EXISTS backlog_statictics_daily_summary
||
CREATE PROCEDURE backlog_statictics_daily_summary(inputFrom DateTime, inputUpto DateTime)
SQL SECURITY INVOKER
DETERMINISTIC
TSDS:BEGIN
  -- Main

set @inputFromShifted = inputFrom - interval 1 day;
set @nowDate = UTC_TIMESTAMP();

insert into BacklogStatisticsDaily
(
   ServerDate,
   EventDate,
   EntityType,
   Name,
   EndTime,
   StartTime,
   avgRecords,
   maxRecords,
   minRecords,
   avgXmlFiles,
   maxXmlFiles,
   minXmlFiles,
   avgTarFiles,
   maxTarFiles,
   minTarFiles,
   avgServiceBacklog,
   maxServiceBacklog,
   minServiceBacklog,
   avgMaxPendingFiles,
   avgBundleSize
)
select
@nowDate,
R.EventDay,
R.EntityType,
R.Name,
-- count(*),
max(R.ServerDate) as end,
min(L.ServerDate) as start,
-- sum(TIME_TO_SEC(timediff(R.ServerTime ,L.ServerTime))) as span,
sum( (R.avgRecords + L.avgRecords) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavgRecords,
max(R.maxRecords) as maxRecords,
min(L.minRecords) as minRecords,
sum( (R.avgXmlFiles + L.avgXmlFiles) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavgXmlFiles,
max(R.maxXmlFiles) as maxXmlFiles,
min(L.minXmlFiles) as minXmlFiles,
sum( (R.avgTarFiles + L.avgTarFiles) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavgTarFiles,
max(R.maxTarFiles) as maxTarFiles,
min(L.minTarFiles) as minTarFiles,
sum( (R.avgServiceBacklog + L.avgServiceBacklog) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavgServiceBacklog,
max(R.maxServiceBacklog) as maxServiceBacklog,
min(L.minServiceBacklog) as minServiceBacklog,
sum( (R.avgMaxPendingFiles + L.avgMaxPendingFiles) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavgMaxPendingFiles,
sum( (R.avgBundleSize + L.avgBundleSize) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavgBundleSize
from
(
   select
   @serial := @serial+1 as serial,
   EntityType,
   Name,
   ServerDate,
   timestamp(ServerDate) as ServerTime,
   avgRecords,
   maxRecords,
   minRecords,
   avgXmlFiles,
   maxXmlFiles,
   minXmlFiles,
   avgTarFiles,
   maxTarFiles,
   minTarFiles,
   avgServiceBacklog,
   maxServiceBacklog,
   minServiceBacklog,
   avgMaxPendingFiles,
   avgBundleSize,
   timestamp(EventDay) as EventTime,
   addtime(timestamp(EventDay),10000) as EventTimePlusOne,
   EventDay
   from
   (
      select
      @serial := 0,
      EntityType,
      Name,
      ServerDate,
      avgRecords,
      maxRecords,
      minRecords,
      avgXmlFiles,
      maxXmlFiles,
      minXmlFiles,
      avgTarFiles,
      maxTarFiles,
      minTarFiles,
      avgServiceBacklog,
      maxServiceBacklog,
      minServiceBacklog,
      avgMaxPendingFiles,
      avgBundleSize,
      date_format(ServerDate,"%Y/%m/%d:00:00:00") as EventDay
      from BacklogStatisticsHourly
      where ServerDate > @inputFromShifted
   )
   I1
   order by EntityType, Name, ServerDate
)
R
left join
(
   select
   @serial1 := @serial1+1 as serial,
   EntityType,
   Name,
   ServerDate,
   timestamp(ServerDate) as ServerTime,
   avgRecords,
   maxRecords,
   minRecords,
   avgXmlFiles,
   maxXmlFiles,
   minXmlFiles,
   avgTarFiles,
   maxTarFiles,
   minTarFiles,
   avgServiceBacklog,
   maxServiceBacklog,
   minServiceBacklog,
   avgMaxPendingFiles,
   avgBundleSize
   from
   (
      select
      @serial1 := 0, EntityType, Name, ServerDate, avgRecords, maxRecords, minRecords, avgXmlFiles, maxXmlFiles, minXmlFiles, avgTarFiles, maxTarFiles, minTarFiles, avgServiceBacklog, maxServiceBacklog, minServiceBacklog,avgMaxPendingFiles, avgBundleSize
      from BacklogStatisticsHourly
      where ServerDate > @inputFromShifted
   )
   I1
   order by EntityType, Name, ServerDate
)
L on R.serial = L.serial+1
  and L.EntityType = R.EntityType
  and L.Name = R.Name
where R.EventDay < inputUpto
  and R.EventDay > inputFrom
group by R.EntityType, R.Name, R.EventDay;

END;
||

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

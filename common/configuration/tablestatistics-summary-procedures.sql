DELIMITER ||

DROP PROCEDURE IF EXISTS table_statictics_hourly_summary
||
CREATE PROCEDURE table_statictics_hourly_summary(inputFrom DateTime, inputUpto DateTime)
SQL SECURITY INVOKER
DETERMINISTIC
TSHS:BEGIN
  -- Main

set @inputFromShifted = inputFrom - interval 1 hour;
set @nowDate = UTC_TIMESTAMP();

insert into TableStatisticsHourly
(
   ServerDate,
   EventDate,
   ValueType,
   RecordType,
   Qualifier,
   EndTime,
   StartTime,
   avgRecords,
   maxRecords,
   minRecords
)
select
@nowDate,
R.EventHour,
R.ValueType,
R.RecordType,
R.Qualifier,
-- count(*),
max(R.ServerDate) as end,
min(L.ServerDate) as start,
-- sum(TIME_TO_SEC(timediff(R.ServerTime ,L.ServerTime))) as span,
sum( (R.nRecords + L.nRecords) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) 
   / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavg,
max(R.nRecords) as maxRecords,
min(L.nRecords) as minRecords
from
(
   select
   @serial := @serial+1 as serial,
   ValueType,
   RecordType,
   Qualifier,
   ServerDate,
   timestamp(ServerDate) as ServerTime,
   nRecords,
   timestamp(EventHour) as EventTime,
   addtime(timestamp(EventHour),10000) as EventTimePlusOne,
   EventHour
   from
   (
      select
      @serial := 0,
      ValueType,
      RecordType,
      Qualifier,
      ServerDate,
      nRecords,
      date_format(ServerDate,"%Y/%m/%d:%H:00:00") as EventHour
      from TableStatisticsSnapshots
      where ServerDate > @inputFromShifted
   )
   I1
   order by ValueType, RecordType, Qualifier,ServerDate
)
R
left join
(
   select
   @serial1 := @serial1+1 as serial,
   ValueType,
   RecordType,
   Qualifier,
   ServerDate,
   timestamp(ServerDate) as ServerTime,
   nRecords
   from
   (
      select
      @serial1 := 0,ValueType,RecordType, Qualifier,ServerDate, nRecords
      from TableStatisticsSnapshots
      where ServerDate > @inputFromShifted
   )
   I1
   order by ValueType, RecordType, Qualifier,ServerDate
)
L on R.serial = L.serial+1
     and L.ValueType = R.ValueType
     and L.RecordType = R.RecordType
     and L.Qualifier = R.Qualifier
where R.EventHour < inputUpto
      and R.EventHour > inputFrom
group by R.ValueType, R.RecordType, R.Qualifier, R.EventHour;

END;
||
DROP PROCEDURE IF EXISTS table_statictics_daily_summary
||
CREATE PROCEDURE table_statictics_daily_summary(inputFrom DateTime, inputUpto DateTime)
SQL SECURITY INVOKER
DETERMINISTIC
TSDS:BEGIN
  -- Main

set @inputFromShifted = inputFrom - interval 1 day;
set @nowDate = UTC_TIMESTAMP();

insert into TableStatisticsDaily
(
   ServerDate,
   EventDate,
   ValueType,
   RecordType,
   Qualifier,
   EndTime,
   StartTime,
   avgRecords,
   maxRecords,
   minRecords
)
select
@nowDate,
R.EventDay,
R.ValueType,
R.RecordType,
R.Qualifier,
-- count(*),
max(R.ServerDate) as end,
min(L.ServerDate) as start,
-- sum(TIME_TO_SEC(timediff(R.ServerTime ,L.ServerTime))) as span,
sum( (R.avgRecords + L.avgRecords) * (least(R.ServerTime,R.EventTimePlusOne) - greatest(L.ServerTime,R.EventTime) ) ) / (2 *( max(least(R.ServerTime,R.EventTimePlusOne)) - min(greatest(L.Servertime,R.EventTime) ) ) ) as myavg,
max(R.maxRecords) as maxRecords,
min(L.minRecords) as minRecords
from
(
   select
   @serial := @serial+1 as serial,
   ValueType,
   RecordType,
   Qualifier,
   ServerDate,
   timestamp(ServerDate) as ServerTime,
   avgRecords,
   maxRecords,
   minRecords,
   timestamp(EventDay) as EventTime,
   addtime(timestamp(EventDay),10000) as EventTimePlusOne,
   EventDay
   from
   (
      select
      @serial := 0,
      ValueType,
      RecordType,
      Qualifier,
      ServerDate,
      avgRecords,
      maxRecords,
      minRecords,
      date_format(ServerDate,"%Y/%m/%d:00:00:00") as EventDay
      from TableStatisticsHourly
      where ServerDate > @inputFromShifted
   )
   I1
   order by ValueType, RecordType, Qualifier,ServerDate
)
R
left join
(
   select
   @serial1 := @serial1+1 as serial,
   ValueType,
   RecordType,
   Qualifier,
   ServerDate,
   timestamp(ServerDate) as ServerTime,
   avgRecords,
   maxRecords,
   minRecords
   from
   (
      select
      @serial1 := 0, ValueType, RecordType, Qualifier,ServerDate, avgRecords, maxRecords, minRecords
      from TableStatisticsHourly
      where ServerDate > @inputFromShifted
   )
   I1
   order by ValueType, RecordType, Qualifier,ServerDate
)
L on R.serial = L.serial+1
  and L.ValueType = R.ValueType
  and L.RecordType = R.RecordType
  and L.Qualifier = R.Qualifier
where R.EventDay < inputUpto
  and R.EventDay > inputFrom
group by R.ValueType, R.RecordType, R.Qualifier, R.EventDay;

END;
||

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

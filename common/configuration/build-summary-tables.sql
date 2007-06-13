delimiter ||

drop table if exists ProbeSummary, UserProbeSummary, VOProbeSummary,
	ProbeStatus, HostDescriptionProbeSummary;
||
CREATE TABLE `ProbeStatus` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`Njobs` INTEGER NOT NULL DEFAULT 0
);
||
CREATE TABLE `ProbeSummary` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`SiteName` VARCHAR(255) DEFAULT 'Unknown',
	`ResourceType` VARCHAR(255) DEFAULT 'Unknown',
	`Njobs` INTEGER NOT NULL DEFAULT 0,
	`WallDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);
||
CREATE TABLE `UserProbeSummary` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`CommonName` VARCHAR(255) DEFAULT 'Unknown',
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`ResourceType` VARCHAR(255) DEFAULT 'Unknown',
	`Njobs` INTEGER NOT NULL DEFAULT 0,
	`WallDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);
||
CREATE TABLE `VOProbeSummary` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`VOName` VARCHAR(255) DEFAULT 'Unknown',
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`CommonName` VARCHAR(255) DEFAULT 'Unknown',
	`ResourceType` VARCHAR(255) DEFAULT 'Unknown',
	`Njobs` INTEGER NOT NULL DEFAULT 0,
	`WallDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);
||
CREATE TABLE `HostDescriptionProbeSummary` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`HostDescription` VARCHAR(255) DEFAULT 'Unknown',
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`ResourceType` VARCHAR(255) DEFAULT 'Unknown',
	`Njobs` INTEGER NOT NULL DEFAULT 0,
	`WallDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);
||
insert into ProbeStatus
	(select
		str_to_date(date_format(ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00') as EndTime,
		ProbeName,
		count(*) as Njobs
		from JobUsageRecord_Meta
		group by ProbeName,str_to_date(date_format(ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00')
);
||
alter table ProbeStatus
	add index index01(EndTime);
||
alter table ProbeStatus
	add index index02(ProbeName);
||
insert into ProbeSummary
	(select
		date(EndTime) as EndTime,
		ProbeName,
		SiteName,
		ResourceType,
		sum(Njobs) as Njobs,
		sum(WallDuration) as WallDuration,
		sum(CpuUserDuration) as CpuUserDuration,
		sum(CpuSystemDuration) as CpuSystemDuration
		from JobUsageRecord, JobUsageRecord_Meta
		where CpuUserDuration is not null
      and JobUsageRecord.dbid = JobUsageRecord_Meta.dbid
		group by ProbeName,ResourceType,date(EndTime)
);
||
alter table ProbeSummary
	add index index01(EndTime);
||
alter table ProbeSummary
	add index index02(ProbeName);
||
alter table ProbeSummary
	add index index03(ResourceType);
||
insert into UserProbeSummary
	(select
		date(EndTime) as EndTime,
		CommonName,
		ProbeName,
		ResourceType,
		sum(Njobs) as Njobs,
		sum(WallDuration) as WallDuration,
		sum(CpuUserDuration) as CpuUserDuration,
		sum(CpuSystemDuration) as CpuSystemDuration
		from JobUsageRecord, JobUsageRecord_Meta
		where CpuUserDuration is not null
      and JobUsageRecord.dbid = JobUsageRecord_Meta.dbid
		group by CommonName,ProbeName,ResourceType,date(EndTime)
);
||
alter table UserProbeSummary
	add index index01(EndTime);
||
alter table UserProbeSummary
	add index index02(CommonName);
||
alter table UserProbeSummary
	add index index03(ProbeName);
||
alter table UserProbeSummary
	add index index04(ResourceType);
||
insert into VOProbeSummary
	(select
		date(EndTime) as EndTime,
		VOName,
		ProbeName,
		CommonName,
		ResourceType,
		sum(Njobs) as Njobs,
		sum(WallDuration) as WallDuration,
		sum(CpuUserDuration) as CpuUserDuration,
		sum(CpuSystemDuration) as CpuSystemDuration
		from JobUsageRecord, JobUsageRecord_Meta
		where CpuUserDuration is not null
      and JobUsageRecord.dbid = JobUsageRecord_Meta.dbid
		group by VOName,ProbeName,CommonName,ResourceType,date(EndTime)
);
||
alter table VOProbeSummary
	add index index01(EndTime);
||
alter table VOProbeSummary
	add index index02(VOName);
||
alter table VOProbeSummary
	add index index03(ProbeName);
||
alter table VOProbeSummary
	add index index04(ResourceType);
||
alter table VOProbeSummary
	add index index05(CommonName);
||
insert into HostDescriptionProbeSummary
	(select
		date(EndTime) as EndTime,
		HostDescription,
		ProbeName,
		ResourceType,
		sum(Njobs) as Njobs,
		sum(WallDuration) as WallDuration,
		sum(CpuUserDuration) as CpuUserDuration,
		sum(CpuSystemDuration) as CpuSystemDuration
		from JobUsageRecord, JobUsageRecord_Meta
		where CpuUserDuration is not null
      and JobUsageRecord.dbid = JobUsageRecord_Meta.dbid
		group by HostDescription,ProbeName,ResourceType,date(EndTime)
);
||
alter table HostDescriptionProbeSummary
	add index index01(EndTime);
||
alter table HostDescriptionProbeSummary
	add index index02(HostDescription);
||
alter table HostDescriptionProbeSummary
	add index index03(ProbeName);
||
alter table HostDescriptionProbeSummary
	add index index04(ResourceType);
||
-- show triggers;
-- ||

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

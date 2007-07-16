drop table if exists ProbeSummary, UserProbeSummary, VOProbeSummary, VOProbeSummaryData,
	ProbeStatus, HostDescriptionProbeSummary;

drop view if exists VOProbeSummary;

CREATE TABLE `ProbeStatus` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`Njobs` INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE `ProbeSummary` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`ReportedSiteName` VARCHAR(255) DEFAULT 'Unknown',
	`ResourceType` VARCHAR(255) DEFAULT 'Unknown',
	`Njobs` INTEGER NOT NULL DEFAULT 0,
	`WallDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);

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

CREATE TABLE `VOProbeSummaryData` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`VOcorrid` INT NOT NULL DEFAULT 0,
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`CommonName` VARCHAR(255) DEFAULT 'Unknown',
	`ResourceType` VARCHAR(255) DEFAULT 'Unknown',
	`Njobs` INTEGER NOT NULL DEFAULT 0,
	`WallDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);

CREATE VIEW `VOProbeSummary` AS
  SELECT VPSD.EndTime AS EndTime,
         VO.VOName AS VOName,
         VPSD.ProbeName AS ProbeName,
         VPSD.CommonName AS CommonName,
         VPSD.ResourceType AS ResourceType,
         SUM(VPSD.Njobs) AS Njobs,
         SUM(VPSD.WallDuration) AS WallDuration,
         SUM(VPSD.CpuUserDuration) AS CpuUserDuration,
         SUM(VPSD.CpuSystemDuration) AS CpuSystemDuration
  FROM VOProbeSummaryData VPSD, VO, VONameCorrection VC
  WHERE VPSD.VOcorrid = VC.corrid
    AND VC.VOid = VO.VOid
  GROUP BY EndTime, VOName, ProbeName, CommonName, ResourceType;

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

insert into ProbeStatus
	(select
		str_to_date(date_format(ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00') as EndTime,
		ProbeName,
		count(*) as Njobs
		from JobUsageRecord_Meta
		group by ProbeName,str_to_date(date_format(ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00')
);

alter table ProbeStatus
	add index index01(EndTime);

alter table ProbeStatus
	add index index02(ProbeName);

insert into ProbeSummary
	(select
		date(EndTime) as EndTime,
		ProbeName,
		ReportedSiteName,
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

alter table ProbeSummary
	add index index01(EndTime);

alter table ProbeSummary
	add index index02(ProbeName);

alter table ProbeSummary
	add index index03(ResourceType);

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

alter table UserProbeSummary
	add index index01(EndTime);

alter table UserProbeSummary
	add index index02(CommonName);

alter table UserProbeSummary
	add index index03(ProbeName);

alter table UserProbeSummary
	add index index04(ResourceType);

INSERT INTO VOProbeSummaryData
	(SELECT
		DATE(EndTime) AS EndTime,
		VC.corrid AS VOcorrid,
		ProbeName,
		CommonName,
		ResourceType,
		SUM(Njobs) AS Njobs,
		SUM(WallDuration) AS WallDuration,
		SUM(CpuUserDuration) AS CpuUserDuration,
		SUM(CpuSystemDuration) AS CpuSystemDuration
		FROM JobUsageRecord J, JobUsageRecord_Meta M, VONameCorrection VC
		WHERE J.CpuUserDuration IS NOT NULL
      AND J.dbid = M.dbid
      AND J.VOName = binary VC.VOName
      AND ((binary J.ReportableVOName = binary VC.ReportableVOName)
           OR ((J.ReportableVOName IS NULL) AND (VC.ReportableVOName IS NULL)))
		GROUP BY VOcorrid, ProbeName, CommonName, ResourceType, DATE(EndTime)
);

alter table VOProbeSummaryData
	add index index01(EndTime);

alter table VOProbeSummaryData
	add index index02(VOcorrid);

alter table VOProbeSummaryData
	add index index03(ProbeName);

alter table VOProbeSummaryData
	add index index04(ResourceType);

alter table VOProbeSummaryData
	add index index05(CommonName);

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

alter table HostDescriptionProbeSummary
	add index index01(EndTime);

alter table HostDescriptionProbeSummary
	add index index02(HostDescription);

alter table HostDescriptionProbeSummary
	add index index03(ProbeName);

alter table HostDescriptionProbeSummary
	add index index04(ResourceType);

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

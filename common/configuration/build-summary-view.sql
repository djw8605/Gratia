-- ----------------------------------
-- ProbeSummary
-- ----------------------------------
-- Previous versions had a table.
DROP TABLE IF EXISTS ProbeSummary;
DROP VIEW IF EXISTS ProbeSummary;

CREATE VIEW `ProbeSummary` AS
  SELECT MSD.EndTime AS EndTime,
         MSD.ProbeName AS ProbeName,
         MSD.ResourceType AS ResourceType,
         MSD.Njobs AS Njobs,
         IF(MSD.Cores > 0,(MSD.WallDuration * MSD.Cores),MSD.WallDuration) as WallDuration,
         MSD.CpuUserDuration AS CpuUserDuration,
         MSD.CpuSystemDuration AS CpuSystemDuration,
         MSD.Grid AS Grid
  FROM MasterSummaryData MSD;

-- ----------------------------------
-- UserProbeSummary
-- ----------------------------------
-- Previous versions had a table.
DROP TABLE IF EXISTS UserProbeSummary;
DROP VIEW IF EXISTS UserProbeSummary;

CREATE VIEW `UserProbeSummary` AS
  SELECT MSD.EndTime AS EndTime,
         MSD.CommonName as CommonName,
         MSD.DistinguishedName as DistinguishedName,
         MSD.ProbeName AS ProbeName,
         MSD.ResourceType AS ResourceType,
         PNC.ReportableProjectName AS ReportableProjectName,
         MSD.Grid AS Grid,
         MSD.Njobs AS Njobs,
         IF(MSD.Cores > 0,(MSD.WallDuration * MSD.Cores),MSD.WallDuration) as WallDuration,
         MSD.CpuUserDuration AS CpuUserDuration,
         MSD.CpuSystemDuration AS CpuSystemDuration
  FROM MasterSummaryData MSD
       JOIN ProjectNameCorrection PNC ON (MSD.ProjectNameCorrid = PNC.ProjectNameCorrid);

-- ----------------------------------
-- VOProbeSummary
-- ----------------------------------
-- Previous versions had a table.
DROP TABLE IF EXISTS VOProbeSummary;
DROP VIEW IF EXISTS VOProbeSummary;

CREATE VIEW `VOProbeSummary` AS
  SELECT MSD.EndTime AS EndTime,
         VO.VOName AS VOName,
         MSD.ProbeName AS ProbeName,
         MSD.CommonName AS CommonName,
         MSD.DistinguishedName as DistinguishedName,
         MSD.ResourceType AS ResourceType,
         MSD.Grid AS Grid,
         PNC.ReportableProjectName AS ReportableProjectName,
         MSD.Njobs AS Njobs,
         IF(MSD.Cores > 0,(MSD.WallDuration * MSD.Cores),MSD.WallDuration) as WallDuration,
         MSD.CpuUserDuration AS CpuUserDuration,
         MSD.CpuSystemDuration AS CpuSystemDuration
  FROM MasterSummaryData MSD
        JOIN VONameCorrection VC ON (MSD.VOcorrid = VC.corrid)
        JOIN VO ON (VC.VOid = VO.VOid)
        JOIN ProjectNameCorrection PNC ON (MSD.ProjectNameCorrid = PNC.ProjectNameCorrid);

-- ----------------------------------
-- HostDescriptionProbeSummary
-- ----------------------------------
-- Previous versions had a table.
DROP TABLE IF EXISTS HostDescriptionProbeSummary;
DROP VIEW IF EXISTS HostDescriptionProbeSummary;

CREATE VIEW `HostDescriptionProbeSummary` AS
  SELECT MSD.EndTime AS EndTime,
         MSD.HostDescription as HostDescription,
         MSD.ProbeName AS ProbeName,
         MSD.ResourceType AS ResourceType,
         MSD.Njobs AS Njobs,
         IF(MSD.Cores > 0,(MSD.WallDuration * MSD.Cores),MSD.WallDuration) as WallDuration,
         MSD.CpuUserDuration AS CpuUserDuration,
         MSD.CpuSystemDuration AS CpuSystemDuration,
         MSD.Grid AS Grid
  FROM MasterSummaryData MSD;

-- ----------------------------------
-- VOProbeSummaryData
--
-- This summary table is no longer needed.
-- ----------------------------------
DROP TABLE IF EXISTS VOProbeSummaryData;

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

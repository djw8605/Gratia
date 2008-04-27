DROP TABLE IF EXISTS ProbeSummary;
DROP VIEW IF EXISTS ProbeSummary;

CREATE VIEW `ProbeSummary` AS
  SELECT MSD.EndTime AS EndTime,
         MSD.ProbeName AS ProbeName,
         MSD.ResourceType AS ResourceType,
         SUM(MSD.Njobs) AS Njobs,
         SUM(MSD.WallDuration) AS WallDuration,
         SUM(MSD.CpuUserDuration) AS CpuUserDuration,
         SUM(MSD.CpuSystemDuration) AS CpuSystemDuration
  FROM MasterSummaryData MSD
  GROUP BY EndTime, ProbeName, ResourceType;

DROP TABLE IF EXISTS UserProbeSummary;
DROP VIEW IF EXISTS UserProbeSummary;

CREATE VIEW `UserProbeSummary` AS
  SELECT MSD.EndTime AS EndTime,
         MSD.CommonName as CommonName,
         MSD.ProbeName AS ProbeName,
         MSD.ResourceType AS ResourceType,
         SUM(MSD.Njobs) AS Njobs,
         SUM(MSD.WallDuration) AS WallDuration,
         SUM(MSD.CpuUserDuration) AS CpuUserDuration,
         SUM(MSD.CpuSystemDuration) AS CpuSystemDuration
  FROM MasterSummaryData MSD
  GROUP BY EndTime, CommonName, ProbeName, ResourceType;

DROP TABLE IF EXISTS VOProbeSummary;
DROP VIEW IF EXISTS VOProbeSummary;

CREATE VIEW `VOProbeSummary` AS
  SELECT MSD.EndTime AS EndTime,
         VO.VOName AS VOName,
         MSD.ProbeName AS ProbeName,
         MSD.CommonName AS CommonName,
         MSD.ResourceType AS ResourceType,
         SUM(MSD.Njobs) AS Njobs,
         SUM(MSD.WallDuration) AS WallDuration,
         SUM(MSD.CpuUserDuration) AS CpuUserDuration,
         SUM(MSD.CpuSystemDuration) AS CpuSystemDuration
  FROM MasterSummaryData MSD
        JOIN VONameCorrection VC ON (MSD.VOcorrid = VC.corrid)
        JOIN VO ON (VC.VOid = VO.VOid)
  GROUP BY EndTime, VOName, ProbeName, CommonName, ResourceType;

DROP TABLE IF EXISTS HostDescriptionProbeSummary;
DROP VIEW IF EXISTS HostDescriptionProbeSummary;

CREATE VIEW `HostDescriptionProbeSummary` AS
  SELECT MSD.EndTime AS EndTime,
         MSD.HostDescription as HostDescription,
         MSD.ProbeName AS ProbeName,
         MSD.ResourceType AS ResourceType,
         SUM(MSD.Njobs) AS Njobs,
         SUM(MSD.WallDuration) AS WallDuration,
         SUM(MSD.CpuUserDuration) AS CpuUserDuration,
         SUM(MSD.CpuSystemDuration) AS CpuSystemDuration
  FROM MasterSummaryData MSD
  GROUP BY EndTime, HostDescription, ProbeName, ResourceType;

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

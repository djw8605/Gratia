drop view if exists VOProbeSummary;

CREATE VIEW `VOProbeSummary` AS
  SELECT VPSD.EndTime AS EndTime,
         VO.VOName AS VOName,
         VPSD.ProbeName AS ProbeName,
         VPSD.CommonName AS CommonName,
         VPSD.ResourceType AS ResourceType,
         VPSD.Njobs AS Njobs,
         VPSD.WallDuration AS WallDuration,
         VPSD.CpuUserDuration AS CpuUserDuration,
         VPSD.CpuSystemDuration AS CpuSystemDuration
  FROM VOProbeSummaryData VPSD, VO, VONameCorrection VC
  WHERE VPSD.VOcorrid = VC.corrid
    AND VC.VOid = VO.VOid;

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

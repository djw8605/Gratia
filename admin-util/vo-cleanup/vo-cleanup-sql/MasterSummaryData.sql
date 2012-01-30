
\! echo 
\! echo =========================================================
\! echo   Review MasterSummaryData - summarized
\! echo =========================================================

SELECT 
   Site.SiteName AS Site
  ,VO.VOName as GratiaVO
  ,Main.VOCorrid as VOCorrid
  ,VC.ReportableVOName
  ,VC.VOName as ProbeVO
  ,Probe.ProbeName
  ,ResourceType
  ,min(date_format(EndTime,"%Y-%m")) as Earliest
  ,max(date_format(EndTime,"%Y-%m")) as Latest
  ,Sum(NJobs)
  ,Round(Sum(CpuUserDuration+CpuSystemDuration)/3600) as SumCPU
  ,Round(Sum(WallDuration)/3600) as SumWCT
  ,count(*) as rec_cnt
FROM
     VO
    ,VONameCorrection VC
    ,MasterSummaryData Main
    ,Site
    ,Probe
where
      VO.VOName like @voname
  and VO.VOid         = VC.VOid
  and VC.corrid       = Main.VOCorrid
  and Main.ProbeName  = Probe.ProbeName
  and Probe.siteid    = Site.siteid
group by 
   Site
  ,GratiaVO
  ,VOCorrid
  ,VC.ReportableVOName
  ,ProbeVO
  ,Probe.ProbeName
  ,ResourceType
-- limit 10
;

\! echo 
\! echo =========================================================
\! echo   VONameCorrection entries with JUR recs
\! echo =========================================================

SELECT 
   Site.SiteName AS Site
  ,VO.VOName as GratiaVO
  ,Main.VOCorrid as VOCorrid
  ,VC.ReportableVOName
  ,VC.VOName as ProbeVO
  ,Probe.ProbeName
  ,ResourceType
  ,min(date_format(EndTime,"%Y-%m")) as Earliest
  ,max(date_format(EndTime,"%Y-%m")) as Latest
  ,Sum(NJobs)
  ,Round(Sum(CpuUserDuration+CpuSystemDuration)/3600) as SumCPU
  ,Round(Sum(WallDuration)/3600) as SumWCT
  ,count(*) as rec_cnt
FROM
     VO
    ,VONameCorrection VC
    ,MasterSummaryData Main
    ,Site
    ,Probe
where
      VO.VOName like @voname
  and VO.VOid         = VC.VOid
  and VC.corrid       = Main.VOCorrid
-- 3 months back
  and EndTime > @mydate
  and Main.ProbeName  = Probe.ProbeName
  and Probe.siteid    = Site.siteid
group by 
   Site
  ,GratiaVO
  ,VOCorrid
  ,VC.ReportableVOName
  ,ProbeVO
  ,Probe.ProbeName
  ,ResourceType
-- limit 10
;


\! echo 
\! echo ============================================================
\! echo   MasterSummaryData table count
\! echo   - also show number of VONameCorrection entries to delete
\! echo ============================================================

SELECT 
   Main.VOcorrid
  ,VO.VOName as GratiaVO
  ,sum(NJobs) as Jobs
  ,count(*) as nbr_of_recs
FROM
     VO
    ,VONameCorrection VC
    ,MasterSummaryData Main
where
      VO.VOName like @voname
  and VO.VOid   = VC.VOid
  and VC.corrid = Main.VOCorrid
group by 
   Main.VOcorrid
  ,GratiaVO
;

\! echo 
\! echo =========================================================
\! echo   MasterSummaryData table - number of records to delete
\! echo =========================================================

SELECT 
   count(*) as nbr_of_deletions
FROM
     VO
    ,VONameCorrection VC
    ,MasterSummaryData Main
where
      VO.VOName like @voname
  and VO.VOid   = VC.VOid
  and VC.corrid = Main.VOCorrid
;


\! echo 
\! echo ============================================================
\! echo   MasterSummaryData DELETE statements
\! echo ============================================================

SELECT 
   CONCAT("DELETE FROM MasterSummaryData WHERE VOcorrid = ", a.VOCorrid ,";")
as delete_sql 
FROM
 (select distinct(Main.VOCorrid) as VOCorrid
  from
     VO
    ,VONameCorrection VC
    ,MasterSummaryData Main
where
      VO.VOName like @voname
  and VO.VOid   = VC.VOid
  and VC.corrid = Main.VOCorrid
) a
order by 
   delete_sql
;

\! echo 
\! echo ============================================================
\! echo   MasterSummaryData dump of records to be deleted
\! echo ============================================================

SELECT *
FROM MasterSummaryData
WHERE VOCorrid in 
 (select distinct(Main.VOCorrid) as VOCorrid
  from
     VO
    ,VONameCorrection VC
    ,MasterSummaryData Main
where
      VO.VOName like @voname
  and VO.VOid   = VC.VOid
  and VC.corrid = Main.VOCorrid
) 
-- limit 10
;




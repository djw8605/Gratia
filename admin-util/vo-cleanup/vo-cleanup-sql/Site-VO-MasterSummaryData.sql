

\! echo
\! echo =========================================================
\! echo   MasterSummaryData - Site/VO
\! echo   The purpose is to identify where non-registered VO
\! echo   names are coming from ... I think
\! echo =========================================================

\! echo =========================================================
\! echo   VOs used at a single Site 
\! echo     with void and vocorrid
\! echo     and VC.VOName and VC.ReportableVOName which are the
\! echo     the values that came up on the probe.
\! echo 
\! echo   For a given VO from a given Site, there may be more 
\! echo   than one VONameCorrection table entry.
\! echo =========================================================
SELECT
   VO.VOName as GratiaVO
  ,Site.SiteName AS Site
  ,VO.void 
  ,VC.corrid
  ,VC.VOName 
  ,VC.ReportableVOName
  ,max(date_format(EndTime,"%Y-%m-%d")) as Latest
  ,min(date_format(EndTime,"%Y-%m-%d")) as Earliest
  ,Sum(Njobs) as TotalJobs
  ,count(*) as rec_cnt
FROM
     MasterSummaryData Main
    ,VO
    ,VONameCorrection VC
    ,Site
    ,Probe
where
      Main.EndTime    > @mydate
  and Main.VOCorrid   = VC.corrid
  and VC.VOid         = VO.VOid
  and Main.ProbeName  = Probe.ProbeName
  and Probe.siteid    = Site.siteid
  and VO.VOName in (
  SELECT GratiaVO FROM (
    SELECT
       VO.VOName as GratiaVO
      ,count(distinct(Site.SiteName)) as NbrOfSites
    FROM
         MasterSummaryData Main
        ,VO
        ,VONameCorrection VC
        ,Site
        ,Probe
    where
        Main.EndTime    > @mydate
      and Main.ProbeName  = Probe.ProbeName
      and Probe.siteid    = Site.siteid
      and Main.VOCorrid   = VC.corrid
      and VC.VOid         = VO.VOid
    group by
       GratiaVO
--    limit 20
    ) a
    WHERE a.NbrOfSites = 1
    order by
       GratiaVO
    ) 
group by
   GratiaVO
  ,Site
  ,VO.void 
  ,VC.corrid
;

\! echo =========================================================
\! echo   VOs used at a single Site
\! echo =========================================================
SELECT * FROM (
SELECT
   a.GratiaVO
  ,count(a.Site) as NbrOfSites
  ,a.Site
  ,max(a.Latest) as Latest
  ,min(a.Earliest) as Earliest
  ,sum(a.TotalJobs) as TotalJobs
  ,sum(a.rec_cnt) as NbrOfRecs
FROM (
SELECT
   VO.VOName as GratiaVO
  ,Site.SiteName AS Site
  ,min(date_format(EndTime,"%Y-%m")) as Earliest
  ,max(date_format(EndTime,"%Y-%m")) as Latest
  ,Sum(Njobs) as TotalJobs
  ,count(*) as rec_cnt
FROM
     MasterSummaryData Main
    ,VO
    ,VONameCorrection VC
    ,Site
    ,Probe
where
      Main.EndTime    > @mydate
  and Main.VOCorrid   = VC.corrid
  and VC.VOid         = VO.VOid
  and Main.ProbeName  = Probe.ProbeName
  and Probe.siteid    = Site.siteid
group by
   GratiaVO
  ,Site
-- limit 20
) a
group by
   a.GratiaVO
--  ,a.Site
) b
WHERE b.NbrOfSites = 1
;

\! echo =========================================================
\! echo   VOs used at more than 1 Site
\! echo =========================================================
SELECT * FROM (
SELECT
   a.GratiaVO
--  ,a.Site
  ,count(a.Site) as NbrOfSites
  ,max(a.Latest)
  ,min(a.Earliest)
  ,sum(a.TotalJobs)
  ,sum(a.rec_cnt)
FROM (
SELECT
   VO.VOName as GratiaVO
  ,Site.SiteName AS Site
  ,min(date_format(EndTime,"%Y-%m")) as Earliest
  ,max(date_format(EndTime,"%Y-%m")) as Latest
  ,Sum(Njobs) as TotalJobs
  ,count(*) as rec_cnt
FROM
     MasterSummaryData Main
    ,VO
    ,VONameCorrection VC
    ,Site
    ,Probe
where
      Main.EndTime    > @mydate
  and Main.VOCorrid   = VC.corrid
  and VC.VOid         = VO.VOid
  and Main.ProbeName  = Probe.ProbeName
  and Probe.siteid    = Site.siteid
group by
   GratiaVO
  ,Site
-- limit 20
) a
group by
   a.GratiaVO
--  ,a.Site
) b
WHERE b.NbrOfSites > 1
;

\! echo =========================================================
\! echo   Ordered by GratiaVO, Site, Latest 
\! echo =========================================================
SELECT
   a.GratiaVO
  ,a.Site
  ,a.Latest
  ,a.Earliest
  ,a.TotalJobs
  ,a.rec_cnt
FROM (
SELECT
   VO.VOName as GratiaVO
  ,Site.SiteName AS Site
  ,min(date_format(EndTime,"%Y-%m")) as Earliest
  ,max(date_format(EndTime,"%Y-%m")) as Latest
  ,Sum(Njobs) as TotalJobs
  ,count(*) as rec_cnt
FROM
     MasterSummaryData Main
    ,VO
    ,VONameCorrection VC
    ,Site
    ,Probe
where
      Main.EndTime    > @mydate
  and Main.VOCorrid   = VC.corrid
  and VC.VOid         = VO.VOid
  and Main.ProbeName  = Probe.ProbeName
  and Probe.siteid    = Site.siteid
group by
   GratiaVO
  ,Site
-- limit 20
) a
order by
   a.GratiaVO
  ,a.Site
  ,a.Latest desc
;

\! echo =========================================================
\! echo   Ordered by Site, GratiaVO,  Latest 
\! echo =========================================================

SELECT
   a.Site
  ,a.GratiaVO
  ,a.Latest
  ,a.Earliest
  ,a.TotalJobs
  ,a.rec_cnt
FROM (
SELECT
   Site.SiteName AS Site
  ,VO.VOName as GratiaVO
  ,min(date_format(EndTime,"%Y-%m")) as Earliest
  ,max(date_format(EndTime,"%Y-%m")) as Latest
  ,Sum(Njobs) as TotalJobs
  ,count(*) as rec_cnt
FROM
     MasterSummaryData Main
    ,VO
    ,VONameCorrection VC
    ,Site
    ,Probe
where
      Main.EndTime    > @mydate
  and Main.VOCorrid   = VC.corrid
  and VC.VOid         = VO.VOid
  and Main.ProbeName  = Probe.ProbeName
  and Probe.siteid    = Site.siteid
group by
   GratiaVO
  ,Site
-- limit 20
) a
order by
   a.Site
  ,a.GratiaVO
  ,a.Latest desc
;

\! echo =========================================================
\! echo   Ordered by GratiaVO, Site, Probe,  Latest 
\! echo =========================================================

SELECT
   a.GratiaVO
  ,a.Site
  ,a.ProbeName
  ,a.Latest
  ,a.Earliest
  ,a.TotalJobs
  ,a.rec_cnt
FROM (
SELECT
   VO.VOName as GratiaVO
  ,Site.SiteName AS Site
  ,Main.ProbeName AS ProbeName
  ,min(date_format(EndTime,"%Y-%m")) as Earliest
  ,max(date_format(EndTime,"%Y-%m")) as Latest
  ,Sum(Njobs) as TotalJobs
  ,count(*) as rec_cnt
FROM
     MasterSummaryData Main
    ,VO
    ,VONameCorrection VC
    ,Site
    ,Probe
where
      Main.EndTime    > @mydate
  and Main.VOCorrid   = VC.corrid
  and VC.VOid         = VO.VOid
  and Main.ProbeName  = Probe.ProbeName
  and Probe.siteid    = Site.siteid
group by
   GratiaVO
  ,Site
  ,ProbeName
-- limit 20
) a
order by
   a.GratiaVO
  ,a.Site
  ,a.ProbeName
  ,a.Latest desc
;

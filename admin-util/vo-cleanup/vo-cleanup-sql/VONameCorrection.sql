
\! echo 
\! echo =========================================================
\! echo   JUR recs still with bad VOName
\! echo   This stops the VOName from being deleted from the
\! echo   VONameCorrection and VO table until the JUR records are dropped.
\! echo =========================================================


select distinct VOName, 
       count(*) as JUR_records
from JobUsageRecord 
where VOName like @voname
group by VOName
;

\! echo 
\! echo =========================================================
\! echo   VO table count of the VOName
\! echo =========================================================


select count(*) as GratiaVO_cnt
from VO
where VOName like @voname
;

\! echo 
\! echo =========================================================
\! echo    VONameCorrection table count of the VOName
\! echo =========================================================


select count(*) as ProbeVO_cnt
from VONameCorrection
where VOName like @voname
;

\! echo ========================================================
\! echo    Query - Unused VONameCorrection table entries.
\! echo ========================================================
SELECT 
   VO.VOName as GratiaVO
  ,VO.void
  ,VC.corrid
  ,VC.VOName as ProbeVO
  ,VC.ReportableVOName
FROM
   VONameCorrection VC
  ,VO VO
where 
  VC.VOid = VO.VOid
and VC.corrid not in (SELECT distinct(VOCorrid) FROM MasterSummaryData)
and VC.corrid not in (SELECT distinct(VOCorrid) FROM MasterServiceSummaryHourly)
and VC.corrid not in (SELECT distinct(VOCorrid) FROM MasterServiceSummary)
and VC.corrid not in (SELECT distinct(VOCorrid) FROM MasterTransferSummary)
order by 
  GratiaVO
 ,VO.void
 ,VC.corrid
 ,ProbeVO       
 ,ReportableVOName
;


\! echo ========================================================
\! echo    Query - VONameCorrection table entries not in VO.
\! echo ========================================================
SELECT 
   VC.VOName as ProbeVO
  ,VC.ReportableVOName
  ,VC.corrid
  ,VC.void
FROM
   VONameCorrection VC
  ,VO VO
where 
  VC.void not in (
  SELECT distinct(void) FROM VO)
order by 
  ProbeVO       
 ,VC.corrid
 ,VC.void
 ,ReportableVOName
;


\! echo ========================================================
\! echo    Query - Used VONameCorrection table entries.
\! echo ========================================================

SELECT 
  count(*) as nbr_of_entries
FROM
   VONameCorrection VC
  ,VO VO
where 
  VC.VOid = VO.VOid
and (
     VC.corrid in (SELECT distinct(VOCorrid) FROM MasterSummaryData)
  or VC.corrid in (SELECT distinct(VOCorrid) FROM MasterServiceSummaryHourly)
  or VC.corrid in (SELECT distinct(VOCorrid) FROM MasterServiceSummary)
  or VC.corrid in (SELECT distinct(VOCorrid) FROM MasterTransferSummary)
  )
;

SELECT 
   VO.VOName as GratiaVO
  ,VO.void
  ,VC.corrid
  ,VC.VOName as ProbeVO
  ,VC.ReportableVOName
--  ,count(*) as nbr_of_refs
FROM
   VONameCorrection VC
  ,VO VO
where 
  VC.VOid = VO.VOid
and (
     VC.corrid in (SELECT distinct(VOCorrid) FROM MasterSummaryData)
  or VC.corrid in (SELECT distinct(VOCorrid) FROM MasterServiceSummaryHourly)
  or VC.corrid in (SELECT distinct(VOCorrid) FROM MasterServiceSummary)
  or VC.corrid in (SELECT distinct(VOCorrid) FROM MasterTransferSummary)
  )
order by 
  GratiaVO
 ,VO.void
 ,VC.corrid
 ,ProbeVO       
 ,ReportableVOName
 ,VC.corrid
;


\! echo  ============================================================
\! echo    VONameCorrection DELETE statements - unused ones
\! echo  ============================================================

SELECT
   CONCAT("DELETE FROM VONameCorrection WHERE corrid = ", a.corrid ,";")
as delete_sql
FROM
 ( SELECT distinct(corrid)
   FROM
      VONameCorrection VC
   where 
       VC.corrid not in (SELECT distinct(VOCorrid) FROM MasterSummaryData)
   and VC.corrid not in (SELECT distinct(VOCorrid) FROM MasterServiceSummaryHourly)
   and VC.corrid not in (SELECT distinct(VOCorrid) FROM MasterServiceSummary)
   and VC.corrid not in (SELECT distinct(VOCorrid) FROM MasterTransferSummary)
 ) a
order by delete_sql
;


\! echo  ============================================================
\! echo    VONameCorrection dump of records to be deleted
\! echo  ============================================================

SELECT *
FROM VONameCorrection
WHERE corrid in
 ( SELECT distinct(corrid)
   FROM
      VONameCorrection VC
   where 
       VC.corrid not in (SELECT distinct(VOCorrid) FROM MasterSummaryData)
   and VC.corrid not in (SELECT distinct(VOCorrid) FROM MasterServiceSummaryHourly)
   and VC.corrid not in (SELECT distinct(VOCorrid) FROM MasterServiceSummary)
   and VC.corrid not in (SELECT distinct(VOCorrid) FROM MasterTransferSummary)
 ) 
-- limit 10
;



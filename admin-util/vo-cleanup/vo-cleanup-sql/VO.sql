

\! echo ========================================================
\! echo    Query - Total VO table entries for summary tables.
\! echo ========================================================
SELECT count(*) from VO;

\! echo ========================================================
\! echo    Query - Unused VO table entries for summary tables.
\! echo ========================================================
SELECT count(*) FROM VO
where VO.void not in (select distinct(void) from VONameCorrection) 
;

SELECT 
   VO.VOName as GratiaVO
  ,VO.void
FROM
   VO VO
where 
  VO.void not in (select distinct(void) from VONameCorrection) 
order by 
  GratiaVO
 ,VO.void
;


\! echo ========================================================
\! echo    Query - Used VO table entries for summary tables.
\! echo ========================================================
SELECT count(*) FROM VO
where VO.void in (select distinct(void) from VONameCorrection) 
;

SELECT 
   VO.VOName as GratiaVO
  ,VO.void
FROM
   VO VO
where 
  VO.void in (select distinct(void) from VONameCorrection) 
order by 
  GratiaVO
 ,VO.void
;

\! echo ============================================================
\! echo   VO table DELETE statements - unused ones
\! echo ============================================================

SELECT
   CONCAT("DELETE FROM VO WHERE void = ", a.void ,";")
as delete_sql
FROM  
  ( SELECT distinct(void) FROM VO 
   where VO.void not in (select distinct(void) from VONameCorrection)) a
order by void
;

\! echo ============================================================
\! echo   VO table dump of records to be deleted
\! echo ============================================================

SELECT * FROM VO
where void not in (select distinct(void) from VONameCorrection)
order by VOName
-- limit 10
;



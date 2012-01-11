
-- ============================================================
--   VO table DELETE statements (unused ones)
-- ============================================================

SELECT
   CONCAT("DELETE FROM VO WHERE void = ", a.void ,";")
as delete_sql
FROM  
  ( SELECT distinct(void) FROM VO 
   where VO.void not in (select distinct(void) from VONameCorrection)) a
order by void
;



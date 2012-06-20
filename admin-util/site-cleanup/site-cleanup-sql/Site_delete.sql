
\! echo "-- ========================================================"
\! echo "--   Site table DELETE statements (unused ones)"
\! echo "-- ============================================================"

SELECT
   CONCAT("DELETE FROM Site WHERE siteid = ", a.siteid ,";")
as delete_sql
FROM  
  ( SELECT siteid FROM Site 
   WHERE siteid NOT IN (select distinct(siteid) from Probe)) a
order by siteid 
;



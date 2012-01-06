
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



SELECT
   CONCAT("DELETE FROM MasterTransferSummary WHERE VOcorrid = ", a.VOCorrid ,";")
as delete_sql
FROM
 (select distinct(Main.VOCorrid) as VOCorrid
  from
     VO
    ,VONameCorrection VC
    ,MasterTransferSummary Main
where
      VO.VOName like @voname
  and VO.VOid   = VC.VOid
  and VC.corrid = Main.VOCorrid
) a
order by
   delete_sql
;


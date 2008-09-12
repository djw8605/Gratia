DELIMITER $$

DROP PROCEDURE IF EXISTS `dCacheSimple`$$
CREATE PROCEDURE `dCacheSimple`()
READS SQL DATA
begin
select dMonth as `Date`, ProbeName, SiteName, sum(X.Njobs) as Njobs,
       sum(X.`MB transferred`) as `MB Transferred`
 from ((select date_format(StartTime, '%Y-%m') as dMonth,
               S.SiteName, T.ProbeName,
               T.Njobs as Njobs,
               T.TransferSize /1024/1024 as `MB transferred`
from MasterTransferSummary T
     join Probe P on (T.ProbeName = P.probename)
     join Site S on (P.siteid = S.siteid)
where T.StorageUnit = 'b') UNION ALL
(select date_format(StartTime, '%Y-%m') as dMonth,
               S.SiteName, T.ProbeName,
       T.Njobs as Njobs,
       T.TransferSize /1024 as `MB transferred`
from MasterTransferSummary T
     join Probe P on (T.ProbeName = P.probename)
     join Site S on (P.siteid = S.siteid)
where T.StorageUnit = 'kb') UNION ALL
(select date_format(StartTime, '%Y-%m') as dMonth,
               S.SiteName, T.ProbeName,
       T.Njobs as Njobs,
       T.TransferSize as `MB transferred`
from MasterTransferSummary T
     join Probe P on (T.ProbeName = P.probename)
     join Site S on (P.siteid = S.siteid)
where T.StorageUnit = 'mb')) X
group by dMonth, ProbeName, SiteName
order by dMonth desc, ProbeName, SiteName;
END $$
DELIMITER ;

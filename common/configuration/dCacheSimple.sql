DELIMITER ||

-- Drop and recreate lookup table for multipliers to avoid awkward joins.
DROP TABLE IF EXISTS SizeUnits
||
CREATE TABLE SizeUnits
(Unit VARCHAR(8) NOT NULL PRIMARY KEY, Multiplier DOUBLE NOT NULL DEFAULT 0.0)
||
-- Populate it. Base unit is MiB
INSERT INTO SizeUnits VALUES
('b', 1 / 1024 / 1024),
('kb', 1 / 1024),
('mb', 1),
('gb', 1024),
('tb', 1024 * 1024),
('pb', 1024 * 1024 * 1024),
('eb', 1024 * 1024 * 1024 * 1024)
 ||
-- Drop and create backward-compatible procedure
DROP PROCEDURE IF EXISTS `dCacheSimple`
||
CREATE PROCEDURE `dCacheSimple`()
SQL SECURITY INVOKER
READS SQL DATA
BEGIN
CALL TransferSimple(NULL, NULL);
END
||
-- Drop and create query procedure
DROP PROCEDURE IF EXISTS `TransferSimple`
||
CREATE PROCEDURE `TransferSimple`(StartMonth DATE,
  EndMonth DATE)
SQL SECURITY INVOKER
READS SQL DATA
BEGIN
SELECT `Date`, S.SiteName AS `Site Name`, `Probe Name`,
`# Jobs`, `Data transferred (MiB)`
FROM (SELECT
DATE_FORMAT(StartTime, '%Y-%m') AS `Date`,
T.ProbeName as `Probe Name`,
SUM(T.Njobs) AS `# Jobs`,
SUM(T.TransferSize * Multiplier) AS `Data Transferred (MiB)`
FROM
MasterTransferSummary T
JOIN SizeUnits SU ON (T.StorageUnit = SU.Unit)
WHERE StartTime >= DATE_FORMAT(IFNULL(StartMonth, '2006-01-01'), '%Y-%m-01')
  AND StartTime < DATE_FORMAT(IFNULL(EndMonth, NOW()) + INTERVAL 1 MONTH, '%Y-%m-01')
GROUP BY `Date`, `Probe Name`) X
JOIN Probe P on (X.`Probe Name` = P.Probename)
JOIN Site S on (P.siteid = S.siteid)
ORDER BY `Date` desc, `Site Name`, `Probe Name`;
END
||
DELIMITER ;

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

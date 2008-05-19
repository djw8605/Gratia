DROP TABLE IF EXISTS ProbeStatus, MasterSummaryData;

CREATE TABLE `ProbeStatus` 
(`EndTime` DATETIME NOT NULL DEFAULT 0,
 `ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
 `Njobs` INTEGER NOT NULL DEFAULT 0,
 INDEX index01(EndTime),
 INDEX index02(ProbeName),
 UNIQUE INDEX index03(EndTime, ProbeName)
) ENGINE = 'innodb';

CREATE TABLE `MasterSummaryData`
(`EndTime` DATETIME NOT NULL DEFAULT 0,
 `VOcorrid` INT NOT NULL DEFAULT 0,
 `ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
 `CommonName` VARCHAR(255) NOT NULL DEFAULT 'Unknown',
 `ResourceType` VARCHAR(255) NOT NULL DEFAULT 'Unknown',
 `HostDescription` VARCHAR(255) NOT NULL DEFAULT 'Unknown', 
 `ApplicationExitCode` VARCHAR(255) NOT NULL DEFAULT '0',
 `Njobs` INTEGER NOT NULL DEFAULT 0,
 `WallDuration` DOUBLE NOT NULL DEFAULT 0,
 `CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
 `CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0,
 INDEX index01(EndTime),
 INDEX index02(VOcorrid),
 INDEX index03(ProbeName),
 INDEX index04(CommonName),
 INDEX index05(ResourceType),
 INDEX index06(HostDescription),
 INDEX index07(ApplicationExitCode),
 UNIQUE INDEX index08(EndTime, VOcorrid, ProbeName,
                      CommonName, ResourceType,
                      HostDescription, ApplicationExitCode)

) ENGINE = 'innodb';

INSERT INTO ProbeStatus
SELECT STR_TO_DATE(DATE_FORMAT(ServerDate, '%Y-%c-%e %H:00:00'),
                   '%Y-%c-%e %H:00:00') AS sTime,
       ProbeName,
       COUNT(*)
FROM JobUsageRecord_Meta
GROUP BY ProbeName, sTime;
         
INSERT INTO MasterSummaryData
SELECT DATE(EndTime) AS eDate,
       VC.corrid AS VOcorrid,
       ProbeName,
       CommonName,
       ResourceType,
       HostDescription,
       IFNULL(RT.value, J.Status) AS ApplicationExitCode,
       SUM(Njobs),
       SUM(WallDuration),
       SUM(CpuUserDuration),
       SUM(CpuSystemDuration)
FROM JobUsageRecord J
     JOIN JobUsageRecord_Meta M ON (J.dbid = M.dbid)
     JOIN VONameCorrection VC ON
      ((J.VOName = BINARY VC.VOName) AND
       (((J.ReportableVOName IS NULL) AND (VC.ReportableVOName IS NULL))
        OR (BINARY J.ReportableVOName = BINARY VC.ReportableVOName)))
     LEFT JOIN Resource RT ON
      ((J.dbid = RT.dbid) AND
       (RT.description = 'ExitCode'))
WHERE J.CpuUserDuration IS NOT NULL and ResourceType = 'Batch'
GROUP BY VOcorrid, ProbeName, CommonName,
         ResourceType, HostDescription, ApplicationExitCode, eDate;

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

DELIMITER ||

DROP TABLE IF EXISTS ProbeStatus -- Obsolete summary table
||
DROP PROCEDURE IF EXISTS updatenodesummary -- Obsolete summary procedure
||
DROP PROCEDURE IF EXISTS add_JUR_to_summary
||
CREATE PROCEDURE add_JUR_to_summary(inputDbid BIGINT(20))
SQL SECURITY INVOKER
DETERMINISTIC
AJUR:BEGIN
  -- Main
  DECLARE n_ProbeName VARCHAR(255);
  DECLARE n_CommonName VARCHAR(255);
  DECLARE n_DistinguishedName VARCHAR(255);
  DECLARE n_VOcorrid BIGINT(20);
  DECLARE n_ResourceType VARCHAR(255);
  DECLARE n_HostDescription VARCHAR(255);
  DECLARE n_ApplicationExitCode VARCHAR(255);
  DECLARE n_Njobs BIGINT(20);
  DECLARE n_WallDuration DOUBLE;
  DECLARE n_CpuUserDuration DOUBLE;
  DECLARE n_CpuSystemDuration DOUBLE;
  DECLARE n_ServerDate DATETIME;
  DECLARE n_StartTime DATETIME;
  DECLARE n_EndTime DATETIME;
  DECLARE n_rowDate DATETIME;
  DECLARE n_Grid VARCHAR(255);
  DECLARE n_Cores BIGINT(20);
  DECLARE n_ProjectNameCorrid BIGINT(20);

  -- temporary variables for a CPU exceeding Wall verification
  DECLARE t_TotalWall  DOUBLE;
  DECLARE t_TotalCPU   DOUBLE;

  -- Storage only
  DECLARE n_DN VARCHAR(255);
  DECLARE n_Protocol VARCHAR(255);
  DECLARE n_RemoteSite VARCHAR(255);
  DECLARE n_Status BIGINT(20);
  DECLARE n_IsNew BIGINT(20);
  DECLARE n_StorageUnit VARCHAR(64);
  DECLARE n_TransferDuration DOUBLE;
  DECLARE n_TransferSize DOUBLE;

  -- NodeSummary update only
  DECLARE wantNodeSummary VARCHAR(64) DEFAULT '';
  DECLARE n_Host text;
  DECLARE mycpucount BIGINT DEFAULT 0;
  DECLARE mybenchmarkscore BIGINT DEFAULT 0;
  DECLARE divide INT DEFAULT 0;
  DECLARE counter INT DEFAULT 0;  
  DECLARE newdate DATETIME;
  DECLARE newcpusystemtime INT DEFAULT 0;
  DECLARE newcpuusertime INT DEFAULT 0;
  DECLARE imax INT DEFAULT 0;

  -- Data collection
  --
  SELECT M.ProbeName,
         IFNULL(J.CommonName, ''),
         IFNULL(IF(LOCATE("<ds:X509SubjectName>", J.KeyInfoContent) != 0,
                   CONCAT('/', 
                       REPLACE(SUBSTRING(J.KeyInfoContent,
                                         LOCATE('<ds:X509SubjectName>', J.KeyInfoContent) + LENGTH('<ds:X509SubjectName>'),
                                         LOCATE('</ds:X509SubjectName>', J.KeyInfoContent) - (LOCATE('<ds:X509SubjectName>', J.KeyInfoContent) + LENGTH('<ds:X509SubjectName>'))),
                               ', ',
                               '/')),
                   J.KeyInfoContent),
                ''),
         VC.corrid,
         IFNULL(J.ResourceType, ''),
         IFNULL(J.HostDescription, ''),
         IFNULL(J.Host, ''),
         IFNULL(EC.`Value`,
                IF(EBS.`Value` = 'TRUE' OR EBS.`Value` = '1',
                   IFNULL(ES.`Value`, 0) + 128,
                   IF(JS.`Value` = 3, 3 << 16, IFNULL(J.`Status`, 0)))),
         J.Njobs,
         J.WallDuration,
         J.CpuUserDuration,
         J.CpuSystemDuration,
         M.ServerDate,
         J.StartTime,
         J.EndTime,
         IF(ResourceType IN ('Storage', 'RawCPU', 'Transfer'),
            DATE(J.StartTime),
            DATE(J.EndTime)),
         IFNULL(M.Grid, ''),
         IFNULL(J.Processors, 1),
         PNC.ProjectNameCorrid
  INTO n_ProbeName,
       n_CommonName,
       n_DistinguishedName,
       n_VOcorrid,
       n_ResourceType,
       n_HostDescription,
       n_Host,
       n_ApplicationExitCode,
       n_Njobs,
       n_WallDuration,
       n_CpuUserDuration,
       n_CpuSystemDuration,
       n_ServerDate,
       n_StartTime,
       n_EndTime,
       n_rowdate,
       n_Grid,
       n_Cores,
       n_ProjectNameCorrid
  FROM JobUsageRecord J
       JOIN JobUsageRecord_Meta M ON (J.dbid = M.dbid)
       JOIN VONameCorrection VC ON
        ((J.VOName = BINARY VC.VOName) AND
         (((J.ReportableVOName IS NULL) AND (VC.ReportableVOName IS NULL))
          OR (BINARY J.ReportableVOName = BINARY VC.ReportableVOName)))
       JOIN ProjectNameCorrection PNC ON
         ((J.ProjectName = BINARY PNC.ProjectName) OR
         (((J.ProjectName IS NULL) AND (PNC.ProjectName IS NULL))
          OR (BINARY J.ProjectName = BINARY PNC.ProjectName)))
       LEFT JOIN Resource EC ON
        ((J.dbid = EC.dbid) AND
         (EC.description = 'ExitCode'))
       LEFT JOIN Resource ES ON
        ((J.dbid = ES.dbid) AND
         (ES.description = 'ExitSignal'))
       LEFT JOIN Resource EBS ON
        ((J.dbid = EBS.dbid) AND
         (EBS.description = 'ExitBySignal'))
       LEFT JOIN Resource JS ON
        ((J.dbid = JS.dbid) AND
         (JS.description = 'condor.JobStatus'))
  WHERE J.dbid = inputDbid;

  -- Basic data checks
  IF n_ProbeName IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', inputDbid, 'Failed due to null ProbeName');
     LEAVE AJUR;
  END IF;

  IF n_VOcorrid IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', inputDbid, 'Failed due to null VOcorrid');
     LEAVE AJUR;
  END IF;

  IF n_ProjectNameCorrid IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', inputDbid, 'Failed due to null ProjectNameCorrid');
     LEAVE AJUR;
  END IF;

  IF n_Njobs IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', inputDbid, 'Failed due to null Njobs');
     LEAVE AJUR;
  END IF;

  IF n_ResourceType IN ('Storage', 'Transfer') THEN

    IF n_StartTime IS NULL THEN
      INSERT INTO trace(eventtime, procName, p1, sqlQuery)
       VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', inputDbid, 'Failed due to null StartTime');
      LEAVE AJUR;
    END IF;

    SELECT Protocol,
           IF(IsNew = '1', `Source`, `Destination`),
           IsNew,
           Status,
           StorageUnit,
           PhaseUnit,
           IF(`Value` > 0, `Value`, 0)
    INTO n_Protocol,
         n_RemoteSite,
         n_IsNew,
         n_Status,
         n_StorageUnit,
         n_TransferDuration,
         n_TransferSize
    FROM JobUsageRecord R
         join TDCorr on (R.dbid = TDCorr.dbid)
         join TransferDetails T on (TDCorr.TransferDetailsId = T.TransferDetailsId)
         join Network N on (R.dbid = N.dbid)
    WHERE R.dbid = inputDbid
    LIMIT 1;

    IF n_Protocol IS NULL THEN
      INSERT INTO trace(eventtime, procName, p1, sqlQuery)
       VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', inputDbid, 'Failed due to null Protocol');
      LEAVE AJUR;
    END IF;

    -- Note entries with different StorageUnit values get stored
    -- independently and must be combined manually outside the DB
    INSERT INTO MasterTransferSummary(StartTime, VOcorrid, ProbeName, Grid,
                                      CommonName, DistinguishedName, Protocol, RemoteSite, Status,
                                      IsNew, Njobs, TransferSize, StorageUnit,
                                      TransferDuration,ProjectNameCorrid)
    VALUES(n_rowDate,
           n_VOcorrid,
           n_ProbeName,
           n_Grid,
           n_CommonName,
           n_DistinguishedName,
           n_Protocol,
           n_RemoteSite,
           n_Status,
           n_IsNew,
           n_Njobs,
           n_TransferSize,
           n_StorageUnit,
           n_TransferDuration,
           n_ProjectNameCorrid)
    ON DUPLICATE KEY UPDATE
     Njobs = Njobs + VALUES(Njobs),
     TransferSize = TransferSize + VALUES(TransferSize),
     TransferDuration = TransferDuration + VALUES(TransferDuration);

    LEAVE AJUR; -- Done

  END IF;

  IF n_WallDuration IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', inputDbid, 'Failed due to null WallDuration');
     LEAVE AJUR;
  END IF;

  IF n_CpuUserDuration IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', inputDbid, 'Failed due to null CpuUserDuration');
     LEAVE AJUR;
  END IF;

  IF n_CpuSystemDuration IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', inputDbid, 'Failed due to null CpuSystemDuration');
     LEAVE AJUR;
  END IF;

  IF n_EndTime < n_StartTime THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', inputDbid, 'Failed due to EndTime < StartTime');
     LEAVE AJUR;
  END IF;


  SET t_TotalWall = n_WallDuration * n_Cores;
  SET t_TotalCPU  = n_CpuUserDuration + n_CpuSystemDuration;
  IF t_TotalCPU > t_TotalWall THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', inputDbid, 'Warning due to CPU exceeding Wall');
     -- LEAVE AJUR; -- currently just a warning. MasterSummaryData will be updated
  END IF;

  -- MasterSummaryData
  INSERT INTO MasterSummaryData(EndTime, VOcorrid, ProbeName, CommonName,
                                DistinguishedName, ResourceType, HostDescription,
                                ApplicationExitCode, Njobs, WallDuration,
                                CpuUserDuration, CpuSystemDuration,
                                Grid, Cores, ProjectNameCorrid)
  VALUES(DATE(n_EndTime),
         n_VOcorrid,
         n_ProbeName,
         n_CommonName,
         n_DistinguishedName,
         n_ResourceType,
         n_HostDescription,
         n_ApplicationExitCode,
         n_Njobs,
         n_WallDuration,
         n_CpuUserDuration,
         n_CpuSystemDuration,
         n_Grid,
         n_Cores,
         n_ProjectNameCorrid)
  ON DUPLICATE KEY UPDATE
   Njobs = Njobs + VALUES(Njobs),
   WallDuration = WallDuration + VALUES(WallDuration),
   CpuUserDuration = CpuUserDuration + VALUES(CpuUserDuration),
   CpuSystemDuration = CpuSystemDuration + VALUES(CpuSystemDuration);

  -- NodeSummary
  select cdr into wantNodeSummary from SystemProplist
    where car = 'gratia.database.wantNodeSummary';

  if wantNodeSummary = '1' then
    set mycpucount = 0;
    set mybenchmarkscore = 0;
    select BenchmarkScore, CPUCount
      into mybenchmarkscore, mycpucount from CPUInfo
      where n_HostDescription = CPUInfo.HostDescription;

    set imax = datediff(n_EndTime, n_StartTime) + 1;
    set newcpusystemtime = n_CpuSystemDuration / imax;
    set newcpuusertime = n_CpuUserDuration / imax;
    set counter = 0;
    while counter < imax do
      -- Calculate date for summary entry
      set newdate = adddate(n_StartTime,counter);
--      insert into trace(eventtime, procName, userKey, userName, userRole, userVO, sqlQuery, procTime, queryTime, p1, p2, p3)
--        values(n_EndTime, 'add_JUR_to_summary',
--               date(newdate), n_Host, n_ProbeName, n_ResourceType,
--               'Insert / Update NodeSummary'
--               newcpusystemtime, newcpuusertime, 
--               mycpucount, n_HostDescription, mybenchmarkscore,
--               );
      -- Insert / update
      insert into NodeSummary
        (EndTime, Node, ProbeName, ResourceType,
         CpuSystemTime, CpuUserTime,
         CpuCount, HostDescription,
         BenchmarkScore, DaysInMonth)
       values(date(newdate), n_Host, n_ProbeName, n_ResourceType,
              newcpusystemtime, newcpuusertime, mycpucount,
              n_HostDescription, mybenchmarkscore,
              extract(DAY from last_day(newdate)))
       on duplicate key update
         CpuSystemTime = CpuSystemTime + values(CpuSystemTime),
         CpuUserTime = CpuUserTime + values(CpuUserTime);
      -- Update counter
      set counter = counter + 1;
    end while;
  end if; -- wantNodeSummary
END;
||
DROP PROCEDURE IF EXISTS del_JUR_from_summary
||
CREATE PROCEDURE del_JUR_from_summary(inputDbid BIGINT(20))
SQL SECURITY INVOKER
DETERMINISTIC
DJUR:BEGIN
  -- Main
  DECLARE n_ProbeName VARCHAR(255);
  DECLARE n_CommonName VARCHAR(255);
  DECLARE n_DistinguishedName VARCHAR(255);
  DECLARE n_VOcorrid BIGINT(20);
  DECLARE n_ResourceType VARCHAR(255);
  DECLARE n_HostDescription VARCHAR(255);
  DECLARE n_ApplicationExitCode VARCHAR(255);
  DECLARE n_Njobs BIGINT(20);
  DECLARE n_WallDuration DOUBLE;
  DECLARE n_CpuUserDuration DOUBLE;
  DECLARE n_CpuSystemDuration DOUBLE;
  DECLARE n_ServerDate DATETIME;
  DECLARE n_StartTime DATETIME;
  DECLARE n_EndTime DATETIME;
  DECLARE n_rowDate DATE;
  DECLARE n_Grid VARCHAR(255);
  DECLARE n_Cores BIGINT(20);
  DECLARE n_ProjectNameCorrid BIGINT(20);

  -- Storage only
  DECLARE n_DN VARCHAR(255);
  DECLARE n_Protocol VARCHAR(255);
  DECLARE n_RemoteSite VARCHAR(255);
  DECLARE n_Status BIGINT(20);
  DECLARE n_IsNew BIGINT(20);
  DECLARE n_StorageUnit VARCHAR(64);
  DECLARE n_TransferDuration DOUBLE;
  DECLARE n_TransferSize DOUBLE;

  -- NodeSummary update only
  DECLARE wantNodeSummary VARCHAR(64) DEFAULT '';
  DECLARE n_Host text;
  DECLARE mycpucount BIGINT DEFAULT 0;
  DECLARE mybenchmarkscore BIGINT DEFAULT 0;
  DECLARE divide INT DEFAULT 0;
  DECLARE counter INT DEFAULT 0;  
  DECLARE newdate DATETIME;
  DECLARE newcpusystemtime INT DEFAULT 0;
  DECLARE newcpuusertime INT DEFAULT 0;
  DECLARE imax INT DEFAULT 0;

  -- Data collection
  SELECT M.ProbeName,
         IFNULL(J.CommonName, ''),
         IFNULL(IF(LOCATE("<ds:X509SubjectName>", J.KeyInfoContent) != 0,
                   CONCAT('/', 
                       REPLACE(SUBSTRING(J.KeyInfoContent,
                                         LOCATE('<ds:X509SubjectName>', J.KeyInfoContent) + LENGTH('<ds:X509SubjectName>'),
                                         LOCATE('</ds:X509SubjectName>', J.KeyInfoContent) - (LOCATE('<ds:X509SubjectName>', J.KeyInfoContent) + LENGTH('<ds:X509SubjectName>'))),
                               ', ',
                               '/')),
                   J.KeyInfoContent),
                ''),
         VC.corrid,
         IFNULL(J.ResourceType, ''),
         IFNULL(J.HostDescription, ''),
         IFNULL(J.Host, ''),
         IFNULL(EC.`Value`,
                IF(EBS.`Value` = 'TRUE' OR EBS.`Value` = '1',
                   IFNULL(ES.`Value`, 0) + 128,
                   IF(JS.`Value` = 3, 3 << 16, IFNULL(J.`Status`, 0)))),
         J.Njobs,
         J.WallDuration,
         J.CpuUserDuration,
         J.CpuSystemDuration,
         M.ServerDate,
         J.StartTime,
         J.EndTime,
         IF(ResourceType = 'Batch', DATE(J.EndTime),
            DATE(J.StartTime)),
         M.Grid,
         IFNULL(J.Processors, 1),
         PNC.ProjectNameCorrid
  INTO n_ProbeName,
       n_CommonName,
       n_DistinguishedName,
       n_VOcorrid,
       n_ResourceType,
       n_HostDescription,
       n_Host,
       n_ApplicationExitCode,
       n_Njobs,
       n_WallDuration,
       n_CpuUserDuration,
       n_CpuSystemDuration,
       n_ServerDate,
       n_StartTime,
       n_EndTime,
       n_rowDate,
       n_Grid,
       n_Cores,
       n_ProjectNameCorrid
  FROM JobUsageRecord J
       JOIN JobUsageRecord_Meta M ON (J.dbid = M.dbid)
       JOIN VONameCorrection VC ON
        ((J.VOName = BINARY VC.VOName) AND
         (((J.ReportableVOName IS NULL) AND (VC.ReportableVOName IS NULL))
          OR (BINARY J.ReportableVOName = BINARY VC.ReportableVOName)))
       JOIN ProjectNameCorrection PNC ON
         ((J.ProjectName = BINARY PNC.ProjectName) OR
         (((J.ProjectName IS NULL) AND (PNC.ProjectName IS NULL))
          OR (BINARY J.ProjectName = BINARY PNC.ProjectName)))
       LEFT JOIN Resource EC ON
        ((J.dbid = EC.dbid) AND
         (EC.description = 'ExitCode'))
       LEFT JOIN Resource ES ON
        ((J.dbid = ES.dbid) AND
         (ES.description = 'ExitSignal'))
       LEFT JOIN Resource EBS ON
        ((J.dbid = EBS.dbid) AND
         (EBS.description = 'ExitBySignal'))
       LEFT JOIN Resource JS ON
        ((J.dbid = JS.dbid) AND
         (JS.description = 'condor.JobStatus'))
  WHERE J.dbid = inputDbid;

  -- Basic data checks
  IF n_ResourceType IS NOT NULL AND
     n_ResourceType NOT IN ('Batch', 'RawCPU', 'Storage') THEN
     -- Very common case: no message necessary
     LEAVE DJUR;
  END IF;

  IF n_ProbeName IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', inputDbid, 'Failed due to null ProbeName');
     LEAVE DJUR;
  END IF;

  IF n_VOcorrid IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', inputDbid, 'Failed due to null VOcorrid');
     LEAVE DJUR;
  END IF;

  IF n_ProjectNameCorrid IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', inputDbid, 'Failed due to null ProjectNameCorrid');
     LEAVE DJUR;
  END IF;

  IF n_Njobs IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', inputDbid, 'Failed due to null Njobs');
     LEAVE DJUR;
  END IF;

  IF n_ResourceType = 'Storage' THEN
    IF n_StartTime IS NULL THEN
      INSERT INTO trace(eventtime, procName, p1, sqlQuery)
       VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', inputDbid, 'Failed due to null StartTime');
      LEAVE DJUR;
    END IF;

    SELECT Protocol,
           IF(IsNew = '1', `Source`, `Destination`),
           IsNew,
           Status,
           StorageUnit,
           TransferDuration,
           IF(TransferSize > 0, TransferSize, 0)
    INTO n_Protocol,
         n_RemoteSite,
         n_IsNew,
         n_Status,
         n_StorageUnit,
         n_TransferDuration,
         n_TransferSize
    FROM JobUsageRecord R
         join TDCorr on (R.dbid = TDCorr.dbid)
         join TransferDetails T on (TDCorr.TransferDetailsId = T.TransferDetailsId),
         (SELECT StorageUnit,
                 PhaseUnit as TransferDuration,
                 `Value` as TransferSize
          FROM Network where dbid = inputDbid limit 1) N
    WHERE R.dbid = inputDbid;

    -- Update MasterTransferSummary
    UPDATE MasterTransferSummary
    SET Njobs = Njobs - n_Njobs,
        TransferSize = TransferSize - n_TransferSize,
        TransferDuration = TransferDuration - n_TransferDuration
    WHERE StartTime = n_rowDate
      AND VOcorrid = n_VOcorrid
      AND CommonName = n_CommonName
      AND DistinguishedName = n_DistinguishedName
      AND Protocol = n_Protocol
      AND RemoteSite = n_RemoteSite
      AND Status = n_Status
      AND IsNew = n_IsNew
      AND StorageUnit = n_StorageUnit
      AND ProjectNameCorrid = n_ProjectNameCorrid;

    -- Clean up emptied rows
    DELETE FROM MasterTransferSummary
    WHERE StartTime = n_rowDate
        AND VOcorrid = n_VOcorrid
        AND CommonName = n_CommonName
        AND DistinguishedName = n_DistinguishedName
        AND Protocol = n_Protocol
        AND RemoteSite = n_RemoteSite
        AND Status = n_Status
        AND IsNew = n_IsNew
        AND StorageUnit = n_StorageUnit
        AND ProjectNameCorrid = n_ProjectNameCorrid
        AND Njobs <= 0;

    LEAVE DJUR; -- Done
  END IF;

  IF n_WallDuration IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', inputDbid, 'Failed due to null WallDuration');
     LEAVE DJUR;
  END IF;

  IF n_CpuUserDuration IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', inputDbid, 'Failed due to null CpuUserDuration');
     LEAVE DJUR;
  END IF;

  IF n_CpuSystemDuration IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', inputDbid, 'Failed due to null CpuSystemDuration');
     LEAVE DJUR;
  END IF;

  IF n_EndTime < n_StartTime THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', inputDbid, 'Failed due to EndTime < StartTime');
     LEAVE DJUR;
  END IF;

  -- MasterSummaryData
  UPDATE MasterSummaryData
  SET Njobs = Njobs - n_Njobs,
      WallDuration = WallDuration - n_WallDuration,
      CpuUserDuration = CpuUserDuration - n_CpuUserDuration,
      CpuSystemDuration = CpuSystemDuration - n_CpuSystemDuration
  WHERE EndTime = n_rowdate
    AND VOcorrid = n_VOcorrid
    AND ProbeName = n_ProbeName
    AND CommonName = n_CommonName
    AND DistinguishedName = n_DistinguishedName
    AND ResourceType = n_ResourceType
    AND HostDescription = n_HostDescription
    AND ApplicationExitCode = n_ApplicationExitCode
    AND Grid = n_Grid
    AND Cores = n_Cores
    AND ProjectNameCorrid = n_ProjectNameCorrid;

  -- Clean up emptied rows
  DELETE FROM MasterSummaryData
  WHERE EndTime = n_rowdate
    AND VOcorrid = n_VOcorrid
    AND ProbeName = n_ProbeName
    AND CommonName = n_CommonName
    AND DistinguishedName = n_DistinguishedName
    AND ResourceType = n_ResourceType
    AND HostDescription = n_HostDescription
    AND ApplicationExitCode = n_ApplicationExitCode
    AND Grid = n_Grid
    AND Cores = n_Cores
    AND ProjectNameCorrid = n_ProjectNameCorrid
    AND Njobs <= 0;

  -- NodeSumary
  select cdr into wantNodeSummary from SystemProplist
    where car = 'gratia.database.wantNodeSummary';

  if wantNodeSummary = '1' then
    set mycpucount = 0;
    set mybenchmarkscore = 0;
    select BenchmarkScore, CPUCount
      into mybenchmarkscore, mycpucount from CPUInfo
      where n_HostDescription = CPUInfo.HostDescription;

    set imax = datediff(n_EndTime, n_StartTime) + 1;
    set newcpusystemtime = n_CpuSystemDuration / imax;
    set newcpuusertime = n_CpuUserDuration / imax;
    set counter = 0;
    while counter < imax do
      -- Calculate date for summary entry
      set newdate = adddate(n_StartTime,counter);
      -- Update
      update NodeSummary
      set CpuSystemTime = CpuSystemTime - newcpusystemtime,
          CpuUserTime = CpuUserTime - newcpuusertime
      where EndTime = date(newdate)
        and Node = n_Host
        and ProbeName = n_ProbeName
        and ResourceType = n_ResourceType;

      -- Clean up emptied rows
      delete from NodeSummary
      where EndTime = d_EndTime
        and Node = n_Host
        and ProbeName = n_ProbeName
        and ResourceType = n_ResourceType
        and CpuSystemTime <= 0
        and CpuUserTime <= 0;

      -- Update counter
      set counter = counter + 1;
    end while;
  end if; -- wantNodeSummary

END;
||

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

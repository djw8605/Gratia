DELIMITER ||

DROP PROCEDURE IF EXISTS add_service_to_hourly_summary
||
CREATE PROCEDURE add_service_to_hourly_summary(inputDbid BIGINT(20))
SQL SECURITY INVOKER
DETERMINISTIC
ASERVH:BEGIN
  -- Main
  DECLARE n_ProbeName VARCHAR(255);
  DECLARE n_VOcorrid BIGINT(20);
  DECLARE n_CEName VARCHAR(255);
  DECLARE n_Grid VARCHAR(255);
  DECLARE n_HostName VARCHAR(255);
  DECLARE n_Clustercorrid BIGINT(20);
  DECLARE n_SiteName VARCHAR(255);
  DECLARE n_RunningJobs BIGINT(20);
  DECLARE n_WaitingJobs BIGINT(20);
  DECLARE n_TotalJobs BIGINT(20);
  DECLARE n_RecordTime DATETIME;
  DECLARE n_RecordCount BIGINT(20);
  DECLARE n_CEUniqueID VARCHAR(255);

  -- Variables for the summary table.
  DECLARE n_SummaryTime DATETIME;
  DECLARE n_SummaryRunning BIGINT(20);
  DECLARE n_SummaryWaiting BIGINT(20);
  DECLARE n_SummaryTotal   BIGINT(20);
  DECLARE n_dbid BIGINT(20);

  -- Data collection from the record table
  --
  SELECT CER.ProbeName,
         VC.corrid,
         CER.UniqueID,
         CER.RunningJobs,
         CER.WaitingJobs,
         CER.TotalJobs,
         CER.Timestamp
  INTO n_ProbeName,
       n_VOcorrid,
       n_CEUniqueID,
       n_RunningJobs,
       n_WaitingJobs,
       n_TotalJobs,
       n_RecordTime
  FROM ComputeElementRecord CER
       JOIN VONameCorrection VC ON
        ((BINARY CER.VO = BINARY VC.VOName) AND
         (BINARY CER.VO = BINARY VC.ReportableVOName))
  WHERE CER.dbid = inputDbid
  LIMIT 1;

  -- Basic data checks
  IF n_ProbeName IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_hourly_summary', inputDbid, 'Failed due to null ProbeName');
     LEAVE ASERVH;
  END IF;

  IF n_CEUniqueID IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_hourly_summary', inputDbid, 'Failed due to null CEUniqueID in CE Record');
     LEAVE ASERVH;
  END IF;

  IF n_VOcorrid IS NULL THEN
    INSERT INTO trace(eventtime, procName, p1, sqlQuery)
     VALUES(UTC_TIMESTAMP(), 'add_service_to_hourly_summary', inputDbid, 'Failed due to null VOcorrid');
    LEAVE ASERVH;
  END IF;

  IF n_RecordTime IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_hourly_summary', inputDbid, 'Failed due to null CE Record timestamp');
    LEAVE ASERVH;
  END IF;

  -- Determine the most relevant CE:
  --
  SELECT CE.CEName,
         CE.HostName,
         CNC.corrid,
         CE.SiteName,
         CE.Grid
  INTO n_CEName,
       n_HostName,
       n_Clustercorrid,
       n_SiteName,
       n_Grid
  FROM ComputeElement CE
  JOIN ClusterNameCorrection CNC on CE.Cluster = CNC.ClusterName
  WHERE CE.Timestamp = 
    (SELECT max(Timestamp) from ComputeElement CE2 where CE2.Timestamp < n_RecordTime and CE2.UniqueID = n_CEUniqueID)
    AND CE.UniqueID = n_CEUniqueID 
  LIMIT 1;

  IF n_CEName IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_hourly_summary', inputDbid, 'Failed due to null CE name');
    LEAVE ASERVH;
  END IF;

  IF n_HostName IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_hourly_summary', inputDbid, 'Failed due to null CE hostname');
    LEAVE ASERVH;
  END IF;

  IF n_Clustercorrid IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_hourly_summary', inputDbid, 'Failed due to CE cluster name not being in the correction table.');
    LEAVE ASERVH;
  END IF;

  IF n_SiteName IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_hourly_summary', inputDbid, 'Failed due to null CE site name');
    LEAVE ASERVH;
  END IF;


  -- Determine the time bin we will insert into.
  SELECT DATE_ADD(DATE(n_RecordTime), INTERVAL EXTRACT(HOUR from n_RecordTime) HOUR) INTO n_SummaryTime;
  
  -- Determine the existing summary data.
  --
  set n_RecordCount = 0;
  SELECT MSS.RecordCount,
         MSS.RunningJobs,
         MSS.WaitingJobs,
         MSS.TotalJobs,
         MSS.dbid
  INTO n_RecordCount,
       n_SummaryRunning,
       n_SummaryWaiting,
       n_SummaryTotal,
       n_dbid
  FROM MasterServiceSummaryHourly MSS
  WHERE MSS.Timestamp = n_SummaryTime AND
        MSS.CEUniqueID = n_CEUniqueID AND
        MSS.VOcorrid = n_VOcorrid AND
        MSS.Grid = n_Grid
  LIMIT 1;

-- In this case, we have a new record; add it to the summary and leave
IF n_RecordCount=0 THEN

  INSERT INTO MasterServiceSummaryHourly(Timestamp, ProbeName, CEUniqueID, VOcorrid,
                                         CEName, HostName, Clustercorrid, SiteName, Grid,
                                         RecordCount, RunningJobs, WaitingJobs, TotalJobs)
  VALUES(n_SummaryTime,
         n_ProbeName,
         n_CEUniqueID,
         n_VOcorrid,
         n_CEName,
         n_HostName,
         n_Clustercorrid,
         n_SiteName,
         n_Grid,
         1,
         n_RunningJobs,
         n_WaitingJobs,
         n_TotalJobs);

  LEAVE ASERVH;
END IF;

-- In this case, we have to update an existing summary record.
UPDATE MasterServiceSummaryHourly SET
  RecordCount = n_RecordCount+1,
  RunningJobs = (RunningJobs + n_RunningJobs*n_RecordCount)/(n_RecordCount+1),
  WaitingJobs = (WaitingJobs + n_WaitingJobs*n_RecordCount)/(n_RecordCount+1),
  TotalJobs = (TotalJobs + n_TotalJobs*n_RecordCount)/(n_RecordCount+1)
WHERE
  dbid = n_dbid;

END;
||

DROP PROCEDURE IF EXISTS add_service_to_daily_summary
||
CREATE PROCEDURE add_service_to_daily_summary(inputDbid BIGINT(20))
SQL SECURITY INVOKER
DETERMINISTIC
ASERV:BEGIN
  -- Main
  DECLARE n_ProbeName VARCHAR(255);
  DECLARE n_VOcorrid BIGINT(20);
  DECLARE n_CEName VARCHAR(255);
  DECLARE n_HostName VARCHAR(255);
  DECLARE n_Clustercorrid VARCHAR(255);
  DECLARE n_SiteName VARCHAR(255);
  DECLARE n_Grid VARCHAR(255);
  DECLARE n_RunningJobs BIGINT(20);
  DECLARE n_WaitingJobs BIGINT(20);
  DECLARE n_TotalJobs BIGINT(20);
  DECLARE n_RecordTime DATETIME;
  DECLARE n_RecordCount BIGINT(20);
  DECLARE n_CEUniqueID VARCHAR(255);

  -- Variables for the summary table.
  DECLARE n_SummaryTime DATETIME;
  DECLARE n_SummaryRunning BIGINT(20);
  DECLARE n_SummaryWaiting BIGINT(20);
  DECLARE n_SummaryTotal   BIGINT(20);
  DECLARE n_dbid BIGINT(20);

  -- Data collection from the record table
  --
  SELECT CER.ProbeName,
         VC.corrid,
         CER.UniqueID,
         CER.RunningJobs,
         CER.WaitingJobs,
         CER.TotalJobs,
         CER.Timestamp
  INTO n_ProbeName,
       n_VOcorrid,
       n_CEUniqueID,
       n_RunningJobs,
       n_WaitingJobs,
       n_TotalJobs,
       n_RecordTime
  FROM ComputeElementRecord CER
       JOIN VONameCorrection VC ON
        ((BINARY CER.VO = BINARY VC.VOName) AND
         (BINARY CER.VO = BINARY VC.ReportableVOName))
  WHERE CER.dbid = inputDbid
  LIMIT 1;

  -- Basic data checks
  IF n_ProbeName IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_daily_summary', inputDbid, 'Failed due to null ProbeName');
     LEAVE ASERV;
  END IF;

  IF n_CEUniqueID IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_daily_summary', inputDbid, 'Failed due to null CEUniqueID in CE Record');
     LEAVE ASERV;
  END IF;

  IF n_VOcorrid IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_daily_summary', inputDbid, 'Failed due to null VOcorrid');
     LEAVE ASERV;
  END IF;

  IF n_RecordTime IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_daily_summary', inputDbid, 'Failed due to null CE Record timestamp');
    LEAVE ASERV;
  END IF;

  -- Determine the most relevant CE:
  --
  SELECT CE.CEName,
         CE.HostName,
         CNC.corrid,
         CE.SiteName,
         CE.Grid
  INTO n_CEName,
       n_HostName,
       n_Clustercorrid,
       n_SiteName,
       n_Grid
  FROM ComputeElement CE
  JOIN ClusterNameCorrection CNC on CE.Cluster = CNC.ClusterName
  WHERE CE.Timestamp = 
    (SELECT max(Timestamp) from ComputeElement CE2 where CE2.Timestamp < n_RecordTime and CE2.UniqueID = n_CEUniqueID)
	AND CE.UniqueID = n_CEUniqueID
  LIMIT 1;

  IF n_CEName IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_daily_summary', inputDbid, 'Failed due to null CE name');
    LEAVE ASERV;
  END IF;

  IF n_HostName IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_daily_summary', inputDbid, 'Failed due to null CE hostname');
    LEAVE ASERV;
  END IF;

  IF n_Clustercorrid IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_daily_summary', inputDbid, 'Failed due to CE cluster name not being in the correction table');
    LEAVE ASERV;
  END IF;

  IF n_SiteName IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'add_service_to_daily_summary', inputDbid, 'Failed due to null CE site name');
    LEAVE ASERV;
  END IF;


  -- Determine the time bin we will insert into.
  -- 
  SELECT DATE(n_RecordTime) INTO n_SummaryTime;

  -- Determine the existing summary data.
  --
  set n_RecordCount = 0;
  SELECT MSS.RecordCount,
         MSS.RunningJobs,
         MSS.WaitingJobs,
         MSS.TotalJobs,
         MSS.dbid
  INTO n_RecordCount,
       n_SummaryRunning,
       n_SummaryWaiting,
       n_SummaryTotal,
       n_dbid
  FROM MasterServiceSummary MSS
  WHERE MSS.Timestamp = n_SummaryTime AND
        MSS.CEUniqueID = n_CEUniqueID AND
        MSS.VOcorrid = n_VOcorrid AND
        MSS.Grid = n_Grid
  LIMIT 1;

-- In this case, we have a new record; add it to the summary and leave
IF n_RecordCount=0 THEN

  INSERT INTO MasterServiceSummary(Timestamp, ProbeName, CEUniqueID, VOcorrid,
                                    CEName, HostName, Clustercorrid, SiteName, Grid,
                                    RecordCount, RunningJobs, WaitingJobs, TotalJobs)
  VALUES(n_SummaryTime,
         n_ProbeName,
         n_CEUniqueID,
         n_VOcorrid,
         n_CEName,
         n_HostName,
         n_Clustercorrid,
         n_SiteName,
         n_Grid,
         1,
         n_RunningJobs,
         n_WaitingJobs,
         n_TotalJobs);

  LEAVE ASERV;
END IF;

-- In this case, we have to update an existing summary record.

UPDATE MasterServiceSummary SET
  RecordCount = n_RecordCount+1,
  RunningJobs = (RunningJobs + n_RunningJobs*n_RecordCount)/(n_RecordCount+1),
  WaitingJobs = (WaitingJobs + n_WaitingJobs*n_RecordCount)/(n_RecordCount+1),
  TotalJobs = (TotalJobs + n_TotalJobs*n_RecordCount)/(n_RecordCount+1)
WHERE
  dbid = n_dbid;

END;
||

-- Note: THIS ASSUMES THAT CEUNIQUEID IS UNIQUE THROUGHOUT ALL GRID TYPES

DROP PROCEDURE IF EXISTS del_service_from_hourly_summary
||
CREATE PROCEDURE del_service_from_hourly_summary(inputDbid BIGINT(20))
SQL SECURITY INVOKER
DETERMINISTIC
DSERVH:BEGIN
  -- Main
  DECLARE n_ProbeName VARCHAR(255);
  DECLARE n_VOcorrid BIGINT(20);
  DECLARE n_CEUniqueID VARCHAR(255);
  DECLARE n_RunningJobs BIGINT(20);
  DECLARE n_WaitingJobs BIGINT(20);
  DECLARE n_TotalJobs BIGINT(20);
  DECLARE n_RecordTime DATETIME;
  DECLARE n_SummaryTime DATETIME;
  DECLARE n_dbid BIGINT(20);
  DECLARE n_RecordCount BIGINT(20);

  -- Data collection from the record table
  --
  SELECT CER.ProbeName,
         VC.corrid,
         CER.UniqueID,
         CER.RunningJobs,
         CER.WaitingJobs,
         CER.TotalJobs,
         CER.Timestamp
  INTO n_ProbeName,
       n_VOcorrid,
       n_CEUniqueID,
       n_RunningJobs,
       n_WaitingJobs,
       n_TotalJobs,
       n_RecordTime
  FROM ComputeElementRecord CER
       JOIN VONameCorrection VC ON
        ((BINARY CER.VO = BINARY VC.VOName) AND
         (BINARY CER.VO = BINARY VC.ReportableVOName))
  WHERE CER.dbid = inputDbid
  LIMIT 1;

  -- Basic data validation
  IF n_ProbeName IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_service_from_hourly_summary', inputDbid, 'Failed due to null ProbeName');
     LEAVE DSERVH;
  END IF;

  IF n_VOcorrid IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_service_from_hourly_summary', inputDbid, 'Failed due to null VOcorrid');
     LEAVE DSERVH;
  END IF;

  IF n_CEUniqueID IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_service_from_hourly_summary', inputDbid, 'Failed due to null CE Unique ID');
     LEAVE DSERVH;
  END IF;

  IF n_RecordTime IS NULL THEN
    INSERT INTO trace(eventtime, procName, p1, sqlQuery)
     VALUES(UTC_TIMESTAMP(), 'del_service_from_hourly_summary', inputDbid, 'Failed due to null CE Record timestamp');
    LEAVE DSERVH;
  END IF;

  -- Determine the time bin for the summary table.
  SELECT DATE_ADD(DATE(n_RecordTime), INTERVAL EXTRACT(HOUR from n_RecordTime) HOUR) INTO n_SummaryTime;

  -- Collect the dbid, totals, and record count from the summary table.
  set n_RecordCount = 0;
  SELECT MSS.dbid,
         MSS.RunningJobs,
         MSS.WaitingJobs,
         MSS.TotalJobs,
         MSS.RecordCount
  INTO n_dbid,
       n_RunningJobs,
       n_WaitingJobs,
       n_TotalJobs,
       n_RecordCount
  FROM MasterServiceSummaryHourly MSS
  WHERE CEUniqueID=n_CEUniqueID AND Timestamp = n_SummaryTime AND VOcorrid=n_VOcorrid
  LIMIT 1;

  -- No corresponding table entry, error!
  IF n_RecordCount = 0 THEN
    INSERT INTO trace(eventtime, procName, p1, sqlQuery)
     VALUES(UTC_TIMESTAMP(), 'del_service_from_hourly_summary', inputDbid, 'Failed due to no corresponding summary entry.');
    LEAVE DSERVH;
  END IF;

  -- This is the last record in the summary entry; delete the whole thing.
  IF n_RecordCount <= 1 THEN
    DELETE FROM MasterServiceSummaryHourly WHERE dbid=n_dbid;
    LEAVE DSERVH;
  END IF;

  -- Otherwise, we update the averages.
  UPDATE MasterServiceSummaryHourly SET
    RecordCount = n_RecordCount-1,
    RunningJobs = (n_RecordCount * RunningJobs - n_RunningJobs) / (n_RecordCount-1),
    WaitingJobs = (n_RecordCount * WaitingJobs - n_WaitingJobs) / (n_RecordCount-1),
    TotalJobs = (n_RecordCount * TotalJobs - n_TotalJobs) / (n_RecordCount-1)
  WHERE dbid = n_dbid;

END;
||

-- Note: THIS ASSUMES THAT CEUNIQUEID IS UNIQUE THROUGHOUT ALL GRID TYPES

DROP PROCEDURE IF EXISTS del_service_from_daily_summary
||
CREATE PROCEDURE del_service_from_daily_summary(inputDbid BIGINT(20))
SQL SECURITY INVOKER
DETERMINISTIC
DSERV:BEGIN
  -- Main
  DECLARE n_ProbeName VARCHAR(255);
  DECLARE n_VOcorrid BIGINT(20);
  DECLARE n_CEUniqueID VARCHAR(255);
  DECLARE n_RunningJobs BIGINT(20);
  DECLARE n_WaitingJobs BIGINT(20);
  DECLARE n_TotalJobs BIGINT(20);
  DECLARE n_RecordTime DATETIME;
  DECLARE n_SummaryTime DATETIME;
  DECLARE n_dbid BIGINT(20);
  DECLARE n_RecordCount BIGINT(20);

  -- Data collection from the record table
  --
  SELECT CER.ProbeName,
         VC.corrid,
         CER.UniqueID,
         CER.RunningJobs,
         CER.WaitingJobs,
         CER.TotalJobs,
         CER.Timestamp
  INTO n_ProbeName,
       n_VOcorrid,
       n_CEUniqueID,
       n_RunningJobs,
       n_WaitingJobs,
       n_TotalJobs,
       n_RecordTime
  FROM ComputeElementRecord CER
       JOIN VONameCorrection VC ON
        ((BINARY CER.VO = BINARY VC.VOName) AND
         (BINARY CER.VO = BINARY VC.ReportableVOName))
  WHERE CER.dbid = inputDbid
  LIMIT 1;

  -- Basic data validation
  IF n_ProbeName IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_service_from_daily_summary', inputDbid, 'Failed due to null ProbeName');
     LEAVE DSERV;
  END IF;

  IF n_VOcorrid IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_service_from_daily_summary', inputDbid, 'Failed due to null VOcorrid');
     LEAVE DSERV;
  END IF;

  IF n_CEUniqueID IS NULL THEN
     INSERT INTO trace(eventtime, procName, p1, sqlQuery)
      VALUES(UTC_TIMESTAMP(), 'del_service_from_daily_summary', inputDbid, 'Failed due to null CE Unique ID');
     LEAVE DSERV;
  END IF;

  IF n_RecordTime IS NULL THEN
    INSERT INTO trace(eventtime, procName, p1, sqlQuery)
     VALUES(UTC_TIMESTAMP(), 'del_service_from_daily_summary', inputDbid, 'Failed due to null CE Record timestamp');
    LEAVE DSERV;
  END IF;

  -- Determine the time bin for the summary table.
  SELECT DATE(n_RecordTime) INTO n_SummaryTime;

  -- Collect the dbid, totals, and record count from the summary table.
  set n_RecordCount = 0;
  SELECT MSS.dbid,
         MSS.RunningJobs,
         MSS.WaitingJobs,
         MSS.TotalJobs,
         MSS.RecordCount
  INTO n_dbid,
       n_RunningJobs,
       n_WaitingJobs,
       n_TotalJobs,
       n_RecordCount
  FROM MasterServiceSummary MSS
  WHERE CEUniqueID=n_CEUniqueID AND Timestamp = n_SummaryTime AND VOcorrid=n_VOcorrid
  LIMIT 1;

  -- No corresponding table entry, error!
  IF n_RecordCount = 0 THEN
    INSERT INTO trace(eventtime, procName, p1, sqlQuery)
     VALUES(UTC_TIMESTAMP(), 'del_service_from_daily_summary', inputDbid, 'Failed due to no corresponding summary entry.');
    LEAVE DSERV;
  END IF;

  -- This is the last record in the summary entry; delete the whole thing.
  IF n_RecordCount <= 1 THEN
    DELETE FROM MasterServiceSummary WHERE dbid=n_dbid;
    LEAVE DSERV;
  END IF;

  -- Otherwise, we update the averages.
  UPDATE MasterServiceSummary SET
    RecordCount = n_RecordCount-1,
    RunningJobs = (n_RecordCount * RunningJobs - n_RunningJobs) / (n_RecordCount-1),
    WaitingJobs = (n_RecordCount * WaitingJobs - n_WaitingJobs) / (n_RecordCount-1),
    TotalJobs = (n_RecordCount * TotalJobs - n_TotalJobs) / (n_RecordCount-1)
  WHERE dbid = n_dbid;

END;
||

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

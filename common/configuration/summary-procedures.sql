DELIMITER ||

DROP PROCEDURE IF EXISTS add_JUR_to_summary
||
CREATE PROCEDURE add_JUR_to_summary(input_dbid INT(11))
SP:BEGIN
  -- Main
  DECLARE n_ProbeName VARCHAR(255);
  DECLARE n_CommonName VARCHAR(255);
  DECLARE n_VOcorrid INT(11);
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

  -- NodeSummary update only
  DECLARE n_Host text;
  DECLARE mycount INT(11);
  DECLARE startdate DATETIME;
  DECLARE enddate DATETIME;
  DECLARE node VARCHAR(255);
  DECLARE myprobename VARCHAR(255);
  DECLARE myresourcetype VARCHAR(255);
  DECLARE mycpuusertime INT DEFAULT 0;
  DECLARE mycpusystemtime INT DEFAULT 0;
  DECLARE mycpucount INT DEFAULT 0;
  DECLARE myhostdescription VARCHAR(255);
  DECLARE mybenchmarkscore INT DEFAULT 0;
  DECLARE divide INT DEFAULT 0;
  DECLARE counter INT DEFAULT 0;  
  DECLARE newdate DATETIME;
  DECLARE newcpusystemtime INT DEFAULT 0;
  DECLARE newcpuusertime INT DEFAULT 0;
  DECLARE numberofdays INT DEFAULT 0;
  DECLARE imax INT DEFAULT 0;

  -- Data collection
  SELECT M.ProbeName,
         IFNULL(J.CommonName, ''),
         VC.corrid,
         IFNULL(J.ResourceType, ''),
         IFNULL(J.HostDescription, ''),
         IFNULL(J.Host, ''),
         IFNULL(IFNULL(RT.value, J.Status), 0),
         J.Njobs,
         J.WallDuration,
         J.CpuUserDuration,
         J.CpuSystemDuration,
         M.ServerDate,
         J.StartTime,
         J.EndTime
  INTO n_ProbeName,
       n_CommonName,
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
       n_EndTime
  FROM JobUsageRecord J
       JOIN JobUsageRecord_Meta M ON (J.dbid = M.dbid)
       JOIN VONameCorrection VC ON
        ((J.VOName = BINARY VC.VOName) AND
         (((J.ReportableVOName IS NULL) AND (VC.ReportableVOName IS NULL))
          OR (BINARY J.ReportableVOName = BINARY VC.ReportableVOName)))
       LEFT JOIN Resource RT ON
        ((J.dbid = RT.dbid) AND
         (RT.description = 'ExitCode'))
  WHERE J.dbid = input_dbid;

  -- Basic data checks
  IF n_ResourceType IS NOT NULL AND
     n_ResourceType NOT IN ('Batch', 'RawCPU') THEN
     -- Very common case: no message necessary
     LEAVE SP;
  END IF;

  IF n_ProbeName IS NULL THEN
     INSERT INTO trace(eventtime, pname, p1, `data`)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', input_dbid, 'Failed due to null ProbeName');
     LEAVE SP;
  END IF;

  IF n_VOcorrid IS NULL THEN
     INSERT INTO trace(eventtime, pname, p1, `data`)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', input_dbid, 'Failed due to null VOcorrid');
     LEAVE SP;
  END IF;

  IF n_Njobs IS NULL THEN
     INSERT INTO trace(eventtime, pname, p1, `data`)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', input_dbid, 'Failed due to null Njobs');
     LEAVE SP;
  END IF;

  IF n_WallDuration IS NULL THEN
     INSERT INTO trace(eventtime, pname, p1, `data`)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', input_dbid, 'Failed due to null WallDuration');
     LEAVE SP;
  END IF;

  IF n_CpuUserDuration IS NULL THEN
     INSERT INTO trace(eventtime, pname, p1, `data`)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', input_dbid, 'Failed due to null CpuUserDuration');
     LEAVE SP;
  END IF;

  IF n_CpuSystemDuration IS NULL THEN
     INSERT INTO trace(eventtime, pname, p1, `data`)
      VALUES(UTC_TIMESTAMP(), 'add_JUR_to_summary', input_dbid, 'Failed due to null CpuSystemDuration');
     LEAVE SP;
  END IF;

  -- MasterSummaryData
  INSERT INTO MasterSummaryData
  VALUES(DATE(n_EndTime),
         n_VOcorrid,
         n_ProbeName,
         n_CommonName,
         n_ResourceType,
         n_HostDescription,
         n_ApplicationExitCode,
         n_Njobs,
         n_WallDuration,
         n_CpuUserDuration,
         n_CpuSystemDuration)
  ON DUPLICATE KEY UPDATE
   Njobs = Njobs + VALUES(Njobs),
   WallDuration = WallDuration + VALUES(WallDuration),
   CpuUserDuration = CpuUserDuration + VALUES(CpuUserDuration),
   CpuSystemDuration = CpuSystemDuration + VALUES(CpuSystemDuration);

  -- NodeSummary: remains old-style due to its complication.
  select count(*) into mycount from information_schema.tables where
    table_schema = Database() and
    table_name = 'NodeSummary';

  if mycount > 0 then

    set startdate = n_StartTime;
    set enddate = n_EndTime;
    set node = n_Host;
    set myprobename = n_ProbeName;
    set myresourcetype = n_ResourceType;
    set mycpuusertime = n_CpuUserDuration;
    set mycpusystemtime = n_CpuSystemDuration;
    set myhostdescription = n_HostDescription;
  
    if myprobename = null then
      set myprobename = 'Unknown';
    end if;

    begin
      set mycpucount = 0;
      set mybenchmarkscore = 0;
      select BenchmarkScore,CPUCount
        into mybenchmarkscore,mycpucount from CPUInfo
        where myhostdescription = CPUInfo.HostDescription;
    end;

    set numberofdays = datediff(enddate,startdate);
    set divide = numberofdays + 1;
    set newcpusystemtime = mycpusystemtime / divide;
    set newcpuusertime = mycpuusertime / divide;

--    insert into trace(eventtime,pname,p1,p2,data)
--     values(UTC_TIMESTAMP(), 'add_JUR_to_summary', n_dbid, numberofdays, 'Calling updatenodesummary');

    if numberofdays = 0 then
        call updatenodesummary(
        date(enddate),node,myprobename,myresourcetype,
        mycpusystemtime,mycpuusertime,mycpucount,myhostdescription,
        mybenchmarkscore,extract(DAY from last_day(enddate)));
    end if;

    if numberofdays > 0 then
      set imax = numberofdays + 1;
      set counter = 0;
      while counter < imax do
        set newdate = adddate(startdate,counter);
        call updatenodesummary(
          date(newdate),node,myprobename,myresourcetype,
          newcpusystemtime,newcpuusertime,mycpucount,
          myhostdescription,mybenchmarkscore,
          extract(DAY from last_day(newdate)));
        set counter = counter + 1;
      end while;
    end if;
  end if;
END;
||
DROP PROCEDURE IF EXISTS del_JUR_from_summary
||
CREATE PROCEDURE del_JUR_from_summary(input_dbid INT(11))
SP:BEGIN
  -- Main
  DECLARE n_ProbeName VARCHAR(255);
  DECLARE n_CommonName VARCHAR(255);
  DECLARE n_VOcorrid INT(11);
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
  DECLARE ps_ServerDate DATETIME;
  DECLARE d_EndTime DATE;

  -- Data collection
  SELECT M.ProbeName,
         J.CommonName,
         VC.corrid,
         J.ResourceType,
         J.HostDescription,
         IFNULL(RT.value, J.Status),
         J.Njobs,
         J.WallDuration,
         J.CpuUserDuration,
         J.CpuSystemDuration,
         M.ServerDate,
         J.StartTime,
         J.EndTime,
         STR_TO_DATE(DATE_FORMAT(M.ServerDate, '%Y-%c-%e %H:00:00'),
                     '%Y-%c-%e %H:00:00'),
         DATE(J.EndTime)
  INTO n_ProbeName,
       n_CommonName,
       n_VOcorrid,
       n_ResourceType,
       n_HostDescription,
       n_ApplicationExitCode,
       n_Njobs,
       n_WallDuration,
       n_CpuUserDuration,
       n_CpuSystemDuration,
       n_ServerDate,
       n_StartTime,
       n_EndTime,
       ps_ServerDate,
       d_EndTime
  FROM JobUsageRecord J
       JOIN JobUsageRecord_Meta M ON (J.dbid = M.dbid)
       JOIN VONameCorrection VC ON
        ((J.VOName = BINARY VC.VOName) AND
         (((J.ReportableVOName IS NULL) AND (VC.ReportableVOName IS NULL))
          OR (BINARY J.ReportableVOName = BINARY VC.ReportableVOName)))
       LEFT JOIN Resource RT ON
        ((J.dbid = RT.dbid) AND
         (RT.description = 'ExitCode'))
  WHERE J.dbid = input_dbid;

  -- Basic data checks
  IF n_ProbeName IS NULL THEN
     INSERT INTO trace(eventtime, pname, p1, `data`)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', input_dbid, 'Failed due to null ProbeName');
     LEAVE SP;
  END IF;

  IF n_VOcorrid IS NULL THEN
     INSERT INTO trace(eventtime, pname, p1, `data`)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', input_dbid, 'Failed due to null VOcorrid');
     LEAVE SP;
  END IF;

  IF n_Njobs IS NULL THEN
     INSERT INTO trace(eventtime, pname, p1, `data`)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', input_dbid, 'Failed due to null Njobs');
     LEAVE SP;
  END IF;

  IF n_WallDuration IS NULL THEN
     INSERT INTO trace(eventtime, pname, p1, `data`)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', input_dbid, 'Failed due to null WallDuration');
     LEAVE SP;
  END IF;

  IF n_CpuUserDuration IS NULL THEN
     INSERT INTO trace(eventtime, pname, p1, `data`)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', input_dbid, 'Failed due to null CpuUserDuration');
     LEAVE SP;
  END IF;

  IF n_CpuSystemDuration IS NULL THEN
     INSERT INTO trace(eventtime, pname, p1, `data`)
      VALUES(UTC_TIMESTAMP(), 'del_JUR_from_summary', input_dbid, 'Failed due to null CpuSystemDuration');
     LEAVE SP;
  END IF;

  -- MasterSummaryData
  UPDATE MasterSummaryData
  SET Njobs = Njobs - n_Njobs,
      WallDuration = WallDuration - n_WallDuration,
      CpuUserDuration = CpuUserDuration - n_CpuUserDuration,
      CpuSystemDuration = CpuSystemDuration - n_CpuSystemDuration
  WHERE EndTime = d_EndTime
    AND VOcorrid = n_VOcorrid
    AND ProbeName = n_ProbeName
    AND CommonName = IFNULL(n_CommonName, '')
    AND ResourceType = IFNULL(n_ResourceType, '')
    AND HostDescription = IFNULL(n_HostDescription, '')
    AND ApplicationExitCode = n_ApplicationExitCode;

 -- Clean up emptied rows (may check multiple rows, but trying to be
 -- economical with dynamically evaluated expressions (like IFNULL).
 DELETE FROM MasterSummaryData
  WHERE EndTime = d_EndTime
    AND VOcorrid = n_VOcorrid
    AND ProbeName = n_Probename
    AND ApplicationExitCode = n_ApplicationExitCode
    AND Njobs <= 0;

  -- Don't do anything for NodeSummary -- AT YOUR OWN RISK.

END;

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

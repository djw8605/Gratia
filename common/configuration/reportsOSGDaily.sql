DELIMITER $$

DROP PROCEDURE IF EXISTS `reportsOSGDaily` $$
CREATE PROCEDURE `reportsOSGDaily`
(
 userName VARCHAR(64),
 userRole VARCHAR(64),
 fromdate VARCHAR(64),
 todate VARCHAR(64),
 timeUnit VARCHAR(64),
 dateGrouping VARCHAR(64),
 groupBy VARCHAR(128),
 orderBy VARCHAR(128),
 resourceType VARCHAR(64),
 VOs VARCHAR(1024),
 Sites VARCHAR(1024),
 Probes VARCHAR(1024),
 Ranked VARCHAR(5),
 selType VARCHAR(5),
 reportName VARCHAR(64)
) READS SQL DATA
BEGIN

  SELECT SYSDATE() INTO @proc_start;
  SET @iuser := CONCAT_WS('', 'GratiaUser|', UNIX_TIMESTAMP(), '|Unknown');
  IF userName IS NOT NULL THEN
    IF (TRIM(userName) != '') AND (TRIM(userName) != 'null') THEN
      SET @iuser := TRIM(userName);
    END IF;
  END IF;

  SET @irole := 'GratiaUser';
  IF userRole IS NOT NULL THEN
    IF (TRIM(userRole) != '') AND (TRIM(userRole) != 'null') THEN
      SET @irole := TRIM(userRole);
    END IF;
  END IF;

  SET @idatr := '%Y-%m-%d';
  SET @idats := '%Y-%m-%d';
  SET @igroup := 'day';
  IF dateGrouping IS NOT NULL THEN
    IF (TRIM(dateGrouping) != '') AND (TRIM(dateGrouping) != 'null') THEN
      SET @igroup := LOWER(TRIM(dateGrouping));
      IF STRCMP(@igroup, 'day') = 0 THEN
        SET @idatr := '%Y-%m-%d';
        SET @idats := '%Y-%m-%d';
      ELSEIF STRCMP(@igroup, 'week') = 0 THEN
        SET @idatr := '%x-%v Monday';
        SET @idats := '%x-%v %W';
      ELSEIF STRCMP(@igroup, 'month') = 0  THEN
        SET @idatr := '%Y-%m-01';
        SET @idats := '%Y-%m-%d';
      ELSEIF STRCMP(@igroup, 'year') = 0  THEN
        SET @idatr := '%Y-01-01';
        SET @idats := '%Y-%m-%d';
      END IF;
    END IF;
  END IF;

  SET @thisFromDate := ''; 
  SET @ifrom := CONCAT_WS('', ' JR.EndTime >= ''', SUBDATE(CURDATE(), 800),'''');
  IF fromdate IS NOT NULL THEN
    IF (TRIM(fromdate) != '') AND (TRIM(fromdate) != 'null') THEN
      SELECT STR_TO_DATE(fromdate, '%M %e, %Y') into @testDate;
      IF @testDate IS NOT NULL THEN
        SELECT DATE_FORMAT(@testDate, '%Y-%m-%d') into @thisFromDate;
      ELSE
        SET @thisFromDate := fromdate;
      END IF;
      IF (todate IS NULL) OR (TRIM(todate) = '') OR (TRIM(todate) = 'null') THEN
        IF STRCMP(@igroup, 'day') = 0 THEN
        	SET @ifrom := CONCAT_WS('', ' JR.EndTime = ''', TRIM(@thisFromDate), '''');
        ELSE
        	SET @ifrom := CONCAT_WS('', ' STR_TO_DATE(DATE_FORMAT(JobUsageRecord_Report.EndTime,''', @idatr, '''), ''', @idats, ''') = ''', TRIM(@thisFromDate), '''');
        END IF;
      ELSE
        SET @ifrom := CONCAT_WS('', ' JR.EndTime >= ''', TRIM(@thisFromDate), '''');
      END IF;
    END IF;
  END IF;

  SET @thisToDate := ''; 
  SET @ito := '';
  IF todate IS NOT NULL THEN
    IF (TRIM(todate) != '') AND (TRIM(todate) != 'null') THEN
      SELECT STR_TO_DATE(todate, '%M %e, %Y') into @testDate;
      IF @testDate IS NOT NULL THEN
        SELECT DATE_FORMAT(@testDate, '%Y-%m-%d') into @thisToDate;
      ELSE
        SET @thisToDate := todate;
      END IF;
      SET @ito := CONCAT_WS('', 'AND JR.EndTime <= ''', TRIM(@thisToDate),'''');
    END IF;
  END IF;

  SET @iunit := '3600';
  IF timeUnit IS NOT NULL THEN
    IF (TRIM(timeUnit) != '') AND (TRIM(timeUnit) != 'null') THEN
      SET @iunit := TRIM(timeUnit);
    END IF;
  END IF;

  SET @igroupBy := 'GROUP BY DateValue, UserName, SiteName, VOName';
  IF groupBy IS NOT NULL THEN
    IF TRIM(groupBy) != 'null' THEN
      SET @igroupBy := CONCAT_WS('', 'GROUP BY ', TRIM(groupBy));
    END IF;
  END IF;

  SET @iorderBy := 'ORDER BY UserName, SiteName, VOName, DateValue';
  IF orderBy IS NOT NULL THEN
    IF TRIM(orderBy) != 'null' THEN
      SET @iorderBy := CONCAT_WS('', 'ORDER BY ', TRIM(orderBy));
    END IF;
  END IF;

  SET @itype := 'Batch';
  IF resourceType IS NOT NULL THEN
    IF (TRIM(resourceType) != '') AND (TRIM(resourceType) != 'null') THEN
      SET @itype := TRIM(resourceType);
    END IF;
  END IF;
  
  SET @iselTypeA := '=''';
  SET @iselTypeB := '''';
  IF selType IS NOT NULL THEN
    IF selType = 'NOT' THEN
      SET @iselTypeA := ' NOT IN ';
      SET @iselTypeB := '';
    ELSEIF selType = 'IN' THEN
      SET @iselTypeA := ' IN ';
      SET @iselTypeB := '';
    END IF;
  END IF;

  SET @iVOs := '';
  SET @rVOs := '';
  IF VOs IS NOT NULL THEN
    IF (TRIM(VOs) != '') AND (TRIM(VOs) != 'null') THEN
      SET @iVOs := CONCAT_WS('', 'AND JR.VOName', @iselTypeA, '', TRIM(VOs), @iselTypeB, '');
      SET @rVOs := CONCAT_WS('', 'AND rJR.VOName', @iselTypeA, '', TRIM(VOs), @iselTypeB, '');
    END IF;
  END IF;

  SET @iSites := '';
  SET @rSites := '';
  IF Sites IS NOT NULL THEN
    IF (TRIM(Sites) != '') AND (TRIM(Sites) != 'null') THEN
      SET @iSites := CONCAT_WS('', 'AND JR.ReportedSiteName', @iselTypeA, '', TRIM(Sites), @iselTypeB, '');
      SET @rSites := CONCAT_WS('', 'AND rJR.ReportedSiteName', @iselTypeA, '', TRIM(Sites), @iselTypeB, '');
    END IF;
  END IF;

  SET @iProbes := '';
  SET @iProbes := '';
  IF Probes IS NOT NULL THEN
    IF (TRIM(Probes) != '') AND (TRIM(Probes) != 'null') THEN
      SET @iProbes := CONCAT_WS('', 'AND JR.ProbeName', @iselTypeA, '', TRIM(Probes), @iselTypeB, '');
      SET @rProbes := CONCAT_WS('', 'AND rJR.ProbeName', @iselTypeA, '', TRIM(Probes), @iselTypeB, '');
    END IF;
  END IF;

  SELECT generateResourceTypeClause(@itype) INTO @myresourceclause;
  SELECT SystemProplist.cdr INTO @usereportauthentication FROM SystemProplist WHERE SystemProplist.car = 'use.report.authentication';
  SELECT Role.whereclause INTO @mywhereclause FROM Role WHERE Role.role = @irole;
  SELECT generateWhereClause(@iuser,@irole,@mywhereclause) INTO @mywhereclause;
  call parse(@iuser,@name,@key,@vo);

  SET @rankfrom := '';
  SET @rankwhere := '';
  SET @final_rank := '';
  IF Ranked IS NOT NULL THEN
    IF (TRIM(Ranked) != '') AND (TRIM(Ranked) != 'null') THEN
      SET @rankfrom := CONCAT_WS('',
           ' (SELECT @rank:=@rank+1 AS final_rank, sitenamex, walldurationx',
           '   FROM (SELECT @rank:=0 AS rank,',
           '      rJR.ReportedSiteName AS sitenamex,',
           '      rJR.EndTime AS endtimex,',
           '      SUM(rJR.WallDuration)/', @iunit, ' AS walldurationx',
           '        FROM JobUsageRecord_Report rJR',
           '        WHERE (rJR.EndTime) >= (''', @thisFromDate, ''')',
           '        AND (rJR.EndTime) <= (''', @thisToDate, ''')',
           ' ', @rVOs,
           ' ', @rSites,
           ' ', @rProbes,
           '   GROUP BY sitenamex',
           '   ORDER BY walldurationx desc) AS foox) AS foo', ','
           );
       SET @rankwhere := ' JR.ReportedSiteName = sitenamex AND ';
       SET @final_rank := 'final_rank,';
    END IF;
  END IF;

  SET @sql := CONCAT_WS('',
           'SELECT ', @final_rank,
           ' JR.ReportedSiteName AS SiteName,',
           ' SUM(JR.WallDuration)/', @iunit, ' AS WallDuration,',
           ' SUM(JR.CpuUserDuration + JR.CpuSystemDuration)/', @iunit, ' AS cpu,',
           ' SUM(JR.CpuUserDuration)/', @iunit, ' AS cpuUser,',
           ' SUM(JR.CpuSystemDuration)/', @iunit, ' AS cpuSystem,',
           ' SUM(JR.Njobs) AS Njobs,',
           ' JR.VOName AS VOName,',
           ' JR.CommonName AS UserName,',
           ' STR_TO_DATE(DATE_FORMAT(JR.EndTime,''', @idatr, '''), ''', @idats, ''') AS  DateValue',
           ' FROM ', @rankfrom, 'JobUsageRecord_Report JR',
           ' WHERE ',
           ' ', @rankwhere,
           ' ', @ifrom,
           ' ', @ito,
           ' ', @iVOs,
           ' ', @iSites,
           ' ', @iProbes,
           ' ', @myresourceclause,
           ' ', @mywhereclause,
           ' ', @igroupBy,
           ' ', @iorderBy,
           ';'
        );

  SELECT SYSDATE() INTO @query_start;
  PREPARE STATEMENT FROM @sql;
  EXECUTE STATEMENT;
  SELECT SYSDATE() INTO @query_end;

  INSERT INTO
   trace(procName,
         userKey,
         userName,
         userRole,
         userVO,
         sqlQuery,
         procTime,
         queryTime)
  VALUES (TRIM(reportName),
          @key,
          @iuser,
          @irole,
          @vo,
          @sql,
          timediff(@query_start, @proc_start),
          timediff(@query_end, @query_start));              

  DEALLOCATE PREPARE STATEMENT;

END $$

DELIMITER ;
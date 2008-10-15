DELIMITER $$

DROP PROCEDURE IF EXISTS `reportsRanked` $$
CREATE PROCEDURE `reportsRanked`
(
 userName VARCHAR(64),
 userRole VARCHAR(64),
 fromdate VARCHAR(64),
 todate VARCHAR(64),
 timeUnit VARCHAR(64),
 dateGrouping VARCHAR(64),
 resourceType VARCHAR(64),
 metric VARCHAR(15),
 selValues VARCHAR(1024),
 selType VARCHAR(5),
 reportName VARCHAR(64)
) READS SQL DATA
BEGIN

 SELECT SYSDATE() INTO @proc_start;

  SET @iuser := CONCAT_WS('','GratiaUser|', UNIX_TIMESTAMP(), '|Unknown');
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

  SET @thisFromDate := ''; 
  IF fromdate IS NOT NULL THEN
    IF (TRIM(fromdate) != '') AND (TRIM(fromdate) != 'null') THEN
      SELECT STR_TO_DATE(fromdate, '%M %e, %Y') INTO @testDate;
      IF @testDate IS NOT NULL THEN
        SELECT DATE_FORMAT(@testDate, '%Y-%m-%d') INTO @thisFromDate;
      ELSE
        SET @thisFromDate := fromdate;
      END IF;
    END IF;
  END IF;

  SET @thisToDate := ''; 
  IF todate IS NOT NULL THEN
    IF (TRIM(todate) != '') AND (TRIM(todate) != 'null') THEN
      SELECT STR_TO_DATE(todate, '%M %e, %Y') INTO @testDate;
      IF @testDate IS NOT NULL THEN
        SELECT DATE_FORMAT(@testDate, '%Y-%m-%d') INTO @thisToDate;
      ELSE
        SET @thisToDate := todate;
      END IF;
    END IF;
  ELSE
    SET @thisToDate := @thisFromDate;
  END IF;


  SET @iunit := '3600';
  IF timeUnit IS NOT NULL THEN
    IF (TRIM(timeUnit) != '') AND (TRIM(timeUnit) != 'null') THEN
      SET @iunit := TRIM(timeUnit);
    END IF;
  END IF;


  SET @idatr := '%Y-%m-%d';
  SET @idats := '%Y-%m-%d';
  SET @igroup := 'day';
  IF dateGrouping IS NOT NULL THEN
    IF (TRIM(dateGrouping) != '') AND (TRIM(dateGrouping) != 'null') THEN
      SET @igroup := TRIM(dateGrouping);
      IF STRCMP(LOWER(TRIM(dateGrouping)), 'day') = 0 THEN
        SET @idatr := '%Y-%m-%d';
        SET @idats := '%Y-%m-%d';
      ELSEIF STRCMP(LOWER(TRIM(dateGrouping)),'week') = 0 THEN
        SET @idatr := '%x-%v Monday';
        SET @idats := '%x-%v %W';
      ELSEIF STRCMP(LOWER(TRIM(dateGrouping)), 'month') = 0  THEN
        SET @idatr := '%Y-%m-01';
        SET @idats := '%Y-%m-%d';
      ELSEIF STRCMP(LOWER(TRIM(dateGrouping)), 'year') = 0  THEN
        SET @idatr := '%Y-01-01';
        SET @idats := '%Y-%m-%d';
      END IF;
    END IF;
  END IF;

  SET @itype := 'Batch';
  IF resourceType IS NOT NULL THEN
    IF (TRIM(resourceType) != '') AND (TRIM(resourceType) != 'null') THEN
      SET @itype := TRIM(resourceType);
    END IF;
  END IF;

  SET @imetric := 'WallDuration';
  IF metric IS NOT NULL THEN
    IF (TRIM(metric) != '') AND (TRIM(metric) != 'null') THEN
      SET @imetric := TRIM(metric);
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


  SET @iselValues := '';
  IF selValues IS NOT NULL THEN
    IF (TRIM(selValues) != '') AND (TRIM(selValues) != 'null') THEN
      SET @iselValues := concat_ws('', 'AND VO.VOName', @iselTypeA, '',TRIM(selValues), @iselTypeB, '');
    END IF;
  END IF;

  SELECT generateResourceTypeClause(resourceType) INTO @myresourceclause;
  SELECT SystemProplist.cdr INTO @usereportauthentication from SystemProplist
  WHERE SystemProplist.car = 'use.report.authentication';
  SELECT Role.whereclause INTO @mywhereclause from Role
    WHERE Role.role = userRole;
  SELECT generateWhereClause(@iuser,@irole,@mywhereclause)
    INTO @mywhereclause;
  call parse(@iuser,@name,@key,@vo);

IF @imetric = 'SiteCount' THEN
  SET @sql :=
           concat_ws('',
           'SELECT final_rank,
                VOProbeSummary.VOName,
                sitecntx,
                VOProbeSummary.WallDuration,
                VOProbeSummary.cpu,
                VOProbeSummary.Njobs
            FROM (SELECT @rank:=@rank+1 AS final_rank,
                          vonamex,
                          sitecntx
                  FROM (SELECT @rank:=0 AS rank,
                             VO.VOName AS vonamex,
                             COUNT(distinct S.sitename) AS sitecntx
                        FROM MasterSummaryData M, Probe P, Site S, VO, VONameCorrection Corr
                        WHERE EndTime >= (''', @thisFromDate, ''')
                          AND EndTime <= (''', @thisToDate, ''')',
                          ' ', @myresourceclause,
                          ' ', @mywhereclause,
                          ' AND P.siteid = S.siteid
                          AND M.probename = P.probename
                          AND M.VOCorrid = Corr.corrid
                          AND Corr.void = VO.void
                        GROUP BY VO.VOName
                        ORDER BY sitecntx DESC
                       ) AS foox
                 ) AS foo,
            (SELECT VOName,
                   SUM(VOProbeSummary.WallDuration)/', @iunit, ' AS WallDuration,
                   SUM(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration)/', @iunit, ' AS cpu,
                   SUM(VOProbeSummary.Njobs) AS Njobs
             FROM VOProbeSummary, Probe, Site
             WHERE Probe.siteid = Site.siteid
                AND VOProbeSummary.probename = Probe.probename
                AND EndTime >= (''', @thisFromDate, ''')
                AND EndTime <= (''', @thisToDate, ''')',
                ' ', @myresourceclause,
                ' ', @mywhereclause,
           ' GROUP BY VOProbeSummary.VOName
             ) AS VOProbeSummary
           WHERE VOProbeSummary.VOName = foo.vonamex
           ORDER BY final_rank, VOProbeSummary.VOName;'
      );
ELSE
    SET @sql :=
             concat_ws('',
                 ' SELECT final_rank,',
                 ' zVOProbeSummary.zVOName AS VOName,',
                 ' zVOProbeSummary.zDateValue AS DateValue,',
                 ' SUM(zVOProbeSummary.zWallDuration) AS WallDuration,'
                 ' SUM(zVOProbeSummary.zcpu) AS cpu,',
                 ' SUM(zVOProbeSummary.zNjobs) AS Njobs',
                 ' FROM',
                 ' (SELECT @rank:=@rank+1 AS final_rank,',
                 '   foox.xVOName AS oVOName, foox.x', @imetric,' AS o', @imetric,'',
                 '   FROM ',
                 '   (SELECT @rank:=0 AS rank,',
                 '     VO.VOName AS xVOName, SUM(V.m', @imetric,') AS x', @imetric,'',
                 '     FROM ',
                 '     (SELECT VOCorrid AS mVOCorrid,',
                 '        SUM(', @imetric,') AS m', @imetric,' ',
                 '         FROM MasterSummaryData',
                 '         WHERE (EndTime) >= (''', @thisFromDate, ''')',
                 '         AND (EndTime) <= (''', @thisToDate, ''')',
                 '         AND ResourceType = ''', @itype, '''',
                 '         GROUP BY mVOCorrid',
                 '     ) V, VONameCorrection Corr, VO',
                 '     WHERE V.mVOCorrid = Corr.corrid AND Corr.VOid = VO.VOid',
                 ' ', @iselValues,
                 '     GROUP BY xVOName',
                 '     ORDER BY x', @imetric,' desc',
                 '   ) AS foox',
                 ' ) foo,',
                 ' (SELECT VOProbeSummary.EndTime, VOProbeSummary.VOName AS zVOName,',
                 '   SUM(VOProbeSummary.WallDuration)/', @iunit, ' AS zWallDuration,',
                 '   SUM(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration)/', @iunit, ' AS zcpu,',
                 '   SUM(VOProbeSummary.Njobs) AS zNjobs,',
                 '   STR_TO_DATE(DATE_FORMAT(VOProbeSummary.EndTime,''', @idatr, '''), ''', @idats, ''') AS zDateValue',
                 '   FROM VOProbeSummary',
                 '   WHERE (EndTime) >= (''', @thisFromDate, ''')'
                 '   AND (EndTime) <= (''', @thisToDate, ''')',
                 ' ', @myresourceclause,
                 ' ', @mywhereclause,
                 '   GROUP BY zDateValue, zVOName',
                 ' ) zVOProbeSummary',
                 ' WHERE zVOProbeSummary.zVOName = foo.oVOName',
                 ' GROUP BY DateValue, VOName',
                 ' ORDER BY final_rank, VOName, DateValue',
                 ';'
              );
END IF;

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


-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- end:

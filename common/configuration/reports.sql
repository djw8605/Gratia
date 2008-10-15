DELIMITER $$

DROP PROCEDURE IF EXISTS `reports` $$
CREATE PROCEDURE `reports`
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

  SET @thisFromDate := ''; 
  SET @ifrom := CONCAT_WS('', 'AND VOProbeSummary.EndTime >= ''', SUBDATE(CURDATE(), 800),'''');
  IF fromdate IS NOT NULL THEN
    IF (TRIM(fromdate) != '') AND (TRIM(fromdate) != 'null') THEN
      SELECT STR_TO_DATE(fromdate, '%M %e, %Y') into @testDate;
      IF @testDate IS NOT NULL THEN
        SELECT DATE_FORMAT(@testDate, '%Y-%m-%d') into @thisFromDate;
      ELSE 
        SET @thisFromDate := fromdate;
      END IF;
      IF todate IS NULL THEN
        SET @ifrom := CONCAT_WS('', 'AND VOProbeSummary.EndTime = ''', TRIM(@thisFromDate), '''');
      ELSE
        SET @ifrom := CONCAT_WS('', 'AND VOProbeSummary.EndTime >= ''', TRIM(@thisFromDate), '''');
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
      SET @ito := CONCAT_WS('', 'AND VOProbeSummary.EndTime <= ''', TRIM(@thisToDate),'''');
    END IF;
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
      ELSE IF STRCMP(LOWER(TRIM(dateGrouping)), 'week') = 0 THEN
        SET @idatr := '%x-%v Monday';
        SET @idats := '%x-%v %W';
      ELSE IF STRCMP(LOWER(TRIM(dateGrouping)), 'month') = 0  THEN
        SET @idatr := '%Y-%m-01';
        SET @idats := '%Y-%m-%d';
      ELSE IF STRCMP(LOWER(TRIM(dateGrouping)), 'year') = 0  THEN
        SET @idatr := '%Y-01-01';
        SET @idats := '%Y-%m-%d';
      END IF;
    END IF;
  END IF;

  SET @igroupBy := 'GROUP BY DateValue, UserName, SiteName, VOName';
  IF groupBy IS NOT NULL THEN
    IF TRIM(groupBy) != 'null' THEN
      SET @igroupBy := CONCAT_WS('', 'GROUP BY ', TRIM(groupBy));
    END IF;
  END IF;

  SET @iorderBy := 'ORDER BY CommonName, SiteName, VOName, DateValue';
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
    ELSE IF selType = 'IN' THEN
      SET @iselTypeA := ' IN ';
      SET @iselTypeB := '';
    END IF;
  END IF;

  SET @iVOs := '';
  SET @rVOs := '';
  IF VOs IS NOT NULL THEN
    IF (TRIM(VOs) != '') AND (TRIM(VOs) != 'null') THEN
      SET @iVOs := CONCAT_WS('', 'AND VOProbeSummary.VOName', @iselTypeA, '', TRIM(VOs), @iselTypeB, '');
      SET @rVOs := CONCAT_WS('', 'AND V.VOName', @iselTypeA, '', TRIM(VOs), @iselTypeB, '');
    END IF;
  END IF;

  SET @iSites := '';
  SET @rSites := '';
  IF Sites IS NOT NULL THEN
    IF (TRIM(Sites) != '') AND (TRIM(Sites) != 'null') THEN
      SET @iSites := CONCAT_WS('', 'AND Site.SiteName', @iselTypeA, '', TRIM(Sites), @iselTypeB, '');
      SET @rSites := CONCAT_WS('', 'AND S.SiteName', @iselTypeA, '', TRIM(Sites), @iselTypeB, '');
    END IF;
  END IF;

  SET @iProbes := '';
  SET @rProbes := '';
  IF Probes IS NOT NULL THEN
    IF (TRIM(Probes) != '') AND (TRIM(Probes) != 'null') THEN
      SET @iProbes := CONCAT_WS('', 'AND VOProbeSummary.ProbeName', @iselTypeA, '', TRIM(Probes), @iselTypeB, '');
      SET @rProbes := CONCAT_WS('', 'AND P.ProbeName', @iselTypeA, '', TRIM(Probes), @iselTypeB, '');
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
           '      S.SiteName AS sitenamex,',
           '      V.EndTime AS endtimex,',
           '      SUM(V.WallDuration)/', @iunit, ' AS walldurationx',
           '        FROM VOProbeSummary V, Site S, Probe P',
           '        WHERE V.ProbeName = P.probename AND P.siteid = S.Siteid',
           '        AND (V.EndTime) >= (''', @thisFromDate, ''')',
           '        AND (V.EndTime) <= (''', @thisToDate, ''')',
           ' ', @rVOs,
           ' ', @rSites,
           ' ', @rProbes,
           '   GROUP BY sitenamex',
           '   ORDER BY walldurationx desc) AS foox) AS foo', ','
           );
       SET @rankwhere := ' AND Site.SiteName = sitenamex';
       SET @final_rank := 'final_rank,';
    END IF;
  END IF;

  SET @sql := CONCAT_WS('',
           'SELECT ', @final_rank,
           ' Site.SiteName AS SiteName,',
           ' SUM(VOProbeSummary.WallDuration)/', @iunit, ' AS WallDuration,',
           ' SUM(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration)/', @iunit, ' AS cpu,',
           ' SUM(VOProbeSummary.CpuUserDuration)/', @iunit, ' AS cpuUser,',
           ' SUM(VOProbeSummary.CpuSystemDuration)/', @iunit, ' AS cpuSystem,',
           ' SUM(VOProbeSummary.Njobs) AS Njobs,',
           ' VOProbeSummary.VOName AS VOName,',
           ' VOProbeSummary.CommonName AS UserName,',
           ' VOProbeSummary.ProbeName AS ProbeName,',
           ' STR_TO_DATE(DATE_FORMAT(VOProbeSummary.EndTime,''', @idatr, '''), ''', @idats, ''') AS  DateValue',
           ' FROM ', @rankfrom, 'Site,Probe,VOProbeSummary',
           ' WHERE Probe.siteid = Site.siteid',
           '    AND VOProbeSummary.ProbeName = Probe.probename',
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


-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

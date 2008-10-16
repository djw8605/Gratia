DELIMITER $$

DROP PROCEDURE IF EXISTS `reportsPSacct` $$
CREATE PROCEDURE `reportsPSacct`(
                userName varchar(64), 
                userRole varchar(64),
                fromdate varchar(64), 
                todate varchar(64),
                dateGrouping varchar(64),
                timeUnit varchar(64), 
                scaleUnit varchar(64),
                groupBy varchar(128), 
                orderBy varchar(128),
                resourceType varchar(64),
                VOs varchar(1024),
                Sites varchar(1024),
                queryType varchar(10),
                reportName varchar(64)
                )
    READS SQL DATA
BEGIN
  SELECT sysdate() INTO @proc_start;
  SET @iuser := concat_ws('','GratiaUser|', UNIX_TIMESTAMP(), '|Unknown');
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

  SET @ifrom := '';
  SET @ifromV := '';
  SET @ifromN := '';
  IF fromdate IS NOT NULL THEN
    IF (TRIM(fromdate) != '') AND (LOWER(TRIM(fromdate)) != 'null') THEN
      SET @thisFromDate := fromdate;
      SELECT STR_TO_DATE(fromdate, '%M %e, %Y') INTO @testDate;
      if @testDate IS NOT NULL THEN
        SELECT  DATE_FORMAT(@testDate, '%Y-%m-%d') INTO @thisFromDate;
      end if;
      IF (todate IS NULL) OR (TRIM(todate) = '') OR (TRIM(todate) = 'null') THEN
        SET @ifrom := CONCAT_WS('', 'AND VOProbeSummary.EndTime = ''', TRIM(@thisFromDate), '''');
        SET @ifromV := concat_ws('','AND V.EndTIme = ''', TRIM(@thisFromDate), '''');
        SET @ifromN := concat_ws('','AND N.EndTIme = ''', TRIM(@thisFromDate), '''');
      ELSE
        SET @ifrom := CONCAT_WS('', 'AND VOProbeSummary.EndTime >= ''', TRIM(@thisFromDate), '''');
        SET @ifromV := concat_ws('','AND V.EndTIme >= ''', TRIM(@thisFromDate), '''');
        SET @ifromN := concat_ws('','AND N.EndTIme >= ''', TRIM(@thisFromDate), '''');
      END IF;
    END IF;
  END IF;
  SET @ito := '';
  SET @itoV := '';
  SET @itoN := '';
  IF todate IS NOT NULL THEN
    IF (TRIM(todate) != '') AND (LOWER(TRIM(todate)) != 'null') THEN
      SET @thisToDate := todate;
      SELECT STR_TO_DATE(todate, '%M %e, %Y') INTO @testDate;
      if @testDate IS NOT NULL THEN
        SELECT  DATE_FORMAT(@testDate, '%Y-%m-%d') INTO @thisToDate;
      end if;
      SET @ito := concat_ws('','and VOProbeSummary.EndTime <= ''', TRIM(@thisToDate),'''');
      SET @itoV := concat_ws('','and V.EndTime <= ''', TRIM(@thisToDate),'''');
      SET @itoN := concat_ws('','and N.EndTime <= ''', TRIM(@thisToDate),'''');
    END IF;
  END IF;
  SET @idatr := '%Y-%m-%d';
  SET @idats := '%Y-%m-%d';
  SET @igroup := 'day';
  IF dateGrouping IS NOT NULL THEN
    IF (TRIM(dateGrouping) != '') AND (LOWER(TRIM(dateGrouping)) != 'null') THEN
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
  SET @iVOs := '';
  SET @rVOs := '';
  IF VOs IS NOT NULL THEN
    IF (TRIM(VOs) != '') AND (LOWER(TRIM(VOs)) != 'null') THEN
      SET @iVOs := concat_ws('', ' and VOProbeSummary.VOName = ''', TRIM(VOs), '''');
      SET @rVOs := concat_ws('', ' and V.VOName = ''',TRIM(VOs), '''');
    END IF;
  END IF;
  SET @iSites := '';
  SET @rSites := '';
  IF Sites IS NOT NULL THEN
    IF (TRIM(Sites) != '') AND (LOWER(TRIM(Sites)) != 'null') THEN
      SET @iSites := concat_ws('', 'and Site.SiteName = ''', TRIM(Sites), '''');
      SET @rSites := concat_ws('', 'and S.SiteName = ''', TRIM(Sites), '''');
    END IF;
  END IF;
  SET @iscale := '1';
  IF scaleUnit IS NOT NULL THEN
    IF (TRIM(scaleUnit) != '') AND (LOWER(TRIM(scaleUnit)) != 'null') THEN
      SET @iscale := TRIM(scaleUnit);
    END IF;
  END IF;
  SET @iunit := '3600';
    IF timeUnit IS NOT NULL THEN
    IF (TRIM(timeUnit) != '') AND (LOWER(TRIM(timeUnit)) != 'null') THEN
      SET @iunit := TRIM(timeUnit);
    END IF;
  END IF;
  SET @thisScale := concat_ws('', '/(', @iscale, '*', @iunit, ')');
  SET @thisiscale := concat_ws('', '/', @iscale);

  SET @iquery := 'SUM'; 
  IF queryType IS NOT NULL THEN
    IF (TRIM(queryType) != '') AND (LOWER(TRIM(queryType)) != 'null') THEN
      SET @iquery := TRIM(queryType);
    END IF;
  END IF;
  SET @igroupBy := 'GROUP BY DateValue, SiteName,  VOName';
  IF groupBy IS NOT NULL THEN
    IF (TRIM(groupBy) != '') AND (LOWER(TRIM(groupBy)) != 'null') THEN
      SET @igroupBy := concat_ws('', 'GROUP BY ', TRIM(groupBy));
    END IF;
  END IF;
  SET @iorderBy := 'order by DateValue';
  IF orderBy IS NOT NULL THEN
    IF (TRIM(orderBy) != '') AND (LOWER(TRIM(orderBy)) != 'null') THEN
      SET @iorderBy := concat_ws('', 'order by ', TRIM(orderBy));
    END IF;
  END IF;
  SET @itype := 'RawCPU';
  IF resourceType IS NOT NULL THEN
    IF (TRIM(resourceType) != '') AND (LOWER(TRIM(resourceType)) != 'null') THEN
      SET @itype := TRIM(resourceType);
    END IF;
  END IF;
  
  SELECT generateResourceTypeClause(@itype) INTO @myresourceclause;
  SELECT SystemProplist.cdr INTO @usereportauthentication FROM SystemProplist WHERE SystemProplist.car = 'use.report.authentication';
  SELECT Role.whereclause INTO @mywhereclause FROM Role WHERE Role.role = @irole;
  SELECT generateWhereClause(@iuser,@irole,@mywhereclause) INTO @mywhereclause;
  CALL parse(@iuser,@name,@key,@vo);
  
  SET @final_rank := '';
  SET @sqlSELECTs := '';
  SET @sqlFrom    := '';
  SET @sqlWhere   := '';
  IF (STRCMP(LOWER(TRIM(@iquery)), 'count') = 0) THEN
    SET @ndays := 'N.DaysInMonth';
    IF (STRCMP(LOWER(TRIM(@igroup)),'week') = 0)  THEN
      SET @ndays := '7';
    END IF;
    
    SET @sqlSELECTs := concat_ws('',
           ' S.SiteName AS SiteName,',
           ' STR_TO_DATE(DATE_FORMAT(N.EndTime,''', @idatr, '''), ''', @idats, ''') AS  DateValue,',
           ' SUM(N.CpuSystemTime)*C.BenchmarkScore', @thisScale, ' AS cpuSystemBenchDays,',
           ' SUM(N.CpuUserTime)*C.BenchmarkScore', @thisScale, ' AS cpuUserBenchDays,',
           ' count(distinct N.Node)*C.BenchmarkScore*C.CPUCount*', @ndays, @thisiscale, ' AS AvailableBenchDays,',
           ' count(distinct N.Node)*C.BenchmarkScore*C.CPUCount*0.5*', @ndays, @thisiscale, ' AS HalfAvailableBenchDays,',
           ' count(distinct N.Node) AS NodeCount,',
           ' C.HostDescription AS HostDescription,',
           ' C.BenchmarkScore AS BenchmarkScore,',
           ' C.CPUCount AS CPUCount'
           );
    SET @sqlFrom    := 'NodeSummary N, Probe P, Site S, CPUInfo C';
    SET @sqlWhere   := concat_ws('',
           ' P.siteid = S.siteid and N.ProbeName = P.ProbeName and N.HostDescription = C.HostDescription',
           ' ', @myresourceclause,
           ' ', @ifromN,
           ' ', @itoN,
           ' ', @rVOs,
           ' ', @rSites
           );
  ELSE
    SET @sqlSELECTs := concat_ws('',
           ' Site.SiteName AS SiteName,',
           ' STR_TO_DATE(DATE_FORMAT(VOProbeSummary.EndTime,''', @idatr, '''), ''', @idats, ''') AS  DateValue,',
           ' sum(VOProbeSummary.WallDuration)', @thisScale, ' AS WallDuration,',
           ' sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration)', @thisScale, ' AS cpu,',
           ' sum(VOProbeSummary.CpuUserDuration)', @thisScale, ' AS cpuUser,',
           ' sum(VOProbeSummary.CpuSystemDuration)', @thisScale, ' AS cpuSystem,',
           ' sum(VOProbeSummary.Njobs) AS Njobs,',
           ' VOProbeSummary.VOName AS VOName,',
           ' VOProbeSummary.CommonName AS UserName,',
           ' VOProbeSummary.ProbeName AS ProbeName'
           );
    SET @sqlFrom  := 'VOProbeSummary, Site, Probe';
    SET @sqlWhere := concat_ws('',
           ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename ',
           ' ', @myresourceclause,
           ' ', @ifrom,
           ' ', @ito,
           ' ', @iVOs,
           ' ', @iSites
           );
  END IF;
  IF (STRCMP(LOWER(TRIM(@iquery)), 'ranked') = 0) THEN
    SET @sqlSELECTs := concat_ws('',
           ' DateValue,',
           ' sum(VOProbeSummary.WallDuration)', @thisScale, ' AS WallDuration,',
           ' sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration)', @thisScale, ' AS cpu,',
           ' sum(VOProbeSummary.CpuUserDuration)', @thisScale, ' AS cpuUser,',
           ' sum(VOProbeSummary.CpuSystemDuration)', @thisScale, ' AS cpuSystem,',
           ' sum(VOProbeSummary.Njobs) AS Njobs,',
           ' VOProbeSummary.VOName AS VOName'
           );
    SET @sqlWhere   := 'VOProbeSummary.VOName = foo.VONamex';
    SET @final_rank := 'final_rank,';
    SET @rsqlWhere  := 'P.siteid = S.Siteid AND V.ProbeName = P.probename ';
    SET @sqlFrom    := concat_ws('',
         ' (SELECT @rank:= @rank+1 AS final_rank,',
         '         VONamex,',
         '         cpuuserdurationx',
         '   FROM (SELECT @rank:=0 AS rank,',
         '            VOName AS VONamex,',
         '            V.EndTime AS EndTimex,',
         '            sum(V.cpuuserduration) AS cpuuserdurationx',
         '         FROM VOProbeSummary V, Site S, Probe P',
         '         WHERE  ',
         ' ',         @rsqlWhere,
         ' ',         @ifromV,
          ' ',        @itoV,
         ' ',         @rVOs,
         ' ',         @rSites,
         ' ',         @myresourceclause,
         ' ',         @mywhereclause,
         '         GROUP BY VONamex',
         '         ORDER BY cpuuserdurationx desc',
         '         ) AS foox',
         ' ) AS foo, '
         ' ( SELECT ',
         '     VOName,'
         '     sum(WallDuration)', @thisScale, ' AS WallDuration,',
         '     sum(CpuUserDuration + CpuSystemDuration)', @thisScale, ' AS Cpu,',
         '     sum(CpuUserDuration)', @thisScale, ' AS CpuUserDuration,',
         '     sum(CpuSystemDuration)', @thisScale, ' AS CpuSystemDuration,',
         '     sum(Njobs) AS Njobs,',
         '     STR_TO_DATE(DATE_FORMAT(EndTime,''', @idatr, '''), ''', @idats, ''') AS  DateValue',
         '   FROM VOProbeSummary V, Probe P, Site S'
         '   WHERE ',
         ' ', @rsqlWhere,
         ' ', @ifromV,
         ' ', @itoV,
         ' ', @rVOs,
         ' ', @rSites,
         ' ', @myresourceclause,
         ' ', @mywhereclause,
         '   GROUP BY DateValue, VOName',
         ' ) AS VOProbeSummary'
         );
  END IF;
  
  SET @sql := concat_ws('',
           ' SELECT ', @final_rank,
           ' ', @sqlSELECTs,
           ' FROM ', @sqlFrom, 
           ' WHERE ', @sqlWhere,
           ' ', @igroupBy,
           ' ', @iorderBy,
           ';'
        );
  SELECT sysdate() INTO @query_start;
    prepare statement FROM @sql;
    execute statement;
  SELECT sysdate() INTO @query_end;
  INSERT INTO trace(
  			procName,
  			userKey,
  			userName,
  			userRole,
  			userVO,
  			sqlQuery,
  			procTime,
  			queryTime,
  			p1, p2, p3)
	values(
			TRIM(reportName), 
			@key,
			@iuser,
			@irole,
			@vo,
			@sql,
			timediff(@query_start, @proc_start),
			timediff(@query_end,   @query_start),
			null, null, null);      	
  deallocate prepare statement;
END $$

DELIMITER ;

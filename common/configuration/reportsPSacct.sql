DELIMITER $$

DROP PROCEDURE IF EXISTS `reportsPSacct` $$
CREATE PROCEDURE `reportsPSacct`(
                userName varchar(64), userRole varchar(64),
                fromdate varchar(64), todate varchar(64),
                dateGrouping varchar(64),
                timeUnit varchar(64), scaleUnit varchar(64),
                groupBy varchar(128), orderBy varchar(128),
                resourceType varchar(64),
                VOs varchar(1024),
                Sites varchar(1024),
                queryType varchar(10),
                reportName varchar(64)
                )
    READS SQL DATA
begin
  select sysdate() into @proc_start;
  set @iuser := concat_ws('','GratiaUser|', UNIX_TIMESTAMP(), '|Unknown');
  IF userName IS NOT NULL THEN
    IF (TRIM(userName) != '') AND (TRIM(userName) != 'null') THEN
      set @iuser := TRIM(userName);
    END IF;
  END IF;
  set @irole := 'GratiaUser';
  IF userRole IS NOT NULL THEN
    IF (TRIM(userRole) != '') AND (TRIM(userRole) != 'null') THEN
      set @irole := TRIM(userRole);
    END IF;
  END IF;
  select generateResourceTypeClause(@itype) into @myresourceclause;
  select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
  select Role.whereclause into @mywhereclause from Role where Role.role = @irole;
  select generateWhereClause(@iuser,@irole,@mywhereclause) into @mywhereclause;
  call parse(@iuser,@name,@key,@vo);
  set @ifrom := '';
  set @ifromV := '';
  set @ifromN := '';
  IF fromdate IS NOT NULL THEN
    IF (TRIM(fromdate) != '') AND (LOWER(TRIM(fromdate)) != 'null') THEN
      set @thisFromDate := fromdate;
      select STR_TO_DATE(fromdate, '%M %e, %Y') into @testDate;
      if @testDate IS NOT NULL THEN
        select  date_format(@testDate, '%Y-%m-%d') into @thisFromDate;
      end if;
      set @ifrom := concat_ws('','and VOProbeSummary.EndTIme >=''', TRIM(@thisFromDate), '''');
      set @ifromV := concat_ws('','and V.EndTIme >=''', TRIM(@thisFromDate), '''');
      set @ifromN := concat_ws('','and N.EndTIme >=''', TRIM(@thisFromDate), '''');
    END IF;
  END IF;
  set @ito := '';
  set @itoV := '';
  set @itoN := '';
  IF todate IS NOT NULL THEN
    IF (TRIM(todate) != '') AND (LOWER(TRIM(todate)) != 'null') THEN
      set @thisToDate := todate;
      select STR_TO_DATE(todate, '%M %e, %Y') into @testDate;
      if @testDate IS NOT NULL THEN
        select  date_format(@testDate, '%Y-%m-%d') into @thisToDate;
      end if;
      set @ito := concat_ws('','and VOProbeSummary.EndTime <= ''', TRIM(@thisToDate),'''');
      set @itoV := concat_ws('','and V.EndTime <= ''', TRIM(@thisToDate),'''');
      set @itoN := concat_ws('','and N.EndTime <= ''', TRIM(@thisToDate),'''');
    END IF;
  END IF;
  set @idatr := '%Y-%m-%d';
  set @idats := '%Y-%m-%d';
  set @igroup := 'day';
  IF dateGrouping IS NOT NULL THEN
    IF (TRIM(dateGrouping) != '') AND (LOWER(TRIM(dateGrouping)) != 'null') THEN
      set @igroup := TRIM(dateGrouping);
      IF STRCMP(LOWER(TRIM(dateGrouping)), 'day') = 0 THEN
        set @idatr := '%Y-%m-%d';
        set @idats := '%Y-%m-%d';
      ELSEIF STRCMP(LOWER(TRIM(dateGrouping)),'week') = 0 THEN
        set @idatr := '%x-%v Monday';
        set @idats := '%x-%v %W';
      ELSEIF STRCMP(LOWER(TRIM(dateGrouping)), 'month') = 0  THEN
        set @idatr := '%Y-%m-01';
        set @idats := '%Y-%m-%d';
      ELSEIF STRCMP(LOWER(TRIM(dateGrouping)), 'year') = 0  THEN
        set @idatr := '%Y-01-01';
        set @idats := '%Y-%m-%d';
      END IF;
    END IF;
  END IF;
  set @iVOs := '';
  set @rVOs := '';
  IF VOs IS NOT NULL THEN
    IF (TRIM(VOs) != '') AND (LOWER(TRIM(VOs)) != 'null') THEN
      set @iVOs := concat_ws('', ' and VOProbeSummary.VOName = ''', TRIM(VOs), '''');
      set @rVOs := concat_ws('', ' and V.VOName = ''',TRIM(VOs), '''');
    END IF;
  END IF;
  set @iSites := '';
  set @rSites := '';
  IF Sites IS NOT NULL THEN
    IF (TRIM(Sites) != '') AND (LOWER(TRIM(Sites)) != 'null') THEN
      set @iSites := concat_ws('', 'and Site.SiteName = ''', TRIM(Sites), '''');
      set @rSites := concat_ws('', 'and S.SiteName = ''', TRIM(Sites), '''');
    END IF;
  END IF;
  set @iscale := '1';
  IF scaleUnit IS NOT NULL THEN
    IF (TRIM(scaleUnit) != '') AND (LOWER(TRIM(scaleUnit)) != 'null') THEN
      set @iscale := TRIM(scaleUnit);
    END IF;
  END IF;
  set @iunit := '3600';
    IF timeUnit IS NOT NULL THEN
    IF (TRIM(timeUnit) != '') AND (LOWER(TRIM(timeUnit)) != 'null') THEN
      set @iunit := TRIM(timeUnit);
    END IF;
  END IF;
  set @thisScale := concat_ws('', '/(', @iscale, '*', @iunit, ')');
  set @thisiscale := concat_ws('', '/', @iscale);

  set @iquery := 'SUM'; 
  IF queryType IS NOT NULL THEN
    IF (TRIM(queryType) != '') AND (LOWER(TRIM(queryType)) != 'null') THEN
      set @iquery := TRIM(queryType);
    END IF;
  END IF;
  set @igroupBy := 'group by DateValue, SiteName,  VOName';
  IF groupBy IS NOT NULL THEN
    IF (TRIM(groupBy) != '') AND (LOWER(TRIM(groupBy)) != 'null') THEN
      set @igroupBy := concat_ws('', 'group by ', TRIM(groupBy));
    END IF;
  END IF;
  set @iorderBy := 'order by DateValue';
  IF orderBy IS NOT NULL THEN
    IF (TRIM(orderBy) != '') AND (LOWER(TRIM(orderBy)) != 'null') THEN
      set @iorderBy := concat_ws('', 'order by ', TRIM(orderBy));
    END IF;
  END IF;
  set @itype := 'RawCPU';
  IF resourceType IS NOT NULL THEN
    IF (TRIM(resourceType) != '') AND (LOWER(TRIM(resourceType)) != 'null') THEN
      set @itype := TRIM(resourceType);
    END IF;
  END IF;
  set @final_rank := '';
  set @sqlSelects := '';
  set @sqlFrom    := '';
  set @sqlWhere   := '';
  IF (STRCMP(LOWER(TRIM(@iquery)), 'count') = 0) THEN
    set @ndays := 'N.DaysInMonth';
    IF (STRCMP(LOWER(TRIM(@igroup)),'week') = 0)  THEN
      set @ndays := '7';
    END IF;
    
    set @sqlSelects := concat_ws('',
           ' S.SiteName as SiteName,',
           ' str_to_date(date_format(N.EndTime,''', @idatr, '''), ''', @idats, ''') as  DateValue,',
           ' SUM(N.CpuSystemTime)*C.BenchmarkScore', @thisScale, ' as cpuSystemBenchDays,',
           ' SUM(N.CpuUserTime)*C.BenchmarkScore', @thisScale, ' as cpuUserBenchDays,',
           ' count(distinct N.Node)*C.BenchmarkScore*C.CPUCount*', @ndays, @thisiscale, ' as AvailableBenchDays,',
           ' count(distinct N.Node)*C.BenchmarkScore*C.CPUCount*0.5*', @ndays, @thisiscale, ' as HalfAvailableBenchDays,',
           ' count(distinct N.Node) as NodeCount,',
           ' C.HostDescription as HostDescription,',
           ' C.BenchmarkScore as BenchmarkScore,',
           ' C.CPUCount as CPUCount'
           );
    set @sqlFrom    := 'NodeSummary N, Probe P, Site S, CPUInfo C';
    set @sqlWhere   := concat_ws('',
           ' P.siteid = S.siteid and N.ProbeName = P.ProbeName and N.HostDescription = C.HostDescription',
           '    and ResourceType =''',@itype, '''',
           ' ', @ifromN,
           ' ', @itoN,
           ' ', @rVOs,
           ' ', @rSites
           );
  ELSE
    set @sqlSelects := concat_ws('',
           ' Site.SiteName as SiteName,',
           ' str_to_date(date_format(VOProbeSummary.EndTime,''', @idatr, '''), ''', @idats, ''') as  DateValue,',
           ' sum(VOProbeSummary.WallDuration)', @thisScale, ' as WallDuration,',
           ' sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration)', @thisScale, ' as cpu,',
           ' sum(VOProbeSummary.CpuUserDuration)', @thisScale, ' as cpuUser,',
           ' sum(VOProbeSummary.CpuSystemDuration)', @thisScale, ' as cpuSystem,',
           ' sum(VOProbeSummary.Njobs) as Njobs,',
           ' VOProbeSummary.VOName as VOName,',
           ' VOProbeSummary.CommonName as UserName,',
           ' VOProbeSummary.ProbeName as ProbeName'
           );
    set @sqlFrom  := 'VOProbeSummary, Site, Probe';
    set @sqlWhere := concat_ws('',
           ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename ',
           '    and ResourceType =''', @itype, '''',
           ' ', @ifrom,
           ' ', @ito,
           ' ', @iVOs,
           ' ', @iSites
           );
  END IF;
  IF (STRCMP(LOWER(TRIM(@iquery)), 'ranked') = 0) THEN
    set @sqlSelects := concat_ws('',
           ' DateValue,',
           ' sum(VOProbeSummary.WallDuration)', @thisScale, ' as WallDuration,',
           ' sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration)', @thisScale, ' as cpu,',
           ' sum(VOProbeSummary.CpuUserDuration)', @thisScale, ' as cpuUser,',
           ' sum(VOProbeSummary.CpuSystemDuration)', @thisScale, ' as cpuSystem,',
           ' sum(VOProbeSummary.Njobs) as Njobs,',
           ' VOProbeSummary.VOName as VOName'
           );
    set @sqlWhere   := 'VOProbeSummary.VOName = foo.VONamex';
    set @final_rank := 'final_rank,';
    set @rsqlWhere  := 'P.siteid = S.Siteid and V.ProbeName = P.probename ';
    set @sqlFrom    := concat_ws('',
         ' (SELECT @rank:= @rank+1 as final_rank,',
         '         VONamex,',
         '         cpuuserdurationx',
         '   FROM (SELECT @rank:=0 as rank,',
         '            VOName as VONamex,',
         '            V.EndTime as EndTimex,',
         '            sum(V.cpuuserduration) as cpuuserdurationx',
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
         '         ) as foox',
         ' ) as foo, '
         ' ( SELECT ',
         '     VOName,'
         '     sum(WallDuration)', @thisScale, ' as WallDuration,',
         '     sum(CpuUserDuration + CpuSystemDuration)', @thisScale, ' as Cpu,',
         '     sum(CpuUserDuration)', @thisScale, ' as CpuUserDuration,',
         '     sum(CpuSystemDuration)', @thisScale, ' as CpuSystemDuration,',
         '     sum(Njobs) as Njobs,',
         '     str_to_date(date_format(EndTime,''', @idatr, '''), ''', @idats, ''') as  DateValue',
         '   FROM VOProbeSummary V, Probe P, Site S'
         '   WHERE ',
         ' ', @rsqlWhere,
         ' ', @ifromV,
         ' ', @itoV,
         ' ', @rVOs,
         ' ', @rSites,
         ' ', @myresourceclause,
         ' ', @mywhereclause,
         '   GROUP by DateValue, VOName',
         ' ) as VOProbeSummary'
         );
  END IF;
  
  set @sql := concat_ws('',
           ' select ', @final_rank,
           ' ', @sqlSelects,
           ' FROM ', @sqlFrom, 
           ' WHERE ', @sqlWhere,
           ' ', @igroupBy,
           ' ', @iorderBy,
           ';'
        );
  select sysdate() into @query_start;
    prepare statement from @sql;
    execute statement;
  select sysdate() into @query_end;
  insert into trace(procName,  userKey, userName, userRole, userVO, sqlQuery, procTime, queryTime, p1, p2, p3)
           values(TRIM(reportName), @key,    @iuser,   @irole,   @vo,    @sql,
           		  timediff(@query_start, @proc_start),
    			  timediff(@query_end,   @query_start),
    			  null, null, null);      	
  deallocate prepare statement;
END $$

DELIMITER ;

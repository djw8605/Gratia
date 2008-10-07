DELIMITER $$

DROP PROCEDURE IF EXISTS `reports` $$
CREATE PROCEDURE `reports`(
                userName varchar(64), userRole varchar(64),
                fromdate varchar(64), todate varchar(64),
                timeUnit varchar(64), dateGrouping varchar(64),
                groupBy varchar(128), orderBy varchar(128),
                resourceType varchar(64),
                VOs varchar(1024),
                Sites varchar(1024),
                Probes varchar(1024),
                Ranked varchar(5),
                selType varchar(5)
                )
    READS SQL DATA
begin

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

  set @thisFromDate := ''; 
  set @ifrom := concat_ws('','and VOProbeSummary.EndTIme >=''', SUBDATE(CURDATE(), 800),'''');
  IF fromdate IS NOT NULL THEN
    IF (TRIM(fromdate) != '') AND (TRIM(fromdate) != 'null') THEN
      set @thisFromDate := fromdate;
      select STR_TO_DATE(fromdate, '%M %e, %Y') into @testDate;
      if @testDate IS NOT NULL THEN
        select  date_format(@testDate, '%Y-%m-%d') into @thisFromDate;
      end if;
      set @ifrom := concat_ws('','and VOProbeSummary.EndTIme >=''', TRIM(@thisFromDate), '''');
    END IF;
  END IF;

  set @thisToDate := ''; 
  set @ito := '';
  IF todate IS NOT NULL THEN
    IF (TRIM(todate) != '') AND (TRIM(todate) != 'null') THEN
      set @thisToDate := todate;
      select STR_TO_DATE(todate, '%M %e, %Y') into @testDate;
      if @testDate IS NOT NULL THEN
        select  date_format(@testDate, '%Y-%m-%d') into @thisToDate;
      end if;
      set @ito := concat_ws('','and VOProbeSummary.EndTime <= ''', TRIM(@thisToDate),'''');
    END IF;
  END IF;

  set @iunit := '3600';
  IF timeUnit IS NOT NULL THEN
    IF (TRIM(timeUnit) != '') AND (TRIM(timeUnit) != 'null') THEN
      set @iunit := TRIM(timeUnit);
    END IF;
  END IF;


  set @idatr := '%Y-%m-%d';
  set @idats := '%Y-%m-%d';
  set @igroup := 'day';
  IF dateGrouping IS NOT NULL THEN
    IF (TRIM(dateGrouping) != '') AND (TRIM(dateGrouping) != 'null') THEN
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

  set @igroupBy := 'group by DateValue, UserName, SiteName, VOName';
  IF groupBy IS NOT NULL THEN
    IF TRIM(groupBy) != 'null' THEN
      set @igroupBy := concat_ws('', 'group by ', TRIM(groupBy));
    END IF;
  END IF;

  set @iorderBy := 'order by CommonName, SiteName, VOName, DateValue';
  IF orderBy IS NOT NULL THEN
    IF TRIM(orderBy) != 'null' THEN
      set @iorderBy := concat_ws('', 'order by ', TRIM(orderBy));
    END IF;
  END IF;

  set @itype := 'batch';
  IF resourceType IS NOT NULL THEN
    IF (TRIM(resourceType) != '') AND (TRIM(resourceType) != 'null') THEN
      set @itype := TRIM(resourceType);
    END IF;
  END IF;
  
  set @iselTypeA := '=''';
  set @iselTypeB := '''';
  IF selType IS NOT NULL THEN
    IF selType = 'NOT' THEN
      set @iselTypeA := ' NOT IN ';
      set @iselTypeB := '';
    ELSEIF selType = 'IN' THEN
      set @iselTypeA := ' IN ';
      set @iselTypeB := '';
    END IF;
  END IF;

  set @iVOs := '';
  set @rVOs := '';
  IF VOs IS NOT NULL THEN
    IF (TRIM(VOs) != '') AND (TRIM(VOs) != 'null') THEN
      set @iVOs := concat_ws('', 'and VOProbeSummary.VOName', @iselTypeA, '',TRIM(VOs), @iselTypeB, '');
      set @rVOs := concat_ws('', 'and V.VOName', @iselTypeA, '',TRIM(VOs), @iselTypeB, '');
    END IF;
  END IF;

  set @iSites := '';
  set @rSites := '';
  IF Sites IS NOT NULL THEN
    IF (TRIM(Sites) != '') AND (TRIM(Sites) != 'null') THEN
      set @iSites := concat_ws('', 'and Site.SiteName', @iselTypeA, '',TRIM(Sites), @iselTypeB, '');
      set @rSites := concat_ws('', 'and S.SiteName', @iselTypeA, '',TRIM(Sites), @iselTypeB, '');
    END IF;
  END IF;

  set @iProbes := '';
  set @rProbes := '';
  IF Probes IS NOT NULL THEN
    IF (TRIM(Probes) != '') AND (TRIM(Probes) != 'null') THEN
      set @iProbes := concat_ws('', 'and VOProbeSummary.ProbeName', @iselTypeA, '',TRIM(Probes), @iselTypeB, '');
      set @rProbes := concat_ws('', 'and P.ProbeName', @iselTypeA, '',TRIM(Probes), @iselTypeB, '');
    END IF;
  END IF;

  set @rankfrom := '';
  set @rankwhere := '';
  set @final_rank := '';
  IF Ranked IS NOT NULL THEN
    IF (TRIM(Ranked) != '') AND (TRIM(Ranked) != 'null') THEN
      set @rankfrom := concat_ws('',
           ' (SELECT @rank:=@rank+1 as final_rank, sitenamex, walldurationx',
           '   FROM (SELECT @rank:=0 as rank,',
           '      S.SiteName as sitenamex,',
           '      V.EndTime as endtimex,',
           '      sum(V.WallDuration)/', @iunit, ' as walldurationx',
           '        FROM VOProbeSummary V, Site S, Probe P',
           '        WHERE V.ProbeName = P.probename and P.siteid = S.Siteid',
           '        and (V.EndTime) >= (''', @thisFromDate, ''')',
           '        and (V.EndTime) <= (''', @thisToDate, ''')',
           ' ', @rVOs,
           ' ', @rSites,
           ' ', @rProbes,
           '   GROUP BY sitenamex',
           '   ORDER BY walldurationx desc) as foox) as foo', ','
           );
       set @rankwhere := ' and Site.SiteName = sitenamex';
       set @final_rank := 'final_rank,';
    END IF;
  END IF;

  select generateResourceTypeClause(@itype) into @myresourceclause;
  select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
  select Role.whereclause into @mywhereclause from Role where Role.role = @irole;
  select generateWhereClause(@iuser,@irole,@mywhereclause) into @mywhereclause;
  call parse(@iuser,@name,@key,@vo);

  set @sql := concat_ws('',
           ' select ', @final_rank,
           ' Site.SiteName as SiteName,',
           ' sum(VOProbeSummary.WallDuration)/', @iunit, ' as WallDuration,',
           ' sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration)/', @iunit, ' as cpu,',
           ' sum(VOProbeSummary.CpuUserDuration)/', @iunit, ' as cpuUser,',
           ' sum(VOProbeSummary.CpuSystemDuration)/', @iunit, ' as cpuSystem,',
           ' sum(VOProbeSummary.Njobs) as Njobs,',
           ' VOProbeSummary.VOName as VOName,',
           ' VOProbeSummary.CommonName as UserName,',
           ' VOProbeSummary.ProbeName as ProbeName,',
           ' str_to_date(date_format(VOProbeSummary.EndTime,''', @idatr, '''), ''', @idats, ''') as  DateValue',
           ' FROM ', @rankfrom, 'Site,Probe,VOProbeSummary',
           ' WHERE Probe.siteid = Site.siteid',
           '    and VOProbeSummary.ProbeName = Probe.probename',
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

    insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,p5,p6,p7,data)
        values('reports',@key,@iuser,@irole,@vo,
        @ifrom,@ito,@idatr,@idats,@itype,@iunit,@igroup,@sql);
    prepare statement from @sql;
    execute statement;
    deallocate prepare statement;


END $$

DELIMITER ;
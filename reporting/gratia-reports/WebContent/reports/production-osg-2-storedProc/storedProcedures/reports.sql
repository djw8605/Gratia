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

  IF userName IS NOT NULL then
    set @iuser := TRIM(userName);
  ELSE
    set @iuser := concat_ws('','GratiaUser|', UNIX_TIMESTAMP(), '|Unknown');
  END IF;

  IF userRole IS NOT NULL then
    set @irole := TRIM(userRole);
  ELSE
    set @irole := 'GratiaUser';
  END IF;

  IF fromdate IS NOT NULL then
    set @thisFromDate := fromdate;
    select STR_TO_DATE(fromdate, '%M %e, %Y') into @testDate;
    if @testDate IS NOT NULL then
      select  date_format(@testDate, '%Y-%m-%d') into @thisFromDate;
    end if;

    set @ifrom := concat_ws('','and VOProbeSummary.EndTIme >=''', TRIM(@thisFromDate), '''');
  ELSEIF todate IS NULL then
      set @ifrom := concat_ws('','and VOProbeSummary.EndTIme >=''', SUBDATE(CURDATE(), 800),'''');
  ELSE
      set @ifrom := '';
  END IF;

  IF todate IS NOT NULL then
    set @thisToDate := todate;
    select STR_TO_DATE(todate, '%M %e, %Y') into @testDate;
    if @testDate IS NOT NULL then
      select  date_format(@testDate, '%Y-%m-%d') into @thisToDate;
    end if;
    set @ito := concat_ws('','and VOProbeSummary.EndTime <= ''', TRIM(@thisToDate),'''');
  ELSE
    set @ito := '';
  END IF;

  IF timeUnit IS NOT NULL then
    set @iunit := TRIM(timeUnit);
  ELSE
    set @iunit := '3600';
  END IF;

  IF dateGrouping IS NOT NULL then
    set @igroup := dateGrouping;
    IF STRCMP(LOWER(TRIM(dateGrouping)), 'day') = 0 then
      set @idatr := '%Y-%m-%d';
      set @idats := '%Y-%m-%d';
    ELSEIF STRCMP(LOWER(TRIM(dateGrouping)),'week') = 0 then
      set @idatr := '%x-%v Monday';
      set @idats := '%x-%v %W';
    ELSEIF STRCMP(LOWER(TRIM(dateGrouping)), 'month') = 0  then
      set @idatr := '%Y-%m-01';
      set @idats := '%Y-%m-%d';
    ELSEIF STRCMP(LOWER(TRIM(dateGrouping)), 'year') = 0  then
      set @idatr := '%Y-01-01';
      set @idats := '%Y-%m-%d';
    ELSE
      set @idatr := '%Y-%m-%d';
      set @idats := '%Y-%m-%d';
    END IF;
  ELSE
    set @idatr := '%Y-%m-%d';
    set @idats := '%Y-%m-%d';
    set @igroup := 'day';
  END IF;

  IF groupBy IS NOT NULL then
    set @igroupBy := concat_ws('', 'group by ', TRIM(groupBy));
  ELSE
    set @igroupBy := 'group by DateValue, UserName, SiteName, VOName';
  END IF;

  IF orderBy IS NOT NULL then
    set @iorderBy := concat_ws('', 'order by ', TRIM(orderBy));
  ELSE
    set @iorderBy := 'order by UserName, SiteName, VOName, DateValue';
  END IF;

  IF resourceType IS NOT NULL then
    set @itype := TRIM(resourceType);
  ELSE
    set @itype := 'batch';
  END IF;

  IF selType IS NOT NULL then
    IF selType = 'NOT' then
      set @iselTypeA := ' NOT IN ';
      set @iselTypeB := '';
    ELSE
      set @iselTypeA := ' IN ';
      set @iselTypeB := '';
    END IF;
  ELSE
    set @iselTypeA := '=''';
    set @iselTypeB := '''';
  END IF;

  set @iVOs := '';
  set @rVOs := '';
  IF VOs IS NOT NULL then
    IF TRIM(VOs) != '' then
      set @iVOs := concat_ws('', 'and VOProbeSummary.VOName', @iselTypeA, '',TRIM(VOs), @iselTypeB, '');
      set @rVOs := concat_ws('', 'and V.VOName', @iselTypeA, '',TRIM(VOs), @iselTypeB, '');
    END IF;
  END IF;

  set @iSites := '';
  set @rSites := '';
  IF Sites IS NOT NULL then
    IF TRIM(Sites) != '' then
      set @iSites := concat_ws('', 'and Site.SiteName', @iselTypeA, '',TRIM(Sites), @iselTypeB, '');
      set @rSites := concat_ws('', 'and S.SiteName', @iselTypeA, '',TRIM(Sites), @iselTypeB, '');
    END IF;
  END IF;

  set @iProbes := '';
  set @rProbes := '';
  IF Probes IS NOT NULL then
    IF TRIM(Probes) != '' then
      set @iProbes := concat_ws('', 'and VOProbeSummary.ProbeName', @iselTypeA, '',TRIM(Probes), @iselTypeB, '');
      set @rProbes := concat_ws('', 'and P.ProbeName', @iselTypeA, '',TRIM(Probes), @iselTypeB, '');
    END IF;
  END IF;

  IF Ranked IS NOT NULL then
     set @rankfrom := concat_ws('',
           ' (SELECT @rank:=@rank+1 as final_rank, sitenamex, walldurationx',
           '   FROM (SELECT @rank:=0 as rank,',
           '      S.SiteName as sitenamex,',
           '      V.EndTime as endtimex,',
           '      sum(V.WallDuration)/', @iunit, ' as walldurationx',
           '        FROM VOProbeSummary V, Site S, Probe P',
           '        WHERE V.ProbeName = P.probename and P.siteid = S.Siteid',
           '        and (V.EndTime) >= (''', fromdate, ''')',
           '        and (V.EndTime) <= (''', todate, ''')',
           ' ', @rVOs,
           ' ', @rSites,
           ' ', @rProbes,
           '   GROUP BY sitenamex',
           '   ORDER BY walldurationx desc) as foox) as foo', ','
           );
     set @rankwhere := ' and Site.SiteName = sitenamex';
     set @final_rank := 'final_rank,';
  ELSE
     set @rankfrom := '';
     set @rankwhere := '';
     set @final_rank := '';
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
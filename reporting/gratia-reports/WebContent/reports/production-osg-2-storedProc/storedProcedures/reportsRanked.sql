DELIMITER $$

DROP PROCEDURE IF EXISTS `reportsRanked` $$
CREATE PROCEDURE `reportsRanked`(
          userName varchar(64), userRole varchar(64),
          fromdate varchar(64), todate varchar(64),
          timeUnit varchar(64), dateGrouping varchar(64),
          resourceType varchar(64), metric varchar(15),
          selValues varchar(1024),
          selType varchar(3)
          )
    READS SQL DATA
begin

  IF fromdate IS NOT NULL then
    set @thisFromDate := fromdate;
    select STR_TO_DATE(fromdate, '%M %e, %Y') into @testDate;
    IF @testDate IS NOT NULL then
      select  date_format(@testDate, '%Y-%m-%d') into @thisFromDate;
    END IF;
  END IF;
  
  IF todate IS NOT NULL then
    set @thisToDate := todate;
    select STR_TO_DATE(todate, '%M %e, %Y') into @testDate;
    if @testDate IS NOT NULL then
      select  date_format(@testDate, '%Y-%m-%d') into @thisToDate;
    end if;
  END IF;
  
  IF dateGrouping IS NOT NULL then
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
  END IF;


  IF timeUnit IS NOT NULL then
    set @iunit := TRIM(timeUnit);
  ELSE
    set @iunit := '3600';
  END IF;

  IF resourceType IS NOT NULL then
    set @itype := TRIM(resourceType);
  ELSE
    set @itype := 'batch';
  END IF;

  IF metric IS NOT NULL then
    set @imetric := TRIM(metric);
  ELSE
    set @imetric := '';
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

  set @iselValues := '';
  IF selValues IS NOT NULL then
    IF TRIM(selValues) != '' then
      set @iselValues := concat_ws('', 'and VO.VOName', @iselTypeA, '',TRIM(selValues), @iselTypeB, '');
    END IF;
  END IF;

  select sysdate() into @proc_start;
  select generateResourceTypeClause(resourceType) into @myresourceclause;
  select SystemProplist.cdr into @usereportauthentication from SystemProplist
  where SystemProplist.car = 'use.report.authentication';
  select Role.whereclause into @mywhereclause from Role
    where Role.role = userRole;
  select generateWhereClause(userName,userRole,@mywhereclause)
    into @mywhereclause;
  call parse(userName,@name,@key,@vo);
  select sysdate() into @query_start;

if @imetric = 'SiteCount' then
  set @sql :=
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
                        GROUP by VO.VOName
                        ORDER BY sitecntx DESC
                       ) AS foox
                 ) AS foo,
            (SELECT VOName,
                   sum(VOProbeSummary.WallDuration)/', @iunit, ' AS WallDuration,
                   sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration)/', @iunit, '  AS cpu,
                   sum(VOProbeSummary.Njobs) AS Njobs
             FROM VOProbeSummary, Probe, Site
             WHERE Probe.siteid = Site.siteid
                AND VOProbeSummary.probename = Probe.probename
                AND EndTime >= (''', @thisFromDate, ''')
                AND EndTime <= (''', @thisToDate, ''')',
                ' ', @myresourceclause,
                ' ', @mywhereclause,
           ' GROUP by VOProbeSummary.VOName
             ) AS VOProbeSummary
           WHERE VOProbeSummary.VOName = foo.vonamex
           ORDER BY final_rank, VOProbeSummary.VOName;'
      );
else
    set @sql :=
             concat_ws('',
                 ' select final_rank, VOname, DateValue, WallDuration, cpu, Njobs from (',
                 ' select final_rank,',
                 ' zVOProbeSummary.zVOName as VOName,',
                 ' zVOProbeSummary.zDateValue as DateValue,',
                 ' sum(zVOProbeSummary.zWallDuration) as WallDuration,'
                 ' sum(zVOProbeSummary.zcpu) as cpu,',
                 ' sum(zVOProbeSummary.zNjobs) as Njobs',
                 ' FROM',
                 ' (SELECT @rank:=@rank+1 as final_rank,',
                 '   foox.xVOName as oVOName, foox.x', @imetric,' as o', @imetric,'',
                 '   FROM ',
                 '   (SELECT @rank:=0 as rank,',
                 '     VO.VOName as xVOName, sum(V.m', @imetric,') as x', @imetric,'',
                 '     FROM ',
                 '     (SELECT VOCorrid as mVOCorrid,',
                 '        sum(', @imetric,') as m', @imetric,' ',
                 '         FROM MasterSummaryData',
                 '         WHERE (EndTime) >= (''', @thisFromDate, ''')',
                 '         and (EndTime) <= (''', @thisToDate, ''')',
                 '         and ResourceType = ''', @itype, '''',
                 '         GROUP by mVOCorrid',
                 '     ) V, VONameCorrection Corr, VO',
                 '     WHERE V.mVOCorrid = Corr.corrid and Corr.VOid = VO.VOid',
                 ' ', @iselValues,
                 '     GROUP BY xVOName',
                 '     ORDER BY x', @imetric,' desc',
                 '   ) as foox',
                 ' ) foo,',
                 ' (select VOProbeSummary.EndTime, VOProbeSummary.VOName as zVOName,',
                 '   sum(VOProbeSummary.WallDuration)/', @iunit, ' as zWallDuration,',
                 '   sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration)/', @iunit, ' as zcpu,',
                 '   sum(VOProbeSummary.Njobs) as zNjobs,',
                 '   str_to_date(date_format(VOProbeSummary.EndTime,''', @idatr, '''), ''', @idats, ''') as  zDateValue',
                 '   FROM VOProbeSummary',
                 '   WHERE (EndTime) >= (''', @thisFromDate, ''')'
                 '   and (EndTime) <= (''', @thisToDate, ''')',
                 '   and ResourceType = ''', @itype, '''',
                 '   GROUP by zDateValue, zVOName',
                 ' ) zVOProbeSummary',
                 ' WHERE zVOProbeSummary.zVOName = foo.oVOName',
                 ' GROUP by DateValue, VOName',
                 ' ORDER by final_rank, VOName, DateValue',
                 ') as realquery',
                 ';'
              );
end if;


  prepare statement from @sql;
  execute statement;
  insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,p5,p6,p7,p8,data)
    values('reportsRanked',@key,userName,userRole,@vo,
    @thisFromDate,@thisToDate,dateGrouping,resourceType,
    timediff(@query_start, @proc_start),
    timediff(sysdate(), @query_start),
    @idatr,@idats,
    @sql);
  deallocate prepare statement;

END $$

DELIMITER ;
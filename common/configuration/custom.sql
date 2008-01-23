DELIMITER ||

drop procedure if exists UsageBySite
||
create procedure UsageBySite (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
READS SQL DATA
begin

  select generateResourceTypeClause(resourceType) into @myresourceclause;
  select SystemProplist.cdr into @usereportauthentication from SystemProplist
  where SystemProplist.car = 'use.report.authentication';
  select Role.whereclause into @mywhereclause from Role
    where Role.role = userRole;
  select generateWhereClause(userName,userRole,@mywhereclause)
    into @mywhereclause;
  call parse(userName,@name,@key,@vo);
  if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) <= 1 and datediff(todate,fromdate) >= 0 then

  set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, JobUsageRecord_Report.EndTime as endtime,
                      sum(JobUsageRecord_Report.WallDuration) as WallDuration,
                      sum(JobUsageRecord_Report.CpuUserDuration +
                        JobUsageRecord_Report.CpuSystemDuration) as Cpu,
                      sum(JobUsageRecord_Report.Njobs) as Njobs',
                     ' from Site,Probe,',
                     '      (select JobUsageRecord.EndTime as endtime,
                                    sum(JobUsageRecord.WallDuration) as WallDuration,
                                    sum(JobUsageRecord.CpuUserDuration +
                                         JobUsageRecord.CpuSystemDuration) as Cpu,
                                   sum(JobUsageRecord.Njobs) as Njobs',
                            'from JobUsageRecord',
                     '       where '
                     '          JobUsageRecord.WallDuration is not null',
                     '          and JobUsageRecord.CpuUserDuration is not null',
                     '          and JobUsageRecord.CpuSystemDuration is not null',
                     '          and',
                     '          (EndTime) >= (''', fromdate, ' 00:00:00 '')',
                     '          and (EndTime) <= (''', todate, ' 00:00:00 '')',
                     '          ', @myresourceclause,
                     '          ', @mywhereclause,
                     '       group by ProbeName',
                     '     ) JobUsageRecord_Report',
                     ' where',
                     ' Probe.siteid = Site.siteid and JobUsageRecord_Report.ProbeName = Probe.probename',
                     ' group by Site.SiteName',
                     ' order by Site.SiteName'
                    );

  else
    -- Use summary table
    set @sql :=
           concat_ws('', 
                     'select Site.SiteName as sitename, VOProbeSummary.EndTime as endtime,
                      sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.Cpu) as Cpu,
                      sum(VOProbeSummary.Njobs) as Njobs',
                     ' from Site,Probe,
                            (select  EndTime as endtime,
                                     sum(WallDuration) as WallDuration,
                                     sum(CpuUserDuration + CpuSystemDuration) as Cpu,
                                     sum(Njobs) as Njobs,',
                     '               ProbeName',
                     '       from VOProbeSummaryData',
                     '       where',
                     '          EndTime >= ''', fromdate, ' 00:00:00'' ',
                     '          and EndTime <= ''', todate, ' 00:00:00'' ',
                     '          ', @myresourceclause,
                     '          ', @mywhereclause,
                     '       group by ProbeName) VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename',
                     ' group by Site.SiteName'
                     ' order by Site.SiteName'
                 );
  end if;
  insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
    values('UsageBySite',@key,userName,userRole,@vo,
    fromdate,todate,format,resourceType,@sql);
  prepare statement from @sql;
  execute statement;
  deallocate prepare statement;
end
||
-- call UsageBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call UsageBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call UsageBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call UsageBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

drop procedure if exists DailyUsageBySiteByVO
||
create procedure DailyUsageBySiteByVO (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64), forvo varchar(64), forvoname varchar(64), forsite varchar(64), forsitename varchar(64))
READS SQL DATA
begin

  select generateResourceTypeClause(resourceType) into @myresourceclause;
  select SystemProplist.cdr into @usereportauthentication from SystemProplist
  where SystemProplist.car = 'use.report.authentication';
  select Role.whereclause into @mywhereclause from Role
    where Role.role = userRole;
  select generateWhereClause(userName,userRole,@mywhereclause)
    into @mywhereclause;
  call parse(userName,@name,@key,@vo);
  if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) <= 1 and datediff(todate,fromdate) >= 0 then

  set @sql :=
           concat_ws('', 'select date_format(JobUsageRecord_Report.EndTime,''', format, ''') as endtime, Site.SiteName as sitename,
                      sum(JobUsageRecord_Report.WallDuration) as WallDuration,
                      sum(JobUsageRecord_Report.CpuUserDuration + JobUsageRecord_Report.CpuSystemDuration) as Cpu,
                      JobUsageRecord_Report.VOName, sum(JobUsageRecord_Report.Njobs) as Njobs',
                     ' from Site,Probe,JobUsageRecord_Report',
                     ' where',
                     ' Probe.siteid = Site.siteid and JobUsageRecord_Report.ProbeName = Probe.probename
             and (JobUsageRecord_Report.VOName =''', forvoname, ''' or
                  (''', forvo, ''' = ''AnyVO'' and
                   JobUsageRecord_Report.VOName like ''%'' ))
             and (Site.SiteName =''', forsitename, ''' or
                  (''', forsite, ''' = ''AnySite'' and
                   Site.SiteName like ''%'' ))',
                     ' and ',
                     '     JobUsageRecord_Report.WallDuration is not null',
                     ' and JobUsageRecord_Report.CpuUserDuration is not null',
                     ' and JobUsageRecord_Report.CpuSystemDuration is not null',
                     ' and',
                     ' (EndTime) >= (''', fromdate, ' 00:00:00 '')',
                     ' and (EndTime) <= (''', todate, ' 00:00:00 '')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(JobUsageRecord_Report.EndTime,''', format, '''), sitename, JobUsageRecord_Report.VOName'
                     , ' order by date_format(JobUsageRecord_Report.EndTime,''', format, '''), sitename, JobUsageRecord_Report.VOName'
                    );

  else
    -- Use summary table
    set @sql :=
         concat_ws('', 'select EndTime as endtime, Site.SiteName as sitename,
                      sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.Cpu) as Cpu,
                      VO.VOName, sum(VOProbeSummary.Njobs) as Njobs',
                     ' from Site,Probe, ',
                     '      (select date_format(VOProbeSummaryData.EndTime,''', format, ''') as EndTime, sum(VOProbeSummaryData.WallDuration) as WallDuration,
                      sum(VOProbeSummaryData.CpuUserDuration + VOProbeSummaryData.CpuSystemDuration) as Cpu,
                      sum(VOProbeSummaryData.Njobs) as Njobs, ProbeName, VOcorrid'
                     '   from VOProbeSummaryData ',
                     '   where ',
                     '     (EndTime) >= (''', fromdate, ' 00:00:00 '')',
                     '     and (EndTime) <= (''', todate, ' 00:00:00 '')',
                     '    ', @myresourceclause,
                     '    ', @mywhereclause,
                     '     and VOcorrid IN(select corrid from VONameCorrection VC, VO where (`VC`.`VOid` = `VO`.`VOid`) and (VO.VOName =''', forvoname, ''' or (''', forvo, ''' = ''AnyVO'' and VO.VOName like ''%'' ))) ',
                     '     and ProbeName IN(select ProbeName from Probe,Site where Probe.siteid = Site.siteid and (Site.SiteName =''', forsitename, ''' or (''', forsite, ''' = ''AnySite'' and Site.SiteName like ''%'' )))',
                     '     group by date_format(VOProbeSummaryData.EndTime,''', format, '''), ProbeName, VOcorrid ',
                     '    ) VOProbeSummary, VO, VONameCorrection VC',
                     ' where',
                     ' ((`VOProbeSummary`.`VOcorrid` = `VC`.`corrid`) and (`VC`.`VOid` = `VO`.`VOid`)) and ',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename',
                     ' group by EndTime, Site.SiteName, VO.VOName'
                     ' order by EndTime, Site.SiteName, VO.VOName'
                 );
  end if;
  insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
    values('DailyUsageBySiteByVO',@key,userName,userRole,@vo,
    fromdate,todate,format,resourceType,@sql);
  prepare statement from @sql;
  execute statement;
  deallocate prepare statement;
end
||
-- call DailyUsageBySiteByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyUsageBySiteByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','')
-- ||
-- call DailyUsageBySiteByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyUsageBySiteByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','')
-- ||

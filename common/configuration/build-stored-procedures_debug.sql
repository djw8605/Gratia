delimiter ||

drop table if exists trace
||
CREATE TABLE trace (
  eventtime TIMESTAMP NOT NULL,
	pname varchar(64),
	userkey varchar(64),
	user varchar(64),
	role varchar(64),
	vo varchar(64),
	p1 varchar(64),
	p2 varchar(64),
	p3 varchar(64),
	p4 varchar(64),
	p5 varchar(64),
	p6 varchar(64),
	p7 varchar(64),
	p8 varchar(64),
	p9 varchar(64),
  data TEXT
)
||
drop procedure if exists parse
||
create procedure parse(username varchar(64),out outname varchar(64),
	out outkey varchar(64),out outvo varchar(64))
begin
	set outname = '';
	set outkey = '';
	set outvo = '';
	set @username = username;
	set @index = locate('|',@username);
	if @index > 0 then
		set outname = substring(@username,1,@index - 1);
		set @username = substring(@username,@index + 1);
	end if;
	set @index = locate('|',@username);
	if @index > 0 then
		set outkey = substring(@username,1,@index - 1);
		set outvo = substring(@username,@index + 1);
	else
		set outkey = @username;
	end if;
	insert into trace(pname,p1,user,userkey,vo) values('parse',username,outname,outkey,outvo);
end
||
drop function if exists generateWhereClause
||
create function generateWhereClause(userName varchar(64),userRole varchar(64),
	whereClause varchar(255)) returns varchar(255)
READS SQL DATA
begin
	select SystemProplist.cdr into @usereportauthentication from SystemProplist
		where SystemProplist.car = 'use.report.authentication';
	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' then
		return '';
	end if;
	if whereClause = 'Everything' then
		return '';
	end if;
	return concat(' and ',whereClause,' ');
end
||
drop function if exists generateResourceTypeClause
||
create function generateResourceTypeClause(resourceType varchar(64))
	returns varchar(255)
DETERMINISTIC
begin
	if resourceType = '' or resourceType = NULL then
		return '';
	else
		return concat(
			' and ResourceType = ''',
			resourceType,
			'''');
	end if;
end
||
drop procedure if exists ProbeStatus
||
create procedure ProbeStatus (userName varchar(64),userRole varchar(64),
	fromdate varchar(64),todate varchar(64),format varchar(64))
READS SQL DATA
begin

	declare mywhereclause varchar(255);
	set @myfromdate := fromdate;
	set @mytodate := todate;

	insert into trace(pname,userkey) values('ProbeStatus','step00');
	select SystemProplist.cdr into @usereportauthentication from SystemProplist
		where SystemProplist.car = 'use.report.authentication';
	insert into trace(pname,userkey) values('ProbeStatus','step01');
	select Role.whereclause into @mywhereclause from Role
		where Role.role = userRole;
	insert into trace(pname,userkey) values('ProbeStatus','step02');
	select generateWhereClause(userName,userRole,@mywhereclause)
		into @mywhereclause;
	insert into trace(pname,userkey) values('ProbeStatus','step03');
	call parse(userName,@name,@key,@vo);
	insert into trace(pname,userkey) values('ProbeStatus','step04');

	insert into trace(pname,userkey,user,role,vo,p1,p2)
		values('ProbeStatus',@key,userName,userRole,@vo,fromdate,todate);

	if @mywhereclause = '' then
			insert into trace(pname,userKey) values('ProbeStatus','Got It !!');
			select ProbeName,EndTime as endtime,Njobs as Njobs
				from ProbeStatus
				where
					EndTime >= fromdate and EndTime <= todate
				group by EndTime,ProbeName
				order by EndTime;
	end if;
end
||
-- call ProbeStatus('GratiaGlobalAdmin','GratiaUser','2006-01-01 00:00:00','2007-12-31 00:00:00','ignore');
-- ||

drop procedure if exists DailyJobsByProbe
||
create procedure DailyJobsByProbe (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select ProbeSummary.ProbeName, ProbeSummary.EndTime as endtime,
                      sum(ProbeSummary.Njobs) as Njobs',
                     ' from ProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(ProbeSummary.EndTime,''', format, '''), ProbeSummary.ProbeName'
                     , ' order by ProbeSummary.EndTime'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select ProbeSummary.ProbeName, ProbeSummary.EndTime as endtime,
                      sum(ProbeSummary.Njobs) as Njobs',
                     ' from ProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(ProbeSummary.EndTime,''', format, '''), ProbeSummary.ProbeName'
                     , ' order by ProbeSummary.EndTime'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('DailyJobsByProbe',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call DailyJobsByProbe('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyJobsByProbe('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','')
-- ||
-- call DailyJobsByProbe('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyJobsByProbe('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','')
-- ||

drop procedure if exists DailyJobsBySite
||
create procedure DailyJobsBySite (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, ProbeSummary.EndTime as endtime,
                      sum(ProbeSummary.Njobs) as Njobs',
                     ' from Site,Probe,ProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and ProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(ProbeSummary.EndTime,''', format, '''), Site.SiteName'
                     , ' order by ProbeSummary.EndTime'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, ProbeSummary.EndTime as endtime,
                      sum(ProbeSummary.Njobs) as Njobs',
                     ' from Site,Probe,ProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and ProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(ProbeSummary.EndTime,''', format, '''), Site.SiteName'
                     , ' order by ProbeSummary.EndTime'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('DailyJobsBySite',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call DailyJobsBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyJobsBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','')
-- ||
-- call DailyJobsBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyJobsBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','')
-- ||

drop procedure if exists DailyJobsByVO
||
create procedure DailyJobsByVO (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select VOProbeSummary.VOName, VOProbeSummary.EndTime as endtime,
                      sum(VOProbeSummary.Njobs) as Njobs',
                     ' from VOProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(VOProbeSummary.EndTime,''', format, '''), VOProbeSummary.VOName'
                     , ' order by VOProbeSummary.EndTime'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select VOProbeSummary.VOName, VOProbeSummary.EndTime as endtime,
                      sum(VOProbeSummary.Njobs) as Njobs',
                     ' from VOProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(VOProbeSummary.EndTime,''', format, '''), VOProbeSummary.VOName'
                     , ' order by VOProbeSummary.EndTime'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('DailyJobsByVO',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call DailyJobsByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyJobsByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','')
-- ||
-- call DailyJobsByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyJobsByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','')
-- ||

drop procedure if exists DailyUsageByProbe
||
create procedure DailyUsageByProbe (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select ProbeSummary.ProbeName, ProbeSummary.EndTime as endtime,
                      sum(ProbeSummary.WallDuration) as WallDuration',
                     ' from ProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(ProbeSummary.EndTime,''', format, '''), ProbeSummary.ProbeName'
                     , ' order by ProbeSummary.EndTime'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select ProbeSummary.ProbeName, ProbeSummary.EndTime as endtime,
                      sum(ProbeSummary.WallDuration) as WallDuration',
                     ' from ProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(ProbeSummary.EndTime,''', format, '''), ProbeSummary.ProbeName'
                     , ' order by ProbeSummary.EndTime'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('DailyUsageByProbe',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call DailyUsageByProbe('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyUsageByProbe('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','')
-- ||
-- call DailyUsageByProbe('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyUsageByProbe('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','')
-- ||

drop procedure if exists DailyUsageBySite
||
create procedure DailyUsageBySite (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, ProbeSummary.EndTime as endtime,
                      sum(ProbeSummary.WallDuration) as WallDuration',
                     ' from Site,Probe,ProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and ProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(ProbeSummary.EndTime,''', format, '''),Site.SiteName'
                     , ' order by ProbeSummary.EndTime'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, ProbeSummary.EndTime as endtime,
                      sum(ProbeSummary.WallDuration) as WallDuration',
                     ' from Site,Probe,ProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and ProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(ProbeSummary.EndTime,''', format, '''),Site.SiteName'
                     , ' order by ProbeSummary.EndTime'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('DailyUsageBySite',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call DailyUsageBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyUsageBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','')
-- ||
-- call DailyUsageBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyUsageBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','')
-- ||

drop procedure if exists DailyUsageBySiteByVO
||
create procedure DailyUsageBySiteByVO (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select date_format(VOProbeSummary.EndTime,''', format, ''') as endtime, Site.SiteName as sitename,
                      sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu,
                      VOProbeSummary.VOName, sum(VOProbeSummary.Njobs) as Njobs',
                     ' from Site,Probe,VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(date_format(VOProbeSummary.EndTime,''', format, '''),''', format, '''), sitename, VOProbeSummary.VOName'
                     , ' order by date_format(VOProbeSummary.EndTime,''', format, '''), sitename, VOProbeSummary.VOName'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select date_format(VOProbeSummary.EndTime,''', format, ''') as endtime, Site.SiteName as sitename,
                      sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu,
                      VOProbeSummary.VOName, sum(VOProbeSummary.Njobs) as Njobs',
                     ' from Site,Probe,VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(date_format(VOProbeSummary.EndTime,''', format, '''),''', format, '''), sitename, VOProbeSummary.VOName'
                     , ' order by date_format(VOProbeSummary.EndTime,''', format, '''), sitename, VOProbeSummary.VOName'
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

drop procedure if exists DailyUsageByVO
||
create procedure DailyUsageByVO (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select VOProbeSummary.VOName, VOProbeSummary.EndTime as endtime,
                      sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu',
                     ' from VOProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(VOProbeSummary.EndTime,''', format, '''), VOProbeSummary.VOName'
                     , ' order by VOProbeSummary.EndTime'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select VOProbeSummary.VOName, VOProbeSummary.EndTime as endtime,
                      sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu',
                     ' from VOProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(VOProbeSummary.EndTime,''', format, '''), VOProbeSummary.VOName'
                     , ' order by VOProbeSummary.EndTime'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('DailyUsageByVO',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call DailyUsageByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyUsageByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d','')
-- ||
-- call DailyUsageByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','Batch')
-- ||
-- call DailyUsageByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d','')
-- ||

drop procedure if exists DcacheBySource
||
create procedure DcacheBySource (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select avg(N.Value/N.PhaseUnit/1024.0/1024.0) as RateInMBPerSecond, date_format(J.EndTime,''', format , ''') as Day,
                      J.SiteName,Resource.value as Source',
                     ' from JobUsageRecord J, Network N, Resource R, ',
                     ' where',
                     ' J.ResourceType = ''Storage'' and J.dbid = N.dbid
              and J.dbid = Resource.dbid
              and Resource.Description = ''Source'' and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by Day, SiteName'
                     , ' order by Day'
                    );

	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('DcacheBySource',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call DcacheBySource('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call DcacheBySource('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call DcacheBySource('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call DcacheBySource('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

drop procedure if exists JobsByProbeNoFacility
||
create procedure JobsByProbeNoFacility (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select ProbeSummary.ProbeName, sum(ProbeSummary.Njobs) as Njobs',
                     ' from ProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by ProbeSummary.ProbeName'
                     , ' order by ProbeSummary.ProbeName'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select ProbeSummary.ProbeName, sum(ProbeSummary.Njobs) as Njobs',
                     ' from ProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by ProbeSummary.ProbeName'
                     , ' order by ProbeSummary.ProbeName'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('JobsByProbeNoFacility',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call JobsByProbeNoFacility('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call JobsByProbeNoFacility('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call JobsByProbeNoFacility('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call JobsByProbeNoFacility('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

drop procedure if exists JobsBySite
||
create procedure JobsBySite (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, sum(ProbeSummary.Njobs) as Njobs',
                     ' from Site,Probe,ProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and ProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by Site.SiteName'
                     , ' order by Site.SiteName'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, sum(ProbeSummary.Njobs) as Njobs',
                     ' from Site,Probe,ProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and ProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by Site.SiteName'
                     , ' order by Site.SiteName'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('JobsBySite',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call JobsBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call JobsBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call JobsBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call JobsBySite('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

drop procedure if exists JobsBySiteByVO
||
create procedure JobsBySiteByVO (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, sum(VOProbeSummary.Njobs) as Njobs,
                      VOProbeSummary.VOName',
                     ' from Site,Probe,VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by sitename, VOProbeSummary.VOName'
                     , ' order by sitename, VOProbeSummary.VOName'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, sum(VOProbeSummary.Njobs) as Njobs,
                      VOProbeSummary.VOName',
                     ' from Site,Probe,VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by sitename, VOProbeSummary.VOName'
                     , ' order by sitename, VOProbeSummary.VOName'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('JobsBySiteByVO',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call JobsBySiteByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call JobsBySiteByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call JobsBySiteByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call JobsBySiteByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

drop procedure if exists JobsByVO
||
create procedure JobsByVO (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select VOProbeSummary.VOName, sum(VOProbeSummary.Njobs) as Njobs',
                     ' from VOProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by VOProbeSummary.VOName'
                     , ' order by VOProbeSummary.VOName'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select VOProbeSummary.VOName, sum(VOProbeSummary.Njobs) as Njobs',
                     ' from VOProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by VOProbeSummary.VOName'
                     , ' order by VOProbeSummary.VOName'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('JobsByVO',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call JobsByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call JobsByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call JobsByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call JobsByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

drop procedure if exists UsageByProbe
||
create procedure UsageByProbe (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select ProbeSummary.ProbeName, sum(ProbeSummary.WallDuration) as WallDuration',
                     ' from ProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by ProbeSummary.ProbeName'
                     , ' order by ProbeSummary.ProbeName'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select ProbeSummary.ProbeName, sum(ProbeSummary.WallDuration) as WallDuration',
                     ' from ProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by ProbeSummary.ProbeName'
                     , ' order by ProbeSummary.ProbeName'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('UsageByProbe',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call UsageByProbe('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call UsageByProbe('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call UsageByProbe('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call UsageByProbe('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

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

	set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, VOProbeSummary.EndTime as endtime,
                      sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu',
                     ' from Site,Probe,VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by Site.SiteName'
                     , ' order by Site.SiteName'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, VOProbeSummary.EndTime as endtime,
                      sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu',
                     ' from Site,Probe,VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by Site.SiteName'
                     , ' order by Site.SiteName'
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

drop procedure if exists UsageBySiteByVO
||
create procedure UsageBySiteByVO (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu,
                      VOProbeSummary.VOName',
                     ' from Site,Probe,VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by sitename, VOProbeSummary.VOName'
                     , ' order by sitename, VOProbeSummary.VOName'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu,
                      VOProbeSummary.VOName',
                     ' from Site,Probe,VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename and',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by sitename, VOProbeSummary.VOName'
                     , ' order by sitename, VOProbeSummary.VOName'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('UsageBySiteByVO',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call UsageBySiteByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call UsageBySiteByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call UsageBySiteByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call UsageBySiteByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

drop procedure if exists UsageBySiteByVO1
||
create procedure UsageBySiteByVO1 (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64), forvo varchar(64), forvoname varchar(64), forsite varchar(64), forsitename varchar(64))
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

	set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, sum(VOProbeSummary.WallDuration) as WallDuration,
					  sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu,
					  sum(VOProbeSummary.Njobs) as Njobs,
					  VOProbeSummary.VOName',
                     ' from Site,Probe,VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename 
	 		 and (VOProbeSummary.VOName =''', forvoname, ''' or (''', forvo, ''' = ''AnyVO'' and VOProbeSummary.VOName like ''%'' ))
			 and (Site.SiteName =''', forsitename, ''' or (''', forsite, ''' = ''AnySite'' and Site.SiteName like ''%'' )) and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by sitename, VOProbeSummary.VOName'
                     , ' order by sitename, VOProbeSummary.VOName'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, sum(VOProbeSummary.WallDuration) as WallDuration,
					  sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu,
					  sum(VOProbeSummary.Njobs) as Njobs,
					  VOProbeSummary.VOName',
                     ' from Site,Probe,VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename 
	 		 and (VOProbeSummary.VOName =''', forvoname, ''' or (''', forvo, ''' = ''AnyVO'' and VOProbeSummary.VOName like ''%'' ))
			 and (Site.SiteName =''', forsitename, ''' or (''', forsite, ''' = ''AnySite'' and Site.SiteName like ''%'' )) and',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by sitename, VOProbeSummary.VOName'
                     , ' order by sitename, VOProbeSummary.VOName'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('UsageBySiteByVO1',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call UsageBySiteByVO1('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call UsageBySiteByVO1('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call UsageBySiteByVO1('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call UsageBySiteByVO1('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

drop procedure if exists UsageByUser
||
create procedure UsageByUser (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64), forvo varchar(64), forvoname varchar(64), forsite varchar(64), forsitename varchar(64))
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

	set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, sum(VOProbeSummary.WallDuration) as WallDuration,
					  sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu,
					  sum(VOProbeSummary.Njobs) as Njobs,
					  VOProbeSummary.VOName,
					  VOProbeSummary.CommonName as UserName',
                     ' from Site,Probe,VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename
	 					and (VOProbeSummary.VOName =''', forvoname, ''' or (''', forvo, ''' = ''AnyVO'' and VOProbeSummary.VOName like ''%'' ))
						and (Site.SiteName =''', forsitename, ''' or (''', forsite, ''' = ''AnySite'' and Site.SiteName like ''%'' )) and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by UserName, sitename, VOProbeSummary.VOName'
                     , ' order by UserName, sitename, VOProbeSummary.VOName'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select Site.SiteName as sitename, sum(VOProbeSummary.WallDuration) as WallDuration,
					  sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu,
					  sum(VOProbeSummary.Njobs) as Njobs,
					  VOProbeSummary.VOName,
					  VOProbeSummary.CommonName as UserName',
                     ' from Site,Probe,VOProbeSummary',
                     ' where',
                     ' Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename
	 					and (VOProbeSummary.VOName =''', forvoname, ''' or (''', forvo, ''' = ''AnyVO'' and VOProbeSummary.VOName like ''%'' ))
						and (Site.SiteName =''', forsitename, ''' or (''', forsite, ''' = ''AnySite'' and Site.SiteName like ''%'' )) and',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by UserName, sitename, VOProbeSummary.VOName'
                     , ' order by UserName, sitename, VOProbeSummary.VOName'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('UsageByUser',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call UsageByUser('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call UsageByUser('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call UsageByUser('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call UsageByUser('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

drop procedure if exists UsageByVO
||
create procedure UsageByVO (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select VOProbeSummary.VOName, sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu',
                     ' from VOProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by VOProbeSummary.VOName'
                     , ' order by VOProbeSummary.VOName'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select VOProbeSummary.VOName, sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu',
                     ' from VOProbeSummary',
                     ' where',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by VOProbeSummary.VOName'
                     , ' order by VOProbeSummary.VOName'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('UsageByVO',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call UsageByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call UsageByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call UsageByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call UsageByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

drop procedure if exists WeeklyJobsByVO
||
create procedure WeeklyJobsByVO (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64), vos varchar(128), voseltype varchar(8))
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
	-- Inelegant kludge to get around trouble .jsp has handling
	-- arguments with embedded spaces; this will be unnecessary when
	-- the view code gets rewritten.
	if voseltype = 'NOT' then
		set voseltype := 'NOT IN';
	end if;

	set @sql :=
           concat_ws('', 'select VOProbeSummary.VOName, date_format(VOProbeSummary.EndTime, ''', format, ''') as endtime,
                      sum(VOProbeSummary.Njobs) as Njobs',
                     ' from VOProbeSummary',
                     ' where',
                     ' VOProbeSummary.VOName ', voseltype, vos, ' and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(VOProbeSummary.EndTime,''', format, '''), VOProbeSummary.VOName'
                     , ' order by VOProbeSummary.EndTime'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select VOProbeSummary.VOName, date_format(VOProbeSummary.EndTime, ''', format, ''') as endtime,
                      sum(VOProbeSummary.Njobs) as Njobs',
                     ' from VOProbeSummary',
                     ' where',
                     ' VOProbeSummary.VOName ', voseltype, vos, ' and',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(VOProbeSummary.EndTime,''', format, '''), VOProbeSummary.VOName'
                     , ' order by VOProbeSummary.EndTime'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('WeeklyJobsByVO',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call WeeklyJobsByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call WeeklyJobsByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call WeeklyJobsByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call WeeklyJobsByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

drop procedure if exists WeeklyUsageByVO
||
create procedure WeeklyUsageByVO (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64), vos varchar(128), voseltype varchar(8))
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
	-- Inelegant kludge to get around trouble .jsp has handling
	-- arguments with embedded spaces; this will be unnecessary when
	-- the view code gets rewritten.
	if voseltype = 'NOT' then
		set voseltype := 'NOT IN';
	end if;

	set @sql :=
           concat_ws('', 'select VOProbeSummary.VOName, date_format(VOProbeSummary.EndTime, ''', format, ''') as endtime,
                      sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu,
                      sum(VOProbeSummary.Njobs) as Njobs',
                     ' from VOProbeSummary',
                     ' where',
                     ' VOProbeSummary.VOName ', voseltype, vos, ' and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(VOProbeSummary.EndTime,''', format, '''), VOProbeSummary.VOName'
                     , ' order by VOProbeSummary.EndTime'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select VOProbeSummary.VOName, date_format(VOProbeSummary.EndTime, ''', format, ''') as endtime,
                      sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu,
                      sum(VOProbeSummary.Njobs) as Njobs',
                     ' from VOProbeSummary',
                     ' where',
                     ' VOProbeSummary.VOName ', voseltype, vos, ' and',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by date_format(VOProbeSummary.EndTime,''', format, '''), VOProbeSummary.VOName'
                     , ' order by VOProbeSummary.EndTime'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('WeeklyUsageByVO',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call WeeklyUsageByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call WeeklyUsageByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call WeeklyUsageByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call WeeklyUsageByVO('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||

drop procedure if exists WeeklyUsageByVORanked
||
create procedure WeeklyUsageByVORanked (userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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

	set @sql :=
           concat_ws('', 'select final_rank, VOProbeSummary.VOName,
                      date_format(VOProbeSummary.EndTime,''', format, ''') as datevalue,
                      sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu,
                      sum(VOProbeSummary.Njobs) as Njobs',
                     ' from (SELECT @rank:=@rank+1 as final_rank, VONamex,
                    walldurationx
                    FROM (SELECT @rank:=0 as rank,
                          V.VOName as VONamex,
                          V.EndTime as endtimex,
                          sum(V.WallDuration) as walldurationx
                          FROM VOProbeSummary V
                          WHERE V.EndTime >= Date(''', fromdate, ''')
                            and V.EndTime <= Date(''', todate, ''')
                          GROUP BY VONamex
                          ORDER BY walldurationx desc) as foox) as foo, VOProbeSummary',
                     ' where',
                     ' VOProbeSummary.VOName = VONamex and',
                     ' EndTime >= date(''', fromdate, ''')'
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by datevalue, VOProbeSummary.VOName'
                     , ' order by final_rank, VOProbeSummary.VOName,datevalue'
                    );

    if ( @mywhereclause = '' or @mywhereclause is NULL ) and datediff(todate,fromdate) > 6 then
		-- Use summary table
		set @sql :=
           concat_ws('', 'select final_rank, VOProbeSummary.VOName,
                      date_format(VOProbeSummary.EndTime,''', format, ''') as datevalue,
                      sum(VOProbeSummary.WallDuration) as WallDuration,
                      sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) as Cpu,
                      sum(VOProbeSummary.Njobs) as Njobs',
                     ' from (SELECT @rank:=@rank+1 as final_rank, VONamex,
                    walldurationx
                    FROM (SELECT @rank:=0 as rank,
                          V.VOName as VONamex,
                          V.EndTime as endtimex,
                          sum(V.WallDuration) as walldurationx
                          FROM VOProbeSummary V
                          WHERE V.EndTime >= Date(''', fromdate, ''')
                            and V.EndTime <= Date(''', todate, ''')
                          GROUP BY VONamex
                          ORDER BY walldurationx desc) as foox) as foo, VOProbeSummary',
                     ' where',
                     ' VOProbeSummary.VOName = VONamex and',
                     ' EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')',
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by datevalue, VOProbeSummary.VOName'
                     , ' order by final_rank, VOProbeSummary.VOName,datevalue'
                 );
	end if;
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
		values('WeeklyUsageByVORanked',@key,userName,userRole,@vo,
		fromdate,todate,format,resourceType,@sql);
	prepare statement from @sql;
	execute statement;
	deallocate prepare statement;
end
||
-- call WeeklyUsageByVORanked('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call WeeklyUsageByVORanked('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-10 00:00:00','%y:%m:%d:%H:%i','')
-- ||
-- call WeeklyUsageByVORanked('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','Batch')
-- ||
-- call WeeklyUsageByVORanked('GratiaUser','GratiaUser','2007-02-01 00:00:00','2007-02-04 00:00:00','%y:%m:%d:%H:%i','')
-- ||


-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

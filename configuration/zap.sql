delimiter ||
drop table trace
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
drop procedure DailyJobsByProbe
||
create procedure DailyJobsByProbe (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
begin

	declare mywhereclause varchar(255);
	set @myfromdate := fromdate;
	set @mytodate := todate;
	set @myformat := format;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @mysql :=
		concat(
			'select ProbeName,EndTime as endtime,sum(Njobs) as Njobs',
			' from JobUsageRecord',
			' where',
			'	EndTime >= date(?) and EndTime <= date(?)',
			@mywhereclause,
			' group by date_format(EndTime,?),ProbeName',
			' order by date_format(EndTime,?)');
	insert into trace(pname,userkey) values('test',@mysql);
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data) 
		values('DailyJobsByProbe',@key,userName,userRole,@vo,@myfromdate,@mytodate,@myformat,@myformat,@mysql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select ProbeName,EndTime as endtime,sum(Njobs) as Njobs
				from ProbeSummary
				where
					EndTime >= date(fromdate) and EndTime <= date(todate)
				group by EndTime,ProbeName
				order by EndTime;
		else
			select ProbeName,date_format(EndTime,format) as endtime,sum(Njobs) as Njobs
				from JobUsageRecord
				where
					EndTime >= fromdate and EndTime <= todate
				group by date_format(EndTime,format),ProbeName
				order by date_format(EndTime,format);
		end if;
	else
		prepare statement from @mysql;
		execute statement using @myfromdate,@mytodate,@myformat,@myformat;
		deallocate prepare statement;
	end if;
end
||

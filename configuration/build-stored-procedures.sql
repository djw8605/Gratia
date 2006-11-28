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
drop procedure parse
||
create procedure parse(username varchar(64),out outname varchar(64),out outkey varchar(64),out outvo varchar(64))
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
	-- insert into trace(p1,p2,p3,p4) values(username,outname,outkey,outvo);
end
||
drop function generateWhereClause
||
create function generateWhereClause(userName varchar(64),userRole varchar(64),whereClause varchar(255)) returns varchar(255)
begin
	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' then
		return '';
	end if;
	if whereClause = 'Everything' then
		return '';
	end if;
	return concat(' and ',whereClause,' ');
end
||
drop procedure DailyJobsByFacility
||
create procedure DailyJobsByFacility (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;
	set @myformat := format;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select CETable.facility_name,JobUsageRecord.EndTime as endtime,sum(JobUsageRecord.Njobs) as Njobs',
			' from CETable,CEProbes,JobUsageRecord',
			' where',
			' CEProbes.facility_id = CETable.facility_id',
			' and JobUsageRecord.ProbeName = CEProbes.probename',
			' and EndTime >= date(?)',
			' and EndTime <= date(?)',
			@mywhereclause,
			' group by date_format(JobUsageRecord.EndTime,?),CETable.facility_name',
			' order by date_format(JobUsageRecord.EndTime,?)');
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data) 
		values('DailyJobsByFacility',@key,userName,userRole,@vo,@myfromdate,@mytodate,@myformat,@myformat,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select CETable.facility_name,ProbeSummary.EndTime as endtime,sum(ProbeSummary.Njobs) as Njobs
				from CETable,CEProbes,ProbeSummary
				where
					CEProbes.facility_id = CETable.facility_id
					and ProbeSummary.ProbeName = CEProbes.probename
					and EndTime >= date(fromdate)
					and EndTime <= date(todate)
				group by ProbeSummary.EndTime,CETable.facility_name
				order by ProbeSummary.EndTime;
		else
			select CETable.facility_name,JobUsageRecord.EndTime as endtime,sum(JobUsageRecord.Njobs) as Njobs
				from CETable,CEProbes,JobUsageRecord
				where
					CEProbes.facility_id = CETable.facility_id
					and JobUsageRecord.ProbeName = CEProbes.probename
					and EndTime >= fromdate
					and EndTime <= todate
				group by date_format(JobUsageRecord.EndTime,format),CETable.facility_name
				order by date_format(JobUsageRecord.EndTime,format);
		end if;
	else
		prepare statement from @sql;
		execute statement using @myfromdate,@mytodate,@myformat,@myformat;
		deallocate prepare statement;
	end if;
end
||
-- call DailyJobsByFacility('GratiaUser|1234|Unknown','GratiaUser','2006-09-01 00:00:00','2006-09-15 00:00:00','%y:%m:%d:%H:%i')
||
drop procedure DailyJobsByFacilityAndDate
||
create procedure DailyJobsByFacilityAndDate (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
begin

	declare mywhereclause varchar(255);
	set @myfromdate := fromdate;
	set @mytodate := todate;
	set @myformat := format;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select CETable.facility_name,date_format(JobUsageRecord.EndTime,?) as endtime,sum(JobUsageRecord.Njobs) as Njobs',
			' from CETable,CEProbes,JobUsageRecord',
			' where',
			' CEProbes.facility_id = CETable.facility_id',
			'	and JobUsageRecord.ProbeName = CEProbes.probename',
			'	and JobUsageRecord.EndTime between ? and ?',
			@mywhereclause,
			' group by date_format(JobUsageRecord.EndTime,?),CETable.facility_name',
			' order by date_format(JobUsageRecord.EndTime,?), CETable.facility_name');
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,p5,data) 
		values('DailyJobsByFacilityAndDate',@key,userName,userRole,@vo,@myformat,@myfromdate,@mytodate,@myformat,@myformat,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select CETable.facility_name,ProbeSummary.EndTime as endtime,sum(ProbeSummary.Njobs) as Njobs
				from CETable,CEProbes,ProbeSummary
				where
					CEProbes.facility_id = CETable.facility_id
					and ProbeSummary.ProbeName = CEProbes.probename
					and ProbeSummary.EndTime between date(fromdate) and date(todate)
				group by date_format(ProbeSummary.EndTime,format),CETable.facility_name
				order by date_format(ProbeSummary.EndTime,format),CETable.facility_name;
		else
			select CETable.facility_name,date_format(JobUsageRecord.EndTime,format) as endtime,sum(JobUsageRecord.Njobs) as Njobs
				from CETable,CEProbes,JobUsageRecord
				where
					CEProbes.facility_id = CETable.facility_id
					and JobUsageRecord.ProbeName = CEProbes.probename
					and JobUsageRecord.EndTime between fromdate and todate
				group by date_format(JobUsageRecord.EndTime,format),CETable.facility_name
				order by date_format(JobUsageRecord.EndTime,format), CETable.facility_name;
		end if;
	else
		prepare statement from @sql;
		execute statement using @myformat,@myfromdate,@mytodate,@myformat,@myformat;
		deallocate prepare statement;
	end if;
end
||
-- call DailyJobsByFacilityAndDate('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
||
-- call DailyJobsByFacilityAndDate('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
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

	set @sql :=
		concat(
			'select ProbeName,EndTime as endtime,sum(Njobs) as Njobs',
			' from JobUsageRecord',
			' where',
			'	EndTime >= date(?) and EndTime <= date(?)',
			@mywhereclause,
			' group by date_format(EndTime,?),ProbeName',
			' order by date_format(EndTime,?)');
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data) 
		values('DailyJobsByProbe',@key,userName,userRole,@vo,@myfromdate,@mytodate,@myformat,@myformat,@sql);

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
		prepare statement from @sql;
		execute statement using @myfromdate,@mytodate,@myformat,@myformat;
		deallocate prepare statement;
	end if;
end
||
-- call DailyJobsByProbe('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
||
-- call DailyJobsByProbe('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
||
drop procedure DailyJobsByProbeAndDate
||
create procedure DailyJobsByProbeAndDate (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;
	set @myformat := format;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select ProbeName,date_format(EndTime,?) as endtime,sum(Njobs) as Njobs',
			' from JobUsageRecord',
			' where',
			'	EndTime >= ? and EndTime <= ?',
			@mywhereclause,
			' group by date_format(EndTime,?),ProbeName',
			' order by date_format(EndTime,?),ProbeName');
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,p5,data) 
		values('DailyJobsByProbeAndDate',@key,userName,userRole,@vo,@myformat,@myfromdate,@mytodate,@myformat,@myformat,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select ProbeName,EndTime as endtime,sum(Njobs) as Njobs
				from ProbeSummary
				where
					EndTime >= date(fromdate) and EndTime <= date(todate)
				group by EndTime,ProbeName
				order by EndTime,ProbeName;
		else
			select ProbeName,date_format(EndTime,format) as endtime,sum(Njobs) as Njobs
				from JobUsageRecord
				where
					EndTime >= fromdate and EndTime <= todate
				group by date_format(EndTime,format),ProbeName
				order by date_format(EndTime,format),ProbeName;
		end if;
	else
		prepare statement from @sql;
		execute statement using @myformat,@myfromdate,@mytodate,@myformat,@myformat;
		deallocate prepare statement;
	end if;
end
||
-- call DailyJobsByProbeAndDate('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
||
-- call DailyJobsByProbeAndDate('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
||
drop procedure DailyJobsByVO
||
create procedure DailyJobsByVO (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;
	set @myformat := format;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql := 
		concat(
			'select VOName,EndTime as endtime,sum(Njobs) as Njobs',
			' from JobUsageRecord',
			' where',
			'	EndTime >= ?',
			'	and EndTime <= ?',
			@mywhereclause,
			' group by date_format(EndTime,?),VOName',
			' order by EndTime');
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,data) 
		values('DailyJobsByVO',@key,userName,userRole,@vo,@myfromdate,@mytodate,@myformat,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select VOName, EndTime as endtime,sum(Njobs) as Njobs
				from VOProbeSummary
				where
					EndTime >= date(fromdate)
					and EndTime <= date(todate)
				group by EndTime,VOName
				order by EndTime;
		else
			select VOName,EndTime as endtime,sum(Njobs) as Njobs
				from JobUsageRecord
				where
					EndTime >= fromdate
					and EndTime <= todate
				group by date_format(EndTime,format),VOName
				order by EndTime;
		end if;
	else
		prepare statement from @sql;
		execute statement using @myfromdate,@mytodate,@myformat;
		deallocate prepare statement;
	end if;
end
||
-- call DailyJobsByVO('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00','%y:%m:%d:%H:%i')
||
-- call DailyJobsByVO('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00','%y:%m:%d:%H:%i')
||
drop procedure DailyJobsByVOAndDate
||
create procedure DailyJobsByVOAndDate (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;
	set @myformat := format;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select VOName,EndTime as endtime,sum(Njobs) as Njobs',
			' from JobUsageRecord',
			' where',
			'	EndTime >= ?',
			'	and EndTime <= ?',
			@mywhereclause,
			' group by date_format(EndTime,?),VOName',
			' order by EndTime,VOName');
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,data) 
		values('DailyJobsByVOAndDate',@key,userName,userRole,@vo,@myfromdate,@mytodate,@myformat,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select VOName, EndTime as endtime,sum(Njobs) as Njobs
				from VOProbeSummary
				where
					EndTime >= date(fromdate)
					and EndTime <= date(todate)
				group by EndTime,VOName
				order by EndTime,VOName;
		else
			select VOName,EndTime as endtime,sum(Njobs) as Njobs
				from JobUsageRecord
				where
					EndTime >= fromdate
					and EndTime <= todate
				group by date_format(EndTime,format),VOName
				order by EndTime,VOName;
		end if;
	else
		prepare statement from @sql;
		execute statement using @myfromdate,@mytodate,@myformat;
		deallocate prepare statement;
	end if;
end
||
-- call DailyJobsByVOAndDate('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00','%y:%m:%d:%H:%i')
||
-- call DailyJobsByVOAndDate('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00','%y:%m:%d:%H:%i')
||
drop procedure DailyUsageByFacility
||
create procedure DailyUsageByFacility (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;
	set @myformat := format;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select CETable.facility_name,JobUsageRecord.EndTime as endtime,sum(JobUsageRecord.WallDuration) as WallDuration',
			' from CETable,CEProbes,JobUsageRecord',
			' where',
			'	CEProbes.facility_id = CETable.facility_id',
			'	and JobUsageRecord.ProbeName = CEProbes.probename',
			'	and EndTime >= ?',
			'	and EndTime <= ?',
			@mywhereclause,
			' group by date_format(JobUsageRecord.EndTime,?),CETable.facility_name',
			' order by date_format(JobUsageRecord.EndTime,?)');
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data) 
		values('DailyJobsByFacility',@key,userName,userRole,@vo,@myfromdate,@mytodate,@myformat,@myformat,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select CETable.facility_name,ProbeSummary.EndTime as endtime,sum(ProbeSummary.WallDuration) as WallDuration
				from CETable,CEProbes,ProbeSummary
				where
					CEProbes.facility_id = CETable.facility_id
					and ProbeSummary.ProbeName = CEProbes.probename
					and EndTime >= date(fromdate)
					and EndTime <= date(todate)
				group by ProbeSummary.EndTime,CETable.facility_name
				order by ProbeSummary.EndTime;
		else
			select CETable.facility_name,JobUsageRecord.EndTime as endtime,sum(JobUsageRecord.WallDuration) as WallDuration
				from CETable,CEProbes,JobUsageRecord
				where
					CEProbes.facility_id = CETable.facility_id
					and JobUsageRecord.ProbeName = CEProbes.probename
					and EndTime >= fromdate
					and EndTime <= todate
				group by date_format(JobUsageRecord.EndTime,format),CETable.facility_name
				order by date_format(JobUsageRecord.EndTime,format);
		end if;
	else
		prepare statement from @sql;
		execute statement using @myfromdate,@mytodate,@myformat,@myformat;
		deallocate prepare statement;
	end if;
end
||
-- call DailyUsageByFacility('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00','%y:%m:%d:%H:%i')
||
-- call DailyUsageByFacility('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00','%y:%m:%d:%H:%i')
||
drop procedure DailyUsageByFacilityAndDate
||
create procedure DailyUsageByFacilityAndDate (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;
	set @myformat := format;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select CETable.facility_name,JobUsageRecord.EndTime as endtime,sum(JobUsageRecord.WallDuration) as WallDuration',
			' from CETable,CEProbes,JobUsageRecord',
			' where',
			'	CEProbes.facility_id = CETable.facility_id',
			'	and JobUsageRecord.ProbeName = CEProbes.probename',
			'	and EndTime >= ?',
			'	and EndTime <= ?',
			@mywhereclause,
			' group by date_format(JobUsageRecord.EndTime,?),CETable.facility_name',
			' order by date_format(JobUsageRecord.EndTime,?),JobUsageRecord.ProbeName');
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data) 
		values('DailyUsageByFacilityAndDate',@key,userName,userRole,@vo,@myfromdate,@mytodate,@myformat,@myformat,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select CETable.facility_name,ProbeSummary.EndTime as endtime,sum(ProbeSummary.WallDuration) as WallDuration
				from CETable,CEProbes,ProbeSummary
				where
					CEProbes.facility_id = CETable.facility_id
					and ProbeSummary.ProbeName = CEProbes.probename
					and EndTime >= date(fromdate)
					and EndTime <= date(todate)
				group by ProbeSummary.EndTime,CETable.facility_name
				order by ProbeSummary.EndTime,ProbeSummary.ProbeName;
		else
			select CETable.facility_name,JobUsageRecord.EndTime as endtime,sum(JobUsageRecord.WallDuration) as WallDuration
				from CETable,CEProbes,JobUsageRecord
				where
					CEProbes.facility_id = CETable.facility_id
					and JobUsageRecord.ProbeName = CEProbes.probename
					and EndTime >= fromdate
					and EndTime <= todate
				group by date_format(JobUsageRecord.EndTime,format),CETable.facility_name
				order by date_format(JobUsageRecord.EndTime,format),JobUsageRecord.ProbeName;
		end if;
	else
		prepare statement from @sql;
		execute statement using @myfromdate,@mytodate,@myformat,@myformat;
		deallocate prepare statement;
	end if;
end
||
-- call DailyUsageByFacilityAndDate('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00','%y:%m:%d:%H:%i')
||
-- call DailyUsageByFacilityAndDate('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00','%y:%m:%d:%H:%i')
||
drop procedure DailyUsageByProbe
||
create procedure DailyusageByProbe (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;
	set @myformat := format;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select ProbeName,EndTime as endtime,sum(WallDuration) as WallDuration',
			' from JobUsageRecord',
			' where',
			'	EndTime >= ? and EndTime <= ?',
			@mywhereclause,
			' group by date_format(EndTime,?),ProbeName',
			' order by date_format(EndTime,?)');
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data) 
		values('DailyUsageByProbe',@key,userName,userRole,@vo,@myfromdate,@mytodate,@myformat,@myformat,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select ProbeName,EndTime as endtime,sum(WallDuration) as WallDuration
				from ProbeSummary
				where
					EndTime >= date(fromdate) and EndTime <= date(todate)
				group by EndTime,ProbeName
				order by EndTime;
		else
			select ProbeName,date_format(EndTime,format) as endtime,sum(WallDuration) as WallDuration
				from JobUsageRecord
				where
					EndTime >= fromdate and EndTime <= todate
				group by date_format(EndTime,format),ProbeName
				order by date_format(EndTime,format);
		end if;
	else
		prepare statement from @sql;
		execute statement using @myfromdate,@mytodate,@myformat,@myformat;
		deallocate prepare statement;
	end if;
end
||
-- call DailyUsageByProbe('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
||
-- call DailyUsageByProbe('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
||
drop procedure DailyUsageByProbeAndDate
||
create procedure DailyUsageByProbeAndDate (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;
	set @myformat := format;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select ProbeName,date_format(EndTime,?) as endtime,sum(WallDuration) as WallDuration',
			' from JobUsageRecord',
			' where',
			'	EndTime >= ? and EndTime <= ?',
			@mywhereclause,
			' group by date_format(EndTime,?),ProbeName',
			' order by date_format(EndTime,?),ProbeName');
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data) 
		values('DailyUsageByProbeAndDate',@key,userName,userRole,@vo,@myfromdate,@mytodate,@myformat,@myformat,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select ProbeName,EndTime as endtime,sum(WallDuration) as WallDuration
				from ProbeSummary
				where
					EndTime >= date(fromdate) and EndTime <= date(todate)
				group by EndTime,ProbeName
				order by EndTime,ProbeName;
		else
			select ProbeName,date_format(EndTime,format) as endtime,sum(WallDuration) as WallDuration
				from JobUsageRecord
				where
					EndTime >= fromdate and EndTime <= todate
				group by date_format(EndTime,format),ProbeName
				order by date_format(EndTime,format),ProbeName;
		end if;
	else
		prepare statement from @sql;
		execute statement using @myformat,@myfromdate,@mytodate,@myformat,@myformat;
		deallocate prepare statement;
	end if;
end
||
-- call DailyUsageByProbeAndDate('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
||
-- call DailyUsageByProbeAndDate('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
||
drop procedure DailyUsageBySite
||
-- create procedure DailyUsageBySite (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
-- begin
-- 
-- 	set @myfromdate := fromdate;
-- 	set @mytodate := todate;
-- 	set @myformat := format;
-- 
-- 	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
-- 	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
-- 	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
-- 	call parse(userName,@name,@key,@vo);
-- 
-- 	set @sql :=
-- 		concat(
-- 			'select ProbeName,date_format(EndTime,format) as endtime,sum(WallDuration) as WallDuration,',
-- 			' sum(CpuUserDuration + CpuSystemDuration) as Cpu',
-- 			' from JobUsageRecord',
-- 			' where',
-- 			'	EndTime >= ? and EndTime <= ?',
-- 			@mywhereclause,
-- 			' group by date_format(EndTime,?),ProbeName',
-- 			' order by date_format(EndTime,?),ProbeName');
-- 	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data) 
-- 		values('DailyUsageBySite',@key,userName,userRole,@vo,@myfromdate,@mytodate,@myformat,@myformat,@sql);
-- 
-- 	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
-- 		if datediff(todate,fromdate) > 6 then
-- 			select SiteName,EndTime as endtime,sum(WallDuration) as WallDuration,sum(CpuUserDuration + CpuSystemDuration) as Cpu
-- 				from ProbeSummary
-- 				where
-- 					EndTime >= date(fromdate) and EndTime <= date(todate)
-- 				group by EndTime,ProbeName
-- 				order by EndTime,ProbeName;
-- 		else
-- 			select ProbeName,date_format(EndTime,format) as endtime,sum(WallDuration) as WallDuration,
-- 				sum(CpuUserDuration + CpuSystemDuration) as Cpu
-- 				from JobUsageRecord
-- 				where
-- 					EndTime >= fromdate and EndTime <= todate
-- 				group by date_format(EndTime,format),ProbeName
-- 				order by date_format(EndTime,format),ProbeName;
-- 		end if;
-- 	else
-- 		prepare statement from @sql;
-- 		execute statement using @myformat,@myfromdate,@mytodate,@myformat,@myformat;
-- 		deallocate prepare statement;
-- 	end if;
-- end
||
-- call DailyUsageBySite('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
||
-- call DailyUsageBySite('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
||
drop procedure DailyUsageByVO
||
-- create procedure DailyUsageByVO (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
-- begin
-- 
-- 	set @myfromdate := fromdate;
-- 	set @mytodate := todate;
-- 	set @myformat := format;
-- 
-- 	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
-- 	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
-- 	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
-- 	call parse(userName,@name,@key,@vo);
-- 
-- 	set @sql :=
-- 		concat(
-- 			'select VOName,EndTime as endtime,sum(WallDuration) as WallDuration,sum(CpuUserDuration + CpuSystemDuration) as Cpu',
-- 			' from JobUsageRecord',
-- 			' where',
-- 			'	EndTime >= ? and EndTime <= ?',
-- 			@mywhereclause,
-- 			' group by date_format(EndTime,?),VOName',
-- 			' order by date_format(EndTime,?)');
-- 	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data) 
-- 		values('DailyUsageByVO',@key,userName,userRole,@vo,@myfromdate,@mytodate,@myformat,@myformat,@sql);
-- 
-- 	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
-- 		if datediff(todate,fromdate) > 6 then
-- 			select VOName,EndTime as endtime,sum(WallDuration) as WallDuration,sum(CpuUserDuration + CpuSystemDuration) as Cpu
-- 				from VOProbeSummary
-- 				where
-- 					EndTime >= date(fromdate) and EndTime <= date(todate)
-- 				group by EndTime,VOName
-- 				order by EndTime;
-- 		else
-- 			select VOName,date_format(EndTime,format) as endtime,sum(WallDuration) as WallDuration,sum(CpuUserDuration + CpuSystemDuration) as Cpu
-- 				from JobUsageRecord
-- 				where
-- 					EndTime >= fromdate and EndTime <= todate
-- 				group by date_format(EndTime,format),VOName
-- 				order by date_format(EndTime,format);
-- 		end if;
-- 	else
-- 		prepare statement from @sql;
-- 		execute statement using @myfromdate,@mytodate,@myformat,@myformat;
-- 		deallocate prepare statement;
-- 	end if;
-- end
||
-- call DailyUsageByVO('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
||
-- call DailyUsageByVO('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
||
drop procedure DailyUsageByVOAndDate
||
create procedure DailyUsageByVOAndDate (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;
	set @myformat := format;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select VOName,date_format(EndTime,?) as endtime,sum(WallDuration) as WallDuration,sum(CpuUserDuration + CpuSystemDuration) as Cpu',
			' from JobUsageRecord',
			' where',
			'	EndTime >= ? and EndTime <= ?',
			@mywhereclause,
			' group by date_format(EndTime,?),VOName',
			' order by date_format(EndTime,?),VOName');
	insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data) 
		values('DailyUsageByVOAndDate',@key,userName,userRole,@vo,@myfromdate,@mytodate,@myformat,@myformat,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select VOName,EndTime as endtime,sum(WallDuration) as WallDuration,sum(CpuUserDuration + CpuSystemDuration) as Cpu
				from VOProbeSummary
				where
					EndTime >= date(fromdate) and EndTime <= date(todate)
				group by EndTime,VOName
				order by EndTime,VOName;
		else
			select VOName,date_format(EndTime,format) as endtime,sum(WallDuration) as WallDuration,
				sum(CpuUserDuration + CpuSystemDuration) as Cpu
				from JobUsageRecord
				where
					EndTime >= fromdate and EndTime <= todate
				group by date_format(EndTime,format),VOName
				order by date_format(EndTime,format),VOName;
		end if;
	else
		prepare statement from @sql;
		execute statement using @myformat,@myfromdate,@mytodate,@myformat,@myformat;
		deallocate prepare statement;
	end if;
end
||
-- call DailyUsageByVOAndDate('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
||
-- call DailyUsageByVOAndDate('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
||
drop procedure JobsByFacility
||
create procedure JobsByFacility (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select CETable.facility_name,sum(JobUsageRecord.Njobs) as Njobs',
			' from CETable,CEProbes,JobUsageRecord',
			' where',
			'	CEProbes.facility_id = CETable.facility_id',
			'	and JobUsageRecord.ProbeName = CEProbes.probename',
			'	and EndTime >= ?',
			'	and EndTime <= ?',
			@mywhereclause,
			' group by CETable.facility_name',
			' order by CETable.facility_name');
	insert into trace(pname,userkey,user,role,vo,p1,p2,data) 
		values('JobsByFacility',@key,userName,userRole,@vo,@myfromdate,@mytodate,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select CETable.facility_name,sum(ProbeSummary.Njobs) as Njobs
				from CETable,CEProbes,ProbeSummary
				where
					CEProbes.facility_id = CETable.facility_id
					and ProbeSummary.ProbeName = CEProbes.probename
					and EndTime >= date(fromdate)
					and EndTime <= date(todate)
				group by CETable.facility_name
				order by CETable.facility_name;
		else
			select CETable.facility_name,sum(JobUsageRecord.Njobs) as Njobs
				from CETable,CEProbes,JobUsageRecord
				where
					CEProbes.facility_id = CETable.facility_id
					and JobUsageRecord.ProbeName = CEProbes.probename
					and EndTime >= fromdate
					and EndTime <= todate
				group by CETable.facility_name
				order by CETable.facility_name;
		end if;
	else
		prepare statement from @sql;
		execute statement using @myfromdate,@mytodate;
		deallocate prepare statement;
	end if;
end
||
-- call JobsByFacility('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00')
||
-- call JobsByFacility('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00')
||
drop procedure JobsByFacility
||
create procedure JobsByFacility (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select CETable.facility_name,sum(JobUsageRecord.Njobs) as Njobs',
			' from CETable,CEProbes,JobUsageRecord',
			' where',
			'	CEProbes.facility_id = CETable.facility_id',
			'	and JobUsageRecord.ProbeName = CEProbes.probename',
			'	and EndTime >= ?',
			'	and EndTime <= ?',
			@mywhereclause,
			' group by CETable.facility_name',
			' order by CETable.facility_name');
	insert into trace(pname,userkey,user,role,vo,p1,p2,data) 
		values('JobsByFacility',@key,userName,userRole,@vo,@myfromdate,@mytodate,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select CETable.facility_name,sum(ProbeSummary.Njobs) as Njobs
				from CETable,CEProbes,ProbeSummary
				where
					CEProbes.facility_id = CETable.facility_id
					and ProbeSummary.ProbeName = CEProbes.probename
					and EndTime >= date(fromdate)
					and EndTime <= date(todate)
				group by CETable.facility_name
				order by CETable.facility_name;
		else
			select CETable.facility_name,sum(JobUsageRecord.Njobs) as Njobs
				from CETable,CEProbes,JobUsageRecord
				where
					CEProbes.facility_id = CETable.facility_id
					and JobUsageRecord.ProbeName = CEProbes.probename
					and EndTime >= fromdate
					and EndTime <= todate
				group by CETable.facility_name
				order by CETable.facility_name;
		end if;
	else
		prepare statement from @sql;
		execute statement using @myfromdate,@mytodate;
		deallocate prepare statement;
	end if;
end
||
-- call JobsByFacility('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00')
||
-- call JobsByFacility('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00')
||
drop procedure JobsByProbeNoFacility
||
create procedure JobsByProbeNoFacility (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select ProbeName,sum(Njobs) as Njobs',
			' from JobUsageRecord',
			' where',
			'	EndTime >= ?',
			'	and EndTime <= ?',
			@mywhereclause,
			' group by ProbeName',
			' order by ProbeName');
	insert into trace(pname,userkey,user,role,vo,p1,p2,data) 
		values('JobsByProbeNoFacility',@key,userName,userRole,@vo,@myfromdate,@mytodate,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select ProbeName,sum(Njobs) as Njobs
				from ProbeSummary
				where
					EndTime >= date(fromdate)
					and EndTime <= date(todate)
				group by ProbeName
				order by ProbeName;
		else
			select ProbeName,sum(Njobs) as Njobs
				from JobUsageRecord
				where
					EndTime >= fromdate
					and EndTime <= todate
				group by ProbeName
				order by ProbeName;
		end if;
	else
		prepare statement from @sql;
		execute statement using @myfromdate,@mytodate;
		deallocate prepare statement;
	end if;
end
||
-- call JobsByProbeNoFacility('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00')
||
-- call JobsByProbeNoFacility('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00')
||
drop procedure JobsBySite
||
-- create procedure JobsBySite (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64))
-- begin
-- 
-- 	set @myfromdate := fromdate;
-- 	set @mytodate := todate;
-- 
-- 	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
-- 	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
-- 	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
-- 	call parse(userName,@name,@key,@vo);
-- 
-- 	set @sql :=
-- 		concat(
-- 			'select SiteName,sum(Njobs) as Njobs',
-- 			' from JobUsageRecord',
-- 			' where',
-- 			'	EndTime >= ?',
-- 			'	and EndTime <= ?',
-- 			@mywhereclause,
-- 			' group by SiteName',
-- 			' order by SiteName');
-- 	insert into trace(pname,userkey,user,role,vo,p1,p2,data) 
-- 		values('JobsBySite',@key,userName,userRole,@vo,@myfromdate,@mytodate,@sql);
-- 
-- 	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
-- 		if datediff(todate,fromdate) > 6 then
-- 			select SiteName,sum(Njobs) as Njobs
-- 				from ProbeSummary
-- 				where
-- 					EndTime >= date(fromdate)
-- 					and EndTime <= date(todate)
-- 				group by SiteName
-- 				order by SiteName;
-- 		else
-- 			select SiteName,sum(Njobs) as Njobs
-- 				from JobUsageRecord
-- 				where
-- 					EndTime >= fromdate
-- 					and EndTime <= todate
-- 				group by SiteName
-- 				order by SiteName;
-- 		end if;
-- 	else
-- 		prepare statement from @sql;
-- 		execute statement using @myfromdate,@mytodate;
-- 		deallocate prepare statement;
-- 	end if;
-- end
||
-- call JobsBySite('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00')
||
-- call JobsBySite('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00')
||
drop procedure JobsByUser
||
-- create procedure JobsByUser (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64))
-- begin
-- 
-- 	set @myfromdate := fromdate;
-- 	set @mytodate := todate;
-- 
-- 	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
-- 	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
-- 	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
-- 	call parse(userName,@name,@key,@vo);
-- 
-- 	set @sql :=
-- 		concat(
-- 			'select CommonName as UserName,sum(Njobs) as Njobs',
-- 			' from JobUsageRecord',
-- 			' where',
-- 			'	EndTime >= ?',
-- 			'	and EndTime <= ?',
-- 			@mywhereclause,
-- 			' group by CommonName',
-- 			' order by CommonName');
-- 	insert into trace(pname,userkey,user,role,vo,p1,p2,data) 
-- 		values('JobsByUser',@key,userName,userRole,@vo,@myfromdate,@mytodate,@sql);
-- 
-- 	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
-- 		if datediff(todate,fromdate) > 6 then
-- 			select CommonName as UserName,sum(Njobs) as Njobs
-- 				from UserProbeSummary
-- 				where
-- 					EndTime >= date(fromdate)
-- 					and EndTime <= date(todate)
-- 				group by CommonName
-- 				order by CommonName;
-- 		else
-- 			select CommonName as UserName,sum(Njobs) as Njobs
-- 				from JobUsageRecord
-- 				where
-- 					EndTime >= fromdate
-- 					and EndTime <= todate
-- 				group by CommonName
-- 				order by CommonName;
-- 		end if;
-- 	else
-- 		prepare statement from @sql;
-- 		execute statement using @myfromdate,@mytodate;
-- 		deallocate prepare statement;
-- 	end if;
-- end
||
-- call JobsByUser('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00')
||
-- call JobsByUser('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00')
||
drop procedure JobsByVO
||
-- create procedure JobsByVO (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64))
-- begin
-- 
-- 	set @myfromdate := fromdate;
-- 	set @mytodate := todate;
-- 
-- 	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
-- 	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
-- 	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
-- 	call parse(userName,@name,@key,@vo);
-- 
-- 	set @sql :=
-- 		concat(
-- 			'select VOName,sum(Njobs) as Njobs',
-- 			' from JobUsageRecord',
-- 			' where',
-- 			'	EndTime >= ?',
-- 			'	and EndTime <= ?',
-- 			@mywhereclause,
-- 			' group by VOName',
-- 			' order by VOName');
-- 	insert into trace(pname,userkey,user,role,vo,p1,p2,data) 
-- 		values('JobsByVO',@key,userName,userRole,@vo,@myfromdate,@mytodate,@sql);
-- 
-- 	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
-- 		if datediff(todate,fromdate) > 6 then
-- 			select VOName,sum(Njobs) as Njobs
-- 				from VOProbeSummary
-- 				where
-- 					EndTime >= date(fromdate)
-- 					and EndTime <= date(todate)
-- 				group by VOName
-- 				order by VOName;
-- 		else
-- 			select VOName,sum(Njobs) as Njobs
-- 				from JobUsageRecord
-- 				where
-- 					EndTime >= fromdate
-- 					and EndTime <= todate
-- 				group by VOName
-- 				order by VOName;
-- 		end if;
-- 	else
-- 		prepare statement from @sql;
-- 		execute statement using @myfromdate,@mytodate;
-- 		deallocate prepare statement;
-- 	end if;
-- end
||
-- call JobsByVO('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00')
||
-- call JobsByVO('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00')
||
drop procedure UsageByFacility
||
create procedure UsageByFacility (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select CETable.facility_name,JobUsageRecord.EndTime as endtime,sum(JobUsageRecord.WallDuration) as WallDuration',
			' from CETable,CEProbes,JobUsageRecord',
			' where',
			'	CEProbes.facility_id = CETable.facility_id',
			'	and JobUsageRecord.ProbeName = CEProbes.probename',
			'	and EndTime >= ?',
			'	and EndTime <= ?',
			@mywhereclause,
			' group by CETable.facility_name',
			' order by CETable.facility_name');
	insert into trace(pname,userkey,user,role,vo,p1,p2,data) 
		values('UsageByFacility',@key,userName,userRole,@vo,@myfromdate,@mytodate,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select CETable.facility_name,ProbeSummary.EndTime as endtime,sum(ProbeSummary.WallDuration) as WallDuration
				from CETable,CEProbes,ProbeSummary
				where
					CEProbes.facility_id = CETable.facility_id
					and ProbeSummary.ProbeName = CEProbes.probename
					and EndTime >= date(fromdate)
					and EndTime <= date(todate)
				group by CETable.facility_name
				order by CETable.facility_name;
		else
			select CETable.facility_name,JobUsageRecord.EndTime as endtime,sum(JobUsageRecord.WallDuration) as WallDuration
				from CETable,CEProbes,JobUsageRecord
				where
					CEProbes.facility_id = CETable.facility_id
					and JobUsageRecord.ProbeName = CEProbes.probename
					and EndTime >= fromdate
					and EndTime <= todate
				group by CETable.facility_name
				order by CETable.facility_name;
		end if;
	else
		prepare statement from @sql;
		execute statement using @myfromdate,@mytodate;
		deallocate prepare statement;
	end if;
end
||
-- call UsageByFacility('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00')
||
-- call UsageByFacility('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00')
||
drop procedure UsageByProbe
||
create procedure UsageByProbe (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64))
begin

	set @myfromdate := fromdate;
	set @mytodate := todate;

	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	call parse(userName,@name,@key,@vo);

	set @sql :=
		concat(
			'select ProbeName,sum(WallDuration) as WallDuration',
			' from JobUsageRecord',
			' where',
			'	EndTime >= ? and EndTime <= ?',
			@mywhereclause,
			' group by ProbeName',
			' order by ProbeName');
	insert into trace(pname,userkey,user,role,vo,p1,p2,data) 
		values('UsageByProbe',@key,userName,userRole,@vo,@myfromdate,@mytodate,@sql);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
		if datediff(todate,fromdate) > 6 then
			select ProbeName,sum(WallDuration) as WallDuration
				from ProbeSummary
				where
					EndTime >= date(fromdate) and EndTime <= date(todate)
				group by ProbeName
				order by ProbeName;
		else
			select ProbeName,sum(WallDuration) as WallDuration
				from JobUsageRecord
				where
					EndTime >= fromdate and EndTime <= todate
				group by ProbeName
				order by ProbeName;
		end if;
	else
		prepare statement from @sql;
		execute statement using @myfromdate,@mytodate;
		deallocate prepare statement;
	end if;
end
||
-- call UsageByProbe('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00')
||
-- call UsageByProbe('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00')
||
drop procedure UsageBySite
||
-- create procedure UsageBySite (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64))
-- begin
-- 
-- 	set @myfromdate := fromdate;
-- 	set @mytodate := todate;
-- 
-- 	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
-- 	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
-- 	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
-- 	call parse(userName,@name,@key,@vo);
-- 
-- 	set @sql :=
-- 		concat(
-- 			' select SiteName,sum(WallDuration) as WallDuration',
-- 			' from JobUsageRecord',
-- 			' where',
-- 			'	EndTime >= ? and EndTime <= ?',
-- 			@mywhereclause,
-- 			' group by SiteName',
-- 			' order by SiteName');
-- 	insert into trace(pname,userkey,user,role,vo,p1,p2,data) 
-- 		values('UsageBySite',@key,userName,userRole,@vo,@myfromdate,@mytodate,@sql);
-- 
-- 	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
-- 		if datediff(todate,fromdate) > 6 then
-- 			select SiteName,sum(WallDuration) as WallDuration
-- 				from ProbeSummary
-- 				where
-- 					EndTime >= date(fromdate) and EndTime <= date(todate)
-- 				group by SiteName
-- 				order by SiteName;
-- 		else
-- 			select SiteName,sum(WallDuration) as WallDuration
-- 				from JobUsageRecord
-- 				where
-- 					EndTime >= fromdate and EndTime <= todate
-- 				group by SiteName
-- 				order by SiteName;
-- 		end if;
-- 	else
-- 		prepare statement from @sql;
-- 		execute statement using @myfromdate,@mytodate;
-- 		deallocate prepare statement;
-- 	end if;
-- end
||
-- call UsageBySite('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-10 00:00:00')
||
-- call UsageBySite('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00')
||
drop procedure UsageByVO
||
-- create procedure UsageByVO (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64))
-- begin
-- 
-- 	set @myfromdate := fromdate;
-- 	set @mytodate := todate;
-- 
-- 	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
-- 	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
-- 	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
-- 	call parse(userName,@name,@key,@vo);
-- 
-- 	set @sql :=
-- 		concat(
-- 			'select VOName,',
-- 			' sum(WallDuration) as wallduration,',
-- 			' sum(CpuUserDuration + CpuSystemDuration) as cpu',
-- 			' from JobUsageRecord',
-- 			' where EndTime between date(?) and date(?)',
-- 			@mywhereclause,
-- 			' group by VOName',
-- 			' order by VOName');
-- 	insert into trace(pname,userkey,user,role,vo,p1,p2,data) 
-- 		values('UsageByVO',@key,userName,userRole,@vo,@myfromdate,@mytodate,@sql);
-- 
-- 	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
-- 		if datediff(todate,fromdate) > 6 then
-- 			select VOName, 
-- 				sum(WallDuration) as wallduration,
-- 				sum(CpuUserDuration + CpuSystemDuration) as cpu
-- 				from VOProbeSummary
-- 				where EndTime between date(fromdate) and date(todate)
-- 				group by VOName
-- 				order by VOName;
-- 		else
-- 			select VOName, 
-- 				sum(WallDuration) as wallduration,
-- 				sum(CpuUserDuration + CpuSystemDuration) as cpu
-- 				from JobUsageRecord
-- 				where EndTime between date(fromdate) and date(todate)
-- 				group by VOName
-- 				order by VOName;
-- 		end if;
-- 	else
-- 		prepare statement from @sql;
-- 		execute statement using @myfromdate,@mytodate;
-- 		deallocate prepare statement;
-- 	end if;
-- end
||
-- call UsageByVO('GratiaUser','GratiaUser','2006-09-01 00:00:00','2006-09-10 00:00:00')
||
-- call UsageByVO('GratiaUser','GratiaUser','2006-10-01 00:00:00','2006-10-03 00:00:00')
||

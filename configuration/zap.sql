delimiter ||
drop procedure ProbeStatus
||
create procedure ProbeStatus (userName varchar(64),userRole varchar(64),fromdate varchar(64),todate varchar(64),format varchar(64))
begin

	declare mywhereclause varchar(255);
	set @myfromdate := fromdate;
	set @mytodate := todate;

	insert into trace(pname,userkey) values('ProbeStatus','step00');
	select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
	-- insert into trace(pname,userkey) values('ProbeStatus','step01');
	select RolesTable.whereclause into @mywhereclause from RolesTable where RolesTable.role = userRole;
	insert into trace(pname,userkey) values('ProbeStatus','step02');
	select generateWhereClause(userName,userRole,@mywhereclause) into @mywhereclause;
	-- insert into trace(pname,userkey) values('ProbeStatus','step03');
	call parse(userName,@name,@key,@vo);
	-- insert into trace(pname,userkey) values('ProbeStatus','step04');

	insert into trace(pname,userkey,user,role,vo,p1,p2) 
		values('ProbeStatus',@key,userName,userRole,@vo,fromdate,todate);

	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' or @mywhereclause = 'Everything' then
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
call ProbeStatus('GratiaGlobalAdmin','GratiaUser','2006-01-01 00:00:00','2006-12-31 00:00:00','ignore');
||

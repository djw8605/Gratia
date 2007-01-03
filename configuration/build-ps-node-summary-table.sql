delimiter ||
drop table if exists NodeSummary;
||
CREATE TABLE NodeSummary (
  EndTime DATETIME NOT NULL DEFAULT 0,
  Node VARCHAR(255) NOT NULL DEFAULT '',
  VOName VARCHAR(255) DEFAULT '',
	CpuSystemTime int default 0,
	CpuUserTime int default 0,
	CpuCount int default 1,
  HostDescription VARCHAR(255) DEFAULT 'Unknown',
  BenchmarkScore int default 0,
	DaysInMonth int default 0
);
||
alter table NodeSummary
  add index index01(EndTime);
||
alter table NodeSummary
  add index index02(Node);
||
alter table NodeSummary
  add index index03(VOName);
||
drop procedure updatenodesummary
||
create procedure updatenodesummary(enddate datetime,mynode varchar(255),myvoname varchar(255),
	mycpusystemtime int,mycpuusertime int,mycpucount int,myhostdescription varchar(255),mybenchmarkscore int,mydaysinmonth int)
begin
	declare mycount int default 0;
	select count(*) into mycount from NodeSummary
		where EndTime = enddate
		and Node = mynode
		and VOName = myvoname;
	if mycount = 0 then
		insert into NodeSummary values(
			date(enddate),
			mynode,
			myvoname,
			mycpusystemtime,
			mycpuusertime,
			mycpucount,
			myhostdescription,
			mybenchmarkscore,
			mydaysinmonth);
			insert into trace(user) values('insert');
	else
		update NodeSummary
			set 
				CpuSystemTime = CpuSystemTime + mycpusystemtime,
				CpuUserTime = CpuUserTime + mycpuusertime
			where
				EndTime = enddate
				and Node = mynode
				and VOName = myvoname;
		insert into trace(user) values('update');
	end if;
end;
||
drop procedure buildnodesummary
||
create procedure buildnodesummary()
begin
	declare done bool default false;
	declare startdate datetime;
	declare enddate datetime;
	declare node varchar(255);
	declare myvoname varchar(255);
	declare mycpuusertime int default 0;
	declare mycpusystemtime int default 0;
	declare mycpucount int default 0;
	declare myhostdescription varchar(255);
	declare mybenchmarkscore int default 0;
	declare divide int default 0;
	declare counter int default 0;	
	declare newdate datetime;

	declare newcpusystemtime int default 0;
	declare newcpuusertime int default 0;

	declare numberofdays int default 0;

	declare cur01 cursor for select StartTime,EndTime,Host,VOName,CpuUserDuration,CpuSystemDuration,HostDescription
		from JobUsageRecord;
	declare continue handler for sqlstate '02000' set done = true;

	open cur01;

	myloop: loop

		fetch cur01 into startdate,enddate,node,myvoname,mycpuusertime,mycpusystemtime,myhostdescription;

		insert into trace(pname,userkey) values(node,myvoname);

		if done then
			close cur01;
			leave myloop;
		end if;

		if myvoname = null then
			set myvoname = 'Unknown';
		end if;

		begin
			select BenchmarkScore,CPUCount into mybenchmarkscore,mycpucount from CPUInfo where myhostdescription = CPUInfo.NodeName;
			set done = false;
		end;

		set numberofdays = datediff(enddate,startdate);
		set divide = numberofdays + 1;
		set newcpusystemtime = mycpusystemtime / divide;
		set newcpuusertime = mycpuusertime / divide;

		if numberofdays = 0 then
			call updatenodesummary(
				date(enddate),node,myvoname,mycpusystemtime,mycpuusertime,mycpucount,myhostdescription,
				mybenchmarkscore,extract(DAY from last_day(enddate)));
		end if;

		if numberofdays > 0 then
			while counter < numberofdays do
				set newdate = adddate(startdate,counter);
				call updatenodesummary(
					date(newdate),node,myvoname,newcpusystemtime,newcpuusertime,mycpucount,
					myhostdescription,mybenchmarkscore,extract(DAY from last_day(newdate)));
				set counter = counter + 1;
			end while;
		end if;

	end loop;
end
||
delete from trace
||
drop trigger trigger01;
||
create trigger trigger01 after insert on JobUsageRecord
for each row
glr:begin
	declare mycount int;
	declare startdate datetime;
	declare enddate datetime;
	declare node varchar(255);
	declare myvoname varchar(255);
	declare mycpuusertime int default 0;
	declare mycpusystemtime int default 0;
	declare mycpucount int default 0;
	declare myhostdescription varchar(255);
	declare mybenchmarkscore int default 0;
	declare divide int default 0;
	declare counter int default 0;	
	declare newdate datetime;
	declare newcpusystemtime int default 0;
	declare newcpuusertime int default 0;
	declare numberofdays int default 0;

	--
	-- basic data checks
	--
	if new.ProbeName is null then
		leave glr;
	end if;
	if new.VOName is null then
		leave glr;
	end if;
	if new.Njobs is null then
		leave glr;
	end if;
	if new.WallDuration is null then
		leave glr;
	end if;
	if new.CpuUserDuration is null then
		leave glr;
	end if;
	if new.CpuSystemDuration is null then
		leave glr;
	end if;

	--
	-- ProbeStatus
	--

	select count(*) into mycount from ProbeStatus
		where ProbeStatus.ProbeName = new.ProbeName
		and ProbeStatus.EndTime = str_to_date(date_format(new.ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00');
	if mycount = 0 then
		insert into ProbeStatus values(
			str_to_date(date_format(new.ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00'),
			new.ProbeName,1);
	elseif mycount > 0 then
		update ProbeStatus
			set
				ProbeStatus.Njobs = ProbeStatus.Njobs + 1
				where ProbeStatus.ProbeName = new.ProbeName
				and ProbeStatus.EndTime = str_to_date(date_format(new.ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00');
	end if;

	--
	-- ProbeSummary
	--

	select count(*) into mycount from ProbeSummary
		where ProbeSummary.ProbeName = new.ProbeName
		and ProbeSummary.EndTime = date(new.EndTime);
	if mycount = 0 then
		insert into ProbeSummary values(date(new.EndTime),new.ProbeName,new.SiteName,new.Njobs,new.WallDuration,new.CpuUserDuration,new.CpuSystemDuration);
	elseif mycount > 0 then
		update ProbeSummary
			set
				ProbeSummary.Njobs = ProbeSummary.Njobs + new.Njobs,
				ProbeSummary.WallDuration = ProbeSummary.WallDuration + new.WallDuration,
				ProbeSummary.CpuUserDuration = ProbeSummary.CpuUserDuration + new.CpuUserDuration,
				ProbeSummary.CpuSystemDuration = ProbeSummary.CpuSystemDuration + new.CpuSystemDuration
				where ProbeSummary.ProbeName = new.ProbeName
				and ProbeSummary.EndTime = date(new.EndTime);
	end if;

	--
	-- UserProbeSummary
	--

	select count(*) into mycount from UserProbeSummary
		where 
		UserProbeSummary.CommonName = new.CommonName
		and UserProbeSummary.ProbeName = new.ProbeName
		and UserProbeSummary.EndTime = date(new.EndTime);
	if mycount = 0 then
		insert into UserProbeSummary values(date(new.EndTime),new.CommonName,new.ProbeName,new.Njobs,new.WallDuration,new.CpuUserDuration,new.CpuSystemDuration);
	elseif mycount > 0 then
		update UserProbeSummary
			set
				UserProbeSummary.Njobs = UserProbeSummary.Njobs + new.Njobs,
				UserProbeSummary.WallDuration = UserProbeSummary.WallDuration + new.WallDuration,
				UserProbeSummary.CpuUserDuration = UserProbeSummary.CpuUserDuration + new.CpuUserDuration,
				UserProbeSummary.CpuSystemDuration = UserProbeSummary.CpuSystemDuration + new.CpuSystemDuration
				where 
				UserProbeSummary.CommonName = new.CommonName
				and UserProbeSummary.ProbeName = new.ProbeName
				and UserProbeSummary.EndTime = date(new.EndTime);
	end if;

	--
	-- VOProbeSummary
	--

	select count(*) into mycount from VOProbeSummary
		where VOProbeSummary.VOName = new.VOName
		and VOProbeSummary.ProbeName = new.ProbeName
		and VOProbeSummary.EndTime = date(new.EndTime);
	if mycount = 0 then
		insert into VOProbeSummary values(date(new.EndTime),new.VOName,new.ProbeName,new.Njobs,new.WallDuration,new.CpuUserDuration,new.CpuSystemDuration);
	elseif mycount > 0 then
		update VOProbeSummary
			set
				VOProbeSummary.Njobs = VOProbeSummary.Njobs + new.Njobs,
				VOProbeSummary.WallDuration = VOProbeSummary.WallDuration + new.WallDuration,
				VOProbeSummary.CpuUserDuration = VOProbeSummary.CpuUserDuration + new.CpuUserDuration,
				VOProbeSummary.CpuSystemDuration = VOProbeSummary.CpuSystemDuration + new.CpuSystemDuration
				where 
				VOProbeSummary.VOName = new.VOName
				and VOProbeSummary.ProbeName = new.ProbeName
				and VOProbeSummary.EndTime = date(new.EndTime);
	end if;

	--
	-- HostDescriptionProbeSummary
	--

	select count(*) into mycount from HostDescriptionProbeSummary
		where HostDescriptionProbeSummary.HostDescription = new.HostDescription
		and HostDescriptionProbeSummary.ProbeName = new.ProbeName
		and HostDescriptionProbeSummary.EndTime = date(new.EndTime);
	if mycount = 0 then
		insert into HostDescriptionProbeSummary values(date(new.EndTime),new.HostDescription,new.ProbeName,new.Njobs,new.WallDuration,new.CpuUserDuration,new.CpuSystemDuration);
	elseif mycount > 0 then
		update HostDescriptionProbeSummary
			set
				HostDescriptionProbeSummary.Njobs = HostDescriptionProbeSummary.Njobs + new.Njobs,
				HostDescriptionProbeSummary.WallDuration = HostDescriptionProbeSummary.WallDuration + new.WallDuration,
				HostDescriptionProbeSummary.CpuUserDuration = HostDescriptionProbeSummary.CpuUserDuration + new.CpuUserDuration,
				HostDescriptionProbeSummary.CpuSystemDuration = HostDescriptionProbeSummary.CpuSystemDuration + new.CpuSystemDuration
				where 
				HostDescriptionProbeSummary.HostDescription = new.HostDescription
				and HostDescriptionProbeSummary.ProbeName = new.ProbeName
				and HostDescriptionProbeSummary.EndTime = date(new.EndTime);
	end if;

	--
	-- NodeSummary
	--

	set startdate = new.StartTime;
	set enddate = new.EndTime;
	set node = new.Host;
	set myvoname = new.VOName;
	set mycpuusertime = new.CpuUserDuration;
	set mycpusystemtime = new.CpuSystemDuration;
	set myhostdescription = new.HostDescription;

	if myvoname = null then
		set myvoname = 'Unknown';
	end if;

	begin
		select BenchmarkScore,CPUCount into mybenchmarkscore,mycpucount from CPUInfo where myhostdescription = CPUInfo.NodeName;
	end;

	set numberofdays = datediff(enddate,startdate);
	set divide = numberofdays + 1;
	set newcpusystemtime = mycpusystemtime / divide;
	set newcpuusertime = mycpuusertime / divide;

	if numberofdays = 0 then
		call updatenodesummary(
			date(enddate),node,myvoname,mycpusystemtime,myspuusertime,mycpucount,myhostdescription,
			mybenchmarkscore,extract(DAY from last_day(enddate)));
	end if;

	if numberofdays > 0 then
		while counter < numberofdays do
			set newdate = adddate(startdate,counter);
			call updatenodesummary(
				date(newdate),node,myvoname,newcpusystemtime,newcpuusertime,mycpucount,
				myhostdescription,mybenchmarkscore,extract(DAY from last_day(newdate)));
			set counter = counter + 1;
		end while;
	end if;

end;
||
delete from trace
||
-- call buildnodesummary();
||

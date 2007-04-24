delimiter ||
drop table if exists NodeSummary;
||
CREATE TABLE NodeSummary (
	EndTime DATETIME NOT NULL DEFAULT 0,
	Node VARCHAR(255) NOT NULL DEFAULT '',
	ProbeName varchar(255) not null default '',
	ResourceType varchar(255) DEFAULT 'Unknown',
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
	add index index03(ProbeName);
||
alter table NodeSummary
	add index index04(ResourceType);
||
drop procedure updatenodesummary
||
create procedure updatenodesummary(enddate datetime,mynode varchar(255),
	myprobename varchar(255),myresourcetype varchar(255),
	mycpusystemtime int,mycpuusertime int,mycpucount int,
	myhostdescription varchar(255),mybenchmarkscore int,mydaysinmonth int)
begin
	declare mycount int default 0;
	select count(*) into mycount from NodeSummary
		where EndTime = enddate
		and Node = mynode
		and ResourceType = myresourcetype;
	if mycount = 0 then
		insert into NodeSummary values(
			date(enddate),
			mynode,
			myprobename,
			myresourcetype,
			mycpusystemtime,
			mycpuusertime,
			mycpucount,
			myhostdescription,
			mybenchmarkscore,
			mydaysinmonth);
	else
		update NodeSummary
			set 
				CpuSystemTime = CpuSystemTime + mycpusystemtime,
				CpuUserTime = CpuUserTime + mycpuusertime
			where
				EndTime = enddate
				and Node = mynode
				and ResourceType = myresourcetype;
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
	declare myprobename varchar(255);
	declare myresourcetype varchar(255);
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
	declare imax int default 0;

	declare cur01 cursor for select StartTime,EndTime,Host,ProbeName,ResourceType,
		CpuUserDuration,CpuSystemDuration,HostDescription
		from JobUsageRecord order by ProbeName,ResourceType,StartTime,EndTime;
	declare continue handler for sqlstate '02000' set done = true;

	open cur01;

	myloop: loop

		fetch cur01 into startdate,enddate,node,myprobename,myresourcetype,
			mycpuusertime,mycpusystemtime,myhostdescription;

		if done then
			close cur01;
			leave myloop;
		end if;

		if myprobename = null then
			set myprobename = 'Unknown';
		end if;

		if myresourcetype = null then
			set myresourcetype = 'Unknown';
		end if;

		begin
				set mycpucount = 0;
				set mybenchmarkscore = 0;
				select BenchmarkScore,CPUCount into mybenchmarkscore,mycpucount
					from CPUInfo where
					myhostdescription = CPUInfo.NodeName;
				set done = false;
		end;

		set numberofdays = datediff(enddate,startdate);
		set divide = numberofdays + 1;
		set newcpusystemtime = mycpusystemtime / divide;
		set newcpuusertime = mycpuusertime / divide;

		if numberofdays = 0 then
			call updatenodesummary(
				date(enddate),node,myprobename,myresourcetype,
				mycpusystemtime,mycpuusertime,mycpucount,myhostdescription,
				mybenchmarkscore,extract(DAY from last_day(enddate)));
		end if;

		if numberofdays > 0 then
			set imax = numberofdays + 1;
			set counter = 0;
			while counter < imax do
				set newdate = adddate(startdate,counter);
				call updatenodesummary(
					date(newdate),node,myprobename,myresourcetype,
					newcpusystemtime,newcpuusertime,mycpucount,
					myhostdescription,mybenchmarkscore,extract(DAY from last_day(newdate)));
				set counter = counter + 1;
			end while;
		end if;

	end loop;
end
||
delete from trace
||
call buildnodesummary();
||
-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

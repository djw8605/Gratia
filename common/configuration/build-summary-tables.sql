delimiter ||

drop table if exists ProbeSummary, UserProbeSummary, VOProbeSummary,
	ProbeStatus, HostDescriptionProbeSummary;
||
CREATE TABLE `ProbeStatus` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`Njobs` INTEGER NOT NULL DEFAULT 0
);
||
CREATE TABLE `ProbeSummary` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`SiteName` VARCHAR(255) DEFAULT 'Unknown',
	`ResourceType` VARCHAR(255) DEFAULT 'Unknown',
	`Njobs` INTEGER NOT NULL DEFAULT 0,
	`WallDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);
||
CREATE TABLE `UserProbeSummary` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`CommonName` VARCHAR(255) DEFAULT 'Unknown',
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`ResourceType` VARCHAR(255) DEFAULT 'Unknown',
	`Njobs` INTEGER NOT NULL DEFAULT 0,
	`WallDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);
||
CREATE TABLE `VOProbeSummary` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`VOName` VARCHAR(255) DEFAULT 'Unknown',
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`CommonName` VARCHAR(255) DEFAULT 'Unknown',
	`ResourceType` VARCHAR(255) DEFAULT 'Unknown',
	`Njobs` INTEGER NOT NULL DEFAULT 0,
	`WallDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);
||
CREATE TABLE `HostDescriptionProbeSummary` (
	`EndTime` DATETIME NOT NULL DEFAULT 0,
	`HostDescription` VARCHAR(255) DEFAULT 'Unknown',
	`ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
	`ResourceType` VARCHAR(255) DEFAULT 'Unknown',
	`Njobs` INTEGER NOT NULL DEFAULT 0,
	`WallDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
	`CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);
||
insert into ProbeStatus
	(select
		str_to_date(date_format(ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00') as EndTime,
		ProbeName,
		count(*) as Njobs
		from JobUsageRecord
		group by ProbeName,str_to_date(date_format(ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00')
);
||
alter table ProbeStatus
	add index index01(EndTime);
||
alter table ProbeStatus
	add index index02(ProbeName);
||
insert into ProbeSummary
	(select
		date(EndTime) as EndTime,
		ProbeName,
		SiteName,
		ResourceType,
		sum(Njobs) as Njobs,
		sum(WallDuration) as WallDuration,
		sum(CpuUserDuration) as CpuUserDuration,
		sum(CpuSystemDuration) as CpuSystemDuration
		from JobUsageRecord
		where CpuUserDuration is not null
		group by ProbeName,ResourceType,date(EndTime)
);
||
alter table ProbeSummary
	add index index01(EndTime);
||
alter table ProbeSummary
	add index index02(ProbeName);
||
alter table ProbeSummary
	add index index03(ResourceType);
||
insert into UserProbeSummary
	(select
		date(EndTime) as EndTime,
		CommonName,
		ProbeName,
		ResourceType,
		sum(Njobs) as Njobs,
		sum(WallDuration) as WallDuration,
		sum(CpuUserDuration) as CpuUserDuration,
		sum(CpuSystemDuration) as CpuSystemDuration
		from JobUsageRecord
		where CpuUserDuration is not null
		group by CommonName,ProbeName,ResourceType,date(EndTime)
);
||
alter table UserProbeSummary
	add index index01(EndTime);
||
alter table UserProbeSummary
	add index index02(CommonName);
||
alter table UserProbeSummary
	add index index03(ProbeName);
||
alter table UserProbeSummary
	add index index04(ResourceType);
||
insert into VOProbeSummary
	(select
		date(EndTime) as EndTime,
		VOName,
		ProbeName,
		CommonName,
		ResourceType,
		sum(Njobs) as Njobs,
		sum(WallDuration) as WallDuration,
		sum(CpuUserDuration) as CpuUserDuration,
		sum(CpuSystemDuration) as CpuSystemDuration
		from JobUsageRecord
		where CpuUserDuration is not null
		group by VOName,ProbeName,CommonName,ResourceType,date(EndTime)
);
||
alter table VOProbeSummary
	add index index01(EndTime);
||
alter table VOProbeSummary
	add index index02(VOName);
||
alter table VOProbeSummary
	add index index03(ProbeName);
||
alter table VOProbeSummary
	add index index04(ResourceType);
||
alter table VOProbeSummary
	add index index05(CommonName);
||
insert into HostDescriptionProbeSummary
	(select
		date(EndTime) as EndTime,
		HostDescription,
		ProbeName,
		ResourceType,
		sum(Njobs) as Njobs,
		sum(WallDuration) as WallDuration,
		sum(CpuUserDuration) as CpuUserDuration,
		sum(CpuSystemDuration) as CpuSystemDuration
		from JobUsageRecord
		where CpuUserDuration is not null
		group by HostDescription,ProbeName,ResourceType,date(EndTime)
);
||
alter table HostDescriptionProbeSummary
	add index index01(EndTime);
||
alter table HostDescriptionProbeSummary
	add index index02(HostDescription);
||
alter table HostDescriptionProbeSummary
	add index index03(ProbeName);
||
alter table HostDescriptionProbeSummary
	add index index04(ResourceType);
||
drop procedure if exists conditional_trigger_drop
||
create procedure conditional_trigger_drop()
begin

	declare mycount int;

	select count(*) into mycount from information_schema.triggers where
		trigger_schema = Database()
		and event_object_table = 'JobUsageRecord'
		and trigger_name = 'trigger01';

	if mycount > 0 then
		drop trigger trigger01;
  end if;
end
||
call conditional_trigger_drop()
||
create trigger trigger01 after insert on JobUsageRecord
for each row
glr:begin
	declare mycount int;
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
		and ProbeStatus.EndTime =
		str_to_date(date_format(new.ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00');
	if mycount = 0 then
		insert into ProbeStatus values(
			str_to_date(date_format(new.ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00'),
			new.ProbeName,1);
	elseif mycount > 0 then
		update ProbeStatus
			set
				ProbeStatus.Njobs = ProbeStatus.Njobs + 1
				where ProbeStatus.ProbeName = new.ProbeName
				and ProbeStatus.EndTime =
				str_to_date(date_format(new.ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00');
	end if;

	--
	-- ProbeSummary
	--

	select count(*) into mycount from ProbeSummary
		where ProbeSummary.ProbeName = new.ProbeName
		and ProbeSummary.EndTime = date(new.EndTime)
		and ProbeSummary.ResourceType = new.ResourceType;
	if mycount = 0 then
		insert into ProbeSummary values(date(new.EndTime),new.ProbeName,new.SiteName,new.ResourceType,
			new.Njobs,new.WallDuration,new.CpuUserDuration,new.CpuSystemDuration);
	elseif mycount > 0 then
		update ProbeSummary
			set
				ProbeSummary.Njobs = ProbeSummary.Njobs + new.Njobs,
				ProbeSummary.WallDuration = ProbeSummary.WallDuration + new.WallDuration,
				ProbeSummary.CpuUserDuration = ProbeSummary.CpuUserDuration + new.CpuUserDuration,
				ProbeSummary.CpuSystemDuration = ProbeSummary.CpuSystemDuration + new.CpuSystemDuration
				where ProbeSummary.ProbeName = new.ProbeName
				and ProbeSummary.EndTime = date(new.EndTime)
				and ProbeSummary.ResourceType = new.ResourceType;
	end if;

	--
	-- UserProbeSummary
	--

	select count(*) into mycount from UserProbeSummary
		where 
		UserProbeSummary.CommonName = new.CommonName
		and UserProbeSummary.ProbeName = new.ProbeName
		and UserProbeSummary.EndTime = date(new.EndTime)
		and UserProbeSummary.ResourceType = new.ResourceType;
	if mycount = 0 then
		insert into UserProbeSummary values(date(new.EndTime),new.CommonName,
			new.ProbeName,new.ResourceType,new.Njobs,new.WallDuration,
			new.CpuUserDuration,new.CpuSystemDuration);
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
				and UserProbeSummary.EndTime = date(new.EndTime)
				and UserProbeSummary.ResourceType = new.ResourceType;
	end if;

	--
	-- VOProbeSummary
	--

	select count(*) into mycount from VOProbeSummary
		where VOProbeSummary.VOName = new.VOName
		and VOProbeSummary.CommonName = new.CommonName
		and VOProbeSummary.ProbeName = new.ProbeName
		and VOProbeSummary.EndTime = date(new.EndTime)
		and VOProbeSummary.ResourceType = new.ResourceType;
	if mycount = 0 then
		insert into VOProbeSummary values(date(new.EndTime),new.VOName,new.ProbeName,
			new.CommonName,new.ResourceType,new.Njobs,new.WallDuration,new.CpuUserDuration,
			new.CpuSystemDuration);
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
				and VOProbeSummary.CommonName = new.CommonName
				and VOProbeSummary.EndTime = date(new.EndTime)
				and VOProbeSummary.ResourceType = new.ResourceType;
	end if;

	--
	-- HostDescriptionProbeSummary
	--

	select count(*) into mycount from HostDescriptionProbeSummary
		where HostDescriptionProbeSummary.HostDescription = new.HostDescription
		and HostDescriptionProbeSummary.ProbeName = new.ProbeName
		and HostDescriptionProbeSummary.EndTime = date(new.EndTime)
		and HostDescriptionProbeSummary.ResourceType = new.ResourceType;
	if mycount = 0 then
		insert into HostDescriptionProbeSummary values(date(new.EndTime),new.HostDescription,
			new.ProbeName,new.ResourceType,new.Njobs,new.WallDuration,
			new.CpuUserDuration,new.CpuSystemDuration);
	elseif mycount > 0 then
		update HostDescriptionProbeSummary
			set
				HostDescriptionProbeSummary.Njobs = HostDescriptionProbeSummary.Njobs + new.Njobs,
				HostDescriptionProbeSummary.WallDuration =
					HostDescriptionProbeSummary.WallDuration + new.WallDuration,
				HostDescriptionProbeSummary.CpuUserDuration =
					HostDescriptionProbeSummary.CpuUserDuration + new.CpuUserDuration,
				HostDescriptionProbeSummary.CpuSystemDuration =
					HostDescriptionProbeSummary.CpuSystemDuration + new.CpuSystemDuration
				where 
				HostDescriptionProbeSummary.HostDescription = new.HostDescription
				and HostDescriptionProbeSummary.ProbeName = new.ProbeName
				and HostDescriptionProbeSummary.EndTime = date(new.EndTime)
				and HostDescriptionProbeSummary.ResourceType = new.ResourceType;
	end if;

	--
	-- NodeSummary
	--

	select count(*) into mycount from information_schema.tables where
		table_schema = Database() and
		table_name = 'NodeSummary';

	if mycount > 0 then

		set startdate = new.StartTime;
		set enddate = new.EndTime;
		set node = new.Host;
		set myprobename = new.ProbeName;
		set myresourcetype = new.ResourceType;
		set mycpuusertime = new.CpuUserDuration;
		set mycpusystemtime = new.CpuSystemDuration;
		set myhostdescription = new.HostDescription;
	
		if myprobename = null then
			set myprobename = 'Unknown';
		end if;

		begin
			set mycpucount = 0;
			set mybenchmarkscore = 0;
			select BenchmarkScore,CPUCount into mybenchmarkscore,mycpucount from CPUInfo
				where myhostdescription = CPUInfo.NodeName;
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
	end if;

end;
||
-- show triggers;
-- ||

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

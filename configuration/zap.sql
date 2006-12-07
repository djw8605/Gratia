delimiter ||
drop table if exists ProbeStatus, HostDescriptionProbeSummary
||
CREATE TABLE `ProbeStatus` (
  `EndTime` DATETIME NOT NULL DEFAULT 0,
  `ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
  `Njobs` INTEGER NOT NULL DEFAULT 0
)
||
CREATE TABLE `HostDescriptionProbeSummary` (
  `EndTime` DATETIME NOT NULL DEFAULT 0,
  `HostDescription` VARCHAR(255) DEFAULT 'Unknown',
  `ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
  `Njobs` INTEGER NOT NULL DEFAULT 0,
  `WallDuration` DOUBLE NOT NULL DEFAULT 0,
  `CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
  `CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
)
||
insert into ProbeStatus
  (select
    str_to_date(date_format(ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00') as EndTime,
    ProbeName,
    count(*) as Njobs
    from JobUsageRecord
    group by ProbeName,str_to_date(date_format(ServerDate,'%Y-%c-%e %H:00:00'),'%Y-%c-%e %H:00:00')
)
||
alter table ProbeStatus
  add index index01(EndTime)
||
alter table ProbeStatus
  add index index02(ProbeName)
||
insert into HostDescriptionProbeSummary
  (select
    date(EndTime) as EndTime,
    HostDescription,
    JobUsageRecord.ProbeName,
    sum(Njobs) as Njobs,
    sum(WallDuration) as WallDuration,
    sum(CpuUserDuration) as CpuUserDuration,
    sum(CpuSystemDuration) as CpuSystemDuration
    from JobUsageRecord
		where CpuUserDuration is not null
    group by HostDescription,ProbeName,date(EndTime)
)
||
alter table HostDescriptionProbeSummary
  add index index01(EndTime)
||
alter table HostDescriptionProbeSummary
  add index index02(HostDescription)
||
alter table HostDescriptionProbeSummary
  add index index03(ProbeName)
||
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
drop trigger trigger01;
||
create trigger trigger01 after insert on JobUsageRecord
for each row
glr:begin
	declare mycount int;
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

end;
||

drop table if exists ProbeSummary, UserProbeSummary, VOProbeSummary;

CREATE TABLE `ProbeSummary` (
  `EndTime` DATETIME NOT NULL DEFAULT 0,
  `ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
  `SiteName` VARCHAR(255) NOT NULL DEFAULT '',
  `Njobs` INTEGER NOT NULL DEFAULT 0,
  `WallDuration` DOUBLE NOT NULL DEFAULT 0,
  `CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
  `CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);

CREATE TABLE `UserProbeSummary` (
  `EndTime` DATETIME NOT NULL DEFAULT 0,
  `CommonName` VARCHAR(255) NOT NULL DEFAULT '',
  `ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
  `Njobs` INTEGER NOT NULL DEFAULT 0,
  `WallDuration` DOUBLE NOT NULL DEFAULT 0,
  `CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
  `CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);

CREATE TABLE `VOProbeSummary` (
  `EndTime` DATETIME NOT NULL DEFAULT 0,
  `VOName` VARCHAR(255) NOT NULL DEFAULT '',
  `ProbeName` VARCHAR(255) NOT NULL DEFAULT '',
  `Njobs` INTEGER NOT NULL DEFAULT 0,
  `WallDuration` DOUBLE NOT NULL DEFAULT 0,
  `CpuUserDuration` DOUBLE NOT NULL DEFAULT 0,
  `CpuSystemDuration` DOUBLE NOT NULL DEFAULT 0
);

insert into ProbeSummary
  (select
    date(EndTime) as EndTime,
    ProbeName,
		SiteName,
    sum(Njobs) as Njobs,
    sum(WallDuration) as WallDuration,
    sum(CpuUserDuration) as CpuUserDuration,
    sum(CpuSystemDuration) as CpuSystemDuration
    from JobUsageRecord
		where CpuUserDuration is not null
    group by ProbeName,date(EndTime)
);

alter table ProbeSummary
  add index index01(EndTime);

alter table ProbeSummary
  add index index02(ProbeName);

insert into UserProbeSummary
  (select
    date(EndTime) as EndTime,
    CommonName,
    JobUsageRecord.ProbeName,
    sum(Njobs) as Njobs,
    sum(WallDuration) as WallDuration,
    sum(CpuUserDuration) as CpuUserDuration,
    sum(CpuSystemDuration) as CpuSystemDuration
    from JobUsageRecord
		where CpuUserDuration is not null
    group by CommonName,ProbeName,date(EndTime)
);

alter table UserProbeSummary
  add index index01(EndTime);

alter table UserProbeSummary
  add index index02(CommonName);

alter table UserProbeSummary
  add index index03(ProbeName);

insert into VOProbeSummary
  (select
    date(EndTime) as EndTime,
    VOName,
    JobUsageRecord.ProbeName,
    sum(Njobs) as Njobs,
    sum(WallDuration) as WallDuration,
    sum(CpuUserDuration) as CpuUserDuration,
    sum(CpuSystemDuration) as CpuSystemDuration
    from JobUsageRecord
		where CpuUserDuration is not null
    group by VOName,ProbeName,date(EndTime)
);

alter table VOProbeSummary
  add index index01(EndTime);

alter table VOProbeSummary
  add index index02(VOName);

alter table VOProbeSummary
  add index index03(ProbeName);

drop trigger trigger01;

delimiter |
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
	if new.SiteName is null then
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

end;
|




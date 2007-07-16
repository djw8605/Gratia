delimiter ||

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

	select count(*) into mycount from information_schema.triggers where
		trigger_schema = Database()
		and event_object_table = 'JobUsageRecord_Meta'
		and trigger_name = 'trigger02';

	if mycount > 0 then
		drop trigger trigger02;
	end if;
end
||
call conditional_trigger_drop()
||
create trigger trigger02 after insert on JobUsageRecord_Meta
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

  declare n_VOName varchar(255);
	declare n_ReportableVOName varchar(255);
	declare n_VOcorrid int;
	declare n_ProbeName varchar(255);
	declare n_ResourceType varchar(255);
	declare n_WallDuration int default 0;
	declare n_CpuUserDuration int default 0;
	declare n_CpuSystemDuration  int default 0;
	declare n_HostDescription varchar(255);
	declare n_Host text;
  declare n_NJobs int;
  declare n_CommonName varchar(255);
  declare n_StartTime datetime;
  declare n_EndTime datetime;

	--
	-- basic data checks
	--
	if new.ProbeName is null then
		leave glr;
	end if;

  SELECT J.VOName,J.NJobs,J.WallDuration,J.CpuUserDuration,J.CpuSystemDuration,
         J.CommonName,J.ResourceType,J.EndTime,J.StartTime,J.HostDescription,J.Host,VC.corrid
             INTO n_VOName,n_NJobs,n_WallDuration,n_CpuUserDuration,n_CpuSystemDuration,
                  n_CommonName,n_ResourceType,n_EndTime,n_StartTime,n_HostDescription,n_Host,n_VOcorrid
             FROM JobUsageRecord J, VONameCorrection VC
             WHERE J.dbid = new.dbid
               AND J.VOName = binary VC.VOName
               AND ((J.ReportableVOName = binary VC.ReportableVOName)
	                  OR ((J.ReportableVOName IS NULL) AND (VC.ReportableVOName IS NULL)));

	if n_VOName is null then
		leave glr;
	end if;
	if n_Njobs is null then
		leave glr;
	end if;
	if n_WallDuration is null then
		leave glr;
	end if;
	if n_CpuUserDuration is null then
		leave glr;
	end if;
	if n_CpuSystemDuration is null then
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
		and ProbeSummary.EndTime = date(n_EndTime)
		and ProbeSummary.ResourceType = n_ResourceType;
	if mycount = 0 then
		insert into ProbeSummary values(date(n_EndTime),new.ProbeName,new.ReportedSiteName,n_ResourceType,
			n_Njobs,n_WallDuration,n_CpuUserDuration,n_CpuSystemDuration);
	elseif mycount > 0 then
		update ProbeSummary
			set
				ProbeSummary.Njobs = ProbeSummary.Njobs + n_Njobs,
				ProbeSummary.WallDuration = ProbeSummary.WallDuration + n_WallDuration,
				ProbeSummary.CpuUserDuration = ProbeSummary.CpuUserDuration + n_CpuUserDuration,
				ProbeSummary.CpuSystemDuration = ProbeSummary.CpuSystemDuration + n_CpuSystemDuration
				where ProbeSummary.ProbeName = new.ProbeName
				and ProbeSummary.EndTime = date(n_EndTime)
				and ProbeSummary.ResourceType = n_ResourceType;
	end if;

	--
	-- UserProbeSummary
	--

	select count(*) into mycount from UserProbeSummary
		where 
		UserProbeSummary.CommonName = n_CommonName
		and UserProbeSummary.ProbeName = new.ProbeName
		and UserProbeSummary.EndTime = date(n_EndTime)
		and UserProbeSummary.ResourceType = n_ResourceType;
	if mycount = 0 then
		insert into UserProbeSummary values(date(n_EndTime),n_CommonName,
			new.ProbeName,n_ResourceType,n_Njobs,n_WallDuration,
			n_CpuUserDuration,n_CpuSystemDuration);
	elseif mycount > 0 then
		update UserProbeSummary
			set
				UserProbeSummary.Njobs = UserProbeSummary.Njobs + n_Njobs,
				UserProbeSummary.WallDuration = UserProbeSummary.WallDuration + n_WallDuration,
				UserProbeSummary.CpuUserDuration = UserProbeSummary.CpuUserDuration + n_CpuUserDuration,
				UserProbeSummary.CpuSystemDuration = UserProbeSummary.CpuSystemDuration + n_CpuSystemDuration
				where 
				UserProbeSummary.CommonName = n_CommonName
				and UserProbeSummary.ProbeName = new.ProbeName
				and UserProbeSummary.EndTime = date(n_EndTime)
				and UserProbeSummary.ResourceType = n_ResourceType;
	end if;

	--
	-- VOProbeSummaryData
	--

	select count(*) into mycount from VOProbeSummaryData VPSD
		where VPSD.CommonName = n_CommonName
		 and VPSD.ProbeName = new.ProbeName
		 and VPSD.EndTime = date(n_EndTime)
		 and VPSD.ResourceType = n_ResourceType
     and VPSD.VOcorrid = n_VOcorrid;
	if mycount = 0 then
		insert into VOProbeSummaryData values(date(n_EndTime),n_VOcorrid,new.ProbeName,
			n_CommonName,n_ResourceType,n_Njobs,n_WallDuration,n_CpuUserDuration,
			n_CpuSystemDuration);
	elseif mycount > 0 then
		update VOProbeSummaryData VPSD
			set
				VPSD.Njobs = VPSD.Njobs + n_Njobs,
				VPSD.WallDuration = VPSD.WallDuration + n_WallDuration,
				VPSD.CpuUserDuration = VPSD.CpuUserDuration + n_CpuUserDuration,
				VPSD.CpuSystemDuration = VPSD.CpuSystemDuration + n_CpuSystemDuration
				where 
				VPSD.VOcorrid = n_VOcorrid
				and VPSD.ProbeName = new.ProbeName
				and VPSD.CommonName = n_CommonName
				and VPSD.EndTime = date(n_EndTime)
				and VPSD.ResourceType = n_ResourceType;
	end if;

	--
	-- HostDescriptionProbeSummary
	--

	select count(*) into mycount from HostDescriptionProbeSummary
		where HostDescriptionProbeSummary.HostDescription = n_HostDescription
		and HostDescriptionProbeSummary.ProbeName = new.ProbeName
		and HostDescriptionProbeSummary.EndTime = date(n_EndTime)
		and HostDescriptionProbeSummary.ResourceType = n_ResourceType;
	if mycount = 0 then
		insert into HostDescriptionProbeSummary values(date(n_EndTime),n_HostDescription,
			new.ProbeName,n_ResourceType,n_Njobs,n_WallDuration,
			n_CpuUserDuration,n_CpuSystemDuration);
	elseif mycount > 0 then
		update HostDescriptionProbeSummary
			set
				HostDescriptionProbeSummary.Njobs = HostDescriptionProbeSummary.Njobs + n_Njobs,
				HostDescriptionProbeSummary.WallDuration =
					HostDescriptionProbeSummary.WallDuration + n_WallDuration,
				HostDescriptionProbeSummary.CpuUserDuration =
					HostDescriptionProbeSummary.CpuUserDuration + n_CpuUserDuration,
				HostDescriptionProbeSummary.CpuSystemDuration =
					HostDescriptionProbeSummary.CpuSystemDuration + n_CpuSystemDuration
				where 
				HostDescriptionProbeSummary.HostDescription = n_HostDescription
				and HostDescriptionProbeSummary.ProbeName = new.ProbeName
				and HostDescriptionProbeSummary.EndTime = date(n_EndTime)
				and HostDescriptionProbeSummary.ResourceType = n_ResourceType;
	end if;

	--
	-- NodeSummary
	--

	select count(*) into mycount from information_schema.tables where
		table_schema = Database() and
		table_name = 'NodeSummary';

	if mycount > 0 then

		set startdate = n_StartTime;
		set enddate = n_EndTime;
		set node = n_Host;
		set myprobename = new.ProbeName;
		set myresourcetype = n_ResourceType;
		set mycpuusertime = n_CpuUserDuration;
		set mycpusystemtime = n_CpuSystemDuration;
		set myhostdescription = n_HostDescription;
	
		if myprobename = null then
			set myprobename = 'Unknown';
		end if;

		begin
			set mycpucount = 0;
			set mybenchmarkscore = 0;
			select BenchmarkScore,CPUCount into mybenchmarkscore,mycpucount from CPUInfo
				where myhostdescription = CPUInfo.HostDescription;
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

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

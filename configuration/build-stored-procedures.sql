delimiter |
drop procedure DailyJobsByFacility
|
create procedure DailyJobsByFacility (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
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
end
|
-- call DailyJobsByFacility('2006-10-01 00:00:00','2006-10-10 00:00:00','%y:%m:%d:%H:%i')
|
-- call DailyJobsByFacility('2006-10-01 00:00:00','2006-10-03 00:00:00','%y:%m:%d:%H:%i')
|
drop procedure DailyJobsByFacilityAndDate
|
create procedure DailyJobsByFacilityAndDate (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
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
end
|
-- call DailyJobsByFacilityAndDate('2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
|
-- call DailyJobsByFacilityAndDate('2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
|
drop procedure DailyJobsByProbe
|
create procedure DailyJobsByProbe (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
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
end
|
-- call DailyJobsByProbe('2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
|
-- call DailyJobsByProbe('2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
|
drop procedure DailyJobsByProbeAndDate
|
create procedure DailyJobsByProbeAndDate (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
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
end
|
-- call DailyJobsByProbeAndDate('2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
|
-- call DailyJobsByProbeAndDate('2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
|
drop procedure DailyJobsByVO
|
create procedure DailyJobsByVO (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
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
end
|
-- call DailyJobsByVO('2006-10-01 00:00:00','2006-10-10 00:00:00','%y:%m:%d:%H:%i')
|
-- call DailyJobsByVO('2006-10-01 00:00:00','2006-10-03 00:00:00','%y:%m:%d:%H:%i')
|
drop procedure DailyJobsByVOAndDate
|
create procedure DailyJobsByVOAndDate (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
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
end
|
-- call DailyJobsByVOAndDate('2006-10-01 00:00:00','2006-10-10 00:00:00','%y:%m:%d:%H:%i')
|
-- call DailyJobsByVOAndDate('2006-10-01 00:00:00','2006-10-03 00:00:00','%y:%m:%d:%H:%i')
|
drop procedure DailyUsageByFacility
|
create procedure DailyUsageByFacility (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
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
end
|
-- call DailyUsageByFacility('2006-10-01 00:00:00','2006-10-10 00:00:00','%y:%m:%d:%H:%i')
|
-- call DailyUsageByFacility('2006-10-01 00:00:00','2006-10-03 00:00:00','%y:%m:%d:%H:%i')
|
drop procedure DailyUsageByFacilityAndDate
|
create procedure DailyUsageByFacilityAndDate (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
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
end
|
-- call DailyUsageByFacilityAndDate('2006-10-01 00:00:00','2006-10-10 00:00:00','%y:%m:%d:%H:%i')
|
-- call DailyUsageByFacilityAndDate('2006-10-01 00:00:00','2006-10-03 00:00:00','%y:%m:%d:%H:%i')
|
drop procedure DailyUsageByProbe
|
create procedure DailyusageByProbe (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
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
end
|
-- call DailyUsageByProbe('2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
|
-- call DailyUsageByProbe('2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
|
drop procedure DailyUsageByProbeAndDate
|
create procedure DailyUsageByProbeAndDate (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
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
end
|
-- call DailyUsageByProbeAndDate('2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
|
-- call DailyUsageByProbeAndDate('2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
|
drop procedure DailyUsageBySite
|
create procedure DailyUsageBySite (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
	if datediff(todate,fromdate) > 6 then
		select SiteName,EndTime as endtime,sum(WallDuration) as WallDuration,sum(CpuUserDuration + CpuSystemDuration) as Cpu
			from ProbeSummary
			where
				EndTime >= date(fromdate) and EndTime <= date(todate)
			group by EndTime,ProbeName
			order by EndTime,ProbeName;
	else
		select ProbeName,date_format(EndTime,format) as endtime,sum(WallDuration) as WallDuration,sum(CpuUserDuration + CpuSystemDuration) as Cpy
			from JobUsageRecord
			where
				EndTime >= fromdate and EndTime <= todate
			group by date_format(EndTime,format),ProbeName
			order by date_format(EndTime,format),ProbeName;
	end if;
end
|
-- call DailyUsageBySite('2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
|
-- call DailyUsageBySite('2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
|
drop procedure DailyUsageByVO
|
create procedure DailyUsageByVO (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
	if datediff(todate,fromdate) > 6 then
		select VOName,EndTime as endtime,sum(WallDuration) as WallDuration,sum(CpuUserDuration + CpuSystemDuration) as Cpu
			from VOProbeSummary
			where
				EndTime >= date(fromdate) and EndTime <= date(todate)
			group by EndTime,VOName
			order by EndTime;
	else
		select VOName,date_format(EndTime,format) as endtime,sum(WallDuration) as WallDuration,sum(CpuUserDuration + CpuSystemDuration) as Cpy
			from JobUsageRecord
			where
				EndTime >= fromdate and EndTime <= todate
			group by date_format(EndTime,format),VOName
			order by date_format(EndTime,format);
	end if;
end
|
-- call DailyUsageByVO('2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
|
-- call DailyUsageByVO('2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
|
drop procedure DailyUsageByVOAndDate
|
create procedure DailyUsageByVOAndDate (fromdate varchar(255),todate varchar(255),format varchar(255))
begin
	if datediff(todate,fromdate) > 6 then
		select VOName,EndTime as endtime,sum(WallDuration) as WallDuration,sum(CpuUserDuration + CpuSystemDuration) as Cpu
			from VOProbeSummary
			where
				EndTime >= date(fromdate) and EndTime <= date(todate)
			group by EndTime,VOName
			order by EndTime,VOName;
	else
		select VOName,date_format(EndTime,format) as endtime,sum(WallDuration) as WallDuration,sum(CpuUserDuration + CpuSystemDuration) as Cpy
			from JobUsageRecord
			where
				EndTime >= fromdate and EndTime <= todate
			group by date_format(EndTime,format),VOName
			order by date_format(EndTime,format),VOName;
	end if;
end
|
-- call DailyUsageByVOAndDate('2006-10-01 00:00:00','2006-10-10 00:00:00','%m/%d/%Y %T')
|
-- call DailyUsageByVOAndDate('2006-10-01 00:00:00','2006-10-03 00:00:00','%m/%d/%Y %T')
|
drop procedure JobsByFacility
|
create procedure JobsByFacility (fromdate varchar(255),todate varchar(255))
begin
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
end
|
-- call JobsByFacility('2006-10-01 00:00:00','2006-10-10 00:00:00')
|
-- call JobsByFacility('2006-10-01 00:00:00','2006-10-03 00:00:00')
|
drop procedure JobsByFacility
|
create procedure JobsByFacility (fromdate varchar(255),todate varchar(255))
begin
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
end
|
-- call JobsByFacility('2006-10-01 00:00:00','2006-10-10 00:00:00')
|
-- call JobsByFacility('2006-10-01 00:00:00','2006-10-03 00:00:00')
|
drop procedure JobsByProbeNoFacility
|
create procedure JobsByProbeNoFacility (fromdate varchar(255),todate varchar(255))
begin
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
end
|
-- call JobsByProbeNoFacility('2006-10-01 00:00:00','2006-10-10 00:00:00')
|
-- call JobsByProbeNoFacility('2006-10-01 00:00:00','2006-10-03 00:00:00')
|
drop procedure JobsBySite
|
create procedure JobsBySite (fromdate varchar(255),todate varchar(255))
begin
	if datediff(todate,fromdate) > 6 then
		select SiteName,sum(Njobs) as Njobs
			from ProbeSummary
			where
				EndTime >= date(fromdate)
				and EndTime <= date(todate)
			group by SiteName
			order by SiteName;
	else
		select SiteName,sum(Njobs) as Njobs
			from JobUsageRecord
			where
				EndTime >= fromdate
				and EndTime <= todate
			group by SiteName
			order by SiteName;
	end if;
end
|
-- call JobsBySite('2006-10-01 00:00:00','2006-10-10 00:00:00')
|
-- call JobsBySite('2006-10-01 00:00:00','2006-10-03 00:00:00')
|
drop procedure JobsByUser
|
create procedure JobsByUser (fromdate varchar(255),todate varchar(255))
begin
	if datediff(todate,fromdate) > 6 then
		select CommonName as UserName,sum(Njobs) as Njobs
			from UserProbeSummary
			where
				EndTime >= date(fromdate)
				and EndTime <= date(todate)
			group by CommonName
			order by CommonName;
	else
		select CommonName as UserName,sum(Njobs) as Njobs
			from JobUsageRecord
			where
				EndTime >= fromdate
				and EndTime <= todate
			group by CommonName
			order by CommonName;
	end if;
end
|
-- call JobsByUser('2006-10-01 00:00:00','2006-10-10 00:00:00')
|
-- call JobsByUser('2006-10-01 00:00:00','2006-10-03 00:00:00')
|
drop procedure JobsByUser
|
create procedure JobsByVO (fromdate varchar(255),todate varchar(255))
begin
	if datediff(todate,fromdate) > 6 then
		select VOName,sum(Njobs) as Njobs
			from VOProbeSummary
			where
				EndTime >= date(fromdate)
				and EndTime <= date(todate)
			group by VOName
			order by VOName;
	else
		select VOName,sum(Njobs) as Njobs
			from JobUsageRecord
			where
				EndTime >= fromdate
				and EndTime <= todate
			group by VOName
			order by VOName;
	end if;
end
|
-- call JobsByVO('2006-10-01 00:00:00','2006-10-10 00:00:00')
|
-- call JobsByVO('2006-10-01 00:00:00','2006-10-03 00:00:00')
|
drop procedure UsageByFacility
|
create procedure UsageByFacility (fromdate varchar(255),todate varchar(255))
begin
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
end
|
-- call UsageByFacility('2006-10-01 00:00:00','2006-10-10 00:00:00')
|
-- call UsageByFacility('2006-10-01 00:00:00','2006-10-03 00:00:00')
|
drop procedure UsageByProbe
|
create procedure UsageByProbe (fromdate varchar(255),todate varchar(255))
begin
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
end
|
-- call UsageByProbe('2006-10-01 00:00:00','2006-10-10 00:00:00')
|
-- call UsageByProbe('2006-10-01 00:00:00','2006-10-03 00:00:00')
|
drop procedure UsageByProbe
|
create procedure UsageBySite (fromdate varchar(255),todate varchar(255))
begin
	if datediff(todate,fromdate) > 6 then
		select SiteName,sum(WallDuration) as WallDuration
			from ProbeSummary
			where
				EndTime >= date(fromdate) and EndTime <= date(todate)
			group by SiteName
			order by SiteName;
	else
		select SiteName,sum(WallDuration) as WallDuration
			from JobUsageRecord
			where
				EndTime >= fromdate and EndTime <= todate
			group by SiteName
			order by SiteName;
	end if;
end
|
-- call UsageBySite('2006-10-01 00:00:00','2006-10-10 00:00:00')
|
-- call UsageBySite('2006-10-01 00:00:00','2006-10-03 00:00:00')
|

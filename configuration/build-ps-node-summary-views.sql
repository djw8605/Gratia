delimiter ||
drop view if exists NodeSummaryView1;
||
create view NodeSummaryView1 as
select
				EndTime as EndTime,
				Node as Node,
				(CpuSystemTime + CpuUserTime) as Utilization,
				CpuCount as CpuCount,
				(CpuCount * 86400) as MaxUtilization,
				round(((CpuSystemTime + CpuUserTime) / (CpuCount * 86400)) * 100,2) as Percentage
				from NodeSummary
				group by Node,EndTime
				order by Node,EndTime;
||



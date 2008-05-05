select P.probename, M.dbid, M.ServerDate
from JobUsageRecord R
     join JobUsageRecord_Meta M on (R.dbid = M.dbid)
     join Probe P on (M.probeid = P.probeid),
 (select unix_timestamp('2008-01-01 00:00:00') as compare_date) X
where M.ServerDate > '2008-04-01 00:00:00'
  and ((WallDuration > compare_date)
    or (CpuSystemDuration > compare_date)
    or (CpuUserDuration > compare_date))
order by M.ServerDate desc;

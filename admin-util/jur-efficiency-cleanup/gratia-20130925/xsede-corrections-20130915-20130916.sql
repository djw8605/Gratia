--  This are updates to the 9/15/13 and 9/16/13 corrections for
--  probe: condor:osg-xsede.grid.iu.edu  site: OSG-XSEDE
--  that were made on 9/17/13.
--  For some reason I cannot determine, those changes did not take.
--  At the time we made them they appeared good.
--  These will be applied one at at time and a query made to verify eacn one.
--  The 18446744476 CpuSystemDuration equates to roughtly 5,124,095 hrs.
--  The current view for those dates is
--                   Jobs        CPU         Wall
--     2013-09-15 | 144322 |  51,447,840 |  263,722 |
--     2013-09-16 | 146063 |  25,823,952 |  266,756 |
--                            77,271,792    530,966
--  The expectation is that it should drop by
--                            76,861,435         62
--  resulting cpu for 2 days     411,227 roughly
--
-- Verifcation query:
set @start := "2013-09-15";
set @end   := "2013-09-17";
SELECT
  SiteName
 ,date_format(EndTime, "%Y-%m-%d") as period
 ,sum(Njobs) as Jobs
 ,round(sum(CpuUserDuration + CpuSystemDuration)/3600,0) as CpuHrs
 ,round(sum(WallDuration)/3600,0) as WallHrs
FROM VOProbeSummary vps , Site s , Probe p
WHERE vps.EndTime >= @start AND vps.EndTime <  @end
AND vps.ProbeName like '%xsede%'
AND vps.ProbeName  = p.probename
AND p.siteid = s.siteid
GROUP by SiteName ,period
;
--
-- --------------------------------------------------------------------------------------------
update JobUsageRecord set CpuSystemDuration= 18446744476 , CpuUserDuration= 30975 , WallDuration= 34140  where dbid= 1140271758 ;
call del_JUR_from_summary(1140271758);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744305 , CpuUserDuration= 1487 , WallDuration= 3451  where dbid= 1140297735 ;
call del_JUR_from_summary(1140297735);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744489 , CpuUserDuration= 447 , WallDuration= 5156  where dbid= 1140325757 ;
call del_JUR_from_summary(1140325757);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744146 , CpuUserDuration= 94 , WallDuration= 568  where dbid= 1140326891 ;
call del_JUR_from_summary(1140326891);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744461 , CpuUserDuration= 1752 , WallDuration= 5293  where dbid= 1140354089 ;
call del_JUR_from_summary(1140354089);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744426 , CpuUserDuration= 1388 , WallDuration= 7307  where dbid= 1140471648 ;
call del_JUR_from_summary(1140471648);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744502 , CpuUserDuration= 10671 , WallDuration= 14249  where dbid= 1140519133 ;
call del_JUR_from_summary(1140519133);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744177 , CpuUserDuration= 938 , WallDuration= 2030  where dbid= 1140655640 ;
call del_JUR_from_summary(1140655640);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744517 , CpuUserDuration= 12848 , WallDuration= 18350  where dbid= 1140671577 ;
call del_JUR_from_summary(1140671577);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744637 , CpuUserDuration= 30509 , WallDuration= 35259  where dbid= 1140678693 ;
call del_JUR_from_summary(1140678693);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744441 , CpuUserDuration= 12271 , WallDuration= 14820  where dbid= 1140801318 ;
call del_JUR_from_summary(1140801318);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744620 , CpuUserDuration= 25815 , WallDuration= 33046  where dbid= 1140801788 ;
call del_JUR_from_summary(1140801788);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744593 , CpuUserDuration= 34549 , WallDuration= 39039  where dbid= 1140852176 ;
call del_JUR_from_summary(1140852176);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744372 , CpuUserDuration= 4229 , WallDuration= 6682  where dbid= 1140925901 ;
call del_JUR_from_summary(1140925901);
-- 
update JobUsageRecord set CpuSystemDuration= 18446744293 , CpuUserDuration= 831 , WallDuration= 4037  where dbid= 1141008111 ;
call del_JUR_from_summary(1141008111);


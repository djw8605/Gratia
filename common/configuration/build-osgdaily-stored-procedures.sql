DELIMITER $$

DROP PROCEDURE IF EXISTS `gratia_osg_daily`.`UsageByProbeForVOForSite` $$
CREATE DEFINER=CURRENT_USER PROCEDURE `UsageByProbeForVOForSite`(userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64), vos varchar(128), voseltype varchar(8),sites varchar(128))
    READS SQL DATA
begin
  select generateResourceTypeClause(resourceType) into @myresourceclause;
  select SystemProplist.cdr into @usereportauthentication from SystemProplist
  where SystemProplist.car = 'use.report.authentication';
  select Role.whereclause into @mywhereclause from Role
    where Role.role = userRole;
  select generateWhereClause(userName,userRole,@mywhereclause)
    into @mywhereclause;
  call parse(userName,@name,@key,@vo);


  
  if voseltype = 'NOT' then
    set voseltype := 'NOT IN';
  end if;
  set @sql :=
           concat_ws('', 'select  date_format(J.EndTime, ''', format, ''') as endtime,
                      sum(J.WallDuration) as WallDuration,
                      sum(J.CpuUserDuration +
                        J.CpuSystemDuration) as Cpu,
                      sum(J.Njobs) as Njobs,
                      JM.ReportedSiteName as sitename,
                      JM.ProbeName as probename',
                     ' from JobUsageRecord_Meta JM, JobUsageRecord J',
                     ' where',
                     '  J.dbid = JM.dbid'
                     ' and J.VOName ', voseltype, vos,
                     ' and JM.ReportedSiteName IN ', sites,
                     ' and EndTime >= date(''', fromdate, ''')',
                     ' and EndTime <= date(''', todate, ''')'
                     ' ', @myresourceclause,
                     ' ', @mywhereclause
                     , ' group by sitename,probename,date_format(J.EndTime,''', format, ''')'
                     , ' order by probename, sitename, J.EndTime'
                    );

  insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,data)
    values('UsageByProbeForVOForSite',@key,userName,userRole,@vo,
    fromdate,todate,format,resourceType,@sql);
  prepare statement from @sql;
  execute statement;
  deallocate prepare statement;
end $$

DELIMITER ;
DELIMITER $$

DROP PROCEDURE IF EXISTS `gratia_osg_daily`.`UsageByProbeForVOForSite` $$
CREATE DEFINER=CURRENT_USER PROCEDURE `UsageByProbeForVOForSite`(userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64), vos varchar(128), voseltype varchar(8),sites varchar(128))
    READS SQL DATA

begin
  select sysdate() into @proc_start;
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


  select sysdate() into @query_start;
  prepare statement from @sql;
  execute statement;
  select sysdate() into @query_end;
  
  insert into trace(procName,  userKey, userName, userRole, userVO, sqlQuery, procTime, queryTime, p1, p2, p3)
             values('reports', @key,    userName,   userRole,   @vo,    @sql,
             		  timediff(@query_start, @proc_start),
      			  timediff(@query_end,   @query_start),
    			  null, null, null);   
  deallocate prepare statement;
end $$

DELIMITER ;
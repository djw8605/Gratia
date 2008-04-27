DELIMITER $$

DROP PROCEDURE IF EXISTS `WeeklyUsageByVORanked`$$
CREATE PROCEDURE `WeeklyUsageByVORanked`(userName varchar(64), userRole varchar(64), fromdate varchar(64), todate varchar(64), format varchar(64), resourceType varchar(64))
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
  select sysdate() into @query_start;
  set @sql :=
           concat_ws('', 'select final_rank,
       VOProbeSummary.VOName,
       datevalue,
       sum(VOProbeSummary.WallDuration) as WallDuration,
       sum(VOProbeSummary.Cpu) as Cpu,
       sum(VOProbeSummary.Njobs) as Njobs 
from (SELECT @rank:=@rank+1 as final_rank, 
      VONamex,
      walldurationx
      FROM (SELECT @rank:=0 as rank,
                   VO.VOName as VONamex,
                   endtimex,
                   sum(V.walldurationx) as walldurationx
            FROM (SELECT VOCorrid as VOCorridx,
                         EndTime as endtimex,
                         sum(WallDuration) as walldurationx 
                  FROM MasterSummaryData 
       WHERE (EndTime) >= (''', fromdate, ' 00:00:00 '')
                            and (EndTime) <= (''', todate, ' 00:00:00 '')
             and ResourceType = ''Batch''
                  GROUP by VOCorrid) V, 
                 VONameCorrection Corr, 
                 VO
            WHERE V.VOCorridx = Corr.corrid and Corr.VOid = VO.VOid
            GROUP BY VONamex
            ORDER BY walldurationx desc
           ) as foox
     ) as foo, 
     ( select EndTime, 
              VOName,
              sum(VOProbeSummary.WallDuration) as WallDuration,
              sum(VOProbeSummary.CpuUserDuration +
VOProbeSummary.CpuSystemDuration) as Cpu,
              sum(VOProbeSummary.Njobs) as Njobs,
              date_format(VOProbeSummary.EndTime,''', format, ''') as datevalue
       from VOProbeSummary 
       WHERE (EndTime) >= (''', fromdate, ' 00:00:00 '')
                            and (EndTime) <= (''', todate, ' 00:00:00 '')
             and ResourceType = ''Batch''
       GROUP by datevalue, VOProbeSummary.VOName 
     ) as VOProbeSummary
WHERE VOProbeSummary.VOName = foo.VONamex
GROUP by datevalue, VOProbeSummary.VOName
order by final_rank, VOProbeSummary.VOName,datevalue'
                 );
  prepare statement from @sql;
  execute statement;
  insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,p5,p6,data)
    values('WeeklyUsageByVORanked',@key,userName,userRole,@vo,
    fromdate,todate,format,resourceType,
    timediff(@query_start, @proc_start),
    timediff(sysdate(), @query_start),
    @sql);
  deallocate prepare statement;
end $$

DELIMITER ;

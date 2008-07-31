DELIMITER $$

DROP PROCEDURE IF EXISTS `SiteUsageCountByVO`$$
CREATE PROCEDURE `SiteUsageCountByVO`(userName varchar(64), userRole varchar(64),
                                         fromdate varchar(64), todate varchar(64),
                                         dateGrouping varchar(64), resourceType varchar(64))
    READS SQL DATA
begin

  declare idatr varchar(64);
  declare idats varchar(64);

  if dateGrouping IS NOT NULL then
    IF STRCMP(LOWER(TRIM(dateGrouping)), 'day') = 0 then
      set idatr = '%Y-%m-%d';
      set idats = '%Y-%m-%d';
    ELSEIF STRCMP(LOWER(TRIM(dateGrouping)),'week') = 0 then
      set idatr = '%x-%v Monday';
      set idats = '%x-%v %W';
    ELSEIF STRCMP(LOWER(TRIM(dateGrouping)), 'month') = 0  then
      set idatr = '%Y-%m';
      set idats = '%Y-%m';
    ELSEIF STRCMP(LOWER(TRIM(dateGrouping)), 'year') = 0  then
      set idatr = '%Y';
      set idats = '%Y';
    ELSE
       set idatr = '%Y-%m-%d';
      set idats = '%Y-%m-%d';
    end if;
  ELSE
    set idatr = '%Y-%m-%d';
    set idats = '%Y-%m-%d';
  end if;


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
           concat_ws('', 'SELECT final_rank,
       VOProbeSummary.VOName,
       sitecntx,
       VOProbeSummary.WallDuration,
       VOProbeSummary.Cpu,
       VOProbeSummary.Njobs
FROM (SELECT @rank:=@rank+1 AS final_rank,
      vonamex,
      sitecntx
      FROM (SELECT @rank:=0 AS rank,
                   VO.VOName AS vonamex,
                   COUNT(distinct S.sitename) AS sitecntx
            FROM MasterSummaryData M, Probe P, Site S, VO, VONameCorrection Corr
            WHERE EndTime >= (''', fromdate, ' 00:00:00 '')
              AND EndTime <= (''', todate, ' 00:00:00 '')
              AND ResourceType = ''Batch''
	      AND P.siteid = S.siteid
              AND M.probename = P.probename
              AND M.VOCorrid = Corr.corrid
              AND Corr.void = VO.void
             GROUP by VO.VOName
             ORDER BY sitecntx DESC
           ) AS foox
     ) AS foo,
     (SELECT VOName,
             sum(VOProbeSummary.WallDuration) AS WallDuration,
             sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration) AS Cpu,
             sum(VOProbeSummary.Njobs) AS Njobs
      FROM VOProbeSummary, Probe, Site
      WHERE Probe.siteid = Site.siteid
        AND VOProbeSummary.probename = Probe.probename
        AND EndTime >= (''', fromdate, ' 00:00:00 '')
        AND EndTime <= (''', todate, ' 00:00:00 '')
        AND ResourceType = ''Batch''
       GROUP by VOProbeSummary.VOName
     ) AS VOProbeSummary
WHERE VOProbeSummary.VOName = foo.vonamex
ORDER BY final_rank, VOProbeSummary.VOName;'
                 );
  prepare statement from @sql;
  execute statement;
  insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,p5,p6,p7,p8,data)
    values('SiteUsageCountByVO',@key,userName,userRole,@vo,
    fromdate,todate,dateGrouping,resourceType,
    timediff(@query_start, @proc_start),
    timediff(sysdate(), @query_start),
    idatr,idats,
    @sql);
  deallocate prepare statement;
end $$

DELIMITER ;

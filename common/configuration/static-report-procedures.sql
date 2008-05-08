delimiter ||

drop procedure if exists generate_static_report
||

create procedure `generate_static_report`(userName varchar(64),
                                          userRole varchar(64),
                                          fromdate varchar(64), todate varchar(64),
                                          dateGrouping varchar(64),
                                          timeUnit varchar(64),
                                          resourceType varchar(64))
READS SQL DATA
begin
 declare iuser varchar(64);
 declare irole varchar(64);
 declare ifrom varchar(64);
 declare ito   varchar(64);
 declare iunit varchar(64);
 declare idatr varchar(64);
 declare idats varchar(64);
 declare itype varchar(64);

 if userName IS NOT NULL then
   set iuser = TRIM(userName);
 else
   set iuser = concat_ws('','GratiaUser|', UNIX_TIMESTAMP(), '|Unknown');
 end if;

 if userRole IS NOT NULL then
   set irole = TRIM(userRole);
 else
   set irole = 'GratiaUser';
 end if;

 if fromdate IS NOT NULL then
   set ifrom = TRIM(fromdate);
 else
   set ifrom = SUBDATE(CURDATE(), 800);
 end if;

 if todate IS NOT NULL then
   set ito = TRIM(todate);
 else
   set ito = CURDATE();
 end if;

 if timeUnit IS NOT NULL then
   set iunit = TRIM(timeUnit);
 else
   set iunit = '1';
 end if;


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

 if resourceType IS NOT NULL then
   set itype = TRIM(resourceType);
 else
   set itype = 'batch';
 end if;

 select generateResourceTypeClause(itype) into @myresourceclause;
 select SystemProplist.cdr into @usereportauthentication from SystemProplist where SystemProplist.car = 'use.report.authentication';
 select Role.whereclause into @mywhereclause from Role where Role.role = irole;
 select generateWhereClause(iuser,irole,@mywhereclause) into @mywhereclause;
 call parse(iuser,@name,@key,@vo);

set @sql := concat_ws('',
          ' select Site.SiteName as sitename,',
          ' sum(VOProbeSummary.WallDuration)/', iunit, ' as WallDuration,',
          ' sum(VOProbeSummary.CpuUserDuration + VOProbeSummary.CpuSystemDuration)/', iunit, ' as Cpu,',
          ' sum(VOProbeSummary.Njobs) as Njobs,',
          ' VOProbeSummary.VOName as voname,',
          ' VOProbeSummary.CommonName as UserName,',
          ' str_to_date(date_format(VOProbeSummary.EndTime,''', idatr, '''), ''', idats, ''') as  DateValue',
          ' from Site,Probe,VOProbeSummary',
          ' where Probe.siteid = Site.siteid',
          ' and VOProbeSummary.EndTIme >''', ifrom, ''' and VOProbeSummary.EndTime < ''', ito, '''',
          ' and VOProbeSummary.ProbeName = Probe.probename',
          ' ', @myresourceclause,
          ' ', @mywhereclause,
          ' group by DateValue, UserName, sitename, VOProbeSummary.VOName',
          ' order by UserName, sitename, VOProbeSummary.VOName, DateValue;'
          );

   insert into trace(pname,userkey,user,role,vo,p1,p2,p3,p4,p5,p6, data)
       values('generate_static_report',@key,iuser,irole,@vo,
       ifrom,ito,idatr,idats,dateGrouping,iunit,@sql);
   prepare statement from @sql;
   execute statement;
   deallocate prepare statement;

END
||


-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

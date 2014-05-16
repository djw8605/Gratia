#!/bin/bash
###################################################################
# John Weigand (10/03/13)
#
# This script is intended to find JobUsageRecord with a 
# excessively high CPU being recorded.  It is based on using the
# trace table entries created by the add_JUR_to_summary stored
# procedure which was modified and implemented around 10/1/13
# to create a record for each JobUsageRecord where CPU exceeded
# Wall time. 
#
# See usage for more details.
###################################################################
function logit {
  [ ! -d "$OUTPUTDIR" ] && return
  echo "$1" >>$OUTPUTDIR/$LOGFILE
}
#----------------
function logerr {
  logit "ERROR: $1";echo "ERROR: $1";exit 1
}
#----------------
function set_mysql_cmd {
  local options="$1"
  cmd="mysql $options --unbuffered --host=$HOST --port=$PORT -u $USER $PSWD $DATABASE"
}
#----------------
function run_mysql {
  if [ "$SHOWSQL" = "yes" ];then
    logit "$cmd
$(cat $SQLFILE)
"
  fi
  $cmd <$SQLFILE >$TMPFILE;rtn=$?
  if [ "$rtn" != "0" ];then
    logit  "$(cat $SQLFILE)"
    logerr "mysql error for DB($DATABASE)
$(cat $TMPFILE)
"
  fi
  cat $TMPFILE >>$OUTPUTDIR/$LOGFILE
  remove_file $SQLFILE
}
#----------------
function set_subselect {
  SUBSELECT="select
   SiteName
  ,jurm.ProbeName
  ,vo.VOName
  ,jur.ResourceType
  ,jur.CommonName
  ,jur.EndTime
  ,jur.dbid
  ,jur.Njobs
  ,jur.CpuSystemDuration
  ,jur.CpuuserDuration
  ,jur.WallDuration
  ,IFNULL(jur.Processors,1) as Processors
  ,td.Value                 as CommittedTime
  ,if(jur.WallDuration>0,round(((jur.CpuSystemDuration + jur.CpuUserDuration)/(jur.WallDuration * IFNULL(jur.Processors,1)) * 100),1),'Unknown')  as CpuEfficiency
  ,if(jur.WallDuration=td.Value,'Same',if(td.Value>0,round(((jur.CpuSystemDuration + jur.CpuUserDuration)/(td.Value * IFNULL(jur.Processors,1)) * 100),1),'Unknown')) as CommittedEfficiency
from trace               t
JOIN JobUsageRecord jur ON (
      t.p1 = jur.dbid
  and jur.ResourceType in ('Batch', 'BatchPilot', 'GridMonitor', 'RawCPU', 'Backfill')
  and jur.Njobs        > 0
  and jur.WallDuration > 0
  and jur.EndTime >= \"$START_TIME\"
  and jur.Endtime <  \"$END_TIME\"
)
JOIN JobUsageRecord_Meta jurm ON (jur.dbid = jurm.dbid)
JOIN Probe p                  ON (jurm.ProbeName = p.probename)
JOIN Site  s                  ON (p.siteid       = s.siteid)
JOIN VONameCorrection voc     ON (
     jur.ReportableVOName = voc.ReportableVOName
 and jur.VOName           = voc.VOName
)
JOIN VO vo                    ON (voc.VOid = vo.VOid)
LEFT JOIN TimeDuration td
      ON (jur.dbid = td.dbid
           and
        td.type = 'CommittedTime')
where t.eventtime >= \"$START_TIME\"
  and t.eventtime <  \"$END_TIME\"
  and procName   = 'add_JUR_to_summary'
  and t.sqlQuery like '%MasterSummaryData: CPU exceeds Wall%'
order by
   SiteName
  ,ProbeName
  ,VOName
  ,jur.ResourceType
  ,jur.CommonName
  ,Endtime
  ,jur.dbid"
}
#----------------
function find_totals {
  logit "
#
# NOTE: The purpose of these queries is to go against the trace table
# which now logs JobUsageRecords with efficiencies > 100%.
#
# Key columns in trace table 
# - only populated on main collector db and not the replica (reporting) db
#   as the trace table is no replicated):
#      procName: add_JUR_to_summary
#      p1      : dbid of JobUsageRecord
#      sqlQuery:  WARNING: MasterSummaryData: CPU exceeds Wall: Njobs 1 
#                 WallDuration 0 Cores 6 Wall_w_Cores 0 
#                 CpuUserDuration 77777777 CpuSystemDuration 0
#
#=========================================================
#  Totals for all jobs > ${THRESHOLD}% efficiency
#  for $START_TIME to $END_TIME
#=========================================================
#"
  cat >$SQLFILE <<EOF
select
  "Totals"                                as Period
 ,sum(x.Njobs)                            as Jobs
 ,round(sum(x.CpuSystemDuration)/3600,0)  as CpuSystemHrs
 ,round(sum(x.CpuuserDuration)/3600,0)    as CpuUserHrs
 ,round((sum(x.WallDuration * x.Processors))/3600,0) as WallHrs
 ,round((sum((x.CpuSystemDuration + x.CpuUserDuration))/sum(if(x.WallDuration>0,x.WallDuration,1) * x.Processors)) * 100,0) as CpuEfficiency
from (
$SUBSELECT
) x
where x.CpuEfficiency > $THRESHOLD
group by Period
EOF
  set_mysql_cmd "--table"
  run_mysql  
}

#----------------
function find_totals_all {
  logit "
#=========================================================
#  Totals for all jobs  (any efficiency - VOProbeSummary)
#  for $START_TIME to $END_TIME
#=========================================================
#"
  cat >$SQLFILE <<EOF
select
  "Totals"                              as Period
 ,sum(v.Njobs)                            as Jobs
 ,round(sum(v.CpuSystemDuration)/3600,0)  as CpuSystemHrs
 ,round(sum(v.CpuuserDuration)/3600,0)    as CpuUserHrs
 ,round((sum(v.WallDuration))/3600,0)     as WallHrs
 ,round((sum((v.CpuSystemDuration + v.CpuUserDuration))/sum(v.WallDuration)) * 100,0) as CpuEfficiency
from VOProbeSummary v
where v.EndTime >= "$START_TIME"
  and v.EndTime <  "$END_TIME"
  and v.ProbeName in ( select distinct(ProbeName) from ($SUBSELECT) x ) 
group by Period
EOF
  set_mysql_cmd "--table"
  run_mysql  
}

#--------------------------
function find_site_totals {
  logit "
#=========================================================
#  Site totals for all jobs > ${THRESHOLD}% efficiency
#  for $START_TIME to $END_TIME
#=========================================================
#"
  cat >$SQLFILE <<EOF
select
  x.SiteName                              as SiteName
 ,sum(x.Njobs)                            as Jobs
 ,round(sum(x.CpuSystemDuration)/3600,0)  as CpuSystemHrs
 ,round(sum(x.CpuuserDuration)/3600,0)    as CpuUserHrs
 ,round((sum(x.WallDuration * x.Processors))/3600,0) as WallHrs
 ,round((sum((x.CpuSystemDuration + x.CpuUserDuration))/sum(if(x.WallDuration>0,x.WallDuration,1) * x.Processors)) * 100,0) as CpuEfficiency
from (
$SUBSELECT
) x
where x.CpuEfficiency > $THRESHOLD
group by SiteName
EOF
  set_mysql_cmd "--table"
  run_mysql  
}

#--------------------------
function find_site_totals_all {
  logit "
#=========================================================
#  Site totals for all jobs  (any efficiency - VOProbeSummary)
#  for $START_TIME to $END_TIME
#=========================================================
#"
  cat >$SQLFILE <<EOF
select
  SiteName                              as SiteName
 ,sum(Njobs)                            as Jobs
 ,round(sum(CpuSystemDuration)/3600,0)  as CpuSystemHrs
 ,round(sum(CpuuserDuration)/3600,0)    as CpuUserHrs
 ,round((sum(WallDuration))/3600,0)     as WallHrs
 ,round((sum((CpuSystemDuration + CpuUserDuration))/sum(WallDuration)) * 100,0) as CpuEfficiency
from VOProbeSummary v
JOIN Probe p ON (v.ProbeName = p.probename)
JOIN Site  s ON (p.siteid = s.siteid)
where EndTime >= "$START_TIME"
  and EndTime <  "$END_TIME"
  and v.ProbeName in ( select distinct(ProbeName) from ($SUBSELECT) x ) 
group by SiteName
EOF
  set_mysql_cmd "--table"
  run_mysql  
}
#----------------
#----------------
function find_probe_totals {
  logit "
#=========================================================
#  Probes with efficiencies > ${THRESHOLD}% efficiency
#  for $START_TIME to $END_TIME
#=========================================================
#
#--------------------------------------
#-- summary by site/probe/vo/date   ---
#--------------------------------------"
  cat >$SQLFILE <<EOF
select
  x.SiteName                              as SiteName
 ,x.VOName                                as VOName
 ,x.ProbeName                             as ProbeName
 ,date_format(x.EndTime,"%Y-%m-%d")       as Period
 ,x.CommonName                            as CommonName
 ,x.ResourceType                          as ResourceType
 ,x.Processors                            as Cores
 ,sum(x.Njobs)                            as Jobs
 ,round(sum(x.CpuSystemDuration)/3600,2)  as CpuSystemHrs
 ,round(sum(x.CpuuserDuration)/3600,2)    as CpuUserHrs
 ,round((sum(x.WallDuration * x.Processors))/3600,2) as WallHrs
 ,round((sum((x.CpuSystemDuration + x.CpuUserDuration))/sum(if(x.WallDuration>0,x.WallDuration,1) * x.Processors)) * 100,0) as CpuEfficiency
from (
$SUBSELECT
) x
where x.CpuEfficiency > $THRESHOLD
group by
   SiteName
  ,VOName
  ,ProbeName
  ,Period
  ,CommonName
  ,ResourceType
  ,Cores
EOF
  set_mysql_cmd "--table"
  run_mysql  
}

#----------------
function find_jur_records {
  logit "   
#----------------------------------------------------
#  Distinct Probes with efficiencies > $THRESHOLD 
#----------------------------------------------------"
  cat >$SQLFILE <<EOF
SELECT distinct(x.ProbeName) as ProbeName
FROM (
$SUBSELECT
) x
where x.CpuEfficiency > $THRESHOLD
order by ProbeName
EOF
  set_mysql_cmd "--skip-column-names"
  run_mysql  
  PROBES="$(cat $TMPFILE |egrep -v '^#')" 
  local main_logfile=$LOGFILE
  if [ -z "$PROBES" ];then
    logit;logit "No problem probes found."
    rm -rf $OUTPUTDIR
    return
  fi
  echo "
#-------------------
#-- Probe files   --
#-------------------" >>$OUTPUTDIR/$main_logfile
  for PROBE in $PROBES
  do
    LOGFILE=$PROBE.log
    echo  "$OUTPUTDIR/$LOGFILE" >> $OUTPUTDIR/$main_logfile
    logit "
#=============================================================
#  JobUsageRecords for the Probes with efficiencies > $THRESHOLD 
#  for $START_TIME to $END_TIME
#  Probe: $PROBE
#============================================================="
  cat >$SQLFILE <<EOF
select
  x.SiteName                              as SiteName
 ,x.VOName                                as VOName
 ,x.ProbeName                             as ProbeName
 ,x.CommonName                            as CommonName
 ,date_format(x.EndTime,"%Y-%m-%d")       as Period
 ,x.ResourceType                          as ResourceType
 ,x.dbid
 ,x.Njobs                           
 ,x.CpuSystemDuration                     as CpuSystem
 ,x.CpuUserDuration                       as CpuUser
 ,x.WallDuration                          as Wall
 ,x.CommittedTime                         as Committed
 ,x.Processors                            as Cores
 ,x.CpuEfficiency                         as WallEff
 ,x.CommittedEfficiency                   as CommittedEff
from (
$SUBSELECT
) x
where (x.CpuEfficiency > $THRESHOLD
          or 
       x.CpuEfficiency = "Unknown")
  and x.ProbeName = "$PROBE"
order by 
   SiteName
  ,VOName
  ,ProbeName
  ,CommonName
  ,Period
  ,ResourceType
EOF
    set_mysql_cmd "--table"
    run_mysql 
    DBIDS="$(cat $TMPFILE | egrep -v '^#|^$' | awk -F'|' '{ if ( NR < 4 ) {next}; print $8}')"
    get_jur_details
    create_del_summary_commands
    #-- Inserting sql comment characters into probe log file --
    #-- Doing this so entire file can be read in from stdin if needed --
    awk '{ if ($1 == "call") { print $0 } else {print "--",$0}}' $OUTPUTDIR/$LOGFILE >$OUTPUTDIR/$LOGFILE.tmp
    mv $OUTPUTDIR/$LOGFILE.tmp $OUTPUTDIR/$LOGFILE 
  done
}
#---------------------------------------
function get_jur_details {
  logit "
-- ------------------------------------
-- JobUsageRecord details for possible troubleshooting.
-- JobsUsageRecords: $(echo $DBIDS | wc -w)
-- ------------------------------------"
  local inclause=""
  for dbid in $DBIDS
  do
    inclause="$inclause $dbid,"
  done
  inclause="$inclause $dbid"
  cat >$SQLFILE <<EOF
select
  GlobalJobId
 ,StartTime
 ,EndTime
 ,dbid
 ,LocalJobId
 ,LocalUserId
 ,Status
 ,SubmitHost
 ,Host
from JobUsageRecord
where dbid in ($inclause)
order by GlobalJobId,EndTime
EOF
  set_mysql_cmd "--table"
  run_mysql 
}
#---------------------------------------
function create_del_summary_commands {
  logit "
-- ------------------------------------
-- Probe: $PROBE
-- JobsUsageRecords: $(echo $DBIDS | wc -w)
-- ------------------------------------"
  logit  "$probehdr" 
  for dbid in $DBIDS
  do
    logit "call del_JUR_from_summary($dbid);" 
  done
}
#----------------
function validate_environ {
  #-- verify mysql client is available
  if [ "`type mysql &>/dev/null;echo $?`" != "0" ];then
    logerr "Cannot find the mysql client."
  fi
  #-- verify output directory exists --
  make_dir $OUTPUTDIR
}
#----------------
function remove_file {
  local file="$1"
  if [ -e "$file" ];then
    rm -f $file
  fi
}
#----------------
function make_dir {
  local dir=$1
  [ -z "$dir" ] && logerr " in make_dir.  no arg"
  if [ ! -d "$dir" ];then
    mkdir $dir
  fi
}
#----------------
function validate_args {
  local arg="$1"
  while [ $# -gt 0 ]
  do
    case $1 in
      "--db"       | "-db"        ) DATABASE=$2;   shift   ;;
      "--start"    | "-start"     ) START_TIME=$2; shift   ;;
      "--end"      | "-end"       ) END_TIME=$2;   shift   ;;
      "--threshold"| "-threshold" ) THRESHOLD=$2;  shift   ;;
      "--showsql"  | "-showsql"   ) SHOWSQL="yes"          ;;
      "--help"     | "-help"  | "--h" | "-h" ) usage;exit 1 ;;
      * ) usage;echo  "ERROR: Invalid command line argument" ; exit 1 ;;
    esac
    shift
  done
  if [ "$(date -d $START_TIME &>/dev/null;echo $?)" != "0" ];then
      echo "ERROR: Invalid --start date. Format is YYYY-MM-DD";exit 1
  fi
  if [ "$(date -d $END_TIME &>/dev/null;echo $?)" != "0" ];then
      echo "ERROR: Invalid --end date. Format is YYYY-MM-DD";exit 1
  fi
  set_db_parameters
}
#----------------
function set_db_parameters {
  USER=reader
  PSWD="-preader"
  case $DATABASE in
    "gratia"    ) 
                  HOST=gr-osg-mysql-collector.opensciencegrid.org
                  HOST=fcl411.fnal.gov
                  PORT=3306
                  ;;
    "gratia_osg_daily"    ) 
                  HOST=gr-osg-mysql-collector.opensciencegrid.org
                  PORT=3306
                  ;;
     *          ) echo"ERROR: Invalid data base: $DATABASE. 
Script supports gratia, gratia_osg_daily"
                  exit 1
                  ::
  esac
}
#----------------
function clean_all_directories {
    make_dir $OUTPUTDIR
    SQLFILE=$OUTPUTDIR/$SQLFILE
    TMPFILE=$OUTPUTDIR/$TMPFILE
    remove_all_files $OUTPUTDIR
}
#----------------
function remove_all_files {
  local dir=$1
  files="$(ls $dir)"
  for file in $files
  do
    remove_file $dir/$file
  done
}

#----------------
function usage {
  echo "\
Usage: $PGM [--db dbname] [--start YYYY-MM-DD] [--end YYYY-MM-DD] 
            [--threshold PERCENT] [--showsql]

Optiions:
 --db   Allows you to restrict the analysis to a specific database.
        These are supported: gratia, gratia_osg_daily DEFAULT: $DATABASE

--start This restricts the query to a certain time period.  
--end   DEFAULT: start- $START_TIME  end- $END_TIME

--threshold  Restricts the jobs select to only those that exceed a specified 
             percent.  DEFAULT: $THRESHOLD

--showsql Displays the sql statements in the various log files.
          By default, it will not display them

In October 2013, the add_JUR_to_summary stored procedure was modified to 
insert a record in the trace table for any jobs where CPU exceed Wall times.
The trace table is an internal table that functions pretty much like an 
error/warning log internally in the Gratia database.

NOTE: This query HAS to go against the main Gratia database and NOT the replica
      since the trace table is not replicated.

This script queries the trace table for the time period specified searching
for entries with a 
- procName = 'add_JUR_to_summary' 
- and a sqlQuery like '%MasterSummaryData: CPU exceeds Wall%'

For those probes, it then searches the JobUsageRecord table for the
specific records exceeding that threshold.  It will then create two
files with sql commands to zero out those records and adjust the summary
tables accordingly.  A log file is also created.

./<DATABASE>-YYYMMDD/analysis.log
   Shows all the queries and results used.  It is best to review this
   before applying any adjustments.

./<DATABASE>-YYYMMDD/<ProbeName>.log
   Contains the following sql procedure call for each dbid
      call del_JUR_from_summary(DBID);
   This called procedure will remove the current values for that specific
   JobUsageRecord from the summary tables and set all CPU, Wall and Njobs
   values to zero to avoid accidently removing the values twice.

NOTE: If no records are found, the program will remove the <DATABASE>-YYMMDD
      directory and files.

VERY IMPORTANT:  The del_JUR_from_summary statements MUST be applied against
                 the PRIMARY Gratia database and NOT the replica.
"
}
#### MAIN ###############################################
PGM=`basename $0`
DIR=`dirname $0`
TMPFILE=tmpfile
SQLFILE=query.sql
LOGFILE=analysis.log

#-- default command line arg values --
START_TIME="$(date +'%Y-%m')-01"
END_TIME="2020-01-01"
THRESHOLD=500
DATABASE=gratia 
SHOWSQL="no"

validate_args $*
OUTPUTDIR=$DIR/$DATABASE-$(echo $START_TIME |sed 's/-//g')-$(echo $END_TIME |sed 's/-//g')
make_dir $OUTPUTDIR
clean_all_directories
logit "
#================================
# PGM: $PGM
# Database: $DATABASE
# for $START_TIME to $END_TIME
#================================"
validate_environ
set_subselect
find_totals 
find_totals_all
find_site_totals
find_site_totals_all
find_probe_totals 
find_jur_records 

LOGFILE=analysis.log
remove_file $TMPFILE
logit "
#================================
# PGM: $PGM
# Database: $DATABASE
# for $START_TIME to $END_TIME
# DONE
#================================"
exit 0


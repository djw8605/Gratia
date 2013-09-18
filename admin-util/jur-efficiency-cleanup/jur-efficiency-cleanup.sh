#!/bin/bash
###################################################################
# John Weigand (11//18/13)
#
# This script is intended to find JobUsageRecord with a 
# ridiculously high CPU being recorded.  It is crude.
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
  logit "$cmd
$(cat $SQLFILE)
"
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
function find_probes {
  logit "
#
#=========================================================
#  Probes with ridiculous efficiency 
#  Start: $(date)
#=========================================================
#
# NOTE: The purpose of these queries (going against the summary tables) is
#        is to narrow down the queries against the JobUsageRecord table.
#        It gives a ballpark estimation of the probes causing the problem.
#        The number of Jobs shown is NOT an indicationn of the actual
#        number of jobs with ridiculous CPU.
#
"
  whereClause="WHERE
       EndTime >= \"$START_TIME\"
   and EndTime <  \"$END_TIME\"
   and ((CpuSystemDuration + CpuUserDuration)/WallDuration) * 100 > $THRESHOLD
"
  logit "
#----------------------------
#-- summary by probe/vo   ---
#----------------------------"
  cat >$SQLFILE <<EOF
SELECT  x.ProbeName
        ,SiteName
        ,VOName
        ,Jobs
        ,CPU_Hrs
        ,Wall_hrs
        ,round((CPU_Hrs/Wall_Hrs)*100,0) as Efficiency
FROM 
(
SELECT ProbeName 
     ,VOName
     ,sum(NJobs) as Jobs
     ,round(sum((CpuSystemDuration + CpuUserDuration))/3600,0) as CPU_Hrs
     ,round(sum(WallDuration)/3600,0) as Wall_Hrs
FROM VOProbeSummary
$whereClause
group by ProbeName
        ,VOName
) x
   ,Probe p
   ,Site s
where x.ProbeName = p.probename
and p.siteid    = s.siteid
EOF
  set_mysql_cmd "--table"
  run_mysql  

  logit "
#-------------------------------
#-- summary by probe/vo/date ---
#-------------------------------"
  cat >$SQLFILE <<EOF
SELECT   ProbeName
        ,VOName
        ,EndTime
        ,Jobs
        ,CPU_Hrs
        ,Wall_hrs
        ,round((CPU_Hrs/Wall_Hrs)*100,0) as Efficiency
FROM 
(
SELECT
      ProbeName
     ,VOName
     ,date_format(EndTime,'%Y-%m-%d') as EndTime
     ,sum(NJobs) as Jobs
     ,round(sum((CpuSystemDuration + CpuUserDuration))/3600,0) as CPU_Hrs
     ,round(sum(WallDuration)/3600,0) as Wall_Hrs
   FROM
      VOProbeSummary
  $whereClause
   group by
     ProbeName
    ,VOName
    ,EndTime
) x
;
EOF
  set_mysql_cmd "--table"
  run_mysql  
  logit "   
#========================================================
#  Distinct Probes with ridiculous efficiency 
#  Start: $(date)
#=========================================================
"
  cat >$SQLFILE <<EOF
SELECT distinct(ProbeName) 
FROM VOProbeSummary
$whereClause
EOF
  set_mysql_cmd "--skip-column-names"
  run_mysql  
  PROBES="$(cat $TMPFILE |egrep -v '^#')" 
}
#----------------
function find_jur_records {
  local main_logfile=$LOGFILE
  if [ -z "$PROBES" ];then
    logit;logit "No problem probes found."
    rm -rf $OUTPUTDIR
    return
  fi
  for PROBE in $PROBES
  do
    LOGFILE=$PROBE.log
    echo  "... $PROBE ($LOGFILE) - start $(date)" >> $OUTPUTDIR/$main_logfile

    whereClause="WHERE
    meta.ProbeName = \"$PROBE\"
-- AND meta.ServerDate >= \"$START_TIME\"  ## May miss some as Server is usually > End
-- AND meta.ServerDate <  \"$END_TIME\"
AND meta.dbid = jur.dbid
AND jur.EndTime >= \"$START_TIME\"
AND jur.EndTime < \"$END_TIME\"
AND (CpuSystemDuration + CpuUserDuration)/(WallDuration * IFNULL(Processors,1)) * 100 > $THRESHOLD
"
    start_time=$(date)
    logit "
#=============================================================
#  JobUsageRecords for the Probes with ridiculous efficiency 
#  Start: $start_time
#  Probe: $PROBE
#=============================================================
"
  cat >$SQLFILE <<EOF
   SELECT
      jur.dbid as dbid
     ,CpuSystemDuration 
     ,CpuUserDuration
     ,WallDuration
     ,Processors
     ,(WallDuration * IFNULL(Processors,1)) as Wall_w_Cores
     ,round((CpuSystemDuration + CpuUserDuration)/(WallDuration * IFNULL(Processors,1)) * 100,0) as Efficiency
   FROM
      JobUsageRecord_Meta meta
     ,JobUsageRecord jur
   $whereClause
   order by
     dbid
;
EOF
    set_mysql_cmd "--table"
    run_mysql 
    DBIDS="$(cat $TMPFILE | egrep -v '^#|^$' | awk -F'|' '{ if ( NR < 4 ) {next}; print $2}')"
    create_sqlcmds
    echo  "... $PROBE ($LOGFILE) -   end $(date)" >> $OUTPUTDIR/$main_logfile
  done
  logit "
#=============================================================
#  JobUsageRecords for the Probes with ridiculous efficiency 
#  Start: $start_time
#    End: $(date)
#  Probe: $PROBE
#=============================================================
"
}
#---------------------------------------
function create_sqlcmds {
  local probehdr="
-- ------------------------------------
-- Probe: $PROBE
-- ------------------------------------"
  logit  "$probehdr" 
  for dbid in $DBIDS
  do
    logit "call del_JUR_from_summary($dbid);" 
    logit "update JobUsageRecord set WallDuration=0, CpuUserDuration=0, CpuSystemDuration=0 where dbid = $dbid;" 
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
      "--db"    | "-db"    ) DATABASE=$2;   shift   ;;
      "--start" | "-start" ) START_TIME=$2; shift   ;;
      "--end"   | "-end"   ) END_TIME=$2;   shift   ;;
      "--help"  | "-help"  | "--h" | "-h" ) usage;exit 1 ;;
      * ) echo  "ERROR: Invalid command line argument" ; usage; exit 1 ;;
    esac
    shift
  done
  if [ "$(date -d $START_TIME &>/dev/null;echo $?)" != "0" ];then
      logerr "Invalid --start date. Format is YYYY-MM-DD"
  fi
  if [ "$(date -d $END_TIME &>/dev/null;echo $?)" != "0" ];then
      logerr "Invalid --end date. Format is YYYY-MM-DD"
  fi
  set_db_parameters
}
#----------------
function set_db_parameters {
  USER=reader
  PSWD="-preader"
  case $DATABASE in
    "gratia"    ) 
                  HOST=gr-osg-mysql-reports.opensciencegrid.org
                  PORT=3306
                  ;;
    "gratia_osg_daily"    ) 
                  HOST=gr-osg-mysql-reports.opensciencegrid.org
                  PORT=3306
                  ;;
     *          ) logerr "Invalid data base: $DATABASE. 
Script supports gratia, gratia_osg_daily"
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
Usage: $PGM [--db dbname] [--start YYYY-MM-DD --end YYYY-MM-DD]

Optiions:
 --db   
     Allows you to restrict the analysis to a specific database.
     These are supported: gratia, gratia_osg_daily
     DEFAULT: gratia

--start YYYY-MM-DD --end   YYYY-MM-DD
     This restricts you to a certain time period.  This is IMPORTANT.
     Make the interval no more than one month.  You can but you may wait 
     forever for the script to end.
     DEFAULT: start- $START_TIME  end- $END_TIME

This scripts queries the VOProbeSummary table with the time period specified
for probes where the CPU time exceeds Wall time by more than ${THRESHOLD}%.

For those probes, it then searches the JobUsageRecord table for the
specific records exceeding that threshold.  It will then create two
files with sql commands to zero out those records and adjust the summary
tables accordingly.  A log file is also created.

./DATABASE/analysis.log
   Shows all the queries and results used.  It is best to review this
   before applying the adjustments.

   Contains the following procedure call for each dbid
      call del_JUR_from_summary(DBID);
   This called procedure will remove the current values for that specific
   JobUsageRecord from the summary tables.

   Contains the sql update statements for each dbid to zeroe out the 
   user/system cpu time and the wall time on those specific records.  
   This is done to insure that one does not accidently run the 
   del_JUR_from_summary procedure calls more than once for a dbid thus really 
   screwing up the summaries.
"
}
#### MAIN ###############################################
PGM=`basename $0`
DIR=`dirname $0`
TMPFILE=tmpfile
SQLFILE=query.sql
LOGFILE=analysis.log

START_TIME="$(date +'%Y-%m')-01"
END_TIME="2020-01-01"
THRESHOLD=2500

DATABASE=gratia 

OUTPUTDIR=$DIR/$DATABASE-$(date +'%Y%m%d')
make_dir $OUTPUTDIR
STARTING_LOG_TIME="$(date)"
logit "
#================================
# Database: $DATABASE
# Start: $(date)
#================================
"
validate_args $*
clean_all_directories
validate_environ
find_probes 
find_jur_records 

LOGFILE=analysis.log
remove_file $TMPFILE
logit "
#================================
# Database: $DATABASE
# Started: $STARTING_LOG_TIME
#     End: $(date)
#================================
"
exit 0


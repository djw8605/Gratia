#!/bin/bash
###################################################################
# John Weigand (12/30/11)
#
# This script should be used to analyse VO names that can be
# removed from the Gratia database summary tables and VO tables.
###################################################################
function logit {
  echo "$1"
}
#----------------
function logdebug {
  if [ $VERBOSE -eq 1 ];then
    logit "$1"
  fi
}
#----------------
function logerr {
  logit "ERROR: $1";exit 1
}
#----------------
function set_mysql_cmd {
  local options="$1"
  cmd="mysql $options --unbuffered --host=$HOST --port=$PORT -u $USER $PSWD $DB"
}
#----------------
function run_mysql {
  local sqlfile=$1
  local outfile=$2
  local tmpfile=$outputdir/tmpfile.$table
  logdebug "sql cmd: $cmd"
  $cmd <$sqlfile >$tmpfile;rtn=$?
  if [ "$rtn" != "0" ];then
    logit  "$(cat sqlfile)"
    logerr "mysql error for DB($DB) Table($table)
$(cat $tmpfile)
"
  fi
  mv $tmpfile $outfile
  remove_file $sqlfile
}
#----------------
function analyze_table {
  logdebug "... in function analyze_table"
  local outfile=$outputdir/$table.out
  local sqlfile=$outputdir/sqlfile.$table
  local table_sql=$sqlDir/$table.sql
  set_mysql_cmd "--table --verbose"
  ( echo "`date`"; echo ""; echo $cmd ) >$outfile
cat >$sqlfile <<EOF;rtn=$?
\! echo
\! echo =========================================================
\! echo   VOName being analized
\! echo =========================================================
set @voname="${voname}";
`cat $table_sql`
EOF
  run_mysql $sqlfile $outfile
}
#----------------
function create_delete_file {
  logdebug "... in function create_delete_file"
  local sqlfile=$outputdir/sqlfile_$table
  local outfile=$outputdir/delete_${table}.out
  local table_delete=$sqlDir/${table}_delete.sql
  if [ ! -e "$table_delete" ];then
    logdebug "... removing $outfile" 
    logdebug "... no $table_delete exist" 
    remove_file  $outfile
    return  # no delete sql file set up
  fi
  set_mysql_cmd "--silent --skip-column-names"
cat  >$sqlfile <<EOF
set @voname="${voname}";
`cat $table_delete`
EOF
  run_mysql $sqlfile $outfile
  if [ ! -s $outfile ];then
    remove_file $outfile
  fi
}
#----------------
function check_for_empty_table {
  logdebug "... in function check_for_empty_table"
  local outfile=$outputdir/empty_$table
  local sqlfile=$outputdir/sqlfile.$table
  set_mysql_cmd "--silent --skip-column-names"
cat  >$sqlfile <<EOF
select * from $table limit 1
EOF
  run_mysql $sqlfile $outfile
  #-- check if empty --
  if [ ! -s "$outfile" ];then   
    logdebug "... EMPTY"
    EMPTY=1
    > $outfile
  else 
    logdebug "... HAS DATA"
    EMPTY=0
    remove_file  $outfile
    if [ "$table" = "NodeSummary" ];then
      logerr "WARNING: This code does not take into account the NodeSummary being
populated.  Analysis should be performed before proceeding with anything."
    fi
  fi 
}
#----------------
function check_for_vo_usage {
  #  If the VO name we want to analyse is not used in this database,
  #  we just want to skip everything.
  logdebug "... in function check_for_vo_usage"
  local outfile=$outputdir/vo_usage
  local sqlfile=$outputdir/sqlfile.vo_usage
  set_mysql_cmd "--silent --skip-column-names"
cat  >$sqlfile <<EOF
set @voname="${voname}";
select * from VO where VOName like @voname limit 1
EOF
  run_mysql $sqlfile $outfile
  logdebug "$(ls -l $outfile)"
  logdebug "$(cat $outfile)"
  #-- check if empty --
  if [ -s "$outfile" ];then   
    logdebug "... FOUND ONE"
    NOT_FOUND=0
    echo "VO Name: $voname" > $outputdir/HAS-VONAME
  else 
    logdebug "... NOT FOUND"
    NOT_FOUND=1
    echo "VO Name: $voname" > $outputdir/DOES-NOT-HAVE-VONAME
  fi 
  remove_file $outfile
  

}
#----------------
function remove_all_files {
  local dir=$1
  logdebug "... in function remove_all_files: $dir"
  files="$(ls $dir)"
  for file in $files
  do
    remove_file $dir/$file
  done
}
#----------------
function validate_environ {
  #-- verify mysql client is available
  if [ "`type mysql &>/dev/null;echo $?`" != "0" ];then
    logerr "Cannot find the mysql client."
  fi
  #-- verify output directory exists --
  make_dir $outputDir
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
      "--vo" | "-vo"             ) voname=$2;  shift   ;;
      "--db" | "-db"             ) dbs=$2;     shift   ;;
      "--verbose" | "-verbose"   ) VERBOSE=1           ;;
      "--voc-only" | "-voc-only" ) VOC_ONLY=1;         ;;
      "--help" | "-help" | "--h" | "-h" ) usage;exit 1 ;;
      * ) echo  "ERROR: Invalid command line argument" ; usage; exit 1 ;;
    esac
    shift
  done
  if [ $VOC_ONLY -eq 1 ];then
    if [ ! -z "$voname" ];then
      usage;logerr "--vo option is should not be used with --voc-only" 
    fi
  else
    if [ -z "$voname" ];then
      usage;logerr "--vo voname option is required" 
    fi
  fi
}
#----------------
function validate_dbs {
  for db in $dbs
  do
    set_db_parameters $db  # just validating here
  done  
}
#----------------
function set_db_parameters {
  local db="$1"
  USER=reader
  PSWD="-preader"
  case $db in
    "gratia"    ) DB=gratia
                  HOST=gr-osg-mysql-reports.opensciencegrid.org
                  PORT=3306
                  ;;
    "gratia_osg_daily"    ) 
                  DB=gratia_osg_daily
                  HOST=gr-osg-mysql-reports.opensciencegrid.org
                  PORT=3306
                  ;;
    "gratia_osg_transfer"    ) 
                  DB=gratia_osg_transfer
                  HOST=gr-osg-mysql-reports.opensciencegrid.org
                  PORT=3306
                  ;;
     *          ) logerr "Invalid data base: $db" ::
  esac
}
#----------------
function clean_all_directories {
  logdebug "====================================="
  logdebug "--- clean up all directories --"
  for db in $dbs
  do
    outputdir="$outputDir/$db"
    remove_all_files $outputdir
    make_dir $outputdir
  done
}
#----------------
function perform_voname_analysis {
  logdebug "================================================"
  logdebug "--- Perform the queries for each db and table --"
  for db in $dbs
  do
    outputdir="$outputDir/$db"
    logdebug "=========================="
    logdebug "Database: $db" 
    set_db_parameters $db
    check_for_vo_usage
    if [ $NOT_FOUND -eq 1 ];then
      continue
    fi
    for table in $tables
    do
      logdebug "-------------------"
      logdebug "Table: $table"
      check_for_empty_table
      if [ $EMPTY -eq 1 ];then
        continue
      fi
      analyze_table
      create_delete_file
    done
  done
}
#-----------------
function check_for_unused_corrids {
  logdebug "==========================================================="
  logdebug "-- check the VONameCorrection table for unused entries ---"
  for db in $dbs
  do
    outputdir="$outputDir/$db"
    logdebug "------------------"
    logdebug "Database: $db" 
    table=VONameCorrection
    outputdir="$outputDir/$db"
    make_dir $outputdir
    set_db_parameters $db
    analyze_table
    create_delete_file
  done
}

#----------------
function usage {
  echo "\
Usage: $PGM --vo voname [--db dbname] [--verbose]
       $PGM --voc-only  [--db dbname] [--verbose]

Modes:
--vo 
     This mode is intended to allow for the removal of summary table
     records for VOs that were mistakenly added to a Gratia instance.
     In this mode, a partial or complete VO name will be searched for in the 
     summary tables.  The 'voname' string can contain a wild card which in 
     MySQL is '%'.  The query uses a 'like' statement.
        e.g., --vo TG-%

--voc-only
     This mode is intended to just identify VONameCorrection table entries
     that are not used and can be deleted.
     In this mode, it will bypass the summary tables and only search for 
     VONameCorrection table entries that are not used in any summary tables.
     Some of these may not be able to be deleted if the JobUsageRecord
     still exists (foreign key constraint) but can be once the JUR record
     is archived.

The summary tables that are searched are:
VONameCorrection
${tables}
The databases searched are:
${dbs}
Other optiions:
 --db   
     Allows you to restrict the analysis to a specific database.
 --verbose 
     Generated statements showing the progress. Some queries could take forever.
            
Output from the script will be created in a ${outputDir}/[DB_NAME] directory:
  HAS-VONAME / DOES-NOT-HAVE-VONAME - visual indicator if analysis required
  [TABLE_NAME].out        - shows analysis performed for the --vo specified
  empty_[TABLE_NAME]      - indicates the table had zero records
  delete_[TABLE_NAME].out - contains DML DELETE statements for table
"
}
#### MAIN ###############################################
PGM=`basename $0`
outputDir="./data"
sqlDir="./vo-cleanup-sql"
VERBOSE=0             # debug mode
VOC_ONLY=0            # do only the VONameCorrection table

dbs="\
gratia 
gratia_osg_daily 
gratia_osg_transfer
"
tables="\
NodeSummary
MasterServiceSummary
MasterServiceSummaryHourly
MasterSummaryData
MasterTransferSummary
"

validate_args $*
validate_dbs   
validate_environ
clean_all_directories
if [ $VOC_ONLY -eq 0 ];then
  perform_voname_analysis
fi
check_for_unused_corrids 

exit 0


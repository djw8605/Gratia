#!/bin/bash
###################################################################
# John Weigand (12/30/11)
#
# This script should be used to analyse Site names that can be
# removed from the Gratia Site table.
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
  logdebug "... in function run_mysql"
  local sqlfile=$1
  local outfile=$2
  local tmpfile=$outputdir/tmpfile.$table
  logdebug "sqlfile: $sqlfile"
  logdebug "tmpfile: $tmpfile"
  logdebug "sql cmd: $cmd"
  $cmd <$sqlfile >$tmpfile;rtn=$?
  if [ "$rtn" != "0" ];then
    logit  "$(cat sqlfile)"
    logerr "mysql error for DB($DB) Table($table)
$(cat $tmpfile)
"
  fi
  cat $tmpfile >>$outfile
  remove_file $tmpfile
  remove_file $sqlfile
}
#----------------
function analyze_table {
  local table="$1"
  local mydate="$2"
  logdebug "... in function analyze_table"
  if [ -z "$table" ];then
    logerr "Programming error. Function analyze_table requires an argument"
  fi
  local outfile=$outputdir/$table.out
  local sqlfile=$outputdir/sqlfile.$table
  local table_sql=$sqlDir/$table.sql
  set_mysql_cmd "--table --verbose"
  ( echo "`date`"; echo ""; echo $cmd ) >$outfile
cat >$sqlfile <<EOF;rtn=$?
\! echo
\! echo =========================================================
\! echo   $table tables being analized
\! echo =========================================================
`cat $table_sql`
EOF
  run_mysql $sqlfile $outfile
}

#----------------
function create_delete_file {
  local table="$1"
  logdebug "... in function create_delete_file"
  if [ -z "$table" ];then
    logerr "Programming error. Function create_delete_file requires an argument"
  fi
  local sqlfile=$outputdir/sqlfile.$table
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
      "--db" | "-db"             ) dbs=$2;     shift   ;;
      "--verbose" | "-verbose"   ) VERBOSE=1           ;;
      "--help" | "-help" | "--h" | "-h" ) usage;exit 1 ;;
      * ) echo  "ERROR: Invalid command line argument" ; usage; exit 1 ;;
    esac
    shift
  done
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
#-----------------
function check_for_unused_sites {
  logdebug "#####################################################"
  logdebug "#### check_for_unused_sites #######################"
  logdebug "#####################################################"
  for db in $dbs
  do
    logdebug "======================="
    logdebug "== Database: $db" 
    logdebug "======================="
    set_db_parameters $db
    outputdir="$outputDir/$db"
    make_dir $outputdir
    for table in $tables 
    do
      logdebug "------------------"
      logdebug "Table: $table"
      analyze_table $table
      create_delete_file $table
    done
  done
}

#----------------
function usage {
  echo "\
Usage: $PGM [--db dbname] [--verbose]

The databases searched are:
${dbs}

Options:
 --db   
     Allows you to restrict the analysis to a specific database.
 --verbose 
     Generated statements showing the progress. Some queries could take forever.
            
Output from the script will be created in a ${outputDir}/[DB_NAME] directory:
  [TABLE_NAME].out        - shows analysis performed for unused sites
  empty_[TABLE_NAME]      - indicates the table had zero records
  delete_[TABLE_NAME].out - contains DML DELETE statements for table
"
}
#### MAIN ###############################################
PGM=`basename $0`
outputDir="./data"
sqlDir="./site-cleanup-sql"
VERBOSE=0        # debug mode

dbs="\
gratia 
gratia_osg_daily 
gratia_osg_transfer
"
tables="\
Site
"

validate_args $*
validate_dbs   
validate_environ
clean_all_directories
check_for_unused_sites 

exit 0


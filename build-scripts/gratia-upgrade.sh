#!/bin/bash
########################################################
# John Weigand (6/26/08)
#
# For gratia upgrades anywhere
########################################################
function ask_continue {
  echo 
  echo -n "Do you want to proceed? (y/n): "
  read ans
  if [ "$ans" != "y" ];then
    echo;echo "... bye";exit 1
  fi
}
#------------------------------------
function logit {
  logdir=$tomcat_dir/${tomcat}-upgrade.log
  logfile=$logdir/$DATE.log
  if [ ! -d "$logdir" ];then
    mkdir -p $logdir
  fi
  case $daily in
    "yes" ) echo "$1" >>$logfile 2>&1 ;;
       *  ) echo "$1" 2>&1 | tee -a $logfile ;;
  esac
}
#------------------------------------
function logerr {
  logit;logit "ERROR- $1";logit
  send_mail "FAILED" "$1"
  log_upgrade_end 
  exit 1
}
#------------------------------------
function usage_error {
  usage;echo;echo "ERROR- $1";echo
  exit 1
}
#------------------------------------
function runit {
  cmd="$1"
  logit "...RUNNING: $cmd"
  case $daily in
   "yes" ) $cmd >>$logfile 2>&1;rtn=$?
           if [ "$rtn" != "0" ];then
             logerr "Command failed: $cmd"
           fi
          ;;
       * ) ( $cmd;rtn=$?
            if [ "$rtn" != "0" ];then
              logerr "Command failed: $cmd"
              exit 1  # because it is in a child shell
            fi ) 2>&1 | tee -a $logfile
           ;;
  esac
}
#----------------------
function try_again {
  if [ "$prompt" = "no" ];then
    logerr "$1"
  fi
  logit "$1";logit "... try again!";sleep 2;continue
}
#------------------------------------
function delimit {
  logit;
  logit "#-------------------------------" 
  logit "# $1"
  logit "#-------------------------------" 
}
#-----------------------------------
function usage {
echo "
Usage: $PGM --help
This script is intended to simplify the upgrading of a gratia instance
in development, integration and production.  

Modes for running the script:
1. Question and answer mode (no command line arguments)
     $PGM 
   This is the recommended mode for performing upgrades.

2. Expert user mode.  
     $PGM --instance TOMCAT_INSTANCE 
          --source SOURCE_DIR 
          --pswd ROOT_PSWD
          [--force-log4j]
   For those that disdain questions and answers. This mode eliminates some
   questions.  There will be prompts for starting the service and a few 
   tests.  Available, but not a recommended mode of upgrading.

3. Cron mode.  Used for daily upgrades of the test environment.
   There are no prompts (all arguments are required).
   Refer to the paragraph below as to the prompts for the value of the
   individual arguments.
     $PGM --instance TOMCAT_INSTANCE 
          --source SOURCE_DIR 
          --pswd ROOT_PSWD
          --daily  EMAIL_ADDRESS(ES) 
          --mysql MYSQL_FILE
          --force-log4j

Although it does not really do much more than is already available to do, its
intent is to take some of the guesswork (memory-like) out of the upgrade
process and it does some validation.

A major assumption it makes is that all tomcat collectors are in $tomcat_dir.
If this is not true, you will have to do it the old fashion way.

 No prompt mode   Prompt mode questions
 --------------   --------------------- 
 --instance       tomcat instance (e.g, tomcat-itb_gratia_itb)
 --source         source directory
                    - daily builds..... /home/gratia/gratia-builds
                    - release builds... /home/gratia/gratia-releases
                    - other............ specified by you
 --pswd           mysql root password
 --daily          do you want to start the tomcat/collector service
 --mysql          DB MySql file containing the password for upgrades 
                  (required when running from cron and when used the 
                  --pswd argument is ignored)
 --force-log4j    Force overwrite of log4j config file.

If '--daily' is used, then all arguments are required and use of this argument
will automatically start the tomcat/collector and requires specifying the
email address(es).  It is intended for use ONLY when running from cron for 
the purpose of daily test installations.

The '--force-log4j' argument is used to force the over-writing of the
log4j.properties file used for logging.  If these properties have been 
modified locally and you desire to preserve these changes, then this option
should not be used and any changes will have to be performed manually.

The script performs the following:
 1. shutdown your tomcat instance/collector
 2. run update-gratia-local with the appropriate arguments
 3. clean your log directory saving the old logs in 
      $tomcat_dir/gratia_tomcat_logs_backups
 4. optionally, allow you to start your collector
    (when the --daily argument is specified, it will start the collector)
 5. optionally, overwrite your log4j.properties file

Log files are maintained for every execution of this script in this directory:
  /data/TOMCAT_INSTANCE-upgrades.log 
as YYYY-MM-DD.log.  These files are appended to and this script perform a 
housekeeping retaining only the last 10 log files.

The latest release data will be shown in:
  /data/TOMCAT_INSTANCE.gratia-release

You must be root user to execute this script.
"
}
#--------------------------------
function initial_dialog {
  echo "
You can terminate this script at any time using <CNTL-C> EXCEPT after
you give the FINAL approval to perform the update.

This script will allow you to upgrade one of these Gratia collectors in 
the $tomcat_dir directory on $tomcat_host:"
  cnt=0
  for dir in $(ls -d $tomcat_dir/tomcat-* |egrep -v "log|gratia-release|grep")
  do 
    ls -ld $dir |egrep -v 'log|grep'
    if [ -w "$dir" ];then
      collectors="$collectors $(basename $dir)"
      cnt=1
    fi
  done
  if [ $cnt -eq 0 ];then
    echo "You lack permission to update any Gratia collector on this node.
... bye":exit 1
  fi
  ask_continue
}
#--------------------------------
function choose_collector {
  echo "----- choose_collector ------"
  while :
  do
    echo -n "
These are the collectors on this node ($tomcat_host) you are permitted to upgrade:
$(for a in $collectors;do echo "   $a";done)

Choose a collector (none to exit): " 
    read tomcat 
    if [ -z "$tomcat" ];then
      exit 0
    fi
    validate_collector
    break
  done
}
#------------------------
function validate_collector {
  if [ ! -d "$tomcat_dir/$tomcat" ];then
    try_again "The tomcat directory ($tomcat_dir/$tomcat) does not exist..something wrong?"
  fi
}
#------------------------
function validate_source {
  if [ -d "$source" ] || [ -L "$source" ];then
    break 
  fi
  try_again "... the source directory ($source) does not exist."
}
#-------------------------------
function choose_source_directory {
  echo "----- choose_source_directory ------"
  while :
  do
    echo -n "Official releases or nightly builds (releases/builds/other)? [default - $source_type]: "
    read ans
    if [ -n "$ans" ];then
      source_type=$ans
    fi
    case $source_type in 
      "releases" ) find_release_source ; break ;;
      "builds"   ) find_build_source   ; break ;;
      "other"    ) find_other_source   ; break ;;
      *          ) try_again "... WRONG!!"
    esac
  done
}
#--------------------------------
function find_release_source {
  echo "----- find_release_source ------"
  release_dir=/home/gratia/gratia-releases 
  while :
  do
    echo -n "Which release?
$(ls -d $release_dir/gratia-v*)
... choose the version (e.g. v0.34.9a) [default - $release]: "
    read ans
    if [ -n "$ans" ];then
      release=$ans
    fi
    source=$release_dir/gratia-$release
    validate_source
  done
}
#--------------------------------
function find_build_source {
  echo "----- find_build_source ------"
  release_dir=/home/gratia/gratia-builds 
  while :
  do
    echo -n "Which date?
$(ls -d $release_dir/gratia-* |egrep -v "\.log")
... choose the date (eg, 2008-07-14) or 'latest' [default - $release]: "
    read ans
    if [ -n "$ans" ];then
      release=$ans
    fi
    source=$release_dir/gratia-$release
    validate_source
  done
}
#--------------------------------
function find_other_source {
  echo "----- find_other_source ------"
  while :
  do
    echo -n "Specify directory? [default - $release]: "
    read ans
    if [ -n "$ans" ];then
      release=$ans
    fi
    source=$release
    validate_source
  done
}
#--------------------------------
function choose_db_root_password {
  echo "----- choose_db_root_password ------"
  stty -echo   # turns off echo of keyboard
  read -p  "Enter the root MySql password [default - $pswd]: " ans
  stty echo    # turns echo back on
  if [ -n "$ans" ];then
    pswd=$ans
  fi
  echo
}
#--------------------------------
function choose_force_mode {
  echo "----- choose_force_mode -----"
  echo -n "Do you wish to force update of some config files (eg log4j): (y/n) [default - $force]: "
  read ans
  if [ "$ans" = "y" ];then
    force="${force_log4j} "
  fi
}
#--------------------------------
function clean_log_directory {
  delimit clean_log_directory
  backup_file=$log_backup_dir/$tomcat.$(date '+%Y%m%d-%H%M').tgz
  if [ ! -d "$log_backup_dir" ];then
    mkdir $log_backup_dir >/dev/null 2>&1
  fi
  tomcat_log_dir=$tomcat_dir/$tomcat/logs
  prev_dir=$(pwd)
  cd $tomcat_log_dir
  if [ ! -d "$tomcat_log_dir" ];then
    logerr "the tomcat log directory does not exist ($tomcat_log_dir)"
  fi
  if [ "$PWD" != "$tomcat_log_dir" ];then
    logerr "We are not in the log directory ($PWD)"
  fi
  logit "... tar'ing and cleaning log files in $PWD:
$(ls -l)
"
  sleep 2
  tar zcf $backup_file *
  rm -f $tomcat_log_dir/*
  logit "... all done
$PWD:
$(ls -l)

Log file backup: 
$(ls -l $backup_file)
"
  cd $prev_dir
  sleep 2
}
#------------------------------- 
function install_upgrade {
  delimit install_upgrade
  if [ "$mysql_file" != "NONE" ];then
    pswd="$(cat $mysql_file)"
  fi
  runit "$pgm -d $pswd -S $source ${force}-s $(echo $tomcat|cut -d'-' -f2)"
  logit "Install was successful"
  sleep 3
}
#-------------------------------
function verify_the_makefile_target_dir_exists {
  dir=$source/target
  if [ ! -d "$dir" ];then
    logerr "The target build directory ($dir) does not exist ... did the build fail?"
  fi
}
#-------------------------------
function verify_the_update_program_exists {
  pgm=$source/$update_pgm
  if [ ! -f "$pgm" ];then
    logerr "The update program ($pgm) does not exist ... something wrong?"
  fi
}
#-------------------------------
function finish_up {
  delimit finish_up
  logit "
You have installed the following release of the Gratia collector:
$(cat $tomcat_dir/$tomcat/gratia/gratia-release)

You can start the tomcat service with:
      service $(echo $tomcat |cut -d'/' -f3) start

You can then check the log files
      cd $tomcat_dir/$tomcat/logs
      tail -f *

Cron scripts that should be tested/executed after you start the collector:
$(crontab -l | grep $tomcat_dir/$tomcat)

"
}
#-------------------------------
function ask_to_start_collector {
  echo "----- ask_to_start_collector -----"
  echo -n "Do you want to start the collector: (y/n): "
  read ans
  if [ "$ans" = "y" ];then
    start_collector
  fi
}
#-----------------------
function start_collector {
  delimit start_collector
  logit "... starting collector"
  runit "/sbin/service $(echo $tomcat |cut -d'/' -f3) start"
  logit
  logit "... collector started
$(ps -ef |grep $tomcat | grep '   1' | egrep -v grep)
"
  logit
  logit "... sleeping 20 seconds to allow tomcat to deploy war files"
  sleep 20
  verify_upgrade
}
#-----------------------
function ask_to_run_static_reports {
  echo "-----  ask_to_run_static_reports -----"
  echo -n "Do you want to run the static reports: (y/n): "
  read ans
  if [ "$ans" = "y" ];then
    verify_static_reports
  fi
}
#--------------------------------
function final_verification {
  echo "----- final_verification ------"
printf "
We are ready to upgrade with the following information provided:
  host...................... $(hostname -f)
  tomcat instance........... $tomcat
  source location........... $source
  password.................. $pswd
  log4j config overwrite.... "
if [[ -n "$force" ]]; then
  printf "yes"
else
  printf "no"
fi
echo "
"
  ask_continue
}
#-----------------------------------
function log_final_verification {
  logit "\
Upgrading with the following information provided:
  host............... $(hostname -f)
  tomcat instance.... $tomcat
  source location.... $source
  password........... cannot say
"
  sleep 2
}
#-----------------------------------
function verify_root_user {
  if [ "$(id -u)" != "0" ];then
    logerr "You must be root user!"
  fi
}
#-------------------------------------
function send_mail {
  delimit send_mail
  status="$1"
  msg="$2"
  if [ -z "$recipients" ];then
    recipients=$default_recipients
  fi
  subject="Gratia upgrade: $(hostname -s) of $tomcat - $status"
  logit "Sending mail to $recipients"
  logit "Subject: $subject"

##cat <<EOF
##mail -s "$subject" $recipients
mail -s "$subject" $recipients <<EOF
$subject

This is an automated message from $PGM running on $(hostname -s). 

The following logs are available for more details:
  $logfile
  $tomcat_dir/$tomcat/logs/*.log files.
  $tomcat_dir/$tomcat/gratia-release

$msg
EOF
}
#----------------------------
function log_upgrade_start {
  logit
  logit "=============================================================="
  logit "Upgrade of $tomcat on $(hostname -s) started: $(date)"
  logit "Program: $PGM"
  logit "Log file: $logfile"
  logit
}
#----------------------------
function log_upgrade_end {
  logit
  logit "Log file: $logfile"
  logit "Upgrade of $tomcat on $(hostname -s) completed: $(date)"
  logit "=============================================================="
  cleanup
}
#----------------------------
function cleanup {
  cd $logdir
  total=$(ls |  wc -l)
  cnt=$(($total - $total_logfiles))
  if [ $cnt -gt 0 ];then
    for file in $(ls | head -$cnt )
    do
      rm -f $file
    done
  fi
}
#--------------------------------
function verify_instance_is_running {
  delimit  verify_instance_is_running
  cmd="ps -ef |grep file=$tomcat_dir/$tomcat |egrep -v grep"
  logit "Verifying that the tomcat instance is running using
  $cmd
"
  sleep 4
  process_cnt="$(ps -ef |grep file=$tomcat_dir/$tomcat |egrep -v grep |wc -l)"
  case $process_cnt in 
    0 ) logerr "... tomcat instance ($tomcat) not running" ;;
    1 ) ;;
    * ) logerr "More than 1 tomcat instance running for this gratia instance:
$(ps -ef |grep file=$tomcat_dir/$tomcat |egrep -v grep)
" 
        ;;
  esac
  logit "Tomcat instance for ($tomcat):
$(ps -ef |grep file=$tomcat_dir/$tomcat |egrep -v grep)
"
  logit "PASSED: tomcat instance ($tomcat) is running"
  sleep 1
}
#--------------------------------
function verify_tomcat_connection {
  delimit  verify_tomcat_connection
  logit "\
Verifying that tomcat is listening for the following connection
by doing a wget on the gratia-release file:"
  properties_file=$tomcat_dir/$tomcat/gratia/service-configuration.properties
  if [ ! -e $properties ];then
    logerr "Properties file does not exist: $properties_file"
  fi
  property=service.open.connection 
  service="$(grep $property $properties_file |egrep -v '#'|egrep -v grep| cut -d'=' -f2)"
  if [ -z $service ];then
    logerr "Cannot find attribute ($property) in $properties_file"
  fi
  logit "    tomcat service: $service" 
  gratia_release="$service/gratia-services/gratia-release"
  release_file=$tomcat_dir/$tomcat.$(basename $gratia_release)
  logit "... checking access to $gratia_release"
  sleep 4
  cmd="/usr/bin/wget --tries=5 --timeout=30 --dns-timeout=30 -O $release_file $gratia_release"
  rm -f $release_file
  runit "$cmd"
  logit "\
$(ls -l $release_file)

$(cat $release_file)
"
  logit "PASSED: Tomcat connection ($service) is good"
  sleep 1
}
#--------------------------------
function verify_port_availability {
  delimit  verify_port_availability
  expected_number_of_ports=6
  logit "
Verifying that the tomcat process has $expected_number_of_ports ports it
is listening on for this tomcat instance and all are by the same process.
Disclaimer: This is the closest one can do for this type validation.
"
  properties_file=$tomcat_dir/$tomcat/gratia/service-configuration.properties
  if [ ! -e $properties ];then
    logerr "Properties file does not exist: $properties_file"
  fi
  property=service.rmi.port 
  port="$(grep $property $properties_file |egrep -v '#' |egrep -v grep| cut -d'=' -f2)"
  if [ -z $port ];then
    logerr "Cannot find attribute ($property) in $properties_file"
  fi
  pid="$(/bin/netstat -n --listening --program |egrep $port |egrep -v grep|awk '{print $7}')"
  logit "...using port $port as the basis for the netstat command:
  /bin/netstat -n --listening --program |egrep $port
$(/bin/netstat -n --listening --program |egrep $port |egrep -v grep)
"
  sleep 4
  cmd="/bin/netstat -n --listening --program |egrep $pid |egrep -v grep"
  connection_cnt="$(/bin/netstat -n --listening --program |egrep $pid |egrep -v grep |wc -l)"
  case $connection_cnt in 
    $expected_number_of_ports ) ;;
    * ) logerr "Expected $expected_number_of_ports connections.... found ${connection_cnt}.
$(/bin/netstat -n --listening --program |egrep $pid |egrep -v grep)
" ;;
  esac
  logit "... connections based on pid ($pid):
$(/bin/netstat -n --listening --program |egrep $pid |egrep -v grep)
"
  logit "PASSED: number of connections look good"
  sleep 1
}
#-----------------------
function verify_static_reports {
  delimit verify_static_reports
  pdf_dir=$tomcat_dir/$tomcat/webapps/gratia-reports/reports-static
  csv_dir=$tomcat_dir/$tomcat/webapps/gratia-reports/reports-static_csv
  script="$(crontab -l| grep -e '^[^#]*'$tomcat_dir/$tomcat |awk '{print $6,$7,$8}' |sed -e s/\'//g)"
  if [[ -n "$script" ]]; then
         logit "... running static reports."
         runit "$script"
         if [ ! -d "$pdf_dir" ];then
           logerr "Static reports not available. Directory does not exist: $pdf_dir"
         fi
         if [ ! -d "$csv_dir" ];then
           logerr "Static reports not available. Directory does not exist: $csv_dir"
         fi
         logit "Static reports:
$(ls -l $pdf_dir)
$(ls -l $csv_dir)
"
         logit "PASSED: static reports processed"
  else
    logit "NOT APPLICABLE: static reports not used for $tomcat"
  fi
}
#---------------------------
function verify_upgrade {
   verify_instance_is_running 
   verify_tomcat_connection 
   verify_port_availability 
}
#----------------------------
function process_in_prompt_mode {
  choose_collector
  choose_source_directory
  choose_db_root_password 
  choose_force_mode
  final_verification
  log_upgrade_start
  log_final_verification
  verify_the_makefile_target_dir_exists 
  verify_the_update_program_exists
  install_upgrade
  clean_log_directory 
  finish_up
  ask_to_start_collector
  ask_to_run_static_reports
  record_successful_upgrade
}
#----------------------------
function process_in_no_prompt_mode {
  validate_collector
  verify_the_makefile_target_dir_exists 
  verify_the_update_program_exists
  log_upgrade_start
  log_final_verification
  install_upgrade
  clean_log_directory 
  finish_up
  if [ "$daily" = "yes" ];then
    start_collector 
    verify_static_reports
  else
    ask_to_start_collector
    ask_to_run_static_reports
  fi
  record_successful_upgrade
}
#----------------------------
function record_successful_upgrade {
  delimit record_successful_upgrade
  cleanup
  send_mail "SUCCESS" "The Gratia upgrade on $(hostname -f) of the $tomcat instance completed on $(date).

Release data:
$(cat $tomcat_dir/$tomcat/gratia/gratia-release)
"
  log_upgrade_end
}
#### MAIN ##############################################
PGM=$(basename $0)

DATE=$(date '+%Y-%m-%d')
total_logfiles=10
daily=NONE
new_instance="no"
prompt=yes
pswd=NONE
qa_mode=yes
source_type=""
source=NONE
##default_recipients="weigand@fnal.gov"
default_recipients="gratia-operation@fnal.gov"
recipients=""
release=NONE
release_dir=""
tomcat=NONE
tomcat_host=$(hostname -s)
tomcat_dir=/data
log_backup_dir=$tomcat_dir/gratia_tomcat_logs_backups
update_pgm=common/configuration/update-gratia-local
mysql_file=NONE
force_log4j="--force-log4j"

#--- get command line arguements ----
while test "x$1" != "x"; do
   if [ "$1" == "--help" ];then
        HELP="yes";shift
   elif [ "$1" == "--instance" ];then
        tomcat="$2";shift;shift
   elif [ "$1" == "--source" ];then 
        source="$2";shift;shift
   elif [ "$1" == "--pswd" ];then 
        pswd="$2";shift;shift
   elif [ "$1" == "--daily" ];then
        daily="yes";recipients="$2";shift;shift
   elif [ "$1" == "--mysql" ];then
        mysql_file="$2";shift;shift
   elif [ "$1" == "${force_log4j}" ];then
        force="${force_log4j} ";shift
   else
        usage_error "Invalid command line argument: $1"
   fi
done

if [ -n "$HELP" ];then
  usage;exit 1
fi

verify_root_user

#-- check for Q/A mode or required args---
if [ "$tomcat" != "NONE" ] && \
   [ "$source" != "NONE" ] && \
   [ "$pswd"   != "NONE" ];then
  prompt=no
elif [ "$tomcat" = "NONE" ] && \
     [ "$source" = "NONE" ] && \
     [ "$pswd"   = "NONE" ] && \
     [ "$daily"  = "NONE" ] && \
     [ "$mysql_file"  = "NONE" ];then
  prompt=yes
  initial_dialog
else
  usage_error "Missing one or more required arguments."
fi

#-- verify some required args -----
if [ "$daily" = "yes" ];then
  if [ -z "$recipients" ];then
    usage_error "Email address is required when --daily is used"
  fi
  if [ -z "$mysql_file" ];then
    usage_error "--mysql argument required when --daily is used"
  fi
  if [ ! -e "$mysql_file" ];then
    usage_error "--mysql file ($mysql_file) does not exist."
  fi
fi

#--- this is so we don't write badly named log files in $tomcat_dir --
if [ "$prompt" = "no" ];then
  if [ ! -d "$tomcat_dir/$tomcat" ];then
    usage_error "--instance directory ($tomcat_dir/$tomcat) does not exist."
  fi
fi

#--- do it -----------
qa_mode=yes
while 
 [ "$qa_mode" = "yes" ]
do 
  case $prompt in 
    "yes" ) qa_mode="yes"  # allows multiple upgrades to be performed
            process_in_prompt_mode
            ;;
   "no"  ) qa_mode="no"   # forces a single upgrade to be performed
           process_in_no_prompt_mode
           ;;
    * ) usage_error "System error - problem in this script. This should never occur.";;
  esac
done

exit 0

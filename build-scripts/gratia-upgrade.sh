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
  DATE="$(date '+%Y/%m/%d-%H:%M:%S')"
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
  logit "----- $1 ----------"
}
#-----------------------------------
function usage {
echo "
Usage: $PGM --help
This script is intended to simplify the upgrading of a gratia instance
in development, integration and production.  

There are 2 modes:
1. Question and answer mode (no command line arguments)
     $PGM 
2. No prompt mode (all arguments are required)
   Refer to the paragraph below as to the prompts for the value of the
   individual arguments.
     $PGM --instance TOMCAT_INSTANCE --source SOURCE_DIR --pswd ROOT_PSWD
          [--daily  EMAIL_ADDRESS(ES) --mysql MYSQL_FILE]

Although it does not really do much more than is already available to do, its
intent is to take some of the guesswork (memory-like) out of the upgrade
process.

A major assumption it makes is that all tomcat collectors are in $tomcat_dir.
If this is not true, you will have to do it the old fashion way.

 No prompt mode   Prompt mode questions
 --------------   --------------------- 
 --instance       tomcat instance (e.g, tomcat-weigand)
 --source         source directory
                    - daily builds..... /home/gratia/gratia-builds
                    - release builds... /home/gratia/gratia-releases
                    - other............ specified by you
 --pswd           mysql root password
 --daily          do you want to start the tomcat/collector service
 --mysql          DB MySql file containing the password for upgrades 
                  (required when running from cron and when used the 
                  --pswd argument is ignored)

If '--daily' is used, then all arguments are required and use of this argument
will automatically start the tomcat/collector and requires specifying the
email address(es).  It is intended for use ONLY when running from cron for 
the purpose of daily test installations.

The script performs the following:
 1. shutdown your tomcat instance/collector
 2. run update-gratia-local with the appropriate arguments
 3. clean your log directory saving the old logs in 
      $tomcat_dir/gratia_tomcat_logs_backups
 4. optionally, allow you to start your collector
    (when the --daily argument is specified, it will start the collector)

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
  for dir in $(ls -d $tomcat_dir/tomcat-* |egrep -v "log|grep")
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
    echo prompt:  $prompt
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
function clean_log_directory {
  delimit clean_log_directory
  backup_file=$log_backup_dir/$tomcat.$(date '+%Y%m%d-%H%M').tgz
  if [ ! -d "$log_backup_dir" ];then
    mkdir $log_backup_dir >/dev/null 2>&1
  fi
  tomcat_log_dir=$tomcat_dir/$tomcat/logs
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
  rm -f admin* catalina.* glite-* gratia* hibernate* host* localhost* manager*
  logit "... all done
$PWD:
$(ls -l)

Log file backup: 
$(ls -l $backup_file)
"
  sleep 2
}
#------------------------------- 
function install_upgrade {
  delimit install_upgrade
  if [ "$mysql_file" != "NONE" ];then
    pswd="$(cat $mysql_file)"
  fi
  runit "$pgm -d $pswd -S $source -s $(echo $tomcat|cut -d'-' -f2)"
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
}
#-----------------------
function ask_to_run_static_reports {
  echo "-----  ask_to_run_static_reports -----"
  echo -n "Do you want to run the static reports: (y/n): "
  read ans
  if [ "$ans" = "y" ];then
    run_static_reports
  fi
}
#-----------------------
function run_static_reports {
  delimit run_static_reports
  pdf_dir=$tomcat_dir/$tomcat/webapps/gratia-reports/reports-static
  csv_dir=$tomcat_dir/$tomcat/webapps/gratia-reports/reports-static_csv
  case $tomcat in 
   "tomcat-itb_gratia_itb" ) 
         logit "... sleeping 20 seconds for tomcat to autodeply war files."
         sleep 20
         logit "... running static reports."
         script="$(crontab -l| grep $tomcat_dir/$tomcat |awk '{print $6,$7,$8}' |sed -e s/\'//g)"
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
         ;;
   * ) logit "... static reports not used for $tomcat";logit;;
  esac     
}
#--------------------------------
function final_verification {
  echo "----- final_verification ------"
echo "
We are ready to upgrade with the following information provided:
  host............... $(hostname -f)
  tomcat instance.... $tomcat
  source location.... $source
  password........... $pswd
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
  status="$1"
  msg="$2"
  if [ -z "$recipients" ];then
    recipients=$default_recipients
  fi
  logit "... sending mail to $recipients"
  subject="Gratia upgrade: $(hostname -s) of $tomcat - $status"

##cat <<EOF
##mail -s "$subject" $recipients
mail -s "$subject" $recipients <<EOF
$subject

This is an automated message from $PGM running on $(hostname -s). 

The following logs are available for more details:
  $logfile
  $tomcat_dir/$tomcat/logs/*.log files.

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
function process_in_prompt_mode {
  choose_collector
  choose_source_directory
  choose_db_root_password 
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
  log_upgrade_end
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
    run_static_reports
  else
    ask_to_start_collector
  fi
  log_upgrade_end
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
#### MAIN ##############################################
PGM=$(basename $0)
tomcat_tarball=/home/gratia/tomcat-tarballs/apache-tomcat-5.5.25.tar.gz

total_logfiles=10
daily=NONE
new_instance="no"
prompt=yes
pswd=NONE
qa_mode=yes
source_type=""
source=NONE
##default_recipients="weigand@fnal.gov greenc@fnal.gov pcanal@fnal.gov fermigrid-operations@fnal.gov"
default_recipients="weigand@fnal.gov"
recipients=""
release=NONE
release_dir=""
tomcat=NONE
tomcat_host=$(hostname -s)
tomcat_dir=/data
log_backup_dir=$tomcat_dir/gratia_tomcat_logs_backups
update_pgm=common/configuration/update-gratia-local
mysql_file=NONE


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

#--- set log file name ---
logdir=$tomcat_dir/${tomcat}-upgrade.log
logfile=$logdir/$(date '+%Y-%m-%d').log
if [ ! -d "$logdir" ];then
  mkdir -p $logdir
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

cleanup
send_mail "SUCCESS" "The Gratia upgrade on $(hostname -f) of the $tomcat instance completed on $(date).

Release data:
$(cat $tomcat_dir/$tomcat/gratia/gratia-release)
"
exit 0

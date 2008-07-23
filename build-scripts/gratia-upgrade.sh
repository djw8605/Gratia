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
  echo "$1"
}
#------------------------------------
function logerr {
  logit;logit "ERROR: $1";logit;exit 1
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
  logit;logit "----- $1 ----------"
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

Although it does not really do much more than is already available to do, its
intent is to take some of the guesswork (memory-like) out of the upgrade
process.

The only assumption it makes is that all tomcat collectors are in /data.
If this is not true, you will have to do it the old fashion way.

The script will prompt you for:
 tomcat instance (e.g, tomcat-weigand)
 source directory
   - daily builds..... /home/gratia/gratia-builds
   - release builds... /home/gratia/gratia-releases
   - other............ specified by you
 mysql root password

The script then:
 1. shutdown your tomcat instance/collector
 2. run update-gratia-local with the appropriate arguments
 3. clean your log directory saving the old logs in 
      /data/gratia_tomcat_logs_backups
 4. optionally, allow you to start your collector
    (when in 'no prompt' mode, it will start the collector)

You must be root user to execute this script.
"
}
#--------------------------------
function initial_dialog {
  logit "
You can terminate this script at any time using <CNTL-C> EXCEPT after
you give the FINAL approval to perform the update.

This script will allow you to upgrade one of these Gratia collectors in 
the $tomcat_dir directory on $tomcat_host:"
  cnt=0
  for dir in $(ls -d $tomcat_dir/tomcat-*)
  do 
    logit "$(ls -ld $dir)"
    if [ -w "$dir" ];then
      collectors="$collectors $(basename $dir)"
      cnt=1
    fi
  done
  if [ $cnt -eq 0 ];then
    logerr "You lack permission to update any Gratia collector on this node.
... bye"
  fi
  ask_continue
}
#--------------------------------
function choose_collector {
  delimit choose_collector
  while :
  do
    echo -n "
These are the collectors on this node ($tomcat_host) you are permitted to upgrade:
$(for a in $collectors;do echo "   $a";done)

Choose a collector: " 
    read tomcat 
    if [ -z "$tomcat" ];then
      try_again "... You HAVE to choose one to proceed."
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
  delimit choose_source_directory
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
  delimit find_release_source
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
  delimit find_build_source
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
  delimit find_other_source
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
  delimit  choose_db_root_password 
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
  cmd="$pgm -d $pswd -S $source -s $(echo $tomcat|cut -d'-' -f2)"
echo "
... executing:
$cmd"
  sleep 3
  ${cmd};rtn=$?
  if [ "$rtn" != "0" ];then
    logerr "Install FAILED!!!!"
  fi
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
  echo "
You have installed the following release of the Gratia collector:
$(cat $tomcat_dir/$tomcat/gratia/gratia-release)

You can start the tomcat service with:
      service $(echo $tomcat |cut -d'/' -f3) start

You can then check the log files
      cd $tomcat_dir/$tomcat/logs
      tail -f *

Cron scripts that should be tested/executed:
$(crontab -l | grep $tomcat_dir/$tomcat)

"
}
#-------------------------------
function ask_to_start_collector {
  delimit ask_to_start_collector
  echo -n "Do you want to start the collector: (y/n): "
  read ans
  if [ "$ans" = "y" ];then
    start_collector
  fi
}
#-----------------------
function start_collector {
  cmd="service $(echo $tomcat |cut -d'/' -f3) start"
  logit "... starting collector"
  logit "    $cmd" 
  $cmd
}
#--------------------------------
function final_verification {
  delimit final_verification
  logit "
We are ready to upgrade with the following information provided:
  host............... $(hostname -f)
  tomcat instance.... $tomcat
  source location.... $source
  password........... $pswd
"
  ask_continue
}
#-----------------------------------
function verify_root_user {
  if [ "$(id -u)" != "0" ];then
    logerr "You must be root user!"
  fi
}
#### MAIN ##############################################
PGM=$(basename $0)
tomcat_tarball=/home/gratia/tomcat-tarballs/apache-tomcat-5.5.25.tar.gz
new_instance="no"
do_more_than_once="yes"
prompt=yes
source_type=""
source=NONE
pswd=NONE
release=NONE
tomcat=NONE
tomcat_host=$(hostname -s)
tomcat_dir=/data
log_backup_dir=$tomcat_dir/gratia_tomcat_logs_backups
release_dir=""
update_pgm=common/configuration/update-gratia-local

#--- get command line arguements ----
while test "x$1" != "x"; do
   if [ "$1" == "--help" ]; then
        HELP="yes";shift
#   elif [ "$1" == "--new" ]; then
#        new_instance="yes"
#        shift
   elif [ "$1" == "--instance" ]; then
        tomcat=$2;shift 2
   elif [ "$1" == "--source" ]; then 
        source="$2";shift 2
   elif [ "$1" == "--pswd" ]; then 
        pswd="$2";shift 2
   else
        usage
        logerr "Invalid command line argument: $1"
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
  do_more_than_one=no
  prompt=no
elif [ "$tomcat" = "NONE" ] && \
     [ "$source" = "NONE" ] && \
     [ "$pswd"   = "NONE" ];then
  do_more_than_one=yes
  prompt=yes
  initial_dialog
else
  usage
  logerr "Missing one or more required arguments."
fi

#--- do it -----------
while 
 [ "$do_more_than_once" = "yes" ]
do 
  case $prompt in 
    "yes" ) do_more_than_once="yes"
            choose_collector
            choose_source_directory
            choose_db_root_password 
            final_verification
            verify_the_makefile_target_dir_exists 
            verify_the_update_program_exists
            install_upgrade
            clean_log_directory 
            finish_up
            ask_to_start_collector
            ;;

   "no"  ) do_more_than_once="no"
           validate_collector
           verify_the_makefile_target_dir_exists 
           verify_the_update_program_exists
           install_upgrade
           clean_log_directory 
           finish_up
           start_collector 
           ;;
    * ) logerr "System error - problem in this script. This should never occur.";;
  esac
done

logit  DONE
exit 0




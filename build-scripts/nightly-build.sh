#!/bin/bash
##########################################################
# John Weigand (06/26/2008)
#
# Script building gratia nightly from cron
#
# See the usage function for a description of what this
# does or execute this with the --help argument.
##########################################################
function usage {
  echo "\

Usage: $PGM [--dir directory] [--mail addresses] [--files n] [--help]

This script will perform an export of the HEAD of the Gratia SVN repository
and then perform a 'make release' on the software.  

NOTE: Since it is an export, you will not be able to effect any 
      commits in this area.

It will create a directory called $(basename $build_dir) in your \$HOME 
area unless overridden using the --dir argument. If the directory does not
exist, it will create it. 

Parent source directory.. $nightly_dir
Log file................. $logfile

SVN: $svn.

Arguments:
   --dir   - the parent directory for the $(basename $build_dir) directory
   --mail  - email address(es) to be used on completion of the build.
   --files - number of build directories to retain in $build_dir
             Since the source directory and log files are timestamped by day,
             this argument allows you to specify the number (not days) of 
             builds to retain.  This is a housekeeping thing. 
             It is not perfect either.  If you have too many successive days
             with failures, the symlink to the last good build will be
             invalid.
             Default: $files

Cron entry note: do not redirect stdout/stderr to /dev/null or you may be 
                 oblivious to any uncaught errors.

If you run this more than once per day, the parent source directory will be
overwritten each time.  However the log file will be appended to.

On success, it will create a symbolic link to the newly built gratia source 
directory called:
  $latest_good_build

An email notification upon completion will be sent to: 
  $MAILTO
"
exit 1
}
# -------------------------------
function usage_error {
  usage
  echo;echo "ERROR: $1"
  exit 1
}
# -------------------------------
function logit {
  if [ -z "$MAILTO" ];then
    echo "$1" | tee -a $logfile
  else
    echo "$1" >>$logfile
  fi
}
# -------------------------------
function logerr {
  logit "ERROR: $1"
  send_error_mail "$1"  
  logit "... refer to the log file for details"
  logit "Logfile: $logfile"
  logit "==== $PGM   End: $(date) ==="
  exit 1
}
# -------------------------------
function send_error_mail {
  message="It failed for the following reason:
$1
"
  send_mail "$message" "FAILED"
}
# -------------------------------
function send_success_mail {
  logit "$1"
  send_mail "$1" "SUCCESSFUL"
}
# -------------------------------
function send_mail {
  msg="$1"
  status="$2"
  if [ -z "$MAILTO" ];then
    logit "... no mail being sent"
    return
  fi
  subject="Gratia build on $(hostname -s) $status"
  logit "... mail being sent to: $MAILTO
Subject: $subject
"
mail -s "$subject" $MAILTO <<EOF

This is an automated message from a cron script running on $(hostname -f).

This script performs a nightly build of Gratia.

Status: $status
Date: $(date)
Script: $PGM
Logfile: $logfile

$msg

Refer to the logfile shown above for more details.

EOF
}
# -------------------------------
function runit {
  cmd=$1
  logit "... executing in $PWD:
  $cmd
"
  $cmd >>$logfile 2>&1;rtn=$?
  logit "Return code: $rtn"
  if [ "$rtn" != "0" ];then
    logerr "Command failed: $cmd 
Return code: $rtn"
  fi
}
# -------------------------------
function validate_environment {
  logit "==== validate_environment - $(date) ==="
  #--- verify nightly directory ---- 
  dir=$nightly_dir
  if [ -d $dir ];then
    logit "... deleting $dir"
    rm -rf $dir
  fi 
  #--- verify svn is available ---- 
  if [ "$(type svn >/dev/null 2>&1 ;echo $?)" != "0" ];then
    logerr "Cannot find svn on this host: $(hostname)"
  fi
}
# -------------------------------
function svn_checkout_gratia {
  logit "==== svn_checkout_gratia - $(date) ==="
  cd $build_dir
  cmd="$svn export http://gratia.svn.sourceforge.net/svnroot/gratia/trunk $(basename $nightly_dir)"
  runit "$cmd"
}
# -------------------------------
function validate_build_env {
  logit "==== validate_build_env - $(date) ==="
  if [ ! -d $buildDir ];then
    logerr "Build directory ($buildDir) does not exist";exit 1
  fi

  if [ ! -r $jdk ];then
    logit "jdk setup script ($jdk) does not exist";exit 1
  fi
}
# -------------------------------
function make_gratia {
  logit "==== make_gratia - $(date) ==="
  source $jdk
  cd $buildDir
  runit "make release" 
  logit "
====================================
... target dir:
$(ls -l $buildDir/../target)
====================================
"
}
# -------------------------------
function cleanup {
  logit "==== cleanup $(date) ==="
  cd $build_dir
  logit "... PWD: $PWD"
  files=$(($files * 2 + 1))
  logit "... Files to keep: $files"
  total=$(ls |  wc -l)
  logit "... Total files: $total"
  cnt=$(($total - $files))
  if [ $cnt -le 0 ];then
    logit "... No files to remove. $total total files"
  else
    logit "... Files to remove: $cnt"
    for file in $(ls | head -$cnt )
    do
      if [ -f $file ];then
        logit "... removing file: $file"
        runit "rm -f $file"
      fi
      if [ -d $file ];then
        logit "... removing directory: $file"
        runit "rm -rf $file"
      fi
    done 
  fi
}
# -------------------------------
function make_symlink {
  logit "==== make_symlink $(date) ==="
  runit "rm -f $latest_good_build"
  runit "ln -s $nightly_dir $latest_good_build"
}
#----------------------
function set_build_area {
  build_dir=$buildHOME/gratia-builds
  nightly_dir=$build_dir/gratia-$DATE
  latest_good_build=$build_dir/gratia-latest
  logfile=$nightly_dir.log
}
##### MAIN ############################################
PGM=$(basename $0)
DATE=$(date '+%Y-%m-%d')

#-- defaults ---
MAILTO="gratia-builds@fnal.gov"
buildHOME=$HOME
files=4
set_build_area

#--- svn ----
svn='svn'

#--- validate command line ---
while test "x$1" != "x"; do
   if [ "$1" == "--help" ]; then
        usage
   elif [ "$1" == "--mail" ]; then
        MAILTO="$2"
        shift
        shift
   elif [ "$1" == "--dir" ]; then
        buildHOME=$2
        shift
        shift
   elif [ "$1" == "--files" ]; then
        files=$2
        shift
        shift
   else
        echo "ERROR: Invalid command line argument"
        usage
   fi
done

if [ ! -d "$buildHOME" ];then
  mkdir $buildHOME
fi
if [ -z "$files" ];then
  files=7
fi

#--- build area ----
set_build_area

#--- make area ----
buildDir=$nightly_dir/build-scripts
jdk=$buildDir/setup-jdk15.sh

if [ ! -d $build_dir ];then
  mkdir $build_dir
fi 

logit "==== $PGM Start: $(date) ==="
#--- check out from svn ---
validate_environment
svn_checkout_gratia 

#--- make gratia ---
validate_build_env 
make_gratia

#--- clean up make area ---
cleanup

#--- make symlink ---
make_symlink

#--- all done ---
message="Build directory: $nightly_dir/target
$(ls -l $nightly_dir/target)

Symlink:
$(ls -l $latest_good_build)
"

send_success_mail "$message" 
logit "Logfile: $logfile"
logit "==== $PGM   End: $(date) ==="

exit 0

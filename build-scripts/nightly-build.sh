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

This script will perform an export of the HEAD of the Gratia CVS repository
and then perform a 'make release' on the software.  

NOTE: Since it is an export, you will not be able to effect any 
      commits in this area.

It will create a directory called $(basename $build_dir) in your \$HOME 
area unless overridden using the --dir argument. If the directory does not
exist, it will create it. 

Parent source directory: $nightly_dir
Log file: $logfile

CVS: $cvs.

Arguments:
   --dir   - the parent directory for the $(basename $build_dir) directory
   --mail  - email address(es) to be used if the build fails.  This should be
             used only when running as a cron job.  It will only send mail on
             failure. 
             This should be specified when running as a cron process.
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

On success, it will create a symbolic link called gratia-latest to the
newly built gratia source directory.
"
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
  send_mail "ERROR: $1"
  logit "... refer to the log file for details"
  logit "Logfile: $logfile"
  logit "==== $PGM   End: $(date) ==="
  exit 1
}
# -------------------------------
function send_mail {
  if [ -z "$MAILTO" ];then
    logit "... no mail being sent"
    return
  fi
  logit "... mail being sent to: $MAILTO"
  msg="$1"
  subject="Gratia build on $(hostname) FAILED"
mail -s "$subject" $MAILTO <<EOF

This is an automated message from a cron script running
on $(hostname) that has failed.

Script: $PGM
Date: $(date)
Logfile: $logfile

This script performs a nightly build of Gratia.

It failed with the following error:
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
    logerr "command failed...exitting"
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
  #--- verify ups is available ---- 
  if [ -f /usr/local/etc/setups.sh ];then
    . /usr/local/etc/setups.sh
  elif [ -f "/fnal/ups/etc/setups.sh" ];then
      . /fnal/ups/etc/setups.sh
  else
    logerr "Cannot find UPS CVS on this host: $(hostname)"
  fi
  setup cvs
}
# -------------------------------
function cvs_checkout_gratia {
  logit "==== cvs_checkout_gratia - $(date) ==="
  export CVSROOT=":pserver:anonymous@gratia.cvs.sourceforge.net:2401/cvsroot/gratia"
  cd $build_dir
  cmd="$cvs export -d $(basename $nightly_dir) -D $DATE gratia"
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
  fi
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
}
# -------------------------------
function make_symlink {
  logit "==== make_symlink $(date) ==="
  runit "rm -f $latest_good_build"
  runit "ln -s $nightly_dir $latest_good_build"
}
##### MAIN ############################################
PGM=$(basename $0)
DATE=$(date '+%Y-%m-%d')
buildHOME=$HOME
build_dir=$buildHOME/gratia-builds
nightly_dir=$build_dir/gratia-$DATE
latest_good_build=$build_dir/gratia-latest
logfile=$nightly_dir.log
files=7

MAILTO=""

#--- cvs ----
cvs='cvs -z3 -d:pserver:anonymous@gratia.cvs.sourceforge.net:/cvsroot/gratia'

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

#--- build area ----
build_dir=$buildHOME/$(basename $build_dir)
nightly_dir=$build_dir/$(basename $nightly_dir)
logfile=$build_dir/$(basename $logfile)


#--- make area ----
buildDir=$nightly_dir/build-scripts
jdk=$buildDir/setup-jdk15.sh

if [ ! -d $build_dir ];then
  mkdir $build_dir
fi 

logit "==== $PGM Start: $(date) ==="
#--- check out from cvs ---
validate_environment
cvs_checkout_gratia 

#--- make gratia ---
validate_build_env 
make_gratia

#--- clean up make area ---
cleanup

#--- make symlink ---
make_symlink

logit "Logfile: $logfile"
logit "Symbolic link: $(ls -l $latest_good_build)"
logit "==== $PGM   End: $(date) ==="

exit 0

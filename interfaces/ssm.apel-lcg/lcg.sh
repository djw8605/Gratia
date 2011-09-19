#!/bin/bash
###############################################################
# Gratia - APEL/LCG Interface script
#
# Author: John Weigand (4/25/2007)
#
# If you are looking for assistance with this script, you should first read
# the README-Gratia-APEL-interface document as it is a little too much to
# generate here.
# 
###############################################################
#-------------------------
function logit {
  echo "$1"
}
function logerr {
  logit "The  Gratia to APEL transfer failed. 

ERROR condition: $1

This messages is being generated by the script:
  $PGM
running on node: $(hostname -f)

This is normally run as a cron process.

The  log files associated with this process will provide further details
as to the failure condition.  See the README-Gratia-APEL-interface 
file for further details on the location of these files.
"
  exit 1
}
#### MAIN ######################################
PGM=$(basename $0)

#--- source ups for setup script ----
#setups=/fnal/ups/etc/setups.sh
#if [ ! -f $setups ];then
#  logerr "UPS setups.sh ($setups) script not available" 
#fi
#source $setups

#--- setup mysql ----
#setup mysql 2>/dev/null
if [ "$(type mysql 1>/dev/null 2>&1;echo $?)" != "0" ];then
  logerr "MySql client not available.  This script assumes it is 
available via Fermi UPS in $setups"
fi

#--- verify python is availabl ----
if [ "$(type python 1>/dev/null 2>&1;echo $?)" != "0" ];then
  logerr "Python is required for this script to run and it is not available."
fi

#--- run the transfer ----
python LCG.py  $*

exit 0

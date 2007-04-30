#!/bin/bash
###############################################################
#
###############################################################
function logit {
  echo "$1"
}
function logerr {
  logit "ERROR: $1"
  exit 1
}
#### MAIN ######################################
dates="\
2006/02
2006/03
2006/04
2006/05
2006/06
2006/07
2006/08
2006/09
2006/10
2006/11
2006/12
2007/01
2007/02
2007/03
2007/04"
echo $dates

for date in $dates
do
  cmd="lcg.sh --date=$date $*" 
  echo "...running: $cmd"
  $cmd
  if [ "$?" != "0" ];then
    logerr "......FAILED for $date"
  fi
done

exit 0

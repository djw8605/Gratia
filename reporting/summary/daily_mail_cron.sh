#!/usr/bin/env bash

#. /data/test-vdt/setup.sh
#--- find a mysql client ----
if [ "$(type mysql >/dev/null 2>&1;echo $?)" != 0 ];then
  setups=/fnal/ups/etc/setups.sh
  if [ ! -f $setups ];then
    logerr  "UPS setups.sh ($setups) script not available"
    exit 1
  fi
  source $setups
  setup mysql 2>/dev/null
fi
if [ "$(type mysql >/dev/null 2>&1;echo $?)" != "0" ];then
  logerr "MySql client not available.  This script assumes it is
available via Fermi UPS in $setups or an rpm install"
  exit 1
fi

export http_proxy=http://squid.fnal.gov:3128 
where=`dirname $0`
cd $where
if [ $2 == "--grid" -a $3 != "" ] ; then
  $where/daily_mutt.sh $1 $2 $3
else
  $where/daily_mutt.sh $1
fi


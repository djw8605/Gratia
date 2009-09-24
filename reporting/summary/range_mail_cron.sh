#!/usr/bin/env bash

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
if [ $3 == "--grid" -a $4 != "" ] ; then
  $where/range_mutt.sh $1 $2 $3 $4 
else
  $where/range_mutt.sh $1 $2
fi


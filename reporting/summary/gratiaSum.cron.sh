#!/usr/bin/env bash

gsum="./gratiaSum.py"

while test "x$1" != "x"; do
   if [ "$1" == "--help" ]; then 
	echo "usage: $0 [--debug] [quoted_string_representing_starting_date (as accepted by date -d)]"
	exit 1
   elif [ "$1" == "--debug" ]; then
	debug=x
	shift
   else 
        date_arg=$1
	shift
   fi
done

export http_proxy=http://squid.fnal.gov:3128 
when=$(date -d "${date_arg:-yesterday}" +"%d %B %Y")
whenarg=$(date -d "${date_arg:-yesterday}" +"%Y/%m/%d")
where=`dirname $0`

# . /data/test-vdt/setup.sh
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

export PYTHONPATH=${PYTHON_PATH}:$where/probe/common
cd $where
[[ -x "$gsum" ]] || chmod +x "$gsum"
"$gsum" $whenarg

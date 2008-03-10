#!/usr/bin/env bash


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

when=$(date -d "${date_arg:-yesterday}" +"%d %B %Y")
whenarg=$(date -d "${date_arg:-yesterday}" +"%Y/%m/%d")
where=`dirname $0`

# . /data/test-vdt/setup.sh
. /usr/local/etc/setups.sh 
setup mysql

cd $where
./gratiaSum.py $whenarg

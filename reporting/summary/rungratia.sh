#!/bin/sh 
mutt=/usr/bin/mutt
#$HOME/bin/mymutt/runmutt

# space separated list of mail recipients
PROD_MAILTO="osg-accounting-info@fnal.gov"
MAILTO="pcanal@fnal.gov"
WEBLOC="http://gratia-osg.fnal.gov:8880/gratia-reporting"

while test "x$1" != "x"; do
   if [ "$1" == "--help" ]; then 
	echo "usage: $0 [--debug] [--mail email] [quoted_string_representing_starting_date (as accepted by date -d)]"
	exit 1
   elif [ "$1" == "--debug" ]; then
	debug=x
	shift
   elif [ "$1" == "--production" ]; then
	MAILTO=$PROD_MAILTO
	shift
   elif [ "$1" == "--mail" ]; then
	MAILTO=$2
	shift
	shift
   else 
        date_arg=$1
	shift
   fi
done

where=`dirname $0`
#Use pushd/popd rather than a simple 'cd', just in case
#`dirname $0 is 'dot'/current directory
pushd $where > /dev/null

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
cd $HOME/root.mysql
. ../set_root_opt

popd > /dev/null

WORK_DIR=/var/tmp/workdir.${RANDOM}
mkdir -p $WORK_DIR
mkdir -p $WORK_DIR/osg_gratia_display

function sendto {
    cmd=$1
    when=$2
    txtfile=$3.txt
    csvfile=$3.csv
    subject="$4"

    echo "See $WEBLOC for more information" > $txtfile
    echo >> $txtfile
    eval $1 --output=text $when >> $txtfile

    echo "For more information see:,$WEBLOC" > $csvfile
    echo >> $csvfile
    eval $1 --output=csv $when >>  $csvfile
    
    echo $mutt -a $csvfile -s "$subject" $MAILTO < $txtfile
}

if [ "$debug" != "x" ]; then
    root.exe -b -q -l "rungratia.C(\"$WORK_DIR\",0)" | grep -v -e 'Processing' -e '^$' 
else
    root.exe -b -q -l "rungratia.C(\"$WORK_DIR\",1)" | grep -v -e 'Processing' -e '^$' 
fi

#scp -r $WORK_DIR/osg_gratia_display flxi02.fnal.gov:/afs/fnal.gov/files/expwww/gratia/html/Files

when=$(date -d "${date_arg:-yesterday}" +"%d %B %Y")

$mutt -F./muttrc -a $WORK_DIR/share.csv -s "Fraction of resource used by owner of resource on $when" $MAILTO < $WORK_DIR/share.txt

if [ "$debug" != "x" ]; then 
   rm -rf $WORK_DIR
fi

exit 1

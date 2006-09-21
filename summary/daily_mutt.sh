#!/usr/bin/env bash

# space separated list of mail recipients
MAILTO="osg-accounting-info@fnal.gov"
WEBLOC="http://gratia-osg.fnal.gov:8880/gratia-reporting"

while test "x$1" != "x"; do
   if [ "$1" == "--help" ]; then 
	echo "usage: $0 [--debug] [--mail email] [quoted_string_representing_starting_date (as accepted by date -d)]"
	exit 1
   elif [ "$1" == "--debug" ]; then
	debug=x
	shift
   elif [ "$1" == "--mail" ]; then
	MAILTO=$2
	shift
	shift
   fi
done

when=$(date -d "${1:-yesterday}" +"%d %B %Y")
whenarg=$(date -d "${1:-yesterday}" +"%Y/%m/%d")

MAIL_MSG="report for $when"

# Transfer the file now
WORK_DIR=workdir.${RANDOM}
REPORTTXT=${WORK_DIR}/report.txt
REPORTCSV=${WORK_DIR}/report.csv

mkdir $WORK_DIR

echo "See $WEBLOC for more information" > $REPORTTXT 
echo >> $REPORTTXT 
./daily --output=text $whenarg >> $REPORTTXT 

echo "For more information see:,$WEBLOC" > $REPORTCSV
echo >> $REPORTCSV
./daily --output=csv $whenarg >>  $REPORTCSV

mutt -a $REPORTCSV -s "$MAIL_MSG" $MAILTO < $REPORTTXT

if [ "$debug" != "x" ]; then 
   rm -rf $WORK_DIR
fi

exit 1

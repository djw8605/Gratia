#!/usr/bin/env bash

# space separated list of mail recipients
MAILTO="osg-accounting-info@fnal.gov"
WEBLOC="http://gratia-osg.fnal.gov:8880/gratia-reporting"
SUM_WEBLOC="http://gratia-osg.fnal.gov:8884/gratia-reporting"

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
   else 
        date_arg=$1
	shift
   fi
done

when=$(date -d "${date_arg:-yesterday}" +"%d %B %Y")
whenarg=$(date -d "${date_arg:-yesterday}" +"%Y/%m/%d")

MAIL_MSG="Report from the job level Gratia db for $when"
SUM_MAIL_MSG="Report from the daily summary Gratia db (i.e. provided by VOs) for $when"
STATUS_MAIL_MSG="Job Success Rate by Site for $when (from the job level Gratia db)"
VO_STATUS_MAIL_MSG="Job Success Rate by VO for $when (from the job level Gratia db) "
BOTH_STATUS_MAIL_MSG="Job Success Rate by Site and VO for $when (from the job level Gratia db) "
VO_MAIL_MSG="Gratia Summary for $when for VO: "

# Transfer the file now
WORK_DIR=workdir.${RANDOM}
#REPORTTXT=${WORK_DIR}/report.txt
#REPORTCSV=${WORK_DIR}/report.csv
#SUM_REPORTTXT=${WORK_DIR}/summary_report.txt
#SUM_REPORTCSV=${WORK_DIR}/summary_report.csv


mkdir $WORK_DIR

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
    mutt -F./muttrc -a $csvfile -s "$subject" $MAILTO < $txtfile
}

sendto ./daily $whenarg ${WORK_DIR}/report "$MAIL_MSG"
sendto ./dailyFromSummary $whenarg ${WORK_DIR}/summary_report "$SUM_MAIL_MSG"
sendto ./dailyStatus  $whenarg ${WORK_DIR}/status_report "$STATUS_MAIL_MSG"
sendto "./dailyStatus --groupby=VO"  $whenarg ${WORK_DIR}/vo_status_report "$VO_STATUS_MAIL_MSG"
sendto "./dailyStatus --groupby=Both"  $whenarg ${WORK_DIR}/vo_status_report "$BOTH_STATUS_MAIL_MSG"

grep -v '^#' voreports.config | while read line; do
    MYVO=`echo $line | cut -d\  -f1`
    export EMAIL1=`echo $line | cut -d\  -f2`
    export EMAIL_CC=`echo $line | cut -d\  -f3-`
    if [ "x$EMAIL_CC" == "x" ]; then
       export MAILTO=$EMAIL1
    else 
       export MAILTO="-c \"$EMAIL_CC\" $EMAIL1"
    fi
    sendto "./dailyForVO --voname=${MYVO}"   $whenarg ${WORK_DIR}/forvo "${VO_MAIL_MSG}${MYVO}"
done 

if [ "$debug" != "x" ]; then 
   rm -rf $WORK_DIR
fi

exit 1

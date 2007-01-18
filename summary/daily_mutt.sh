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
SUM_MAIL_MSG="Report from the daily summary Gratia db for $when"
STATUS_MAIL_MSG="Job Success Rate for $when (from the job level Gratia db)"

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
    
    mutt -a $csvfile -s "$subject" $MAILTO < $txtfile
}

#echo "See $WEBLOC for more information" > $REPORTTXT 
#echo >> $REPORTTXT 
#./daily --output=text $whenarg >> $REPORTTXT 

#echo "For more information see:,$WEBLOC" > $REPORTCSV
#echo >> $REPORTCSV
#./daily --output=csv $whenarg >>  $REPORTCSV

#echo mutt -a $REPORTCSV -s "$MAIL_MSG" $MAILTO < $REPORTTXT

#./dailyFromSummary --output=text $whenarg > $SUM_REPORTTXT 
#echo >> $SUM_REPORTTXT 
#echo "See $SUM_WEBLOC for more information" >> $SUM_REPORTTXT 

#./dailyFromSummary --output=csv $whenarg >  $SUM_REPORTCSV
#echo >> $SUM_REPORTCSV
#echo "For more information see:,$SUM_WEBLOC" >> $SUM_REPORTCSV

#echo mutt -a $SUM_REPORTCSV -s "$SUM_MAIL_MSG" $MAILTO < $SUM_REPORTTXT

sendto ./daily $whenarg ${WORK_DIR}/report "$MAIL_MSG"
sendto ./dailyFromSummary $whenarg ${WORK_DIR}/summary_report "$SUM_MAIL_MSG"
sendto ./dailyStatus  $whenarg ${WORK_DIR}/status_report "$STATUS_MAIL_MSG"


if [ "$debug" != "x" ]; then 
   rm -rf $WORK_DIR
fi

exit 1

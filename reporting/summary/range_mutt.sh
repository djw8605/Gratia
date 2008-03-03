#!/usr/bin/env bash

# space separated list of mail recipients
MAILTO="osg-accounting-info@fnal.gov"
#MAILTO="pcanal@fnal.gov"
WEBLOC="http://gratia-osg.fnal.gov:8880/gratia-reporting"
SUM_WEBLOC="http://gratia-osg.fnal.gov:8884/gratia-reporting"

ExtraArgs=--daily

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
   elif [ "$1" == "--weekly" ]; then
    ExtraArgs=$1
    ExtraHeader="Weekly "
    shift
   elif [ "$1" == "--monthly" ]; then
    ExtraArgs=$1
    ExtraHeader="Monthly "
    shift
   else 
    date_arg=$1
	shift
   fi
done

when=$(date -d "${date_arg:-yesterday}" +"%d %B %Y")
whenarg=$(date -d "${date_arg:-yesterday}" +"%Y/%m/%d")

MAIL_MSG="${ExtraHeader}Report from the job level Gratia db for $when"
SUM_MAIL_MSG="${ExtraHeader}Report from the daily summary Gratia db for $when"
STATUS_MAIL_MSG="${ExtraHeader}Job Success Rate for $when (from the job level Gratia db)"
REPORTING_MAIL_MSG="${ExtraHeader}Summary on how sites are reporting to Gratia for $when"
LONGJOBS_MAIL_MSG="${ExtraHeader}Report of jobs longer than 7 days for $when"
USER_MAIL_MSG="${ExtraHeader}Report by user for $when"

# Transfer the file now
WORK_DIR=workdir.${RANDOM}

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
    
    mutt -F ./muttrc -a $csvfile -s "$subject" $MAILTO < $txtfile
}


sendto ./range "$ExtraArgs $whenarg" ${WORK_DIR}/report "$MAIL_MSG"
sendto ./reporting "$ExtraArgs $whenarg" ${WORK_DIR}/report "$REPORTING_MAIL_MSG"
sendto ./longjobs "$ExtraArgs $whenarg" ${WORK_DIR}/report "$LONGJOBS_MAIL_MSG"
sendto ./usersreport "$ExtraArgs $whenarg" ${WORK_DIR}/report "$USER_MAIL_MSG"


if [ "$debug" != "x" ]; then 
   rm -rf $WORK_DIR
fi

exit 1

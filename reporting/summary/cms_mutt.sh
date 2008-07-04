#!/usr/bin/env bash

# space separated list of mail recipients
MAILTO="cms-t1@fnal.gov dmason@fnal.gov gutsche@fnal.gov pcanal@fnal.gov"
#MAILTO="pcanal@fnal.gov"
WEBLOC="http://gratia-osg.fnal.gov:8880/gratia-reporting"
SUM_WEBLOC="http://gratia-osg.fnal.gov:8884/gratia-reporting"

ExtraArgs=--daily

when=$(date -d "${date_arg:-yesterday}" +"%d %B %Y")
whenarg=$(date -d "${date_arg:-yesterday}" +"%Y/%m/%d")

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
    when=$(date -d "${date_arg:-today}" +"%d %B %Y")
    whenarg=$(date -d "${date_arg:-today}" +"%Y/%m/%d")
    shift
   elif [ "$1" == "--monthly" ]; then
    ExtraArgs=$1
    ExtraHeader="Monthly "
    when=$(date -d "${date_arg:-today}" +"%d %B %Y")
    whenarg=$(date -d "${date_arg:-today}" +"%Y/%m/%d")
    shift
   else 
    date_arg=$1
	shift
   fi
done


MAIL_MSG="${ExtraHeader}CMS FNAL T1 Production Report for $when"

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


sendto ./cms_range "$ExtraArgs $whenarg" ${WORK_DIR}/report "$MAIL_MSG"


if [ "$debug" != "x" ]; then 
   rm -rf $WORK_DIR
fi

exit 1

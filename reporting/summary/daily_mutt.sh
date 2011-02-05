#!/usr/bin/env bash

WEBLOC="http://gratia-osg.fnal.gov:8880/gratia-reporting"
SUM_WEBLOC="http://gratia-osg.fnal.gov:8884/gratia-reporting"
VOREPORT_CONFIG="voreports.debug.config"
[ -z $INSTALL_DIR ] && INSTALL_DIR=`python getConfigInfo.py|grep installDir|cut -d' ' -f2-` && [ `echo $INSTALL_DIR|wc -w` -gt 1 ] && echo $INSTALL_DIR && exit 1
export INSTALL_DIR

while test "x$1" != "x"; do
   if [ "$1" == "--help" ]; then 
	echo "usage: $0 [--grid gridType ] [--debug] [--mail|--email email] [quoted_string_representing_starting_date (as accepted by date -d)]"
	exit 1
   elif [ "$1" == "--debug" ]; then
	debug=x
	shift
   elif [ "$1" == "--grid" ]; then
	gridOption="--grid=$2"
	shift
        shift
   elif [ "$1" == "--dry-run" ]; then
	dryrun=yes
	shift
   elif [ "$1" == "--mail" -o "$1" == "--email" ]; then
	RECIPIENT=$2
	shift
	shift
   else 
        date_arg=$1
	shift
   fi
done

[ -z $RECIPIENT ] && RECIPIENT=`python $INSTALL_DIR/getConfigInfo.py|grep "1to"|cut -d' ' -f2-` && [ `echo $RECIPIENT|wc -w` -gt 1 ] && echo $RECIPIENT && exit 1
[ -z $RECIPIENT ] && echo -e -n "Warning!!! No recipient email has been provided either from command line or in the config file. The reports will be printed to the screen.\\nContinue? (y/n): " && read input && [ $input != "y" ] && echo exiting... && exit 0

when=$(date -d "${date_arg:-yesterday}" +"%d %B %Y")
whenarg=$(date -d "${date_arg:-yesterday}" +"%Y/%m/%d")

MAIL_MSG="Report from the job level Gratia db for $when"
TR_MAIL_MSG="Data transfer report summary by site for $when"
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

function sendtohtml {
    cmd=$1
    rep_args=$2
    txtfile=$3.txt
    csvfile=$3.csv
    htmlfile=$3.html
    subject="$4"
    to="$5"

    local whenarg="${ExtraArgs#--}"
    whenarg=${whenarg:-all}

    newto=$(sed -ne 's/^[ 	]*'"`basename $cmd`"'[ 	]\{1,\}'"${ExtraArgs#--}"'[ 	]\{1,\}\(.*\)$/\1/p' \
            $INSTALL_DIR/reportType.config | sed -e 's/\b\default\b/'"$to"'/' | head -1) 
    to=${newto:-$to}

    if [ "$dryrun" != "yes" ]; then 
       $cmd "--subject=$subject" --emailto=$to --output=all $gridOption $rep_args
    else
       echo $cmd \"--subject=$subject\" --emailto=$to --output=all $gridOption $rep_args
       $cmd  --output=all $gridOption $rep_args
    fi
    return   
}

rm -f daily.check

sendtohtml $INSTALL_DIR/daily    $whenarg ${WORK_DIR}/report "$MAIL_MSG" $RECIPIENT
sendtohtml $INSTALL_DIR/newUsers $whenarg ${WORK_DIR}/report "New users on OSG ($when)" $RECIPIENT
sendtohtml $INSTALL_DIR/dailyStatus $whenarg ${WORK_DIR}/report "$STATUS_MAIL_MSG" $RECIPIENT
sendtohtml "$INSTALL_DIR/dailyStatus --groupby=VO" $whenarg ${WORK_DIR}/report "$VO_STATUS_MAIL_MSG" $RECIPIENT
sendtohtml "$INSTALL_DIR/dailyStatus --groupby=Both" $whenarg ${WORK_DIR}/report "$BOTH_STATUS_MAIL_MSG" $RECIPIENT
sendtohtml $INSTALL_DIR/transfer $whenarg ${WORK_DIR}/report "$TR_MAIL_MSG" $RECIPIENT

echo "`python $INSTALL_DIR/getConfigInfo.py|grep voemaillist|cut -d' ' -f2-`" | while read line; do
    MYVO=`echo $line | cut -d\  -f1`
    export RECIPIENT=`echo $line | cut -d\  -f2- | sed -e "s: :,:g" `
    sendtohtml "$INSTALL_DIR/dailyForVO --voname=${MYVO}" $whenarg ${WORK_DIR}/forvo "${VO_MAIL_MSG}${MYVO}" $RECIPIENT
done 

if [ "$debug" != "x" ]; then 
   rm -rf $WORK_DIR
fi

exit 1

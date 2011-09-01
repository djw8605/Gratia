#!/bin/bash

WEBLOC="http://gratia-osg-prod-reports.opensciencegrid.org/gratia-reporting"
SUM_WEBLOC="http://gratia-osg-daily-reports.opensciencegrid.org/gratia-reporting"

ExtraHeader=""
ExtraArgs=--daily

cmdFile=`(cd \`dirname $0\`; /bin/pwd)`/range_mutt_nightly.sh

function capitalizeFirstLetter {
echo $(echo $1 | python -c "print raw_input().capitalize()")
}

while test "x$1" != "x"; do
    if [ "$1" == "--help" ]; then 
        echo "usage: $0 [--grid gridType ] [--dry-run] [--debug] [--mail email] [--draft] [quoted_string_representing_starting_date (as accepted by date -d)]"
        exit 1
    elif [ "$1" == "--debug" ]; then
        debug=yes
        shift
    elif [ "$1" == "--grid" ]; then
        gridOption="--grid=$2"
        shift
        shift
    elif [ "$1" == "--dry-run" ]; then
        dryrun=yes
        shift
    elif [ "$1" == "--draft" ]; then
        ExtraHeader="[Draft] ${ExtraHeader}"
        shift
    elif [ "$1" == "--mail" ]; then
        RECIPIENT=$2
        USER_RECIPIENT=$2
        shift
        shift
    elif [ "$1" == "--weekly" -o "$1" == "--monthly" ]; then
        ExtraArgs="$1"
        ExtraHeader="${ExtraHeader}"`capitalizeFirstLetter ${1/--/}`
        shift
    elif [[ "$1" == --cmd[Ff]ile ]]; then
        cmdFile=$2
        shift
        shift
    elif [ "$1" == "--config" ]; then
        ExtraArgs="$ExtraArgs --config=$2"
        shift; shift
    else 
        date_arg=$1
        shift
    fi
done

[ -z $RECIPIENT ] && RECIPIENT=`python getConfigInfo.py|grep "1to"|cut -d' ' -f2-` && [ `echo $RECIPIENT|wc -w` -gt 1 ] && echo $RECIPIENT && exit 1
[ -z $USER_RECIPIENT ] && USER_RECIPIENT=`python getConfigInfo.py|grep "2to"|cut -d' ' -f2-` && [ -z $USER_RECIPIENT ] && USER_RECIPIENT=$RECIPIENT
[ -z $RECIPIENT ] && echo -e -n "Warning!!! No recipient email has been provided either from command line or in the config file. The reports will be printed to the screen.\\nContinue? (y/n): " && read input && [ $input != "y" ] && echo exiting... && exit 0

when=$(date -d "${date_arg:-yesterday}" +"%d %B %Y")
whenarg=$(date -d "${date_arg:-yesterday}" +"%Y/%m/%d")

MAIL_MSG="${ExtraHeader}Report from the job level Gratia db for $when"
TR_MAIL_MSG="${ExtraHeader}Data transfer summary report by site for $when"
SUM_MAIL_MSG="${ExtraHeader}Report from the daily summary Gratia db for $when"
STATUS_MAIL_MSG="${ExtraHeader}Job Success Rate for $when (from the job level Gratia db)"
REPORTING_MAIL_MSG="${ExtraHeader}Summary on how sites are reporting to Gratia for $when"
LONGJOBS_MAIL_MSG="${ExtraHeader}Report of jobs longer than 7 days for $when"
USER_MAIL_MSG="${ExtraHeader}Report by user for $when"

# Transfer the file now
WORK_DIR=`mktemp -d "${TMPDIR:-/tmp}/range_mutt.workdir.XXXXXXXXXX"`
if [ "$debug" != "yes" ]; then 
  trap "[[ -d \"$WORK_DIR\" ]] && rm -rf \"$WORK_DIR\" 2>/dev/null" EXIT
fi

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

    additionalRecipients=`python getConfigInfo.py|grep additionalRecipients|cut -d' ' -f2-`
    if [ "$additionalRecipients" != "" ]; then
        newto=$(echo "$additionalRecipients"|sed -ne 's/^[      ]*'"`basename $cmd`"'[  ]\{1,\}'"${ExtraArgs#--}"'[     ]\{1,\}\(.*\)$/\1/p' \
                | sed -e 's/\b\default\b/'"$to"'/' | head -1)
        to="\"${newto:-$to}\""
        to=${to/ /,}
    fi

    #echo "See $WEBLOC for more information" > $txtfile
    #echo "For more information see: <a href=$WEBLOC>$WEBLOC</a>" > $htmlfile
    if [ "$dryrun" != "yes" ]; then 
       $cmd "--subject=$subject" --emailto=$to --output=all $gridOption $rep_args
    else
       echo $cmd \"--subject=$subject\" --emailto=$to --output=all $gridOption $rep_args
       $cmd  --output=all $gridOption $rep_args
    fi
    return   
}


rm -f "$WORK_DIR/range.check"

. $cmdFile || { echo "Execution of commands $cmdFile failed" 1>&2; exit 1; }

exit 0

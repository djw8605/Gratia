#!/bin/bash -x

# space separated list of mail recipients
PROD_MAILTO="osg-accounting-info@fnal.gov"
PROD_USER_MAILTO="osg-accounting-info@opensciencegrid.org"
MAILTO="pcanal@fnal.gov"
USER_MAILTO=$MAILTO
WEBLOC="http://gratia-osg.fnal.gov:8880/gratia-reporting"
SUM_WEBLOC="http://gratia-osg.fnal.gov:8884/gratia-reporting"

ExtraHeader=
ExtraArgs=--daily
(( mailOverride = 0 ))
(( production = 0 ))

cmdFile=`(cd \`dirname $0\`; /bin/pwd)`/range_mutt_nightly.sh

while test "x$1" != "x"; do
    if [ "$1" == "--help" ]; then 
        echo "usage: $0 [--grid gridType ] [--dry-run] [--debug] [--mail email] [--draft] [--production] [quoted_string_representing_starting_date (as accepted by date -d)]"
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
        MAILTO=$PROD_MAILTO
        USER_MAILTO=$PROD_USER_MAILTO
        (( production = 1 ))
        shift
    elif [ "$1" == "--production" ]; then
        MAILTO=$PROD_MAILTO
        USER_MAILTO=$PROD_USER_MAILTO
        (( production = 1 ))
        shift
    elif [ "$1" == "--mail" ]; then
        MAILTO=$2
        USER_MAILTO=$2
        (( mailOverride = 1 ))
        shift
        shift
    elif [ "$1" == "--weekly" ]; then
        ExtraArgs="$ExtraArgs $1"
        ExtraHeader="${ExtraHeader}Weekly "
        shift
    elif [ "$1" == "--monthly" ]; then
        ExtraArgs="$ExtraArgs $1"
        ExtraHeader="${ExtraHeader}Monthly "
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

function sendto {

    cmd=$1
    rep_args=$2
    txtfile=$3.txt
    csvfile=$3.csv
    subject="$4"
    to="$5"

    local whenarg="${ExtraArgs#--}"
    whenarg=${whenarg:-all}

    if (( $mailOverride == 0 )) && (( $production > 0 )); then
      newto=$(sed -ne 's/^[ 	]*'"`basename $cmd`"'[ 	]\{1,\}'"${ExtraArgs#--}"'[ 	]\{1,\}\(.*\)$/\1/p' \
              reportMail.config | sed -e 's/\b\default\b/'"$to"'/' | head -1) 
      to=${newto:-$to}
    fi
    echo "See $WEBLOC for more information" > $txtfile
    echo >> $txtfile
    eval $1 $gridOption --output=text $rep_args >> $txtfile

    echo "$subject" >> "$WORK_DIR/range.check"
    grep 'All ' $txtfile >> "$WORK_DIR/range.check"

    echo "For more information see:,$WEBLOC" > $csvfile
    echo >> $csvfile
    eval $1 --output=csv $rep_args >>  $csvfile
    
    if [ "$dryrun" != "yes" ]; then 
       mutt -F ./muttrc -a $csvfile -s "$subject" $to < $txtfile
    else 
       echo mutt -F ./muttrc -a $csvfile -s "$subject" $to < $txtfile
    fi
   
}

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

    if (( $mailOverride == 0 )) && (( $production > 0 )); then
      newto=$(sed -ne 's/^[ 	]*'"`basename $cmd`"'[ 	]\{1,\}'"${ExtraArgs#--}"'[ 	]\{1,\}\(.*\)$/\1/p' \
              reportMail.config | sed -e 's/\b\default\b/'"$to"'/' | head -1) 
      to=${newto:-$to}
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

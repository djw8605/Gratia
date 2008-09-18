#!/usr/bin/env bash
###########################################################################
# 06/30/2008 (John Weigand)
#
# Query to determine if a collector is receiving metric records when
# it is not supposed to.
# 
# Refer to the usage function for more details.
###########################################################################
function logerr {
  echo;echo "ERROR: $1";usage;exit 1
}
#------------------
function send_mail {
  msg="\
This is an automated message from a script most likely running from cron.

Cron host...... $(hostname -f)
Cron user...... $(whoami)
Script......... $PGM

Database node.. $DBNODE
Schema......... $SCHEMA

This script checks to see if there are any metric records being
reported to this database's Gratia collector.  Metric probe data is sometimes 
erroneously sent to the wrong collector due incorrect configurations of the 
Probe configuration files.  This script is intended for use on those collectors
(as a cron job) to determine if this is occurring.

This following metric records were detected as being reported:
$(cat $tmpfile)

These sites should be contacted to direct their probes to the correct
collector.
"
  
  if [ -z "$MAILTO" ];then
    echo;echo "$msg"
  else
    subject="Error in Metric Probe Configuration affecting ${DBNODE}:$SCHEMA"
    mail -s "$subject" $MAILTO <<EOF


$msg
EOF
  fi
}

#-------
function usage {
  echo "
Usage: $0 --schema database_schema --dbhost database_host --port port 
         [--date YYYY-MM-DD] [--mail email] [--help]

    --schema  The database schema to query (required)
    --dbhost  The host (full) of the database host (required)
    --port    Port number of the dbms instance  (required)
    --date    Optional: You can specify the last date (YYYY-MM-DD) to check for
              metric records. Do not use this if running as a cron process.
    --mail    Optional: Specifies the email adddress(es) to be sent an email
              notice if metric records have been added to the database
              since the '--date' argument value.
              If not specified, output is sent to stdout
    --help    provides usage
    
   Metric probe data is sometimes erroneously sent to the wrong collector due 
   incorrect configurations of the Probe configuration files.  This script is 
   intended for use on those collectors (as a cron job) to determine if this 
   is occurring.

   This script queries the database to see if any metric data entries exist
   since a certain date (minus 1 day)  and will send an email notification 
   (--email argument) if detected.
   
   It should be cron scheduled to run daily so the condition is resolved
   quickly.

   Email (--email argument) will be sent if any metric records are detected.
   If no --email option is specified, it will display the results to stdout.

   If not running in cron mode, you can specify a date argument in YYYY-MM-DD
   format and it will use that in the query.  No validation is performed on 
   the date argument.

   When running as a cron process, a file will be created that specfiies
   the last time this process has run:
     ${lasttime_file}-<DB_SCHEMA>
   If the file does not exist, the script will default to current date on its 
   first execution and then use that file in subsequent daily runs. This is to 
   insure no more than one mail message per day is sent until the problem 
   is resolved.

   The query used is:
     select SiteName, ProbeName, 
       min(ServerDate) as "Starting at",
       max(ServerDate) as "Ending at",
       count(*) as "Records"
     from MetricRecord_Meta 
     where ServerDate >= DATE_SUB("$LAST_DATE",INTERVAL $INTERVAL DAY) 
     group by SiteName, ProbeName 
"
}
### MAIN ###########################################################
PGM=$(basename $0)
tmpfile=/tmp/$PGM.$$
lasttime_file=$0.lasttime
date_arg=no
CURRENT_DATE="$(date '+%Y-%m-%d %H:%M:%S')"
LAST_DATE="$CURRENT_DATE"
INTERVAL=0


#--------------------------------------------
# space separated list of default mail recipients
HELP=""
MAILTO=""
SCHEMA=""
DBNODE=""
PORT=""

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


#--- validate any command line arguments
while test "x$1" != "x"; do
   if [ "$1" == "--help" ]; then 
	HELP="yes"
	shift
   elif [ "$1" == "--mail" ]; then
	MAILTO="$2"
	shift
	shift
   elif [ "$1" == "--schema" ]; then
	SCHEMA=$2
   	shift
   	shift
   elif [ "$1" == "--dbhost" ]; then
	DBNODE=$2
   	shift
   	shift
   elif [ "$1" == "--port" ]; then
	PORT=$2
   	shift
   	shift
   elif [ "$1" == "--date" ]; then
	LAST_DATE="$2"
	date_arg=yes
   	shift
   	shift
   else 
        logerr "Invalid command line argument"
        usage
        exit 1
   fi
done

if [ -n "$HELP" ];then
  usage;exit 1
fi
if [ -z "$SCHEMA" ];then
  logerr "--schema is a required argument"
fi
if [ -z "$DBNODE" ];then
  logerr "--host is a required argument"
fi
if [ -z "$PORT" ];then
  logerr "--port is a required argument"
fi

lasttime_file=${lasttime_file}-$SCHEMA

case $date_arg in
  "yes" ) LAST_DATE="$LAST_DATE 00:00:00" 
          ;;
  "no"  ) if [ -r "$lasttime_file" ];then
            LAST_DATE="$(cat $lasttime_file)"
          else
            INTERVAL=0
          fi
          ;;
     *  ) logerr "Program error: date_arg variable should be yes/no"
esac

#--- perform the query ----
mysql --table --host=$DBNODE --port=$PORT -u reader -preader $SCHEMA >$tmpfile <<EOF
select SiteName,
       ProbeName, 
       min(ServerDate) as "Starting at",
       max(ServerDate) as "Ending at",
       count(*) as "Records"
from MetricRecord_Meta 
where ServerDate >= DATE_SUB("$LAST_DATE",INTERVAL $INTERVAL DAY) 
group by SiteName,
         ProbeName 
EOF

#-- see if mail should be sent ---
if [ -s $tmpfile ];then
  send_mail 
fi

#--- cleanup
rm -f $tmpfile

#--- set the last date this script ran ---
echo $CURRENT_DATE >$lasttime_file 

exit 0

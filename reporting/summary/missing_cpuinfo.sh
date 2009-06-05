#!/usr/bin/env bash
###########################################################################
# 06/30/2008 (John Weigand)
#
# Query to find any JobUsageRecords entries with a HostDecription missing
# in the CPUInfo table.
# 
# Refer to the usage function for more details.
###########################################################################
function logerr {
  echo;echo "ERROR: $1";usage;exit 1
}
#------------------
function send_mail {
  msg="\
Cron host...... $(hostname -f)
Cron user...... $(whoami)
Script......... $PGM

Database node.. $DBNODE
Schema......... $SCHEMA

This script checks to see if there are any JobUsageRecords whose
HostDescription records have not been defined in the CPUInfo table.

This following HostDescriptions were  detected:
$(cat $tmpfile)

These should be reviewed and a Specint2000 benchmark value set.
"
  
  if [ -z "$MAILTO" ];then
    echo;echo "$msg"
  else
    subject="${SCHEMA}: CPUInfo HostDescriptions missing entries"
    mail -s "$subject" $MAILTO <<EOF

This is an automated message from a script most likely running from cron.

$msg
EOF
  fi
}

#-------
function usage {
  echo "
Usage: $0 --schema database_schema --dbhost database_host --port port 
         [--mail email] [--help]

    --schema  The database schema to query (required)
    --dbhost  The host (full) of the database host (required)
    --port    Port number of the dbms instance  (required)

    --mail    Specifies the email adddress(es) to be sent an email
              notice if HostDescriptions are not in the CPUInfo table.
              (optional)
    --help    provides usage
    
   This script queries to find any JobUsageRecords entries with a 
   HostDecription missing in the CPUInfo table.
   
   It is primarily intended for use against the gratia_psacct database
   but can be run for others.
  
   It should probably be cron scheduled to run weekly.
  
   It currently needs to be run on a node that has a ups version of mysql.
   It also makes some assumptions about the current environment.

   Email (--email argument) will be sent if any updates to CPUInfo are 
   required.  If no --email option is specified it will display the
   results to stdout.

"
}
### MAIN ###########################################################
PGM=$(basename $0)
tmpfile=/tmp/$PGM.$$


#--------------------------------------------
# space separated list of default mail recipients
HELP=""
MAILTO=""
SCHEMA=""
DBNODE=""
PORT=""

#--- source ups for setup script for mysql ----
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


#--- perform the query ----
mysql --skip-column-names --host=$DBNODE --port=$PORT -u reader -preader $SCHEMA >$tmpfile <<EOF
select distinct(j.HostDescription)
from JobUsageRecord j
where j.HostDescription IS NOT NULL
  and NOT EXISTS (select c.HostDescription from CPUInfo c where j.HostDescription = c.HostDescription)
EOF

if [ -s $tmpfile ];then
  send_mail 
fi

rm -f $tmpfile

exit 0

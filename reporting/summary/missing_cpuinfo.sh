#!/usr/bin/env bash
###########################################################################
# Query to find any JobUsageRecords entries with a HostDecription missing
# in the CPUInfo table.
# 
# This is primarily intended for use against the gratia_psacct database
# but can be run for others.
#
# It should probably be cron scheduled to run weekly.
#
# It currently needs to be run on a node that has a ups version of mysql.
# It also makes some assumptions about the current environment.
###########################################################################
function send_mail {
    cmd=$1
    when=$2
    txtfile=$3.txt
    csvfile=$3.csv
    subject="${SCHEMA}: CPUInfo HostDescriptions missing entries"
    mail -s "$subject" $MAILTO <<EOF
This is an automated message running from cron. 

Cron host: $(hostname -f)
Cron user: $(whoami)

Database node: $DBNODE
Schema: $SCHEMA

This script checks to see if there are any JobUsageRecords whose
HostDescription records have not been defined in the CPUInfo table.

This following were  detected:
$(cat $tmpfile)

These should be reviewed and a Specint2000 benchmark value set.

EOF
}
#-------
function usage {
  echo "
Usage: $0 [--mail email] [--schema database_schema] [--dbhost database_host]

    --mail    default: $MAILTO
    --schema  default: $SCHEMA
    --dbhost  default: $DBNODE

"
}
### MAIN ###########################################################
PGM=$(basename $0)
tmpfile=/tmp/$PGM.$$
echo HI


#--------------------------------------------
# space separated list of default mail recipients
MAILTO="osg-operation@opensciencegrid.org"
SCHEMA=gratia_psacct
DBNODE="gratia-db01.fnal.gov"

#--- source ups for setup script for mysql ----
setups=/fnal/ups/etc/setups.sh
if [ ! -f $setups ];then
  echo  "UPS setups.sh ($setups) script not available"
  exit 1
fi
source $setups

#--- setup mysql ----
setup mysql 2>/dev/null
if [ "$(type mysql 1>/dev/null 2>&1;echo $?)" != "0" ];then
  echo "MySql client not available.  This script assumes it is
available via Fermi UPS in $setups"
  exit 1
fi


#--- validate any command line arguments
while test "x$1" != "x"; do
   if [ "$1" == "--help" ]; then 
	usage
	exit 1
   elif [ "$1" == "--mail" ]; then
	MAILTO="$2"
	shift
	shift
   elif [ "$1" == "--schema" ]; then
	SCHEMA=$1
   	shift
   	shift
   elif [ "$1" == "--dbhost" ]; then
	DB_NODE=$1
   	shift
   	shift
   else 
        echo "ERROR: Invalid command line argument"
        usage
        exit 1
   fi
done


#--- perform the query ----
mysql --skip-column-names --host=$DBNODE --port=3320 -u reader -preader $SCHEMA >$tmpfile <<EOF
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

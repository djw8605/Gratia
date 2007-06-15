#!/bin/bash
#
# Create the mysql gratia summary tables, triggers and stored procedures
# Called automatically by the services.
#

if (( $# == 0 )); then
  set -- "stored"
elif [[ "$*" == *all* ]]; then
  set -- "stored" "summary" "trigger"
fi

while [[ -n "$1" ]]; do
  action="$1"
  shift
  case $action in
			*summary*)
				proc="build-summary-tables.sql"
			;;
			*stored*)
		    proc="build-stored-procedures.sql"
			;;
			*trigger*)
			  proc="build-trigger.sql"
			;;
			*)
			  echo "Unrecognized action \"$action\"" 1>&2
        exit 1
	esac

	mysql -B --force -unbuffered --user=root --password=ROOTPASS  --host=localhost --port=PORT gratia < MAGIC_VDT_LOCATION/tomcat/v55/gratia/${proc}
done

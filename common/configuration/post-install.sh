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
			summary)
	      proc="build-summary-tables.sql"
        set -- "$@" summary_view
        ;;
      stored)
        proc="build-stored-procedures.sql"
        ;;
      trigger)
        proc="build-trigger.sql"
        ;;
      ps)
        proc="build-ps-node-summary-table.sql"
        ;;
      summary_view)
        proc="build-summary-view.sql"
        ;;
      *)
        echo "Unrecognized action \"$action\"" 1>&2
        exit 1
  esac

  printf "post-install.sh: loading $proc ... "

  CMD_PREAMBLE
  cat ${VDT_LOCATION}/tomcat/v55/gratia/${proc} | CMD_PREFIX ${VDT_LOCATION}/mysql5/bin/mysql -B --unbuffered --user=root --password=ROOTPASS --host=localhost --port=PORT gratia CMD_SUFFIX
  status=$?
  if (( $status != 0 )); then
    echo "FAILED with status $status" 1>&2
    exit $status
  else
    echo "OK"
  fi
done

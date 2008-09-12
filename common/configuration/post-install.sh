#!/bin/bash
#
# Create the mysql gratia summary tables, triggers and stored procedures
# Called automatically by the services.
#
script_location=${VDT_LOCATION}/tomcat/v55/gratia/

TMP=${TMPDIR:-/tmp}/post-install.sh.$$
trap "rm $TMP* 2>/dev/null" EXIT

if grep -e 'org\.hibernate\.dialect\.MySQLInnoDBDialect' \
   "${script_location}/hibernate.cfg.xml" \
   >/dev/null 2>&1; then
  is_innodb=1
fi

function preprocess_proc() {
    if [[ -z "$is_innodb" ]]; then
        TPROC=`mktemp "$TMP.preprocess.XXXXXXXXXX"`
        if [[ -z "$TPROC" ]]; then
            echo "Unable to create temporary file \"$TMP.preprocess.XXXXXXXXXX\"" 1>&2
            exit 1
        fi
        sed -e 's/[ 	]*ENGINE[ 	]*=[ 	]*'"'"'innodb'"'"'//gi' "$proc" > "$TPROC"
        proc="$TPROC"
     fi
}

# This would be so much easier if we could guarantee a "drop trigger if
# exists" syntax.
function prepareCountTrigger() {
  local table_name=${1##countTrigger}
  TPROC="${TMP}.countTrigger.${table_name}"
  if [[ "${table_name}" == "DupRecord" ]]; then
    maybe_increment_error_line="  update TableStatistics set nRecords = nRecords + 1 where RecordType = new.RecordType and Qualifier = new.error;"
    maybe_decrement_error_line=${maybe_increment_error_line/\+/-}
    maybe_decrement_error_line=${maybe_decrement_error_line//new/old}
    maybe_increment_error_line="  insert ignore into TableStatistics values(new.RecordType, 0, new.error);${maybe_increment_error_line}"
    maybe_decrement_error_line="  insert ignore into TableStatistics values(old.RecordType, 1, old.error);${maybe_decrement_error_line}"
  fi
  cat > "${TPROC}" <<EOF
delimiter ||
drop procedure if exists conditional_trigger_drop;
create procedure conditional_trigger_drop()
begin
  declare mycount int;

-- Old trigger
  select count(*) into mycount from information_schema.triggers where
    trigger_schema = Database()
    and event_object_table = 'JobUsageRecord'
    and trigger_name = 'trigger01';

  if mycount > 0 then
    drop trigger trigger01;
  end if;

-- Previous increment trigger
  select count(*) into mycount from information_schema.triggers where
    trigger_schema = Database()
    and event_object_table = '${table_name}'
    and trigger_name = 'countInc${table_name}';

  if mycount > 0 then
    drop trigger countInc${table_name};
  end if;

-- Previous decrement trigger
  select count(*) into mycount from information_schema.triggers where
    trigger_schema = Database()
    and event_object_table = '${table_name}'
    and trigger_name = 'countDec${table_name}';

  if mycount > 0 then
    drop trigger countDec${table_name};
  end if;
end
||
call conditional_trigger_drop();
||
create trigger countInc${table_name} after insert on ${table_name}
for each row
f:begin
  insert ignore into TableStatistics values('${table_name}',0,'');
  update TableStatistics set nRecords = nRecords + 1 where RecordType = '${table_name}' and Qualifier = '';
${maybe_increment_error_line}
end
||
create trigger countDec${table_name} after delete on ${table_name}
for each row
f:begin
  insert ignore into TableStatistics values('${table_name}',1,'');
  update TableStatistics set nRecords = nRecords - 1 where RecordType = '${table_name}' and Qualifier = '';
${maybe_decrement_error_line}
end
EOF

  proc=$TPROC
}

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
#        proc="${script_location}build-summary-tables.sql"
        set -- "$@" summary-view
        # build-summary-tables.sql removed.
        echo "argument \"summary\" obsolete -- ignored" 1>&2
        continue
        ;;
      stored)
        proc="${script_location}build-stored-procedures.sql"
        set -- "$@" stored-extra-1 stored-extra-2 static-reports
        if [[ `hostname -f` == *.fnal.gov ]]; then
          set -- "$@" proc-edit-permission
        fi
        ;;
      stored-extra-1)
        # Hand-tweaked procedure (temporary)
        proc="${script_location}WeeklyUsageByVORanked.sql"
        ;;
      stored-extra-2)
        # Hand-tweaked procedure (temporary)
        proc="${script_location}SiteUsageCountByVO.sql"
        ;;
      stored-extra-3)
        # Hand-tweaked procedure (temporary)
        proc="${script_location}dCacheSimple.sql"
        ;;
      static-reports)
        # For CSV static reports
        proc="${script_location}static-report-procedures.sql"
        ;;
      trigger)
        proc="${script_location}build-trigger.sql"
        set -- "$@" summary-procedures
        ;;
      summary-procedures)
        proc="${script_location}summary-procedures.sql"
        ;;
      ps)
#        proc="${script_location}build-ps-node-summary-table.sql"
        echo "argument \"ps\" obsolete -- ignored" 1>&2
        continue;
        ;;
      summary[-_]view)
        proc="${script_location}build-summary-view.sql"
        ;;
      countTrigger*)
        prepareCountTrigger $action
        ;;
      proc-edit-permission)
        proc="${script_location}proc-edit-permission.sql"
        ;;
      *)
        echo "Unrecognized action \"$action\"" 1>&2
        exit 1
  esac

  CMD_PREAMBLE
  if [[ -r "${proc}" ]]; then
    printf "post-install.sh: loading $proc ... "
    preprocess_proc
    cat ${proc} | CMD_PREFIX ${VDT_LOCATION}/mysql5/bin/mysql -B --unbuffered --user=root --password=ROOTPASS --host=localhost --port=PORT gratia CMD_SUFFIX
    status=$?
  else
    echo "$proc is not readable!" 1>&2
    status=1
  fi
  if (( $status != 0 )); then
    echo "FAILED with status $status" 1>&2
    exit $status
  else
    echo "OK"
  fi
done

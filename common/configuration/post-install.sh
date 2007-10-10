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
        sed -e 's/ ENGINE='"'"'innodb'"'"'/g' "$proc" > "$TPROC"
        proc="$TPROC"
     fi
}

# This would be so much easier if we could guarantee a "drop trigger if
# exists" syntax.
function prepareCountTrigger() {
  local table_name=${1##countTrigger}
  cat > "${TMP}.countTrigger.${table_name}" <<EOF
delimiter ||
drop procedure if exists conditional_trigger_drop;
create procedure conditional_trigger_drop()
begin
  declare mycount int;
  select count(*) into mycount from information_schema.triggers where
    trigger_schema = Database()
    and event_object_table = '${table_name}'
    and trigger_name = 'countInc${table_name}';

  if mycount > 0 then
    drop trigger countInc${table_name};
  end if;

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
  update TableStatistics set nRecords = nRecords + 1 where RecordType = '${table_name}';
end
||
create trigger countDec${table_name} after delete on ${table_name}
for each row
f:begin
  update TableStatistics set nRecords = nRecords - 1 where RecordType = '${table_name}';
end
EOF

  proc=$TMP
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
        proc="${script_location}build-summary-tables.sql"
        set -- "$@" summary-view
        ;;
      stored)
        proc="${script_location}build-stored-procedures.sql"
        ;;
      trigger)
        proc="${script_location}build-trigger.sql"
        ;;
      ps)
        proc="${script_location}build-ps-node-summary-table.sql"
        ;;
      summary[-_]view)
        proc="${script_location}build-summary-view.sql"
        ;;
      countTrigger*)
        prepareCountTrigger $action
        ;;
      *)
        echo "Unrecognized action \"$action\"" 1>&2
        exit 1
  esac

  printf "post-install.sh: loading $proc ... "

  CMD_PREAMBLE
  if [[ -r "${proc}" ]]; then
    preprocess_proc
    cat ${proc} | CMD_PREFIX ${VDT_LOCATION}/mysql5/bin/mysql -B --unbuffered --user=root --password=ROOTPASS --host=localhost --port=PORT gratia CMD_SUFFIX
    status=$?
  else
    status=1
  fi
  if (( $status != 0 )); then
    echo "FAILED with status $status" 1>&2
    exit $status
  else
    echo "OK"
  fi
done

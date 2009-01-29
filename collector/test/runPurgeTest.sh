#!/bin/sh

function usage {
cat 1>&2 <<EOF
usage: runPurgeTest.sh [-h] [-l] [-d] [-c] [-p port_number]
   -h print this help
   -d reset the schema
   -c reinstall the collector
   -p use this port for the main collector (default 9000)
   -l load the data
   -f execute fix ups
   -w [filename] upload the war file
   -s stop server
   -t test content
EOF
}

http_port=`expr 8000 + ${UID}`


dbhost=gratia-vm02.fnal.gov
dbport=3320
webhost=gratia-vm02.fnal.gov

# Need obfuscation
update_password=proto
reader_password=reader
pass=lisp01

schema_name=gratia_purge_${USER}
tomcatpwd=/data/tomcat-${schema_name}

host=`hostname`
source=${PWD}
source=`dirname $source`
source=`dirname $source`
export PYTHONPATH=${source}/probe/common:${source}/probe/metric:${PYTHONPATH}

server_status=unknown

function stop_server {
  if [ "${server_status}" != "stopped" ]; then 
     echo "Stopping server ${webhost}:${http_port}"
     alive=`ssh ${webhost} netstat -l | grep ${http_port} | wc -l`
     if [ ${alive} -eq 1 ]; then 
       ssh -l root ${webhost} service tomcat-${schema_name} stop 2>&1 | head -3 
       server_status=stopping
       wait_for_server_shutdown 
     fi
     server_status=stopped
  fi
}

function start_server {
  if [ "${server_status}" = "unknown" -o "${server_status}" = "stopped"  ]; then 
     echo "Starting server ${webhost}:${http_port}"
     ssh -l root ${webhost} service tomcat-${schema_name} start
     server_status=starting
#     sleep 7
     server_status=started
  fi
}

function restart_server {
  stop_server
  start_server
}

function write_ProbeConfig {
   cat > ProbeConfig <<EOF
<ProbeConfiguration 
    UseSSL="0" 

    UseGratiaCertificates="0"

    SSLHost="${webhost}:8443" 
    SSLCollectorService="/gratia-servlets/rmi"
    SSLRegistrationHost="${webhost}:${http_port}"
    SSLRegistrationService="/gratia-security/security"

    GratiaCertificateFile="gratia.hostcert.pem"
    GratiaKeyFile="gratia.hostkey.pem"

    SOAPHost="${webhost}:${http_port}" 
    CollectorService="/gratia-servlets/rmi" 
    UseSoapProtocol="0"
    
    MeterName="LocalTester" 
    SiteName="LocalTesting"
    Grid="OSG"
    
    LogLevel="2"
    DebugLevel="0" 
    GratiaExtension="gratia.xml"
    CertificateFile="/etc/grid-security/hostcert.pem"
    KeyFile="/etc/grid-security/hostkey.pem"

    VDTSetupFile="MAGIC_VDT_LOCATION/setup.sh"
    UserVOMapFile="MAGIC_VDT_LOCATION/monitoring/grid3-user-vo-map.txt"

    MaxPendingFiles="100000"
    DataFolder="MAGIC_VDT_LOCATION/gratia/var/data/"
    WorkingFolder="MAGIC_VDT_LOCATION/gratia/var/tmp"
    LogFolder="MAGIC_VDT_LOCATION/gratia/var/logs/"
    LogRotate="31"
    UseSyslog="0"
    SuppressUnknownVORecords="0"
    SuppressNoDNRecords="0"
    EnableProbe="0"

/>

EOF

}

function wait_for_server {

   write_ProbeConfig

   alive=0
   try=0
   while [ ${alive} -eq 0 -a ${try} -lt 10 ]; do   \
      echo "Waiting for server"
      python > alive.tmp <<EOF
import Gratia
Gratia.Initialize()
print Gratia.successfulHandshakes
EOF
      alive=`cat alive.tmp`
      rm alive.tmp
      try=`expr ${try} + 1`
      if [ ${alive} -ne 1 ]; then
         sleep 1
      fi
   done
   if [ ${try} -gt 9 ]; then
      echo "Error server is not started after 10 checks"
      exit
   fi
   server_status=started
}

function wait_for_server_shutdown {

   alive=1
   try=0
   while [ ${alive} -eq 1 -a ${try} -lt 10 ]; do   \
      echo "Waiting for server shutdown"
      alive=`ssh ${webhost} netstat -l | grep ${http_port} | wc -l`
      try=`expr ${try} + 1`
      if [ ${alive} -ne 0 ]; then
         sleep 1
      fi
   done
   if [ ${try} -gt 9 ]; then
      echo "Error server is not started after 10 checks"
      exit
   fi
}

function reset_database {

  stop_server

  ssh ${webhost} mysql -h ${dbhost} --port=${dbport} -u root --password=${pass}<<EOF 
drop database if exists ${schema_name};
EOF

  ssh ${webhost} mysql -h ${dbhost} --port=${dbport} -u root --password=${pass}<<EOF 
CREATE DATABASE ${schema_name};
GRANT ALL PRIVILEGES ON ${schema_name}.* TO 'gratia'@'${host}' IDENTIFIED BY '${update_password}';
GRANT ALL PRIVILEGES ON ${schema_name}.* TO 'gratia'@'localhost' IDENTIFIED BY '${update_password}';
GRANT ALL PRIVILEGES ON ${schema_name}.* TO 'gratia'@'${webhost}' IDENTIFIED BY '${update_password}';
GRANT SELECT,EXECUTE ON ${schema_name}.* TO 'reader'@'${host}' IDENTIFIED BY '${reader_password}';
GRANT SELECT,EXECUTE ON ${schema_name}.* TO 'reader'@'localhost' IDENTIFIED BY '${reader_password}';
GRANT SELECT,EXECUTE ON ${schema_name}.* TO 'reader'@'${webhost}' IDENTIFIED BY '${reader_password}';
EOF

#mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password}<<EOF 
#show databases;
#use ${schema_name};
#show tables;
#EOF
}

function reset_collector {

   filename=${PWD}/collector-for-test
   cat > $filename <<EOF
(
  ${schema_name} => { http_port => ${http_port},
             ssl_port => ${ssl_port},
             rmi_port => ${rmi_port},
             jmx_port => ${jmx_port},
             server_port => ${server_port},
             collector_host => "${webhost}",
             max_perm_size => "256m",
             max_heap_size => "1024m",
             db_host => "${dbhost}",
             db_schema => "${schema_name}",
             "properties.attributes" =>
             {
              "service.lifetime.JobUsageRecord" => "3 Months",
              "service.lifetime.JobUsageRecord.RawXML" => "1 Month",
              "service.lifetime.MetricRecord" => "36 months",
              "service.lifetime.MetricRecord.RawXML" => "1 month",
              "service.lifetime.DupRecord.Duplicates" => "1 month",
              "service.lifetime.DupRecord" => "UNLIMITED",
              "maintain.history.log" => 2,
              "monitor.listener.wait" => 240,
              "monitor.to.address0" => '${USER}@fnal.gov',
              "service.admin.DN.0" => 'ALLOW ALL',
              "service.secure.connection" => 'http://${webhost}:${ssl_port}'
             }
           }
);

### Local Variables:
### mode: cperl
### End:
EOF

   stop_server

   ssh -l root ${webhost} mkdir -p ${tomcatpwd} \; cp -rp /data/tomcat-install/\* ${tomcatpwd}\; mkdir -p ${tomcatpwd}/gratia\; chown -R ${USER} ${tomcatpwd}\; rm -f ${tomcatpwd}/logs/*

   ssh -l root ${webhost}  cd ${source}/common/configuration\; \
     ./update-gratia-local -s -S ${source} -d ${pass} -i ${filename} ${schema_name} \; \
     chown ${USER} ${tomcatpwd}/gratia/service-configuration.properties

   start_server
}


function fix_duplicate_date {
  expected_duplicate=23

  echo '0' > dups.count
  while [ `tail -1 dups.count` -lt ${expected_duplicate} ]; do
  echo "Waiting for Duplicate data to be loaded." `tail -1 dups.count`
  mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password} > dups.count 2>&1 <<EOF 
use ${schema_name};
select count(*) from DupRecord;
EOF
  if [ $? -ne 0 ]; then
     cat dups.count
     return
  fi
  sleep 2
  done

  echo "Attempt fix-up the DupRecord table so that we have a range of eventtime."

  mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password}<<EOF 
use ${schema_name};
update DupRecord set eventdate = date_sub(eventdate,interval dupid week) where Error = 'Parse';
update DupRecord set eventdate = date_sub(eventdate,interval dupid-12 week) where Error = 'Duplicate';
EOF
}

function fix_usage_server_date {

  expected_records=431

  echo '0' > records.count
  while [ `tail -1 records.count` -lt ${expected_records} ]; do
  echo "Waiting for Record data to be loaded." `tail -1 records.count` " out of ${expected_records}"
  mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password} > records.count 2>&1 <<EOF 
use ${schema_name};
select count(*) from JobUsageRecord;
EOF
  if [ $? -ne 0 ]; then
     cat records.count
     return
  fi
  sleep 2
  done

  echo "Attempt fix-up the JobUsageRecord table so that we have a range of ServerDate."

  mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password}<<EOF 
use ${schema_name};
update JobUsageRecord_Meta M, JobUsageRecord J set ServerDate = date_add(EndTime, interval 65 minute) where M.dbid = J.dbid;
EOF

}

function fix_metric_server_date {

  expected_records=400

  echo '0' > mrecords.count
  while [ `tail -1 mrecords.count` -lt ${expected_records} ]; do
  echo "Waiting for Metric Record data to be loaded." `tail -1 mrecords.count` " out of ${expected_records}"
  mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password} > mrecords.count 2>&1 <<EOF 
use ${schema_name};
select count(*) from MetricRecord;
EOF
  if [ $? -ne 0 ]; then
     cat mrecords.count
     return
  fi
  sleep 2
  done

  echo "Attempt fix-up the MetricRecord table so that we have a range of ServerDate."

  mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password}<<EOF 
use ${schema_name};
update MetricRecord_Meta M, MetricRecord J set ServerDate = date_add(Timestamp, interval 65 minute) where M.dbid = J.dbid;
EOF

}

function fix_server_date {
    fix_usage_server_date
    fix_metric_server_date
}

function loaddata {

   start_server

   write_ProbeConfig
   wait_for_server

   # First extend artificially the retention to let the old record in.
   echo "Turning off record purging"

   wget --dns-timeout=5 --connect-timeout=10 --read-timeout=40 -O - "http://${webhost}:${ssl_port}/gratia-administration/systemadministration.html?action=disableHousekeeping" 2>&1 | tee wget.full.log | grep House | grep DISABLED > wget.log
   result=$?
   if [ ${result} -ne 0 ]; then
      echo "Error: Turning off the record purging failed"
      exit 1
   fi

   echo "Sending data"

   tar xfz preparedrecords.tar.gz
   if [ "gratia-vm02.fnal.gov_9000" != "${webhost}_${http_port}" ]; then 
      find MAGIC_VDT_LOCATION/gratia/var/tmp/gratiafiles -type f -name '*'"gratia-vm02.fnal.gov_9000"'*' | while read file; do
         new_file=`echo "$file" | perl -wpe 's&\Q'"gratia-vm02.fnal.gov_9000.gratia.xml"'\E&'"${webhost}_${http_port}.gratia.xml"'&'`
         mv "$file" "$new_file"
      done
   fi

   python <<EOF
import Gratia
Gratia.Initialize()
EOF

   echo "Loading job usage record"
   python loaddata.py
   echo "Loading metric record"
   python loadmetric.py
}

function check_result {
   msg=$2
   stem=$1

   diff $1.ref $1.validate
   res=$?
   if [ ${res} -eq 0 ]; then
      echo ${msg} is OK.
   else
      echo ${msg} is INCORRECT.
      check_failed=1
   fi;
}


function turn_on_purging {

   # Restore the original (we could also set it to specific values)
   echo "Turning on record purging"
    #   ssh ${webhost} cd ${tomcatpwd}/gratia\; mv service-configuration.properties.auto.old service-configuration.properties \;
    #   restart_server

   start_server
   wait_for_server

   wget --dns-timeout=5 --connect-timeout=10 --read-timeout=40 -O - http://${webhost}:${ssl_port}/gratia-administration/systemadministration.html?action=startHousekeepingNow 2>&1 | tee wget.full.log | grep House | grep STOPPED > wget.log
   result=$?
   if [ ${result} -ne 1 ]; then
      echo "Error: Turning on the record purging failed"
      exit 1
   fi

   # Wait until the purge has been done
   sleep 3

   # Load a few old record to make sure they are caught by the filter.
   python loadstale.py

   sleep 2
   expected_count=0
   echo 99 > file.count
   while [ `tail -1 file.count` -gt ${expected_count} ]; do
      ssh ${webhost} ls ${tomcatpwd}/gratia/data/thread\? | wc -l > file.count
      echo "Waiting for all input msg to be used." `tail -1 file.count` left
      sleep 2
   done
}

function check_data {

   echo "Checking duplicates"
   mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password} > duplicate.validate 2>&1 <<EOF 
use ${schema_name};
select RecordType, error, count(*) from DupRecord group by RecordType, error;
EOF

   echo "Check JobUsageRecord"
   mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password} > jobusagerecord.validate 2>&1 <<EOF 
use ${schema_name};
select ProbeName, VOName, count(*) as Nrecord, Sum(NJobs) as NJobs, Sum(WallDuration) as Wall, Sum(CpuUserDuration+CpuSystemDuration) as Cpu 
    from JobUsageRecord_Report group by ProbeName, VOName;
EOF

   echo "Check JobUsageRecord_Xml"
   mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password} > jobusagerecordxml.validate 2>&1 <<EOF 
use ${schema_name};
select count(*),(ExtraXml!="" and not isnull(ExtraXml)) as hasExtraXml from JobUsageRecord_Xml group by hasExtraXml
EOF

   echo "Check MasterSummaryData"
   mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password} > jobsummary.validate 2>&1 <<EOF 
use ${schema_name};
select ProbeName, VOName, count(*) as Nrecord, Sum(NJobs) as NJobs, Sum(WallDuration) as Wall, Sum(CpuUserDuration+CpuSystemDuration) as Cpu 
    from VOProbeSummary group by ProbeName, VOName;
EOF

   echo "Check MetricRecord"
   mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password} > metricrecord.validate 2>&1 <<EOF 
use ${schema_name};
select ProbeName, MetricName, count(*) as Nrecord
    from MetricRecord J, MetricRecord_Meta M where J.dbid = M.dbid group by ProbeName, MetricName;
EOF

   echo "Check MetricRecord_Xml"
   mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password} > metricrecordxml.validate 2>&1 <<EOF 
use ${schema_name};
select count(*),(ExtraXml!="" and not isnull(ExtraXml)) as hasExtraXml from MetricRecord_Xml group by hasExtraXml
EOF

  check_result duplicate "Duplicate"
  check_result jobsummary "JobUsageRecord Summary Table"
  check_result jobusagerecord "JobUsageRecord"
  check_result jobusagerecordxml "JobUsageRecord's RawXml"
  check_result metricrecord "MetricRecord"
  check_result metricrecordxml "MetricRecord's RawXml" 
}

function upload_war()
{
    stop_server
    ssh -l root ${webhost} rm -rf ${tomcatpwd}/webapps/$WAR\*
    scp ../../target/$WAR.war ${webhost}:${tomcatpwd}/webapps
    start_server
}

#--- get command line args ----
while getopts :tshcfdlw:p: OPT; do
    case $OPT in
        w)  do_war=1
            WAR=$OPTARG
            ;;
        c)  do_collector=1
            ;;
        l)  do_load=1
            ;;
        d)  do_databasereset=1
            ;;
        f)  do_fixup=1
            ;;
        h)
            usage
            exit 1
            ;;
        p)  http_port=$OPTARG
            ;;
        t)  do_test=1
            ;;
        s)  do_stop=1
            ;;
        *)
            usage
            exit 1
            ;;
    esac
done
shift $[ OPTIND - 1 ]

ssl_port=`expr $http_port + 0`
rmi_port=`expr $http_port + 2`
jmx_port=`expr $http_port + 3`
server_port=`expr $http_port + 4`

if [ $do_stop ]; then
   stop_server
fi
if [ $do_databasereset ];then
   echo "Cleanup all database content"
   reset_database
fi

if [ $do_collector ]; then 
   reset_collector
fi

if [ $do_war ]; then
   upload_war
fi

if [ $do_load ]; then
   loaddata
fi

if [ $do_fixup ]; then 
   fix_duplicate_date
   fix_server_date
   turn_on_purging
fi

if [ $do_test ]; then
   check_data
fi

if [ $check_failed ]; then
   exit 1;
fi

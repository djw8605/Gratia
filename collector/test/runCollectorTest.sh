#!/bin/sh

function usage {
cat 1>&2 <<EOF
usage: runCollectorTest.sh [-h] [-l] [-d] [-c] [-p port_number]
   -h print this help
   -d reset the schema and clean collector data directory
   -c reinstall the collector
   -p use this port for the main collector (default 9000)
   -l load the data
   -f execute fix ups
   -k Turn on the housekeeping
   -w [filename] upload the war file
   -b [filename] build and upload the war file
   -s stop server
   -m test timeout feature
   -t test content
   --lsf load lsf data
   --pbs load pbs data
EOF
}

function setfromconfig {
   what=$1
   res=`grep -c $what runCollectorTest.config `
   if [ $res -eq 0 ] ; then
     echo "The configuration key $what is not set in runCollectorTest.config" 1>&2
     exit 1;
   else if [ $res -gt 1 ] ; then
       echo "The configuration key $what is more than once in runCollectorTest.config" 1>&2
       exit 1;
     fi
   fi
   echo `grep $what runCollectorTest.config | cut -d= -f2-`
}

http_port=`expr 8000 + ${UID}`
if [ ! -e runCollectorTest.config ] ; then
   echo "runCollectorTest.config does not exist, see runCollectorTest.template for an example"
   exit 1;
fi

webhost=`setfromconfig webhost`
if [ "x$webhost" = "x" ] ; then exit 1; fi

dbhost=`setfromconfig dbhost`
if [ "x$dbhost" = "x" ] ; then exit 1; fi

dbport=`setfromconfig dbport`
if [ "x$dbport" = "x" ] ; then exit 1; fi

# Need obfuscation
update_password=proto
reader_password=reader
pass=lisp01

schema_name=`setfromconfig schema_name`
if [ "x$schema_name" = "x" ] ; then 
   schema_name=gratia_purge_${USER}
fi
tomcatpwd=/data/tomcat-${schema_name}

host=`hostname`
source=${PWD}
source=`dirname $source`
source=`dirname $source`
export PYTHONPATH=${source}/probe/common:${source}/probe/metric:${PYTHONPATH}

server_status=unknown

function readonly_mysql() {
    mysql -h ${dbhost} --port=${dbport} -u reader --password=${reader_password} $*
}

function readwrite_mysql() {
    ssh ${webhost} mysql -h ${dbhost} --port=${dbport} -u gratia --password=${update_password} $*
}

function adminCollector() {
   wget --dns-timeout=5 --connect-timeout=10 --read-timeout=40 -O - "http://${webhost}:${ssl_port}/gratia-administration/systemadministration.html?action=$1"
}

function statusCollector() {
   wget --dns-timeout=5 --connect-timeout=10 --read-timeout=40 -O - "http://${webhost}:${ssl_port}/gratia-administration/collector-status.html?out=txt&subsystem=$1"
}

function stop_server {
  if [ "${server_status}" != "stopped" ]; then 
     echo "Stopping server ${webhost}:${http_port}"
     alive=`ssh ${webhost} netstat -l | grep ${http_port} | wc -l`
     if [ ${alive} -eq 1 ]; then 
       ssh -l root ${webhost} service tomcat-${schema_name} stop 2>&1 | head -3 
       server_status=stopping
       wait_for_server_shutdown 
     fi
     echo "Stopped server ${webhost}:${http_port}"
     server_status=stopped
  fi
}

function start_server {
  if [ "${server_status}" = "unknown" ] ; then 
      echo "Checking for server status"
      alive=`ssh ${webhost} netstat -l | grep ${http_port} | wc -l`
      if [ ${alive} -ne 0 ]; then
         server_status=started
      else
         server_status=stopped
      fi
  fi
  if [ "${server_status}" = "stopped"  ]; then 
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

function write_ProbeConfig
{  
   name=$1
   remotehost=$2
   bundleSize=$3
   service=$4
   if [ "x$5" = "x" ] ; then 
      probeprefix=tester
   else 
      probeprefix=$5
   fi

   cat > $name <<EOF
<ProbeConfiguration 
    UseSSL="0" 

    UseGratiaCertificates="0"

    SSLHost="${remotehost}:8443" 
    SSLCollectorService="/gratia-servlets/rmi"
    SSLRegistrationHost="${remotehost}:${http_port}"
    SSLRegistrationService="/gratia-security/security"

    GratiaCertificateFile="gratia.hostcert.pem"
    GratiaKeyFile="gratia.hostkey.pem"

    SOAPHost="${remotehost}:${http_port}" 
    CollectorService="/$service/rmi" 
    UseSoapProtocol="0"
    
    MeterName="${probeprefix}:LocalTester.where.edu" 
    SiteName="LocalTesting"
    Grid="OSG"
   
    ${bundleSize}
    ConnectionTimeout="3" 

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
    EnableProbe="1"
/>
EOF
}

function write_ProbeConfigs
{  
   write_ProbeConfig ProbeConfig ${webhost} "" gratia-servlets
   write_ProbeConfig ProbeConfigSingle ${webhost} 'BundleSize="1"' gratia-servlets
   write_ProbeConfig ProbeConfigTimeout ${webhost} "" gratia-testtimeout
   write_ProbeConfig ProbeConfigFirewall atlasgw.bnl.gov "" gratia-testtimeout
   write_ProbeConfig ProbeConfigLsf ${webhost} "" gratia-servlets lsf
   write_ProbeConfig ProbeConfigPbs ${webhost} "" gratia-servlets pbs
}

function wait_for_server {

   write_ProbeConfigs

   alive=0
   try=0
   while [ ${alive} -eq 0 -a ${try} -lt 100 ]; do   \
      echo "Waiting for server"
      python > alive.tmp <<EOF
import GratiaCore
GratiaCore.Initialize()
GratiaCore.ProcessBundle(GratiaCore.CurrentBundle)
print GratiaCore.successfulHandshakes
EOF
      alive=`cat alive.tmp`
      rm alive.tmp
      try=`expr ${try} + 1`
      if [ ${alive} -ne 1 ]; then
         sleep 1
      fi
   done
   if [ ${try} -gt 99 ]; then
      echo "Error server is not started after 10 checks"
      exit
   fi
   
   server_status=started
}

function wait_for_server_shutdown {

   alive=1
   try=0
   while [ ${alive} -eq 1 -a ${try} -lt 10 ]; do
      echo "Waiting for server shutdown"
      alive=`ssh ${webhost} netstat -l | grep ${http_port} | wc -l`
      try=`expr ${try} + 1`
      if [ ${alive} -ne 0 ]; then
         sleep 1
      fi
   done
   if [ ${try} -gt 9 ]; then
      echo "Error server is not shutdown after 10 checks"
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

   # And now remove the files
   ssh ${webhost} "rm -f ${tomcatpwd}/gratia/data/thread*/*"

   # And the pbs/lsf memory
   rm -rf */MAGIC_VDT_LOCATION

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
              "service.security.level" => "1",
              "service.lifetime.JobUsageRecord" => "3 Months",
              "service.lifetime.JobUsageRecord.RawXML" => "1 Month",
              "service.lifetime.MetricRecord" => "36 months",
              "service.lifetime.MetricRecord.RawXML" => "1 month",
              "service.lifetime.DupRecord.Duplicates" => "1 month",
              "service.lifetime.DupRecord" => "UNLIMITED",
              "maintain.history.log" => 2,
              "monitor.recordProcessor.wait" => 240,
              "max.q.size" => 700,
              "max.housekeeping.nrecords" => 500,
              "min.housekeeping.nrecords" => 50,
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

   ssh -l root ${webhost} mkdir -p ${tomcatpwd} \; tar -zxf ~gratia/tomcat-tarballs/apache-tomcat-5.5.28.tar.gz --strip 1 -C ${tomcatpwd}\; mkdir -p ${tomcatpwd}/gratia\; chown -R ${USER} ${tomcatpwd}\; rm -f ${tomcatpwd}/logs/*

   ssh -l root ${webhost}  cd ${source}/common/configuration\; \
     ./update-gratia-local -s -S ${source} -d ${pass} -i ${filename} ${schema_name} \; \
     chown ${USER} ${tomcatpwd}/gratia/service-configuration.properties

   scp ../../target/gratia-testtimeout.war root@${webhost}:${tomcatpwd}/webapps

   start_server
}


function fix_duplicate_date {
  expected_duplicate=21

  echo '0' > dups.count
  while [ `tail -1 dups.count` -lt ${expected_duplicate} ]; do
    echo "Waiting for Duplicate data to be loaded." `tail -1 dups.count`
    readonly_mysql > dups.count 2>&1 <<EOF 
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

echo "use ${schema_name}; select dupid from DupRecord where error='Parse'" \
  | readonly_mysql -s \
  | perl -wane "use strict; use vars qw(\$weeks \$dupid); BEGIN { \$weeks = 1; printf \"use ${schema_name};\n\" }; chomp; \$dupid=\$_; printf \"update  DupRecord set eventdate = date_sub(eventdate,interval \$weeks week) where dupid = \$dupid;\n\"; ++\$weeks; " \
  | readwrite_mysql

echo "use ${schema_name}; select dupid from DupRecord where error='Duplicate'" \
  | readonly_mysql -s \
  | perl -wane "use strict; use vars qw(\$weeks \$dupid); BEGIN { \$weeks = 1; printf \"use ${schema_name};\n\" }; chomp; \$dupid=\$_; printf \"update  DupRecord set eventdate = date_sub(eventdate,interval \$weeks week) where dupid = \$dupid;\n\"; ++\$weeks; " \
  | readwrite_mysql

}

function fix_usage_server_date {

  expected_records=431

  echo '0' > records.count
  while [ `tail -1 records.count` -lt ${expected_records} ]; do
  echo "Waiting for Record data to be loaded." `tail -1 records.count` " out of ${expected_records}"
  readonly_mysql > records.count 2>&1 <<EOF 
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

  readwrite_mysql<<EOF 
use ${schema_name};
update JobUsageRecord_Meta M, JobUsageRecord J set ServerDate = date_add(EndTime, interval 65 minute) where M.dbid = J.dbid;
EOF

}

function fix_metric_server_date {

  expected_records=400

  echo '0' > mrecords.count
  while [ `tail -1 mrecords.count` -lt ${expected_records} ]; do
  echo "Waiting for Metric Record data to be loaded." `tail -1 mrecords.count` " out of ${expected_records}"
  readonly_mysql > mrecords.count 2>&1 <<EOF 
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

  readwrite_mysql<<EOF 
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

   write_ProbeConfigs
   wait_for_server

   # First extend artificially the retention to let the old record in.
   echo "Turning off record purging"

   adminCollector "disableHousekeeping" 2>&1 | tee wget.full.log | grep House | grep DISABLED > wget.log
   result=$?
   if [ ${result} -ne 0 ]; then
      echo "Error: Turning off the record purging failed"
      exit 1
   fi

   echo "Sending data"

   tar xfz preparedrecords.tar.gz
   if [ "gratia-vm02.fnal.gov_9000" != "${webhost}_${http_port}" ]; then 
      find MAGIC_VDT_LOCATION/gratia/var/tmp/gratiafiles -type f -name '*'"gratia-vm02.fnal.gov_9000"'*' | while read file; do
         new_file=`echo "$file" | perl -wpe 's&\Q'"gratia-vm02.fnal.gov_9000.gratia.xml"'\E&'"${webhost}_${http_port}.gratia.xml"'&' | sed -e 's/LocalTester/tester_LocalTester.where.edu/' `
         mv "$file" "$new_file"
      done
   fi

   python <<EOF
import Gratia
Gratia.Initialize("ProbeConfigSingle")
EOF

   echo "Loading job usage record"
   python loaddata.py
   echo "Loading metric record"
   python loadmetric.py
}

function check_result {
   period=$1
   if [ "x$period" != "x" ] ; then
     period=".$period"
   fi 
   stem=$2
   msg=$3

   diffcmd="diff $stem$period.ref $stem.validate"
   
   eval $diffcmd > /var/tmp/diff.$$
   res=$?
   if [ ${res} -eq 0 ]; then
      echo ${msg} is OK.
   else
      echo $diffcmd
      cat /var/tmp/diff.$$
      echo ${msg} is INCORRECT.
      check_failed=1
   fi;
}

function loadpbs 
{
    pbsprobedir=${PWD}/pbs

    mkdir -p ${pbsprobedir}
    mkdir -p ${pbsprobedir}/MAGIC_VDT_LOCATION/var/tmp/urCollector
    mkdir -p ${pbsprobedir}/MAGIC_VDT_LOCATION/var/logs
    if [ ! -e common ] ; then 
	ln -s ../../probe/common .
    fi

    start_server

    # Disable housekeeping
    echo "Turning off record purging"
    adminCollector "disableHousekeeping" 2>&1 | tee wget.full.log | grep House | grep DISABLED > wget.log
    result=$?
    if [ ${result} -ne 0 ]; then
	echo "Error: Turning off the record purging failed"
	exit 1
    fi

    write_ProbeConfig ${pbsprobedir}/ProbeConfig ${webhost} "" gratia-servlets pbs

    echo "Sending PBS data"
    export PERL5LIB=${PWD}/../../probe/pbs-lsf/urCollector-src
    ${pbsprobedir}/pbs-lsf_meter.cron.sh


    # Wait for the record to be in
    wait_for_input_use 0

    # Should we fix the dates?

}

function loadlsf 
{
    lsfprobedir=${PWD}/lsf

    mkdir -p ${lsfprobedir}
    mkdir -p ${lsfprobedir}/MAGIC_VDT_LOCATION/var/tmp/urCollector
    mkdir -p ${lsfprobedir}/MAGIC_VDT_LOCATION/var/logs
    if [ ! -e common ] ; then 
	ln -s ../../probe/common .
    fi

    start_server

    # Disable housekeeping
    echo "Turning off record purging"
    adminCollector "disableHousekeeping" 2>&1 | tee wget.full.log | grep House | grep DISABLED > wget.log
    result=$?
    if [ ${result} -ne 0 ]; then
	echo "Error: Turning off the record purging failed"
	exit 1
    fi

    write_ProbeConfig ${lsfprobedir}/ProbeConfig ${webhost} "" gratia-servlets lsf

    echo "Sending LSF data"
    export PERL5LIB=${PWD}/../../probe/pbs-lsf/urCollector-src
    ${lsfprobedir}/pbs-lsf_meter.cron.sh

    # Wait for the record to be in
    wait_for_input_use 0

    # Should we fix the dates?

}

function wait_for_input_use {
   expected_count=$1
   ssh ${webhost} ls ${tomcatpwd}/gratia/data/thread\? | wc -l > file.count
   while [ `tail -1 file.count` -gt ${expected_count} ]; do
      ssh ${webhost} ls ${tomcatpwd}/gratia/data/thread\? | wc -l > file.count
      echo "Waiting for all input msg to be used." `tail -1 file.count` left
      sleep 2
   done
}

function turn_on_purging {

   # Restore the original (we could also set it to specific values)
   echo "Turning on record purging"
    #   ssh ${webhost} cd ${tomcatpwd}/gratia\; mv service-configuration.properties.auto.old service-configuration.properties \;
    #   restart_server

   start_server
   wait_for_server

   adminCollector startHousekeepingNow 2>&1 | tee wget.full.log | grep House | grep STOPPED > wget.log
   result=$?
   if [ ${result} -ne 1 ]; then
      echo "Error: Turning on the record purging failed"
      exit 1
   fi

   # Wait until the purge has been done
   sleep 3

   # Load a few old record to make sure they are caught by the filter.
   python loadstale.py

   wait_for_input_use 0
}

function check_data 
{
   # Number of days in the last 3 months:
   ydays=`expr \( 3672 + \`date +%s\` - \`date --date='12 month ago' +%s\` \) / 3600 / 24 `
   days=`expr \( 3672 + \`date +%s\` - \`date --date='3 month ago' +%s\` \) / 3600 / 24 `
   mdays=`expr \( 3672 + \`date +%s\` - \`date --date='1 month ago' +%s\` \) / 3600 / 24 `
   echo "Checking results with $days days in the last 3 months"
   echo "Checking results with $mdays days in the last month"

   echo "Checking duplicates"
   readonly_mysql > duplicate.validate 2>&1 <<EOF 
use ${schema_name};
select RecordType, error, count(*) from DupRecord group by RecordType, error;
EOF

   echo "Check JobUsageRecord"
   readonly_mysql > jobusagerecord.validate 2>&1 <<EOF 
use ${schema_name};
select ProbeName, VOName, count(*) as Nrecord, Sum(NJobs) as NJobs, Sum(WallDuration) as Wall, Sum(CpuUserDuration+CpuSystemDuration) as Cpu 
    from JobUsageRecord_Report group by ProbeName, VOName;
EOF

   echo "Check JobUsageRecord_Xml"
   readonly_mysql > jobusagerecordxml.validate 2>&1 <<EOF 
use ${schema_name};
select count(*),(ExtraXml!="" and not isnull(ExtraXml)) as hasExtraXml from JobUsageRecord_Xml group by hasExtraXml
EOF

   echo "Check MasterSummaryData"
   readonly_mysql > jobsummary.validate 2>&1 <<EOF 
use ${schema_name};
select ProbeName, VOName, count(*) as Nrecord, Sum(NJobs) as NJobs, Sum(WallDuration) as Wall, Sum(CpuUserDuration+CpuSystemDuration) as Cpu 
    from VOProbeSummary group by ProbeName, VOName;
EOF

   echo "Check MetricRecord"
   readonly_mysql > metricrecord.validate 2>&1 <<EOF 
use ${schema_name};
select ProbeName, MetricName, count(*) as Nrecord
    from MetricRecord J, MetricRecord_Meta M where J.dbid = M.dbid group by ProbeName, MetricName;
EOF

   echo "Check MetricRecord_Xml"
   readonly_mysql > metricrecordxml.validate 2>&1 <<EOF 
use ${schema_name};
select count(*),(ExtraXml!="" and not isnull(ExtraXml)) as hasExtraXml from MetricRecord_Xml group by hasExtraXml
EOF

   echo "Check Origin"
   readonly_mysql > origin.validate 2>&1 <<EOF 
use ${schema_name};
select count(*)<60 from Origin;
EOF

  echo "Check status monitoring"
  wait_for_input_use 0
  wget --dns-timeout=5 --connect-timeout=10 --read-timeout=40 -O - "http://${webhost}:${ssl_port}/gratia-administration/monitor-status.html" 2>wget.full.log | cut -d\| -f3- > status.validate

  check_result $days duplicate "Duplicate"
  check_result $ydays jobsummary "JobUsageRecord Summary Table"
  check_result $days jobusagerecord "JobUsageRecord"
  check_result $mdays jobusagerecordxml "JobUsageRecord's RawXml"
  check_result $days metricrecord "MetricRecord"
  check_result $mdays metricrecordxml "MetricRecord's RawXml"
  check_result $days origin "Origin records"
  check_result "" status "Status monitoring"
}

function check_timeout()
{
   echo "Checking timeout feature"

   start_server

   write_ProbeConfigs
   wait_for_server

   echo "Checking connection timeout"
   start=`date +%s`
   python timeout.py ProbeConfigFirewall
   len=`expr \( \`date +%s\` - $start \)`
   if [ $len -gt 12 ] ; then 
      echo "Error: the timeout time ($len) when accessing a firewall host was too long (expected less than 12)"
      check_failed=1
   fi

   echo "Checking post timeout"
   start=`date +%s`
   python timeout.py ProbeConfigTimeout
   len=`expr \( \`date +%s\` - $start \)`
   if [ $len -gt 12 ] ; then 
      echo "Error: the timeout time ($len) when accesing a hung server was too long (expected less than 12)"
      check_failed=1
   fi

}

function check_queue_threshold()
{
   echo "Check the queue threshold mechanism"

   # Make sure the queue is empty before stopping the database updates
   wait_for_input_use 0

   # Turn off updates so the record are not consumed
   adminCollector "stopDatabaseUpdateThreads" 2>wget.threshold.stderr.log  | tee wget.threshold.full.log  | grep 'Database' > wget.threshold.log

   # Dump a bunch of (empty) files in the thread0 queue.
   ssh ${webhost}  "for i in \`seq 1 800\` ; do touch ${tomcatpwd}/gratia/data/thread0/emptyfile\$i.2.xml ; done"

   # Refresh the queue statistics.
   adminCollector "refreshStatus" 2>>wget.threshold.stderr.log | tee -a wget.threshold.full.log |  grep 'Collector Status' > wget.threshold.log
   
   # Let's start the housekeeping.
   adminCollector "startHousekeepingNow" 2>>wget.threshold.stderr.log | tee -a wget.threshold.full.log |  grep 'DataHousekeeping' > wget.threshold.log
   sleep 5
   # We run the housekeeping twice to make sure our time waster is run.
   adminCollector "startHousekeepingNow" 2>>wget.threshold.stderr.log | tee -a wget.threshold.full.log |  grep 'DataHousekeeping' > wget.threshold.log
      
   # Wait one minute to let the housekeeping time run
   echo "Sleeping 5s to let the housekeeping time to start"
   sleep 5

   # Force the checking of the queue sizes
   adminCollector "runQSizeMonitor"  2>>wget.threshold.stderr.log | grep QSizeMonitor | tee -a wget.threshold.full.log

   # Wait one minute to let the QSizeMonitor run
   #echo "Sleeping 5s to let the QSizeMonitor time to run"
   sleep 2

   # Check whether the servlets are disabled in 2 ways
   statusCollector collector  2>>wget.threshold.stderr.log > collector-status.validate
   check_result "" collector-status "Servlets Status"

   wget --dns-timeout=5 --connect-timeout=10 --read-timeout=40 -O - "http://${webhost}:${ssl_port}/gratia-servlets/rmi" --post-data="nothing" 2>>wget.threshold.stderr.log > collector-input.validate
   check_result "" collector-input "Probe response when disabled"

   # Now let's check the housekeeping
   statusCollector "datahousekeeping"  2>>wget.threshold.stderr.log > housekeeping-status.validate
   check_result "" housekeeping-status "HouseKeeping Status"
   
   # And now remove the files
   ssh ${webhost}  "rm ${tomcatpwd}/gratia/data/thread0/emptyfile*"
   
   # Refresh the queue statistics.
   adminCollector "refreshStatus" 2>>wget.threshold.stderr.log | tee -a wget.threshold.full.log |  grep 'Collector Status' > wget.threshold.log

   # Force the checking of the queue sizes
   adminCollector "runQSizeMonitor"  2>>wget.threshold.stderr.log | grep QSizeMonitor | tee -a wget.threshold.full.log

   # Wait one minute to let the QSizeMonitor run
   echo "Sleeping 5s to let the QSizeMonitor time to run"
   sleep 2

   # Check whether the servlets are disabled in 2 ways
   statusCollector "collector"  2>>wget.threshold.stderr.log > collector-run-status.validate
   check_result "" collector-run-status "Servlets Status when enabled"

   wget --dns-timeout=5 --connect-timeout=10 --read-timeout=40 -O - "http://${webhost}:${ssl_port}/gratia-servlets/rmi" --post-data="nothing" 2>>wget.threshold.stderr.log  | tee -a wget.threshold.full.log | cut -c-49 > collector-run-input.validate
   check_result "" collector-run-input "Probe response when enabled"

   # Now let's check the housekeeping
   statusCollector "datahousekeeping"  2>>wget.threshold.stderr.log > housekeeping-run-status.validate
   check_result "" housekeeping-run-status "HouseKeeping Status when enabled"

} 

function build_war()
{
    (cd ../../build-scripts; gmake $BUILDWAR)
    result=$?
    if [ $result -ne 0 ]; then
       exit $result  
    fi
}

function upload_war()
{
    stop_server
    for warfile in $WAR; do 
      if [ ! -e ../../target/$warfile.war ] ; then 
         warfile=gratia-$warfile
      fi
      ssh -l root ${webhost} rm -rf ${tomcatpwd}/webapps/$warfile\*
      scp ../../target/$warfile.war root@${webhost}:${tomcatpwd}/webapps
    done
    start_server
}

#--- get command line args ----
#while getopt :tshcfkdlmn:w:p:b: OPT; do

ARGS=$(getopt -s bash --options :tshcfkdlmn:w:p:b:  \
  --longoptions pbs,lsf,help --name runCollectorTest.sh -- "$@" )

getoptresult=$?

if [ $getoptresult -eq 1 ] ; then
   ARGS=`echo $ARGS | sed -e 's/ *-- *$//' `
   if [ "x$ARGS" != "x" ] ; then 
      BADS=`echo $@ | sed -e "s/${ARGS}//" `
   else
      BADS=$@
   fi
   echo "Illegal option(s):" "$BADS"
   usage
   exit 1
fi

eval set -- "$ARGS"

while true ; do 
    OPT=$1
    OPTARG=$2
    case $OPT in
        -n)  schema_name=$OPTARG
            tomcatpwd=/data/tomcat-$OPTARG
            ;;
        -w)  do_war=1
            WAR="$WAR $OPTARG"
            ;;
        -b)  do_war=1
            do_build=1
            WAR="$WAR $OPTARG"
            BUILDWAR="$BUILDWAR $OPTARG"
            ;;
        -c)  do_collector=1
            ;;
        -l)  do_load=1
            ;;
        -d)  do_databasereset=1
            ;;
        -f)  do_fixup=1; 
            ;;
        -k)  do_purge=1
            ;;
        -h|--help)
            usage
            exit 1
            ;;
        -p)  http_port=$OPTARG
            ;;
        -t)  do_test=1
            ;;
        -s)  do_stop=1
            ;;
        -m)  do_timeout=1
            ;;
        --lsf) do_lsf=1
            ;;
        --pbs) do_pbs=1
            ;;
        --)
            shift
            break
            ;;
        *)
            shift
            break
            ;; 
    esac
    shift
done
shift $[ OPTIND - 1 ]

#NOTE: the ssl_port MUST be equal to the http port for the test to work properly
# In case of equality, the dataHouskeeping when there is no work do to will spend
# a minute or two sleeping (by 1 second interval). 
ssl_port=`expr $http_port + 0`
rmi_port=`expr $http_port + 2`
jmx_port=`expr $http_port + 3`
server_port=`expr $http_port + 4`

if [ $do_stop ]; then
   stop_server
fi
if [ $do_databasereset ];then
   echo "Cleanup all database content and outstanding collector records"
   reset_database
fi

if [ $do_collector ]; then 
   reset_collector
fi

if [ $do_build ]; then
   build_war
fi

if [ $do_war ]; then
   upload_war
fi

if [ $do_load ]; then
   loaddata
fi

if [ $do_pbs ] ; then 
   loadpbs
fi

if [ $do_lsf ] ; then 
   loadlsf
fi

if [ $do_fixup ]; then 
   fix_duplicate_date
   fix_server_date
fi

if [ $do_purge ]; then
   turn_on_purging
fi

if [ $do_timeout ]; then
   check_timeout
fi

if [ $do_test ]; then
   check_data
   check_queue_threshold
fi

if [ $check_failed ]; then
   exit 1;
fi

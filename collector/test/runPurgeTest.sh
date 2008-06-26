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
EOF
}

http_port=9000


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


function stop_server {
  echo "Stopping server ${webhost}:${http_port}"
  ssh -l root ${webhost} service tomcat-${schema_name} stop 2>&1 | head -3 
  sleep 7
}

function start_server {
  echo "Starting server ${webhost}:${http_port}"
  ssh -l root ${webhost} service tomcat-${schema_name} start
  sleep 7
}

function restart_server {
  stop_server
  start_server
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
             db_host => "${dbhost}",
             db_schema => "${schema_name}",
             "properties.attributes" =>
             {
              "maintain.history.log" => 2,
              "monitor.listener.wait" => 240,
              "monitor.to.address0" => '${USER}@fnal.gov',
              "service.admin.DN.0" => 'ALLOW ALL'
             }
           }
);

### Local Variables:
### mode: cperl
### End:
EOF

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
  echo "Waiting for Duplicate data to be loaded." `cat dups.count`
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

function fix_server_date {

  expected_records=32

  echo '0' > records.count
  while [ `tail -1 records.count` -lt ${expected_records} ]; do
  echo "Waiting for Record data to be loaded."
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

function loaddata {
    echo "Sending data"

    start_server

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

  export PYTHONPATH=${source}/probe/common:${PYTHONPATH}

  tar xfz preparedrecords.tar.gz
  find MAGIC_VDT_LOCATION/gratia/var/tmp/gratiafiles -type f -name '*'"gratia-vm02.fnal.gov_9000"'*' | while read file; do
    new_file=`echo "$file" | perl -wpe 's&\Q'"gratia-vm02.fnal.gov_9000.gratia.xml"'\E&'"${webhost}_${http_port}.gratia.xml"'&'`
    mv "$file" "$new_file"
  done

  # First extend artificially the retention to let the old record in.
  ssh ${webhost} cd ${tomcatpwd}/gratia \; chown ${USER} service-configuration.properties \; mv service-configuration.properties service-configuration.properties.auto.old \; sed  -e '"s:service.lifetime.JobUsageRecord = .*:service.lifetime.JobUsageRecord = 24 months:"' service-configuration.properties.auto.old \> service-configuration.properties
  #restart_server

  python <<EOF
import Gratia
Gratia.Initialize()
EOF
  python loaddata.py

  # Restore the original (we could also set it to specific values)
  ssh ${webhost} cd ${tomcatpwd}/gratia\; mv service-configuration.properties.auto.old service-configuration.properties \;
  #restart_server
}

function upload_war()
{
    stop_server
    ssh -l root ${webhost} rm -rf ${tomcatpwd}/webapps/$WAR\*
    scp ../../target/$WAR.war ${webhost}:${tomcatpwd}/webapps
    start_server
}

#--- get command line args ----
while getopts :hcfdlw:p: OPT; do
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
        *)
            usage
            exit 1
            ;;
    esac
done
shift $[ OPTIND - 1 ]

ssl_port=`expr $http_port + 1`
rmi_port=`expr $http_port + 2`
jmx_port=`expr $http_port + 3`
server_port=`expr $http_port + 4`

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
   #fix_duplicate_date
   fix_server_date
fi

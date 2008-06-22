#!/bin/sh

function usage {
cat 1>&2 <<EOF
usage: runPurgeTest.sh [-h] [-l] [-d] [-c] [-p port_number]
   -h print this help
   -d reset the schema
   -c reinstall the collector
   -p use this port for the main collector (default 9000)
   -l load the data
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
host=`hostname`
source=${PWD}
source=`dirname $source`
source=`dirname $source`


function start_server {
  ssh -l root ${webhost} service tomcat-${schema_name} start
  sleep 5
}

function restart_server {
  echo "Restarting server ${webhost}:${http_port}"
  ssh -l root ${webhost} service tomcat-${schema_name} stop
  sleep 7
  ssh -l root ${webhost} service tomcat-${schema_name} start
  sleep 5
}

function reset_database {

  ssh -l root ${webhost} service tomcat-${schema_name} stop
  sleep 5

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

   tomcatpwd=/data/tomcat-${schema_name}

   ssh -l root ${webhost} cp -rp /data/tomcat-install ${tomcatpwd}\; mkdir -p ${tomcatpwd}/gratia\; chown -R ${USER} ${tomcatpwd}

   ssh -l root ${webhost}  cd ${source}/common/configuration\; \
     ./update-gratia-local -s -S ${source} -d ${pass} -i ${filename} ${schema_name} \; \
     chown ${USER} ${tomcatpwd}/gratia/service-configuration.properties

   start_server
}


function loaddata {

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

set -x
  # First extend artificially the retention to let the old record in.
  tomcatpwd=/data/tomcat-${schema_name}
  ssh ${webhost} set -x \; cd ${tomcatpwd}/gratia \; chown ${USER} service-configuration.properties \; mv service-configuration.properties service-configuration.properties.auto.old \; sed  -e '"s:service.lifetime.JobUsageRecord = .*:service.lifetime.JobUsageRecord = 24 months:"' service-configuration.properties.auto.old \> service-configuration.properties
  restart_server

  python <<EOF
import Gratia
Gratia.Initialize()
EOF
  python loaddata.py

  # Restore the original (we could also set it to specific values)
  ssh ${webhost} cd ${tomcatpwd}/gratia\; mv service-configuration.properties.auto.old service-configuration.properties \;
  restart_server

}

#--- get command line args ----
while getopts :hcdlp: OPT; do
    case $OPT in
        c)  do_collector=1
            ;;
        l)  do_load=1
            ;;
        d)  do_databasereset=1
            ;;
        h)
            usage
            exit 1
            ;;
        p)  http_port=$OPTARG
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
if [ $do_load ]; then
   echo "Uploading data"
   loaddata
fi


#!/bin/sh
function package_common_classes(){
	path=$1
	echo "input path is: $path"
	if [ ! -d $path ]
	then
		echo "$path not a directory" 
		exit 1
	fi
	for file in  "$path/net/sf/gratia/util/TidiedDailyRollingFileAppender\$DatedFileFilter.class"  "$path/net/sf/gratia/util/TidiedDailyRollingFileAppender.class"; do \
    		[[ -z "$file" ]] && continue; \
    		dir="tarball/net/`dirname \"${file##*/net/}\"`"; \
    		mkdir -p "${dir}"; \
    	cp -v "${file}" "${dir}"; \
    	done; 
}
function update_gratia_release() {
        #Example output is as follows:
        # Gratia release: v1.13-1 Build date: Wed Jun 12 14:35:11 CDT 2013 Build host: fermicloud331.fnal.gov Build path: /root/ant_build Builder: uid=0(root)
	base=$1
	gratia_release=$2
	target_dir=$3
	echo "Gratia release: ${gratia_release} Build date: `date` Build host: `hostname -f` Build path: ${base} Builder: `id |cut -d\" \" -f1`" > ${target_dir}/gratia-release
}
function create_versions_java() {
	base=$1
	gratia_release=$2
	file=${base}/collector/gratia-services/net/sf/gratia/services/Versions.java
	package="package net.sf.gratia.services;"
        touch /var/tmp/versions.$$.java
        echo "${package}

	public class Versions {
        	private static final String fgPackageVersionString = \""${gratia_release}"\";
        	public static String GetPackageVersionString() { return fgPackageVersionString; }
        };" > /var/tmp/versions.$$.java
        if [ ! -e $file ] ;  then
        	mv /var/tmp/versions.$$.java $file
        else
        	diff $file /var/tmp/versions.$$.java > /dev/null
                result=$?
                if [ $result -eq 0 ] ; then
               		rm /var/tmp/versions.$$.java
                else
                        mv /var/tmp/versions.$$.java $file
                fi
        fi

}
function create_clean_up_scripts(){
	base=$1
        ##### Create cleanup_common_lib #####
        sed -e 's/%%%FILES%%%/ log4j-*.jar  commons-logging-1*.jar/' "${base}/common/configuration/cleanup_template" > "${base}/target/cleanup_common_lib"
        chmod a+x "${base}/target/cleanup_common_lib"

        ##### Create cleanup_server_lib #####
        sed -e 's/%%%FILES%%%/ glite-security-trustmanager*.jar  glite-security-util-java*.jar  bcprov-*.jar/' "${base}/common/configuration/cleanup_template" > "${base}/target/cleanup_server_lib"
        chmod a+x "${base}/target/cleanup_server_lib"

        ##### Create cleanup_slf4j_lib #####
        sed -e 's/%%%FILES%%%/ slf4j-api*.jar  slf4j-log4j12*.jar/' "${base}/common/configuration/cleanup_template" > "${base}/target/cleanup_slf4j_lib"
        chmod a+x "${base}/target/cleanup_slf4j_lib"
}
function set_service_properties() {
	base=$1
	gratia_release=$2
	target_dir=$3
        sed -e "s/^gratia.services.version.*=.*/gratia.services.version = v${gratia_release}/"  -e "s/^gratia.reporting.version.*=.*/gratia.reporting.version = v${gratia_release}/" ${base}/common/configuration/service-configuration.properties > ${target_dir}/service-configuration.properties
        sed -e "s/^gratia.services.version.*=.*/gratia.services.version = v${gratia_release}/"  -e "s/^gratia.reporting.version.*=.*/gratia.reporting.version = v${gratia_release}/" ${base}/common/configuration/service-authorization.properties > ${target_dir}/service-authorization.properties
}


case $1 in
	"package_common_classes") package_common_classes $2;;
	"edit_web_xml") edit_web_xml $2;;
	"update_gratia_release") update_gratia_release $2 $3 $4;;
	"create_versions_java") create_versions_java $2 $3;;
	"create_clean_up_scripts") create_clean_up_scripts $2;;
	"set_service_properties")set_service_properties $2 $3 $4;;
	*) echo "Unknown function $1" 
	   exit 1;;
esac
exit 0

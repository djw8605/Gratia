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
function edit_web_xml() {
	#####This script takes the path to a web.xml which needs to be modified for gratia-reporting.war#####
        path=$1
        echo "input path is: $path"
        if [ ! -d $path ]
        then
                echo "$path not a directory" 
                exit 1
	fi
	#####Create two temporary files#####
	tmp=`mktemp ${TMPDIR:-/tmp}/gratia-build.XXXXXXXXXX`; \
	tmp2=`mktemp ${TMPDIR:-/tmp}/gratia-build.XXXXXXXXXX`; \
		[[ -n "$tmp" ]] || exit 1; \
		[[ -n "$tmp2" ]] || exit 1; \
		trap "[[ -n \"$tmp\" ]] && rm -f \"$tmp\" \"$tmp2\"" EXIT; \
	#####Use perl to make changes to web.xml and store in "$tmp"#####
	perl -we 'use strict; my $seen_par; my $par = "BIRT_VIEWER_TIMEZONE"; my $val = "GMT"; while (<>) { m&<param-name>\Q$par\E</param-name>& and $seen_par = 1; $seen_par and s&<param-value>.*&<param-value>$val</param-value>& and undef $seen_par; print }' $1/web.xml > "$tmp"; \
  	(( $? == 0 )) || exit 1; \

	#####Use perl to make changes to "$tmp" and store in "$tmp2"#####
	perl -we 'use strict; my $seen_par; my $par = "WORKING_FOLDER_ACCESS_ONLY"; my $val = "false"; while (<>) { m&<param-name>\Q$par\E</param-name>& and $seen_par = 1; $seen_par and s&<param-value>.*&<param-value>$val</param-value>& and undef $seen_par; print }' "$tmp" > "$tmp2"; \
  	(( $? == 0 )) || exit 1; \

	#####Use perl to make changes to "$tmp2" and store in "$tmp"#####
	perl -wpe 'use strict; m&display-name& and do { print "  <!-- UrlRewriteFilter configuration -->\n <filter>\n	<filter-name>UrlRewriteFilter</filter-name>\n	<filter-class>org.tuckey.web.filters.urlrewrite.UrlRewriteFilter</filter-class>\n	<init-param>\n	 <param-name>logLevel</param-name>\n	 <param-value>WARN</param-value>\n	 </init-param>\n </filter>\n <filter-mapping>\n	 <filter-name>UrlRewriteFilter</filter-name>\n	 <url-pattern>/*</url-pattern>\n </filter-mapping>\n\n <!-- Gratia Reporting Service Listener -->\n        <listener>\n                <listener-class>net.sf.gratia.reporting.ReportingService</listener-class>\n        </listener>\n\n"; print "    <servlet>\n        <servlet-name>MonitorStatus</servlet-name>\n        <servlet-class>net.sf.gratia.administration.MonitorStatus</servlet-class>\n        <load-on-startup>2</load-on-startup>\n    </servlet>\n\n    <servlet-mapping>\n        <servlet-name>MonitorStatus</servlet-name>\n        <url-pattern>/monitor-status.html</url-pattern>\n    </servlet-mapping>\n\n"};' "$tmp2" > "$tmp"; \
  	(( $? == 0 )) || exit 1; \

	#####Copy web.xml to web.xml.old and copy $tmp contents to web.xml#####
	cp -p $1/web.xml $1/web.xml.old; \
	cat "$tmp" > $1/web.xml;
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
	if [ $2 = "reporting" ]
	then
		file=${base}/reporting/gratia-reporting/src/net/sf/gratia/reporting/Versions.java
		package="package net.sf.gratia.reporting;"
	else
		file=${base}/collector/gratia-services/net/sf/gratia/services/Versions.java
		package="package net.sf.gratia.services;"
	fi
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

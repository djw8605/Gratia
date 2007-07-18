PLATFORM := ${shell uname}

# tarbal version
gratia_reporting_version=v0.26
gratia_services_version=v0.26

# define useful variables
base=..

target=${base}/target

# module top location
collector=${base}/collector
common=${base}/common
docs=${base}/docs
probes=${base}/probe
reporting=${base}/reporting

# collector modules
services=${collector}/gratia-services
servlets=${collector}/gratia-servlets
soap=${collector}/gratia-soap
administration=${collector}/gratia-administration

# reporting modules
reports=${reporting}/gratia-reports
reportsrc=${reporting}/gratia-reporting
birtwars=${reporting}/birt-wars/birt-v20.war

# common modules
configuration=${common}/configuration
security=${common}/gratia-security
util=${common}/gratia-util
commonLib=${common}/lib

# check where we are as the tar file ownership cannot be set correctely on cygwin

ifeq ($(filter CYGWIN%,${PLATFORM}),)
tarowner=--owner=root --group=root
classpath=.:${services}/:${servlets}/:${soap}/:${administration}/:${reportsrc}/src:${security}/:${util}/
else
warn := ${shell echo "Building the tar file on cygwin will result in incorrect ownership" 1>&2 }
tarowner=
classpath=.\;${services}\;${servlets}\;${soap}\;${administration}\;${reportsrc}/src;${security}:${util}
endif


clean:
	find ${base}/ -name "*class" -exec rm -f {} \;
	find ${base}/ -name "*~" -exec rm -f {} \;
	find ${base}/ -name "*#" -exec rm -f {} \;
	find ${base}/ -name "#*" -exec rm -f {} \;
	rm -f -r ${target}
	mkdir ${target}

# services will eventually need the util package

services: ${target}/gratia-services.war

${target}/gratia-services.war:  ${services}/net/sf/gratia/services/*.java  ${services}/net/sf/gratia/storage/*.java ${services}/net/sf/gratia/services/web.xml
	rm -f -r war
	mkdir war
	mkdir war/META-INF
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/services
	mkdir war/WEB-INF/classes/net/sf/gratia/storage
	mkdir war/WEB-INF/lib
	rm -f -r net
	javac -cp ${classpath} -extdirs ${commonLib} ${services}/net/sf/gratia/services/*.java
	javac -cp ${classpath} -extdirs ${commonLib} ${services}/net/sf/gratia/storage/*.java
	rmic -vcompat -classpath ${classpath} -d . net.sf.gratia.services.JMSProxyImpl
	cp net/sf/gratia/services/* ${services}/net/sf/gratia/services
	rm -f -r net
	cp ${services}/net/sf/gratia/services/*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/storage/*.class war/WEB-INF/classes/net/sf/gratia/storage
	cp ${commonLib}/*.jar war/WEB-INF/lib
	rm -f  war/WEB-INF/lib/serv*
	cp ${services}/net/sf/gratia/services/web.xml war/WEB-INF/web.xml
	jar -cfM ${target}/gratia-services.war -C war .
	rm -f -r war


servlets: services
	rm -f -r war
	mkdir war
	mkdir war/META-INF
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/services
	mkdir war/WEB-INF/classes/net/sf/gratia/servlets
	mkdir war/WEB-INF/lib
	javac -cp ${classpath} -extdirs ${commonLib} ${servlets}/net/sf/gratia/servlets/*.java
	cp ${servlets}/net/sf/gratia/servlets/*.class war/WEB-INF/classes/net/sf/gratia/servlets
	cp ${services}/net/sf/gratia/services/Logging.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/Configuration.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/*Skel.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/*Stub.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/X*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/JMS*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${servlets}/net/sf/gratia/servlets/web.xml war/WEB-INF/web.xml
	jar -cfM ${target}/gratia-servlets.war -C war .
	rm -f -r war

soap: services
	rm -f -r war
	mkdir war
	mkdir war/META-INF
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/services
	mkdir war/WEB-INF/classes/net/sf/gratia/soap
	mkdir war/WEB-INF/lib
	mkdir war/wsdl
	javac -cp ${classpath} -extdirs ${commonLib} ${soap}/net/sf/gratia/soap/*.java
	cp ${soap}/net/sf/gratia/soap/*.class war/WEB-INF/classes/net/sf/gratia/soap
	cp ${services}/net/sf/gratia/services/Logging.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/Configuration.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/*Skel.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/*Stub.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/X*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/JMS*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${soap}/net/sf/gratia/soap/server-config.wsdd war/WEB-INF/server-config.wsdd
	cp ${soap}/net/sf/gratia/soap/web.xml war/WEB-INF/web.xml
	cp ${soap}/net/sf/gratia/soap/collector.wsdl war/wsdl
	cp ${commonLib}/axis*.jar war/WEB-INF/lib
	cp ${commonLib}/commons-dis*.jar war/WEB-INF/lib
	cp ${commonLib}/jaxrpc*.jar war/WEB-INF/lib
	cp ${commonLib}/saaj*.jar war/WEB-INF/lib
	cp ${commonLib}/wsdl*.jar war/WEB-INF/lib
	jar -cfM ${target}/gratia-soap.war -C war .
	rm -f -r war

administration: services ${target}/gratia-administration.war

${target}/gratia-administration.war: ${administration}/net/sf/gratia/administration/*.java ${administration}/net/sf/gratia/administration/web.xml \
   ${administration}/net/sf/gratia/administration/web.xml ${administration}/net/sf/gratia/administration/*.html \
   ${administration}/net/sf/gratia/administration/*.css ${administration}/net/sf/gratia/administration/*.jsp \
   ${administration}/net/sf/gratia/administration/images/*.gif ${docs}/*.html \
   ${commonLib}/mysql*.jar
	rm -f -r war
	mkdir war
	mkdir war/images
	mkdir war/META-INF
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/services
	mkdir war/WEB-INF/classes/net/sf/gratia/administration
	mkdir war/WEB-INF/lib
	javac -cp ${classpath} -extdirs ${commonLib} ${administration}/net/sf/gratia/administration/*.java
	cp ${administration}/net/sf/gratia/administration/images/*.gif war/images
	cp ${administration}/net/sf/gratia/administration/*.class war/WEB-INF/classes/net/sf/gratia/administration
	cp ${services}/net/sf/gratia/services/*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${administration}/net/sf/gratia/administration/web.xml war/WEB-INF/web.xml
	cp ${administration}/net/sf/gratia/administration/*.html war
	cp ${administration}/net/sf/gratia/administration/*.css war
	cp ${administration}/net/sf/gratia/administration/*.jsp war
	cp ${administration}/net/sf/gratia/administration/images/*.gif war/images
	cp ${docs}/*.html war
	cp ${commonLib}/mysql*.jar war/WEB-INF/lib
	jar -cfM ${target}/gratia-administration.war -C war .
	rm -f -r war

security: services
	rm -f -r war
	mkdir war
	mkdir war/META-INF
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/services
	mkdir war/WEB-INF/classes/net/sf/gratia/security
	mkdir war/WEB-INF/lib
	javac -cp ${classpath} -extdirs ${commonLib} ${security}/net/sf/gratia/security/*.java
	cp ${security}/net/sf/gratia/security/*.class war/WEB-INF/classes/net/sf/gratia/security
	cp ${services}/net/sf/gratia/services/Logging.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/Configuration.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/*Skel.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/*Stub.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/X*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/JMS*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${security}/net/sf/gratia/security/web.xml war/WEB-INF/web.xml
	cp ${commonLib}/mysql*.jar war/WEB-INF/lib
	jar -cfM ${target}/gratia-security.war -C war .
	rm -f -r war

util:
	rm -f -r war
	mkdir war
	mkdir war/META-INF
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/util
	mkdir war/WEB-INF/lib
	javac -cp ${classpath} -extdirs ${commonLib} ${util}/net/sf/gratia/util/*.java
	cp ${util}/net/sf/gratia/util/*.class war/WEB-INF/classes/net/sf/gratia/util
	cp ${commonLib}/*.jar war/WEB-INF/lib
	rm -f  war/WEB-INF/lib/util*
# ###	cp ${util}/net/sf/gratia/util/web.xml war/WEB-INF/web.xml
	jar -cfM ${target}/gratia-util.war -C war .
	rm -f -r war

reporting: util
	rm -f -r war
	mkdir war
	mkdir war/calendar
	mkdir war/images
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/util
	mkdir war/WEB-INF/classes/net/sf/gratia/reporting
	mkdir war/WEB-INF/classes/net/sf/gratia/reporting/exceptions
	mkdir war/WEB-INF/lib
	javac -cp ${classpath} -extdirs ${commonLib} ${util}/net/sf/gratia/util/*.java
	javac -cp ${classpath} -extdirs ${commonLib} ${reportsrc}/src/net/sf/gratia/reporting/exceptions/*.java
	javac -cp ${classpath} -extdirs ${commonLib} ${reportsrc}/src/net/sf/gratia/reporting/*.java
	cp ${util}/net/sf/gratia/util/*.class war/WEB-INF/classes/net/sf/gratia/util
	cp ${reportsrc}/WebContent/*.jsp war
	cp ${reportsrc}/WebContent/*.css war
	cp ${reportsrc}/src/net/sf/gratia/reporting/*.class war/WEB-INF/classes/net/sf/gratia/reporting
	cp ${reportsrc}/src/net/sf/gratia/reporting/exceptions/*.class war/WEB-INF/classes/net/sf/gratia/reporting/exceptions
	cp -R ${reportsrc}/WebContent/calendar/*.* war/calendar/
	cp -R ${reportsrc}/WebContent/images/*.* war/images/
	cp ${reportsrc}/src/net/sf/gratia/reporting/web.xml war/WEB-INF/web.xml
	cp ${commonLib}/*.jar war/WEB-INF/lib
	rm -f war/WEB-INF/lib/servlet-api.jar
	jar -cvfM ${target}/gratia-reporting.war -C war .
	rm -f -r war

reports:
	rm -f -r war
	mkdir war
	mkdir war/reports
	mkdir war/MenuConfig
	mkdir war/WEB-INF
	mkdir war/META-INF
	cp -R ${reports}/WebContent/MenuConfig/*.xml war/MenuConfig
	cp -R ${reports}/WebContent/*/production/*.rptdesign war/reports
	cp ${reports}/WebContent/WEB-INF/web.xml war/WEB-INF/web.xml
	jar -cvf ${target}/gratia-reports.war -C war .
	rm -f -r war

local: clean util services servlets soap reporting reports administration security 

release: clean util services servlets soap reporting reports administration security
	rm -f -r tarball
	#
	# create reporting tarball
	#
	mkdir tarball
	mkdir tarball/tomcat
	mkdir tarball/tomcat/v55
	mkdir tarball/tomcat/v55/gratia
	mkdir tarball/gratia
	mkdir tarball/gratia/gratia_reporting
#	mkdir tarball/var
#	mkdir tarball/var/data
#	mkdir tarball/var/logs
#	mkdir tarball/var/tmp
	cp ${configuration}/*gov ${configuration}/*xml ${configuration}/*properties tarball/tomcat/v55/gratia
	cp ${configuration}/*sql ${configuration}/*sh tarball/tomcat/v55/gratia
	chmod 700 tarball/tomcat/v55/gratia/post-install.sh
	perl -wapi.bak -e 's&CMD_(?:PREAMBLE|PREFIX|SUFFIX)&&g' tarball/tomcat/v55/gratia/post-install.sh
	rm -f tarball/tomcat/v55/gratia/post-install.sh.bak
	cp ${configuration}/truststore ${configuration}/keystore tarball/tomcat/v55/gratia
	cp ${configuration}/service-configuration.properties tarball/tomcat/v55/gratia/service-configuration.properties
	cp ${target}/gratia-reports.war tarball/gratia/gratia_reporting
	cp ${target}/gratia-reporting.war tarball/gratia/gratia_reporting
	cp ${target}/gratia-security.war tarball/gratia/gratia_reporting
	cp ${birtwars} tarball/gratia/gratia_reporting
	tar ${tarowner} -cvf ${target}/gratia_reporting_${gratia_reporting_version}.tar -C tarball .
	rm -f -r tarball
	#
	# create services tarball
	#
	mkdir tarball
	mkdir tarball/tomcat
	mkdir tarball/tomcat/v55
	mkdir tarball/tomcat/v55/gratia
	mkdir tarball/gratia
	mkdir tarball/gratia/gratia_services
	mkdir tarball/var
	mkdir tarball/var/data
	mkdir tarball/var/logs
	mkdir tarball/var/tmp
	cp ${configuration}/*gov ${configuration}/*xml ${configuration}/*properties tarball/tomcat/v55/gratia
	cp ${configuration}/*sql ${configuration}/*sh tarball/tomcat/v55/gratia
	perl -wapi.bak -e 's&CMD_(?:PREAMBLE|PREFIX|SUFFIX)&&g' tarball/tomcat/v55/gratia/post-install.sh
	rm -f tarball/tomcat/v55/gratia/post-install.sh.bak
	cp ${configuration}/truststore ${configuration}/keystore tarball/tomcat/v55/gratia
	cp ${configuration}/service-configuration.properties tarball/tomcat/v55/gratia/service-configuration.properties
	cp ${target}/gratia-services.war tarball/gratia/gratia_services
	cp ${target}/gratia-servlets.war tarball/gratia/gratia_services
	cp ${target}/gratia-soap.war tarball/gratia/gratia_services
	cp ${target}/gratia-administration.war tarball/gratia/gratia_services
	cp ${target}/gratia-security.war tarball/gratia/gratia_services
	tar ${tarowner} -cvf ${target}/gratia_services_${gratia_services_version}.tar -C tarball .
	rm -f -r tarball

gratia: clean util services servlets soap reporting reports administration security
	rm -f -r tarball
	mkdir tarball
	cp ${configuration}/UserVoMap.* ${configuration}/*xml ${configuration}/*properties tarball
	cp ${configuration}/*sql ${configuration}/*sh tarball
	chmod 700 tarball/post-install.sh
	cp ${configuration}/service-configuration.properties tarball/service-configuration.properties
	cp ${configuration}/configure-* ${configuration}/update-gratia-local tarball
	cp ${configuration}/truststore ${configuration}/keystore tarball
	tar -cvf ${target}/gratia.tar -C tarball .
	rm -f -r tarball

glr: clean util services servlets soap reporting reports administration security
	rm -f -r tarball
	mkdir tarball
	cp ${configuration}/UserVoMap.* ${configuration}/*xml ${configuration}/*properties tarball
	cp ${configuration}/*sql ${configuration}/*sh tarball
	chmod 700 tarball/post-install.sh
	cp ${configuration}/service-configuration.properties tarball/service-configuration.properties
	tar -cvf ${target}/gratia.tar -C tarball .
	rm -f -r tarball
	rm -f -r ~/Desktop/tomcat/webapps/gratia*
	cp ${target}/*war ~/Desktop/tomcat/webapps
	cp ${birtwars} ~/Desktop/tomcat/webapps

define defaultdirs
	mkdir -p gratia/ ; \
	mkdir -p gratia/var/; \
	mkdir -p gratia/var/data/; \
	mkdir -p gratia/var/logs/; \
	mkdir -p gratia/var/tmp/; \
	chmod -R og+rw gratia/var/; 
endef

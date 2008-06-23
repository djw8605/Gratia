PLATFORM := ${shell uname}

# tarbal version
version_default = v0.35

gratia_reporting_version := $(shell echo '$$Name: not supported by cvs2svn $$' | sed -e 's/^\$$\Name: \(v[^ ]*\) \$$/\1/' -e 's/-/./g')
gratia_services_version = $(gratia_reporting_version)

ifneq (,$(findstring $$Name,$(gratia_reporting_version)))
  override gratia_reporting_version := $(version_default)
endif

# define useful variables
base := $(shell cd .. >/dev/null 2>&1; /bin/pwd)

gratia_release_data := Gratia release: ${gratia_reporting_version} \
	Build date: $(shell date) \
	Build host: $(shell hostname -f) \
	Build path: ${base} \
	Builder: $(shell id |cut -d" " -f1)

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
staticScript=$(reportsrc)/WebContent/staticReports
birtDir=${reporting}/birt-wars
birtwarFile=birt-v220.war

# common modules
configuration=${common}/configuration
security=${common}/gratia-security
util=${common}/gratia-util
commonLib=${common}/lib

server_lib_jars = log4j- \
                  glite-security-trustmanager \
                  glite-security-util-java \
                  bcprov-

SERVER_LIBS = $(foreach d, $(server_lib_jars), $(wildcard $(commonLib)/$(d)*.jar))

# check where we are as the tar file ownership cannot be set correctly on cygwin
ifeq ($(filter CYGWIN%,${PLATFORM}),)
tarowner=--owner=root --group=root
classpath=.:${services}/:${servlets}/:${soap}/:${administration}/:${reportsrc}/src:${security}/:${util}/
else
warn := ${shell echo "Building the tar file on cygwin will result in incorrect ownership" 1>&2 }
tarowner=
classpath=.\;${services}\;${servlets}\;${soap}\;${administration}\;${reportsrc}/src;${security}:${util}
endif

util_lib=${target}/gratia-util.jar

define copy_util_lib
  mkdir -p war/WEB-INF/lib; cp -v ${util_lib} war/WEB-INF/lib/
endef

#-------------------------------------------------------------
all: gratia
	#------------------------------
	# --- Target: all COMPLETED ---
	#------------------------------

#-------------------------------------------------------------
clean:
	#----------------------
	# --- Target: clean ---
	#----------------------
	find ${base}/ -name "*class" -exec rm -f {} \;
	find ${base}/ -name "*~" -exec rm -f {} \;
	find ${base}/ -name "*#" -exec rm -f {} \;
	find ${base}/ -name "#*" -exec rm -f {} \;
	rm -f ${configuration}/build-stored-procedures.sql ${configuration}/build-stored-procedures_debug.sql
	rm -f -r ${target}
	mkdir ${target}
	# --- Target: clean COMPLETED ---

#-------------------------------------------------------------
# services will eventually need the util package
services: ${configuration}/build-stored-procedures.sql \
${configuration}/build-stored-procedures_debug.sql \
${target}/gratia-services.war
	# --- Target: services COMPLETED ---

#-------------------------------------------------------------
${configuration}/build-stored-procedures_debug.sql : ${configuration}/create_build-stored-procedures-sql
	#-------------------------------------------------------------------
	# --- Target: ${configuration}/build-stored-procedures_debug.sql ---
	#-------------------------------------------------------------------
	$(<) -dt > $(@)
	# --- Target: ${configuration}/build-stored-procedures_debug.sql COMPLETED ---

#-------------------------------------------------------------
${configuration}/build-stored-procedures.sql : ${configuration}/create_build-stored-procedures-sql
	#-------------------------------------------------------------
	# --- Target: ${configuration}/build-stored-procedures.sql ---
	#-------------------------------------------------------------
	$(<) -d > $(@)
	# --- Target: ${configuration}/build-stored-procedures.sql COMPLETED ---

#-------------------------------------------------------------
${target}/gratia-services.war:  ${services}/net/sf/gratia/services/*.java  \
                                ${services}/net/sf/gratia/storage/*.java \
                                ${services}/net/sf/gratia/services/web.xml \
                                util
	#------------------------------------
	# --- Target: gratia-services.war ---
	#------------------------------------
	rm -rf   war
	mkdir -p war/META-INF
	mkdir -p war/WEB-INF/classes/net/sf/gratia/services
	mkdir -p war/WEB-INF/classes/net/sf/gratia/storage
	mkdir -p war/WEB-INF/classes/net/sf/gratia/util
	mkdir -p war/WEB-INF/lib
	echo "${gratia_release_data}" >war/gratia-release
	rm -f -r net
	javac -cp ${classpath} -extdirs ${commonLib} ${services}/net/sf/gratia/services/*.java
	javac -cp ${classpath} -extdirs ${commonLib} ${services}/net/sf/gratia/storage/*.java
	rmic -vcompat -classpath ${classpath} -d . net.sf.gratia.services.JMSProxyImpl
	cp net/sf/gratia/services/* ${services}/net/sf/gratia/services
	rm -f -r net
	cp ${services}/log4j.properties war/WEB-INF/classes/
	cp ${services}/ehcache.xml war/WEB-INF/classes/
	cp ${services}/c3p0.properties war/WEB-INF/classes/
	cp ${services}/net/sf/gratia/services/*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/storage/*.class war/WEB-INF/classes/net/sf/gratia/storage
	cp ${util}/net/sf/gratia/util/Logging.class war/WEB-INF/classes/net/sf/gratia/util
	cp ${commonLib}/*.jar war/WEB-INF/lib
	rm -f  war/WEB-INF/lib/serv*
	cp ${services}/net/sf/gratia/services/web.xml war/WEB-INF/web.xml
	${copy_util_lib}
	jar -cfM ${target}/gratia-services.war -C war .
	rm -rf  war
	# --- Target: gratia-services.war COMPLETED  ---


#-------------------------------------------------------------
servlets: services util
	#-------------------------
	# --- Target: servlets ---
	#-------------------------
	rm -rf r war
	mkdir -p war/META-INF
	mkdir -p war/WEB-INF/lib
	mkdir -p war/WEB-INF/classes/net/sf/gratia/services
	mkdir -p war/WEB-INF/classes/net/sf/gratia/servlets
	mkdir -p war/WEB-INF/classes/net/sf/gratia/util
	echo "${gratia_release_data}" >war/gratia-release
	javac -cp ${classpath} -extdirs ${commonLib} ${servlets}/net/sf/gratia/servlets/*.java
	cp ${servlets}/net/sf/gratia/servlets/*.class war/WEB-INF/classes/net/sf/gratia/servlets
	cp ${services}/net/sf/gratia/services/*Skel.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/*Stub.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/JMS*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${util}/net/sf/gratia/util/Logging.class war/WEB-INF/classes/net/sf/gratia/util
	cp ${servlets}/net/sf/gratia/servlets/web.xml war/WEB-INF/web.xml
	${copy_util_lib}
	jar -cfM ${target}/gratia-servlets.war -C war .
	rm -rf  war
	# --- Target: services COMPLETED  ---

#------------------------------------------------------------------
soap: services
	#---------------------
	# --- Target: soap ---
	#---------------------
	rm -rf  war
	mkdir war
	mkdir -p war/wsdl
	mkdir -p war/META-INF
	mkdir -p war/WEB-INF/lib
	mkdir -p war/WEB-INF/classes/net/sf/gratia/services
	mkdir -p war/WEB-INF/classes/net/sf/gratia/soap
	echo "${gratia_release_data}" >war/gratia-release
	javac -cp ${classpath} -extdirs ${commonLib} ${soap}/net/sf/gratia/soap/*.java
	cp ${soap}/net/sf/gratia/soap/*.class war/WEB-INF/classes/net/sf/gratia/soap
	cp ${services}/net/sf/gratia/services/*Skel.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/*Stub.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/services/JMS*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${soap}/net/sf/gratia/soap/server-config.wsdd war/WEB-INF/server-config.wsdd
	cp ${soap}/net/sf/gratia/soap/web.xml war/WEB-INF/web.xml
	cp ${soap}/net/sf/gratia/soap/collector.wsdl war/wsdl
	cp ${commonLib}/axis*.jar war/WEB-INF/lib
	cp ${commonLib}/commons-discovery*.jar war/WEB-INF/lib
	cp ${commonLib}/jaxrpc*.jar war/WEB-INF/lib
	cp ${commonLib}/saaj*.jar war/WEB-INF/lib
	cp ${commonLib}/wsdl*.jar war/WEB-INF/lib
	${copy_util_lib}
	jar -cfM ${target}/gratia-soap.war -C war .
	rm -rf war
	# --- Target: soap COMPLETED  ---

#------------------------------------------------------------------
administration: security ${target}/gratia-administration.war
	# --- Target: administration COMPLETED  ---

#------------------------------------------------------------------
${target}/gratia-administration.war: ${administration}/net/sf/gratia/administration/*.java \
                                     ${administration}/net/sf/gratia/administration/web.xml \
                                     ${administration}/WebContent/*.html \
                                     ${administration}/WebContent/*.css \
                                     ${administration}/WebContent/*.jsp \
                                     ${administration}/WebContent/*.js \
                                     ${administration}/WebContent/images/*.gif \
                                     ${commonLib)/tar.jar \
                                     ${commonLib)/bzip2.jar \
                                     ${commonLib}/mysql*.jar util
	#----------------------------------------------------
	# --- Target: ${target}/gratia-administration.war ---
	#----------------------------------------------------
	rm   -rf war
	mkdir -p war/images
	mkdir -p war/META-INF
	mkdir -p war/WEB-INF/classes/net/sf/gratia/services
	mkdir -p war/WEB-INF/classes/net/sf/gratia/administration
	mkdir -p war/WEB-INF/classes/net/sf/gratia/util
	mkdir -p war/WEB-INF/lib
	echo "${gratia_release_data}" >war/gratia-release
	javac -cp ${classpath} -extdirs ${commonLib} ${administration}/net/sf/gratia/administration/*.java
	cp ${administration}/net/sf/gratia/administration/*.class war/WEB-INF/classes/net/sf/gratia/administration
	cp ${services}/net/sf/gratia/services/*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${administration}/net/sf/gratia/administration/web.xml war/WEB-INF/web.xml
	cp ${administration}/WebContent/*.html war
	cp ${administration}/WebContent/*.css war
	cp ${administration}/WebContent/*.jsp war
	cp ${administration}/WebContent/*.js war
	cp ${administration}/WebContent/images/*.gif war/images
	# cp ${docs}/*.html war # included in the administration WebContent
	cp ${util}/net/sf/gratia/util/Logging.class war/WEB-INF/classes/net/sf/gratia/util
	cp ${commonLib}/axis.jar ${commonLib}/bcprov-jdk14-133.jar ${commonLib}/commons-discovery*.jar war/WEB-INF/lib
	cp ${commonLib}/commons-logging*.jar ${commonLib}/glite-security-trustmanager.jar  war/WEB-INF/lib
	cp ${commonLib}/glite-security-util-java.jar ${commonLib}/glite-security-voms-admin-server.jar war/WEB-INF/lib
	cp ${commonLib}/jaxrpc.jar ${commonLib}/log4j-*.jar ${commonLib}/saaj.jar ${commonLib}/xercesImpl.jar war/WEB-INF/lib
	cp ${commonLib}/mysql*.jar ${commonLib}/tar.jar ${commonLib}/bzip2.jar war/WEB-INF/lib
	cp ${target}/gratiaSecurity.jar war/WEB-INF/lib
	${copy_util_lib}
	jar -cfM ${target}/gratia-administration.war -C war .
	rm -rf  war
	# --- Target: ${target}/gratia-administration.war COMPLETED ---

#------------------------------------------------------------------
util:
	# --------------------
	# --- Target: util ---
	# --------------------
	rm -rf  jar-lib
	mkdir -p jar-lib
	mkdir -p jar-lib/net/sf/gratia/util
	javac -Xlint:unchecked -cp ${classpath} -extdirs ${commonLib} ${util}/net/sf/gratia/util/*.java
	cp ${util}/net/sf/gratia/util/*.class jar-lib/net/sf/gratia/util
	jar -cfM ${target}/gratia-util.jar -C jar-lib .
	rm -rf jar-lib
	# --- Target: util COMPLETED ---

#------------------------------------------------------------------
security: services util
	# ----------------------------
	# --- Target: security ---
	# ----------------------------
	rm   -rf jar-lib
	mkdir -p jar-lib/net/sf/gratia/util
	mkdir -p jar-lib/net/sf/gratia/services
	mkdir -p jar-lib/net/sf/gratia/vomsSecurity
	javac -cp ${classpath} -extdirs ${commonLib} ${util}/net/sf/gratia/util/*.java
	javac -cp ${classpath} -extdirs ${commonLib} ${security}/net/sf/gratia/vomsSecurity/*.java
	cp ${util}/net/sf/gratia/util/*.class jar-lib/net/sf/gratia/util
	cp ${security}/net/sf/gratia/vomsSecurity/*.class jar-lib/net/sf/gratia/vomsSecurity
	cp ${services}/net/sf/gratia/services/JMS*.class jar-lib/net/sf/gratia/services
	cp ${util}/net/sf/gratia/util/Logging.class jar-lib/net/sf/gratia/services
	jar -cvfM ${target}/gratiaSecurity.jar -C jar-lib .
	rm -rf jar-lib
	# --- Target: security COMPLETED ---

#-------------------------------------------------------------
reporting: birt-reporting
	# -------------------------
	# --- Target: reporting ---
	# -------------------------
	rm -rf   war
	mkdir -p war/documents
	mkdir -p war/WEB-INF/attachments
	echo "${gratia_release_data}" >war/gratia-release
	# --- expand the birt war file  -----
	cp ${birtDir}/${birtwarFile} war/.
	cd war && jar xf ${birtwarFile}
	rm -f war/${birtwarFile}
	rm -f war/test.rptdesign
	rm -f war/test1.rptdesign
	# --- copy in needed libraries ---
	cp -p ${target}/gratiaReporting.jar                    war/WEB-INF/lib/.
	cp -p ${target}/gratiaReporting.jar                    war/scriptlib/.
	cp -p ${commonLib}/mysql-connector-java-5.0.4-bin.jar  war/WEB-INF/lib/.
	cp -p ${commonLib}/mysql-connector-java-5.0.4-bin.jar  war/WEB-INF/platform/plugins/org.eclipse.birt.report.data.oda.jdbc*/drivers/.
	cp -p ${commonLib}/dom4j-1.6.1.jar                     war/WEB-INF/lib/.
	# --- copy needed jsp and css files ----
	cp -p ${reportsrc}/WebContent/*.jsp                    war/.
	cp -p ${reportsrc}/WebContent/*.html                   war/.
	cp -p ${reportsrc}/WebContent/*.css                    war/.
	# --- copy directories paths ---
	cp -R ${reportsrc}/WebContent/calendar                 war/.
	cp -R ${reportsrc}/WebContent/images                   war/.
	cp -R ${reportsrc}/WebContent/tooltip                  war/.
	# --- copy the web.xml  file ---
	jar -cfM ${target}/gratia-reporting.war -C war .
	rm -rf war
	# --- Target: reporting COMPLETED ---

#-------------------------------------------------------------
reports:
	# -----------------------
	# --- Target: reports ---
	# -----------------------
	rm   -rf war
	mkdir -p war/reports
	mkdir -p war/MenuConfig
	mkdir -p war/WEB-INF
	mkdir -p war/META-INF
	echo "${gratia_release_data}" >war/gratia-release
	cp -p ${reports}/WebContent/WEB-INF/web.xml          war/WEB-INF/web.xml
	cp -p ${reports}/WebContent/MenuConfig/*.xml         war/MenuConfig
	cp -p ${reports}/WebContent/reports/production-osg/*.rptdesign            war/reports
	-cp -p ${reports}/WebContent/reports/production-osg/*.rptlibrary war/reports
	-cp -p ${reports}/WebContent/reports/production-fermi/*.rptlibrary war/reports
	cp -p ${reports}/WebContent/reports/production-osgdaily/*.rptdesign            war/reports
	cp -p ${reports}/WebContent/reports/production-fermi/*.rptdesign      war/reports
	${copy_util_lib}
	jar -cvf ${target}/gratia-reports.war -C war .
	rm -rf war
	# --- Target: reports COMPLETED ---

#-------------------------------------------------------------
birt-reporting: 
	# ------------------------------
	# --- Target: birt-reporting ---
	# ------------------------------
	rm   -rf  jar-lib
	mkdir -p jar-lib/WEB-INF
	mkdir -p jar-lib/WEB-INF/tlds
	mkdir -p jar-lib/net/sf/gratia/util
	mkdir -p jar-lib/net/sf/gratia/reporting/exceptions
	javac -cp ${classpath} -extdirs ${commonLib} ${util}/net/sf/gratia/util/*.java
	javac -cp ${classpath} -extdirs ${commonLib} ${reportsrc}/src/net/sf/gratia/reporting/exceptions/*.java
	javac -cp ${classpath} -extdirs ${commonLib} ${reportsrc}/src/net/sf/gratia/reporting/*.java
	cp ${util}/net/sf/gratia/util/*.class jar-lib/net/sf/gratia/util
	cp ${reportsrc}/src/net/sf/gratia/reporting/*.class jar-lib/net/sf/gratia/reporting
	cp ${reportsrc}/src/net/sf/gratia/reporting/exceptions/*.class jar-lib/net/sf/gratia/reporting/exceptions
	jar -cvfM ${target}/gratiaReporting.jar -C jar-lib .
	rm -rf jar-lib
	# --- Target: birt-reporting COMPLETED ---


#---------------------------------------------------------------
local: clean util services servlets soap reporting reports administration 
	# --- Target: local COMPLETED ---
#---------------------------------------------------------------

release: gratia
	# --------------------------------
	# --- Target: release          ---
	# --- create reporting tarball ---
	# --------------------------------
	rm   -rf tarball; \
	mkdir -p tarball/tomcat/v55/gratia; \
	mkdir -p tarball/gratia/gratia_reporting; \
	echo "${gratia_release_data}" >tarball/tomcat/v55/gratia/gratia-release
	cp ${configuration}/*gov ${configuration}/*xml ${configuration}/*properties $(staticScript)/*.py tarball/tomcat/v55/gratia; \
	chmod 755 tarball/tomcat/v55/gratia/*.py
	cp ${configuration}/*sql ${configuration}/*sh tarball/tomcat/v55/gratia; \
	chmod 700 tarball/tomcat/v55/gratia/post-install.sh; \
	perl -wapi.bak -e 's&CMD_(?:PREAMBLE|PREFIX|SUFFIX)&&g' tarball/tomcat/v55/gratia/post-install.sh; \
	rm -f tarball/tomcat/v55/gratia/post-install.sh.bak; \
	cp ${configuration}/truststore ${configuration}/keystore tarball/tomcat/v55/gratia; \
	perl -wape 's&^(\s*gratia\.services\.version\s*=\s*).*$$&$$1${gratia_services_version}&; s&^(\s*gratia\.reporting\.version\s*=\s*).*$$&$$1${gratia_reporting_version}&; ' ${configuration}/service-configuration.properties > tarball/tomcat/v55/gratia/service-configuration.properties && chmod 600 tarball/tomcat/v55/gratia/service-configuration.properties; \
	cp ${target}/gratia-reports.war tarball/gratia/gratia_reporting; \
	cp ${target}/gratia-reporting.war tarball/gratia/gratia_reporting; \
	tar ${tarowner} -cvf ${target}/gratia_reporting_${gratia_reporting_version}.tar -C tarball .; \
	rm -rf tarball
	# -------------------------------
	# --- Target: release         ---
	# --- create services tarball ---
	# -------------------------------
	rm -rf tarball; \
	mkdir -p tarball/tomcat/v55/gratia; \
	mkdir -p tarball/gratia/gratia_services; \
	mkdir -p tarball/var/data; \
	mkdir -p tarball/var/logs; \
	mkdir -p tarball/var/tmp; \
	echo "${gratia_release_data}" >tarball/tomcat/v55/gratia/gratia-release
	cp ${configuration}/voms-servers    tarball/tomcat/v55/gratia; 
	chmod 644 tarball/tomcat/v55/gratia/voms-servers;
	cp ${security}/scripts/voms-server.sh   tarball/tomcat/v55/gratia; 
	chmod 744 tarball/tomcat/v55/gratia/voms-server.sh;
	cp ${configuration}/*gov ${configuration}/*xml ${configuration}/*properties  $(staticScript)/*.py tarball/tomcat/v55/gratia; \
	chmod 755 tarball/tomcat/v55/gratia/*.py
	cp ${configuration}/*sql ${configuration}/*sh tarball/tomcat/v55/gratia; \
	perl -wapi.bak -e 's&CMD_(?:PREAMBLE|PREFIX|SUFFIX)&&g' tarball/tomcat/v55/gratia/post-install.sh; \
	rm -f tarball/tomcat/v55/gratia/post-install.sh.bak; \
	cp ${configuration}/truststore ${configuration}/keystore tarball/tomcat/v55/gratia; \
	perl -wape 's&^(\s*gratia\.services\.version\s*=\s*).*$$&$$1${gratia_services_version}&; s&^(\s*gratia\.reporting\.version\s*=\s*).*$$&$$1${gratia_reporting_version}&; ' ${configuration}/service-configuration.properties > tarball/tomcat/v55/gratia/service-configuration.properties && chmod 600 tarball/tomcat/v55/gratia/service-configuration.properties; \
	cp ${target}/gratia-services.war tarball/gratia/gratia_services; \
	cp ${target}/gratia-servlets.war tarball/gratia/gratia_services; \
	cp ${target}/gratia-soap.war tarball/gratia/gratia_services; \
	cp ${target}/gratia-administration.war tarball/gratia/gratia_services; \
	tar ${tarowner} -cvf ${target}/gratia_services_${gratia_services_version}.tar -C tarball .; \
	rm -f -r tarball
	# --- Target: release COMPLETED ---

server_lib:
	# ------------------------------
	# --- Target: server_lib     ---
	# ------------------------------
	rm -f -r tarball; \
	mkdir tarball; \
	cp -v $(SERVER_LIBS) tarball/; \
	tar -cvf ${target}/server_lib.tar -C tarball .; \
        rm -f -r tarball
	# --- Target: server_lib COMPLETED ---

#-------------------------------------------------------------
gratia: clean util services servlets soap reporting reports administration server_lib
	# ------------------------------
	# --- Target: gratia         ---
	# ------------------------------
	rm -f -r tarball; \
	mkdir tarball; \
	echo "${gratia_release_data}" >tarball/gratia-release
	cp ${configuration}/UserVoMap.* ${configuration}/*xml $(staticScript)/*.py ${configuration}/*properties tarball; \
	cp ${configuration}/*sql ${configuration}/*sh tarball; \
	cp ${security}/scripts/voms-server.sh         tarball; chmod 744 tarball/voms-server.sh
	chmod 755 tarball/*.py
	chmod 700 tarball/post-install.sh; \
	perl -wape 's&^(\s*gratia\.services\.version\s*=\s*).*$$&$$1${gratia_services_version}&; s&^(\s*gratia\.reporting\.version\s*=\s*).*$$&$$1${gratia_reporting_version}&; ' ${configuration}/service-configuration.properties > tarball/service-configuration.properties && chmod 600 tarball/service-configuration.properties; \
	cp ${configuration}/configure-* ${configuration}/update-gratia-local tarball; \
	cp ${configuration}/truststore ${configuration}/keystore tarball; \
	cp ${configuration}/cleanup_server_lib tarball/; \
	cp ${configuration}/voms-servers tarball; \
	tar -cvf ${target}/gratia.tar -C tarball .; \
	rm -f -r tarball
	# --- Target: gratia COMPLETED ---

#-------------------------------------------------------------
define defaultdirs
	mkdir -p gratia/ ; \
	mkdir -p gratia/var/; \
	mkdir -p gratia/var/data/; \
	mkdir -p gratia/var/lock/; \
	mkdir -p gratia/var/logs/; \
	mkdir -p gratia/var/tmp/; \
	chmod -R og+rw gratia/var/; 
endef

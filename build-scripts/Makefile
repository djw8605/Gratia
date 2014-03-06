PLATFORM := $(shell uname)
# Makefile is not used any more. Please refer to ANT and build.xml

# tarball version
version_default = v1.13-1

gratia_reporting_version := $(shell echo '$$HeadURL$$' | sed -e 's?^\$$\HeadURL: .*gratia/[^/]*tags/\(v[^ ]*\)/build-scripts/Makefile \$$?\1?' -e 's/-/./g' )
gratia_services_version = $(gratia_reporting_version)

ifneq (,$(findstring $$HeadURL,$(gratia_reporting_version)))
  override gratia_reporting_version := $(version_default)
endif
override gratia_reporting_version := $(version_default)
override gratia_services_version  := $(version_default)

# define useful variables
base := $(shell cd .. >/dev/null 2>&1; /bin/pwd)

gratia_release_data := Gratia release: $(gratia_reporting_version) \
	Build date: $(shell date) \
	Build host: $(shell hostname -f) \
	Build path: $(base) \
	Builder: $(shell id |cut -d" " -f1)

target=$(base)/target
output-dir=./output-dir
classes-dir=$(output-dir)/classes
war-dir=$(output-dir)/wars
jar-dir=$(output-dir)/jars

# module top location
collector=$(base)/collector
common=$(base)/common
docs=$(base)/docs
probes=$(base)/probe
reporting=$(base)/reporting

# collector modules
services=$(collector)/gratia-services
servlets=$(collector)/gratia-servlets
soap=$(collector)/gratia-soap
registration=$(collector)/gratia-registration
administration=$(collector)/gratia-administration
testtimeout=$(collector)/gratia-testtimeout
testbacklog=$(collector)/gratia-testbacklog

# reporting modules
reports=$(reporting)/gratia-reports
reportsrc=$(reporting)/gratia-reporting
staticScript=$(reportsrc)/WebContent/staticReports
birtDir=$(reporting)/birt-wars
#birtwarFile=birt-v231-N20080820.war
#birtwarFile=birt-v231-N20080818.war
#birtwarFile=birt-v261.war
birtwarFile=birt-v431.war
# common modules
configuration=$(common)/configuration
security=$(common)/gratia-security
util=$(common)/gratia-util
commonLib=$(common)/lib

server_lib_jars = glite-security-trustmanager \
                  glite-security-util-java \
                  bcprov-
SERVER_LIBS = $(foreach d, $(server_lib_jars), $(wildcard $(commonLib)/$(d)*.jar)) $(target)/gratiaSecurity.jar

slf4j_lib_jars= slf4j-api \
		slf4j-log4j12
SLF4J_LIBS = $(foreach d, $(slf4j_lib_jars), $(wildcard $(commonLib)/$(d)*.jar))

common_lib_jars = log4j- \
                  commons-logging-1
COMMON_LIBS = $(foreach d, $(common_lib_jars), $(wildcard $(commonLib)/$(d)*.jar))

shared_lib_jars = 
SHARED_LIBS = $(foreach d, $(shared_lib_jars), $(wildcard $(commonLib)/$(d)*.jar))

COMMON_CLASSES = $(classes-dir)/net/sf/gratia/util/TidiedDailyRollingFileAppender$$DatedFileFilter.class \
                 $(classes-dir)/net/sf/gratia/util/TidiedDailyRollingFileAppender.class

# check where we are as the tar file ownership cannot be set correctly on cygwin
ifeq ($(filter CYGWIN%,$(PLATFORM)),)
  tarowner=--owner=root 
  classpath=.:$(classes-dir):$(services)/:$(servlets)/:$(soap)/:$(administration)/:$(reportsrc)/src:$(security)/:$(util)/
else
  warn := $(shell echo "Building the tar file on cygwin will result in incorrect ownership" 1>&2 )
  tarowner=
  classpath=.\;$(classes-dir);$(services)\;$(servlets)\;$(soap)\;$(administration)\;$(reportsrc)/src;$(security);$(util)
endif

util_lib=$(target)/gratia-util.jar

define copy_util_lib
  mkdir -p $(war-dir)/$@/WEB-INF/lib; cp -v $(util_lib) $(war-dir)/$@/WEB-INF/lib/
endef

define copy2_util_lib
  mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/lib; cp -v $(util_lib) $(war-dir)/$(name_from_war)/WEB-INF/lib/
endef

define name_from_war
$(subst .war,,$(@F))
endef

define name_from_jar
$(subst .jar,,$(@F))
endef

ifeq ($(target),) 
   $(error The target variable can not be null)
endif

ifeq ($(output-dir),) 
   $(error The output-dir variable can not be null)
endif

ifeq ($(war-dir),) 
   $(error The war-dir variable can not be null)
endif

ifeq ($(jar-dir),) 
   $(error The jar-dir variable can not be null)
endif

ifeq ($(classes-dir),) 
   $(error The classes-dir variable can not be null)
endif

#-------------------------------------------------------------
all: gratia
	#------------------------------
	# --- Target: all COMPLETED at $(shell date) ---
	#------------------------------

#-------------------------------------------------------------
clean:
	#----------------------
	# --- Target: clean ---
	#----------------------
	find $(base)/ -name "*class" -exec rm -f {} \;
	find $(base)/ -name "*~" -exec rm -f {} \;
	find $(base)/ -name "*#" -exec rm -f {} \;
	find $(base)/ -name "#*" -exec rm -f {} \;
	rm -f $(configuration)/build-stored-procedures.sql $(configuration)/build-stored-procedures_debug.sql
	rm -rf $(war-dir) $(jar-dir) $(classes-dir) $(target) $(output-dir)
	# --- Target: clean COMPLETED at $(shell date) ---

#-------------------------------------------------------------
# services will eventually need the util package
# services:  $(configuration)/build-stored-procedures.sql \
# $(configuration)/build-stored-procedures_debug.sql \

services: $(target)/gratia-services.war
	#------------------------------------
	# --- Target: services COMPLETED at $(shell date) ---
	#------------------------------------

#-------------------------------------------------------------
# $(configuration)/build-stored-procedures_debug.sql : $(configuration)/create_build-stored-procedures-sql
	#-------------------------------------------------------------------
	# --- Target: $(configuration)/build-stored-procedures_debug.sql ---
	#-------------------------------------------------------------------
	#$(<) -dt > $(@)
	# --- Target: $(configuration)/build-stored-procedures_debug.sql COMPLETED at $(shell date) ---

#-------------------------------------------------------------
# $(configuration)/build-stored-procedures.sql : $(configuration)/create_build-stored-procedures-sql
	#-------------------------------------------------------------
	# --- Target: $(configuration)/build-stored-procedures.sql ---
	#-------------------------------------------------------------
	#$(<) -d > $(@)
	# --- Target: $(configuration)/build-stored-procedures.sql COMPLETED at $(shell date) ---


$(services)/net/sf/gratia/services/Versions.java: ./createVersion.sh ./Makefile
	./createVersion.sh  services $(gratia_services_version) $@

$(reportsrc)/src/net/sf/gratia/reporting/Versions.java: ./createVersion.sh ./Makefile
	./createVersion.sh  reporting $(gratia_reporting_version) $@

#-------------------------------------------------------------
$(target)/gratia-services.war:  $(services)/net/sf/gratia/services/*.java  \
                                $(services)/net/sf/gratia/storage/*.java \
                                $(services)/net/sf/gratia/services/web.xml \
				$(services)/ehcache.xml $(services)/c3p0.properties \
				$(commonLib)/*.jar \
                                $(util_lib) $(services)/net/sf/gratia/services/Versions.java
	#------------------------------------
	# --- Target: gratia-services.war ---
	#------------------------------------
	rm -rf $(war-dir)/$(name_from_war)
	mkdir -p $(classes-dir) $(target)
	mkdir -p $(war-dir)/$(name_from_war)/META-INF
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/storage
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/util
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/lib
	echo "$(gratia_release_data)" >$(war-dir)/$(name_from_war)/gratia-release
	javac -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(services)/net/sf/gratia/services/*.java
	javac -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(services)/net/sf/gratia/storage/*.java
	rmic -vcompat -classpath $(classpath) -d $(classes-dir) net.sf.gratia.services.JMSProxyImpl
	cp $(services)/ehcache.xml $(war-dir)/$(name_from_war)/WEB-INF/classes/
	cp $(services)/c3p0.properties $(war-dir)/$(name_from_war)/WEB-INF/classes/
	cp $(classes-dir)/net/sf/gratia/services/*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/storage/*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/storage
	cp $(classes-dir)/net/sf/gratia/util/Logging.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/util
	cp $(commonLib)/*.jar $(war-dir)/$(name_from_war)/WEB-INF/lib
	rm -f  $(war-dir)/$(name_from_war)/WEB-INF/lib/{serv,log4j}*
	cp $(services)/net/sf/gratia/services/web.xml $(war-dir)/$(name_from_war)/WEB-INF/web.xml
	$(copy2_util_lib)
	jar -cfM $(target)/gratia-services.war -C $(war-dir)/$(name_from_war) .
	# --- Target: $@ COMPLETED at $(shell date) ---


#-------------------------------------------------------------
testtimeout: $(target)/gratia-testtimeout.war

#-------------------------------------------------------------
$(target)/gratia-testtimeout.war: $(util_lib) $(testtimeout)/net/sf/gratia/testtimeout/*.java $(testtimeout)/net/sf/gratia/testtimeout/web.xml $(target)/gratia-services.war 
	#-------------------------
	# --- Target: servlets ---
	#-------------------------
	rm -rf $(war-dir)/$(name_from_war)
	mkdir -p $(classes-dir) $(target)
	mkdir -p $(war-dir)/$(name_from_war)/META-INF
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/lib
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/testtimeout
	echo "$(gratia_release_data)" >$(war-dir)/$(name_from_war)/gratia-release
	javac -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(testtimeout)/net/sf/gratia/testtimeout/*.java
	cp $(classes-dir)/net/sf/gratia/testtimeout/*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/testtimeout
	cp $(testtimeout)/net/sf/gratia/testtimeout/web.xml $(war-dir)/$(name_from_war)/WEB-INF/web.xml
	#
#	cp $(classes-dir)/net/sf/gratia/services/*Skel.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
#	cp $(classes-dir)/net/sf/gratia/services/*Stub.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
#	cp $(classes-dir)/net/sf/gratia/services/JMS*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
#	cp $(classes-dir)/net/sf/gratia/services/AccessException.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/util/Logging.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/util
	$(copy2_util_lib)
	jar -cfM $(target)/gratia-testtimeout.war -C $(war-dir)/$(name_from_war) .
	# --- Target: services COMPLETED at $(shell date) ---

#-------------------------------------------------------------
testbacklog: $(target)/gratia-testbacklog.war

#-------------------------------------------------------------
$(target)/gratia-testbacklog.war: $(util_lib) $(servlets)/net/sf/gratia/servlets/RMIHandlerServlet.java $(testbacklog)/net/sf/gratia/testbacklog/*.java $(testbacklog)/net/sf/gratia/testbacklog/web.xml $(target)/gratia-services.war 
	#-------------------------
	# --- Target: servlets for backlog testing ---
	#-------------------------
	rm -rf $(war-dir)/$(name_from_war)
	mkdir -p $(classes-dir) $(target)
	mkdir -p $(war-dir)/$(name_from_war)/META-INF
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/lib
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/testbacklog
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/servlets
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/util
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/storage
	echo "$(gratia_release_data)" >$(war-dir)/$(name_from_war)/gratia-release
	javac -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(testbacklog)/net/sf/gratia/testbacklog/*.java $(servlets)/net/sf/gratia/servlets/RMIHandlerServlet.java 
	cp $(classes-dir)/net/sf/gratia/testbacklog/*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/testbacklog
	cp $(classes-dir)/net/sf/gratia/servlets/*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/servlets
	cp $(testbacklog)/net/sf/gratia/testbacklog/web.xml $(war-dir)/$(name_from_war)/WEB-INF/web.xml
	#
	cp $(classes-dir)/net/sf/gratia/services/*Skel.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/services/*Stub.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/services/JMS*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/services/AccessException.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/util/Logging.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/util
	$(copy2_util_lib)
	jar -cfM $(target)/gratia-testbacklog.war -C $(war-dir)/$(name_from_war) .
	# --- Target: services COMPLETED at $(shell date) ---


#-------------------------------------------------------------
servlets: $(target)/gratia-servlets.war

#-------------------------------------------------------------
$(target)/gratia-servlets.war: $(util_lib) $(servlets)/net/sf/gratia/servlets/*.java $(servlets)/net/sf/gratia/servlets/web.xml $(target)/gratia-services.war
	#-------------------------
	# --- Target: servlets ---
	#-------------------------
	rm -rf $(war-dir)/$(name_from_war)
	mkdir -p $(classes-dir) $(target)
	mkdir -p $(war-dir)/$(name_from_war)/META-INF
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/lib
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/servlets
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/util
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/storage
	echo "$(gratia_release_data)" >$(war-dir)/$(name_from_war)/gratia-release
	javac -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(servlets)/net/sf/gratia/servlets/*.java
	cp $(classes-dir)/net/sf/gratia/servlets/*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/servlets
	cp $(servlets)/net/sf/gratia/servlets/web.xml $(war-dir)/$(name_from_war)/WEB-INF/web.xml
	#
	cp $(classes-dir)/net/sf/gratia/services/*Skel.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/services/*Stub.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/services/JMS*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/services/AccessException.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/util/Logging.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/util
	$(copy2_util_lib)
	jar -cfM $(target)/gratia-servlets.war -C $(war-dir)/$(name_from_war) .
	# --- Target: services COMPLETED at $(shell date) ---

#------------------------------------------------------------------
soap: $(target)/gratia-soap.war

$(target)/gratia-soap.war: services
	#---------------------
	# --- Target: soap ---
	#---------------------
	rm -rf $(war-dir)/$(name_from_war)
	mkdir -p $(classes-dir) $(target) $(war-dir)
	mkdir -p $(war-dir)/$(name_from_war)/wsdl
	mkdir -p $(war-dir)/$(name_from_war)/META-INF
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/lib
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/soap
	echo "$(gratia_release_data)" >$(war-dir)/$(name_from_war)/gratia-release
	javac -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(soap)/net/sf/gratia/soap/*.java
	cp $(classes-dir)/net/sf/gratia/soap/*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/soap
	cp $(classes-dir)/net/sf/gratia/services/*Skel.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/services/*Stub.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/services/JMS*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(soap)/net/sf/gratia/soap/server-config.wsdd $(war-dir)/$(name_from_war)/WEB-INF/server-config.wsdd
	cp $(soap)/net/sf/gratia/soap/web.xml $(war-dir)/$(name_from_war)/WEB-INF/web.xml
	cp $(soap)/net/sf/gratia/soap/collector.wsdl $(war-dir)/$(name_from_war)/wsdl
	cp $(commonLib)/axis*.jar $(war-dir)/$(name_from_war)/WEB-INF/lib
	cp $(commonLib)/commons-discovery*.jar $(war-dir)/$(name_from_war)/WEB-INF/lib
	cp $(commonLib)/jaxrpc*.jar $(war-dir)/$(name_from_war)/WEB-INF/lib
	cp $(commonLib)/saaj*.jar $(war-dir)/$(name_from_war)/WEB-INF/lib
	cp $(commonLib)/wsdl*.jar $(war-dir)/$(name_from_war)/WEB-INF/lib
	$(copy_util_lib)
	jar -cfM $(target)/gratia-soap.war -C $(war-dir)/$(name_from_war) .
	rm -rf $(war-dir)/$(name_from_war)
	# --- Target: soap COMPLETED at $(shell date) ---

#------------------------------------------------------------------
administration: $(target)/gratia-administration.war
	# --- Target: administration COMPLETED at $(shell date) ---

#------------------------------------------------------------------
${target}/gratia-administration.war: $(util_lib)  $(target)/gratiaSecurity.jar \
                                     ${administration}/net/sf/gratia/administration/*.java \
                                     ${administration}/net/sf/gratia/administration/web.xml \
                                     ${administration}/WebContent/*.html \
                                     ${administration}/WebContent/*.css \
                                     ${administration}/WebContent/*.jsp \
                                     ${administration}/WebContent/*.js \
                                     ${administration}/WebContent/images/*.gif \
                                     $(commonLib)/tar.jar \
                                     $(commonLib)/bzip2.jar \
                                     $(commonLib)/mysql*.jar \
				 $(services)/net/sf/gratia/services/Versions.java \
				$(administration)/ehcache.xml $(administration)/c3p0.properties
	#----------------------------------------------------
	# --- Target: $(target)/gratia-administration.war ---
	#----------------------------------------------------
	rm -rf $(war-dir)/$(name_from_war); mkdir -p $(war-dir)/$(name_from_war)/META-INF $(war-dir)/$(name_from_war)/WEB-INF/classes $(war-dir)/$(name_from_war)/WEB-INF/lib;
	mkdir -p $(classes-dir) $(target)
	mkdir -p $(war-dir)/$(name_from_war)/images
	mkdir -p $(war-dir)/$(name_from_war)/META-INF
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/storage
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/administration
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/util
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/lib
	echo "$(gratia_release_data)" >$(war-dir)/$(name_from_war)/gratia-release
	javac -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(administration)/net/sf/gratia/administration/*.java
	javac -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(services)/net/sf/gratia/services/Versions.java
	cp $(classes-dir)/net/sf/gratia/administration/*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/administration
	cp $(classes-dir)/net/sf/gratia/services/*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/storage/*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/storage
	cp $(administration)/net/sf/gratia/administration/web.xml $(war-dir)/$(name_from_war)/WEB-INF/web.xml
	cp $(administration)/WebContent/*.html $(war-dir)/$(name_from_war)
	cp $(administration)/WebContent/*.css $(war-dir)/$(name_from_war)
	cp $(administration)/WebContent/*.jsp $(war-dir)/$(name_from_war)
	cp $(administration)/WebContent/*.js $(war-dir)/$(name_from_war)
	cp $(administration)/WebContent/images/*.gif $(war-dir)/$(name_from_war)/images
	cp $(administration)/ehcache.xml $(war-dir)/$(name_from_war)/WEB-INF/classes/
	cp $(administration)/c3p0.properties $(war-dir)/$(name_from_war)/WEB-INF/classes/
	# cp $(docs)/*.html war # included in the administration WebContent
	cp $(classes-dir)/net/sf/gratia/util/Logging.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/util
	cp $(commonLib)/*.jar $(war-dir)/$(name_from_war)/WEB-INF/lib
	rm -f  $(war-dir)/$(name_from_war)/WEB-INF/lib/{serv,log4j}*
	cp $(target)/gratiaSecurity.jar $(war-dir)/$(name_from_war)/WEB-INF/lib
	$(copy2_util_lib)
	jar -cfM $(target)/gratia-administration.war -C $(war-dir)/$(name_from_war) .
	# --- Target: $@ COMPLETED at $(shell date) ---

#------------------------------------------------------------------
registration: $(target)/gratia-registration.war
	# --- Target: administration COMPLETED at $(shell date) ---

#------------------------------------------------------------------
${target}/gratia-registration.war: $(util_lib) $(registration)/net/sf/gratia/registration/*.java $(registration)/net/sf/gratia/registration/web.xml
	#----------------------------------------------------
	# --- Target: $(target)/gratia-administration.war ---
	#----------------------------------------------------
	rm -rf $(war-dir)/$(name_from_war); mkdir -p $(war-dir)/$(name_from_war)/META-INF $(war-dir)/$(name_from_war)/WEB-INF/classes $(war-dir)/$(name_from_war)/WEB-INF/lib;
	echo "$(gratia_release_data)" > $(war-dir)/$(name_from_war)/gratia-release
	#
	mkdir -p $(classes-dir) $(target)
	javac -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(registration)/net/sf/gratia/registration/*.java
	#
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/registration
	cp $(classes-dir)/net/sf/gratia/registration/*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/registration
	cp $(registration)/net/sf/gratia/registration/web.xml $(war-dir)/$(name_from_war)/WEB-INF
	#
	cp $(commonLib)/mysql*.jar $(war-dir)/$(name_from_war)/WEB-INF/lib
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/util
	mkdir -p $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/services/AccessException.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/util/Logging.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/util
	cp $(classes-dir)/net/sf/gratia/services/*Skel.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/services/*Stub.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	cp $(classes-dir)/net/sf/gratia/services/JMS*.class $(war-dir)/$(name_from_war)/WEB-INF/classes/net/sf/gratia/services
	$(copy2_util_lib)
	jar -cfM $@ -C $(war-dir)/$(name_from_war) .
	# --- Target: $@ COMPLETED at $(shell date) ---

#------------------------------------------------------------------
$(target)/gratia-util.jar: ${util}/net/sf/gratia/util/*.java
	# --------------------
	# --- Target: $(target)/gratia-util.war ---
	# --------------------
	rm -rf $(jar-dir)/$(name_from_war)
	mkdir -p $(classes-dir) $(target)
	mkdir -p $(jar-dir)/$(name_from_war)/net/sf/gratia/util
	javac -d $(classes-dir) -Xlint:unchecked -cp $(classpath) -extdirs $(commonLib) $(util)/net/sf/gratia/util/*.java
	cp $(classes-dir)/net/sf/gratia/util/*.class $(jar-dir)/$(name_from_war)/net/sf/gratia/util
	jar -cfM $(target)/gratia-util.jar -C $(jar-dir)/$(name_from_war) .
	# --- Target: $(target)/gratia-util.war COMPLETED at $(shell date) ---

util: $(target)/gratia-util.jar
	# --- Target: util COMPLETED at $(shell date) ---
#------------------------------------------------------------------
$(target)/cleanup_slf4j_lib: $(configuration)/cleanup_template
        # ------------------------------
        # --- Target: cleanup_slf4j_lib
        # ------------------------------
	sed -e 's/%%%FILES%%%/$(foreach d, $(slf4j_lib_jars), $(d)*.jar)/' $(^) > $(@)
	chmod a+x $(@)


slf4j_lib: $(target)/cleanup_slf4j_lib
        # ------------------------------
        # --- Target: slf4j_lib     ---
        # ------------------------------
	rm -f -r tarball; \
	mkdir tarball; \
	cp -v $(SLF4J_LIBS) tarball/; \
	tar -cvf $(target)/slf4j_lib.tar -C tarball .; \
	rm -f -r tarball
	# --- Target: slf4j_lib COMPLETED at $(shell date) ---

#------------------------------------------------------------------
security: $(target)/gratiaSecurity.jar
	# --- Target: security COMPLETED at $(shell date) ---

#------------------------------------------------------------------
$(target)/gratiaSecurity.jar: $(target)/gratia-services.war $(util_lib) $(security)/net/sf/gratia/vomsSecurity/*.java \
                              $(security)/org/glite/security/trustmanager/*.java $(security)/net/sf/gratia/trustmanager/*.java
	# ----------------------------
	# --- Target: $(target)/gratiaSecurity.jar ---
	# ----------------------------
	rm   -rf $(jar-dir)/$(name_from_jar)
	mkdir -p $(classes-dir) $(target)
	mkdir -p $(jar-dir)/$(name_from_jar)/net/sf/gratia/util
	mkdir -p $(jar-dir)/$(name_from_jar)/net/sf/gratia/services
	mkdir -p $(jar-dir)/$(name_from_jar)/net/sf/gratia/vomsSecurity
	mkdir -p $(jar-dir)/$(name_from_jar)/net/sf/gratia/trustmanager
	mkdir -p $(jar-dir)/$(name_from_jar)/org/glite/security/trustmanager
	javac -Xlint:unchecked -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(security)/net/sf/gratia/vomsSecurity/*.java
	javac -Xlint:unchecked -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(security)/org/glite/security/trustmanager/*.java
	javac -Xlint:unchecked -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(security)/net/sf/gratia/trustmanager/*.java
	cp $(classes-dir)/net/sf/gratia/util/*.class $(jar-dir)/$(name_from_jar)/net/sf/gratia/util
	cp $(classes-dir)/net/sf/gratia/vomsSecurity/*.class $(jar-dir)/$(name_from_jar)/net/sf/gratia/vomsSecurity
	cp $(classes-dir)/net/sf/gratia/trustmanager/*.class $(jar-dir)/$(name_from_jar)/net/sf/gratia/trustmanager
	cp $(classes-dir)/org/glite/security/trustmanager/*.class $(jar-dir)/$(name_from_jar)/org/glite/security/trustmanager/
	cp $(classes-dir)/net/sf/gratia/services/JMS*.class $(jar-dir)/$(name_from_jar)/net/sf/gratia/services
	jar -cvfM $(target)/gratiaSecurity.jar -C $(jar-dir)/$(name_from_jar) .
	# --- Target: $@ COMPLETED at $(shell date) ---

#-------------------------------------------------------------
reporting: birt-reporting
	# -------------------------
	# --- Target: reporting ---
	# -------------------------
	rm -rf $(war-dir)/$@
	mkdir -p $(war-dir)/$@/documents
	mkdir -p $(war-dir)/$@/WEB-INF/attachments
	echo "$(gratia_release_data)" >$(war-dir)/$@/gratia-release
	# --- expand the birt war file  -----
	cp $(birtDir)/$(birtwarFile) $(war-dir)/$@/.
	cd $(war-dir)/$@ && jar xf $(birtwarFile)
	rm -f $(war-dir)/$@/$(birtwarFile)
	rm -f $(war-dir)/$@/test.rptdesign
	rm -f $(war-dir)/$@/test1.rptdesign
	# --- edit the web.xml for timezone configuration ---
	tmp=`mktemp $${TMPDIR:-/tmp}/gratia-build.XXXXXXXXXX`; \
	tmp2=`mktemp $${TMPDIR:-/tmp}/gratia-build.XXXXXXXXXX`; \
	[[ -n "$$tmp" ]] || exit 1; \
	[[ -n "$$tmp2" ]] || exit 1; \
	trap "[[ -n \"$$tmp\" ]] && rm -f \"$$tmp\" \"$$tmp2\"" EXIT; \
	perl -we 'use strict; my $$seen_par; my $$par = "BIRT_VIEWER_TIMEZONE"; my $$val = "GMT"; while (<>) { m&<param-name>\Q$$par\E</param-name>& and $$seen_par = 1; $$seen_par and s&<param-value>.*&<param-value>$$val</param-value>& and undef $$seen_par; print }' $(war-dir)/$@/WEB-INF/web.xml > "$$tmp"; \
  (( $$? == 0 )) || exit 1; \
	perl -we 'use strict; my $$seen_par; my $$par = "WORKING_FOLDER_ACCESS_ONLY"; my $$val = "false"; while (<>) { m&<param-name>\Q$$par\E</param-name>& and $$seen_par = 1; $$seen_par and s&<param-value>.*&<param-value>$$val</param-value>& and undef $$seen_par; print }' "$$tmp" > "$$tmp2"; \
  (( $$? == 0 )) || exit 1; \
  perl -wpe 'use strict; m&display-name& and do { print "  <!-- UrlRewriteFilter configuration -->\n <filter>\n	<filter-name>UrlRewriteFilter</filter-name>\n	<filter-class>org.tuckey.web.filters.urlrewrite.UrlRewriteFilter</filter-class>\n	<init-param>\n	 <param-name>logLevel</param-name>\n	 <param-value>WARN</param-value>\n	 </init-param>\n </filter>\n <filter-mapping>\n	 <filter-name>UrlRewriteFilter</filter-name>\n	 <url-pattern>/*</url-pattern>\n </filter-mapping>\n\n <!-- Gratia Reporting Service Listener -->\n        <listener>\n                <listener-class>net.sf.gratia.reporting.ReportingService</listener-class>\n        </listener>\n\n"; print "    <servlet>\n        <servlet-name>MonitorStatus</servlet-name>\n        <servlet-class>net.sf.gratia.administration.MonitorStatus</servlet-class>\n        <load-on-startup>2</load-on-startup>\n    </servlet>\n\n    <servlet-mapping>\n        <servlet-name>MonitorStatus</servlet-name>\n        <url-pattern>/monitor-status.html</url-pattern>\n    </servlet-mapping>\n\n"};' "$$tmp2" > "$$tmp"; \
  (( $$? == 0 )) || exit 1; \
	cp -p $(war-dir)/$@/WEB-INF/web.xml $(war-dir)/$@/WEB-INF/web.xml.old; \
	cat "$$tmp" > $(war-dir)/$@/WEB-INF/web.xml;
	cp -p $(reportsrc)/WebContent/urlrewrite.xml	       $(war-dir)/$@/WEB-INF/.
	# --- copy in needed libraries ---
	cp -p $(target)/gratiaReporting.jar                    $(war-dir)/$@/WEB-INF/lib/.
	cp -p $(target)/gratiaReporting.jar                    $(war-dir)/$@/scriptlib/.
	cp -p $(commonLib)/mysql-connector-java-*-bin.jar      $(war-dir)/$@/WEB-INF/lib/.
	cp -p $(commonLib)/mysql-connector-java-*-bin.jar      $(war-dir)/$@/WEB-INF/platform/plugins/org.eclipse.birt.report.data.oda.jdbc*/drivers/.
	cp -p $(commonLib)/dom4j-1.6.1.jar                     $(war-dir)/$@/WEB-INF/lib/.
	cp -p $(commonLib)/commons-lang-*.jar                  $(war-dir)/$@/WEB-INF/lib/.
	cp -p $(commonLib)/urlrewritefilter-4.0.3.jar          $(war-dir)/$@/WEB-INF/lib/.
	# --- copy needed jsp and css files ----
	cp -p $(reportsrc)/WebContent/*.jsp                    $(war-dir)/$@/.
	cp -p $(reportsrc)/WebContent/*.xml                    $(war-dir)/$@/.
	# --- don't need urlrewrite.xml here , it is in WEB-INF
	rm -f $(war-dir)/$@/urlrewrite.xml
        # ---
	cp -p $(reportsrc)/WebContent/*.html                   $(war-dir)/$@/.
	cp -p $(reportsrc)/WebContent/*.css                    $(war-dir)/$@/.
	# --- copy directories paths ---
	cp -R $(reportsrc)/WebContent/calendar                 $(war-dir)/$@/.
	cp -R $(reportsrc)/WebContent/images                   $(war-dir)/$@/.
	cp -R $(reportsrc)/WebContent/tooltip                  $(war-dir)/$@/.
	# --- Remove .svn directories
	find $(war-dir)/$@ -depth -type d -name .svn -exec rm -rf {} \;
	# -- Copy required class files in from other services:
	mkdir -p $(war-dir)/$@/WEB-INF/classes/net/sf/gratia/administration
	javac -d $(classes-dir) -cp $(classpath) -extdirs $(commonLib) $(administration)/net/sf/gratia/administration/MonitorStatus*.java
	cp $(classes-dir)/net/sf/gratia/administration/MonitorStatus*.class $(war-dir)/$@/WEB-INF/classes/net/sf/gratia/administration
	# --- copy the web.xml  file ---
	jar -cfM $(target)/gratia-reporting.war -C $(war-dir)/$@ .
	rm -rf $(war-dir)/$@
	# --- Target: reporting COMPLETED at $(shell date) ---
#-------------------------------------------------------------
reports:
	# -----------------------
	# --- Target: reports ---
	# -----------------------
	rm   -rf $(war-dir)/$@
	mkdir -p $(war-dir)/$@/reports
	mkdir -p $(war-dir)/$@/MenuConfig
	mkdir -p $(war-dir)/$@/WEB-INF
	mkdir -p $(war-dir)/$@/META-INF
	echo "${gratia_release_data}" >$(war-dir)/$@/gratia-release
	cp -p ${reports}/WebContent/WEB-INF/web.xml          $(war-dir)/$@/WEB-INF/web.xml
	cp -p ${reports}/WebContent/MenuConfig/*.xml         $(war-dir)/$@/MenuConfig
	cp -p ${reports}/WebContent/reports/production-osg/*.rptdesign            $(war-dir)/$@/reports
	-cp -p ${reports}/WebContent/reports/production-osg/*.rptlibrary $(war-dir)/$@/reports
	-cp -p ${reports}/WebContent/reports/production-fermi/*.rptlibrary $(war-dir)/$@/reports
	-cp -p ${reports}/WebContent/reports/production-osgdaily/*.rptlibrary $(war-dir)/$@/reports
	cp -p ${reports}/WebContent/reports/production-osgdaily/*.rptdesign            $(war-dir)/$@/reports
	cp -p ${reports}/WebContent/reports/production-fermi/*.rptdesign      $(war-dir)/$@/reports
	${copy_util_lib}
	jar -cvf ${target}/gratia-reports.war -C $(war-dir)/$@ .
	rm -rf $(war-dir)/$@
	# --- Target: reports COMPLETED at $(shell date) ---

#-------------------------------------------------------------
birt-reporting: $(reportsrc)/src/net/sf/gratia/reporting/Versions.java
	# ------------------------------
	# --- Target: birt-reporting ---
	# ------------------------------
	rm -rf $(jar-dir)/$@
	mkdir -p $(classes-dir)/$@ $(target)
	mkdir -p $(jar-dir)/$@/WEB-INF
	mkdir -p $(jar-dir)/$@/WEB-INF/tlds
	mkdir -p $(jar-dir)/$@/net/sf/gratia/util
	mkdir -p $(jar-dir)/$@/net/sf/gratia/reporting/exceptions
	mkdir -p $(jar-dir)/$@/net/sf/gratia/administration
	javac -d $(classes-dir)/$@ -cp $(classpath) -extdirs $(commonLib) $(util)/net/sf/gratia/util/*.java
	javac -d $(classes-dir)/$@ -cp $(classpath) -extdirs $(commonLib) $(reportsrc)/src/net/sf/gratia/reporting/exceptions/*.java
	javac -d $(classes-dir)/$@ -cp $(classpath) -extdirs $(commonLib) $(reportsrc)/src/net/sf/gratia/reporting/*.java
	javac -d $(classes-dir)/$@ -cp $(classpath) -extdirs $(commonLib) $(reportsrc)/src/net/sf/gratia/administration/*.java
	cp $(classes-dir)/$@/net/sf/gratia/util/*.class $(jar-dir)/$@/net/sf/gratia/util
	cp $(classes-dir)/$@/net/sf/gratia/reporting/*.class $(jar-dir)/$@/net/sf/gratia/reporting
	cp $(classes-dir)/$@/net/sf/gratia/administration/*.class $(jar-dir)/$@/net/sf/gratia/administration
	cp $(classes-dir)/$@/net/sf/gratia/reporting/exceptions/*.class $(jar-dir)/$@/net/sf/gratia/reporting/exceptions
	jar -cvfM $(target)/gratiaReporting.jar -C $(jar-dir)/$@ .
	rm -rf $(jar-dir)/$@
	# --- Target: birt-reporting COMPLETED at $(shell date) ---


#---------------------------------------------------------------
local: clean util services servlets  administration soap reporting reports 
	# --- Target: local COMPLETED at $(shell date) ---
#---------------------------------------------------------------

release: gratia
	# --------------------------------
	# --- Target: release          ---
	# --- create reporting tarball ---
	# --------------------------------
	rm   -rf tarball; \
	mkdir -p tarball/tomcat/v55/gratia; \
	mkdir -p tarball/gratia/gratia_reporting; \
	echo "$(gratia_release_data)" >tarball/tomcat/v55/gratia/gratia-release
	cp $(configuration)/*gov $(configuration)/log4j.properties $(staticScript)/staticReports tarball/tomcat/v55/gratia; \
	chmod 755 tarball/tomcat/v55/gratia/staticReports
	cp $(configuration)/*sql $(configuration)/configure_tomcat $(configuration)/configure_urlredirect $(configuration)/*sh $(configuration)/post-install $(configuration)/install-database $(configuration)/collector-backlog-byprobes $(configuration)/*.template tarball/tomcat/v55/gratia; \
	rm -rf tarball/tomcat/v55/gratia/.svn; \
	cp -R $(configuration)/hibernate tarball/tomcat/v55/gratia; \
	rm -rf tarball/tomcat/v55/gratia/hibernate/.svn; \
	chmod 700 tarball/tomcat/v55/gratia/post-install; \
	perl -wapi.bak -e 's&CMD_(?:PREAMBLE|PREFIX|SUFFIX)&&g' tarball/tomcat/v55/gratia/post-install; \
	rm -f tarball/tomcat/v55/gratia/post-install.bak; \
        chmod 700 tarball/tomcat/v55/gratia/install-database; \
        perl -wapi.bak -e 's&CMD_(?:PREAMBLE|PREFIX|SUFFIX)&&g' tarball/tomcat/v55/gratia/install-database; \
        rm -f tarball/tomcat/v55/gratia/install-database.bak; \
        chmod 700 tarball/tomcat/v55/gratia/configure_tomcat; \
        chmod 700 tarball/tomcat/v55/gratia/configure_urlredirect; \
        perl -wapi.bak -e 's&CMD_(?:PREAMBLE|PREFIX|SUFFIX)&&g' tarball/tomcat/v55/gratia/configure_tomcat; \
        rm -f tarball/tomcat/v55/gratia/configure_tomcat.bak; \
	cp $(configuration)/truststore $(configuration)/keystore tarball/tomcat/v55/gratia; \
	sed -e 's/^gratia.services.version.*=.*/gratia.services.version = v$(gratia_services_version)/'  -e 's/^gratia.reporting.version.*=.*/gratia.reporting.version = v$(gratia_reporting_version)/' $(configuration)/service-configuration.properties > tarball/tomcat/v55/gratia/service-configuration.properties.new && chmod 600 tarball/tomcat/v55/gratia/service-configuration.properties.new; \
	sed -e 's/^gratia.services.version.*=.*/gratia.services.version = v$(gratia_services_version)/'  -e 's/^gratia.reporting.version.*=.*/gratia.reporting.version = v$(gratia_reporting_version)/' $(configuration)/service-authorization.properties > tarball/tomcat/v55/gratia/service-authorization.properties.new &&  chmod 600 tarball/tomcat/v55/gratia/service-authorization.properties.new; \
	cp $(target)/gratia-reports.war tarball/gratia/gratia_reporting; \
	cp $(target)/gratia-reporting.war tarball/gratia/gratia_reporting; \
	tar $(tarowner) -cvf $(target)/gratia_reporting_$(gratia_reporting_version).tar -C tarball .; \
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
	echo "$(gratia_release_data)" >tarball/tomcat/v55/gratia/gratia-release
	cp $(configuration)/voms-servers    tarball/tomcat/v55/gratia; 
	chmod 644 tarball/tomcat/v55/gratia/voms-servers;
	cp $(security)/scripts/voms-server.sh   tarball/tomcat/v55/gratia; 
	chmod 744 tarball/tomcat/v55/gratia/voms-server.sh;
	cp $(security)/scripts/voms-server.cron   tarball/tomcat/v55/gratia; 
	cp $(configuration)/*gov $(configuration)/log4j.properties  $(staticScript)/staticReports tarball/tomcat/v55/gratia; \
	chmod 755 tarball/tomcat/v55/gratia/staticReports
	cp -R $(configuration)/hibernate tarball/tomcat/v55/gratia; \
	rm -rf tarball/tomcat/v55/gratia/hibernate/.svn; \
	cp $(configuration)/*sql $(configuration)/configure_tomcat $(configuration)/configure_urlredirect $(configuration)/*sh $(configuration)/collector-backlog-byprobes $(configuration)/*.template   $(staticScript)/staticReports tarball/tomcat/v55/gratia; \
	perl -wapi.bak -e 's&CMD_(?:PREAMBLE|PREFIX|SUFFIX)&&g' tarball/tomcat/v55/gratia/post-install; \
	rm -f tarball/tomcat/v55/gratia/post-install.bak; \
        perl -wapi.bak -e 's&CMD_(?:PREAMBLE|PREFIX|SUFFIX)&&g' tarball/tomcat/v55/gratia/install-database; \
        rm -f tarball/tomcat/v55/gratia/install-database.bak; \
        perl -wapi.bak -e 's&CMD_(?:PREAMBLE|PREFIX|SUFFIX)&&g' tarball/tomcat/v55/gratia/configure_tomcat; \
        rm -f tarball/tomcat/v55/gratia/configure_tomcat.bak; \
	cp $(configuration)/truststore $(configuration)/keystore tarball/tomcat/v55/gratia; \
	sed -e 's/^gratia.services.version.*=.*/gratia.services.version = v$(gratia_services_version)/'  -e 's/^gratia.reporting.version.*=.*/gratia.reporting.version = v$(gratia_reporting_version)/' $(configuration)/service-configuration.properties > tarball/tomcat/v55/gratia/service-configuration.properties.new &&  chmod 600 tarball/tomcat/v55/gratia/service-configuration.properties.new; \
	sed -e 's/^gratia.services.version.*=.*/gratia.services.version = v$(gratia_services_version)/' -e 's/^gratia.reporting.version.*=.*/gratia.reporting.version = v$(gratia_reporting_version)/' $(configuration)/service-authorization.properties > tarball/tomcat/v55/gratia/service-authorization.properties.new &&  chmod 600 tarball/tomcat/v55/gratia/service-authorization.properties.new; \
	cp $(target)/gratia-services.war tarball/gratia/gratia_services; \
	cp $(target)/gratia-servlets.war tarball/gratia/gratia_services; \
	cp $(target)/gratia-soap.war tarball/gratia/gratia_services; \
	cp $(target)/gratia-administration.war tarball/gratia/gratia_services; \
	cp $(target)/gratia-registration.war tarball/gratia/gratia_services; \
	cp $(target)/cleanup_{server,common}_lib tarball/gratia/gratia_services; \
	cp $(target)/{server,common}_lib.tar tarball/gratia/gratia_services; \
	cp $(target)/common_classes.tar tarball/gratia/gratia_services; \
	tar $(tarowner) -cvf $(target)/gratia_services_$(gratia_services_version).tar -C tarball .; \
	rm -f -r tarball
	# --- Target: release COMPLETED at $(shell date) ---

server_lib: $(target)/cleanup_server_lib
	# ------------------------------
	# --- Target: server_lib     ---
	# ------------------------------
	rm -f -r tarball; \
	mkdir tarball; \
	cp -v $(SERVER_LIBS) tarball/; \
	tar -cvf $(target)/server_lib.tar -C tarball .; \
        rm -f -r tarball
	# --- Target: server_lib COMPLETED at $(shell date) ---

common_lib: $(target)/cleanup_common_lib
	# ------------------------------
	# --- Target: common_lib     ---
	# ------------------------------
	rm -f -r tarball; \
	mkdir tarball; \
	cp -v $(COMMON_LIBS) tarball/; \
	tar -cvf $(target)/common_lib.tar -C tarball .; \
        rm -f -r tarball
	# --- Target: common_lib COMPLETED at $(shell date) ---

shared_lib: $(target)/cleanup_shared_lib
	# ------------------------------
	# --- Target: shared_lib     ---
	# ------------------------------
	rm -f -r tarball; \
	mkdir tarball; \
	cp -v $(SHARED_LIBS) tarball/; \
	tar -cvf $(target)/shared_lib.tar -C tarball .; \
        rm -f -r tarball
	# --- Target: shared_lib COMPLETED at $(shell date) ---

common_classes: util
	# ------------------------------
	# --- Target: common_classes ---
	# ------------------------------
	rm -f -r tarball; \
	mkdir tarball; \
        for file in $(foreach d, $(COMMON_CLASSES), '$(d)'); do \
          [[ -z "$$file" ]] && continue; \
	  dir="tarball/net/`dirname \"$${file##*/net/}\"`"; \
	  mkdir -p "$${dir}"; \
          cp -v "$${file}" "$${dir}"; \
	done; \
	tar -cvf $(target)/common_classes.tar -C tarball .; \
	rm -f -r tarball
	# --- Target: common_classes COMPLETED at $(shell date) ---

#-------------------------------------------------------------
gratia: clean util services servlets registration administration soap reporting reports common_lib server_lib common_classes slf4j_lib
	# ------------------------------
	# --- Target: gratia         ---
	# ------------------------------
	rm -f -r tarball; \
	mkdir tarball; \
	echo "$(gratia_release_data)" >tarball/gratia-release
	cp $(configuration)/UserVoMap.* $(staticScript)/staticReports $(configuration)/*properties tarball; \
	cp ${staticScript}/staticReports $(configuration)/*sql $(configuration)/configure_tomcat $(configuration)/configure_urlredirect $(configuration)/*sh $(configuration)/post-install $(configuration)/install-database  $(configuration)/*.template  tarball; \
	cp -R $(configuration)/hibernate tarball; \
	rm -rf tarball/hibernate/.svn; \
	cp $(security)/scripts/voms-server.sh         tarball; chmod 744 tarball/voms-server.sh
	cp $(security)/scripts/voms-server.cron        tarball
	chmod 755 tarball/staticReports
	chmod 700 tarball/post-install; \
	chmod 700 tarball/install-database; \
	chmod 700 tarball/configure_tomcat;  \
	chmod 700 tarball/configure_urlredirect;  \
	sed -e 's/^gratia.services.version.*=.*/gratia.services.version = v$(gratia_services_version)/'  -e 's/^gratia.reporting.version.*=.*/gratia.reporting.version = v$(gratia_reporting_version)/' $(configuration)/service-configuration.properties > tarball/service-configuration.properties && chmod 600 tarball/service-configuration.properties;  \
	sed -e 's/^gratia.services.version.*=.*/gratia.services.version = v$(gratia_services_version)/'  -e 's/^gratia.reporting.version.*=.*/gratia.reporting.version = v$(gratia_reporting_version)/' $(configuration)/service-authorization.properties > tarball/service-authorization.properties &&  chmod 600 tarball/service-authorization.properties;  \
	cp $(configuration)/configure-* $(configuration)/update-gratia-local tarball; \
	cp $(configuration)/truststore $(configuration)/keystore tarball; \
	cp $(configuration)/cleanup_misc tarball/; \
	cp $(configuration)/voms-servers tarball; \
	cp $(wildcard $(target)/cleanup_*) tarball; \
	tar -cvf $(target)/gratia.tar -C tarball .; \
	rm -f -r tarball
	# --- Target: gratia COMPLETED at $(shell date) ---

$(target)/cleanup_server_lib: $(configuration)/cleanup_template
	# ------------------------------
	# --- Target: cleanup_server_lib
	# ------------------------------
	sed -e 's/%%%FILES%%%/$(foreach d, $(server_lib_jars), $(d)*.jar)/' $(^) > $(@)
	chmod a+x $(@)

$(target)/cleanup_common_lib: $(configuration)/cleanup_template
	# ------------------------------
	# --- Target: cleanup_common_lib
	# ------------------------------
	sed -e 's/%%%FILES%%%/$(foreach d, $(common_lib_jars), $(d)*.jar)/' $(^) > $(@)
	chmod a+x $(@)

$(target)/cleanup_shared_lib: $(configuration)/cleanup_template
	# ------------------------------
	# --- Target: cleanup_shared_lib
	# ------------------------------
	sed -e 's/%%%FILES%%%/$(foreach d, $(shared_lib_jars), $(d)*.jar)/' $(^) > $(@)
	chmod a+x $(@)

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

$(base)/probe/pbs-lsf/urCollector-src/urCreator: $(wildcard $(base)/probe/pbs-lsf/urCollector-src/*.h $(base)/probe/pbs-lsf/urCollector-src/*.cpp )
	cd $(base)/probe/pbs-lsf/urCollector-src &&  make 

urCollector: $(base)/probe/pbs-lsf/urCollector-src/urCreator


PLATFORM := $(shell uname)

condor_probe_version=v0.9
psacct_probe_version=v0.9

base=../../
root=../
tarballdir=tarball
services=$(root)services
reports=${base}GratiaReports
reporting=${base}GratiaReporting
servlets=$(root)servlets
soap=$(root)soap
administration=$(root)administration
security=$(root)security
jars=$(root)jars
ifeq ($(filter CYGWIN%,$(PLATFORM)),)
tarowner=--owner=root --group=root
classpath=.:$(root)services:$(root)servlets:$(root)soap:$(root)administration:${base}GratiaReporting/src:$(root)security
else
warn := $(shell echo "Building the tar file on cygwin will result in incorrect ownership" 1>&2 )
tarowner=
classpath=.\;$(root)services\;$(root)servlets\;$(root)soap\;$(root)administration\;${base}GratiaReporting/src;$(root)security
endif

clean:
	find ${base} -name "*class" -exec rm -f {} \;
	find ${base} -name "*~" -exec rm -f {} \;
	find ${base} -name "*#" -exec rm -f {} \;
	rm -f -r $(root)target
	mkdir $(root)target

services:
	rm -f -r war
	mkdir war
	mkdir war/meta-inf
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/services
	mkdir war/WEB-INF/classes/net/sf/gratia/storage
	mkdir war/WEB-INF/lib
	rm -f -r net
	javac -cp ${classpath} -extdirs $(jars) ../services/net/sf/gratia/services/*.java
	javac -cp ${classpath} -extdirs $(jars) ../services/net/sf/gratia/storage/*.java
	rmic -vcompat -classpath ${classpath} -d . net.sf.gratia.services.JMSProxyImpl
	cp net/sf/gratia/services/* ../services/net/sf/gratia/services
	rm -f -r net
	cp ${services}/net/sf/gratia/services/*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${services}/net/sf/gratia/storage/*.class war/WEB-INF/classes/net/sf/gratia/storage
	cp $(jars)/*.jar war/WEB-INF/lib
	rm -f  war/WEB-INF/lib/serv*
	cp ${services}/net/sf/gratia/services/web.xml war/WEB-INF/web.xml
	jar -cfM ../target/gratia-services.war -C war .
	rm -f -r war

servlets:	services
	rm -f -r war
	mkdir war
	mkdir war/meta-inf
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/services
	mkdir war/WEB-INF/classes/net/sf/gratia/servlets
	mkdir war/WEB-INF/lib
	javac -cp ${classpath} -extdirs $(jars) ../servlets/net/sf/gratia/servlets/*.java
	cp ${servlets}/net/sf/gratia/servlets/*.class war/WEB-INF/classes/net/sf/gratia/servlets
	cp ../services/net/sf/gratia/services/Logging.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/Configuration.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/*Skel.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/*Stub.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/X*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/JMS*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ${servlets}/net/sf/gratia/servlets/web.xml war/WEB-INF/web.xml
	jar -cfM ../target/gratia-servlets.war -C war .
	rm -f -r war

soap: services
	rm -f -r war
	mkdir war
	mkdir war/meta-inf
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/services
	mkdir war/WEB-INF/classes/net/sf/gratia/soap
	mkdir war/WEB-INF/lib
	mkdir war/wsdl
	javac -cp ${classpath} -extdirs $(jars) ../soap/net/sf/gratia/soap/*.java
	cp ${soap}/net/sf/gratia/soap/*.class war/WEB-INF/classes/net/sf/gratia/soap
	cp ../services/net/sf/gratia/services/Logging.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/Configuration.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/*Skel.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/*Stub.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/X*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/JMS*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../soap/net/sf/gratia/soap/server-config.wsdd war/WEB-INF/server-config.wsdd
	cp ../soap/net/sf/gratia/soap/web.xml war/WEB-INF/web.xml
	cp ../soap/net/sf/gratia/soap/collector.wsdl war/wsdl
	cp ../jars/axis*.jar war/WEB-INF/lib
	cp ../jars/commons-dis*.jar war/WEB-INF/lib
	cp ../jars/jaxrpc*.jar war/WEB-INF/lib
	cp ../jars/saaj*.jar war/WEB-INF/lib
	cp ../jars/wsdl*.jar war/WEB-INF/lib
	jar -cfM ../target/GratiaServices.war -C war .
	jar -cfM ../target/gratia-soap.war -C war .
	rm -f -r war

administration: services
	rm -f -r war
	mkdir war
	mkdir war/meta-inf
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/services
	mkdir war/WEB-INF/classes/net/sf/gratia/administration
	mkdir war/WEB-INF/lib
	javac -cp ${classpath} -extdirs $(jars) ../administration/net/sf/gratia/administration/*.java
	cp ${administration}/net/sf/gratia/administration/*.class war/WEB-INF/classes/net/sf/gratia/administration
	cp ../services/net/sf/gratia/services/Logging.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/Configuration.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/*Skel.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/*Stub.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/X*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/JMS*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../administration/net/sf/gratia/administration/web.xml war/WEB-INF/web.xml
	cp ../administration/net/sf/gratia/administration/*.html war
	cp ../administration/net/sf/gratia/administration/*.jsp war
	cp ../administration/net/sf/gratia/administration/images/*.gif war/images
	cp ../*.html war
	jar -cfM ../target/gratia-administration.war -C war .
	rm -f -r war

security: services
	rm -f -r war
	mkdir war
	mkdir war/meta-inf
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/services
	mkdir war/WEB-INF/classes/net/sf/gratia/security
	mkdir war/WEB-INF/lib
	javac -cp ${classpath} -extdirs $(jars) ../security/net/sf/gratia/security/*.java
	cp ${security}/net/sf/gratia/security/*.class war/WEB-INF/classes/net/sf/gratia/security
	cp ../services/net/sf/gratia/services/Logging.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/Configuration.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/*Skel.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/*Stub.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/X*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../services/net/sf/gratia/services/JMS*.class war/WEB-INF/classes/net/sf/gratia/services
	cp ../security/net/sf/gratia/security/web.xml war/WEB-INF/web.xml
	jar -cfM ../target/gratia-security.war -C war .
	rm -f -r war

reporting:
	rm -f -r war
	mkdir war
	mkdir war/calendar
	mkdir war/images
	mkdir war/WEB-INF
	mkdir war/WEB-INF/classes
	mkdir war/WEB-INF/classes/net
	mkdir war/WEB-INF/classes/net/sf
	mkdir war/WEB-INF/classes/net/sf/gratia
	mkdir war/WEB-INF/classes/net/sf/gratia/reporting
	mkdir war/WEB-INF/classes/net/sf/gratia/reporting/exceptions
	mkdir war/WEB-INF/lib
	javac -cp ${classpath} -extdirs $(jars) ${base}GratiaReporting/src/net/sf/gratia/reporting/*.java
	javac -cp ${classpath} -extdirs $(jars) ${base}GratiaReporting/src/net/sf/gratia/reporting/exceptions/*.java
	cp ${base}GratiaReporting/WebContent/*.jsp war
	cp ${base}GratiaReporting/WebContent/*.css war
	cp ${base}GratiaReporting/src/net/sf/gratia/reporting/*.class war/WEB-INF/classes/net/sf/gratia/reporting
	cp ${base}GratiaReporting/src/net/sf/gratia/reporting/exceptions/*.class war/WEB-INF/classes/net/sf/gratia/reporting/exceptions
	cp -R ${base}GratiaReporting/WebContent/calendar/* war/calendar
	cp -R ${base}GratiaReporting/WebContent/images/* war/images
	cp ${base}GratiaReporting/src/net/sf/gratia/reporting/web.xml war/WEB-INF
	cp ../jars/*.jar war/WEB-INF/lib
	rm -f war/WEB-INF/lib/servlet-api.jar
	jar -cvfM ../target/gratia-reporting.war -C war .
	rm -f -r war

reports:
	rm -f -r war
	mkdir war
	mkdir war/images
	mkdir war/WEB-INF
	cp ${base}GratiaReports/WebContent/*design war
	cp ${base}GratiaReports/WebContent/images/*.gif war/images
	cp ${base}GratiaReports/src/web.xml war/WEB-INF
	jar -cvf ../target/gratia-reports.war -C war .
	rm -f -r war

report-configuration:
	rm -f -r war
	mkdir war
	mkdir war/WEB-INF
	cp ${base}/GratiaReportConfiguration/WebContent/Reporting* war
	cp ${base}GratiaReportConfiguration/WebContent/User* war
	cp ${base}GratiaReportConfiguration/src/web.xml war/WEB-INF
	jar -cvf ../target/gratia-report-configuration.war -C war .
	rm -f -r war

release: services servlets soap reporting reports report-configuration administration security condor_probe_tar
	rm -f -r tarball
	#
	# create reporting tarball
	#
	mkdir tarball
	mkdir tarball/gratia
	mkdir tarball/gratia/gratia_reporting
	mkdir tarball/var
	mkdir tarball/var/data
	mkdir tarball/var/logs
	mkdir tarball/var/tmp
	cp ../target/gratia-report-configuration.war tarball/gratia/gratia_reporting
	cp ../target/gratia-reports.war tarball/gratia/gratia_reporting
	cp ../target/gratia-reporting.war tarball/gratia/gratia_reporting
	tar $(tarowner) -cvf ../target/gratia_reporting_v0.1.tar -C tarball .
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
	cp ../configuration/*gov ../configuration/*xml ../configuration/*properties tarball/tomcat/v55/gratia
	rm -f tarball/tomcat/v55/gratia/local.*
	rm -f tarball/tomcat/v55/gratia/release.*
	cp ../configuration/release.service-configuration.properties tarball/tomcat/v55/gratia/service-configuration.properties
	cp ../target/gratia-services.war tarball/gratia/gratia_services
	cp ../target/gratia-servlets.war tarball/gratia/gratia_services
	cp ../target/gratia-soap.war tarball/gratia/gratia_services
	cp ../target/gratia-administration.war tarball/gratia/gratia_services
	cp ../target/gratia-security.war tarball/gratia/gratia_services
	tar $(tarowner) -cvf ../target/gratia_services_v0.2.tar -C tarball .
	rm -f -r tarball

local: services servlets soap reporting reports report-configuration administration security condor_probe_tar
	rm -f -r tarball
	#
	# create reporting tarball
	#
	mkdir tarball
	mkdir tarball/gratia
	mkdir tarball/gratia/gratia_reporting
	mkdir tarball/var
	mkdir tarball/var/data
	mkdir tarball/var/logs
	mkdir tarball/var/tmp
	cp ../target/gratia-report-configuration.war tarball/gratia/gratia_reporting
	cp ../target/gratia-reports.war tarball/gratia/gratia_reporting
	cp ../target/gratia-reporting.war tarball/gratia/gratia_reporting
	cp ../wars/Birt.war tarball/gratia/gratia_reporting
	tar $(tarowner) -cvf ../target/gratia_reporting_v0.1.tar -C tarball .
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
	cp ../configuration/*gov ../configuration/*xml ../configuration/*properties tarball/tomcat/v55/gratia
	rm -f tarball/tomcat/v55/gratia/local.*
	rm -f tarball/tomcat/v55/gratia/release.*
	cp ../configuration/release.service-configuration.properties tarball/tomcat/v55/gratia/service-configuration.properties
	cp ../target/gratia-services.war tarball/gratia/gratia_services
	cp ../target/gratia-servlets.war tarball/gratia/gratia_services
	cp ../target/GratiaServices.war tarball/gratia/gratia_services
	cp ../target/gratia-soap.war tarball/gratia/gratia_services
	cp ../target/gratia-administration.war tarball/gratia/gratia_services
	cp ../target/gratia-security.war tarball/gratia/gratia_services
	tar $(tarowner) -cvf ../target/gratia_services_v0.2.tar -C tarball .
	rm -f -r tarball

define defaultdirs
	mkdir -p gratia/ ; \
	mkdir -p gratia/var/; \
	mkdir -p gratia/var/data/; \
	mkdir -p gratia/var/logs/; \
	mkdir -p gratia/var/tmp/; \
	chmod -R og+rw gratia/var/; 
endef

condordirs:
	(mkdir -p $(tarballdir); cd $(tarballdir); mkdir -p condor-tar; cd condor-tar; \
	 $(defaultdirs) \
	 mkdir -p gratia/condor-probe/ ; \
	 mkdir -p gratia/condor-probe/gram_mods/ \
        )

psacctdirs: 
	(mkdir -p $(tarballdir); cd $(tarballdir); mkdir -p psacct-tar; cd psacct-tar; \
	 $(defaultdirs) \
	 mkdir -p gratia/psacct/ \
        )

condorfiles: condordirs
	cp $(root)/condor-probe/Clarens.py $(tarballdir)/condor-tar/gratia/condor-probe
	cp $(root)/condor-probe/Gratia.py $(tarballdir)/condor-tar/gratia/condor-probe
	chmod ogu+x $(root)/condor-probe/condor_meter.pl $(root)/condor-probe/condor_meter.cron.sh 
	cp $(root)/condor-probe/condor_meter.cron.sh $(tarballdir)/condor-tar/gratia/condor-probe
	cp $(root)/condor-probe/condor_meter.pl $(tarballdir)/condor-tar/gratia/condor-probe
	cp $(root)/condor-probe/ProbeConfigTemplate $(tarballdir)/condor-tar/gratia/condor-probe
	cp $(root)/condor-probe/README $(tarballdir)/condor-tar/gratia/condor-probe
	cp $(root)/condor-probe/gram_mods/condor.pm.diff $(tarballdir)/condor-tar/gratia/condor-probe/gram_mods
	cp $(root)/condor-probe/gram_mods/managedfork.pm.diff $(tarballdir)/condor-tar/gratia/condor-probe/gram_mods

psacctfiles: psacctdirs
	chmod uog+x $(root)/psacct/gratia-psacct 
	chmod u+x $(root)/psacct/*sh $(root)/psacct/PSACCTProbe.py $(root)/psacct/facct-catchup
	cp $(root)/psacct/psacct_probe.cron.sh $(tarballdir)/psacct-tar/gratia/psacct
	cp $(root)/psacct/facct-turnoff.sh $(tarballdir)/psacct-tar/gratia/psacct
	cp $(root)/psacct/PSACCTProbe.py $(tarballdir)/psacct-tar/gratia/psacct
	cp $(root)/psacct/PSACCTProbeLib.py $(tarballdir)/psacct-tar/gratia/psacct
	cp $(root)/psacct/facct-catchup $(tarballdir)/psacct-tar/gratia/psacct
	cp $(root)/psacct/gratia-psacct $(tarballdir)/psacct-tar/gratia/psacct
	cp $(root)/condor-probe/ProbeConfigTemplate $(tarballdir)/psacct-tar/gratia/psacct
	cp $(root)/condor-probe/Gratia.py $(tarballdir)/psacct-tar/gratia/psacct

$(root)/target/gratia_condor_probe_$(condor_probe_version).tar: condorfiles
	tar $(tarowner) -cvf $(root)/target/gratia_probe_$(condor_probe_version).tar -C tarball/condor-tar .
	#rm -f -r tarball

$(root)/target/gratia_psacct_probe_$(psacct_probe_version).tar: psacctfiles
	tar $(tarowner) -cvf $(root)/target/psacct_probe_$(psacct_probe_version).tar -C tarball/psacct-tar .
	#rm -f -r tarball

condor_probe_tar: $(root)/target/gratia_condor_probe_$(condor_probe_version).tar

psacct_probe_tar: $(root)/target/gratia_psacct_probe_$(psacct_probe_version).tar

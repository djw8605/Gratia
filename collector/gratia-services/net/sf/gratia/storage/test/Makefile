PLATFORM := $(shell uname)

ifeq ($(filter CYGWIN%,$(PLATFORM)),)
# Linux
root=../../../../../../
#root=/home/pcanal/gratia/collector/
configuratio=${root}/../common/configuration
jarloc=$(root)/../common/lib/
#export CLASSPATH=$(root)/gratia-services:${root}/../common/gratia-util:$(jarloc)/antlr-2.7.6.jar:jars/bsh.jar:jars/derby.jar:jars/derbytools.jar:\
#$(jarloc)/hibernate3.jar:$(jarloc)/dom4j-1.6.1.jar:$(jarloc)/commons-logging-1.1.1.jar:$(jarloc)/commons-collections-2.1.1.jar:$(jarloc)/cglib-2.1.3.jar:\
#$(jarloc)/ehcache-1.1.jar:$(jarloc)/jta.jar:$(jarloc)/jaxen-1.1-beta-7.jar:$(jarloc)/asm.jar:$(jarloc)/tar.jar:$(jarloc)/bzip2.jar:$(jarloc)/log4j-1.2.15.jar:$(jarloc)/commons-lang-2.4.jar:$(jarloc)/servlet-api.jar:$(jarloc)/mail.jar
export CLASSPATH=$(root)/gratia-services:${root}/../common/gratia-util:$(shell echo $(jarloc)/*.jar | tr ' ' ':'):$(shell echo jars/*.jar | tr ' ' ':')

fixpath=echo
else
# Cygwin
root=..\..\..\..\..\..\\
jarloc=..\..\..\..\..\..\..\common\lib\\
export CLASSPATH=$(root)\gratia-services;$(jarloc)\antlr-2.7.5.jar;jars\bsh.jar;jars\derby.jar;jars\derbytools.jar;$(jarloc)\hibernate3.jar;$(jarloc)\dom4j-1.6.1.jar;$(jarloc)\commons-logging-1.0.4.jar;$(jarloc)\commons-collections-2.1.1.jar;$(jarloc)\cglib-2.1.3.jar;$(jarloc)\ehcache-1.1.jar;$(jarloc)\jta.jar;$(jarloc)\jaxen-1.1-beta-7.jar;$(jarloc)\asm.jar;$(jarloc)\log4j-1.2.15.jar;$(jarloc)\commons-lang-2.4.jar;$(jarloc)\servlet-api.jar;$(jarloc)\mail.jar
fixpath=cygpath -w 
endif

hibconf=${root}/../common/configuration/hibernate


all: test

XP:
	javac $(shell $(fixpath) '${root}/../common/gratia-util/net/sf/gratia/util/XP.java' )

Logging:
	javac $(shell $(fixpath) '${root}/../common/gratia-util/net/sf/gratia/util/Logging.java' )

storage:
	javac $(shell $(fixpath) $(root)/gratia-services/net/sf/gratia/storage/*.java )

hibernate:
	mkdir -p hibernate

#hibernate/JobUsage.hbm.xml: $(hibconf)/JobUsage.hbm.xml
#	cp $(hibconf)/JobUsage.hbm.xml hibernate/

#hibernate/Gratia.hbm.xml: $(hibconf)/Gratia.hbm.xml
#	cp $(hibconf)/Gratia.hbm.xml hibernate/

#hibernate/MetricRecord.hbm.xml: $(hibconf)/MetricRecord.hbm.xml
#	cp $(hibconf)/MetricRecord.hbm.xml hibernate/

#hibernate/TransferDetails.hbm.xml: $(hibconf)/TransferDetails.hbm.xml
#	cp $(hibconf)/TransferDetails.hbm.xml hibernate/
	

hibernate_files=$(subst $(hibconf),hibernate,$(wildcard $(hibconf)/*.xml))
debug:=$(shell echo $(hibernate_files) 1>&2 )

hibernate/%.xml: $(hibconf)/%.xml hibernate
	cp $< $@

test:  XP Logging storage $(hibernate_files)
	rm -rf derby output *logfile*; mkdir output
	java bsh.Interpreter test.bsh

qtest:
	rm -rf derby output *logfile*; mkdir output
	java bsh.Interpreter test.bsh

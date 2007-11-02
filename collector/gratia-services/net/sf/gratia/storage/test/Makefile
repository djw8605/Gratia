PLATFORM := $(shell uname)

ifeq ($(filter CYGWIN%,$(PLATFORM)),)
# Linux
root=../../../../../../
jarloc= ../../../../../../../common/lib/
export CLASSPATH=$(root)/gratia-services:$(jarloc)/jars/antlr-2.7.5.jar:jars/bsh.jar:jars/derby.jar:jars/derbytools.jar:$(jarloc)/hibernate3.jar:$(jarloc)/dom4j-1.6.1.jar:$(jarloc)/commons-logging-1.0.4.jar:$(jarloc)/commons-collections-2.1.1.jar:$(jarloc)/cglib-2.1.3.jar:$(jarloc)/ehcache-1.1.jar:$(jarloc)/jta.jar:$(jarloc)/jaxen-1.1-beta-7.jar:$(jarloc)/asm.jar
else
# Cygwin
root=..\..\..\..\..\..\\
jarloc=..\..\..\..\..\..\..\common\lib\\
export CLASSPATH=$(root)\gratia-services;$(jarloc)\antlr-2.7.5.jar;jars\bsh.jar;jars\derby.jar;jars\derbytools.jar;$(jarloc)\hibernate3.jar;$(jarloc)\dom4j-1.6.1.jar;$(jarloc)\commons-logging-1.0.4.jar;$(jarloc)\commons-collections-2.1.1.jar;$(jarloc)\cglib-2.1.3.jar;$(jarloc)\ehcache-1.1.jar;$(jarloc)\jta.jar;$(jarloc)\jaxen-1.1-beta-7.jar;$(jarloc)\asm.jar
endif

all: test

XP:
	javac '..\..\services\XP.java'

Logging:
	javac '..\..\services\Logging.java'

storage:
	javac '..\..\storage\*.java'

JobUsage.hbm.xml: ../../../../../../../common/configuration/JobUsage.hbm.xml
	cp ../../../../../../../common/configuration/JobUsage.hbm.xml .

Gratia.hbm.xml: ../../../../../../../common/configuration/Gratia.hbm.xml
	cp ../../../../../../../common/configuration/Gratia.hbm.xml .

MetricRecord.hbm.xml: ../../../../../../../common/configuration/MetricRecord.hbm.xml
	cp ../../../../../../../common/configuration/MetricRecord.hbm.xml .

test:  XP Logging storage JobUsage.hbm.xml MetricRecord.hbm.xml Gratia.hbm.xml
	rm -rf derby output *logfile*; mkdir output
	java bsh.Interpreter test.bsh

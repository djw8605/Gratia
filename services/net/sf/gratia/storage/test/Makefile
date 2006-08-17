PLATFORM := $(shell uname)

ifeq ($(filter CYGWIN%,$(PLATFORM)),)
# Linux
root=../../../../../../
export CLASSPATH=../../../../../../services:../../../../../../jars/antlr-2.7.5.jar:jars/bsh.jar:jars/derby.jar:jars/derbytools.jar:../../../../../../jars/hibernate3.jar:../../../../../../jars/dom4j-1.6.1.jar:../../../../../../jars/commons-logging-1.0.4.jar:../../../../../../jars/commons-collections-2.1.1.jar:../../../../../../jars/cglib-2.1.3.jar:../../../../../../jars/ehcache-1.1.jar:../../../../../../jars/jta.jar:../../../../../../jars/jaxen-1.1-beta-7.jar
else
# Cygwin
root=..\..\..\..\..\..\\
export CLASSPATH=..\..\..\..\..\..\services;..\..\..\..\..\..\jars\antlr-2.7.5.jar;jars\bsh.jar;jars\derby.jar;jars\derbytools.jar;..\..\..\..\..\..\jars\hibernate3.jar;..\..\..\..\..\..\jars\dom4j-1.6.1.jar;..\..\..\..\..\..\jars\commons-logging-1.0.4.jar;..\..\..\..\..\..\jars\commons-collections-2.1.1.jar;..\..\..\..\..\..\jars\cglib-2.1.3.jar;..\..\..\..\..\..\jars\ehcache-1.1.jar;..\..\..\..\..\..\jars\jta.jar;..\..\..\..\..\..\jars\jaxen-1.1-beta-7.jar
endif


all: test

XP:
	javac '$(root)services\net\sf\gratia\services\XP.java'

Logging:
	javac '$(root)services\net\sf\gratia\services\Logging.java'

storage:
	javac '$(root)services\net\sf\gratia\storage\*.java'

JobUsage.hbm.xml: ../../../../../../configuration/JobUsage.hbm.xml
	cp ../../../../../../configuration/JobUsage.hbm.xml .

test:  XP Logging storage
	rm -rf derby output *logfile*; mkdir output
	java bsh.Interpreter test.bsh

set root=..\..\..\..\..\..\
dir %root%
set classpath=%root%services;%root%jars\antlr-2.7.5.jar;jars\bsh.jar;jars\derby.jar;jars\derbytools.jar;%root%jars\hibernate3.jar
set classpath=%classpath%;%root%jars\dom4j-1.6.1.jar;%root%jars\commons-logging-1.0.4.jar
set classpath=%classpath%;%root%jars\commons-collections-2.1.1.jar;%root%jars\cglib-2.1.3.jar
set classpath=%classpath%;%root%jars\ehcache-1.1.jar;%root%jars\jta.jar;%root%jars\jaxen-1.1-beta-7.jar

del *~
del *#
del derby.log
rmdir /q /s derby
rmdir /q /s output
mkdir output

javac %root%services\net\sf\gratia\services\XP.java
javac %root%services\net\sf\gratia\services\Logging.java
javac %root%services\net\sf\gratia\storage\*.java

copy %root%configuration\JobUsage.hbm.xml

java bsh.Interpreter test.bsh


del *~
del *#
del *logfile*
del derby.log
del JobUsage*
rmdir /q /s derby









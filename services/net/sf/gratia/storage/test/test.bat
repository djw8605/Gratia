set root=..\..\..\..\..\..\
dir %root%
set classpath=%root%services;jars\antlr-2.7.5.jar;jars\bsh.jar;jars\derby.jar;jars\derbytools.jar;jars\hibernate3.jar
set classpath=%classpath%;jars\dom4j-1.6.1.jar;jars\commons-logging-1.0.4.jar
set classpath=%classpath%;jars\commons-collections-2.1.1.jar;jars\cglib-2.1.3.jar
set classpath=%classpath%;jars\ehcache-1.1.jar;jars\jta.jar

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









export root=../../../../../../

export CLASSPATH=${root}services:jars/antlr-2.7.5.jar:jars/bsh.jar:jars/derby.jar:jars/derbytools.jar:jars/hibernate3.jar
export CLASSPATH=${CLASSPATH}:jars/dom4j-1.6.1.jar:jars/commons-logging-1.0.4.jar
export CLASSPATH=${CLASSPATH}:jars/commons-collections-2.1.1.jar:jars/cglib-2.1.3.jar
export CLASSPATH=${CLASSPATH}:jars/ehcache-1.1.jar:jars/jta.jar

rm -f *~
rm -f *#
rm -f derby.log
rm -f -r derby
rm -f -r output
mkdir output

javac ${root}services/net/sf/gratia/services/XP.java
javac ${root}services/net/sf/gratia/services/Logging.java
javac ${root}services/net/sf/gratia/storage/*.java
cp ${root}configuration/JobUsage.hbm.xml .

java bsh.Interpreter test.bsh


rm -f *~
rm -f *#
rm -f *logfile*
rm -f derby.log
rm -f JobUsage*
rm -f -r derby


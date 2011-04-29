#!/bin/sh

# Author: Karthik
# Date: 25th Apr 2011
# Purpose: Helper script to compile and execute the java parsing test code 
#	   This code provides the class path for all the jar files needed for the code to compile/execute.
# Run this script to view the results of the stand alone test that tests the parsing code and check if the actual results match with the expected results

#baseDir="/myhome1/osg/standaloneParser" # The base directory for gratia code could be either hard coded here or if not, given from the command line 

usage()
{
	echo Usage:
	echo "./$0 <baseDir>"
	echo baseDir is the absolute path to the root directory in which the gratia code is available/installed. 
	echo This is also the parent directory containing the build-scripts directory underneath it. Please note that
	echo you should have already built the code using "gmake release" from  the build-scripts directory.
}

commandExists()
{
        if command -v $1 &>/dev/null
        then
                return 1
        else
                echo " Command :$1: was NOT found. Please make sure that $1 is in the path. Exiting ..." && exit 1
        fi
}

sanityCheck()
{
	echo checking...
	# Check if base directory is either defined inside this script and if not it is given from command line. If neither, display usage and exit
	[ -z $baseDir ] && [ $# -ne 1 ] && usage && exit 1

	# Check to see if the java and javac command exists in the path. If not, display error and exit
	commandExists javac
	commandExists java
}

constructClassPath()
{
	baseDir=$1
	pathToJar=$2
	classPath=".:$baseDir/collector/gratia-services:$baseDir/$pathToJar/JCup.jar:$baseDir/$pathToJar/activation.jar:$baseDir/$pathToJar/ant-antlr-1.7.1.jar:$baseDir/$pathToJar/antlr-2.7.6.jar:$baseDir/$pathToJar/asm-3.2.jar:$baseDir/$pathToJar/axis-ant.jar:$baseDir/$pathToJar/axis.jar:$baseDir/$pathToJar/bcprov-jdk14-133.jar:$baseDir/$pathToJar/bzip2.jar:$baseDir/$pathToJar/c3p0-0.9.1.2.jar:$baseDir/$pathToJar/cglib-2.2.jar:$baseDir/$pathToJar/commons-cli-1.2.jar:$baseDir/$pathToJar/commons-codec-1.4.jar:$baseDir/$pathToJar/commons-collections-3.2.1.jar:$baseDir/$pathToJar/commons-discovery-0.4.jar:$baseDir/$pathToJar/commons-lang-2.4.jar:$baseDir/$pathToJar/commons-logging-1.1.1.jar:$baseDir/$pathToJar/commons-logging-adapters-1.1.1.jar:$baseDir/$pathToJar/commons-logging-api-1.1.1.jar:$baseDir/$pathToJar/dom4j-1.6.1.jar:$baseDir/$pathToJar/ehcache-1.6.2.jar:$baseDir/$pathToJar/glite-security-trustmanager.jar:$baseDir/$pathToJar/glite-security-util-java.jar:$baseDir/$pathToJar/glite-security-voms-admin-server.jar:$baseDir/$pathToJar/hibernate3.jar:$baseDir/$pathToJar/jakarta-regexp-1.5.jar:$baseDir/$pathToJar/javassist-3.9.0.GA.jar:$baseDir/$pathToJar/jaxen-1.1.1.jar:$baseDir/$pathToJar/jaxp-api.jar:$baseDir/$pathToJar/jaxrpc.jar:$baseDir/$pathToJar/jms.jar:$baseDir/$pathToJar/jmxri.jar:$baseDir/$pathToJar/jta-1.1.jar:$baseDir/$pathToJar/jtidy-r820.jar:$baseDir/$pathToJar/mail.jar:$baseDir/$pathToJar/mysql-connector-java-5.0.8-bin.jar:$baseDir/$pathToJar/ow_monolog.jar:$baseDir/$pathToJar/saaj.jar:$baseDir/$pathToJar/sac.jar:$baseDir/$pathToJar/scriptapi.jar:$baseDir/$pathToJar/serializer.jar:$baseDir/$pathToJar/slf4j-api-1.5.8.jar:$baseDir/$pathToJar/tar.jar:$baseDir/$pathToJar/tomcat-util.jar:$baseDir/$pathToJar/viewservlets.jar:$baseDir/$pathToJar/wsdl4j-1.6.2.jar:$baseDir/$pathToJar/xercesImpl.jar:$baseDir/$pathToJar/gratia-util.jar:$baseDir/common/lib/log4j-1.2.15.jar"
}

# Do sanity check to make sure the script has all it needs before it could run the parsing test
sanityCheck $1

# If base directory is still not defined, get it from the 1st command line argument
[ -z $baseDir ] && baseDir=$1  

# Hard coded relative path to the jar files 
pathToJar="build-scripts/output-dir/wars/gratia-services/WEB-INF/lib"

# construct the class path using the base directory and path to jar information
constructClassPath $baseDir $pathToJar

# Now compile and execute the java parsing test code
file="ParseTester.java"
class=`echo $file|cut -d'.' -f1`

echo compiling $file
javac -cp $classPath $file
echo executing $class
java -cp $classPath $class

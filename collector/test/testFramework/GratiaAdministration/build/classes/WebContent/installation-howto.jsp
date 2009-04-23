<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link href="docstyle.css" type="text/css" rel="stylesheet" >
<title>Gratia - Installation howto</title>
</head>

<body>
<%
		
		String uriPart = request.getRequestURI();
		int slash2 = uriPart.substring(1).indexOf("/") + 1;
		uriPart = uriPart.substring(slash2);
		String queryPart = request.getQueryString();
		if (queryPart == null)
			queryPart = "";
		else
			queryPart = "?" + queryPart;

		session.setAttribute("displayLink", "." + uriPart + queryPart);
%>
<p align="center"><b ><u>Installation</u></b></p>
<p >O.K. &hellip; You&rsquo;ve now downloaded from CVS the gratia source tree. Where to go from here?</p>
<p >There are a number of assumptions being made when you bring up gratia for the first time:</p>
<ul type="disc">
  <li>You are running java 1.5 or greater.</li>
  <li>You have installed mysql and have created an empty schema (default name gratia) that the system will use. The userid and password associated with the schema must have table/index creation privileges.</li>
</ul>
<p ><b><u>Building:</u></b></p>
<ul type="disc">
  <li>There are 4 projects that must be downloaded from CVS in order to build the complete gratia release. Specifically, gratia, GratiaReportConfiguration, GratiaReporting, and GratiaReports.</li>
  <li>Additionally, all 4 projects must be placed in a common directory.</li>
  <li>To actually build a complete release, run the makefile found in gratia/build-scripts. This will create all of the necessary war files and place them in gratia/target.</li>
</ul>

<p ><b><u>Deployment:</u></b></p>
<ul type="disc">
  <li>Create a subdirectory in your tomcat installation called $CATALINA_HOME/gratia.</li>
  <li>Copy the contents of gratia/configuration to this new directory.</li>
  <li>Finally, copy all war files in gratia/target to $CATALINA_HOME/webapps.</li>
</ul>
<p ><b><u>Tomcat Configuration:</u></b></p>
<ul type="disc">
  <li>Modify $CATALINA_HOME/gratia/service-configuration.properties:

  <ul type="circle">
    <li>HOSTNAME entry to the fully qualified host name of the machine you have installed on.</li>
    <li>DBURL to point the what ever mysql database you are going to use. The form of the url is
	<br><em>jdbc:mysql://HOSTNAME:&lt;mysqlport&gt;/&lt;schema name&gt;. </em>
	<br> Look at the entries in configuration-psg3 to get an example.</li>
    <li>DBUSER must be set to an appropriate user name on the database with table create priveledges.</li>
    <li>DBPASSWORD must be set.</li>
  </ul></li>
 <li>Modify $CATALINA_HOME/conf/server.xml. Specifically, you must define at least 1 open socket
  (we use 8880 as a default) and, if running a secure connection,  a ssl port (we use 8843 as a default).
  More information on security can be found in the gratia security howto page.</li>
</ul>

<p ><b><u>Server Configuration:</u></b></p>
<p >Under linux, at least, you should use something similar to the following for you inet.d configuration:</p>
<pre>
#!/bin/sh
#
# Penelope: Start tomcat-ps
#

export CATALINA_HOME=/data/tomcat
export JAVA_HOME=/data/vdt/jdk1.5
export JAVA_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8004 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dssl.port=8443"

if [ "$1" = "start" ] ; then
 $CATALINA_HOME/bin/catalina.sh start
elif [ "$1" = "stop" ] ; then
 $CATALINA_HOME/bin/shutdown.sh
else
 echo "Usage: tomcat-ps [start | stop]"
 exit 1
fi
</pre>
<p >Also, under linux, you should modify the shutdown.sh script to the following (note the addition of the unset command):</p>
<pre>
#!/bin/sh
# -----------------------------------------------------------------------------
# Stop script for the CATALINA Server
#
# $Id: installation-howto.jsp 2997 2009-02-16 15:56:13Z pcanal $
# -----------------------------------------------------------------------------

unset $JAVA_OPTS

# resolve links - $0 may be a softlink
PRG="$0"
while [ -h "$PRG" ] ; do
 ls=`ls -ld "$PRG"`
 link=`expr "$ls" : '.*-&gt; \(.*\)$'`
 if expr "$link" : '.*/.*' &gt; /dev/null; then
  PRG="$link"
 else
  PRG=`dirname "$PRG"`/"$link"
 fi
done

PRGDIR=`dirname "$PRG"`
EXECUTABLE=catalina.sh

# Check that target executable exists
if [ ! -x "$PRGDIR"/"$EXECUTABLE]; then
 echo "Cannot find $PRGDIR/$EXECUTABLE"
 echo "This file is needed to run this program"
 exit 1
fi

exec "$PRGDIR"/"$EXECUTABLE" stop "$@"
</pre>

<p ><b><u>Running Multiple Gratias:</u></b></p>

<p >If you want to run multiple instances of gratia on the same server (meaning multiple copies of tomcat) there are several other items to be taken care of:</p>
<ul type="disc">
  <li>The obvious is that each tomcat instance must have unique port numbers (e.g. &ndash; altering the server.xml file).</li>
  <li>The port numbers in the service-configuration.properties file must match those in server.xml.</li>
  <li>Finally, JAVA_OPTS in the inet.d must have new ports.</li>
</ul>
</body>
</html>

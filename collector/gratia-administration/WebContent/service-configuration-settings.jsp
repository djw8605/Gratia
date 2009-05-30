<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<style type="text/css">
th
{
background-color: #CCCCCC;
}

tr.section
{
background-color: #DEDEDE;
}

td.section
{
font-weight: bold;
}

td.section_info
{
font-style: italic;
}

td.property
{
font-family: monospace;
}

td.explanation
{
}

td.example
{
}

</style>
<link href="docstyle.css" type="text/css" rel="stylesheet">

<title>Gratia - Service configuration howto</title>
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

<h1 align="center">Service Configuration Settings </h1>

<table border="1" cellspacing="0" width="80%" align="center">
  <thead>
    <tr >
      <th valign="top" width="30%">Setting</th>
      <th valign="top" width="55%">Meaning</th>
      <th valign="top" width="15%">Default/Example</th>
    </tr>
  </thead>
  <tbody>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"style=""><a name="safe_mode"/>Safe mode</td>
    </tr>
    <tr >
      <td valign="top" class="property" width="30%">gratia.service.safeStart</td>
      <td valign="top" class="explanation" width="55%">Safe mode -- no uploads accepted, no operational threads. Start services from admin page.</td>
      <td valign="top" class="example" width="15%">0</td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="rmi_port_settings"/>RMI port settings</td>
    </tr> <tr >
      <td valign="top" class="property">service.rmi.port</td>
      <td valign="top" class="explanation">The default port used by gratia for internal rmi use</td>
      <td valign="top" class="example">17000</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.rmi.rmibind</td>
      <td valign="top" class="explanation">The registered rmi name for rmi access</td>
      <td valign="top" class="example">//Fermi.fnal.gov:17000</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.rmi.lookup</td>
      <td valign="top" class="explanation">The &quot;external&quot; rmi name that outside services can access</td>
      <td valign="top" class="example">rmi://Fermi.fnal.gov:17000</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.rmi.service</td>
      <td valign="top" class="explanation">The tomcat webapp that rmi requests can be sent to</td>
      <td valign="top" class="example">/gratia</td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="db_access"/>DB access</td>
    </tr>
    <tr>
      <td valign="top" class="property">service.mysql.driver</td>
      <td valign="top" class="explanation">The name of the mysql sql driver being used.</td>
      <td valign="top" class="example">com.mysql.jdbc.Driver</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.mysql.url</td>
      <td valign="top" class="explanation">The url of the gratia database</td>
      <td valign="top" class="example">jdbc:mysql://Fermi.fnal.gov:3306/gratia</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.mysql.user</td>
      <td valign="top" class="explanation">The user id allowed to access/update the database</td>
      <td valign="top" class="example">youruserid</td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="schema_flavor_settings"/>Schema flavor settings</td>
    </tr>
    <tr >
      <td valign="top" class="property">gratia.database.wantSummaryTable</td>
      <td valign="top" class="explanation">Needed for osg-daily only</td>
      <td valign="top" class="example">1 (set to 0 for osg-daily)</td>
    </tr>
    <tr >
      <td valign="top" class="property">gratia.database.wantSummaryTrigger</td>
      <td valign="top" class="explanation">Needed for osg-daily only</td>
      <td valign="top" class="example">1 (set to 0 for osg-daily)</td>
    </tr>
    <tr >
      <td valign="top" class="property">gratia.database.wantStoredProcedures</td>
      <td valign="top" class="explanation">Load stored procedures to DB (or not)</td>
      <td valign="top" class="example">1 (no current application for non-default value)</td>
    </tr>
    <tr >
      <td valign="top" class="property">gratia.database.useJobUsageSiteName</td>
      <td valign="top" class="explanation">Needed for ps-accounting or osg-daily only</td>
      <td valign="top" class="example">0 (set to 1 for ps-accounting or osg-daily)</td>
    </tr>
    <tr >
      <td valign="top" class="property">gratia.database.wantNodeSummary</td>
      <td valign="top" class="explanation">Needed for ps-accounting</td>
      <td valign="top" class="example">0 (set to 1 for ps-accounting)</td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="db_cleanup"/>DB cleanup</td>
    </tr>
    <tr>
      <td colspan="3" class="section_info">All lifetime attributes take a simple, "plain English" value, such as "3 years" or "1 day"</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.JobUsageRecord</td>
      <td valign="top" class="explanation">Lifetime of job usage records.</td>
      <td valign="top" class="example">3 months</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.JobUsageRecord.<br/>RawXML</td>
      <td valign="top" class="explanation">Lifetime of raw job usage XML records.</td>
      <td valign="top" class="example">1 month</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.MetricRecord</td>
      <td valign="top" class="explanation">Lifetime of metric records.</td>
      <td valign="top" class="example">36 months</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.MetricRecord.<br/>RawXML</td>
      <td valign="top" class="explanation">Lifetime of raw metric XML records.</td>
      <td valign="top" class="example">1 month</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.ComputeElement</td>
      <td valign="top" class="explanation">Lifetime of ComputeElement records.</td>
      <td valign="top" class="example">36 months</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.StorageElement</td>
      <td valign="top" class="explanation">Lifetime of StorageElement records.</td>
      <td valign="top" class="example">36 months</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.ComputeElementRecord</td>
      <td valign="top" class="explanation">Lifetime of ComputeElementRecord records.</td>
      <td valign="top" class="example">36 months</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.StorageElementRecord</td>
      <td valign="top" class="explanation">Lifetime of StorageElementRecord records.</td>
      <td valign="top" class="example">36 months</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.Subcluster</td>
      <td valign="top" class="explanation">Lifetime of Subcluster records.</td>
      <td valign="top" class="example">36 months</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.DupRecord.<br/>Duplicate</td>
      <td valign="top" class="explanation">Lifetime of duplicates.</td>
      <td valign="top" class="example">1 month</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.DupRecord.<br/>ExpirationDate</td>
      <td valign="top" class="explanation">Lifetime of entries recording out-of-date job records.</td>
      <td valign="top" class="example">1 month</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.DupRecord</td>
      <td valign="top" class="explanation">Lifetime of other DupRecord entries.</td>
      <td valign="top" class="example">UNLIMITED</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.Trace</td>
      <td valign="top" class="explanation">Lifetime of Trace table records.</td>
      <td valign="top" class="example">6 months</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.lifetime.Trace.<br/>add_JUR_to_summary</td>
      <td valign="top" class="explanation">Lifetime of Trace table records from the Job Usage summarization procedure.</td>
      <td valign="top" class="example">3 months</td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="administrative_security_settings"/>Administrative security settings</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.voms.connections</td>
      <td valign="top" class="explanation">File located in TOMCAT_LOCATION/gratia containing the voms URL(s) for any
            service.admin.FQAN attributes that are active.</td>
      <td valign="top" class="example">Not set.<br/>Example: <tt>voms-servers</tt><br>File can be updated
            using <tt>TOMCAT_LOCATION/gratia/<br/>voms-server.sh</tt></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.admin.FQAN.<em>n</em></td>
      <td valign="top" class="explanation">FQANs granting administrative privileges.</td>
      <td valign="top" class="example">Not set.<br/>Example: /cms/Role=GratiaAdmin</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.admin.DN.<em>n</em></td>
      <td valign="top" class="explanation">DNs granted administrative privileges.</td>
      <td valign="top" class="example">Not set. Examples:<br/>ALLOW ALL<br/>Any personal, service or host certificate.</td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="upload_security_settings"/>Upload security settings</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.security.level</td>
      <td valign="top" class="explanation">Flag indicating what form of security should gratia assume.</td>
      <td valign="top" class="example">0 = No security<br>
          1 = Track connections without SSL, check connection validity<br>
          2 = Check only SSL certificate (both cert validity and validity within Collector)<br>
          3 = Same as 2 + Track connection with SSL and check connection validity<br>
          4 = Require SSL connection + same checks as 2<br>
          5 = Same as 4 + Track connection with SSL and check connection validity<br>
    </td>
    </tr>
    <tr >
      <td valign="top" class="property">service.use.selfgenerated.certs</td>
      <td valign="top" class="explanation">Flag indicating whether or not gratia should generate it&rsquo;s own certs or use vdt/doe certs</td>
      <td valign="top" class="example">1 = Use self generated certs</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.vdt.cert.file</td>
      <td valign="top" class="explanation">Absolute path to vdt/doe public cert file</td>
      <td valign="top" class="example">/etc/grid-security/httpcert.pem</td>
    </tr>
    <tr >
      <td valign="top" class="property">Service.vdt.key.file</td>
      <td valign="top" class="explanation">Absolute path to vdt/doe private key file</td>
      <td valign="top" class="example">/etc/grid-security/httpkey.pem</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.autoregister.pem</td>
      <td valign="top" class="explanation">Flag indicating whether or not external services (when running ssl) will automatically be trusted. values are 0 (no) or 1 (yes) &ndash; refer to security howto</td>
      <td valign="top" class="example">1</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.open.connection</td>
      <td valign="top" class="explanation">The open, unsecured url clients can connect to</td>
      <td valign="top" class="example">http://fermilab:8880</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.secure.connection</td>
      <td valign="top" class="explanation">The ssl enabled url that clients can connect to</td>
      <td valign="top" class="example">https://fermilab:8843</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.ca.certificates</td>
      <td valign="top" class="explanation">Location of trusted CA certificates.</td>
      <td valign="top" class="example">/etc/grid-security/certificates/</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.ca.crls</td>
      <td valign="top" class="explanation">Location of up-to-date CRLs for CAs.</td>
      <td valign="top" class="example">/etc/grid-security/certificates/</td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="reporting"/>Reporting</td>
    </tr>
    <tr >
      <td valign="top" class="property">use.report.authentication</td>
      <td valign="top" class="explanation">disallow certain reports or restrict information based on credentials.</td>
      <td valign="top" class="example"><b>true</b> / false</td>
    </tr>    <tr >
      <td valign="top" class="property">service.reporting.user</td>
      <td valign="top" class="explanation">The userid (with read only access) to your database</td>
      <td valign="top" class="example">reader</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.reporting.password</td>
      <td valign="top" class="explanation">Password for user.</td>
      <td valign="top" class="example">reader</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.reporting.reports.folder</td>
      <td valign="top" class="explanation">Path (relative to webapps) of the folder containing the various rptdesign files.</td>
      <td valign="top" class="example">gratia-reports/reports</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.reporting.static.folder</td>
      <td valign="top" class="explanation">Path (relative to webapps) of the folder to store the static reports (pdf format) produced daily.</td>
      <td valign="top" class="example">gratia-reports/reports-static</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.reporting.menuconfig</td>
      <td valign="top" class="explanation">Path (relative to webapps) and file name of the file containg the menu configuration.</td>
      <td valign="top" class="example">gratia-reports/MenuConfig/UserConfig_osg.xml</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.reporting.staticreports</td>
      <td valign="top" class="explanation">Path (relative to webapps) and file name of the file containg the static reports to be produced on a daily basis.</td>
      <td valign="top" class="example">gratia-reports/MenuConfig/StaticReports.xml</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.reporting.interfaces.<br/>apel.folder</td>
      <td valign="top" class="explanation">Area to contain the files generated containing information for upload to WLCG.</td>
      <td valign="top" class="example">gratia-data/interfaces/apel-lcg</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.reporting.interfaces.<br/>apel.folder.groupowner</td>
      <td valign="top" class="explanation">UNIX(tm) group owning the APEL interfaces folder.</td>
      <td valign="top" class="example">gratia</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.reporting.logging</td>
      <td valign="top" class="explanation">Whether to log reporting information.</td>
      <td valign="top" class="example"><b>true</b> / false</td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="service_controls"/>Service controls</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.listener.threads</td>
      <td valign="top">The number of threads used to process incoming data transmissions</td>
      <td valign="top">1</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.listener.threads</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">monitor.q.size</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">max.q.size</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">maintain.history.log</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">maintain.history.compress</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">maintain.recordsPerDirectory</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.replication.wait</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.probe.monitor.wait=1000</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.datapump.chunksize=30000</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.datapump.trace</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">monitor.listener.threads</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">monitor.listener.wait</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">monitor.smtp.server</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">monitor.smtp.authentication.required</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">monitor.smtp.user</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">monitor.smtp.password</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">monitor.from.address</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">monitor.subject</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property">monitor.to.address.<em>n</em></td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr >
      <td valign="top" class="property"></td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"></td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="logging"/>Logging</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.logging.useLog4j</td>
      <td valign="top" class="explanation"></td>
      <td valign="top" class="example"><b>1</b></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.max.logsize</td>
      <td valign="top" class="explanation">Maximum size of logfile before flipping (overrides service-specific values of <tt>.maxlog</tt>.<br/>Ignored if <b>log4j</b> output is configured (see above).</td>
      <td valign="top" class="example"><b>250000000</b></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.logging.dailyLogging</td>
      <td valign="top" class="explanation">Save log entries by day (only applicable when <b>log4j</b> logging is enabled).</td>
      <td valign="top" class="example"><b>1</b></td>
    </tr>
    <tr >
      <td colspan="3" class="section_info">Each service may have a level of:<br/><tt>ALL|CONFIG|FINE|FINER|FINEST|INFO|SEVERE|WARNING|OFF</tt>.</td>
    </tr>
    <tr >
      <td colspan="3" class="section_info">If <tt>service.logging.useLog4j</tt> is selected, then the <tt>.logfile</tt> specification should contain a <tt>%g</tt> for log file numbering and rotation purposes. Also, the <tt>.logfile</tt> attribute is ignored in this case.</td>
    </tr>
    <tr >
      <td colspan="3" class="section_info">Some third party libraries log via <b>log4j</b> and can be controlled via <tt>log4j.properties</tt> in <tt>common/classes/</tt></td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="collector_logging"/>collector logging</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.service.logfile</td>
      <td valign="top" class="explanation">Log file name.</td>
      <td valign="top" class="example">/logs/gratia.log</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.service.console</td>
      <td valign="top" class="explanation">Copy log messages to console.</td>
      <td valign="top" class="example"><b>0</b> or 1.</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.service.maxlog</td>
      <td valign="top" class="explanation">Max log file size (b).</td>
      <td valign="top" class="example"><b>10000000</b></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.service.level</td>
      <td valign="top" class="explanation">Logging level.</td>
      <td valign="top" class="example"><b><tt>FINE</tt></b></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.service.numlogs</td>
      <td valign="top" class="explanation">No. of logs to keep.</td>
      <td valign="top" class="example"><b>30</b></td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="servlet_logging"/>Servlet logging</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.rmiservlet.logfile</td>
      <td valign="top" class="explanation">Log file name.</td>
      <td valign="top" class="example">/logs/gratia-rmi-servlet.log</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.rmiservlet.console</td>
      <td valign="top" class="explanation">Copy log messages to console.</td>
      <td valign="top" class="example"><b>0</b> or 1.</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.rmiservlet.maxlog</td>
      <td valign="top" class="explanation">Max log file size (b).</td>
      <td valign="top" class="example"><b>10000000</b></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.rmiservlet.level</td>
      <td valign="top" class="explanation">Logging level.</td>
      <td valign="top" class="example"><b><tt>FINE</tt></b></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.rmiservlet.numlogs</td>
      <td valign="top" class="explanation">No. of logs to keep.</td>
      <td valign="top" class="example"><b>30</b></td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="security_logging"/>Security logging</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.security.logfile</td>
      <td valign="top" class="explanation">Log file name.</td>
      <td valign="top" class="example">/logs/gratia-security.log</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.security.console</td>
      <td valign="top" class="explanation">Copy log messages to console.</td>
      <td valign="top" class="example"><b>0</b> or 1.</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.security.maxlog</td>
      <td valign="top" class="explanation">Max log file size (b).</td>
      <td valign="top" class="example"><b>10000000</b></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.security.level</td>
      <td valign="top" class="explanation">Logging level.</td>
      <td valign="top" class="example"><b><tt>INFO</tt></b></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.security.numlogs</td>
      <td valign="top" class="explanation">No. of logs to keep.</td>
      <td valign="top" class="example"><b>30</b></td>
    </tr>
    <tr class="section" >
      <td valign="top" colspan="3" class="section"><a name="administration_logging"/>Administration logging</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.administration.logfile</td>
      <td valign="top" class="explanation">Log file name.</td>
      <td valign="top" class="example">/logs/gratia-administration.log</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.administration.console</td>
      <td valign="top" class="explanation">Copy log messages to console.</td>
      <td valign="top" class="example"><b>0</b> or 1.</td>
    </tr>
    <tr >
      <td valign="top" class="property">service.administration.maxlog</td>
      <td valign="top" class="explanation">Max log file size (b).</td>
      <td valign="top" class="example"><b>10000000</b></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.administration.level</td>
      <td valign="top" class="explanation">Logging level.</td>
      <td valign="top" class="example"><b><tt>FINE</tt></b></td>
    </tr>
    <tr >
      <td valign="top" class="property">service.administration.numlogs</td>
      <td valign="top" class="explanation">No. of logs to keep.</td>
      <td valign="top" class="example"><b>30</b></td>
    </tr>

  </tbody>
</table>


</body>
</html>

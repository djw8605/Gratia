<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link href="docstyle.css" type="text/css" rel="stylesheet">
<title>Gratia - Replication Instructions</title>
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
<h1 align="center">Replication Instructions</h1>
<p>Gratia has the ability to replicate its database contents 
(JobUsageRecords, Metric and ProbeDetails data) 
to another instance of Gratia running "out there somewhere". 
This replication service can be found on the gratia-administration web service 
menu.</p>

<p>NB: when deciding what (if any data) to replicate: if you replicate
JobUsage or Metric data for a particular probe, please also replicate
ProbeDetails data for that same probe.</p>

<p>Assuming your database is the source, and the OSG/ITB Gratia collector at 
Fermilab is the target:</p>
<ol>
<li>Under the Replication menu, select either Job Usage Replication or 
Metric Replication depending on the accounting data you wish to forward.</li>

<li><p>On the Replication screen click on "New Entry" at the bottom of
the main section and fill in the details:</p>
<ol><li><p>If you are forwarding to the central Gratia OSG services at Fermilab, 
these are the urls for the respective collectors:</p>
<pre>
ITB 	http://gratia.opensciencegrid.org:8881
OSG 	http://gratia.opensciencegrid.org:8880
</pre>
<p>Otherwise, enter the correct URL including the protocol specifier and port number.</p>
</li>
<li>There are no operating collectors requiring security at this
time. Instructions will be updated if and when this becomes
available.</li>

<li><p>Under the Probe Name column, you can specify selective probes, probes 
for specific VOs or All. Press the Update button to add the entry to the 
database.</p>

<p>NOTE: If do not specify All, you will need to create a replication
entry for each probe or VO you desire to forward.</p></li>

<li>The, "Check Interval" column specifies the interval in minutes
between successive checks for new entries to replicate.</li>

<li>DBID is the dbid of the last entry replicated. Think <em>very
carefully</em> before changing this.</li>

<li>Bundle size is the number of records to send in one, "envelope." 0
indicates to use the default, which is usually 10. 1 will send each
record unwrapped and will work with older collectors.</li> </ol></li>

<li>When your new entry is complete, select, "update," or, "cancel" as
appropriate.</li>

<li><p>Then, for your entry, select Test under the Actions column on the
far right.</p>

<p>Your server will send a dummy transaction to the remote host and let
you know whether or not the connection was made.</p>

<p>It should indicate either success or failure. A failure means either
your entry was incorrect or the Remote Host is not available. If it is
not your entry, contact the Gratia administrator of the remote host or
try to access it via your web browser.</p>
</li>

<li><p>Finally, start the replication process by using the Start button under 
the Actions column. Replication should start within a minute or two. You can 
stop the process with the Stop link (which will also take a minute or so).</p>
</li>
</ol>

<p>You can view the status of your Replication entries by viewing:</p>
<ul>
<li> Running column which indicates if its running or not;</li>
<li> DBID column indicating the Gratia internal ID of the last record sent;</li>
<li> Row Count column indicating the number of replication entries sent.</li> 
</ul>


</body>
</html>

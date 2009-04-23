<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link href="docstyle.css" type="text/css" rel="stylesheet">
<title>Gratia Administration Login Configuration</title>
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

  <h1 align="center">Administration Login Configuration</h1>

  <p>Gratia administration services provides several options for securing the 
     administrative tasks.  The basic system status is available to all users.  
     All other services that allow you configure the system, turn on/off service, 
     perform replication, etc, can be restricted to selected personnel.
  </p>
  <p>The  <i>secured</i> tasks are all shown as <i>dimmed</i> in the menu until
     you are authorized. 
     When you attempt to access those tasks, you will be redirected to an 
     SSL secured port requiring you to have a certificate in your browser from 
     a trusted CA (Certificate Authority).  This initiates the login process.
  </p>

<p>The properties for the administration login can be found in the
<i>TOMCAT_LOCATION/gratia/service-configuration.properties</i> file set as
follows: 
<pre>
# service.admin.DN.0=ALLOW ALL
# service.admin.FQAN.0=FQAN
# service.voms.connections=voms-servers
</pre>
</p>

  <p>As you may have noticed above or if you have tried to access an 
     administrative task, all the properties are commented out. 
     Hence, the default is <i>no access</i> on initial installation.
  </p>

<p>A little comment on the <i>DN</i> and <i>FQAN</i> properities syntax as 
   they allow you to specify more than one for each of the properties.  
   The last level in the property can be either alpha or numeric as long as 
   it is unique as shown below:
</p>
<pre>
service.admin.DN.0=PERSON_ONE
service.admin.DN.a=PERSON_TWO
</pre>
<p>The <i>service.voms.connections</i> attribute is only required when using the
<i>service.admin.FQAN.x</i> property and identifies a file containing the url of 
the VO's VOMS web service that will be used for authorization.</p>

<hr width="75%"/>
<p><b>IMPORTANT:</b> Note the <i>logout</i> menu option at the top of the menu.
<br>You should <u>always logout</u> when you have completed your administrative
tasks or <u>close your browser session</u>.
</p>



<hr width="75%"/>
<p><b>Certificate only</b></p>
<p>This is the least secure option and only requires that the user 
present a certificate from a trusted CA.</p
<p><b>It is <u>not recommended</u> you operate in this manner.</b></p>
<p>It may be useful when you first install the the system and are just 
playing around or evaluating but, again, it is 
<b><u>not recommended</u></b> to operate in this mode.</p>

<p>If any <i>service.admin.DN.x</i> specifies the <i>ALLOW ALL</i> keyword, this
will take precedence over all other properties.
<pre>
service.admin.DN.0=ALLOW ALL
</pre>
<p/>

<hr width="75%"/>
<p><b>By specific individual</b></p>
<p>This option allows you to specify individual administrators based on 
the DN (certificate subject) of their browser's certificate.
</p>

<p>You can specify as many individuals as desired based on their certificates DN 
(subject) as shown below:</p>
<pre>
service.admin.DN.0=/DC=org/DC=doegrids/OU=People/CN=Person Name One  1234
service.admin.DN.1=/DC=org/DC=doegrids/OU=People/CN=Person Name Two
service.admin.DN.c=/DC=org/DC=doegrids/OU=People/CN=Person Three
</pre>

<hr width="75%"/>
<p><b>Based on an FQAN (Fully Qualified Attribute Name) of any VO using a 
VOMS  web service.</b></p> 
<p>This option is intended to allow individuals from specified VO's having 
an agreed upon FQAN (belonging to a certain group/role) within the VO to 
administer this Gratia instance.</p>
<pre>
service.admin.FQAN.0=/cms/usmcs/Role=GratiaAdmin
service.admin.FQAN.1=/atlas/usatlas/Role=gratia
service.admin.FQAN.d=/fermilab/gratia/Role=admin
</pre>
<p>So for the first entry, any CMS VO member in the <i>/cms/uscms</i> group with a 
role of <i>GratiaAdmin</i> will be authorized to perform administative tasks on 
this instance of Gratia.
<font color="blue">At this time, it is necessary to define a role in the VO VOMS 
to validate against.  This should be changed in the next release to allow just 
a subgroup, if desired.</font></p>

<p>When this option is used, the login process will contact the VOMS 
web service for the VO identified by the top level group in the FQAN. In the 1st
example, it would be <i>cms</i>.
This is where the <i>service.voms.connections</i> property comes it to play.  It identifies the file located in 
<i>TOMCAT_LOCATION/gratia</i> that contains a list of the VOMS web service urls for each
VO.</p>
<p>The actual steps that the user will see are:</p>
<ol>
<li>A selection window will appear listing the VO (top level group) for all the
<i>service.admin.FQAN.x</i> properties you have defined</li>

<li>When the user selects a VO, the login process will retrieve all the possible
FQAN's (groups-roles) for that user 
from the VO's VOMS web services  and present them in another seleection window.
The <i>service.admin.ccnnections</i> will identify the url for that VO's VOMS
web service.</li>

<li>When the user selects the FQAN, the login process will match that against the
set of <i>service.admin.FQAN.x</i> that have been defined</li>
</ol>
<p>Authorization will be granted (all administrative options will be available)
if all of the above conditions are met.</p>

<p>This process uses both the DN and CA from the browsers certificate to access the VOMS web services.</p>



<hr width="75%"/>
<p><b>Maintaining the <i>voms-servers</i> file.</b></p>
<p>This file will be empty on initial installation. 
It only needs to be created (maintained) if the <i>service.admin.FQAN.x</i>
properties are in effect.
</p>
<p>This file (<i>voms-servers</i>) can be manually maintained or you can use a 
script that has been provided that will update this file based on the OSG 
edg-mkgridmap.conf template at:</p>

<pre>
http://software.grid.iu.edu/pacman/tarballs/vo-version/edg-mkgridmap.osg
</pre>

<p>The format for the <i>voms-servers</i> file is a 
simple <i>VO</i>=<i>VOMS_URL</i> format.</p>

<p>The script that has been provided is:</p>
<pre>
TOMCAT_LOCATION/gratia/voms-server.sh
</pre>

<p>This can be run manually or as a cron process. Running it as a cron process 
is the recommended method since this insures any changes in VOMS service urls 
is automatic.</p>

<p><font color="blue">In this release, the script does not create a perfect list 
of voms server urls.  It is close.  You will probably have to tweek it for some VO's.
We hope to resolve this in the very near future.</font></p>

<p>In a VDT installation of Gratia services, then the cron process has 
already been registered for <i>vdt-control</i> use.  However, it has <u>not</u>
been <i>enabled</i>.  This was done as we weren't sure how you would configure the
login process and did not need unnecessary processes running. So to <i>enable</i>
for a VDT installation:</p>
<pre>
source VDT_LOCATION/setup.sh
vdt-control --enable gratia-voms-servers
vdt-control --on     gratia-voms-servers
</pre>

<p>In a non-VDT installation of Gratia services, you will have to set the <i>root</i> cron up 
manually as:</p>
<pre>
42 1 * * * TOMCAT_LOCATION/gratia/voms-server.sh -t TOMCAT_LOCATION >/dev/null 2>&1
</pre>

  


</body>
</html>

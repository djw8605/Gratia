<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link href="docstyle.css" type="text/css" rel="stylesheet">
<title>Gratia - Security howto</title>
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

  <h1 align="center">Security How To</h1>
  <h3>Introduction:</h3>
  <p>Gratia &ldquo;understands&rdquo; 3 levels of security:</p>
  <ol>
    <li>No security at all.</li>
    <li>SSL with 2 way authentication between clients (such as a probe) and gratia, 
	or between two instances of gratia using Tomcat&rsquo;s authentication mechanism.</li>
	<li>(Untested) Gratia running under Apache using Apache&rsquo;s authentication mechanism. 
	Note that this is what a normal vdt install does.</li>
  </ol>
  <p>All of these options (and variations) are controlled by flags in the service-configuration.properties file 
  found in the $CATALINA_HOME/gratia directory.
  <br>The first security level is straight-forward. Gratia does nothing.
  <br>The second level involves gratia working with tomcat. Using this approach requires a number of prerequisites:
  <ul>
   <li>The tomcat environment must have access to both the openssl binaries (which are located somewhere in 
	the globus installation directory) as well as the java program &ldquo;keytool&rdquo;. 
	Both will be used by gratia for certificate handling.</li>
   <li>The JAVA_OPTS options must be set before starting tomcat. A typical example 
   (which we set in the init.d script starting tomcat) would be something like:
   <pre>
   export JAVA_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8004 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dssl.port=8443"
  </pre>
  </li>
    <li>
    Finally, JAVA_OPTS must be unset before stopping tomcat.</li>
</ul>
Note that the  ssl.port property must be equal to the ssl connector defined in  tomcat's server.xml
<h3>Tomcat Settings:</h3>
  Add something similar to your tomcat's server.xml to initiate ssl:
  <pre>
 &lt;!-- Define a SSL HTTP/1.1 Connector on port 8443 --&gt;
   &lt;Connector port=&quot;8443&quot; maxHttpHeaderSize=&quot;8192&quot;
               maxThreads=&quot;150&quot; minSpareThreads=&quot;25&quot; maxSpareThreads=&quot;75&quot;
               enableLookups=&quot;false&quot; disableUploadTimeout=&quot;true&quot; 
               acceptCount=&quot;100&quot; scheme=&quot;https&quot; secure=&quot;true&quot;
               keystoreFile=&quot;/gratia/keystore&quot; keystorePass=&quot;server&quot;
               truststoreFile=&quot;/gratia/truststore&quot; truststorePass=&quot;server&quot;
               clientAuth=&quot;true&quot; sslProtocol=&quot;TLS&quot; /&gt; 
</pre>
  <p>Note that (for linux, at least) the location of the keystore file and the truststore file must be an absolute directory location.</p>

<h3>Final Steps:</h3>
  <p>(And you thought you were finished) &hellip;</p>
  <p>At this point tomcat has been configured with both an open port (8880 in our example) as well as a ssl secure port. 
  Unfortunately, tomcat will still service requests on both ports. 
  So, what you have to do is to force ssl authentication for each of the various services you are concerned with. 
  Specifically, the major service(s) of concern is gratia-servlets and gratia-soap 
  (which basically handle machine to machine communications). 
  To secure this service (as well as any other) you must:</p>
  <ul>
    <li>Goto $CATALINA_HOME/webapps/gratia-servlets/WEB-INF (or service of choice)</li>
  <li>Edit the web.xml file and add the following to the tail end of the file (right before the &lt;/webapp&gt; clause):
  <pre>
&lt;security-constraint&gt;
    &lt;web-resource-collection&gt;
       &lt;web-resource-name&gt;Automatic SSL Forwarding&lt;/web-resource-name&gt;
       &lt;url-pattern&gt;>/*&lt;/url-pattern&gt;
    &lt;/web-resource-collection&gt;
    &lt;user-data-constraint&gt;
       &lt;transport-guarantee&gt;
           CONFIDENTIAL
       &lt;/transport-guarantee&gt;
    &lt;/user-data-constraint&gt;
&lt;/security-constraint&gt;
  </pre>
  </li>
    <li>Restart tomcat</li>
<li>Finally, the easiest way to handle services such as gratia-administration or gratia-reporting is to use the 
normal tomcat userid/password security. </li>
</ul>
  <h3>Service Configuration Settings:</h3>
  Service.security.level
  <blockquote>
  This flag controls the level of security gratia must deal with: <br>
  If set to 0, gratia doesn't do anything. <br>
  If set to 1, gratia works with tomcat to provide two way authentication using tomcat's ssl authentication mechanism.<br> 
  If set to 2 (which is untested), gratia understands that it is running as a worker under Apache and the normal 
  vdt/Apache security mechanisms will be used.
  </blockquote>
  <p>The rest of the options apply when running at security level <strong>1</strong>.</p>
Service.use.selfgenerated.certs
    <blockquote>When set to 1 gratia will generate self signed certs for its use and will provide certs to 
	requesting parties (such as a probe). <br>
	When set to 0 gratia will use doe/vdt certs but can still provide self signed certs to requesting parties.
	</blockquote>

Service.vdt.cert.file/service.vdt.key.file
<blockquote>Absolute path to the vdt/doe certificates and key pem's. </blockquote>

Service.autoregister.pem
<blockquote>When running at security level 1, an outside client (such as a probe) must "register" itself with 
gratia  e.g.  by sending gratia a copy of its public key. 
<br>If service.autoregister.pem = 0, gratia will add the key to the database but not insert it into its truststore. 
The administrator must manually do this via the gratia-administration screen. 
<br>If set to 1, however, the key is added both to the database as well as the truststore (no administration is required).
</blockquote>

Service.open.connection
<blockquote>The open, unsecured url gratia is providing. As an example, http://yourgratiahost:8880</blockquote>

Service.secure.connection
<blockquote>The secure url gratia is providing. As an example, https://yourgratiahost:8843.</blockquote>

</body>
</html>

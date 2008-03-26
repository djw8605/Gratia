<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional //EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link href="stylesheet.css" type="text/css" rel="stylesheet">
<title>Gratia Accounting</title>
</head>
<body>

<a href="http://opensciencegrid.org/" target="_blank"><img src="./images/osg-logo.gif" alt="OSG Logo" width="174" height="81" border="0"></a><br /> <hr>
<%
	// Check if a certificate handler seesion attribute exists to pick up the settings
	net.sf.gratia.vomsSecurity.CertificateHandler certificateHandler = (net.sf.gratia.vomsSecurity.CertificateHandler) session.getAttribute("certificateHandler");
	if (certificateHandler == null)
	{
		certificateHandler = new net.sf.gratia.vomsSecurity.CertificateHandler(request);
		session.setAttribute("certificateHandler", certificateHandler);
	}
	certificateHandler = (net.sf.gratia.vomsSecurity.CertificateHandler) session.getAttribute("certificateHandler");

	String loginLink = certificateHandler.getSecureConnection() + request.getContextPath() + "/gratia-login.jsp";
	String logoutLink  = certificateHandler.getOpenConnection() + request.getContextPath() + "/gratia-logout.jsp";

	String userDN = (String) session.getAttribute("userDN");
	String fqan = (String) session.getAttribute("FQAN");
	boolean login = false;
	if (fqan != null)
	{
		if (fqan.indexOf("NoPrivileges") > -1)
			login = false;
		else
			login = true;
	}

	if (login)
	{
		%>
		<a href="<%=logoutLink %>" target="adminContent" class="menuItem">logout</a><br /> 
		<%
	}
	else
	{
		%>
		<a href="<%=loginLink %>" target="adminContent" class="menuItem">login</a><br />
		<%
	}

	if (login)
	{
		%>
		<hr><label class="menuGroup">Maintenance</label> <br />
		<a href="./site.html" target="adminContent" class="menuItem">Site Table</a><br />
		<a href="./vo.html" target="adminContent" class="menuItem">VO Management</a><br />
		<font size="-2">Probes: </font><a href="./probetable.html?activeFilter=active" target="adminContent" class="menuItem">active</a>,
		<a href="./probetable.html?activeFilter=inactive" target="adminContent" class="menuItem">inactive</a><font size="-2"> or </font>
		<a href="./probetable.html" target="adminContent" class="menuItem">all</a><br />

		<a href="./cpuinfo.html" target="adminContent" class="menuItem">CPU Info</a><br />
		<a href="./roles.html" target="adminContent" class="menuItem">Roles</a><br />

		<hr><label class="menuGroup">Authentication</label> <br />
		<a href="./securitytable.html" target="adminContent" class="menuItem">Certificates</a><br />

		<hr><label class="menuGroup">Replication</label> <br />
		<a href="./replicationtable.html" target="adminContent" class="menuItem">Job Usage Replication</a><br />
		<a href="./metricreplicationtable.html" target="adminContent" class="menuItem">Metric Replication</a><br />
		<%
	}
	%>

	<hr><label class="menuGroup">System</label> <br />
	<font size="-2">System Status: </font><a href="./status.html?wantDetails=0" target="adminContent" class="menuItem">normal</a>
	<font size="-2"> or </font><a href="./status.html?wantDetails=1" target="adminContent" class="menuItem">detailed</a><br />
	<%
	if (login)
	{
		%>
		<a href="./systemadministration.html" target="adminContent" class="menuItem">Administration</a><br />
		<%
	}
	%>
	<hr><label class="menuGroup">Documentation</label> <br />
	<a href="./installation-howto.html" target="adminContent" class="menuItem">Installation</a><br />
	<a href="./security-howto.html" target="adminContent" class="menuItem">Security</a><br />
	<a href="./replication-howto.html" target="adminContent" class="menuItem">Replication</a><br />
	<a href="./service-configuration-settings.html" target="adminContent" class="menuItem">Gratia Service Settings</a><br />

	<%
	String displayLink = (String) session.getAttribute("displayLink");
	if (displayLink != null)
	{
		session.removeAttribute("displayLink");
		%>
		<script type="text/javascript">
			parent.adminContent.location = "./pleaseWait.html";
			parent.adminContent.location = "<%=displayLink %>";
		</script>
	<%
	}
%>

</body>
</html>

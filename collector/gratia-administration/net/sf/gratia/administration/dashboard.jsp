<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="net.sf.gratia.reporting.*"
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional //EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link href="stylesheet.css" type="text/css" rel="stylesheet">
<base target="paramFrame">
<title>Gratia Accounting</title>

</head>
<body>

	<a href="http://opensciencegrid.org/" target="_blank"><img src="./images/osg-logo.gif" alt="OSG Logo" width="174" height="81" border="0"></a>
	<%
	String username = (String) session.getAttribute("username");
	String userRole = "";

	if (username != null)
		userRole = (String) session.getAttribute(username);

	if (userRole.indexOf("admin") > -1) {
		%>
		<hr><label class="menuGroup">Maintenance</label>
		<br /><a href="./site.html" target="adminContent" class="menuItem">Site Table</a>
		<br /><a href="./vo.html" target="adminContent" class="menuItem">VO Management</a>
		<br /><a href="./vonamecorrection.html" target="adminContent" class="menuItem">VOName correction</a>
		<br /><a href="./probetable.html" target="adminContent" class="menuItem">Probe Table</a>

		<br /><a href="./vonamecorrection.html" target="adminContent" class="menuItem">VOName correction</a>

		<br /><label class="menuText">Probes: </label>
			<a href="./probetable.html?activeFilter=active" target="adminContent" class="menuItem">active</a><label class="menuText">, </label>
			<a href="./probetable.html?activeFilter=inactive" target="adminContent" class="menuItem">inactive</a>
			<label class="menuText">or </label>
			<a href="./probetable.html" target="iframe" class="menuItem">all</a>

		<br /><a href="./cpuinfo.html" target="adminContent" class="menuItem">CPU Info</a>
		<br /><a href="./roles.html" target="adminContent" class="menuItem">Roles</a>

		<hr><label class="menuGroup">Authentication</label>
		<br /><a href="./securitytable.html" target="adminContent" class="menuItem">Certificates</a>

		<hr><label class="menuGroup">Replication</label>
		<br /><a href="./replicationtable.html" target="adminContent" class="menuItem">Job Usage Replication</a>
		<br /><a href="./metricreplicationtable.html" target="adminContent" class="menuItem">Metric Replication</a>
		<%
	}
	%>
	<hr><label class="menuGroup">System</label>
	<br /><label class="menuText">System Status: </label>
		<a href="./status.html?wantDetails=0" target="adminContent" class="menuItem">normal</a>
		<label class="menuText">or </label>
		<a href="./status.html?wantDetails=1" target="adminContent" class="menuItem">detailed</a>

	<%
	if (userRole.indexOf("admin") > -1) {
		%>
		<br /><a href="./systemadministration.html" target="adminContent" class="menuItem">Administration</a>
		<%
	}
	%>
	<hr><label class="menuGroup">Documentation</label>
	<br /><a href="./installation-howto.html" target="adminContent" class="menuItem">Installation</a>
	<br /><a href="./security-howto.html" target="adminContent" class="menuItem">Security</a>
	<br /><a href="./replication-howto.html" target="adminContent" class="menuItem">Replication</a>
	<br /><a href="./service-configuration-settings.html" target="adminContent" class="menuItem">Gratia Service Settings</a>

	<hr><label class="menuGroup">Commands</label>
	<br /><a href="./admin-login.jsp" target="adminContent" class="menuItem">login</a>
	<br /><a href="./admin-logout.jsp" target="adminContent" class="menuItem">logout</a>

</body>
</html>

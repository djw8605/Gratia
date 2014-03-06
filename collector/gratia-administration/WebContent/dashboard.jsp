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
	String dbConnection1 = certificateHandler.getDBconnection();
	dbConnection1 = dbConnection1.substring(dbConnection1.indexOf(":")+1);
	String dbConnection = dbConnection1.substring(dbConnection1.lastIndexOf("/")+1);
	
	String version = net.sf.gratia.services.Versions.GetPackageVersionString();
	String fqan = (String) session.getAttribute("FQAN");
	boolean login = false;
	String menuClass = "menuItem2";
	if (fqan != null)
	{
		login = true;
		menuClass = "menuItem";
		dbConnection = dbConnection1;
	}

	%>
	<a href="<%=logoutLink %>" target="adminContent" class="<%=menuClass%>">logout</a><br />
	<hr><label class="menuGroup">Maintenance</label> <br />
	<a href="./site.html" target="adminContent" class="<%=menuClass%>">Site Table</a><br />
 	<a href="./projectnamecorrection.html" target="adminContent" class="<%=menuClass%>">ProjectName correction</a><br />
	<a href="./vo.html" target="adminContent" class="<%=menuClass%>">VO Management</a><br />
	<a href="./vonamecorrection.html" target="adminContent" class="<%=menuClass%>">VOName correction</a><br />
	<font size="-2">Probes: </font><a href="./probetable.html?activeFilter=active" target="adminContent" class="<%=menuClass%>">active</a>,
	<a href="./probetable.html?activeFilter=inactive" target="adminContent" class="<%=menuClass%>">inactive</a><font size="-2"> or </font>
	<a href="./probetable.html" target="adminContent" class="<%=menuClass%>">all</a><br />
	<a href="./cpuinfo.html" target="adminContent" class="<%=menuClass%>">CPU Info</a><br />
	<a href="./roles.html" target="adminContent" class="<%=menuClass%>">Roles</a><br />

	<hr><label class="menuGroup">Authentication</label> <br />
	<a href="./certificatetable.html" target="adminContent" class="<%=menuClass%>">Certificates</a><br />
	<a href="./connectiontable.html" target="adminContent" class="<%=menuClass%>">Connections</a><br />

	<hr><label class="menuGroup">Replication</label> <br />
	<a href="./replicationtablesummary.html" target="adminContent" class="menuItem">Replication Summary</a><br />
	<a href="./jobusagerecordreplicationtable.html" target="adminContent" class="<%=menuClass%>">Job Usage Replication</a><br />
	<a href="./metricrecordreplicationtable.html" target="adminContent" class="<%=menuClass%>">Metric Replication</a><br />
	<a href="./probedetailsreplicationtable.html" target="adminContent" class="<%=menuClass%>">ProbeDetails Replication</a><br />
	<a href="./computeelementreplicationtable.html" target="adminContent" class="<%=menuClass%>">CE Description Replication</a><br />
	<a href="./storageelementreplicationtable.html" target="adminContent" class="<%=menuClass%>">SE Description Replication</a><br />
	<a href="./subclusterreplicationtable.html" target="adminContent" class="<%=menuClass%>">Sub-cluster Description Replication</a><br />
	<a href="./computeelementrecordreplicationtable.html" target="adminContent" class="<%=menuClass%>">CE Record Replication</a><br />
	<a href="./storageelementrecordreplicationtable.html" target="adminContent" class="<%=menuClass%>">SE Record Replication</a><br />

	<hr><label class="menuGroup">System</label> <br />
	<font size="-2">System Status: </font><a href="./status.html?wantDetails=0" target="adminContent" class="menuItem">normal</a>
	<font size="-2"> or </font><a href="./status.html?wantDetails=1" target="adminContent" class="menuItem">detailed</a>
	<font size="-2"> or </font><a href="./monitor-status.html" target="adminContent" class="menuItem">minimal</a><br />

	<font size="-2">System Performance: </font><a href="./performance-rate.html" target="adminContent" class="menuItem">rates</a>
	<font size="-2"> or </font><a href="./backlog.html" target="adminContent" class="menuItem">backlog details</a><br />

	<font size="-2">Subsystem Status: </font>
    <a href="./collector-status.html?out=html" target="adminContent" class="menuItem">normal</a>
    <font size="-2"> or </font>	
    <a href="./collector-status.html?out=txt" target="adminContent" class="menuItem">text</a><br />

	<a href="./systemadministration.html" target="adminContent" class="<%=menuClass%>">Administration</a><br />

	<hr><label class="menuGroup">Documentation</label> <br />
	<a href="./adminlogin-howto.jsp" target="adminContent" class="menuItem">Administration Login</a><br />
	<a href="./security-howto.jsp" target="adminContent" class="menuItem">Security</a><br />
	<a href="./replication-howto.jsp" target="adminContent" class="menuItem">Replication</a><br />
	<a href="./service-configuration-settings.jsp" target="adminContent" class="menuItem">Gratia Service Settings</a><br />

	<hr>
	<a href="https://twiki.grid.iu.edu/twiki/bin/view/Accounting/ContactUs" target="_blank" class="menuItem">Contact us</a>

	<p class = "menuVersion">Gratia Services Version: <%= version %></p>
	<p class="menuVersion">DB Connection: <%=dbConnection %></p>

	<%
	String displayLink = (String) session.getAttribute("displayLink");
	if (displayLink == null)
		displayLink = "./status.html?wantDetails=0";
	%>
	<script type="text/javascript">
		parent.adminContent.location = "<%=displayLink %>";
	</script>

</body>
</html>

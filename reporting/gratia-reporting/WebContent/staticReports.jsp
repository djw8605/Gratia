<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="java.io.*" 
    import="net.sf.gratia.reporting.*"
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
   <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

   <base target="reportFrame">
   <link href="stylesheet.css" type="text/css" rel="stylesheet">
   <title>Gratia Accounting - Static Reports</title>
</head>

<body>
	<jsp:include page="common.jsp" />
        
	<div class="osgcolor">&nbsp;&nbsp;&nbsp;&nbsp;Gratia Reporting&nbsp;&nbsp;&nbsp;&nbsp;</div> <br /> 
	<% 
	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");
	String staticFolderPath = reportingConfiguration.getStaticFolderPath();
	String staticFolder = reportingConfiguration.getStaticFolder();
	if (staticFolder.substring(staticFolder.length()-1, staticFolder.length()) != "/")
		staticFolder = staticFolder + "/";
	if (staticFolder.substring(0, 1) != "/")
		staticFolder = "/" + staticFolder;

	File f = new File(staticFolderPath);
	String [] fileNames = f.list();
	File [] fileObjects = f.listFiles();
	%>
	<ul>
	<%
	for (int i = 0; i < fileObjects.length; i++) 
	{
		if(!fileObjects[i].isDirectory())
		{
		%><li><a class = "reportItem" target = "reportFrame" href="<%= staticFolder+fileNames[i] %>"><%= fileNames[i] %></a></li>
		<%
		}
	}
	%>
	</ul>
</body>
</html>
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
   <title>Gratia Accounting</title>
</head>

<body>
	<jsp:include page="common.jsp" />

	
	<%
	String title = request.getParameter("ReportTitle");
	if (title == null)
		title = "Static Reports";
	%><div class="reportTitle"><%= title %></div><br><%
	String linkURL = null;
	String linkNAME = null;
	String linkREPORT = null;
	String groupName = null;
	StaticReportConfig staticReportConfig = (StaticReportConfig)session.getAttribute("staticReportConfig");
	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");
	String staticFolderPath = reportingConfiguration.getStaticFolderPath();
	String staticFolder = reportingConfiguration.getStaticFolder();

	if (staticFolder.charAt(staticFolder.length()-1) != '/')
		staticFolder = staticFolder + "/";
	if (staticFolder.charAt(0) != '/')
		staticFolder = "/" + staticFolder;

	// Construct the csv path
	String csvFolder = staticFolder.substring(0, staticFolder.length() - 1) + "_csv/";
	String csvFolderPath = staticFolderPath.substring(0, staticFolderPath.length() - 1) + "_csv/";
	
		
	int pdf = -1;
	int csv = -1;
	File [] pdfObjects = null;
	File [] csvObjects = null;
	
	File f = new File(staticFolderPath);
	File c = new File(csvFolderPath);

	if (!f.exists() && !c.exists())
	{
		%><p class = "reportItem">There are no available reports</p> <%
		return;
	}

// At leat one directory exist


	if (staticReportConfig.getStatReportGroups() == null)
	{
		%><p class = "reportItem">No Static reports have been defined for gratia accounting</p> <%
		return;
	}

// There is a list of static reports

	if (f.exists())
	{
		pdfObjects = f.listFiles();	// Get all files in the directory
		pdf = pdfObjects.length;
	}
	if (c.exists())
	{
		csvObjects = c.listFiles();	// Get all files in the directory
		csv = csvObjects.length;
	}
	
	if ((pdf <= 0) && (csv <= 0))
	{
		%> <p class = "reportItem" >No Static reports are available</p> <%
		return;
	}

// At this point we have reports

	%> <ul> <%
	for(int i=0; i < staticReportConfig.getStatReportGroups().size(); i++)
	{
		StaticReportGroup statGroup = (StaticReportGroup)staticReportConfig.getStatReportGroups().get(i);
		groupName = statGroup.getGroup();
		for(int z=0; z < statGroup.getStatItems().size(); z++)
		{
			StaticReportItem statItem = (StaticReportItem)statGroup.getStatItems().get(z);

			linkNAME = statItem.getName();
			linkURL  = statItem.getLink();
			linkREPORT  = statItem.getReport();

			linkURL = linkURL.replace(" ", "%20");
			linkURL = linkURL.replace("&amp;", "&");
			linkURL = linkURL.replace("&", "&amp;");
			linkURL = linkURL.replace(">", "%3e");
			linkURL = linkURL.replace("<", "%3c");
			linkURL = linkURL.replace("\\", "%5c");

			if (pdf > 0)
			{
			  for (int j = 0; j < pdf; j++)
			  {
				if(!pdfObjects[j].isDirectory())
				{
					String fileName = pdfObjects[j].getName();
					if (fileName.startsWith(linkREPORT) && (fileName.endsWith(".pdf")))
					{
						%><li><a class = "reportItem" target = "reportFrame" href="<%= staticFolder+fileName %>"><%= linkNAME %> (pdf)</a></li>
						<%
					}
				}
				
			  }
			}

			if (csv > 0)
			{
			  for (int j = 0; j < csv; j++)
			  {
				if(!csvObjects[j].isDirectory())
				{
					String fileName = csvObjects[j].getName();
					if (fileName.startsWith(linkREPORT) && (fileName.endsWith(".csv")))
					{
						%><li onClick="window.open('downloadFile.jsp?csvFile=<%= csvFolderPath+fileName %>', 'Gratia');"><span class = "txtasLink"><%= linkNAME %> (csv)</span></li>
						<%
					}
				}
			  }
			}
		}
	}
	%></ul>

</body>
</html>

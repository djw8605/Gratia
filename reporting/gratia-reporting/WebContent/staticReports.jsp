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

	<div class="osgcolor">&nbsp;&nbsp;&nbsp;&nbsp;Gratia Reporting&nbsp;&nbsp;&nbsp;&nbsp;</div> <br />
	<%
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


	File f = new File(staticFolderPath);
	if (!f.exists())
	{
		%><p class = "reportItem">There are no available reports</p> <%
	}
	else
	{
		if (staticReportConfig.getStatReportGroups() == null)
		{
			%><p class = "reportItem">No Static reports have been defined for gratia accounting</p> <%
		}
		else
		{
			File [] fileObjects = f.listFiles();	// Get all files in the directory
			if (fileObjects.length == 0)
			{
				%> <p class = "reportItem" >No Static reports are available</p> <%
			}
			else
			{

				for(int i=0; i < staticReportConfig.getStatReportGroups().size(); i++)
				{
					StaticReportGroup statGroup = (StaticReportGroup)staticReportConfig.getStatReportGroups().get(i);
					groupName = statGroup.getGroup();
					%> <ul> <%
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


						for (int j = 0; j < fileObjects.length; j++)
						{
							if(!fileObjects[j].isDirectory())
							{
								String fileName = fileObjects[j].getName();
								if (fileName.startsWith(linkREPORT) && (fileName.endsWith(".pdf") || fileName.endsWith(".csv")))
								{
									if (fileName.endsWith(".pdf"))
									{
										%><li><a class = "reportItem" target = "reportFrame" href="<%= staticFolder+fileName %>"><%= linkNAME %> (pdf)</a></li>
										<%
									}
									else
									{
										%><li><a class = "reportItem" target = "reportFrame" href="<%= staticFolder+fileName %>" onClick="ww=window.open('downloadFile.jsp?csvFile=<%= fileName %>', 'Gratia', 'width=10,height=10'); ww.close('Gratia');"><%= linkNAME %> (csv)</a></li>
										<%
									}
								}
							}
						}
					}
				}
				%></ul><%
			}
		}
	}
%>
</body>
</html>
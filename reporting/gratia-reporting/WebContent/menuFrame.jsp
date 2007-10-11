<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="net.sf.gratia.reporting.*"%>    
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<LINK href="stylesheet.css" type="text/css" rel="stylesheet">
<title>Gratia Accounting</title>
</head>
<body>
<jsp:include page="common.jsp" />
<%
	// Get the reporting configuration setting
	UserConfiguration userConfiguration = (UserConfiguration)session.getAttribute("userConfiguration"); 
	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");
	String reportsFolder = reportingConfiguration.getReportsFolder();

%>
	<%
	String linkURL = null;
	String linkNAME = null;
	for(int i=0; i < userConfiguration.getMenuGroups().size(); i++)
	{
		MenuGroup menuGroup = (MenuGroup)userConfiguration.getMenuGroups().get(i);
		if(i>0){
	%>	<hr><label class=menuGroup><%=menuGroup.getName() %></label> <br /><%
		}
		for(int z=0; z < menuGroup.getMenuItems().size(); z++)
		{
			MenuItem menuItem = (MenuItem)menuGroup.getMenuItems().get(z);
						
// Add the "name" of the report (title=) on the created link for display purposes
// Make sure that if this is the first argument to include "?"
// Eliminate the starting "-" and any blank spaces in the string

			linkNAME = menuItem.getName();
			if (menuItem.getLink().indexOf("?") > -1) 
			{
			   linkURL = menuItem.getLink() + "&reportTitle=" + linkNAME.replaceFirst("-", "").trim();
			}else
			{
			   linkURL = menuItem.getLink() + "?reportTitle=" + linkNAME.replaceFirst("-", "").trim();
			}
			
			linkURL = linkURL.replace(" ", "%20");
			linkURL = linkURL.replace("&", "&amp;");
			%> 
			<a class=menuItem href="<%=linkURL %>" target="central"><%=linkNAME %></a> <br />
			<%
		}
	}
	%>	
	 <div class=menuGroup><hr>Commands</div>
	 <a class=menuItem href="logout.jsp">Logout</a><br />
</body>
</html>

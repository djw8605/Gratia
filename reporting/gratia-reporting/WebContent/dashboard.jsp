<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="net.sf.gratia.reporting.*"%>
   
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<LINK href="stylesheet.css" type="text/css" rel="stylesheet">
<title>Dashboard</title>
</head>
<body>
	<jsp:include page="common.jsp" />

<%	
	String testingagain = request.getQueryString();
	String myReportTitle = request.getParameter("__title");
	if (myReportTitle != null)
   	{
%>
<div align="left" class="reportTitle"><%=myReportTitle%></div><br>
<%
	}

	UserConfiguration userConfiguration =  (UserConfiguration)session.getAttribute("userConfiguration"); 
%>	
	
	<table style="height: 100%" width="100%">
	<%
	// Figure out the row that has the most columns.  This is required to calculate the colSpan for each cell.
	int maxColumns = 1;
	for(int i=0; i < userConfiguration.getDashboardRows().size(); i++)
	{
		DashboardRow row = (DashboardRow)userConfiguration.getDashboardRows().get(i);
		if(row.getDashboardItems().size() > maxColumns)
			maxColumns = row.getDashboardItems().size();
	}
		
	for(int i=0; i < userConfiguration.getDashboardRows().size(); i++)
	{
		DashboardRow row = (DashboardRow)userConfiguration.getDashboardRows().get(i);		
		
		int colSpan = maxColumns - (row.getDashboardItems().size() -1);		
	%>
		<tr style="height:600">
	<%
		for(int z=0; z < row.getDashboardItems().size(); z++)
		{
			DashboardItem dashboardItem = (DashboardItem)row.getDashboardItems().get(z);
			int width = 800;
			if(colSpan > 1)
				width = width * (colSpan + 1);			
	%>
			<td colspan=<%=colSpan %> valign=top>
<!-- Find if the browser is either Konquer or Safari. If so then make the width and height of the iframe fixed -->

<%@ page import="javax.servlet.*" %>

<%
String browserType = request.getHeader("User-Agent");
String browser = new String("");
String version = new String("");
browserType = browserType.toLowerCase();
if(browserType != null ){
        if ((browserType.indexOf("safari") != -1) || (browserType.indexOf("konqueror") != -1)) {


%>                               <iframe name="viewDashboard" frameborder=0 scrolling="auto" width="790" height="600" src="<%=dashboardItem.getLink() %>"></iframe>
<%
        }
        else {
%>
                               <iframe name="viewDashboard" frameborder=0 scrolling="auto" width="100%" height="100%" src="<%=dashboardItem.getLink() %>"></iframe>
<%
        }
}

 %>

			</td>
	<%
		}
	%>
		</tr>
	<%
		
	}
	%>		
	</table>		
</body>
</html>

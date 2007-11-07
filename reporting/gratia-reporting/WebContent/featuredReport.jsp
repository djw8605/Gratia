<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="net.sf.gratia.reporting.*"%>
   
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link href="stylesheet.css" type="text/css" rel="stylesheet">
<base target="reportFrame">
<title>Gratia Accounting</title>
</head>
<body>
	<jsp:include page="common.jsp" />

<%
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
						
			String link = request.getRequestURL().toString();			
			link = "/frameset?__report=" + dashboardItem.getLink() + "&amp;__title";
			int width = 800;
			if(colSpan > 1)
				width = width * (colSpan + 1);			
	%>
			<td colspan="<%=colSpan %>" valign="top">
			
			<jsp:forward  page="<%=link %>" /> 
			
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

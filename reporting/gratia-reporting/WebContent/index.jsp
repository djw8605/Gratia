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
	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");
	String reportsFolder = reportingConfiguration.getReportsFolder();
	
	UserConfiguration userConfiguration = (UserConfiguration)session.getAttribute("userConfiguration"); 
	String link = request.getParameter("link");
	String param1 = null;
	if(link == null)
		link="featuredReport.jsp";
	else {		
		StringBuffer sb = new StringBuffer(link.length());
		char c;
                boolean sqlmode = false;
		for(int i = 0; i < link.length(); ++i) {
			c = link.charAt(i);
                        if (c=='=' && i>3 && link.substring(i-3,i).equals("sql")) {
                           sqlmode = true;
                        }
                        if (c==';' && sqlmode) {
                           sqlmode = false;
                        }
                        if (c==',') { 
                           if (sqlmode) sb.append(',');
                           else sb.append('&');
                        } else {
                           sb.append(c);
                        }

		}
		link = sb.toString();
	}
%>
	
	<table width=100% style="height: 95%">
		<tr>
			<td valign="top" width=200>
				<table class=menu>
			<tr><td><a href="http://opensciencegrid.org/"><img src="./images/osg-logo.gif" alt="OSG Logo" width="174" height="81" border="0"></a><hr></td></tr>
				<%
				String linkURL = null;
				String linkNAME = null;
				for(int i=0; i < userConfiguration.getMenuGroups().size(); i++)
				{
					MenuGroup menuGroup = (MenuGroup)userConfiguration.getMenuGroups().get(i);
                                        if(i>0){

				%>	
					<tr>
					<td><hr><label class=menuGroup><%=menuGroup.getName() %></label><td>
					</tr>
				<%
					}
					for(int z=0; z < menuGroup.getMenuItems().size(); z++)
					{
						MenuItem menuItem = (MenuItem)menuGroup.getMenuItems().get(z);
						int rowHeight = ((menuItem.getName().length()/30) + 1) * 20;
						
// Add the "name" of the report (title=) on the created link for display purposes
// Make sure that if this is the first argument to include "?"
// Eliminate the starting "-" and any blank spaces in the string

						linkNAME = menuItem.getName();
						if (menuItem.getLink().indexOf("?") > -1) 
						{
						   linkURL = menuItem.getLink() + ",reportTitle=" + linkNAME.replaceFirst("-", "").trim();
						}else
						{
						   linkURL = menuItem.getLink() + "?reportTitle=" + linkNAME.replaceFirst("-", "").trim();
						}
						linkURL = linkURL.replace(" ", "%20");
						linkURL = linkURL.replace("&", "&amp;");
				%>
					<tr >
     					<td>
     						<a class=menuItem href="index.jsp?link=<%=linkURL %>"><%=linkNAME %></a>
     					</td>
                    </tr>
				<%
					}
				}
				%>															
					<tr>
					<td class=menuGroup><hr>Commands</td>
					</tr>
					<tr>
						<td><a class=menuItem href="logout.jsp">Logout</a><br /></td>
					</tr>
				</table>
			</td>
			<td width=1 bgcolor="black">
			</td>
			<td valign="top">
			<table width=100% style="height: 90%">
			<tr> 
			<td valign="top">
<!--
			   <div align="center" class="osgcolor">&nbsp;&nbsp;&nbsp;&nbsp;Gratia Reporting&nbsp;&nbsp;&nbsp;&nbsp;</div>
			   <br> 
-->

<!-- Find if the browser is either Konquer or Safari. If so then make the width and height of the iframe fixed -->
<%@ page import="javax.servlet.*" %>

<%
String browserType = request.getHeader("User-Agent");
String browser = new String("");
String version = new String("");
browserType = browserType.toLowerCase();
if(browserType != null ){
	if ((browserType.indexOf("safari") != -1) || (browserType.indexOf("konqueror") != -1)) {


%>
                <iframe name="viewPanel"  width="800" height="700" SCROLLING="auto" frameborder=0 src="<%=link %>" > </iframe>
<%
	}
	else {
%>
                <iframe name="viewPanel"  width="100%" height="100%" SCROLLING="auto" frameborder=0 src="<%=link %>" > </iframe>
<%
	}
   }
 %>
			<!-- jsp:include page="<%=link %>" / -->
			</td> </tr> </table>
			</td>
		</tr>
	</table>
</body>
</html>

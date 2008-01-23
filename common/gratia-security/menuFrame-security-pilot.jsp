<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="net.sf.gratia.reporting.*"
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional //EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<LINK href="stylesheet.css" type="text/css" rel="stylesheet">
<base target="paramFrame">
<title>Gratia Accounting</title>

<script type="text/javascript">

function clearReportFrame() {
	parent.reportFrame.location = "about:blank";
}

function clearParamFrame() {
	parent.paramFrame.location = "about:blank";
}

</script>
</head>
<body>
<jsp:include page="common.jsp" />
<%
	// Get the reporting configuration setting
	UserConfiguration userConfiguration = (UserConfiguration)session.getAttribute("userConfiguration");
	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");
	String reportsFolder = reportingConfiguration.getReportsFolder();
	String reportingVersion = reportingConfiguration.getReportingVersion();

%>
<a href="http://opensciencegrid.org/"><img src="./images/osg-logo.gif" alt="OSG Logo" width="174" height="81" border="0"></a><hr>
	<%
	String linkURL = null;
	String linkNAME = null;
	String groupName = null;
	String targetFrame = "paramFrame";
	String clearFrame = "clearReportFrame();";

	String sessionid = session.getId();
	String sessVoStat = "";

	Boolean showMenuItems = true;

	 /*%>  menuFrame, Session id  =  <%= sessionid %> <br><%  */

	System.out.println("==MMMM== menuFrame,checking on session attributes ");
// The enumeration does not work!
        /* Enumeration e = session.getAttributeNames();
        while (e.hasMoreElements()) {
            String name = (String)e.nextElement();
            String value = session.getAttribute(name).toString();
            System.out.println(name + " = " + value);
        } */
        String[] sessNames = session.getValueNames();
	for(int ie=0; ie< sessNames.length; ie++) {
	   System.out.println("==MMMM== name= " + sessNames[ie]);
	   String sname = sessNames[ie];
	   if (sname.toUpperCase().equals("VOCONNECTSTATUS") ){
	      sessVoStat = (String)session.getAttribute(sname);
	      System.out.println("==### getVOindex== name= " + sname + " val= " + sessVoStat);
	   }

        }

	for(int i=0; i < userConfiguration.getMenuGroups().size(); i++)
	{
		MenuGroup menuGroup = (MenuGroup)userConfiguration.getMenuGroups().get(i);
		if(i>0){
			groupName = menuGroup.getName();
			//System.out.println("==MMMM== menuFrame, groupName = " + groupName);

// Check on special character to see if we can show or not
		String conStat="";
		showMenuItems=true;
		if (groupName.startsWith("*") ) {
		        System.out.println("==MMMM== found * ");
	   		if (sessVoStat.toUpperCase().equals("CONNECTED") ){
			   showMenuItems=true;
			   %><hr><label class="menuGroup"><%= groupName %></label> <br /><%

			} else {
	                %>	<hr> **** <br /><%
	                /* %>	<hr>Grp Skipped, VO con = <%= sessVoStat %> <br /><% */
			   showMenuItems=false;
			}
		} else {
	%>	<hr><label class="menuGroup"><%= groupName %></label> <br /><%
		}
		if (showMenuItems) {
		  for(int z=0; z < menuGroup.getMenuItems().size(); z++)
		  {
			MenuItem menuItem = (MenuItem)menuGroup.getMenuItems().get(z);

// Add the "name" of the report (title=) on the created link for display purposes
// Make sure that if this is the first argument to include "?"
// Eliminate the starting "-" and any blank spaces in the string

			linkNAME = menuItem.getName();
			System.out.println("==MMMM== menuFrame, linkNAME = " + linkNAME);
			clearFrame = "clearReportFrame();";
			if (linkNAME.indexOf("Featured") > -1)
			{
				clearFrame = "clearParamFrame();";
				targetFrame = "reportFrame";
			}
			else
			{
				clearFrame = "clearReportFrame();";
				targetFrame = "paramFrame";
			}

			if (menuItem.getLink().indexOf("?") > -1)
			{
			   linkURL = menuItem.getLink() + "&ReportTitle=" + linkNAME.replaceFirst("-", "").trim();
			}else
			{
			   linkURL = menuItem.getLink() + "?ReportTitle=" + linkNAME.replaceFirst("-", "").trim();
			}

			linkURL = linkURL.replace(" ", "%20");
			linkURL = linkURL.replace("&amp;", "&");
			linkURL = linkURL.replace("&", "&amp;");
			linkURL = linkURL.replace(">", "%3e");
			linkURL = linkURL.replace("<", "%3c");
			%>
			<a class="menuItem" href="<%= linkURL %>" target="<%= targetFrame %>" onclick="<%= clearFrame %>"><%= linkNAME %></a> <br />
			<%
		  }  // loop on items
		}    // flag on show items

	  }
	}	// End of loop on menu groups read from file.
	%>
	<hr><label class="menuGroup">Static Reports</label> <br />
	 <a class = "menuItem" href="staticReports.jsp" onclick="clearReportFrame();">Static Reports</a><br />

	 <hr><label class = "menuGroup">Commands</label> <br />
	 <a class = "menuItem" href="logout.jsp" onclick="clearReportFrame();">Logout</a><br />
	 <hr><a target=_blank class="contact" href="https://twiki.grid.iu.edu/twiki/bin/view/Accounting/ContactUs">Contact us</a><br />
	 <p class = "menuVersion">Gratia Reporting Version: <%= reportingVersion %></p>
</body>
</html>

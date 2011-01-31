<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"
	import="java.util.Date"
	import="java.text.SimpleDateFormat"
	import="java.io.*"
	import="net.sf.gratia.reporting.*"
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link href="stylesheet.css" type="text/css" rel="stylesheet">
<base target="reportFrame">
<title>Gratia Accounting</title>

<script type="text/javascript">

var currentFramePath = findPath(self);


function findPath(currentFrame) {
	var path = "";
	while (currentFrame != top) {
		path = "." + currentFrame.name+path;
		currentFrame = currentFrame.parent;
	}
	return "top" + path;
}

function displayReport(link) {
	if (currentFramePath != 'top')
		parent.reportFrame.location = link;
	else
		parent.location = link;
}

</script>

</head>

<body>

<jsp:include page="common.jsp" />

<%
	ReportingConfiguration reportingConfig = (ReportingConfiguration)session.getAttribute("reportingConfiguration");

		// Add a time stamp to the log file

		String timeStampFile = System.getProperty("catalina.home") + "/gratia-logs/gratiaReportingLog.csv";
		String timeStampFolder = System.getProperty("catalina.home") + "/gratia-logs/";

		try
		{
			if (reportingConfig.getLogging())
			{
				File checkFolder = new java.io.File(timeStampFolder);
				if (!checkFolder.exists())
				{
					checkFolder.mkdirs();
				}

				BufferedWriter outLog = new BufferedWriter(new FileWriter(timeStampFile, true));
				checkFolder = null;
				long endM = System.currentTimeMillis();
				outLog.write("InJSP = ," + endM);
				outLog.flush();
				outLog.close();
			}
			else
			{
				File checkFile = new java.io.File(timeStampFile);
				if (checkFile.exists())
				{
					File dest = new java.io.File (timeStampFile + System.currentTimeMillis());
					checkFile.renameTo(dest);
				}
			}
		}
		catch  (Exception e) {
			//e.printStackTrace();
	}

	String params = request.getQueryString() + "&amp;__title";

	String inStart = request.getParameter("StartDate");
	String inEnd = request.getParameter("EndDate");
	String interval = request.getParameter("IntervalDays");
	if (interval == null || interval == "")
		interval = "7";
	long inInterval = new Long(interval);

	String initUrl = request.getRequestURL().toString();
	initUrl=initUrl.substring(0, initUrl.lastIndexOf("/"));

	String link = initUrl + "/frameset?"; // "/run?"; //"/frameset?";

	String inFormat = request.getParameter("__format");
	boolean staticReport = false;
	if (inFormat != null && inFormat.indexOf("html") == -1)
	{
		link = "/run?__fittopage=true&";
		staticReport = true;
	}

	// Get current date (End date) and a week ago (Start Date)
	Date now = new Date();
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
	String End = format.format(now);
	now = new Date(now.getTime() - (inInterval * 24 * 60 * 60 * 1000));
	String Start = format.format(now);

	int i1 = -1;
	int i2 = -1;
	String subString = "";

	// If Start/End Date are not present add the parameters with correct values
	if (inStart == null)
	{
		params += "&StartDate=" + Start;

		if (inEnd == null)
		{
			params += "&EndDate=" + End;
		}
		else
		{
		//Replace EndDate from start of string "EndDate" to the first "&" in case the "=" is present
			i1 = params.indexOf("EndDate");
			i2 = params.substring(i1).indexOf("&");
			if ( i2 == -1)
				i2 = params.length();	// last parameter
			else
				i2 += i1;

			subString = params.substring(i1, i2);
			params = params.replace(subString, "EndDate=" + End);
		}
	}
	else if (inStart == "")
	{
		//Replace StartDate from start of string "StartDate" to the first "&" in case the "=" is present
		i1 = params.indexOf("StartDate");
		i2 = params.substring(i1).indexOf("&");
		if ( i2 == -1)
			i2 = params.length();	// last parameter
		else
			i2 += i1;

		subString = params.substring(i1, i2);
		params = params.replace(subString, "StartDate=" + Start);

		if (inEnd == null)
		{
			params += "&EndDate=" + End;
		}
		else
		{
		//Replace EndDate from start of string "EndDate" to the first "&" in case the "=" is present
			i1 = params.indexOf("EndDate");
			i2 = params.substring(i1).indexOf("&");
			if ( i2 == -1)
				i2 = params.length();	// last parameter
			else
				i2 += i1;

			subString = params.substring(i1, i2);
			params = params.replace(subString, "EndDate=" + End);
		}
	}
	link += params;
	link = link.replace("\\", "/");
	if (staticReport)
	{
%>
	<jsp:forward page="<%=link %>"></jsp:forward>
<%
	}
	else
	{
%>
	<script type="text/javascript">
		displayReport("<%=link %>");
	</script>
<%
	}
%>

</body>
</html>

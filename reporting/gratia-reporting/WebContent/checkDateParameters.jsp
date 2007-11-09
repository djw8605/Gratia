<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="java.util.Date"
    import="java.text.SimpleDateFormat"
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<LINK href="stylesheet.css" type="text/css" rel="stylesheet">
<base target="central">
<title>Gratia Accounting</title>
</head>
<body>

<%
	String params = request.getQueryString();
	
	String inStart = request.getParameter("StartDate");
	String inEnd = request.getParameter("EndDate");
	String interval = request.getParameter("IntervalDays");
	if (interval == null || interval == "")
		interval = "7";
	long inInterval = new Long(interval);
	
	String link = "/frameset?";
	
	String inFormat = request.getParameter("__format");
	if (inFormat != null && inFormat.indexOf("pdf") > -1)
		link = "/run?";
	
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
%>
	<jsp:forward page="<%= link %>"></jsp:forward>
	
</body>
</html>

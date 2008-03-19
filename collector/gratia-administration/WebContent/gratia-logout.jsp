<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link href="stylesheet.css" type="text/css" rel="stylesheet">
<title>Gratia Accounting</title>
</head>

<body>
<%
	session.removeAttribute("FQAN");
	session.removeAttribute("displayLink");
%>

<script type="text/javascript">
	parent.location = "./index.html";
	//parent.adminDashboard.location = "./dashboard.jsp";
</script>
	
</body>
</html>

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
	session.invalidate();
%>

<script type="text/javascript">
	parent.adminDashboard.location = "./dashboard.jsp";
</script>
	
<p class="txt"> Thank you for using the gratia grid Accounting System.</p>
 
</body>
</html>

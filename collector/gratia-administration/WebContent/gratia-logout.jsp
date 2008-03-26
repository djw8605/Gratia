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
	// This is different session (https), so remove session attributes
   if ((String) session.getAttribute("FQAN") !=null)
	session.removeAttribute("FQAN");

   if ((String) session.getAttribute("displayLink") !=null)
	session.removeAttribute("displayLink");

   if ((net.sf.gratia.vomsSecurity.CertificateHandler) session.getAttribute("certificateHandler") != null)
	session.removeAttribute("certificateHandler");
%>

<script type="text/javascript">
	parent.location = "./index.html";
	//parent.adminDashboard.location = "./dashboard.jsp";
</script>
	
</body>
</html>

<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link href="stylesheet.css" type="text/css" rel="stylesheet">
<title>Gratia Accounting</title>
</head>

<body>
<%

   String userDN = (String) session.getAttribute("userDN");
	String logoutLink = "./index.html";
   	
	java.util.Properties p = net.sf.gratia.util.Configuration.getProperties();
	if (p != null)
		logoutLink = p.getProperty("service.open.connection") + request.getContextPath() + "/index.html";
	
	session.invalidate();
	session = request.getSession();
	session.setAttribute("userDN", userDN);
%>
<script type="text/javascript">
	parent.location = "<%=logoutLink %>";
</script>
	
</body>
</html>

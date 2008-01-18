<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Gratia Accounting</title>
</head>
<body>
	<%
	session.invalidate();
	%>

	<script type="text/javascript">
		parent.adminDashboard.location = "./dashboard.jsp";
	</script>
	<table>
		<tr>
			<td>Thank you for using the OSG reporting system</td>
		</tr>
	</table>
</body>
</html>

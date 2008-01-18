<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Gratia Accounting</title>
</head>
<body>
	<table>
		<tr>
			<td>This is a temporary page.
				<br>It will be replaced by the page that allows a
				user to login using his/hers browser certificate
				and selecting a VO.
			</td>
		</tr>

	<%
	// get the user information and set session attributes,
	// e.g.	session.setAttribute("username", certname);
	//		session.setAttribute(certname, vomsrole);
	//
	String certname = "penelope";
	String vomsrole = "admin";
	session.setAttribute("username", certname);
	session.setAttribute(certname, vomsrole);
	%>
	<%
	String user = (String) session.getAttribute("username");
	String role = (String) session.getAttribute(user);
	if (role.indexOf("admin") > -1) {
		%>
		<tr><td><strong><%=certname %> </strong>has logged in with the role of <strong><%=vomsrole %></strong></td></tr>
		<script type="text/javascript">
			parent.adminDashboard.location = "./dashboard.jsp";
		</script>
		<%
	}
	else {
		%>
		<tr><td>Invalid login information</td></tr>
		<%
	}
	%>
	</table>
</body>
</html>

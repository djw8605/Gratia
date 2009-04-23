<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional //EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link href="stylesheet.css" type="text/css" rel="stylesheet">
<title>Gratia Accounting</title>
</head>
<body>

<%
   String userDN = (String) session.getAttribute("userDN");
   String fqan   = (String) session.getAttribute("FQAN");

   if (userDN != null)
	userDN = "DN: " + userDN;
   else
	userDN = "";

   if (fqan != null)
	fqan = " FQAN: " + fqan;
   else
	fqan = "No Administrative Privileges";
%>
<div class='menuFqan'>
  <table width="80%" cellspacing="0" cellpadding="0">
    <tr>
    <%
    if (userDN.length() > 1)
    {
	%>
	<td nowrap><%=userDN %> <br>
		<hr size="1">
	</td>
	<%
    }

    if (fqan.length() > 7)
    {
	%>
	<td nowrap align="right"><%=fqan %> <br>
		<hr size="1">
	</td>
	<%
    }
    %>
    </tr>
  </table>
</div>

</body>
</html>

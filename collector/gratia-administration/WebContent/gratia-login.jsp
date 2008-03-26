<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Gratia Accounting</title>
<LINK href="stylesheet.css" type="text/css" rel="stylesheet">
<script src="resolveRequest.js" type="text/javascript"></script>
</head>

<body>
<%
try
{
	// This is different session (https), so remove session attributes
   if ((String) session.getAttribute("FQAN") !=null)
	session.removeAttribute("FQAN");

   if ((String) session.getAttribute("displayLink") !=null)
	session.removeAttribute("displayLink");

   if ((net.sf.gratia.vomsSecurity.CertificateHandler) session.getAttribute("certificateHandler") != null)
	session.removeAttribute("certificateHandler");

   // Set session attribute for the CertificateHandler
	net.sf.gratia.vomsSecurity.CertificateHandler certificateHandler = new net.sf.gratia.vomsSecurity.CertificateHandler(request);
	session.setAttribute("certificateHandler", certificateHandler);
	
	certificateHandler = (net.sf.gratia.vomsSecurity.CertificateHandler) session.getAttribute("certificateHandler");

	String[] VOlist = certificateHandler.getVOlist();

%>
<div id="roleSelected">

<form action="">
	<table class="query">
	<tr>
		<td valign="top" align="left">
			<table>
			<tr>
				<td><label class='paramName'> Select a VO </label> </td>
			</tr>
			<tr>
				<td>
				
				<select size="5" id="myVO" name="myVO" onchange="getRoles(this.value); return false;" >

				<%
				for(int i=0; i < VOlist.length; i++)
				{
					%><option value="<%= VOlist[i] %>" ><%= VOlist[i] %> </option>
					<%
				}
				%>
				</select>
				</td>
			</tr>
			</table>
		</td>
		<td>&nbsp;&nbsp;&nbsp;</td>

		<td valign="top" align="left">
				<div id="displayRoles"></div>
		</td>
	</tr>
	</table>
</form>
<%
}
catch (Exception ex)
{
	String msg = ex.getMessage();
	%>
	<table class="query" border="0"><tr> <td align="left" ><hr color="#FF8330"></td></tr>
	<tr><td><font color="#FF8330">
		The following error occured: <br>
		&nbsp;&nbsp;&nbsp;&nbsp;<strong><%= msg %></strong> <br>
		<em><%= ex %> </em>
		</font></td></tr><tr> <td align="left" ><hr color="#FF8330"></td></tr></table>
	<%
}
%>
</div>
</body>
</html>

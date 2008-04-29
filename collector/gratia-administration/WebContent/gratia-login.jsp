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
   if ((net.sf.gratia.vomsSecurity.CertificateHandler) session.getAttribute("certificateHandler") != null)
	session.removeAttribute("certificateHandler");

   // Set session attribute for the CertificateHandler
	net.sf.gratia.vomsSecurity.CertificateHandler certificateHandler = new net.sf.gratia.vomsSecurity.CertificateHandler(request);
	session.setAttribute("certificateHandler", certificateHandler);

	certificateHandler = (net.sf.gratia.vomsSecurity.CertificateHandler) session.getAttribute("certificateHandler");

	String[] VOlist = certificateHandler.getVOlist();
	String[] DNlist = certificateHandler.getDNlist();
	String vomsChck = certificateHandler.checkVOMSFile();
	String vomsFile = certificateHandler.getVomsFile();
	String userDN   = certificateHandler.getDN();
	boolean foundDN = false;

	if (DNlist.length > 0)
	{
		session.setAttribute("userDN", userDN);

		for (int j = 0; j < DNlist.length; j++)
		{
			if (userDN.equals(DNlist[j].trim()))
			{
				session.setAttribute("FQAN", "");
				foundDN = true;
			}
		}
		if (foundDN)
		{
			String displayLink = (String) session.getAttribute("displayLink");
			if (displayLink == null)
				displayLink = "./status.html?wantDetails=0";

			%>
			<script type="text/javascript">
				parent.adminContent.location = "./index.html";
			</script>
			<%
		}
	}
	
	if (vomsFile != null && vomsChck.length() > 1)
	{
		%><%= vomsChck %><%
	}
	else if (!foundDN && vomsFile != null)
	{
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
	else
	{
	%>
	<hr>
	<p class='txterror'>You have no privileges to access the administration pages</p>
	<hr>
	<%
	}
}
catch (Exception ex)
{
	String msg = ex.toString();
	%>
	<hr>
	<p class='txterror'>
		The following error occured: <br>
		&nbsp;&nbsp;&nbsp;&nbsp;<strong><%= msg %></p>
	<hr>
	<%
}
%>
</div>
</body>
</html>

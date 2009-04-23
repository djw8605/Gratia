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

	if ((String) session.getAttribute("FQAN") != null)
		session.removeAttribute("FQAN");

	// Set session attribute for the CertificateHandler
	net.sf.gratia.vomsSecurity.CertificateHandler certificateHandler = new net.sf.gratia.vomsSecurity.CertificateHandler(request);
	session.setAttribute("certificateHandler", certificateHandler);

        String displayLink = (String) session.getAttribute("displayLink");   

	certificateHandler = (net.sf.gratia.vomsSecurity.CertificateHandler) session.getAttribute("certificateHandler");

	String[] VOlist = certificateHandler.getVOlist();
	String[] DNlist = certificateHandler.getDNlist();
	String vomsChck = certificateHandler.checkVOMSFile();
	String userDN   = certificateHandler.getDN();
	boolean foundDN = false;

	if (DNlist.length > 0)
	{
		session.setAttribute("userDN", userDN);

		for (int j = 0; j < DNlist.length; j++)
		{
			if (DNlist[j].trim().equals("ALLOW ALL"))
			{
				if (userDN.trim().length() == 0)
				{
					userDN = "Non SSL Connection";
					session.setAttribute("userDN", userDN);
					session.setAttribute("FQAN", "Privileges based on property: ALLOW ALL");
				}
				else
				{
					session.setAttribute("FQAN", "Privileges based on a valid certificate and property: ALLOW ALL");
				}
				foundDN = true;
			}
			else if (userDN.equals(DNlist[j].trim()))
			{
				session.setAttribute("FQAN", "Privileges based on DN authorization");
				foundDN = true;
			}
		}
	}
	else
		foundDN = false;
	
	if ((vomsChck.length() > 1) && !foundDN)
	{
		%><%= vomsChck %><%
	}
	else if ((vomsChck.length() < 1) && !foundDN && (VOlist.length > 0))
	{
		foundDN = true;
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
	<tr>
	<td><div id="roleDisplay"></div>
	</td>
	</tr>
	</table>
</form>
<%
	}
	else if (!foundDN)
	{
		%>
		<hr>
		<p class='txterror'>You have no privileges to access the administration pages.
		<br>Login by selecting a VO and then a Role. </p>
		<hr>
		<%
	} else if (((String) session.getAttribute("FQAN") == null) && foundDN)
	{
		%>
		<hr>
		<p class='txterror'>You have no privileges to access the administration pages.
		<br>Login by selecting a VO and then a Role. </p>
		<hr>
		<%
	} else if (displayLink != null) {

            String agent = (String) request.getHeader("User-Agent");
            boolean hasjavascript = !agent.startsWith("Wget/");

            if (hasjavascript) {
               // Case where javascript is supported
			%>
			<script type="text/javascript">
				parent.adminContent.location = "./index.html";
			</script>
			<%
            } else {
              // No javascript
              response.sendRedirect(displayLink);
            }

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

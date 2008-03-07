<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Gratia Accounting</title>
<LINK href="stylesheet.css" type="text/css" rel="stylesheet">
<script src="resolveRequest.js"></script>
</head>

<body>
<%
	String[] VoNodes	= {""};
	String UserName		= "";
	String UserDN		= "";
	String UserGroup 	= "";
	String VO		= "";

	net.sf.gratia.vomsSecurity.CertificateHandler certificateHandler = new net.sf.gratia.vomsSecurity.CertificateHandler(request);
	
	UserName  = certificateHandler.getName();
	UserDN    = certificateHandler.getDN();
	UserGroup = certificateHandler.getGroups();
	VO        = certificateHandler.getVO();
	VoNodes	  = certificateHandler.getVoNodes();

%>
<div id="confirmRole">
<form>
	<table class="query">
	<tr>
		<td valign="top" align="left">
			<table>
			<tr>
				<td><label class='paramName'> Select a VO </label> </td>
			</tr>
			<tr>
				<td>
				<select size="5" id="myVO" name="myVO" onchange="getRoles(this.value)">

				<%
				for(int i=0; i < VoNodes.length; i++)
				{
					%><option value="<%= VoNodes[i] %>" ><%= VoNodes[i] %> </option>
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
			<table>
				<div id="getRoles"></div>
			</table>
		</td>
	</tr>
	</table>
</form>

</div>
</body>
</html>

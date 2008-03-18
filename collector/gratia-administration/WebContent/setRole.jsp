<%
 net.sf.gratia.vomsSecurity.CertificateHandler certificateHandler = new net.sf.gratia.vomsSecurity.CertificateHandler(request);

 String selectedRole = request.getParameter("selectedRole");
	
 if (selectedRole.indexOf("Admin") > -1)
 {
	session.setAttribute("FQAN", selectedRole);
 }
 else
 {
	session.setAttribute("FQAN", "NoPriveleges");
 }
%>

<%
 net.sf.gratia.vomsSecurity.CertificateHandler certificateHandler = new net.sf.gratia.vomsSecurity.CertificateHandler(request);

 String selectedRole = request.getParameter("selectedRole");

 String fqan = (String) session.getAttribute("FQAN");
 if (fqan !=null)
	session.removeAttribute("FQAN");
	
 if (selectedRole.indexOf("Admin") > -1)
 {
	out.println("<font size='-1'>Selected Role: " + selectedRole + "</font><br>");
	session.setAttribute("FQAN", selectedRole);
 }
 else
 {
	out.println("No privileges");
	session.setAttribute("FQAN", "NoPriveleges");
 }
%>

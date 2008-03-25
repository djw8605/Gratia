<%
 if ((String) session.getAttribute("FQAN") !=null)
	session.removeAttribute("FQAN");

 session.setAttribute("FQAN", "NoPrivileges");

 String selectedRole = request.getParameter("selectedRole");
 if (selectedRole != null)
 {
 	selectedRole = selectedRole.trim();
	
	net.sf.gratia.vomsSecurity.CertificateHandler certificateHandler = new net.sf.gratia.vomsSecurity.CertificateHandler(request);
	if (certificateHandler != null)
	{
		String[] voFQANlist = certificateHandler.getFQANlist();
		for (int j = 0; j < voFQANlist.length; j++)
		{
			if (selectedRole.equals(voFQANlist[j].trim()))
				session.setAttribute("FQAN", selectedRole);
		}
		
	}
 }

%>

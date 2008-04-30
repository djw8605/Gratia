<%
	if ((String) session.getAttribute("FQAN") != null)
		session.removeAttribute("FQAN");

	String selectedRole = request.getParameter("selectedRole");
	if (selectedRole != null)
	{
		selectedRole = selectedRole.trim();

		//  Check if a certificate handler seesion attribute exists to pick up the settings
		net.sf.gratia.vomsSecurity.CertificateHandler certificateHandler = (net.sf.gratia.vomsSecurity.CertificateHandler) session.getAttribute("certificateHandler");
		if (certificateHandler == null)
		{
			certificateHandler = new net.sf.gratia.vomsSecurity.CertificateHandler(request);
			session.setAttribute("certificateHandler", certificateHandler);
		}
		certificateHandler = (net.sf.gratia.vomsSecurity.CertificateHandler) session.getAttribute("certificateHandler");
	
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

	if ((String) session.getAttribute("FQAN") == null)
	{
		session.setAttribute("displayLink", "./noPrivileges.html");

		%>
		<script type="text/javascript">
			parent.adminContent.location = "./index.html";
		</script>
		<%
		
	}
%>

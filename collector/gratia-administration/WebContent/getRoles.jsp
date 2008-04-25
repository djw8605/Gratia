<%
	// Check if a certificate handler seesion attribute exists to pick up the settings
	net.sf.gratia.vomsSecurity.CertificateHandler certificateHandler = (net.sf.gratia.vomsSecurity.CertificateHandler) session.getAttribute("certificateHandler");
	if (certificateHandler == null)
	{
		certificateHandler = new net.sf.gratia.vomsSecurity.CertificateHandler(request);
		session.setAttribute("certificateHandler", certificateHandler);
	}
	certificateHandler = (net.sf.gratia.vomsSecurity.CertificateHandler) session.getAttribute("certificateHandler");

	String setVO           = null;
	String getSetVO        = null;
	String connectVOMsg    = null;
	String userDN          = null;
	String[] vomsUserRoles = null;
	
	String selectedVO = request.getParameter("selectedVO");

	if(selectedVO != null)
	{
		setVO = certificateHandler.setVOname(selectedVO);
		getSetVO = certificateHandler.getVOname();
		
		connectVOMsg = certificateHandler.connectVOname(selectedVO);

		if (connectVOMsg == "")
		{
			userDN   = certificateHandler.getDN();
			vomsUserRoles = certificateHandler.getVOMSroles();

			if ((String) session.getAttribute("userDN") !=null)
				session.removeAttribute("userDN");
				
			session.setAttribute("userDN", userDN);

			if (vomsUserRoles.length > 0)
			{
				out.println("<table><tr><td><label class='paramName'>Select a Role for VO: " + selectedVO + "</label><br></td></tr>");
				out.println("<tr><td><select size='5' id='myRole' name='myRole' onchange='confirmRole(this.value);' >");
				for(int i=0; i < vomsUserRoles.length; i++)
				{
					out.println("<option value='" + vomsUserRoles[i] +"'>" + vomsUserRoles[i] + "</option>");
				}
				out.println("</select></td></tr></table>");
			}
			else 
			{
				out.println("<table><tr><td><p class='txt'>No Roles defined for VO: " + selectedVO + "</p></td></tr></table>");
			}
		}
		else
		{
			out.println("<table><tr><td><p class='txterror'>" + connectVOMsg + "</p></td></tr></table>");
		}
	}
%>

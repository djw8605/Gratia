 <%
 net.sf.gratia.vomsSecurity.CertificateHandler certificateHandler = new net.sf.gratia.vomsSecurity.CertificateHandler(request);

String setVO        = null;
String getSetVO     = null;
String connectVOMsg = null;
String VoUserName   = null;
String VoUserDN     = null;
String[] VoUserRole ={"NoRoles"};
String VoUserGrp    = null;
String RolesStr     = "";
	
String selectedVO = request.getParameter("selectedVO");
//out.println("Selected VO: " + selectedVO + "<br>");

	if(selectedVO != null)
	{
		setVO = certificateHandler.setVOname(selectedVO);
		getSetVO = certificateHandler.getVOname();
		
		connectVOMsg = certificateHandler.connectVOname(selectedVO);

		if (connectVOMsg.indexOf("Connected") > -1)
		{
       			VoUserName = certificateHandler.getName();
       			VoUserDN   = certificateHandler.getDN();
       			VoUserRole = certificateHandler.getRole();
       			VoUserGrp  = certificateHandler.getGroups();
			
			String userDN = (String) session.getAttribute("userDN");
			if (userDN !=null)
				session.removeAttribute("userDN");
			session.setAttribute("userDN", VoUserDN);
		
			if (VoUserRole.length > 0)
			{
				out.println("<tr><td><label class='paramName'>Select a Role for VO: " + selectedVO + "</label><br></td></tr>");
				out.println("<tr><td><select size='5' id='myRole' name='myRole' onchange='confirmRole(this.value);' >");
				for(int i=0; i < VoUserRole.length; i++)
				{
		     			out.println("<option value=" + VoUserRole[i] +">" + VoUserRole[i] + "</option>");
				}
				out.println("</select></td></tr>");
			}
			else 
			{
				out.println("<tr><td><p class='txt'>No Roles defined for VO: " + selectedVO + "</p></td></tr>");
			}
		}
		else
		{
			out.println("<tr><td><p class='txt'>Could not connect to VO: " + selectedVO + "</p></td></tr>");
		}
	
	}

%>

<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="net.sf.gratia.security.*"
    import="java.sql.*"
    import="java.io.*"
 %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<LINK href="stylesheet.css" type="text/css" rel="stylesheet">
<title>SQL Report</title>

<script type="text/javascript">

function addVOs (form)
{
   /* Construct the VOs string from the selection */
   	form.SelectVOs.value = "";

	//parent.paramFrame.document.write('hello');

	for(var i = 0; i < form.myVOs.options.length; i++)
   	{
   		if (form.myVOs.options[i].selected)
   		{
   			if (form.SelectVOs.value != "")
   				form.SelectVOs.value += ";" + form.myVOs.options[i].value;
   			else
   				form.SelectVOs.value += form.myVOs.options[i].value;
   		}
   	}
}
function clearReportFrame() {
	parent.reportFrame.location = "about:blank";
}

function clearParamFrame() {
	parent.paramFrame.location = "about:blank";
}

</script>

</head>
<body>
<script type="text/javascript" src="tooltip/wz_tooltip.js"></script>

<%

//String[] VoNodes = {"VDT","oiv_test1","oiv_test2","cms"};
String[] VoNodes = {""};
String[] OptSel = {"","","","","","",""};

%>
<form action="">
<%
	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");

	//String viewerPath = null;
	//String csvFileName = null;
	String vsel = null;
	String vselget = null;
	String mvi_connect = null;
	String retcode = null;
	String VoUserName = null;
	String VoUserDN = null;
	String RolesStr="";
	String[] VoUserRole ={"NoRoles"};
	String VoUserGrp = null;
// Instantiate here Certificate handlre

	net.sf.gratia.reporting.CertificateHandler certificateHandler = new net.sf.gratia.reporting.CertificateHandler(request);
	System.out.println("### DONE with instantiating CertificateHandler ###");
	System.out.println("### ========================================== ###");

	String UserName  = certificateHandler.getName();
	String UserDN    = certificateHandler.getDN();
	//String[] UserRole  = certificateHandler.getRole();
	String UserGroup = certificateHandler.getGroups();
	String Subtitle  = certificateHandler.getSubtitle();
	String VO        = certificateHandler.getVO();
	VoNodes		 = certificateHandler.getVoNodes();

	String UserKey   = "" + System.currentTimeMillis();
	UserName = UserName + "|" + UserKey + "|" + VO;

	System.out.println("### getVO: User:  " + UserName + " ###");
	System.out.println("### getVO: Group: " + UserGroup + " UserDN: " + UserDN + " ###");



	String ReportTitle = request.getParameter("ReportTitle");
	if (ReportTitle != null)
   	{
%>
<div align="left" class="reportTitle"><%= ReportTitle%></div><br />
<input type="hidden" id="ReportTitle" name="ReportTitle" value="<%= ReportTitle%>">
<%
	}else
	{
		ReportTitle = "";
	}

	String mvi = request.getParameter("mySelectName");
	String mvi_vonode = request.getParameter("myVOs");

	if(mvi == null)
		mvi = "INITVAL";
	if(mvi_vonode == null){
		mvi_vonode = "Fermilab";
	}else{
		vsel = certificateHandler.setVOname(mvi_vonode);
		vselget = certificateHandler.getVOname();
		System.out.println("### getVO: calling getVOname: vselget= "  + vselget + " ###");
		System.out.println("### getVO: calling getVOname: connecting  ###");
		retcode = certificateHandler.connectVOname(mvi_vonode);
		if (retcode == null){

		 retcode = "Not connected yet";}

		System.out.println("### getVO: after calling getVOname: after connection , retcode = " + retcode + "###");
		//if(session.getAttribute("voConnect") == null) {
          	session.setAttribute("voConnect", retcode);
		//}
       		VoUserName = certificateHandler.getName();
       		VoUserDN = certificateHandler.getDN();
       		VoUserRole = certificateHandler.getRole();
       		VoUserGrp = certificateHandler.getGroups();

		session.setAttribute("VoConnectNode", vselget);
		session.setAttribute("VoUserName", VoUserName);

		String sesscode = (String)session.getAttribute("voConnect");
		System.out.println("### getVO: retcode = " + retcode + "###");
		System.out.println("### getVO: sesscode = " + sesscode + "###");
		System.out.println("### getVO: User    = " + VoUserName + "###");
		System.out.println("### getVO: UserDn  = " + VoUserDN + "###");
		//System.out.println("### getVO: Role    = " + VoUserRole + "###");
		System.out.println("### getVO: Group   = " + VoUserGrp + "###");
	}
	if (VoUserRole.length >0){
	  for(int i=0; i < VoUserRole.length; i++){
	     System.out.println("### getVO:role = " + VoUserRole[i]);
	     RolesStr = RolesStr + VoUserRole[i];
	     if (i < (VoUserRole.length -1)) RolesStr = RolesStr + ",";

	  }
	}else {
	  System.out.println(" ### getVO:NO roles found for user " + VoUserName);
	  RolesStr = "NoRoles";
	}
	session.setAttribute("VoUserRoles", VoUserRole);

	for(int i=0; i < VoNodes.length; i++){
		if (VoNodes[i] == mvi_vonode) {
		  OptSel[i] = "selected";
		}
	}

	%>  VO connection status =  <%= retcode %> <br><%
	%>  UserName =  <%= VoUserName %> Roles    =  <%= RolesStr %><br><%

	%>
	  <table class="query">
	   <tr>

	     <td valign="top" align="right">
        	 <label class="paramName" onmouseover="Tip('Select VO\'s to connect to ');"> Select VO and then GO</label>
	     </td>


	     <td>
  		 <select multiple size="5" id="myVOs" name="myVOs"  onchange="addVOs(this.form); " >

		   <%
		   for(int i=0; i < VoNodes.length; i++)
		   {
		      %><option value="<%= VoNodes[i] %> " <%= OptSel[i] %> ><%= VoNodes[i] %> </option>

		      <%
		   }
		   %>
  		 </select>
	     </td>
	   </tr>
	   <tr>
	       <td align="right"><em><label class="paramName" onmouseover="Tip('Readonly field')" >Selected VO:</label></em>
	       </td>
	       <td><em><input id="SelectVOs" type="text" size="30" name="SelectVOs" Value = "<%= mvi_vonode %> " readonly onmouseover="Tip('Readonly field', CLICKCLOSE, false)"></em>
	       </td>
	   </tr>

	       <tr>
	    	   <td>
	    	      <input class= "button" type="submit" value="Connect" onclick="clearReportFrame();">
	    	   </td>
	       </tr>
	 </table>

</form>
</body>
</html>

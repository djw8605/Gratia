<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="org.eclipse.birt.report.engine.api.*"
    import="org.eclipse.birt.core.framework.*"
    import="org.eclipse.birt.report.model.*"
    import="javax.servlet.*"
    import="java.util.HashMap"
    import="java.util.Map"
    import="java.util.Collection"
    import="java.util.Collections"
    import="java.util.Iterator"
    import="java.util.Date"
    import="java.util.logging.Level"
    import="java.text.SimpleDateFormat"
    import="java.io.File"
    import="java.io.FileOutputStream"
    import="net.sf.gratia.reporting.*"
    import="java.sql.*"
    import="java.io.*"
%>
    
<%@ taglib uri="/WEB-INF/tlds/birt.tld" prefix="birt" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<LINK href="stylesheet.css" type="text/css" rel="stylesheet">
<title>Gratia Reporting: Parameter Entry</title>

<script language="javascript" src="calendar/calendardef.js"></script>
<script language="javascript" src="calendar/calendarstd.js"></script>
<script language="javascript">
<!--
	
var c1 = new CodeThatCalendar(caldef1);

function addVO (form) {
/* Construct the VOs string from the selection */  
        form.VOs.value = "(";
		
	for(var i = 0; i < form.myVOs.options.length; i++)
		if (form.myVOs.options[i].selected)
		   if (form.VOs.value != "(") 
		   	form.VOs.value += "," + "'"+ form.myVOs.options[i].value + "'";
		   else
                	form.VOs.value += "'"+ form.myVOs.options[i].value + "'";
        form.VOs.value += ")";
    }

function getURL (form) {
       
       form.ReportURL.value = form.BaseURL.value;
	
       form.ReportURL.value += "&VOs=" + form.VOs.value; 
       form.ReportURL.value += "&StartDate=" + form.StartDate.value;
       form.ReportURL.value += "&EndDate=" + form.EndDate.value;
    }

//-->
</script>


</head>
<body>

	<jsp:include page="common.jsp" />
	

<%
// get the parameters passed
String report =request.getParameter("report");
	String myReportTitle = request.getParameter("reportTitle");
	if (myReportTitle != null)
   	{
%>
<div align="left" class="reportTitle"><%=myReportTitle%></div><br>
<%
	}


String requestedReportNameAndPath = request.getParameter("report");
String pageID = "" + System.currentTimeMillis();

// Load the report parameters

ReportParameters reportParameters = new ReportParameters();
reportParameters.loadReportParameters(requestedReportNameAndPath);

// Define current date (End date) and a week ago (Start Date)
Date now = new Date();
SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
String End = format.format(now);
now = new Date(now.getTime() - (7 * 24 * 60 * 60 * 1000));
String Start = format.format(now);

String url = request.getRequestURL().toString();
url=url.substring(0, url.lastIndexOf("/")) + "/frameset?__report="+report;

// Get the reporting configuration setting
ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");
	
%>

<!-- START: Debugging messages 

<hr>
****** DEBUGGING ***** <br>
Report = <%= report %><br>
BASE URL = <%=url %> <br>
Datadase URL = <%=reportingConfiguration.getDatabaseURL() %> <br> 
Datadase user = <%=reportingConfiguration.getDatabaseUser() %> <br> 
Datadase password = <%=reportingConfiguration.getDatabasePassword() %> <br> 
EngineHome = <%=reportingConfiguration.getEngineHome() %> <br>
ReportsFolder = <%=reportingConfiguration.getReportsFolder() %> <br>
WebappHome = <%=reportingConfiguration.getWebappHome() %> <br>
ReportsMenuConfig = <%=reportingConfiguration.getReportsMenuConfig() %> <br>
LogsHome = <%=reportingConfiguration.getLogsHome() %> <br>
CsvHome = <%=reportingConfiguration.getCsvHome() %> <br>
ConfigLoaded = <%=reportingConfiguration.getConfigLoaded() %> <br>
Report URL Initial value = <%=url%> <br>
<hr>

END: Debugging messages -->


<%
String paramName = null;
String propertyName = null;
String propertyValue = null;
String selectName = null;
String selectValue = null;
%> 

<birt:parameterPage id="<%=pageID %>" name="parameterInput" reportDesign="<%= report %>" isCustom="true" title="">

<table>	
<input type=hidden name="BaseURL" Value = <%=url %>>
<%
for(int i=0; i < reportParameters.getParamGroups().size(); i++)
{
	ParameterGroup paramGroup = (ParameterGroup)reportParameters.getParamGroups().get(i);
	paramName = paramGroup.getParameterName();
	String helpText = null;
	String promptText  = null;
	String defaultValue  = null;
	Boolean hasSelectionOptions = false;
	Boolean hidden = false;
	
// Get the Parameter properties and set the appropriate variables
	for(int z=0; z < paramGroup.getParameterProperties().size(); z++)
	{
		ParameterProperty paramProperty = (ParameterProperty)paramGroup.getParameterProperties().get(z);
						
		propertyName = paramProperty.getPropertyName();				
		propertyValue = paramProperty.getPropertyValue();
		if ((propertyName.indexOf("hidden") >-1) && (propertyValue.indexOf("true") >-1))
		{
			hidden = true;
			break;
		}
		if (propertyName.indexOf("helpText") >-1)
			helpText = propertyValue;
		if (propertyName.indexOf("promptText") >-1)
			promptText = propertyValue;
		if (propertyName.indexOf("defaultValue") >-1)
			defaultValue = propertyValue;
		if ((propertyName.indexOf("controlType") >-1) && (propertyValue.indexOf("list-box") >-1))
			hasSelectionOptions = true;	
	}

// Display information only for parameters that are not hidden
	if (!hidden)
	{
		if (promptText == null)
			promptText = paramName;
		if (helpText == null)
			helpText = promptText;
		if (defaultValue == null)
			defaultValue = "";
			
	// Set the start and end date
		if(paramName.equals("StartDate"))
			defaultValue = Start;
		if (paramName.equals("EndDate"))
			defaultValue = End;

     		if (hasSelectionOptions)
		{
			%>
		 	<tr>
			   <td><label class=paramName><%=promptText %></label><br> <font size=-1><%=helpText%></font></td>
			   <td>
			   <select class=paramSelect id="<%=paramName%>" name="<%=paramName%>" >
			<%
			for(int s=0; s < paramGroup.getParameterListSelection().size(); s++)
			{
				ParameterListSelection paramListSelection = (ParameterListSelection)paramGroup.getParameterListSelection().get(s);
						
				selectName = paramListSelection.getSelectionName();				
				selectValue = paramListSelection.getSelectionValue();
				String selected = "";
				if(selectValue.equals(defaultValue))
					selected="selected";
%>
				<option value=<%=selectValue %> <%=selected %>><%=selectName %></option>
<%							
			}
%>
			   </select>
			   </td>
			</tr>			
<%     		
		}else if(paramName.indexOf("Date") > -1)
		{
%>
			<tr>
			   <td><label class=paramName><%=promptText %></label></td>
			   <td>
			   <input type="text" id="<%=paramName %>" name="<%=paramName %>" value="<%=defaultValue %>" >
			   	<BUTTON name="cal1" value="cal1" type="button" class=button onclick="c1.popup('<%=paramName %>');" >
    				<IMG SRC="./calendar/img/cal.gif" ALT="test"></BUTTON>
			   </td>
			</tr>
	<%							
		}
		else if (paramName.indexOf("VOs") > -1)
		{
	%>
			<tr>
			   <td valign="top"><label class=paramName> Select one or more VOs:</label></td>
			   <td> 
				<SELECT multiple size="10" id="myVOs" name="myVOs" onChange="addVO(this.form)" >
						
	<%		
			// define the sql string to get the list of VOs that the user can selct from
			String sql = "select distinct (VO.VOName) from VO, VONameCorrection where VO.VOid = VONameCorrection.VOid order by VO.VOName";
			
			// Execute the sql statement to get the vos
			
			Connection con = null;
			Statement statement = null;
			ResultSet results = null;
			String VOName = "";
			String SelectedVOs = "(";					 
						
			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();
			} catch (ClassNotFoundException ce){
				out.println(ce);			
			}
			
			try{						
				con = DriverManager.getConnection(reportingConfiguration.getDatabaseURL(), reportingConfiguration.getDatabaseUser(), reportingConfiguration.getDatabasePassword());
				statement = con.createStatement();
				results = statement.executeQuery(sql);	

			// Loop through the SQL results to add a row for each record, we have only one column that contains the VOName
						
				while(results.next())
				{								
				// Get the value for this column from the recordset, ommitting nulls
						
					Object value = results.getObject(1);
							
					if (value != null) 
					{								
						VOName = value.toString();
													
						String selected = "";
						if(defaultValue.indexOf(VOName) > -1)
						{
							selected="selected";
							
							if (SelectedVOs != "(") 
								SelectedVOs += "," + "'"+ VOName + "'";
							else
                						SelectedVOs += "'"+ VOName + "'";
						}
						%> <OPTION value="<%=VOName %>" <%=selected %>><%=VOName %></OPTION> <%
					}
				}
				if ( SelectedVOs == "(") 
					SelectedVOs ="";
				else 
					SelectedVOs += ")";
					
			}catch(SQLException exception){
				out.println("<!--");
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				exception.printStackTrace(pw);
				out.print(sw);
				sw.close();
				pw.close();
				out.println("-->");
			}
			finally
			{
				try
				{
					con.close();
				}
				catch(Exception ex) {}		
				try
				{
					statement.close();
				}
				catch(Exception ex) {}		
				try
				{
					results.close();
				}
				catch(Exception ex) {}	
	
				results = null;
				statement = null;
				con = null;
			}
				
			%>
			   </SELECT>
			</td>
		</tr>
		<tr>
		   <td> Selected VOs:</td><td><input id="VOs" type="text"  name="<%=paramName%>" Value = "<%=SelectedVOs %>" readonly size="60" ></td>
		</tr>
		<%
		}			
		else
		{
		%>
		    <tr>
			<td><label class=paramName><%=promptText %></label></td>
			<td>
				<input id="<%=paramName%>" type="text" name="<%=paramName %>" value="<%=defaultValue %>" >
			</td>
		    </tr>
		<%		  
		} 
     	}
}
%>
	<tr>
	   <td colspan=3>
		<input class=button type=submit name=submitButton value=Submit>
	   </td>
	</tr>
</table>

</birt:parameterPage>
</body>
</html>

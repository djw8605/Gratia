<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="java.util.Date"
    import="java.text.SimpleDateFormat"
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

<script type="text/javascript" src="calendar/calendardef.js"></script>
<script type="text/javascript" src="calendar/calendarstd.js"></script>
<script type="text/javascript">
	
var c1 = new CodeThatCalendar(caldef1);

function addVO (form) 
{
/* Construct the VOs string from the selection */  
	form.VOs.value = "(";
		
	for(var i = 0; i < form.myVOs.options.length; i++)
	{
		if (form.myVOs.options[i].selected)
		{
			if (form.VOs.value != "(") 
				form.VOs.value += "," + "'"+ form.myVOs.options[i].value + "'";
			else
				form.VOs.value += "'"+ form.myVOs.options[i].value + "'";
		}
	}
 	form.VOs.value += ")";
}
   
   function addVO2 (form) 
   {
   /* Construct the VOs string from the selection */  
   	form.VOs.value = "";
   		
   	for(var i = 0; i < form.myVOs.options.length; i++)
   	{
   		if (form.myVOs.options[i].selected)
   		{
   			if (form.VOs.value != "") 
   				form.VOs.value += ";" + form.myVOs.options[i].value;
   			else
   				form.VOs.value += form.myVOs.options[i].value;
   		}
   	}
}

function getURL ()
{
   var x=document.getElementsByTagName('form')[0]
   var url = "";

	for (var i=0;i<x.length;i++)
	{
  		if (x.elements[i].name == "BaseURL" )
     		url = x.elements[i].value;
 	}
 
	for (var i=0;i<x.length;i++)
	{
		name = x.elements[i].name;
		value = x.elements[i].value;
		if (name != "myVOs" && name != "submitButton" && name != "BaseURL"  && name != "ReportURL" )
		{
			url += "&" + name + "=" + value;
		}
	}
	// replace all spaces --> %20 and ":" --> %7C
	url = url.replace(/ /g, "%20");
	url = url.replace(/;/g, "%3B");
	x.ReportURL.value = url;
  	// document.write(url);
  	// document.write("<br />");
}
    
</script>


</head>
<body>

	<jsp:include page="common.jsp" />

<%
// get the parameters passed
	String ReportTitle = request.getParameter("reportTitle");
	if (ReportTitle != null)
   	{
%>
<div align="left" class="reportTitle"><%=ReportTitle%></div><br>
<%
	}else
	{
		ReportTitle = "";
	}

String report = request.getParameter("report");
String pageID = "gratiaReporting";

// Load the report parameters

ReportParameters reportParameters = new ReportParameters();
reportParameters.loadReportParameters(report);

// Define current date (End date) and a week ago (Start Date)
Date now = new Date();
SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
String End = format.format(now);
now = new Date(now.getTime() - (7 * 24 * 60 * 60 * 1000));
String Start = format.format(now);

String initUrl = request.getRequestURL().toString();
initUrl=initUrl.substring(0, initUrl.lastIndexOf("/")) + "/frameset?__report="+report;

// Get the reporting configuration setting
ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");
	
%>


<!-- START: Debugging messages 
<hr>
****** DEBUGGING ***** <br>
BASE URL = <%=initUrl %> <br>
Report = <%= report %><br>
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


<input type=hidden id="baseURL" name="BaseURL" Value = "<%=initUrl %>">
<input type=hidden id="ReportURL" name="ReportURL" Value="<%=initUrl %>">
<input type=hidden id="ReportTitle" name="ReportTitle" Value="<%=ReportTitle %>">

<table>
<%

for(int i=0; i < reportParameters.getParamGroups().size(); i++)
{
	ParameterGroup paramGroup = (ParameterGroup)reportParameters.getParamGroups().get(i);
	paramName = paramGroup.getParameterName();
	String helpText = null;
	String promptText  = null;
	String defaultValue  = null;
	String inputValue = null;
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
		if (helpText == null || helpText.indexOf(promptText) > -1)
			helpText = "";
		if (defaultValue == null)
			defaultValue = "";

		// check if a value has been passed for this parameter in the URL.
		// If so then make it the default value for this parameter.
		// If the parameters are Start/EndDate do not use the given default value.
		inputValue = request.getParameter(paramName);
		if (inputValue != null) 
				defaultValue = inputValue;
			
	// Set the start and end date
		if(paramName.equals("StartDate"))
			defaultValue = Start;
		if (paramName.equals("EndDate"))
			defaultValue = End;

     		
		if(paramName.indexOf("Date") > -1)
		{
%>
			<tr>
			   <td><label class=paramName><%=promptText %></label><br> <font size=-1><%=helpText%></font></td>
			   <td>
			   <input type="text" id="<%=paramName %>" name="<%=paramName %>" value="<%=defaultValue %>"  onchange="getURL();" >
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
			   <td valign="top"><label class=paramName> Select one or more VOs:</label><br> <font size=-1><%=helpText%></font></td>
			   <td> 
				<SELECT multiple size="10" id="myVOs" name="myVOs" onChange="addVO2(this.form); getURL();" >
						
	<%		
			// define the sql string to get the list of VOs that the user can selct from
			String sql = "select distinct (VO.VOName) from VO, VONameCorrection where VO.VOid = VONameCorrection.VOid order by VO.VOName";
			
			// Execute the sql statement to get the vos
			
			Connection con = null;
			Statement statement = null;
			ResultSet results = null;
			String VOName = "";
			String SelectedVOs = "";					 
						
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
							
							if (SelectedVOs != "") 
								SelectedVOs += ";" + VOName;
							else
								SelectedVOs += VOName;
						}
						%> <OPTION value="<%=VOName %>" <%=selected %>><%=VOName %></OPTION> <%
					}
				}
				//if ( SelectedVOs == "(") 
				//	SelectedVOs ="";
				//else 
				//	SelectedVOs += ")";
					
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
		   <td> Selected VOs:</td><td><input id="VOs" type="text"  name="<%=paramName%>" Value = "<%=SelectedVOs %>" readonly size="60"  onchange="getURL()" ></td>
		</tr>
		<%
		}
		else if (paramName.indexOf("ForVOName") > -1)
		{
	%>
			<tr>
			   <td valign="top"><label class=paramName><%=promptText%></label><br> <font size=-1><%=helpText%></font></td>
			   <td> 
				<SELECT size="10" id="ForVOName" name="ForVOName" onChange="getURL();" >
						
	<%		
			// define the sql string to get the list of VOs that the user can selct from
			String sql = "select distinct (VO.VOName) from VO, VONameCorrection where VO.VOid = VONameCorrection.VOid order by VO.VOName";
			
			// Execute the sql statement to get the vos
			
			Connection con = null;
			Statement statement = null;
			ResultSet results = null;
			String VOName = "";
			String SelectedVOs = "";					 
						
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
							
							if (SelectedVOs != "") 
								SelectedVOs += ";" + VOName;
							else
								SelectedVOs += VOName;
						}
						%> <OPTION value="<%=VOName %>" <%=selected %>><%=VOName %></OPTION> <%
					}
				}
					
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
		<%
		}
		else if (paramName.indexOf("ForSiteName") > -1)
		{
	%>
			<tr>
			   <td valign="top"><label class=paramName><%=promptText%></label><br> <font size=-1><%=helpText%></font></td>
			   <td> 
				<SELECT size="10" id="ForSiteName" name="ForSiteName" onChange="getURL();" >
						
	<%		
			// define the sql string to get the list of VOs that the user can selct from
			String sql = "select Site.SiteName as sitename from Site";
			
			// Execute the sql statement to get the vos
			
			Connection con = null;
			Statement statement = null;
			ResultSet results = null;
			String SiteName = "";
			String SelectedSite = "";					 
						
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
						SiteName = value.toString();
													
						String selected = "";
						if(defaultValue.indexOf(SiteName) > -1)
						{
							selected="selected";
							
							if (SelectedSite != "") 
								SelectedSite += ";" + SiteName;
							else
								SelectedSite += SiteName;
						}
						%> <OPTION value="<%=SiteName %>" <%=selected %>><%=SiteName %></OPTION> <%
					}
				}
					
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
		<%
		}
		else if (paramName.indexOf("ForProbeName") > -1)
		{
	%>
			<tr>
			   <td valign="top"><label class=paramName><%=promptText%></label><br> <font size=-1><%=helpText%></font></td>
			   <td> 
				<SELECT size="10" id="ForProbeName" name="ForProbeName" onChange="getURL();" >
						
	<%		
			// define the sql string to get the list of VOs that the user can selct from
			String sql = "select Site.SiteName as sitename from Site";
			
			// Execute the sql statement to get the vos
			
			Connection con = null;
			Statement statement = null;
			ResultSet results = null;
			String ProbeName = "";
			String SelectedProbe = "";					 
						
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
						ProbeName = value.toString();
													
						String selected = "";
						if(defaultValue.indexOf(ProbeName) > -1)
						{
							selected="selected";
							
							if (SelectedProbe != "") 
								SelectedProbe += ";" + ProbeName;
							else
								SelectedProbe += ProbeName;
						}
						%> <OPTION value="<%=ProbeName %>" <%=selected %>><%=ProbeName %></OPTION> <%
					}
				}
					
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
		<%
		}
		else if (hasSelectionOptions)
				{
					%>
				 	<tr>
					   <td><label class=paramName><%=promptText %></label><br> <font size=-1><%=helpText%></font></td>
					   <td>
					   <select class=paramSelect id="<%=paramName%>" name="<%=paramName%>"  onchange="getURL()" >
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
<%     		}
		else
		{
		%>
		    <tr>
			<td><label class=paramName><%=promptText %></label><br> <font size=-1><%=helpText%></font></td>
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
		<input class=button type=submit name=submitButton value=Submit >
	   </td>
	</tr>
</table>

<p>
<script type="text/javascript">

// load initial url
   var x=document.getElementsByTagName('form')[0];
   var url = "";

	for (var i=0;i<x.length;i++)
	{
  		if (x.elements[i].name == "BaseURL" )
     		url = x.elements[i].value;
	}
 
	for (var i=0;i<x.length;i++)
	{
		name = x.elements[i].name;
		value = x.elements[i].value;
		if (name != "myVOs" && name != "submitButton" && name != "BaseURL" && name != "ReportURL" )
		{
			url += "&" + name + "=" + value;
		}
	}
	// replace all spaces --> %20 and ";" --> %3B
	url = url.replace(/ /g, "%20");
	url = url.replace(/;/g, "%3B");
	x.ReportURL.value = url;

</script>
</p>    

</birt:parameterPage>

</body>
</html>

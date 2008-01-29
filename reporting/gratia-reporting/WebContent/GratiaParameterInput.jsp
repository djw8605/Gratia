<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="java.util.Date"
    import="java.text.SimpleDateFormat"
    import="net.sf.gratia.reporting.*"
    import="java.sql.*"
    import="java.io.*"
%>
 
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<base target="reportFrame">
<link href="stylesheet.css" type="text/css" rel="stylesheet">
<title>Gratia Reporting</title>

<script type="text/javascript" src="calendar/calendardef.js"></script>
<script type="text/javascript" src="calendar/calendarstd.js"></script>
<script type="text/javascript">
	
var c1 = new CodeThatCalendar(caldef1);

function addVOs (form) 
{
	/* Construct the VOs string from the selection */
	form.SelectVOs.value = "";
		
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

function getURL ()
{
	var x=document.getElementsByTagName('form')[0];
	var myurl = "";
	var elnam = "";
	var elval = "";

	for (var i=0; i < x.length; i++)
	{
		if (x.elements[i].name == "partURL" )
		myurl = x.elements[i].value;
	}

	for (var i=0; i<x.length; i++)
	{
		elnam = x.elements[i].name;
		elval = x.elements[i].value;
		if (elnam != "myVOs" && elnam != "cal1" && elnam != "submitButton" && elnam != "partURL"  && elnam != "ReportURL" )
		{
			myurl += "&" + elnam + "=" + elval;
		}
	}
	// replace all spaces --> %20 and ";" --> %3B
	myurl = myurl.replace(/ /g, "%20");
	myurl = myurl.replace(/;/g, "%3B");
	x.ReportURL.value = myurl;
	
	return myurl;
	
	// document.write(myurl);
	// document.write("<br />");
}

function getAction()
{
	var x=document.getElementsByTagName('form')[0];
	var newAction = x.ReportURL.value;
	document.mySubmitForm.action = newAction;
	// document.write(newAction);
	// document.write("<br />");
}

</script>

</head>

<body>

<script type="text/javascript" src="tooltip/wz_tooltip.js"></script>
<jsp:include page="common.jsp" />

<%
// get the parameters passed. Make sure to parse the queryString in case getParameter does not get the parameter

int i1 = -1;
int i2 = -1;

String wholeParams = request.getQueryString();
String displayReport = "true"; //request.getParameter("displayReport");

//if (displayReport == null)
//	displayReport = "false";

//if (wholeParams.indexOf("displayReport=true") > -1)	
//	displayReport = "true";

String report = request.getParameter("report");

String inTitle = request.getParameter("ReportTitle");
if (inTitle == null)
{	
	inTitle = "";
	if (wholeParams.indexOf("ReportTitle") > -1)
	{
		String str = "ReportTitle=";
		i1 = wholeParams.indexOf("ReportTitle") + str.length();
		i2 = wholeParams.substring(i1).indexOf("&");
		if ( i2 == -1)
			i2 = wholeParams.length();	// last parameter
		else
			i2 += i1;
				
		inTitle = wholeParams.substring(i1, i2).replace("%20", " ");
	}
}

%><div class="reportTitle"><%= inTitle%></div><br /><%

// Load the report parameters

ReportParameters reportParameters = new ReportParameters();
reportParameters.loadReportParameters(report);

// Define current date (End date) and a week ago (Start Date)
Date now = new Date();
SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
String End = format.format(now);
now = new Date(now.getTime() - (7 * 24 * 60 * 60 * 1000));
String Start = format.format(now);

// Initialize ReportURL
String initUrl = request.getRequestURL().toString();
initUrl=initUrl.substring(0, initUrl.lastIndexOf("/")) + "/checkDateParameters.jsp?__report=" + report + "&amp;__title";

// Get the reporting configuration setting
ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");

String paramName = null;
String propertyName = null;
String propertyValue = null;
String selectName = null;
String selectValue = null;
%> 
<table>
<tr>
<td>
<form action="">

<input type="hidden" id="partURL" name="partURL" value = "<%= initUrl %>">
<input type="hidden" id="ReportURL" name="ReportURL" value="<%= initUrl %>">
<input type="hidden" id="ReportTitle" name="ReportTitle" value="<%= inTitle %>">

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
		if (helpText == null)
			helpText = "";
		if (defaultValue == null)
			defaultValue = "";
		
		helpText = helpText.replace("'", "\\'"); // Escape "'" so that the tool tip works

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
			   <td align="right"><label class="paramName" onMouseOver="Tip('<%= helpText%>')" ><%= promptText %></label></td>
			   <td>&nbsp;&nbsp;</td>
			   <td>
			   <input type="text" id="<%= paramName %>" name="<%= paramName %>" value="<%= defaultValue %>" onmouseover="Tip('<%= helpText%>')" onchange="getURL();" >
				<button name="cal1" value="cal1" type="button" class="button" onmouseover="Tip('<%= helpText%>')" onclick="c1.popup('<%= paramName %>');" >
				<img src="./calendar/img/cal.gif" alt="<%= helpText%>"></BUTTON>
			   </td>
			</tr>
			<%
		}
		else if (paramName.indexOf("Select") > -1 )
		{
			displayReport = "false";
			String selectNameID = paramName;
			String selectedLabel = "";
			String selectMultiple = "";
			String selected = "selected";
			String onchangeFunction = "getURL();";
			String sql = "";
			
			if (paramName.indexOf("SelectVOs") > -1 )
			{
				selectNameID = "myVOs";
				selectedLabel = "Selected VOs:";
				selectMultiple = "multiple";
				selected = "";
				onchangeFunction = "addVOs(this.form); getURL();";
				sql = "select distinct (VO.VOName) from VO, VONameCorrection where VO.VOid = VONameCorrection.VOid order by VO.VOName";
			}
			else if (paramName.indexOf("SelectVOName") > -1 )
			{
				sql = "select distinct (VO.VOName) from VO, VONameCorrection where VO.VOid = VONameCorrection.VOid order by VO.VOName";
			}
			else if (paramName.indexOf("SelectSiteName") > -1 )
			{
				sql = "select Site.SiteName as sitename from Site order by sitename";
			}
			else if (paramName.indexOf("SelectProbeName") > -1 )
			{
				sql = "select 'All' as probename from CEProbes union select CEProbes.probename as probename from CEProbes order by probename";
			}
			%>
			<tr>
			   <td valign="top" align="right"><label class="paramName" onMouseOver="Tip('<%= helpText%>');" ><%= promptText %></label></td>
			   
			   <td>&nbsp;&nbsp;</td>
			   <td> 
				<select <%= selectMultiple %> size="5" id="<%= selectNameID %>" name="<%= selectNameID %>" onmouseover="Tip('<%= helpText%>');" onchange="<%= onchangeFunction %>" >					
			<%

			// Execute the sql statement
			Connection con = null;
			Statement statement = null;
			ResultSet results = null;
			String resultValue = "";
			String selectedItems = "";

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
						resultValue = value.toString();

						if(defaultValue.indexOf(resultValue) > -1)
						{
							selected="selected";

							if (selectedItems != "") 
								selectedItems += ";" + resultValue;
							else
								selectedItems += resultValue;
						}
						%> <option value="<%= resultValue %>" <%= selected %> ><%= resultValue %></option> 
						<%
					}
					selected="";
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
				</select>
			   </td>
			</tr>
			<%
			if (paramName.indexOf("SelectVOs") > -1 )
			{
				%>
				<tr>
				   <td align="right"><em><label class="paramName" onmouseover="Tip('Readonly field')" ><%= selectedLabel %></label></em></td>
				   <td>&nbsp;&nbsp;</td>
				   <td><em><input id="<%= paramName%>" type="text" size="30" name="<%= paramName%>" Value = "<%= selectedItems %>" readonly onmouseover="Tip('Readonly field', CLICKCLOSE, false)"></em>
				   </td>
				</tr>
				<%
			}
		}
		else if (hasSelectionOptions)
		{
			%>
			<tr>
			   <td align="right"><label class="paramName" onmouseover="Tip('<%= helpText%>')" ><%= promptText %></label></td>
			   <td>&nbsp;&nbsp;</td>
			   <td>
			   <select class="paramSelect" id="<%= paramName%>" name="<%= paramName%>"  onchange="getURL()" onmouseover="Tip('<%= helpText%>')" >
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
				<option value=<%= selectValue %> <%= selected %>><%= selectName %></option>
				<%
			}
			%>
				</select>
			   </td>
			</tr>
			<%
		}
		else
		{
			%>
			<tr>
			<td align="right"><label class="paramName" onmouseover="Tip('<%= helpText%>')" ><%= promptText %></label></td>
			<td>&nbsp;&nbsp;</td>
			<td><input id="<%= paramName%>" type="text" name="<%= paramName %>" value="<%= defaultValue %>" onmouseover="Tip('<%= helpText%>')" >
				</td>
			</tr>
			<%
		} 
	}
}
%>
</table>

<script type="text/javascript">

// load initial url
	var x=document.getElementsByTagName('form')[0];
	var outurl = "";
	var elname = "";
	var elvalue = "";

	for (var i=0; i < x.length; i++)
	{
		if (x.elements[i].name == "partURL" )
		outurl = x.elements[i].value;
	}

	for (var i=0;i<x.length;i++)
	{
		elname = x.elements[i].name;
		elvalue = x.elements[i].value;
		if (elname != "myVOs" && elname != "cal1" && elname != "submitButton" && elname != "partURL" && elname != "ReportURL" )
		{
			outurl += "&" + elname + "=" + elvalue;
		}
	}
	// replace all spaces --> %20 and ";" --> %3B
	outurl = outurl.replace(/ /g, "%20");
	outurl = outurl.replace(/;/g, "%3B");
	x.ReportURL.value = outurl;

</script>

</form>

</td>
<td>&nbsp;&nbsp;</td>
<td valign="top">
	<input class="button" type="submit" name="submitButton" value="Display Report below" onclick="parent.reportFrame.location = getURL();">
	<p>
	<input class="button" type="submit" name="submitButton" value="Display Report in new Window" onclick="window.open(getURL());">
	</p>
</td>
</tr>
</table>
<script type="text/javascript">
		parent.reportFrame.document.write('<hr color="#CBCB97" size="4" >');
		parent.reportFrame.document.close();
</script>
<%
if (displayReport.indexOf("true") > -1)
{
%>
	<script type="text/javascript">
		parent.reportFrame.location = getURL();
	</script>
<%
}
%>
</body>
</html>

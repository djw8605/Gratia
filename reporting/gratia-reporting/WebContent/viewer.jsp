<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="org.eclipse.birt.report.engine.api.*"
    import="java.util.HashMap"
    import="java.util.Map"
    import="java.util.Collection"
    import="java.util.Iterator"
    import="java.util.Date"
    import="java.text.SimpleDateFormat"
    import="java.io.File"
    import="java.io.FileOutputStream"
    import="net.sf.gratia.reporting.*"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<LINK href="stylesheet.css" type="text/css" rel="stylesheet">
<script language="javascript1.2" src="calendar/calendarstd.js"></script>
<script language="javascript1.2" src="calendar/calendardef.js"></script>
<script language="javascript1.2">
<!--
var c1 = new CodeThatCalendar(caldef1);
//-->
</script>
<title>Report Viewer</title>
</head>
<body>

<form>
	<jsp:include page="common.jsp" />
	
	<%	
	net.sf.gratia.reporting.CertificateHandler certificateHandler = new net.sf.gratia.reporting.CertificateHandler(request);
	certificateHandler.dump();
	String UserName = certificateHandler.getName();
	String UserRole = certificateHandler.getRole();
	String Subtitle = certificateHandler.getSubtitle();
	String VO = certificateHandler.getVO();
	String UserKey = "" + System.currentTimeMillis();
	UserName = UserName + "|" + UserKey + "|" + VO;

	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");
	System.out.println("User: " + UserName + " Role: " + UserRole);
	
	EngineConfig reportEngineConfig = new EngineConfig();
	reportEngineConfig.setEngineHome( reportingConfiguration.getEngineHome() );
	//reportEngineConfig.setLogConfig("", java.util.logging.Level.SEVERE);
	
	ReportEngine reportEngine = new ReportEngine(reportEngineConfig);
	
	// Check if we should force the system to regenerate the report even if a cached version exists
	String forceRefresh = request.getParameter("__forceRefresh");
	
	// Determine which report the user is requesting
	
	String testingagain = request.getQueryString();
	String myReportTitle = request.getParameter("__title");
   
%>
<div align="left" class="reportTitle"><%=myReportTitle%></div><br>
<%
	String requestedReportNameAndPath = request.getParameter("__report");

	if(requestedReportNameAndPath == null)
		throw new Exception("__report parameter was not specified in the URL.  The full path to a .rptdesign file must be specified in the __report paramter so the viewer knows which report to open.");
	
	// Ensure the requested report file actually exists
	File reportDesignFile = new File(requestedReportNameAndPath);
	if(reportDesignFile.exists() == false)
		throw new Exception(requestedReportNameAndPath + " does not exist");
	reportDesignFile = null;
	
	// Glean the name of the .rptdesign file from the __report parameter in case a path exists in the value
	String requestedReportName = requestedReportNameAndPath.substring(requestedReportNameAndPath.lastIndexOf("/") +1);
	
	// Open the requested report design file
	IReportRunnable design = reportEngine.openReportDesign(requestedReportNameAndPath); 

	// Create a task to get the reports parameters.  
	IGetParameterDefinitionTask paramTask =  reportEngine.createGetParameterDefinitionTask( design );
	Collection reportParameters = paramTask.getParameterDefns(false);
	// Determine if all report parameters have been provided in the URL
	// While were at it, build a unique temp file name to store the html generated for the requested report with the specified parameters		
	boolean promptForParameters = false;
	String htmlName = requestedReportName;
		
	// Get only the name without the directory, check if it is windows or unix
	if (htmlName.lastIndexOf('/') != -1)
		htmlName = htmlName.substring(htmlName.lastIndexOf('/') + 1);
	else 
		htmlName = htmlName.substring(htmlName.lastIndexOf('\\') + 1);
	
	for (Iterator paramIterator = reportParameters.iterator(); paramIterator.hasNext();)
	{
		IParameterDefnBase param = (IParameterDefnBase) paramIterator.next( );
		IScalarParameterDefn scalar = (IScalarParameterDefn) param;
		if(param.getName().indexOf("Database") == -1)
		{	
			String paramValue = (String)request.getParameter(param.getName());
			if(paramValue == null || paramValue.trim().equals(""))
			{
	    		if(!scalar.isHidden())
					promptForParameters = true;	
			}
			else
			{
				// TODO:  When the html name gets too long, it just won't open!  Find a better way to uniquely identify cached reports.
				htmlName = htmlName + paramValue.replaceAll("/", "").replaceAll(":", "").replaceAll(" ","");
			}
		} // End check for not a database specific parameter
		
		param = null;
		scalar = null;
	} // Loop through report parameters
	htmlName = htmlName + ".html";
	
	// If one or more of the required report parameters were not in the request URL, then we need to prompt for parameter entry
	//  otherwise we can display the report
	if(promptForParameters == true)
	{
%>
	<input type=hidden name="__report" value="<%=requestedReportNameAndPath %>">
	<table>	
<%
		//Loop through each report parameter to set the database information.
		for (Iterator paramIterator = reportParameters.iterator(); paramIterator.hasNext();) 
		{ 
			IParameterDefnBase param = (IParameterDefnBase) paramIterator.next( );
			IScalarParameterDefn scalar = (IScalarParameterDefn) param;
			// Set any parameters 
			// Identify database specific parameters and set them from their session values
			if(param.getName().equals("DatabaseURL"))
				paramTask.setParameterValue(param.getName(), reportingConfiguration.getDatabaseURL());
			else if(param.getName().equals("DatabasePassword"))
				paramTask.setParameterValue(param.getName(), reportingConfiguration.getDatabasePassword());					
			else if(param.getName().equals("DatabaseUser"))
				paramTask.setParameterValue(param.getName(), reportingConfiguration.getDatabaseUser());
			else if(param.getName().equals("UserName"))
				paramTask.setParameterValue(param.getName(),UserName);
			else if(param.getName().equals("UserRole"))
				paramTask.setParameterValue(param.getName(),UserRole);
			else if(param.getName().equals("Subtitle"))
				paramTask.setParameterValue(param.getName(),Subtitle);
			else if(param.getName().equals("Start Date") || param.getName().equals("StartDate"))
			{
					Date now = new Date();
					SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
					paramTask.setParameterValue(param.getName(),format.format(now));
			}
			else if (param.getName().equals("End Date") || param.getName().equals("EndDate"))
			{
					Date now = new Date();
					SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
					paramTask.setParameterValue(param.getName(),format.format(now));
			}
		}

// Loop through each report parameter and create an entry row for it containing the parameter name and an input field
		for (Iterator paramIterator = reportParameters.iterator(); paramIterator.hasNext();) 
		{ 
			IParameterDefnBase param = (IParameterDefnBase) paramIterator.next( );
			IScalarParameterDefn scalar = (IScalarParameterDefn) param;
			// Set any parameters 


			String paramValue = request.getParameter(param.getName());

			if(!scalar.isHidden())
			{
				String displayName = param.getDisplayName();
		
				if(displayName == null)
					displayName = param.getName();

				if(param.getName().equals("Start Date") || param.getName().equals("StartDate"))
						{
								Date now = new Date();
								now = new Date(now.getTime() - (7 * 24 * 60 * 60 * 1000));
								SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
								paramValue = format.format(now);
						}
				if (param.getName().equals("End Date") || param.getName().equals("EndDate"))
						{
								Date now = new Date();
								SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
								paramValue = format.format(now);
						}
				if(paramValue == null)
				{
					Object tempVal = paramTask.getDefaultValue(param);

					if(tempVal == null)
						paramValue = "";
					else
					{						
						if(scalar.getDataType() == 4)
						{
							paramValue = ((Date)tempVal).toLocaleString();
						}
						else
							paramValue = (String)tempVal;
					}
				}
				String helpText = param.getHelpText();
				if(helpText == null)
					helpText = "";
								
				//Parameter is a List Box
				if(scalar.getControlType() ==  IScalarParameterDefn.LIST_BOX)
				{
%>
		<tr>
			<td><label class=paramName><%=displayName %></label></td>
			<td>
				<select class=paramSelect name="<%=param.getName() %>">
<%					
					Collection paramValues = paramTask.getSelectionList(param.getName());
					
					if(paramValues != null)
					{
						for ( Iterator sliter = paramValues.iterator( ); sliter.hasNext( ); )
						{
							IParameterSelectionChoice selectionItem = (IParameterSelectionChoice) sliter.next( );
							String value = (String)selectionItem.getValue( );
							String label = selectionItem.getLabel( );
							String selected = "";
							if(paramValue.equals(value))
								selected="selected";
%>
					<option value=<%=value %> <%=selected %>><%=label %></option>
<%							
						}
	
					}
%>
				</select>
			</td>
			<td><label class=paramHelp><%=helpText %></label></td>
		</tr>			
<%				
					
					paramValues = null;
				}
				else if(scalar.getDataType() == 4 || scalar.getName().indexOf("Date") > -1)
				{
					// If the date value is >= the year 2100, default it to today's date instead
					if (new Date(paramValue).after(new Date("12/31/2099")))
					{
						SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
						paramValue = df.format(new Date());
					}
%>
		<tr>
			<td><label class=paramName><%=displayName %></label></td>
			<td>
				<input id="<%=param.getName() %>" name="<%=param.getName() %>" value="<%=paramValue %>">
				<input type=button class=button onclick="c1.popup('<%=param.getName() %>');" value="...">
			</td>
			<td><label class=paramHelp><%=helpText %></label></td>
		</tr>
<%							
				}
				else
				{					
%>
		<tr>
			<td><label class=paramName><%=displayName %></label></td>
			<td>
				<input name="<%=param.getName() %>" value="<%=paramValue %>">
			</td>
			<td><label class=paramHelp><%=helpText %></label></td>
		</tr>
<%		  
				} // End check for parameter type
				
				scalar = null;
			} // End check for not a database specific parameter
			
			param = null;
		} // Loop through report parameters
%>	
		<tr>
			<td colspan=2>
				<input class=button type=submit name=submitButton value=Submit>
			</td>
		</tr>
	</table>
<%		
	} // End check for parameter entry needed
	else
	{
		// TODO:  Force refresh all the time for now, but in the future we'll need to set something up that caches per user
		//  for a certain amount of time (hourly, daily, etc.)
		forceRefresh = "1";
		
		// Check to see if an HTML file with the URL parameters already exists for the requested report.  Then we can use it instead of generating again
		File htmlFile = new File(reportingConfiguration.getWebappHome() + "temp_html/" + htmlName);		
		if(htmlFile.exists() == false || forceRefresh != null)
		{			
			// Create task to run the report 
			IRunAndRenderTask runTask = reportEngine.createRunAndRenderTask(design); 

			//Set Render context to handle url and image locations
			HTMLRenderContext renderContext = new HTMLRenderContext();
			renderContext.setImageDirectory("temporary_images");
			renderContext.setBaseURL("../viewer.jsp");
			HashMap contextMap = new HashMap();
			contextMap.put( EngineConstants.APPCONTEXT_HTML_RENDER_CONTEXT, renderContext );
			runTask.setAppContext( contextMap );
		
			// Save the report to a temporary html file for viewing
			HTMLRenderOption options = new HTMLRenderOption();
			options.setOutputFileName(reportingConfiguration.getWebappHome() + "temp_html/" + htmlName);
			options.setOutputFormat("html");
			options.setEmbeddable(true);
			runTask.setRenderOption(options);
			
			// Set any parameters 
			for (Iterator paramIterator = reportParameters.iterator(); paramIterator.hasNext();) 
			{ 
				IParameterDefnBase param = (IParameterDefnBase) paramIterator.next( );
				
				// Identify database specific parameters and set them from their session values
				if(param.getName().equals("DatabaseURL"))
					runTask.setParameterValue(param.getName(), reportingConfiguration.getDatabaseURL());
				else if(param.getName().equals("DatabasePassword"))
					runTask.setParameterValue(param.getName(), reportingConfiguration.getDatabasePassword());					
				else if(param.getName().equals("DatabaseUser"))
					runTask.setParameterValue(param.getName(), reportingConfiguration.getDatabaseUser());					
				else if(param.getName().equals("UserName"))
					runTask.setParameterValue(param.getName(), UserName);					
				else if(param.getName().equals("UserRole"))
					runTask.setParameterValue(param.getName(), UserRole);					
				else if(param.getName().equals("Subtitle"))
					runTask.setParameterValue(param.getName(), Subtitle);					
				else {
					IScalarParameterDefn scalar = (IScalarParameterDefn) param;
					String paramValue = request.getParameter(param.getName());
					if(paramValue == null)
					{
						Object tempVal = paramTask.getDefaultValue(param);

						if(tempVal == null)
							paramValue = "";
						else
						{						
							if(scalar.getDataType() == 4)
							{
								paramValue = ((Date)tempVal).toLocaleString();
							}
							else
								paramValue = (String)tempVal;
						}
					}
					runTask.setParameterValue(param.getName(), paramValue);
				}
			} // Loop through report parameters			

			String reqHighlightDate = request.getParameter("HighlightDate");
			String reqSelectedDate = request.getParameter("SelectedDate");
			String reqStartDate = request.getParameter("Start Date");
			String reqEndDate = request.getParameter("End Date");
			Date HighlightDate = null;
			if (reqSelectedDate != null) {
				if (reqHighlightDate == null || reqHighlightDate.length()==0) {
					runTask.setParameterValue("HighlightDate",reqSelectedDate);
					HighlightDate = ReportParams.convertStringToDate(reqSelectedDate);
				} else {
					// We have both an highlighted and selected date, use 
					// those as the begin and end.
					reqStartDate = reqHighlightDate;
					reqEndDate= reqSelectedDate;
					runTask.setParameterValue("HighlightDate","");
				}
			} else if (reqHighlightDate == null) {
				runTask.setParameterValue("HighlightDate","");
			}
			
			Date startDate = null;
			Date endDate = null;
			String groupByFormat = null;
			String axisPattern = null;
			if (reqStartDate==null) {
				reqStartDate = (String)runTask.getParameterValue("Start Date");
				if (reqEndDate==null) reqEndDate = (String)runTask.getParameterValue("End Date");
			}
			if (reqStartDate!=null) {
				startDate = ReportParams.convertStringToDate(reqStartDate);
				if (reqEndDate == null) endDate = new Date();
				else endDate = ReportParams.convertStringToDate(reqEndDate);

				groupByFormat = "%c/%d/%Y";
				axisPattern = "MM/dd/yyyy";
			
				// If start date is after end date, then swap the two
				if(startDate.after(endDate))
				{
					Date origStartDate = startDate;
					startDate = endDate;
					endDate = origStartDate;
				} else if (startDate.equals(endDate)) {
				    ReportParams.resetHours(startDate,1);
				    ReportParams.resetHours(endDate,0);
				}
			    long elapsed = DateUtil.getElapsedDays(startDate, endDate);
				
				// If there is less than 2 days between start and end date, group by minutes.  Less than 6, group by hours
				if(elapsed < 1)	
				{
					groupByFormat = "%c/%d/%Y %HH:%ii";
				    axisPattern = "MM/dd/yyyy hh:mm";
				}
				else if(elapsed < 2)	
				{
					groupByFormat = "%c/%d/%Y %HH";
 				    axisPattern = "MM/dd/yyyy hh:00";
				}		
				else if(elapsed < 6)	
				{
					groupByFormat = "%c/%d/%Y %HH";
				    axisPattern = "MM/dd/yyyy hh:00";
				}
				else
				{       
					ReportParams.resetHours(startDate,1);
					ReportParams.resetHours(endDate,0);
				}
				if (runTask.getParameterValue("UserRole") != null)
				{	
					System.out.println("Setting Special Variables");
					runTask.setParameterValue("UserRole",UserRole);
					runTask.setParameterValue("UserName",UserName);
					runTask.setParameterValue("Subtitle",Subtitle);
				}
				runTask.setParameterValue("Start Date", ReportParams.convertDateToString(startDate));
				runTask.setParameterValue("End Date", ReportParams.convertDateToString(endDate));
				runTask.setParameterValue("GroupByFormat", groupByFormat);
				runTask.setParameterValue("TimeAxisPattern", axisPattern);
			    if (HighlightDate!=null) {
				    // strHighlightDate must match exactly the labels.
				   	java.text.DateFormat f = new java.text.SimpleDateFormat(axisPattern);
				   	runTask.setParameterValue("HighlightDate",f.format(HighlightDate));
				}
			}
			
			// Run the report and destroy the engine
			System.out.println("runtask parameters: " + runTask.getParameterValues());
			runTask.run();

			String sqlselect = ""; //"select";
   		    if (design.getDesignHandle()!=null && design.getDesignHandle().getModule()!=null) {
   		    	org.eclipse.birt.report.model.elements.ReportDesign ra = 
   		    		(org.eclipse.birt.report.model.elements.ReportDesign)design.getDesignHandle().getModule();
		
   			    if (ra!=null) {
   			    	org.eclipse.birt.report.model.core.DesignElement el = ra.findDataSet("TimeQuery");
   			    	if (el==null) el = ra.findDataSet("UsageBy");
   			    	if (el==null) el = ra.findDataSet("UsageDrill");
   			    	if (el==null) el = ra.findDataSet("UsageByVORanked");
   			    	if (el==null) el = ra.findDataSet("UsageBySiteRanked");
   			    	if (el==null) el = ra.findDataSet("JobsCountBy");
   			    	if (el==null) el = ra.findDataSet("JobsCountByWhere");
   			    	if (el==null) el = ra.findDataSet("UsageDrill");
   		  		  	if (el!=null) {
   		  		  		java.util.List qparams = (java.util.List)el.getProperty(ra,"parameters");
   		  		  		String qparamsStr = qparams.toString();
   		  		  		if (qparamsStr == null) qparamsStr = "";
   		  		  		Object query = el.getProperty(ra,"queryText");
		       	        if (query!=null) {
		       	        	sqlselect = query.toString();
		       	        	int count = 0;
		       	        	while (sqlselect.indexOf("?")>=0 && count < qparams.size()) {
		       	        		
		       	        		org.eclipse.birt.report.model.api.elements.structures.DataSetParameter param = 
		       	        			(org.eclipse.birt.report.model.api.elements.structures.DataSetParameter)qparams.get(count);
		       	        		String pname = param.getName();
		       	        		String what;
		       	        		if (pname.equals("Start Date") || pname.equals("StartDate") ) {
							        what = "'"+ReportParams.convertDateToString(startDate)+"'";
								} else if (pname.equals("End Date") || pname.equals("EndDate")) {
									what = "'"+ReportParams.convertDateToString(endDate)+"'";
								} else if (pname.equals("GroupByFormat")) {
									what = "'"+groupByFormat+"'";
								} else {
									what = "'"+runTask.getParameterValue(pname)+"'";									
								}
								sqlselect = sqlselect.replaceFirst("\\?",what);
								count = count + 1;
							}
		       	        }
   		  		  	}
   		 	   }
   		    }

		    runTask.close();
			runTask = null;
			options = null;
			renderContext = null;
			contextMap = null;			
			
			//htmlFile = new File(reportingConfiguration.getWebappHome() + "temp_html/" + htmlName);
			if(htmlFile.exists() && requestedReportNameAndPath.indexOf("temp_custom") == -1)
			{	
				// Add a refresh button to the html to redirect it to itself with the '__forceRefresh' parameter
				// TODO:  Since caching is off, there is no need for a refresh button			
       			//	String refreshButton = "<div style=\"position: absolute; top: 10px; z-index=0\"><a href = " + request.getRequestURL() + "?" + request.getQueryString() + "&__forceRefresh=1><img src=\"../images/refresh.png\" alt=\"Refresh\" border=0></a></div>";
			    
       			if (startDate!=null && (sqlselect != null && sqlselect.length()!=0)) {
	
    	   			String act = request.getRequestURI();
       				act = act.replaceFirst("viewer","admin");

						try
						{	
							net.sf.gratia.reporting.GenerateSQL generator = new net.sf.gratia.reporting.GenerateSQL(sqlselect,UserKey);
							sqlselect = generator.generate();
						}
						catch (Exception ignore)
						{
						}

				    String tableButton = "\n<form action=\""+act+"\">\n"
			    	    +"	<input type=hidden name=\"sql\" value=\""+sqlselect+"\">\n"
				    	+"	<input type=hidden name=\"__report\" value=\""+requestedReportNameAndPath+"\">\n"
				    	+"  <input type=hidden name=\"StartDate\" value=\""+ReportParams.convertDateToString(startDate)+"\">\n"
				    	+"  <input type=hidden name=\"EndDate\" value=\""+ReportParams.convertDateToString(endDate)+"\">\n"
				    	+"  <input type=hidden name=\"GroupByFormat\" value=\""+groupByFormat+"\">\n"
				    	+"  <input class=button type=submit name=submitButton value=\"See Table\">\n  </form>";
	
				    FileOutputStream htmlFileOut = new FileOutputStream(reportingConfiguration.getWebappHome() + "temp_html/" + htmlName, true);
					//htmlFileOut.write(refreshButton.getBytes());
					htmlFileOut.write(tableButton.getBytes());
					htmlFileOut.close();
					htmlFileOut = null;
       			}
			}

		} // End check for HTML file existing
		htmlFile = null;
		response.sendRedirect("temp_html/" + htmlName);
	} // End check for parameter entry not needed

	design = null;
	reportParameters =  null;
	paramTask.close();
	paramTask = null;
	reportEngine.destroy();
	reportEngine = null;
	reportEngineConfig = null;
%>
</form>
</body>
</html>

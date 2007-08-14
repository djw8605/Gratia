<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="net.sf.gratia.reporting.ReportingConfiguration"
    import="java.sql.*"
    import="org.eclipse.birt.report.engine.api.*"
    import="org.eclipse.birt.report.model.api.*"%>
    <%@page import="java.io.*"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<LINK href="stylesheet.css" type="text/css" rel="stylesheet">
<title>SQL Report</title>
</head>
<form>
	<jsp:include page="common.jsp" />
<%	
	String testingagain = request.getQueryString();
	String myReportTitle = request.getParameter("__title"); 
	if (myReportTitle != null)
	{
%>
<div align="left" class="reportTitle"><%=myReportTitle%></div><br>
<%
	}	
	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");	
	String csvFileName = null;
	String viewerPath = null;

	String sql = request.getParameter("sql");
	if(sql == null)
		sql = "";

	if(sql.trim().length() > 0)
	{
		Connection cnn = null;
		Statement statement = null;
		ResultSet results = null;
		ResultSetMetaData metadata = null;
		 
		try
		{  
// Create, if it does not exist, a temporary directory to hold the csv files. 
// TO DO: Keep only last week's files and delete the rest.
			
			// Create the temporary folder if it doesn't already exist
			String tempCsvPath=reportingConfiguration.getWebappHome() + "temp_csv/";
			java.io.File newCsvDir=new java.io.File(tempCsvPath);
			if (!newCsvDir.exists()) 
				newCsvDir.mkdirs();
			newCsvDir = null;
			
			// Open for write the csv file to a temporary location with a unique file name
			java.util.Calendar calCsv = java.util.Calendar.getInstance(java.util.TimeZone.getDefault());
			csvFileName = tempCsvPath + "gratia_report_data_" + calCsv.getTimeInMillis() + ".csv";

		 	BufferedWriter csvOut = new BufferedWriter(new FileWriter(csvFileName, true));
// Ready to write to the csv file 
  
			// Execute the SQL so we can get the resulting column names (and check any errors)
			Class.forName("com.mysql.jdbc.Driver");
			cnn = DriverManager.getConnection(reportingConfiguration.getDatabaseURL(), reportingConfiguration.getDatabaseUser(), reportingConfiguration.getDatabasePassword());
			statement = cnn.createStatement();
			results = statement.executeQuery(sql);	
			metadata = results.getMetaData();			
			
			// Open the customSqlReport design template and get a reference to its grid object
			//  This allows all the formatting to be done within the design template instead of dynamically
			SessionHandle designSession = DesignEngine.newSession( null );
			ReportDesignHandle design = designSession.openDesign(reportingConfiguration.getReportsFolder() + "customSqlReport.rptdesign" ); 
			GridHandle grid = (GridHandle)design.getBody().get(0);
			ElementFactory factory = design.getElementFactory( ); 				
			
			// Add a column to the table for each column in the resultset
			String csvLine = "";
			int j=1;
			RowHandle row = (RowHandle) grid.getRows( ).get( 0);
			for(int i=1; i<metadata.getColumnCount()+1; i++)
			{
				// Add a column if one doesn't already exist
				if(i > grid.getColumns().getCount())
					grid.getColumns().add(factory.newTableColumn());
				
				// Add a cell if one doesn't already exist
				if(i > row.getCells().getCount())
					row.getCells().add(factory.newCell());

				// Create a label to hold the column name, and insert it into the cell
				LabelHandle label = factory.newLabel( null ); 
				label.setStringProperty("textAlign","right");
				label.setText( metadata.getColumnName(i) );
				CellHandle cell = (CellHandle) row.getCells( ).get( i-1 ); 
				cell.getContent( ).add( label ); 
				
				// Construct the csv file header line  				
				if (i<metadata.getColumnCount()) 
					{csvLine = csvLine + metadata.getColumnName(i) + ",";						
					}
				else
					{ csvLine = csvLine + metadata.getColumnName(i);
					}
			}
			row = null;
			
			// write header line to the csv file		
			csvOut.write(csvLine+"\n"); 
			csvLine = "";
			
			// Loop through the SQL results to add a row for each record
			int rowIndex = 1;
			while(results.next())
			{
				// Add a row if one doesn't already exist
				if(rowIndex > grid.getRows().getCount()-1)
					grid.getRows().add(grid.getRows().get(1).copy().getHandle(design.getModule()));				
				
				// Get a reference to the current row
				row = (RowHandle) grid.getRows( ).get( rowIndex);
				row.clearContents(0);
				
				// Loop through each column to add its data to the row cell-by-cell
				for(int i=1; i<metadata.getColumnCount()+1; i++)
				{
					// Add a cell if one doesn't already exist
					if(i > row.getCells().getCount())
						row.getCells().add(factory.newCell());
	
					// Get the value for this column from the recordset, casting nulls to a valid string value
					Object value = results.getObject(i);
					if(value == null)
						value = "[null]";
					
					// Create a label to hold the data value, and insert it into the cell
					LabelHandle label = factory.newLabel( null );
					label.setText( value.toString() );
					if (value instanceof Double) {
						int intvalue = (int)Math.floor(((Double)value).doubleValue());
						Integer toprint = new Integer(intvalue);
						label.setText( toprint.toString() );
					}
					else
						label.setText( value.toString() );
						
					label.setStringProperty("textAlign","right");
		
					CellHandle cell = (CellHandle) row.getCells( ).get( i-1 ); 
					cell.getContent( ).add( label ); 
					
					//Construct a csv file line of data				
					if (i<metadata.getColumnCount()) 
						{csvLine = csvLine + value + ",";						
						}
					else
						{ csvLine = csvLine + value;
						}
				}
				
				row =  null;
				
				// write a new csv line
				csvOut.write(csvLine+"\n"); 
				csvLine = "";
				
				rowIndex++;
			}
			// Flush all the data to the csv file and close it.
			csvOut.flush();
			csvOut.close();
			csvLine = "";
			csvOut = null;
			
			// Create the temporary folder if it doesn't already exist
			String tempPath=reportingConfiguration.getWebappHome() + "temp_custom/";
			java.io.File newDir=new java.io.File(tempPath);
			if (!newDir.exists()) 
				newDir.mkdirs();
			newDir = null;
			
			// Save the rptdesign to a temporary location with a unique file name
			java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getDefault());
			String designFileName = cal.getTimeInMillis() + ".rptdesign";
			design.saveAs( tempPath + designFileName );
			design.close( );
			cal = null;
			
			// Set the viewer path to point to the generated design file
			if (myReportTitle != null)
			{
			  viewerPath = "viewer.jsp?__report=" + tempPath + designFileName + "&amp;__title=" + myReportTitle;
			}
			else
			{
			  viewerPath = "viewer.jsp?__report=" + tempPath + designFileName;
			}
			
			//designSession.closeAll(true);
			//designSession =  null;
			design = null;
			grid = null;
			factory = null;
		}
		catch(Exception ex)
		{
			throw new Exception(ex);
		}
		finally
		{
			try
			{
				cnn.close();
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

			metadata = null;
			results = null;
			statement = null;
			cnn = null;
		}
	}
%>
	<table width="100%" height="100%">		
		<tr>
			<td>
				Enter in a SQL statement below and click 'Execute' to see the results.
			</td>
		</tr>
		<tr>
			<td>
				<textarea name="sql" rows=10 cols=80><%=sql %></textarea>
			</td>
		</tr>
		<tr>
			<td>
				<input class=button type=submit value=Execute> 
			</td>
		</tr>
		<tr height="100%">
			<td>
			   <div align="center"><input type="button" value="Download Report Data - CSV Format"
			   onClick="window.open('downloadFile.jsp?csvFile=<%=csvFileName %>', 'Gratia');"></div>
			   <br>
<%			
			if(viewerPath != null)
			{
%>
<!-- Find if the browser is either Konquer or Safari. If so then make the width and height of the iframe fixed --> 
<%@ page import="javax.servlet.*" %>

<%
String browserType = request.getHeader("User-Agent");
String browser = new String("");
String version = new String("");
browserType = browserType.toLowerCase();
if(browserType != null ){
	if ((browserType.indexOf("safari") != -1) || (browserType.indexOf("konqueror") != -1)) {


%>
	       <iframe border=0 frameborder=0 height="400" width="600" SCROLLING="auto" src="<%=viewerPath %>"></iframe>
<%
	}
	else {
%>
	       <iframe border=0 frameborder=0 height="85%" width="100%" SCROLLING="auto" src="<%=viewerPath %>"></iframe>
<%
	}
}

	}
%>
			</td>
		</tr>
	</table>
</form>
</html>

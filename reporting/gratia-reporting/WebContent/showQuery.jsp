<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="net.sf.gratia.reporting.*"
    import="java.sql.*"
    import="java.io.*"
 %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<LINK href="stylesheet.css" type="text/css" rel="stylesheet">
<title>SQL Report</title>
</head>
<body>
<form action="">
	
<%	
	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");	
	
	String viewerPath = null;
	String csvFileName = null;
	

	String ReportTitle = request.getParameter("ReportTitle");
	if (ReportTitle != null)
   	{
%>
<div align="left" class="reportTitle"><%= ReportTitle%></div><br />
<%
	}else
	{
		ReportTitle = "";
	}
	
	String sql = request.getParameter("sql");
		
	if (sql == null)
	{
	
		String traceTableKey = request.getParameter("TraceTableKey");
	// Get the query from the trace table
		
		try {	
			GenerateSQL generator = new GenerateSQL(sql, traceTableKey);
			sql = generator.generate();	
			}
		catch (Exception ignore) { }	
	}
	
%>

<table class="query">		
<%
	// Get the query from the trace table
	
	if(sql == null)
		sql = "";

	if(sql.trim().length() > 0)
	{
		Connection cnn = null;
		Statement statement = null;
		ResultSet results = null;
		ResultSetMetaData metadata = null;
		
		//create a temporary file and folder to contain the csv output
		// TO DO: Keep only last week's files and delete the rest.
		
		try
		{ 
		// Create, if it does not exist, a temporary directory to hold the csv files. // TO DO: Keep only last week's files and delete the rest.
			
			// Create the temporary folder if it doesn't already exist
			String tempCsvPath = "";
			if (System.getProperty( "os.name" ).indexOf("Windows") != -1){ 
				tempCsvPath = "C:\\gratia\\temp_csv\\";
			} else {
				tempCsvPath = reportingConfiguration.getCsvHome();
			}
				
			java.io.File newCsvDir=new java.io.File(tempCsvPath);
			if (!newCsvDir.exists()) 
				newCsvDir.mkdirs();
			newCsvDir = null;
			
			// Open for write the csv file to a temporary location with a unique file name
			
			String somethingUnique = "" + System.currentTimeMillis();
			csvFileName = tempCsvPath + "gratia_report_data_" + somethingUnique + ".csv";

		 	BufferedWriter csvOut = new BufferedWriter(new FileWriter(csvFileName, true)); 	
		 	
		// Execute the SQL so we can get the resulting column names (and check any errors)
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			cnn = DriverManager.getConnection(reportingConfiguration.getDatabaseURL(), reportingConfiguration.getDatabaseUser(), reportingConfiguration.getDatabasePassword());
			// cnn = DriverManager.getConnection("jdbc:mysql://gratia-db01.fnal.gov:3320/gratia_itb", "reader", "reader");
			statement = cnn.createStatement();
			results = statement.executeQuery(sql);	
			metadata = results.getMetaData();
			%>
			   <tr>
				<td colspan="<%= metadata.getColumnCount()+1%>">
					<em>Enter bellow a SQL statement and press 'Execute' to see the results.</em>
					<br />
					<textarea name="sql" rows=12 cols=85  class="querytxt"><%= sql %></textarea>
					<br />
					<input class=button type=submit value=Execute> 
					<br />
					 <div align="center"><input type="button" value="Download Report Data - CSV Format"
						   onClick="window.open('downloadFile.jsp?csvFile=<%= csvFileName %>', 'Gratia');"></div>
					 <br />
				</td>
			   </tr> 
			   
		<%			
			// Add a column to the table for each column in the resultset
			
			String csvLine = "";
			%> <tr> <%
			for(int i=1; i<metadata.getColumnCount()+1; i++)
			{
				%> <td align="right"><strong><%= metadata.getColumnName(i) %></strong></td> <td>&nbsp;</td> <%
					
				// Construct the csv file header line  				
				if (i<metadata.getColumnCount()){
					csvLine = csvLine + metadata.getColumnName(i) + ",";						
				}
				else {
					csvLine = csvLine + metadata.getColumnName(i);
				}		
			}
			
			// write header line to the csv file		
			csvOut.write(csvLine+"\n"); 
			csvLine = "";
			
			%> </tr> <%
			
			// Loop through the SQL results to add a row for each record
			int rowIndex = 1;
			String cellvalue = "";
			while(results.next())
			{
				%> <tr> <%
							
				// Loop through each column to add its data to the row cell-by-cell
				for(int i=1; i<metadata.getColumnCount()+1; i++)
				{							
					// Get the value for this column from the recordset, casting nulls to a valid string value
					Object value = results.getObject(i);
					if(value == null)
						cellvalue = "[null]";
				
					if (value instanceof Double) {
						int intvalue = (int)Math.floor(((Double)value).doubleValue());
						Integer toprint = new Integer(intvalue);
						cellvalue= String.format("%,d", toprint); //toprint.toString(); 
					}
					else if (value instanceof Integer || value instanceof Long ||value instanceof Short ) {
						cellvalue= String.format("%,d", value); 
					}
					else
						cellvalue= value.toString();
						
					%><td align="right"><%= cellvalue %></td>  <td>&nbsp;</td> <%					
					
					//Construct a csv file line of data				
					if (i<metadata.getColumnCount()) 
						{csvLine = csvLine + value + ",";						
						}
					else
						{ csvLine = csvLine + value;
						}
								
				} // end of "for(int i=1; i<metadata.getColumnCount()+1; i++)"
												
				rowIndex++;
				
			// write a new csv line
				csvOut.write(csvLine+"\n"); 
				csvLine = "";
				%> </tr> <%	
			}// end of "while(results.next())"
			
			// Flush all the data to the csv file and close it.
			csvOut.flush();
			csvOut.close();
			csvLine = "";
			csvOut = null;				
		}
		 catch (Exception ex)
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
	} //end of "if(sql.trim().length() > 0)"
	else
	{
	%>	
		   <tr>
			<td>
				<em>Enter bellow a SQL statement and press 'Execute' to see the results.</em>
				<br />
				<textarea name="sql" rows=12 cols=85 class="querytxt"><%= sql %></textarea>
				<br />
				<input class=button type=submit value=Execute> 
			</td>
		  </tr>
	<%
	} //end of "if(sql.trim().length() > 0){...} else "
%>
</table>
</form>
</body>
</html>

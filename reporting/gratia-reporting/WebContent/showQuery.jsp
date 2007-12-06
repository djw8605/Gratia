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
<title>Gratia Accounting</title>

<script type="text/javascript">

var currentFramePath = findPath(self);

function clearReportFrame() {
	if (currentFramePath != 'top')
	   parent.reportFrame.location = "about:blank"; 
}

function clearParamFrame() {
	if (currentFramePath != 'top')
	   parent.paramFrame.location = "about:blank"; 
}

function findPath(currentFrame) {
	var path = "";
	while (currentFrame != top) {
		path = "."+currentFrame.name+path;
		currentFrame = currentFrame.parent;
	}
	return "top"+path;
}

function writeTop (text) {

	if (currentFramePath == 'top')
	   document.write(text);
	else
	   parent.paramFrame.document.write(text);
}

function writeBottom (text) {

	if (currentFramePath == 'top')
	   document.write(text);
	else
	   parent.reportFrame.document.write(text);
}

function closeAll () {

	if (currentFramePath == 'top')
	   document.close();
	else
	{
	   parent.reportFrame.document.close();
	   parent.paramFrame.document.close();
	}
}

</script>

</head>
<body>

<script type="text/javascript">
	writeTop('<form action="">');
</script>
<%
	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");	

	String viewerPath = null;
	String csvFileName = null;


	String ReportTitle = request.getParameter("ReportTitle");
	if (ReportTitle != null)
   	{
%>

<script type="text/javascript">
	writeTop('<div align="left" class="reportTitle"><%= ReportTitle%><\/div><br \/> <input type="hidden" id="ReportTitle" name="ReportTitle" value="<%= ReportTitle%>">');
</script>

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
			// for displaying using document.write must replace "'" with "\'" 
			// and eliminate line feeds, carriage returns and multiple spaces
			sql = sql.replace("'", "\\'").replace("\n", " ").replace("\r", " ").trim();
			
			StringBuffer sb = new StringBuffer(sql.length());
			char c;
			boolean firstBlank = false;
			for(int i = 0; i < sql.length(); ++i) 
			{
				c = sql.charAt(i);
			            
				if (c == ' ' && i>1 && (sql.charAt(i-1) != ' '))
			            	firstBlank = true;
			        else
			            	firstBlank = false;			        	
			       			                        
				if (c != ' ' || firstBlank) 
					sb.append(c);
			}
			sql = sb.toString();
			%>
			<script type="text/javascript">
				writeTop('<table class="query"> <tr> <td> <em>Enter bellow a SQL statement and press "Execute Query" to see the results.<\/em> <br \/>');
				writeTop('<textarea name="sql" rows="8" cols="85"  class="querytxt"><%= sql %><\/textarea> <\/td>');
				writeTop('<td> <input class= "button" type="submit" value="Execute Query" onclick="clearReportFrame();"> <br \/>');
				writeTop('<p ><input  class= "button" type="button" value="Export Data (csv)" onClick="window.open(\'downloadFile.jsp?csvFile=<%= csvFileName %>\', \'Gratia\');"><\/p> <br \/> <\/td> <\/tr> <\/table>');
			</script>
			<%
			// Add a column to the table for each column in the resultset

			String csvLine = "";
			%>
			<script type="text/javascript">
				writeBottom('<hr> <table class="query"> <tr>');
			</script>

			<%
			for(int i=1; i<metadata.getColumnCount()+1; i++)
			{
			%> 
			<script type="text/javascript">
				writeBottom('<td align="right"><strong><%= metadata.getColumnName(i) %><\/strong><\/td> <td>&nbsp;<\/td>');
			</script>
			<%		
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
			%>
			<script type="text/javascript">
				writeBottom('<\/tr>');
			</script>
			<%
			// Loop through the SQL results to add a row for each record
			String cellvalue = "";
			while(results.next())
			{
			%>
			<script type="text/javascript">
				writeBottom('<tr>');
			</script>
			<%			
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
					else if (value instanceof Integer || value instanceof Long ||value instanceof Short) {
						cellvalue= String.format("%,d", value);
					}
					else
						cellvalue= value.toString();
					%>
				<script type="text/javascript">
					writeBottom('<td align="right"><%= cellvalue %><\/td>  <td>&nbsp;<\/td> ');
				</script>
				<%			
					//Construct a csv file line of data				
					if (i < metadata.getColumnCount()) 
					{
						csvLine = csvLine + value + ",";						
					}
					else
					{
						csvLine = csvLine + value;
					}
				} // end of "for(int i=1; i<metadata.getColumnCount()+1; i++)"
					
				// write a new csv line
				csvOut.write(csvLine + "\n"); 
				csvLine = "";
				%>
				<script type="text/javascript">
					writeBottom('<\/tr> ');
				</script>  
				<%
			}// end of "while(results.next())"

			// Flush all the data to the csv file and close it.
			csvOut.flush();
			csvOut.close();
			csvLine = "";
			csvOut = null;
			%>
			<script type="text/javascript">
				writeBottom('<\/table> <hr>');
			</script> 
			<%
		}
		 catch (Exception ex) {
			throw new Exception(ex);
		} finally {
			try {
				cnn.close();
			} catch(Exception ex) {}		
			try {
				statement.close();
			} catch(Exception ex) {}		
			try {
				results.close();
			} catch(Exception ex) {}	
	
			metadata = null;
			results = null;
			statement = null;
			cnn = null;
		}
	} // end of "if(sql.trim().length() ...)"
	else
	{
	%>
	
	<script type="text/javascript">
		writeTop('<table class="query"> <tr> <td> <em>Enter bellow a SQL statement and press "Execute Query" to see the results.<\/em> <br \/>');
		writeTop('<textarea name="sql" rows="8" cols="85"  class="querytxt"><%= sql %><\/textarea> <\/td>');
		writeTop('<td> <input class= "button" type="submit" value="Execute Query" onclick="clearReportFrame();"> <br \/> <\/td> <\/tr> <\/table>');
	</script>
	<%
	} //end of "if(sql.trim().length() > 0){...} else "
%>

<script type="text/javascript">
	writeTop('<\/form>');
	closeAll();
</script>

</body>
</html>
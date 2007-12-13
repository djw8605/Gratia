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

<link href="stylesheet.css" type="text/css" rel="stylesheet">
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
		path = "." + currentFrame.name+path;
		currentFrame = currentFrame.parent;
	}
	return "top" + path;
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
	writeTop('<link href="stylesheet.css" type="text\/css" rel="stylesheet">');
	writeBottom('<link href="stylesheet.css" type="text\/css" rel="stylesheet">');
	writeTop('<form action="">');
</script>
<%
	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");

	String viewerPath = null;
	String csvFileName = null;


	String title = request.getParameter("ReportTitle");
	
	if (title != null)
	{
		title = title.replace("'", "\\'").replace("/", "\\/'");
		%>
		<script type="text/javascript">
			writeTop('<div class="reportTitle"><%= title %><\/div><br>');
			writeTop('<input type="hidden" id="ReportTitle" name="ReportTitle" value="<%= title %>">');
		</script>
		<%
	}
	else
		title = "";

	String inSQL = request.getParameter("sql");

	if (inSQL == null)
	{
		// Get the query from the trace table
		String traceTableKey = request.getParameter("TraceTableKey");

		try 
		{
			GenerateSQL generator = new GenerateSQL(inSQL, traceTableKey);
			inSQL = generator.generate();
		}
		catch (Exception ignore) { }
	}

	if(inSQL == null)
		inSQL = "";

	if(inSQL.trim().length() > 0)
	{
		// Do not put in the try/catch block the output to the screen - query and buttons
		// in case of error they will be displayed and the user can correct the query
		// Using document.write so: 
		// must replace "'" with "\'" and eliminate line feeds, carriage returns and multiple spaces

		String sql = inSQL.replace("'", "\\'").replace("/", "\\/'").replace("\n", " ").replace("\r", " ").trim();

		// Eliminate multiple blanks
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

		// Construct the csv file name. Do not open any files at this point
		String tempCsvPath = "";
		if (System.getProperty( "os.name" ).indexOf("Windows") != -1)
			tempCsvPath = "";
		else
			tempCsvPath = reportingConfiguration.getCsvHome();

		String somethingUnique = "" + System.currentTimeMillis();
		csvFileName = tempCsvPath + "gratia_report_data_" + somethingUnique + ".csv";

		%>
		<script type="text/javascript">
			writeTop('<table class="query"> <tr> <td> <em>Enter bellow a SQL statement and press "Execute Query" to see the results.<\/em> <br \/>');
			writeTop('<textarea name="sql" rows="8" cols="85"  class="querytxt"><%= sql %><\/textarea> <\/td>');
			writeTop('<td> <input class= "button" type="submit" value="Execute Query" onclick="clearParamFrame();clearReportFrame();"> <br \/>');
			writeTop('<p ><input  class= "button" type="button" value="Export Data (csv)" onClick="window.open(\'downloadFile.jsp?csvFile=<%= csvFileName %>\', \'Gratia\');"><\/p> <br \/> <\/td> <\/tr> <\/table>');
		</script>

		<%
		Connection cnn = null;
		Statement statement = null;
		ResultSet results = null;
		ResultSetMetaData metadata = null;

		try
		{
			// Create, if it does not exist, a temporary directory to hold the csv files. 
			// TO DO: Keep only last week's files and delete the rest.

			java.io.File newCsvDir=new java.io.File(tempCsvPath);
			if (!newCsvDir.exists())
				newCsvDir.mkdirs();
			newCsvDir = null;

			BufferedWriter csvOut = new BufferedWriter(new FileWriter(csvFileName, true));

			// Connect to the database and execute the query
			Class.forName("com.mysql.jdbc.Driver").newInstance();

			cnn = DriverManager.getConnection(reportingConfiguration.getDatabaseURL(), reportingConfiguration.getDatabaseUser(), reportingConfiguration.getDatabasePassword());
			// cnn = DriverManager.getConnection("jdbc:mysql://gratia-db01.fnal.gov:3320/gratia_itb", "reader", "reader");
			statement = cnn.createStatement();
			results = statement.executeQuery(inSQL);
			metadata = results.getMetaData();
			
			// Get the number of records (rows) in the data). No direct way for this. 
			// So go to the last row, get the row number and then back to top
			results.last();
			int numRows = results.getRow();
			results.beforeFirst();
			
			// Get the number of columns
			int numColumns = metadata.getColumnCount();

			%>
			<script type="text/javascript">
				writeBottom('<table class="query"> <tr> <td align="left" colspan="<%= 2*numColumns + 1 %>"><hr color="#E07630"><strong>Number of records = <%= numRows %><\/strong><\/td><\/tr>');
			</script>
			<%
			if (numRows > 0)
			{
				%>
				<script type="text/javascript">
					writeBottom('<tr><td align="right"><strong> # <\/strong><\/td>');
				</script>
				<%
			}
			
			String csvLine = "";
			csvOut.write("Number of records = " + numRows + "\n\n");
			for(int i = 1; i < numColumns + 1; i++)
			{
				// Put header row only if there are data
				if (numRows > 0)
				{
					%> 
					<script type="text/javascript">
						writeBottom('<td>&nbsp;<\/td> <td align="right"><strong><%= metadata.getColumnName(i) %><\/strong><\/td> ');
					</script>
					<%
				}
				// Construct the csv file header line
				if (i < numColumns)
					csvLine = csvLine + metadata.getColumnName(i) + ",";
				else 
					csvLine = csvLine + metadata.getColumnName(i);
			}
			if (numRows > 0)
			{
				%>
				<script type="text/javascript">
					writeBottom('<\/tr>');
				</script>
				<%
			}
			// write header line to the csv file
			csvOut.write(csvLine+"\n");
			csvLine = "";

			// Loop through the SQL results to add a row for each record
			String cellvalue = "";
			int r = 1;
			while(results.next())
			{
				%>
				<script type="text/javascript">
					writeBottom('<tr><td align="right"><%= r %><\/td>  ');
				</script>
				<%
				// Loop through each column to add its data to the row cell-by-cell
				r++;
				for(int i = 1; i < numColumns + 1; i++)
				{
					// Get the value for this column from the recordset, casting nulls to a valid string value
					Object value = results.getObject(i);
					if(value == null)
						cellvalue = "[null]";

					if (value instanceof Double) 
					{
						int intvalue = (int)Math.floor(((Double)value).doubleValue());
						Integer toprint = new Integer(intvalue);
						cellvalue= String.format("%,d", toprint); //toprint.toString(); 
					}
					else if (value instanceof Integer || value instanceof Long ||value instanceof Short) 
						cellvalue= String.format("%,d", value);
					else
						cellvalue= value.toString();
					%>
					<script type="text/javascript">
						writeBottom('<td>&nbsp;<\/td> <td align="right"><%= cellvalue %><\/td>');
					</script>
					<%
					//Construct a csv file line of data
					if (i < numColumns) 
						csvLine = csvLine + value + ",";
					else
						csvLine = csvLine + value;
				} // end of "for(int i = 1; i < numColumns + 1; i++)"
					
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
				writeBottom('<tr> <td align="left" colspan="<%= 2*numColumns + 1 %>"><hr color="#E07630"><\/td><\/tr>');
				writeBottom('<\/table>');
			</script> 
			<%
		}
		catch (com.mysql.jdbc.exceptions.MySQLSyntaxErrorException ex)
		{
			String msg = ex.getMessage();
			%>
			<table class="query" border="0"><tr> <td align="left" ><hr color="#E07630"></td></tr>
			<tr><td><font color="#E07630"> 
				There is an SQL syntax error in the query: <br>
				&nbsp;&nbsp;&nbsp;&nbsp;<strong><%= msg %></strong> <br>
			</font></td></tr><tr> <td align="left" ><hr color="#E07630"></td></tr></table>
			<script type="text/javascript">
				if (currentFramePath == 'top')
					document.close();
				else
				{
					parent.reportFrame.document.close();
					parent.paramFrame.document.close();
				}
			</script> 
			<%
			// DO NOT: throw new Exception(ex);
		}
		catch (Exception ex) 
		{
			String msg = ex.getMessage();
			%>
			<table class="query" border="0"><tr> <td align="left" ><hr color="#E07630"></td></tr>
			<tr><td><font color="#E07630"> 
				The following error occur while executing the query: <br>
				&nbsp;&nbsp;&nbsp;&nbsp;<strong><%= msg %></strong> <br>
				<em><%= ex %> </em>
			</font></td></tr><tr> <td align="left" ><hr color="#E07630"></td></tr></table>
			<script type="text/javascript">
				if (currentFramePath == 'top')
					document.close();
				else
				{
					parent.reportFrame.document.close();
					parent.paramFrame.document.close();
				}
			</script> 
			<%
			// DO NOT: throw new Exception(ex);
		} 
		finally 
		{
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
		writeTop('<textarea name="sql" rows="8" cols="85"  class="querytxt"><\/textarea> <\/td>');
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

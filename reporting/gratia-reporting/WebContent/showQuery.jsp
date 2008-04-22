<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="net.sf.gratia.reporting.*"
    import="java.sql.*"
    import="java.io.*"
    import="java.util.Date"
    import="java.text.SimpleDateFormat"
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
		title = title.replace("'", "\\'").replace("/", "\\/");
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
		// must replace ' -> \' , / -> \/
		// and eliminate line feeds, carriage returns and multiple spaces

		String sql = inSQL.replace("'", "\\'").replace("/", "\\/").replace("\n", " ").replace("\r", " ").trim();

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

		String tempCsvPath = "";
		String somethingUnique = "" + System.currentTimeMillis();
		tempCsvPath = reportingConfiguration.getCsvHome();
		csvFileName = tempCsvPath + "gratia_report_data_" + somethingUnique + ".csv";

		%>
		<script type="text/javascript">
			writeTop('<table class="query"> <tr> <td> <em>Enter below a SQL statement and press "Execute Query" to see the results.<\/em> <br \/>');
			writeTop('<textarea name="sql" rows="8" cols="85"  class="querytxt"><%= sql %><\/textarea> <\/td>');
			writeTop('<td> <input class= "button" type="submit" value="Execute Query" onclick="clearParamFrame();clearReportFrame();"> <br \/>');
			writeTop('<p ><input  class= "button" type="button" value="Export Data (csv)" onClick="window.open(\'ww=downloadFile.jsp?csvFile=<%= csvFileName %>\', \'Gratia\'); ww.close()"><\/p> <br \/> <\/td> <\/tr> <\/table>');
		</script>

		<%
		Connection cnn = null;
		Statement statement = null;
		ResultSet results = null;
		ResultSetMetaData metadata = null;

		try
		{
			// Create, if it does not exist, a temporary directory to hold the csv files.

			java.io.File newCsvDir=new java.io.File(tempCsvPath);
			if (!newCsvDir.exists())
				newCsvDir.mkdirs();
			newCsvDir = null;

			// Cleanup old temp csv files. Keep only last 12 hours files
			File f = new File(tempCsvPath);
			File [] fileObjects = f.listFiles();	// Get all files in the directory
			if (fileObjects.length != 0)
			{
				for (int j = 0; j < fileObjects.length; j++)
				{
					if(!fileObjects[j].isDirectory())
					{
						String fileName = fileObjects[j].getName();
						Date modDate = new Date(fileObjects[j].lastModified());
						modDate = new Date(modDate.getTime());
						Date now = new Date();
						now = new Date(now.getTime() - (12 * 60 * 60 * 1000));

						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
						if ((fileName.startsWith("gratia_report_data_") && fileName.endsWith(".csv")) && (modDate.before(now) || modDate.equals(now)))
								fileObjects[j].delete();
					}
				}
			}

			BufferedWriter csvOut = new BufferedWriter(new FileWriter(csvFileName, true));

			// Connect to the database and execute the query
			Class.forName("com.mysql.jdbc.Driver").newInstance();

			cnn = DriverManager.getConnection(reportingConfiguration.getDatabaseURL(), reportingConfiguration.getDatabaseUser(), reportingConfiguration.getDatabasePassword());
			statement = cnn.createStatement();
			results = statement.executeQuery(inSQL);
			metadata = results.getMetaData();

			// Get the number of columns
			int numColumns = metadata.getColumnCount();

			// Loop through the SQL results to add a row for each record

			%>
			<script type="text/javascript">
				writeBottom('<table class="query"> <tr> <td align="left" colspan="<%= 2*numColumns + 1 %>"><hr color="#FF8330"><\/td><\/tr>');
			</script>
			<%
			String cellvalue = "";
			String csvLine = "";
			int r = 0;
			while(results.next())
			{
				if (r == 0)
				{
					%>
					<script type="text/javascript">
						writeBottom('<tr><td align="right"><strong> # <\/strong><\/td>');
					</script>
					<%
					for(int i = 1; i < numColumns + 1; i++)
					{
						// Put header row only if there are data
						%>
						<script type="text/javascript">
							writeBottom('<td>&nbsp;<\/td> <td align="right"><strong><%= metadata.getColumnName(i) %><\/strong><\/td> ');
						</script>
						<%

						// Construct the csv file header line
						if (i < numColumns)
							csvLine = csvLine + metadata.getColumnName(i) + ",";
						else
							csvLine = csvLine + metadata.getColumnName(i);
					}
					%>
					<script type="text/javascript">
						writeBottom('<\/tr>');
					</script>
					<%

					csvOut.write(csvLine+"\n");
					csvLine = "";
				}
				r++;
				%>
				<script type="text/javascript">
					writeBottom('<tr><td align="right"><%= r %><\/td>');
				</script>
				<%
				// Loop through each column to add its data to the row cell-by-cell

				for(int i = 1; i < numColumns + 1; i++)
				{
					// Get the value for this column from the recordset, casting nulls to a valid string value
					Object value = results.getObject(i);
					if(value == null)
						cellvalue = "[null]";

					else if (value instanceof Double)
					{
						int intvalue = (int)Math.floor(((Double)value).doubleValue());
						Integer toprint = new Integer(intvalue);
						cellvalue= String.format("%,d", toprint); //toprint.toString();
					}
					else if (value instanceof Integer || value instanceof Long ||value instanceof Short)
						cellvalue= String.format("%,d", value);
					else
						cellvalue= value.toString();

					// Escape quotes and slashes, so that the value is displayed correctly
					cellvalue = cellvalue.replace("'", "\\'").replace("/", "\\/");
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
				writeBottom('<tr> <td align="left" colspan="<%= 2*numColumns + 1 %>"><strong>Number of records = <%= r %><\/strong><hr color="#FF8330"><\/td><\/tr>');
				writeBottom('<\/table>');
			</script>
			<%
		}
		catch (com.mysql.jdbc.exceptions.MySQLSyntaxErrorException ex)
		{
			String msg = ex.toString();
			%>
			<table class="query" border="0"><tr> <td align="left" ><hr color="#FF8330"></td></tr>
			<tr><td><font color="#FF8330">
				There is an SQL syntax error in the query: <br>
				&nbsp;&nbsp;&nbsp;&nbsp;<strong><%= msg %></strong> <br>
			</font></td></tr><tr> <td align="left" ><hr color="#FF8330"></td></tr></table>
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
			String msg = ex.toString();
			%>
			<table class="query" border="0"><tr> <td align="left" ><hr color="#FF8330"></td></tr>
			<tr><td><font color="#FF8330">
				The following error occur while executing the query: <br>
				&nbsp;&nbsp;&nbsp;&nbsp;<strong><%= msg %></strong> <br>
				<em><%= ex %> </em>
				<% ex.printStackTrace(); %>
			</font></td></tr><tr> <td align="left" ><hr color="#FF8330"></td></tr></table>
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
		writeTop('<table class="query"> <tr> <td> <em>Enter below a SQL statement and press "Execute Query" to see the results.<\/em> <br \/>');
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

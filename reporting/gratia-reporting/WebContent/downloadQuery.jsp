<%@ page language="java" contentType="text/html; charset=windows-1252"
    import="java.util.*, java.io.*"
    import="java.sql.*"
    import="net.sf.gratia.reporting.*"
%>

<%
   ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");
   String inSQL = request.getParameter("sql");

   if (inSQL == null)
   {
        %>No query specified<%
	// Get the query from the trace table
	//String traceTableKey = request.getParameter("TraceTableKey");

	//try
	//{
	//	GenerateSQL generator = new GenerateSQL(inSQL, traceTableKey);
	//	inSQL = generator.generate();
	//}
	//catch (Exception ignore) { }
   } else {

	// - Set the http content type to "application/download"
	// - Initialize the http content-disposition header to
	//    indicate a file attachment with a default filename
	//    the same as the input file.
	// - Validate the data after download
		 
		 
	response.setContentType("application/download"); 
   	response.setHeader("Content-Disposition","Attachment; filename=\"gratia.csv\"");
	response.setHeader("cache-control", "must-revalidate");

	// response.setHeader("cache-control", "no-cache"); // IE does not work if set
	// response.setDateHeader ("Expires", 0);	    // IE does not work if set
			
	ServletOutputStream fileOutputStream = response.getOutputStream();

	// Connect to the database and execute the query
	Class.forName("com.mysql.jdbc.Driver").newInstance();

	Connection cnn = null;
	Statement statement = null;
	ResultSet results = null;
	ResultSetMetaData metadata = null;

	cnn = DriverManager.getConnection(reportingConfiguration.getDatabaseURL(), reportingConfiguration.getDatabaseUser(), reportingConfiguration.getDatabasePassword());
	statement = cnn.createStatement();
	results = statement.executeQuery(inSQL);
	metadata = results.getMetaData();

	// Get the number of columns
	int numColumns = metadata.getColumnCount();

	// Loop through the SQL results to add a row for each record
	String csvLine = "";
	int r = 0;
	while(results.next())
	{
		if (r == 0)
		{
			for(int i = 1; i < numColumns + 1; i++)
			{
				// Construct the csv file header line
				if (i < numColumns)
					csvLine = csvLine + metadata.getColumnName(i) + ",";
				else
					csvLine = csvLine + metadata.getColumnName(i);
			}
			fileOutputStream.println(csvLine);
			csvLine = "";
		}
		r++;
		// Loop through each column to add its data to the row cell-by-cell

		for(int i = 1; i < numColumns + 1; i++)
		{
			// Get the value for this column from the recordset, casting nulls to a valid string value
			Object value = results.getObject(i);

			//Construct a csv file line of data
                        if (value instanceof String)
				csvLine = csvLine + '"' + value + '"';
			else 
				csvLine = csvLine + value;

			if (i < numColumns)
				csvLine = csvLine + ",";

		} // end of "for(int i = 1; i < numColumns + 1; i++)"

		// write a new csv line
		fileOutputStream.println(csvLine);
		csvLine = "";
	}// end of "while(results.next())"


	// Flush all the data to the csv file and close it.
	fileOutputStream.flush();
	fileOutputStream.close();
	csvLine = "";
	fileOutputStream = null;
    }
%>

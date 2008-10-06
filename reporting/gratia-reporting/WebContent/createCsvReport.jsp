<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
		pageEncoding="ISO-8859-1"
	import="java.util.Calendar"
	import="java.text.SimpleDateFormat"
	import="java.sql.*"
	import="java.io.*"
	import="java.util.Date"
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Gratia Accounting</title>
</head>

<body>
<%
/*
 * Accepts the following parameters (* denotes default value if no parameter or wrong value):
 *   o _dateRange    =  7-days  : previous 7 days  (today - 7 days)
 *                   * 14-days  : previous 15 day  (today -14 days)
 *                      1-week  : previous week    (previous calendar week Monday - Sunday)
 *                      2-week  : previous 2 weeks (previous calendar 2 weeks Monday - Sunday)
 *                      3-week  : previous 3 weeks (previous calendar 3 weeks Monday - Sunday)
 *                      4-week  : previous 4 weeks (previous calendar 4 weeks Monday - Sunday)
 *                      0-month : current month    (1st of current month - today)
 *                      1-month : current month    (previous calendar month)
 *                      2-month : current month    (previous calendar 2 months)
 *                      3-month : current month    (previous calendar 3 months)
 *                      4-month : current month    (previous calendar 4 months)
 *                      5-month : current month    (previous calendar 5 months)
 *                      6-month : current month    (previous calendar 6 months)
 *                      7-month : current month    (previous calendar 7 months)
 *                      8-month : current month    (previous calendar 8 months)
 *                      9-month : current month    (previous calendar 9 months)
 *                     10-month : current month    (previous calendar 10 months)
 *                     11-month : current month    (previous calendar 11 months)
 *                     12-month : current month    (previous calendar 12 months)
 *                      1-year  : previous year    (previous calendar year)
 *
 *   o _timeUnit     = * seconds :      1 second
 *                       minutes :     60 seconds
 *                       hours   :   3600 seconds
 *                       days    :  86400 seconds
 *                       weeks   : 604800 seconds
 *
 *   o _dateGrouping = * day
 *                       week
 *                       month
 *                       year
 *
 *   o _execute      = csv file name including path
 *                     * temporary file in csvHome
 *
 * calls the following stored procedure:
 * PROCEDURE reports (	userName, userRole, fromdate, todate, timeUnit, dateGrouping, 
 *						groupBy, orderBy, resourceType, VOs, Sites, Probes, Ranked, selType)
 * 	passing a "null" as an argument results is using the procedure default value
 */
try
{
	net.sf.gratia.reporting.ReportingConfiguration reportingConfiguration = new net.sf.gratia.reporting.ReportingConfiguration();
	reportingConfiguration.loadReportingConfiguration(request);

	// Define the date format to be used in this jsp
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

	String selectedRange = request.getParameter("_dateRange");
	if (selectedRange == null)
		selectedRange ="14-days";
	selectedRange = selectedRange.toLowerCase();

	if (selectedRange.indexOf("-") < 0)
		selectedRange ="14-days";
	if ((selectedRange.indexOf("days") < 0) && (selectedRange.indexOf("week") < 0) && (selectedRange.indexOf("month") < 0) && (selectedRange.indexOf("year") < 0))
		selectedRange ="14-days";

	// The format of the range is "xx-nnnn" where xx is a number and nnnn={days, week, month}
	// So simply split the string to get the number.
	String[] range;
	range = selectedRange.split("-");

	// Convert the string to its negative integer so that
	// the calendar.add operation will actually subtract correctly
	int rangeNum = -Integer.parseInt(range[0]);

	// Initiallize calendar variables to be today
	Calendar today = Calendar.getInstance();
	Calendar startDate = (Calendar) today.clone();
	Calendar endDate   = (Calendar) today.clone();


	if (selectedRange.indexOf("days") > 0)
	{
		startDate.add(Calendar.DAY_OF_YEAR, rangeNum);		// subtract the requested days
	}
	else if (selectedRange.indexOf("week") > 0)
	{
		// Set the end time to be the end of the previous week (previous Sunday).
		endDate.add(Calendar.DAY_OF_YEAR, - (endDate.get(Calendar.DAY_OF_WEEK) - 1));
		startDate = (Calendar) endDate.clone();
		startDate.add(Calendar.DAY_OF_YEAR, 1);		// make startdate start on Monday

		startDate.add(Calendar.WEEK_OF_YEAR, rangeNum);		// subtract the requested weeks
	}
	else if (selectedRange.indexOf("month") > 0)
	{
		// Set the end time to be the end of the previous month.
			endDate.add(Calendar.DAY_OF_YEAR, -endDate.get(Calendar.DAY_OF_MONTH));

		// Set the start date to be the first of this month.
		startDate = (Calendar) endDate.clone();
		startDate.add(Calendar.DAY_OF_YEAR, 1);			// get the first of this month
		startDate.add(Calendar.MONTH, rangeNum);		// subtract the requested months

		// Selection was "This month", so end date is today
		if (rangeNum == 0)
			endDate = (Calendar) today.clone();
	}
	else if (selectedRange.indexOf("year") > 0)
	{
		// Set the end time to be the start (yyyy-01-01) and end (yyyy-12-31) of the previous year.
		// Note: Month numbering starts from 0

			startDate = (Calendar) endDate.clone();
			startDate.add(Calendar.YEAR, rangeNum);					// subtract the requested years
			startDate.set(startDate.get(Calendar.YEAR), 0, 1);		// set the start date to be Jan 1
			endDate.add(Calendar.DAY_OF_YEAR, -endDate.get(Calendar.DAY_OF_YEAR)); // set the end date to be Dec 31
	}

	// Set the session variables for later use
	String sDate = formatter.format(startDate.getTime());
	String eDate = formatter.format(endDate.getTime());

	String timeUnit = request.getParameter("_timeUnit");
	if (timeUnit == null)
		timeUnit ="seconds";
	timeUnit = timeUnit.toLowerCase();

	if (timeUnit.indexOf("second") >= 0)
		timeUnit = "1";
	else if (timeUnit.indexOf("minute") >= 0)
		timeUnit = "60";
	else if (timeUnit.indexOf("hour") >= 0)
		timeUnit = "3600";
	else if (timeUnit.indexOf("day") >= 0)
		timeUnit = "86400";
	else if (timeUnit.indexOf("week") >= 0)
		timeUnit = "604800";
	else
		timeUnit = "1";

	String dateGrouping = request.getParameter("_dateGrouping");
	if (dateGrouping == null)
		dateGrouping ="day";
	dateGrouping = dateGrouping.toLowerCase();

	if (dateGrouping.indexOf("day") >= 0)
		dateGrouping = "day";
	else if (dateGrouping.indexOf("week") >= 0)
		dateGrouping = "week";
	else if (dateGrouping.indexOf("month") >= 0)
		dateGrouping = "month";
	else if (dateGrouping.indexOf("year") >= 0)
		dateGrouping = "year";
	else
		dateGrouping = "day";
	
	String groupBy = "DateValue, UserName, SiteName, VOName";
	String orderBy = "UserName, SiteName, VOName, DateValue";

	String incsvFileName = request.getParameter("_csvFile");
	if (incsvFileName == null)
		incsvFileName = "";

	String sql = "call reports(null, null,'" + sDate +"', '" + eDate + "','" + timeUnit + "','" + dateGrouping + "','" + groupBy  + "','" + orderBy + "','batch', null, null, null, null, null)";
            
	String tempCsvPath = "";
	String csvFileName = "";
	if (incsvFileName.length() == 0)
	{
		// Create a temp file to hold the results in case the user wants to download it
		String somethingUnique = "" + System.currentTimeMillis();
		tempCsvPath = reportingConfiguration.getCsvHome();
		csvFileName = tempCsvPath + "gratia_report_data_" + somethingUnique + ".csv";
	}
	else
	{
		csvFileName = incsvFileName;
	}
	java.io.File ff = new java.io.File(csvFileName);
	tempCsvPath = ff.getParent() + "_csv/";
	String thisFile = ff.getName();
	csvFileName = tempCsvPath + thisFile;

	// if it does not exist create the directory to hold the csv file
	java.io.File newCsvDir = new java.io.File(tempCsvPath);
	if (!newCsvDir.exists())
		newCsvDir.mkdirs();
	newCsvDir = null;

	// Cleanup old temp csv files. Keep only last 24 hours files
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
				now = new Date(now.getTime() - (24 * 60 * 60 * 1000));
				// SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				if ((fileName.startsWith("gratia_report_data_") && fileName.endsWith(".csv")) && (modDate.before(now) || modDate.equals(now)))
						fileObjects[j].delete();
			}
		}
	}

	BufferedWriter csvOut = new BufferedWriter(new FileWriter(csvFileName, true));

	// Connect to the database and execute the query
	// Connect to the database
	Connection cnn = null;
	Statement statement = null;
	ResultSet results = null;
	ResultSetMetaData metadata = null;

	Class.forName("com.mysql.jdbc.Driver").newInstance();

	 cnn = DriverManager.getConnection(reportingConfiguration.getDatabaseURL(), reportingConfiguration.getDatabaseUser(), reportingConfiguration.getDatabasePassword());
	//cnn = DriverManager.getConnection("jdbc:mysql://gratia-vm02.fnal.gov:3320/gratia_penelope", "reader", "reader");

	statement = cnn.createStatement();
	results = statement.executeQuery(sql);
	metadata = results.getMetaData();

	// Get the number of columns
	int numColumns = metadata.getColumnCount();

	String cellvalue = "";
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
			csvOut.write(csvLine + "\n");
			csvLine = "";
		}
		r++;

		// Loop through each column to add its data to the row cell-by-cell
		for(int i = 1; i < numColumns + 1; i++)
		{
			// Get the value for this column from the recordset, casting nulls to a valid string value
			Object value = results.getObject(i);

			//Construct a csv file line of data
			if (i < numColumns)
				csvLine = csvLine + value + ",";
			else
				csvLine = csvLine + value;
		} // end of "for(int i = 1; i < numColumns + 1; i++)"

		// write a new csv line
		csvOut.write(csvLine + "\n");
		csvLine = "";
	}// end of "while(results.next())"

	// Flush all the data to the csv file and close it.
	csvOut.flush();
	csvOut.close();
	csvLine = "";
	csvOut = null;
	cnn.close();
	metadata = null;
	results = null;
	statement = null;
	cnn = null;
}
catch (Exception ex)
{
	ex.printStackTrace();
}
%>
</body>
</html>

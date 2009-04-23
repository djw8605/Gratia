package net.sf.gratia.administration;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.io.*;

import java.util.Properties;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;

import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;

import java.util.regex.*;
import java.lang.Math;

public class Status extends HttpServlet {
	XP xp = new XP();
	//
	// database related
	//
	String driver = "";
	String url = "";
	String user = "";
	String password = "";
	Connection connection;
	Statement statement;
	ResultSet resultSet;
	//
	// processing related
	//
	String html = "";
	String tableHeader = "";
	String tableSection = "";
	String row = "";
	StringBuffer buffer = new StringBuffer();
	static Pattern yesMatcher =
		Pattern.compile("^[YyTt1]");
	static Pattern colgroupDefn =
		Pattern.compile("^\\s*<colgroup class=\"tablespan\".*?/>\\s*?\n",
				Pattern.MULTILINE + Pattern.DOTALL);
	static Pattern colgroupHdr =
		Pattern.compile("^\\s*<th\\s+.*?#RecordType#</th>\\s*?\n",
				Pattern.MULTILINE);
	static Pattern tableHead2 =
		Pattern.compile("^(\\s*<tr class=\"tablehead2\">\\s*?\n)" +
				"(.*?<th.*?\n)(\\s*</tr>\n)",
				Pattern.MULTILINE + Pattern.DOTALL);
	static Pattern timeIntervalRow =
		Pattern.compile("^(\\s*<tr class=\"timeintervalrow\">\\s*?\n)" +
				"(.*?<td.*?#timeinterval#.*?</td>\\s*?\n)" +
				"(.*?<td.*?#tabledatum#.*?</td>\\s*?\n)(\\s*</tr>\n)",
				Pattern.MULTILINE + Pattern.DOTALL);
	static Pattern qsizeblock =
		Pattern.compile("<tr class=\"qsize\">.*?</tr>",
				Pattern.MULTILINE + Pattern.DOTALL);
	boolean detailedDisplay = false;

	//
	// globals
	//
	HttpServletRequest request;
	HttpServletResponse response;
	boolean initialized = false;
	Properties props;
	String message = null;
	int nHoursBack = 6;
	String color_a = "#ffffff";
	String color_b = "#cccccc";

	//
	// support
	//
	String dq = "\"";
	String comma = ",";
	String cr = "\n";

	//
	// matching
	//


	class TableStatusInfo {
		int nRecords = 0;
		int nDups = 0;
		int nSQLErrors = 0;
		int nParse = 0;
		int nOther = 0;

		public TableStatusInfo(int nRecords) {
			this.nRecords = nRecords;
		}
		public TableStatusInfo(int nRecords, int nDups, int nSQLErrors, int nParse, int nOther) {
			this.nRecords = nRecords;
			this.nDups = nDups;
			this.nSQLErrors = nSQLErrors;
			this.nParse = nParse;
			this.nOther = nOther;
		}
		public int nRecords() {
			return this.nRecords;
		}
		public int nDups() {
			return this.nDups;
		}
		public int nSQLErrors() {
			return this.nSQLErrors;
		}
		public int nParse() {
			return this.nParse;
		}
		public int nOther() {
			return this.nOther;
		}
		public void nRecords(int in) {
			this.nRecords = in;
		}
		public void nDups(int in) {
			this.nDups = in;
		}
		public void nSQLErrors(int in) {
			this.nSQLErrors = in;
		}
		public void nParse(int in) {
			this.nParse = in;
		}
		public void nOther(int in) {
			this.nOther = in;
		}

	}

	public void init(ServletConfig config) throws ServletException 
	{
	}

	public void openConnection()
	{
		try {
			props = Configuration.getProperties();
			driver = props.getProperty("service.mysql.driver");
			url = props.getProperty("service.mysql.url");
			user = props.getProperty("service.mysql.user");
			password = props.getProperty("service.mysql.password");
		}
		catch (Exception ignore) {
		}
		try {
			Class.forName(driver).newInstance();
			connection = DriverManager.getConnection(url,user,password);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void closeConnection() {
		try {
			connection.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		openConnection();
		this.request = request;
		this.response = response;
		
		String uriPart = request.getRequestURI();
		int slash2 = uriPart.substring(1).indexOf("/") + 1;
		uriPart = uriPart.substring(slash2);
		String queryPart = request.getQueryString();
		if (queryPart == null)
			queryPart = "";
		else
			queryPart = "?" + queryPart;

		request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);
		
		String wantDetails = request.getParameter("wantDetails");
		Logging.debug("Got parameter wantDetails=" + wantDetails);
		if (wantDetails != null) {
			if (yesMatcher.matcher(wantDetails).matches()) {
				detailedDisplay = true;
			} else {
				detailedDisplay = false;
			}
		}
		String lastHours = request.getParameter("lastHours");
		Logging.debug("Got parameter lastHours=" + lastHours);
		if ((lastHours != null) && (lastHours != "")) {
			try {
				nHoursBack = Integer.parseInt(lastHours);
			}
			catch (NumberFormatException e) {
			}
		}
		setup();
		process();
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0
		PrintWriter writer = response.getWriter();
		writer.write(html);
		writer.flush();
		writer.close();
		closeConnection();
	}

	public static String DisplayInt(Integer value) {
		if (value == null)
			return "n/a";
		else 
			return value.toString();
	}

	public void setup() {
		html = xp.get(request.getRealPath("/") + "status.html");
	}

	public void process() {
		HashMap<String, HashMap<Integer, TableStatusInfo> > tableInfo = new
		HashMap<String, HashMap<Integer, TableStatusInfo> >(5);

		Matcher m = null;

		int index = 0;
		String command = "";
		buffer = new StringBuffer();
		String dq = "'";

		int nPeriods = nHoursBack + 3;
		Integer nDataCols = 5;
		if (!detailedDisplay) nDataCols = 2;
		html = html.replaceAll("#nDcols#", nDataCols.toString());

		try {
			// Start transaction so numbers are consistent.
			connection.setAutoCommit(false);
			// Get list of _Meta tables in this database
			command = "select table_name from information_schema.tables " +
			"where table_schema = Database() and table_name like '%_Meta'" +
			" order by table_name;";
			Logging.log("command: " + command);
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);
			while(resultSet.next()) {
				String table_name = resultSet.getString(1);
				if (table_name.equals("ProbeDetails_Meta")) continue; // Not interested
				int end_index = table_name.lastIndexOf("_Meta");
				String base_table = table_name.substring(0,end_index);
				// Only display table if we have any records
				command = "select * from " + base_table + " limit 1";
				Statement tableUseCheck = connection.prepareStatement(command);
				ResultSet tableUseResult = tableUseCheck.executeQuery(command);
				if (tableUseResult.next()) {
					tableInfo.put(base_table,
							new HashMap<Integer, TableStatusInfo>(nPeriods));
				}
				tableUseResult.close();
				tableUseCheck.close();
			}
			resultSet.close();
			statement.close();

			// Now for each table, get record information followed by
			// error info
			TreeSet keySet = new TreeSet(tableInfo.keySet());
			int hourly_time_limit = Math.max(nHoursBack, 24);
			for (Iterator x = keySet.iterator(); x.hasNext();) {
				String table_name = (String) x.next();
				HashMap<Integer, TableStatusInfo> thisTableInfo = tableInfo.get(table_name);
				TableStatusInfo dayTableStatus = new TableStatusInfo(0);
				thisTableInfo.put(nHoursBack, dayTableStatus);
				////////////////////////////////////
				// For each of last 24 hours
				////////////////////////////////////

				// nRecords
				command = "select timestampdiff(HOUR, M.ServerDate, " +
				"UTC_TIMESTAMP()) as period, count(*) from " + 
				table_name + "_Meta M " +
				"where M.ServerDate > (UTC_TIMESTAMP() - INTERVAL " + hourly_time_limit + " HOUR) " +
				"group by period " +
				"order by period;";
				Logging.log("command: " + command);
				statement = connection.prepareStatement(command);
				resultSet = statement.executeQuery(command);
				while (resultSet.next()) {
					int time_index = resultSet.getInt(1);
					int count = resultSet.getInt(2);
					if (time_index < nHoursBack) { // Don't record if we're not interested
						thisTableInfo.put(time_index,
								new TableStatusInfo(count));
					}
					dayTableStatus.nRecords(dayTableStatus.nRecords() + count);
				}
				resultSet.close();
				statement.close();

				// Error info
				command = "select timestampdiff(HOUR, D.eventdate, " +
				"UTC_TIMESTAMP()) as period, error, count(*) from DupRecord D " + 
				"where D.eventdate > (UTC_TIMESTAMP() - INTERVAL " + hourly_time_limit + " HOUR) " +
				"and D.RecordType = " + dq + table_name + dq +
				" group by period, error " +
				"order by period";
				Logging.log("command: " + command);
				statement = connection.prepareStatement(command);
				resultSet = statement.executeQuery(command);
				while (resultSet.next()) {
					int time_index = resultSet.getInt(1);
					TableStatusInfo tableStatus = 
						thisTableInfo.get(time_index);
					if (tableStatus == null) {
						tableStatus = new TableStatusInfo(0);
						if (time_index < nHoursBack) { // Don't record if we're not interested
							thisTableInfo.put(time_index, tableStatus);
						}
					}
					String errorType = resultSet.getString(2);
					int count = resultSet.getInt(3);
					if (errorType.equals("Duplicate")) {
						tableStatus.nDups(count);
						dayTableStatus.nDups(dayTableStatus.nDups() + count);
					} else if (errorType.equals("Parse")) {
						tableStatus.nParse(count);
						dayTableStatus.nParse(dayTableStatus.nParse() + count);
					} else if (errorType.equals("SQLError")) {
						tableStatus.nSQLErrors(count);
						dayTableStatus.nSQLErrors(dayTableStatus.nSQLErrors() + count);
					} else {
						tableStatus.nOther(tableStatus.nOther() + count);
						dayTableStatus.nOther(dayTableStatus.nOther() + count);
					}
				}
				resultSet.close();
				statement.close();

				////////////////////////////////////
				// Total for last 7 days
				////////////////////////////////////
				command = "select count(*) from " + table_name + "_Meta M " +
				"where M.ServerDate > (UTC_TIMESTAMP() - interval 1 week);";
				Logging.log("command: " + command);
				statement = connection.prepareStatement(command);
				resultSet = statement.executeQuery(command);

				TableStatusInfo weekTableStatus = new TableStatusInfo(0);
				thisTableInfo.put(nHoursBack + 1, weekTableStatus);
				if (resultSet.next()) {
					weekTableStatus.nRecords(resultSet.getInt(1));
				}
				resultSet.close();
				statement.close();
				command = "select count(*), error from DupRecord D " +
				"where D.eventdate > (UTC_TIMESTAMP() - interval 1 week) " +
				"and D.RecordType = " + dq + table_name + dq +
				" group by error ";
				Logging.log("command: " + command);
				statement = connection.prepareStatement(command);
				resultSet = statement.executeQuery(command);
				while (resultSet.next()) {
					int count = resultSet.getInt(1);
					String errorType = resultSet.getString(2);
					if (errorType.equals("Duplicate")) {
						weekTableStatus.nDups(count);
					} else if (errorType.equals("Parse")) {
						weekTableStatus.nParse(count);
					} else if (errorType.equals("SQLError")) {
						weekTableStatus.nSQLErrors(count);
					} else {
						weekTableStatus.nOther(weekTableStatus.nOther() + count);
					}
				}
				resultSet.close();
				statement.close();

				////////////////////////////////////
				// Totals for all time
				////////////////////////////////////
				command = "select nRecords from TableStatistics where RecordType = '" +
				table_name + "' and Qualifier = ''";
				Logging.log("command: " + command);
				statement = connection.prepareStatement(command);
				resultSet = statement.executeQuery(command);

				TableStatusInfo totalTableStatus = new TableStatusInfo(0);
				thisTableInfo.put(nHoursBack + 2, totalTableStatus);
				if (resultSet.next()) {
					totalTableStatus.nRecords(resultSet.getInt(1));
				}
				resultSet.close();
				statement.close();
				command = "select nRecords, Qualifier from TableStatistics where RecordType = " +
				dq + table_name + dq +
				" and Qualifier is not null and Qualifier != '' group by Qualifier";
				Logging.log("command: " + command);
				statement = connection.prepareStatement(command);
				resultSet = statement.executeQuery(command);
				while (resultSet.next()) {
					int count = resultSet.getInt(1);
					String errorType = resultSet.getString(2);
					if (errorType.equals("Duplicate")) {
						totalTableStatus.nDups(count);
					} else if (errorType.equals("Parse")) {
						totalTableStatus.nParse(count);
					} else if (errorType.equals("SQLError")) {
						totalTableStatus.nSQLErrors(count);
					} else {
						totalTableStatus.nOther(totalTableStatus.nOther() + count);
					}
				}
				resultSet.close();
				statement.close();
			}

			try {
				connection.commit();
				connection.setAutoCommit(true);
			}
			catch (Exception e) { // Ignore if we don't support exceptions
			}
			int maxthreads = Integer.parseInt(props.getProperty("service.listener.threads"));
			String path = System.getProperties().getProperty("catalina.home");
			path = xp.replaceAll(path,"\\","/");

			////////////////////////////////////
			// Fill in the table
			////////////////////////////////////

			//////////
			// Define the right number of column groups
			//////////
			m = colgroupDefn.matcher(html);

			int nTables = tableInfo.size();
			String nTablesFullGroupString = "";
			for (int i = 0; i < nTables; ++i) {
				nTablesFullGroupString += "$0";
			}
			html = m.replaceFirst(nTablesFullGroupString);
			Logging.debug("Matched " + m.group());

			//////////
			// Construct the table head, first line
			//////////
			m = colgroupHdr.matcher(html);
			html = m.replaceFirst(nTablesFullGroupString);
			for (Iterator x = keySet.iterator(); x.hasNext();) {
				String table_name = (String) x.next();
				html = html.replaceFirst("#RecordType#", table_name);
				Logging.debug("Matched " + m.group());
			}

			//////////
			// Construct the table head, second line
			//////////
			m = tableHead2.matcher(html);
			m.find();
			String tableHead2RepString = "$1";
			String columnHead = m.group(2);
			for (int i = 0; i < nTables; ++i) {
				tableHead2RepString += columnHead.replaceFirst("#datumHeader#", "Records");
				tableHead2RepString += columnHead.replaceFirst("#datumHeader#", "Duplicates");
				if (! detailedDisplay) continue; // Less info and clutter
				tableHead2RepString += columnHead.replaceFirst("#datumHeader#", "Parse errors");
				tableHead2RepString += columnHead.replaceFirst("#datumHeader#", "SQL errors");
				tableHead2RepString += columnHead.replaceFirst("#datumHeader#", "Other errors");
			}
			tableHead2RepString += "$3";
			html = m.replaceFirst(tableHead2RepString);
			Logging.debug("Matched " + m.group());

			//////////
			// Construct the body of the table
			//////////

			// Construct the replace string
			String rowReplaceString = "$1$2";
			for (int i = 0; i < nTables * nDataCols; ++i) {
				rowReplaceString += "$3";
			}
			rowReplaceString += "$4";

			m = timeIntervalRow.matcher(html);
			m.find();
			String matchedRow = m.group();
			Logging.debug("Matched " + m.group());
			m.reset(m.group());
			String fullRow = m.replaceFirst(rowReplaceString);
			Logging.debug("Matched " + m.group());
			Logging.debug("New row:\n" + fullRow);

			// Loop over tables
			rowReplaceString = "";
			for (int period_index = 0; period_index < nPeriods; ++period_index) {
				String newRow = fullRow;
				String period_label = null;
				if (period_index < nHoursBack) {
					period_label = "< " + ((Integer)(period_index + 1)).toString() + " hour";
					if (period_index != 1) period_label += "s";
					period_label += " ago";
				} else if (period_index == nHoursBack) {
					period_label = "Previous day";
				} else if (period_index == nHoursBack + 1) {
					period_label = "Previous week";
				} else if (period_index == nHoursBack + 2) {
					period_label = "All time total";
				} else {
					Logging.warning("Unrecognized period index: " + period_index);
				}
				newRow = newRow.replaceFirst("#timeinterval#", period_label);
				newRow = newRow.replaceFirst("#bgcolor#", color_b);
				int table_count = 0;
				for (Iterator x = keySet.iterator(); x.hasNext(); ++table_count) {
					String table_name = (String) x.next();
					HashMap<Integer, TableStatusInfo> thisTableInfo = tableInfo.get(table_name);
					TableStatusInfo tableStatus = 
						thisTableInfo.get(period_index);
					if (tableStatus == null) {
						for (int i = 0; i < nDataCols; ++i) {
							if ((table_count % 2) == 0) {
								newRow = newRow.replaceFirst("#bgcolor#", color_a);
							} else {
								newRow = newRow.replaceFirst("#bgcolor#", color_b);
							}
							newRow = newRow.replaceFirst("#tabledatum#", "0");
						}
					} else {
						for (int i = 0; i < nDataCols; ++i) {
							if ((table_count % 2) == 0) {
								newRow = newRow.replaceFirst("#bgcolor#", color_a);
							} else {
								newRow = newRow.replaceFirst("#bgcolor#", color_b);
							}
						}
						newRow = newRow.replaceFirst("#tabledatum#",
								((Integer) tableStatus.nRecords()).toString());
						newRow = newRow.replaceFirst("#tabledatum#",
								((Integer) tableStatus.nDups()).toString());
						if (detailedDisplay) {
							newRow = newRow.replaceFirst("#tabledatum#",
									((Integer) tableStatus.nParse()).toString());
							newRow = newRow.replaceFirst("#tabledatum#",
									((Integer) tableStatus.nSQLErrors()).toString());
							newRow = newRow.replaceFirst("#tabledatum#",
									((Integer) tableStatus.nOther()).toString());
						}
					}
				}
				rowReplaceString += newRow;
			}

			html = html.replaceFirst(matchedRow, rowReplaceString);

			////////////////////////////////////
			// Fill in the thread information.
			////////////////////////////////////

			m = qsizeblock.matcher(html);
			m.find();
			row = m.group();
			buffer = new StringBuffer();

			for (int i = 0; i < maxthreads; i++)
			{
				String newrow = new String(row);
				String xpath = path + "/gratia/data/thread" + i;
				long filenumber = xp.getFileNumber(xpath);
				newrow = xp.replaceAll(newrow,"#queue#","Q" + i);
				newrow = xp.replaceAll(newrow,"#queuesize#","" + filenumber);
				buffer.append(newrow);
			}
			html = xp.replaceAll(html,row,buffer.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

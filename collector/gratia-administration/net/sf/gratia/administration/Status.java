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
	//
	// processing related
	//
	String html = "";
	String tableHeader = "";
	String tableSection = "";
	String row = "";
	StringBuffer buffer = new StringBuffer();
	static final Pattern yesMatcher =
		Pattern.compile("^[YyTt1]");
	static final Pattern colgroupDefn =
		Pattern.compile("^\\s*<colgroup class=\"tablespan\".*?/>\\s*?\n",
				Pattern.MULTILINE + Pattern.DOTALL);
	static final Pattern colgroupHdr =
		Pattern.compile("^\\s*<th\\s+.*?#RecordType#</th>\\s*?\n",
				Pattern.MULTILINE);
	static final Pattern tableHead2 =
		Pattern.compile("^(\\s*<tr class=\"tablehead2\">\\s*?\n)" +
				"(.*?<th.*?\n)(\\s*</tr>\n)",
				Pattern.MULTILINE + Pattern.DOTALL);
	static final Pattern timeIntervalRow =
		Pattern.compile("^(\\s*<tr class=\"timeintervalrow\">\\s*?\n)" +
				"(.*?<td.*?#timeinterval#.*?</td>\\s*?\n)" +
				"(.*?<td.*?#tabledatum#.*?</td>\\s*?\n)(\\s*</tr>\n)",
				Pattern.MULTILINE + Pattern.DOTALL);
	static final Pattern qsizeblock =
		Pattern.compile("<tr class=\"qsize\">.*?</tr>",
				Pattern.MULTILINE + Pattern.DOTALL);
	boolean detailedDisplay = false;

	//
	// globals
	//
	boolean initialized = false;
	Properties props;
	int nHoursBack = 6;
	String color_a = "#ffffff";
	String color_b = "#cccccc";

	//
	// matching
	//


	class TableStatusInfo {
		long nRecords = 0;
		long nDups = 0;
		long nSQLErrors = 0;
		long nParse = 0;
		long nOther = 0;

		public TableStatusInfo(long nRecords) {
			this.nRecords = nRecords;
		}
		public TableStatusInfo(long nRecords, long nDups, long nSQLErrors, long nParse, long nOther) {
			this.nRecords = nRecords;
			this.nDups = nDups;
			this.nSQLErrors = nSQLErrors;
			this.nParse = nParse;
			this.nOther = nOther;
		}
		public long nRecords() {
			return this.nRecords;
		}
		public long nDups() {
			return this.nDups;
		}
		public long nSQLErrors() {
			return this.nSQLErrors;
		}
		public long nParse() {
			return this.nParse;
		}
		public long nOther() {
			return this.nOther;
		}
		public void nRecords(long in) {
			this.nRecords = in;
		}
		public void nDups(long in) {
			this.nDups = in;
		}
		public void nSQLErrors(long in) {
			this.nSQLErrors = in;
		}
		public void nParse(long in) {
			this.nParse = in;
		}
		public void nOther(long in) {
			this.nOther = in;
		}

	}

	public void init(ServletConfig config) throws ServletException 
	{
	}

	public Connection openConnection()
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
			return DriverManager.getConnection(url,user,password);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
      return null;
	}

	public void closeConnection(Connection connection) {
		try {
			connection.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		Connection connection = openConnection();
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
		setup(request);
		process(connection);
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0
		PrintWriter writer = response.getWriter();
		writer.write(html);
		writer.flush();
		writer.close();
		closeConnection(connection);
	}

	public static String DisplayInt(Integer value) {
		if (value == null)
			return "n/a";
		else 
			return value.toString();
	}

	public void setup(HttpServletRequest request) throws IOException {
		html = xp.get(request.getRealPath("/") + "status.html");
	}

   static final String fgCountHourly = "select period, RecordType, Qualifier, max(maxRecords) from ( " + 
      "   select timestampdiff(HOUR, ServerDate, UTC_TIMESTAMP()) as period, RecordType, Qualifier, max(nRecords) as maxRecords" +
      "   from TableStatisticsSnapshots where ValueType = 'lifetime' and nRecords > 0 " +
      "   and ServerDate > (UTC_TIMESTAMP() - INTERVAL ? HOUR) " +
      "   group by RecordType,Qualifier,period " +
      "   union select 0 as period, RecordType, Qualifier, nRecords as maxRecords from TableStatistics " +
      "         where nRecords > 0 and ValueType = 'lifetime' ) Sub " + 
      " where RecordType != 'DupRecord' and RecordType != 'ProbeDetails' and RecordType != '' " +
      " group by RecordType,Qualifier,period " +
      " order by RecordType,Qualifier,period desc ";
   static final String fgCountLastWeekQuery = "select currentRecords - ifnull(maxRecords,0),forJoin.RecordType,forJoin.Qualifier from " +
      "( select ValueType,RecordType,Qualifier,max(maxRecords) as maxRecords from " +
      "( select ValueType,RecordType,Qualifier,max(nRecords) as maxRecords from TableStatisticsSnapshots where ValueType = 'lifetime'" +
      "     and ServerDate <= (UTC_TIMESTAMP() - interval 1 week) " +
      "     and RecordType != 'DupRecord' and RecordType != 'ProbeDetails' and RecordType != '' " +
      "     and nRecords > 0 " +
      "     group by ValueType,RecordType,Qualifier" + 
      "  union select ValueType,RecordType,Qualifier,max(maxRecords) as maxRecords from TableStatisticsHourly where ValueType = 'lifetime'" +
      "     and EndTime <= (UTC_TIMESTAMP() - interval 1 week) " +
      "     and RecordType != 'DupRecord' and RecordType != 'ProbeDetails' and RecordType != '' "+
      "     and maxRecords > 0 " +
      "     group by ValueType,RecordType,Qualifier" +
      "  union select ValueType,RecordType,Qualifier,max(maxRecords) as maxRecords from TableStatisticsDaily where ValueType = 'lifetime'" +
      "    and EndTime <= (UTC_TIMESTAMP() - interval 1 week) " + 
      "    and RecordType != 'DupRecord' and RecordType != 'ProbeDetails' and RecordType != '' " +
      "     and maxRecords > 0 " +
      "    group by ValueType,RecordType,Qualifier " + 
      ") forMax " +
      "group by ValueType,RecordType,Qualifier ) forWrapup " +
      "right outer join " + 
      "( select ValueType,RecordType,Qualifier,max(nRecords) as currentRecords from TableStatistics where ValueType = 'lifetime'" + 
      "     and nRecords > 0 " +
      "     and RecordType != 'DupRecord' and RecordType != 'ProbeDetails' and RecordType != '' "+
      "     group by ValueType,RecordType,Qualifier " +
      ") forJoin " +
      "on forWrapup.RecordType = forJoin.RecordType and forWrapup.Qualifier = forJoin.Qualifier and forWrapup.ValueType = forJoin.ValueType ";
   static final String fgCountAllTime = "select nRecords, RecordType, Qualifier from TableStatistics where " +
      " RecordType != 'DupRecord' and RecordType != 'ProbeDetails' and RecordType != '' " +
      " and nRecords > 0 " +
      " and ValueType = 'lifetime' group by RecordType, Qualifier";

	public void process(Connection connection) {
		HashMap<String, HashMap<Integer, TableStatusInfo> > tableInfo = new HashMap<String, HashMap<Integer, TableStatusInfo> >(5);
      
		Matcher m = null;

		int index = 0;
		String command = "";
		buffer = new StringBuffer();

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
			PreparedStatement statement = connection.prepareStatement(command);
			ResultSet resultSet = statement.executeQuery();
			Logging.log("Status SQL query:"+statement);
			while(resultSet.next()) {

				String table_name = resultSet.getString(1);
				if (table_name.equals("ProbeDetails_Meta")) continue; // Not interested
				int end_index = table_name.lastIndexOf("_Meta");
				String base_table = table_name.substring(0,end_index);
				// Only display table if we have any records

				command = "select * from " + base_table + " limit 1";
				PreparedStatement tableUseCheck = connection.prepareStatement(command);
				ResultSet tableUseResult = tableUseCheck.executeQuery();
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
			int hourly_time_limit = Math.max(nHoursBack, 25);

//			for (Iterator x = keySet.iterator(); x.hasNext();) {
//				String table_name = (String) x.next();
//				HashMap<Integer, TableStatusInfo> thisTableInfo = tableInfo.get(table_name);
//            if (thisTableInfo == null) {
//               thisTableInfo = new HashMap<Integer, TableStatusInfo>(nPeriods);
//               tableInfo.put(table_name,thisTableInfo);
//            }
//				TableStatusInfo dayTableStatus = new TableStatusInfo(0);
//				thisTableInfo.put(nHoursBack, dayTableStatus);
				////////////////////////////////////
				// For each of last 24 hours
				////////////////////////////////////

				// nRecords
				// Error info
				statement = connection.prepareStatement(fgCountHourly);
            statement.setLong(1,hourly_time_limit);
//            statement.setString(2,table_name);
				Logging.debug("Status SQL query: " + statement);
				resultSet = statement.executeQuery();
            long prevCount = 0;
            String prevRecordType = null;
            String prevQualifier = null;
            HashMap<Integer, TableStatusInfo> thisTableInfo = null;
				TableStatusInfo dayTableStatus = null;
            while (resultSet.next()) {
               String recordType = resultSet.getString(2);

               if ( ! recordType.equals(prevRecordType) ) {
                  thisTableInfo = tableInfo.get(recordType);
                  if (thisTableInfo == null) {
                     thisTableInfo = new HashMap<Integer, TableStatusInfo>(nPeriods);
                     tableInfo.put(recordType,thisTableInfo);
                  }
                  prevRecordType = recordType;
                  prevQualifier = null;
                  prevCount = 0;
                  
                  dayTableStatus = new TableStatusInfo(0);
                  thisTableInfo.put(nHoursBack, dayTableStatus);
               }
					
               int time_index = resultSet.getInt(1);

					TableStatusInfo tableStatus = thisTableInfo.get(time_index);
					if (tableStatus == null) {
						tableStatus = new TableStatusInfo(0);
						if (time_index < nHoursBack) { // Don't record if we're not interested
							thisTableInfo.put(time_index, tableStatus);
						}
					}
					String qualifier = resultSet.getString(3);
					long currentCount = resultSet.getLong(4);
               long count = 0;

               if ( ! qualifier.equals(prevQualifier) ) {
                  prevCount = currentCount;
                  prevQualifier = qualifier;
                  
                  // We just register the first one.
               } else {
                  count = currentCount - prevCount;
                  prevCount = currentCount;
               
                  if (qualifier.equals("")) {
                     if (time_index < nHoursBack) { // Don't record if we're not interested
                        thisTableInfo.put(time_index, new TableStatusInfo(count));
                     }
                     dayTableStatus.nRecords(dayTableStatus.nRecords() + count);                  
                  } else if (qualifier.equals("Duplicate")) {
                     if (time_index < nHoursBack) { // Don't record if we're not interested
                        tableStatus.nDups(count);
                     }
                     dayTableStatus.nDups(dayTableStatus.nDups() + count);
                  } else if (qualifier.equals("Parse")) {
                     if (time_index < nHoursBack) { // Don't record if we're not interested
                        tableStatus.nParse(count);
                     }
                     dayTableStatus.nParse(dayTableStatus.nParse() + count);
                  } else if (qualifier.equals("SQLError")) {
                     if (time_index < nHoursBack) { // Don't record if we're not interested
                        tableStatus.nSQLErrors(count);
                     }
                     dayTableStatus.nSQLErrors(dayTableStatus.nSQLErrors() + count);
                  } else {
                     if (time_index < nHoursBack) { // Don't record if we're not interested
                        tableStatus.nOther(tableStatus.nOther() + count);
                     }
                     dayTableStatus.nOther(dayTableStatus.nOther() + count);
                  }
               }
				}
				resultSet.close();
				statement.close();

				////////////////////////////////////
				// Total for last 7 days
				////////////////////////////////////

//				TableStatusInfo weekTableStatus = new TableStatusInfo(0);
//				thisTableInfo.put(nHoursBack + 1, weekTableStatus);

				statement = connection.prepareStatement(fgCountLastWeekQuery);
//            statement.setString(1,table_name);
				Logging.debug("Status SQL query: " + statement);
				resultSet = statement.executeQuery();
            prevRecordType = null;
            TableStatusInfo weekTableStatus = null;
				while (resultSet.next()) {
					long count = resultSet.getLong(1);
               String recordType = resultSet.getString(2);
					String qualifier = resultSet.getString(3);
               
               if ( ! recordType.equals(prevRecordType) ) {
                  thisTableInfo = tableInfo.get(recordType);
                  if (thisTableInfo == null) {
                     thisTableInfo = new HashMap<Integer, TableStatusInfo>(nPeriods);
                     tableInfo.put(recordType,thisTableInfo);
                  }
                  prevRecordType = recordType;

                  weekTableStatus = new TableStatusInfo(0);
                  thisTableInfo.put(nHoursBack + 1, weekTableStatus);
               }
               
					if (qualifier.equals("")) {
                  weekTableStatus.nRecords(count);                 
               } else if (qualifier.equals("Duplicate")) {
						weekTableStatus.nDups(count);
					} else if (qualifier.equals("Parse")) {
						weekTableStatus.nParse(count);
					} else if (qualifier.equals("SQLError")) {
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
//				TableStatusInfo totalTableStatus = new TableStatusInfo(0);
//				thisTableInfo.put(nHoursBack + 2, totalTableStatus);

				statement = connection.prepareStatement(fgCountAllTime);
//            statement.setString(1,table_name);
				Logging.debug("Status SQL query: " + statement);
				resultSet = statement.executeQuery();
            prevRecordType = null;
				TableStatusInfo totalTableStatus = null;
            while (resultSet.next()) {
					long count = resultSet.getLong(1);
               String recordType = resultSet.getString(2);
					String errorType = resultSet.getString(3);

               if ( ! recordType.equals(prevRecordType) ) {
                  thisTableInfo = tableInfo.get(recordType);
                  if (thisTableInfo == null) {
                     thisTableInfo = new HashMap<Integer, TableStatusInfo>(nPeriods);
                     tableInfo.put(recordType,thisTableInfo);
                  }
                  prevRecordType = recordType;
                  totalTableStatus = new TableStatusInfo(0);
                  thisTableInfo.put(nHoursBack + 2, totalTableStatus);
               }

					if (errorType.equals("")) {
                  totalTableStatus.nRecords(count);
               } else if (errorType.equals("Duplicate")) {
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
//			}

			try {
				connection.commit();
				connection.setAutoCommit(true);
			}
			catch (Exception e) { // Ignore if we don't support exceptions
			}
			int maxthreads = Integer.parseInt(props.getProperty("service.recordProcessor.threads"));
			String path = System.getProperties().getProperty("catalina.home");
			path = xp.replaceAll(path,"\\","/");

			////////////////////////////////////
			// Fill in the table
			////////////////////////////////////

			//////////
			// Define the right number of column groups
			//////////
			m = colgroupDefn.matcher(html);

         keySet = new TreeSet(tableInfo.keySet());
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
					HashMap<Integer, TableStatusInfo> thisTableInfo2 = tableInfo.get(table_name);
					TableStatusInfo tableStatus = 
						thisTableInfo2.get(period_index);
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
								((Long) tableStatus.nRecords()).toString());
						newRow = newRow.replaceFirst("#tabledatum#",
								((Long) tableStatus.nDups()).toString());
						if (detailedDisplay) {
							newRow = newRow.replaceFirst("#tabledatum#",
									((Long) tableStatus.nParse()).toString());
							newRow = newRow.replaceFirst("#tabledatum#",
									((Long) tableStatus.nSQLErrors()).toString());
							newRow = newRow.replaceFirst("#tabledatum#",
									((Long) tableStatus.nOther()).toString());
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
         try
         {
            command = "select Queue, Files, Records from CollectorStatus order by Queue";
            statement = connection.prepareStatement(command);
            resultSet = statement.executeQuery();
            while(resultSet.next()) {
               int q = resultSet.getInt(1);
               long nFiles = resultSet.getLong(2);
               long nRecords = resultSet.getLong(3);
               String newrow = new String(row);
               newrow = xp.replaceAll(newrow,"#queue#","" + q);
               newrow = xp.replaceAll(newrow,"#queuefiles#","" + nFiles);
               newrow = xp.replaceAll(newrow,"#queuerecords#","" + nRecords);
               buffer.append(newrow);
            }
            resultSet.close();
            statement.close();
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }   

			html = xp.replaceAll(html,row,buffer.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

package net.sf.gratia.administration;

import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Logging;

import net.sf.gratia.services.JMSProxy;

import net.sf.gratia.storage.ExpirationDateCalculator;

import java.io.*;

import java.util.Properties;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;

import java.rmi.Naming;

import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;

import java.util.regex.*;
import java.lang.Math;

public class PerformanceRates extends HttpServlet {

   JMSProxy fProxy = null;
   List<String> fTableNames = null;
   ExpirationDateCalculator fExpCalc = new ExpirationDateCalculator();   

   static final Pattern yesMatcher = Pattern.compile("^[YyTt1]");

   // Which Servlet/web page is this
   String fName;
   static final String fApplicationURL = "performance-rate.html";
   
   
   static final String fgPreamble = 
   "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
   "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
   "<head>\n" +
   "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\" />\n" +
   "<title>Gratia Accounting</title>\n" +
   "<link href=\"stylesheet.css\" type=\"text/css\" rel=\"stylesheet\" />\n" +
   "<link href=\"docstyle.css\" type=\"text/css\" rel=\"stylesheet\" />\n" +
   "<style type=\"text/css\">\n" +
   ".problem {color: #FF0000; font-weight:bold;}\n" +
   ".good {color: green; font-weight:bold;}\n" +
   ".improving {color: #000000; font-weight:bold;}\n" +
   ".ancient {color: purple; font-style:italic; font-weight:bold;}\n" +
   "</style>" +
   "</head>\n" +
   "<body>\n" +
   "<h1 align=\"center\" class=\"osgcolor\">&nbsp;&nbsp;&nbsp;&nbsp;Gratia Administration&nbsp;&nbsp;&nbsp;&nbsp;</h1>\n" +
   "<h3 align=\"center\">Performance Rates</h3>\n";
   
   //
   // globals
   //
   static final String color_a = "#ffffff";
   static final String color_b = "#cccccc";

   //
   // matching
   //

   public void init(ServletConfig config) throws ServletException 
   {
      Logging.debug("PerformanceRates.init()");
      fName = config.getServletName();
   }

   public Connection openConnection()
   {
      Properties properties = Configuration.getProperties();
      if (fProxy == null) {
         while (true) {
            // Wait until JMS service is up
            try { 
               fProxy = (JMSProxy)
               Naming.lookup(properties.getProperty("service.rmi.rmilookup") +
                             properties.getProperty("service.rmi.service"));
            } catch (Exception e) {
               try {
                  Thread.sleep(5000);
               } catch (Exception ignore) {
               }
            }
            break;
         }
      }

      //
      // database related
      //
      String driver = "";
      String url = "";
      String user = "";
      String password = "";
      try {
         driver = properties.getProperty("service.mysql.driver");
         url = properties.getProperty("service.mysql.url");
         user = properties.getProperty("service.mysql.user");
         password = properties.getProperty("service.mysql.password");
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
   
   static boolean Requested(HttpServletRequest request, String paramName)
   {
      // Return true if the parameter is set to 'y[es]','1' or 'T[rue]'
      
      String value = request.getParameter(paramName);
      Logging.debug("Got parameter "+paramName+"=" + value);
      
      if (value != null) {
         if (yesMatcher.matcher(value).lookingAt()) {
            return true;
         } else {
            return false;
         }
      } else {
         return false;
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
      
      boolean detailedDisplay = Requested(request,"wantDetails");
      boolean housekeepingDetails = Requested(request,"housekeepingDetails");

      String html = process(request, connection, detailedDisplay, housekeepingDetails);
      response.setContentType("text/html");
      response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
      response.setHeader("Pragma", "no-cache"); // HTTP 1.0
      PrintWriter writer = response.getWriter();
      writer.write(fgPreamble);
      writer.write("<h4 align=\"center\">");
      writer.write(fProxy.getName());
      writer.write("</h4>\n");
      writer.write(html);
      writer.write("</body>\n      </html>\n");
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

   static final String fgProcessQueueLengthQuery = "select Queue, Files, Records from CollectorStatus order by Queue";
   public static long addQueueLength(StringBuffer buffer, Connection connection, boolean retry)
   {
      long total_records = 0;
      buffer.append("<h3>Queue information</h3>\n");
      buffer.append("<table border=\"1\">\n");
      buffer.append("<tr class=\"qsize_head\">\n");
      buffer.append("<th>Queue</th><th>Files</th><th>Records</th>\n");
      buffer.append("</tr>\n");
      try
      {
         PreparedStatement statement = connection.prepareStatement(fgProcessQueueLengthQuery);
         ResultSet resultSet = statement.executeQuery();
         while(resultSet.next()) {
            int q = resultSet.getInt(1);
            long nFiles = resultSet.getLong(2);
            long nRecords = resultSet.getLong(3);
            total_records = total_records + nRecords;
            if (retry && (nFiles < 0 || nRecords < 0) ) {
               // We know something went wrong.  We need to abord, refresh the Status
               // and retry.
               resultSet.close();
               statement.close();
               // RefreshStatus
               if (RefreshCollectorStatus.ExecuteRefresh()) {
                  // Retry
                  return addQueueLength(buffer, connection, false);
               } else {
                  // We can't fix it from here, let's display the
                  // wrong numbers :(
               }
            }
            buffer.append("<tr class=\"qsize\">\n");
            buffer.append("<td align=\"center\"><strong>").append(q).append("</strong></td>");
            buffer.append("<td align=\"right\">").append(nFiles).append("</td>");
            buffer.append("<td align=\"right\">").append(nRecords).append("</td>\n");
         }
         resultSet.close();
         statement.close();
       }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      buffer.append("</table>\n");
      return total_records;
   }
   
   java.util.Date GetOldest(Connection connection, String table) 
   {
      String cmd = "select min(ServerDate) from "+table+"_Meta ";
      java.util.Date result = null;
      try {
         PreparedStatement statement = connection.prepareStatement(cmd);
         Logging.debug("Status SQL query:"+statement);
         ResultSet resultSet = statement.executeQuery();
         while(resultSet.next()) {
            result = resultSet.getTimestamp(1);
         }
         resultSet.close();
         statement.close();
      } catch (Exception e) {
         Logging.warning("PerformanceRates: Failed to get table oldest record information.",e);
      }
      return result;
   }
   
   long GetRecordNumber(Connection connection, String table, java.util.Date limit) 
   {
      String cmd = "select count(*) from "+table+"_Meta where ServerDate < ?";
      long result = 0;
      try {
         PreparedStatement statement = connection.prepareStatement(cmd);
         statement.setTimestamp(1,new java.sql.Timestamp(limit.getTime()));
         Logging.debug("Status SQL query:"+statement);
         ResultSet resultSet = statement.executeQuery();
         while(resultSet.next()) {
            result = resultSet.getLong(1);
         }
         resultSet.close();
         statement.close();
      } catch (Exception e) {
         Logging.warning("PerformanceRates: Failed to get table oldest record information.",e);
      }
      return result;
   }
   
   java.util.Date GetOldestXml(Connection connection, String table) 
   {
      String dbidcmd = "select dbid from "+table+"_Xml X where ExtraXml = \"\" order by dbid limit 1";
      String datecmd = "select ServerDate from "+table+"_Meta M where dbid = ?";
      // "select min(ServerDate) from "+table+"_Xml X,"+table+"_Meta M where X.dbid = M.dbid and ExtraXml = \"\" ";
      java.util.Date result = null;
      try {
         long dbid = 0;
         PreparedStatement statement = connection.prepareStatement(dbidcmd);
         Logging.debug("Status SQL query:"+statement);
         ResultSet resultSet = statement.executeQuery();
         while(resultSet.next()) {
            dbid = resultSet.getLong(1);
         }
         resultSet.close();
         statement.close();
         if (dbid != 0) {
            statement = connection.prepareStatement(datecmd);
            statement.setLong(1,dbid);
            Logging.debug("Status SQL query:"+statement);
            resultSet = statement.executeQuery();
            while(resultSet.next()) {
               result = resultSet.getTimestamp(1);
            }
            resultSet.close();
            statement.close();
         }
      } catch (Exception e) {
         Logging.warning("PerformanceRates: Failed to get table oldest xml record information.",e);
      }
      return result;
   }

   long GetRecordNumberXml(Connection connection, String table, java.util.Date limit) 
   {
      String dbidcmd = "select dbid from "+table+"_Meta M where ServerDate < ?";
      String countcmd = "select count(*) from "+table+"_Xml X where dbid < ? and ExtraXml = \"\"";
      long result = 0;
      try {
         PreparedStatement statement = connection.prepareStatement(dbidcmd);
         statement.setTimestamp(1,new java.sql.Timestamp(limit.getTime()));
         Logging.debug("Status SQL query:"+statement);
         long highdbid = 0;
         ResultSet resultSet = statement.executeQuery();
         while(resultSet.next()) {
            highdbid = resultSet.getLong(1);
         }
         resultSet.close();
         statement.close();

         statement = connection.prepareStatement(countcmd);
         statement.setLong(1,highdbid);
         Logging.debug("Status SQL query:"+statement);
         resultSet = statement.executeQuery();
         while(resultSet.next()) {
            result = resultSet.getLong(1);
         }
         resultSet.close();
         statement.close();
         
      } catch (Exception e) {
         Logging.warning("PerformanceRates: Failed to get table oldest record information.",e);
      }
      return result;
   }
   
   
   static final String fgHouseKeepingCmd = "select current.ValueType,EventDate,new_values-old_values,old_values,new_values from " +
                                           " (select sum(avgRecords) as old_values,EventDate,ValueType from TableStatisticsHourly where "+ 
                                           "     EventDate < date_sub(?,interval 1 day) group by EventDate,ValueType order by EventDate desc limit 2) as prev,"+
                                           " (select sum(nRecords) as new_values,ValueType from TableStatistics  group by ValueType) as current "+
                                          "  where prev.ValueType = current.ValueType";
   static final String fgHouseKeepingCurrentCmd = "select ValueType,date_sub(?,interval 1 day),sum(nRecords) as new_values from TableStatistics  group by ValueType";
   void appendHouseKeeping(StringBuffer buffer, Connection connection, boolean housekeepingDetails)
   {
      // Add information about housekeeping.
      buffer.append("<h4>Housekeeping (<a href=\""+fApplicationURL+"?housekeepingDetails=yes\" target=\"adminContent\">details</a>)</h4>\n");
      
      java.util.Date now = new java.util.Date();

      long lifetime = 0;
      long current = 0;
      java.util.Date then = null;
      try {
         PreparedStatement statement = connection.prepareStatement(fgHouseKeepingCmd);
         statement.setTimestamp(1,new java.sql.Timestamp(now.getTime()));
         Logging.debug("Status SQL query:"+statement);
         ResultSet resultSet = statement.executeQuery();
         while(resultSet.next()) {
            String valuetype = resultSet.getString(1);
            if (valuetype.equals("lifetime")) {
               lifetime = resultSet.getLong(3);
            } else if  (valuetype.equals("current")) {
               current = resultSet.getLong(3);
            }
            then = resultSet.getTimestamp(2);
         }
         resultSet.close();
         statement.close();
         if (then == null) {
            statement = connection.prepareStatement(fgHouseKeepingCurrentCmd);
            statement.setTimestamp(1,new java.sql.Timestamp(now.getTime()));
            Logging.debug("Status SQL query:"+statement);
            resultSet = statement.executeQuery();
            while(resultSet.next()) {
               String valuetype = resultSet.getString(1);
               if (valuetype.equals("lifetime")) {
                  lifetime = resultSet.getLong(3);
               } else if  (valuetype.equals("current")) {
                  current = resultSet.getLong(3);
               }
               then = resultSet.getTimestamp(2);
            }            
         }
         resultSet.close();
         statement.close();
      } catch (Exception e) {
         Logging.warning("PerformanceRates: Failed to get housekeeping information.",e);
      }
      try {
         buffer.append("The house keeping service is currently "+fProxy.housekeepingServiceStatus()+".<p/>");
      } catch (Exception e) {
         buffer.append("The house keeping service's status could not be retrieved.<p/>");
         Logging.warning("PerformanceRates: Failed to get housekeeping information.",e);
      }
      double rate_per_day = 0;
      if (then == null) {
         buffer.append("The housekeeping rate could not be retrieved.");
      } else {
         long housekeeping = lifetime - current;
         long delta = now.getTime() - then.getTime();
         if (housekeeping < 0 || delta <= 0) {
            buffer.append("The housekeeping rate could not be properly retrieved.<p/>");
         } else {
            rate_per_day = housekeeping / ( delta / (24 * 3600.0 * 1000.0 ) );
            buffer.append("The housekeeping is removing ");
            java.text.DecimalFormat decForm = new java.text.DecimalFormat();
            decForm.applyPattern("0.0");
            java.text.FieldPosition pos1 = new java.text.FieldPosition(java.text.NumberFormat.FRACTION_FIELD);
            decForm.format(rate_per_day, buffer, pos1);
            buffer.append(" records per day.<p/>");
         }
      }
      if (housekeepingDetails) {
         class DateInfo implements Comparable<DateInfo> {
            String         fName;
            java.util.Date fLimit;
            java.util.Date fOldest;
            long           fNRecords;
            public DateInfo(String name, java.util.Date limit, java.util.Date old, long nrecords) {
               fName = name;
               fLimit = limit;
               fOldest = old;
               fNRecords = nrecords;
            }
            public int compareTo(DateInfo o) {
               return fName.compareTo(o.fName);
            }
         };
         List<DateInfo> datelist = new java.util.ArrayList<DateInfo>();
         try {
            List<String> tables = getListOfTables(connection);
            for(String name : tables) {
               java.util.Date limit = fExpCalc.expirationRange(now, name, "").fExpirationDate;
               java.util.Date oldest = GetOldest(connection,name);
               long nrecords = GetRecordNumber(connection,name,limit);
               if (oldest != null) {
                  // There are some records.
                  datelist.add(new DateInfo(name,limit,oldest,nrecords));
               }
            }
            java.util.Date limit = fExpCalc.expirationRange(now, "JobUsageRecord", "RawXML").fExpirationDate;
            java.util.Date oldest = GetOldestXml(connection,"JobUsageRecord");
            long nrecords = GetRecordNumberXml(connection,"JobUsageRecord",limit);
            if (oldest != null) {
               datelist.add(new DateInfo("JobUsageRecord_Xml",limit,oldest,nrecords));
            }
            limit = fExpCalc.expirationRange(now, "MetricRecord", "RawXML").fExpirationDate;
            oldest = GetOldestXml(connection,"MetricRecord");
            nrecords = GetRecordNumberXml(connection,"MetricRecord",limit);
            if (oldest != null) {
               datelist.add(new DateInfo("MetricRecord_Xml",limit,oldest,nrecords));
            }
         } catch (Exception e) {
            Logging.warning("PerformanceRates: Could not retrieve the list of housekeeping statuses.",e);
         }
         buffer.append("<table border=\"1\" cellpadding=\"10\">\n");
         buffer.append("<tr class=\"housekeeping_head\">\n");
         buffer.append("<th >Table</th><th>Expiration Date</th><th>Oldest Record</th><th>Backlog</th><th>Backlog in records</th><th>Recovery time</th>\n");
         buffer.append("</tr>\n");
         java.util.Collections.sort(datelist);
         java.text.SimpleDateFormat dateformat = new java.text.SimpleDateFormat("yyyy-MM-dd");
         java.text.FieldPosition pos2 = new java.text.FieldPosition(0);
         
         for(DateInfo line : datelist) {
            buffer.append("<tr class=\"housekeeping\">\n");
            buffer.append("<td style=\"text-align: left; font-weight:bold;\" >").append(line.fName).append("</td>");
            buffer.append("<td style=\"text-align: right; \" >");
            dateformat.format(line.fLimit,buffer,pos2);
            buffer.append("</td>");
            buffer.append("<td style=\"text-align: right; \" >");
            dateformat.format(line.fOldest,buffer,pos2);
            buffer.append("</td>\n");
            buffer.append("<td style=\"text-align: right; \" >");
            double hours_back = (line.fLimit.getTime() - line.fOldest.getTime()) / (1000.0 * 3600);
            if (hours_back < 0) {
               buffer.append("<em class=\"good\">Up to date</em>");
            } else {
               appendHours(buffer,hours_back);
            }
            buffer.append("</td>\n");
            buffer.append("<td style=\"text-align: right; \" >");
            buffer.append(line.fNRecords);
            buffer.append("</td>\n");
            buffer.append("<td style=\"text-align: right; \" >");
            if (rate_per_day > 0 && line.fNRecords > 0) {
               appendHours(buffer,line.fNRecords / (rate_per_day*24.0) );
            }
            buffer.append("</td>\n");
         }
         buffer.append("</table>\n");
      } else {
         // buffer.append("Details NOT requested.<br/>");
      }
   }
   
   List<String> getListOfTables(Connection connection) 
   {
      
      if (fTableNames == null) {
         fTableNames = new java.util.ArrayList();
         try {
            // Start transaction so numbers are consistent.
            connection.setAutoCommit(false);
            
            // Get list of _Meta tables in this database
            String command = "select table_name from information_schema.tables " +
                             "where table_schema = Database() and table_name like '%_Meta'" +
                             " order by table_name;";
            PreparedStatement statement = connection.prepareStatement(command);
            ResultSet resultSet = statement.executeQuery();
            Logging.debug("Status SQL query:"+statement);
            while(resultSet.next()) {
               
               String table_name = resultSet.getString(1);
               if (table_name.equals("ProbeDetails_Meta")) continue; // Not interested
               int end_index = table_name.lastIndexOf("_Meta");
               String base_table = table_name.substring(0,end_index);
               
               // Extra check to only look at non empty tables.
               //command = "select * from " + base_table + " limit 1";
               //PreparedStatement tableUseCheck = connection.prepareStatement(command);
               //ResultSet tableUseResult = tableUseCheck.executeQuery();
               //if (tableUseResult.next()) {
               fTableNames.add(base_table);
               //}
               //tableUseResult.close();
               //tableUseCheck.close();
            }
            resultSet.close();
            statement.close();
            connection.commit();
         } catch (Exception e) {
            Logging.warning("PerformanceRates: Failed to load table information from DB. Try reload.",e);
            fTableNames = null;
         }
      }
      return fTableNames;
   }
   
   static void appendHours(StringBuffer buffer, double hours)
   {
      java.text.DecimalFormat decForm = new java.text.DecimalFormat();
      decForm.applyPattern("0.0");
      java.text.FieldPosition pos1 = new java.text.FieldPosition(java.text.NumberFormat.FRACTION_FIELD);
      if ( hours < 2.0) {
         double minutes = 60 * hours;
         buffer = decForm.format(minutes, buffer, pos1);
         buffer.append(" minutes");
      } else if ( hours < 48.0 ) {
         buffer = decForm.format(hours, buffer, pos1);
         buffer.append(" hours");
      } else if ( hours < (24 * 14) ) {
         buffer = decForm.format(hours / 24.0, buffer, pos1);
         buffer.append(" days");               
      } else {
         buffer = decForm.format(hours / (24.0 * 7), buffer, pos1);
         buffer.append(" week");
      }      
   }
   
   static final String fgQueueOneHour = "select ServerDate,EventDate,nRecords,serviceBacklog from BacklogStatisticsSnapshots where EntityType = 'local' " +
                                        " and ServerDate > date_sub(?,interval 1 hour) order by ServerDate desc limit 1";
   
   public String process(HttpServletRequest request, Connection connection, boolean detailedDisplay, boolean housekeepingDetails) throws java.io.IOException
   {
      //
      // processing related
      //      
      try {
         connection.setAutoCommit(false);
      }
      catch (Exception e)
      {
         Logging.warning("PerformanceRates: Failed to commit the transaction.",e);
      }
   
      java.util.Date now = new java.util.Date();
      StringBuffer buffer = new StringBuffer();
      java.text.DecimalFormat decForm = new java.text.DecimalFormat();
      decForm.applyPattern("0.0");
      java.text.FieldPosition pos1 = new java.text.FieldPosition(java.text.NumberFormat.FRACTION_FIELD);
      
      // Print in the thread/Queue information.      
      long records_in_queue = addQueueLength(buffer,connection,true);
      double records_per_hour = 0;
   
      buffer.append("<h3>Performance and Catchup</h3>\n");
      buffer.append("<h4>Performance</h3>\n");
      // Print the current processing rate.
      // Sum of the number of records in all the _Meta tables in the last 25 seconds
      // divided by ( now - earliest ServerDate )
      if (getListOfTables(connection)!=null) {
         long nrecords = 0;

         java.util.Date oldest = now;
         java.util.Date newest = now;
         try {
            for (String table : fTableNames) {
               String command = "select min(ServerDate),max(ServerDate),count(*) from "+table+"_Meta where ServerDate > date_sub(?,interval 25 second) and ServerDate < ? ";
               PreparedStatement statement = connection.prepareStatement(command);
               statement.setTimestamp(1,new java.sql.Timestamp(now.getTime()));
               statement.setTimestamp(2,new java.sql.Timestamp(now.getTime()));
               Logging.debug("Performance SQL query:"+statement);
               ResultSet resultSet = statement.executeQuery();
               while(resultSet.next()) {
                  java.util.Date first = resultSet.getTimestamp(1);
                  java.util.Date last = resultSet.getTimestamp(2);
                  if (first != null && last != null) {
                     nrecords = nrecords + resultSet.getLong(3);
                     if (first.compareTo(oldest) < 0) {
                        oldest = first;
                     }
                     if (newest == now) {
                        newest = last;
                     } else if (last.compareTo(newest) > 0) {
                        newest = last;
                     }
                  }
               }
            }
            // And now add the duplicates.
            {
               String command = "select min(eventdate),max(eventdate),count(*) from DupRecord where eventdate > date_sub(?,interval 25 second) and eventdate < ? ";
               PreparedStatement statement = connection.prepareStatement(command);
               statement.setTimestamp(1,new java.sql.Timestamp(now.getTime()));
               statement.setTimestamp(2,new java.sql.Timestamp(now.getTime()));
               Logging.debug("Performance SQL query:"+statement);
               ResultSet resultSet = statement.executeQuery();
               while(resultSet.next()) {
                  java.util.Date first = resultSet.getTimestamp(1);
                  java.util.Date last = resultSet.getTimestamp(2);
                  if (first != null && last != null) {
                     nrecords = nrecords + resultSet.getLong(3);
                     if (first.compareTo(oldest) < 0) {
                        oldest = first;
                     }
                     if (newest == now) {
                        newest = last;
                     } else if (last.compareTo(newest) > 0) {
                        newest = last;
                     }
                  }
               }
            }               
         } catch (Exception e) {
            Logging.warning("PerformanceRates: Failed to get performance rates.",e);
         }
         double hours = (newest.getTime() - oldest.getTime()) / 1000.0 / 3600.0;
         if (nrecords == 0) {
            // No recent records
            buffer.append("The collector is current processing 0 records an hour.<p />");
         } else {
            if (hours == 0) {
               hours = 1 / 3600.0;
            }
            buffer.append("The collector is currently processing ");
            records_per_hour = nrecords/hours;
            buffer = decForm.format(nrecords/hours, buffer, pos1);
            buffer.append(" records an hour.<p/>\n");
         }
      }

      // Total processed the last hour
      long totalrecords_onehour = 0;
      try
      {
         // We need to get the information for now (using TableStatistics/current)
         
         long totalrecords_current = 0;
         PreparedStatement statement = connection.prepareStatement(MonitorStatus.fgCountCurrent);
         Logging.debug("PerformanceRates SQL query: " + statement);
         ResultSet resultSet = statement.executeQuery();
         if (resultSet.next()) {
            totalrecords_current = resultSet.getLong(1);
         } else {
            totalrecords_current = 0;
         }
         resultSet.close();
         statement.close();
         
         // Then the information one hour ago (as close as possible).
         java.sql.Timestamp to = new java.sql.Timestamp(now.getTime() - 1 * 3600 * 1000);
         
         statement = connection.prepareStatement(MonitorStatus.fgCountLastHourQuery);
         statement.setTimestamp(1,to);
         statement.setTimestamp(2,to);
         Logging.debug("PerformanceRates SQL query: " + statement);
         resultSet = statement.executeQuery();
         if (resultSet.next()) {
            totalrecords_onehour = totalrecords_current - resultSet.getLong(1);
         } else {
            totalrecords_onehour = totalrecords_current;
         }            
         resultSet.close();
         statement.close();
      }
      catch (Exception e)
      {
         Logging.warning("PerformanceRates: Failed to get number of records processed in the last hour.",e);
      }

      // Variation in the queue size (number of records) in the last hour.
      long queue_variation = records_in_queue;
      try
      {
         PreparedStatement statement = connection.prepareStatement(fgQueueOneHour);
         statement.setTimestamp(1,new java.sql.Timestamp(now.getTime()));
         Logging.debug("PerformanceRates SQL query: " + statement);
         ResultSet resultSet = statement.executeQuery();
         if (resultSet.next()) {
            queue_variation = queue_variation - resultSet.getLong(3);
         }
         resultSet.close();
         statement.close();
      }
      catch (Exception e)
      {
         Logging.warning("PerformanceRates: Failed to get number of records in the queue one hour ago.",e);
      }
      
      // Print the average processing rate for the last hour
      buffer.append("The collector has processed ");
      buffer.append(totalrecords_onehour);
      buffer.append(" records in the last hour.<p/>\n");

      // Print recovery rate (processing rate - incoming rate)
      buffer.append("The queue has varied by ");
      buffer.append(queue_variation);
      buffer.append(" records in the last hour.<p/>\n");

      // Incoming rate is delta in queue length + record processed.
      buffer.append("The collector has received  ");
      buffer.append(totalrecords_onehour + queue_variation);
      buffer.append(" records in the last hour.<p/>\n");
      
      buffer.append("<h4>Catchup based on queue size</h4>\n");

      // Print the estimated time to catch up
      
      // So we received (totalrecords_onehour + queue_variation) records in one hour,
      // if we assume that the incoming rate is steady (it isn't but this is a good approximation).
      // The time to catching up would be with I = incoming rate ((totalrecords_onehour + queue_variation)
      // with Q = current queue size
      // with s = processing speed (records_per_hour)
      // the geometric sum of ( I / s ) multiplied by (Q / s ) (because by the time (Q/s) we finished to 
      // processed the queue we got more new records (Q/s)*I and etc ...
      
      double recovery_time = 0;
      double incoming_rate = totalrecords_onehour + queue_variation;
      if (records_in_queue <= records_per_hour / 60) {
         // Less then one minutes worth of record;
         buffer.append("The collector is <em class=\"good\">up to date</em>.<p/>");
      } else {
         if (records_per_hour == 0) {
            buffer.append("The collector is not processing records.<p/>");
         } else {            
            double incoming_over_speed = incoming_rate / records_per_hour;
//            buffer.append(records_in_queue);
//            buffer.append(".<p />\n");
//            buffer.append(records_per_hour);
//            buffer.append(".<p />\n");
//            buffer.append(incoming_rate);
//            buffer.append(".<p />\n");
//            buffer.append(incoming_over_speed);
//            buffer.append(".<p />\n");
//            buffer.append((records_in_queue / records_per_hour ) );
//            buffer.append(".<p />\n");
//            buffer.append(1 - incoming_over_speed );
//            buffer.append(".<p />\n");
//            appendHours(buffer,(records_in_queue / records_per_hour ) / ( 1 - incoming_over_speed ));
//            buffer.append(".<p />\n");
            if (incoming_over_speed < 1) {
               recovery_time = (records_in_queue / records_per_hour ) / ( 1 - incoming_over_speed );
               buffer.append("The collector should <em class=\"improving\">recover</em> in  ");
               // recovery_time = records_in_queue / records_per_hour;
               appendHours(buffer,recovery_time);
            } else if (incoming_over_speed < 1.0001) {
               buffer.append("The collector is stable");
            } else {
               buffer.append("The collector is <em class=\"problem\">losing ground</em>, about ");
               decForm.format(incoming_rate - records_per_hour, buffer, pos1);
               buffer.append(" records per hour");
            }
            buffer.append(".<p />\n");
         }
      }

      buffer.append("<h4>Catchup based on full service backlog</h4>\n");
      // Get the current service backlog
      String fgGetServiceBacklog = "select sum(serviceBacklog+nRecords) from BacklogStatistics where EntityType != 'local' and ServerDate > date_sub(?,interval 1 day) ";
      String fgGetServiceBacklogOneHour = "select sum(serviceBacklog+nRecords) from BacklogStatisticsSnapshots where EntityType != 'local' " +
                                          " and ServerDate < date_sub(?,interval 1 hour) group by ServerDate order by ServerDate desc limit 1 ";
      long service_backlog_current = 0;
      long service_backlog_one_hour_ago = 0;
      try
      {
         PreparedStatement statement = connection.prepareStatement(fgGetServiceBacklog);
         statement.setTimestamp(1,new java.sql.Timestamp(now.getTime()));
         Logging.debug("PerformanceRates SQL query: " + statement);
         ResultSet resultSet = statement.executeQuery();
         if (resultSet.next()) {
            service_backlog_current = resultSet.getLong(1);
         }
         resultSet.close();
         statement.close();
         
         statement = connection.prepareStatement(fgGetServiceBacklogOneHour);
         statement.setTimestamp(1,new java.sql.Timestamp(now.getTime()));
         Logging.debug("PerformanceRates SQL query: " + statement);
         resultSet = statement.executeQuery();
         if (resultSet.next()) {
            service_backlog_one_hour_ago = resultSet.getLong(1);
         }
         resultSet.close();
         statement.close();         
      }
      catch (Exception e)
      {
         Logging.warning("PerformanceRates: Failed to get the current service backlog.",e);
      }
      buffer.append("The collector has ");
      buffer.append(service_backlog_current);
      buffer.append(" records in external backlog.<p/>\n");
      
      // Print recovery rate (processing rate - incoming rate including the backlog)
      long service_backlog_variation = service_backlog_current - service_backlog_one_hour_ago;
      buffer.append("The collector external backlog has varied by ");
      buffer.append(service_backlog_variation);
      buffer.append(" records in the last hour.<br/>\n");
      buffer.append("The collector external backlog plus queue has varied by ");
      buffer.append(service_backlog_variation + queue_variation);
      buffer.append(" records in the last hour.<p/>\n");
      
      // Print the estimated time to catch up including the backlog

      // So the system is seeing (totalrecords_onehour + service_backlog_variation + queue_variation) more records in one hour,
      // if we assume that the incoming rate is steady (it isn't but this is a good approximation).
      // The time to catching up would be with I = incoming rate ((service_backlog_variation + totalrecords_onehour + queue_variation)
      // with Q = current queue size + service_backlog_current
      // with s = processing speed (records_per_hour)
      // the geometric sum of ( I / s ) multiplied by (Q / s ) (because by the time (Q/s) we finished to 
      // processed the queue we got more new records (Q/s)*I and etc ...
      
      double service_incoming_rate = service_backlog_variation + incoming_rate;
      double service_recovery_time = 0;
      if (service_backlog_current <= records_per_hour / 60) {
         // Less then one minutes worth of record;
         buffer.append("The collector is <em class=\"good\">up to date</em>.<p/>");
      } else {
         if (records_per_hour == 0) {
            buffer.append("The collector is not processing records.<p/>");
         } else {
            double service_incoming_over_speed = service_incoming_rate / records_per_hour;
            if (service_incoming_over_speed < 1 ) {
               service_recovery_time = ( (records_in_queue+service_backlog_current) / records_per_hour ) / ( 1 - service_incoming_over_speed );
               buffer.append("The collector should <em class=\"improving\">recover</em> in  ");
               appendHours(buffer,service_recovery_time);
            } else if (service_incoming_over_speed < 1.0001) {
               buffer.append("The collector is stable");
            } else {
               buffer.append("The collector is <em class=\"problem\">losing ground</em>, about ");
               decForm.format(service_incoming_rate - records_per_hour, buffer, pos1);
               buffer.append(" records per hour");
            }
            buffer.append(".<p />\n");
         }
      }
      
      appendHouseKeeping(buffer, connection, housekeepingDetails);
      
      // Information about how housekeeping is doing (or not).

      try {
         connection.setAutoCommit(false);
      }
      catch (Exception e)
      {
         Logging.warning("PerformanceRates: Failed to commit the transaction.",e);
      }
      return buffer.toString();
   }
}

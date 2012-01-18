package net.sf.gratia.administration;

import net.sf.gratia.util.XP;
import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Logging;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Vector;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import java.util.regex.*;
import java.text.*;

public class MonitorStatus extends HttpServlet 
{
   XP xp = new XP();

   //
   // processing related
   //
   //
   // globals
   //
   boolean initialized = false;
   Properties props;
   //
   // support
   //
   static final SimpleDateFormat fgDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


   public void init(ServletConfig config) throws ServletException 
   {
   }

   public Connection openConnection()
   {
      //
      // database related
      //
      String driver = "";
      String url = "";
      String user = "";
      String password = "";
      try
      {
         props = Configuration.getProperties();
         driver = props.getProperty("service.mysql.driver");
         url = props.getProperty("service.mysql.url");
         user = props.getProperty("service.reporting.user");
         password = props.getProperty("service.reporting.password");
      }
      catch (Exception ignore)
      {
      }
      try
      {
         Class.forName(driver).newInstance();
         Connection connection = DriverManager.getConnection(url,user,password);
         return connection;
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      return null;
   }

   public void closeConnection(Connection connection)
   {
      try
      {
         connection.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
   
   public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
   {
      String probename = null;
      String sitename = null;
      String host = null;
      String allsites = null;
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

      probename = request.getParameter("probename");
      sitename = request.getParameter("sitename");
      allsites = request.getParameter("allsites");
      host = request.getParameter("host");
      boolean xml = false;
      String xmlreq = request.getParameter("xml");
      if (xmlreq != null && xmlreq.equals("yes")) {
         xml = true;
      }
      StringBuffer buffer = new StringBuffer();
      if (allsites != null)
         processAllSites(connection,buffer,xml);
      else if (probename != null)
         processProbe(connection,buffer,probename,xml);
      else if (sitename != null)
         processSite(connection,buffer,sitename,xml);
      else if (host != null)
         processHost(connection,buffer,host,xml);
      else
         process(connection,buffer,xml);
      response.setContentType("text/plain");
      response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
      response.setHeader("Pragma", "no-cache"); // HTTP 1.0
      PrintWriter writer = response.getWriter();
      writer.write(buffer.toString());
      writer.flush();
      writer.close();
      closeConnection(connection);
   }
   
   private static void append(StringBuffer buffer, String what, int thread, long value, boolean xml) 
   {
      if (xml) {
         buffer.append("<"+what+" thread="+thread+">");
         buffer.append(value);
         buffer.append("</"+what+">");
      } else {
         buffer.append(what+thread+"=");
         buffer.append(value);
         buffer.append("|");
      }
   }
   
   private static void append(StringBuffer buffer, String what, long value, boolean xml) 
   {
      if (xml) {
         buffer.append("<"+what+">");
         buffer.append(value);
         buffer.append("</"+what+">");
      } else {
         buffer.append(what+"=");
         buffer.append(value);
         buffer.append("|");
      }
   }
   
   private static void append(StringBuffer buffer, String what, String value, boolean xml) 
   {
      if (xml) {
         buffer.append("<"+what+">");
         buffer.append(value);
         buffer.append("</"+what+">");
      } else {
         buffer.append(what+"=");
         buffer.append(value);
         buffer.append("|");
      }
   }
   
   static final String fgProcessProbeQuery = "select currenttime from Probe where probename = ? ";
   public void processProbe(Connection connection, StringBuffer buffer, String probename, boolean xml)
   {
      try
      {
         //
         // return time stamp of last probe contact
         //
         Logging.log("MonitorStatus SQL query: " + fgProcessProbeQuery + " with " + probename);
         PreparedStatement statement = connection.prepareStatement(fgProcessProbeQuery);
         statement.setString(1,probename);
         ResultSet resultSet = statement.executeQuery();
         java.util.Date date = null;         
         while(resultSet.next())
            date = resultSet.getTimestamp(1);
         resultSet.close();
         statement.close();
         if (date == null)
            append(buffer,"last-contact","never",xml);
         else
            append(buffer,"last-contact",fgDateFormat.format(date),xml);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   static final String fgProcessSiteQuery = "select P.currenttime, P.probename from Probe P, Site T where P.active = 1 and T.SiteName = ? and T.siteid = P.siteid order by currenttime desc";
   public void processSite(Connection connection, StringBuffer buffer, String sitename, boolean xml)
   {
      try
      {
         //
         // return time stamp of last site contact
         //
         Logging.log("MonitorStatus SQL query: " + fgProcessSiteQuery + " with " + sitename);
         PreparedStatement statement = connection.prepareStatement(fgProcessSiteQuery);
         statement.setString(1,sitename);
         ResultSet resultSet = statement.executeQuery();

         java.util.Date date = null;
         String probename = null;
         while(resultSet.next()) {
            date = resultSet.getTimestamp(1);
            probename = resultSet.getString(2);
            if (date == null) {
               if (probename == null) {
                  append(buffer,"last-contact","never",xml);
               } else {
                  append(buffer,"probename",probename,xml);
                  append(buffer,"last-contact","never",xml);
               }
            } else {
               if (probename == null)
                  probename = "Unknown probe";
               append(buffer,"probename",probename,xml);
               append(buffer,"last-contact",fgDateFormat.format(date),xml);
            }
            buffer.append("\n");
         }
         resultSet.close();
         statement.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   static final String fgProcessAllSitesQuery = "select T.SiteName, P.currenttime, P.probename from Probe P, Site T where P.active = 1 and T.siteid = P.siteid order by T.SiteName,P.currenttime desc";
   public void processAllSites(Connection connection, StringBuffer buffer, boolean xml)
   {

      try
      {
         //
         // return time stamp of last site contact
         //
         String site = "";
         java.util.Date date = null;
         String probename = null;
         Logging.log("MonitorStatus SQL query: " + fgProcessAllSitesQuery);
         PreparedStatement statement = connection.prepareStatement(fgProcessAllSitesQuery);
         ResultSet resultSet = statement.executeQuery();
         while(resultSet.next()) {
            site = resultSet.getString(1);
            date = resultSet.getTimestamp(2);
            probename = resultSet.getString(3);
            if (probename == null)
               probename = "Unknown probe";
            append(buffer,"site",site,xml);
            append(buffer,"probename",probename,xml);
            if (date == null) {
               append(buffer,"last-contact","never",xml);
            } else {
               append(buffer,"last-contact",fgDateFormat.format(date),xml);
            }
            buffer.append("\n");
         }
         resultSet.close();
         statement.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }
   
   static final String fgProcessHostQuery = "select max(ServerDate) from JobUsageRecord,JobUsageRecord_Meta where JobUsageRecord.dbid = JobUsageRecord_Meta.dbid and Host = ?";
   public void processHost(Connection connection, StringBuffer buffer, String host, boolean xml)
   {
      try
      {
         //
         // return time stamp of last host contact
         //
         Logging.log("MonitorStatus SQL query: " + fgProcessHostQuery);
         PreparedStatement statement = connection.prepareStatement(fgProcessHostQuery);
         statement.setString(1,host);
         ResultSet resultSet = statement.executeQuery();
         java.util.Date date = null;
         while(resultSet.next())
            date = resultSet.getTimestamp(1);
         resultSet.close();
         statement.close();
         if (date == null)
            append(buffer,"last-contact","never",xml);
         else
            append(buffer,"last-contact",fgDateFormat.format(date),xml);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   static final String fgProcessQueueLengthQuery = "select Queue, Files, Records from CollectorStatus order by Queue";
   public StringBuffer processQueueLength(Connection connection, boolean xml, boolean retry)
   {
      try
      {
         StringBuffer queueBuffer = new StringBuffer();
         PreparedStatement statement = connection.prepareStatement(fgProcessQueueLengthQuery);
         ResultSet resultSet = statement.executeQuery();

         while(resultSet.next()) {
            int q = resultSet.getInt(1);
            long nFiles = resultSet.getLong(2);
            long nRecords = resultSet.getLong(3);
            if (retry && (nFiles < 0 || nRecords < 0) ) {
               // We know something went wrong.  We need to abord, refresh the Status
               // and retry.
               resultSet.close();
               statement.close();
               // RefreshStatus
               if (RefreshCollectorStatus.ExecuteRefresh()) {
                  // Retry
                  return processQueueLength(connection, xml, false);
               } else {
                  // We can't fix it from here, let's display the
                  // wrong numbers :(
                  append(queueBuffer,"queuesize",q,nFiles,xml);
                  append(queueBuffer,"record-queue",q,nRecords,xml);
                  return queueBuffer;
               }
            }
            append(queueBuffer,"queuesize",q,nFiles,xml);
            append(queueBuffer,"record-queue",q,nRecords,xml);
         }
         resultSet.close();
         statement.close();
         return queueBuffer;
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      return null;
   }

   static public final String fgCountCurrent = "select sum(nRecords) from TableStatistics where ValueType = 'lifetime' and RecordType != 'DupRecord' ";
   static public final String fgCountLastHourQuery =  "select sum(maxRecords) from ( select max(maxRecords) as maxRecords from " +
      "( select ValueType,RecordType,Qualifier,max(nRecords) as maxRecords from TableStatisticsSnapshots where ValueType = 'lifetime'" +
      "     and ServerDate <= ? " +
      "     and RecordType != 'DupRecord' " +
      "     group by ValueType,RecordType,Qualifier " +
      "  union select ValueType,RecordType,Qualifier,max(maxRecords) as maxRecords from TableStatisticsHourly where ValueType = 'lifetime'" +
      "     and EndTime <= ? " +
      "     and RecordType != 'DupRecord' " +
      "     group by ValueType,RecordType,Qualifier " + 
      ") forMax " + 
      "group by ValueType,RecordType,Qualifier ) forSum ";

   static final String fgCountLastDayQuery = "select sum(maxRecords) from ( select max(maxRecords) as maxRecords from " +
      "( select ValueType,RecordType,Qualifier,max(nRecords) as maxRecords from TableStatisticsSnapshots where ValueType = 'lifetime'" +
      "     and ServerDate <= ? " +
      "     and RecordType != 'DupRecord' " +
      "     group by ValueType,RecordType,Qualifier" + 
      "  union select ValueType,RecordType,Qualifier,max(maxRecords) as maxRecords from TableStatisticsHourly where ValueType = 'lifetime'" +
      "     and EndTime <= ? " +
      "     and RecordType != 'DupRecord' "+
      "     group by ValueType,RecordType,Qualifier" +
      "  union select ValueType,RecordType,Qualifier,max(maxRecords) as maxRecords from TableStatisticsDaily where ValueType = 'lifetime'" +
      "    and EndTime <= ? " + 
      "    and RecordType != 'DupRecord' " +
      "    group by ValueType,RecordType,Qualifier " + 
      ") forMax " + 
      "group by ValueType,RecordType,Qualifier ) forSum";
   
   public void process(Connection connection, StringBuffer buffer, boolean xml)
   {
      java.util.Date now = new java.util.Date();
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

      long totalrecords_current;
      long totalrecords_onehour;
      long totalrecords_oneday;
      try
      {
         // We need to get the information for now (using TableStatistics/current)

         Logging.debug("MonitorStatus SQL query: " + fgCountCurrent);
         PreparedStatement statement = connection.prepareStatement(fgCountCurrent);
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
         
         statement = connection.prepareStatement(fgCountLastHourQuery);
         statement.setTimestamp(1,to);
         statement.setTimestamp(2,to);
         Logging.debug("MonitorStatus SQL query: " + statement);
         resultSet = statement.executeQuery();
         if (resultSet.next()) {
            totalrecords_onehour = totalrecords_current - resultSet.getLong(1);
         } else {
            totalrecords_onehour = totalrecords_current;
         }            
         append(buffer,"record-count-hour",totalrecords_onehour,xml);
         resultSet.close();
         statement.close();

         // Then the information 24 hour ago (as close as possible).         
         java.sql.Timestamp yesterday = new java.sql.Timestamp(now.getTime() - 24 * 60 * 60 * 1000);
         statement = connection.prepareStatement(fgCountLastDayQuery);
         statement.setTimestamp(1,yesterday);
         statement.setTimestamp(2,yesterday);
         statement.setTimestamp(3,yesterday);
         Logging.debug("MonitorStatus SQL query: " + statement);
         resultSet = statement.executeQuery();
         if (resultSet.next()) {
            totalrecords_oneday = totalrecords_current - resultSet.getLong(1);
         } else {
            totalrecords_oneday = totalrecords_current;
         }            
         append(buffer,"record-count-24hour",totalrecords_oneday,xml);
         resultSet.close();
         statement.close();

      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      StringBuffer queueBuffer = processQueueLength(connection, xml, true);
      if (queueBuffer != null) {
         buffer.append(queueBuffer);
      }
   }
}

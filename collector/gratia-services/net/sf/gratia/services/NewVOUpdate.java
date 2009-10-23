package net.sf.gratia.services;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.util.Properties;
import java.sql.*;
import net.sf.gratia.storage.*;

import org.hibernate.Session;

public class NewVOUpdate {

   public Properties p;
   Statement statement = null;
   ResultSet resultSet = null;

   public void check(Record rec, Session session) throws Exception {
     String voname;
     String reportablevoname;
     
     if (rec instanceof JobUsageRecord) {
       JobUsageRecord record = (JobUsageRecord) rec;
       voname = record.getUserIdentity().getVOName();
       reportablevoname = record.getUserIdentity().getReportableVOName();
     } else if (rec instanceof ComputeElementRecord) {
       ComputeElementRecord record = (ComputeElementRecord)rec;
       voname = record.getVO().getValue();
       reportablevoname = new String(voname);
       Logging.debug("VO from ComputeElementRecord: " + voname);
     } else {
       return;
     }

      java.sql.Connection connection = session.connection();

      String dq = "\"";
      String comma = ",";
      
      String comparisonString;
      if (reportablevoname == null) {
         comparisonString = "is null";
      } else {
         comparisonString = "= " + dq + reportablevoname + dq;
      }
      int VOid = -1;

      //
      // see if the probe exists
      //
      String command = "select count(*) from VONameCorrection where" +
            " binary voname = binary " + dq + voname + dq +
            " and binary reportablevoname " + comparisonString;
      try {
         int icount = 0;
         Logging.debug("NewVOUpdate: executing " + command);
         statement = connection.prepareStatement(command);
         resultSet = statement.executeQuery(command);
         while (resultSet.next()) {
            icount = resultSet.getInt(1);
         }
         resultSet.close();
         statement.close();
         //
         // already there - just exit
         //
         if (icount > 0) {
            return;
         }
      } catch (Exception e) {
         //
         // communications error
         //
         if (HibernateWrapper.databaseUp()) {
            Logging.warning("NewVOUpdate: Processing Error, command " +
                  command + ", exception " + e);
            Logging.debug("Exception detail: ", e);
         }
         throw new Exception("NewVOUpdate: No Connection");
      }
      try {
         //
         // Otherwise check for an existing entry for this VO to put in a default mapping
         //
         String votablekey;
         if (voname.startsWith("/")) { // FQAN -- use ReportableVOName for default instead
            votablekey = reportablevoname;
         } else {
            votablekey = voname;
         }

         command = "select VOid from VO where VOName = " + dq + votablekey + dq;
         Logging.debug("NewVOUpdate: executing " + command);
         statement = connection.prepareStatement(command);
         resultSet = statement.executeQuery(command);
         while (resultSet.next()) {
            VOid = resultSet.getInt(1);
         }
         resultSet.close();
         statement.close();
         //
         // if VOid == -1 it doesn't exist - add it to VO table
         //
         if (VOid == -1) {
            command = "insert into VO(VOName) values(" + dq + votablekey + dq + ")";
            Logging.debug("NewVOUpdate: executing " + command);
            statement = connection.createStatement();
            statement.executeUpdate(command);
            statement.close();
            Logging.debug("NewVOUpdate: executing " + command);
            command = "select VOid from VO where VOName = " + dq + votablekey + dq;
            statement = connection.prepareStatement(command);
            resultSet = statement.executeQuery(command);
            while (resultSet.next()) {
               VOid = resultSet.getInt(1);
            }
            resultSet.close();
            statement.close();
         }
         //
         // now add a new entry to VONameCorrection with default values
         //
         command =
               "insert into VONameCorrection(VOid,VOName,ReportableVOName) values(" +
               VOid + comma + dq + voname + dq + comma +
               ((reportablevoname == null) ? "null" : (dq + reportablevoname + dq)) + ")";
         Logging.debug("NewVOUpdate: executing " + command);
         statement = connection.createStatement();
         statement.executeUpdate(command);
         statement.close();
      } catch (Exception e) {
         //
         // communications error
         //
         if (HibernateWrapper.databaseUp()) {
            Logging.warning("NewVOUpdate: Processing Error, command " +
                  command + ", exception " + e);
            Logging.debug("Exception detail: ", e);
         }
         throw new Exception("NewVOUpdate: No Connection");
      }
   }
}

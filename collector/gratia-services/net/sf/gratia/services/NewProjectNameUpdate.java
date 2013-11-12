package net.sf.gratia.services;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.util.Properties;
import java.sql.*;
import net.sf.gratia.storage.*;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;


public class NewProjectNameUpdate {

   public Properties p;
   Statement statement = null;
   ResultSet resultSet = null;

   public void check(final Record rec, Session session) throws Exception {

       session.doWork(new Work() {
               @Override
                   public void execute(java.sql.Connection connection) throws SQLException {





     // John Weigand 8/9/12 - special note
     // The StringElement class (from the beginning of time in Gratia) appends
     // the ProjectNameDescription to the value if such a column exists.
     // Knowing no way to really bypass this for just the Project Name, I use
     // the getValue method of that class to retrieve ProjectName.  This is
     // something one must be aware of when making any changes in this class.
     String        ProjectNameValue = ""; 
     StringElement JURProjectName;
     int    ProjectNameCorrid = -1;
     
     if (rec instanceof JobUsageRecord) {
       JobUsageRecord record = (JobUsageRecord) rec;
       JURProjectName = record.getProjectName();
       if ( JURProjectName == null ) {
         // This indicates that no ProjectName was present on the JUR.
         // This check is necessary to avoid getting a NullPointerException on 
         // the 'else' JURProjectName.getValue call.
         // We must still continue processing so that the ProjectNameCorrection
         // table gets updated, most likely the first time.
         Logging.debug("NewProjectNameUpdate: No ProjectName on JobUsageRecord");
       } else {
         ProjectNameValue = JURProjectName.getValue();
         Logging.debug("NewProjectNameUpdate: ProjectName from JobUsageRecord: " + ProjectNameValue);
       }
     } else {
       return;
     }

     //java.sql.Connection connection = session.connection();

      String dq = "\"";
      String comma = ",";
      
      String comparisonString;
      if ( JURProjectName == null ) {
        comparisonString = "ProjectName is NULL ";
      } else {
        comparisonString = "binary ProjectName = " + dq + ProjectNameValue + dq; 
      }

      //
      // see if the project name exists
      //
      String command = "select count(*) from ProjectNameCorrection where " + comparisonString;
      try {
         int icount = 0;
         Logging.debug("NewProjectNameUpdate: executing " + command);
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
            Logging.warning("NewProjectNameUpdate: Processing Error, command " +
                  command + ", exception " + e);
            Logging.debug("Exception detail: ", e);
         }
         //throw new Exception("NewProjectNameUpdate: No Connection");
      }
      //
      // Otherwise create a new entry
      //
      Logging.info("NewProjectNameUpdate: new ProjectName(" + ProjectNameValue + ")"); 
      String values;
      try {
         if ( JURProjectName == null ) {
           values = "values(NULL," + dq + "Unknown" + dq + ")";
         } else {
           values = "values(" + dq + ProjectNameValue + dq + "," + dq + ProjectNameValue + dq + ")";
         }
         command = "insert into ProjectNameCorrection(ProjectName,ReportableProjectName) " + values + ";";
         Logging.debug("NewProjectNameUpdate: executing " + command);
         statement = connection.createStatement();
         statement.executeUpdate(command);
         statement.close();
      } catch (Exception e) {
         //
         // communications error
         //
         if (HibernateWrapper.databaseUp()) {
            Logging.warning("NewProjectNameUpdate: Processing Error, command " +
                  command + ", exception " + e);
            Logging.debug("Exception detail: ", e);
         }
         //throw new Exception("NewProjectNameUpdate: No Connection");
      }



	       } // end of execute                                                                                                                                                                                             
           }); // end of doWork( new Work() {                                                                                                                                                                                  



   }
}

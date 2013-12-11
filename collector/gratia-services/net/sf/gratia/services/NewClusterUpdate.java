package net.sf.gratia.services;

import net.sf.gratia.util.Logging;

import java.util.Properties;
import java.sql.*;
import net.sf.gratia.storage.*;

import org.hibernate.Session;

public class NewClusterUpdate {

  public Properties p;
  PreparedStatement statement = null;
  ResultSet resultSet = null;

  /**
   * Check to see if a cluster name already exists.
   * 
   * Performs the following against the DB:
   * 1) See if the cluster name has ever occurred before in the correction table.  If so, return.
   * 2) If not, insert the cluster name into the cluster table.
   * 3) Map the cluster name to itself in the correction table.
   * 
   * This only takes affect when the record is a ComputeElement record.
   * 
   * This implementation is NOT thread safe with regards to database usage
   * @param rec
   * @param session
   * @throws Exception
   */

  public synchronized void check(Record rec, Session session) throws Exception {

    // Only looking at compute element records
    if (!(rec instanceof ComputeElement)) {
      return;
    }

    ComputeElement record = (ComputeElement)rec;
    String clusterName = record.getCluster().getValue();

    java.sql.Connection connection = session.connection();

    // If there is no cluster attached, we punt.
    if (clusterName == null) {
      return;
    }
    int clusterid = -1;


    String command = "select count(*) from ClusterNameCorrection where" + 
    " ClusterName = ? ";
    try {
      int icount = 0;
      Logging.debug("NewClusterUpdate: executing " + command);
      statement = connection.prepareStatement(command);
      statement.setString(1, clusterName);
      resultSet = statement.executeQuery();
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
        Logging.warning("NewClusterUpdate: Processing Error, command " +
            command + ", exception " + e);
        Logging.debug("Exception detail: ", e);
      }
      throw new Exception("NewClusterUpdate: No Connection");
    }
    try {
      //
      // Check for an existing entry for this cluster in Cluster table.
      // If the existing entry does not exist, add it.
      //

      command = "select clusterid from Cluster where name = ?";
      Logging.debug("NewClusterUpdate: executing " + command);
      statement = connection.prepareStatement(command);
      statement.setString(1, clusterName);
      resultSet = statement.executeQuery();
      while (resultSet.next()) {
        clusterid = resultSet.getInt(1);
      }
      resultSet.close();
      statement.close();
      //
      // if clusterid == -1, this is a new cluster - add it to cluster name correction
      // table with the default mapping (to itself).
      //
      if (clusterid == -1) {
        command = "insert into Cluster(name) values (?)";
        Logging.debug("NewClusterUpdate: executing " + command);
        statement = connection.prepareStatement(command);
        statement.setString(1, clusterName);
        statement.executeUpdate();
        statement.close();
        Logging.debug("NewClusterUpdate: executing " + command);
        command = "select clusterid from Cluster where name = ?";
        statement = connection.prepareStatement(command);
        statement.setString(1, clusterName);
        resultSet = statement.executeQuery();
        while (resultSet.next()) {
          clusterid = resultSet.getInt(1);
        }
        resultSet.close();
        statement.close();
      }
      //
      // now add a new entry to ClusterNameCorrection with default values
      //
      command = "insert into ClusterNameCorrection (clusterid, ClusterName) values (?, ?)";
      Logging.debug("NewClusterUpdate: executing " + command);
      statement = connection.prepareStatement(command);
      statement.setInt(1, clusterid);
      statement.setString(2, clusterName);
      statement.executeUpdate();
      statement.close();
    } catch (Exception e) {
      //
      // communications error
      //
      if (HibernateWrapper.databaseUp()) {
        Logging.warning("NewClusterUpdate: Processing Error, command " +
            command + ", exception " + e);
        Logging.debug("Exception detail: ", e);
      }
      throw new Exception("NewClusterUpdate: No Connection");
    }
  }
}

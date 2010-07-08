package net.sf.gratia.services;

import java.sql.*;
import java.util.*;
import java.io.*;
import net.sf.gratia.util.Execute;
import net.sf.gratia.util.Logging;
import net.sf.gratia.util.LogLevel;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.*;

/*
Author: Karthik

Date: Wed, Feb 4th 2010

Purpose: Java code to generate and execute the needed SQL statements for upgrading the gratia database schema from version 85 to 86.
         The DROP sql queries have to be generated, since in the MySQL information schema, the name of the foreign keys are random and it will vary from one schema to the next.
         Hence dropping of the foreign key will work only if the query is generated dynamically.
         Example: ALTER TABLE Probe DROP foreign key FK49CD790E1AC33EA.
         This mainly has to do with the conversion of int to bigint for most of the id variables in the tables.
         Anyone using the version 85 database schema has to use this java code to upgrade their schema to 86.
         This code will be eventually called from the DatabaseMaintenance.java code .

Algorithm:   
   It isn't possible to directly do an alter on the data type from int to bigint for the id columns, since they have foreign key constraints
   So do the following:
      1) Drop all the foreign key constraints from the schema
      2) Use the alter table command to convert all appropriate ids from int to bigint
      3) Add all the foreign key constraints back to the schema 

Program logic: 
        - define a list of MODIFY int to bigint queries 
        - define a list of ADD FOREIGN KEY queries
	- Generate a list of DROP FOREIGN KEY constraints sql statements
        - Add the MODIFY int to bigint statements to this list 
        - Fuse/combine/glue separate DROP and MODIFY queries together into a single query
        - Filter out the DROP and MODIFY queries, only to retain those that need to be executed (by checking the database schema and making a determination if the condition resulting from the execution of the query is already satisfied or not). This will enable us to re-start the upgrade from where it was left off before (in case the upgrade crashed somewhere previously, without having to do the upgrade all over again from scratch 
        - Execute the DROP and MODIFY queries
        - Filter out the ADD FOREIGN KEY queries (procedure is similar to the one used above for filtering DROP and MODIFY queries)
        - Execute the ADD FOREIGN KEY queries
         	- The reason ADD FOREIGN KEY queries are processed after execution of DROP and MODIFY queries is that the execution of DROP and MODIFY queries will affect which ADD FOREIGN KEY query needs to be executed
*/

public class Upgrade86 {

   /*********************************************************************  VARIABLES ****************************************************************************************/

   private String logPrefix = "Upgrade86.java: ";

   private Connection connection = null;

   //Tables affected in the int to bigint modification
   //Only these tables are considered for different queries
   private final String[] affectedTables = {"CPUInfo", "Certificate", "Cluster", "ClusterNameCorrection", "ComputeElement", "ComputeElementRecord", "ComputeElementRecord_Meta", "ComputeElementRecord_Origin", "ComputeElement_Meta", "ComputeElement_Origin", "ConsumableResource", "Disk", "DupRecord", "JobUsageRecord", "JobUsageRecord_Meta", "JobUsageRecord_Origin", "JobUsageRecord_Xml", "MasterServiceSummary", "MasterServiceSummaryHourly", "MasterSummaryData", "MasterTransferSummary", "Memory", "MetricRecord", "MetricRecord_Meta", "MetricRecord_Origin", "MetricRecord_Xml", "Network", "NodeSummary", "PhaseResource", "Probe", "ProbeDetails", "ProbeDetails_Meta", "ProbeDetails_Origin", "ProbeDetails_Xml", "ProbeSoftware", "Replication", "Resource", "ServiceLevel", "Site", "Software", "StorageElement", "StorageElementRecord", "StorageElementRecord_Meta", "StorageElementRecord_Origin", "StorageElement_Meta", "StorageElement_Origin", "Subcluster", "Subcluster_Meta", "Subcluster_Origin", "Swap", "SystemProplist", "TDCorr", "TimeDuration", "TimeInstant", "trace", "TransferDetails", "VO", "VONameCorrection", "VolumeResource"};


   /*

   //useful tips - notes

   TableName; auto_increment column as extracted from the schema 85 structure
   ===========================================================================
   CPUInfo; HostId
   Certificate; certid
   Cluster; clusterid
   ClusterNameCorrection; corrid
   ComputeElement; dbid
   ComputeElementRecord; dbid
   DupRecord; dupid
   JobUsageRecord; dbid
   MasterServiceSummary; dbid
   MasterServiceSummaryHourly; dbid
   MasterSummaryData; SummaryID
   MasterTransferSummary; TransferSummaryID
   MetricRecord; dbid
   NodeSummary; NodeSummaryID
   Probe; probeid
   ProbeDetails; dbid
   Replication; replicationid
   Site; siteid
   Software; dbid
   StorageElement; dbid
   StorageElementRecord; dbid
   Subcluster; dbid
   SystemProplist; propid
   trace; traceid
   TransferDetails; TransferDetailsId
   VO; VOid
   VONameCorrection; corrid

   //useful sql statements related to this program
   //sample query to check if a table.column is of type auto_increment

   mysql> select EXTRA from information_schema.COLUMNS where table_schema = DATABASE() and table_name='CPUInfo' and column_name='HostId';
   +----------------+
   | EXTRA          |
   +----------------+
   | auto_increment |
   +----------------+
   1 row in set (0.00 sec)

   //query to check gratiaDatabaseVersion:
   mysql> select cdr from SystemProplist where car ="gratia.database.version";
   +------+
   | cdr  |
   +------+
   | 85   | 
   +------+
   1 row in set (0.54 sec)

   //Find the data type of a particular table's field
   //purpose: Check the data type to see if it has already been altered. If so, it could be skipped.
   //This will be useful for repeatibility in case something goes wrong and the script bails out in the middle
   //We can avoid repeating the foreign key drop
   mysql> select DATA_TYPE from information_schema.columns where TABLE_SCHEMA = Database() and TABLE_NAME = 'Probe' and COLUMN_NAME = 'probeid';
   +-----------+
   | DATA_TYPE |
   +-----------+
   | bigint    |
   +-----------+
   1 row in set (0.00 sec)

   //Determine if a foreign key exists or not so that we can conditionally execute a "ADD FOREIGN KEY" query (as shown below) only if needed
   //ALTER TABLE ProbeDetails_Meta ADD FOREIGN KEY (probeid) REFERENCES Probe(probeid), ADD FOREIGN KEY (dbid) REFERENCES ProbeDetails(dbid)
   select TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME from information_schema.key_column_usage where TABLE_SCHEMA = Database() and (CONSTRAINT_NAME like 'FK%' or CONSTRAINT_NAME like '%fk%') and TABLE_NAME='ProbeDetails_Meta' and COLUMN_NAME='probeid' and REFERENCED_TABLE_NAME='Probe' and REFERENCED_COLUMN_NAME='probeid'; 
   select TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME from information_schema.key_column_usage where TABLE_SCHEMA = Database() and (CONSTRAINT_NAME like 'FK%' or CONSTRAINT_NAME like '%fk%') and TABLE_NAME='ComputeElement_Origin'; 

   */

   private final String[] modifyQueriesArray = {
      "ALTER TABLE CPUInfo MODIFY HostId BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE Certificate MODIFY pem MEDIUMTEXT",
      "ALTER TABLE Cluster MODIFY clusterid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE ClusterNameCorrection MODIFY corrid BIGINT(20) AUTO_INCREMENT, MODIFY clusterid BIGINT(20)",
      "ALTER TABLE ComputeElement MODIFY dbid BIGINT(20) AUTO_INCREMENT, MODIFY probeid BIGINT(20)",
      "ALTER TABLE ComputeElementRecord MODIFY probeid BIGINT(20), MODIFY dbid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE ComputeElementRecord_Meta MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE ComputeElementRecord_Origin MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE ComputeElement_Meta MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE ComputeElement_Origin MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE ConsumableResource MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE Disk MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE DupRecord MODIFY rawxml MEDIUMTEXT, MODIFY dbid BIGINT(20), MODIFY dupid BIGINT(20) AUTO_INCREMENT, MODIFY extraxml MEDIUMTEXT",
      "ALTER TABLE JobUsageRecord MODIFY dbid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE JobUsageRecord_Meta MODIFY probeid BIGINT(20), MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE JobUsageRecord_Origin MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE JobUsageRecord_Xml MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE MasterServiceSummary MODIFY dbid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE MasterServiceSummaryHourly MODIFY dbid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE MasterSummaryData MODIFY SummaryID BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE MasterTransferSummary MODIFY TransferSummaryID BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE Memory MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE MetricRecord MODIFY DetailsData MEDIUMTEXT, MODIFY dbid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE MetricRecord_Meta MODIFY probeid BIGINT(20) NOT NULL, MODIFY dbid BIGINT(20) ",
      "ALTER TABLE MetricRecord_Origin MODIFY dbid BIGINT(20)  NOT NULL",
      "ALTER TABLE MetricRecord_Xml MODIFY dbid BIGINT(20)  NOT NULL",
      "ALTER TABLE Network MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE NodeSummary MODIFY NodeSummaryID BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE PhaseResource MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE Probe MODIFY siteid BIGINT(20) NOT NULL, MODIFY probeid BIGINT(20) AUTO_INCREMENT, MODIFY nRecords BIGINT(20), MODIFY nDuplicates BIGINT(20)",
      "ALTER TABLE ProbeDetails MODIFY dbid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE ProbeDetails_Meta MODIFY probeid BIGINT(20), MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE ProbeDetails_Origin MODIFY dbid BIGINT(20)  NOT NULL",
      "ALTER TABLE ProbeDetails_Xml MODIFY dbid BIGINT(20)  NOT NULL",
      "ALTER TABLE ProbeSoftware MODIFY softid BIGINT(20) NOT NULL, MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE Replication MODIFY replicationid BIGINT(20) AUTO_INCREMENT, MODIFY dbid BIGINT(20) NOT NULL, MODIFY rowcount BIGINT(20) NOT NULL",
      "ALTER TABLE Resource MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE ServiceLevel MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE Site MODIFY siteid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE Software MODIFY dbid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE StorageElementRecord_Meta MODIFY dbid BIGINT(20) ",
      "ALTER TABLE StorageElementRecord MODIFY probeid BIGINT(20), MODIFY dbid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE StorageElementRecord_Origin MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE StorageElement_Meta MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE StorageElement MODIFY probeid BIGINT(20), MODIFY dbid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE StorageElement_Origin MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE Subcluster_Meta MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE Subcluster MODIFY probeid BIGINT(20), MODIFY dbid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE Subcluster_Origin MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE Swap MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE SystemProplist MODIFY propid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE TDCorr MODIFY TransferDetailsId BIGINT(20) NOT NULL, MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE TimeDuration MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE TimeInstant MODIFY dbid BIGINT(20) NOT NULL",
      "ALTER TABLE trace MODIFY traceid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE TransferDetails MODIFY TransferDetailsId BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE VO MODIFY VOid BIGINT(20) AUTO_INCREMENT",
      "ALTER TABLE VONameCorrection MODIFY corrid BIGINT(20) AUTO_INCREMENT, MODIFY VOid BIGINT(20)",
      "ALTER TABLE VolumeResource MODIFY dbid BIGINT(20) NOT NULL"
   };

   private final String[] addForeignKeysArray = {
      "ALTER TABLE Connection ADD FOREIGN KEY (certid) REFERENCES Certificate(certid)",
      "ALTER TABLE ConnectionTable ADD FOREIGN KEY (certid) REFERENCES Certificate(certid)",
      "ALTER TABLE ComputeElement_Origin ADD FOREIGN KEY (dbid) REFERENCES ComputeElement(dbid), ADD FOREIGN KEY (originid) REFERENCES Origin(originid)",
      "ALTER TABLE ComputeElement_Meta ADD FOREIGN KEY (dbid) REFERENCES ComputeElement(dbid)",
      "ALTER TABLE ComputeElementRecord_Origin ADD FOREIGN KEY (dbid) REFERENCES ComputeElementRecord(dbid), ADD FOREIGN KEY (originid) REFERENCES Origin(originid)",
      "ALTER TABLE ComputeElementRecord_Meta ADD FOREIGN KEY (dbid) REFERENCES ComputeElementRecord(dbid)",
      "ALTER TABLE Resource ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid)",
      "ALTER TABLE TimeInstant ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid)",
      "ALTER TABLE JobUsageRecord_Xml ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid)",
      "ALTER TABLE JobUsageRecord_Meta ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid), ADD FOREIGN KEY (probeid) REFERENCES Probe(probeid)",
      "ALTER TABLE TDCorr ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid), ADD FOREIGN KEY (TransferDetailsId) REFERENCES TransferDetails(TransferDetailsId)",
      "ALTER TABLE Memory ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid)",
      "ALTER TABLE PhaseResource ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid)",
      "ALTER TABLE ServiceLevel ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid)",
      "ALTER TABLE ConsumableResource ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid)",
      "ALTER TABLE Disk ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid)",
      "ALTER TABLE VolumeResource ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid)",
      "ALTER TABLE Swap ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid)",
      "ALTER TABLE Network ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid)",
      "ALTER TABLE TimeDuration ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid)",
      "ALTER TABLE JobUsageRecord_Origin ADD FOREIGN KEY (dbid) REFERENCES JobUsageRecord(dbid), ADD FOREIGN KEY (originid) REFERENCES Origin(originid)",
      "ALTER TABLE MetricRecord_Origin ADD FOREIGN KEY (dbid) REFERENCES MetricRecord(dbid), ADD FOREIGN KEY (originid) REFERENCES Origin(originid)",
      "ALTER TABLE MetricRecord_Xml ADD FOREIGN KEY (dbid) REFERENCES MetricRecord(dbid)",
      "ALTER TABLE MetricRecord_Meta ADD FOREIGN KEY (dbid) REFERENCES MetricRecord(dbid), ADD FOREIGN KEY (probeid) REFERENCES Probe(probeid)",
      "ALTER TABLE StorageElement_Origin ADD FOREIGN KEY (originid) REFERENCES Origin(originid), ADD FOREIGN KEY (dbid) REFERENCES StorageElement(dbid)",
      "ALTER TABLE StorageElementRecord_Origin ADD FOREIGN KEY (originid) REFERENCES Origin(originid), ADD FOREIGN KEY (dbid) REFERENCES StorageElementRecord(dbid)",
      "ALTER TABLE Subcluster_Origin ADD FOREIGN KEY (originid) REFERENCES Origin(originid), ADD FOREIGN KEY (dbid) REFERENCES Subcluster(dbid)",
      "ALTER TABLE ProbeDetails_Origin ADD FOREIGN KEY (originid) REFERENCES Origin(originid), ADD FOREIGN KEY (dbid) REFERENCES ProbeDetails(dbid)",
      "ALTER TABLE ComputeElement ADD FOREIGN KEY (probeid) REFERENCES Probe(probeid)",
      "ALTER TABLE StorageElement ADD FOREIGN KEY (probeid) REFERENCES Probe(probeid)",
      "ALTER TABLE ProbeDetails_Meta ADD FOREIGN KEY (probeid) REFERENCES Probe(probeid), ADD FOREIGN KEY (dbid) REFERENCES ProbeDetails(dbid)",
      "ALTER TABLE ComputeElementRecord ADD FOREIGN KEY (probeid) REFERENCES Probe(probeid)",
      "ALTER TABLE Subcluster ADD FOREIGN KEY (probeid) REFERENCES Probe(probeid)",
      "ALTER TABLE StorageElementRecord ADD FOREIGN KEY (probeid) REFERENCES Probe(probeid)",
      "ALTER TABLE ProbeDetails_Xml ADD FOREIGN KEY (dbid) REFERENCES ProbeDetails(dbid)",
      "ALTER TABLE ProbeSoftware ADD FOREIGN KEY (dbid) REFERENCES ProbeDetails(dbid), ADD FOREIGN KEY (softid) REFERENCES Software(dbid)",
      "ALTER TABLE Probe ADD FOREIGN KEY (siteid) REFERENCES Site(siteid)",
      "ALTER TABLE StorageElement_Meta ADD FOREIGN KEY (dbid) REFERENCES StorageElement(dbid)",
      "ALTER TABLE StorageElementRecord_Meta ADD FOREIGN KEY (dbid) REFERENCES StorageElementRecord(dbid)",
      "ALTER TABLE Subcluster_Meta ADD FOREIGN KEY (dbid) REFERENCES Subcluster(dbid)"
   };
	
   //variable to store the affected tables in an ArrayList
   //The reason for storing this in the arraylist is that later when we want to drop a foreign key, we can decide
   //if this drop has to be done by checking the table related to that key against this affected tables list
   //If the table is found, then the drop has to be done. Otherwise it can be safely skipped.
   //Also ArrayList obviously provides more flexibility than Arrays, like adding entries, adding lists together, removing entries etc.

   //variables to store the different queries
   private ArrayList<String> dropForeignKeysArrayList = new ArrayList<String>();

   /*********************************************************************  METHODS  ****************************************************************************************/

   //Method to get the stack trace as a String so that the entire stack trace could be logged to a log file etc.
   public String getStackTrace(Throwable t)
   {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw, true);
      t.printStackTrace(pw);
      pw.flush();
      sw.flush();
      return sw.toString();
   }

   /*
     Helper method to analyze the FOREIGN KEY constraints from the db schema and then construct the appropriate DROP FOREIGN KEY queries 
   */ 
   public void upgradeHelper() throws SQLException, ClassNotFoundException
   {
      Logging.log(logPrefix + " upgradeHelper");
      //Example of how a constraint table looks like
      //+--------------------+-----------------------------+-------------------+-----------------------+------------------------+
      //| CONSTRAINT_NAME    | TABLE_NAME                  | COLUMN_NAME       | REFERENCED_TABLE_NAME | REFERENCED_COLUMN_NAME |
      //+--------------------+-----------------------------+-------------------+-----------------------+------------------------+
      //| FK48965AFE4E9C2EB5 | Connection                  | certid            | Certificate           | certid                 |
      //| FKEF8E1F704E9C2EB5 | ConnectionTable             | certid            | Certificate           | certid                 |
      //-------------------------------------------------------------------------------------------------------------------------

      //We need to extract this constraints and then store them into a LinkedHashMap and then eventually construct add and drop foreign key queries using the LinkedHashMap 
      //Why dynamic add and drop foreign key queries?
      //	Because we have to drop the foreign keys by name and the names of these foreign keys are not anything predictable. We need to extract them dynamically
      //	and put them in the drop foreign key query

      //Why LinkedHashMap?
      //	Because for one table, we would like to combine all the relevant columns to the drop and add foreign key constraints.
      //	LinkedHashMap could be conveniently used for this purpose, by using the dependent table's name as a unique key and the other data added as values
      //	Why?
      //	  	Because every drop and add foreign key operation has a overhead that depends on the number of records in that particular table
      //		Executing them separately means the overhead is multiplied that many times
      //		Hence the reason we want to combine them into one single operation/query per table 


      //This is the query that extracts the above type of table containing the foreign keys
      String query = "select CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME from information_schema.key_column_usage where TABLE_SCHEMA = Database() and (CONSTRAINT_NAME like 'FK%' or CONSTRAINT_NAME like '%fk%') order by REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME";

      Logging.log(logPrefix + " Executing: " + query);
      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      getConstraints(rs);
      rs.close();
   }

   public void getConstraints(ResultSet rs) throws SQLException
   {
      Logging.log(logPrefix + " getConstraints");
      //Use a LinkedHashMap in which you can store all the result rows and columns i.e. the entire table for the above query
      //Define a LinkedHashMap, which has a String as a key and a ArrayList<String> as its values
      //Sorting is automatically defined by its natural order of sorting for Strings
      //Reason for using a LinkedHashMap: We want to store the foreign key's dependent table name as a key and then for the value
      //we want to add all the column names as the values in the ArrayList value corresponding to this key 

      //The eventual structure of the data in the LinkedHashMap will look like the sample below:
      //{Probe=[[Probe_ibfk_2, Probe, siteid, Site, siteid], [Probe_ibfk_1, Probe, siteid, Site, siteid]], ProbeDetails_Meta=[[ProbeDetails_Meta_ibfk_1, ProbeDetails_Meta, probeid, Probe, probeid], [ProbeDetails_Meta_ibfk_2, ProbeDetails_Meta, dbid, ProbeDetails, dbid]]}

      //Probe is the key and [[Probe_ibfk_2, Probe, siteid, Site, siteid], [Probe_ibfk_1, Probe, siteid, Site, siteid]] is the ArrayList containing two other ArrayLists
      //From this finally we want to construct combined 'alter drop' and 'alter add' queries as shown below.

      //"Alter table Probe drop foreign key Probe_ibfk_2, drop foreign key Probe_ibfk_1"
      //"Alter table Probe add foreign key siteid references Site(siteid), add foreign key..."

      LinkedHashMap<String, ArrayList<ArrayList<String>>> ar = new LinkedHashMap<String, ArrayList<ArrayList<String>>>(); 

      String dtab = ""; //dependent table that will be used as a *key* for the LinkedHashMap
      HashMap<String,Integer> dtabCount = new HashMap<String,Integer>();
      int count = 0;
	 
      while(rs.next())
         {
            dtab = rs.getString(2);

            //if key doesn't already exist then create a new entry in the Map
            //We need to do this check because we don't want to overwrite an existing entry in the LinkedHashMap 
            if(!ar.containsKey(dtab))
               {
                  //this is the 1st occurance of dtab - index is 0 for ArrayList
                  count = 0;

                  //Create the 2D ArrayList to hold the foreign key row information  (1 Arraylist per row), keyed by dtab
                  ar.put(dtab, new ArrayList<ArrayList<String>>()); 
               }
            else
               {
                  //dtab is already there and the new ArrayList has already been created. 
                  //Hence get the existing count, increment it and overwrite it by the incremented value 
                  count = dtabCount.get(dtab);
                  count++;
               }

            dtabCount.put(dtab, count);  //keep count of the dtab. This count will be used as an index to dynamically obtain the reference to the array list

            ar.get(dtab).add(new ArrayList<String>());

            //Get this ArrayList using the key and add a sigle row's result to this ArrayList
            ar.get(dtab).get(count).add(rs.getString(1)); //1 = get 1st column in resultSet - foreign key
            ar.get(dtab).get(count).add(rs.getString(2)); //dependent table
            ar.get(dtab).get(count).add(rs.getString(3)); //dependent column
            ar.get(dtab).get(count).add(rs.getString(4)); //referenced table
            ar.get(dtab).get(count).add(rs.getString(5)); //referenced column
         }

      constructQuery(ar);
   }

   /*
     - Construct DROP FOREIGN KEY queries from the available TABLE CONSTRAINTS
     - Later these DROP queries will be glued together into a single query with the MODIFY data type queries 
   */
   public void constructQuery(LinkedHashMap<String, ArrayList<ArrayList<String>>> ar)
   {
      ArrayList<String> affectedTablesArrayList = new ArrayList<String>(Arrays.asList(affectedTables)); 

      Logging.log(logPrefix + " constructQuery");

      //Example of the input LinkedHashMap data structure
      //{Probe=[[Probe_ibfk_2, Probe, siteid, Site, siteid], [Probe_ibfk_1, Probe, siteid, Site, siteid]], ProbeDetails_Meta=[[ProbeDetails_Meta_ibfk_1, ProbeDetails_Meta, probeid, Probe, probeid], [ProbeDetails_Meta_ibfk_2, ProbeDetails_Meta, dbid, ProbeDetails, dbid]]}

      String fkey = ""; 
      String dcol = ""; 
      String rtab = ""; 
      String rcol = ""; 
      ArrayList <String> duplicateCheck = new ArrayList<String>();

      for(String dtab : ar.keySet())
         {
            String dropQuery = "";
            String dropPartialQuery = "";
            String tmpStr = "";
            String addQuery = "";
            String addPartialQuery = "";
            String prefix = "ALTER TABLE " + dtab;

            for(ArrayList<String> a1 : ar.get(dtab))
               {
                  fkey = a1.get(0);
                  //dtab = a1.get(1) //We can ignore this, since we already have this stored as a key
                  dcol = a1.get(2);
                  rtab = a1.get(3);
                  rcol = a1.get(4);

                  //Do the drop and add foreign keys only if at least one of these tables is in the affected tables list
                  if (affectedTablesArrayList.contains(dtab) || affectedTablesArrayList.contains(rtab))
                     {
                        tmpStr = "DROP FKEY " + fkey;
                        if(!duplicateCheck.contains(tmpStr))
                           {
                              dropPartialQuery+= " DROP FOREIGN KEY " + fkey + ", ";
                              //dropPartialQuery+= " DROP KEY " + fkey + ", ";
                              duplicateCheck.add(tmpStr);
                           }

                        tmpStr = "ADD FKEY " + dtab + "," + dcol + "," +  rtab + "," +  rcol; //just construct a reference string to add into the duplicate ArrayList. Adding the whole partial
                        //query string isn't working when coming to a duplicate check, because of all the paranthesis etc.
                        //For some reason the duplicate check fails. Hence using a simple string to identify this entry.
                        if(!duplicateCheck.contains(tmpStr))
                           {
                              addPartialQuery+= " ADD FOREIGN KEY (" + dcol + ") REFERENCES " + rtab + "(" + rcol + ")" + ", ";
                              duplicateCheck.add(tmpStr);
                           }
                     } //end if

               } //end inner for


            //Comment by Karthik on Feb 24th 2010
            //Decided to remove generation of dynamic "ADD FOREIGN KEYS" statements and have them as statements.
            //Reason: The ADD FOREIGN KEYS are really not dynamic, but they are static i.e. these statements don't change from one database schema to another. They are the same for all version 85 gratia db schemas. Only the "DROP FOREIGN KEYS" are of dynamic nature, due to the foreign keys name being dynamic. Hence only the drop foreign keys statements need to be dynamically generated. Moreover, the sequence of steps that is done to upgrade the schema is to 1) Dynamically generate Drop the foreign keys statements and execute them, 2) Execute the Modify fields from int to bigint queries and 3) Execute the add the foreign keys. If something goes wrong during step 1 or 2 (particularly step 2) then it means the upgrade will fail and exit in the middle. This will leave an inconsistent database schema with no foreign keys being re-added back to the schema. This means any subsequent attempts to upgrade the schema won't be idempotent and since there are no foreign keys available to start with, neither the "DROP FOREIGN KEYS" nor the "ADD FOREIGN KEYS" statements will be generated dynamically. Only the "ALTER TABLE MODIFY" fields from int to bigint will be available to be executed. Making the "ADD FOREIGN KEYS" statements *static* will at least allow the foreign keys to be re-added, assuming something went wrong in the previous steps and could be fixed subsequently. We don't have to worry about dropping the foreign keys, since they should happen if and only if the Foreign keys really exist. Hence dynamic generation of those statements is really appropriate. Step 2) statements above i.e. MODIFY int to bigint, are idempotent, meaning it doesn't matter how many times they are executed repeatedly - it will have the same effect and won't corrupt or change the database state. 

            //remove the trailing ", " from the partialQuery and construct the query
            if(!dropPartialQuery.equals(""))
               {
                  dropQuery = prefix + dropPartialQuery.replaceAll(", $","").replaceAll(" {2,}"," ");  
                  dropForeignKeysArrayList.add(dropQuery);
               }

         } //end outer for
   } //end method


   /*
     DROP FOREIGN KEY query is a separate query by itself.
     MODIFY data type (from int to bigint) query is a separate query by itself.
     This method will glue these separate queries together into a single query.
     Why do this?: Everytime a drop or modify query is executed, it consumes some overhead to do integrity check for each row of data. The overhead could be directly propotional to the number of rows of data affected by this change. Executing separate DROP and MODIFY queries means twice the overhead. But logically the DROP and MODIFY for a single table could be executed as a single query. Hence the need to glue them together into a single query that will avoid the double overhead. But glueing them together isn't straightforward. Hence the need for a method that will implement this functionality.
     Example of separate queries:
     ALTER TABLE ComputeElement_Meta DROP FOREIGN KEY FK82052B3FEA4D159F;
     ALTER TABLE ComputeElement_Meta MODIFY dbid bigint(20);
     Example of the combined query:
     ALTER TABLE ComputeElement_Meta DROP FOREIGN KEY FK82052B3FEA4D159F, MODIFY dbid bigint(20)
   */

   public ArrayList<String> combineDropAndModifyQueries(ArrayList<String> queries) throws IOException
   {
      Logging.log(logPrefix + " combineDropAndModifyQueries");
      LinkedHashMap<String, String> t = new LinkedHashMap<String, String>();
      String table = "";
      String partialQuery = "";
      for(String s  : queries)
         {
            table = s.replaceAll("ALTER TABLE ","").replaceAll(" (DROP|MODIFY).*","");
            partialQuery = "";

            if( s.indexOf(" DROP ") != -1)
               {
                  partialQuery = "DROP " + s.replaceAll("ALTER TABLE " + table + " DROP ","");
               }
            else if( s.indexOf(" MODIFY ") != -1)
               {
                  partialQuery = "MODIFY " + s.replaceAll("ALTER TABLE " + table + " MODIFY ","");
               }

            try
               {
                  if(!t.containsKey(table))
                     {
                        t.put(table, partialQuery);
                     }
                  else
                     {
                        t.put(table, t.get(table) + ", " + partialQuery);
                     }
               }
            catch(NullPointerException ignore) {}
         }

      //Create an array list containing the combined queries and return it
      ArrayList<String> a = new ArrayList<String>();
      ArrayList<String> head = new ArrayList<String>();
      for(String tbl : t.keySet())
         {
            //There is a problem with the natural sequence of the queries, since 3 of the queries throw an exception.
            //Hence we need to re-order queries to solve this.
            //Queries on tables (StorageElement, StorageElementRecord and SubCluster) need to be executed
            //after their *_Meta table counterparts
            //Also JobUsageRecord_Meta needs to come after the Probe and JobUsageRecord table
            if(tbl.equals("StorageElement_Meta") || tbl.equals("StorageElementRecord_Meta") || tbl.equals("Subcluster_Meta") || tbl.equals("JobUsageRecord_Meta"))
               head.add("ALTER TABLE " + tbl + " " + t.get(tbl));
            else
               a.add("ALTER TABLE " + tbl + " " + t.get(tbl));
         }
      head.addAll(a);
      return head;
   }

  
   /*
     - Input: List of all DROP and MODIFY queries.
     - Output: List of the only DROP and MODIFY queries that need to be executed.
     - Helpful if the upgrade for some reason crashed in the middle and it is is re-started. If this case the upgrade will only continue from the point it failed before (assuming the issue which caused the failure has been resolved). This makes the upgrade reliable, robust and repeatable
   */

   public ArrayList<String> filterDropAndModifyQueries(ArrayList<String> dropAndModifyQueries) throws SQLException
   {
      Logging.log(logPrefix + " filterDropAndModifyQueries - checking which drop and modify queries need to be executed.");
      //If the query contains MODIFY, then it means we need to do that MODIFY conditionally. The reason for doing it conditionally is that we could start things where things were left off if the program bailed out previously due to an error. of course the error has to be fixed. But after that, by providing the conditional execution of query, we could skip right past all those queries that doesn't have to be executed. 
      //So we need to dynamically create a condition by extracting certain information from the query itself. For example in the above example query we need to check for if the data type of DupRecord.dbid is bigint or not and if the data type of DupRecord.extraxml is mediumtext or not. If both of the conditions are true then we don't need to execute this query at all. Even if one of these conditions is false then we need to modify the data type to the right type and hence it means we need to execute the query. 
      ArrayList<String> retList = new ArrayList<String>();
      for(String qry : dropAndModifyQueries)
         {
            if (Pattern.matches(".*MODIFY.*", qry) && needToExecuteModifyQuery(qry)) 
               retList.add(qry);
         } 
      return retList;
   }


   /*
     Same functionality as the above method. The only difference is here ADD FOREIGN KEY queries are being filtered.
   */
   public ArrayList<String> filterAddForeignKeyQueries() throws SQLException, WrongForeignKeyCountException
   {
      Logging.log(logPrefix + " filterAddForeignKeyQueries - checking which add foreign key queries need to be executed.");
      ArrayList<String> addForeignKeyQueries = new ArrayList<String>(Arrays.asList(addForeignKeysArray)); 
      String modifiedQuery = "";
      ArrayList<String> retList = new ArrayList<String>();
      for(String qry : addForeignKeyQueries)
         {
            modifiedQuery = needToExecuteAddForeignKeyQuery(qry);
            if(!modifiedQuery.equals(""))
               retList.add(modifiedQuery);
         } 
      return retList;
   }

   public boolean tableExists(String table) throws SQLException
   {
      int count = 0;
      String query = "select count(*) from information_schema.TABLES where TABLE_SCHEMA = Database() and TABLE_NAME='" + table + "';";
      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      rs.next();
      count = Integer.parseInt(rs.getString(1));
      rs.close();
      if(count == 1)
         return true;
      return false;
   }

   /*
     This method makes the determination if a given MODIFY query needs to be executed or not. 
     Input: MODIFY Query
     Output: "" if the query need not be executed
     The MODIFY Query, if it needs to be executed
   */

   public boolean needToExecuteModifyQuery(String qry) throws SQLException
   {
      Logging.log(logPrefix + "Check if there is a need to execute : " + qry);
      //System.out.println("find if " + qry + " needs to be executed");
      //Determine if the query needs to be executed or not 
      //sample input query: ALTER TABLE DupRecord MODIFY rawxml mediumtext, MODIFY dbid bigint(20), MODIFY dupid bigint(20) auto_increment, MODIFY extraxml mediumtext
      //We need to determine if the query (like the sample shown above) needs to be executed or not
      //From the query (similar to the sample shown above) We need to extract the table name, the column name(s) and the data type(s) and use another query with this information to make the determination

      //Find table
      String table = findTableGivenAlterQuery(qry);
      assert(!table.equals("")):"ERROR!!! Table name cannot be an empty string.";

         if(!tableExists(table))
            {
               Logging.log("\t Table " + table + " doesn't exist. DON'T Execute query: " + qry);
               return false;
            }
         else
            Logging.log("\t Table " + table + " exists ");

         //Find columns and their corresponding data type. This in turn will be used in another query to check if the existing column type is the expected column type or not
         String field = "";
         String expectedType = "";
         String currentType = "";
         StringBuilder checkQry = null;
         Matcher match = Pattern.compile("MODIFY +(\\w+) +(\\w+)").matcher(qry);
         Statement stmt = connection.createStatement();

         while(match.find())
            {
               field = match.group(1);
               assert(!field.equals("")):"ERROR!!! Field name cannot be an empty string.";

                  expectedType = match.group(2);
                  assert(!expectedType.equals("")):"ERROR!!! expectedType cannot be an empty string.";

                     checkQry = new StringBuilder("select DATA_TYPE from information_schema.columns where TABLE_SCHEMA = Database() and TABLE_NAME = '" + table + "' and COLUMN_NAME = '" + field + "'");

                     //System.out.println(checkQry);
                     Logging.log(logPrefix + "Executing: " + checkQry);
                     ResultSet rs = stmt.executeQuery(checkQry.toString());
                     while(rs.next())
                        {
                           currentType = rs.getString(1);
                           Logging.log("\t Current data type of " + field + " is " + currentType);
                           //System.out.println("current type is " + currentType + ", modified type is " + expectedType);
                           if(!currentType.equals(expectedType))
                              {
                                 //System.out.println("returning true");
                                 Logging.log("\t " + field + " needs to be changed from " + currentType + " to " + expectedType + ".\n");
                                 Logging.log("\t YES - Execute query: " + qry);
                                 Logging.log("\n");
                                 return true;
                              }
                        }
            }
         //System.out.println("returning false");
         Logging.log("\t " + field + " is of the type " + currentType + " which is the expected type. So DON'T Execute query: " + qry);
         Logging.log("\n");
         return false;
   }

   /*
     This method makes the determination if a given ADD FOREIGN KEY query needs to be executed or not. 
     Input sample query: ALTER TABLE ComputeElement_Origin ADD FOREIGN KEY (dbid) REFERENCES ComputeElement(dbid), ADD FOREIGN KEY (originid) REFERENCES Origin(originid);
     Output: "" if the query need not be executed
     The same query returned, if it needs to be executed
   */
   public String needToExecuteAddForeignKeyQuery(String qry) throws SQLException, WrongForeignKeyCountException
   {
      Logging.log("\n" + logPrefix + "Check if there is a need to execute : " + qry);
      //System.out.println("find if " + qry + " needs to be executed");
      //Find table
      String table = findTableGivenAlterQuery(qry);
      assert(!table.equals("")):"ERROR!!! Table name cannot be an empty string.";
     
         if(!tableExists(table))
            {
               Logging.log("\t Table " + table + " doesn't exist. DON'T Execute query: " + qry);
               return "";
            }
         else
            Logging.log("\t Table " + table + " exists ");

         String modifiedQuery = "";
         String field = "";
         String rField = ""; //referenced field
         String rTable = ""; //referenced table
         StringBuilder checkQry = null;
         int numRows = 0;
         Matcher match = Pattern.compile("ADD +FOREIGN +KEY +\\((.*?)\\) +REFERENCES +(\\w+)\\((\\w+)\\)").matcher(qry);
         Statement stmt = connection.createStatement();
         while(match.find())
            {
               field = match.group(1);
               assert(!field.equals("")):"ERROR!!! Primary field name cannot be an empty string.";

                  rTable = match.group(2);
                  assert(!rTable.equals("")):"ERROR!!! Referenced table name cannot be an empty string.";

                     rField = match.group(3);
                     assert(!rField.equals("")):"ERROR!!! Referenced field name cannot be an empty string.";

                        //Use the query below to find out if this foreign key already exists in the schema. If it exists, count(*) will have a value of 1. Otherwise it will have 0. Any other value will result in an exception  being thrown
                        checkQry = new StringBuilder("SELECT count(*) as numRows from information_schema.key_column_usage WHERE TABLE_SCHEMA = Database() and (CONSTRAINT_NAME like 'FK%' or CONSTRAINT_NAME like '%fk%') and TABLE_NAME='" + table + "' and COLUMN_NAME='" + field + "' and REFERENCED_TABLE_NAME='" + rTable + "' and REFERENCED_COLUMN_NAME='" + rField + "'");
                        //if(table.equals("TDCorr"))
                        //  System.out.println("Check query is: " + checkQry);
                        Logging.log(logPrefix + "Executing: " + checkQry);
                        ResultSet rs = stmt.executeQuery(checkQry.toString());
                        while(rs.next())
                           {
                              //Get value returned by count(*)
                              numRows = Integer.parseInt(rs.getString(1));
                              Logging.log("\t numRows is " + numRows);
                              //if(table.equals("TDCorr"))
                              //	System.out.println("num rows: " + numRows);
                              if(numRows == 0)
                                 {
                                    //System.out.println("returning true");
                                    //Re-construct the query one foreign key addition at a time. This way we can make sure that each and every foreign key is indeed needed, irrespective of whether more than one foreign key belongs to a table and is added in a single query originally
                                    modifiedQuery+= " ADD FOREIGN KEY (" + field + ") REFERENCES " + rTable + "(" + rField + "),";
                                    Logging.log("\t modifiedQuery is " + modifiedQuery);
                                    //if(table.equals("TDCorr"))
                                    //System.out.println("new query is " + modifiedQuery);
                                 }
                              else if(numRows > 1)
                                 throw new WrongForeignKeyCountException(checkQry + " returned " + numRows + " matches, though it is supposed to return only one row. Please investigate.");
                           }
            }
         if(modifiedQuery.equals(""))
            {
               //System.out.println("\tno");
               Logging.log("\t DON'T Execute query: " + qry);
               Logging.log("\n");
               return "";
            }
         else
            {
               //System.out.println("\tyes");
               //System.out.println("RETURNING: ALTER TABLE " + table + modifiedQuery.replaceAll(",$",""));
               String retQuery = "ALTER TABLE " + table + modifiedQuery.replaceAll(",$",""); //strip off the , at the end
               Logging.log("\t YES - Execute query: " + retQuery);
               Logging.log("\n");
               return retQuery;
            }
   }

   /*
     Custom defined Exception that is thrown if the number of FOREIGN KEY counted is more than expected. Used while trying to make the determination if a ADD FOREIGN KEY query needs to be executed or not.
   */
   public class WrongForeignKeyCountException extends Exception
   {
      public WrongForeignKeyCountException(String msg)
      {
         super(msg);
      }
   } 

   public String findTableGivenAlterQuery(String qry)
   {
      String table = "";
      //Find table
      Matcher match = Pattern.compile("ALTER +TABLE +(\\w+)").matcher(qry);
      while(match.find())
         table = match.group(1);
      return table;
   }

   /*
     Method that consolidates/calls all the other methods to get the upgrade work done. 
   */
   public void upgrade(Connection connection) throws SQLException, ClassNotFoundException, IOException, WrongForeignKeyCountException
   {
      this.connection = connection;
      Statement stmt = connection.createStatement();

      ArrayList<String> modifyQueriesArrayList = new ArrayList<String>(Arrays.asList(modifyQueriesArray));

      Logging.log(logPrefix + " upgrade");
      upgradeHelper();

      //consolidate the drop and modify queries into a single ArrayList
      dropForeignKeysArrayList.addAll(modifyQueriesArrayList);

      //Combine separate drop and modify queries for a table into a single query that contains both the drop and modify clause
      ArrayList<String> dropAndModifyQueriesArrayList = filterDropAndModifyQueries(combineDropAndModifyQueries(dropForeignKeysArrayList));

      Logging.log(logPrefix + " Total # of combined DROP and MODIFY queries to be executed for the version 86 upgrade: " + dropAndModifyQueriesArrayList.size());

      //Execute drop and modify queries in a sequence
      for(String qry : dropAndModifyQueriesArrayList)
         {
            Logging.log(logPrefix + "Executing: " + qry);
            System.out.println(qry);
            stmt.executeUpdate(qry);
         }

      //Filter the add foreign keys queries by checking if they already exist
      ArrayList<String> addForeignKeysArrayList = filterAddForeignKeyQueries();
      Logging.log(logPrefix + " Total # of ADD FOREIGN KEY queries to be executed for the version 86 upgrade: " + addForeignKeysArrayList.size());
      //Execute add foreign key queries
      for(String qry : addForeignKeysArrayList)
         {
            Logging.log(logPrefix + "Executing: " + qry);
            System.out.println(qry);
            stmt.executeUpdate(qry);
         }
   }
}

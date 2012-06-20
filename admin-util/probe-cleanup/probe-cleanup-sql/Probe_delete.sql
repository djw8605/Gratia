
\! echo "-- ============================================================"
\! echo "--    Deletes Probe table entries"
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM Probe WHERE probeid = ",a.probeid,";") 
FROM (SELECT distinct(probeid) from Probe where ProbeName = @probename) a ;

\! echo "-- ============================================================"
\! echo "--    Deletes ProbeDetails_Meta" 
\! echo "--    Deletes ProbeDetails" 
\! echo "--    Deletes ProbeDetails_Origin" 
\! echo "--    Deletes ProbeDetails_Xml" 
\! echo "--    Deletes ProbeSoftware" 
\! echo "--    Deletes Software" 
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM Software            WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM ProbeSoftware       WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM ProbeDetails_Xml    WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM ProbeDetails_Origin WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM ProbeDetails        WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM ProbeDetails_Meta   WHERE dbid = ",a.dbid,";") 
FROM (SELECT distinct(dbid) from ProbeDetails_Meta where ProbeName = @probename) a  ;

\! echo "-- ==========================================================="
\! echo "--    Deletes StorageElementRecord_Meta" 
\! echo "--    Deletes StorageElementRecord_Origin" 
\! echo "--    Deletes StorageElementRecord" 
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM StorageElementRecord_Meta   WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM StorageElementRecord_Origin WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM StorageElementRecord        WHERE dbid = ",a.dbid,";")
FROM (SELECT distinct(dbid) from StorageElementRecord  where ProbeName = @probename) a  ;

\! echo "-- ============================================================"
\! echo "--    Deletes StorageElement_Meta" 
\! echo "--    Deletes StorageElement_Origin" 
\! echo "--    Deletes StorageElement" 
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM StorageElementRecord_Meta   WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM StorageElementRecord_Origin WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM StorageElementRecord        WHERE dbid = ",a.dbid,";")
FROM (SELECT distinct(dbid) from StorageElement  where ProbeName = @probename) a  ;

\! echo "-- ==========================================================="
\! echo "--    Deletes Subcluster_Meta" 
\! echo "--    Deletes Subcluster_Origin" 
\! echo "--    Deletes Subcluster" 
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM Subcluster_Meta   WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM Subcluster_Origin WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM Subcluster        WHERE dbid = ",a.dbid,";")
FROM (SELECT distinct(dbid) from Subcluster  where ProbeName = @probename) a  ;

\! echo "-- ==========================================================="
\! echo "--    Deletes ComputeElementRecord_Meta" 
\! echo "--    Deletes ComputeElementRecord_Origin" 
\! echo "--    Deletes ComputeElementRecord" 
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM ComputeElementRecord_Meta   WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM ComputeElementRecord_Origin WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM ComputeElementRecord        WHERE dbid = ",a.dbid,";")
FROM (SELECT distinct(dbid) from ComputeElementRecord  where ProbeName = @probename) a  ;


\! echo "-- ==========================================================="
\! echo "--    Deletes ComputeElement_Meta" 
\! echo "--    Deletes ComputeElement_Origin" 
\! echo "--    Deletes ComputeElement" 
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM ComputeElement_Meta   WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM ComputeElement_Origin WHERE dbid = ",a.dbid,";"),
   CONCAT("DELETE FROM ComputeElement        WHERE dbid = ",a.dbid,";")
FROM (SELECT distinct(dbid) from ComputeElement  where ProbeName = @probename) a  ;

\! echo "-- ============================================================"
\! echo "--    Deletes MasterSummaryData"
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM MasterSummaryData   WHERE SummaryID = ",a.SummaryID,";")
FROM (SELECT distinct(SummaryID) from MasterSummaryData  where ProbeName = @probename) a  ;

\! echo "-- ============================================================"
\! echo "--    Deletes NodeSummary"
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM NodeSummary   WHERE NodeSummaryID = ",a.NodeSummaryID,";")
FROM (SELECT distinct(NodeSummaryID) from NodeSummary  where ProbeName = @probename) a  ;

\! echo "-- ============================================================"
\! echo "--    Deletes MasterTransferSummary"
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM MasterTransferSummary   WHERE TransferSummaryID = ",a.TransferSummaryID,";")
FROM (SELECT distinct(TransferSummaryID) from MasterTransferSummary  where ProbeName = @probename) a  ;

\! echo "-- ============================================================"
\! echo "--    Deletes MasterServiceSummary"
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM MasterServiceSummary   WHERE dbid = ",a.dbid,";")
FROM (SELECT distinct(dbid) from MasterServiceSummary  where ProbeName = @probename) a  ;

\! echo "-- ============================================================"
\! echo "--    Deletes MasterServiceSummaryHourly"
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM MasterServiceSummaryHourly   WHERE dbid = ",a.dbid,";")
FROM (SELECT distinct(dbid) from MasterServiceSummaryHourly  where ProbeName = @probename) a  ;

\! echo "-- ============================================================"
\! echo "--    Deletes Replication"
\! echo "-- ============================================================"
SELECT
   CONCAT("DELETE FROM Replication   WHERE replicationid = ",a.replicationid,";")
FROM (SELECT distinct(replicationid) from Replication  where ProbeName = @probename) a  ;



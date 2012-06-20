

\! echo "============================================================"
\! echo "   Query - Total Probe table entries"
\! echo "============================================================"
SELECT count(*) from Probe where ProbeName = @probename;
SELECT        * from Probe where ProbeName = @probename;

\! echo "============================================================"
\! echo "   Query - Total ProbeDetails_Meta table entries"
\! echo "============================================================"
SELECT count(*) from ProbeDetails_Meta where ProbeName = @probename;
SELECT        * from ProbeDetails_Meta where ProbeName = @probename;

\! echo "============================================================"
\! echo "   Query - Total StorageElement table entries"
\! echo "============================================================"
SELECT count(*) from StorageElement where ProbeName = @probename;
SELECT        * from StorageElement where ProbeName = @probename;

\! echo "============================================================"
\! echo "   Query - Total StorageElementRecord table entries"
\! echo "============================================================"
SELECT count(*) from StorageElementRecord where ProbeName = @probename;
SELECT        * from StorageElementRecord where ProbeName = @probename;

\! echo "============================================================"
\! echo "   Query - Total Subcluster table entries"
\! echo "============================================================"
SELECT count(*) from Subcluster where ProbeName = @probename;
SELECT        * from Subcluster where ProbeName = @probename;

\! echo "============================================================"
\! echo "   Query - Total ComputeElement table entries"
\! echo "============================================================"
SELECT count(*) from ComputeElement where ProbeName = @probename;
SELECT        * from ComputeElement where ProbeName = @probename;

\! echo "============================================================"
\! echo "   Query - Total ComputeElementRecord table entries"
\! echo "============================================================"
SELECT count(*) from ComputeElementRecord where ProbeName = @probename;
SELECT        * from ComputeElementRecord where ProbeName = @probename;

\! echo "============================================================"
\! echo "   Query - Total MasterSummaryData table entries"
\! echo "============================================================"
SELECT count(*) from MasterSummaryData where ProbeName = @probename;
SELECT        * from MasterSummaryData where ProbeName = @probename;

\! echo "============================================================"
\! echo "   Query - Total NodeSummary table entries"
\! echo "============================================================"
SELECT count(*) from NodeSummary where ProbeName = @probename;
SELECT        * from NodeSummary where ProbeName = @probename;

\! echo "============================================================"
\! echo "   Query - Total MasterTransferSummary table entries"
\! echo "============================================================"
SELECT count(*) from MasterTransferSummary where ProbeName = @probename;
SELECT        * from MasterTransferSummary where ProbeName = @probename;

\! echo "============================================================"
\! echo "   Query - Total MasterServiceSummary table entries"
\! echo "============================================================"
SELECT count(*) from MasterServiceSummary where ProbeName = @probename;
SELECT        * from MasterServiceSummary where ProbeName = @probename;

\! echo "============================================================"
\! echo "   Query - Total MasterServiceSummaryHourly table entries"
\! echo "============================================================"
SELECT count(*) from MasterServiceSummaryHourly where ProbeName = @probename;
SELECT        * from MasterServiceSummaryHourly where ProbeName = @probename;

\! echo "============================================================"
\! echo "   Query - Total Replication table entries"
\! echo "============================================================"
SELECT count(*) from Replication where ProbeName = @probename;
SELECT        * from Replication where ProbeName = @probename;







\! echo "========================================================"
\! echo "  Query - Sites not used in the Probe table."
\! echo "========================================================"
SELECT * from Site 
WHERE siteid NOT IN (SELECT distinct(siteid) FROM Probe) 
;


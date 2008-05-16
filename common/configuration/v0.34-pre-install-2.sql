-- This SQL fragment is intended to ease the pain of upgrading large DBs
-- to v0.34 (and above) by allowing most of the column addition to take
-- place while operations continue.

-- This SQL should be executed AFTER v0.34-pre-install-2.sql. The OLD 
-- collector should still be running, but DB updates should have been
-- stopped. This will do the final catch-up and table pivot.
start transaction;
  set @maxdbid := 0;
  -- Where did we get to last time?
  select @maxdbid:=max(dbid) from NEWJobUsageRecord_Meta;
  -- Catch up newer records  
  insert into NEWJobUsageRecord_Meta
   select *, null
   from JobUsageRecord_Meta M
   where M.dbid > @maxdbid;
  -- Rename tables
  rename table JobUsageRecord_Meta to OLDJobUsageRecord_Meta,
               NEWJobUsageRecord_Meta to JobUsageRecord_Meta;
commit;

-- If this is successful, upgrade the collector and restart.
-- Pre-"in-service" downtime should therefore be limited to the time required
-- to re-create the summary tables.

-- Delete the OLDUsageRecord_Meta table as soon as you are comfortable that
-- it is not longer required.


-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

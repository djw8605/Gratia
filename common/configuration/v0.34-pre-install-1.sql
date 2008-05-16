-- This SQL fragment is intended to ease the pain of upgrading large DBs
-- to v0.34 (and above) by allowing most of the column addition to take
-- place while operations continue.

-- Execute this SQL while the OLD collector is still running: do *NOT*
-- upgrade the running collector otherwise this script will be rendered
-- redundant as soon as the collector starts.
start transaction;
  drop table if exists NEWJobUsageRecord_Meta;
  create table NEWJobUsageRecord_Meta like JobUsageRecord_Meta;
  alter table NEWJobUsageRecord_Meta add column md5v2 varchar(255), add index index17(md5v2);
commit;

start transaction; -- initial fill
  insert into NEWJobUsageRecord_Meta
   select *, null
   from JobUsageRecord_Meta;
commit;


-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

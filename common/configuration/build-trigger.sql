delimiter ||

drop procedure if exists conditional_trigger_drop
||
create procedure conditional_trigger_drop()
begin

  declare mycount int;

  select count(*) into mycount from information_schema.triggers where
    trigger_schema = Database()
    and event_object_table = 'JobUsageRecord'
    and trigger_name = 'trigger01';

  if mycount > 0 then
    drop trigger trigger01;
  end if;

  select count(*) into mycount from information_schema.triggers where
    trigger_schema = Database()
    and event_object_table = 'JobUsageRecord_Meta'
    and trigger_name = 'trigger02';

  if mycount > 0 then
    drop trigger trigger02;
  end if;
end
||
call conditional_trigger_drop()
||
create trigger trigger02 after insert on JobUsageRecord_Meta
for each row
begin
  call add_JUR_to_summary(new.dbid);
end;

-- Local Variables:
-- mode: sql
-- eval: (sql-set-product 'mysql)
-- End:

DELIMITER $$

drop table if exists trace
$$
CREATE TABLE trace (
  	eventtime TIMESTAMP NOT NULL,
	procName varchar(64),
	userKey varchar(64),
	userName varchar(64),
	userRole varchar(64),
	userVO varchar(64),
  	sqlQuery TEXT,
  	procTime varchar(64),
  	queryTime varchar(64),
  	p1 varchar(64),
  	p2 varchar(64),
  	p3 varchar(64)
	)
$$
drop procedure if exists parse
$$
create procedure parse(username varchar(64),out outname varchar(64),
	out outkey varchar(64),out outvo varchar(64))
begin
	set outname = '';
	set outkey = '';
	set outvo = '';
	set @username = username;
	set @index = locate('|',@username);
	if @index > 0 then
		set outname = substring(@username,1,@index - 1);
		set @username = substring(@username,@index + 1);
	end if;
	set @index = locate('|',@username);
	if @index > 0 then
		set outkey = substring(@username,1,@index - 1);
		set outvo = substring(@username,@index + 1);
	else
		set outkey = @username;
	end if;
end
$$


drop function if exists generateWhereClause
$$
create function generateWhereClause(userName varchar(64),userRole varchar(64),
	whereClause varchar(255)) returns varchar(255)
READS SQL DATA
begin
	select SystemProplist.cdr into @usereportauthentication from SystemProplist
		where SystemProplist.car = 'use.report.authentication';
	if userName = 'GratiaGlobalAdmin' or @usereportauthentication = 'false' then
		return '';
	end if;
	if whereClause = 'Everything' then
		return '';
	end if;
	return concat(' and ',whereClause,' ');
end
$$


drop function if exists generateResourceTypeClause
$$
create function generateResourceTypeClause(resourceType varchar(64))
	returns varchar(255)
DETERMINISTIC
begin
	if resourceType = '' or resourceType = NULL then
		return '';
	else
		return concat(
			' and ResourceType = ''',
			resourceType,
			'''');
	end if;
end
$$

DELIMITER ;
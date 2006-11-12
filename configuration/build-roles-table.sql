delimiter |
delete from RolesTable
|
insert into RolesTable(role,subtitle,whereclause) values('GratiaGlobalAdmin','GratiaGlobalAdmin','')
|
insert into RolesTable(role,subtitle,whereclause) values('glr','glr',"CETable.facility_name like 'Cdf%'")
|
insert into RolesTable(role,subtitle,whereclause) values('GratiaUser','GratiaUser',"JobUsageRecord.VOName = 'cms'")
|

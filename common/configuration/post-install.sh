#
# Create the mysql gratia summary tables and stored procedures
# Needs to be executed after Tomcat has started so that the war files are unpacked 
#	and the hibernate has created the gratia schema tables.
#

mysql -v --force -unbuffered --user=root --password=ROOTPASS  --host=localhost --port=PORT gratia < MAGIC_VDT_LOCATION/tomcat/v55/gratia/build-summary-tables.sql
mysql -v --force -unbuffered --user=root --password=ROOTPASS  --host=localhost --port=PORT gratia < MAGIC_VDT_LOCATION/tomcat/v55/gratia/build-stored-procedures.sql
mysql -v --force -unbuffered --user=root --password=ROOTPASS  --host=localhost --port=PORT gratia < MAGIC_VDT_LOCATION/tomcat/v55/gratia/build-trigger,sql


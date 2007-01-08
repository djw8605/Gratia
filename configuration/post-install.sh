#!/bin/bash
cd CATALINA_HOME/gratia
mysql -v --f1gorce -unbuffered --user=root --host=localhost --port=3320 gratia < build-summary-tables.sql
mysql -v --force -unbuffered --user=root --host=localhost --port=3320 gratia < build-stored-procedures.sql
mysql -v --force -unbuffered --user=root --host=localhost --port=3320 gratia < build-roles-table.sql

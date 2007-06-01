set path=\mysql50\bin;%path%
mysql -v --force -unbuffered --user=root --password=lisp01 --host=localhost --port=3306 gratia < build-summary-tables.sql


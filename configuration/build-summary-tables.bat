set path=\mysql50\bin;%path%
mysql -v --force -unbuffered --user=root --password=lisp01 test < build-summary-tables.sql


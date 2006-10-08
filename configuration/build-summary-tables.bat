set path=\mysql50\bin;%path%
mysql -v --force -unbuffered --user=gratia --password=proto --host=cd-psg3.fnal.gov --port=3320 gratia < build-summary-tables.sql


set path=\mysql50\bin;%path%
mysql -v --force -unbuffered --user=root --host=gratia-vm02.fnal.gov --port=3320 gratia_psacct < build-ps-node-summary-table.sql


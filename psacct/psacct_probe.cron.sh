#!/bin/sh

# For now play nice with the old fermi PSACCT package
# So we assume it is working and we do not break it.
# We also assume that for a given date we run after runacct

_date="`date +%m%d`"
_adm=${ACCTDIR:-/var/adm}
_nite=${_adm}/acct/nite
_currentfile=${_nite}/spacct${_date}


mkdir -p /opt/gratia/var/data
cp ${_currentfile} /opt/gratia/var/data

# Now run gratia

_gratia_dir=/opt/gratia_probes

cd ${_gratia_dir}
python PSACCTProbe.py

#!/usr/bin/env bash

#. /data/test-vdt/setup.sh
. /usr/local/etc/setups.sh 
setup mysql > /dev/null 2>&1

export http_proxy=squid.fnal.gov:3128 
where=`dirname $0`
cd $where
$where/cms_mutt.sh --weekly


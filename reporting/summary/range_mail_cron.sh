#!/usr/bin/env bash

#. /data/test-vdt/setup.sh
. /usr/local/etc/setups.sh 
setup mysql

where=`dirname $0`
cd $where
$where/range_mutt.sh $1


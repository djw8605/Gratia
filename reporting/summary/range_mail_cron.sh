#!/usr/bin/env bash

. /usr/local/etc/setups.sh 
setup mysql

where=`dirname $0`
cd $where
$where/range_mutt.sh $1 $2


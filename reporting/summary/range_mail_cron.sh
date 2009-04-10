#!/usr/bin/env bash

. /usr/local/etc/setups.sh 
setup mysql

export http_proxy=http://squid.fnal.gov:3128
where=`dirname $0`
cd $where
$where/range_mutt.sh $1 $2


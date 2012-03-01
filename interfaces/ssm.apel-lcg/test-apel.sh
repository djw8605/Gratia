#!/bin/bash
#######################################################################
# John Weigand (3/1/12)
#
# Simple script to test the APEL RPM interface
#######################################################################
dir=/opt/apel/ssm.apel-lcg
cd $dir
./lcg.sh --config=lcg.conf --date=current --update
exit 0

#!/bin/bash
#set -x
###########################################################################
# John Weigand (5/3/2008)
#
# This script is intended to be run from cron on a once a day basis.
# It will create a file of OSG voms servers that are used by the
# Gratia administrative login process. Note: this same file will be used
# by the reporting login process when it is implemented.
#
# It downloads the OSG edg-mkgridmap.conf file, parses it and replaces the
# existing one in the /var/lib/gratia-service directory.
#
# If the script fails on a given night, it is not critical.
#
# This is only needed if the service.admin.FQAN attributes are activated.
#
##########################################################################
function usage {
   echo "Usage: $PGM  <-h> <-e> [<vo> ...]

    -e - creates an empty file with only comments to insure consistent 
         comments.
         
    -h - help. Displays usage
                
   This script will create the file to be used by the Gratia administration
   login process when FQAN/DN authorizations are used.  The format of the
   file is:
      VO=VOMS_SERVER_URL

If <vo>s are specified, then only these VOs shall be entered into voms-servers.
"   
}
#--------------
function logit {
  echo "$1"
}
#--------------
function logerr {
  logit "ERROR: $1";exit 1
}


function add_vo_line {
  [[ -n "$1" ]] && conditional='if (vo == "'"$1"'") '
  awk '{ 
       url=""
       if ( $2 == "USER-VO-MAP" )  { vo=$3;next }
       if ( $1 == "group" )  { 
         sub(/vomss:/,"",$2)
         url=$2 
         '"$conditional"'printf "%s=https:%s\n",vo,url
     }
  }' $tmpVOMS  |sort -u >>$outfile
}
#### MAIN ################################################################
gratiaVOMS=voms-servers
tmpFile=`grep service.voms.connections /etc/gratia/collector/service-configuration.properties |grep -v "^#"|cut -d'=' -f2`
if [ "$?" != "0" ];then
  logerr "service.voms.connections are not defined /etc/gratia/collector/service-configuration.properties"
fi
if [ x"$tmpFile" != "x" ];then
	gratiaVOMS=$tmpFile
fi



tmpVOMS=/etc/edg-mkgridmap.conf
gratiaLocation=/var/lib/gratia-service
help=n
empty=n

#---- verify root ----
#if [ "`id -u`" != "0" ];then
#  logerr "You must be root to use this command"
#fi

#---- validate command line ----
while getopts he a
do
  case $a in
    h ) help=y;;
    e ) empty=y;;
    * ) usage;logerr "Invalid command line argument($a)";;
  esac
done
if [ "$help" = "y" ];then
  usage;exit 1
fi
yum update vo-client-edgmkgridmap -y >/dev/null 2>&1
if [ "$?" != "0" ];then
  logerr "Gratia cron ($PGM) failed to update vo-client-edgmkgridmap

... updates not applied.
"
fi

if [ ! -f $tmpVOMS ];then
  logerr "The $tmpVOMS does not exist on this machine"
fi

if [ ! -d $gratiaLocation ];then
  logerr "The /var/lib/gratia-service specified does not exist for gratia:
  $gratiaLocation
"
fi

if [ ! -w $gratiaLocation ];then
  logerr "You do not have permissions to write in this directory:
`ls -ld $gratiaLocation`
  
"
fi



#--- create the voms-servers file ----
outfile=$gratiaLocation/$gratiaVOMS
cat >$outfile <<EOF
##########################################################################
# VOMS server file
#
# This file is used in the adminstrative login identifying the
# VOMS servers for all VOs potentially defined by the
# service.admin.FQAN.n attributes of the service-configuration.properties
# file.
#
# Format:
#  VO=VOMS_Server_url
# e.g.
#  cdf=https://
#
# The VO should match the root group of the FQAN entry for attribute
# (w/o the slash).
#
# It can be maintained manually or by executing the voms-server-cron.sh
# script which creates the file using the the OSG edg-mkgridmap.conf file.
# Having extra entries in the file does no harm.
############################################################################
#
EOF
chmod 644 $outfile

if [ "$empty" = "y" ];then
  exit 0
fi
if [[ -n "$*" ]]; then
  for wanted_vo in "$@"; do
    add_vo_line "$wanted_vo"
  done
else
  add_vo_line
fi


exit 0

#!/bin/bash
###########################################################################
# John Weigand (5/3/2008)
#
# This script is intended to be run from cron on a once a day basis.
# It will create a file of OSG voms servers that are used by the
# Gratia administrative login process. Note: this same file will be used
# by the reporting login process when it is implemented.
#
# It downloads the OSG edg-mkgridmap.conf file, parses it and replaces the
# existing one in the ./gratia directory of tomcat.
#
# If the script fails on a given night, it is not critical.
#
# This is only needed if the service.admin.FQAN attributes are activated.
#
##########################################################################
function usage {
   echo "Usage: $PGM -t TOMCAT_LOCATION <-s EDG_MKGRIDMAP> <-h> <-e> [<vo> ...]

    -t - the top level tomcat location specifying where the
         TOMCAT_LOCATION/gratia directory is located.
 
    -s - location of the edg-mkgridmap.conf to be used.
         Default: $osgVOMS

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
PGM=$(basename $0)

osgVOMS=http://software.grid.iu.edu/pacman/tarballs/vo-version/edg-mkgridmap.osg
tmpVOMS=`mktemp ${TMPDIR:-/tmp}/voms-server.sh.XXXXXXXXXX`
if [[ -z "$tmpVOMS" ]]; then
  logerr "Error getting temporary file"
fi

trap "[[ -n \"$tmpVOMS\" ]] && rm \"$tmpVOMS\" 2>/dev/null" EXIT

gratiaVOMS=voms-servers
TOMCAT_LOCATION=""
help=n
empty=n

#---- verify root ----
#if [ "`id -u`" != "0" ];then
#  logerr "You must be root to use this command"
#fi

#---- validate command line ----
while getopts t:s:he a
do
  case $a in
    t ) TOMCAT_LOCATION=$OPTARG;;
    s ) osgVOMS=$OPTARG;;
    h ) help=y;;
    e ) empty=y;;
    * ) usage;logerr "Invalid command line argument($a)";;
  esac
done
shift `expr $OPTIND - 1`

if [ "$help" = "y" ];then
  usage;exit 1
fi

if [ -z "$TOMCAT_LOCATION" ];then
  logerr "The -t TOMCAT_LOCATION is a required argument"
fi

gratiaLocation=$TOMCAT_LOCATION/gratia
if [ ! -d $gratiaLocation ];then
  logerr "The -t TOMCAT_LOCATION specified does not exist for gratia:
  $gratiaLocation
"
fi

if [ ! -w $gratiaLocation ];then
  logerr "You do not have permissions to write in this directory:
`ls -ld $gratiaLocation`
  
"
fi



#--- retrieve the edg-mkgridmap.conf file ---
wget -O $tmpVOMS $osgVOMS >/dev/null 2>&1
if [ "$?" != "0" ];then
  logerr "Gratia cron ($PGM) failed in wget to:
$osgVOMS

... updates not applied.
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

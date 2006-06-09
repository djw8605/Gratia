#!/bin/bash
#
# condor_meter.cron.sh - Shell script used with cron to parse Condor log 
#   files for OSG accounting data collection.
#      By Ken Schumacher <kschu@fnal.gov>  Began 5 April 2006
# $Id: condor_meter.cron.sh,v 1.1 2006-06-09 16:07:36 glr01 Exp $
# Full Path: $Source: /var/tmp/move/gratia/condor-probe/condor_meter.cron.sh,v $

# Setup the proper Grid environment
# . /export/osg/grid/setup.sh  # works on fngp-osg
. /root/setup.sh  # works on fermigrid1

# These default values may be overridden by a configuration file
#Logfile_Dir='/export/osg/grid/globus/tmp/gram_job_state'  # on fngp-osg
Logfile_Dir='/usr/local/vdt-1.3.6/globus/tmp/gram_job_state'  # on fermigrid1
#Logfile_Dir='/usr/local/vdt-1.3.9/globus/tmp/gram_job_state'  # on fermigrid1
Meter_BinDir='/home/kschu/src/osg'

# Need to be sure there is not one of these running already
NCMeter=`ps -ef | grep condor_meter.pl | grep -v grep | wc -l`
if [ ${NCMeter} -eq 0 ]; then
    if [ -d ${Meter_BinDir} ]; then
        cd ${Meter_BinDir}
    else
	/usr/bin/logger -t condor_meter "$0: No such directory ${Meter_BinDir}"
	exit -1
    fi
    
    if [ ! -x ./condor_meter.pl ]; then
	echo "The condor_meter.pl file is not in this ${Meter_BinDir} directory."
	exit -1
    fi
    for Needed_File in Gratia.py ProbeConfig
    do
    if [ ! -f ./${Needed_File} ]; then
	echo "The ${Needed_File} file is not in this directory."
	exit -1
    fi
    done

    eval `grep LogFolder ProbeConfig`
    if [ -d ${LogFolder} ]; then
       Logfile_Dir=${LogFolder}
    elif [ -d ${VDT_LOCATION}/gratia ]; then
       Logfile_Dir="${VDT_LOCATION}/gratia"
    fi
    if [ ! -d ${Logfile_Dir} ]; then
	/usr/bin/logger -t condor_meter "$0: No such directory ${Logfile_Dir}"
	exit -1
    fi

    echo "Begin processing directory ${Logfile_Dir}"
    ./condor_meter.pl ${Logfile_Dir}
    ExitCode=$?
    # If the probe ended in error, report this in Syslog and exit
    if [ $ExitCode != 0 ]; then
	/usr/bin/logger -t condor_meter \
           "ALERT: $0 exited abnormally with [$ExitCode]"
	exit $ExitCode
    fi
    
    # Possibly loop to see if there are any new files before exiting.

    # The following debug statement needs to be removed after testing.
    ls -ltrAF /tmp/py*
else
    echo "There is a 'condor_meter.pl' task running already."
fi
exit 0

#==================================================================
# CVS Log
# $Log: not supported by cvs2svn $
# Revision 1.4  2006/04/17 22:25:14  kschu
# Gets the log file directory from ProbeConfig file.
#
# Revision 1.3  2006/04/13 15:56:29  kschu
# Uses a directory as probe command-line argument
#
# Revision 1.2  2006/04/10 19:52:30  kschu
# Refined data submission after code review
#
# Revision 1.1  2006/04/05 18:10:25  kschu
# First test version of script to be called by Cron
#

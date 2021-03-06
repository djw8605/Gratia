#!/bin/env python

import gratia.common.Gratia as Gratia
import string, time, sys
import AccountingReports
from AccountingReports import FromCondor,UseArgs,gOutput,gBegin
import sys

def toint( value ) :
        value = value.replace(',','')
        return string.atoi(value)

def SendData(values, dateAsSecondSinceEpoch):
        # This function takes the input data passed as a python tuple,
        # create the Gratia record and send it.

        (SiteName, UserVoName, Njobs, WallHours) = values

        r = Gratia.UsageRecord()
        r.Njobs(toint(Njobs))
        r.WallDuration( toint(WallHours)*3600) # The argument must be in seconds
        r.SiteName( SiteName)
        r.StartTime( dateAsSecondSinceEpoch )
        r.EndTime( dateAsSecondSinceEpoch + 3600*24)

        r.VOName(UserVoName)
        #r.CpuDuration(UserCpuHours * 3600)
        #r.CpuDuration(SystemCpuHours * 4600)
        #r.NodeCount(TotalNumberOfNodeInCluster)
        #r.Processors(TotalNumberOfCpuCoreInCluster)

        Gratia.Send(r)

def main(argv=None):
    UseArgs(argv)

    argOutput = "None" # AccountingReports.gOutput
    Gratia.Initialize()

    result = AccountingReports.DailySiteVOReport(AccountingReports.gBegin,output=argOutput)
    
    dateAsSecondSinceEpoch = time.mktime(AccountingReports.gBegin.timetuple())
 #       else:
 #               today = time.localtime()
 #               today = (today.tm_year,today.tm_mon,today.tm_mday, 0, 0, 0, today.tm_wday, today.tm_yday, today.tm_isdst)
 #               dateAsSecondSinceEpoch = time.mktime(today)

    # Read the date and send it to Gratia
    for line in result:
        SendData( line[1:5], dateAsSecondSinceEpoch)

if __name__ == "__main__":
    sys.exit(main())



                

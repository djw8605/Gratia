#!/bin/env python

# Author: Karthik
# Date: Sep 4th 2009
# Purpose: Compare the list of VOs in gratia against the authoritative list of VOs published by MyOSG and identify those VOs that show up in gratia but aren't in OIM and vice-versa
# Logic: Obtain the list of VOs that showed up in the range between start_date and end_date from the gratia database
#	 Obtain the list of VOs that is published by MyOSG
#	 Find the difference
 
import sys
import AccountingReports
from AccountingReports import FromCondor,UseArgs,gOutput,gBegin,gEnd,CheckDB

def compareVOs(argv=None):
    # get the list of vos reported by gratia
    gratia = AccountingReports.GetReportingVOs(AccountingReports.gBegin, AccountingReports.gEnd) 
    # get the list of vos reported by oim
    oim = AccountingReports.GetListOfRegisteredVO() 
    excluded = ['unknown','other']
    # print report header
    print "VO discrepancy between OIM and gratia for date range: ",AccountingReports.gBegin,"to",AccountingReports.gEnd
    print
    # What is in gratia that is not in oim
    diff = list(set(gratia) - set(oim) - set(excluded))
    print "1.",len(diff),"VOs reported by gratia, but not in OIM"
    print diff
    print
    # What is in oim that is not in gratia - might indicate inactivity for that particular VO in the given time range
    diff = list(set(oim) - set(gratia) - set(excluded))
    print "2.",len(diff),"VOs did not report"
    print diff

def main(argv=None):
    # Handle command line arguments
    UseArgs(argv)
    if not CheckDB() :
        return 1
    # compare the VOs to prepare the report
    compareVOs()

if __name__ == "__main__":
    sys.exit(main())

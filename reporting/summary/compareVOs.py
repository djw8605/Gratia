#!/bin/env python

# Author: Karthik
# Date: Sep 4th 2009
# Purpose: Compare the list of VOs in gratia against the authoritative list of VOs published by MyOSG and identify those VOs that show up in gratia but aren't in OIM and vice-versa
# Logic: Obtain the list of VOs that showed up in the range between start_date and end_date from the gratia database
#	 Obtain the list of VOs that is published by MyOSG
#	 Find the difference
 
import sys,re
import AccountingReports
from AccountingReports import FromCondor,UseArgs,gOutput,gBegin,gEnd,CheckDB

def compareVOs(argv=None):
    # get the list of vos reported by gratia
    gratia = AccountingReports.GetReportingVOs(AccountingReports.gBegin, AccountingReports.gEnd) 

    # get the list of vos reported by oim
    oim = AccountingReports.GetListOfRegisteredVO() 
    excluded = ['unknown','other']

    # print report header
    print "VO discrepancy between OIM and gratia from",AccountingReports.gBegin,"to",AccountingReports.gEnd
    print

    # What is in gratia that is not in oim
    diff = list(set(gratia) - set(oim) - set(excluded))
    # Find out the sites that report these VOs. The code below creates a detailed formatted report. 
    voStr=""
    for vo in sorted(diff,key=str.lower):
        voStr+="VOName=\"" + vo + "\" or "
    voStr=re.compile("VOName=.*\"",re.IGNORECASE).search(voStr).group(0)
    # join query to find the list of sites that reported these VOs 
    query = "select T.SiteName, J.VOName from Site T, Probe P, VOProbeSummary J where (" + voStr + ") and P.siteid = T.siteid and J.ProbeName = P.probename and EndTime >= \"" + str(AccountingReports.gBegin) + "\" and EndTime < \"" + str(AccountingReports.gEnd) + "\" and J.ProbeName not like \"psacct:%\" group by J.VOName,T.SiteName order by lower(J.VOName),lower(T.SiteName);"
    siteVO = AccountingReports.RunQueryAndSplit(query)
    # If one or more VOs matched this criteria
    if len(siteVO)!=0:
        print "ALERT!", len(diff), "VOs reported by gratia from",AccountingReports.gBegin,"to",AccountingReports.gEnd,", but not in OIM." # alerting header that could be caught by the wrapper script to alert in the subject line of the email
        print "Listed below are these VOs along with the sites that reported them."
        dashLen=59; # for decoration
        count=0
        print dashLen*"-"
        print(" %s  |%5s%-20s|%5s %-20s|"%("# "," ","VO"," ","SITE"))
        print dashLen*"-"
        for entry in siteVO:
            count+=1
            site = entry.split('\t')[0]
            vo = entry.split('\t')[1]
            if(count<10):
                print(" %d.  |%5s%-20s|%5s %-20s|"%(count," ",vo," ",site))
            else:
                print(" %d. |%5s%-20s|%5s %-20s|"%(count," ",vo," ",site))
        print dashLen*"-"
    else:
        print "All OK! Gratia did not report any VOs not found in OIM for",AccountingReports.gBegin,"to",AccountingReports.gEnd # alerting header that could be caught by the wrapper script to alert in the subject line of the email
  
    print

    # What is in oim that is not in gratia - might indicate inactivity for that particular VO
    diff = list(set(oim) - set(gratia) - set(excluded))
    if(len(diff) > 0):
        print len(diff),"VOs in OIM did not report to gratia. These VOs are listed below."
        # sort VOs in the list alphabetically ignoring case
        for vo in sorted(diff,key=str.lower):
            sys.stdout.write(vo)
            sys.stdout.write("; ")
    else:
        print "All VOs in OIM have reported"
 
    print
    print

def main(argv=None):
    # Handle command line arguments
    UseArgs(argv)
    if not CheckDB() :
        return 1
    # compare the VOs to prepare the report
    compareVOs()

if __name__ == "__main__":
    sys.exit(main())

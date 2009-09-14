#!/bin/env python

# Author: Karthik
# Date: Sep 4th 2009
# Purpose: Compare the list of VOs in gratia against the authoritative list of VOs published by MyOSG and identify those VOs that show up in gratia but aren't in OIM and vice-versa
# Logic: Obtain the list of VOs that showed up in the range between start_date and end_date from the gratia database
#	 Obtain the list of VOs that is published by MyOSG
#	 Find the difference
 
import sys,re
import AccountingReports, sendMail
from AccountingReports import FromCondor,UseArgs,gOutput,gBegin,gEnd,CheckDB

def compareVOs(argv=None):
    # get the list of vos reported by gratia
    gratia = AccountingReports.GetReportingVOs(AccountingReports.gBegin, AccountingReports.gEnd) 

    # get the list of vos reported by oim
    oim = AccountingReports.GetListOfRegisteredVO(voType = 'active') 
    excluded = ['unknown','other']

    # What is in gratia that is not in oim
    diff = list(set(gratia) - set(oim) - set(excluded))
    # Find out the sites that report these VOs. The code below creates a detailed formatted report. 
    voStr=""
    # Construct the mysql query string using the conditions for VOnames like (VOName='zeus' or VOName='aceace')
    for vo in sorted(diff,key=str.lower):
        voStr+="VOName=\"" + vo + "\" or "
    voStr=re.compile("VOName=.*\"",re.IGNORECASE).search(voStr).group(0)

    # join query to find the list of sites that reported these VOs. See the voStr constructed above being used here
    query = "select T.SiteName, J.VOName from Site T, Probe P, VOProbeSummary J where (" + voStr + ") and P.siteid = T.siteid and J.ProbeName = P.probename and EndTime >= \"" + str(AccountingReports.gBegin) + "\" and EndTime < \"" + str(AccountingReports.gEnd) + "\" and J.ProbeName not like \"psacct:%\" group by J.VOName,T.SiteName order by lower(J.VOName),lower(T.SiteName);"
    # Run the query and get the results
    siteVO = AccountingReports.RunQueryAndSplit(query)

    # Title for the report
    message = "VO discrepancy between OIM and gratia from "+ str(AccountingReports.gBegin) +" to "+str(AccountingReports.gEnd) + "\n\n"
    #siteVO=[] # fake test to simulate a the else (All OK) condition 

    # If one or more VOs matched this criteria, then create a formatted report 
    if len(siteVO)!=0:
        subject = "ALERT! "+ str(len(diff)) + " VOs reported by gratia from " + str(AccountingReports.gBegin) +" to " + str(AccountingReports.gEnd) + " are not in OIM." # alerting header that could be caught by the wrapper script to alert in the subject line of the email
        message+=subject
        message+="\nListed below are these VOs along with the sites that reported them.\n"
        dashLen=59; # for decoration
        count=0
        message+=dashLen*"-" + "\n"
        message+=(" %s  |%5s%-20s|%5s %-20s|"%("# "," ","VO"," ","SITE"))+"\n"
        message+=dashLen*"-"+"\n"
        for entry in siteVO:
            count+=1
            site = entry.split('\t')[0]
            vo = entry.split('\t')[1]
            if(count<10):
                message+=(" %d.  |%5s%-20s|%5s %-20s|"%(count," ",vo," ",site))+"\n"
            else:
                message+=(" %d. |%5s%-20s|%5s %-20s|"%(count," ",vo," ",site))+"\n"
        message+=dashLen*"-"+"\n"
    # If no VO matched the criteria
    else:
        subject = "All OK! Gratia did not report any VOs not found in OIM for " + str(AccountingReports.gBegin) + " to " + str(AccountingReports.gEnd) # alerting header that could be caught by the wrapper script to alert in the subject line of the email
        message+=subject
  
    message+="\n\n"

    # What is in oim that is not in gratia - might indicate inactivity for that particular VO
    diff = list(set(oim) - set(gratia) - set(excluded))
    if(len(diff) > 0):
        message+=str(len(diff)) + " VOs in OIM did not report to gratia. These VOs are listed below."+"\n\n"
        # sort VOs in the list alphabetically ignoring case
        count=0
        for vo in sorted(diff,key=str.lower):
            count+=1
            message+= vo + "; "
            if count%5==0:
                message+="\n"        
    else:
        message+="All VOs in OIM have reported"+"\n"
 
    message+="\n"
    message+="\n"

    message = "<pre>" + message + "</pre>"
    content={}
    content['text'] = message
    content['html'] = message
    content['csv'] = str(None)
    AccountingReports.sendEmail( (['karthik','Arun'], ['karunach@nhn.ou.edu','karthikarun@ou.edu']), subject, content, None,None,'phyast.nhn.ou.edu')

def main(argv=None):
    # Handle command line arguments
    UseArgs(argv)
    if not CheckDB() :
        return 1
    # compare the VOs to prepare the report
    compareVOs()

if __name__ == "__main__":
    sys.exit(main())

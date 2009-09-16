#!/bin/env python

# Author: Karthik
# Date: Sep 4th 2009
# Purpose: Compare the list of VOs in gratia against the authoritative list of VOs published by MyOSG and identify those VOs that show up in gratia but aren't in OIM and vice-versa
# Logic: Obtain the list of VOs that showed up in the range between start_date and end_date from the gratia database
#	 Obtain the list of VOs that is published by MyOSG
#	 Find the difference
 
import sys,re, AccountingReports

def compareVOs(argv=None):
    # get the list of vos reported by gratia
    gratia = AccountingReports.GetReportingVOs(AccountingReports.gBegin, AccountingReports.gEnd) 

    # get the list of all vos in oim (active + inactive)
    oimAll = AccountingReports.GetListOfRegisteredVO() 
    # get the list of VOs in OIM marked as active
    oimActive = AccountingReports.GetListOfRegisteredVO(voType = 'active') 
    # get the list of VOs in OIM marked as in-active
    oimInActive = AccountingReports.GetListOfRegisteredVO(voType = 'inactive') 
    excluded = ['unknown','other']

    #================================================================================
    # Section 1) What are the VOs reporting to gratia that are not registered in oim. 
    #================================================================================
    diff = list(set(gratia) - set(oimAll) - set(excluded))
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
        subject = "ALERT! "+ str(len(diff)) + " VOs reporting to gratia were not found in OIM for "+str(AccountingReports.gBegin) + " to " + str(AccountingReports.gEnd) # alerting header that could be caught by the wrapper script to alert in the subject line of the email
        message+=subject
        message+="\nListed below are these VOs along with the sites that reported them.\n"
        dashLen=60; # number of dashes needed for the horizantal dashed separator line in the table
        message+=dashLen*"-" + "\n"
        # heading row
        message+=("| %s.%s  |%5s%-20s|%5s %-20s|"%("#"," "," ","VO"," ","Reporting Site(s)"))+"\n"
        # separator
        message+=dashLen*"-"+"\n"
        # variable to keep track of previous vo. This will be compared with the current vo to detect when the vo changes
        prevVO = ""
        voCount = 0
        siteCount = 0
        # for each row returned by the query, containing the site, vo pair separated by a tab (\t) character
        for entry in siteVO:
            site = entry.split('\t')[0] 
            vo = entry.split('\t')[1]
            # prepare/format a table as shown in the sample below:
            #------------------------------------------------------------
            #| #.   |     VO                  |      SITE                |
            #------------------------------------------------------------
            #| 1.1  |     ahouston            |      Prairiefire         |
            #| 2.1  |     batelaan            |      Prairiefire         |
            #| 3.1  |     belashchenko        |      Prairiefire         |
            #| 4.1  |     berkowitz           |      Prairiefire         |
            #| 5.1  |     choueiry            |      Prairiefire         |
            #| 6.1  |     dteam               |      Nebraska            |
            #| 6.2  |                         |      UFlorida-PG         |
            #| 7.1  |     ducharme            |      Prairiefire         |
            #| 8.1  |     fermilab-test       |      FNAL_FERMIGRID      |
            #| 9.1  |     fgstore             |      FNAL_CDFOSG_1       |
            #| 9.2  |                         |      FNAL_CDFOSG_2       |
            #| 9.3  |                         |      FNAL_CDFOSG_3       |
            #| 9.4  |                         |      FNAL_GPGRID_2       |
            #| 9.5  |                         |      FNAL_GPGRID_3       |
            #------------------------------------------------------------

            # If it is a different VO then print a dashed horizantal line separating this VO entry from the next. This will help to better read the VO discrepancy table.             
            if(vo != prevVO):
                # no need to print a dashed separator line at the very beginning of the table, since it is already printed to start with
                if(voCount != 0):
                    message+=dashLen*"-" + "\n"
                # increase vo count only if we go to the next vo (detected by comparing the current vo to the previous vo)
                voCount+=1
                # Reset the site count to 1 for each new vo
                siteCount = 1

                # check and align the column separators between single digit count and double digit count by printing an extra space for single digit count
                if(voCount < 10):
                    message+=("| %d.%d  |%5s%-20s|%5s %-20s|"%(voCount,siteCount," ",vo," ",site))+"\n"
                else:
                    message+=("| %d.%d |%5s%-20s|%5s %-20s|"%(voCount,siteCount," ",vo," ",site))+"\n"
            # If it is the same VO, then don't print the VO name again, but just the site name that reported that VO in a separate line
            else:
                if(voCount < 10):
                    message+=("| %d.%d  |%5s%-20s|%5s %-20s|"%(voCount,siteCount," "," "," ",site))+"\n"
                else:
                    message+=("| %d.%d |%5s%-20s|%5s %-20s|"%(voCount,siteCount," "," "," ",site))+"\n"
            # keep track of the previous VO
            prevVO = vo
            # increase site count for every row returned
            siteCount+=1
        # print the footer horizantal dashed line
        message+=dashLen*"-"+"\n"
    # If no VO matched the criteria
    else:
        subject = "All OK! Gratia did not report any VOs not found in OIM for " + str(AccountingReports.gBegin) + " to " + str(AccountingReports.gEnd) # alerting header that could be caught by the wrapper script to alert in the subject line of the email
        message+=subject
  
    message+="\n\n"


    #============================================================================================
    # Section 2) What are the VOs reporting to gratia that are registered as in-active VOs in oim 
    #============================================================================================
    diff = []
    diff = list(set(gratia) & set(oimInActive) - set(excluded)) # intersection
    if(len(diff) > 0):
        message+=str(len(diff)) + " VOs reporting to gratia are marked as in-active in OIM. These VOs are listed below.\n\n"
        message+=printBigList(diff)
        message+="\n\n"

    #================================================================================================================
    # Section 3) What are the VOs active in OIM but not reporting to gratia - might indicate inactivity for that particular VO
    #================================================================================================================
    diff = []
    diff = list(set(oimActive) - set(gratia) - set(excluded))
    if(len(diff) > 0):
        message+=str(len(diff)) + " active VOs in OIM did not report to gratia. These VOs are listed below.\n\n"
        message+=printBigList(diff)
        message+="\n\n"
    else:
        message+="All VOs in OIM have reported"+"\n"
 
    message+="\n\n"

    message = "<pre>" + message + "</pre>"
    content={}
    content['text'] = message
    content['html'] = message
    content['csv'] = str(None)
    AccountingReports.sendEmail( ([None], AccountingReports.gEmailTo), subject, content, None,None,None)

def printBigList(bigList):
    # print the elements in the list by inserting an end-line character every 'n' elements
    count=0
    message=""
    # sort VOs in the list alphabetically ignoring case
    for vo in sorted(bigList,key=str.lower):
        count+=1
        message+= vo + "; "
        if count%5==0:
            message+="\n"        
    return message

def main(argv=None):
    # Handle command line arguments
    AccountingReports.UseArgs(argv)
    if not AccountingReports.CheckDB() :
        return 1
    # compare the VOs to prepare the report
    compareVOs()

if __name__ == "__main__":
    sys.exit(main())

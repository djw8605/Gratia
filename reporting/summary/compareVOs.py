#!/bin/env python

##############################################################################################################################################

# Author: Karthik
# Date: Sep 4th 2009
# Purpose: Compare the list of VOs in gratia against the authoritative list of VOs published by MyOSG and identify those VOs that show up in gratia but aren't in OIM and vice-versa
# ----------------------------------------------------------------
# What the VO discrepancy report should contain:

# 1) VOs reporting to gratia:
#         - 1. Not registered in OIM : Error
#         - 2. Not active, but has Disabled status in OIM: Error
#         - 3. Not active, but has Enabled in OIM: Warning

# 2) VOs not reporting to gratia
#         - 4. Registered as Active in OIM: Warning
# ----------------------------------------------------------------
# Logic: Obtain the list of VOs that showed up in the range between start_date and end_date from the gratia database
#	 Obtain the list of VOs that is published by MyOSG
#	 Find the difference

##############################################################################################################################################
 
import sys,re, AccountingReports

def compareVOs(argv=None):
    # Gather different VO lists needed for the report from OIM

    gratia = adjustFermilabVOs(sorted(AccountingReports.GetReportingVOs(AccountingReports.gBegin, AccountingReports.gEnd)))
    oimActive = adjustFermilabVOs(sorted(AccountingReports.GetListOfRegisteredVO('Active',AccountingReports.gBegin,AccountingReports.gEnd)))
    oimEnabled = adjustFermilabVOs(sorted(AccountingReports.GetListOfRegisteredVO('Enabled',AccountingReports.gBegin,AccountingReports.gEnd)))
    oimDisabled = adjustFermilabVOs(sorted(AccountingReports.GetListOfRegisteredVO('Disabled',AccountingReports.gBegin,AccountingReports.gEnd)))

    oimAll = oimActive + oimEnabled + oimDisabled
    excluded = ['unknown','other']

    diff = {}
    diff['registered'] = list(set(gratia) - set(oimAll) - set(excluded))
    diff['enabled']    = list(set(gratia) & set(oimEnabled) - set(excluded))  # intersection
    diff['disabled']   = list(set(gratia) & set(oimDisabled) - set(excluded)) # intersection

    #==========================================================================================================================
    # Section 1 to 3:
    #==========================================================================================================================
    # 1) VOs reporting to gratia:
    #         - 1. Not registered in OIM : Error
    #         - 2. Not active, but has Disabled status in OIM: Error
    #         - 3. Not active, but has Enabled in OIM: Warning

    message = ""
    reportTypes = ['registered','enabled','disabled']

    for type in reportTypes:
        if(len(diff[type]) > 0):
            message+=voReport(diff,type)

    #==========================================================================================================================
    # Section 4: What are the VOs active in OIM but not reporting to gratia - might indicate inactivity for that particular VO
    #==========================================================================================================================
    diff = []
    diff = list(set(oimActive) - set(gratia) - set(excluded))
    if(len(diff) > 0):
        message+="WARNING! " + str(len(diff)) + " active VOs in OIM did not report to gratia. These VOs are listed below.\n\n"
        message+=printBigList(diff)
        message+="\n\n"

    #===================
    # Finish the report
    #===================
    message+="\n"
    subject = "VOs discrepancy report for "+str(AccountingReports.gBegin) + " to " + str(AccountingReports.gEnd)
    content={}
    content['text'] = message
    content['html'] = "<pre>" + message + "</pre>"
    content['csv'] = str(None)

    # If the report is run with a emailto option then email the report to the intended recipients
    if(AccountingReports.gEmailTo):
        AccountingReports.sendEmail( ([None], AccountingReports.gEmailTo), subject, content, None,None,None)

    # otherwise simply print the report to STDOUT
    else:
        print content['text']

def adjustFermilabVOs(voList):
   # input list sample: ["fermilab-astro", "fermilab-hypercp", "usatlas", "astro", "engage", "hypercp"]
   # output list sample: [ "usatlas", "astro", "engage", "hypercp"]
   # logic: The vo "fermilab-xxx" is consider the same as "xxx". Hence strip-off the "fermilab-" prefix and remove duplicates
   ret = [] 
   for vo in voList:
      if(vo == "fermilabgrid"):
         vo = "fermilab"
      elif(vo.find("fermilab-") >= 0): # "fermilab-xxx" will  match
         vo = re.compile("^fermilab-(.*)").search(vo).group(1) # "fermilab-xxx" becomes xxx
      elif(vo.find("fermilab") >= 0): # "fermilabxxx" will  match
         vo = re.compile("^fermilab(.*)").search(vo).group(1) # "fermilabxxx" becomes xxx
      ret.append(vo)
   return list(set(ret)) # eliminate duplicates by converting the list to a set and back to a list

def voReport(diff,type):
    message = ""
    if(type == "registered"):
        message = "ALERT! " + str(len(diff[type])) + " VOs reporting to gratia are not registered in OIM . These VOs are listed below.\n\n"
    elif(type == "enabled"):
        message = "WARNING! " + str(len(diff[type])) + " VOs reporting to gratia are not active, but enabled in OIM . These VOs are listed below.\n\n"
    elif(type == "disabled"):
        message = "ERROR! " + str(len(diff[type])) + " VOs reporting to gratia are not active and disabled in OIM . These VOs are listed below.\n\n"
    message+=voTable(diff[type])
    message+="\n\n"
    return message

def voTable(voList):
    # Find out the sites that report these VOs. The code below creates a detailed formatted report. 
    voStr=""
    # Construct the mysql query string using the conditions for VOnames like (VOName='zeus' or VOName='aceace')
    for vo in sorted(voList,key=str.lower):
        voStr+="VOName=\"" + vo + "\" or "
    voStr=re.compile("VOName=.*\"",re.IGNORECASE).search(voStr).group(0)
    # join query to find the list of sites that reported these VOs. See the voStr constructed above being used here
    query = "select T.SiteName, J.VOName from Site T, Probe P, VOProbeSummary J where (" + voStr + ") and P.siteid = T.siteid and J.ProbeName = P.probename and EndTime >= \"" + str(AccountingReports.gBegin) + "\" and EndTime < \"" + str(AccountingReports.gEnd) + "\" and J.ProbeName not like \"psacct:%\" group by J.VOName,T.SiteName order by lower(J.VOName),lower(T.SiteName);"
    # Run the query and get the results
    siteVO = AccountingReports.RunQueryAndSplit(query)
    # If one or more VOs matched this criteria, then create a formatted report 
    if len(siteVO)!=0:
        dashLen=60; # number of dashes needed for the horizantal dashed separator line in the table
        message = dashLen*"-" + "\n"
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
                message+=("| %2s.%s |%5s%-20s|%5s %-20s|"%(voCount,siteCount," ",vo," ",site))+"\n"
            # If it is the same VO, then don't print the VO name again, but just the site name that reported that VO in a separate line
            else:
                message+=("| %2s.%s |%5s%-20s|%5s %-20s|"%(voCount,siteCount," "," "," ",site))+"\n"
            # keep track of the previous VO
            prevVO = vo
            # increase site count for every row returned
            siteCount+=1
        # print the footer horizantal dashed line
        message+=dashLen*"-"+"\n"
    return message
     

def printBigList(bigList,cols=2): # print into 2 columns by default
    # print the elements in the list by inserting an end-line character every 'n' elements
    count=0
    message=""
    # sort VOs in the list alphabetically ignoring case
    for vo in sorted(bigList,key=str.lower):
        count+=1
        if(count < 10):
            message+="%d.  %-30s"%(count,vo)
        else:
            message+="%d. %-30s"%(count,vo)
        if count%cols==0:
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

#!/usr/bin/env python

import AccountingReports
import os
import sys

# script to validate the config file (gratiareports.conf) and set up the cron job etc. 
# to generate and email the reports

# global variables
gTmpCronFileOld = "/tmp/tmpCronOld.txt"
gTmpCronFileNew = "/tmp/tmpCronNew.txt"
#gCronPattern = "gratia reports - DO NOT edit this line" # unique pattern that identifies a gratia reporting entry in the crontab
gCronPattern = "Gratia entry created by configReport.py. *DON'T* edit this line." # unique pattern that identifies a gratia reporting entry in the crontab
# Save existing crontab entry to a temporary file to start with
os.system("crontab -l > " + gTmpCronFileOld)

def main(argv=None):
    AccountingReports.UseArgs(argv)
    if not AccountingReports.CheckDB() :
        print "Error!!! Cannot connect to the main database. Please check the connection credentials defined in the [main_db] section of " + '"' + AccountingReports.gConfigFiles + '"'
        return 1
    checkSmtpHost()
    editCronTab(stringToBoolean(AccountingReports.gConfig.get("report","cron")))

def checkSmtpHost():
    if (extractVar("email","smtphost") == ""):
        print "Error!!! smtphost needs to be set under the [email] section of \"" + AccountingReports.gConfigFiles + "\""

def extractToEmail(type):
    toEmail = extractVar("email", type+"To")
    if toEmail == "":
        print "ERROR!!! Please set the recipient's (" + type + "To) email address under the [email] section in " + AccountingReports.gConfigFiles
        sys.exit(1)
    return toEmail

def extractVar(section, var):
    try:
        return AccountingReports.gConfig.get(section, var)
    except:
        print "There was a problem trying to extract variable \"" + var + "\" from the section \"" + section + "\" in " + AccountingReports.gConfigFiles + ". Either the section and/or variable doesn't exist. Please check the config file. If you can't figure this out, please report this error to the gratia developers at gratia-operation@opensciencegrid.org along with the contents of the config file."
        sys.exit(1)

def editCronTab(enableCron):
    added = False
    fileR = open(gTmpCronFileOld, 'r')
    fileW = open(gTmpCronFileNew, 'w')
    for line in fileR: 
       if line.find(gCronPattern) != -1:
           if enableCron:
               fileW.write(cronString())
               enableCron = False
               added = True
       else:   
          fileW.write(line)
    if not added and enableCron: # will hit this case if there are no gratia reports related cron entries to start with
        fileW.write(cronString())
    # put the new crontab back
    fileW.close()
    fileR.close()
    cmd = "crontab " + gTmpCronFileNew
    out = os.system(cmd)

# extract all report options from the values defined in the config file
def reportOptions():
    optionStr = ""
    reportType = AccountingReports.gConfig.get("report","type")
    toEmail = extractToEmail(reportType)
    if reportType == "nonProduction":
        optionStr = "--mail " + toEmail
    elif reportType == "production":
        optionStr = "--production"
    return optionStr

def cronString():
    ret = "00 07 * * * sh daily_mutt.sh " + reportOptions() + " # " + gCronPattern + "\n"
    ret += "05 07 * * * sh range_mutt.sh " + reportOptions() + " # " + gCronPattern + "\n"
    return ret

# Convert string values ("True", "False") defined in the config file to equivalent boolean values (True, False)
def stringToBoolean(inStr):
    if inStr == "True":
        return True
    return False

if __name__ == "__main__":
    sys.exit(main())

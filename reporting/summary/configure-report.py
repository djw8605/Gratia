#!/usr/bin/env python

import AccountingReports
import os
import sys

# script to validate the config file (gratiareports.conf) and set up the cron job etc. 
# to generate and email the reports

tmpCronFileOld = "/tmp/tmpCronOld.txt"
tmpCronFileNew = "/tmp/tmpCronNew.txt"
# Save existing crontab entry to a temporary file to start with
os.system("crontab -l > " + tmpCronFileOld)

def main(argv=None):
    AccountingReports.UseArgs(argv)
    if not AccountingReports.CheckDB() :
        print "Error!!! Cannot connect to the main database. Please check the connection credentials defined in the [main_db] section of " + '"' + AccountingReports.gConfigFiles + '"'
        return 1
    checkSmtpHost()
    editCronTab(stringToBoolean(AccountingReports.gConfig.get("cron","enabled")))

def checkSmtpHost():
    if (extractVar("email","smtphost") == ""):
        print "Error!!! smtphost needs to be set under the [email] section of \"" + AccountingReports.gConfigFiles + "\""

def extractToEmail():
    toEmail = extractVar("email", "to")
    if toEmail == "":
        print "ERROR!!! Please set the recipient's (to) email address under the [email] section in " + AccountingReports.gConfigFiles
        sys.exit(1)
    return toEmail

def extractVar(section, var):
    return AccountingReports.gConfig.get(section, var)

def editCronTab(enableCron):
    added = False
    fileR = open(tmpCronFileOld, 'r')
    fileW = open(tmpCronFileNew, 'w')
    for line in fileR: 
       if line.lower().find("daily_mutt.sh") != -1:
           if enableCron:
               fileW.write(cronString())
               added = True
       else:   
          fileW.write(line)
    if not added and enableCron:
        fileW.write(cronString())
    # put the new crontab back
    fileW.close()
    fileR.close()
    cmd = "crontab " + tmpCronFileNew
    out = os.system(cmd)

def cronString():
    return "00 07 * * * sh daily_mutt.sh --mail " + extractToEmail() + "\n"

# Convert string values ("True", "False") defined in the config file to equivalent boolean values (True, False)
def stringToBoolean(inStr):
    if inStr == "True":
        return True
    return False

if __name__ == "__main__":
    sys.exit(main())

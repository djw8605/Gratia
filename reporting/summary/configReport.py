#!/usr/bin/env python

import AccountingReports
import os
import sys

# script to validate the config file (gratiareports.conf) and set up the cron job etc. 
# to generate and email the reports

# global variables
gTmpCronFileOld = "/tmp/tmpCronOld.txt"
gTmpCronFileNew = "/tmp/tmpCronNew.txt"
gCronPattern = " Gratia cron entry :-). Don't edit after #. Could remove the whole entry though." # unique pattern that identifies a gratia reporting entry in the crontab

# Save existing crontab entry to a temporary file to start with
os.system("crontab -l > " + gTmpCronFileOld)

def main(argv=None):
    AccountingReports.UseArgs(argv)
    if not AccountingReports.CheckDB() :
        print "Error!!! Cannot connect to the main database. Please check the connection credentials defined in the [main_db] section of " + '"' + AccountingReports.gConfigFiles + '"'
        configFailedExit()
    sanityCheck()
    editCronTab(stringToBoolean(extractVar("report","cron"))) # This step needs to be done irrespective of if the cron is enabled or disabled in the config file
    print "Configuration completed successfully.\nPlease note that you have to run this script everytime you make changes to your " + AccountingReports.gConfigFiles + " config file."

def configFailedExit():
    print "Configuration failed. Please fix and try again."
    sys.exit(1)

def sanityCheck():
    smtpHostCheck()
    emailCheck()
    installDirCheck()

def smtpHostCheck():
    if (extractVar("email","smtphost") == ""):
        print "Error!!! smtphost needs to be set under the [email] section of \"" + AccountingReports.gConfigFiles + "\""
        configFailedExit()

def emailCheck():
    toEmail = extractVar("email", "to")
    if toEmail == "" :
        if stringToBoolean(extractVar("report", "cron")):
            print "ERROR!!! Please set the recipient's \"to\" email address under the [email] section in " + AccountingReports.gConfigFiles + ". This needs to be set because the report cron is enabled and we need an email to send the reports to."
            configFailedExit() 
        else:
            print "Warning!!! The recipient's \"to\" email address under the [email] section in the config file " + AccountingReports.gConfigFiles + " is empty. This means that when you run the reporting scripts, the reports will be printed to the screen by default, unless you explicitly specify an email recipient by using the \"--mail\" option." 
    return toEmail

def installDirCheck():
    installDir = extractVar("report","installDir")
    if installDir == "" or installDir[0] != "/":
        print "ERROR!!! The report installation directory is the absolute directory path in which the reporting scripts have been installed to. This needs to be set with an absolute path. Refer to the installDir variable under the [ report ] section in " + AccountingReports.gConfigFiles + " to set a absolute path value."
        configFailedExit()
    return installDir

def extractVar(section, var):
    try:
        return AccountingReports.gConfig.get(section, var)
    except:
        print "ERROR!!! There was a problem trying to extract variable \"" + var + "\" from the section \"" + section + "\" in " + AccountingReports.gConfigFiles + ". Either the section and/or variable doesn't exist. Please check the config file. If you can't figure this out, please report this error to the gratia developers at gratia-operation@opensciencegrid.org along with the contents of the config file."
        configFailedExit() 

def editCronTab(enableCron):
   # If enableCron is True, the cron entry if it already exists needs to be edited or if the cron entry doesn't exist, it needs to be added
   # If enableCron is False, the cron entry if it already exists needs to be removed or if the cron entry doesn't exist, nothing needs to be done
   # All the above logic is incorporated in the editCronTab() function
    userInput = ""
    if not enableCron:
        print "ALERT!!! Cron has been disabled in " + AccountingReports.gConfigFiles + ". This will remove existing gratia reporting cron entries if any. Are you sure?: "
        while userInput.strip() != "yes":
            userInput = raw_input("Type yes to continue: ")
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
    cmd = "cp " + gTmpCronFileNew + " /etc/cron.d/gratia-reporting.cron"
    out = os.system(cmd)

# extract all report options from the values defined in the config file
def reportOptions():
    return "--mail " + emailCheck()

def userSiteReportEmail():
    email = extractVar("email","userTo")
    if email == "":
        return emailCheck()
    return email

def cronString():
    installDir = extractVar("report","installDir")
    userInput = ""
    if installDir == "":
        print "ERROR!!! The installation directory needs to be set. Refer to the installDir variable under the [ report ] section in " + AccountingReports.gConfigFiles + " to set a value."
        sys.exit(1)
    cdStr = "cd " + installDir + "; "
    print "ALERT!!! In another terminal window, please edit " +  "/etc/gratia/gratia-reporting/user-reports.dat" + "\n and set/change the VO names, site names and recipient emails for the perl reports (all-vos-oim & all-sites-oim) to the ones of your choice. \n\nWhen done type \"yes\" to continue.\n" 
    while userInput.strip() != "yes":
        userInput = raw_input("Are you done editing the report recipient emails in /etc/gratia/gratia-reporting/user-reports.dat? (Type yes to continue): ")
    ret =  "00 07 * * * root " + cdStr + "sh daily_mutt.sh; # " + gCronPattern + "\n"
    ret +=  "00 07 * * 1 root " + cdStr + "sh range_mutt.sh  # " + gCronPattern + "\n"
    ret += "01 07 * * 1 root " + cdStr + " perl all-vos-oim --production -D /etc/gratia/gratia-reporting/user-reports.dat > /dev/null #" + gCronPattern + "\n"
    ret += "01 07 * * 1 root " + cdStr + " perl all-sites-oim --production -D /etc/gratia/gratia-reporting/user-reports.dat > /dev/null #" + gCronPattern + "\n"
    ret += "02 07 1 * * root " + cdStr + " perl all-vos-oim --production -M -D /etc/gratia/gratia-reporting/user-reports.dat > /dev/null #" + gCronPattern + "\n"
    ret += "02 07 1 * *  root " + cdStr + " perl all-sites-oim --production -M -D /etc/gratia/gratia-reporting/user-reports.dat > /dev/null #" + gCronPattern + "\n"
    ret += "03 07 1 1 *  root " + cdStr + " perl all-vos-oim --production -Y -D /etc/gratia/gratia-reporting/user-reports.dat > /dev/null #" + gCronPattern + "\n"
    return ret

# Convert string values ("True", "False") defined in the config file to equivalent boolean values (True, False)
def stringToBoolean(inStr):
    if inStr == "True":
        return True
    return False

if __name__ == "__main__":
    sys.exit(main())

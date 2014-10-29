#
# Author Philippe Canal
#
# AccountingReports
#
# library to create reports using the Gratia databases
#
#@(#)gratia/summary:$HeadURL$:$Id$

import os
import sys
import commands
import time
import datetime
import getopt
import math
import re
import string
import smtplib

import libxml2
import urllib2

import logging
import optparse
import logging.config
import ConfigParser

from email.MIMEText import MIMEText
from email.MIMEImage import MIMEImage
from email.MIMEMultipart import MIMEMultipart
from email.MIMEBase import MIMEBase
from email.Utils import formataddr
from email.quopriMIME import encode
from cStringIO import StringIO

gMySQL = "mysql"
gProbename = "cmslcgce.fnal.gov"
gLogFileIsWriteable = True;

gBegin = None
gEnd = None
gWithPanda = False
gGroupBy = "Site"
gVOName = ""
gConfigFiles = None
gConfig = ConfigParser.ConfigParser()
gEmailTo = None
gEmailToNames = None
gEmailSubject = "not set"
gGrid = None  # variable to indicate if only to extract rows with Grid='OSG'  
# Database connection related global variables
gDBHostName = {} 
gDBPort = {} 
gDBUserName = {} 
gDBPassword = {} 
gDBSchema = {} 

# section names in confguration file
mainDB = "main_db"
psacctDB = "psacct_db"
dailyDB = "daily_db"
transferDB = "transfer_db"
gDBCurrent = mainDB # variable to keep track of current db on which RunQuery is running the query from
gDBConnectOK = {} # variable to keep track of if the connection to a particular DB is fine, so that this doesn't have to be checked again and again

gOutput="text" # Type of output (text, csv, None)

"""
Having written a bunch of scientific software, I am always amazed
at how few languages have built in routines for displaying numbers
nicely.  I was doing a little programming in Python and got surprised
again.  I couldn't find any routines for displaying numbers to
a significant number of digits and adding appropriate commas and
spaces to long digit sequences.  Below is my attempt to write
a nice number formatting routine for Python.  It is not particularly
fast.  I suspect building the string by concatenation is responsible
for much of its slowness.  Suggestions on how to improve the 
implementation will be gladly accepted.

			David S. Harrison
			(daha@best.com)
"""

# Returns a nicely formatted string for the floating point number
# provided.  This number will be rounded to the supplied accuracy
# and commas and spaces will be added.  I think every language should
# do this for numbers.  Why don't they?  Here are some examples:
# >>> print niceNum(123567.0, 1000)
# 124,000
# >>> print niceNum(5.3918e-07, 1e-10)
# 0.000 000 539 2
# This kind of thing is wonderful for producing tables for
# human consumption.
#
def niceNum(num, precision = 1):
    """Returns a string representation for a floating point number
    that is rounded to the given precision and displayed with
    commas and spaces."""
    accpow = int(math.floor(math.log10(precision)))
    if num < 0:
        digits = int(math.fabs(num/pow(10,accpow)-0.5))
    else:
        digits = int(math.fabs(num/pow(10,accpow)+0.5))
    result = ''
    if digits > 0:
        for i in range(0,accpow):
            if (i % 3)==0 and i>0:
                result = '0,' + result
            else:
                result = '0' + result
        curpow = int(accpow)
        while digits > 0:
            adigit = chr((digits % 10) + ord('0'))
            if (curpow % 3)==0 and curpow!=0 and len(result)>0:
                if curpow < 0:
                    result = adigit + ' ' + result
                else:
                    result = adigit + ',' + result
            elif curpow==0 and len(result)>0:
                result = adigit + '.' + result
            else:
                result = adigit + result
            digits = digits/10
            curpow = curpow + 1
        for i in range(curpow,0):
            if (i % 3)==0 and i!=0:
                result = '0 ' + result
            else:
                result = '0' + result
        if curpow <= 0:
            result = "0." + result
        if num < 0:
            result = '-' + result
    else:
        result = "0"
    return result 

#import sys, os, commands, time, shutil, glob, struct, pwd, string, socket
import commands, os, sys, time, string

class Usage(Exception):
    def __init__(self, msg):
        self.msg = msg

def UseArgs(argv):
    global gProbename, gOutput, gWithPanda, gGroupBy, gVOName, gEmailTo
    global gEmailToNames,gEmailSubject,gConfig,gGrid,gConfigFiles

    monthly = False
    weekly = False
    daily = False
    
    configFiles = "/etc/gratia/gratia-reporting/gratiareports.conf"
    
    if argv is None:
        argv = sys.argv[1:]

    parser = optparse.OptionParser()
    parser.add_option("-m", "--monthly", help="Report range covers the last" \
        " month's worth of data", dest="monthly", default=False, action=\
        "store_true")
    parser.add_option("-w", "--weekly", help="Report range covers the last " \
        " week's worth of data", dest="weekly", default=False, action=\
        "store_true")
    parser.add_option("-d", "--daily", help="Report range covers the last " \
        "day's worth of data", dest="daily", default=False, action="store_true")
    parser.add_option("-p", "--probe", help="Probe to query for report (if " \
        "applicable", dest="probename")
    parser.add_option("--output", help="Output format (text,csv,None).",
        dest="output", default="text")
    parser.add_option("--with-panda", help="Include separate ATLAS " \
        "Panda data.", dest="panda", default=False, action="store_true")
    parser.add_option("--groupby", help="What entity to group data by " \
        "(Site,VO).", dest="groupby", default="Site")
    parser.add_option("--voname", help="VOName for VO-specific reports.",
        dest="voname", default="")
    parser.add_option("-c", "--config", help="Config file(s) to use.",
        dest="config", default=configFiles)
    parser.add_option("--emailto", help="Destination email addresses.",
        dest="email", default=None)
    parser.add_option("--subject", help="Subject line for email.",
        dest="subject", default="not set")
    parser.add_option("--grid", help="Grid to restrict reports to.",
        dest="grid", default=None)

    options, args = parser.parse_args(argv)

    # Eventually, it would be nice to treat options as a configuration object
    # instead of passing around global data.  Eventually.
    monthly       = options.monthly
    weekly        = options.weekly
    daily         = options.daily
    gProbename    = options.probename
    gOutput       = options.output
    gWithPanda    = options.panda
    gGroupBy      = options.groupby
    gVOName       = options.voname
    configFiles   = [i.strip() for i in options.config.split(',')][0]
    if options.email:
        gEmailTo  = [i.strip() for i in options.email.split(',')]
    gEmailSubject = options.subject
    gGrid         = options.grid

    if not os.path.isfile(configFiles):
        print "ERROR!!! Cannot read " + configFiles + ". Make sure file exists and is readable. For an example, refer to gratiareports.conf.template."
        sys.exit(1)

    gConfigFiles = configFiles # store value to a global variable to be used later
    gConfig.read(configFiles)

    # Get DB connection credentials
    DBConnectString()

    if (gEmailToNames == None and gEmailTo != None):
       gEmailToNames = ["" for i in gEmailTo]
    
    start = ""
    end = ""
    if args:
        start = args[0]
        if len(args) > 1:
                end =  args[1]
        if monthly:
            if end:
                print >> sys.stderr, "Warning: With --monthly the 2nd date is" \
                    " ignored"
            SetMonthlyDate(start)
        elif weekly:
            if end:
                print >> sys.stderr, "Warning: With --weekly the 2nd date is" \
                    " ignored"
            SetWeeklyDate(start)
        elif daily:
            if end:
                print >> sys.stderr, "Warning: With --daily the 2nd date is " \
                    "ignored"
            SetDailyDate(start)
        else:
            SetDate(start,end)


def AddMonth(fromd, month):
    newyear = fromd.year
    newmonth = fromd.month + month
    while newmonth < 1:
       newmonth += 12
       newyear -= 1
    while newmonth > 12:
       newmonth -= 12
       newyear += 1
    return datetime.date(newyear,newmonth,fromd.day)
    
def SetMonthlyDate(start):
    " Set the start and end date to be the begin and end of the month given in 'start' "
    global gBegin, gEnd
    
    when = datetime.date(*time.strptime(start, "%Y/%m/%d")[0:3])
    gBegin = datetime.date( when.year, when.month, 1 )
    gEnd = AddMonth( gBegin, 1 )
    
def SetWeeklyDate(end):
    " Set the start and end date to the week preceding 'end' "
    global gBegin, gEnd

    gEnd = datetime.date(*time.strptime(end, "%Y/%m/%d")[0:3])
    gBegin = gEnd - datetime.timedelta(days=7)     
     
def SetDailyDate(end):
    " Set the start and end date to the week preceding 'end' "
    global gBegin, gEnd

    gEnd = datetime.date(*time.strptime(end, "%Y/%m/%d")[0:3])
    gBegin = gEnd - datetime.timedelta(days=1)     
     
def SetDate(start,end):
    " Set the start and begin by string"
    global gBegin, gEnd
    if len(start) > 0:
        gBegin = datetime.date(*time.strptime(start, "%Y/%m/%d")[0:3]) 
    if len(end) > 0:
        gEnd = datetime.date(*time.strptime(end, "%Y/%m/%d")[0:3]) 

def ProbeWhere():
    global gProbename
    if (gProbename != None) :
        return " and ProbeName=\"" + gProbename + "\""
    else:
        return ""

def CommonWhere():
    global gProbeName, gBegin, gEnd
    return " VOName != \"Unknown\" and \"" \
        + DateToString(gBegin) +"\"<=EndTime and EndTime<\"" + DateToString(gEnd) + "\"" \
        + ProbeWhere()

def StringToDate(input):
    return datetime.datetime(*time.strptime(input, "%d/%m/%Y")[0:5])

def DateToString(input,gmt=False):
    if gmt:
        return input.strftime("%Y-%m-%d 07:00:00");
    else:
        return input.strftime("%Y-%m-%d");

def LogToFile(message):
    "Write a message to the Gratia log file"

    global gLogFileIsWriteable
    file = None
    filename = "none"

    try:
        filename = "/var/log/gratia-reporting/"+time.strftime("%Y-%m-%d") + ".log"
        #filename = os.path.join(Config.get_LogFolder(),filename)

        if os.path.exists(filename) and not os.access(filename,os.W_OK):
            os.chown(filename, os.getuid(), os.getgid())
            os.chmod(filename, 0755)

        # Open/Create a log file for today's date
        file = open(filename, 'a')

        # Append the message to the log file
        file.write(message + "\n")

        gLogFileIsWriteable = True;
    except:
        if gLogFileIsWriteable:
            # Print the error message only once
            print "Gratia: Unable to log to file:  ", filename, " ",  sys.exc_info(), "--", sys.exc_info()[0], "++", sys.exc_info()[1]
        gLogFileIsWriteable = False;

    if file != None:
        # Close the log file
        file.close()

def _toStr(toList):
    names = [formataddr(i) for i in zip(*toList)]
    return ', '.join(names)

def sendEmail( toList, subject, content, log, fromEmail = None, smtpServerHost=None):
    """
    This turns the "report" into an email attachment
    and sends it to the EmailTarget(s).
    """
    if (fromEmail == None):
       fromEmail = (gConfig.get("email","realname"),gConfig.get("email","from"))
    if (smtpServerHost == None):
       try:
           smtpServerHost = gConfig.get("email", "smtphost")
       except:
           print "ERROR!!! The email section in " + gConfigFiles + " either does not exist or does not contain the smtphost information or has an error in it. See  gratiareports.conf.template for examples and make sure " + gConfigFiles + " confirms to the requirement and has all values properly filled-in."
           sys.exit(1)
    if (toList[1] == None):
       print "Cannot send mail (no To: specified)!"
       return
       
    msg = MIMEMultipart()
    msg["Subject"] = subject
    msg["From"] = formataddr(fromEmail)
    msg["To"] = _toStr(toList)
    msg1 = MIMEMultipart("alternative")
    #msgText = MIMEText(encode(reportText), "plain", "iso-8859-1")
    msgText1 = MIMEText("<pre>" + content["text"] + "</pre>", "html")
    msgText2 = MIMEText(content["text"])
    msgHtml = MIMEText(content["html"], "html")
    msg1.attach(msgHtml)
    msg1.attach(msgText2)
    msg1.attach(msgText1)
    msg.attach(msg1)
    attachment_html = "<html><head><title>%s</title></head><body>%s</body>" \
        "</html>" % (subject, content["html"])
    part = MIMEBase('text', "html")
    part.set_payload( attachment_html )
    part.add_header('Content-Disposition', \
        'attachment; filename="report_%s.html"' % datetime.datetime.now().\
        strftime('%Y_%m_%d'))
    msg.attach(part)
    attachment_csv = content["csv"]
    part = MIMEBase('text', "csv")
    part.set_payload( attachment_csv )
    part.add_header('Content-Disposition', \
        'attachment; filename="report_%s.csv"' % datetime.datetime.now().\
        strftime('%Y_%m_%d'))
    msg.attach(part)
    msg = msg.as_string()

    #log.debug( "Report message:\n\n" + msg )
    if len(toList[1]) != 0:
        server = smtplib.SMTP( smtpServerHost )
        server.sendmail( fromEmail[1], toList[1], msg )
        server.quit()
    else:
        # The email list isn't valid, so we write it to stdout and hope
        # it reaches somebody who cares.
        print "Problem in sending email to: ",toList

def sendAll(text, filestem = "temp"):
   global gEmailTo,gEmailToNames,gEmailSubject
   
   if len(text["text"]) == 0:
      return;

   if (gEmailTo == None):
      for iterOutput in ("text","csv","html"):
         print "===="+iterOutput+"===="
         print text[iterOutput]
   else:
      LogToFile("Sending: %(subject)s To: %(names1)s / %(names2)s" % \
                { "subject" : gEmailSubject, "names1" : gEmailToNames , "names2" : gEmailTo })
      sendEmail( (gEmailToNames, gEmailTo), gEmailSubject, text, None)

def DBConnectString():
    global gMySQLConnectString,gMySQLFermiConnectString,gMySQLDailyConnectString,gMySQLTransferConnectString
    gMySQLConnectString      = DBConnectStringHelper(mainDB)
    gMySQLFermiConnectString = DBConnectStringHelper(psacctDB)
    gMySQLDailyConnectString = DBConnectStringHelper(dailyDB)
    gMySQLTransferConnectString = DBConnectStringHelper(transferDB)

def DBConnectStringHelper(dbName):
    global gDBHostName,gDBUserName,gDBPort,gDBPassword,gDBSchema,gConfig,gDBConnectOK
    try:
        gDBHostName[dbName] = gConfig.get(dbName, "hostname") 
        gDBUserName[dbName] = gConfig.get(dbName, "username") 
        gDBPort[dbName] = gConfig.get(dbName, "port") 
        gDBPassword[dbName] = gConfig.get(dbName, "password") 
        gDBSchema[dbName] = gConfig.get(dbName, "schema") 
        gDBConnectOK[dbName] = False
    # Issue an error and exit if a section is missing or something isn't set or isn't set properly in the config file
    except:
        print "ERROR!!! The " + dbName + " section in " + gConfigFiles + " either does not exist or does not contain all the needed information or has an error in it. See gratiareports.conf.template for examples and make sure " + gConfigFiles + " confirms to the requirement and has all values properly filled-in."
        sys.exit(1)
    return " -h " + gDBHostName[dbName] + " -u " + gDBUserName[dbName] + " --port=" + gDBPort[dbName] + " --password=" + gDBPassword[dbName] + " -N " +  gDBSchema[dbName]


def CheckDB():
        global gMySQL,gMySQLConnectString
        (status, output) = commands.getstatusoutput( gMySQL + gMySQLConnectString + " -e status "  )
        if status == 0:
            msg =  "Status: \n"+output
            if output.find("ERROR") >= 0 :
                status = 1
                msg = "Error in running mysql:\n" + output
        else:
            msg = "Error in running mysql:\n" + output
            
        if status != 0:
            LogToFile("Gratia: "+ msg)
            print msg
        return status == 0

def RunQuery(select):
        global gMySQL,gMySQLConnectString,gGrid
        if not gDBConnectOK[gDBCurrent]:
            if not CheckDB():
                print  >> sys.stderr, "ERROR!!! Connecting to " + gDBCurrent + " failed. Connection string is \"mysql" + gMySQLConnectString + "\". Check for validity of " + gDBCurrent + " connection credentials in the " + gConfigFiles + " file "
                gDBConnectOK[gDBCurrent] = False
                sys.exit(1) 
            else:
                gDBConnectOK[gDBCurrent] = True
        # If the user explicitly requests from the command line to restrict queries to contain Grid="OSG" in the where clause, adjust the query to add Grid="OSG" at the appropriate place
        if(gGrid != None): # and gGrid.lower() == "osg"):
            select = AddGridToQuery(select,gGrid)
        LogToFile(select)
        # print "echo '" + select + "' | " + gMySQL + gMySQLConnectString
        return commands.getoutput("echo '" + select + "' | " + gMySQL + gMySQLConnectString )

def AddGridToQuery(select,gridvalue):
    query = "" # variable to store the modified query
    # split the query into several parts using 'from' as the de-limiter and process the parts to decide if to add the Grid="OSG" to the where clause
    for part in select.split('from'):
        modified = 0  # flag to indicate if part was altered
        # if part doesn't start with a (
        if(re.compile("^ *\(").search(part) == None):
            # if part has Summary in it
            if(re.compile(".*Summary.*").search(part)):
                # if part has where in it
                if(re.compile(".*where.*").search(part)):
                    # Add Grid="OSG" to the inner most where clause (which is the 1st where clause) and concat with the rest of the part
                    query+="from " + part.split('where')[0] + " where Grid=\""+gridvalue+"\" and " + string.join(part.split('where')[1:],'where ')
                    modified = 1 # mark that the part was modified
        if(modified == 0): # if not modified simply put back the part into the query
            query+="from " + part
    select = query[5:] # remove the prefix "from " in the query to make it correct
    return select


def RunQueryAndSplit(select):
        res = RunQuery(select)
        LogToFile(res)
        if ( len(res) > 0 ) :
           lines = res.split("\n")
        else:
           lines = []
        return lines


def NumberOfCpus():
        global gMySQLConnectString,gDBCurrent
        schema = gDBSchema[psacctDB];
        gDBCurrent = psacctDB
        keepConnectionValue = gMySQLConnectString
        gMySQLConnectString = gMySQLFermiConnectString
        
        select = "select sum(cpus),sum(bench) from " \
			    + " ( SELECT distinct J.Host, cpuinfo.CpuCount as cpus,cpuinfo.CpuCount*cpuinfo.BenchmarkScore/1000 as bench from " \
                + schema + ".CPUInfo cpuinfo,"+schema+".JobUsageRecord J " \
                + "where J.HostDescription=cpuinfo.NodeName " \
                + CommonWhere() + ") as Sub;"
        res = RunQuery(select);
        gMySQLConnectString = keepConnectionValue;
        LogToFile(res)
        values = res.split("\n")[1]
        ncpu = int(values.split("\t")[0])
        benchtotal = float(values.split("\t")[1]) 
        return (ncpu,benchtotal);

def GetListOfSites(filter,location = None):
        if location == None:
            location = 'http://myosg.grid.iu.edu/rgsummary/xml?datasource=summary&summary_attrs_showservice=on&account_type=cumulative_hours&ce_account_type=gip_vo&se_account_type=vo_transfer_volume&start_type=7daysago&all_resources=on&gridtype=on&gridtype_1=on&service=on&service_1=on'
        html = urllib2.urlopen(location).read()
        
        excludedSites = [ 'Engagement_VOMS', 'OSG_VOMS' ]
        
        sites = []
        doc = libxml2.parseDoc(html)
        for resource in doc.xpathEval(filter):
           if (resource.content not in excludedSites): sites.append(resource.content)
        doc.freeDoc();
        
        return sites;

def GetListOfDisabledOSGSites():
        if not gGrid or gGrid.lower() != "local":
            return GetListOfSites( "//Resource[Active='False']/Name" )
        else:
            try:
                return [i.strip() for i in gConfig.get("local",
                    "disabled_sites").split(",")]
            except:
                return None

def GetListOfOSGSEs():
    ret = []
    if not gGrid or gGrid.lower() != "local":
        location = 'http://myosg.grid.iu.edu/rgsummary/xml?datasource=summary&summary_attrs_showservice=on&account_type=cumulative_hours&ce_account_type=gip_vo&se_account_type=vo_transfer_volume&start_type=7daysago&start_date=11%2F04%2F2009&end_type=now&end_date=11%2F04%2F2009&all_resources=on&gridtype=on&gridtype_1=on&service=on&service_3=on&active_value=1&disable_value=1'
        return GetListOfSites("//Resource/Name",location)
    else:
        try:
            return [i.strip() for i in gConfig.get("local", "active_ses").split(",")]
        except:
            return None
                

def GetListOfOSGSites():
    if not gGrid or gGrid.lower() != "local":
        return GetListOfSites("//Resource[Active='True' and ( Services/Service/Name='Compute Element' or Services/Service/Name='CE' or Services='no applicable service exists')]/Name")
    else:
        try:
            return [i.strip() for i in gConfig.get("local", "active_sites").\
                split(",")]
        except:
            return None

gVOsWithReportingGroup = []
def extractReportingGroupVOs():
   global gVOsWithReportingGroup
   location = 'http://myosg.grid.iu.edu/vosummary/xml?datasource=summary&summary_attrs_showdesc=on&all_vos=on&show_disabled=on&summary_attrs_showreporting_group=on&active=on&active_value=1'
   html = urllib2.urlopen(location).read()
   doc = libxml2.parseDoc(html)
   vos = []
   for resource in doc.xpathEval("//VO/Name"):
      voName = ""
      voName = resource.content

      reportingGroupName = []
      filter = "/VOSummary/VO[Name='" + voName + "']/ReportingGroups/ReportingGroup/Name"
      for resource in doc.xpathEval(filter):
         reportingGroupName.append(resource.content)
      if(len(reportingGroupName) > 0):
         for rg in reportingGroupName:
            if(rg.lower().find("fermilab-") != -1):
               rg = re.compile("^fermilab-(.*)").search(rg).group(1)
            if not vosContainsName(rg,vos):
               vos.append((rg,""))
            gVOsWithReportingGroup.append(voName.lower())

   return vos


def GetListOfVOs(filter,voStatus,beginDate,endDate):
        LogToFile("#######################\n## def GetListOfVOs %s" % voStatus)
        name=""
        bd = str(beginDate).split("-") # begin date list
        ed = str(endDate).split("-") # end date list
        # date specific MyOSG url
        location = "http://myosg.grid.iu.edu/voactivation/xml?datasource=activation&start_type=specific&start_date=" + bd[1] +"%2F" + bd[2] + "%2F" + bd[0] + "&end_type=specific&end_date=" + ed[1] + "%2F" + ed[2] + "%2F" + ed[0] + "&all_vos=on&active_value=1"
        LogToFile("MyOsg query: %s" % location)
        html = urllib2.urlopen(location).read()
        vos = []
        doc = libxml2.parseDoc(html)
        if(voStatus == 'Active'):
           vos.extend(extractReportingGroupVOs())
        for resource in doc.xpathEval(filter):
           if resource.name == "Name":
              name = resource.content
           elif resource.name == "LongName" and name.lower() not in gVOsWithReportingGroup:
              vos.append( (name,resource.content))
        doc.freeDoc()
        LogToFile("Registered %s VOs: %s\n----------" % (voStatus,vos))
        if(voStatus == 'Active'):
            vos = addReportingGroups(vos)
        return vos

def addReportingGroups(vos):
    vos1 = addReportingGroupsHelper(vos)
    vosOnlyInReportingGroups = list(set(gVOsWithReportingGroup) - set(vos1))
    LogToFile("VOs only in Reporting Groups: %s\n-------" % (vosOnlyInReportingGroups))
    for vo in vosOnlyInReportingGroups:
        vos.append((vo,''))
    return vos

def addReportingGroupsHelper(vos):
    ret = []
    for vo in vos:
        ret.append(vo[0])
    return ret

def vosContainsName(inName, vos):
   for vo in vos:
      name, longName = vo 
      if name.lower() == inName.lower():
         return True
   return False      

def GetListOfAllRegisteredVO(beginDate,endDate):
    allVOs = []
    active = GetListOfRegisteredVO('Active',beginDate,endDate)
    if active:
        allVOs.extend(active)
    enabled = GetListOfRegisteredVO('Enabled',beginDate,endDate)
    if enabled:
        allVOs.extend(enabled)
    disabled = GetListOfRegisteredVO('Disabled',beginDate,endDate)
    if disabled:
        allVOs.extend(disabled)
    if disabled == None and active == None and enabled == None:
        return None
    return allVOs 

def GetListOfRegisteredVO(voStatus,beginDate,endDate):
    if not gGrid or gGrid.lower() != "local":
        return GetListOfOSGRegisteredVO(voStatus, beginDate, endDate)
    try:
        vos = gConfig.get("local", "%s_vos" % voStatus.lower())
        return [i.strip() for i in vos.split(",")]
    except:
        return None

def GetListOfOSGRegisteredVO(voStatus, beginDate, endDate):
    filter = "/VOActivation/" + voStatus + "/VO/Name|/VOActivation/" + voStatus + "/VO/LongName"
    allVos = GetListOfVOs(filter,voStatus,beginDate,endDate)
    ret = set([])
    printederror = False
    for pair in allVos:
           try:
              (longname,description) = pair
           except:
              if not printederror:
                 LogToFile("Gratia Reports GetListOfRegisteredVO unable to parse the result of: "+cmd)
                 sys.stderr.write("Gratia Reports GetListOfRegisteredVO unable to parse data, one example is: "+pair+"\n")
                 printederror = True
              LogToFile("Gratia Reports GetListOfRegisteredVO unable to parse: "+pair)
              continue
           if ("/" in description):
               ret.add(description.split("/")[1].lower())
           elif len(longname.lower()) > 0:
              ret.add( longname.lower() );
    # And hand add a few 'exceptions!"
    if(voStatus == 'Active'):
            ret.add("other")
            ret.add("other EGEE")
    LogToFile("Final Registered %s VOs (has coded additions): %s" % (voStatus,ret))
    return list(ret)


def UpdateVOName(list, index, range_begin, range_end):
      vos = GetListOfAllRegisteredVO(range_begin,range_end)
      r = []
      for row in list:
         srow = row.split('\t')
         if len(srow)>index and vos and srow[index] not in vos:
            srow[index] = srow[index] + " (nr)"
         r.append( "\t".join(srow) )
      return r

def WeeklyData():
        global gMySQLConnectString,gDBCurrent
        schema = gDBSchema[psacctDB];
        gDBCurrent = psacctDB
        keepConnectionValue = gMySQLConnectString
        gMySQLConnectString = gMySQLFermiConnectString

        select = " SELECT J.VOName, sum((J.CpuUserDuration+J.CpuSystemDuration)) as cputime, " + \
                 " sum((J.CpuUserDuration+J.CpuSystemDuration)*CpuInfo.BenchmarkScore)/1000 as normcpu, " + \
                 " sum(J.WallDuration)*0 as wall, sum(J.WallDuration*CpuInfo.BenchmarkScore)*0/1000 as normwall " + \
                 " from "+schema+".JobUsageRecord_Report J, "+schema+".CPUInfo CpuInfo " + \
                 " where J.HostDescription=CpuInfo.NodeName " + CommonWhere() + \
                 " group by J.VOName; "
        result = RunQueryAndSplit(select)
        gMySQLConnectString = keepConnectionValue;
        return result

def CondorData():
        select = " SELECT J.VOName, sum((J.CpuUserDuration+J.CpuSystemDuration)) as cputime, " + \
                      " sum((J.CpuUserDuration+J.CpuSystemDuration)*0) as normcpu, " + \
                      " sum(J.WallDuration) as wall, sum(J.WallDuration*0) as normwall " + \
                 " from VOProbeSummary J " + \
                 " where 1=1 " + CommonWhere() + \
                 " group by VOName; "
        return RunQueryAndSplit(select)

def DailySiteData(begin,end):
        schema = gDBSchema[mainDB]
        
        select = " SELECT Site.SiteName, sum(NJobs), sum(J.WallDuration) " \
                + " from "+schema+".Site, "+schema+".Probe, "+schema+".VOProbeSummary J " \
                + " where VOName != \"Unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
                + " and \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and ResourceType=\"Batch\"" \
                + " and J.ProbeName not like \"psacct:%\" " \
                + " group by Probe.siteid "
        return RunQueryAndSplit(select)

def DailyVOData(begin,end):
        schema = gDBSchema[mainDB]
            
        select = " SELECT J.VOName, Sum(NJobs), sum(J.WallDuration) " \
                + " from "+schema+".Site, "+schema+".Probe, "+schema+".VOProbeSummary J " \
                + " where VOName != \"Unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
                + " and \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and ResourceType=\"Batch\"" \
                + " and J.ProbeName not like \"psacct:%\" " \
                + " group by J.VOName "
        return RunQueryAndSplit(select)

def DailySiteVOData(begin,end):
        schema = gDBSchema[mainDB]
        
        select = " SELECT Site.SiteName, J.VOName, sum(NJobs), sum(J.WallDuration) " \
                + " from "+schema+".Site, "+schema+".Probe, "+schema+".VOProbeSummary J " \
                + " where VOName != \"Unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
                + " and \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and ResourceType=\"Batch\"" \
                + " and J.ProbeName not like \"psacct:%\" " \
                + " group by J.VOName, Probe.siteid order by Site.SiteName "
        return RunQueryAndSplit(select)

def DailyVOSiteData(begin,end):
        schema = gDBSchema[mainDB]
        
        select = " SELECT J.VOName, Site.SiteName, sum(NJobs), sum(J.WallDuration) " \
                + " from "+schema+".Site, "+schema+".Probe, "+schema+".VOProbeSummary J " \
                + " where VOName != \"Unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
                + " and \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and ResourceType=\"Batch\"" \
                + " and J.ProbeName not like \"psacct:%\" " \
                + " group by J.VOName, Probe.siteid order by J.VOName, Site.SiteName "
        return RunQueryAndSplit(select)

def DailySiteVODataFromDaily(begin,end,select,count):
        global gMySQLConnectString,gDBCurrent
        schema = gDBSchema[dailyDB]
        gDBCurrent = dailyDB
        keepConnectionValue = gMySQLConnectString
        gMySQLConnectString = gMySQLDailyConnectString
        
        select = " SELECT M.ReportedSiteName, J.VOName, "+count+", sum(J.WallDuration) " \
                + " from "+schema+".JobUsageRecord J," + schema +".JobUsageRecord_Meta M " \
                + " where VOName != \"Unknown\" and \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and M.dbid = J.dbid " \
                + " and ProbeName " + select + "\"daily:goc\" " \
                + " group by J.VOName, M.ReportedSiteName order by M.ReportedSiteName, J.VOName "
        result = RunQueryAndSplit(select)
        gMySQLConnectString = keepConnectionValue
        return result 

def DailyVOSiteDataFromDaily(begin,end,select,count):
        global gMySQLConnectString,gDBCurrent
        schema = gDBSchema[dailyDB]
        gDBCurrent = dailyDB
        keepConnectionValue = gMySQLConnectString
        gMySQLConnectString = gMySQLDailyConnectString
        
        select = " SELECT J.VOName, M.ReportedSiteName, "+count+", sum(J.WallDuration) " \
                + " from "+schema+".JobUsageRecord J," \
                + schema + ".JobUsagRecord_Meta M " \
                + " where VOName != \"Unknown\" and \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and M.dbid = J.dbid " \
                + " and ProbeName " + select + "\"daily:goc\" " \
                + " group by J.VOName, M.ReportedSiteName order by J.VOName, M.ReportedSiteName "
        result = RunQueryAndSplit(select)
        gMySQLConnectString = keepConnectionValue
        return result

def DailySiteJobStatusSummary(begin,end,selection = "", count = "", what = "Site.SiteName"):
        schema = gDBSchema[mainDB]
        
        select = " SELECT " + what + ", J.ApplicationExitCode, sum(Njobs), sum(WallDuration) " \
                + " from "+schema+".Site, "+schema+".Probe, " \
                + " ( select ApplicationExitCode, VOcorrid, ProbeName, EndTime, Njobs, WallDuration from MasterSummaryData "\
                + "   where \"" + DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + "   and ResourceType = \"Batch\" " \
                + "  ) J, VONameCorrection, VO " \
                + " where VO.VOName != \"Unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
                + " and " + VONameCorrectionSummaryJoin("J") \
                + selection \
                + " group by " + what + ",J.ApplicationExitCode " \
                + " order by " + what 
        return RunQueryAndSplit(select)
        
                        
def DailySiteJobStatus(begin,end,selection = "", count = "", what = "Site.SiteName"):
        schema = gDBSchema[mainDB]

        select = " SELECT " + what + ", J.Status, count(*), sum(WallDuration) " \
                + " from "+schema+".Site, "+schema+".Probe, " \
                + " ( select M.dbid, Status, VOName,ReportableVOName, ProbeName, EndTime, WallDuration, StatusDescription from JobUsageRecord, JobUsageRecord_Meta M "\
                + "   where JobUsageRecord.dbid = M.dbid " \
                + "   and \"" + DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + "   and ResourceType = \"Batch\" " \
                + "  ) J, VONameCorrection, VO " \
                + " where VO.VOName != \"Unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
                + " and " + VONameCorrectionJoin("J") \
                + selection \
                + " group by " + what + ",J.Status " \
                + " order by " + what 
        return RunQueryAndSplit(select)

def DailySiteJobStatusCondor(begin,end,selection = "", count = "", what = "Site.SiteName"):
        schema = gDBSchema[mainDB]

        select = " SELECT "+what+", R.Value,count(*), sum(WallDuration) " \
                + " from "+schema+".Site, "+schema+".Probe, "+schema+".Resource R, " \
                + " ( select M.dbid, VOName,ReportableVOName, ProbeName, EndTime, WallDuration, StatusDescription from JobUsageRecord, JobUsageRecord_Meta M "\
                + "   where JobUsageRecord.dbid = M.dbid " \
                + "   and \"" + DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + "   and ResourceType = \"Batch\" " \
                + "  ) J, VONameCorrection, VO " \
                + " where VO.VOName != \"Unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
                + " and J.dbid = R.dbid and R.Description = \"ExitCode\" " \
                + " and " + VONameCorrectionJoin("J") \
                + selection \
                + " group by " + what + ",R.Value " \
                + " order by " + what
        return RunQueryAndSplit(select)
# Condor Exit Status

def CMSProdData(begin,end):
    LogToFile("#######################\n## CMSProdData")
    schema = gDBSchema[mainDB]

    select = """select
    COALESCE(voc.VOName,"Total") as FQAN,
    round(sum(WallDuration)/3600,0)
from MasterSummaryData msd
    ,Probe p
    ,Site  s
    ,VONameCorrection voc
    ,VO  vo
where
    SiteName in ("USCMS-FNAL-WC1-CE", "USCMS-FNAL-WC1-CE2", "USCMS-FNAL-WC1-CE3", "USCMS-FNAL-WC1-CE4")
and s.siteid = p.siteid
and p.probename = msd.ProbeName
and msd.EndTime >= "%(begin)s" and msd.EndTime  < "%(end)s"
and msd.ResourceType = "Batch"
and msd.VOcorrid = voc.corrid and voc.VOid = vo.VOid
and vo.VOName = "cms"
group by FQAN
WITH ROLLUP""" % {  "begin" : DateToString(begin), "end" : DateToString(end) }

    return RunQueryAndSplit(select)

def GetSiteVOEfficiency(begin,end):
    schema = gDBSchema[mainDB] + ".";
    #select = "select SiteName, lcase(VO.VOName), sum(Njobs),sum(WallDuration),round(sum(CpuUserDuration+CpuSystemDuration)/sum(WallDuration),2) as CpuToWall, Cores, round(sum(CpuUserDuration+CpuSystemDuration)/(sum(WallDuration)*Cores),2)*100 as eff from " + schema + "MasterSummaryData MSD, " + schema + "Site, " + schema + "Probe, VONameCorrection VC, VO where VO.VOName != \"Unknown\" and VO.VOName != \"other\" and VO.VOName != \"other EGEE\" and Probe.siteid = Site.siteid and MSD.ProbeName = Probe.probename and MSD.VOcorrid = VC.corrid and VC.VOid = VO.VOid and EndTime >= \"" + DateToString(begin) + "\" and EndTime < \"" + DateToString(end) + "\" group by Site.SiteName, lcase(VO.VOName),Cores"
    select = "select SiteName, lcase(VO.VOName), sum(Njobs),sum(WallDuration),round(sum(CpuUserDuration+CpuSystemDuration)/sum(WallDuration),2) as CpuToWall, Cores, round(sum(CpuUserDuration+CpuSystemDuration)/(sum(WallDuration)*Cores),2)*100 as eff from " + schema + "MasterSummaryData MSD, " + schema + "Site, " + schema + "Probe, VONameCorrection VC, VO where VO.VOName != \"Unknown\" and VO.VOName != \"other\" and VO.VOName != \"other EGEE\" and Probe.siteid = Site.siteid and MSD.ProbeName = Probe.probename and MSD.VOcorrid = VC.corrid and VC.VOid = VO.VOid and EndTime >= \"" + DateToString(begin) + "\" and EndTime < \"" + DateToString(end) + "\" group by Site.SiteName, lcase(VO.VOName),Cores order by Site.SiteName, lcase(VO.VOName), Cores"
    return RunQueryAndSplit(select);    

def GetVOEfficiency(begin,end):
    schema = gDBSchema[mainDB] + ".";
    #select = "select lcase(VO.VOName), sum(Njobs),sum(WallDuration),round(sum(CpuUserDuration+CpuSystemDuration)/sum(WallDuration),2) as CpuToWall, Cores, round(sum(CpuUserDuration+CpuSystemDuration)/(sum(WallDuration)*Cores),2)*100 as eff from " + schema + "MasterSummaryData MSD, " + schema + "Site, " + schema + "Probe, VONameCorrection VC, VO where VO.VOName != \"Unknown\" and VO.VOName != \"other\" and VO.VOName != \"other EGEE\" and Probe.siteid = Site.siteid and MSD.ProbeName = Probe.probename and MSD.VOcorrid = VC.corrid and VC.VOid = VO.VOid and EndTime >= \"" + DateToString(begin) + "\" and EndTime < \"" + DateToString(end) + "\" group by lcase(VO.VOName),Cores"
    select = "select lcase(VO.VOName), sum(Njobs),sum(WallDuration),round(sum(CpuUserDuration+CpuSystemDuration)/sum(WallDuration),2) as CpuToWall, Cores, round(sum(CpuUserDuration+CpuSystemDuration)/(sum(WallDuration)*Cores),2)*100 as eff from " + schema + "MasterSummaryData MSD, " + schema + "Site, " + schema + "Probe, VONameCorrection VC, VO where VO.VOName != \"Unknown\" and VO.VOName != \"other\" and VO.VOName != \"other EGEE\" and Probe.siteid = Site.siteid and MSD.ProbeName = Probe.probename and MSD.VOcorrid = VC.corrid and VC.VOid = VO.VOid and EndTime >= \"" + DateToString(begin) + "\" and EndTime < \"" + DateToString(end) + "\" group by lcase(VO.VOName),Cores order by lcase(VO.VOName),Cores"
    return RunQueryAndSplit(select);    

def PrintHeader():
        print " VO        | Wall Hours | Norm Wall | CPU Hours |  Norm CPU | Wall Load| Norm Wall| CPU Load | Norm CPU |"

class Record:
        voname = ""
        cputime = 0.0
        normcpu = 0.0
        walltime= 0.0
        normwall= 0.0
        cpufactor     = 0.0
        normpcufactor = 0.0
        wallfactor     = 0.0
        normwallfactor = 0.0
        
        def __init__(self,vals):
                factor = 3600  # Convert number of seconds to number of hours

                self.voname = vals[0]
                self.cputime = float(vals[1]) / factor
                self.normcpu = float(vals[2]) / factor
                self.walltime= float(vals[3]) / factor
                self.normwall= float(vals[4]) / factor

        def Norm(self,ncpu,days,benchtotal):
                fulltime = ncpu * days * 24 # number of Cpu hours
                fullnormtime = benchtotal * days * 24  # number of Cpu hours of PIV 4.0G equiv
                self.cpufactor = 100 * self.cputime / fulltime
                self.normcpufactor = 100 * self.normcpu / fullnormtime
                self.wallfactor = 100 * self.walltime / fulltime
                self.normwallfactor = 100 * self.normwall / fullnormtime
        
        def Print(self):
                format = "%-10s |%11.0f |%10.0f |%10.0f |%10.0f |  %6.1f%% |  %6.1f%% |  %6.1f%% |  %6.1f%% | %f" 
                factor = 0.0
                if self.cputime != 0 :
                        factor = self.walltime / self.cputime
                values = (self.voname,self.walltime,self.normwall,self.cputime,self.normcpu,self.wallfactor,self.normwallfactor,self.cpufactor,self.normcpufactor, factor)
                print format % values

        def Add(self,other):
                self.cputime  += other.cputime
                self.normcpu  += other.normcpu
                self.walltime += other.walltime
                self.normwall += other.normwall

def IsUser(voname):
        return  voname != "Utility"           

def Weekly():
        global gBegin,gEnd, gProbename
        gProbename = None # "psacct:cmswc1.fnal.gov"

        print "Weekly"
        (ncpu,benchtotal) = NumberOfCpus()
        days = (gEnd - gBegin).days
        
        lines = WeeklyData();
        PrintHeader()
        total = Record(("Total",0,0,0,0))
        usertotal = Record(("User Total",0,0,0,0))
        for i in range (0,len(lines)):
                val = lines[i].split('\t')
                r = Record(val)
                r.Norm(ncpu,days,benchtotal)
                r.Print()
                total.Add(r)
                if IsUser(r.voname):
                        usertotal.Add(r)
        print
        usertotal.Norm(ncpu,days,benchtotal)
        usertotal.Print()
        total.Norm(ncpu,days,benchtotal)
        total.Print()
        print
        print "Other : "
        print "# of CPUS : ",ncpu
        print "Date : " + gBegin.strftime("%m/%Y") + " (" + str(days )+ " days)"

def FromCondor():
        print "From Condor"
        global gProbename,gBegin,gEnd
        # gProbename = "cmsosgce.fnal.gov"
        
        #(ncpu,benchtotal) = NumberOfCpus()
        ncpu = 2182
        benchtotal = ncpu
        days = (gEnd - gBegin).days
        
        lines = CondorData();
        PrintHeader()
        total = Record(("Total",0,0,0,0))
        usertotal = Record(("User Total",0,0,0,0))
        for i in range (0,len(lines)):
                val = lines[i].split('\t')
                r = Record(val)
                r.Norm(ncpu,days,benchtotal)
                r.Print()
                total.Add(r)
                if IsUser(r.voname):
                        usertotal.Add(r)
        print
        usertotal.Norm(ncpu,days,benchtotal)
        usertotal.Print()
        total.Norm(ncpu,days,benchtotal)
        total.Print()
        print
        print "Other : "
        print "# of CPUS : ",ncpu
        print "Date : " + gBegin.strftime("%m/%Y") + " (" + str(days )+ " days)"

class GenericConf: # top parent class. Just for sake of adding a single common attribute to all other Conf classes (triggered by Brian's request of having the delta columns adjacent to the data columns in the DataTransferReport instead of all bunched to the right) 
    delta_column_location = "right"
    factor = 3600 # conversion factor for time from hours to seconds for most reports (except data transfere report where it will be set to 1)

class DailySiteJobStatusConf(GenericConf):
    title = "Summary of the job exit status (midnight to midnight UTC) for %s\nincluding all jobs that finished in that time period.\n\nFor Condor the value used is taken from 'ExitCode' and NOT from 'Exit Status'\n\nWall Success: Wall clock hours of successfully completed jobs\nWall Failed: Wall clock hours of unsuccessfully completed jobs\nWall Success Rate: Wall Success / (Wall Success + Wall Failed)\nSuccess: number of successfully completed jobs\nFailed: Number of unsuccessfully completed jobs\nSuccess Rate: number of successfull jobs / total number of jobs\n"
    headline = "\nFor all jobs finished on %s (UTC)\n"
    headers = ("","Site","Wall Succ Rate","Wall Success","Wall Failed","Success Rate","Success","Failed")
    num_header = 1
    formats = {}
    lines = {}
    totalheaders = ["All sites"]
    CondorSpecial = False
    GroupBy = "Site.SiteName"
    Both = False
    ExtraSelect = ""
    VOName = ""

    def __init__(self, header = False, CondorSpecial = True, groupby = "Site", VOName = ""):
           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %3s | %-22s | %14s | %12s | %11s | %12s | %10s | %10s "
           self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td align=right>%s</td><td align=right>%s</td><td align=right>%s</td><td align=right>%s</td><td align=right>%s</td><td align=right>%s</td></tr>"

           self.lines["csv"] = ""
           self.lines["text"] = "--------------------------------------------------------------------------------------------------------------------"
           self.lines["html"] = ""

           if (not header) :  self.title = ""
           self.CondorSpecial = CondorSpecial
           self.VOName = VOName

           if (groupby == "VO"):
               self.GroupBy = "VO.VOName"
               self.headers = ("","VO","Wall Succ Rate","Wall Success","Wall Failed","Success Rate","Success","Failed")
               self.totalheaders = ["All VOs"]

           elif (groupby == "Both"):
               self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
               self.formats["text"] = "| %3s | %-22s | %-22s | %14s | %12s | %11s | %12s | %10s | %10s "
               self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td>%s</td><td align=right>%s</td><td align=right>%s</td><td align=right>%s</td><td align=right>%s</td><td align=right>%s</td><td align=right>%s</td></tr>"

               self.lines["text"] = "---------------------------------------------------------------------------------------------------------------------------------------------"
               self.GroupBy = "Site.SiteName,VO.VOName"
               self.headers = ("","Site","VO","Wall Succ Rate","Wall Success","Wall Failed","Success Rate","Success","Failed")
               self.totalheaders = ["All Sites","All VOs"]
               self.Both = True

           elif (groupby == "ForVO"):
               self.GroupBy = "Site.SiteName"
               self.ExtraSelect = " and VO.VOName = " + " \"" + self.VOName + "\" "
               

    def Sorting(self, x,y):
        if (self.Both):
            xval = (x[1])[0] + (x[1])[1]
            yval = (y[1])[0] + (y[1])[1] 
            return cmp(xval,yval)
        else:
            if ( ( (x[1])[4] + (x[1])[5] ) > 0) :
               xval = (x[1])[4]*100 / ( (x[1])[4] + (x[1])[5] )
            else:
               xval = (x[1])[2]*100 / ( (x[1])[2] + (x[1])[3] )
            if (  ( (y[1])[4] + (y[1])[5] ) >0):
               yval = (y[1])[4]*100 / ( (y[1])[4] + (y[1])[5] )
            else:
               yval = (y[1])[2]*100 / ( (y[1])[2] + (y[1])[3] )
            return cmp(yval,xval)
        

    def GetData(self,start,end):
       LogToFile("#######################\n## DailySiteJobStatusConf")
       return DailySiteJobStatusSummary(start,end,what=self.GroupBy,selection=self.ExtraSelect)
    
  
class DailySiteReportConf(GenericConf):
        title = "&nbsp;<p>OSG usage summary (midnight to midnight UTC) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nThe number of jobs counted here includes only the jobs directly seen by batch system and does not include the request sent directly to a pilot job.\nThe Wall Duration includes the total duration of the the pilot jobs.\nDeltas are the differences with the previous day.\n(nr) after a VO name indicates that the VO is not registered with OSG.\n"
        headline = "<p>For all jobs finished on %s (UTC)<p>"
        headers = ("","Site","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        num_header = 1
        formats = {}
        lines = {}
        totalheaders = ["All sites"]

        def __init__(self, header = False):

           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %3s | %-22s | %9s | %13s | %10s | %14s"
           self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td></tr>"

           self.lines["csv"] = ""
           self.lines["text"] = "---------------------------------------------------------------------------------------"
           self.lines["html"] = ""

           if (not header) :  self.title = ""


        def GetData(self,start,end):
           LogToFile("#######################\n## DailySiteReportConf")
           return DailySiteData(start,end)      

class DailyVOReportConf(GenericConf):
        title = "&nbsp;<p>OSG usage summary (midnight to midnight UTC) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nThe number of jobs counted here includes only the jobs directly seen by batch system and does not include the request sent directly to a pilot job.\nThe Wall Duration includes the total duration of the the pilot jobs.\nDeltas are the differences with the previous day.\n(nr) after a VO name indicates that the VO is not registered with OSG.\n"
        headline = "<p>For all jobs finished on %s (UTC)<p>"
        headers = ("","VO","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        num_header = 1
        formats = {}
        lines = {}
        totalheaders = ["All VOs"]

        def __init__(self, header = False):
           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %3s | %-22s | %9s | %13s | %10s | %14s"
           self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td></tr>"

           self.lines["csv"] = ""
           self.lines["text"] = "-----------------------------------------------------------------------------------"
           self.lines["html"] = ""

           if (not header) :  self.title = ""

        def GetData(self,start,end):
           LogToFile("#######################\n## DailyVOReportConf")
           return UpdateVOName( DailyVOData(start,end), 0 ,start, end)

class DailySiteVOReportConf(GenericConf):
        title = "&nbsp;<p>OSG usage summary (midnight to midnight UTC) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nThe number of jobs counted here includes only the jobs directly seen by batch system and does not include the request sent directly to a pilot job.\nThe Wall Duration includes the total duration of the the pilot jobs.\nDeltas are the differences with the previous day.\n"
        headline = "<p>For all jobs finished on %s (UTC)<p>"
        headers = ("","Site","VO","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        num_header = 2
        formats = {}
        lines = {}
        select = "=="
        totalheaders = ["All sites", "All VOs"]
        
        def __init__(self, header = False):
           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %3s | %-22s | %-14s | %9s | %13s | %10s | %14s"
           self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td></tr>"

           self.lines["csv"] = ""
           self.lines["text"] = "--------------------------------------------------------------------------------------------------------"
           self.lines["html"] = ""

           if (not header) :  self.title = ""

        def GetData(self,start,end):
           LogToFile("#######################\n## DailySiteVOReportConf")
           return UpdateVOName(DailySiteVOData(start,end),1,start, end)  

class DailyVOSiteReportConf(GenericConf):
        title = "&nbsp;<p>OSG usage summary (midnight to midnight UTC) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nThe number of jobs counted here includes only the jobs directly seen by batch system and does not include the request sent directly to a pilot job.\nThe Wall Duration includes the total duration of the the pilot jobs.\nDeltas are the differences with the previous day.\n(nr) after a VO name indicates that the VO is not registered with OSG.\n"
        headline = "<p>For all jobs finished on %s (UTC)<p>"
        headers = ("","VO","Site","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        num_header = 2
        formats = {}
        lines = {}
        select = "=="
        totalheaders = ["All VOs","All sites"]
        
        def __init__(self, header = False):
           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %3s | %-22s | %-22s | %9s | %13s | %10s | %14s"
           self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td></tr>"

           self.lines["csv"] = ""
           self.lines["text"] = "--------------------------------------------------------------------------------------------------------"
           self.lines["html"] = ""

           if (not header) :  self.title = ""

        def GetData(self,start,end):
           LogToFile("#######################\n## DailyVOSiteReportConf")
           return UpdateVOName(DailyVOSiteData(start,end),0,start, end)   

class DailySiteVOReportFromDailyConf(GenericConf):
        title = "OSG usage summary (midnight to midnight central time) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nDeltas are the differences with the previous day.\nIf the number of jobs stated for a site is always 1\nthen this number is actually the number of summary records sent.\n(nr) after a VO name indicates that the VO is not registered with OSG.\n"
        headline = "For all jobs finished on %s (Central Time)"
        headers = ("Site","VO","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        num_header = 2
        formats = {}
        lines = {}
        select = "=="
        count = "sum(NJobs)"
        totalheaders = ["All sites","All VOs"]
        
        def __init__(self, fromGratia, header = False):
           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = " | %-22s | %-9s | %9s | %13s | %10s | %14s"
           self.lines["csv"] = ""
           self.lines["text"] = "------------------------------------------------------------------------------------------------"

           if (fromGratia) :
               self.select = "="
               self.count = "sum(NJobs)"
           else:
               self.select = "!="
               
           if (not header) :  self.title = ""

        def GetData(self,start,end):
           LogToFile("#######################\n## DailySiteVOReportFromDailyConf")
           return DailySiteVODataFromDaily(start,end,self.select,self.count)

class DailyVOSiteReportFromDailyConf(GenericConf):
        title = "OSG usage summary (midnight to midnight central time) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nDeltas are the differences with the previous day.\nIf the number of jobs stated for a site is always 1\nthen this number is actually the number of summary records sent.\n"
        headline = "For all jobs finished on %s (Central Time)"
        headers = ("VO","Site","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        num_header = 2
        formats = {}
        lines = {}
        select = "=="
        count = "sum(NJobs)"
        totalheaders = ["All sites","All VOs"]

        def __init__(self, fromGratia, header = False):
           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = " | %-9s | %-22s | %9s | %13s | %10s | %14s"
           self.lines["csv"] = ""
           self.lines["text"] = "------------------------------------------------------------------------------------------------"

           if (fromGratia) :
               self.select = "="
               self.count = "sum(NJobs)"
           else:
               self.select = "!="
               
           if (not header) :  self.title = ""

        def GetData(self,start,end):
           LogToFile("#######################\n## DailyVOSiteReportFromDailyConf")
           return DailyVOSiteDataFromDaily(start,end,self.select,self.count)

def sortedDictValues(adict):
    items = adict.items()
    items.sort()
    return [(key,value) for key, value in items]

def sortedDictValuesFunc(adict,compare):
    items = adict.items()
    items.sort( compare )
    return [(key,value) for key, value in items]

def GenericDailyStatus(what, when=datetime.date.today(), output = "text"):
        old_stdout = sys.stdout
        sys.stdout = stdout = StringIO()
        ret = ""

        if not when:
            when = datetime.date.today()

        factor = 3600  # Convert number of seconds to number of hours

        if(output == "html"):
            print "<table><tr><td>"

        if (output != "None") :
            if (what.title != "") :
                if(output == "html"):
                    what.title = what.title.replace("\n","<br>");
                print what.title % ( DateToString(when,False) )
            if (what.headline != "") :
                if(output == "html"):
                    what.headline = what.headline.replace("\n","<br>");
                print what.headline % (DateToString(when,False))
            if(output == "html"):
                print "&nbsp<p>"
                print "</td></tr></table>"
                print "<table bgcolor=black cellspacing=1 cellpadding=5>"
            print what.lines[output]
            print what.formats[output] % what.headers
            print what.lines[output]

        # Get the correct day information
        start = when
        end = start + datetime.timedelta(days=1)
        lines = what.GetData(start,end)

        result = []
        index = 0

        all_values = {}
        sum_values = {}
        
        for i in range (0,len(lines)):
                if (lines[i]=="") : continue

                val = lines[i].split('\t')

                if (val[2] == "count(*)"):
                    continue

                site = val[0]
                if (what.Both):
                   vo = val[1]
                   status = val[2]
                   count = int(val[3])
                   if val[4] == "NULL":
                      wall = 0
                   else:
                      wall = float( val[4] ) / factor
                else:
                   vo = ""
                   status = val[1]
                   count = int(val[2])
                   if val[3] == "NULL":
                      wall = 0
                   else:
                      wall = float( val[3] ) / factor

                key = site + ";" + vo + " has status " + status

                if all_values.has_key(key):
                    (a,b,c,oldvalue,oldwall) = all_values[key]
                    oldvalue = oldvalue + count
                    oldwall = oldwall + wall
                    all_values[key] = (a,b,c,oldvalue,oldwall)
                else:
                    all_values[key] = (site,vo,status,count,wall)

                key = site + ";" + vo
                (tmp, tmp2, success, failed, wsuccess, wfailed) = ("","",0,0, 0.0, 0.0)
                if sum_values.has_key(key) :
                    (tmp, tmp2, success, failed, wsuccess, wfailed ) = sum_values[key]
                if status == "0" :
                    success = success + count
                    wsuccess = wsuccess + wall
                else:
                    failed = failed + count
                    wfailed = wfailed + wall
                sum_values[key] = (site, vo, success, failed, wsuccess, wfailed)
                
##        for key,(site,status,count) in sortedDictValues(all_values):
##            index = index + 1;
##            values = (site,status,count)
##            if (output != "None") :
##                     print "%3d " %(index), what.formats[output] % values
##            result.append(values)

        totaljobs = 0
        totalsuccess = 0
        totalfailed = 0
        totalws = 0.0
        totalwf = 0.0
        for key,(site,vo,success,failed,wsuccess,wfailed) in sortedDictValuesFunc(sum_values,what.Sorting):
            index = index + 1;
            total = success+failed
            wtotal = wsuccess+wfailed
            rate = (success*100/total)
            if (wtotal > 0): 
               wrate = (wsuccess*100/wtotal)
            else:
               wrate = rate
            if (wrate > 90): wrate = wrate - 0.5
            if (what.Both):
               values = (index,site,vo,str(niceNum(wrate))+" %",niceNum(wsuccess),niceNum(wfailed),str(rate)+" %",success,failed)
            else: 
               values = (index,site,str(niceNum(wrate))+" %",niceNum(wsuccess),niceNum(wfailed),str(rate)+" %",success,failed)
            totaljobs = totaljobs + total
            totalsuccess = totalsuccess + success
            totalfailed = totalfailed + failed
            totalws = totalws + wsuccess
            totalwf = totalwf + wfailed
            if (output != "None") :
                     print what.formats[output] % values
            result.append(values)

        if (output != "None") :
                print what.lines[output]
                if ( (totalws+totalwf) > 0 ):
                   totalwrate = niceNum( 100*totalws / (totalws+totalwf))
                elif (totaljobs > 0) :
                   totalwrate = totalsuccess*100/totaljobs
                else:
                   totalwrate = 0
                if (what.Both):
                    tsrate = 0
                    if ( totaljobs > 0) : tsrate = totalsuccess*100/totaljobs
                    print what.formats[output] % ("", what.totalheaders[0], what.totalheaders[1], str(totalwrate) + " %", niceNum(totalws),niceNum(totalwf), str(tsrate) + " %", niceNum(totalsuccess),niceNum(totalfailed))
                else:
                    tsrate = 0
                    if ( totaljobs > 0) : tsrate = totalsuccess*100/totaljobs
                    print what.formats[output] % ("", what.totalheaders[0], str(totalwrate) + " %", niceNum(totalws),niceNum(totalwf), str(tsrate) + " %",niceNum(totalsuccess),niceNum(totalfailed))
                print what.lines[output]

        if(output == "html"):
            print "</table>"
        sys.stdout = old_stdout
        ret = stdout.getvalue()

        return ret
            
        
def GenericDaily(what, when=datetime.date.today(), output = "text"):
        old_stdout = sys.stdout
        sys.stdout = stdout = StringIO()
        ret = ""

        factor = 3600  # Convert number of seconds to number of hours

        if not when:
            when=datetime.date.today()

        if(output == "html"):
            print "<table><tr><td>"

        if output != "None":
            if what.title:
                print what.title % ( DateToString(when,False) )
            if what.headline:
                print what.headline % (DateToString(when,False))
            if(output == "html"):
                print "&nbsp<p>"
                print "</td></tr></table>"
                print "<table bgcolor=black cellspacing=1 cellpadding=5>"
            print what.lines[output]
            #print "    ", what.formats[output] % what.headers
            print what.formats[output] % what.headers
            print what.lines[output]

        # First get the previous day's information
        totalwall = 0
        totaljobs = 0
        oldValues = {}
        result = []

        start = when  + datetime.timedelta(days=-1)
        end = start + datetime.timedelta(days=1)
        lines = what.GetData(start,end)
        for i in range (0,len(lines)):
                val = lines[i].split('\t')
                offset = 0
                site = val[0]
                key = site
                vo = ""
                if (len(val)==4) :
                        vo = val[1]
                        offset = 1
                        num_header = 2
                        key = site + " " + vo
                njobs= int( val[offset+1] )
                if val[offset+2] == "NULL":
                   wall = 0
                else:
                   wall = float( val[offset+2] ) / factor
                totalwall = totalwall + wall
                totaljobs = totaljobs + njobs                
                oldValues[key] = (njobs,wall,site,vo)
        oldValues["total"] = (totaljobs, totalwall, "total","")

        # Then get the previous week's information
 #       totalwall = 0
 #       totaljobs = 0
 #       start = when + datetime.timedelta(days=-8)
 #       end = when
 #       weekValues = {}
 #       lines = what.GetData(start,end)
 #       for i in range (0,len(lines)):
 #               val = lines[i].split('\t')
 #               offset = 0
 #               site = val[0]
 #               key = site
 #               vo = ""
 #               if (len(val)==4) :
 #                       vo = val[1]
 #                       offset = 1
 #                       num_header = 2
 #                       key = site + " " + vo
 #               njobs= int( val[offset+1] )
 #               if val[offset+2] == "NULL":
 #                  wall = 0
 #               else:
 #                  wall = float( val[offset+2] ) / factor
 #               totalwall = totalwall + wall
 #               totaljobs = totaljobs + njobs                
 #               weekValues[key] = (njobs,wall,site,vo)
 #       weekValues["total"] = (totaljobs, totalwall, "total","")
        

        # Then getting the correct day's information and print it
        totalwall = 0
        totaljobs = 0
        start = when
        end = start + datetime.timedelta(days=1)
        lines = what.GetData(start,end)
        num_header = what.num_header;
        index = 0
        printValues = {}
        
        for i in range (0,len(lines)):
                val = lines[i].split('\t')
                if ( len(val) < 2 ) :
                   continue;
                site = val[0]
                key = site
                offset = 0
                if (len(val)==4) :
                        vo = val[1]
                        offset = 1
                        num_header = 2
                        key = site + " " + vo
                (oldnjobs,oldwall) = (0,0)
                if oldValues.has_key(key):
                        (oldnjobs,oldwall,s,v) = oldValues[key]
                        del oldValues[key]
                njobs= int( val[offset+1] )
                if val[offset+2] == "NULL":
                   wall = 0
                else:
                   wall = float( val[offset+2] ) / factor
                totalwall = totalwall + wall
                totaljobs = totaljobs + njobs
                printValues[key] = (njobs,wall,oldnjobs,oldwall,site,vo)

        for key,(oldnjobs,oldwall,site,vo) in oldValues.iteritems():            
            if (key != "total") :
                printValues[key] = (0,0,oldnjobs,oldwall,site,vo)

        for key,(njobs,wall,oldnjobs,oldwall,site,vo) in sortedDictValues(printValues):
            index = index + 1;
            if (num_header == 2) :
                     values = (index, site,vo,niceNum(njobs), niceNum(wall),niceNum(njobs-oldnjobs),niceNum(wall-oldwall))
            else:
                     values = (index, site,niceNum(njobs), niceNum(wall),niceNum(njobs-oldnjobs),niceNum(wall-oldwall))
            if (output != "None") :
                     print what.formats[output] % values
            result.append(values)       
                
        (oldnjobs,oldwall,s,v) = oldValues["total"]
        if (output != "None") :
                print what.lines[output]
                if (num_header == 0) :
                    print 
                elif (num_header == 2) :
                    print what.formats[output] % ("",what.totalheaders[0], what.totalheaders[1], niceNum(totaljobs), niceNum(totalwall), niceNum(totaljobs-oldnjobs), niceNum(totalwall-oldwall))
                else:
                    print what.formats[output] % ("",what.totalheaders[0], niceNum(totaljobs), niceNum(totalwall), niceNum(totaljobs-oldnjobs), niceNum(totalwall-oldwall))
                print what.lines[output]
        if(output == "html"):
            print "</table>"
        sys.stdout = old_stdout
        ret = stdout.getvalue()
        if (output=="None"):
           return result
        else:
           return ret
        
def DailySiteReport(when = datetime.date.today(), output = "text", header = True):
        return GenericDaily( DailySiteReportConf(header), when, output)

def DailyVOReport(when = datetime.date.today(), output = "text", header = True):
        return GenericDaily( DailyVOReportConf(header), when, output)
 
def DailySiteVOReport(when = datetime.date.today(), output = "text", header = True):
        return GenericDaily( DailySiteVOReportConf(header), when, output)

def DailyVOSiteReport(when = datetime.date.today(), output = "text", header = True):
        return GenericDaily( DailyVOSiteReportConf(header), when, output)

def DateTimeToString(input):
    return input.strftime("%Y-%m-%d %H:%M:%S");

def VONameCorrectionJoin(table = "sub"):
    return """(cast("""+table+""".VOName as char charset binary) =
cast(VONameCorrection.VOName as char charset binary))
and ((cast("""+table+""".ReportableVOName as char charset binary) =
cast(VONameCorrection.ReportableVOName as char charset binary))
or (isnull("""+table+""".ReportableVOName) and
isnull(VONameCorrection.ReportableVOName)))
and (VONameCorrection.VOid = VO.VOid)
and ("""+table+""".VOName = VONameCorrection.VOName)"""

def VONameCorrectionSummaryJoin(table = "sub"):
    return table + ".VOcorrid = VONameCorrection.corrid  and (VONameCorrection.VOid = VO.VOid) "

def RangeVOData(begin, end, with_panda = False):
    schema = gDBSchema[mainDB]
    select = """\
select J.VOName, sum(J.NJobs), sum(J.WallDuration)
  from """ + schema + """.VOProbeSummary J
  where VOName != \"Unknown\" and 
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\" and
    J.ResourceType=\"Batch\" and
    J.ProbeName not like \"psacct:%\"
    group by J.VOName
    order by VOName;"""
    if with_panda:
        panda_select = """\
select J.VOName, sum(J.NJobs), sum(J.WallDuration)
  from """ + gDBSchema[dailyDB] + """.JobUsageRecord_Report J
  where 
    J.ProbeName != \"daily:goc\" and
    J.SiteName not in (select GT.SiteName from """ + gDBSchema[mainDB] + """.Site GT) and
    J.EndTime >= \"""" + DateTimeToString(begin) + """\" and
    J.EndTime < \"""" + DateTimeToString(end) + """\"
    group by J.VOName;"""
        return RunQueryAndSplit(select) + RunQueryAndSplit(panda_select)
    else:
        return RunQueryAndSplit(select) 

def getLocalSiteStr(localSites):
    if gGrid != 'local':
        return
    siteStr = '' 
    if localSites == None:
        print "Grid specified as local, but no local active_sites were found in the configuration file. Plese check. Exiting..."
        sys.exit(1)
    else:
        for site in localSites:
            siteStr+="T.SiteName=\"" + site + "\" or "
        if siteStr!='':
            siteStr=re.compile("(.*) or $",re.IGNORECASE).search(siteStr).group(1)
            siteStr = " (" + siteStr + ") and "
    return siteStr

def DataTransferData(begin, end, with_panda = False):
    siteStr=""
    if gGrid == 'local': 
        siteStr = getLocalSiteStr(GetListOfOSGSEs())
    global gMySQLConnectString,gDBCurrent
    schema = gDBSchema[transferDB]
    gDBCurrent = transferDB
    keepConnectionValue = gMySQLConnectString
    gMySQLConnectString = gMySQLTransferConnectString
    select = "select T.SiteName, M.Protocol, sum(M.Njobs), sum(M.TransferSize * Multiplier) from " + schema + ".MasterTransferSummary M, " + schema + ".Probe P, " + schema + ".Site T, " + schema + ".SizeUnits su where " + siteStr + " M.StorageUnit = su.Unit and P.siteid = T.siteid and M.ProbeName = P.Probename and StartTime >= \"" + DateTimeToString(begin) + "\" and StartTime < \"" + DateTimeToString(end) + "\" and M.ProbeName not like \"psacct:%\" group by P.siteid, Protocol"
    result = RunQueryAndSplit(select)
    gMySQLConnectString = keepConnectionValue 
    return result

# HK TransferVO Apr 26 2013 BEGIN 
def DataTransferDataTrVO(begin, end, with_panda = False):
    siteStr=""
    if gGrid == 'local': 
        siteStr = getLocalSiteStr(GetListOfOSGSEs())
    global gMySQLConnectString,gDBCurrent
    schema = gDBSchema[transferDB]
    gDBCurrent = transferDB
    keepConnectionValue = gMySQLConnectString
    gMySQLConnectString = gMySQLTransferConnectString

#    select = "select T.SiteName, M.Protocol, sum(M.Njobs), sum(M.TransferSize * Multiplier) from " + schema + ".MasterTransferSummary M, " + schema + ".Probe P, " + schema + ".Site T, " + schema + ".SizeUnits su where " + siteStr + " M.StorageUnit = su.Unit and P.siteid = T.siteid and M.ProbeName = P.Probename and StartTime >= \"" + DateTimeToString(begin) + "\" and StartTime < \"" + DateTimeToString(end) + "\" and M.ProbeName not like \"psacct:%\" group by P.siteid, Protocol"

# HK TransferVO Apr 26 2013
    select = "select T.SiteName, M.Protocol, V.ReportableVOName, sum(M.Njobs), sum(M.TransferSize * Multiplier) from " + schema + ".MasterTransferSummary M, " + schema + ".Probe P, " + schema + ".Site T, " + schema + ".SizeUnits su, " + schema + ".VONameCorrection V where " + siteStr + " M.StorageUnit = su.Unit and P.siteid = T.siteid and M.ProbeName = P.Probename and StartTime >= \"" + DateTimeToString(begin) + "\" and StartTime < \"" + DateTimeToString(end) + "\" and M.ProbeName not like \"psacct:%\" and V.corrid=M.VOcorrid group by M.VOcorrid, P.siteid, Protocol"

    result = RunQueryAndSplit(select)
    gMySQLConnectString = keepConnectionValue 
    return result
# HK TransferVO Apr 26 2013 END



def RangeSiteData(begin, end, with_panda = False):
    schema = gDBSchema[mainDB]
    select = """\
select T.SiteName, sum(J.NJobs), sum(J.WallDuration)
  from """ + schema + ".Site T, " + schema + ".Probe P, " + schema + """.VOProbeSummary J
  where VOName != \"Unknown\" and 
    P.siteid = T.siteid and
    J.ProbeName = P.probename and
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\" and
    J.ResourceType=\"Batch\" and
    J.ProbeName not like \"psacct:%\"
    group by P.siteid;"""
    if with_panda:
        panda_select = """\
select J.SiteName, sum(J.NJobs), sum(J.WallDuration)
  from """ + gDBSchema[dailyDB] + """.JobUsageRecord_Report J
  where VOName != \"Unknown\" and 
    J.ProbeName != \"daily:goc\" and
    J.SiteName not in (select GT.SiteName from """ + gDBSchema[mainDB] + """.Site GT) and
    J.EndTime >= \"""" + DateTimeToString(begin) + """\" and
    J.EndTime < \"""" + DateTimeToString(end) + """\" and
    J.ResourceType=\"Batch\" 
    group by J.SiteName;"""
        return RunQueryAndSplit(select) + RunQueryAndSplit(panda_select)
    else:
        return RunQueryAndSplit(select)

def RangeSiteVOData(begin, end, with_panda = False):
    LogToFile("#######################\n## def RangeSiteVOData Panda=%s" % with_panda)
    schema = gDBSchema[mainDB]
    select = """\
select T.SiteName, J.VOName, sum(NJobs), sum(J.WallDuration)
  from """ + schema + ".Site T, " + schema + ".Probe P, " + schema + """.VOProbeSummary J
  where VOName != \"Unknown\" and 
    P.siteid = T.siteid and
    J.ProbeName = P.probename and
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\" and
    J.ResourceType=\"Batch\" and
    J.ProbeName not like \"psacct:%\"
    group by T.SiteName,J.VOName
    order by T.SiteName,J.VOName;"""
    if with_panda:
        panda_select = """\
select J.SiteName, J.VOName, sum(J.NJobs), sum(J.WallDuration)
  from """ + gDBSchema[dailyDB] + """.JobUsageRecord_Report J
  where VOName != \"Unknown\" and 
    J.ProbeName != \"daily:goc\" and
    J.ReportedSiteName not in (select GT.SiteName from """ + gDBSchema[mainDB] + """.Site GT) and
    J.EndTime >= \"""" + DateTimeToString(begin) + """\" and
    J.EndTime < \"""" + DateTimeToString(end) + """\" and
    J.ResourceType=\"Batch\" 
    group by J.ReportedSiteName, J.VOName
    order by J.ReportedSiteName, J.VOName;"""
        return RunQueryAndSplit(select) + RunQueryAndSplit(panda_select)
    else:
        return RunQueryAndSplit(select)
    
def RangeVOSiteData(begin, end, with_panda = False):
    LogToFile("#######################\n## def RangeVOSiteData Panda=%s" % with_panda)
    schema = gDBSchema[mainDB]
    select = """\
select J.VOName, T.SiteName, sum(NJobs), sum(J.WallDuration)
  from """ + schema + ".Site T, " + schema + ".Probe P, " + schema + """.VOProbeSummary J
  where VOName != \"Unknown\" and 
    P.siteid = T.siteid and
    J.ProbeName = P.probename and
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\" and
    J.ResourceType=\"Batch\" and 
    J.ProbeName not like \"psacct:%\"
    group by T.SiteName,J.VOName
    order by J.VOName,T.SiteName;"""
    if with_panda:
        panda_select = """\
select J.VOName, J.SiteName, sum(J.NJobs), sum(J.WallDuration)
  from """ + gDBSchema[dailyDB] + """.JobUsageRecord_Report J
  where VOName != \"Unknown\" and 
    J.ProbeName != \"daily:goc\" and
    J.SiteName not in (select GT.SiteName from """ + gDBSchema[mainDB] + """.Site GT) and
    J.EndTime >= \"""" + DateTimeToString(begin) + """\" and
    J.EndTime < \"""" + DateTimeToString(end) + """\" and
    J.ResourceType=\"Batch\" 
    group by J.SiteName, J.VOName
    order by J.VOName, J.SiteName;"""
        return RunQueryAndSplit(select) + RunQueryAndSplit(panda_select)
    else:
        return RunQueryAndSplit(select)

def LongJobsData(begin, end, with_panda = False):
    LogToFile("#######################\n## def LongJobsData Panda=%s" % with_panda)
    schema = gDBSchema[mainDB]
    
    select = """
select SiteName, VO.VOName, sum(NJobs), avg(WallDuration)/3600.0/24.0, avg(Cpu*100/WallDuration),
Date(max(EndTime)) from (select dbid, NJobs, WallDuration,CpuUserDuration+CpuSystemDuration as
Cpu,VOName,ReportableVOName,EndTime from JobUsageRecord J
where
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\" and
    J.ResourceType=\"Batch\" and
    WallDuration > 3600*24*7
) as sub,
JobUsageRecord_Meta M, VONameCorrection, VO, Probe, Site
where sub.dbid = M.dbid
and M.ProbeName = Probe.ProbeName and Probe.siteid = Site.siteid
and (cast(sub.VOName as char charset binary) =
cast(VONameCorrection.VOName as char charset binary))
and ((cast(sub.ReportableVOName as char charset binary) =
cast(VONameCorrection.ReportableVOName as char charset binary))
or (isnull(sub.ReportableVOName) and
isnull(VONameCorrection.ReportableVOName)))
and (VONameCorrection.VOid = VO.VOid)
and (sub.VOName = VONameCorrection.VOName)
and SiteName != \"OU_OSCER_ATLAS\"
group by VO.VOName, SiteName
order by VO.VOName, SiteName"""

    return RunQueryAndSplit(select)

def UserReportData(begin, end, with_panda = False, selection = ""):
    LogToFile("#######################\n## def UserReportData Panda=%s" % with_panda)
    select = """
SELECT VOName, CommonName, sum(NJobs), sum(WallDuration) as Wall
from VOProbeSummary U where
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\"
    and CommonName != \"Unknown\"
    """ + selection + """
    group by CommonName, VOName
"""
    return RunQueryAndSplit(select)
    
def UserSiteReportData(begin, end, with_panda = False, selection = ""):
    LogToFile("#######################\n## def UserSiteReportData Panda=%s" % with_panda)
    select = """
SELECT CommonName, VOName, SiteName, sum(NJobs), sum(WallDuration) as Wall
from VOProbeSummary U, Probe P, Site S where
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\" and
    U.ProbeName = P.ProbeName and P.siteid = S.siteid 
    and CommonName != \"Unknown\"
    """ + selection + """
    group by CommonName, SiteName, VOName
"""
    return RunQueryAndSplit(select)

class RangeVOReportConf(GenericConf):
    title = """\
OSG usage summary for  %s - %s (midnight UTC - midnight UTC)
including all jobs that finished in that time period.
Wall Duration is expressed in hours and rounded to the nearest hour. Wall
Duration is the duration between the instant the job started running
and the instant the job ended its execution.
Deltas are the differences with the previous period."""
    headline = "For all jobs finished between %s and %s (midnight UTC)"
    headers = ("","VO","# of Jobs","Wall Duration","Delta jobs","Delta duration")
    num_header = 1
    formats = {}
    lines = {}
    totalheaders = ["All VOs"]
    defaultSort = True

    def __init__(self, header = False, with_panda = False):
        self.formats["csv"] = ",%s,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %3s | %-18s | %9s | %13s | %10s | %14s"
        self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td></tr>"
        self.lines["csv"] = ""
        self.lines["html"] = ""
        self.lines["text"] = "-----------------------------------------------------------------------------------"
        self.with_panda = with_panda
        if (not header) :  self.title = ""

    def GetData(self,start,end):
        LogToFile("#######################\n## RangeVOReportConf")
        return UpdateVOName(RangeVOData(start, end, self.with_panda),0,start, end)

class RangeSiteReportConf(GenericConf):
    title = """\
OSG usage summary for  %s - %s (midnight UTC - midnight UTC)
including all jobs that finished in that time period.
Wall Duration is expressed in hours and rounded to the nearest hour. Wall
Duration is the duration between the instant the job started running
and the instant the job ended its execution.
Deltas are the differences with the previous period."""
    headline = "For all jobs finished between %s and %s (midnight, UTC)"
    headers = ("","Site","# of Jobs","Wall Duration","Delta jobs","Delta duration")
    num_header = 1
    formats = {}
    lines = {}
    totalheaders = ["All sites"]
    defaultSort = True

    def __init__(self, header = False, with_panda = False):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %3s | %-30s | %9s | %13s | %10s | %14s"
        self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td></tr>"
        self.lines["csv"] = ""
        self.lines["html"] = ""
        self.lines["text"] = "-----------------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda

    def GetData(self, start, end):
        LogToFile("#######################\n## RangeSiteReportConf")
        return RangeSiteData(start, end, self.with_panda)

class DataTransferReportConf(GenericConf):
    #title = """\
#OSG Data transfer summary for  %s - %s (midnight UTC - midnight UTC)
#including all data that transferred in that time period.
#Deltas are the differences with the previous period."""
    #headline = "For all data transferred between %s and %s (midnight, UTC)"
    title = ""
    headline = ""
    headers = ("","Site","Protocol","Num transfer","Delta transfer","Number of MiB","Delta MiB")
    num_header = 2
    factor = 1 # This is the factor to convert time from seconds to hours for other reports. But for data transfer report there is nothing to convert since we are just dealing with the transfer size (not time)
    delta_column_location = "adjacent"
    formats = {}
    lines = {}
    totalheaders = ["All sites","All Protocols"]
    defaultSort = True

    def __init__(self, header = False, with_panda = False):
        self.formats["csv"] = ",\%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %3s | %-30s | %-25s | %15s | %15s | %17s | %17s"
        self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td align=right>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td></tr>"

        self.lines["csv"] = ""
        self.lines["html"] = ""
        self.lines["text"] = "---------------------------------------------------------------------------------------------------------------------------------------------"

        if (not header) :  self.title = ""
        self.with_panda = with_panda

    def GetData(self, start, end):
        LogToFile("#######################\n## DataTransferReportConf")
        return DataTransferData(start, end, self.with_panda)

# HK TransferVO Apr 26 2013 BEGIN 
class DataTransferReportConfTrVO(GenericConf):
    #title = """\
#OSG Data transfer summary for  %s - %s (midnight UTC - midnight UTC)
#including all data that transferred in that time period.
#Deltas are the differences with the previous period."""
    #headline = "For all data transferred between %s and %s (midnight, UTC)"
    title = ""
    headline = ""
# HK TransferVO Apr 26 2013 
#    headers = ("","Site","Protocol","Num transfer","Delta transfer","Number of MiB","Delta MiB")
#    num_header = 2
    headers = ("","Site","Protocol","VO","Num transfer","Delta transfer","Number of MiB","Delta MiB")
    num_header = 3

    factor = 1 # This is the factor to convert time from seconds to hours for other reports. But for data transfer report there is nothing to convert since we are just dealing with the transfer size (not time)
    delta_column_location = "adjacent"
    formats = {}
    lines = {}
# HK TransferVO Apr 26 2013 
#    totalheaders = ["All sites","All Protocols"]
    totalheaders = ["All sites","All Protocols", "All VOs"]
    defaultSort = True

    def __init__(self, header = False, with_panda = False):
# HK TransferVO Apr 26 2013 
#        self.formats["csv"] = ",\%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
#        self.formats["text"] = "| %3s | %-30s | %-25s | %15s | %15s | %17s | %17s"
#        self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td align=right>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td></tr>"
        self.formats["csv"] = ",\%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["html"] = "<tr bgcolor=white> <td>%s</td><td>%s</td><td align=right>%s</td><td align=right>%s</td><td align=right>%s</td><td align=right>%s</td><td align=right>%s</td><td align=right> %s</td> </tr>"
        self.formats["text"] = "| %3s | %-30s | %-30s | %-25s | %15s | %15s | %17s | %17s"

        self.lines["csv"] = ""
        self.lines["html"] = ""
        self.lines["text"] = "---------------------------------------------------------------------------------------------------------------------------------------------"

        if (not header) :  self.title = ""
        self.with_panda = with_panda

    def GetData(self, start, end):
# HK TransferVO Apr 26 2013 
#        LogToFile("#######################\n## DataTransferReportConf")
#        return DataTransferData(start, end, self.with_panda)
        LogToFile("#######################\n## DataTransferReportConfTrVO")
        return DataTransferDataTrVO(start, end, self.with_panda)
# HK TransferVO Apr 26 2013 END

class RangeSiteVOReportConf(GenericConf):
    title = """\
OSG usage summary for  %s - %s (midnight UTC - midnight UTC)
including all jobs that finished in that time period.
Wall Duration is expressed in hours and rounded to the nearest hour. Wall
Duration is the duration between the instant the job started running
and the instant the job ended its execution.
Deltas are the differences with the previous period."""
    headline = "For all jobs finished between %s and %s (midnight UTC)"
    headers = ("","Site", "VO","# of Jobs","Wall Duration","Delta jobs","Delta duration")
    num_header = 2
    formats = {}
    lines = {}
    totalheaders = ["All sites","All VOs"]
    defaultSort = True

    def __init__(self, header = False, with_panda = False):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %3s | %-30s | %-18s | %9s | %13s | %10s | %14s"
        self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td align=right>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td></tr>"
        self.lines["csv"] = ""
        self.lines["html"] = ""
        self.lines["text"] = "--------------------------------------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda

    def GetData(self, start,end):
        LogToFile("#######################\n## RangeSiteVOReportConf")
        return UpdateVOName(RangeSiteVOData(start, end, self.with_panda),1,start, end)  

class RangeVOSiteReportConf(GenericConf):
    title = """\
OSG usage summary for  %s - %s (midnight UTC - midnight UTC)
including all jobs that finished in that time period.
Wall Duration is expressed in hours and rounded to the nearest hour. Wall
Duration is the duration between the instant the job started running
and the instant the job ended its execution.
Deltas are the differences with the previous period."""
    headline = "For all jobs finished between %s and %s (midnight UTC)"
    headers = ("","VO", "Site","# of Jobs","Wall Duration","Delta jobs","Delta duration")
    num_header = 2
    formats = {}
    lines = {}
    totalheaders = ["All VOs","All sites"]
    defaultSort = True

    def __init__(self, header = False, with_panda = False):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %3s | %-18s | %-30s | %-9s | %13s | %10s | %14s"
        self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td align=right>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td></tr>"
        self.lines["csv"] = ""
        self.lines["html"] = ""
        self.lines["text"] = "--------------------------------------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda

    def GetData(self, start,end):
        LogToFile("#######################\n## RangeVOSiteReportConf")
        return UpdateVOName(RangeVOSiteData(start, end, self.with_panda),0,start, end)   

class RangeUserReportConf(GenericConf):
    title = """\
OSG usage summary for  %s - %s (midnight UTC - midnight UTC)
including all jobs that finished in that time period.
Wall Duration is expressed in hours and rounded to the nearest hour. Wall
Duration is the duration between the instant the job started running
and the instant the job ended its execution.
Deltas are the differences with the previous period."""
    headline = "For all jobs finished between %s and %s (midnight UTC)"
    headers = ("","VO", "User", "# of Jobs", "Wall Duration", "Delta jobs", "Delta duration")
    num_header = 2
    formats = {}
    lines = {}
    totalheaders = ["All VOs", "All Users"]
    defaultSort = False
    ExtraSelect = ""

    def __init__(self, header = False, with_panda = False, selectVOName = ""):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %3s | %-22s | %-35s | %9s | %13s | %10s | %14s"
        self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td><td align=right>%s</td></tr>"

        self.lines["csv"] = ""
        self.lines["html"] = ""
        self.lines["text"] = "----------------------------------------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda
        if (len(selectVOName)>0):
            self.ExtraSelect = " and VOName = \""+selectVOName+"\" "

    def GetData(self, start,end):
        LogToFile("#######################\n## RangeUserReportConf")
        l = UpdateVOName(UserReportData(start, end, self.with_panda, self.ExtraSelect),0,start, end)
        r = []
        maxlen = 35
        for x in l:
            (vo,user,njobs,wall) = x.split('\t')
            if ( vo != "Unknown" and vo != "other" and vo != "other EGEE"):
               pos = user.find("/CN=cron/");
               if ( pos >= 0) : user = user[pos+8:maxlen+pos+8]
               pat1 = re.compile("/CN=[0-9]*/");
               user = pat1.sub("/",user);
               pat2 = re.compile("/CN=");
               user = pat2.sub("; ",user);
               if ( user.startswith("; ") ):
                  user = user[2:maxlen+2]
               else :
                  user = user[0:maxlen]
               r.append( vo + '\t' + user + '\t' + njobs + '\t' + wall )
        return r

    def Sorting(self, x,y):
        # decreasing order of WallDuration
        xval = (x[1])[1]
        yval = (y[1])[1]
        res = cmp(yval,xval)
        # dercreasing order of njobs
        if (res==0) :
           xval = (x[1])[0]
           yval = (y[1])[0]
           res = cmp(yval,xval)
        if (res==0) :
           # If the values are equal, sort on the user vo then the username
           xval = (x[0])[0].lower()
           yval = (y[0])[0].lower()
           res = cmp(xval,yval)
        if (res==0) : 
           xval = (x[0])[1].lower()
           yval = (y[0])[1].lower()
           res = cmp(xval,yval)
        return res

class RangeUserSiteReportConf(GenericConf):
    title = """\
OSG usage summary for  %s - %s (midnight UTC - midnight UTC)
including all jobs that finished in that time period.
Wall Duration is expressed in hours and rounded to the nearest hour. Wall
Duration is the duration between the instant the job started running
and the instant the job ended its execution.
Deltas are the differences with the previous period."""
    headline = "For all jobs finished between %s and %s (midnight UTC)"
    headers = ("","User", "VO", "Site", "# of Jobs", "Wall Duration", "Delta jobs", "Delta duration")
    num_header = 3
    formats = {}
    lines = {}
    totalheaders = ["All Users","All VOs","All Sites"]
    defaultSort = False
    ExtraSelect = ""

    def __init__(self, header = False, with_panda = False, selectVOName = ""):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %3s | %-35s | %-14s | %-19s | %9s | %13s | %10s | %14s"
        self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td><td align=right>%s</td><td align=right>%s</td></tr>"
        self.lines["csv"] = ""
        self.lines["html"] = ""
        self.lines["text"] = "-------------------------------------------------------------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda
        if (len(selectVOName)>0):
            self.ExtraSelect = " and VOName = \""+selectVOName+"\" "

    def GetData(self, start,end):
        LogToFile("#######################\n## RangeUserSiteReportConf")
        l = UpdateVOName(UserSiteReportData(start, end, self.with_panda, self.ExtraSelect),1,start, end)
        r = []
        maxlen = 35
        for x in l:
            (user,vo,site,njobs,wall) = x.split('\t')
            if ( vo != "Unknown" and vo != "other" and vo != "other EGEE"):
               pos = user.find("/CN=cron/");
               if ( pos >= 0) : user = user[pos+8:maxlen+pos+8]
               pat1 = re.compile("/CN=[0-9]*/");
               user = pat1.sub("/",user);
               pat2 = re.compile("/CN=");
               user = pat2.sub("; ",user);
               if ( user.startswith("; ") ):
                  user = user[2:maxlen+2]
               else :
                  user = user[0:maxlen]
               r.append( user + '\t' + vo + '\t' + site + '\t' + njobs + '\t' + wall )
        return r

    def Sorting(self, x,y):
        res = 0;
        for index in range(0,len(x[0])):
           if (res==0):
              xval = x[0][index].lower()
              yval = y[0][index].lower()
              res = cmp(xval,yval)
        return res

class LongJobsConf(GenericConf):
    title = """\
Summary of long running jobs that finished between %s - %s (midnight UTC - midnight UTC)
Wall Duration is expressed in days to the nearest days.
%% Cpu is the percentage of the wall duration time where the cpu was used.
Only jobs that last 7 days or longer are counted in this report.\n
"""
    headline = "For all jobs finished between %s and %s (midnight UTC)\n"
    headers = ("","Site", "VO", "# of Jobs","Avg Wall","% Cpu","Max EndTime")
    num_header = 2
    formats = {}
    lines = {}
    totalheaders = ["All VOs","All sites"]

    def __init__(self, header = False, with_panda = False):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %3s | %-18s | %-14s | %9s | %8s | %5s | %11s"
        self.formats["html"] = "<tr bgcolor=white><td>%s</td><td>%s</td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s</td><td align=right>%s</td></tr>"

        self.lines["csv"] = ""
        self.lines["html"] = ""
        self.lines["text"] = "---------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda

    def GetData(self, start,end):
        LogToFile("#######################\n## LongJobsConf")
        return UpdateVOName(LongJobsData(start, end, self.with_panda),1,start, end)      

class RangeSiteVOEfficiencyConf(GenericConf):
        title = """\
OSG efficiency summary by Site & VO for  %s - %s (midnight UTC - midnight UTC)
including all jobs that finished in that time period.
Wall Duration (Wall) is expressed in hours and rounded to the nearest hour. Wall
Duration is the duration between the instant the job started running
and the instant the job ended its execution.
Deltas are the differences with the previous period."""
        headline = "For all jobs finished between %s and %s (midnight UTC)"
        headers = ("  #  ","Site", "VO","Cores","Njobs","Delta","Wall","Delta","CpuToWall","Delta","%Effi","Delta")
        num_header = 2
        formats = {}
        formats1 = {}
        lines = {}
        totalheaders = ["All sites","All VOs"]
        defaultSort = True
        type = "site_vo"

        def __init__(self, header = False):
           self.formats["csv"] = "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats1["csv"] = "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "%4s | %-22s | %-15s | %9s | %9s | %9s | %9s | %10s | %9s | %5s | %5s | %5s"
           self.formats1["text"] = "%4s | %-22s | %-15s | %9s | %9s | %9s | %9s | %10s | %9.2f | %5.2f | %5s | %5s"
           self.formats["html"] = " <tr bgcolor=white><td>%s. </td><td> %s </td><td> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td></tr>"
           self.formats1["html"] = " <tr bgcolor=white><td>%s. </td><td> %s </td><td> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %9.2f </td><td align=right> %s </td><td align=right> %s </td></tr>"
           self.lines["csv"] = ""
           self.lines["text"] = "---------------------------------------------------------------------------------------------------------------------------------------------------------"
           self.lines["html"] = ""

           if (not header) :  self.title = ""

        def GetData(self,start,end):
           LogToFile("#######################\n## RangeSiteVOEfficiencyConf")
           return UpdateVOName(GetSiteVOEfficiency(start,end),1,start, end)

class RangeVOEfficiencyConf(GenericConf):
        title = """\
OSG efficiency summary by VO for  %s - %s (midnight UTC - midnight UTC)
including all jobs that finished in that time period.
Wall Duration (Wall) is expressed in hours and rounded to the nearest hour. Wall
Duration is the duration between the instant the job started running
and the instant the job ended its execution.
Deltas are the differences with the previous period."""
        headline = "For all jobs finished between %s and %s (midnight UTC)"
        headers = ("  #  ","VO","Cores","Njobs","Delta","Wall","Delta","CpuToWall","Delta","%Effi","Delta")
        num_header = 1
        formats = {}
        formats1 = {}
        lines = {}
        totalheaders = ["All VOs"]
        defaultSort = True
        type = "vo"

        def __init__(self, header = False):
           self.formats["csv"] = "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats1["csv"] = "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "%4s | %-15s | %7s | %9s | %9s | %9s | %10s | %9s | %9s | %5s | %5s"
           self.formats1["text"] = "%4s | %-15s | %7s | %9s | %9s | %9s | %10s | %9.2f | %9.2f | %5s | %5s"
           self.formats["html"] = " <tr bgcolor=white><td>%s. </td><td> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td></tr>"
           self.formats1["html"] = " <tr bgcolor=white><td>%s. </td><td> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %9.2f </td><td align=right> %s </td><td align=right> %s </td></tr>"


           self.lines["csv"] = ""
           self.lines["text"] = "---------------------------------------------------------------------------------------------------------------------------------"
           self.lines["html"] = ""

           if (not header) :  self.title = ""

        def GetData(self,start,end):
           LogToFile("#######################\n## RangeVOEfficiencyConf")
           return UpdateVOName(GetVOEfficiency(start,end),0,start, end)
                      
class GradedEfficiencyConf(GenericConf):
        title = """\
OSG efficiency summary for  %s - %s (midnight UTC - midnight UTC)
including all jobs that finished in that time period.
Wall Duration is expressed in hours and rounded to the nearest hour. Wall
Duration is the duration between the instant the job started running
and the instant the job ended its execution.
Cpu Duration is the sum for each core that participated to the job 
of the amount of time the core participated actively to the job.

Efficiency is the ratio of Cpu Duration used over the WallDuration."""
        headline = "For all jobs finished between %s and %s (midnight UTC)"
        headers = ("  #  ","VO","Cores","1 Days","7 Days","30 Days")
        num_header = 1
        formats = {}
        formats1 = {}
        lines = {}
        totalheaders = ["All VOs"]
        defaultSort = True

        def __init__(self, header = False):
           self.formats["text"] = "%4s | %-14s | %9s | %9s | %9s | %9s "
           self.formats["csv"] = "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["html"] = "<tr bgcolor=white><td>%s. </td><td> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td><td align=right> %s </td></tr>"

           self.lines["csv"] = ""
           self.lines["text"] = "------------------------------------------------------------------------"
           self.lines["html"] = ""

           if (not header) :  self.title = ""

        def GetData(self,start,end):
           LogToFile("#######################\n## GradedEfficiencyConf")
           return UpdateVOName(GetVOEfficiency(start,end),0,start, end)

def SimpleRange(what, range_end = datetime.date.today(),
                 range_begin = None,
                 output = "text"):

    old_stdout = sys.stdout
    sys.stdout = stdout = StringIO()
    ret = ""

    if(output == "html"):
        print "<table><tr><td>"

    if not range_begin: range_begin = range_end + datetime.timedelta(days=-1)
    timediff = range_end - range_begin

    if (output != "None") :
        if (what.title != "") :
            if(output == "html"):
                what.title = what.title.replace("\n","<br>");
            print what.title % ( DateToString(range_begin,False),
                                 DateToString(range_end,False) )
        if (what.headline != "") :
            if(output == "html"):
                what.headline = what.headline.replace("\n","<br>");
            print what.headline % ( DateToString(range_begin,False),
                                    DateToString(range_end,False) )

        if(output == "html"):
            print "</td></tr></table>"
            print "<table bgcolor=black cellspacing=1 cellpadding=5>"

        print what.lines[output]
        #print "    ", what.formats[output] % what.headers
        print what.formats[output] % what.headers
        print what.lines[output]

    start = range_begin
    end = range_end
    lines = what.GetData(start,end)
    num_header = 1;
    index = 0
    printValues = {}
    if (len(lines)==1 and lines[0]==""):
        print "\nNo data to report.\n"
        return []
    
    for i in range (0,len(lines)):
        val = lines[i].split('\t')
        site = val[0]
        vo = val[1]
        key = site + " " + vo
        njobs= int( val[2] )
        wall = float( val[3] )
        if ( val[4] == "NULL"):
           cpu = 0
        else:
           cpu = float( val[4] )
        endtime = val[5]
        if printValues.has_key(key):
            printValues[key][0] += njobs
            printValues[key][1] += wall
        else:
            printValues[key] = [njobs,wall,cpu,endtime,site,vo]
        
    result = []

    for key,(njobs,wall,cpu,endtime,site,vo) in sortedDictValues(printValues):
        index = index + 1;
        values = (index,site,vo,niceNum(njobs), niceNum(wall), niceNum(cpu), endtime)

        if (output != "None"):
            #print "%3d " %(index), what.formats[output] % values
            print what.formats[output] % values
        result.append(values)       

    if(output == "html"):
       print "</table>"

    sys.stdout = old_stdout
    ret = stdout.getvalue()
    return ret
                

def GenericRange(what, range_end = datetime.date.today(),
                 range_begin = None,
                 output = "text"):

    old_stdout = sys.stdout
    sys.stdout = stdout = StringIO()
    ret = ""

    factor = what.factor # Convert number of seconds to number of hours for most reports except data transfer report

    if (not range_begin or range_begin == None): range_begin = range_end + datetime.timedelta(days=-1)
    if (not range_end or range_end == None): range_end = range_begin + datetime.timedelta(days=+1)
    timediff = range_end - range_begin

    if (output != "None") :
        if (what.title != "") :
            if(output == "html"):
                what.title = "<br>" + what.title + "<br>"
            print what.title % ( DateToString(range_begin,False),
                                 DateToString(range_end,False) )
        if (what.headline != "") :
            if(output == "html"):
                what.headline = "<br>" + what.headline + "<br>"

            print what.headline % ( DateToString(range_begin,False),
                                    DateToString(range_end,False) )

        if(output == "html"):
            print "&nbsp<p>"
            print "</td></tr></table>"
            print "<table bgcolor=black cellspacing=1 cellpadding=5>"

        print what.lines[output]
        print what.formats[output] % what.headers
        print what.lines[output]
        
    # First get the previous' range-length's information
    totalwall = 0
    totaljobs = 0
    oldValues = {}
    result = []

    start = range_begin - timediff
    end = range_end - timediff
    lines = what.GetData(start,end)
    for i in range (0,len(lines)):
        val = lines[i].split('\t')
        offset = 0
        
        lkeys = ["","",""]
        for iheaders in range(0,what.num_header):
           lkeys[iheaders] = val[iheaders]

        if what.headers[0] == "VO":
            # "site" is really "VO": hack to harmonize Panda output
            if lkeys[0] != "Unknown": lkeys[0] = string.lower(lkeys[0])

        if (len(val)==4) :
            # Nasty hack to harmonize Panda output
            if what.headers[1] == "VO":
               if lkeys[1] != "Unknown": lkeys[1] = string.lower(lkeys[1])

        #for iheaders in range(1,len(keys)):
        #   key = key + keys[iheaders] + " "
        keys = tuple(lkeys)
        
        num_header = what.num_header;
        offset = num_header - 1;

        njobs= int( val[offset+1] )
        wall = float( val[offset+2] ) / factor
        totalwall = totalwall + wall
        totaljobs = totaljobs + njobs
        if (oldValues.has_key(keys)):
            oldValues[keys][0] += njobs
            oldValues[keys][1] += wall
        else:
            oldValues[keys] = [njobs,wall]
    oldValues[("total","","")] = (totaljobs, totalwall)

    # Then getting the current information and print it
    totalwall = 0
    totaljobs = 0
    start = range_begin
    end = range_end
    lines = what.GetData(start,end)
    num_header = what.num_header;
    index = 0
    printValues = {}
    for i in range (0,len(lines)):
        val = lines[i].split('\t')

        lkeys = ["","",""]
        for iheaders in range(0,what.num_header):
           lkeys[iheaders] = val[iheaders]

        if what.headers[0] == "VO":
            # "site" is really "VO": hack to harmonize Panda output
            if lkeys[0] != "Unknown": lkeys[0] = string.lower(lkeys[0])

        if (len(val)==4) :
            # Nasty hack to harmonize Panda output
            if what.headers[1] == "VO":
               if lkeys[1] != "Unknown": lkeys[1] = string.lower(lkeys[1])

#        for iheaders in range(0,len(keys)):
#           key = key + keys[iheaders] + " "
        keys = tuple( lkeys )

        num_header = what.num_header;
        offset = num_header - 1;

        (oldnjobs,oldwall) = (0,0)
        if oldValues.has_key(keys):
            (oldnjobs,oldwall) = oldValues[keys]
            del oldValues[keys]
        njobs= int( val[offset+1] )
        wall = float( val[offset+2] ) / factor
        totalwall = totalwall + wall
        totaljobs = totaljobs + njobs
        if printValues.has_key(keys):
            printValues[keys][0] += njobs
            printValues[keys][1] += wall
        else:
            printValues[keys] = [njobs,wall,oldnjobs,oldwall]
                
    for key,(oldnjobs,oldwall) in oldValues.iteritems():            
        if (key[0] != "total") :
            printValues[key] = (0,0,oldnjobs,oldwall)

    if (what.defaultSort):
        sortedValues = sortedDictValues(printValues)
    else:
        sortedValues = sortedDictValuesFunc(printValues,what.Sorting)
        
    for key,(njobs,wall,oldnjobs,oldwall) in sortedValues:
        index = index + 1;
        printedvalues = []
        printedvalues.append(index)
        for iheaders in range(0,num_header):
           printedvalues.append( key[iheaders] )
        if(what.delta_column_location == "adjacent"): # print the delta columns adjacent to the corresponding field for which the delta has been calculated
            printedvalues.append( niceNum(njobs) )
            printedvalues.append( niceNum(njobs-oldnjobs) )
            printedvalues.append( niceNum(wall) )
            printedvalues.append( niceNum(wall-oldwall) )
	else: # print the delta columns to the right
            printedvalues.append( niceNum(njobs) )
            printedvalues.append( niceNum(wall) )
            printedvalues.append( niceNum(njobs-oldnjobs) )
            printedvalues.append( niceNum(wall-oldwall) )

        if (output != "None") :
            #print "%3d " %(index), what.formats[output] % tuple(printedvalues)
            print what.formats[output] % tuple(printedvalues)
        result.append(tuple(printedvalues))       
                
    (oldnjobs,oldwall) = oldValues[("total","","")]
    if (output != "None") :
        print what.lines[output]
        printedvalues = []
        for iheaders in range(0,num_header):
           printedvalues.append("")
           printedvalues.append( what.totalheaders[iheaders] )
        if(what.delta_column_location == "adjacent"): # sum delta columns adjacent to the corresponding field for which the delta has been calculated
            printedvalues.append( niceNum(totaljobs) )
            printedvalues.append( niceNum(totaljobs-oldnjobs) )
            printedvalues.append( niceNum(totalwall) )
            printedvalues.append( niceNum(totalwall-oldwall) )
	else:
            printedvalues.append( niceNum(totaljobs) )
            printedvalues.append( niceNum(totalwall) )
            printedvalues.append( niceNum(totaljobs-oldnjobs) )
            printedvalues.append( niceNum(totalwall-oldwall) )

        #print "    ", what.formats[output] % tuple(printedvalues)
        #print what.formats[output] % tuple(printedvalues)
        print what.lines[output]

    if(output == "html"):
        print "</table>"

    sys.stdout = old_stdout
    ret = stdout.getvalue()
    return ret

# HK TransferVO Apr 26 2013 BEGIN 
def GenericRangeTrVO(what, range_end = datetime.date.today(),
                 range_begin = None,
                 output = "text"):

    old_stdout = sys.stdout
    sys.stdout = stdout = StringIO()
    ret = ""

    factor = what.factor # Convert number of seconds to number of hours for most reports except data transfer report

    if (not range_begin or range_begin == None): range_begin = range_end + datetime.timedelta(days=-1)
    if (not range_end or range_end == None): range_end = range_begin + datetime.timedelta(days=+1)
    timediff = range_end - range_begin

    if (output != "None") :
        if (what.title != "") :
            if(output == "html"):
                what.title = "<br>" + what.title + "<br>"
            print what.title % ( DateToString(range_begin,False),
                                 DateToString(range_end,False) )
        if (what.headline != "") :
            if(output == "html"):
                what.headline = "<br>" + what.headline + "<br>"

            print what.headline % ( DateToString(range_begin,False),
                                    DateToString(range_end,False) )

        if(output == "html"):
            print "&nbsp<p>"
            print "</td></tr></table>"
            print "<table bgcolor=black cellspacing=1 cellpadding=5>"

        print what.lines[output]
        print what.formats[output] % what.headers
        print what.lines[output]
        
    # First get the previous' range-length's information
    totalwall = 0
    totaljobs = 0
    oldValues = {}
    result = []

    start = range_begin - timediff
    end = range_end - timediff
    lines = what.GetData(start,end)
    for i in range (0,len(lines)):
        val = lines[i].split('\t')
        offset = 0
        
#        lkeys = ["","",""] # HK
        lkeys = ["","","",""]
        for iheaders in range(0,what.num_header):
           lkeys[iheaders] = val[iheaders]

        if what.headers[0] == "VO":
            # "site" is really "VO": hack to harmonize Panda output
            if lkeys[0] != "Unknown": lkeys[0] = string.lower(lkeys[0])

        if (len(val)==4) :
            # Nasty hack to harmonize Panda output
            if what.headers[1] == "VO":
               if lkeys[1] != "Unknown": lkeys[1] = string.lower(lkeys[1])

        #for iheaders in range(1,len(keys)):
        #   key = key + keys[iheaders] + " "
        keys = tuple(lkeys)
        
        num_header = what.num_header;
        offset = num_header - 1;

        njobs= int( val[offset+1] )
        wall = float( val[offset+2] ) / factor
        totalwall = totalwall + wall
        totaljobs = totaljobs + njobs
        if (oldValues.has_key(keys)):
            oldValues[keys][0] += njobs
            oldValues[keys][1] += wall
        else:
            oldValues[keys] = [njobs,wall]
    oldValues[("total","","")] = (totaljobs, totalwall)

    # Then getting the current information and print it
    totalwall = 0
    totaljobs = 0
    start = range_begin
    end = range_end
    lines = what.GetData(start,end)
    num_header = what.num_header;
    index = 0
    printValues = {}
    for i in range (0,len(lines)):
        val = lines[i].split('\t')

#        lkeys = ["","",""] # HK
        lkeys = ["","","",""]
        for iheaders in range(0,what.num_header):
           lkeys[iheaders] = val[iheaders]

        if what.headers[0] == "VO":
            # "site" is really "VO": hack to harmonize Panda output
            if lkeys[0] != "Unknown": lkeys[0] = string.lower(lkeys[0])

        if (len(val)==4) :
            # Nasty hack to harmonize Panda output
            if what.headers[1] == "VO":
               if lkeys[1] != "Unknown": lkeys[1] = string.lower(lkeys[1])

#        for iheaders in range(0,len(keys)):
#           key = key + keys[iheaders] + " "
        keys = tuple( lkeys )

        num_header = what.num_header;
        offset = num_header - 1;

        (oldnjobs,oldwall) = (0,0)
        if oldValues.has_key(keys):
            (oldnjobs,oldwall) = oldValues[keys]
            del oldValues[keys]
        njobs= int( val[offset+1] )
        wall = float( val[offset+2] ) / factor
        totalwall = totalwall + wall
        totaljobs = totaljobs + njobs
        if printValues.has_key(keys):
            printValues[keys][0] += njobs
            printValues[keys][1] += wall
        else:
            printValues[keys] = [njobs,wall,oldnjobs,oldwall]
                
    for key,(oldnjobs,oldwall) in oldValues.iteritems():            
        if (key[0] != "total") :
            printValues[key] = (0,0,oldnjobs,oldwall)

    if (what.defaultSort):
        sortedValues = sortedDictValues(printValues)
    else:
        sortedValues = sortedDictValuesFunc(printValues,what.Sorting)
        
    for key,(njobs,wall,oldnjobs,oldwall) in sortedValues:
        index = index + 1;
        printedvalues = []
        printedvalues.append(index)
        for iheaders in range(0,num_header):
           printedvalues.append( key[iheaders] )
        if(what.delta_column_location == "adjacent"): # print the delta columns adjacent to the corresponding field for which the delta has been calculated
            printedvalues.append( niceNum(njobs) )
            printedvalues.append( niceNum(njobs-oldnjobs) )
            printedvalues.append( niceNum(wall) )
            printedvalues.append( niceNum(wall-oldwall) )
	else: # print the delta columns to the right
            printedvalues.append( niceNum(njobs) )
            printedvalues.append( niceNum(wall) )
            printedvalues.append( niceNum(njobs-oldnjobs) )
            printedvalues.append( niceNum(wall-oldwall) )

        if (output != "None") :
            #print "%3d " %(index), what.formats[output] % tuple(printedvalues)
            print what.formats[output] % tuple(printedvalues)
        result.append(tuple(printedvalues))       
                
    (oldnjobs,oldwall) = oldValues[("total","","")]
    if (output != "None") :
        print what.lines[output]
        printedvalues = []
        for iheaders in range(0,num_header):
           printedvalues.append("")
           printedvalues.append( what.totalheaders[iheaders] )
        if(what.delta_column_location == "adjacent"): # sum delta columns adjacent to the corresponding field for which the delta has been calculated
            printedvalues.append( niceNum(totaljobs) )
            printedvalues.append( niceNum(totaljobs-oldnjobs) )
            printedvalues.append( niceNum(totalwall) )
            printedvalues.append( niceNum(totalwall-oldwall) )
	else:
            printedvalues.append( niceNum(totaljobs) )
            printedvalues.append( niceNum(totalwall) )
            printedvalues.append( niceNum(totaljobs-oldnjobs) )
            printedvalues.append( niceNum(totalwall-oldwall) )

        #print "    ", what.formats[output] % tuple(printedvalues)
        #print what.formats[output] % tuple(printedvalues)
        print what.lines[output]

    if(output == "html"):
        print "</table>"

    sys.stdout = old_stdout
    ret = stdout.getvalue()
    return ret
# HK TransferVO Apr 26 2013 END

def negate(val):
    if(val != 0):
        return -val
    return val

def EfficiencyRange_fill_dict(data,type):
    dict = {}
    count=0
    for entry in data:
        col = entry.split('\t')
        count+=1
        if(type == "site_vo"):
            key = col[0] + "," + col[1] + "," + str(col[5])  # key is formed from a unique combination of site,vo,cores
            dict[key] = [int(col[2]), float(col[3])/3600, col[4], col[6].split('.')[0]] # (njobs, wall, cpuToWall, eff)
        elif(type == "vo"):
            key = col[0] + "," + str(col[4])  # key is formed from a unique combination of vo,cores
            dict[key] = [int(col[1]), float(col[2])/3600, col[3], col[5].split('.')[0]] # (njobs, wall, cpuToWall, eff)
    return dict

def EfficiencyRange(what, range_end = datetime.date.today(),
                 range_begin = None,
                 output = "text"):

    if (not range_begin or range_begin == None): range_begin = range_end + datetime.timedelta(days=-1)
    if (not range_end or range_end == None): range_end = range_begin + datetime.timedelta(days=+1)
    timediff = range_end - range_begin
    out = ""

    if (output != "None") :
        if (what.title != "") :
            out+= what.title % ( DateToString(range_begin,False), DateToString(range_end,False) ) + "\n"
        if (what.headline != "") :
            out+= what.headline % ( DateToString(range_begin,False), DateToString(range_end,False) ) + "\n"
        out+= what.lines[output] + "\n"
        if(output == "html"):
            out+="<table bgcolor=black cellspacing=1 cellpadding=5>"
        out+= what.formats[output] % what.headers + "\n"
        out+= what.lines[output] + "\n"

        data = what.GetData(range_begin,range_end)
        data_prev = what.GetData(range_begin - timediff,range_end - timediff)
        row_data = {}

        num = "~~"
        njobs_sum = 0 
        delta_njobs_sum = 0 
        wall_sum = 0 
        delta_wall_sum = 0 
        cpuToWall_sum = 0 
        delta_cpuToWall_sum = 0 
        eff_sum = 0
        delta_eff_sum = 0

        current = {}
        prev = {}
        current = EfficiencyRange_fill_dict(data,what.type)
        prev    = EfficiencyRange_fill_dict(data_prev,what.type)

        for k in sorted(list(set(sorted(current.iterkeys())) | set(sorted(prev.iterkeys())))):
            #count+=1
            if(what.type == "site_vo"):
                site,vo,cores = k.split(',')
                siteVO = site + "," + vo
                if siteVO not in row_data:
                    row_data[siteVO] = {}
            elif(what.type == "vo"):
                vo,cores = k.split(',')
                if vo not in row_data:
                    row_data[vo] = {}
            if (k in current) and (k not in prev):
                prev[k] = (0,0,0.00,0)
                delta_njobs = current[k][0]
                delta_wall = current[k][1]
                delta_cpuToWall = current[k][2]
                delta_eff = current[k][3]
            elif (k in prev) and (k not in current):
                current[k] = (0,0,0.00,0)
                delta_njobs = negate(prev[k][0])
                delta_wall = negate(prev[k][1])
# HK> This is to deal with Efficiency and VOEfficiency missing error reported by Steve Timm and BNL
# by HK         if(prev[k][2] != "NULL"):
                if(prev[k][2] != "NULL") and (prev[k][3] != "NULL") :
                    delta_cpuToWall = negate(float(prev[k][2]))
                    delta_eff = negate(int(prev[k][3]))
                else:
                    delta_cpuToWall = "n/a"
                    delta_eff = "n/a"
            elif (k in prev) and (k in current):
                delta_njobs = int(current[k][0]) - int(prev[k][0])
                delta_wall = current[k][1] - prev[k][1]
# HK> This is to deal with Efficiency and VOEfficiency missing error reported by Steve Timm and BNL
# by HK         if(current[k][2] != "NULL" and prev[k][2] != "NULL"):
                if(current[k][2] != "NULL" and prev[k][2] != "NULL")  and (current[k][3] != "NULL" and prev[k][3] != "NULL") :
                    delta_cpuToWall = float(current[k][2]) - float(prev[k][2])
                    delta_eff = float(current[k][3]) - float(prev[k][3])
                else:
                    delta_cpuToWall = "n/a"
                    delta_eff = "n/a"
            njobs_sum+= int(current[k][0])
            wall_sum+= int(current[k][1])
# HK> This is to deal with Efficiency and VOEfficiency missing error reported by Steve Timm and BNL
# by HK     if(current[k][2] != "NULL"):
            if(current[k][2] != "NULL") and (current[k][3] != "NULL")  :
                eff_sum+= float(current[k][3])
                cpuToWall_sum+= float(current[k][2]) 
            delta_njobs_sum+= int(delta_njobs)
            delta_wall_sum+= int(delta_wall) 
            if(delta_cpuToWall != 'n/a' and delta_cpuToWall != 'NULL' and delta_cpuToWall != ''):
                delta_cpuToWall_sum+= float(delta_cpuToWall)
                delta_eff_sum+= float(delta_eff)
                if(what.type == "site_vo"):
                    row_data[siteVO][int(cores)] = what.formats1[output] % (num,site,vo,cores,niceNum(int(current[k][0])),niceNum(int(delta_njobs)),niceNum(float(current[k][1]),.1),niceNum(int(delta_wall)),float(current[k][2]),float(delta_cpuToWall),niceNum(int(current[k][3])),niceNum(int(delta_eff))) + "\n"
                elif(what.type == "vo"):
                    row_data[vo][int(cores)] = what.formats1[output] % (num,vo,cores,niceNum(int(current[k][0])),niceNum(int(delta_njobs)),niceNum(float(current[k][1]),.1),niceNum(int(delta_wall)),float(current[k][2]),float(delta_cpuToWall),niceNum(int(current[k][3])),niceNum(int(delta_eff))) + "\n"

        count=0
        for k1 in sorted(row_data.iterkeys()):
            for k2 in sorted(row_data[k1].iterkeys()):
                count+=1
                out+=row_data[k1][k2].replace(num,format_space_adjust(count))

        cpuToWall_avg = float(cpuToWall_sum)/count
        eff_avg = int(eff_sum)/count
        out+= what.lines[output] + "\n"
        if(what.type == "site_vo"):
            out+= what.formats1[output] % ("","All sites","All VOs","All Cores",niceNum(int(njobs_sum)),niceNum(int(delta_njobs_sum)),niceNum(int(wall_sum)),niceNum(int(delta_wall_sum)),float(cpuToWall_avg),delta_cpuToWall_sum/count,eff_avg,niceNum(int(delta_eff_sum/count))) + "\n"
        elif(what.type == "vo"):
            out+= what.formats1[output] % ("","All VOs","All Cores",niceNum(int(njobs_sum)),niceNum(int(delta_njobs_sum)),niceNum(int(wall_sum)),niceNum(int(delta_wall_sum)),float(cpuToWall_avg),delta_cpuToWall_sum/count,eff_avg,niceNum(int(delta_eff_sum/count))) + "\n"
        out+= what.lines[output] + "\n"
        if(output == "html"):
            out+="</table>"
        return out

def format_space_adjust(count):
    if(count < 10):
        return str(count) + "  "
    if(count < 100):
        return str(count) + " "
    return str(count)

def EfficiencyGraded(what, range_end = datetime.date.today(),
                 range_begin = None,
                 output = "text"):

    out = ""
    if (output != "None") :
        if (what.title != "") :
            out+= what.title % ( DateToString(range_begin,False), DateToString(range_end,False) ) + "\n"
        if (what.headline != "") :
            out+= what.headline % ( DateToString(range_begin,False), DateToString(range_end,False) ) + "\n"
        out+= what.lines[output] + "\n"
        if(output == "html"):
            out+="<table bgcolor=black cellspacing=1 cellpadding=5>"
        out+= what.formats[output] % what.headers + "\n"
        out+= what.lines[output] + "\n"

        days = (1,7,30)
        data = {}
        keys = {}
        row_data = {}
        count=0
        num="~~"

        for day in days:
            range_begin = range_end + datetime.timedelta(days=-day)
            data[day] = EfficiencyRange_fill_dict(what.GetData(range_begin,range_end),"vo")

        for k in sorted(data[30].iterkeys()):
            vo,cores = k.split(',')
            if vo not in row_data: 
                row_data[vo] = {}
            for day in days:
                if k in data[day]:
                    if(data[day][k][3] == "NULL"):
                        data[day][k][3] = "n/a"
                else:
                    data[day][k] = ["n/a","n/a","n/a","n/a"]
            if(data[1][k][3] != "n/a" or data[7][k][3] != "n/a" or data[30][k][3] != "n/a" ):
                row_data[vo][int(cores)] = what.formats[output] % (num,vo,cores, EfficiencyGraded_niceNum(data[1][k][3]), EfficiencyGraded_niceNum(data[7][k][3]), EfficiencyGraded_niceNum(data[30][k][3])) + "\n"

        for k1 in sorted(row_data.iterkeys()):
            for k2 in sorted(row_data[k1].iterkeys()):
                count+=1
                out+=row_data[k1][k2].replace(num,format_space_adjust(count))

        if(output == "html"):
            out+="</table>"
        out+= what.lines[output] + "\n"
        return out

def EfficiencyGraded_niceNum(val):
    if(val != "n/a"):
        return niceNum(int(val))
    return val

def RangeVOReport(range_end = datetime.date.today(),
                  range_begin = None,
                  output = "text",
                  header = True,
                  with_panda = False):
    return GenericRange(RangeVOReportConf(header, with_panda),
                        range_end,
                        range_begin,
                        output)

def RangeSiteReport(range_end = datetime.date.today(),
                    range_begin = None,
                    output = "text",
                    header = True,
                    with_panda = False):
    return GenericRange(RangeSiteReportConf(header, with_panda),
                        range_end,
                        range_begin,
                        output)

def RangeSiteVOReport(range_end = datetime.date.today(),
                      range_begin = None,
                      output = "text",
                      header = True,
                      with_panda = False):
    return GenericRange(RangeSiteVOReportConf(header, with_panda),
                        range_end,
                        range_begin,
                        output)

def DataTransferReport(range_end = datetime.date.today(),
                      range_begin = None,
                      output = "text",
                      header = True,
                      with_panda = False):
    return GenericRange(DataTransferReportConf(header, with_panda),
                        range_end,
                        range_begin,
                        output)

# HK TransferVO Apr 26 2013 BEGIN 
def DataTransferReportTrVO(range_end = datetime.date.today(),
                      range_begin = None,
                      output = "text",
                      header = True,
                      with_panda = False):
    return GenericRangeTrVO( DataTransferReportConfTrVO(header, with_panda),
                        range_end,
                        range_begin,
                        output)
# HK TransferVO Apr 26 2013 END

def RangeVOSiteReport(range_end = datetime.date.today(),
                      range_begin = None,
                      output = "text",
                      header = True,
                      with_panda = False):
    return GenericRange(RangeVOSiteReportConf(header, with_panda),
                        range_end,
                        range_begin,
                        output)

def RangeUserReport(range_end = datetime.date.today(),
                      range_begin = None,
                      output = "text",
                      header = True,
                      with_panda = False,
                      selectVOName = ""):
    return GenericRange(RangeUserReportConf(header, with_panda, selectVOName),
                        range_end,
                        range_begin,
                        output)

def RangeSiteUserReport(range_end = datetime.date.today(),
                      range_begin = None,
                      output = "text",
                      header = True,
                      with_panda = False,
                      selectVOName = ""):
    return GenericRange(RangeUserSiteReportConf(header, with_panda, selectVOName),
                        range_end,
                        range_begin,
                        output)

def RangeLongJobs(range_end = datetime.date.today(),
                      range_begin = None,
                      output = "text",
                      header = True,
                      with_panda = False):
    return SimpleRange(LongJobsConf(header, with_panda),
                        range_end,
                        range_begin,
                        output)

def GetSiteLastReportingDate(begin,recent):
    schema = gDBSchema[mainDB] + ".";
    if (recent):
        test = ">="
    else:
        test = "<"
    
    select = """\
select SiteName,Date(currentTime) as DateOfLastContact from (
    select SiteName,ProbeName,max(currentTime) as currentTime from Probe, Site
where Site.siteid = Probe.siteid and active = true group by SiteName) sub
where currentTime """+test+"""  \"""" + DateToString(begin) + """\" order by currentTime
       """
    #print "Query = " + select;

    return RunQueryAndSplit(select);

def GetReportingVOs(begin,end):
    schema = gDBSchema[mainDB] + ".";

    select = """\
select distinct VOName from """+schema+"""VOProbeSummary V where VOName != \"Unknown\" and 
            EndTime >= \"""" + DateToString(begin) + """\" and
            EndTime < \"""" + DateToString(end) + """\" and
	    ResourceType=\"Batch\"
            order by VOName
            """
    #print "Query = " + select;

    return RunQueryAndSplit(select);

def GetLastReportingVOs(when):
    schema = gDBSchema[mainDB] + ".";

    select = """\
select distinct VOName from """+schema+"""VOProbeSummary V where VOName != \"Unknown\" and ResourceType=\"Batch\" and
            EndTime >= \"""" + DateToString(when) + """\" 
            order by VOName
            """
    #print "Query = " + select;

    return RunQueryAndSplit(select);

def GetSiteLastActivity(begin):
    schema = gDBSchema[mainDB] + ".";

    select = """\
    select * from (select SiteName, max(probeMaxTime) as siteMaxTime from
(select ProbeName,max(EndTime) as probeMaxTime from ProbeSummary where ResourceType = \"Batch\" group by ProbeName order by probeMaxTime) sub,Probe P, Site S
where sub.ProbeName = P.ProbeName and P.siteid = S.siteid and P.active = True
group by S.siteid
order by siteMaxTime) ssub where siteMaxTime < \"""" + DateToString(begin) + """\" 
       """
    #print "Query = " + select;

    return RunQueryAndSplit(select);

def GetListOfReportingSites(begin,end):
    LogToFile("#######################\n## def GetListOfReportingSites")
    schema = gDBSchema[mainDB] + ".";

    select = """\
select distinct SiteName from """+schema+"""VOProbeSummary V,Probe P,Site S where VOName != \"Unknown\" and 
            EndTime >= \"""" + DateToString(begin) + """\" and
            EndTime < \"""" + DateToString(end) + """\" and
	    ResourceType = \"Batch\"
            and V.ProbeName = P.ProbeName and P.siteid = S.siteid
            order by SiteName
            """
    #print "Query = " + select;

    result = RunQueryAndSplit(select);
    return result

def GetListOfDataTransferReportingSites(begin,end):
    LogToFile("#######################\n## def GetListOfDataTransferReportingSites")
    global gMySQLConnectString, gDBCurrent
    schema = gDBSchema[transferDB] + ".";
    gDBCurrent = transferDB

    keepConnectionValue = gMySQLConnectString
    gMySQLConnectString = gMySQLTransferConnectString

    select = "select distinct SiteName from " + schema + "MasterTransferSummary M,Probe P,Site S where StartTime >= \"" + DateToString(begin) + "\" and StartTime < \"" + DateToString(end) + "\" and M.ProbeName = P.ProbeName and P.siteid = S.siteid order by SiteName"

    result =  RunQueryAndSplit(select);
    gMySQLConnectString = keepConnectionValue
    return result

def GetTotals(begin,end):
    LogToFile("#######################\n## def GetTotals")
    schema = gDBSchema[mainDB] + ".";

    select = """\
select sum(Njobs),sum(WallDuration),sum(CpuUserDuration+CpuSystemDuration)/sum(WallDuration) from """+schema+"""VOProbeSummary where VOName != \"Unknown\" and 
            EndTime >= \"""" + DateToString(begin) + """\" and
            EndTime < \"""" + DateToString(end) + """\" and
	    ResourceType = \"Batch\"
            """
    #print "Query = " + select;

    return RunQueryAndSplit(select)[0].split('\t');

def GetDataTransferTotals(begin,end):
    LogToFile("#######################\n## def GetDataTransferTotals")
    global gMySQLConnectString, gDBCurrent
    schema = gDBSchema[transferDB] + ".";
    gDBCurrent = transferDB
    keepConnectionValue = gMySQLConnectString
    gMySQLConnectString = gMySQLTransferConnectString
    select = "select T.SiteName, M.Protocol, sum(M.Njobs) as Njobs, round(sum(M.TransferSize * Multiplier ),0) as SizeTotal, round(sum(M.TransferSize * Multiplier )/sum(TransferDuration),4) as rate, round(sum(TransferDuration),2) as duration from gratia_osg_transfer.MasterTransferSummary M, gratia_osg_transfer.Probe P, gratia_osg_transfer.Site T,gratia_osg_transfer.SizeUnits su where M.StorageUnit = su.Unit and P.siteid = T.siteid and M.ProbeName = P.Probename and StartTime >= \"" + DateToString(begin) + "\" and StartTime < \"" + DateToString(end) + "\" and M.ProbeName not like \"psacct:%\" group by P.siteid, Protocol;"
    res = RunQueryAndSplit(select)
    size = 0
    avg = 0
    dur = 0
    njobs = 0
    for entry in res:
        njobs+= int(entry.split('\t')[2])
        size+= int(entry.split('\t')[3])
        avg+= float(entry.split('\t')[4])
        dur+= float(entry.split('\t')[5])
    gMySQLConnectString = keepConnectionValue
    return (njobs,size,avg,dur)

def GetNewUsers(begin,end):
    LogToFile("#######################\n## def GetNewUsers")
    schema = gDBSchema[mainDB] + ".";
    select = """\
select CommonName, VO.VOName, MasterSummaryData.ProbeName, SiteName, EndTime, sum(NJobs) from 
(
   select * from ( select CommonName as subCommonName, min(EndTime) as FirstSubmission
     from """+schema+"""MasterSummaryData
     where EndTime > '2005/01/01'
     group by CommonName ) as innerquery
   where FirstSubmission >= \"""" + DateToString(begin) + """\" and
         FirstSubmission < \"""" + DateToString(end) + """\"
) as subquery, """+schema+"""MasterSummaryData, """+schema+"""VONameCorrection VOCorr, """+schema+"""VO, """+schema+"""Probe, """+schema+"""Site
where CommonName = subCommonName and
      VOcorrid = VOCorr.corrid and VOCorr.VOid = VO.VOid and
      MasterSummaryData.ProbeName = Probe.ProbeName and Probe.siteid = Site.siteid
group by CommonName, VO.VOName,  MasterSummaryData.ProbeName, SiteName
order by CommonName, VO.VOName, SiteName
"""
    return RunQueryAndSplit(select);
  
def prettyInt(n):
    return str(n)

def prettyList(l):
    if (len(l)==0): return "None"
    
    result = ""
    lastname = l.pop()
    for name in l:
        result = result + name + ", "
    result = result + lastname
    l.append(lastname)
    return result

def DataTransferSumup(range_end = datetime.date.today(),
                range_begin = None,
                output = "text",
                header = True):
    LogToFile("#######################\n## def DataTransferSumup")
    old_stdout = sys.stdout
    sys.stdout = stdout = StringIO()
    ret = ""

    br = "\n" # line break
    if(output == "html"):
       br = "<br>" # line break

    title = """\
OSG Data transfer summary for  %s - %s (midnight UTC - midnight UTC)
including all data that transferred in that time period.
Deltas are the differences with the previous period.\n"""

    #title += "\nFor all data transferred between %s and %s (midnight, UTC)\n"
    #title += "\nFor all data transferred in the above time period.\n"

    print title % (range_begin, range_end)
 
    if not gGrid or gGrid.lower() == 'local':
        try:
            gridDisplayName = gConfig.get("local", "grid_name")
        except:
            gridDisplayName = "OSG"
    else:
        gridDisplayName = gGrid 

    if not range_end:
        if not range_begin:
            range_end = datetime.date.today()
        else:
            range_end = range_begin + datetime.timedelta(days=+1)
    if not range_begin:
        range_begin = range_end + datetime.timedelta(days=-1)
    timediff = range_end - range_begin

    regSites = GetListOfOSGSEs();
    disabledSites = GetListOfDisabledOSGSites()
    reportingSitesDate = GetSiteLastReportingDate(range_begin, True)

    pingSites = []
    for data in reportingSitesDate:
        if ( len(data) > 0 ):
           (name,lastreport) = data.split("\t")
           pingSites.append(name)

    exceptionSites = ['AGLT2_CE_2', 'BNL-LCG2', 'BNL_ATLAS_1', 'BNL_ATLAS_2',
        'FNAL_GPGRID_2', 'USCMS-FNAL-XEN', 'USCMS-FNAL-WC1-CE2',
        'USCMS-FNAL-WC1-CE3', 'USCMS-FNAL-WC1-CE4', 'BNL_LOCAL', 'BNL_OSG',
        'BNL_PANDA', 'GLOW-CMS', 'UCSDT2-B', 'Purdue-Lear' ]

    reportingSites = GetListOfDataTransferReportingSites(range_begin,range_end)

    # a super list of all sites to be used as a reference to restore lists back to their original case after finding common entries between one or more lists using a case insensitive search
    completeSiteList = list(set(regSites) | set(exceptionSites) | set(reportingSites) | set(pingSites) | set(disabledSites))

    allSites = None
    if regSites != None:
        allSites = restoreOriginalCase([name for name in listToLower(regSites) if name not in listToLower(exceptionSites)], completeSiteList)

    missingSites, emptySites = None, None
    if allSites:
        missingSites = restoreOriginalCase([name for name in listToLower(allSites) if name not in \
            listToLower(reportingSites) and name not in listToLower(pingSites)], completeSiteList)
        emptySites = restoreOriginalCase([name for name in listToLower(allSites) if name not in listToLower(reportingSites) \
            and name in listToLower(pingSites)], completeSiteList)
 
    extraSites = restoreOriginalCase([name for name in listToLower(reportingSites) if listToLower(allSites) and name not in \
        listToLower(allSites) and listToLower(disabledSites) and name not in listToLower(disabledSites)], completeSiteList)
    knownExtras = restoreOriginalCase([name for name in listToLower(extraSites) if name in listToLower(exceptionSites) and \
        name not in listToLower(regSites)], completeSiteList)
    extraSites = restoreOriginalCase([name for name in listToLower(extraSites) if name not in listToLower(exceptionSites)], completeSiteList)

    reportingDisabled = None
    if disabledSites != None:
        reportingDisabled = restoreOriginalCase([name for name in listToLower(reportingSites) if name in \
            listToLower(disabledSites)], completeSiteList)

    if allSites != None:
        print br + "As of %s, there are %s registered SRMv2 %s storage resources." % \
            (DateToString(datetime.date.today(),False),
            prettyInt(len(allSites)), gridDisplayName)

    #print "\nBetween %s - %s (midnight - midnight UTC):\n" % \
    #    (DateToString(range_begin, False), DateToString(range_end, False))
                                                               
    n = len(reportingSites)
    print prettyInt(n)+" storage resources reported" + br

    (njobs, totalSize, avgRate, duration) = GetDataTransferTotals(range_begin,range_end)

    print br + "Total number of transfers: " + niceNum(njobs)
    print br + "Total transfer size: "+niceNum(totalSize/(1024*1024))+ " TiB" + br

    if reportingSites != None and extraSites != None and knownExtras != None \
            and allSites != None:
        n = len(reportingSites)-len(extraSites)-len(knownExtras)
        print br + "%s registered storage resources reported some activity (%s%% of %s storage resources)" % \
            (prettyInt(n), niceNum(n*100/len(allSites),1), gridDisplayName)

    if emptySites != None and allSites != None:
        n = len(emptySites)
        print br + "%s registered storage resources have reported but have no activity (%s%% " \
            "of %s storage resources)" % (prettyInt(n), niceNum(n*100/len(allSites), 1),
            gridDisplayName)

    if missingSites != None and allSites != None:
        n = len(missingSites)
        print br + "%s registered storage resources have NOT reported (%s%% of %s storage resources)" % \
            (prettyInt(n), niceNum(n*100/len(allSites),1), gridDisplayName)

    print br
    
    n = len(extraSites);
    if not gGrid or gGrid.lower() != "local":
        print br + prettyInt(n)+" non-sanctioned non-registered storage resources reported " \
            "(might indicate a discrepancy between OIM and Gratia)."
    elif allSites != None:
        print br + prettyInt(n)+" non-sanctioned non-registered storage resources reported."

    if reportingDisabled != None: 
        n = len(reportingDisabled)
        print br + prettyInt(n)+" disabled storage resources have reported." + br

    # data transfer table
    print br + DataTransferReport(range_end, range_begin, output, True, gWithPanda)
    
    if emptySites != None:
        print br + "The storage resources with no activity are: "+ br +prettyList(emptySites)

    if missingSites != None:
        print br + br + "The non reporting storage resources are: "+ br +prettyList(missingSites)

    if allSites != None:
        print br + br + "Sites that are registered but have not registered/advertised a SRMV2 service are: " + br +prettyList(extraSites)
    if reportingDisabled != None:
        print br + br + "The disabled storage resources that are reporting: " + br + \
            prettyList(reportingDisabled)

    sys.stdout = old_stdout
    ret = stdout.getvalue()
    return ret

# HK TransferVO Apr 26 2013 BEGIN
def DataTransferSumupTrVO(range_end = datetime.date.today(),
                range_begin = None,
                output = "text",
                header = True):
# HK TransferVO Apr 26 2013
    LogToFile("#######################\n## def DataTransferSumupTrVO")
    old_stdout = sys.stdout
    sys.stdout = stdout = StringIO()
    ret = ""

    br = "\n" # line break
    if(output == "html"):
       br = "<br>" # line break

    title = """\
OSG Data transfer summary for  %s - %s (midnight UTC - midnight UTC)
including all data that transferred in that time period.
Deltas are the differences with the previous period.\n"""

    #title += "\nFor all data transferred between %s and %s (midnight, UTC)\n"
    #title += "\nFor all data transferred in the above time period.\n"

    print title % (range_begin, range_end)
 
    if not gGrid or gGrid.lower() == 'local':
        try:
            gridDisplayName = gConfig.get("local", "grid_name")
        except:
            gridDisplayName = "OSG"
    else:
        gridDisplayName = gGrid 

    if not range_end:
        if not range_begin:
            range_end = datetime.date.today()
        else:
            range_end = range_begin + datetime.timedelta(days=+1)
    if not range_begin:
        range_begin = range_end + datetime.timedelta(days=-1)
    timediff = range_end - range_begin

    regSites = GetListOfOSGSEs();
    disabledSites = GetListOfDisabledOSGSites()
    reportingSitesDate = GetSiteLastReportingDate(range_begin, True)

    pingSites = []
    for data in reportingSitesDate:
        if ( len(data) > 0 ):
           (name,lastreport) = data.split("\t")
           pingSites.append(name)

    exceptionSites = ['AGLT2_CE_2', 'BNL-LCG2', 'BNL_ATLAS_1', 'BNL_ATLAS_2',
        'FNAL_GPGRID_2', 'USCMS-FNAL-XEN', 'USCMS-FNAL-WC1-CE2',
        'USCMS-FNAL-WC1-CE3', 'USCMS-FNAL-WC1-CE4', 'BNL_LOCAL', 'BNL_OSG',
        'BNL_PANDA', 'GLOW-CMS', 'UCSDT2-B', 'Purdue-Lear' ]

    reportingSites = GetListOfDataTransferReportingSites(range_begin,range_end)

    # a super list of all sites to be used as a reference to restore lists back to their original case after finding common entries between one or more lists using a case insensitive search
    completeSiteList = list(set(regSites) | set(exceptionSites) | set(reportingSites) | set(pingSites) | set(disabledSites))

    allSites = None
    if regSites != None:
        allSites = restoreOriginalCase([name for name in listToLower(regSites) if name not in listToLower(exceptionSites)], completeSiteList)

    missingSites, emptySites = None, None
    if allSites:
        missingSites = restoreOriginalCase([name for name in listToLower(allSites) if name not in \
            listToLower(reportingSites) and name not in listToLower(pingSites)], completeSiteList)
        emptySites = restoreOriginalCase([name for name in listToLower(allSites) if name not in listToLower(reportingSites) \
            and name in listToLower(pingSites)], completeSiteList)
 
    extraSites = restoreOriginalCase([name for name in listToLower(reportingSites) if listToLower(allSites) and name not in \
        listToLower(allSites) and listToLower(disabledSites) and name not in listToLower(disabledSites)], completeSiteList)
    knownExtras = restoreOriginalCase([name for name in listToLower(extraSites) if name in listToLower(exceptionSites) and \
        name not in listToLower(regSites)], completeSiteList)
    extraSites = restoreOriginalCase([name for name in listToLower(extraSites) if name not in listToLower(exceptionSites)], completeSiteList)

    reportingDisabled = None
    if disabledSites != None:
        reportingDisabled = restoreOriginalCase([name for name in listToLower(reportingSites) if name in \
            listToLower(disabledSites)], completeSiteList)

    if allSites != None:
        print br + "As of %s, there are %s registered SRMv2 %s storage resources." % \
            (DateToString(datetime.date.today(),False),
            prettyInt(len(allSites)), gridDisplayName)

    #print "\nBetween %s - %s (midnight - midnight UTC):\n" % \
    #    (DateToString(range_begin, False), DateToString(range_end, False))
                                                               
    n = len(reportingSites)
    print prettyInt(n)+" storage resources reported" + br

    (njobs, totalSize, avgRate, duration) = GetDataTransferTotals(range_begin,range_end)

    print br + "Total number of transfers: " + niceNum(njobs)
    print br + "Total transfer size: "+niceNum(totalSize/(1024*1024))+ " TiB" + br

    if reportingSites != None and extraSites != None and knownExtras != None \
            and allSites != None:
        n = len(reportingSites)-len(extraSites)-len(knownExtras)
        print br + "%s registered storage resources reported some activity (%s%% of %s storage resources)" % \
            (prettyInt(n), niceNum(n*100/len(allSites),1), gridDisplayName)

    if emptySites != None and allSites != None:
        n = len(emptySites)
        print br + "%s registered storage resources have reported but have no activity (%s%% " \
            "of %s storage resources)" % (prettyInt(n), niceNum(n*100/len(allSites), 1),
            gridDisplayName)

    if missingSites != None and allSites != None:
        n = len(missingSites)
        print br + "%s registered storage resources have NOT reported (%s%% of %s storage resources)" % \
            (prettyInt(n), niceNum(n*100/len(allSites),1), gridDisplayName)

    print br
    
    n = len(extraSites);
    if not gGrid or gGrid.lower() != "local":
        print br + prettyInt(n)+" non-sanctioned non-registered storage resources reported " \
            "(might indicate a discrepancy between OIM and Gratia)."
    elif allSites != None:
        print br + prettyInt(n)+" non-sanctioned non-registered storage resources reported."

    if reportingDisabled != None: 
        n = len(reportingDisabled)
        print br + prettyInt(n)+" disabled storage resources have reported." + br

    # data transfer table HK TransferVO Apr 26 2013
    print br + DataTransferReportTrVO(range_end, range_begin, output, True, gWithPanda)
    
    if emptySites != None:
        print br + "The storage resources with no activity are: "+ br +prettyList(emptySites)

    if missingSites != None:
        print br + br + "The non reporting storage resources are: "+ br +prettyList(missingSites)

    if allSites != None:
        print br + br + "Sites that are registered but have not registered/advertised a SRMV2 service are: " + br +prettyList(extraSites)
    if reportingDisabled != None:
        print br + br + "The disabled storage resources that are reporting: " + br + \
            prettyList(reportingDisabled)

    sys.stdout = old_stdout
    ret = stdout.getvalue()
    return ret

# HK TransferVO Apr 26 2013 END


def RangeSummup(range_end = datetime.date.today(),
                range_begin = None,
                output = "text",
                header = True):
    LogToFile("#######################\n## def RangeSummup")
    ret = ""

    br = "\n" # line break
    if(output == "html"):
       br = "<br>" # line break

    if not gGrid or gGrid.lower() == 'local':
        try:
            gridDisplayName = gConfig.get("local", "grid_name")
        except:
            gridDisplayName = ""
    else:
        gridDisplayName = 'OSG'

    if not range_end:
        if not range_begin:
            range_end = datetime.date.today()
        else:
            range_end = range_begin + datetime.timedelta(days=+1)
    if not range_begin:
        range_begin = range_end + datetime.timedelta(days=-1)
#    else:
#        range_begin = datetime.date(*time.strptime(range_begin, "%Y/%m/%d")[0:3])
    timediff = range_end - range_begin

    LogToFile("#---------------------------------")
    LogToFile("#--- Site Reporting") 
    LogToFile("#---------------------------------")
    exceptionSites = ['AGLT2_CE_2', 'BNL-LCG2', 'BNL_ATLAS_1', 'BNL_ATLAS_2',
        'FNAL_GPGRID_2', 'USCMS-FNAL-XEN', 'USCMS-FNAL-WC1-CE2',
        'USCMS-FNAL-WC1-CE3', 'USCMS-FNAL-WC1-CE4', 'BNL_LOCAL', 'BNL_OSG',
        'BNL_PANDA', 'GLOW-CMS', 'UCSDT2-B', 'Purdue-Lear' ]
    regSites      = GetListOfOSGSites();
    disabledSites = GetListOfDisabledOSGSites()
    LogToFile("Registered Sites: %s\n------------" % regSites)
    LogToFile("Disabled Sites: %s\n----------" % disabledSites)
    LogToFile("Exception Sites: %s\n----------" % exceptionSites)
    reportingSitesDate = GetSiteLastReportingDate(range_begin, True)
    pingSites = []
    for data in reportingSitesDate:
        if ( len(data) > 0 ):
           (name,lastreport) = data.split("\t")
           pingSites.append(name)

    reportingSites = GetListOfReportingSites(range_begin,range_end)
    allSites = None
    if regSites != None:
        allSites = [name for name in regSites if name not in exceptionSites]

    missingSites, emptySites = None, None
    if allSites:
        missingSites = [name for name in allSites if name not in \
            reportingSites and name not in pingSites]
        emptySites = [name for name in allSites if name not in reportingSites \
            and name in pingSites]
    
    extraSites = [name for name in reportingSites if allSites and name not in \
        allSites and disabledSites and name not in disabledSites]
    knownExtras = [name for name in extraSites if name in exceptionSites and \
        name not in regSites]
    extraSites = [name for name in extraSites if name not in exceptionSites]

    reportingDisabled = None
    if disabledSites != None:
        reportingDisabled = [name for name in reportingSites if name in \
            disabledSites]

    #print allSites
    #print reportingSites
    #print missingSites
    #print extraSites
    if allSites != None:
        ret += "As of %s, there are %s registered %s sites." % \
            (DateToString(datetime.date.today(),False),
            prettyInt(len(allSites)), gridDisplayName)

    ret += (br + br + "Between %s - %s (midnight - midnight UTC):" + br) % \
        (DateToString(range_begin, False), DateToString(range_end, False))
                                                               
    n = len(reportingSites)
    ret += prettyInt(n)+" sites reported" + br

    [njobs,wallduration,div] = GetTotals(range_begin,range_end)
    if (njobs != "NULL"):
       njobs = int(njobs);
       wallduration = float(wallduration)
       div = float(div)
    else:
       njobs = 0
       wallduration = 0
       div = 1
    
    ret += (br + "Total number of jobs: "+prettyInt(njobs))
    ret += (br + "Total wall duration: "+niceNum( wallduration / 3600, 1 )+ " hours")
    ret += (br + "Total cpu / wall duration: "+niceNum(div,0.01))

    if reportingSites != None and extraSites != None and knownExtras != None \
            and allSites != None:
        n = len(reportingSites)-len(extraSites)-len(knownExtras)
        ret += (br + "%s registered sites reported (%s%% of %s sites)" % \
            (prettyInt(n), niceNum(n*100/len(allSites),1), gridDisplayName))

    if missingSites != None and allSites != None:
        n = len(missingSites)
        ret += (br + "%s registered sites have NOT reported (%s%% of %s sites)" % \
            (prettyInt(n), niceNum(n*100/len(allSites),1), gridDisplayName))

    if emptySites != None and allSites != None:
        n = len(emptySites)
        ret += (br + "%s registered sites have reported but have no activity (%s%% " \
            "of %s sites)" % (prettyInt(n), niceNum(n*100/len(allSites), 1),
            gridDisplayName))

    #print
    
    n = len(extraSites);
    if not gGrid or gGrid.lower() != "local":
        ret += (br + prettyInt(n)+" non-sanctioned non-registered sites reported " \
            "(might indicate a discrepancy between OIM and Gratia).")
    elif allSites != None:
        ret += (br + prettyInt(n)+" non-sanctioned non-registered sites reported.")

    #n = len(knownExtras);
    #print prettyInt(n)+" sanctioned non-registered sites reported"

    if reportingDisabled != None: 
        n = len(reportingDisabled)
        ret += (br + prettyInt(n)+" disabled sites have reported.")

    #print "\nThe reporting sites are:\n"+prettyList(reportingSites)
    #print "\nThe registered sites are:\n"+prettyList(allSites)
    
    if emptySites != None:
        ret += (br + br + "The sites with no activity are: " + br +prettyList(emptySites))

    if missingSites != None:
        ret += (br + br + "The non reporting sites are: "+ br +prettyList(missingSites))
    #print "\nThe sanctioned non registered sites are: \n"+prettyList(knownExtras)

    if allSites != None:
        ret += (br + br + "The non registered sites are: "+ br +prettyList(extraSites))
    if reportingDisabled != None:
        ret += (br + br + "The disabled sites that are reporting: "+ br + \
            prettyList(reportingDisabled))

    LogToFile("#------------------------------")
    LogToFile("#--- VO Reporting")
    LogToFile("#------------------------------")
    regVOs       = GetListOfRegisteredVO('Active',range_begin,range_end)
    expectedNoActivity = GetListOfRegisteredVO('Disabled',range_begin,range_end)
    expectedNoActivityAlt = GetListOfRegisteredVO('Enabled', range_begin, range_end)

    reportingVOs = GetReportingVOs(range_begin, range_end)
    if expectedNoActivity and expectedNoActivityAlt:
        expectedNoActivity += expectedNoActivityAlt
    elif expectedNoActivity == None:
        expectedNoActivity = expectedNoActivityAlt

    emptyVO = None
    if regVOs != None:
        emptyVO = [name for name in regVOs if name not in reportingVOs and \
            (not expectedNoActivity or name not in expectedNoActivity)]
    if emptyVO:
        ret += (br + br + "Active VOs with no recent activity are:" + br +prettyList(sorted(emptyVO)))

    if expectedNoActivity != None:
        ret += (br + br + "The following VOs are expected to have no activity:"+ br + \
            prettyList(sorted([name for name in expectedNoActivity if name not in reportingVOs])))

    if regVOs != None:
        nonregVO = sorted([name for name in reportingVOs if name not in regVOs])
        ret += (br + br + "The non-registered VOs with recent activity are:" + br + \
            prettyList(nonregVO))
    
    ret += br 

    if(output == "html"):
       return "<pre>"+ret+"</pre>"
    return ret


def findOriginalEntry(toFind,list):
   if(len(list) > 0):
      for entry in list:
         if entry.lower() == toFind.lower():
            return entry

def listToLower(list):
   ret = []
   if(len(list) > 0):
      for entry in list:
         if(entry != None):
            ret.append(entry.lower())
   return ret

def restoreOriginalCase(listToModify,referenceList):
   ret = []
   if(len(listToModify) > 0):
      for entry in listToModify:
            val = findOriginalEntry(entry,referenceList)
            if(val != None):
                ret.append(val)
   return ret


def NonReportingSites(
                when = datetime.date.today(),
                output = "text",
                header = True):
    LogToFile("#######################\n## def NonReportingSites")

    old_stdout = sys.stdout
    sys.stdout = stdout = StringIO()
    ret = ""

    print "This report indicates which sites Gratia has heard from or have known activity\nsince %s (midnight UTC)\n" % ( DateToString(when,False) )

    regSites = GetListOfOSGSites();
    regVOs = GetListOfRegisteredVO('Active',when,datetime.date.today())
    exceptionSites = ['AGLT2_CE_2', 'BNL-LCG2', 'BNL_ATLAS_1', 'BNL_ATLAS_2',
        'FNAL_GPGRID_2', 'USCMS-FNAL-XEN', 'USCMS-FNAL-WC1-CE2',
        'USCMS-FNAL-WC1-CE3', 'USCMS-FNAL-WC1-CE4', 'BNL_LOCAL', 'BNL_OSG',
        'BNL_PANDA', 'GLOW-CMS', 'UCSDT2-B', 'Purdue-Lear' ]

    allSites = [name for name in regSites if name not in exceptionSites]

    reportingVOs = GetLastReportingVOs(when)
    reportingSitesDate = GetSiteLastReportingDate(when,True)
    stoppedSitesDate = GetSiteLastReportingDate(when,False)
    activitySitesDate = GetSiteLastActivity(when);

    reportingSites = []
    stoppedSites = []
    dates = {}
    for data in reportingSitesDate:
        (name,lastreport) = data.split("\t")
        reportingSites.append(name)
        dates[name] = lastreport
    for data in stoppedSitesDate:
        (name,lastreport) = data.split("\t")
        dates[name] = lastreport
        stoppedSites.append(name);

    # a super list of all sites to be used as a reference to restore lists back to their original case after finding common entries between one or more lists using a case insensitive search
    completeSiteList = list(set(regSites) | set(exceptionSites) | set(reportingSites) | set(stoppedSites))

    stoppedSites = restoreOriginalCase([name for name in listToLower(stoppedSites) if name in listToLower(allSites)], completeSiteList)
    missingSites = restoreOriginalCase([name for name in listToLower(allSites) if name not in listToLower(reportingSites) and name not in listToLower(stoppedSites)], completeSiteList)
    extraSites = restoreOriginalCase([name for name in listToLower(reportingSites) if name not in listToLower(allSites)], completeSiteList)
    knownExtras = restoreOriginalCase([name for name in listToLower(extraSites) if name in listToLower(exceptionSites) and name not in listToLower(regSites)], completeSiteList)
    extraSites = restoreOriginalCase([name for name in listToLower(extraSites) if name not in listToLower(exceptionSites)], completeSiteList)

    print "As of "+DateToString(datetime.date.today(),False) +", there are "+prettyInt(len(allSites))+" registered OSG sites"

    n = len(reportingSites)
    ne = len(knownExtras);
    print prettyInt(n)+" sites reported (including "+prettyInt(ne)+" sanctioned non registered sites)\n"

    n = len(reportingSites)-len(extraSites)-len(knownExtras)
    print prettyInt(n)+" registered sites reported ("+niceNum(n*100/len(allSites),1)+"% of OSG Sites)"

    n = len(stoppedSites)
    print prettyInt(n)+" registered sites have stopped reporting ("+niceNum(n*100/len(allSites),1)+"% of OSG Sites)"
    
    n = len(missingSites);
    print prettyInt(n)+" registered sites have never reported ("+niceNum(n*100/len(allSites),1)+"% of OSG Sites)"

    print
    
    n = len(extraSites);
    print prettyInt(n)+" non-sanctioned non-registered sites reported (might indicate a discrepancy between OIM and Gratia)"

    n = len(knownExtras);
    print prettyInt(n)+" sanctioned non-registered sites reported"

    #print "\nThe reporting sites are:\n"+prettyList(reportingSites)
    #print "\nThe registered sites are:\n"+prettyList(allSites)
    
    print "\nThe sanctioned non registered sites are: \n"+prettyList(knownExtras)
    print "\nThe non registered sites are: \n"+prettyList(extraSites)

    #expectedNoActivity = ['sdss']
    expectedNoActivity = GetListOfRegisteredVO('Disabled',when,datetime.date.today())
    expectedNoActivity.extend(GetListOfRegisteredVO('Enabled',when,datetime.date.today()))
    emptyVO = [name for name in regVOs if name not in reportingVOs and name not in expectedNoActivity]
    nonregVO = [name for name in reportingVOs if name not in regVOs]
    #print "\nActive VOs with no recent activity are:\n"+prettyList(emptyVO)
    #print "\nThe following VOs are expected to have no activity:\n"+prettyList([name for name in expectedNoActivity if name not in reportingVOs])
    #print "\nThe non-registered VOs with recent activity are:\n"+prettyList(nonregVO)

    print "\nThe non reporting sites are: " # \n"+prettyList(missingSites)
    for name in missingSites:
        if len(name)>15:
            delim = "\t"
        else:
            delim = "\t\t"
        if not dates.has_key(name):
            print name+" :"+delim+"never reported or inactive"
    for data in stoppedSitesDate:        
        (name,lastreport) = data.split("\t")
        if name in allSites:
            if len(name)>15:
                delim = "\t"
            else:
                delim = "\t\t"
            print name+":"+delim+lastreport

    print "\nThe sites with no (known) recent activity:"
    for data in activitySitesDate:
        (name,lastreport) = data.split("\t")
        if name in allSites:
            if len(name)>=14:
                delim = "\t"
            if len(name)>=7:
                delim = "\t\t"
            else:
                delim = "\t\t\t"
            print name+":"+delim+lastreport
    
    sys.stdout = old_stdout
    ret = stdout.getvalue()
    if(output == "html"):
       ret = ret.replace("\n","<br>");
    return ret

def LongJobs(range_end = datetime.date.today(),
            range_begin = None,
            output = "text",
            header = True):
    LogToFile("#######################\n## def LongJobs")

    old_stdout = sys.stdout
    sys.stdout = stdout = StringIO()
    ret = ""
    br = "\n"

    if(output == "html"):
        print "<table><tr><td>"
        br = "<br>"

    print ("This report is a summary of long running jobs that finished between %s - %s (midnight - midnight UTC):"+br) % ( DateToString(range_begin,False),
                                                                        DateToString(range_end,False) )
    if(output == "html"):
        print "</td></tr></table>"

    sys.stdout = old_stdout
    ret = stdout.getvalue()

    ret += RangeLongJobs(range_end,range_begin,output,header)

    return ret

def CMSProd(range_end = datetime.date.today(),
            range_begin = None,
            output = "text"):
    LogToFile("#######################\n## def CMSProd")

    if not range_end:
        if not range_begin:
            range_end = datetime.date.today()
        else:
            range_end = range_begin + datetime.timedelta(days=+1)
    if not range_begin:
        range_begin = range_end + datetime.timedelta(days=-1)
#    else:
#        range_begin = datetime.date(*time.strptime(range_begin, "%Y/%m/%d")[0:3])
    timediff = range_end - range_begin

    print "For jobs finished between %s and %s (midnight UTC)" % ( DateToString(range_begin,False),
                                                                   DateToString(range_end,False) )
    print """Number of wallclock hours during the previous 7 days consumed by users FQANs
reported via USCMS-FNAL-WC1-CE/CE2/CE3/CE4:"""
    print

    data = CMSProdData(range_begin,range_end)
    print "%(fqan)-43s %(value)12s" % \
              {"fqan":"FQAN", "value":"Hours"}
    print "%(fqan)-43s %(value)12s" % \
              {"fqan":"------------", "value":"----------"}
    for line in data:
       (fqan, value) = line.split("\t")
       if fqan == "NULL":
         fqan = "Total"
       print "%(fqan)-45s %(value)10s" % \
              {"fqan":fqan, "value":niceNum(float(value))}

def SoftwareVersionData(schema,begin,end):
   select = """SELECT Si.SiteName, M.ProbeName, S.Name, S.Version, M.ServerDate as StartedOn, Pr.CurrentTime as LastReport
from """+schema+""".ProbeSoftware P, """+schema+""".ProbeDetails_Meta M, """+schema+""".Software S, """+schema+""".Probe Pr, """+schema+""".Site Si
where M.dbid = P.dbid and S.dbid = P.softid and M.probeid = Pr.probeid and Pr.siteid = Si.siteid
and Pr.active = true
and M.ServerDate <=  \"""" + DateTimeToString(end) + """\" 
group by Si.SiteName, ProbeName, S.Name, S.Version

order by Si.SiteName, ProbeName, S.Name, ServerDate"""

#and    Pr.CurrentTime >= \"""" + DateTimeToString(begin) + """\" 
#and    Pr.CurrentTime < \"""" + DateTimeToString(end) + """\" 

   return RunQueryAndSplit(select);


class SoftwareVersionConf(GenericConf):
   title = """This reports list the current version of the Gratia probe(s) installed and reporting at each site as of %s.

Only sites registered in OIM are listed.

The recommended probe versions are those available in VDT 1.10.1n or higher and will be listed below as:
Probe Library: v1.04.4d
Condor Probe:  v1.04.4d
PBS/LSF Probe: v1.04.4d
glexec Probe:  v1.04.4d

Note that the '+' after the release number indicates that the same version of the probe has been available in the
given release up to the current release.

"""
   headers = ("","Site","Soft","Release","Last Contact","Probe name")
   formats = {}
   start = {}
   startlines = {}
   lines = {}
   endlines = {}
   end = {}
   num_header = 2
   
   def __init__(self, header = False):
      self.formats["csv"] = "%s,\"%s\",%s,%s,%s,%s  "
      self.formats["text"] = "%6s | %-20s | %-14s | %-12s | %-12s | %s"
      self.formats["html"] = "<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
      self.start["csv"] = ""
      self.start["text"] = ""
      self.start["html"] = "<html><body><br><br>"
      self.startlines["csv"] = ""
      self.startlines["text"] = "----------------------------------------------------------------------------------------------------"
      self.startlines["html"] = "<br><br><table border=\"1\" cellpadding=\"10\" cellspacing=\"0\">"
      self.endlines["csv"] = ""
      self.endlines["text"] = "" # self.startlines["text"]
      self.endlines["html"] = "</table>"
      self.end["csv"] = ""
      self.end["text"] = ""
      self.end["html"] = "</html></body>"

      if (not header) :  self.title = ""

   def GetData(self,start,end):
      LogToFile("#######################\n## SoftwareVersionConf")
      global gMySQLConnectString
      res1 = SoftwareVersionData("gratia",start,end)

      keepConnectionValue = gMySQLConnectString
      gMySQLConnectString = gMySQLFermiConnectString

      res2 = SoftwareVersionData("fermi_osg",start,end)

      gMySQLConnectString = keepConnectionValue;
      return res1 + res2;
   

def SoftwareVersion(range_end = datetime.date.today(),
                range_begin = None,
                output = "text",
                header = True):
   
   if (range_end == None or range_end > datetime.date.today()):
      range_end = datetime.date.today()
   if (range_begin == None):
      range_begin = range_end + datetime.timedelta(days=-31)
   elif (range_begin > range_end):
      range_begin = range_end + datetime.timedelta(days=-31)

   conf = SoftwareVersionConf(header)
   lines = conf.GetData(range_begin,range_end)
   values  = {}

   exceptionSites = ['AGLT2_CE_2','BNL-LCG2','BNL_ATLAS_1', 'BNL_ATLAS_2','USCMS-FNAL-XEN','USCMS-FNAL-WC1-CE2', 'USCMS-FNAL-WC1-CE3', 'USCMS-FNAL-WC1-CE4', 'BNL_LOCAL', 'BNL_OSG', 'BNL_PANDA', 'GLOW-CMS', 'UCSDT2-B', 'Purdue-Lear' ]
   #exceptionSites = ['BNL_ATLAS_2', 'USCMS-FNAL-WC1-CE2', 'USCMS-FNAL-WC1-CE3', 'USCMS-FNAL-WC1-CE4', 'Generic Site', 'BNL_LOCAL', 'BNL_OSG', 'BNL_PANDA', 'GLOW-CMS', 'UCSDT2-B']
   sites = [name for name in GetListOfOSGSites()  if name not in exceptionSites]
   reportingSites = GetListOfReportingSites(range_begin,range_end);

   versions = {
     "Gratia": { "1.65":"v0.27.[1-2]","1.67":"v0.27b","1.68":"v0.28","1.69":"v0.30","1.69.2.1":"v0.32.1","1.78":"v0.32.2",
                 "1.84":"v0.34.[1-8]","1.85":"v0.34.[9-10]","1.86":"v0.36","1.90":"v0.38.4","1.91":"v1.00.1",
                 "1.93":"v1.00.3","1.95":"v1.00.5","1.100":"v1.02.01",
                 "3002":"v1.04.1","3266":"v1.04.3","3316":"v1.04.4c"},
     "condor_meter.pl" : { "$""Revision: 1.29 $  (tag Unknown)":"v0.99", "$""Revision: 1.31 $  (tag Unknown)":"v1.00.3+", 
                           "$""Revision: 1.32 $  (tag 1.02.1-5)":"v1.02.1", "$""Revision: 3277 $  (tag 1.04.3c-1)":"v1.04.3",
                           "$""Revision: 3277 $  (tag 1.04.4d-1)":"v1.04.4d" },
     "pbs-lsf.py" : { "1.7 (tag )":"v1.00.1+", "1.8 (tag )":"v1.00.x", "1.9 (tag 1.02.1-5)":"v1.02.1", "3002 (tag 1.04.3c-1)":"v1.04.3",
                      "3002 (tag 1.04.4d-1)":"v1.04.4d"},
     "glexec_meter.py": {"1.9 (tag )":"v1.00.[3-5]", "1.9 (tag v1-00-3a-1)":"v1.00.3a-1+", "1.10 (tag 1.02.1-5)":"v1.02.01",
                         "3002 (tag 1.04.3a-1)":"v1.04.3","3274 (tag 1.04.3c-1)":"v1.04.03c","3274 (tag 1.04.4d-1)":"v1.04.4d"},
     "GridftpTransferProbeDriver.py" : { "1.2 (tag v0-3)":"v0-3" }
     }
   renames = {
     "Gratia":"Probe Library",
     "condor_meter.pl":"Condor Probe",
     "pbs-lsf.py":"Pbs/Lsf Probe",
     "glexec_meter.py":"Glexec Probe",
     "GridftpTransferProbeDriver.py":"GridFtp Probe"
     }
     
   for site in sites:
      values[site] = {}
      
   for row in lines:
      row = row.split('\t')
      if (len(row) < 2): 
         continue
      site = row[0]
      probe= row[1]

      if (values.has_key(site)):
         current = values[site]
         if (current.has_key(probe)):
            pcurrent = current[probe]
         else:
            pcurrent = [[],{}]
         pcurrent[0] = row[5] # LastReportTime
         pcurrent[1][row[2]] = [ row[3], row[4] ]
       
         current[probe] = pcurrent
         values[site] = current
   
   msg = ""
   msg = msg + conf.start[output] + "\n"
   msg = msg + conf.title % (DateToString(range_end,False)) + "\n"
   
   msg = msg + conf.startlines[output] + "\n"
   if (output == "html"):
      msg = msg + conf.formats[output].replace("td","th") % conf.headers + "\n"
   else:
      msg = msg + conf.formats[output] % conf.headers + "\n"
   if (output == "text"):
      msg = msg + conf.startlines[output] + "\n"
      
   outer = 0
   inner = 0
   for key,data in sortedDictValues(values):
      outer = outer + 1
      inner = 0
      if (len(data)==0):
         if (key in reportingSites):
            #print key,"has reported but no information is available about the probe."
            msg = msg + conf.formats[output] % ("%3d.%-2d" % (outer,inner),key,"n/a","n/a","n/a/","has reported but no information is available about the probe(s).") + "\n"
         else:
            msg = msg + conf.formats[output] % ("%3d.%-2d" % (outer,inner),key,"n/a","n/a","n/a","has not reported") + "\n"
            #print key,"has not reported."
         if (output == "text"):
            msg = msg + conf.startlines[output] + "\n"
      else:
         # print key,"had",len(data),"probe(s) reporting."
         for probename,probeinfo in sortedDictValues(data):
            #print "   ",probename
            inner = inner + 1
            lastReportTime = probeinfo[0]
            for soft,softinfo in probeinfo[1].iteritems():
               v = softinfo[0]
               if (soft == "Condor"):
                  v = v.split(' ')[0]
               if (soft == "LSF" and v == "sh: bsub: command not found "):
                  v = "Version information not available"
               if (versions.has_key(soft)):
                  if (versions[soft].has_key(v)):
                     v = versions[soft][v]
               if (renames.has_key(soft)):
                  soft = renames[soft]
               #print "      ",soft,":",v
               msg = msg + conf.formats[output] % ("%3d.%-2d" % (outer,inner), key,soft,v,lastReportTime[0:10],probename) + "\n"
            #print 
         if (output == "text"):
            msg = msg + conf.startlines[output] + "\n"
      #print
      
   msg = msg + conf.endlines[output] + "\n"
   msg = msg + conf.end[output] + "\n"
   return msg

class NewUsersConf(GenericConf):
   title = """\
The following users's CN very first's job on on the OSG site finished 
between %s - %s (midnight UTC - midnight UTC):
"""
   headers = ("","User","VO","Probe Name","Site Name","End Date Of First Job")
   titleformats = {}
   formats = {}
   start = {}
   startlines = {}
   midlines = {}
   lines = {}
   endlines = {}
   end = {}
   num_header = 2
   
   def __init__(self, header = False):
      self.formats["csv"] = "%s,\"%s\",\"%s\",\"%s\",\"%s\",%s"
      self.formats["text"] = "%2s | %-20s | %-15s | %-30s | %-20s"
      self.formats["html"] = "<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
      self.titleformats["csv"] = self.formats["csv"]
      self.titleformats["text"] = self.formats["text"]
      self.titleformats["html"] = self.formats["html"].replace("td","th")
      self.start["csv"] = ""
      self.start["text"] = ""
      self.start["html"] = "<html><body><br><br>"
      self.startlines["csv"] = ""
      self.startlines["text"] = "----------------------------------------------------------------------------------------------------"
      self.startlines["html"] = "<br><br><table border=\"1\" cellpadding=\"10\" cellspacing=\"0\">"
      self.midlines["csv"] = ""
      self.midlines["text"] = self.startlines["text"] + '\n'
      self.midlines["html"] = ""
      self.endlines["csv"] = ""
      self.endlines["text"] = "" # self.startlines["text"]
      self.endlines["html"] = "</table>"
      self.end["csv"] = ""
      self.end["text"] = ""
      self.end["html"] = "</html></body>"

      if (not header) :  self.title = ""

   def SelectValues(self, output,values) : 
      if (output == "csv"):
         return values
      elif (output == "html"):
         return ( values[0], values[1], values[2], values[3], values[4])
      else:
         return ( values[0], values[4], values[2], values[3], values[1])
         
   def GetData(self,start,end):
      LogToFile("#######################\n## NewUsersConf")
      return GetNewUsers(start,end)

def NewUsers(range_end = datetime.date.today(),
                range_begin = None,
                output = "text",
                header = True):
   if not range_end:
      if not range_begin:
         range_end = datetime.date.today()
      else:
            range_end = range_begin + datetime.timedelta(days=+1)
   if not range_begin:
      range_begin = range_end + datetime.timedelta(days=-1)

   timediff = range_end - range_begin

   conf = NewUsersConf(header)
   newusers = GetNewUsers(range_begin,range_end)

   if len(newusers) > 0:
      msg = ""
      msg = msg + conf.start[output] + '\n'
      msg = msg + conf.title % (DateToString(range_begin,False), DateToString(range_end,False)) + "\n"

      msg = msg + conf.startlines[output] + "\n"
      msg = msg + conf.titleformats[output] % conf.SelectValues( output, conf.headers ) + "\n"
      msg = msg + conf.midlines[output]
      
      count = 0
      for line in newusers:
         (name,voname, probename, sitename, when, njobs) = line.split('\t')
         msg = msg + conf.formats[output] %  conf.SelectValues( output,  ("%2d"%count,name,voname,probename,sitename,when) ) + '\n'
         msg = msg + conf.midlines[output]
         count += 1

      msg = msg + conf.endlines[output] + "\n"
      msg = msg + conf.end[output] + "\n"
      return msg
   return ""

#
#
def TESTER():
    print """select S.VOName,sum(Njobs),sum(WallDuration) from 
(select VC.corrid, VO.VOName from VO, VONameCorrection VC where VC.void = VO.void and VO.VOName in ("atlas","chen") ) S
left join (select VOcorrid,sum(njobs) as njobs,sum(WallDuration) as WallDuration from MasterSummaryData M where '2008/11/10' <= EndTime and EndTime <= '2008/11/11' group by VOcorrid ) M on S.corrid = M.VOcorrid
group by S.VOName"""

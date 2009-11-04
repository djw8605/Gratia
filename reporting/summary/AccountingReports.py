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
    
    configFiles = "gratiareports.conf"
    
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
        if len(argv) > 1:
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
    return " VOName != \"unknown\" and \"" \
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
        filename = time.strftime("%Y-%m-%d") + ".log"
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
        if(gGrid != None and gGrid.lower() == "osg"): 
            select = AddOSGToQuery(select)
        LogToFile(select)
        # print "echo '" + select + "' | " + gMySQL + gMySQLConnectString
        return commands.getoutput("echo '" + select + "' | " + gMySQL + gMySQLConnectString )

def AddOSGToQuery(select):
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
                    query+="from " + part.split('where')[0] + " where Grid=\"OSG\" and " + string.join(part.split('where')[1:],'where ')
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
            location = 'http://myosg.grid.iu.edu/wizardsummary/xml?datasource=summary&summary_attrs_showservice=on&account_type=cumulative_hours&ce_account_type=gip_vo&se_account_type=vo_transfer_volume&start_type=7daysago&all_resources=on&gridtype=on&gridtype_1=on&service=on&service_1=on'
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
        location = 'http://myosg.grid.iu.edu/wizardsummary/xml?datasource=summary&summary_attrs_showservice=on&account_type=cumulative_hours&ce_account_type=gip_vo&se_account_type=vo_transfer_volume&start_type=7daysago&start_date=11%2F04%2F2009&end_type=now&end_date=11%2F04%2F2009&all_resources=on&gridtype=on&gridtype_1=on&service=on&service_3=on&active_value=1&disable_value=1'
        return GetListOfSites("//Resource/Name",location)
    else:
        try:
            return [i.strip() for i in gConfig.get("local", "active_sites").split(",")]
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

def GetListOfVOs(filter,voStatus,beginDate,endDate):
        name=""
        bd = str(beginDate).split("-") # begin date list
        ed = str(endDate).split("-") # end date list
        # date specific MyOSG url
        location = "http://myosg.grid.iu.edu/voactivation/xml?datasource=activation&start_type=specific&start_date=" + bd[1] +"%2F" + bd[2] + "%2F" + bd[0] + "&end_type=specific&end_date=" + ed[1] + "%2F" + ed[2] + "%2F" + ed[0] + "&all_vos=on&active_value=1"
        html = urllib2.urlopen(location).read()
        vos = []
        doc = libxml2.parseDoc(html)
        for resource in doc.xpathEval(filter):
           if resource.name == "Name":
              name = resource.content
           elif resource.name == "LongName":
              vos.append( (name,resource.content) )
        if(voStatus == 'Active'):
            location = 'http://myosg.grid.iu.edu/vosummary/xml?datasource=summary&summary_attrs_showdesc=on&all_vos=on&show_disabled=on&summary_attrs_showreporting_group=on&active=on&active_value=1'
            html = urllib2.urlopen(location).read()
            doc = libxml2.parseDoc(html)
            for resource in doc.xpathEval("/VOSummary/VO/ReportingGroups/ReportingGroup/Name"):
                vos.append((resource.content,'Additional VOs'))
        doc.freeDoc()
        return vos

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
    ret = []
    printederror = False
    for pair in allVos:
           try:
              (longname,description) = pair;  # pair.split(",");
           except:
              if not printederror:
                 LogToFile("Gratia Reports GetListOfRegisteredVO unable to parse the result of: "+cmd)
                 sys.stderr.write("Gratia Reports GetListOfRegisteredVO unable to parse data, one example is: "+pair+"\n")
                 printederror = True
              LogToFile("Gratia Reports GetListOfRegisteredVO unable to parse: "+pair)
              continue
           if ("/" in description):
               (voname,subname) = description.split("/");
               if (subname.lower() not in ret):
                  ret.append(subname.lower())
               else:
                  ret.append(description.lower())
           else:
               if (longname != "ATLAS" and longname!=""):
                  ret.append( longname.lower() );
    # And hand add a few 'exceptions!"
    if(voStatus == 'Active'):
            ret.append("usatlas")
            ret.append("other")
    return ret

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
                 " FROM "+schema+".JobUsageRecord_Report J, "+schema+".CPUInfo CpuInfo " + \
                 " where J.HostDescription=CpuInfo.NodeName " + CommonWhere() + \
                 " group by J.VOName; "
        result = RunQueryAndSplit(select)
        gMySQLConnectString = keepConnectionValue;
        return result

def CondorData():
        select = " SELECT J.VOName, sum((J.CpuUserDuration+J.CpuSystemDuration)) as cputime, " + \
                      " sum((J.CpuUserDuration+J.CpuSystemDuration)*0) as normcpu, " + \
                      " sum(J.WallDuration) as wall, sum(J.WallDuration*0) as normwall " + \
                 " FROM VOProbeSummary J " + \
                 " where 1=1 " + CommonWhere() + \
                 " group by VOName; "
        return RunQueryAndSplit(select)

def DailySiteData(begin,end):
        schema = gDBSchema[mainDB]
        
        select = " SELECT Site.SiteName, sum(NJobs), sum(J.WallDuration) " \
                + " from "+schema+".Site, "+schema+".Probe, "+schema+".VOProbeSummary J " \
                + " where VOName != \"unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
                + " and \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and J.ProbeName not like \"psacct:%\" " \
                + " group by Probe.siteid "
        return RunQueryAndSplit(select)

def DailyVOData(begin,end):
        schema = gDBSchema[mainDB]
            
        select = " SELECT J.VOName, Sum(NJobs), sum(J.WallDuration) " \
                + " from "+schema+".Site, "+schema+".Probe, "+schema+".VOProbeSummary J " \
                + " where VOName != \"unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
                + " and \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and J.ProbeName not like \"psacct:%\" " \
                + " group by J.VOName "
        return RunQueryAndSplit(select)

def DailySiteVOData(begin,end):
        schema = gDBSchema[mainDB]
        
        select = " SELECT Site.SiteName, J.VOName, sum(NJobs), sum(J.WallDuration) " \
                + " from "+schema+".Site, "+schema+".Probe, "+schema+".VOProbeSummary J " \
                + " where VOName != \"unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
                + " and \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and J.ProbeName not like \"psacct:%\" " \
                + " group by J.VOName, Probe.siteid order by Site.SiteName "
        return RunQueryAndSplit(select)

def DailyVOSiteData(begin,end):
        schema = gDBSchema[mainDB]
        
        select = " SELECT J.VOName, Site.SiteName, sum(NJobs), sum(J.WallDuration) " \
                + " from "+schema+".Site, "+schema+".Probe, "+schema+".VOProbeSummary J " \
                + " where VOName != \"unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
                + " and \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
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
                + " where VOName != \"unknown\" and \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
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
                + " where VOName != \"unknown\" and \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
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
                + " where VO.VOName != \"unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
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
                + " where VO.VOName != \"unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
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
                + " where VO.VOName != \"unknown\" and Probe.siteid = Site.siteid and J.ProbeName = Probe.probename" \
                + " and J.dbid = R.dbid and R.Description = \"ExitCode\" " \
                + " and " + VONameCorrectionJoin("J") \
                + selection \
                + " group by " + what + ",R.Value " \
                + " order by " + what
        return RunQueryAndSplit(select)
# Condor Exit Status

def CMSProdData(begin,end):
    schema = gDBSchema[mainDB]
    select = "select sum(WallDuration) from JobUsageRecord_Report  " \
            "where \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\" " \
            "and ResourceType = \"Batch\" and VOName = \"cms\" "\
            "and (LocalUserId = \"cmsprod\" or LocalUserId = \"cmsprd\") and SiteName = \"USCMS-FNAL-WC1-CE\" "

    select = "select (LocalUserId = \"cmsprod\" or LocalUserId = \"cmsprd\") as production, sum(WallDuration) from JobUsageRecord_Report  " \
            "where \""+ DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\" " \
            "and ResourceType = \"Batch\" and VOName = \"cms\" "\
            "and SiteName = \"USCMS-FNAL-WC1-CE\" " \
            "group by production"
    return RunQueryAndSplit(select)

def GetSiteVOEfficiency(begin,end):
    schema = gDBSchema[mainDB] + ".";

    select = """\
            select SiteName, lcase(VOName), sum(Njobs),sum(WallDuration),sum(CpuUserDuration+CpuSystemDuration)/sum(WallDuration) 
            from """+schema+"""VOProbeSummary, """+schema+"""Site, """+schema+"""Probe
            where VOName != \"unknown\" and VOName != \"other\" and
               Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename and 
               EndTime >= \"""" + DateToString(begin) + """\" and
               EndTime < \"""" + DateToString(end) + """\"
            group by Site.SiteName, lcase(VOName)
            """
    #print "Query = " + select;

    return RunQueryAndSplit(select);    

def GetVOEfficiency(begin,end):
    schema = gDBSchema[mainDB] + ".";

    select = """\
            select lcase(VOName), sum(Njobs),sum(WallDuration),sum(CpuUserDuration+CpuSystemDuration)/sum(WallDuration) 
            from """+schema+"""VOProbeSummary, """+schema+"""Site, """+schema+"""Probe
            where VOName != \"unknown\"  and VOName != \"other\" and
               Probe.siteid = Site.siteid and VOProbeSummary.ProbeName = Probe.probename and 
               EndTime >= \"""" + DateToString(begin) + """\" and
               EndTime < \"""" + DateToString(end) + """\"
            group by lcase(VOName)
            """
    #print "Query = " + select;

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
    headline = "For all jobs finished on %s (UTC)"
    headers = ("Site","Wall Succ Rate","Wall Success","Wall Failed","Success Rate","Success","Failed")
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
           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %-22s | %14s | %12s | %11s | %12s | %10s | %10s "
           self.lines["csv"] = ""
           self.lines["text"] = "--------------------------------------------------------------------------------------------------------------------"

           if (not header) :  self.title = ""
           self.CondorSpecial = CondorSpecial
           self.VOName = VOName
           if (groupby == "VO"):
               self.GroupBy = "VO.VOName"
               self.headers = ("VO","Wall Succ Rate","Wall Success","Wall Failed","Success Rate","Success","Failed")
               self.totalheaders = ["All VOs"]
           elif (groupby == "Both"):
               self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
               self.formats["text"] = "| %-22s | %-22s | %14s | %12s | %11s | %12s | %10s | %10s "
               self.lines["text"] = "---------------------------------------------------------------------------------------------------------------------------------------------"
               self.GroupBy = "Site.SiteName,VO.VOName"
               self.headers = ("Site","VO","Wall Succ Rate","Wall Success","Wall Failed","Success Rate","Success","Failed")
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
       return DailySiteJobStatusSummary(start,end,what=self.GroupBy,selection=self.ExtraSelect)
    
  
class DailySiteReportConf(GenericConf):
        title = "OSG usage summary (midnight to midnight UTC) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nThe number of jobs counted here includes only the jobs directly seen by batch system and does not include the request sent directly to a pilot job.\nThe Wall Duration includes the total duration of the the pilot jobs.\nDeltas are the differences with the previous day.\n(nr) after a VO name indicates that the VO is not registered with OSG.\n"
        headline = "For all jobs finished on %s (UTC)"
        headers = ("Site","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        num_header = 1
        formats = {}
        lines = {}
        totalheaders = ["All sites"]

        def __init__(self, header = False):
           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %-22s | %9s | %13s | %10s | %14s"
           self.lines["csv"] = ""
           self.lines["text"] = "---------------------------------------------------------------------------------------"

           if (not header) :  self.title = ""


        def GetData(self,start,end):
           return DailySiteData(start,end)      

class DailyVOReportConf(GenericConf):
        title = "OSG usage summary (midnight to midnight UTC) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nThe number of jobs counted here includes only the jobs directly seen by batch system and does not include the request sent directly to a pilot job.\nThe Wall Duration includes the total duration of the the pilot jobs.\nDeltas are the differences with the previous day.\n(nr) after a VO name indicates that the VO is not registered with OSG.\n"
        headline = "For all jobs finished on %s (UTC)"
        headers = ("VO","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        num_header = 1
        formats = {}
        lines = {}
        totalheaders = ["All VOs"]

        def __init__(self, header = False):
           self.formats["csv"] = ",%s,\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %-18s | %9s | %13s | %10s | %14s"
           self.lines["csv"] = ""
           self.lines["text"] = "-----------------------------------------------------------------------------------"

           if (not header) :  self.title = ""

        def GetData(self,start,end):
           return UpdateVOName( DailyVOData(start,end), 0 ,start, end)

class DailySiteVOReportConf(GenericConf):
        title = "OSG usage summary (midnight to midnight UTC) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nThe number of jobs counted here includes only the jobs directly seen by batch system and does not include the request sent directly to a pilot job.\nThe Wall Duration includes the total duration of the the pilot jobs.\nDeltas are the differences with the previous day.\n"
        headline = "For all jobs finished on %s (UTC)"
        headers = ("Site","VO","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        num_header = 2
        formats = {}
        lines = {}
        select = "=="
        totalheaders = ["All sites", "All VOs"]
        
        def __init__(self, header = False):
           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %-22s | %-14s | %9s | %13s | %10s | %14s"
           self.lines["csv"] = ""
           self.lines["text"] = "--------------------------------------------------------------------------------------------------------"

           if (not header) :  self.title = ""

        def GetData(self,start,end):
           return UpdateVOName(DailySiteVOData(start,end),1,start, end)  

class DailyVOSiteReportConf(GenericConf):
        title = "OSG usage summary (midnight to midnight UTC) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nThe number of jobs counted here includes only the jobs directly seen by batch system and does not include the request sent directly to a pilot job.\nThe Wall Duration includes the total duration of the the pilot jobs.\nDeltas are the differences with the previous day.\n(nr) after a VO name indicates that the VO is not registered with OSG.\n"
        headline = "For all jobs finished on %s (UTC)"
        headers = ("VO","Site","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        num_header = 2
        formats = {}
        lines = {}
        select = "=="
        totalheaders = ["All sites","All VOs"]
        
        def __init__(self, header = False):
           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %-14s | %-22s | %9s | %13s | %10s | %14s"
           self.lines["csv"] = ""
           self.lines["text"] = "--------------------------------------------------------------------------------------------------------"

           if (not header) :  self.title = ""

        def GetData(self,start,end):
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
        if not when:
            when = datetime.date.today()

        factor = 3600  # Convert number of seconds to number of hours

        if (output != "None") :
            if (what.title != "") :
                print what.title % ( DateToString(when,False) )
            if (what.headline != "") :
                print what.headline % (DateToString(when,False))
            print what.lines[output]
            print "    ", what.formats[output] % what.headers
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
               values = (site,vo,str(niceNum(wrate))+" %",niceNum(wsuccess),niceNum(wfailed),str(rate)+" %",success,failed)
            else: 
               values = (site,str(niceNum(wrate))+" %",niceNum(wsuccess),niceNum(wfailed),str(rate)+" %",success,failed)
            totaljobs = totaljobs + total
            totalsuccess = totalsuccess + success
            totalfailed = totalfailed + failed
            totalws = totalws + wsuccess
            totalwf = totalwf + wfailed
            if (output != "None") :
                     print "%3d " %(index), what.formats[output] % values
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
                    print "    ", what.formats[output] % ( what.totalheaders[0], what.totalheaders[1], str(totalwrate) + " %", niceNum(totalws),niceNum(totalwf), str(totalsuccess*100/totaljobs) + " %", niceNum(totalsuccess),niceNum(totalfailed))
                else:
                    tsrate = 0
                    if ( totaljobs > 0) : tsrate = totalsuccess*100/totaljobs
                    print "    ", what.formats[output] % ( what.totalheaders[0], str(totalwrate) + " %", niceNum(totalws),niceNum(totalwf), str(tsrate) + " %",niceNum(totalsuccess),niceNum(totalfailed))
                print what.lines[output]

        return result
            
        
def GenericDaily(what, when=datetime.date.today(), output = "text"):
        factor = 3600  # Convert number of seconds to number of hours

        if not when:
            when=datetime.date.today()

        if output != "None":
            if what.title:
                print what.title % ( DateToString(when,False) )
            if what.headline:
                print what.headline % (DateToString(when,False))
            print what.lines[output]
            print "    ", what.formats[output] % what.headers
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
                     values = (site,vo,niceNum(njobs), niceNum(wall),niceNum(njobs-oldnjobs),niceNum(wall-oldwall))
            else:
                     values = (site,niceNum(njobs), niceNum(wall),niceNum(njobs-oldnjobs),niceNum(wall-oldwall))
            if (output != "None") :
                     print "%3d " %(index), what.formats[output] % values
            result.append(values)       
                
        (oldnjobs,oldwall,s,v) = oldValues["total"]
        if (output != "None") :
                print what.lines[output]
                if (num_header == 0) :
                    print 
                elif (num_header == 2) :
                    print "    ",what.formats[output] % (what.totalheaders[0], what.totalheaders[1], niceNum(totaljobs), niceNum(totalwall), niceNum(totaljobs-oldnjobs), niceNum(totalwall-oldwall))
                else:
                    print "    ",what.formats[output] % (what.totalheaders[0], niceNum(totaljobs), niceNum(totalwall), niceNum(totaljobs-oldnjobs), niceNum(totalwall-oldwall))
                print what.lines[output]
        return result

        
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
  where VOName != \"unknown\" and 
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\" and
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

def DataTransferData(begin, end, with_panda = False):
    global gMySQLConnectString,gDBCurrent
    schema = gDBSchema[transferDB]
    gDBCurrent = transferDB
    keepConnectionValue = gMySQLConnectString
    gMySQLConnectString = gMySQLTransferConnectString
    select = "select T.SiteName, M.Protocol, sum(M.Njobs), sum(M.TransferSize * Multiplier) from " + schema + ".MasterTransferSummary M, " + schema + ".Probe P, " + schema + ".Site T, " + schema + ".SizeUnits su where M.StorageUnit = su.Unit and P.siteid = T.siteid and M.ProbeName = P.Probename and StartTime >= \"" + DateTimeToString(begin) + "\" and StartTime < \"" + DateTimeToString(end) + "\" and M.ProbeName not like \"psacct:%\" group by P.siteid, Protocol"
    result = RunQueryAndSplit(select)
    gMySQLConnectString = keepConnectionValue 
    return result

def RangeSiteData(begin, end, with_panda = False):
    schema = gDBSchema[mainDB]
    select = """\
select T.SiteName, sum(J.NJobs), sum(J.WallDuration)
  from """ + schema + ".Site T, " + schema + ".Probe P, " + schema + """.VOProbeSummary J
  where VOName != \"unknown\" and 
    P.siteid = T.siteid and
    J.ProbeName = P.probename and
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\" and
    J.ProbeName not like \"psacct:%\"
    group by P.siteid;"""
    if with_panda:
        panda_select = """\
select J.SiteName, sum(J.NJobs), sum(J.WallDuration)
  from """ + gDBSchema[dailyDB] + """.JobUsageRecord_Report J
  where VOName != \"unknown\" and 
    J.ProbeName != \"daily:goc\" and
    J.SiteName not in (select GT.SiteName from """ + gDBSchema[mainDB] + """.Site GT) and
    J.EndTime >= \"""" + DateTimeToString(begin) + """\" and
    J.EndTime < \"""" + DateTimeToString(end) + """\"
    group by J.SiteName;"""
        return RunQueryAndSplit(select) + RunQueryAndSplit(panda_select)
    else:
        return RunQueryAndSplit(select)

def RangeSiteVOData(begin, end, with_panda = False):
    schema = gDBSchema[mainDB]
    select = """\
select T.SiteName, J.VOName, sum(NJobs), sum(J.WallDuration)
  from """ + schema + ".Site T, " + schema + ".Probe P, " + schema + """.VOProbeSummary J
  where VOName != \"unknown\" and 
    P.siteid = T.siteid and
    J.ProbeName = P.probename and
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\" and
    J.ProbeName not like \"psacct:%\"
    group by T.SiteName,J.VOName
    order by T.SiteName,J.VOName;"""
    if with_panda:
        panda_select = """\
select J.SiteName, J.VOName, sum(J.NJobs), sum(J.WallDuration)
  from """ + gDBSchema[dailyDB] + """.JobUsageRecord_Report J
  where VOName != \"unknown\" and 
    J.ProbeName != \"daily:goc\" and
    J.ReportedSiteName not in (select GT.SiteName from """ + gDBSchema[mainDB] + """.Site GT) and
    J.EndTime >= \"""" + DateTimeToString(begin) + """\" and
    J.EndTime < \"""" + DateTimeToString(end) + """\"
    group by J.ReportedSiteName, J.VOName
    order by J.ReportedSiteName, J.VOName;"""
        return RunQueryAndSplit(select) + RunQueryAndSplit(panda_select)
    else:
        return RunQueryAndSplit(select)
    
def RangeVOSiteData(begin, end, with_panda = False):
    schema = gDBSchema[mainDB]
    select = """\
select J.VOName, T.SiteName, sum(NJobs), sum(J.WallDuration)
  from """ + schema + ".Site T, " + schema + ".Probe P, " + schema + """.VOProbeSummary J
  where VOName != \"unknown\" and 
    P.siteid = T.siteid and
    J.ProbeName = P.probename and
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\" and
    J.ProbeName not like \"psacct:%\"
    group by T.SiteName,J.VOName
    order by J.VOName,T.SiteName;"""
    if with_panda:
        panda_select = """\
select J.VOName, J.SiteName, sum(J.NJobs), sum(J.WallDuration)
  from """ + gDBSchema[dailyDB] + """.JobUsageRecord_Report J
  where VOName != \"unknown\" and 
    J.ProbeName != \"daily:goc\" and
    J.SiteName not in (select GT.SiteName from """ + gDBSchema[mainDB] + """.Site GT) and
    J.EndTime >= \"""" + DateTimeToString(begin) + """\" and
    J.EndTime < \"""" + DateTimeToString(end) + """\"
    group by J.SiteName, J.VOName
    order by J.VOName, J.SiteName;"""
        return RunQueryAndSplit(select) + RunQueryAndSplit(panda_select)
    else:
        return RunQueryAndSplit(select)

def LongJobsData(begin, end, with_panda = False):
    schema = gDBSchema[mainDB]
    
    select = """
select SiteName, VO.VOName, sum(NJobs), avg(WallDuration)/3600.0/24.0, avg(Cpu*100/WallDuration),
Date(max(EndTime)) from (select dbid, NJobs, WallDuration,CpuUserDuration+CpuSystemDuration as
Cpu,VOName,ReportableVOName,EndTime from JobUsageRecord J
where
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\" and
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
    select = """
SELECT VOName, CommonName, sum(NJobs), sum(WallDuration) as Wall
FROM VOProbeSummary U where
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\"
    and CommonName != \"unknown\"
    """ + selection + """
    group by CommonName, VOName
"""
    return RunQueryAndSplit(select)
    
def UserSiteReportData(begin, end, with_panda = False, selection = ""):
    select = """
SELECT CommonName, VOName, SiteName, sum(NJobs), sum(WallDuration) as Wall
FROM VOProbeSummary U, Probe P, Site S where
    EndTime >= \"""" + DateTimeToString(begin) + """\" and
    EndTime < \"""" + DateTimeToString(end) + """\" and
    U.ProbeName = P.ProbeName and P.siteid = S.siteid 
    and CommonName != \"unknown\"
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
    headers = ("VO","# of Jobs","Wall Duration","Delta jobs","Delta duration")
    num_header = 1
    formats = {}
    lines = {}
    totalheaders = ["All VOs"]
    defaultSort = True

    def __init__(self, header = False, with_panda = False):
        self.formats["csv"] = ",%s,\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %-18s | %9s | %13s | %10s | %14s"
        self.lines["csv"] = ""
        self.lines["text"] = "-----------------------------------------------------------------------------------"
        self.with_panda = with_panda
        if (not header) :  self.title = ""

    def GetData(self,start,end):
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
    headers = ("Site","# of Jobs","Wall Duration","Delta jobs","Delta duration")
    num_header = 1
    formats = {}
    lines = {}
    totalheaders = ["All sites"]
    defaultSort = True

    def __init__(self, header = False, with_panda = False):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %-30s | %9s | %13s | %10s | %14s"
        self.lines["csv"] = ""
        self.lines["text"] = "-----------------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda

    def GetData(self, start, end):
        return RangeSiteData(start, end, self.with_panda)

class DataTransferReportConf(GenericConf):
    title = """\
OSG Data transfer summary for  %s - %s (midnight UTC - midnight UTC)
including all data that transferred in that time period.
Deltas are the differences with the previous period."""
    headline = "For all data transferred between %s and %s (midnight, UTC)"
    headers = ("Site","Protocol","Num transfer","Delta transfer","Number of MiB","Delta MiB")
    num_header = 2
    factor = 1 # This is the factor to convert time from seconds to hours for other reports. But for data transfer report there is nothing to convert since we are just dealing with the transfer size (not time)
    delta_column_location = "adjacent"
    formats = {}
    lines = {}
    totalheaders = ["All sites","All Protocols"]
    defaultSort = True

    def __init__(self, header = False, with_panda = False):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %-30s | %-25s | %15s | %15s | %17s | %17s"
        self.lines["csv"] = ""
        self.lines["text"] = "---------------------------------------------------------------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda

    def GetData(self, start, end):
        return DataTransferData(start, end, self.with_panda)

class RangeSiteVOReportConf(GenericConf):
    title = """\
OSG usage summary for  %s - %s (midnight UTC - midnight UTC)
including all jobs that finished in that time period.
Wall Duration is expressed in hours and rounded to the nearest hour. Wall
Duration is the duration between the instant the job started running
and the instant the job ended its execution.
Deltas are the differences with the previous period."""
    headline = "For all jobs finished between %s and %s (midnight UTC)"
    headers = ("Site", "VO","# of Jobs","Wall Duration","Delta jobs","Delta duration")
    num_header = 2
    formats = {}
    lines = {}
    totalheaders = ["All sites","All VOs"]
    defaultSort = True

    def __init__(self, header = False, with_panda = False):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %-30s | %-18s | %9s | %13s | %10s | %14s"
        self.lines["csv"] = ""
        self.lines["text"] = "--------------------------------------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda

    def GetData(self, start,end):
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
    headers = ("VO", "Site","# of Jobs","Wall Duration","Delta jobs","Delta duration")
    num_header = 2
    formats = {}
    lines = {}
    totalheaders = ["All VOs","All sites"]
    defaultSort = True

    def __init__(self, header = False, with_panda = False):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %-18s | %-30s | %-9s | %13s | %10s | %14s"
        self.lines["csv"] = ""
        self.lines["text"] = "--------------------------------------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda

    def GetData(self, start,end):
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
    headers = ("VO", "User", "# of Jobs", "Wall Duration", "Delta jobs", "Delta duration")
    num_header = 2
    formats = {}
    lines = {}
    totalheaders = ["All VOs", "All Users"]
    defaultSort = False
    ExtraSelect = ""

    def __init__(self, header = False, with_panda = False, selectVOName = ""):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %-14s | %-35s | %9s | %13s | %10s | %14s"
        self.lines["csv"] = ""
        self.lines["text"] = "----------------------------------------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda
        if (len(selectVOName)>0):
            self.ExtraSelect = " and VOName = \""+selectVOName+"\" "

    def GetData(self, start,end):
        l = UpdateVOName(UserReportData(start, end, self.with_panda, self.ExtraSelect),0,start, end)
        r = []
        maxlen = 35
        for x in l:
            (vo,user,njobs,wall) = x.split('\t')
            if ( vo != "unknown" and vo != "other" ):
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
    headers = ("User", "VO", "Site", "# of Jobs", "Wall Duration", "Delta jobs", "Delta duration")
    num_header = 3
    formats = {}
    lines = {}
    totalheaders = ["All Users","All VOs","All Sites"]
    defaultSort = False
    ExtraSelect = ""

    def __init__(self, header = False, with_panda = False, selectVOName = ""):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %-35s | %-14s | %-19s | %9s | %13s | %10s | %14s"
        self.lines["csv"] = ""
        self.lines["text"] = "-------------------------------------------------------------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda
        if (len(selectVOName)>0):
            self.ExtraSelect = " and VOName = \""+selectVOName+"\" "

    def GetData(self, start,end):
        l = UpdateVOName(UserSiteReportData(start, end, self.with_panda, self.ExtraSelect),1,start, end)
        r = []
        maxlen = 35
        for x in l:
            (user,vo,site,njobs,wall) = x.split('\t')
            if ( vo != "unknown" and vo != "other" ):
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
Only jobs that last 7 days or longer are counted in this report.
"""
    headline = "For all jobs finished between %s and %s (midnight UTC)"
    headers = ("Site", "VO", "# of Jobs","Avg Wall","% Cpu","Max EndTime")
    num_header = 2
    formats = {}
    lines = {}
    totalheaders = ["All VOs","All sites"]

    def __init__(self, header = False, with_panda = False):
        self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
        self.formats["text"] = "| %-18s | %-14s | %9s | %8s | %5s | %11s"
        self.lines["csv"] = ""
        self.lines["text"] = "---------------------------------------------------------------------------------------"
        if (not header) :  self.title = ""
        self.with_panda = with_panda

    def GetData(self, start,end):
        return UpdateVOName(LongJobsData(start, end, self.with_panda),1,start, end)      

class RangeSiteVOEfficiencyConf(GenericConf):
        title = """\
OSG efficiency summary for  %s - %s (midnight UTC - midnight UTC)
including all jobs that finished in that time period.
Wall Duration is expressed in hours and rounded to the nearest hour. Wall
Duration is the duration between the instant the job started running
and the instant the job ended its execution.
Deltas are the differences with the previous period."""
        headline = "For all jobs finished between %s and %s (midnight UTC)"
        headers = ("Site", "VO","# of Jobs","Wall Dur.","Cpu / Wall","Delta")
        num_header = 2
        formats = {}
        lines = {}
        totalheaders = ["All sites","All VOs"]
        defaultSort = True

        def __init__(self, header = False):
           self.formats["csv"] = ",\"%s\",\"%s\",%s,%s,%s,\"%s\"  "
           self.formats["text"] = "| %-22s | %-14s | %9s | %9s | %10s | %10s"
           self.lines["csv"] = ""
           self.lines["text"] = "-------------------------------------------------------------------------------------------------"

           if (not header) :  self.title = ""

        def GetData(self,start,end):
           return UpdateVOName(GetSiteVOEfficiency(start,end),1,start, end)

class RangeVOEfficiencyConf(GenericConf):
        title = """\
OSG efficiency summary for  %s - %s (midnight UTC - midnight UTC)
including all jobs that finished in that time period.
Wall Duration is expressed in hours and rounded to the nearest hour. Wall
Duration is the duration between the instant the job started running
and the instant the job ended its execution.
Deltas are the differences with the previous period."""
        headline = "For all jobs finished between %s and %s (midnight UTC)"
        headers = ("VO","# of Jobs","Wall Dur.","Cpu / Wall","Delta")
        num_header = 1
        formats = {}
        lines = {}
        totalheaders = ["All VOs"]
        defaultSort = True

        def __init__(self, header = False):
           self.formats["csv"] = ",\"%s\",%s,%s,%s,\"%s\"  "
           self.formats["text"] = "| %-14s | %9s | %9s | %10s | %10s"
           self.lines["csv"] = ""
           self.lines["text"] = "------------------------------------------------------------------------"

           if (not header) :  self.title = ""

        def GetData(self,start,end):
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
        headers = ("VO","1 Days","7 Days","30 Days")
        num_header = 1
        formats = {}
        lines = {}
        totalheaders = ["All VOs"]
        defaultSort = True

        def __init__(self, header = False):
           self.formats["csv"] = ",\"%s\",%s,%s,%s  "
           self.formats["text"] = "| %-14s | %9s | %9s | %10s "
           self.lines["csv"] = ""
           self.lines["text"] = "-------------------------------------------------------------"

           if (not header) :  self.title = ""

        def GetData(self,start,end):
           return UpdateVOName(GetVOEfficiency(start,end),0,start, end)
                      
def SimpleRange(what, range_end = datetime.date.today(),
                 range_begin = None,
                 output = "text"):

    if not range_begin: range_begin = range_end + datetime.timedelta(days=-1)
    timediff = range_end - range_begin

    if (output != "None") :
        if (what.title != "") :
            print what.title % ( DateToString(range_begin,False),
                                 DateToString(range_end,False) )
        if (what.headline != "") :
            print what.headline % ( DateToString(range_begin,False),
                                    DateToString(range_end,False) )
        print what.lines[output]
        print "    ", what.formats[output] % what.headers
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
        values = (site,vo,niceNum(njobs), niceNum(wall), niceNum(cpu), endtime)

        if (output != "None"):
            print "%3d " %(index), what.formats[output] % values
        result.append(values)       
                

def GenericRange(what, range_end = datetime.date.today(),
                 range_begin = None,
                 output = "text"):
    factor = what.factor # Convert number of seconds to number of hours for most reports except data transfer report

    if (not range_begin or range_begin == None): range_begin = range_end + datetime.timedelta(days=-1)
    if (not range_end or range_end == None): range_end = range_begin + datetime.timedelta(days=+1)
    timediff = range_end - range_begin

    if (output != "None") :
        if (what.title != "") :
            print what.title % ( DateToString(range_begin,False),
                                 DateToString(range_end,False) )
        if (what.headline != "") :
            print what.headline % ( DateToString(range_begin,False),
                                    DateToString(range_end,False) )
        print what.lines[output]
        print "    ", what.formats[output] % what.headers
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
            if lkeys[0] != "unknown": lkeys[0] = string.lower(lkeys[0])
            if lkeys[0] == "atlas": lkeys[0] = "usatlas"

        if (len(val)==4) :
            # Nasty hack to harmonize Panda output
            if what.headers[1] == "VO":
               if lkeys[1] != "unknown": lkeys[1] = string.lower(lkeys[1])
               if lkeys[1] == "atlas": lkeys[1] = "usatlas"

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
            if lkeys[0] != "unknown": lkeys[0] = string.lower(lkeys[0])
            if lkeys[0] == "atlas": lkeys[0] = "usatlas"

        if (len(val)==4) :
            # Nasty hack to harmonize Panda output
            if what.headers[1] == "VO":
               if lkeys[1] != "unknown": lkeys[1] = string.lower(lkeys[1])
               if lkeys[1] == "atlas": lkeys[1] = "usatlas"

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
            print "%3d " %(index), what.formats[output] % tuple(printedvalues)
        result.append(tuple(printedvalues))       
                
    (oldnjobs,oldwall) = oldValues[("total","","")]
    if (output != "None") :
        print what.lines[output]
        printedvalues = []
        for iheaders in range(0,num_header):
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

        print "    ", what.formats[output] % tuple(printedvalues)
        print what.lines[output]
    return result

def EfficiencyRange(what, range_end = datetime.date.today(),
                 range_begin = None,
                 output = "text"):
    factor = 3600  # Convert number of seconds to number of hours

    if (not range_begin or range_begin == None): range_begin = range_end + datetime.timedelta(days=-1)
    if (not range_end or range_end == None): range_end = range_begin + datetime.timedelta(days=+1)
    timediff = range_end - range_begin

    if (output != "None") :
        if (what.title != "") :
            print what.title % ( DateToString(range_begin,False),
                                 DateToString(range_end,False) )
        if (what.headline != "") :
            print what.headline % ( DateToString(range_begin,False),
                                    DateToString(range_end,False) )
        print what.lines[output]
        print "    ", what.formats[output] % what.headers
        print what.lines[output]
        
    # First get the previous' range-length's information
    totalwall = 0
    totaljobs = 0
    totaleff = 0
    nrecords = 0
    oldValues = {}
    result = []

    start = range_begin - timediff
    end = range_end - timediff
    lines = what.GetData(start,end)
    for i in range (0,len(lines)):
        val = lines[i].split('\t')
        offset = 0
        site = val[0]
        if what.headers[0] == "VO":
            # "site" is really "VO": hack to harmonize Panda output
            if site != "unknown": site = string.lower(site)
            if site == "atlas": site = "usatlas"
        key = site
        vo = ""

        num_header = what.num_header;
        offset = num_header - 1;

        if (num_header==2):
           if (len(val)==4) :
              # Nasty hack to harmonize Panda output
              if what.headers[1] == "VO":
                 if vo != "unknown": vo = string.lower(val[1])
                 if vo == "atlas": vo = "usatlas"
              else:
		           vo = val[1]
              key = site + " " + vo
           else:
              vo = val[1]
              key = site + " " + vo

        njobs= int( val[offset+1] )
        wall = float( val[offset+2] ) / factor
        if (wall != 0) :
            eff = float( val[offset+3] )
        else:
            eff = -1
        nrecords = nrecords + 1
        totalwall = totalwall + wall
        totaljobs = totaljobs + njobs
        totaleff = totaleff + eff
        if (oldValues.has_key(key)):
            print "Error: can not add efficiencies"
            print key
            print oldValues[key]
            print [njobs,wall,eff,site,vo]
        else:
            oldValues[key] = [njobs,wall,eff,site,vo]

    [totaljobs,totalwall,totaleff] = GetTotals(start,end)
    totaljobs = int(totaljobs);
    totalwall = float(totalwall) /factor
    totaleff = float(totaleff)

    oldValues["total"] = (totaljobs, totalwall, totaleff, "total","")

    # Then getting the current information and print it
    totalwall = 0
    totaljobs = 0
    totaleff = 0
    nrecords = 0
    start = range_begin
    end = range_end
    lines = what.GetData(start,end)
    num_header = 1;
    index = 0
    printValues = {}
    for i in range (0,len(lines)):
        val = lines[i].split('\t')
        site = val[0]
        if what.headers[0] == "VO":
            # "site" is really "VO": hack to harmonize Panda output
            if site != "unknown": site = string.lower(site)
            if site == "atlas": site = "usatlas"
        key = site
        offset = 0

        num_header = what.num_header;
        offset = num_header - 1;
        
        if (num_header==2):
           if (len(val)==4):
              # Nasty hack to harmonize Panda output
              if what.headers[1] == "VO":
                 if vo != "unknown": vo = string.lower(val[1])
                 if vo == "atlas": vo = "usatlas"
              else:
		           vo = val[1]
              key = site + " " + vo
           else:
              vo = val[1]
              key = site + " " + vo

        (oldnjobs,oldwall,oldeff) = (0,0,0)
        if oldValues.has_key(key):
            (oldnjobs,oldwall,oldeff,s,v) = oldValues[key]
            del oldValues[key]
        njobs= int( val[offset+1] )
        wall = float( val[offset+2] ) / factor
        if (wall != 0) :
            eff = float( val[offset+3] )
        else:
            eff = -1
        totalwall = totalwall + wall
        totaljobs = totaljobs + njobs
        totaleff = totaleff + eff
        nrecords = nrecords + 1
        if printValues.has_key(key):
            print "Error: can not add efficiencies"
            print key
            print printValues[key]
            print [njobs,wall,oldwall,eff,site,vo]
        else:
            printValues[key] = [njobs,wall,oldwall,eff,oldeff,site,vo]
                
    for key,(oldnjobs,oldwall,oldeff,site,vo) in oldValues.iteritems():            
        if (key != "total") :
            printValues[key] = (0,0,oldwall,0,oldeff,site,vo)

    if (what.defaultSort):
        sortedValues = sortedDictValues(printValues)
    else:
        sortedValues = sortedDictValuesFunc(printValues,what.Sorting)
        
    for key,(njobs,wall,oldwall,eff,oldeff,site,vo) in sortedValues:
        index = index + 1;
        if (eff==-1) : 
           effstring = "n/a"
           oldeffstring = niceNum(oldeff*100.0,0.1)
        else: 
           effstring = niceNum(eff*100.0,0.1)
           if (oldeff == - 1 or oldwall < 0.1): 
              oldeffstring = "n/a"
           else:
              oldeffstring = niceNum((eff-oldeff)*100.0,1)
        if (wall < 0.1):
           wallstring = "0.0"
           effstring = "n/a"
        elif (wall < 1):
           wallstring = niceNum( wall, 0.1)
        else:
           wallstring = niceNum( wall )
        if (num_header == 2) :
            values = (site,vo,niceNum(njobs), wallstring,
                      effstring,oldeffstring)
        else:
            values = (site,niceNum(njobs), wallstring,
                      effstring,oldeffstring)
        if (output != "None") :
            #print index
            #print values
            print "%3d " %(index), what.formats[output] % values
        result.append(values)       
        
        
    [totaljobs,totalwall,totaleff] = GetTotals(range_begin,range_end)
    totaljobs = int(totaljobs);
    totalwall = float(totalwall) /factor
    totaleff = float(totaleff)

    (oldnjobs,oldwall,oldeff,s,v) = oldValues["total"]
    if (output != "None") :
        print what.lines[output]
        if (num_header == 2) :
            print "    ", what.formats[output] % \
                  (what.totalheaders[0], what.totalheaders[1], niceNum(totaljobs),
                   niceNum(totalwall), niceNum( totaleff * 100.0, 0.1 ),
                   niceNum( 100.0* (totaleff- oldeff), 0.1 ) )
        else:
            print "    ", what.formats[output] % \
                  (what.totalheaders[0], niceNum(totaljobs), niceNum(totalwall),
                    niceNum( totaleff / nrecords * 100.0 ),
                   niceNum( 100.0* (totaleff - oldeff) ))
        print what.lines[output]
    return result

def EfficiencyGraded(what, range_end = datetime.date.today(),
                     output = "text"):
    factor = 3600  # Convert number of seconds to number of hours

    deltas = [1,7,30]
    
    range_begin = range_end + datetime.timedelta(days=-deltas[0])
    
    if (output != "None") :
        if (what.title != "") :
            print what.title % ( DateToString(range_begin,False),
                                 DateToString(range_end,False) )
        if (what.headline != "") :
            print what.headline % ( DateToString(range_begin,False),
                                    DateToString(range_end,False) )
        print what.lines[output]
        print "    ", what.formats[output] % what.headers
        print what.lines[output]
        
    # First get the previous' range-length's information
    totalwall = [0,0,0]
    totaljobs = [0,0,0]
    totaleff = [0,0,0]
    nrecords = [0,0,0]
    values = {}
    result = []

    for when in range(0,len(deltas)):
       start = range_end - datetime.timedelta(days=deltas[when])
       end = range_end

       lines = what.GetData(start,end)
       for i in range (0,len(lines)):
          val = lines[i].split('\t')
 
          offset = 0
          first = val[0]
          
          num_header = what.num_header;
          offset = num_header - 1;

          if (num_header==2):
             second = val[1]
             key = first + " " + second
          else:
             second = ""
             key = first
             
          njobs= int( val[offset+1] )
          wall = float( val[offset+2] ) / factor
          if (wall != 0) :
             eff = float( val[offset+3] )
          else:
             eff = -1

          nrecords[when] = nrecords[when] + 1
          totalwall[when] = totalwall[when] + wall
          totaljobs[when] = totaljobs[when] + njobs
          totaleff[when] = totaleff[when] + eff
          
          if (values.has_key(key)):
              current = values[key]
              current[when+1] = [njobs,wall,eff]
              values[key] = current
#             print "Error: can not add efficiencies"
#             print key
#             print oldValues[key]
#             print [njobs,wall,eff,site,vo]
          else:
             empty = [[],[],[],[]]
             empty[0] = [first,second]
             empty[when+1] = [njobs,wall,eff]
             values[key] = empty

    [totaljobs,totalwall,totaleff] = GetTotals(start,end)
    totaljobs = int(totaljobs);
    totalwall = float(totalwall) /factor
    totaleff = float(totaleff)

#    for key,(oldnjobs,oldwall,oldeff,site,vo) in values.iteritems():            
#        if (key != "total") :
#            printValues[key] = (0,0,oldwall,0,oldeff,site,vo)

    if (what.defaultSort):
        sortedValues = sortedDictValues(values)
    else:
        sortedValues = sortedDictValuesFunc(values,what.Sorting)
        
    index = 0
    for key,data in sortedValues:

        index = index + 1
        printval = []
        printval.append( data[0][0] )
        if (what.num_header==2):
           printval.append( data[0][1] )
        for inside in data[1:]:
           if (len(inside)>2):
              eff = inside[2]
              if (eff==-1):
                 effstring = "n/a"
              else:
                 effstring = niceNum(eff*100.0,0.1)
              printval.append( effstring )
           else:
              printval.append( "n/a" )

        if (output != "None") :
            print "%3d " %(index), what.formats[output] % tuple(printval)
        result.append(values)       
        
    if (output != "None") :
        print what.lines[output]
    return result

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
select distinct VOName from """+schema+"""VOProbeSummary V where VOName != \"unknown\" and 
            EndTime >= \"""" + DateToString(begin) + """\" and
            EndTime < \"""" + DateToString(end) + """\"
            order by VOName
            """
    #print "Query = " + select;

    return RunQueryAndSplit(select);

def GetLastReportingVOs(when):
    schema = gDBSchema[mainDB] + ".";

    select = """\
select distinct VOName from """+schema+"""VOProbeSummary V where VOName != \"unknown\" and 
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
    schema = gDBSchema[mainDB] + ".";

    select = """\
select distinct SiteName from """+schema+"""VOProbeSummary V,Probe P,Site S where VOName != \"unknown\" and 
            EndTime >= \"""" + DateToString(begin) + """\" and
            EndTime < \"""" + DateToString(end) + """\"
            and V.ProbeName = P.ProbeName and P.siteid = S.siteid
            order by SiteName
            """
    #print "Query = " + select;

    return RunQueryAndSplit(select);

def GetListOfDataTransferReportingSites(begin,end):
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
    schema = gDBSchema[mainDB] + ".";

    select = """\
select sum(Njobs),sum(WallDuration),sum(CpuUserDuration+CpuSystemDuration)/sum(WallDuration) from """+schema+"""VOProbeSummary where VOName != \"unknown\" and 
            EndTime >= \"""" + DateToString(begin) + """\" and
            EndTime < \"""" + DateToString(end) + """\"
            """
    #print "Query = " + select;

    return RunQueryAndSplit(select)[0].split('\t');

def GetDataTransferTotals(begin,end):
    global gMySQLConnectString, gDBCurrent
    schema = gDBSchema[transferDB] + ".";
    gDBCurrent = transferDB
    keepConnectionValue = gMySQLConnectString
    gMySQLConnectString = gMySQLTransferConnectString
    select = "select sum(Njobs),sum(TransferDuration) from " + schema + "MasterTransferSummary where StartTime >= \"" + DateToString(begin) + "\" and StartTime < \"" + DateToString(end) + "\""
    result =  RunQueryAndSplit(select)[0].split('\t');
    gMySQLConnectString = keepConnectionValue
    return result
    
def GetNewUsers(begin,end):
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
    #exceptionSites = ['BNL_ATLAS_1', 'BNL_ATLAS_2', 'USCMS-FNAL-WC1-CE2', 'USCMS-FNAL-WC1-CE3', 'USCMS-FNAL-WC1-CE4', 'BNL_LOCAL', 'BNL_OSG', 'BNL_PANDA', 'GLOW-CMS', 'UCSDT2-B']

    reportingSites = GetListOfDataTransferReportingSites(range_begin,range_end)
    #print "reporting sites",reportingSites

    allSites = None
    if regSites != None:
        allSites = [name for name in regSites if name not in exceptionSites]
    #print "allSites is ", allSites

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
        print "As of %s, there are %s registered SRMv2 %s sites." % \
            (DateToString(datetime.date.today(),False),
            prettyInt(len(allSites)), gridDisplayName)

    print "\nBetween %s - %s (midnight - midnight UTC):\n" % \
        (DateToString(range_begin, False), DateToString(range_end, False))
                                                               
    n = len(reportingSites)
    print prettyInt(n)+" sites reported\n"

    #[njobs,wallduration,div] = GetTotals(range_begin,range_end)
    [njobs,wallduration] = GetDataTransferTotals(range_begin,range_end)
    if (njobs != "NULL"):
       njobs = int(njobs);
       wallduration = float(wallduration)
    else:
       njobs = 0
       wallduration = 0
    
    print "Total number of transfers: "+prettyInt(njobs)
    print "Total transfer duration: "+niceNum( wallduration / 3600, 1 )+ " hours"

    if reportingSites != None and extraSites != None and knownExtras != None \
            and allSites != None:
        n = len(reportingSites)-len(extraSites)-len(knownExtras)
        print "%s registered sites reported (%s%% of %s sites)" % \
            (prettyInt(n), niceNum(n*100/len(allSites),1), gridDisplayName)

    if missingSites != None and allSites != None:
        n = len(missingSites)
        print "%s registered sites have NOT reported (%s%% of %s sites)" % \
            (prettyInt(n), niceNum(n*100/len(allSites),1), gridDisplayName)

    if emptySites != None and allSites != None:
        n = len(emptySites)
        print "%s registered sites have reported but have no activity (%s%% " \
            "of %s sites)" % (prettyInt(n), niceNum(n*100/len(allSites), 1),
            gridDisplayName)

    print
    
    n = len(extraSites);
    if not gGrid or gGrid.lower() != "local":
        print prettyInt(n)+" non-sanctioned non-registered sites reported " \
            "(might indicate a discrepancy between OIM and Gratia)."
    elif allSites != None:
        print prettyInt(n)+" non-sanctioned non-registered sites reported."

    if reportingDisabled != None: 
        n = len(reportingDisabled)
        print prettyInt(n)+" disabled sites have reported."
    
    if emptySites != None:
        print "\nThe sites with no activity are: \n"+prettyList(emptySites)

    if missingSites != None:
        print "\nThe non reporting sites are: \n"+prettyList(missingSites)

    if allSites != None:
        print "\nThe non registered sites are: \n"+prettyList(extraSites)
    if reportingDisabled != None:
        print "\nThe disabled sites that are reporting: \n" + \
            prettyList(reportingDisabled)

    print "\n"

    return missingSites

def RangeSummup(range_end = datetime.date.today(),
                range_begin = None,
                output = "text",
                header = True):

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

    regSites = GetListOfOSGSites();
    regVOs = GetListOfRegisteredVO('Active',range_begin,range_end)
    disabledSites = GetListOfDisabledOSGSites()

    reportingVOs = GetReportingVOs(range_begin, range_end)
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
    #exceptionSites = ['BNL_ATLAS_1', 'BNL_ATLAS_2', 'USCMS-FNAL-WC1-CE2', 'USCMS-FNAL-WC1-CE3', 'USCMS-FNAL-WC1-CE4', 'BNL_LOCAL', 'BNL_OSG', 'BNL_PANDA', 'GLOW-CMS', 'UCSDT2-B']

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
        print "As of %s, there are %s registered %s sites." % \
            (DateToString(datetime.date.today(),False),
            prettyInt(len(allSites)), gridDisplayName)

    print "\nBetween %s - %s (midnight - midnight UTC):\n" % \
        (DateToString(range_begin, False), DateToString(range_end, False))
                                                               
    n = len(reportingSites)
    print prettyInt(n)+" sites reported\n"

    [njobs,wallduration,div] = GetTotals(range_begin,range_end)
    if (njobs != "NULL"):
       njobs = int(njobs);
       wallduration = float(wallduration)
       div = float(div)
    else:
       njobs = 0
       wallduration = 0
       div = 1
    
    print "Total number of jobs: "+prettyInt(njobs)
    print "Total wall duration: "+niceNum( wallduration / 3600, 1 )+ " hours"
    print "Total cpu / wall duration: "+niceNum(div,0.01)

    if reportingSites != None and extraSites != None and knownExtras != None \
            and allSites != None:
        n = len(reportingSites)-len(extraSites)-len(knownExtras)
        print "%s registered sites reported (%s%% of %s sites)" % \
            (prettyInt(n), niceNum(n*100/len(allSites),1), gridDisplayName)

    if missingSites != None and allSites != None:
        n = len(missingSites)
        print "%s registered sites have NOT reported (%s%% of %s sites)" % \
            (prettyInt(n), niceNum(n*100/len(allSites),1), gridDisplayName)

    if emptySites != None and allSites != None:
        n = len(emptySites)
        print "%s registered sites have reported but have no activity (%s%% " \
            "of %s sites)" % (prettyInt(n), niceNum(n*100/len(allSites), 1),
            gridDisplayName)

    print
    
    n = len(extraSites);
    if not gGrid or gGrid.lower() != "local":
        print prettyInt(n)+" non-sanctioned non-registered sites reported " \
            "(might indicate a discrepancy between OIM and Gratia)."
    elif allSites != None:
        print prettyInt(n)+" non-sanctioned non-registered sites reported."

    #n = len(knownExtras);
    #print prettyInt(n)+" sanctioned non-registered sites reported"

    if reportingDisabled != None: 
        n = len(reportingDisabled)
        print prettyInt(n)+" disabled sites have reported."

    #print "\nThe reporting sites are:\n"+prettyList(reportingSites)
    #print "\nThe registered sites are:\n"+prettyList(allSites)
    
    if emptySites != None:
        print "\nThe sites with no activity are: \n"+prettyList(emptySites)

    if missingSites != None:
        print "\nThe non reporting sites are: \n"+prettyList(missingSites)
    #print "\nThe sanctioned non registered sites are: \n"+prettyList(knownExtras)

    if allSites != None:
        print "\nThe non registered sites are: \n"+prettyList(extraSites)
    if reportingDisabled != None:
        print "\nThe disabled sites that are reporting: \n" + \
            prettyList(reportingDisabled)

    expectedNoActivity = GetListOfRegisteredVO('Disabled',range_begin,range_end)
    expectedNoActivityAlt = GetListOfRegisteredVO('Enabled', range_begin,
        range_end)
    if expectedNoActivity and expectedNoActivityAlt:
        expectedNoActivity += expectedNoActivityAlt
    elif expectedNoActivity == None:
        expectedNoActivity = expectedNoActivityAlt

    emptyVO = None
    if regVOs != None:
        emptyVO = [name for name in regVOs if name not in reportingVOs and \
            (not expectedNoActivity or name not in expectedNoActivity)]
    if emptyVO:
        print "\nActive VOs with no recent activity are:\n"+prettyList(emptyVO)
    if expectedNoActivity != None:
        print "\nThe following VOs are expected to have no activity:\n" + \
            prettyList([name for name in expectedNoActivity if name not in \
            reportingVOs])

    if regVOs != None:
        nonregVO = [name for name in reportingVOs if name not in regVOs]
        print "\nThe non-registered VOs with recent activity are:\n" + \
            prettyList(nonregVO)
    
    print "\n"

    return missingSites

def NonReportingSites(
                when = datetime.date.today(),
                output = "text",
                header = True):

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

    stoppedSites = [name for name in stoppedSites if name in allSites]
    missingSites = [name for name in allSites if name not in reportingSites and name not in stoppedSites]
    extraSites = [name for name in reportingSites if name not in allSites]
    knownExtras = [name for name in extraSites if name in exceptionSites and name not in regSites]
    extraSites = [name for name in extraSites if name not in exceptionSites]

    #print allSites
    #print reportingSites
    #print missingSites
    #print extraSites
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
    print "\nActive VOs with no recent activity are:\n"+prettyList(emptyVO)
    print "\nThe following VOs are expected to have no activity:\n"+prettyList([name for name in expectedNoActivity if name not in reportingVOs])
    print "\nThe non-registered VOs with recent activity are:\n"+prettyList(nonregVO)

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
            
            
    
    return missingSites

def LongJobs(range_end = datetime.date.today(),
            range_begin = None,
            output = "text",
            header = True):

    print "This report is a summary of long running jobs that finished between %s - %s (midnight - midnight UTC):\n" % ( DateToString(range_begin,False),
                                                                        DateToString(range_end,False) )
    RangeLongJobs(range_end,range_begin,output,header)

def CMSProd(range_end = datetime.date.today(),
            range_begin = None,
            output = "text"):

    factor = 3600  # Convert number of seconds to number of hours

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
    print "Number of wallclock hours during the previous 7 days consumed by the cmsprod and cmsprd user ids reported via USCMS-FNAL-WC1-CE:"

    data = CMSProdData(range_begin,range_end)
    wall = 0
    user = 0
    for line in data:
       (prod, value) = line.split("\t")
       if (prod == "0"):
          user = float( value ) / factor
       elif (prod == "1"):
          wall = float( value ) / factor
       else:
          print "Unexpected value in first column (production):",prod
    total = wall + user
    print
    print "Production: ",niceNum(wall)
    print "Users     : ",niceNum(user)
    print "Total     : ",niceNum(total)

def SoftwareVersionData(schema,begin,end):
   select = """SELECT Si.SiteName, M.ProbeName, S.Name, S.Version, M.ServerDate as StartedOn, Pr.CurrentTime as LastReport
FROM """+schema+""".ProbeSoftware P, """+schema+""".ProbeDetails_Meta M, """+schema+""".Software S, """+schema+""".Probe Pr, """+schema+""".Site Si
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

   exceptionSites = ['AGLT2_CE_2','BNL-LCG2','BNL_ATLAS_1', 'BNL_ATLAS_2','FNAL_GPGRID_2','USCMS-FNAL-XEN','USCMS-FNAL-WC1-CE2', 'USCMS-FNAL-WC1-CE3', 'USCMS-FNAL-WC1-CE4', 'BNL_LOCAL', 'BNL_OSG', 'BNL_PANDA', 'GLOW-CMS', 'UCSDT2-B', 'Purdue-Lear' ]
   #exceptionSites = ['BNL_ATLAS_2', 'USCMS-FNAL-WC1-CE2', 'USCMS-FNAL-WC1-CE3', 'USCMS-FNAL-WC1-CE4', 'Generic Site', 'BNL_LOCAL', 'BNL_OSG', 'BNL_PANDA', 'GLOW-CMS', 'UCSDT2-B']
   sites = [name for name in GetListOfOSGSites()  if name not in exceptionSites]
   reportingSites = GetListOfReportingSites(range_begin,range_end);

   versions = {
     "Gratia": { "1.65":"v0.27.[1-2]","1.67":"v0.27b","1.68":"v0.28","1.69":"v0.30","1.69.2.1":"v0.32.1","1.78":"v0.32.2",
                 "1.84":"v0.34.[1-8]","1.85":"v0.34.[9-10]","1.86":"v0.36","1.90":"v0.38.4","1.91":"v1.00.1",
                 "1.93":"v1.00.3","1.95":"v1.00.5","1.100":"v1.02.01",
                 "3002":"v1.04.1","3266":"v1.04.3","3316":"v1.04.4c"},
     "condor_meter.pl" : { "$""Revision: 1.29 $  (tag unknown)":"v0.99", "$""Revision: 1.31 $  (tag unknown)":"v1.00.3+", 
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
(select VC.corrid, VO.VOName from VO, VONameCorrection VC where VC.void = VO.void and VO.VOName in ("usatlas","chen") ) S
left join (select VOcorrid,sum(njobs) as njobs,sum(WallDuration) as WallDuration from MasterSummaryData M where '2008/11/10' <= EndTime and EndTime <= '2008/11/11' group by VOcorrid ) M on S.corrid = M.VOcorrid
group by S.VOName"""

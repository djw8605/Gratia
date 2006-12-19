#
# Author Philippe Canal
#
# PSACCTReport
#
# library to create simple report using the Gratia psacct database
#
#@(#)gratia/summary:$Name: not supported by cvs2svn $:$Id: PSACCTReport.py,v 1.12 2006-12-19 20:53:11 pcanal Exp $

import time
import datetime
import getopt
import math
import re

gMySQL = "mysql"
gProbename = "cmslcgce.fnal.gov"
gLogFileIsWriteable = True;

gBegin=datetime.date(2006,06,01)
gEnd=datetime.date(2006,07,01)

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
    global gProbename,gOutput

    if argv is None:
        argv = sys.argv
    try:
        try:
            opts, args = getopt.getopt(argv[1:], "hm:p:", ["help","month=","probe=","output="])
        except getopt.error, msg:
             raise Usage(msg)
        # more code, unchanged
    except Usage, err:
        print >>sys.stderr, err.msg
        print >>sys.stderr, "for help use --help"
        return 2
    start = ""
    end = ""
    if len(argv) > len(opts) + 1:
        start = argv[len(opts)+1]
        if len(argv) > len(opts) + 2:
                end =  argv[len(opts)+2]
        SetDate(start,end)
    for o, a in opts:
        if o in ("-m","--month"):
                month = a;
        if o in ("-p","--probe"):
                gProbename = a;
        if o in ("--output"):
                gOutput = a

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
    return " and \"" \
        + DateToString(gBegin) +"\"<EndTime and EndTime<\"" + DateToString(gEnd) + "\"" \
        + ProbeWhere()

def StringToDate(input):
    return datetime.datetime(*time.strptime(input, "%d/%m/%Y")[0:5])

def DateToString(input,gmt=True):
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

gMySQLConnectString = " -h gratia-db01.fnal.gov -u reader --port=3320 --password=reader "

def CheckDB():
        global gMySQL,gMySQLConnectString
        (status, output) = commands.getstatusoutput( gMySQL + gMySQLConnectString + " gratia -e status "  )
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
        global gMySQL
        LogToFile(select)
        return commands.getoutput("echo '" + select + "' | " + gMySQL + gMySQLConnectString + " gratia " )

def RunQueryAndSplit(select):
        res = RunQuery(select)
        LogToFile(res)
        lines = res.split("\n")
        return lines


def NumberOfCpus():
        schema = "gratia_psacct";
        
        select = "select sum(cpus),sum(bench) from " \
			    + " ( SELECT distinct J.Host, cpuinfo.CpuCount as cpus,cpuinfo.CpuCount*cpuinfo.BenchmarkScore/1000 as bench from " \
                + schema + ".CPUInfo cpuinfo,"+schema+".JobUsageRecord J " \
                + "where J.HostDescription=cpuinfo.NodeName " \
                + CommonWhere() + ") as Sub;"
        res = RunQuery(select);
        LogToFile(res)
        values = res.split("\n")[1]
        ncpu = string.atoi(values.split("\t")[0])
        benchtotal = string.atof(values.split("\t")[1]) 
        return (ncpu,benchtotal);

def WeeklyData():
        schema = "gratia_psacct";
        select = " SELECT J.VOName, sum((J.CpuUserDuration+J.CpuSystemDuration)) as cputime, " + \
                 " sum((J.CpuUserDuration+J.CpuSystemDuration)*CpuInfo.BenchmarkScore)/1000 as normcpu, " + \
                 " sum(J.WallDuration)*0 as wall, sum(J.WallDuration*CpuInfo.BenchmarkScore)*0/1000 as normwall " + \
                 " FROM "+schema+".JobUsageRecord J, "+schema+".CPUInfo CpuInfo " + \
                 " where J.HostDescription=CpuInfo.NodeName " + CommonWhere() + \
                 " group by J.VOName; "
        return RunQueryAndSplit(select)

def CondorData():
        select = " SELECT J.VOName, sum((J.CpuUserDuration+J.CpuSystemDuration)) as cputime, " + \
                      " sum((J.CpuUserDuration+J.CpuSystemDuration)*0) as normcpu, " + \
                      " sum(J.WallDuration) as wall, sum(J.WallDuration*0) as normwall " + \
                 " FROM JobUsageRecord J " + \
                 " where 1=1 " + CommonWhere() + \
                 " group by VOName; "
        return RunQueryAndSplit(select)

def DailySiteData(begin,end):
        schema = "gratia"
        
        select = " SELECT CETable.facility_name, sum(NJobs), sum(J.WallDuration) " \
                + " from "+schema+".CETable, "+schema+".CEProbes, "+schema+".JobUsageRecord J " \
                + " where CEProbes.facility_id = CETable.facility_id and J.ProbeName = CEProbes.probename" \
                + " and \""+ DateToString(begin) +"\"<EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and J.ProbeName not like \"psacct:%\" " \
                + " group by CEProbes.facility_id "
        return RunQueryAndSplit(select)

def DailyVOData(begin,end):
        schema = "gratia"
            
        select = " SELECT J.VOName, Sum(NJobs), sum(J.WallDuration) " \
                + " from "+schema+".CETable, "+schema+".CEProbes, "+schema+".JobUsageRecord J " \
                + " where CEProbes.facility_id = CETable.facility_id and J.ProbeName = CEProbes.probename" \
                + " and \""+ DateToString(begin) +"\"<EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and J.ProbeName not like \"psacct:%\" " \
                + " group by J.VOName "
        return RunQueryAndSplit(select)

def DailySiteVOData(begin,end):
        schema = "gratia"
        
        select = " SELECT CETable.facility_name, J.VOName, sum(NJobs), sum(J.WallDuration) " \
                + " from "+schema+".CETable, "+schema+".CEProbes, "+schema+".JobUsageRecord J " \
                + " where CEProbes.facility_id = CETable.facility_id and J.ProbeName = CEProbes.probename" \
                + " and \""+ DateToString(begin) +"\"<EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and J.ProbeName not like \"psacct:%\" " \
                + " group by J.VOName, J.ProbeName order by CETable.facility_name "
        return RunQueryAndSplit(select)

def DailySiteVODataFromDaily(begin,end,select,count):
        schema = "gratia_osg_daily"
        
        select = " SELECT J.SiteName, J.VOName, "+count+", sum(J.WallDuration) " \
                + " from "+schema+".JobUsageRecord J " \
                + " where \""+ DateToString(begin) +"\"<EndTime and EndTime<\"" + DateToString(end) + "\"" \
                + " and ProbeName " + select + "\"daily:goc\" " \
                + " group by J.VOName, J.SiteName order by J.SiteName, J.VOName "
        return RunQueryAndSplit(select)

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
                self.cputime = string.atof(vals[1]) / factor
                self.normcpu = string.atof(vals[2]) / factor
                self.walltime= string.atof(vals[3]) / factor
                self.normwall= string.atof(vals[4]) / factor

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
        for i in range (1,len(lines)):
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
        for i in range (1,len(lines)):
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

class DailySiteReportConf:
        title = "OSG usage summary (midnight to midnight central time) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nDeltas are the differences with the previous day.\n"
        headline = "For all jobs finished on %s (Central Time)"
        headers = ("Site","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        formats = {}
        lines = {}
        col1 = "All sites"

        def __init__(self, header = False):
           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %-22s | %9s | %13s | %10s | %14s"
           self.lines["csv"] = ""
           self.lines["text"] = "---------------------------------------------------------------------------------------"

           if (not header) :  self.title = ""


        def GetData(self,start,end):
           return DailySiteData(start,end)      

class DailyVOReportConf:
        title = "OSG usage summary (midnight to midnight central time) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nDeltas are the differences with the previous day.\n"
        headline = "For all jobs finished on %s (Central Time)"
        headers = ("VO","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        formats = {}
        lines = {}
        col1 = "All VOs"

        def __init__(self, header = False):
           self.formats["csv"] = ",%s,\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %-18s | %9s | %13s | %10s | %14s"
           self.lines["csv"] = ""
           self.lines["text"] = "-----------------------------------------------------------------------------------"

           if (not header) :  self.title = ""

        def GetData(self,start,end):
           return DailyVOData(start,end)      

class DailySiteVOReportConf:
        title = "OSG usage summary (midnight to midnight central time) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nDeltas are the differences with the previous day.\n"
        headline = "For all jobs finished on %s (Central Time)"
        headers = ("Site","VO","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        formats = {}
        lines = {}
        select = "=="
        col1 = "All sites"
        col2 = "All VOs"
        
        def __init__(self, header = False):
           self.formats["csv"] = ",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\""
           self.formats["text"] = "| %-22s | %-14s | %9s | %13s | %10s | %14s"
           self.lines["csv"] = ""
           self.lines["text"] = "--------------------------------------------------------------------------------------------------------"

           if (not header) :  self.title = ""

        def GetData(self,start,end):
           return DailySiteVOData(start,end)      

class DailySiteVOReportFromDailyConf:
        title = "OSG usage summary (midnight to midnight central time) for %s\nincluding all jobs that finished in that time period.\nWall Duration is expressed in hours and rounded to the nearest hour.\nWall Duration is the duration between the instant the job start running and the instant the job ends its execution.\nDeltas are the differences with the previous day.\nIf the number of jobs stated for a site is always 1\nthen this number is actually the number of summary records sent.\n"
        headline = "For all jobs finished on %s (Central Time)"
        headers = ("Site","VO","# of Jobs","Wall Duration","Delta jobs","Delta duration")
        formats = {}
        lines = {}
        select = "=="
        count = "sum(NJobs)"
        col1 = "All sites"
        col2 = "All VOs"

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

def sortedDictValues(adict):
    items = adict.items()
    items.sort()
    return [(key,value) for key, value in items]

def GenericDaily(what, when = datetime.date.today(), output = "text"):
        factor = 3600  # Convert number of seconds to number of hours

        if (output != "None") :
            if (what.title != "") :
                print what.title % ( DateToString(when,False) )
            if (what.headline != "") :
                print what.headline % (DateToString(when,False))
            print what.lines[output]
            print "    ", what.formats[output] % what.headers
            print what.lines[output]
        
        # First get the previous' day information
        totalwall = 0
        totaljobs = 0
        oldValues = {}
        result = []

        start = when  + datetime.timedelta(days=-1)
        end = start + datetime.timedelta(days=1)
        lines = what.GetData(start,end)
        for i in range (1,len(lines)):
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
                njobs= string.atoi( val[offset+1] )
                wall = string.atof( val[offset+2] ) / factor
                totalwall = totalwall + wall
                totaljobs = totaljobs + njobs                
                oldValues[key] = (njobs,wall,site,vo)
        oldValues["total"] = (totaljobs, totalwall, "total","")

        # Then getting the correct day's information and print it
        totalwall = 0
        totaljobs = 0
        start = when
        end = start + datetime.timedelta(days=1)
        lines = what.GetData(start,end)
        num_header = 1;
        index = 0
        printValues = {}
        for i in range (1,len(lines)):
                val = lines[i].split('\t')
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
                njobs= string.atoi( val[offset+1] )
                wall = string.atof( val[offset+2] ) / factor
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
                if (num_header == 2) :
                    print "    ",what.formats[output] % (what.col1, what.col2, niceNum(totaljobs), niceNum(totalwall), niceNum(totaljobs-oldnjobs), niceNum(totalwall-oldwall))
                else:
                    print "    ",what.formats[output] % (what.col1, niceNum(totaljobs), niceNum(totalwall), niceNum(totaljobs-oldnjobs), niceNum(totalwall-oldwall))
                print what.lines[output]
        return result

        
def DailySiteReport(when = datetime.date.today(), output = "text", header = True):
        return GenericDaily( DailySiteReportConf(header), when, output)

def DailyVOReport(when = datetime.date.today(), output = "text", header = True):
        return GenericDaily( DailyVOReportConf(header), when, output)
 
def DailySiteVOReport(when = datetime.date.today(), output = "text", header = True):
        return GenericDaily( DailySiteVOReportConf(header), when, output)
 
 

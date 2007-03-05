#
# Author Philippe Canal
#
# LCG 
#
# library to transfer the data from Gratia to APEL (WLCG)
#
#@(#)gratia/summary:$Name: not supported by cvs2svn $:$Id: LCG.py,v 1.4 2007-03-05 20:30:27 pcanal Exp $

import time
import datetime
import getopt
import math
import re
import string
import commands, os, sys, time, string

gMySQL = "mysql"
gMySQLConnectString = " -h gratia-db01.fnal.gov -u reader --port=3320 --password=reader "
gLogFileIsWriteable = True;
gBegin = None
gEnd = None
gNormalization = 1.2

def UseArgs(argv):
    global gProbename,gOutput,gWithPanda

    if argv is None:
        argv = sys.argv
    try:
        try:
            opts, args = getopt.getopt(argv[1:], "hm:p:", ["help","output="])
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
        month = argv[len(opts)+1]
        SetDate(month)
    for o, a in opts:
        if o in ("--output"):
                gOutput = a

def SetDate(month):
    " Set the start and begin by string"
    global gBegin, gEnd

    t = time.strptime(month, "%Y/%m")[0:3]
    gBegin = datetime.date(*t)
    t = (t[0],t[1]+1,t[2])
    gEnd = datetime.date(*t)

def StringToDate(input):
    return datetime.datetime(*time.strptime(input, "%d/%m/%Y")[0:5])

def DateToString(input,gmt=True):
    if gmt:
        return input.strftime("%Y-%m-%d 00:00:00");
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
        return commands.getoutput("echo '" + select + "' | " + gMySQL + gMySQLConnectString + " -N gratia " )

def RunQueryAndSplit(select):
        res = RunQuery(select)
        LogToFile(res)
        lines = res.split("\n")
        return lines

def GetQuery(begin,end):
	return "select CETable.facility_name as ExecutingSite, VOName as LCGUserVO, Sum(NJobs),Round(Sum(CpuUserDuration+CpuSystemDuration)/3600) as SumCPU,round(Sum(CpuUserDuration+CpuSystemDuration)/3600/"+str(gNormalization)+") as NormSumCPU, Round(Sum(WallDuration)/3600) as SumWCT, Round(Sum(WallDuration)/3600/"+str(gNormalization)+") as NormSumWCT,date_format(min(EndTime),\"%m\") as Month, date_format(min(EndTime),\"%Y\") as Year, date_format(min(EndTime),\"%Y-%m-%d\") as RecordStart,date_format(max(EndTime),\"%Y-%m-%d\") as RecordEnd " \
	   + " from VOProbeSummary Main, CEProbes, CETable where " \
	   + "\"" + DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" \
	   + " and Main.ProbeName = CEProbes.ProbeName and CEProbes.facility_id = CETable.facility_id group by Main.ProbeName, VOName"

def GetNormQuery(begin,end):
    return "select sum(score)/count(*) from (SELECT I.BenchmarkScore/1000 as score " + \
        "FROM gratia_psacct.NodeSummary H, gratia_psacct.CPUInfo I " + \
        "where " + "\"" + DateToString(begin) +"\"<=EndTime and EndTime<\"" + DateToString(end) + "\"" + \
        "and ProbeName = \"psacct:USCMS-FNAL-WC1-CE\" " + \
        "and H.HostDescription = I.NodeName " + \
        "group by Node) as sub"

def SetNormalization(begin,end):
    global gNormalization
    res = RunQueryAndSplit(GetNormQuery(begin,end))
    gNormalization = string.atof(res[0])
    

ReportableSites = [
    # CMS
    'USCMS-FNAL-WC1-CE',
    'GLOW' ,
    'CIT_CMS_T2',
    'MIT_CMS',
    'Nebraska Tier 2 Center',
    'Purdue-Lear'
    'Purdue-RCAC',
    'SPRACE',
    'UCSanDiegoPG',
    'UFlorida-PG',
    # ATLAS
    'UTA_DPCC' 
    ]

def ReportableSite(sitename):
    return sitename in ReportableSites

def CreateLCGsql(begin,end):

        SetNormalization(begin,end)
	
	lines = RunQueryAndSplit(GetQuery(begin,end))
	for i in range (0,len(lines)):
		val = lines[i].split('\t')
                if (ReportableSite(val[0])):
                    output =  "insert into `OSG_DATA` VALUES " + str(tuple(val)) + ";"
                    print output
		

def main(argv=None):
	global gBegin,gEnd
	
	UseArgs(argv)

	if not CheckDB() :
		return 1

	CreateLCGsql(gBegin,gEnd)

if __name__ == "__main__":
    sys.exit(main())

#
# Author Philippe Canal
#
# LCG 
#
# library to transfer the data from Gratia to APEL (WLCG)
#
#@(#)gratia/summary:$Name: not supported by cvs2svn $:$Id: LCG.py,v 1.1 2007-03-02 21:22:09 pcanal Exp $

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
        start = argv[len(opts)+1]
        if len(argv) > len(opts) + 2:
                end =  argv[len(opts)+2]
        SetDate(start,end)
    for o, a in opts:
        if o in ("--output"):
                gOutput = a

def SetDate(start,end):
    " Set the start and begin by string"
    global gBegin, gEnd
    if len(start) > 0:
        gBegin = datetime.date(*time.strptime(start, "%Y/%m/%d")[0:3]) 
    if len(end) > 0:
        gEnd = datetime.date(*time.strptime(end, "%Y/%m/%d")[0:3]) 

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

def GetQuery(begin,end):
	return "select CETable.facility_name as ExecutingSite, VOName as LCGUserVO, Sum(NJobs),Sum(CpuUserDuration+CpuSystemDuration)/3600 as SumCPU,Sum(CpuUserDuration+CpuSystemDuration)/3600/"+str(gNormalization)+" as NormSumCPU, Sum(WallDuration)/3600 as SumWCT, Sum(WallDuration)/3600/"+str(gNormalization)+" as NormSumWCT,date_format(min(EndTime),\"%m\") as Month, date_format(min(EndTime),\"%Y\") as Year, date_format(min(EndTime),\"%Y-%m-%d\") as RecordStart,date_format(max(EndTime),\"%Y-%m-%d\") as RecordEnd " \
	   + " from VOProbeSummary Main, CEProbes, CETable where " \
	   + "\"" + DateToString(begin) +"\"<EndTime and EndTime<\"" + DateToString(end) + "\"" \
	   + " and Main.ProbeName = CEProbes.ProbeName and CEProbes.facility_id = CETable.facility_id group by Main.ProbeName, VOName"

def CreateLCGsql(begin,end):
	
	lines = RunQuery(GetQuery(begin,end))
	print lines[1]
	for i in range (0,3): 
		# len(lines)):
		val = lines[i].split('\t')
		print "insert into `SumCPU` VALUES "
		print val
		print "\n"
		

def main(argv=None):
	global gBegin,gEnd
	
	UseArgs(argv)

	if not CheckDB() :
		return 1

	CreateLCGsql(gBegin,gEnd)

if __name__ == "__main__":
    sys.exit(main())
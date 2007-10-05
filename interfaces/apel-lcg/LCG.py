########################################################################
# 
# Author Philippe Canal, John Weigand
#
# LCG 
#
# Script to transfer the data from Gratia to APEL (WLCG)
########################################################################
#
#@(#)gratia/summary:$Name: not supported by cvs2svn $:$Id: LCG.py,v 1.8 2007-10-05 12:35:06 jgweigand Exp $
#
#
########################################################################
# Changes:
# 6/19/07 (John Weigand)
#   Instead of using 1 normalization factor for all sites, the
#   lcg-reportableSites now contains a normalization factor for 
#   those sites that have reported on the OSG survey.  Based on that
#   survey, using a Specint2000 specification, individual normalization
#   factors have been assigned to reflect the type of hardware those
#   site's WN clusters are using.  This factor is the 2nd token in that
#   lcg-reportableSites file.  If no token is present, the normalization
#   factor used is derived from the probe specified in the lcg.conf file.
#   This changed required that individual queries of the gratia database
#   be performed on a site basis since the application of the normalization
#   factor is applied there.  The query sent to the log file represents the 
#   first one used in order to reduce the verbage there.  The site and
#   normalization factor used is displayed there.
#   - Another item to note is that coincident with this change the
#     BNL_ATLAS_1 and BNL_ATLAS_2 sites are now being reported as BNL_OSG
#     at that administrators request.  This was a database change to the
#     relation of the Site and Probe tables via siteid.
#
# 9/12/07 (John Weigand)
#   Fixed a problem when an empty set is returned from the query.
# 
########################################################################
import traceback
import exceptions
import time
import datetime
import getopt
import math
import re
import string
import smtplib, rfc822  # for email notifications via smtp
import commands, os, sys, time, string

gSitesWithNoData = []
gSitesWithNoUnknowns = []
gKnownVOs = {}

gFilterParameters = {"SiteFilterFile"       :None,
                     "VOFilterFile"         :None,
                     "DBConfFile"           :None,
                     "LogSqlDir"            :None,
                     "NormalizationProbe"   :None,
                     "NormalizationPeriod"  :None,
                     "NormalizationDefault" :None,
                     "EmailNotice"          :None,
                    }
gDatabaseParameters = {"GratiaHost":None,
                       "GratiaPort":None,
                       "GratiaUser":None,
                       "GratiaPswd":None,
                       "GratiaDB"  :None,
                       "LcgHost"   :None,
                       "LcgPort"   :None,
                       "LcgUser"   :None,
                       "LcgPswd"   :None,
                       "LcgDB"     :None,
                       "LcgTable"  :None,
                      }



# ------------------------------------------------------------------
# -- Default is query only. Must specify --update to effect updates 
# -- This is to protect against accidental running of the script   
gProgramName        = None
gFilterConfigFile   = None
gDateFilter         = None
gNormalization      = None
gInUpdateMode       = False  
gEmailNotificationSuppressed = False  #Command line arg to suppress email notice


#-----------------------------------------------
def Usage():
  """ Display usage """
  print  """\
this is the usage
"""


#-----------------------------------------------
def GetArgs(argv):
    global gProgramName,gDateFilter,gInUpdateMode,gEmailNotificationSuppressed,gProbename,gOutput,gFilterConfigFile
    if argv is None:
        argv = sys.argv
    gProgramName = argv[0]
    arglist=["help","no-email","date=","update","config="]

    try:
      opts, args = getopt.getopt(argv[1:], "", arglist)
    except getopt.error, e:
      msg = e.__str__()
      raise Exception("""Invalid command line argument 
%s 
For help use --help
""" % msg)

    for o, a in opts:
      if o in ("--help"):
        print """
For usage and a complete explanation of this interface, see the
README--Gratia-APEL-interface file as there is too much to explain here. 

...BYE"""
        sys.exit(0)
      if o in ("--config"):
        gFilterConfigFile = a
        continue
      if o in ("--no-email"):
        gEmailNotificationSuppressed = True
        continue
      if o in ("--update"):
        gInUpdateMode = True
        continue
      if o in ("--date"):
        gDateFilter = a
        if gDateFilter  == "current":
          gDateFilter = GetCurrentPeriod()
        if gDateFilter  == "previous":
          gDateFilter = GetPreviousPeriod()
        continue
      raise Exception("""Invalid command line argument""")
    
    #---- required arguments ------
    if gFilterConfigFile == None:
      raise Exception("--config is a required argument")
    if gDateFilter == None:
      raise Exception("--date is a required argument")
   
#-----------------------------------------------
def SendEmailNotificationFailure(error):
  """ Sends a failure  email notification to the EmailNotice attribute""" 
  subject  = "Gratia transfer to APEL (WLCG) for %s - FAILED" % gDateFilter
  message  = "ERROR: " + error
  SendEmailNotification(subject,message)
#-----------------------------------------------
def SendEmailNotificationSuccess(contents):
  """ Sends a successful email notification to the EmailNotice attribute""" 
  subject  = "Gratia transfer to APEL (WLCG) for %s - Completed" % gDateFilter
  SendEmailNotification(subject,contents)

#-----------------------------------------------
def SendEmailNotification(subject,message):
  """ Sends an email notification to the EmailNotice attribute value
      of the lcg-filters.conf file.  This can be overridden on the command
      line to suppress the notification.  This should only be done during
      testing, otherwise it is best to provide some notification on failure.
  """
  if gEmailNotificationSuppressed:
    Logit("Email notification suppressed")
    return
  
  hostname = commands.getoutput("hostname -f")
  logfile  = commands.getoutput("echo $PWD")+"/"+GetFileName("log")
  username = commands.getoutput("whoami")
  sqlfile  = commands.getoutput("echo $PWD")+"/"+GetFileName("sql")
  body = """\
Gratia to APEL/WLCG transfer. 
Script..... %s
Node....... %s
User....... %s
Log file... %s
SQL file... %s

This is normally run as a cron process.  The log files associated with this 
process can provide further details.

Sites with no data:
  %s

Sites with no unknown atlas VOs:
  %s

%s
	""" % (gProgramName,hostname,username,logfile,sqlfile,gSitesWithNoData,gSitesWithNoUnknowns,message)


  try:
    fromaddr = gFilterParameters["EmailNotice"]
    toaddrs  = gFilterParameters["EmailNotice"]
    server   = smtplib.SMTP('localhost')
    server.set_debuglevel(0)
    message = """\
From: %s
To: %s
Subject: %s
X-Mailer: Python smtplib
%s
""" % (fromaddr,toaddrs,subject,body)
    server.sendmail(fromaddr,toaddrs,message)
    server.quit()
  except smtplib.SMTPSenderRefused:
    raise Exception("SMTPSenderRefused, message: %s" % message)
  except smtplib.SMTPRecipientsRefused:
    raise Exception("SMTPRecipientsRefused, message: %s" % message)
  except smtplib.SMTPDataError:
    raise Exception("SMTPDataError, message: %s" % message)
  except:
    raise Exception("Unsent Message: %s" % message)

  Logit("Email notification being sent to %s" % gFilterParameters["EmailNotice"])

#-----------------------------------------------
def GetVOFilters(filename):
  """ Reader for a file of reportable VOs . 
      The file contains a single entry for each filter value.  
      The method returns a formated string for use in a SQL
      'where column_name in ( filters )' structure.
  """
  try:
    filters = ""
    fd = open(filename)
    while 1:
      filter = fd.readline()
      if filter == "":   # EOF
        break
      filter = filter.strip().strip("\n")
      if filter.startswith("#"):
        continue
      if len(filter) == 0:
        continue
      filters = '"' + filter + '",' + filters
    filters = filters.rstrip(",")  # need to remove the last comma
    fd.close()
    return filters
  except IOError, (errno,strerror):
    raise Exception("IO error(%s): %s (%s)" % (errno,strerror,filename))

#-----------------------------------------------
def GetSiteFilters(filename):
  """ Reader for a file of reportable sites. 
      The file contains 2 tokens: the site name and a normalization factor.  
      The normalization factor is optional.  If not available, a default
      normalization factor will be set using the gNormalization value.
      The method returns a hash table with the key being site and the value
      the normalization factor to use.
  """
  try:
    #--- verify the gNormalization value has been set --
    if gNormalization == None:
      raise Exception("Internal error: something in the logic is screwed up. The gNormalization variable must be set before this method (GetSiteFilters) is called.")
    #--- process the reportable sites file ---
    sites = {}
    fd = open(filename)
    while 1:
      filter = fd.readline()
      if filter == "":   # EOF
        break
      filter = filter.strip().strip("\n")
      if filter.startswith("#"):
        continue
      if len(filter) == 0:
        continue
      site = filter.split()
      if sites.has_key(site[0]):
        raise Exception("System error: duplicate - site (%s) already set" % site[0])
      factor = 0
      if len(site) == 1:
        sites[site[0]] = gNormalization
      elif len(site) > 1:
        #-- verify the factor is an integer --
        try:
          tmp = int(site[1])
          factor = float(site[1])/1000
        except:
          raise Exception("Error in %s file: 2nd token must be an integer (%s" % (filename,filter))
        #-- set the factor --
        sites[site[0]] = factor
      else:
        continue
    #-- end of while loop --
    fd.close()
    #-- verify there is at least 1 site --
    if len(sites) == 0:
      raise Exception("Error in %s file: there are no sites to process" % filename)
    return sites
  except IOError, (errno,strerror):
    raise Exception("IO error(%s): %s (%s)" % (errno,strerror,filename))

#----------------------------------------------
def GetDBConfigParams(filename):
  """ Retrieves and validates the database configuration file parameters"""

  params = GetConfigParams(filename)
  for key in gDatabaseParameters.keys():
    if params.has_key(key):
      gDatabaseParameters[key] = params[key]
      continue
    raise Exception("Required parameter (%s) missing in config file %s" % (key,filename))

#----------------------------------------------
def GetFilterConfigParams(filename):
  """ Retrieves and validates the filter configuration file parameters"""

  params = GetConfigParams(filename)
  for key in gFilterParameters.keys():
    if params.has_key(key):
      gFilterParameters[key] = params[key]
      continue
    raise Exception("Required parameter (%s) missing in config file %s" % (key,filename))

#----------------------------------------------
def GetConfigParams(filename):
  """ Generic reader of a file containing configuration parameters.
      The format of the file is 'parameter_name parameter value'.
      e.g.- GratiaHost gratia-db01.fnal.gov
      The method returns a hash table (dictionary in python terms).
  """
  try:
    params = {}
    fd = open(filename)
    while 1:
      line = fd.readline()
      if line == "":   # EOF
        break
      line = line.strip().strip("\n")
      if line.startswith("#"):
        continue
      if len(line) == 0:
        continue
      values = line.split()
      if len(values) <> 2:
        message = "Invalid config file entry ("+line+") in file ("+filename+")"
        raise Exception(message)
      params[values[0]]=values[1]
    fd.close()
    return params
  except IOError, (errno,strerror):
    raise Exception("IO error(%s): %s (%s)" % (errno,strerror,filename))

#---------------------------------------------
def GetCurrentPeriod():
  """ Gets the current time in format for the date filter YYYY?MM 
      This will always be the current month.
  """
  return time.strftime("%Y/%m",time.localtime())
#-----------------------------------------------
def GetPreviousPeriod():
  """ Gets the previous time in format for the date filter YYYY?MM 
      This is done to handle the lag in getting all accounting data
      for the previous month in Gratia.  It will back off to the
      previous month from when this is run.
  """
  t = time.localtime(time.time())
  if t[1] == 1:
    prevMos = [t[0]-1,12,t[2],t[3],t[4],t[5],t[6],t[7],t[8],]
  else:
    prevMos = [t[0],t[1]-1,t[2],t[3],t[4],t[5],t[6],t[7],t[8],]
  return time.strftime("%Y/%m",prevMos)

#-----------------------------------------------
def GetCurrentTime():
  return time.strftime("%Y-%m-%d %H:%M:%S",time.localtime())
#-----------------------------------------------
def Logit(message):
    LogToFile(GetCurrentTime() + " " + message)
#-----------------------------------------------
def Logerr(message):
    Logit("ERROR: " + message)
    

#-----------------------------------------------
def LogToFile(message):
    "Write a message to the Gratia log file"

    file = None
    filename = ""
    try:
        filename = GetFileName("log")
        file = open(filename, 'a')  
        file.write( message + "\n")
        if file != None:
          file.close()
    except IOError, (errno,strerror):
      raise Exception,"IO error(%s): %s (%s)" % (errno,strerror,filename)

#-----------------------------------------------
def GetFileName(prefix):
    """ Sets the file name to YYYY-MM.prefix based on the time
        period for the transfer with the LogSqlDir
        attribute of the filters configuration prepended to it.
    """
    if gDateFilter == None:
      filename = time.strftime("%Y-%m") + "." + prefix
    else:
      filename = gDateFilter[0:4] + "-" + gDateFilter[5:7] + "." + prefix 
    if gFilterParameters["LogSqlDir"] == None:  
      filename = "./" + filename
    else:
      filename = gFilterParameters["LogSqlDir"] + "/" + filename
    if not os.path.exists(gFilterParameters["LogSqlDir"]):
      os.mkdir(gFilterParameters["LogSqlDir"])
    return filename

#-----------------------------------------------
def CheckGratiaDBAvailability(params):
  """ Checks the availability of the Gratia database. """
  CheckDB(params["GratiaHost"], 
          params["GratiaPort"], 
          params["GratiaUser"], 
          params["GratiaPswd"], 
          params["GratiaDB"]) 

#-----------------------------------------------
def CheckLcgDBAvailability(params):
  """ Checks the availability of the LCG database. """
  CheckDB(params["LcgHost"], 
          params["LcgPort"], 
          params["LcgUser"], 
          params["LcgPswd"], 
          params["LcgDB"]) 

#-----------------------------------------------
def CheckDB(host,port,user,pswd,db):
  """ Checks the availability of a MySql database. """

  Logit("Checking availability on %s:%s of %s database" % (host,port,db))
  connectString = " --defaults-extra-file='%s' -h %s --port=%s -u %s %s " % (pswd,host,port,user,db)
  command = "mysql %s -e status" % connectString
  (status, output) = commands.getstatusoutput(command)
  if status == 0:
    msg =  "Status: \n"+output
    if output.find("ERROR") >= 0 :
      msg = "Error in running mysql:\n  %s\n%s" % (command,output)
      raise Exception(msg)
  else:
    msg = "Error in running mysql:\n  %s\n%s" % (command,output)
    raise Exception(msg)
  Logit("Status: available")
      
#-----------------------------------------------
def SetQueryDates():
  """ Sets the beginning and ending dates for the basic Gratia query.
      This is always 1 month.
  """
  return SetDateFilter(1)
#-----------------------------------------------
def SetNormalizationDates():
  """ Sets the beginning and ending dates for the Gratia query used to 
      determining the normalization factor used.
      This is set be a parameter in the configuration file.
  """
  return SetDateFilter(gFilterParameters["NormalizationPeriod"]) 
#-----------------------------------------------
def SetDateFilter(interval):
    """ Given the month and year (YYYY/MM, returns the starting and 
        ending periods for the query.  
        The beginning date will be offset from the date (in months) 
        by the interval provided. 
        The beginning date will always be the 1st of the month.
        The ending date will always be the 1st of the next month to
        insure a complete set of monthly data is selected.
    """
    Logit("Setting begin and end periods with interval of %s months" % interval)
    # --- set the begin date compensating for year change --
    t = time.strptime(gDateFilter, "%Y/%m")[0:3]
    interval = int(interval)
    if t[1] < interval:
      new_t = (t[0]-1,13-(interval-t[1]),t[2])
    else:
      new_t = (t[0],t[1]-interval+1,t[2])
    begin = datetime.date(*new_t)
    # --- set the end date compensating for year change --
    if t[1] == 12:
      new_t = (t[0]+1,1,t[2])
    else:
      new_t = (t[0],t[1]+1,t[2])
    end = datetime.date(*new_t)
    return begin,end

#-----------------------------------------------
def DateToString(input,gmt=True):
    """ Converts an input date in YYYY/MM format local or gmt time """
    if gmt:
        return input.strftime("%Y-%m-%d 00:00:00")
    else:
        return input.strftime("%Y-%m-%d")

#-----------------------------------------------
def GetNormalizationQuery(normalizationProbe):
    """ Returns the query used to get the normalization factor."""

    begin,end = SetNormalizationDates()
    strBegin =  DateToString(begin)
    strEnd   =  DateToString(end)
    query = """\

SELECT sum(score)/count(*) FROM 
 (SELECT I.BenchmarkScore/1000 AS score
  FROM gratia_psacct.NodeSummary H, 
     gratia_psacct.CPUInfo I 
  WHERE "%s" <= EndTime and EndTime < "%s"
    AND ProbeName = "%s" 
    AND H.HostDescription = I.NodeName 
  GROUP BY Node) 
AS sub;""" % (strBegin,strEnd,normalizationProbe)
    Logit("Normalization query: %s" % query)
    return query

#-----------------------------------------
def SetNormalizationFactor(query,params):
    """ Sets the normalization factor applied to all data."""
    results = RunGratiaQuery(query,params)
    if results == "":
      Logit("WARNING: no data return from query")
      normalizationFactor = gFilterParameters["NormalizationDefault"]
      Logit("WARNING: Using default normalization factor")
    else:
      lines = results.split("\n")
      normalizationFactor = "%.6f" % string.atof(lines[0])
    Logit("Normalization factor: %s" % normalizationFactor)
    return normalizationFactor

#-----------------------------------------------
def GetQuery(site,normalizationFactor,vos):
    """ Creates the SQL query DML statement for the Gratia database """
    begin,end = SetQueryDates()
    strBegin =  DateToString(begin)
    strEnd   =  DateToString(end)
    strNormalization = str(normalizationFactor)
    fmtMonth = "%m"
    fmtYear  = "%Y"
    fmtDate  = "%Y-%m-%d"
    query="""\
SELECT Site.SiteName AS ExecutingSite, 
               VOName as LCGUserVO, 
               Sum(NJobs), 
               Round(Sum(CpuUserDuration+CpuSystemDuration)/3600) as SumCPU, 
               Round((Sum(CpuUserDuration+CpuSystemDuration)/3600)*%s) as NormSumCPU, 
               Round(Sum(WallDuration)/3600) as SumWCT, 
               Round((Sum(WallDuration)/3600)*%s) as NormSumWCT, 
               date_format(min(EndTime),"%s")       as Month, 
               date_format(min(EndTime),"%s")       as Year, 
               date_format(min(EndTime),"%s") as RecordStart, 
               date_format(max(EndTime),"%s") as RecordEnd, 
               "%s",
               NOW() 
from 
     Site,
     Probe,
     VOProbeSummary Main 
where 
      Site.SiteName = "%s"
  and Site.siteid = Probe.siteid 
  and Probe.ProbeName  = Main.ProbeName 
  and Main.VOName in ( %s )
  and "%s" <= Main.EndTime and Main.EndTime < "%s"
group by ExecutingSite, 
         LCGUserVO
""" % (strNormalization,strNormalization,fmtMonth,fmtYear,fmtDate,fmtDate,strNormalization,site,vos,strBegin,strEnd)
    return query

#-----------------------------------------------
def GetQueryAtlasUnknowns(site,normalizationFactor,vos):
    """ Creates the SQL query DML statement for the Gratia database 
        This is special to pick up those with an 'Unknown VO' for atlas.
    """
    begin,end = SetQueryDates()
    strBegin =  DateToString(begin)
    strEnd   =  DateToString(end)
    strNormalization = str(normalizationFactor)
    fmtMonth = "%m"
    fmtYear  = "%Y"
    fmtDate  = "%Y-%m-%d"
    percent  = "%"
    query="""\
SELECT Site.SiteName AS ExecutingSite, 
               "us%s" as LCGUserVO, 
               Sum(R.NJobs), 
               Round(Sum(R.CpuUserDuration+R.CpuSystemDuration)/3600) as SumCPU, 
               Round((Sum(R.CpuUserDuration+R.CpuSystemDuration)/3600)*%s) as NormSumCPU, 
               Round(Sum(R.WallDuration)/3600) as SumWCT, 
               Round((Sum(R.WallDuration)/3600)*%s) as NormSumWCT, 
               date_format(min(R.EndTime),"%s")       as Month, 
               date_format(min(R.EndTime),"%s")       as Year, 
               date_format(min(R.EndTime),"%s") as RecordStart, 
               date_format(max(R.EndTime),"%s") as RecordEnd, 
               "%s",
               NOW() 
from 
     Site,
     Probe,
     JobUsageRecord_Meta M,
     JobUsageRecord R
where 
      Site.SiteName = "%s"
  and Site.siteid   = Probe.siteid 
  and Probe.ProbeName  = M.ProbeName 
  and M.dbid           = R.dbid
  and R.VOName = "Unknown"
  and "%s" <= R.EndTime and R.EndTime < "%s"
  and R.LocalUserid like "%s%s%s"  
group by ExecutingSite, 
         LCGUserVO
""" % (vos,strNormalization,strNormalization,fmtMonth,fmtYear,fmtDate,fmtDate,strNormalization,site,strBegin,strEnd,percent,vos,percent)
    return query


#-----------------------------------------------
def RunGratiaQuery(select,params):
  """ Runs the query of the Gratia database """
  Logit("Running query on %s of the %s database" % (params["GratiaHost"],params["GratiaDB"]))
  host = params["GratiaHost"]
  port = params["GratiaPort"] 
  user = params["GratiaUser"] 
  pswd = params["GratiaPswd"] 
  db   = params["GratiaDB"]

  connectString = CreateConnectString(host,port,user,pswd,db)
  (status,output) = commands.getstatusoutput("echo '" + select + "' | " + connectString)
  return EvaluateMySqlResults((status,output))

#-----------------------------------------------
def RunLCGUpdate(inputfile,params):
  """ Performs the update of the APEL database """
  Logit("Running update on %s of the %s database" % (params["LcgHost"],params["LcgDB"]))
  host = params["LcgHost"]
  port = params["LcgPort"] 
  user = params["LcgUser"] 
  pswd = params["LcgPswd"] 
  db   = params["LcgDB"]

  connectString = CreateConnectString(host,port,user,pswd,db)
  (status,output) = commands.getstatusoutput("cat '" + inputfile + "' | " + connectString)
  return EvaluateMySqlResults((status,output))

#------------------------------------------------
def CreateConnectString(host,port,user,pswd,db):
  return "mysql --defaults-extra-file='%s' --disable-column-names -h %s --port=%s -u %s %s " % (pswd,host,port,user,db)

#------------------------------------------------
def EvaluateMySqlResults((status,output)):
  """ Evaluates the output of a MySql execution using the 
      getstatusoutput command.
  """
  Logit("Results:")
  if status == 0:
    if output.find("ERROR") >= 0 :
      raise Exception("MySql error:  %s" % (output))
  else:
    raise Exception("Status (non-zero rc): rc=%d - %s " % (status,output))

  if output == "NULL": 
    LogToFile("... empty results set")
    output = ""
  else:
    LogToFile(output)
  return output

#-----------------------------------------------
def CreateLCGsql(results,filename):
  """ Creates the SQL DML to update the LCG database.

      Some special processing is applied here to facillitate handling any
      site name or vo name changes that may be applied to the gratia
      database.  This is a bit of a hack.  For a given period (month), we
      delete all existing table records, then add back the new data.
      This is all done in the same transaction scope.

      We are also maintaining the latest sql update dml in a file
      called YYYY-MM.sql.
  """ 
  Logit("Creating update sql DML for the LCG database:") 

  if len(results) == 0:
    raise Exception("No updates to apply")

  file = open(filename, 'w')
  lines = results.split("\n")

  file.write("set autocommit=0;\n")

  dates = gDateFilter.split("/")  # YYYY/MM format
  file.write("DELETE FROM %s WHERE Month = %s AND Year = %s ;\n" % (gDatabaseParameters["LcgTable"],dates[1],dates[0]))
        
  for i in range (0,len(lines)):  
    val = lines[i].split('\t')
    if len(val) < 13:
      continue
    output =  "INSERT INTO %s VALUES " % (gDatabaseParameters["LcgTable"]) + str(tuple(val)) + ";"
    file.write(output+"\n")
    LogToFile(output)
    gKnownVOs[val[0] +'/'+val[1]] ="1"
        
  file.write("commit;\n")
  file.close()

#-----------------------------------------------
def CreateLCGsqlUnknownUpdates(results,filename):
  """ Creates the SQL DML to update the LCG database.

      This update will actually be a sql dml 'UPDATE' which
      is different from the  'INSERT's done for normal queries.
      This will allow us to add the results to that has already been 
      added.  This will cause the RecordStart and RecordEnd dates to
      likely not be accurate.

      We are also maintaining the latest sql update dml in a file
      called YYYY-MM.sql.
  """ 
  Logit("Creating update sql DML for the LCG database for Unknown atlas VOs:") 

  if len(results) == 0:
    raise Exception("No updates to apply")

  file = open(filename, 'a')
  lines = results.split("\n")

  file.write("set autocommit=0;\n")

  dates = gDateFilter.split("/")  # YYYY/MM format
        
  for i in range (0,len(lines)):  
    val = lines[i].split('\t')
    if len(val) < 13:
      continue
    if gKnownVOs.has_key(val[0] +'/'+val[1]):
      output =  "UPDATE %s SET " % (gDatabaseParameters["LcgTable"])
      output =  output + " NJobs=Njobs+%s, "            % (int(val[2]))
      output =  output + " SumCPU=SumCPU+%s, "          % (int(val[3]))
      output =  output + " NormSumCPU=NormSumCPU+%s, "  % (int(val[4]))
      output =  output + " SumWCT=SumWCT+%s, "          % (int(val[5]))
      output =  output + " NormSumWCT=NormSumWCT+%s, "  % (int(val[6]))
      output =  output + " Month=%s, "                  % (int(val[7]))
      output =  output + " YEAR=%s, "                   % (int(val[8]))
      output =  output + " RecordStart='%s', "            % (val[9])
      output =  output + " RecordEnd='%s', "              % (val[10])
      output =  output + " NormFactor=%s,"              % (val[11])
      output =  output + " MeasurementDate = '%s'  "    % (val[12])
      output =  output + " WHERE ExecutingSite = '%s' " % (val[0])
      output =  output + "   AND LCGUserVO = '%s' " % (val[1])
      output =  output + "   AND Month = %s " % (val[7])
      output =  output + "   AND Year  = %s " % (val[8])
      output =  output + ";"
    else:
      output =  "INSERT INTO %s VALUES " % (gDatabaseParameters["LcgTable"]) + str(tuple(val)) + ";"
    file.write(output+"\n")
    LogToFile(output)
                
  file.write("commit;\n")
  file.close()

#--- MAIN --------------------------------------------
def main(argv=None):
  global gNormalization
  global gSitesWithNoData
  global gSitesWithNoUnknowns
  firstTime = 1

  #--- get command line arguments  -------------
  try:
    GetArgs(argv)
  except Exception, e:
    print >>sys.stderr, e.__str__()
    return 1
  

  try:      
    #--- get paramters -------------
    GetFilterConfigParams(gFilterConfigFile)
    GetDBConfigParams(gFilterParameters["DBConfFile"])
    Logit("====================================================")
    Logit("Starting transfer from Gratia to APEL")
    Logit("Filter date: %s" % gDateFilter)
 
    #--- check db availability -------------
    CheckGratiaDBAvailability(gDatabaseParameters)
    if gInUpdateMode:
      CheckLcgDBAvailability(gDatabaseParameters)

    #--- set the default normalization factor --------------
    query = GetNormalizationQuery(gFilterParameters["NormalizationProbe"])
    gNormalization =  SetNormalizationFactor(query,gDatabaseParameters)

    #--- get all filters -------------
    ReportableSites    = GetSiteFilters(gFilterParameters["SiteFilterFile"])
    ReportableVOs      = GetVOFilters(gFilterParameters["VOFilterFile"])
    
    #--- query gratia for each site and create updates (appending them ----
    output = ""
    sites = ReportableSites.keys()
    for site in sites:
      normalizationFactor = ReportableSites[site]
      query = GetQuery(site, normalizationFactor, ReportableVOs)
      if firstTime:
        Logit("Query:")
        LogToFile(query)
        firstTime = 0
      Logit("Site: %s  Normalization Factor: %s" % (site,normalizationFactor)) 
      results = RunGratiaQuery(query,gDatabaseParameters)
      if len(results) == 0:
        gSitesWithNoData.append(site)
        continue
      output = output + results + "\n"

    ##### UNKNOWNS#########
    #--- query gratia for Unknown VOs and create updates (appending them ----
    unknowns = ""
    firstTime = 1
    sites = ReportableSites.keys()
    for site in sites:
      normalizationFactor = ReportableSites[site]
      query = GetQueryAtlasUnknowns(site,normalizationFactor,"atlas")
      if firstTime:
        Logit("Query:")
        LogToFile(query)
        firstTime = 0
      Logit("Site (Unknown atlas VO): %s  Normalization Factor: %s" % (site,normalizationFactor)) 
      results = RunGratiaQuery(query,gDatabaseParameters)
      if len(results) == 0:
        gSitesWithNoUnknowns.append(site)
        continue
      unknowns = unknowns + results + "\n"

    #--- update the APEL accounting database ----
    Logit("Sites with no data: %s" % gSitesWithNoData)
    Logit("Sites with no atlas unknown VOs: %s" % gSitesWithNoUnknowns)
    CreateLCGsql(output,GetFileName("sql"))
    CreateLCGsqlUnknownUpdates(unknowns,GetFileName("sql"))
    if gInUpdateMode:
      RunLCGUpdate(GetFileName("sql"),gDatabaseParameters)
      SendEmailNotificationSuccess("Sample query:\n" + query + "\n\nResults of all queries:\n" + output)
      Logit("Transfer Completed SUCCESSFULLY from Gratia to APEL")
    else:
      Logit("The --update arg was not specified. No updates attempted.")
    Logit("====================================================")

  except Exception, e:
    SendEmailNotificationFailure(e.__str__())
    Logit("Transfer FAILED from Gratia to APEL.")
    Logerr(e.__str__())
    Logit("====================================================")
    return 1

  return 0

if __name__ == "__main__":
    sys.exit(main())


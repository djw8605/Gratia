########################################################################
# 
# Author Philippe Canal, John Weigand
#
# LCG 
#
# Script to transfer the data from Gratia to APEL (WLCG)
########################################################################
#
#@(#)gratia/summary:$HeadURL: https://gratia.svn.sourceforge.net/svnroot/gratia/trunk/interfaces/apel-lcg/LCG.py $:$Id: LCG.py 3000 2009-02-16 16:15:08Z pcanal 
#
#
########################################################################
# Changes:
#
########################################################################
import Downtimes
import InactiveResources
import InteropAccounting
import Rebus
import SSMInterface

import traceback
import exceptions
import time
import calendar
import datetime
import getopt
import math
import re
import string
import popen2
import smtplib, rfc822  # for email notifications via smtp
import commands, os, sys, time, string

downtimes          = Downtimes.Downtimes()
inactives          = InactiveResources.InactiveResources()
gVoOutput          = ""
gUserOutput        = ""
gWarnings          = []
gSitesWithData     = []
gSitesMissingData  = {}
gSitesWithNoData   = [] 
gKnownVOs = {}

gFilterParameters = {"GratiaCollector"      :None,
                     "SiteFilterFile"       :None,
                     "SiteFilterHistory"    :None,
                     "VOFilterFile"         :None,
                     "DBConfFile"           :None,
                     "LogSqlDir"            :None,
                     "MissingDataDays"      :None,
                     "FromEmail"            :None,
                     "ToEmail"              :None,
                    }
gDatabaseParameters = {"GratiaHost":None,
                       "GratiaPort":None,
                       "GratiaUser":None,
                       "GratiaPswd":None,
                       "GratiaDB"  :None,
                       "SSMHome"   :None,
                       "SSMConfig" :None,
                       "SSMupdates":None,
                       "SSMdeletes":None,
                      }



# ------------------------------------------------------------------
# -- Default is query only. Must specify --update to effect updates 
# -- This is to protect against accidental running of the script   
gProgramName        = None
gFilterConfigFile   = None
gDateFilter         = None
gInUpdateMode       = False  
gEmailNotificationSuppressed = False  #Command line arg to suppress email notice
gMyOSG_available    = True  # flag indicating if there is a problem w/MyOSG


# ----------------------------------------------------------
# special global variables to display queries in the email 
gVoQuery      = ""
gUserQuery    = ""

# -----------------------------------------------------------------------
# Used to get the set of resource for the resource groups being reported
gInteropAccounting = InteropAccounting.InteropAccounting()

# -----------------------------------------------------------------------
# Used to validate MyOSG Interoperability against WLCG Rebus topology
# to verify is a site/resource group is registered
gRebus = Rebus.Rebus()

#-----------------------------------------------
def Usage():
  """ Display usage """
  print  """\
this is the usage
"""


#-----------------------------------------------
def GetArgs(argv):
    global gProgramName,gDateFilter,gInUpdateMode,gEmailNotificationSuppressed,gProbename,gVoOutput,gUserOutput,gFilterConfigFile
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
   
#-----------------------------------------------
def SendEmailNotificationFailure(error):
  """ Sends a failure  email notification to the EmailNotice attribute""" 
  subject  = "Test SSM Gratia transfer to APEL (WLCG) for %s - FAILED" % gDateFilter
  message  = "ERROR: " + error
  SendEmailNotification(subject,message)
#-----------------------------------------------
def SendEmailNotificationSuccess():
  """ Sends a successful email notification to the EmailNotice attribute""" 
  global gVoOutput
  global gSitesMissingData
  subject  = "Test SSM Gratia transfer to APEL (WLCG) for %s - SUCCESS" % gDateFilter

  contents = ""
  if len(gSitesMissingData) == 0:
    contents = contents + "All sites are reporting.\n"
  else:
    contents = contents + "Sites missing data for more than %s days:" % gFilterParameters["MissingDataDays"]
    sites = gSitesMissingData.keys()
    for site in sites:
      contents = contents + "\n" + site + ": " + str(gSitesMissingData[site])
  contents = contents + "\n\nResults of all VO queries (VO):\n" + gVoOutput 
  contents = contents + "\nSample VO query:\n" + gVoQuery
  contents = contents + "\nSample User query:\n" + gUserQuery
  SendEmailNotification(subject,contents)
#-----------------------------------------------
def SendEmailNotificationWarnings():
  """ Sends a warning email notification to the EmailNotice attribute""" 
  if len(gWarnings) == 0:
    Logit("No warning conditions detected.")
    return
  Logit("Warning conditions have been detected.")
  subject  = "Test SSM Gratia transfer to APEL (WLCG) for %s - WARNINGS/ADVISORY" % gDateFilter
  contents = """
The interface from Gratia to the APEL (WLCG) database was successful.

However, the following possible problems were detected during the execution 
of the interface script and should be investigated.
"""
  for warning in gWarnings:
    contents = "%s\nWARNING/ADVISORY: %s\n" % (contents,warning)
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
  Logit("Email notification being sent to %s" % gFilterParameters["ToEmail"])
  Logit("\n" + message) 

  body = """\
Gratia to APEL/WLCG transfer. 

This is normally run as a cron process.  The log files associated with this 
process can provide further details.

Script............... %(program)s
Node................. %(hostname)s
User................. %(username)s
Log file............. %(logfile)s

SSM_HOME variable.... %(ssmhome)s 
SSM config file...... %(ssmconfig)s 
SSM summary file..... %(ssmupdates)s 
SSM summary records.. %(ssmrecs)s  
SSM deletes file..... %(ssmdeletes)s 
SSM deletes records.. %(ssmdels)s 

Reportable sites file.. %(sitefilter)s
Reportable VOs file.... %(vofilter)s

%(message)s
	""" % { "program"     : gProgramName,
                "hostname"    : commands.getoutput("hostname -f"),
                "username"    : commands.getoutput("whoami"),
                "logfile"     : commands.getoutput("echo $PWD")+"/"+GetFileName(None,"log"),
                "sitefilter"  : gFilterParameters["SiteFilterFile"],
                "vofilter"    : gFilterParameters["VOFilterFile"],
                "ssmhome"     : gDatabaseParameters["SSMHome"],
                "ssmconfig"   : gDatabaseParameters["SSMConfig"],
                "ssmupdates"  : commands.getoutput("echo $PWD")+"/"+GetFileName(gDatabaseParameters["SSMupdates"],"txt"),
                "ssmrecs"     : commands.getoutput("grep -c '%%' %s" % GetFileName(gDatabaseParameters["SSMupdates"],"txt")),
                "ssmdeletes"  : commands.getoutput("echo $PWD")+"/"+GetFileName(gDatabaseParameters["SSMdeletes"],"txt"),
                "ssmdels"     : commands.getoutput("grep -c '%%' %s" % GetFileName(gDatabaseParameters["SSMdeletes"],"txt")),
                "message"     : message,} 

  try:
    fromaddr = gFilterParameters["FromEmail"]
    toaddrs  = string.split(gFilterParameters["ToEmail"],",")
    server   = smtplib.SMTP('localhost')
    server.set_debuglevel(0)
    message = """\
From: %s
To: %s
Subject: %s
X-Mailer: Python smtplib
%s
""" % (fromaddr,gFilterParameters["ToEmail"],subject,body)
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


#-----------------------------------------------
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

#-----------------------------------------------
def GetSiteFilters(filename):
  """ Reader for a file of reportable sites. 
      The file contains 2 tokens: the site name and a normalization factor.  
      The method returns a hash table with the key being site and the value
      the normalization factor to use.
  """
  try:
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
        raise Exception("System error: No normalization factory was provide for site: %s" % site[0])
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

#----------------------------------------------
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
def DetermineReportableSitesFileToUse(reportingPeriod):
  """ This determines the configuration file of reportable sites and
      normalization factors to use.  This data is time sensitive in nature as
      the list of sites and their normalization factors change over time.
      We want to insure that we are using a list that was in effect for
      the month being reported.

      This method will always copy the current SiteFilterFile to the 
      SiteFilterHistory directory during the current month.

      So, we will always use a file in the SiteFilterHistory directory
      with the name SiteFilterFile.YYYYMM in our processing.

      If we cannot find one for the month being processed, we have to fail
      as the data will be repopulated using potentially incorrect data.

      Arguments: reporting period (YYYY/MM)
      Returns: the reportable sites file to use
  """
  filterFile  = gFilterParameters["SiteFilterFile"]
  historyDir  = gFilterParameters["SiteFilterHistory"]
  #--- make the history directory if it does not exist ---
  if not os.path.isdir(historyDir):
    Logit("... creating %s directory for the reportable sites configuration files" % (historyDir))
    os.mkdir(historyDir)

  #--- determine date suffix for history file (YYYYMM) ----
  if reportingPeriod == None:
    raise Exception("System error: the DetermineReportableSitesFileToUse method requires a reporting period argument (YYYY/MM) which is missing")
  fileSuffix = reportingPeriod[0:4] + reportingPeriod[5:7]
  historyFile = historyDir + "/" + filterFile + "." + fileSuffix

  #--- update the history only if it is for the current month --
  currentPeriod = GetCurrentPeriod()
  if currentPeriod == reportingPeriod:
    if not os.path.isfile(historyFile):
      Logwarn("The %s files should be checked to see if any updates should be made to SVN/CVS in order to retain their history." % (historyDir))

    Logit("... updating the reportable sites configuration file: %s" % (historyFile))
    os.system("cp -p %s %s" % (filterFile,historyFile))

  #--- verify a history file exists for the time period. ---
  #--- if it does not, we don't want to update           ---
  if not os.path.isfile(historyFile):
    raise Exception("A reportable sites file (%s) does not exist for this time period.  We do not want to perform an update for this time period as it may not accurately reflect what was used at that time." % (historyFile))

  return historyFile

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

#---------------------------------------------
def GetCurrentPeriod():
  """ Gets the current time in format for the date filter YYYY/MM 
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
def Logwarn(message):
    Logit("WARNING: " + message)
    gWarnings.append(message)

#-----------------------------------------------
def LogToFile(message):
    "Write a message to the Gratia log file"

    file = None
    filename = ""
    try:
        filename = GetFileName(None,"log")
        file = open(filename, 'a')  
        file.write( message + "\n")
        if file != None:
          file.close()
    except IOError, (errno,strerror):
      raise Exception,"IO error(%s): %s (%s)" % (errno,strerror,filename)

#-----------------------------------------------
def GetFileName(type,suffix):
    """ Sets the file name to YYYY-MM[.type].suffix based on the time
        period for the transfer with the LogSqlDir, the type and the 
        attribute of the filters configuration prepended to it.
    """
    if type == None:
      qualifier = ""
    else:
      qualifier = "." + type
    if gDateFilter == None:
      filename = time.strftime("%Y-%m") + qualifier + "." + suffix
    else:
      filename = gDateFilter[0:4] + "-" + gDateFilter[5:7] + qualifier + "." + suffix 
    if gFilterParameters["LogSqlDir"] == None:  
      filename = "./" + filename
    else:
      filename = gFilterParameters["LogSqlDir"] + "/" + filename
    if not os.path.exists(gFilterParameters["LogSqlDir"]):
      os.mkdir(gFilterParameters["LogSqlDir"])
    return filename

#-----------------------------------------------
def CheckGratiaDBAvailability(params):
  """ Checks the availability of the Gratia database. """
  CheckDB(params["GratiaHost"], 
          params["GratiaPort"], 
          params["GratiaUser"], 
          params["GratiaPswd"], 
          params["GratiaDB"]) 

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
      
#-----------------------------------------------
def SetDatesWhereClause():
  """ Sets the beginning and ending dates in a sql 'where' clause format
      to insure consistency on all queries. 
      This is always 1 month.
  """
  begin,end = SetDateFilter(1)
  strBegin =  DateToString(begin)
  strEnd   =  DateToString(end)
  whereClause = """ "%s" <= Main.EndTime and Main.EndTime < "%s" """ % (strBegin,strEnd)
  return whereClause

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

#-----------------------------------------------
def GetUserQuery(resource_grp,normalizationFactor,vos):
    """ Creates the SQL query DML statement for the Gratia database.
        grouping by Site/User/VO.
    """
    Logit("------ User Query: %s  ------" % resource_grp)
    return GetQuery(resource_grp,normalizationFactor,vos)

#-----------------------------------------------
def GetQuery(resource_grp,normalizationFactor,vos):
    """ Creates the SQL query DML statement for the Gratia database.
        On 5/18/09, this was changed to optionally add in CommonName
        to the query.  I chose to make it a python variable in this
        query so as not to replicate the rest of the query and take
        a chance on having it in 2 places to modify.  This is a bit
        of a gimmick but one I think is best.
        The DBflag argument, if True will allow CommonName to be included
        in the query and summary.
        On 11/04/09, this was changed to be the \"best\" of
        DistinguishedName and CommonName.

        IMPORTANT coding gimmick:
        For the ssm/activeMQ updates, the column labels MUST
        match those for the message format. They are being
        used to more easily (prgrammatically) provide the key
        for each value.
    """
    Logit("Resource Group: %(rg)s  Resources: %(resources)s NF: %(nf)s" % \
          { "rg" : resource_grp,
            "nf" : normalizationFactor,
            "resources" : GetSiteClause(resource_grp),
          })
    dates = gDateFilter.split("/")  # YYYY/MM format

    query="""\
SELECT "%(site)s"                  as Site,  
   VOName                          as VO,
   "%(month)s"                     as Month,
   "%(year)s"                      as Year,
   IF(DistinguishedName NOT IN (\"\", \"Unknown\"),IF(INSTR(DistinguishedName,\":/\")>0,LEFT(DistinguishedName,INSTR(DistinguishedName,\":/\")-1), DistinguishedName),CommonName) as GlobalUserName, 
   min(UNIX_TIMESTAMP(EndTime))    as EarliestEndTime,
   max(UNIX_TIMESTAMP(EndTime))    as LatestEndTime, 
   Round(Sum(WallDuration)/3600)                        as WallDuration,
   Round(Sum(CpuUserDuration+CpuSystemDuration)/3600)   as CpuDuration,
   Round((Sum(WallDuration)/3600) * %(nf)s )            as NormalisedWallDuration,
   Round((Sum(CpuUserDuration+CpuSystemDuration)/3600) * %(nf)s) as NormalisedCpuDuration,
   Sum(NJobs) as NumberOfJobs
from
     Site,
     Probe,
     VOProbeSummary Main
where
      Site.SiteName in (%(site_clause)s)
  and Site.siteid = Probe.siteid
  and Probe.ProbeName  = Main.ProbeName
  and Main.VOName in ( %(vos)s )
  and %(period)s
  and Main.ResourceType = "Batch"
group by Site,
         VO,
         Month,
         Year, 
         GlobalUserName
""" % { "site"             : resource_grp,
        "site_clause"      : GetSiteClause(resource_grp),
        "nf"               : str(normalizationFactor),
        "month"            : dates[1],
        "year"             : dates[0],
        "vos"              : vos,
        "period"           : SetDatesWhereClause(),
}

    return query

#-----------------------------------------------
def GetSiteClause(resource_grp):
  global gInteropAccounting
  siteClause = ""
  resources = gInteropAccounting.interfacedResources(resource_grp)
  if len(resources) == 0:
    resources = [resource_grp]
  for resource in resources:
    siteClause = siteClause + '"%s",' % resource 
  siteClause = siteClause[0:len(siteClause)-1]
  return siteClause


#-----------------------------------------------
def GetQueryForDaysReported(resource_grp,vos):
    """ Creates the SQL query DML statement for the Gratia database.
        This is used to determine if there are any gaps in the
        reporting for a site. It just identifies the days that
        data is reported for the site and period (only works if its a month).
    """
    userDataClause=""
    userGroupClause=""
    periodWhereClause = SetDatesWhereClause()
    siteClause        = GetSiteClause(resource_grp)
    dateFmt  =  "%Y-%m-%d"
    Logit("Resource Group: %(rg)s  Resources: %(resources)s" % \
       { "rg" : resource_grp, "resources" : siteClause } )
    query="""\
SELECT distinct(date_format(EndTime,"%(date_format)s"))
from 
     Site,
     Probe,
     VOProbeSummary Main 
where 
      Site.SiteName in ( %(resources)s )
  and Site.siteid = Probe.siteid 
  and Probe.ProbeName  = Main.ProbeName 
--  and Main.VOName in ( %(vos)s )
  and %(period)s 
  and Main.ResourceType = "Batch"
""" % { "date_format"  : dateFmt,
        "resources"    : siteClause,
        "vos"          : vos,
        "period"       : periodWhereClause
      }
    return query

#-----------------------------------------------
def RunGratiaQuery(select,params,LogResults=True,headers=False):
  """ Runs the query of the Gratia database """
  Logit("Running query on %s of the %s db" % (params["GratiaHost"],params["GratiaDB"]))
  host = params["GratiaHost"]
  port = params["GratiaPort"] 
  user = params["GratiaUser"] 
  pswd = params["GratiaPswd"] 
  db   = params["GratiaDB"]

  connectString = CreateConnectString(host,port,user,pswd,db,headers)
  (status,output) = commands.getstatusoutput("echo '" + select + "' | " + connectString)
  results = EvaluateMySqlResults((status,output))
  if len(results) == 0:
    cnt = 0
  elif headers:
    cnt = len(results.split("\n")) - 1
  else:
    cnt = len(results.split("\n")) 
  Logit("Results: %s records" % cnt)
  if LogResults:
    Logit("Results:\n%s" % results)
  return results

#-----------------------------------------------
def FindTierPath(params,table):
  """ The path in the org_Tier1/2 table keeps changing so we need to find it
      using the top level name which does not appear to change that
      frequently.
  """
  Logit("... finding path in table %s" % table)
  type = "data"
  if table == "org_Tier1":
    query = 'select Path from org_Tier1 where Name in ("US-FNAL-CMS","US-T1-BNL")'
  elif  table == "org_Tier2":
    query = 'select Path from org_Tier2 where Name in ("USA","Brazil")'
  else:
    Logerr("System error: method(FindTierPath) does not support this table (%s)" % (table))
  results = RunLCGQuery(query,type,params)
  if len(results) == 0:
    Logit("Results: None")
  else:
    LogToFile("Results:\n%s" % results)
  whereClause = "where "
  lines = results.split("\n")
  for i in range (0,len(lines)):
    if i > 0:
      whereClause = whereClause + " or "
    whereClause = whereClause + " Path like \"%s" % lines[i] + "%\""
  return whereClause + " order by Path"

#-----------------------------------------------
def WriteFile(data,filename):
  file = open(filename, 'w')
  file.write(data+"\n")
  file.close()

#-----------------------------------------------
def SendXmlHtmlFiles(filename,dest):
  """ Copies the xml and html files created to a Gratia collector
      data area to they are accessible for reporting on the
      collector or by other software.
  """
  if dest == 'DO_NOT_SEND':
    Logit("%s file NOT copied to a Gratia collector (arg is '%s')" % (filename,dest))
    return
  cmd = "scp %s %s" % (filename,dest) 
  Logit(cmd)
  p = popen2.Popen3(cmd,1)
  rtn = p.wait()
  if rtn <> 0:
    stderr = p.childerr.readlines()
    p.childerr.close()
    Logwarn("SendXmlHtmlFiles method: command(%s) failed rtn code %d: %s" % (cmd,rtn,stderr))
    return
  p.childerr.close()
  Logit("%s file successfully copied" % filename)
  

#-----------------------------------------------
def RunLCGUpdate(params,type):
  """ Performs the update of the APEL database """
  configfile = params["SSMConfig"]
  os.putenv("SSM_HOME",params["SSMHome"])

  Logit("---------------------")
  Logit("--- Updating APEL ---")
  Logit("---------------------")
  if type == "delete":
    file = GetFileName(params["SSMdeletes"],"txt")
    if not os.path.isfile(file):
      Logit("... this is likely the 1st time run for this period therefore no file to send")
      return
  if type == "update":
    file = GetFileName(params["SSMupdates"],"txt")
  Logit("%(type)s file... %(file)s Records: %(count)s" % \
         { "type"   : type,
           "file"   : file,
           "count"  : commands.getoutput("grep -c '%%' %s" % file), 
         } )
  try:
    ssm = SSMInterface.SSMInterface(configfile)
    ssm.send_outgoing(file)
  except SSMInterface.SSMException,e:
    raise Exception(e)
 
  if ssm.outgoing_sent(): 
    Logit("... successfulling sent")
  else:
    raise Exception("""SSM Interface failed. These files still exist:
%s""" % ssm.show_outgoing())
  Logit("------------------------------")
  Logit("--- Updating APEL complete ---")
  Logit("------------------------------")

#------------------------------------------------
def CreateConnectString(host,port,user,pswd,db,headers=False):
  col_names = ""
  if not headers:
    col_names = "--disable-column-names"
  return "mysql --defaults-extra-file='%(pswd)s' %(col)s -h %(host)s --port=%(port)s -u %(user)s %(db)s " % \
      { "pswd" : pswd,
        "host" : host,
        "port" : port,
        "user" : user,
        "db"   : db,
        "col"  : col_names,
       }

#------------------------------------------------
def EvaluateMySqlResults((status,output)):
  """ Evaluates the output of a MySql execution using the 
      getstatusoutput command.
  """
  if status == 0:
    if output.find("ERROR") >= 0 :
      raise Exception("MySql error:  %s" % (output))
  else:
    raise Exception("Status (non-zero rc): rc=%d - %s " % (status,output))

  if output == "NULL": 
    output = ""
  return output

#-----------------------------------------------
def CreateVOSummary(results,params,reportableSites):
  """ Creates a summary by site,vo for troubleshooting purposes. """

  Logit("-----------------------------------------------------")
  Logit("-- Creating a resource group, vo summary html page --") 
  Logit("-----------------------------------------------------")
  metrics = [ "NumberOfJobs",
             "CpuDuration", 
             "WallDuration", 
             "NormalisedCpuDuration", 
             "NormalisedWallDuration", 
            ]
  headers = { "NumberOfJobs"          : "Jobs",
             "CpuDuration"            : "CPU<br>(hours)", 
             "WallDuration"           : "Wall<br>(hours)", 
             "NormalisedCpuDuration"  : "Normalized CPU<br>(hours)", 
             "NormalisedWallDuration" : "Normalized Wall<br>(hours)", 
            }
  totals = {}
  totals = totalsList(metrics)
  resourceGrp = None
  vo          = None
  filename = GetFileName("summary","html")
  summary  = GetFileName("summary","dat")
  Logit("... summary html file: %s" % filename) 
  Logit("... summary dat  file: %s" % summary) 
  htmlfile    = open(filename,"w")
  summaryfile = open(summary,"w")
  htmlfile.write("""<HTML><BODY>\n""")
  htmlfile.write("Last update: " + time.strftime('%Y-%m-%d %H:%M',time.localtime()))
  htmlfile.write("""<TABLE border="1">""")
  htmlfile.write("""<TR>""")
  htmlfile.write("""<TH align="center">Resource Group</TH>""")
  htmlfile.write("""<TH align="center">NF<br>HS06</TH>""")
  htmlfile.write("""<TH align="center">VO</TH>""")
  for metric in metrics:
    htmlfile.write("""<TH align="center">%s</TH>""" % headers[metric])
  htmlfile.write("""<TH align="left">Resources / Gratia Sites</TH>""")
  htmlfile.write("</TR>\n")

  lines = results.split("\n")
  for i in range (0,len(lines)):
    values = lines[i].split('\t')
    if len(values) < 12:
      continue
    if i == 0:  # creating label list for results
      label = []
      for val in values:
        label.append(val)
      continue
    if label[0] == values[0]:  # filtering out column headings
      continue
    if values[0] != resourceGrp or values[1] != vo:
      if resourceGrp == None:
        resourceGrp = values[0]
        vo          = values[1]
        nf          = reportableSites[resourceGrp]
        continue
      else:
        writeHtmlLine(htmlfile, resourceGrp, vo, nf, totals, metrics)
        writeSummaryFile(summaryfile, resourceGrp, vo, nf, totals, metrics)
        totals = totalsList(metrics)
    idx = 0
    for val in values:
      if label[idx] in metrics:
        totals[label[idx]] = totals[label[idx]] + int(val)
      idx = idx +1
    resourceGrp = values[0]
    vo          = values[1]
    nf          = reportableSites[resourceGrp]

  writeHtmlLine(htmlfile, resourceGrp, vo, nf, totals, metrics)
  writeSummaryFile(summaryfile, resourceGrp, vo, nf, totals, metrics)
  htmlfile.write("</TABLE></BODY></HTML>\n")

  htmlfile.close()
  summaryfile.close()

#--------------------------------
def totalsList(metrics):
  totalsDict = {}
  for metric in metrics:
    totalsDict[metric] = 0 
  return totalsDict
#--------------------------------
def writeHtmlLine(file, rg, vo, nf, totals, metrics):
  file.write("""<TR><TD>%s</TD><TD align="center">%s</TD><TD align="center">%s</TD>""" % (rg,nf,vo))
  for metric in metrics:
    file.write("""<TD align="right">"""+str(totals[metric])+"</TD>")
  file.write("<TD>"+GetSiteClause(rg)+"</TD>")
  file.write("</TR>\n")

#--------------------------------
def writeSummaryFile(file, rg, vo, nf, totals, metrics):
  line = "%s\t%s\t%s" % (rg,nf,vo)
  for metric in metrics:
    line += "\t" + str(totals[metric])
  line += "\t" + GetSiteClause(rg)
  file.write(line + "\n")
  Logit("SUMMARY: " + line)

#-----------------------------------------------
def CreateLCGssmUpdates(results,params):
  """ Creates the SSM summary job records for the EGI portal."""
  Logit("-----------------------------------------------------")
  Logit("--- Creating SSM update records for the EGI portal --") 
  Logit("-----------------------------------------------------")
  if len(results) == 0:
    raise Exception("No updates to apply")
  ssmHeaderRec = "APEL-summary-job-message: v0.1\n"
  ssmRecordEnd = "%%\n"
  filename  =  GetFileName(params["SSMupdates"]  ,"txt")
  deletions =  GetFileName(params["SSMdeletes"],"txt")

  Logit("... update file: %s" % filename) 
  Logit("... delete file: %s" % deletions) 
  updates = open(filename,  'w')
  deletes = open(deletions, 'w')
  updates.write(ssmHeaderRec)
  deletes.write(ssmHeaderRec)

  lines = results.split("\n")
  for i in range (0,len(lines)):  
    values = lines[i].split('\t')
    if len(values) < 12:
      continue
    if i == 0:  # creating label list for results
      label = []
      for val in values:
        label.append(val)
      continue
    if label[0] == values[0]:  # filtering out column headings
      continue
    idx = 0
    for val in values:
      updates.write("%(label)s: %(value)s\n"  %  { "label" : label[idx], "value" : val, })
      #-- create a file that will zero out all entries (to be used like a delete) ---
      if label[idx] in ["WallDuration","CpuDuration","NormalisedWallDuration","NormalisedCpuDuration","NumberOfJobs"]:
        deletes.write("%(label)s: %(value)s\n"  %  { "label" : label[idx], "value" : 0, })
      else:
        deletes.write("%(label)s: %(value)s\n"  %  { "label" : label[idx], "value" : val, })
      idx = idx + 1
    updates.write(ssmRecordEnd)
    deletes.write(ssmRecordEnd)
  updates.close()
  deletes.close()

#-----------------------------------------------
def RetrieveUserData(reportableVOs,reportableSites,params):
  """ Retrieves Gratia data for reportable sites """
  Logit("--------------------------------------------")
  Logit("---- Gratia user data retrieval started ----")
  Logit("--------------------------------------------")
  global gUserQuery
  global gInteropAccounting
  output = ""
  firstTime = 1
  resource_grps = sorted(reportableSites.keys())
  for resource_grp in resource_grps:
    normalizationFactor = reportableSites[resource_grp]
    query = GetUserQuery(resource_grp,normalizationFactor,reportableVOs)
    if firstTime:
      gUserQuery = query
      Logit("Query:")
      LogToFile(query)
      firstTime = 0
    results = RunGratiaQuery(query,params,LogResults=False,headers=True)
    if len(results) == 0:
      results = ProcessEmptyResultsSet(resource_grp,reportableVOs)
    output += results + "\n"
  Logit("---------------------------------------------")
  Logit("---- Gratia user data retrieval complete ----")
  Logit("---------------------------------------------")
  return output

#-----------------------------------------------
def ProcessEmptyResultsSet(resource_grp,reportableVOs):
  """ Creates an update for each reportable VO with no Gratia
      data from query. 
      The purpose of this is to indicate in the APEL table that
      the site was processed.
  """
  gSitesWithNoData.append(resource_grp)
  output      = ""
  year        = int(gDateFilter.split("/")[0])
  month       = int(gDateFilter.split("/")[1])
  currentTime = calendar.timegm((year, month, 1, 0, 0, 0, 0, 0, 0))

  for vo in reportableVOs.split(","):
    output += "%(rg)s\t%(vo)s\t%(mos)s\t%(yr)s\t%(dn)s\t%(earliestdate)s\t%(latestdate)s\t%(wall)s\t%(cpu)s\t%(nfwall)s\t%(nfcpu)s\t%(jobs)s\n" % \
        { "rg"           : resource_grp,
          "vo"           : vo.strip('"'),
          "mos"          : month,
          "yr"           : year,
          "dn"           : "None",
          "earliestdate" : currentTime,
          "latestdate"   : currentTime,
          "cpu"          : "0",
          "wall"         : "0",
          "nfcpu"        : "0",
          "nfwall"       : "0",
          "jobs"         : "0",
        }
  return output

#-----------------------------------------------
def ProcessUserData(ReportableVOs,ReportableSites):
  """ Retrieves and creates the DML for the new (5/16/09) Site/User/VO summary
      data for the APEL interface.
  """
  gUserOutput = RetrieveUserData(ReportableVOs,ReportableSites,gDatabaseParameters)
  CreateLCGssmUpdates(gUserOutput,gDatabaseParameters)
  CreateVOSummary(gUserOutput,gDatabaseParameters,ReportableSites)

#-----------------------------------------------
def CheckMyOsgInteropFlag(reportableSites):
  """ Checks to see for mismatches between the reportable sites config
      file and MyOsg.  Include in email potential problems.
  """
  Logit("-------------------------------------------------------")
  Logit("---- Checking against MyOsg InteropAccounting flag ----")
  Logit("---- and against the WLCG REBUS Topology           ----")
  Logit("-------------------------------------------------------")
  global gInteropAccounting
  global gRebus
  if not gRebus.isAvailable():
    Logwarn("The WLCG REBUS topology is not available so no validations are being performed")
  myosgRGs = gInteropAccounting.interfacedResourceGroups()

  #-- for resource groups we are reporting, see if registered in MyOsg and Rebus
  for rg in reportableSites:
    msg = "Resource group (%s) is being reported" % rg
    #--- check MyOsg --
    if gInteropAccounting.isRegistered(rg):
      msg += "and is registered in MyOSG/OIM"
      if gInteropAccounting.isInterfaced(rg):
        msg += " and has resources (%s) with the InteropAccounting flag set in MyOsg" %  gInteropAccounting.interfacedResources(rg)
        #-- check Rebus ---
        if gRebus.isAvailable():
          if gRebus.isRegistered(rg):
            if gRebus.accountingName(rg) != gInteropAccounting.WLCGAcountingName(rg):
              Logwarn("Resource group %(rg)s MyOsg AccountingName (%(myosg)s) does NOT match the REBUS Accounting Name (%(rebus)s)" % \
               { "rg"     : rg,
                 "rebus"  : gRebus.accountingName(rg), 
                 "myosg"  : gInteropAccounting.WLCGAcountingName(rg)})
          else:
            Logwarn("%s and is NOT registered in REBUS" % msg)
      else:
        msg += " BUT has NO resources with the InteropAccounting flag set in MyOsg"
        if gRebus.isAvailable():
          if gRebus.isRegistered(rg):
            Logwarn("%s and IS registered in REBUS" % msg)
          else:
            Logwarn("%s and is NOT registered in REBUS" % msg)
        else:
            Logwarn(msg)
    else:
      msg += " and is NOT registered in MyOSG/OIM" 
      #-- check Rebus ---
      if gRebus.isAvailable():
        if gRebus.isRegistered(rg):
          Logwarn("%s and is registered in Rebus" % msg)
        else:
          Logwarn("%s and is NOT registered in Rebus" % msg)
      else:
        Logwarn(msg)

  #-- for MyOsg resource groups with Interop flag set, see if we are reporting
  #-- and if they have been registered in Rebus
  for rg in myosgRGs:
    if rg not in reportableSites:
      msg = "Resource group (%(rg)s) is NOT being reported BUT HAS resources (%(resources)s) with the InteropAccounting flag set in MyOsg" % \
            { "rg"       : rg, 
             "resources" : gInteropAccounting.interfacedResources(rg), }
      if gRebus.isAvailable():
        if gRebus.isRegistered(rg):
          Logwarn("%s but IS registered in REBUS as %s" % (msg,gRebus.tier(rg)))
        else:
          Logwarn("%s and is NOT registered in REBUS" % msg)
      else:
        Logwarn(msg)

  Logit("-----------------------------------------------------------------")
  Logit("---- Checking against MyOsg InteropAccounting flag completed ----")
  Logit("-----------------------------------------------------------------")

#-----------------------------------------------
def CheckForUnreportedDays(reportableVOs,reportableSites):
  """ Checks to see if any sites have specific days where no data is
      reported.  If a site is off-line for maintenance, upgrades, etc, this
      could be valid.  There is no easy way to check for this however.
      So the best we can do is check for this condition and then manually
      validate by contacting the site admins.
      On 12/10/09, another condition arose.  If a site goes inactive at 
      anytime, then all downtimes for that site are never available in
      MyOsg.  So, best we can do under those circumstances is 'pretend'
      any missing days were after the site went inactive and not raise
      any alarm.
  """
  global gSitesMissingData 
  global gMyOSG_available
  daysMissing = int(gFilterParameters["MissingDataDays"])
  Logit("-------------------------------------------")
  Logit("---- Check for unreported days started ----")
  Logit("-------------------------------------------")
  Logit("Starting checking for sites that are missing data for more than %d days" % (daysMissing))
  output = ""
  firstTime = 1

  #-- using general query to see all dates reported for the period --
  periodWhereClause = SetDatesWhereClause()
  endTimeFmt = "%Y-%m-%d"
  sites = reportableSites.keys()
  query="""select distinct(date_format(EndTime,"%s")) from VOProbeSummary Main where %s """ % (endTimeFmt,periodWhereClause)
  dateResults = RunGratiaQuery(query,gDatabaseParameters,LogResults=False,headers=False)
  Logit("Available dates: " + str(dateResults.split("\n"))) 

  #-- now checking for each site ---
  missingDataList = []
  for site in sorted(sites):
    allDates = dateResults.split("\n")
    Logit("------ User Query: %s  ------" % site)
    query = GetQueryForDaysReported(site,reportableVOs)
    if firstTime:
      Logit("Sample Query:")
      LogToFile(query)
      firstTime = 0
    results = RunGratiaQuery(query,gDatabaseParameters,LogResults=False,headers=False)

    #--- determine if any days are missing

    reportedDates = []
    if len(results) > 0:
      reportedDates = results.split("\n")
    for i in range (0,len(reportedDates)):  
      allDates.remove(reportedDates[i])
    if  len(allDates) > 0: 
      missingDataList.append("")
      missingDataList.append(site + "  (missing days): " + str(allDates))

      #--- see if dowmtime for those days was scheduled ---
      if gMyOSG_available:
        try:
          shutdownDays = CheckForShutdownDays(site,allDates)
          missingDataList.append(site + " (shutdown days): " + str(shutdownDays))
          for i in range (0,len(shutdownDays)):  
            allDates.remove(shutdownDays[i])
          #--- see if the site is inactive ----
          if  len(allDates) > daysMissing: 
            if inactives.resource_is_inactive(site):
              missingDataList.append(site + " is marked as inactive in MyOsg")
              allDates = []  # this keeps it from being reported as missing data
        except Exception, e:
          allDates.append("WARNING: Unable to determine planned shutdowns - MyOSG error (" + str(sys.exc_info()[1]) +")" )
          gMyOSG_available = False   
      else: 
        allDates.append("WARNING: Unable to determine planned shutdowns - MyOSG error (" + str(sys.exc_info()[1]) +")" )

      #--- see if we have any missing days now ----
      if  len(allDates) > daysMissing: 
        missingDataList.append(site + " is missing data for %d days" % len(allDates))
        gSitesMissingData[site] = allDates

  #--- create html file of missing data ---
  CreateMissingDaysHtml(missingDataList)

  #--- see if any need to be reported ---
  if len(gSitesMissingData) > 0:
    sites = gSitesMissingData.keys()
    for site in sites:
      Logwarn(site + " missing data for more than %d days: " % (daysMissing) + str(gSitesMissingData[site]))
  else:
      Logit("No sites had missing data for more than %d days" % (daysMissing))

  Logit("Ended checking for sites that are missing data for more than %d days" % (daysMissing))
  Logit("---------------------------------------------")
  Logit("---- Check for unreported days completed ----")
  Logit("---------------------------------------------")

#-----------------------------------------
def CreateMissingDaysHtml(missingData):
  """ Creates an html file of those sites that are missing data for
      any days during the period.
  """
  file = None
  filename = ""
  try:
    filename = GetFileName("missingdays","html") 
    Logit("#--------------------------------#")
    Logit("Creating html file of sites with missing data: %s" % filename)
    file = open(filename, 'w')  
    file.write("<html>\n")
    period = "%s-%s" % (gDateFilter[0:4],gDateFilter[5:7])
    file.write("<title>Gratia - APEL/LCG Interface (Missing data for %s</title>\n" % period)
    file.write("<head><h2><center><b>Gratia - APEL/LCG Interface<br/> (Missing data for %s)</b></center></h2></head>\n" % (period)) 
    file.write("<body><hr width=\"75%\" /><pre>\n")
    if len(missingData) > 0:
      for line in range (0,len(missingData)):
        Logit(missingData[line])
        file.write( missingData[line] + "\n")
    else:
      Logit("All sites reporting for all days in time period")
      file.write("\nAll sites reporting for all days in time period\n")
    file.write("</pre><hr width=\"75%\" />\n")
    file.write("Last update %s\n" % (time.strftime("%Y-%m-%d %H:%M:%S",time.localtime())))
    file.write("</body></html>\n")
    if file != None:
      file.close()
  except IOError, (errno,strerror):
    raise Exception,"IO error(%s): %s (%s)" % (errno,strerror,filename)
  # ---- send to collector -----
  SendXmlHtmlFiles(filename,gFilterParameters["GratiaCollector"])

#-----------------------------------------
def CheckForShutdownDays(site,missingDays):
  """ Determines if a site had a scheduled shutdown for the days that
      accounting data was unreported. 
  """
  shutdownDates = []
  for i in range (0,len(missingDays)):  
    day = missingDays[i]
    if downtimes.site_is_shutdown(site,day,"CE"):
      shutdownDates.append(day)
  return shutdownDates


#--- MAIN --------------------------------------------
def main(argv=None):
  global gWarnings
  global gSitesWithNoData
  global gVoQuery
  global gUserQuery
  global gVoOutput
  global gUserOutput

  #--- get command line arguments  -------------
  try:
    old_umask = os.umask(002)  # set so new files are 644 permissions
    GetArgs(argv)
  except Exception, e:
    print >>sys.stderr, e.__str__()
    return 1
  

  try:      
    #--- get parameters -------------
    GetFilterConfigParams(gFilterConfigFile)
    GetDBConfigParams(gFilterParameters["DBConfFile"])

    Logit("====================================================")
    Logit("Starting transfer from Gratia to APEL")
    Logit("Filter date............ %s" % (gDateFilter))
    gFilterParameters["SiteFilterFile"] = DetermineReportableSitesFileToUse(gDateFilter)
    Logit("Reportable sites file.. %s" % (gFilterParameters["SiteFilterFile"]))
    Logit("Reportable VOs file.... %s" % (gFilterParameters["VOFilterFile"]))
    Logit("Gratia database host... %s:%s" % (gDatabaseParameters["GratiaHost"],gDatabaseParameters["GratiaPort"]))
    Logit("Gratia database........ %s" % (gDatabaseParameters["GratiaDB"]))
    Logit("Missing days threshold. %s" % (gFilterParameters["MissingDataDays"]))
    Logit("LCG SSM_HOME variable.. %s" % (gDatabaseParameters["SSMHome"]))
    Logit("LCG SSM config file.... %s" % (gDatabaseParameters["SSMConfig"]))
    Logit("LCG SSM update file.... %s" % (GetFileName(gDatabaseParameters["SSMupdates"]  ,"txt")))
    Logit("LCG SSM delete file.... %s" % (GetFileName(gDatabaseParameters["SSMdeletes"],"txt")))

    #--- process deletions ---
    if gInUpdateMode:
      RunLCGUpdate(gDatabaseParameters,"delete")

    #--- check db availability -------------
    CheckGratiaDBAvailability(gDatabaseParameters)

    #--- get all filters -------------
    ReportableSites    = GetSiteFilters(gFilterParameters["SiteFilterFile"])
    ReportableVOs      = GetVOFilters(gFilterParameters["VOFilterFile"])
    
    ProcessUserData(ReportableVOs,ReportableSites)
    CheckForUnreportedDays(ReportableVOs,ReportableSites)
    CheckMyOsgInteropFlag(ReportableSites)

    #--- apply the updates to the APEL accounting database ----
    if gInUpdateMode:
      RunLCGUpdate(gDatabaseParameters,"update")
      SendEmailNotificationSuccess()
      SendEmailNotificationWarnings()
      Logit("Transfer Completed SUCCESSFULLY from Gratia to APEL")
    else:
      Logit("The --update arg was not specified. No updates attempted.")
    Logit("====================================================")

  except Exception, e:
    SendEmailNotificationFailure(e.__str__())
    Logit("Transfer FAILED from Gratia to APEL.")
    ## traceback.print_exc()
    Logerr(e.__str__())
    Logit("====================================================")
    return 1

  return 0

if __name__ == "__main__":
    sys.exit(main())


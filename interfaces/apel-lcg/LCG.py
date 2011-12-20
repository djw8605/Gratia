########################################################################
# 
# Author Philippe Canal, John Weigand
#
# LCG 
#
# Script to transfer the data from Gratia to APEL (WLCG)
########################################################################
#
#@(#)gratia/summary:$HeadURL$:$Id: LCG.py 3000 2009-02-16 16:15:08Z pcanal 
#
#
########################################################################
# Changes:
#
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
# 2/19/08 (John Weigand)
#   Due to the length of time it was taking to collect data on the 
#   'Unknown' VOs, added an extra set of methods to determine the 
#   'Unknown' VOs for all sites and only process those.  This reduces
#   the duration significantly.
#
# 4/15/08 (John Weigand)
#   Added methods for creating an xml and html file from the APEL database.
#   And reorganized some of the main section.
#
# 6/3/08 (John Weigand)
#   In the past when a site had no data, no update was made to the APEL
#   table.  Now we will create an update record showing it was processed.  
#   We will create the update records for each VO.
# 
# 6/25/08 (John Weigand)
#   Removed the calculation (just commented in case it has to be
#   re-instated) of a default normalization factor using the 
#   NormalizationProbe attribute.  The NormalizationDefault attribute
#   is used soley for sites not having a normalization factor which
#   as of now, there are none and hopefully in the future this will 
#   remain true.   
#
# 9/25/08 (John Weigand)
#   1. Added methods to copy the xml/html files that are extracted from
#   the APEL database tables to the Gratia collector for this
#   database.  The lcg.conf file has a new attribute called
#   GratiaCollector defining (in scp format) the directory to copy
#   this files to.
#   2. Also added method(s) to trap 'warning' type messages that do
#   not affect the actual update of the APEL data, but may represent
#   conditions that should be investigated.  This was put in to trap
#   any errors that may occur on the copy to the Gratia collector,
#   but can be used to detect site not reporting for a period or
#   other conditions.
#
# 11/11/08 (John Weigand)
#   Added an additional filter on the selection criterea. Only 
#   selecting ResourceType='Batch'.  Changes to the condor probes 
#   (v1.00.3) will result in distinguishing between the actual
#   grid jobs (Batch) and the grid monitoring job (GridMonitor) when
#   jobs are submitted using condor_submit. Any 'local' job used to
#   submit a job on the CE node will be filtered, but should they
#   at some point be passed to Gratia, these will be identified as
#   Local.
#
# 03/11/2009 (John Weigand)
#   1. Added a setting of the umask to 002 for all files being written.
#   This was to, hopefully, insure the setting of the right permissions
#   on the files sent to the tomcat ./webapps/gratia-data/interfaces/apel-lcg
#   directory for viewing data in the Gratia reporting service.
#   2. Also commented out (so it can be re-activated easily) the checking
#   for 'unknown' VOs with atlas or cms-like unix accounts.  This slow down
#   the interface considerably and that problem appears to have gone away.
#
# 3/12/2009 (John Weigand)
#   Added a method (DetermineReportableSitesFileToUse) that will retain
#   the lcg-reportableSites file in a 'history' directory and use that
#   during the process.  It copies the main configuration file to this
#   directory on when running in the 'current' month thereby preserving
#   prior months configuration in the event we have make updates there.
#   Refer to the comments in that method for more details.
#   This required an additional attribute in the lcg.conf file
#   (SiteFilterHistory) to define the directory these are retained in.
#   This allows us to change the SiteFilterFile at anytime during the
#   current month without fear of mis-stating the previous months updates
#   which currently are performed for the 1st 15 days of the current month.
#   This is not perfect since, in order to really retain this data, the
#   previous month should be added/updated in SVN/CVS.  A warning notification 
#   will be sent the first time a new history file as a reminder.
#
# 5/19/2009 (John Weigand)
#   Added functionality to retrieve summary data by CommonName and populate
#   a new APEL table called OSG_CN_DATA.  The WLCG requirement is to use full 
#   DN but Gratia currently only summarizes by the CN portion of the user proxy.
#   This is an interim solution until Gratia is modified to summarize by DN.
#   The changes for this new functionality are:
#   - lcg-db.conf: added an LcgUserTable attribute to identify the new table
#   - method GetQuery: Changed to optionally add in CommonName to the query.  
#     I chose to make it a python variable in this query so as not to replicate 
#     the rest of the query and take a chance on having it in 2 places to modify.
#     This is a bit of a gimmick but one I think is best.  The DBflag argument, 
#     if True will allow CommonName to be included in the query and summary.
#   - added methods GetVoQuery and GetUserQuery
#     These were added to allow for more intuitive reading of the main
#     section of the program.  Both call GetQuery, one using the original
#     Site/VO grouping and the other using Site/User/VO grouping.
#   - method RetrieveVoData
#     Changed to call GetVoQuery instead of GetQuery
#   - added method RetrieveUserData
#     This is clone of RetrieveVoData except it calls GetUserQuery and does not 
#     check for empty results at this time.
#   - added methods ProcessVoData and ProcessUserData
#     This pulled a section of code from the main section in order to keep the
#     main section more readable.
#   - added methods CreateLCGsqlVoUpdates and CreateLCGsqlUserUpdates
#     Both call CreateLCGsqlUpdates abstracting the table name to that level.
#   - method CreateLCGsqlUpdates
#   Added tableName as an argument.
#   - method RunLCGUpdate
#     Used for both VO and User summaries.  Added logging to identify the
#     file being used and the number of insert DML statements being processed.
#   IMPORTANT NOTE: Changes were NOT made to look for 'unknown' VOs at the User
#   level.  The 'unknown' VO check was left in (allowing it to be activated via
#   a command line argument) back on 3/11 in the event it needed to be 
#   re-activated.  Since it uncertain if this will be needed again and the code 
#   changes much more complex and maybe no longer needed, I chose NOT to make 
#   changes to the code for this at this time.  
# 
# 6/1/2009 (John Weigand)
#    In the CreateXmlHtmlFiles method, changed the Path selection criteria
#    for the org_Tier2 table:
#     - USA Path changed from 1.31 to 1.32
#     - for SPRACE (which was inadvertantly omitted since April 2008), they
#       are not included under USA, but under Brazil which is 1.4.    
#
# 6/26/2009 (John Weigand)
#    Added method (CheckForUnreportedDays) to check to see if a site
#    has more than 2 days unreported during the month and send a warning
#    email so it can be investigated.  This provides a little better handle
#    on sites that are having problems.  Not perfect, but better.
#    Added new parameter to lcg.conf (MissingDataDays) to set the threshold.
#
#    Also added a method (SetDatesWhereClause) to insure consistency in 
#    the where clause for the time period across all the queries.
#
# 7/7/2009 (John Weigand)
#    Added new method (CheckForShutdownDays) called by CheckForUnreportedDays
#    that will use a new class (Downtimes) to check MyOSG for days in 
#    which there are planned shutdowns for a site.  This will eliminate
#    sending a warning email for sites that have unreported days for this
#    reason.  It should be noted that if the url/criteria used in the
#    Downtime class to query MyOSG changes, this class will not be able 
#    to detect it as different xml and may not detect planned shutdowns
#    accurately.
#
# 7/29/2009 (John Weigand)
#    Added method FindTierPath to find the correct org_Tier1/2 entries for
#    the OSG data.  It was using a query based on Path but this has been
#    changing to much.  Now using the Name which appears pretty stable.
#
# 8/28/2009 (John Weigand)
#    Changed the syntax on the lcg.conf attributes for the email
#    notifications so that multiple 'To' addresses can be specified.
#
# 11/04/09 (Chris Green).
#   Inclusion of CommonName from summary table was changed to:
#     IF(DistinguishedName NOT IN ("", "Unknown"),DistinguishedName,CommonName)
#   to take advantage of new information. November 2009 is the first month for
#   which any item with a CommonName also has a DistinguishedName (except in
#   the case where CommonName is "Generic XXX user", in which case
#   DistinguishedName is blank).
#
# 12/14/09 (John Weigand)
#   Added use of a new class (InactiveResources) to query MyOsg for Resource 
#   that have been marked inactive.  This is used in conjunction with the 
#   Downtimes class when checking why a site/resource has no data for a
#   day.  Unfortunately, there is no MyOSG query that will give downtimes and
#   active/inactive status.
#
#  2/1/10 (John Weigand)
#   Uncommented a logging of the INSERT dml to the log file.
#   The commenting of this entry (done in revision 3654 on 11/2/2009)
#   inadvertantly #  affected the checking for late updates performed by the
#   find-late--updates.sh script.
#   I had forgotten that script was parsing the log file to make this
#   determination.  I added a comment to that affect when I uncommented the line
#   so I don't do this again.  I am not sure if it is worth creating another
#   method of detecting this type of condition at this time.
#
#  3/3/10 (John Weigand)  
#   In the etQueryForDaysReported method, which checks to see if a resource
#   has missed any days of reporting,  I removed the check for just cms and
#   atlas VOs.  Some sites are only used as a backup or for overflow from the
#   main site.  This eliminates falsely reoprting problems. 
#
#  4/8/10 (John Weigand)
#   Modified the  CreateXmlHtmlFiles method to output an extract of the
#   OSG_DATA table with 3 additional calculated columns in order to
#   see what the HepSpec2006 value, that is now used in WLCG reporting,
#   would be:
#      HS06_CPU    HS06_WCT    HS06Factor
#   Since HepSpec2006 is not available across all sites, WLCG does a simple
#   multiplication by 4 on the SI2K normalization factor we use. At some
#   my guess is we will start using the HS06 value but,until then, this 
#   is the only means of being able to compare easily.
#
#  8/16/11 (John Weigand)
#  ---------------------
#  This represents a major change to the interface.  All OSG reporting to
#  WLCG has been in reference to MyOsg resource group.  However, the MyOsg
#  InteropAccounting flag for WLCG Information is at the resource,
#  not resource group, level.  The changes made in this revision will now
#   1. treat the lcg-reportableSites config file entries as resource groups
#      This also means the Normalization Factors should be calculated
#      at the resource group level.
#   2. determine which resources within that resource group should have
#      their gratia data reported to APEL/WLCG.
#   3. summarize (using sql) the accounting data for the month for all
#      interfaced resources for the resource group.
#  A new class, InteropAccounting(.py), provides the access to MyOsg.
#
#  Due to a disconnect between Gratia site and MyOsg resource group/resource
#  that has existed from the beginning of time, some previously identifiable
#  "non-reporting" conditions may not get detected.  If all "resources"
#  reported to Gratia with the resource name, this would resolve that
#  problem.  However, many resources report to Gratia with the
#  resource group name.  Due to this, any "non-reporting" conditions
#  are ignored unless the entire "resource group" shows as not reporting.
#
#  Added a couple more validations for the WARNING email:
#   1. site we are reporting that do not have the MyOsg InteropAccounting set
#   2. sites we are not reporting that do have the MyOsg InteropAccounting set
#   3. Resource groups we are reporting that are not defined in Rebus
#   4. Resource groups that have and Accounting Name different from Rebus
#
#  A new class, Rebus(.py), provides the access the the WLCG REBUS
#  topology necessarty to do items 3 and 4 above.
#
#  Another word of warning.  Since this is now assuming Gratia sites
#  are equivalent to MyOsg resource groups and not resources, any updates
#  needed to re-populate will be a problem since MyOsg is not time 
#  sensitive.  Resource may be added or dropped from MyOsg at any point
#  in time.  It only reflects the current state.  So re-populating previous
#  periods may  result in the wrong data being used.
#  
#  Additional cleanup to simplify the change to SSM/ActiveMQ for the interface
#   1. Eliminated all code associated with updating the OSG_DATA table.
#      The OSG_CN_TABLE is the one soley used by APEL.
#   2. Eliminated all code associated with find "unknown" VOs for Atlas
#      which was a problem with Atlas rerorting that has since been resolved.
#   3. Eliminated all code assoicated with using a default Normalization
#      Factor.  All resource groups/sites must have one
#
#  Another new module, NormalizationFactors.py, was added but is not used
#  by the interface module LCG.py.  It may or may not be useful in 
#  determining NFs.  Not sure yet.
#
#  Last word... These changes have all been done over the last 6 months
#  and have been running in parallel for that period to verify that
#  there is no affect on the data sent.  It was not until Nebraska
#  added 2 resources (not being reported as Nebraska) this month that this
#  change had to go into production.  In all existing cases, MyOsg and
#  Gratia were in a state that it did not matter.
########################################################################
import Downtimes
import InactiveResources
import InteropAccounting
import Rebus

import traceback
import exceptions
import time
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
                       "LcgHost"   :None,
                       "LcgPort"   :None,
                       "LcgUser"   :None,
                       "LcgPswd"   :None,
                       "LcgDB"     :None,
                       "LcgUserTable"  :None,
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
  subject  = "Gratia transfer to APEL (WLCG) for %s - FAILED" % gDateFilter
  message  = "ERROR: " + error
  SendEmailNotification(subject,message)
#-----------------------------------------------
def SendEmailNotificationSuccess():
  """ Sends a successful email notification to the EmailNotice attribute""" 
  global gVoOutput
  global gSitesMissingData
  subject  = "Gratia transfer to APEL (WLCG) for %s - SUCCESS" % gDateFilter

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
  subject  = "Gratia transfer to APEL (WLCG) for %s - WARNINGS/ADVISORY" % gDateFilter
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

  hostname = commands.getoutput("hostname -f")
  logfile  = commands.getoutput("echo $PWD")+"/"+GetFileName(None,"log")
  username = commands.getoutput("whoami")
  vosqlfile   = commands.getoutput("echo $PWD")+"/"+GetFileName(None,"sql")
  vosqlrecs   = commands.getoutput("grep -c INSERT %s" % vosqlfile)
  usersqlfile = commands.getoutput("echo $PWD")+"/"+GetFileName("user","sql")
  usersqlrecs = commands.getoutput("grep -c INSERT %s" % usersqlfile)
  usertable   = gDatabaseParameters["LcgUserTable"]
  body = """\
Gratia to APEL/WLCG transfer. 

This is normally run as a cron process.  The log files associated with this 
process can provide further details.

Script............ %(program)s
Node.............. %(hostname)s
User.............. %(username)s
Log file.......... %(logfile)s

User SQL file..... %(usersqlfile)s 
User SQL records.. %(usersqlrecs)s  
User table names.. %(usertable)s  

Reportable sites file.. %(sitefilter)s
Reportable VOs file.... %(vofilter)s

%(message)s
	""" % { "program"     : gProgramName,
                "hostname"    : hostname,
                "username"    : username,
                "logfile"     : logfile,
                "usersqlfile" : usersqlfile,
                "usersqlrecs" : usersqlrecs,
                "usertable"   : usertable,
                "sitefilter"  : gFilterParameters["SiteFilterFile"],
                "vofilter"    : gFilterParameters["VOFilterFile"],
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

##JGW #-----------------------------------------------
##JGW def SetQueryDates():
##JGW   """ Sets the beginning and ending dates for the basic Gratia query.
##JGW       This is always 1 month.
##JGW   """
##JGW   return SetDateFilter(1)
##JGW #-----------------------------------------------
##JGW def SetNormalizationDates():
##JGW   """ Sets the beginning and ending dates for the Gratia query used to 
##JGW       determining the normalization factor used.
##JGW       This is set be a parameter in the configuration file.
##JGW   """
##JGW  return SetDateFilter(gFilterParameters["NormalizationPeriod"]) 
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
    return GetQuery(resource_grp,normalizationFactor,vos,"True")

#-----------------------------------------------
def GetQuery(resource_grp,normalizationFactor,vos,DNflag):
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
    """
    Logit("Resource Group: %s  Normalization Factor: %s" % (resource_grp,normalizationFactor)) 
    userDataClause=""
    userGroupClause=""
    if DNflag == "True":
      userDataClause="IF(DistinguishedName NOT IN (\"\", \"Unknown\"),IF(INSTR(DistinguishedName,\":/\")>0,LEFT(DistinguishedName,INSTR(DistinguishedName,\":/\")-1), DistinguishedName),CommonName) as UserDN, "
      userGroupClause=", UserDN "
    periodWhereClause = SetDatesWhereClause()
    siteClause        = GetSiteClause(resource_grp)
    strNormalization = str(normalizationFactor)
    fmtMonth = "%m"
    fmtYear  = "%Y"
    fmtDate  = "%Y-%m-%d"
    query="""\
SELECT "%(site)s" AS ExecutingSite,  
   VOName as LCGUserVO,
   %(user_data_clause)s
   Sum(NJobs), 
   Round(Sum(CpuUserDuration+CpuSystemDuration)/3600) as SumCPU,
   Round((Sum(CpuUserDuration+CpuSystemDuration)/3600) * %(nf)s) as NormSumCPU,
   Round(Sum(WallDuration)/3600) as SumWCT,
   Round((Sum(WallDuration)/3600) * %(nf)s ) as NormSumWCT,
   date_format(min(EndTime),"%(month)s")  as Month,
   date_format(min(EndTime),"%(year)s")   as Year,
   date_format(min(EndTime),"%(date_format)s") as RecordStart,
   date_format(max(EndTime),"%(date_format)s") as RecordEnd,
   "%(nf)s",
   NOW()
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
group by ExecutingSite,
         LCGUserVO
         %(user)s
""" % { "site"             : resource_grp,
        "site_clause"      : siteClause,
        "user_data_clause" : userDataClause,
        "nf"               : strNormalization,
        "month"            : fmtMonth,
        "year"             : fmtYear,
        "date_format"      : fmtDate,
        "vos"              : vos,
        "period"           : periodWhereClause,
        "user"             : userGroupClause
}
    return query

#-----------------------------------------------
def GetSiteClause(resource_grp):
  global gInteropAccounting
  siteClause = ""
  resources = gInteropAccounting.interfacedResources(resource_grp)
  if len(resources) == 0:
    resources = [resource_grp]
  Logit("Resource Group: %s Resources: %s" % (resource_grp,resources))
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
def RunGratiaQuery(select,params,LogResults=True):
  """ Runs the query of the Gratia database """
  Logit("Running query on %s of the %s db" % (params["GratiaHost"],params["GratiaDB"]))
  host = params["GratiaHost"]
  port = params["GratiaPort"] 
  user = params["GratiaUser"] 
  pswd = params["GratiaPswd"] 
  db   = params["GratiaDB"]

  connectString = CreateConnectString(host,port,user,pswd,db)
  (status,output) = commands.getstatusoutput("echo '" + select + "' | " + connectString)
  results = EvaluateMySqlResults((status,output))
  if len(results) == 0:
    Logit("Results: empty results set")
  if LogResults:
    Logit("Results:\n%s" % results)
  else:
    Logit("Results: %s records" % len(results))
  return results

#-----------------------------------------------
def RunLCGQuery(query,type,params):
  """ Performs a query of the APEL database with output in either xml or 
      an html format.
  """
  host = params["LcgHost"]
  port = params["LcgPort"] 
  user = params["LcgUser"] 
  pswd = params["LcgPswd"] 
  db   = params["LcgDB"]
  Logit("Running query on %s of the %s db" % (host,db))
  Logit("Query - type %s: %s" % (type,query))

  if type == "data":
    connectString = CreateConnectString(host,port,user,pswd,db)
  else:
    connectString = CreateQueryConnectString(host,port,user,pswd,db,type)
  (status,output) = commands.getstatusoutput("echo '" + query + "' | " + connectString)
  results = EvaluateMySqlResults((status,output))
  return results

#-----------------------------------------------
def CreateXmlHtmlFiles(params):
  """ Performs a query of the APEL database tables with output in either xml or 
      an html format creating those files.
      Additionally, it creates a .dat file for use with a shell script. The
      format of this data (showing relationships only using the Path column)
      makes it difficult to parse using xml.

      NOTE: The 'HS06' query needed a little gimmick added in the naming
            of the output file.  As HepSpec2006 normalized values are
            currently being done by WLCG and a Nebraska interface uses
            the current OSG_DATA extract, I had to add this to keep
            the files unique. (John Weigand 4/8/10)            
  """
  Logit("---------------------------------------")
  Logit("---- Retrieving APEL database data ----")
  Logit("---------------------------------------")
  dates = gDateFilter.split("/")  # YYYY/MM format
  lcgtable = gDatabaseParameters["LcgUserTable"]
  queries =  {
    "OSG_DATA"      : """select ExecutingSite, LCGUserVO, SUM(Njobs) as Njobs, SUM(SumCPU) as SumCPU, SUM(NormSumCPU) as NormSumCPU, SUM(SumWCT) as SumWCT, SUM(NormSumWCT) as NormSumWCT, Month, Year, MIN(RecordStart) as RecordStart, MAX(RecordEnd) as RecordEnd, MIN(NormFactor) as NormFactor, MeasurementDate FROM %(table)s WHERE Year=%(year)s and Month=%(month)s GROUP BY ExecutingSite, LCGUserVO, Year, Month""" % { "table" : lcgtable, "year"  : dates[0], "month" : dates[1], },
    "org_Tier1" : 'select * from org_Tier1 ' + FindTierPath(params,"org_Tier1"),
    "org_Tier2" : 'select * from org_Tier2 ' + FindTierPath(params,"org_Tier2"),
    "HS06_OSG_DATA"  : """select ExecutingSite, LCGUserVO, SUM(Njobs) as Njobs, SUM(SumCPU) as SumCPU, SUM(NormSumCPU) as NormSumCPU, (SUM(NormSumCPU) * 4) as HS06_CPU, SUM(SumWCT) as SumWCT, SUM(NormSumWCT) as NormSumWCT, (SUM(NormSumWCT) * 4) as HS06_WCT, Month, Year, MIN(RecordStart) as RecordStart, MAX(RecordEnd) as RecordEnd, MIN(NormFactor) as NormFactor, (MIN(NormFactor) * 4) as HS06Factor, MeasurementDate FROM %(table)s WHERE Year=%(year)s and Month=%(month)s GROUP BY ExecutingSite, LCGUserVO, Year, Month""" % { "table" : lcgtable, "year"  : dates[0], "month" : dates[1], },
  } 
  tables = queries.keys() 
  for table in tables:
    Logit("")
    #--- create xml file ---
    type = "xml"
    output = RunLCGQuery(queries[table],type,params)
    filename = GetFileName(table,type)
    WriteFile(output,filename)
    Logit("%s file created: %s" % (type,filename)) 
    SendXmlHtmlFiles(filename,gFilterParameters["GratiaCollector"])

    #--- create html file ---
    Logit("")
    type = "html"
    output = RunLCGQuery(queries[table],type,params)
    filename = GetFileName(table,type)
    WriteFile(output,filename)
    Logit("%s file created: %s" % (type,filename)) 
    SendXmlHtmlFiles(filename,gFilterParameters["GratiaCollector"])

    #--- create dat file ---
    Logit("")
    type = "skip-column-names"
    output = RunLCGQuery(queries[table],type,params)
    type = "dat"
    filename = GetFileName(table,type)
    WriteFile(output,filename)
    Logit("%s file created: %s" % (type,filename)) 
    SendXmlHtmlFiles(filename,gFilterParameters["GratiaCollector"])
  Logit("-------------------------------------------------")
  Logit("--- Retrieval of APEL database data complete ----")
  Logit("-------------------------------------------------")

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
def RunLCGUpdate(inputfile,params):
  """ Performs the update of the APEL database """
  host = params["LcgHost"]
  port = params["LcgPort"] 
  user = params["LcgUser"] 
  pswd = params["LcgPswd"] 
  db   = params["LcgDB"]
  
  Logit("---------------------------------------------------------")
  Logit("--- Updating APEL %s database at %s" % (db,host))
  Logit("---------------------------------------------------------")
  Logit("Input file: %s Inserts: %s" % (inputfile,commands.getoutput("grep -c INSERT %s" % inputfile)))

  connectString = CreateConnectString(host,port,user,pswd,db)
  (status,output) = commands.getstatusoutput("cat '" + inputfile + "' | " + connectString)
  results = EvaluateMySqlResults((status,output))
  if len(results) == 0:
    Logit("Results: None")
  else:
    LogToFile("Results:\n%s" % results)
  return results

#------------------------------------------------
def CreateConnectString(host,port,user,pswd,db):
  return "mysql --defaults-extra-file='%s' --disable-column-names -h %s --port=%s -u %s %s " % (pswd,host,port,user,db)

#------------------------------------------------
def CreateQueryConnectString(host,port,user,pswd,db,type):
  return "mysql --defaults-extra-file='%s' --%s -h %s --port=%s -u %s %s " % (pswd,type,host,port,user,db)

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
def CreateLCGsqlUserUpdates(results,filename):
  """ Creates the SQL DML to update the LCG database for the User summary."""
  tableName =  gDatabaseParameters["LcgUserTable"]
  CreateLCGsqlUpdates(results,filename,tableName)

#-----------------------------------------------
def CreateLCGsqlUpdates(results,filename,tableName):
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
  file.write("DELETE FROM %s WHERE Month = %s AND Year = %s ;\n" % (tableName,dates[1],dates[0]))
        
  for i in range (0,len(lines)):  
    val = lines[i].split('\t')
    if len(val) < 13:
      continue
    output =  "INSERT INTO %s VALUES " % (tableName) + str(tuple(val)) + ";"
    file.write(output+"\n")
    if tableName[0:8] == "OSG_DATA":
      LogToFile(output) ## this is needed to find late updates (find-late-update.sh)
    gKnownVOs[val[0] +'/'+val[1]] ="1"
        
  file.write("commit;\n")
  file.close()

#-----------------------------------------------
def RetrieveUserData(reportableVOs,reportableSites,params):
  """ Retrieves Gratia data for reportable sites """
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
    results = RunGratiaQuery(query,params,False)
    output = output + results + "\n"
  return output

##JGW #-----------------------------------------------
##JGW def ProcessEmptyResultsSet(site,reportableVOs,reportableSites):
##JGW   """ Creates an update for each reportable VO with no Gratia
##JGW       data from query. 
##JGW       The purpose of this is to indicate in the APEL table that
##JGW       the site was processes.
##JGW   """
##JGW   gSitesWithNoData.append(site)
##JGW   output      = ""
##JGW   year        = gDateFilter[0:4]
##JGW   month       = gDateFilter[5:7]
##JGW   currentTime = time.strftime("%Y-%m-%d %H:%M:%S",time.localtime())
##JGW   today       = time.strftime("%Y-%m-%d",time.localtime())
##JGW   for vo in reportableVOs.split(","):
##JGW     normalizationFactor = reportableSites[site]
##JGW     output = output + "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" % (site,vo.strip('"'),"0","0","0","0","0",month,year,today,today,normalizationFactor,currentTime)
##JGW 
##JGW   return output

#-----------------------------------------------
def ProcessUserData(ReportableVOs,ReportableSites):
  """ Retrieves and creates the DML for the new (5/16/09) Site/User/VO summary
      data for the APEL interface.
  """
  #--- User query gratia for each site and create updates ----
  Logit("----------------------------------------")
  Logit("---- User updates retrieval started ----")
  Logit("----------------------------------------")
  gUserOutput = RetrieveUserData(ReportableVOs,ReportableSites,gDatabaseParameters)

  #--- create the updates for the APEL accounting database ----
  CreateLCGsqlUserUpdates(gUserOutput,GetFileName("user","sql"))
  Logit("------------------------------------------")
  Logit("---- User updates retrieval completed ----")
  Logit("------------------------------------------")

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
      msg += " and is registered in MyOSG/OIM"
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
  dateResults = RunGratiaQuery(query,gDatabaseParameters,False)
  Logit("Available dates: " + str(dateResults.split("\n"))) 

  #-- now checking for each site ---
  missingDataList = []
  for site in sorted(sites):
    allDates = dateResults.split("\n")
    query = GetQueryForDaysReported(site,reportableVOs)
    if firstTime:
      Logit("Sample Query:")
      LogToFile(query)
      firstTime = 0
    results = RunGratiaQuery(query,gDatabaseParameters,False)

    #--- determine is any days are missing

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
    #--- get paramters -------------
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
    Logit("APEL database host..... %s:%s" % (gDatabaseParameters["LcgHost"],gDatabaseParameters["LcgPort"]))
    Logit("APEL database.......... %s" % (gDatabaseParameters["LcgDB"]))
    Logit("APEL database table.... %s" % (gDatabaseParameters["LcgUserTable"]))
    Logit("Missing days threshold. %s" % (gFilterParameters["MissingDataDays"]))

    #--- check db availability -------------
    CheckGratiaDBAvailability(gDatabaseParameters)
    if gInUpdateMode:
      CheckLcgDBAvailability(gDatabaseParameters)

    #--- get all filters -------------
    ReportableSites    = GetSiteFilters(gFilterParameters["SiteFilterFile"])
    ReportableVOs      = GetVOFilters(gFilterParameters["VOFilterFile"])
    
    ProcessUserData(ReportableVOs,ReportableSites)
    CheckForUnreportedDays(ReportableVOs,ReportableSites)
    CheckMyOsgInteropFlag(ReportableSites)

    #--- apply the updates to the APEL accounting database ----
    if gInUpdateMode:
      RunLCGUpdate(GetFileName("user","sql"),gDatabaseParameters)
      CreateXmlHtmlFiles(gDatabaseParameters)
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


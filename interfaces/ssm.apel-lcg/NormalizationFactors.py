#!/usr/bin/python

import os
import sys
import commands
import datetime
import traceback
import getopt

import InteropAccounting

class NFerror(Exception):
  pass

class NormalizationFactors:
  def __init__(self,date=None,site=None,debug=False):
    self.reportableSites = "/home/gratia/interfaces/ssm.apel-lcg/lcg-reportableSites"
    self.dbArgs         = "/home/gratia/interfaces/ssm.apel-lcg/lcg-db.conf"
    self.dbDict         = None
    self.period         = date
    self.site           = site
    self.DEBUG          = debug
    self.currentNF      = None
    self.subclusterData = None
    self.benchmarkData  = None
    self.gipNF          = None
    #--- used to summarize problems -----
    self.problemList       = {}
    if self.period == None:
      self.period = str(datetime.date.today())
    #--- used for MyOsg queries ---
    self.myosg = InteropAccounting.InteropAccounting()
    #--- warning message if no data is gound for date requested ---
    self.dateWarning = ""

  #------------------------------------------
  def Logit(self,msg):
    print msg
  #------------------------------------------
  def Logwarn(self,msg):
    self.Logit("WARNING: %s" % msg)
  #------------------------------------------
  def Logdebug(self,msg):
    if self.DEBUG:
      self.Logit(msg)
  #------------------------------------------
  def Logerr(self,msg):
    self.Logit("ERROR: %s" % msg)
    raise NFerror(Exception)
  #------------------------------------------
  def checkDB(self,host,port,user,pswd,db):
    """ Checks the availability of a MySql database. """
    self.Logdebug("Checking availability on %s:%s of %s database" % (host,port,db))
    if not os.path.isfile(pswd):
      self.Logerr("Password file does not exits: %s" % pswd)
    connectString = " --defaults-extra-file='%s' -h %s --port=%s -u %s %s " % (pswd,host,port,user,db)
    command = "mysql %s -e status" % connectString
    (status, output) = commands.getstatusoutput(command)
    if status == 0:
      if output.find("ERROR") >= 0 :
        self.Logit("""RUNNING: %s """ % (command)) 
        self.Logerr("""%s""" % output)
    else:
        self.Logit("""RUNNING: %s """ % (command)) 
        self.Logerr("""%s""" % output)
    self.Logdebug("Status: available")
  
  #------------------------------------------
  def createConnectString(self,host,port,user,pswd,db):
    return "mysql --defaults-extra-file='%s' --disable-column-names -h %s --port=%s -u %s %s " % (pswd,host,port,user,db)
  
  #------------------------------------------
  def runGratiaQuery(self,query,LogResults=True):
    """ Runs the query of the Gratia database """
    #-- get database parameters ---
    self.get_databaseArgs()

    #-- db parameters ---
    host  = self.dbDict["NormHost"]
    port  = self.dbDict["NormPort"]
    user  = self.dbDict["NormUser"]
    pswd  = self.dbDict["NormPswd"]
    db    = self.dbDict["NormDB"]
    #-- verify db is available -- 
    self.checkDB(host,port,user,pswd,db)
    #-- run the query -- 
    self.Logdebug("Running query on %s of the %s database" % (host,db))
    connectString = self.createConnectString(host,port,user,pswd,db)
    (status,output) = commands.getstatusoutput("echo '" + query + "' | " + connectString)
    results = self.evaluateMySqlResults(status,output)
    if LogResults:
      if len(results) == 0:
        raise NFerror("""No Subcluster data returned from Gratia database""")
      else:
        self.Logit("Results:\n%s" % results)
    lines =  results.split("\n")
    self.Logdebug("\n#### Query results ####")
    data = []
    for i in lines:
      if len(i) == 0:
        continue
      data.append(i)
      self.Logdebug(i)
    if len(data) == 0:
      self.Logerr("No Subcluster data returned from Gratia query for %s" % self.period)
    return data
  
  #-----------------------------------------
  def getSubclusterQuery(self):
    query =  """
  SELECT SiteName,
         Cluster,
         Name,
         DATE(timestamp) as date,
         coalesce(Processor, "Unknown"),
         max(Cores),
         max(BenchmarkValue)
  FROM Subcluster
  WHERE
       DATE(Timestamp) = "%(period)s"
    AND BenchmarkName   = "SI2K"
    -- AND BenchmarkName   = "HS06"
  GROUP BY
       SiteName,
       date,
       Cluster,
       Name
  """ % { "period" : self.period}
    self.Logdebug("\n#### Query used ####\n%s" % query)
    return query  
 
  #-----------------------------------------
  def getBenchmarkQuery(self):
    query =  """
 SELECT
       coalesce(Processor, "Unknown") as Processor,
       OS,
       OSVersion,
       max(BenchmarkValue)
  FROM Subcluster
  WHERE
       DATE(Timestamp) = "%(period)s"
   AND BenchmarkName   = "SI2K"
  GROUP BY
       Processor,
       OS,
       OSVersion
  """ % { "period" : self.period}
    self.Logdebug("\n#### Query used ####\n%s" % query)
    return query  

  #-----------------------------------------
  def getLastSubclusterUpdateQuery(self):
    query =  """
 SELECT max(DATE(Timestamp)) FROM Subcluster
  """ 
    self.Logdebug("\n#### Query used ####\n%s" % query)
    return query  
 
  #------------------------------------------
  def evaluateMySqlResults(self,status,output):
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
  
  #------------------------------------------
  def get_databaseArgs(self):
    if self.dbDict != None:  # already have it
      return
    self.dbDict = {}
    requiredAttrs = ["NormHost","NormPort", "NormUser", "NormPswd", "NormDB",]
    try:
      fd = open(self.dbArgs)
      while 1:
        line = fd.readline()
        if line == "":   # EOF
          break
        line = line.strip().strip("\n")
        if line.startswith("#"):
          continue
        if len(line) == 0:
          continue
        dbList    = line.split()
        attribute = dbList[0]
        if attribute not in requiredAttrs:
          continue
        value =  dbList[1]
        self.dbDict[attribute] = value
        requiredAttrs.remove(attribute)
      fd.close()
    except IOError, (errno,strerror):
      raise NFerror("""Cannot access configuration file with the databae arguments.
IO error(%(errno)s): %(error)s (%(file)s)""" % \
     { "errno" : errno, "error" : strerror, "file" : self.dbArgs})
    #-- make sure all the needed attributes are available --
    if len(requiredAttrs) != 0:
      raise NFerror("""Required attribute (%(attr)s) needed to access the database
not found in %(file)s.""" % \
             { "attr" : requiredAttrs, "file" : self.dbArgs})
  
  
  #------------------------------------------
  def get_currentNF(self):
    if self.currentNF != None:  # already have it
      return
    self.currentNF = {}
    try:
      fd = open(self.reportableSites)
      while 1:
        line = fd.readline()
        if line == "":   # EOF
          break
        line = line.strip().strip("\n")
        if line.startswith("#"):
          continue
        if len(line) == 0:
          continue
        siteList = line.split()
        sitename = siteList[0]
        nf =  siteList[1]
        self.currentNF[sitename] = nf
      fd.close()
    except IOError, (errno,strerror):
      raise NFerror("""Cannot access configuration file with the reportable sites  and normalization factors.
IO error(%(errno)s): %(error)s (%(file)s)""" % \
     { "errno" : errno, "error" : strerror, "file" : self.reportableSites})
  
  #------------------------------------------
  def siteIsInGIP(self,site):
    self.get_gipNF()
    if site in self.gipNF:
      return True
    return False
  
  #------------------------------------------
  def merge_Sites(self,siteLists=[]):
    """ This creates a list of all sites, from various sources, so that
        "out-of-sync" conditions can be determined for the various sources.
    """
    allSites = []
    for siteList in siteLists:
      for site in siteList:
        if site in allSites:
          continue
        allSites.append(site)
    return allSites

  #------------------------------------------
  def compare(self):
    """ Compares GIP and Configuration file data.  Descrepancies are noted
        and corrective action specified.
    """ 
    self.Logit("############################################")
    self.Logit("#####  Normalization Factor Comparisons ####")
    self.Logit("#####  Date used: %s            ####" % self.period)
    self.Logit("############################################")
    self.get_currentNF()
    self.get_gipNF()
    head_format = "%-20s %7s %7s %6s %7s%s %s"
    format = "%-20s %7i %7s %6i %6.2f%s %s"
    self.Logit(head_format % ("Resource Group","GIP NF","APEL NF","Delta","Percent","","Problems"))
    self.Logit(head_format % ("--------------","------","-------","-----","-------","","----------------"))
    #-- combine GIP and configuration file resource so we can cross check --
    allSites = self.merge_Sites([self.gipNF.keys(), self.currentNF.keys(),])
    #-- check them all --
    for site in sorted(allSites):
      # -- in both --
      if site in self.currentNF and site in self.gipNF:
        delta = self.gipNF[site] - int(self.currentNF[site])
        percent = (float(delta) / float(self.currentNF[site]))  * 100.00
        if abs(percent) >= 5:
          self.add_to_problemList(site,"Configuration file needs updating.")
        if delta == 0:
          output = "%-20s %7i %7s " % (site, self.gipNF[site], self.currentNF[site])
        else:
          output = format % (site,self.gipNF[site],self.currentNF[site],delta,percent,'%',"")
      # -- only in GIP --
      elif site in self.gipNF:  
        output = "%-20s %7i" % (site,self.gipNF[site])
      #-- only in configuration file --
      elif self.myosg.isRegistered(site):
        self.add_to_problemList(site,"Reporting to APEL/WLCG but no GIP data.")
        output = head_format % (site,"",self.currentNF[site],"","","","")
      else:
        self.add_to_problemList(site,"Reporting to APEL/WLCG. No GIP data. Not a registered MyOsg Resource Group")
        output = head_format % (site,"",self.currentNF[site],"","","","")
      if len(self.returnProblems(site)) == 0:
        self.show_output(site,output)
      else:
        for problem in self.returnProblems(site):
        ##  self.Logit("%-52s %s" % (output,problem))
          output += problem
          self.show_output(site,output)
          output = "%-52s" % ""
    self.Logit("############################################")
    self.Logit("#####  Normalization Factor Comparisons ####")
    self.Logit("#####  Date used: %s            ####" % self.period)
    self.Logit("############################################")

  #-----------------------------------------
  def returnProblems(self,site):
    if site in self.problemList:
       return self.problemList[site]
    return ["",]
  
  #------------------------------------------
  def get_gipNF(self):
    """ Retrieves the NF defined in the reportable sites configuration file."""
    if self.gipNF != None:  # already have it
      return
    self.get_subclusterData()
    self.gipNF = {}
    for site in sorted(self.subclusterData.keys()):
      si2k  = 0
      cores = 0
      for subcluster in self.subclusterData[site]:
        cores = cores + int(subcluster[0])
        si2k = si2k + (int(subcluster[0]) * int(subcluster[1]))
      nf = si2k / cores
      self.gipNF[site] = nf

  #----------------------------------------
  def get_latest_update_date(self):
    dateQuery = self.getLastSubclusterUpdateQuery()
    lastUpdate = self.runGratiaQuery(dateQuery,LogResults=False)
    self.dateWarning = """
****#############********************************************************
*** WARNING: Date requested (%s) has no data. Using %s
****************############*********************************************
""" %  (self.period, lastUpdate[0])
    print self.dateWarning
    self.period = lastUpdate[0]
  
  #------------------------------------------
  def get_subclusterData(self):
    """ Retrieves the GIP subcluseter data from the Gratia database. """
    if self.subclusterData != None:  # already have it
      return
    self.subclusterData = {}
    subclusters = [] 
    processors  = [] 
    try:
      #-- subcluster data ---
      subclusterQuery = self.getSubclusterQuery()
      subclusters = self.runGratiaQuery(subclusterQuery,LogResults=False)
      if len(subclusters) == 0: # no data. need to get latest date with updates
        self.get_latest_update_date()
        subclusterQuery = self.getSubclusterQuery()
        subclusters = self.runGratiaQuery(subclusterQuery,LogResults=False)
      self.populate_subclusterDataDict(subclusters)

      #-- benchmark data ---
      query = self.getBenchmarkQuery()
      processors  = self.runGratiaQuery(query,LogResults=False)
      self.populate_benchmarkDataDict(processors)
    except Exception,e:
      raise 
    except:
      traceback.print_exc()
      raise

  #---------------------------------------------
  def populate_subclusterDataDict(self,subclusters):
    self.subclusterData = {}
    for subcluster in subclusters:
      siteList = subcluster.split('\t')
      site      = siteList[0]
      cores     = siteList[5]
      si2k      = siteList[6]
      cluster   = siteList[1]
      name      = siteList[2]
      date      = siteList[3]
      processor = siteList[4]
      subclusterValues = [cores, si2k,cluster,name,processor,date]
      if int(cores) == 0 or cores == "":
        self.add_to_problemList(site,"Subcluster shows 0 cores: cluster (%(cluster)s) named (%(name)s)." % { "cluster":cluster,"name":name })
      if int(si2k) == 0:
        self.add_to_problemList(site,"Missing SI2K: %(processor)s" % { "processor":processor })
      if siteList[0] in self.subclusterData:
        self.subclusterData[site].append(subclusterValues)
      else:
        self.subclusterData[site] = [subclusterValues, ]

  #---------------------------------------------
  def populate_benchmarkDataDict(self,processors):
    self.benchmarkData = {}
    for processor in processors:
      processorList = processor.split('\t')
      model      = processorList[0]
      os         = processorList[1]
      osversion  = processorList[2]
      benchmark  = processorList[3]
      self.benchmarkData[model] = [os,osversion,benchmark,]

  #---------------------------------------------
  def benchmark_si2k(self,model):
    self.get_gipNF()
    if model in self.benchmarkData:
      return self.benchmarkData[model][2]
    else:
      return "."
  #-----------------------------------------
  def add_to_problemList(self,site,msg):
    if site in self.problemList:
      self.problemList[site].append(msg)
    else:
      self.problemList[site] = [msg,]

  #-----------------------------------------
  def show_problemList(self):
    if len(self.problemList) == 0:
      return
    self.Logit("#######################################")
    self.Logit("######## Potential Problems ###########")
    self.Logit("#######################################")
    format = "%-20s  %s"
    self.Logit(format % ("Resource Group","Problem"))
    self.Logit(format % ("--------------","--------------"))
    for site in sorted(self.problemList.keys()):
      displaySite = site 
      for problem in self.problemList[site]:
        self.Logit(format % (displaySite,problem))
        displaySite = ""

  #------------------------------------------
  def show_output(self,site,msg):
    """ Displays data based on any site filter specified. """
    if self.site == None:
      self.Logit(msg)
    elif self.site == site:
      self.Logit(msg)
      
  #------------------------------------------
  def show_currentNF(self):
    """ Displays the NF currently used for reportable resource groups
        based on the configuration file used. 
    """
    self.Logit("#################################################")
    self.Logit("#####  Currently Used Normalization Factors  ####")
    self.Logit("#################################################")
    self.get_currentNF()
    format = "%-20s  %6s %6s"
    self.Logit(format % ("Resource Group","SI2K","HS06"))
    self.Logit(format % ("--------------","----","----"))
    for site in sorted(self.currentNF.keys()):
      hs06 = int(self.currentNF[site]) * 4
      self.show_output(site,format % (site,self.currentNF[site],hs06))
    self.Logit("#################################################")
    self.Logit("#####  Currently Used Normalization Factors  ####")
    self.Logit("#################################################")

  #------------------------------------------
  def show_GipNF(self):
    """ Displays the NF calculated from GIP subcluster data for all 
        resource groups. 
    """
    self.Logit("#################################################")
    self.Logit("#####  GIP Calculated Normalization Factors  ####")
    self.Logit("#####  Date used: %s                 ####" % self.period)
    self.Logit("#################################################")
    self.get_gipNF()
    format = "%-20s  %6s %6s  %s"
    self.Logit(format % ("Resource Group","SI2K","HS06"," Problem"))
    self.Logit(format % ("--------------","----","----"," -----------"))
    for site in sorted(self.gipNF.keys()):
      hs06 = self.gipNF[site] * 4
      output = format % (site,self.gipNF[site],hs06,"")
      if len(self.returnProblems(site)) == 0:
        self.show_output(site,output)
      else:
        for problem in self.returnProblems(site):
          output += problem
          self.show_output(site,output)
          output = " "
    self.Logit("#################################################")
    self.Logit("#####  GIP Calculated Normalization Factors  ####")
    self.Logit("#####  Date used: %s                 ####" % self.period)
    self.Logit("#################################################")

  #------------------------------------------
  def show_subclusterData(self):
    """ Displays the GIP subcluster data for all resource groups.  """
    self.Logit("#################################################")
    self.Logit("#####  GIP subcluster data                   ####")
    self.Logit("#####  Date used: %s                 ####" % self.period)
    self.Logit("#################################################")
    self.get_gipNF()
    format = "%-20s  %-13s"
    self.Logit(format % ("","   NF Factor"))
    format = "%-20s  %6s %6s %6s %-30s %-40s %6s %6s %-30s"
    self.Logit(format % ("Resource Group","SI2K","HS06","Cores","Cluster","Name","SI2K","HS06","Processor"))
    self.Logit(format % ("--------------","----","----","-----","-------","----","----","----","---------"))
    for site in sorted(self.gipNF.keys()):
      rg = site
      si2knf = self.gipNF[site]
      hs06nf = si2knf * 4
      for subcluster in  self.subclusterData[site]:
        hs06 = int(subcluster[1]) * 4
        self.show_output(site, format % (rg,si2knf,hs06nf,
                                             subcluster[0],
                                             subcluster[2],
                                             subcluster[3],
                                             subcluster[1],
                                             hs06,
                                             subcluster[4],
                                             ))
        rg   = ""
        si2knf = ""
        hs06nf = ""
      self.show_output(site,"")
    self.Logit("#################################################")
    self.Logit("#####  GIP subcluster data                   ####")
    self.Logit("#####  Date used: %s                  ####" % self.period)
    self.Logit("#################################################")

  #------------------------------------------
  def show_benchmarkData(self):
    """ Displays the benchmark data for each process model. """
    self.Logit("##########################################")
    self.Logit("#####  Processor model benchmark data ####")
    self.Logit("#####  Date used: %s             ####" % self.period)
    self.Logit("##########################################")
    self.get_gipNF()
    format = "%6s %6s %-50s %-25s %-6s"
    self.Logit(format % ("SI2K","HS06","Processor","OS","OS Version"))
    self.Logit(format % ("----","----","---------","-------","---------"))
    for model in sorted(self.benchmarkData.keys()):
      hs06 = int(self.benchmarkData[model][2]) * 4
      self.Logit(format % (self.benchmarkData[model][2],hs06,model,self.benchmarkData[model][0],self.benchmarkData[model][1]))
    self.Logit("##########################################")
    self.Logit("#####  Processor model benchmark data ####")
    self.Logit("#####  Date used: %s            ####" % self.period)
    self.Logit("##########################################")

  #------------------------------------------
  def siteNormalizationFactor(self,site):
    """ For a specified site, a normalization factor is returned.
        It firsts looks in the reportables sites configuration file.
        If not there, it will search those generated based on GIP data.
        If still not found, it will return 1.
    """
    self.Logdebug("... retrieving normalization factor for %s" % site)
    self.get_currentNF()
    if site in self.currentNF:
      return self.currentNF[site]
    #-- if not in current, get from GIP ---
    self.Logwarn("Resource Group (%s) is not currently defined as reportable to APEL/WLCG" % site)
    self.get_gipNF()
    if site in self.gipNF:
      return self.gipNF[site]
    self.Logwarn("Resource Group (%s) has no normalization factor" % site)
    return 1

#--- end of NormalizationFactors class --

####################################
def retrieve_NF():
  """ Method used to test a couple retrievals of NF factors. """
  print
  print "#### Retrieving test NF factors ####"
  sites = ["AGLT2", "CIT_CMS_T2","UNAVAILABLE","XXXX"]
  format =  "%-20s %6s"
  print format % ("Resource Group","NF Used")
  for site in sites:
    print format % (site,nf.siteNormalizationFactor(site))

#--------------------------
def usage(arglist):
  global gProgamName
  nf = NormalizationFactors()
  print """
Usage:  %(program)s  action [--help] [--debug] [--site=<resource group>]

  If --site is specified, the actions will only display data for that
  resource group (aka site).

  Actions:
   --show-current  
        Displays the currently reportable resource groups and NFs
        Requires access to the reportable sites files used in the interface:
          %(sites)s
   --show-gip      
        Displays the NFs for resource groups caluculated from GIP 
        subcluster data.
        Requires access to the Gratia database containing the SubCluster table
        using a config file: %(conf)s 
   --compare       
        Displays all resource groups NFs compared against the currently 
        reportable  resource group's NF.
        Requires access to the reportable sites files used in the interface:
          %(sites)s
        Requires access to the Gratia database containing the SubCluster table
        using a config file: %(conf)s 
   --show-subcluster-data 
        Displays the NFs for resource groups caluculated and the details of the
        GIP subcluster data used to calculate the NF.
        Requires access to the Gratia database containing the SubCluster table
        using a config file: %(conf)s 
   --show-benchmark
        Displays the SI2K value for all processor models
        Requires access to the Gratia database containing the SubCluster table
        using a config file: %(conf)s 

""" %  {"program" : gProgramName, 
        "sites"   : nf.reportableSites,
        "conf"    : nf.dbArgs,
}
 
#--------------------------
def main(argv):
  global gProgramName
  gProgramName = argv[0]
  gAction = None
  gSite   = None
  gDEBUG  = False
  gDate   = str(datetime.date.today())
  arglist = [ "help", "debug", "show-current", "show-gip", "compare", "show-subcluster-data","show-benchmark","site=","date="]
  try:    
    opts, args = getopt.getopt(argv[1:], "", arglist)
    if len(opts) == 0:
      usage(arglist)
      print "ERROR: No command line arguments specified"
      return 1
    for o, a in opts:
      if o in ("--help"):
        usage()
        return 1
      if o in ("--debug"):
        gDEBUG = True
        continue
      if o in ("--site"):
        gSite = a
        continue
      if o in ("--date"):
        gDate = a
        continue
      if o[2:] in arglist[2:]:
        gAction = o
        continue 
    nf = NormalizationFactors(gDate,gSite,gDEBUG)
    if gAction == "--compare":
      nf.compare()
    elif gAction == "--show-current":
      nf.show_currentNF()
    elif gAction == "--show-subcluster-data":
      nf.show_subclusterData()
    elif gAction == "--show-benchmark":
      nf.show_benchmarkData()
    elif gAction == "--show-gip":
      nf.show_GipNF()
    elif gAction == None and gSite != None:
      nf.compare()
      nf.show_subclusterData()
    nf.show_problemList()
    print nf.dateWarning
  except getopt.error, e:
    msg = e.__str__()
    print "ERROR: Invalid command line argument: %s" % msg
    usage(arglist)
    return 1
  except NFerror,e:
    print "ERROR:",e
    return 1
  except Exception,e:
    traceback.print_exc()
    return 1
  return 0

####################################
if __name__ == "__main__":
  sys.exit(main(sys.argv)) 

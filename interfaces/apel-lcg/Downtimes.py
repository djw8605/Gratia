#!/usr/bin/python

import commands, os, sys, time, string
import libxml2
import urllib2
import exceptions

  
#------------------------------
class Downtimes:
  """
     Author: John Weigand (7/2/09)
     Description: 
       This script was written for use in the Gratia-APEL interface (LCG.py).
       The LCG.py script was modified to look for individual days that has had
       no data reported and send an email for investigation.  If a site has
       planned maintenance, this raises false "red" flags.

       This class will query MyOSG for all scheduled down times and return
       a boolean in the site_is_shutdown method.  It retrieves all downtimes
       (past and future) as this is usually checked with after the fact or
       during a shutdown.

       For a given site (ResourceName), date (YYYY-MM-DD) and service ("CE"),
       it will tell you if the site has scheduled downtime for that day.
       It will query MyOSG only once.

       Several problems with this method of doing this that we have to live
       with at this time:
       1. if the url changes, one is never notified.
       2. if the xml changes, one is never notified.
       3. if MyOSG is down (planned/unplanned), even if notified, can't do
          anything about it.
       
  """
  #-------------------------------------- 
  def __init__(self):
    #----------------------------------------------------
    # --- url for retrieving all Downtimes from MyOsg ---
    #----------------------------------------------------
    self.location = "http://myosg.grid.iu.edu/wizarddowntime/xml?datasource=downtime&summary_attrs_showservice=on&summary_attrs_showrsvstatus=on&summary_attrs_showgipstatus=on&summary_attrs_showfqdn=on&summary_attrs_showwlcg=on&summary_attrs_showenv=on&summary_attrs_showcontact=on&gip_status_attrs_showtestresults=on&gip_status_attrs_showfqdn=on&downtime_attrs_showpast=on&account_type=cumulative_hours&ce_account_type=gip_vo&se_account_type=vo_transfer_volume&start_type=7daysago&start_date=03%2F20%2F2009&end_type=now&end_date=03%2F27%2F2009&all_resources=on&gridtype=on&gridtype_1=on&service_4=on&service_1=on&service_5=on&service_2=on&service_3=on&active=on&active_value=1&disable_value=1"
    #-----------------------------------------------------------------
    # --- urls below are deliberately incorrect and used for testing ---
    #-----------------------------------------------------------------
    #self.location = "htt://myosg.grid.iu.edu/wizarddowntime/xml?datasource=downtime&summary_attrs_showservice=on&summary_attrs_showrsvstatus=on&summary_attrs_showgipstatus=on&summary_attrs_showfqdn=on&summary_attrs_showwlcg=on&summary_attrs_showenv=on&summary_attrs_showcontact=on&gip_status_attrs_showtestresults=on&gip_status_attrs_showfqdn=on&downtime_attrs_showpast=on&account_type=cumulative_hours&ce_account_type=gip_vo&se_account_type=vo_transfer_volume&start_type=7daysago&start_date=03%2F20%2F2009&end_type=now&end_date=03%2F27%2F2009&all_resources=on&gridtype=on&gridtype_1=on&service_4=on&service_1=on&service_5=on&service_2=on&service_3=on&active=on&active_value=1&disable_value=1"
    #self.location = "http://myosg.grid.iu.edu/wizarddowntime/xml?datasource=owntime&summary_attrs_showservice=on&summary_attrs_showrsvstatus=on&summary_attrs_showgipstatus=on&summary_attrs_showfqdn=on&summary_attrs_showwlcg=on&summary_attrs_showenv=on&summary_attrs_showcontact=on&gip_status_attrs_showtestresults=on&gip_status_attrs_showfqdn=on&downtime_attrs_showpast=on&account_type=cumulative_hours&ce_account_type=gip_vo&se_account_type=vo_transfer_volume&start_type=7daysago&start_date=03%2F20%2F2009&end_type=now&end_date=03%2F27%2F2009&all_resources=on&gridtype=on&gridtype_1=on&service_4=on&service_1=on&service_5=on&service_2=on&service_3=on&active=on&active_value=1&disable_value=1"
  
    self.doc = None
      
  #-------------------------------------- 
  def site_is_shutdown(self,site,date,service):
    """ For a site/date(YYYY-MM-DD)/service determine if this is a
        planned shutdown.
        Returns:  Boolean
    """
    self.__RetrieveXML__()
    downtimes = self.__GetListOfSiteDownTimes__(site) 
    shutdown = False
    for downtime in downtimes:
      startTime = self.__convert_date__(self.__get_element_value__(downtime,"StartTime"))
      endTime   = self.__convert_date__(self.__get_element_value__(downtime,"EndTime"))
      if self.__find_service__(downtime,service) == False:
        continue
      if date >= startTime and date <= endTime:
        shutdown = True
        break
    return shutdown

  #---------------------------------
  def __RetrieveXML__(self):
    """ Retrieve the xml document from MyOSG, only one time, for all
        downtimes (past and future).
    """
    try:
      if self.doc == None:
        html     = urllib2.urlopen(self.location).read()
        self.doc = libxml2.parseDoc(html)
    except:
      raise Exception("Unable to retrieve or parse xml from MyOSG")
  
  #---------------------------------
  def __GetListOfSiteDownTimes__(self,site):
    try:
      filter="/Downtimes/*/Downtime[ResourceName='%s']" % (site)
      downtimes =  self.doc.xpathEval(filter)
    except:
      raise Exception("Cannot find " + path + " in the xml retrieved")
    return downtimes
  #-----------------------------
  def __convert_date__(self,downtime):
    """ Converts a date in the format MyOSG keeps them
         e.g. Jan 25, 2009 14:00:00 UTC
        into a format where we can do a simple >/< comparison.
        We have to get rid of the comman to convert it in python.
    """
    downtime = string.replace(downtime,",","")
    downtime2 = time.strptime(downtime, "%b %d %Y %H:%M:%S %Z")
    downtime3 = time.strftime("%Y-%m-%d",downtime2)
    return downtime3
  #-----------------------------
  def __get_element_value__(self,resource,element):
    """ Returns the value of the xml element."""
    object = resource.xpathEval(element)
    value  =  object[0].content
    return value
  #---------------------------------
  def __find_service__(self,downtimeResource,name):
    """ Determines if a specified service is affected by the downtime.
        Returns:  Boolean
    """
    filter="Services/Service[Name='%s']" % (name)
    services = downtimeResource.xpathEval(filter)
    if len(services) <> 0:
      return True
    return False

#-- end of Downtimes ---------------------------
#### MAIN ############################
if __name__ == "__main__":
  print "#---- test -------------------------------------------------------"
  print "At this time (7/2/09), these will be True. others False:"
  print "  UFlorida-PG 2009-06-26 shutdown:  True"
  print "  BNL_ATLAS_1 2008-05-07 shutdown:  True"
  print "#----------------------------------------------------------------"
  sites = ["Nonexistent","UFlorida-PG", "BNL_ATLAS_1", "UFlorida-SE"]
  dates = ["2008-06-26", "2009-06-26", "2008-05-07", "2008-05-20"]
  try:
    downtime = Downtimes()
    for site in sites:
      for date in dates:
         shutdown = downtime.site_is_shutdown(site,date,"CE")
         print site,date,"shutdown: ",shutdown
  except Exception,e:
    print "ERROR:",sys.exc_info()[1]
    sys.exit(1)
  print "#----- end of test ---------------------------------------------"
   
  sys.exit(0)


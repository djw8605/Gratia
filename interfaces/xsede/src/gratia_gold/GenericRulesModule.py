import csv
import sys
import gratia
import re

class GenericRules(object):
    """This class defines generic methods and attributes pertaining to Rules, Blacklist and Whitelist"""
 

    def __init__(self, cp, log):
        self.cp = cp
        self.log = log
        self.file_reference = None
        self.rules_rows = None
        self.file_exists = None
        self.hosts_list = None
        self.patterns_list = None

    def GetFileExists(self):
        return self.file_exists

    def GetRulesRows(self):
        return self.rules_rows

    def GetHostsList(self):
        return self.hosts_list

    def GetPatternsList(self):
        return self.patterns_list

    def GetPatternsListForHost(self, hostname):
        try:
            return self.patterns_list[hostname]
        except Exception, e:
            self.log.error("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !") 
            sys.exit(1)

    def ReadFile(self, file_parameter, exitOnError = False):
        file = None
        file_exists = False
        try:    
            file = self.cp.get("rules", file_parameter)
            self.log.debug("File to be read is: " + str(file))
            self.file_reference = open(file, 'rb')
            self.file_exists = True
        except Exception, e:
            if(exitOnError == True):
                self.log.error("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !") 
                sys.exit(1)
            else:
                self.file_exists = False


        if(self.file_exists == True):
            try:
                self.rules_rows = []
                reader = csv.reader(self.file_reference)
                for row in reader: 
                    self.rules_rows.append(row)
            except Exception, e:
                self.log.error("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !") 
                sys.exit(1)

            self.log.debug(file_parameter + " length is: " + str(len(self.rules_rows)))
            for row in self.rules_rows:
                self.log.debug(file_parameter + " row is: " + str(row))
        else:
            self.log.debug(file_parameter + " doesn't exist.")


    def DetermineHostQueryRegularExpressions(self, host):
        host.SetCatchAllReDefinedForHost(False)
        host.SetCatchAllReIndexForHost(-1)
        rules_row_index = 0 #temporary index into rules_rows, needed for populating host_regex_index_dict
        try:    
            for rules_row in self.rules_rows:
                if(rules_row[0] == host.GetHostName()):
                    if(rules_row[1] != ''): 
                        if(rules_row[1] == '.*'): #catch_all defined for this host
                            host.SetCatchAllReDefinedForHost(True)
                            host.SetCatchAllReIndexForHost(rules_row_index)
                            self.log.debug("host: " + str(host) + " catch_all_re_defined_for_host is: " + str(host.GetCatchAllReDefinedForHost()))
                            self.log.debug("host: " + str(host) + "catch_all_re_index_for_host is: " + str(host.GetCatchAllReIndexForHost()))
                        else: #add this query regex pattern to host_regex_list
                            host.SetHostRegexIndexDict(rules_row[1], rules_row_index)
                            self.log.debug("rules_row_index: " + str(rules_row_index) + " host: " + str(host.GetHostName()) + "rules_row[1]: " + str(rules_row[1]) + " rules_row_index: " + str(rules_row_index))
                rules_row_index += 1
        except Exception, e:
            self.log.exception("Caught an exception and the detail is: \n\"" + str(e) + "\". Please check your rules file syntax. Exiting Now !") 
            sys.exit(1)


    def DetermineRulesMinDBID(self, host, probename):
        #Let's find the first occurence of Monitoring Start Time, in the rules file, for the host. 
        #If there are multiple defined, rest will be ignored.
        starttime = None
        self.log.debug("DetermineRulesMinDBID, self.rules_rows is: " + str(self.rules_rows))
        try:
            for rules_row in self.rules_rows:
                if((rules_row[0] == host) and (rules_row[4] != '')):
                    starttime = rules_row[4]
                    break
            self.log.debug("starttime is: " + str(starttime))
        except Exception, e:
            self.log.error("Caught an exception and the detail is: \n\"" + str(e) + "\". Please check your rules file syntax. Exiting Now !") 
            sys.exit(1)

        rules_min_dbid = 0
        starttime_dict = {}
        #Need to find the minimum dbid, from the Monitoring Start Time, if defined
        if(starttime is not None):
            try:
                rules_min_dbid = gratia.query_gratia(self.cp,probename=probename,starttime=starttime)
            except:
                self.log.error("Unable to Query Gratia ! Please check your database access parameters ! Exiting now.")
                sys.exit(1)
        self.log.debug("rules_min_dbid is: " + str(rules_min_dbid))
        return rules_min_dbid


    def CloseFile(self):
        #close the open files now
        if(self.file_reference != None):
            self.file_reference.close()
            self.log.debug("Closed File: " + str(self.file_reference))



    def DetermineHostsAndPatternsList(self, Type):
        """ This generic method, can be used for both Blacklist and Whitelist. Here's some background information:
        Blacklist Related:
            hosts_list, as defined in the configuration file, is supposed to be a temporary stopgap measure 
            for the case when the rulesfile has a reference to some host, which is currently not supported, 
            and the user does not wish to change the rules file.

            patterns_list, as defined in the configuration file, can be used for skipping over 
            one or more of Gratia Project Name Regular Expression values for a particular host. 
            Note that the host itself is NOT blacklisted, in such a case. 
            Also, note that the when a pattern is deleted from the patterns_blacklist, 
            it will be processed again in the next interval BUT the previously skipped jobs will NOT be re-processed.

        Whitelist Related:
            hosts_list, as defined in the configuration file, can be used when no job from this host
            would be quarantined.

            patterns_list, as defined in the configuration file, can be used when a job from a particular project
            need NOT be validated.
            Note that the host itself may or may not be whitelisted"""

        if(self.file_exists == True):
            self.hosts_list = []
            self.patterns_list = dict()
            try:
                for list_row in self.rules_rows:
                    if(list_row[1] != ''):
                        if (list_row[1] == '.*'): #Entire host needs to be listed
                            if(list_row[0] not in self.hosts_list):
                                self.hosts_list.append(list_row[0])
                        else: #loop through and add patterns to list
                            i = 1
                            while (i < len(list_row)):
                                #Using setdefault since defaultdict is NOT available in Python 2.4.3
                                self.patterns_list.setdefault(list_row[0], []).append(list_row[i])
                                i += 1
                self.log.debug(str(Type) + " hosts_list is: " + str(self.hosts_list))
                self.log.debug(str(Type) + " patterns_list is: " + str(self.patterns_list.items()))
            except Exception, e:
                self.log.error("Please check your " + str(Type) + " file syntax!. Exception detail is: \n\"" + str(e) + "\" Exiting Now !")
                sys.exit(1)
        else:
            self.log.debug(Type + " File doesn't exist and hence, no need to determine hosts/patterns list")

    def IsProjectInPatternsListForHost(self, hostname, project):
        if(self.patterns_list is not None):
            listed_patterns = []
            if hostname in self.patterns_list: #Check if this host has any listed patterns.
                listed_patterns = self.GetPatternsListForHost(hostname)
            else:   
                self.log.debug("patterns_blacklist is empty for host: " + str(hostname))
                return False
        else: #patterns_list is not defined...
            return False

        if(project is not None):
            for pattern in listed_patterns:
                self.log.debug("pattern is: " + str(pattern) + " project is: " + str(project))
                if(re.search(pattern, project) != None):
                    self.log.debug("project name = " + str(project) + " matches a listed pattern = " + str(pattern) + " for host: " + hostname)
                    return True
            else: #Completed the for loop without finding a pattern match...
                return False
        else: #project is None
            return False

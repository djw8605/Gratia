import sys
import gratia
import transaction
import gold
import re

class Host(object):
    """This class defines methods and attributes pertaining to a particular Host"""
 

    def __init__(self, cp, log, hostname):
        self.cp = cp
        self.log = log
        self.hostname = hostname
        self.host_regex_index_dict = {}
        self.catch_all_re_defined_for_host = None
        self.catch_all_re_index_for_host = None
        self.isWhiteListed = False
        self.previous_query_dbid = None #Saves the dbid used for query - if there's a validation failure, this would be used for quarantine

    def GetHostName(self):
        return self.hostname

    def SetHostName(self, hostname):
        self.hostname = hostname

    def SetCatchAllReDefinedForHost(self, Value):
        self.catch_all_re_defined_for_host = Value

    def GetCatchAllReDefinedForHost(self):
        return self.catch_all_re_defined_for_host

    def SetCatchAllReIndexForHost(self, Value):
        self.catch_all_re_index_for_host = Value

    def GetCatchAllReIndexForHost(self):
        return self.catch_all_re_index_for_host

    def IsCatchAllProjectRegExProjectAndUserDefinedAsCatchall(self, genericRules):
        #If this host has a "catchall" in the rules file, determine if
        #the corresponding "Project" and "User" are defined as catchall too.
        #If so, for a whitelisted host, this means that ALL Projects are whitelisted
        if(self.catch_all_re_defined_for_host == False):
            return False
        else: #This host has "catchall" defined in the rules file
            try:
                rules_row = genericRules.rules_rows[self.catch_all_re_index_for_host] #Get the rules_row for this pattern
                if((rules_row[2] == '.*') and (rules_row[3] == '.*')):
                    return True
            except Exception, e:
                self.log.debug("Caught an exception and the detail is: \n\"" + str(e) + ".\"\n Unable to read project and user fields.")
                return False


    def SetHostRegexIndexDict(self, Key, Value):
        self.host_regex_index_dict[Key] = Value

        #loop through the created dictionary and print contents
        for key in self.host_regex_index_dict:
            self.log.debug("host: " + str(self.hostname) + " host_regex_index_dict key is: " + str(key) + " and value is: " + str(self.host_regex_index_dict[key]))

    def GetHostRegexIndexDict(self, Key):
        return self.host_regex_index_dict[Key]

    def SetIsWhiteListed(self,Value):
        self.isWhiteListed = Value

    def GetIsWhiteListed(self):
        return self.isWhiteListed

    def DetermineIfWhiteListed(self,whiteList):
        self.log.debug("str(self.hostname) is: " + str(self.hostname))
        self.log.debug("str(whiteList.GetHostsList()) is: " + str(whiteList.GetHostsList()))
        if((whiteList is not None) and (whiteList.GetFileExists() == True)):
            if(str(self.hostname) in str(whiteList.GetHostsList())):
                self.isWhiteListed = True
            else:
                self.log.debug(str(self.hostname) + " not in " + str(whiteList.GetHostsList()))
        else:
            pass #self.isWhiteListed set to False, by default
        self.log.debug("hostname is: " + str(self.hostname) + " isWhiteListed is: " + str(self.isWhiteListed))
        return self.isWhiteListed
 
    def Query_And_Process(self, cp, genericRules, blackList, whiteList, quarantine, gcharge_machine):

            probename='condor:'+str(self.hostname)
            self.log.debug("probename is: " + str(probename))
              
            gratia_min_dbid = gratia_max_dbid = rules_min_dbid = disk_min_dbid = None
     
            #If probename entry exists in quarantine, need to read the quarantine and the last query dbid from disk
            quarantine.ReadQuarantineDBIDFromDisk(probename)

            #read the last successful dbid from the disk
            curr_txn = {}
            try:
                curr_txn = transaction.start_txn(cp, probename) #Get the dictionary stored on disk
            except Exception, e:
                self.log.error("Caught an exception and the detail is: \n\"" + str(e) + ".\"\n Exiting now !")
                sys.exit(1)
            if(probename in curr_txn):
                disk_min_dbid = curr_txn[probename] #Get the last_successful_id from the above referenced dictionary

            #read gratia_min_dbid and gratia_max_dbid from the gratia database
            try:
                (gratia_min_dbid, gratia_max_dbid) = gratia.initialize_txn(cp, probename)
            except Exception, e:
                self.log.error("Caught an exception and the detail is: \n\"" + str(e) + ".\"\n Exiting now !")
                sys.exit(1)
            self.log.debug("From Gratia, gratia_min_dbid is "+ str(gratia_min_dbid) + " gratia_max_dbid is "+str(gratia_max_dbid))

            #If a quarantine dbid or a last successful dbid exists, use that for query
            #Otherwise, determine where to start

            if (quarantine.GetQuarantineDBID(probename) is not None):
                curr_dbid = quarantine.GetPreviousQueryDBID(probename)
            elif ((disk_min_dbid is not None) and (disk_min_dbid != 0)):
                #Subtracting 1 (one) from some dbid values, before computing the maximum, because:
                #When getting the jobs, the condition is "strictly greater than" (JUR.dbid > %%(last_successful_id)s)

                #Compute the maximum of gratia_min_dbid AND disk_min_dbid
                curr_dbid = max((gratia_min_dbid - 1), (disk_min_dbid))
            else:
                #Determine if there's a starttime defined in the rules file
                rules_min_dbid = genericRules.DetermineRulesMinDBID(self.hostname, probename)
                if(rules_min_dbid != 0):
                    #Compute the maximum of gratia_min_dbid AND rules_min_dbid
                    curr_dbid = max((gratia_min_dbid - 1), (rules_min_dbid - 1))
                else:
                    #Since there is no other starting point, start with gratia_max_dbid
                    #The reasoning, per Mats, is to avoid double-charging any
                    #previously charged jobs. Note that, for the same reason, we are NOT
                    #subtracting one from gratia_max_dbid.
                    curr_dbid = gratia_max_dbid 
            self.log.debug("Starting curr_dbid = " + str(curr_dbid))

            #If the last_saved_on_disk min_dbid is lesser than the curr_dbid from the result above,
            #save the curr_dbid in the dictionary, to be committed to disk later
            if (disk_min_dbid < curr_dbid):
                curr_txn[probename] = curr_dbid

            txn = curr_txn
            txn[probename] = curr_dbid

            while curr_dbid < gratia_max_dbid:

                self.log.debug("Current transaction: probe=" + str(probename) + " DBID=" + str(txn[probename]))
                roll_fd = None
                try:
                    roll_fd = transaction.check_rollback(cp)
                except Exception, e:
                    self.log.error("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !")
                    sys.exit(1)

                self.log.debug("Query_And_Process: setting gratia_query_txn...")
                jobs = None
                self.previous_query_dbid = txn[probename]
                gratia_query_txn = {'probename': probename, 'last_successful_id':txn[probename]}
                try:
                    # If there's no quarantine id, GetQuarantineDBID would return None and so, it should be safe to pass it in directly
                    jobs = gratia.query_gratia(cp, gratia_query_txn,quarantine_dbid=quarantine.GetQuarantineDBID(probename))
                except:
                    self.log.error("Unable to Query Gratia ! Please check your database access parameters ! Exiting now.")
                    sys.exit(1)

                processed_jobs = {}
                max_id = 0
                job_count = 0

                self.log.debug("jobs.length is: " + str(len(jobs)))
                for job in jobs:
                    self.log.debug("Processing job: %s" % str(job))
                    self.log.debug("Befor:DBID:" + str(job['dbid']) + ":Project_Name:" + str(job['project_name']) + ":User:" + str(job['user']) + ":Hostname:" + str(self.hostname))
                    skip_job = False
                    job_status_valid = True

                    # If we have a quarantined job for this host and the dbid is lesser than the quarantined id, 
                    # skip the job since it would have been charged in the previous run
                    if((quarantine.GetQuarantineDBID(probename) is not None) and (job['dbid'] < quarantine.GetQuarantineDBID(probename))):
                        skip_job = True
                        job_status_valid = True
                        self.log.debug("dbid: " + str(job['dbid']) + " is less than the quarantined dbid: " + str(quarantine.GetQuarantineDBID(probename)) + " and hence, skipping it, since it would have been charged before.")
                    else:
                        #If this host is whitelisted 
                        #AND 
                        #the "project" and "user" fields are defined as "catchall" in the host "catchall" row in the rules file
                        #then, charge to gold, as is, since it means that all projects from the host are whitelisted

                        if((self.isWhiteListed == True) and (self.IsCatchAllProjectRegExProjectAndUserDefinedAsCatchall(genericRules) == True)):
                            self.log.debug("Project: " + str(job['project_name']) + " is WhiteListed. No need to validate job: " + str(job['dbid']) + ". Hostname:" + str(self.hostname))
                        else:
                            job, skip_job, job_status_valid = self.Validate_Job(job, probename, genericRules, blackList, quarantine)
                    self.log.debug("After:DBID:" + str(job['dbid']) + ":Project_Name:" + str(job['project_name']) + ":User:" + str(job['user']) + ":Hostname:" + str(self.hostname) + ":skip_job:" + str(skip_job) + ":job_status_valid:" + str(job_status_valid))
                     
                    if(skip_job == True):
                        job_count += 1
                        if job['dbid'] > max_id:
                            max_id = job['dbid']
                            self.log.debug("job['dbid']= " + str(job['dbid']) + ". max_id= " + str(max_id))
                        txn[probename] = max_id
                        self.log.debug("Query_And_Process: txn[probename] is: " + str(txn[probename]))
                        continue #Continue to the next job since it has been determined that this job needs to be skipped
     
                    if(job_status_valid == True):
                        # Record the job into rollback log.  We write it in before we call
                        # gcharge - this way, if the script is killed unexpectedly, we'll
                        # refund the job.  So, this errs on the conservative side.
        
                        try:
                            transaction.add_rollback(roll_fd, job)
                        except Exception, e:
                            self.log.error("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !")
                            sys.exit(1)
                        status = None
                        try:
                            status = gold.call_gcharge(job, gcharge_machine)
                        except Exception, e:
                            self.log.error("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !")
                            sys.exit(1)

                        if status != 0:
                            #roll_fd.close()
                            transaction.check_rollback(cp)
                            continue
                        job_count += 1
                        self.log.debug("Query_And_Process: job_count is: " + str(job_count))

                        # Keep track of the max ID
                        if job['dbid'] > max_id:
                            max_id = job['dbid']
                    else:
                        #Job status is NOT valid was would have been quarantined already; No need to save it as a successful one...
                        return 0

                if job_count == 0:
                    max_id = gratia_max_dbid
                    self.log.debug("job_count is 0. max_id= " + str(max_id))
                txn[probename] = max_id
                self.log.debug("Query_And_Process: txn[probename] is: " + str(txn[probename]))
                try:
                    transaction.commit_txn(cp, txn)
                except Exception, e:
                    self.log.error("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !")
                    sys.exit(1)
                curr_dbid = max_id
                self.log.debug("Query_And_Process: curr_dbid is: " + str(txn[probename]))


    def Validate_Job(self, job, probename, genericRules, blackList, quarantine):
        self.log.debug("Validate_Job method is called - job['dbid'] is: " + str(job['dbid']) + "\n")
        curr_qt={}      
        curr_pt={}      

        self.log.debug("quarantine_job_dbid is: " + str(quarantine.GetQuarantineDBID(probename)) + "\n")
        self.log.debug("job['dbid'] is: " + str(job['dbid']) + "\n")

        if (blackList.IsProjectInPatternsListForHost(self.hostname, job['project_name']) == True):
            self.log.debug("Need to skip this job since the job project name = " + str(job['project_name']) + " matches a blacklisted pattern for this host. DBID = " + str(job['dbid']) + " and User is: " + str(job['user']))
            return (job, True, False) #job, skip_job, validation_result

        rules_row = None
        for pattern in self.host_regex_index_dict:
            if(re.search(pattern, str(job['project_name'])) != None):
                self.log.debug("job project name = " + str(job['project_name']) + " matches a regex pattern = " + str(pattern) + " defined for this host.")
                self.log.debug("host_regex_index_dict[pattern] is: " + str(self.host_regex_index_dict[pattern]))
                rules_row = genericRules.rules_rows[self.host_regex_index_dict[pattern]] #Get the rules_row for this pattern
                break #Found a pattern match and so, break out of the for loop

        if(rules_row == None): #regex pattern match was not found, earlier
            #If catch all regular expression was defined in the rules file, use that instead
            if(self.catch_all_re_defined_for_host == True):
                rules_row = genericRules.rules_rows[self.catch_all_re_index_for_host] #Get the rules_row for this pattern
            else:       
                #If the host is whitelisted, don't quarantine but simply skip the job
                if(self.isWhiteListed == True):
                    return (job, True, False) #job, skip_job, validation_result
                else:       
                    quarantine.SetQuarantineReason(probename, 'Project Name for the job did not match any regular expression and no catchall defined !')

        if(quarantine.GetQuarantineReason(probename) is None): #Proceed only if it's not yet determined that this job needs to be quarantined
            try:            
                if((rules_row[2] == '') or (rules_row[3] == '')):
                    if(self.isWhiteListed == True):
                        return (job, True, False) #job, skip_job, validation_result
                    else:       
                        quarantine.SetQuarantineReason(probename, 'Syntax issue in the rules file. Offending row: ' + str(rules_row))
                else: #relevant rules row fields are not empty
                    if(rules_row[2] != '.*'):
                        job['project_name'] = rules_row[2]
                    if(rules_row[3] != '.*'):
                        job['user'] = rules_row[3]
                    self.log.debug("job['project_name'] set to: " + str(job['project_name']))
                    self.log.debug("job['user'] set to: " + str(job['user']))
            except:         
                self.log.warning("Unable to overwrite project name and user for this job. Please check the rules file !")
                if(self.isWhiteListed == True):
                    return (job, True, False) #job, skip_job, validation_result
                else:       
                    quarantine.SetQuarantineReason(probename, 'Syntax issue in the rules file. Offending row: ' + str(rules_row))

        if(quarantine.GetQuarantineReason(probename) is not None): 
            self.log.debug("Validation Failed for job['dbid']: " + str(job['dbid']))

            #if this job was quarantined before, do nothing. Otherwise, add this entry to the quarantine dictionary
            #Write the qurantine logs only if this job is being quarantined now

            if ((quarantine.GetQuarantineDBID(probename) is not None) and (int(job['dbid']) == quarantine.GetQuarantineDBID(probename))): #Check if this job was quarantined before
                self.log.debug("dbid = " + str(job['dbid']) + " exists in the quarantine dictionary\n")
            else:
                self.log.debug("dbid = " + str(job['dbid']) + " does NOT exist in the quarantine dictionary\n")
                # Writing the self.previous_query_dbid also because we need to be able to use the same query 
                # dbid later on to start processing from where we left off
                try:
                    curr_qt = transaction.get_qt(self.cp) #Get the dictionary stored on disk
                    curr_pt = transaction.get_qt(self.cp, previous_query = True) #Get the dictionary stored on disk
                    self.log.debug("Got curr_qt from disk. curr_qt = " + str(curr_qt) + "\n")
                    self.log.debug("Got curr_pt from disk. curr_pt = " + str(curr_pt) + "\n")
                except:
                    self.log.debug("Unable to get the quarantined ids file - assuming it did not exist")
                curr_qt[probename] = int(job['dbid'])
                curr_pt[probename] = int(self.previous_query_dbid)

                if(quarantine.GetQReportAndArchive() == False):
                    quarantine.SetQReportAndArchive(True)
                    self.log.debug("*****Set q_report_and_archive is now set to: " + str(quarantine.GetQReportAndArchive()) + "\n")

                #Write the qurantined logs since this job is being quarantined just now (was not quarantined before)
                quarantine.WriteQuarantineLog(probename, job, self.previous_query_dbid)

                try:            
                    transaction.commit_qt(self.cp, curr_qt) #Commit the current quarantine transaction to disk
                    transaction.commit_qt(self.cp, curr_pt, previous_query = True) #Commit the current quarantine transaction to disk
                    self.log.debug("Committed quarantine transaction log to disk.\n")
                except Exception, e:
                    self.log.error("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !")
                    sys.exit(1)
            return (job, False, False) #job, skip_job, validation_result

        else: #job validation succeeded
            self.log.debug("Validation succeeded for job['dbid']: " + str(job['dbid']))
            #if this job was quarantined before, delete the key from the dictionary
            if (job['dbid'] == quarantine.GetQuarantineDBID(probename)): 
                quarantine.ResetQuarantineDBID(probename)
            return (job, False, True) #job, skip_job, validation_result

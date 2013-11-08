import datetime
import shutil
import transaction
import socket
import smtplib
import re
import os
import sys

class Quarantine:
    """This class defines methods and attributes pertaining to quarantining a job"""

    def __init__(self, cp, log):
        self.cp = cp
        self.log = log
        self.q_file_archive = None
        self.q_dir = None
        self.q_report_and_archive = False
        self.quarantine_directory_defined = False
        self.quarantine_dictionary = {}
        self.previous_query_dictionary = {}
        self.quarantine_reason = {}

        #Get the quarantine directory location, if defined
        try:
            self.q_dir = self.cp.get("quarantine", "quarantine_directory")
            self.log.debug("quarantine directory is: " + str(self.q_dir) + "\n")
            self.quarantine_directory_defined = True
        except:
             self.log.debug("quarantine directory not defined...\n ")
             self.quarantine_directory_defined = False

    def SetQReportAndArchive(self, value):
        self.q_report_and_archive = value

    def GetQReportAndArchive(self):
        return self.q_report_and_archive

    def SetQuarantineDBID(self, probename, value):
        self.quarantine_dictionary[probename] = value

    def ResetQuarantineDBID(self, probename):
        try:        
            self.quarantine_dictionary = transaction.get_qt(self.cp) #Get the dictionary stored on disk 
            self.previous_query_dictionary = transaction.get_qt(self.cp, previous_query = True) #Get the dictionary stored on disk 
            self.log.debug("Got quarantine_dictionary from disk: " + str(self.quarantine_dictionary) + "\n") 
            self.log.debug("Got previous_query_dictionary from disk: " + str(self.previous_query_dictionary) + "\n") 
        except:        
            self.log.error("Unable to get the quarantined ids file. Exiting now since this could cause the software to behave incorrectly.")
            sys.exit(1)
        del self.quarantine_dictionary[probename] 
        del self.previous_query_dictionary[probename] 
        try:        
            transaction.commit_qt(self.cp, self.quarantine_dictionary) #Commit the current quarantine transaction to disk 
            transaction.commit_qt(self.cp, self.previous_query_dictionary, previous_query = True) #Commit the current quarantine transaction to disk 
            self.log.debug("Committed quarantine transaction log to disk.\n")
        except Exception, e:
            self.log.error("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !") 
            sys.exit(1)
        self.log.debug("job is valid and probename = " + str(probename) + " existed in quarantine database. Hence, it was deleted from the quarantine database\n")


    def GetQuarantineDBID(self, probename):
        if probename in self.quarantine_dictionary:
            return self.quarantine_dictionary[probename]
        else:
            return None

    def GetPreviousQueryDBID(self, probename):
        if(probename in self.previous_query_dictionary):
            return self.previous_query_dictionary[probename]
        else:
            return None
 
    def SetQuarantineReason(self, probename, value):
        self.quarantine_reason[probename] = value

    def GetQuarantineReason(self, probename):
        if(probename in self.quarantine_reason):
            return self.quarantine_reason[probename]
        else:
            return None

    def Archive_and_Report_IfNeeded(self):
        if(self.q_report_and_archive == True):
            self.log.debug("Archiving and reporting quarantine logs now...")
            if(self.quarantine_directory_defined == True):

                #Archiving quarantine file
                utc_datetime = datetime.datetime.utcnow()
                formated_string = utc_datetime.strftime("%Y-%m-%d-%H%MZ") #Result: '2011-12-12-0939Z'
                q_file_current = self.q_dir+'/quarantine_log.txt'
                self.q_file_archive = self.q_dir+'/quarantine_log_%s.txt'% formated_string
                shutil.move(q_file_current, self.q_file_archive)

                #Sending quarantine email
                self.SendQuarantineEmail()

                #Performing quarantine related cleanup
                self.q_report_and_archive = False #Need to reset flag
                self.log.debug("self.q_report_and_archive has been set to: " + str(self.q_report_and_archive))

    def ReadQuarantineDBIDFromDisk(self, probename):
        #If probename entry exists in quarantine, need to get the dbid
        try:
            self.quarantine_dictionary = transaction.get_quarantined_job_dbid(self.cp, probename)
            #Fetch previous_query_dictionary only if there's a quarantined job for the probename to avoid un-necessary disk access
            if(probename in self.quarantine_dictionary):
                self.previous_query_dictionary = transaction.get_qt(self.cp, previous_query = True)
        except Exception, e:
            self.log.exception("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !")
            sys.exit(1)
    
    def SendQuarantineEmail(self):
        quarantine_email_from = None
        quarantine_email_to = None
        try:
            quarantine_email_from = self.cp.get("quarantine", "quarantine_email_from")
            self.log.debug("quarantine_email_from is: " + str(quarantine_email_from) + "\n")
            quarantine_email_to = self.cp.get("quarantine", "quarantine_email_to")
            self.log.debug("quarantine_email_to is: " + str(quarantine_email_to) + "\n")
            self.log.debug("quarantine_email_to type is: \n")
            self.log.debug(type(quarantine_email_to))
        except:
            #Performing quarantine related cleanup
            self.q_report_and_archive = False #Need to reset flag and delete quarantine transactions file
            self.log.debug("q_report_and_archive has been set to: " + str(q_report_and_archive))
            transaction.delete_qt(self.cp) #Not checking for failure, since we are anyway in a cleanup situation.
            self.log.error("Please specify quarantine_email_from and quarantine_email_to addresses in gratia-gold.cfg file ! Exiting now.")
            sys.exit(1)

        msg={}
        host = socket.gethostname()
        SUBJECT = 'Please check the archived Gratia-gold quarantine logfile and take appropriate action'
        TEXT = 'Please check the archived Gratia-gold quarantine logfile at the following location:\n\n Host: %s\n Archived logfile: %s' %(host, self.q_file_archive)
        msg['Subject'] = 'Subject: %s\n\n%s' % (SUBJECT, TEXT)
        msg['From'] = quarantine_email_from
        s = smtplib.SMTP('localhost')
        for address in quarantine_email_to.split(","):
            self.log.debug("str(address) is: " + str(address))
            s.sendmail(msg['From'], str(address), msg['Subject']) 
        s.quit()


    def WriteQuarantineLog(self, probename, job, previous_query_dbid):
        self.log.debug("self.q_dir is: " + str(self.q_dir))
        if (self.quarantine_directory_defined == True):
            try:
                if not os.path.exists(self.q_dir):
                    os.makedirs(self.q_dir)
                    self.log.debug("Created: " + str(self.q_dir))
                q_file=self.q_dir+'/quarantine_log.txt'
                qf = open(q_file, 'a')
                qf.write("*****START LOG FOR THE QUARANTINED JOB*****\n")
                qf.write("Quarantine Reason: " + self.GetQuarantineReason(probename) + "\n\n")
                qf.write("Details for the quarantined job follow:\n\n")
                qf.write("Quarantine Job DBID is: " + str(job['dbid']) + "\n")
                qf.write("Query DBID is: " + str(previous_query_dbid) + "\n")
                qf.write("resource_type is: " + str(job['resource_type']) + "\n")
                qf.write("vo_name is: " + str(job['vo_name']) + "\n")
                qf.write("user is: " + str(job['user']) + "\n")
                qf.write("charge is: " + str(job['charge']) + "\n")
                qf.write("wall_duration is: " + str(job['wall_duration']) + "\n")
                qf.write("cpu is: " + str(job['cpu']) + "\n")
                qf.write("node_count is: " + str(job['node_count']) + "\n")
                qf.write("njobs is: " + str(job['njobs']) + "\n")
                qf.write("processors is: " + str(job['processors']) + "\n")
                qf.write("endtime is: " + str(job['endtime']) + "\n")
                qf.write("machine_name is: " + str(job['machine_name']) + "\n")
                qf.write("project_name is: " + str(job['project_name']) + "\n")
                qf.write("queue is: " + str(job['queue']) + "\n")
                qf.write("*****END LOG FOR THE QUARANTINED JOB*****\n\n\n\n\n")
                qf.close()
            except Exception, e:
                self.log.error("Unable to open quarantine file for writing. Exception detail is: \n\"" + str(e) + "\" Exiting Now !")
                sys.exit(1)
        else: #quarantine_directory_defined == False
            self.log.warning("Unable to write quarantine logs since the quarantine logfile is NOT defined !")

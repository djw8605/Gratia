
"""
Library for synchronizing Gratia job accounting with GOLD.

This library will connect to the Gratia database, summarize the accounting
records it finds, and submit the summaries to GOLD via a command-line script.

"""

import os
import time
import random
import logging
import optparse
import ConfigParser

import gold
import gratia
import locking
import transaction
import csv, sys
import re
import smtplib
import datetime
import shutil
import socket
import traceback

import GenericRulesModule
import HostModule
import QuarantineModule

log = None
logfile = None
logfile_handler = None

def parse_opts():

    parser = optparse.OptionParser(conflict_handler="resolve")
    parser.add_option("-c", "--config", dest="config",
                      help="Location of the configuration file.",
                      default="/etc/gratia-gold.cfg")
    parser.add_option("-v", "--verbose", dest="verbose",
                      default=False, action="store_true",
                      help="Increase verbosity.")
    parser.add_option("-s", "--cron", dest="cron",
                      type="int", default=0,
                      help = "Called from cron; cron interval (adds a random sleep)")
    
    opts, args = parser.parse_args()

    if not os.path.exists(opts.config):
        raise Exception("Configuration file, %s, does not exist." % \
            opts.config)

    return opts, args


def config_logging(cp, opts):
    global log
    global logfile
    # return a logger with the specified name gratia_gold
    log = logging.getLogger("gratia_gold")

    # log to the console
    # no stream is specified, so sys.stderr will be used for logging output
    console_handler = logging.StreamHandler()

    # Log to file, default is /var/log/gratia-gold/gratia-gold.cfg
    logfile = cp.get("logging", "file")

    logfile_handler = logging.FileHandler(logfile)

    # default log level - make logger/console match
    # Logging messages which are less severe than logging.WARNING will be ignored
    log.setLevel(logging.WARNING)
    console_handler.setLevel(logging.WARNING)
    logfile_handler.setLevel(logging.WARNING)

    if opts.verbose: 
        log.setLevel(logging.DEBUG)
        console_handler.setLevel(logging.DEBUG)
        logfile_handler.setLevel(logging.DEBUG)

    # formatter
    formatter = logging.Formatter("[%(process)d] [%(filename)20s:%(lineno)4d] %(asctime)s %(levelname)7s:  %(message)s")
    console_handler.setFormatter(formatter)
    logfile_handler.setFormatter(formatter)
    if opts.cron == 0:
        log.addHandler(console_handler)
    log.addHandler(logfile_handler)
    log.debug("Logger has been configured")


def DetermineApprovedHosts(genericRules, blackList, whiteList):
    """ Approved Hosts is the set of {hosts in rules file which are NOT blacklisted}"""

    approved_hosts = []
    try:    
        #Find unique hosts from the rules file which are NOT blacklisted (if blacklist file exists)
        for rules_row in genericRules.GetRulesRows():
            if((rules_row[0] != '') and (rules_row[0] not in approved_hosts)):
                if(blackList.GetFileExists() == True):
                    if(rules_row[0] not in blackList.GetHostsList()):
                        approved_hosts.append(rules_row[0])
                else: #blacklist file does NOT exist - add this host since it's not already added before
                        approved_hosts.append(rules_row[0])
        log.debug("approved_hosts list is: " + str(approved_hosts))
    except Exception, e:
        log.error("Caught an exception and the detail is: \n\"" + str(e) + "\". Please check your rules file syntax. Exiting Now !") 
        sys.exit(1)
    return approved_hosts

def GetGchargeMachine(cp):
    """ Get the machine name used for gcharge from the rules configuration file. """

    gcharge_machine = None
    try:    
        gcharge_machine = cp.get("gratia", "gcharge_machine")
    except Exception, e:
        log.error("Caught an exception and the detail is: \n\"" + str(e) + "\". It's mandatory to define 'gcharge_machine' parameter in the 'gratia' section of gratia-gold configuration file. Exiting Now !") 
        sys.exit(1)
    log.debug("gcharge_machine is: " + str(gcharge_machine))
    return gcharge_machine


def main():
    opts, args = parse_opts()
    cp = ConfigParser.ConfigParser()
    cp.read(opts.config)
    config_logging(cp, opts)

    if opts.cron > 0:
        random_sleep = random.randint(1, opts.cron)
        log.info("gratia-gold called from cron; sleeping for %d seconds." % \
            random_sleep)
        time.sleep(random_sleep)

    lockfile = cp.get("transaction", "lockfile")
    try:
        locking.exclusive_lock(lockfile)
    except Exception, e:
        log.exception("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !")
        sys.exit(1)

    try:
        gold.drop_privs(cp)
    except Exception, e:
        log.error("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !")
        sys.exit(1)

    try:
        gold.setup_env(cp)
    except Exception, e:
        log.error("Caught an exception and the detail is: \n\"" + str(e) + "\" Exiting Now !")
        sys.exit(1)

    #Get the machine name used for gold gcharge command
    gcharge_machine = GetGchargeMachine(cp)

    #Read the rulesfile
    genericRules = GenericRulesModule.GenericRules(cp, log)
    genericRules.ReadFile("rulesfile", exitOnError = True)

    #Read the blacklist file, if available
    #Note: Blacklist is an object of type GenericRules
    blackList = GenericRulesModule.GenericRules(cp, log)
    blackList.ReadFile("blacklist") 
    log.debug("blacklist_file_exists set to: " + str(blackList.GetFileExists()))

    #Determine hosts and patterns blacklist now
    blackList.DetermineHostsAndPatternsList("blacklist")

    #Read the whitelist file, if available.
    #Note: Whitelist is an object of type GenericRules
    whiteList = GenericRulesModule.GenericRules(cp, log)
    whiteList.ReadFile("whitelist")
    log.debug("whitelist_file_exists set to: " + str(whiteList.GetFileExists()))

    #Determine hosts and patterns whitelist now
    whiteList.DetermineHostsAndPatternsList("whitelist")

    #Determine the "approved" hosts
    approved_hosts = DetermineApprovedHosts(genericRules, blackList, whiteList)

    #Create a Quarantine Object to handle any quarantine related tasks
    quarantine = QuarantineModule.Quarantine(cp, log)
    
    #Loop through the approved hosts, query and process
    for hostname in approved_hosts:
        #Create a host object
        host = HostModule.Host(cp, log, hostname)

        #Create the list of query regular expressions for this host from the rules file.
        #Also, determine if there's a "catch-all" regular expression for this host and set appropriate parameters
        genericRules.DetermineHostQueryRegularExpressions(host)

        #Query and process for this host now
        host.DetermineIfWhiteListed(whiteList)
        host.Query_And_Process(cp, genericRules, blackList, whiteList, quarantine, gcharge_machine)
        
    #If needed, perform quarantine related tasks now. 
    #This is purposely done towards the end so that any quarantine email is sent only ONCE.
    quarantine.Archive_and_Report_IfNeeded()

    #Close the opened files now
    genericRules.CloseFile()
    blackList.CloseFile()
    whiteList.CloseFile()

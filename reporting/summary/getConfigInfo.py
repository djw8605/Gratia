#!/usr/bin/env python

import AccountingReports
from configReport import *
import sys

# script to extract needed variables from the config file

def main(argv=None):
    AccountingReports.UseArgs(argv)

    for line in extractVar("email","voEmailList").split(','):
        print "voemaillist", line.strip()

    installDir = installDirCheck()
    print "installDir",installDir

    fileW = open(installDir + "/reportType.config", 'w')
    fileW.write(extractVar("report","reportType") + "\n")

    print "1to", emailCheck()

    print "2to", userSiteReportEmail()

if __name__ == "__main__":
    sys.exit(main())

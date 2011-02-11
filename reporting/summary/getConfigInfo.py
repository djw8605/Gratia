#!/usr/bin/env python

import AccountingReports
from configReport import *
import sys

# script to extract needed variables from the config file

def main(argv=None):
    AccountingReports.UseArgs(argv)

    print "1to", emailCheck()

    print "2to", userSiteReportEmail()

    for line in extractVar("email","additionalReportRecipients").split("\n"):
        if line.strip() != "":
            print "additionalRecipients",line

    for line in extractVar("email","voEmailList").split("\n"):
        if line.strip() != "":
            print "voEmailList",line

if __name__ == "__main__":
    sys.exit(main())

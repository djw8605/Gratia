#!/usr/bin/env python

import AccountingReports
from configReport import *
import sys

# script to extract the report options from the config file

def main(argv=None):
    AccountingReports.UseArgs(argv)
    print "reportOptions", reportOptions()
    print "prod_mailto", extractVar("email", "productionTo")
    print "prod_user_mailto", extractVar("email", "productionUserTo")

if __name__ == "__main__":
    sys.exit(main())

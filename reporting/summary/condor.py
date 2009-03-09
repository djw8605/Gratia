#!/bin/env python
 
from AccountingReports import FromCondor,UseArgs
import sys

def main(argv=None):
    UseArgs(argv)
    FromCondor();

if __name__ == "__main__":
    sys.exit(main())

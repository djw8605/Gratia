#!/bin/env python
 
from AccountingReports import Weekly,UseArgs
import sys

def main(argv=None):
    UseArgs(argv)
    Weekly();

if __name__ == "__main__":
    sys.exit(main())

#!/bin/env python
 
from PSACCTReport import Weekly,UseArgs
import sys

def main(argv=None):
    UseArgs(argv)
    Weekly();

if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python

# Purpose: Merge config file options and command line options. To be used from inside scripts like daily_mutt.sh and range_mutt.sh 
# Condition: For common options command line options override the config file options. Mutually exclusive options are merged
# Example:
# "--mail karthik --one --three --five arg5" + "--mail karunach --two --three arg3 --five arg5"
# will become
#--mail karunach --five arg5 --three arg3 --two --one
# The above example is run from command line as:
# python mergeOptions.py "--mail karthik --one --three --five arg5" "--mail karunach --two --three arg3 --five arg5"

import sys

def main(argv):
    argCount = 0
    dict = {}
    for arg in argv[1:]:
        for val in arg.split():
            if val.find("--") != -1:
                dict[val] = ""
                key = val
            else:
                dict[key] += val 
    for key in dict:
        print key, dict[key],

if __name__ == "__main__":
    sys.exit(main(sys.argv))

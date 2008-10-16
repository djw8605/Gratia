#!/usr/bin/env python

import Gratia, sys

if __name__ == '__main__':
        rev = Gratia.ExtractCvsRevision("$Revision: 1.6 $");
        tag = Gratia.ExtractCvsRevision("$Name: not supported by cvs2svn $");
        Gratia.RegisterReporter("pbs-lsf.py", str(rev) + " (tag " + str(tag) + ")")
	if hasattr(sys,'argv') and sys.argv[1]:
                if (len(sys.argv) == 4) and sys.argv[2] and sys.argv[3]:
                        Gratia.RegisterService(sys.argv[2], sys.argv[3])
                Gratia.Initialize()
		print Gratia.SendXMLFiles(sys.argv[1], True, "Batch")
#		print Gratia.SendXMLFiles(sys.argv[1], False, "Batch")
	else:
		Gratia.DebugPrint(-1, "No records directory specified")

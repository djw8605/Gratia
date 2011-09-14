import gratia.common.GratiaCore as GratiaCore
import sys

if __name__ == '__main__':
        argv = sys.argv
        
        if len(argv) > 1:
           config = argv[1]
        else:
           config = "ProbeConfigTimeout"

        rev = "$Revision: 3345 $"
        GratiaCore.RegisterReporter("timeout.py",GratiaCore.ExtractSvnRevision(rev))

        GratiaCore.Initialize(config)

        #if (verbose):
        #print "Number of successful handshakes: "+str(GratiaCore.successfulHandshakes)
        sys.exit(0!=GratiaCore.successfulHandshakes)


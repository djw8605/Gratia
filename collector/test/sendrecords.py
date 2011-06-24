import GratiaCore
import Gratia
import time
import datetime
import profile
import sys

class Simple:
        "A simple example class"
        i = 12345
        def f(self):
                return 'hello world'

def GetRecord(jobid = 0):
        r = Gratia.UsageRecord("Batch")

        r.LocalUserId("cmsuser000")
        r.GlobalUsername("john ainsworth")
        r.UserKeyInfo("CN=john ainsworth/moreinfo, OUP=other, CN=moretesting, L=MC, OU=Manchester, O=eScience, C=UK")

        r.LocalJobId("PBS.1234.0bad")
        r.LocalJobId("PBS.1234.2" + str(jobid))        # overwrite the previous entry

        r.JobName("cmsreco ","this is not a real job name")
        r.Charge("1240")
        r.Status("4")
        r.Status(4)

        r.Njobs(3,"Aggregation over 10 days")

        r.Network(3.5,"Gb",30,"total")
        #r.Disk(3.5,"Gb",13891,"max")
        #r.Memory(650000,"KB","min")
        #r.Swap(1.5,"GB","max")
        r.ServiceLevel("BottomFeeder","QOS")

        r.TimeDuration(24,"submit")
        r.TimeInstant("2005-11-02T15:48:39Z","submit")

        r.WallDuration(6000*3600*25+63*60+21.2,"Was entered in seconds")
        r.CpuDuration("PT23H12M1.75S","user","Was entered as text")
        r.CpuDuration("PT12M1.75S","sys","Was entered as text")
        r.NodeCount(3) # default to total
        r.Processors(3,.75,"total")
        r.StartTime(1130946550,"Was entered in seconds")

        endtime = datetime.datetime.now();
        r.EndTime(time.mktime(endtime.timetuple()))
        #r.EndTime("2005-11-03T17:52:55Z","Was entered as text")

        r.MachineName("flxi02.fnal.gov")
        r.SubmitHost("patlx7.fnal.gov")
        r.Host("flxi02.fnal.gov",True)
        r.Queue("CepaQueue")

        r.ProjectName("cms reco")

        r.AdditionalInfo("RemoteWallTime",94365)
        r.Resource("RemoteCpuTime","PT23H")

        return r

def SendRecords(argv=None):

        if (len(argv)>1): configfile = argv[1]
        else: configfile = "ProbeConfig"
        if (len(argv)>2): nrecords = int(argv[2])
        else: nrecords = 0

        rev = "$Revision: 1.6 $"
        GratiaCore.RegisterReporterLibrary("sendrecords.py",Gratia.ExtractCvsRevision(rev))
        GratiaCore.RegisterReporterLibrary("sendrecords.py",Gratia.ExtractCvsRevision(rev))
        GratiaCore.RegisterReporterLibrary("sendrecords.py",Gratia.ExtractCvsRevision(rev))
        GratiaCore.RegisterEstimatedServiceBacklog(nrecords)

        Gratia.Initialize(configfile)

        i = -1
        for i in range(nrecords):
           #if ( i % 100 == 0 ) : print i
           r = GetRecord(i)
           msg = Gratia.Send(r)
           if msg == "Fatal Error: too many pending files":
              break
        #print i+1

if __name__ == '__main__': 
        #profile.run('SendRecords()','prof.log')
        SendRecords(sys.argv)

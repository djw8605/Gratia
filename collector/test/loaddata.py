import gratia.common.Gratia as Gratia
import gratia.common.GratiaCore as GratiaCore
import time
import datetime

def GetRecord(jobid, endtime, type):
        r = Gratia.UsageRecord("Batch")

        r.LocalUserId("cmsuser000")
        r.GlobalUsername("john ainsworth")
        r.UserKeyInfo("CN=john ainsworth, L=MC, OU=Manchester, O=eScience, C=UK")

        r.LocalJobId(str(jobid))        # overwrite the previous entry

        r.JobName("cmsreco","this is not a real job name")
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

        # Use TimeToString since endtime is already in UTC.
        r.EndTime(GratiaCore.TimeToString(endtime.timetuple())) 

        r.MachineName("flxi02.fnal.gov")
        r.SubmitHost("patlx7.fnal.gov")
        r.Host("flxi02.fnal.gov",True)
        r.Queue("CepaQueue")

        r.ProjectName(type)

        r.AdditionalInfo("RemoteWallTime",94365)
        r.Resource("RemoteCpuTime","PT23H")

        return r


def sendRecords(nrecords, end, type, extra = ""):

        # We want to start a bit (17 hours) less than 1 year ago.
        end_fixed = end.replace(hour=17,minute=10,second=00)
        start = end_fixed - datetime.timedelta(days=365)

        step = 365.0 / nrecords;
        ndays = 0;

        current = start
        
        #print "Starting at ",current
        for i in range(nrecords):
                r = GetRecord(i,current, type)
                r.RecordData.append(extra);
                Gratia.Send(r)
                ndays = ndays + step;
                current = start + datetime.timedelta(days=ndays)
        #print "Ending at ", start + datetime.timedelta(days=ndays)
        

if __name__ == '__main__': 
        rev = "$Revision$"
        Gratia.RegisterReporterLibrary("loaddata.py",Gratia.ExtractSvnRevision(rev))
        
        Gratia.Initialize()

        # Use UTC(/GMT) time to be in sync with the Collector (the DataScrubber in particular)
        end = datetime.datetime.utcnow();

        #sendRecords(3,end)

        # Send several records records
        sendRecords(500,end, "regular");

        # Send a few duplicates
        sendRecords(20,end, "duplicate")
        sendRecords(20,end, "duplicate")

        # Send a few record with ExtraXml
        sendRecords(17,end,"extraxml","<RealJobName>testing extra xml</RealJobName>");
        

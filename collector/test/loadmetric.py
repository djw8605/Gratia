import GratiaCore
import Gratia
import Metric
import time
import datetime


def GetRecord(id,name,endtime):
        r = Metric.MetricRecord()

        r.MetricName(name)
        r.MetricStatus("OK")
        r.Timestamp(GratiaCore.TimeToString(endtime.timetuple()))

        return r
        

def sendRecords(nrecords, name, end, extra = ""):
        len = 4 * 365.0

        # We want to start a bit (17 hours) less than 1 year ago.
        end_fixed = end.replace(hour=17,minute=10,second=00)
        start = end_fixed - datetime.timedelta(days=len)

        step = len / nrecords;
        ndays = 0;

        current = start
        
        for i in range(nrecords):
                r = GetRecord(i,name,current)
                r.RecordData.append(extra);
                Gratia.Send(r)
                ndays = ndays + step;
                current = start + datetime.timedelta(days=ndays)

if __name__ == '__main__':
        rev = "$Revision$"
        Gratia.RegisterReporterLibrary("loadmetric.py",Gratia.ExtractSvnRevision(rev))

        Gratia.Initialize()

        end = datetime.datetime.now();
       
        sendRecords(400,"LFC-READ",end)

        # Send a few record with ExtraXml
        sendRecords(17,"MetricXml-Test",end,"<RealJobName>testing extra xml</RealJobName>");

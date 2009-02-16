import Gratia
import Metric
import time
import datetime


def GetRecord(id,name,endtime):
        r = Metric.MetricRecord()

        r.MetricName(name)
        r.MetricStatus("OK")
        r.Timestamp(time.mktime(endtime.timetuple()))

        return r
        

def sendRecords(nrecords, name, end, extra = ""):
        len = 4 * 365.0
        start =  end - datetime.timedelta(days=len)
        start = start.replace(hour=18,minute=10,second=00);

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
        sendRecords(12,"MetricXml-Test",end,"<RealJobName>testing extra xml</RealJobName>");

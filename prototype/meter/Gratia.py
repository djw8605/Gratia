import os, sys, time, glob


GratiaExtension = ".gratia.xml"
BackupDirList = []
OutstandingRecord = []
RecordPid = os.getpid()
DebugLevel = 1

def Initialize():
        "This function initialize the Gratia metering engine"
        "We connect/register with the collector and load"
        "this meter's configuration"
        "We also load the list of record files that have not"
        "yet been send"
        print "Initializing Gratia"
        # Need to initialize the list of possible directories
        InitDirList()
        # Need to look for left over files
        SearchOustandingRecord()

def DebugPrint(level, *arg):
        if (level<DebugLevel):
                out = ""
                for val in arg:
                        out = out + str(val)
                print "Gratia ",out

def DirListAdd(value):
        "Utility method to add directory to the list of directories"
        "to be used for backup of the xml record"
        if len(value)>0 and value!="None" : BackupDirList.append(value)

def InitDirList():
        "Initialize the list of backup directories"
        "We prefer $DATA_DIR, but will also (if needed)"
        "try various tmp directory (/var/tmp, /tmp,"
        "$TMP_DIR, etc.."

        DirListAdd(os.getenv('DATA_DIR',""))
        DirListAdd("/var/tmp");
        DirListAdd("/tmp");
        DirListAdd(os.getenv('TMP_DIR',""))
        DirListAdd(os.getenv('TMP_WN_DIR ',""))
        DirListAdd(os.getenv('TMP',""))
        DirListAdd(os.getenv('TMPDIR',""))
        DirListAdd(os.getenv('TMP_DIR',""))
        DirListAdd(os.getenv('TEMP',""))
        DirListAdd(os.getenv('TEMPDIR',""))
        DirListAdd(os.getenv('TEMP_DIR',""))
        DirListAdd(os.environ['HOME'])
        DebugPrint(0,"List of backup directories: ",BackupDirList)

def SearchOustandingRecord():
        "Search the list of backup directories for"
        "any record that has not been sent yet"

        for dir in BackupDirList:
                path = os.path.join(dir,"gratia");
                path = os.path.join(path,"*"+GratiaExtension);
                files = glob.glob(path)
                for f in files:
                        if f not in OutstandingRecord:
                                OutstandingRecord.append(f)
        DebugPrint(0,"List of Outstanding records: ",OutstandingRecord)

def GenerateFilename(dir,RecordIndex):
        "Generate a filename of the for gratia/r$index.$pid.gratia.xml"
        "in the directory 'dir'"
        filename = "r"+str(RecordIndex)+"."+str(RecordPid)+GratiaExtension
        filename = os.path.join(dir,filename)
        return filename

def OpenNewRecordFile(DirIndex,RecordIndex):
        "Try to open the first available file"
        "DirIndex indicates which directory to try first"
        "RecordIndex indicates which file index to try first"
        "The routine returns the opened file and the next"
        "directory index and record index"
        "If all else fails, we print the xml to stdout"

        # The file name will be r$index.$pid.gratia.xml

        DebugPrint(3,"Open request: ",DirIndex," ",RecordIndex)
        index = 0
        for dir in BackupDirList:
                index = index + 1
                if index <= DirIndex or not os.path.exists(dir):
                        continue
                DebugPrint(3,"Open request: looking at ",dir)
                dir = os.path.join(dir,"gratia")
                if not os.path.exists(dir):
                        try:
                                os.mkdir(dir)
                        except:
                                continue
                if not os.path.exists(dir):
                        continue
                if not os.access(dir,os.W_OK): continue
                filename = GenerateFilename(dir,RecordIndex)
                while os.access(filename,os.F_OK):
                        RecordIndex = RecordIndex + 1
                        filename = GenerateFilename(dir,RecordIndex)
                try:
                        DebugPrint(0,"Creating file:",filename)
                        f = open(filename,'w')
                        DirIndex = index
                        return(f,DirIndex,RecordIndex)
                except:
                        continue;
        f = sys.stdout
        DirIndex = index
        return (f,DirIndex,RecordIndex)


def TimeToString(t = time.gmtime() ):
        return time.strftime("%Y-%m-%dT%H:%M:%SZ",t)

class UsageRecord:
        "Base class for the Gratia Usage Record"
        XmlData = []
        RecordData = []
        JobId = []
        UserId = []
        def __init__(self):
                DebugPrint(0,"Creating a usage Record "+TimeToString())
                self.Username = "none"

        def Description(self,value):
                if len(value)>0 : return  "urwg:description=\""+value+"\" "
                else : return ""

        def Metric(self,value):
                if len(value)>0 : return  "urwg:metric=\""+value+"\" "
                else : return ""

        def Unit(self,value):
                if len(value)>0 : return  "urwg:unit=\""+value+"\" "
                else : return ""

        def StorageUnit(self,value):
                if len(value)>0 : return  "urwg:storageUnit=\""+value+"\" "
                else : return ""

        def PhaseUnit(self,value):
                if type(value)==str : realvalue = value
                else : realvalue = self.Duration(value)
                if len(realvalue)>0 : return  "urwg:phaseUnit=\""+realvalue+"\" "
                else : return ""

        def Type(self,value):
                if len(value)>0 : return  "urwg:type=\""+value+"\" "
                else : return ""

        def Duration(self,value):
                seconds = value % 60
                value = int( (value - seconds) / 60 )
                minutes = value % 60
                value = (value - minutes) / 60
                hours = value % 24
                value = (value - hours) / 24
                result = "P"
                if value>0: result = result + str(value) + "D"
                if (hours>0 or minutes>0 or seconds>0) :
                        result = result + "T"
                        if hours>0 : result = result + str(hours)+ "H"
                        if minutes>0 : result = result + str(minutes)+ "M"
                        if seconds>0 : result = result + str(seconds)+ "S"
                else : result = result + "T0S"
                return result

        def AddToList(self,where,what,comment,value):
                # First filter out the previous value
                where = [x for x in where if x.find("<"+what)!=0]
                where.append("<"+what+" "+comment+">"+value+"</"+what+">")
                return where

        def AppendToList(self,where,what,comment,value):
                where.append("<"+what+" "+comment+">"+value+"</"+what+">")
                return where


        def LocalJobId(self,value):
                self.JobId = self.AddToList(self.JobId,"LocalJobId","",value)
        def GlobalJobId(self,value):
                self.JobId = self.AddToList(self.JobId,"GlobalJobId","",value)
        def ProcessId(self,value):
                self.JobId = self.AddToList(self.JobId,"ProcessId","",str(value))

        def LocalUserId(self,value):
                self.UserId = self.AddToList(self.UserId,"LocalUserId","",value);
        def UserKeyInfo(self,value):
                " Example: \
                  <ds:KeyInfo xmlns:ds=""http://www.w3.org/2000/09/xmldsig#""> \
                      <ds:X509Data> \
                         <ds:X509SubjectName>CN=john ainsworth, L=MC, OU=Manchester, O=eScience, C=UK</ds:X509SubjectName> \
                      </ds:X509Data> \
                  </ds:KeyInfo>"
                complete = "\n\t\t<ds:X509Data>\n\t\t<ds:X509SubjectName>"+value+"</ds:X509SubjectName>\n\t\t</ds:X509Data>\n\t"
                self.UserId = self.AddToList(self.UserId,"ds:KeyInfo","xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" ",complete)

        def JobName(self, value, description = ""):
                self.RecordData = self.AddToList(self.RecordData, "JobName", self.Description(description) ,value)
        def Charge(self,value, unit = "", formula = "", description = ""):
                if len(formula)>0 : Formula = "formula=\""+formula+"\" "
                else : Formula = ""
                self.RecordData = self.AddToList(self.RecordData,"Charge",self.Description(description)+self.Unit(unit)+Formula , value)
        def Status(self,value, description = "") :
                self.RecordData = self.AddToList(self.RecordData, "Status", self.Description(description), str(value))

        def WallDuration(self, value, description = ""):
                if type(value)==str : realvalue = value
                else : realvalue = self.Duration(value)
                self.RecordData = self.AddToList(self.RecordData, "WallDuration", self.Description(description), realvalue)
        def CpuDuration(self, value, description = ""):
                if type(value)==str : realvalue = value
                else : realvalue = self.Duration(value)
                self.RecordData = self.AddToList(self.RecordData, "CpuDuration", self.Description(description), realvalue)
        def EndTime(self, value, description = ""):
                if type(value)==str : realvalue = value
                else : realvalue = TimeToString(time.gmtime(value))
                self.RecordData = self.AddToList(self.RecordData, "EndTime", self.Description(description), realvalue)
        def StartTime(self, value, description = ""):
                if type(value)==str : realvalue = value
                else : realvalue = TimeToString(time.gmtime(value))
                self.RecordData = self.AddToList(self.RecordData, "StartTime", self.Description(description), realvalue)
        def TimeDuration(self, value, timetype, description = ""):
                " Additional measure of time duration that is relevant to the reported usage "
                " timetype can be one of 'submit','connect','dedicated' (or other) "
                if type(value)==str : realvalue = value
                else : realvalue = self.Duration(value)
                self.AppendToList(self.RecordData, "TimeDuration", self.Type(timetype)+self.Description(description), realvalue)
        def TimeInstant(self, value, timetype, description = ""):
                " Additional identified discrete time that is relevant to the reported usage "
                " timetype can be one of 'submit','connect' (or other) "
                if type(value)==str : realvalue = value
                else : realvalue = TimeToString(time.gmtime(value))
                self.AppendToList(self.RecordData, "TimeInstant", self.Type(timetype)+self.Description(description), realvalue)

        def MachineName(self, value, description = "") :
                self.RecordData = self.AddToList(self.RecordData, "MachineName", self.Description(description), value)
        def Host(self, value, primary = False, description = "") :
                if primary : pstring = "primary=\"true\" "
                else : pstring = "primary=\"false\" "
                pstring = pstring + self.Description(description)
                self.RecordData = self.AddToList(self.RecordData, "Host", pstring, value)
        def SubmitHost(self, value, description = "") :
                self.RecordData = self.AddToList(self.RecordData, "SubmitHost", self.Description(description), value)
        def Queue(self, value, description = "") :
                self.RecordData = self.AddToList(self.RecordData, "Queue", self.Description(description), value)
        def ProjectName(self, value, description = "") :
                self.RecordData = self.AddToList(self.RecordData, "ProjectName", self.Description(description), value)


        def Network(self, value, storageUnit = "", phaseUnit = "", metric = "total", description = "") :
                " Metric should be one of 'total','average','max','min' "
                self.AppendToList(self.RecordData, "Network",
                        self.StorageUnit(storageUnit)+self.PhaseUnit(phaseUnit)+self.Metric(metric)+self.Description(description),
                        str(value))
        def Disk(self, value, storageUnit = "", phaseUnit = "", type = "", metric = "total", description = "") :
                " Metric should be one of 'total','average','max','min' "
                " Type can be one of scratch or temp "
                self.AppendToList(self.RecordData, "Disk",
                        self.StorageUnit(storageUnit)+self.PhaseUnit(phaseUnit)+self.Type(type)+self.Metric(metric)+self.Description(description),
                        str(value))

        def Memory(self, value, storageUnit = "", phaseUnit = "", type = "", metric = "total", description = "") :
                " Metric should be one of 'total','average','max','min' "
                " Type can be one of shared, physical, dedicated "
                self.AppendToList(self.RecordData, "Memory",
                        self.StorageUnit(storageUnit)+self.PhaseUnit(phaseUnit)+self.Type(type)+self.Metric(metric)+self.Description(description),
                        str(value))

        def Swap(self, value, storageUnit = "", phaseUnit = "", type = "", metric = "total", description = "") :
                " Metric should be one of 'total','average','max','min' "
                " Type can be one of shared, physical, dedicated "
                self.AppendToList(self.RecordData, "Swap",
                        self.StorageUnit(storageUnit)+self.PhaseUnit(phaseUnit)+self.Type(type)+self.Metric(metric)+self.Description(description),
                        str(value))

        def NodeCount(self, value, metric = "total", description = "") :
                " Metric should be one of 'total','average','max','min' "
                self.AppendToList(self.RecordData, "NodeCount",
                        self.Metric(metric)+self.Description(description),
                        str(value))
        def Processors(self, value, consumptionRate = 0, metric = "total", description = "") :
                " Metric should be one of 'total','average','max','min' "
                " consumptionRate specifies te consumption rate for the report "
                " processor usage.  The cinsumption rate is a sclaing factor that "
                " indicates the average percentage of utilization. "
                if consumptionRate>0 : pstring = "consumptionRate=\""+str(consumptionRate)+"\" "
                else : pstring = ""
                self.AppendToList(self.RecordData, "Processors",
                        pstring+self.Metric(metric)+self.Description(description),
                        str(value))
        def ServiceLevel(self, value, type, description = ""):
                self.AppendToList(self.RecordData, "ServiceLevel", self.Type(type)+self.Description(description), str(value))


        def Resource(self,description,value) :
                self.AppendToList(self.RecordData, "Resource", self.Description(description), str(value))
        def AdditionalInfo(self,description,value) :
                self.Resource(description,value)


        def XmlCreate(self):
                self.XmlData.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                self.XmlData.append("<JobUsageRecord xmlns=\"http://www.gridforum.org/2003/ur-wg\"\n")
                self.XmlData.append("		xmlns:urwg=\"http://www.gridforum.org/2003/ur-wg\"\n")
                self.XmlData.append("		xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n")
#               self.XmlData.append("		xsi:schemaLocation=\"http://www.gridforum.org/2003/ur-wg file:/Users/bekah/Documents/GGF/URWG/urwg-schema.09.xsd\">\n")
                self.XmlData.append("		xsi:schemaLocation=\"http://www.gridforum.org/2003/ur-wg file:///u:/OSG/urwg-schema.11.xsd\">\n")

                # Add the record indentity
                self.XmlData.append("<RecordIdentity urwg:recordId=\""+str(RecordPid)+"\" urwg:createTime=\""+TimeToString(time.gmtime())+"\" />\n");

                if len(self.JobId)>0 :
                        self.XmlData.append("<JobIdentity>\n")
                        for data in self.JobId:
                                self.XmlData.append("\t")
                                self.XmlData.append(data)
                                self.XmlData.append("\n")
                        self.XmlData.append("</JobIdentity>\n")
                if len(self.UserId)>0 :
                        self.XmlData.append("<UserIdentity>\n")
                        for data in self.UserId:
                                self.XmlData.append("\t")
                                self.XmlData.append(data)
                                self.XmlData.append("\n")
                        self.XmlData.append("</UserIdentity>\n")
                for data in self.RecordData:
                        self.XmlData.append("\t")
                        self.XmlData.append(data)
                        self.XmlData.append("\n")
                self.XmlData.append("</JobUsageRecord>\n");

def LocalJobId(record,value):
        record.LocalJobId(value);

def GlobalJobId(record,value):
        record.GlobalJobId(value);

def ProcessJobId(record,value):
        record.ProcessJobId(value);

def Send(record):
        DebugPrint(0,"Record: ",record)
        DebugPrint(0,"Username: ", record.Username)

        # Assemble the record into xml

        record.XmlCreate()

        # Open the back up file
        # fill the back up file

        dirIndex = 0
        recordIndex = 0
        success = False
        ind = 0
        f = 0
        while not success:
                (f,dirIndex,recordIndex) = OpenNewRecordFile(dirIndex,recordIndex)
                DebugPrint(0,"Will save in the record in:",f.name)
                DebugPrint(3,"DirIndex=",dirIndex," RecordIndex=",recordIndex)
                if f.name == "<stdout>":
                        success = True
                else:
                        try:
                                for line in record.XmlData:
                                        f.write(line)
                                f.flush();
                                if f.tell() > 0:
                                        success = True
                                        DebugPrint(3,"suceeded to fill: ",f.name)
                                else:
                                        DebugPrint(2,"failed to fill: ",f.name)
                                        if f.name != "<stdout>": os.remove(f.name)
                        except:
                                DebugPrint(2,"failed to fill with exception: ",f.name,"--", sys.exc_info(),"--",sys.exc_info()[0],"++",sys.exc_info()[1])
                                if f.name != "<stdout>": os.remove(f.name)

        # Attempt to send the record to the collector
        sent = False

        if not sent and f.name == "<stdout>":
                print "Error: Gratia was un-enable to send the record and was unable to"
                print "       find a location to store the xml backup file.  The record"
                print "       will be printed to stdout:"
                for line in record.XmlData:
                        f.write(line)
        if sent:
                # if success delete the file
                os.remove(f.name)
        else:
                # else record the file into the list of
                # unsent files
                OutstandingRecord.append(f.name)

        # Try to send lingering files
        DebugPrint(0,"List of Outstanding records: ",OutstandingRecord)




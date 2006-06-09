package net.sf.gratia.storage;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Title: BackupFileManager</p>
 *
 * <p>Description: Manage the backup data payload xml files.  Thoses backups files
 * are assumed to have been stored in subdirectory named gratia and end with
 * the gratia extension (.gratia.xml)  </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 */
public class BackupFileManager {
	
    public BackupFileManager() {
        Initialize();
    }

    /**
     * Initialize
     */
    public void Initialize() {
        Utils.GratiaInfo("Initializing Gratia");
        // Setting the pid
        java.util.Random r = new java.util.Random();
        RecordPid = r.nextInt();
        // Need to initialize the list of possible directories
        InitDirList();
        // Need to look for left over files
        SearchOustandingRecord();

    }

    /**
     * DirListAdd
     *
     * Utility method to add directory to the list of directories
     * to be used for backup of the xml record
     *
     * @param value String
     */
    protected void DirListAdd(String value) {
        if (BackupDirList == null)
            BackupDirList = new java.util.LinkedList();
        if (value != null && value.length() > 0 && value != "None") {
            BackupDirList.add(value);
        }
    }

    /**
     * InitDirList
               "Initialize the list of backup directories"
               "We prefer $DATA_DIR, but will also (if needed)"
               "try various tmp directory (/var/tmp, /tmp,"
               "$TMP_DIR, etc.."
     */
    protected void InitDirList() {

        // getenv works with jdk 1.5 but does not work at all with jdk 1.4

        /*
        DirListAdd(System.getenv("DATA_DIR"));
        DirListAdd("/var/tmp");
        DirListAdd("/tmp");

        DirListAdd(System.getenv("TMP_DIR"));
        DirListAdd(System.getenv("TMP_WN_DIR"));
        DirListAdd(System.getenv("TMP"));
        DirListAdd(System.getenv("TMPDIR"));
        DirListAdd(System.getenv("TMP_DIR"));
        DirListAdd(System.getenv("TEMP"));
        DirListAdd(System.getenv("TEMPDIR"));
        DirListAdd(System.getenv("TEMP_DIR"));
        DirListAdd(System.getenv("HOME"));
        */

        DirListAdd(System.getProperty("DATA_DIR"));
        DirListAdd("/var/tmp");
        DirListAdd("/tmp");

        DirListAdd(System.getProperty("TMP_DIR"));
        DirListAdd(System.getProperty("TMP_WN_DIR"));
        DirListAdd(System.getProperty("TMP"));
        DirListAdd(System.getProperty("TMPDIR"));
        DirListAdd(System.getProperty("TMP_DIR"));
        DirListAdd(System.getProperty("TEMP"));
        DirListAdd(System.getProperty("TEMPDIR"));
        DirListAdd(System.getProperty("TEMP_DIR"));
        DirListAdd(System.getProperty("HOME"));

        Utils.GratiaInfo("List of backup directories: " + BackupDirList);
    }

    /**
     * SearchOustandingRecord
                "Search the list of backup directories for"
                "any record that has not been sent yet"
     */
    public void SearchOustandingRecord() {
        if (BackupDirList == null)
            return;
        if (OutstandingRecord == null)
            OutstandingRecord = new java.util.LinkedList();
        for (Iterator i = BackupDirList.iterator(); i.hasNext(); ) {
            String dir = (String) i.next();
            File f = new File(dir, "gratia");
            if (f.exists()) {
                Utils.GratiaDebug("Looking at " + f);
                File[] files = f.listFiles();
                for (int j = 0; j < files.length; j = j + 1) {
                    File rec = files[j];
                    if (rec.getName().endsWith(GratiaExtension) &&
                        !OutstandingRecord.contains(rec)) {
                        OutstandingRecord.add(rec);
                    }
                }
            }
        }
        Utils.GratiaInfo("List of Outstanding records: " + OutstandingRecord);
    }

    /**
     * GenerateFilename
     *
            "Generate a filename of the for gratia/r$index.$pid.gratia.xml"
            "in the directory 'dir'"

     * @param dir Sting
     * @param RecordIndex Integer
     * @return String
     */
    public String GenerateFilename(String dir, Integer RecordIndex) {
        String filename = "r"+RecordIndex+"."+RecordPid+GratiaExtension;
        File f = new File(dir,filename);
        return f.getAbsolutePath();
    }

    /**
     * OpenNewRecordFile
     *
        "Try to open the first available file"
        "DirIndex indicates which directory to try first"
        "RecordIndex indicates which file index to try first"
        "The routine returns the opened file and the next"
        "directory index and record index"
        "If all else fails, we print the xml to stdout"

     * @param DirIndex String
     * @param RecordIndex Integer
     * @return File
     */
    public File OpenNewRecordFile(Integer DirIndex,Integer RecordIndex) {

        // The file name will be r$index.$pid.gratia.xml

        Utils.GratiaDebug("Open request: "+DirIndex+" "+RecordIndex);
        int index = 0;
        for(Iterator i = BackupDirList.iterator(); i.hasNext(); ) {
            File dir = new File((String) i.next());
            index = index + 1;
            if (index <= DirIndex.intValue() || !dir.exists()) {
                continue;
            }
            Utils.GratiaDebug("Open request: looking at " + dir);
            dir = new File(dir, "gratia");
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    continue;
                }
                if (!dir.exists()) {
                    continue;
                }
                if (!dir.canRead() || !dir.canWrite()) {
                    continue;
                }
                String filename = GenerateFilename(dir.getAbsolutePath(), RecordIndex);
                File f = new File(filename);
                while (f.exists() && !f.canWrite()) {
                    RecordIndex = new Integer(RecordIndex.intValue() + 1);
                    f = new File(GenerateFilename(dir.getAbsolutePath(), RecordIndex));
                }
                Utils.GratiaInfo("Creating file:" + f);
                // f = open(filename,'w')
                DirIndex = new Integer(index);
                return f;
            }
        }
        DirIndex = new Integer(index);
        return null;
    }

    public void setBackupDirList(List BackupDirList) {
        this.BackupDirList = BackupDirList;
    }

    public List getBackupDirList() {
        return BackupDirList;
    }

    public void setOutstandingRecord(List OutstandingRecord) {
        this.OutstandingRecord = OutstandingRecord;
    }

    public List getOutstandingRecord() {
        return OutstandingRecord;
    }

    public void setGratiaExtension(String GratiaExtension) {
        this.GratiaExtension = GratiaExtension;
    }

    public String getGratiaExtension() {
        return GratiaExtension;
    }

    private List BackupDirList;
    private List OutstandingRecord;
    private int RecordPid;
    private String GratiaExtension = ".gratia.xml";


}

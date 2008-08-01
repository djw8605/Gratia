package net.sf.gratia.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.helpers.LogLog;

public class TidiedDailyRollingFileAppender 
    extends DailyRollingFileAppender {

    private int maxAgeDays = -1;

    public TidiedDailyRollingFileAppender() {
        // LogLog.warn("Instantiated a TidiedDailyRollingFileAppender");
    }

    public TidiedDailyRollingFileAppender(Layout layout, String filename,
                                          String datePattern)
        throws IOException {
        super(layout, filename, datePattern);
        // LogLog.warn("Instantiated a TidiedDailyRollingFileAppender");
    }

    public void setMaxAgeDays(int value) {
        maxAgeDays = value;
    }

    public int getMaxAgeDays() {
        return maxAgeDays;
    }

    protected void subAppend(LoggingEvent event) {
        super.subAppend(event);
        checkAndDeleteFiles();
    }

    public File[] checkOldFiles() {
        // LogLog.warn("Checking for old files based on " + fileName);
        File file = new File(fileName);
        DatedFileFilter ff = new DatedFileFilter(getDatePattern(),
                                                 file.getName(),
                                                 maxAgeDays);
        return file.getParentFile().listFiles(ff);
    }

    private void checkAndDeleteFiles() {
        for (File dFile : checkOldFiles()) {
            LogLog.debug("Deleting old log file " + dFile.getName());
            dFile.delete();
        }
    }


}

class DatedFileFilter implements FilenameFilter {

    private String filename;
    private SimpleDateFormat sdf;
    private int maxAgeDays;

    public DatedFileFilter(String datePattern, String filename, int maxAgeDays) {
        // LogLog.warn("Instantiated a DatedFileFilter(" + datePattern + ", " + filename + ", " + maxAgeDays + ")");
        sdf = new SimpleDateFormat(datePattern);
        this.filename = filename;
        this.maxAgeDays = maxAgeDays;
    }

    public boolean accept(File dir, String name) {
        // LogLog.warn("DatedFileFilter.accept(" + dir + ", " + name + ")");
        boolean result = false;
        if (maxAgeDays < 0) {
            // LogLog.warn("DatedFileFilter.accept(" + dir + ", " + name + "): maxAgeDays < 0");
            return result; // Quick kick out
        }
        if (!name.startsWith(filename)) {
            // LogLog.warn("DatedFileFilter.accept(" + dir + ", " + name + "): no match to " + filename);
            return result; // Not our file
        }
        ParsePosition pos = new ParsePosition(filename.length());
        Date filedate = sdf.parse(name, pos);
        // LogLog.warn("DatedFileFilter.accept(" + dir + ", " + name + "): SimpleDateFormat.parse() returned " +
        // ((filedate == null)?"null":filedate.toString()) +
        // ", error pos = " + pos.getErrorIndex());
        if ((filedate == null) || (pos.getErrorIndex() > -1)) {
            return result;
        }
        GregorianCalendar fileCal = new GregorianCalendar();
        fileCal.setTime(filedate);
        GregorianCalendar cmpCal = new GregorianCalendar();
        cmpCal.add(Calendar.DAY_OF_MONTH, -1 * maxAgeDays);
        // LogLog.warn("DatedFileFilter.accept(" + dir + ", " + name + "): comparing " + filedate + " to " + cmpCal.getTime());
        if (cmpCal.after(fileCal)) {
            // LogLog.warn("DatedFileFilter.accept(" + dir + ", " + name + "): identified old file " + name);
            result = true;
        }
        return result;
    }

}

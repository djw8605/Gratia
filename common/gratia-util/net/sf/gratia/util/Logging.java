package net.sf.gratia.util;

import java.io.File;
import java.util.*;
import java.text.*;
import java.util.logging.*;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Layout;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.net.SyslogAppender;

public class Logging {
    static Logger oldLogger;
    static org.apache.log4j.Logger log4jLogger;

    static boolean initialized = false;
    static boolean console = false;
    static DateFormat format = new SimpleDateFormat("kk:mm:ss z");
    static DateFormat screenFormat = new SimpleDateFormat("MMM dd, yyyy kk:mm:ss z");
    static String logDomain = "Unknown";

    public static synchronized void initialize(String logDomain) {
        if (initialized)
            return;

	// HK: This seems to be the ultimate solution
	// HK: All other uses of TimeZone.setDefault should be removed to clear confusions..
	TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        screenFormat.setTimeZone(TimeZone.getDefault());
        Properties p = net.sf.gratia.util.Configuration.getProperties();

        String path = p.getProperty("service." + logDomain + ".logfile" , "/gratia-" + logDomain + ".log");
        String maxSize = p.getProperty("service." + logDomain + ".maxlog" , "10000000");
        String useConsole = p.getProperty("service." + logDomain + ".console", "0");
        String level = p.getProperty("service." + logDomain + ".level" , "INFO");
        String sNumLogFiles = p.getProperty("service." + logDomain + ".numLogs" , "30");

        Logging.logDomain = logDomain;
        try {
            int numLogFiles = 0;
            try {
                numLogFiles = Integer.valueOf(sNumLogFiles).intValue();
            }
            catch (Exception ignore) {
            }
            if (numLogFiles == 0) numLogFiles = 3;
            int limit = 0;
            try {
                Integer.parseInt(maxSize);
            }
            catch (Exception ignore) {
            }
            if (limit == 0) limit = 10000000;
            if ((level == null) || (level.length() == 0)) {
                logToScreen("logging level not set -- defaulting to INFO");
                level = "INFO";
            }
            if ((path == null) || path.length() == 0) {
                logToScreen("path for log file null or empty: activating logging to console");
                useConsole = "1";
            }
        
            if (p.getProperty("service.logging.useLog4j", "1").equals("1")) {
                // Use log4j

                // Fix path from old style;
/*
                if (path.contains("%g")) {
                    String newPath = System.getProperty("catalina.home") + (path.startsWith("/")?"":"/") + path;
                    newPath = newPath.replaceFirst("-?%g", "");
                    logToScreen("fixing old-style path spec " +
                                path + " to log4j-style " + newPath);
                    path = newPath;
                } else {
                    String newPath = System.getProperty("catalina.home");
                    if (newPath == null) {
                        newPath = ".";
                    }
                    newPath = newPath + (path.startsWith("/")?"":"/") + path;
                    logToScreen("Use log4j-style path: "+newPath);
                    path = newPath;
                }
*/
		path = "/var/log/gratia-service/" + path;
                Layout layout =
                    new PatternLayout("%d %c{2}(%t) [%p]: %m%n");

                log4jLogger = org.apache.log4j.Logger.getLogger("net.sf.gratia." + logDomain);
                log4jLogger.setAdditivity(false); // Don't propagate to root logger
                FileAppender appender = null;
                if (p.getProperty("service.logging.dailyLogging", "0").equals("1")) {
                    appender = new TidiedDailyRollingFileAppender();
                    ((TidiedDailyRollingFileAppender) appender).setDatePattern("'.'yyyy-MM-dd");
                    ((TidiedDailyRollingFileAppender) appender).setMaxAgeDays(numLogFiles);
                } else {
                    appender = new RollingFileAppender();
                    ((RollingFileAppender) appender).setMaximumFileSize(limit);
                    ((RollingFileAppender) appender).setMaxBackupIndex(numLogFiles);
                }
                appender.setFile(path);
                appender.setAppend(true);
                appender.setBufferedIO(false);
                //                appender.setBufferSize(4096);
                appender.setLayout(layout);
                appender.activateOptions();
                File logFile = new File(appender.getFile());
                if ((RollingFileAppender.class.isInstance(appender)) &&
                    (logFile.length() > 0L)) {
                    logToScreen("Rolling over existing non-zero log file " + logFile);
                    ((RollingFileAppender) appender).rollOver();
                }
                log4jLogger.setLevel(LogLevel.toLevel(level));
                log4jLogger.addAppender(appender);

                String syslog_host = p.getProperty("service.logging.syslog_host", "example.com"); 
                if (!syslog_host.equals("example.com")) {
                  SyslogAppender syslog_appender = new SyslogAppender();
                  syslog_appender.setHeader(true);
                  syslog_appender.setSyslogHost(syslog_host);
                  log4jLogger.addAppender(syslog_appender);
                }
                
                if (useConsole.equals("1")) {
                    ConsoleAppender consoleAppender =
                        new ConsoleAppender(layout);
                    consoleAppender.activateOptions();
                    log4jLogger.addAppender(consoleAppender);
                }
                logToScreen("logging level set to " + log4jLogger.getLevel() + " (log4j)");

            } else { // Old-style logger
                logToScreen("using legacy logger path");
            
                try {
                    FileHandler fh = new FileHandler("/var/log/gratia-service" + path, limit, numLogFiles);
                    fh.setFormatter(new SimpleFormatter());
                    // Add to logger
                    oldLogger = Logger.getLogger("gratia");
                    oldLogger.setUseParentHandlers(false);
                    oldLogger.addHandler(fh);
                }
                catch (Exception e) {
                    logToScreen("caught exception initializing logging to " +
                                "/var/log/gratia-service" + path + ": " + e);
                    logToScreen("activating Console logging for this stream");
                    useConsole = "1";
                }
                if (useConsole.equals("1")) {
                    oldLogger.addHandler(new ConsoleHandler());
                    console = true;
                }
                if (level.equals("ALL"))
                    oldLogger.setLevel(Level.ALL);
                else if (level.equals("CONFIG"))
                    oldLogger.setLevel(Level.CONFIG);
                else if (level.equals("FINE"))
                    oldLogger.setLevel(Level.FINE);
                else if (level.equals("FINER"))
                    oldLogger.setLevel(Level.FINER);
                else if (level.equals("FINEST"))
                    oldLogger.setLevel(Level.FINEST);
                else if (level.equals("INFO"))
                    oldLogger.setLevel(Level.INFO);
                else if (level.equals("OFF"))
                    oldLogger.setLevel(Level.OFF);
                else if (level.equals("SEVERE"))
                    oldLogger.setLevel(Level.SEVERE);
                else if (level.equals("WARNING"))
                    oldLogger.setLevel(Level.WARNING);
                logToScreen("logging level set to " + oldLogger.getLevel() + " (old-style logging)");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        initialized = true;
    }

    public static void fine(String message) {
        log(LogLevel.FINE, message);
    }

    public static void fine(String message, Exception ex) {
        log(LogLevel.FINE, message, ex);
    }

    public static void log(org.apache.log4j.Level level, String message) {
        if (! initialized) {
            logToScreen("Logger Not Initialized!");
            return;
        }
        if (log4jLogger != null) {
            log4jLogger.log(level, message);
        } else {
            oldLogger.log(Level.parse(level.toString()), message);
            if (console)
                System.out.println(format.format(new Date()) + ": " + message);
        }
    }
    
    public static void log(org.apache.log4j.Level level, String message, Exception ex) {
        if (! initialized) {
            logToScreen("Logger Not Initialized!");
            return;
        }
        if (log4jLogger != null) {
            log4jLogger.log(level, message, ex);
        } else {
            oldLogger.log(Level.parse(level.toString()), message, ex);
            if (console)
                System.out.println(format.format(new Date()) + ": " + message);
        }
    }
    
    public static void log(String message) {
        if (! initialized) {
            logToScreen("Logger Not Initialized!");
            return;
        }
        if (log4jLogger != null) {
            log4jLogger.log(LogLevel.FINER, message);
        } else {
            oldLogger.finer(message);
            if (console)
                System.out.println(format.format(new Date()) + ": " + message);
        }
    }
    
    public static void log(String message, Exception ex) {
        if (! initialized) {
            logToScreen("Logger Not Initialized!");
            return;
        }
        if (log4jLogger != null) {
            log4jLogger.log(LogLevel.FINER, message, ex);
        } else {
            oldLogger.log(Level.FINER,message,ex);
            if (console) {
                System.out.println(format.format(new Date()) + ": " + message);
                ex.printStackTrace();
            }
        }
    }
    
    public static void info(String message) {
        if (! initialized) {
            logToScreen("Logger Not Initialized!");
            return;
        }
        if (log4jLogger != null) {
            log4jLogger.log(LogLevel.INFO, message);
        } else {
            oldLogger.info(message);
            if (console)
                System.out.println(format.format(new Date()) + ": " + message);
        }
    }
    
    public static void warning(String message) {
        if (! initialized) {
            logToScreen("Logger Not Initialized!");
            return;
        }
        if (log4jLogger != null) {
            log4jLogger.log(LogLevel.WARNING, message);
        } else {
            oldLogger.warning(message);
            if (console)
                System.out.println(format.format(new Date()) + ": " + message);
        }
    }
    
    public static void warning(String message, Exception ex) {
        if (! initialized) {
            logToScreen("Logger Not Initialized!");
            return;
        }
        if (log4jLogger != null) {
            log4jLogger.log(LogLevel.WARNING, message, ex);
        } else {
            oldLogger.log(Level.WARNING,message,ex);
            if (console)
                System.out.println(format.format(new Date()) + ": " + message);
        }
    }
    
    public static void debug(String message) {
        if (! initialized) {
            logToScreen("Logger Not Initialized!");
            return;
        }
        if (log4jLogger != null) {
            log4jLogger.log(LogLevel.FINEST, message);
        } else {
            oldLogger.finest(message);
            if (console)
                System.out.println(format.format(new Date()) + ": " + message);
        }
    }

    public static void debug(String message, Exception ex) {
        if (! initialized) {
            logToScreen("Logger Not Initialized!");
            return;
        }
        if (log4jLogger != null) {
            log4jLogger.log(LogLevel.FINEST, message, ex);
        } else {
            oldLogger.log(Level.FINEST, message, ex);
            if (console)
                System.out.println(format.format(new Date()) + ": " + message);
        }
    }
    
    public static void logToScreen(String message) {
        System.out.println(screenFormat.format(new Date()) + " Logging (" + logDomain + "): " + message);
    }

}

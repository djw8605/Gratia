package net.sf.gratia.util;

import org.apache.log4j.Level;

public class LogLevel extends Level {

    static public final int SEVERE_INT = Level.ERROR_INT;
    static public final int WARNING_INT = Level.WARN_INT;
    static public final int CONFIG_INT =
        (Level.DEBUG_INT + ((Level.INFO_INT - Level.DEBUG_INT)/2));
    static public final int FINE_INT = Level.DEBUG_INT;
    static public final int FINER_INT =
        (Level.TRACE_INT + ((Level.DEBUG_INT - Level.TRACE_INT)/2));
    static public final int FINEST_INT = Level.TRACE_INT;

    private static String SEVERE_STR = "SEVERE";
    private static String WARNING_STR = "WARNING";
    private static String CONFIG_STR = "CONFIG";
    private static String FINE_STR = "FINE";
    private static String FINER_STR = "FINER";
    private static String FINEST_STR = "FINEST";

    public static final LogLevel SEVERE =
        new LogLevel(SEVERE_INT, SEVERE_STR, 0);
    public static final LogLevel WARNING =
        new LogLevel(WARNING_INT, WARNING_STR, 2);
    public static final LogLevel CONFIG =
        new LogLevel(CONFIG_INT, CONFIG_STR, 4);
    public static final LogLevel FINE =
        new LogLevel(FINE_INT, FINE_STR, 5);
    public static final LogLevel FINER =
        new LogLevel(FINER_INT, FINER_STR, 6);
    public static final LogLevel FINEST =
        new LogLevel(FINEST_INT, FINEST_STR, 6);

    protected LogLevel(int level, String strLevel, int syslogEquiv) {
        super(level, strLevel, syslogEquiv);
    }

    public static Level toLevel(String sArg) {
        return (Level) toLevel(sArg, LogLevel.FINEST);
    }

    public static Level toLevel(String sArg, Level defaultValue) {
        if (sArg == null) {
            return defaultValue;
        }
        String stringVal = sArg.toUpperCase();

        if (stringVal.equals(SEVERE_STR)) {
            return LogLevel.SEVERE;
        } else if (stringVal.equals(WARNING_STR)) {
            return LogLevel.WARNING;
        } else if (stringVal.equals(CONFIG_STR)) {
            return LogLevel.CONFIG;
        } else if (stringVal.equals(FINE_STR)) {
            return LogLevel.FINE;
        } else if (stringVal.equals(FINER_STR)) {
            return LogLevel.FINER;
        } else if (stringVal.equals(FINEST_STR)) {
            return LogLevel.FINEST;
        }

        return Level.toLevel(sArg, (Level) defaultValue);
    }

    public static Level toLevel(int i) throws IllegalArgumentException {
        switch (i) {
        case SEVERE_INT: return LogLevel.SEVERE;
        case WARNING_INT: return LogLevel.WARNING;
        case CONFIG_INT: return LogLevel.CONFIG;
        case FINE_INT: return LogLevel.FINE;
        case FINER_INT: return LogLevel.FINER;
        case FINEST_INT: return LogLevel.FINEST;
        }
        return Level.toLevel(i);
    }

}

package net.sf.gratia.storage;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Date;
import java.util.regex.*;
import java.lang.Integer;

public class Duration {

    public class DurationParseException extends Exception {
        public DurationParseException(String message) {
            super(message);
        }
        public DurationParseException(String message, Throwable cause) {
            super(message, cause);
        }
        public DurationParseException(Throwable cause) {
            super(cause);
        }
    }

    public enum DurationUnit {
        HOUR(Calendar.HOUR),
            DAY(Calendar.DAY_OF_MONTH),
            WEEK(Calendar.WEEK_OF_YEAR),
            MONTH(Calendar.MONTH),
            YEAR(Calendar.YEAR),
            UNLIMITED(Calendar.ERA);

        private final int calField;

        DurationUnit(int calField) { this.calField = calField; }

        public int calField() { return calField; }
    };

    int ordinality;
    DurationUnit unit;

    static final DurationUnit defaultUnit = DurationUnit.MONTH;
    static  Pattern durationPattern =
        Pattern.compile("(\\d+)\\s*([HDWMY]|UNLIMITED)?",
                        Pattern.CASE_INSENSITIVE);
    public int ordinality() {
        return ordinality;
    }

    public DurationUnit unit() {
        return unit;
    }

    public Duration() {
        ordinality = 0;
        unit = DurationUnit.UNLIMITED;
    }

    public Duration(int ordinality, DurationUnit unit) {
        this.ordinality = ordinality;
        this.unit = unit;
    }

    public Duration(String property) throws DurationParseException {
        initializeByProperty(property, defaultUnit);
    }

    public Duration(String property, DurationUnit defaultUnit) throws DurationParseException {
        initializeByProperty(property, defaultUnit);
    }

    private void initializeByProperty(String property, DurationUnit defaultUnit)
        throws DurationParseException {
        if ((property == null) || (property.length() == 0)) {
            unit = DurationUnit.UNLIMITED;
        } else {
            String low = property.toLowerCase();
            if (low.equals("unlimited")) {
                unit = DurationUnit.UNLIMITED;
            } else {
                Matcher m = durationPattern.matcher(low);
                if (m.lookingAt()) {
                    ordinality = Integer.parseInt(m.group(1));
                    char switchChar;
                    if (m.group(2) == null) {
                        unit = defaultUnit;
                    } else {
                        switch(m.group(2).charAt(0)) {
                        case 'h':
                            unit = DurationUnit.HOUR;
                            break;
                        case 'd':
                            unit = DurationUnit.DAY;
                            break;
                        case 'w':
                            unit = DurationUnit.WEEK;
                            break;
                        case 'm':
                            unit = DurationUnit.MONTH;
                            break;
                        case 'y':
                            unit = DurationUnit.YEAR;
                            break;
                        case 'u':
                            unit = DurationUnit.UNLIMITED;
                            break;
                        default:
                            throw new
                                DurationParseException("unable to parse unit specification \"" +
                                                       m.group(2) + "\"");
                        }
                    }
                } else {
                    throw new
                        DurationParseException("ExpirationDateCalculator.Duration: unable to parse lifetime specification \"" +
                                               property + "\"");
                }
            }
        }
    }

    public long msFromDate(Date refDate) {
        if (unit == DurationUnit.UNLIMITED) {
            return Long.MAX_VALUE;
        } else {
            GregorianCalendar cal = new
                GregorianCalendar(TimeZone.getTimeZone("GMT"));
            cal.setTime(refDate);
            cal.add(unit.calField(),
                    ordinality);
            return cal.getTime().getTime() - refDate.getTime();

        }
    }

    @Override
    public String toString() {
        if (unit == DurationUnit.UNLIMITED)
            return "UNLIMITED";
        else return ordinality + " " + unit + (((ordinality == 1 ) || (ordinality == -1))?"":"S");
    }

}


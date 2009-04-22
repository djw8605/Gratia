package net.sf.gratia.storage;

import net.sf.gratia.util.Logging;
import net.sf.gratia.storage.Duration.*;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Enumeration;
import java.util.regex.*;

public class ExpirationDateCalculator {
    // Caching handler class for calculating expiration dates of various
    // types of data.
    //
    // Two levels of caching:
    //
    // 1. Pre-parsed limit values from properties file.
    //
    // 2. Calculated expiration dates.
    static Hashtable limitCache = new Hashtable(); // Pre-parsed limit values
    static Lock cacheLock = new ReentrantLock(); // Lock for concurrency issues
    static Properties p; // Everyone sees the same properties

    Properties changeDetector;

    static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    static final Date invalidDate = new Date(0);

    Hashtable eDateCache = new Hashtable(); // Pre-calculated expiration dates still valid?
    String lastExpirationRefDate = ""; // Pre-calculated expiration dates still valid?

    public ExpirationDateCalculator() {
        cacheLock.lock();
        try {
            if (p == null) {
                p = net.sf.gratia.util.Configuration.getProperties(); // Read once
                Pattern lifetimePattern = Pattern.compile("service\\.lifetime\\.(.*)");
                Enumeration properties = p.keys();
                while (properties.hasMoreElements()) {
                    String key = (String) properties.nextElement();
                    Matcher m = lifetimePattern.matcher(key);
                    if (m.lookingAt()) { // Match
                        // Parse and cache
                        try {
                            Duration duration = new Duration(p.getProperty(key));
                            limitCache.put(m.group(1).toLowerCase(), duration);
                            Logging.fine("ExpirationDateCalculator found lifetime property " +
                                         key + " of " + duration);
                        }
                        catch (DurationParseException e) {
                            Logging.warning("ExpirationDateCalculator: found problem with lifetime property " +
                                            key + ": error was, " +
                                            e.getMessage() + " -- property ignored");
                        }
                    } else {
                        // Trim properties object to only have lifetime properties.
                        p.remove(key);
                    }
                }
            }
        }
        finally {
            cacheLock.unlock();
        }
        
    }

    public Properties lifetimeProperties() {
        return p;
    }

    public String expirationDateAsSQLString(Date refDate, String table) {
        Date result = expirationDate(refDate, table, "");
        return ((result.equals(invalidDate))?"":dateFormatter.format(result));
    }

    public String expirationDateAsSQLString(Date refDate, String table,
                                         String qualifier) {
        Date result = expirationDate(refDate, table, qualifier);
        return ((result.equals(invalidDate))?"":dateFormatter.format(result));        
    }

    public Date expirationDate(Date refDate, String table,
                               String qualifier) {
        String key = (table + ((qualifier.length() > 0)?("." + qualifier):"")).toLowerCase();
        String date = dateFormatter.format(refDate);
        cacheLock.lock();
        GregorianCalendar cal = new
            GregorianCalendar(TimeZone.getTimeZone("GMT"));
        try {
            if (lastExpirationRefDate.equals(date)) {
                if (eDateCache.containsKey(key)) {
                    return (Date) eDateCache.get(key);
                } else {
                    // Calendar starts from the reference date
                    try {
                        cal.setTime(dateFormatter.parse(lastExpirationRefDate));
                    }
                    catch (java.text.ParseException e) {
                        Logging.warning("ExpirationDateCalculator: internal error: could not parse " +
                                        lastExpirationRefDate, e);
                        return (Date) invalidDate.clone();
                    }
                }
            } else {
                // Reset cache.
                eDateCache = new Hashtable();
                // New calendar set to refDate
                cal.setTime(refDate);
                // Expiration date calculated with respect to the
                // beginning of the day
                cal.set(GregorianCalendar.MILLISECOND, 0);
                cal.set(GregorianCalendar.SECOND, 0);
                cal.set(GregorianCalendar.MINUTE, 0);
                cal.set(GregorianCalendar.HOUR, 0);
                lastExpirationRefDate = dateFormatter.format(cal.getTime());
            }
            // Obtain the duration from the limit cache
            try {
                Duration limitDuration =
                    (Duration) limitCache.get(key);
                if (limitDuration.unit().equals(DurationUnit.UNLIMITED)) {
                    eDateCache.put(key, invalidDate);
                    return (Date) invalidDate.clone();
                } else {
                    cal.add(limitDuration.unit().calField(),
                            -1 * limitDuration.ordinality());
                    eDateCache.put(key, cal.getTime());
                    return cal.getTime();
                }
            }
            catch (NullPointerException e) {
                // No property for that, return invalid date;
                return (Date) invalidDate.clone();
            }
        }
        finally {
            cacheLock.unlock();
        }
    }

    public Date expirationDate(Date refDate, String table) {
        return expirationDate(refDate, table, "");
    }

}

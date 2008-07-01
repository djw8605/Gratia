package net.sf.gratia.services;

import net.sf.gratia.util.Logging;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    static final Date invalidDate = new Date(0);

    Hashtable eDateCache = new Hashtable(); // Pre-calculated expiration dates still valid?
    String lastExpirationRefDate; // Pre-calculated expiration dates still valid?

    public ExpirationDateCalculator() {
        refreshLimits();
    }

    public Properties refreshLimits() {
        p = net.sf.gratia.util.Configuration.getProperties();        
        limitCache = new Hashtable();
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
        return (Date) invalidDate.clone();
    }

    public Date expirationDate(Date refDate, String table) {
        return expirationDate(refDate, table, "");
    }

}

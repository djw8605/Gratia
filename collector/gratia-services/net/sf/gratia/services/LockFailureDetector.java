package net.sf.gratia.services;

import java.text.*;
import java.util.regex.Pattern;

import org.hibernate.exception.LockAcquisitionException;
import net.sf.gratia.util.Logging;

public class LockFailureDetector {
    
    private static Pattern fRestartMatcher = Pattern.compile("try restarting transaction");

    public static Boolean detectAndReportLockFailure(Exception e, Integer nTries, String ident) {
        Throwable cause = e.getCause();
        if ((e instanceof LockAcquisitionException)
            || ( (cause != null) && fRestartMatcher.matcher(cause.getMessage()).find())) {
            if (nTries == 1) {
                Logging.info(ident + ": lock exception: trying a second time.");
            } else if (nTries < 5) {
                Logging.warning(ident + ": multiple contiguous lock errors: keep trying.");
            } else if (nTries == 5) {
                Logging.warning(ident + ": multiple contiguous lock errors: keep trying (warnings throttled).");
            } else if ( (nTries % 100) == 0) {
                Logging.warning(ident + ": hit " + nTries + " contiguous lock errors: check DB.");
            }
            return true;
        } else {
            return false;
        }
    }
}

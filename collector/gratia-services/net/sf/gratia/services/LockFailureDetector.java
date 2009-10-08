package net.sf.gratia.services;

import java.text.*;
import org.hibernate.exception.LockAcquisitionException;
import net.sf.gratia.util.Logging;

public class LockFailureDetector {
    public static Boolean detectAndReportLockFailure(Exception e, Integer nTries, String ident) {
        Throwable cause = e.getCause();
        if ((e instanceof LockAcquisitionException)
            || ( (cause != null) && cause.getMessage().matches(".*try restarting transaction.*"))) {
            if (nTries == 1) {
                Logging.info(ident + ": lock exception.  Trying a second time.");
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

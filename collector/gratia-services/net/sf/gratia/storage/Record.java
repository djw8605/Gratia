package net.sf.gratia.storage;

import java.util.Date;
import net.sf.gratia.services.ExpirationDateCalculator;

/**
 * <p>Title: Record </p>
 *
 * <p>Description: Base class of the Gratia Records</p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Chris Green
 * @version 1.0
 */
public abstract class Record implements RecordInterface {

    static ExpirationDateCalculator eCalc =
        new ExpirationDateCalculator();

    // Returns the date of the oldest raw records we keep
    public Date getExpirationDate() {
        return eCalc.expirationDate(new Date(), getTableName());
    }

    public void executeTrigger(org.hibernate.Session session) throws Exception {
        // NOP
    }

}

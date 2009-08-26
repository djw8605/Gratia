package net.sf.gratia.storage;

import java.util.Date;

//
// Gratia's in-memory representation of a JobUsage summary record.
//

//2009/07/18 Brian Bockelman -- MINIMAL adoptations from ServiceSummary to make the class work.

public class ServiceSummaryHourly extends ServiceSummary {

    public String getTableName() {
        return "MasterServiceSummaryHourly";
    }

}

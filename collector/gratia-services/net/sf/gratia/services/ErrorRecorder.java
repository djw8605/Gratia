package net.sf.gratia.services;

import net.sf.gratia.util.Logging;

import net.sf.gratia.storage.DupRecord;
import net.sf.gratia.storage.Record;
import net.sf.gratia.storage.Probe;

import org.hibernate.Transaction;
import org.hibernate.Session;

public class ErrorRecorder {

    public ErrorRecorder() { }

    private Boolean detectAndReportLockFailure(Exception e, Integer nTries) {
        if (e instanceof org.hibernate.exception.LockAcquisitionException) {
            String ident = "ErrorRecorder: ";
            if (nTries == 1) {
                Logging.info(ident + ": lock acquisition exception.  Trying a second time.");
            } else if (nTries < 5) {
                Logging.warning(ident + ": multiple contiguous lock acquisition errors: keep trying.");
            } else if (nTries == 5) {
                Logging.warning(ident + ": multiple contiguous lock acquisition errors: keep trying (warnings throttled).");
            } else if ( (nTries % 100) == 0) {
                Logging.warning(ident + ": hit " + nTries + " contiguous lock acqusition errors: check DB.");
            }
            return true;
        } else {
            return false;
        }
    }

    private void saveDupRecord(DupRecord record) throws Exception {
        saveDupRecord(record, null);
    }

    private void saveDupRecord(DupRecord record,
                               Probe probe) throws Exception {
        Session session;
        Transaction tx;
        Integer nTries = 0;
        Boolean keepTrying = true;
        while (keepTrying) {
            ++nTries;
            session = HibernateWrapper.getSession();
            tx = session.beginTransaction();
            try {
                session.save(record);
                if (probe != null) {
                    session.saveOrUpdate(probe);
                }
                tx.commit();
                keepTrying = false;
            } catch (Exception e) {
                tx.rollback();
                session.close();
                if (!detectAndReportLockFailure(e, nTries)) {
                    Logging.warning("ErrorRecorder: error saving in table!", e);
                    keepTrying = false;
                }
            }
        }
    }

    public void saveDuplicate(String source, String error,
                              int dupdbid, Record current) throws Exception {
        DupRecord record = new DupRecord();

        record.seteventdate(new java.util.Date());
        record.setrawxml(current.asXML());
        record.setsource(source);
        record.seterror(error);
        record.setdbid(dupdbid);
        record.setRecordType(current.getTableName());

        saveDupRecord(record, current.getProbe());
    }

    public void saveDuplicate(String source, String error,
                              int dupdbid, String xml,
                              String tableName) throws Exception {
        DupRecord record = new DupRecord();

        record.seteventdate(new java.util.Date());
        record.setrawxml(xml);
        record.setsource(source);
        record.seterror(error);
        record.setdbid(dupdbid);
        record.setRecordType(tableName);

        saveDupRecord(record);
    }

    public void saveParse(String source, String error,
                          String xml) throws Exception {
        DupRecord record = new DupRecord();

        record.seteventdate(new java.util.Date());
        record.setrawxml(xml);
        record.setsource(source);
        record.seterror(error);

        saveDupRecord(record);
    }

    public void saveSQL(String source, String error,
                        Record current) throws Exception {
        DupRecord record = new DupRecord();

        record.seteventdate(new java.util.Date());
        record.setrawxml(current.asXML());
        record.setsource(source);
        record.seterror(error);
        record.setRecordType(current.getTableName());

        saveDupRecord(record, current.getProbe());
    }
}

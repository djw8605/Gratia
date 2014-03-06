package net.sf.gratia.services;

import net.sf.gratia.util.Logging;

import net.sf.gratia.storage.DupRecord;
import net.sf.gratia.storage.Record;
import net.sf.gratia.storage.Probe;

import org.hibernate.Transaction;
import org.hibernate.Session;

public class ErrorRecorder {
   
   public ErrorRecorder() { }
   
   private void saveDupRecord(DupRecord record) throws Exception {
      saveDupRecord(record, null);
   }
   
   private void saveDupRecord(DupRecord record,
                              Probe probe) throws Exception {
      Integer nTries = 0;
      Boolean keepTrying = true;
      Session session = null;
      Transaction tx = null;
      while (keepTrying) {
         ++nTries;
         try {
            session = HibernateWrapper.getSession();
            tx = session.beginTransaction();
            session.save(record);
            if (probe != null) {
               session.saveOrUpdate(probe);
            }
            tx.commit();
            keepTrying = false;
            HibernateWrapper.closeSession(session);
         } catch (Exception e) {
            HibernateWrapper.closeSession(session);
            if (!LockFailureDetector.detectAndReportLockFailure(e, nTries, "ErrorRecorder")) {
               Logging.warning("ErrorRecorder: error saving in table!", e);
               throw e; // Rethrow
            }
         }
      }
   }
   
   public void saveDuplicate(String source, String error,
                             long dupdbid, Record current) throws Exception {
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
                             long dupdbid, String xml,
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

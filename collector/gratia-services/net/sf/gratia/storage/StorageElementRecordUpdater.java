/**
 * 
 */
package net.sf.gratia.storage;

/**
 * @author pcanal
 *
 */
public abstract class StorageElementRecordUpdater implements RecordUpdater
{
   public boolean Update(Record rec)
   {
      if (rec.getClass() != StorageElementRecord.class) return true;
      return Update((StorageElementRecord)rec);
   }
   public abstract boolean Update(StorageElementRecord rec);
}

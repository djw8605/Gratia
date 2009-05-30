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
   public void Update(Record rec)
   {
      if (rec.getClass() != StorageElementRecord.class) return;
      Update((StorageElementRecord)rec);
   }
   public abstract void Update(StorageElementRecord rec);
}

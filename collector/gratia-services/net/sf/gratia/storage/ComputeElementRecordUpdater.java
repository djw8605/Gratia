/**
 * 
 */
package net.sf.gratia.storage;

/**
 * @author pcanal
 *
 */
public abstract class ComputeElementRecordUpdater implements RecordUpdater
{
   public void Update(Record rec)
   {
      if (rec.getClass() != ComputeElementRecord.class) return;
      Update((ComputeElementRecord)rec);
   }
   public abstract void Update(ComputeElementRecord rec);
}

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
   public boolean Update(Record rec)
   {
      if (rec.getClass() != ComputeElementRecord.class) return true;
      return Update((ComputeElementRecord)rec);
   }
   public abstract boolean Update(ComputeElementRecord rec);
}

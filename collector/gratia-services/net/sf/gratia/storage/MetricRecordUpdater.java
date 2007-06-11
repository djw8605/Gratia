/**
 * 
 */
package net.sf.gratia.storage;

/**
 * @author pcanal
 *
 */
public abstract class MetricRecordUpdater implements RecordUpdater
{
   public void Update(Record rec)
   {
      if (rec.getClass() != MetricRecord.class) return;
      Update((MetricRecord)rec);
   }
   public abstract void Update(MetricRecord rec);
}

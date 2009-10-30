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
   public boolean Update(Record rec)
   {
      if (rec.getClass() != MetricRecord.class) return true;
      return Update((MetricRecord)rec);
   }
   public abstract boolean Update(MetricRecord rec);
}

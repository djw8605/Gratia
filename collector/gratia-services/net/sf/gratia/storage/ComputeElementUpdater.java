/**
 * 
 */
package net.sf.gratia.storage;

/**
 * @author pcanal
 *
 */
public abstract class ComputeElementUpdater implements RecordUpdater
{
   public void Update(Record rec)
   {
      if (rec.getClass() != ComputeElement.class) return;
      Update((ComputeElement)rec);
   }
   public abstract void Update(ComputeElement rec);
}

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
   public boolean Update(Record rec)
   {
      if (rec.getClass() != ComputeElement.class) return true;
      return Update((ComputeElement)rec);
   }
   public abstract boolean Update(ComputeElement rec);
}

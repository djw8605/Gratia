/**
 * 
 */
package net.sf.gratia.storage;

/**
 * @author pcanal
 *
 */
public abstract class SubclusterUpdater implements RecordUpdater
{
   public void Update(Record rec)
   {
      if (rec.getClass() != Subcluster.class) return;
      Update((Subcluster)rec);
   }
   public abstract void Update(Subcluster rec);
}

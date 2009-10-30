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
   public boolean Update(Record rec)
   {
      if (rec.getClass() != Subcluster.class) return true;
      return Update((Subcluster)rec);
   }
   public abstract boolean Update(Subcluster rec);
}

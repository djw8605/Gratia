/**
 * 
 */
package net.sf.gratia.storage;

/**
 * @author pcanal
 *
 */
public abstract class StorageElementDescriptionUpdater implements RecordUpdater
{
   public boolean Update(Record rec)
   {
      if (rec.getClass() != StorageElement.class) return true;
      return Update((StorageElement)rec);
   }
   public abstract boolean Update(StorageElement rec);
}

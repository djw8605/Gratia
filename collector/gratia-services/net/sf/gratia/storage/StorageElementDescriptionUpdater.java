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
   public void Update(Record rec)
   {
      if (rec.getClass() != StorageElement.class) return;
      Update((StorageElement)rec);
   }
   public abstract void Update(StorageElement rec);
}

/**
 * 
 */
package net.sf.gratia.storage;

/**
 * @author pcanal
 *
 */
public interface RecordUpdater {
   public class UpdateException extends Exception {
      public UpdateException(String msg) {
         super(msg);
      }
   }
   public boolean Update(Record rec) throws UpdateException;
}

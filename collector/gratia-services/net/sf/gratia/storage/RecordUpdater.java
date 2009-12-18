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
      public UpdateException(String msg, Throwable e) {
         super(msg, e);
      }
      public UpdateException(Throwable e) {
         super(e);
      }
   }
   public boolean Update(Record rec) throws UpdateException;
}

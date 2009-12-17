package net.sf.gratia.services;

public class ConnectionException extends java.sql.SQLException {
   public ConnectionException(String msg) {
      super(msg);
   }
   public ConnectionException(String msg, Throwable e) {
      // Java 5 does not have an SQLException(String msg, Throwable e)
      super(msg);
      initCause(e);
   }
   public ConnectionException(Throwable e) {
      // Java 5 does not have an SQLException(Throwable e)
      super("ConnectionException");
      initCause(e);
   }
}

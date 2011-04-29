//package net.sf.gratia.storage.standAloneParser;
import java.io.*;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
	Class to get the stack trace as a String so that the entire stack trace could be logged to a log file etc.

	@author Karthik
	Date: Apr 2011

*/

public class StackTrace
{
   public static String getStackTrace(Throwable t)
   {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw, true);
      t.printStackTrace(pw);
      pw.flush();
      sw.flush();
      return sw.toString();
   }
}

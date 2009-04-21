package net.sf.gratia.storage;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.util.Calendar;
import javax.xml.datatype.Duration;
import javax.xml.datatype.DatatypeConfigurationException;


import java.security.MessageDigest;

/**
 * <p>Title: Utils</p>
 *
 * <p>Description: A few utility functions for the JobUsageRecord parsing.
 * Note that the printing function needs to be update to use log4j</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 */
public class Utils
{
   private static javax.xml.datatype.DatatypeFactory gDurationFactory = null;

   public Utils()
   {
   }


   /**
     * md5key
     *
     * @param input String containg the xml file
     * @return the md5 of the xml file
     */
   static public String md5key(String input) throws Exception
   {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(input.getBytes());
      return HexString.bufferToHex(md.digest());
   }

   /**
     * StringToDuration
     *
    * @param str String
    * @return double
    */
   public static double StringToDuration(String str) throws
                        DatatypeConfigurationException
   {
      if (str.compareTo("P") == 0) return 0;

      if (gDurationFactory==null) {
          gDurationFactory = javax.xml.datatype.DatatypeFactory.newInstance();
      }

      Duration du = gDurationFactory.newDurationDayTime(str.trim());
      return du.getTimeInMillis(Calendar.getInstance()) / 1000.0;
   }

   /**
     * DurationToXml
     *
     * @param str String
    * @return double
    */
   public static String DurationToXml(double val) throws
                        DatatypeConfigurationException
   {
      double seconds = (((long)(val*100)) % 6000 ) / 100.0;
      long value = ((long)(val - seconds)) / 60;
      long minutes = value % 60;
      value = (value - minutes) / 60;
      long hours = value % 24;
      long days = (value - hours) / 24;
      StringBuilder str = new StringBuilder("P");
      if (days > 0) {
         str.append(days);
         str.append("D");
      }
      if (hours>0 || minutes>0 || seconds>0) {
         str.append("T");
         if (hours>0) {
            str.append(hours).append("H");
         }
         if (minutes>0) {
            str.append(minutes).append("M");
         }
         if (seconds>0) {
            str.append(seconds).append("S");
         }
      } else {
         str.append("T0S");
      }
        
      return str.toString();
   }

   /**
     * GratiaError
     *
     * @param routine String
     * @param action String
    * @param message String
    * @param fatal Boolean
    * @throws Exception
    */
   public static void GratiaError(String routine, String action,
                                                   String message, boolean fatal) throws
                                  Exception
   {
      String full = "Error in " + routine + " during " + action + ": " +
           message;
      if (fatal)
         throw (new Exception(full));
      else
         Logging.warning(full);
   }

   public static void GratiaError(String routine, String action,
                           String message)
   {
      String full = "Error in " + routine + " during " + action + ": " +
                 message;
      Logging.warning(full);
   }

   /**
     * GratiaDebug
     *
     * @param msg String
     */
   public static void GratiaDebug(String msg)
   {
      Logging.debug(msg);
   }

   public static void GratiaInfo(String msg)
   {
      Logging.info(msg);
   }

   public static void GratiaError(Exception e)
   {
      Logging.warning(e.getMessage());
      XP xp = new XP();
      Logging.warning(xp.parseException(e));
   }

   public static void GratiaError(Exception e, String extraInfo)
   {
      Logging.warning(e.getMessage());
      XP xp = new XP();
      Logging.warning(xp.parseException(e));
      Logging.warning(extraInfo);
   }

}

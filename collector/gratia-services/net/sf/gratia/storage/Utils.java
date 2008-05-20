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
      javax.xml.datatype.DatatypeFactory fac = javax.xml.datatype.
                                     DatatypeFactory.
                                     newInstance();
      if (str.compareTo("P") == 0) return 0;

      Duration du = fac.newDurationDayTime(str.trim());
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
      javax.xml.datatype.DatatypeFactory fac = javax.xml.datatype.
                               DatatypeFactory.
                               newInstance();
      Duration du = fac.newDuration((long)(val * 1000));
      String str = du.toString();      
      return str;
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

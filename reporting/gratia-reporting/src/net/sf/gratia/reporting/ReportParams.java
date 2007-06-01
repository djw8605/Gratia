package net.sf.gratia.reporting;

import java.text.DateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class ReportParams
{

   // Converts a date from mm/dd/yyyy hh:mm:ss am where any one of the 
   // time fields are missing to a java date object
   public static Date convertStringToDate(String dateToConvert)
   {
      String working = dateToConvert;
      int pos = working.indexOf("/");
      Integer month, day, year;
      if (pos >= 0)
      {
         month = new Integer(working.substring(0, working.indexOf("/")));
         working = working.substring(working.indexOf("/") + 1);
         day = new Integer(working.substring(0, working.indexOf("/")));
         working = working.substring(working.indexOf("/") + 1);
         year = new Integer(working.substring(0, 4));
      }
      else
      {
         pos = working.indexOf("-");
         if (pos <= 2)
         {
            month = new Integer(working.substring(0, working.indexOf("-")));
            working = working.substring(working.indexOf("-") + 1);
            day = new Integer(working.substring(0, working.indexOf("-")));
            working = working.substring(working.indexOf("-") + 1);
            year = new Integer(working.substring(0, 4));
         }
         else
         {
            year = new Integer(working.substring(0, working.indexOf("-")));
            working = working.substring(working.indexOf("-") + 1);
            month = new Integer(working.substring(0, working.indexOf("-")));
            working = working.substring(working.indexOf("-") + 1);
            day = new Integer(working.substring(0, 2));
         }
      }

      Integer hour = 0;
      Integer minute = 0;
      Integer second = 0;

      if (working.indexOf(" ") > -1)
      {
         working = working.substring(working.indexOf(" ") + 1);
         hour = new Integer(working.substring(0, working.indexOf(":")));
         working = working.substring(working.indexOf(":") + 1);
         if (working.indexOf(":") > -1)
         {
            if (working.substring(0, working.indexOf(":")) != "00")
               minute = new Integer(working.substring(0, working.indexOf(":")));
            working = working.substring(working.indexOf(":"));
            //second = working.substring(working.indexOf(":") +1);
         }
         else
         {
            // System.out.println("ReportParams: working:" + working + ":date to convert:" + dateToConvert);
            // In the case of 'selected start date', the start date is in the format that we display, so convert accordingly
            if (working.indexOf(" ") > -1)
            {
               if (working.substring(0, working.indexOf(" ")) != "00")
                  minute = new Integer(working.substring(0, working.indexOf(" ")));
               //second = 0;
               working = working.substring(working.indexOf(" ") + 1);
               if (working == "PM")
                  hour = hour + 12;
            }
            else
            {
               minute = 0;
               second = 0;
            }
         }
      }
      return new Date(new GregorianCalendar(year, month - 1, day, hour, minute, second).getTimeInMillis());
   }

   //	Converts a java date object to string format with zero padding, as required by the SQL
   public static String convertDateToString(Date dateToConvert)
   {
      DateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
      return f.format(dateToConvert);
   }

   //	Reset the hour to the end or begins of the day
   public static Date resetHours(Date dateToReset, int isStartDate)
   {
      int hour, minute, second;
      if (isStartDate == 1)
      {
         hour = 0;
         minute = 0;
         second = 0;
      }
      else
      {
         // If it is the end date, make hours 23, minutes and seconds 59
         hour = 23;
         minute = 59;
         second = 59;
      }
      GregorianCalendar cal = new GregorianCalendar();
      cal.setTime(dateToReset);
      cal.set(Calendar.HOUR_OF_DAY, hour);
      cal.set(Calendar.MINUTE, minute);
      cal.set(Calendar.SECOND, second);
      dateToReset.setTime(cal.getTimeInMillis());
      return dateToReset;
   }

}

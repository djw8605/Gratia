package net.sf.gratia.storage;

import java.util.*;

public class DupRecord
{
   private int dupid;
   private Date eventdate;
   private String rawxml;
   private String extraxml;
   private String source;
   private String error;
   private int dbid;
   private String RecordType;

   public DupRecord()
   {
   }

   public int getdupid()
   {
      return dupid;
   }

   public void setdupid(int value)
   {
      dupid = value;
   }

   public Date geteventdate()
   {
      return eventdate;
   }

   public void seteventdate(Date value)
   {
      eventdate = value;
   }

   public String getrawxml()
   {
      return rawxml;
   }

   public void setrawxml(String value)
   {
      rawxml = value;
   }


   public String getextraxml()
   {
      return extraxml;
   }

   public void setextraxml(String value)
   {
      extraxml = value;
   }

   public void setsource(String value)
   {
      source = value;
   }

   public String getsource()
   {
      return source;
   }

   public void seterror(String value)
   {
      error = value;
   }

   public String geterror()
   {
      return error;
   }

   public void setdbid(int value)
   {
      dbid = value;
   }

   public int getdbid()
   {
      return dbid;
   }

   public void setRecordType(String value)
   {
      RecordType = value;
   }

   public String getRecordType()
   {
      return RecordType;
   }
}

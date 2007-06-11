package net.sf.gratia.services;

import java.util.ArrayList;

import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Hashtable;

import java.text.*;

import java.io.*;

import net.sf.gratia.storage.*;


import org.hibernate.*;

public class ListenerThread extends Thread
{
   String ident = null;
   String directory = null;
   Hashtable global;

   //
   // database parameters
   //

   org.hibernate.Session session;
   Transaction tx;
   RecordUpdaterManager updater = new RecordUpdaterManager();
   RecordConverter converter = new RecordConverter();
   int itotal = 0;
   boolean duplicateCheck = false;
   Properties p;

   XP xp = new XP();

   StatusUpdater statusUpdater = null;
   NewProbeUpdate newProbeUpdate = null;
   NewVOUpdate newVOUpdate = null;

   Object lock;

   int dupdbid = 0;

   String historypath = "";

   //
   // various things used in the update loop
   //

   String file = "";
   String blob = "";
   String xml = "";
   String rawxml = "";
   String extraxml = "";
   String md5key = "";
   boolean gotreplication = false;
   boolean gothistory = false;
   boolean gotduplicate = false;
   boolean goterror = false;

   boolean stopflag = false;

   public ListenerThread(String ident,
                                    String directory,
                                    Object lock,
                                    Hashtable global)
   {      
      this.ident = ident;
      this.directory = directory;
      this.lock = lock;
      this.global = global;

      loadProperties();
      try
      {
         String url = p.getProperty("service.jms.url");
         Logging.log("");
         Logging.log("ListenerThread: " + ident + ":" + directory + ": Started");
         Logging.log("");
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      historypath = System.getProperties().getProperty("catalina.home") + "/gratia/data/";

      JobUsageRecordUpdater.AddDefaults(updater);
   }

   public void loadProperties()
   {
      p = Configuration.getProperties();
      String temp = p.getProperty("service.duplicate.check");
      if (temp.equals("1"))
         duplicateCheck = true;
      else
         duplicateCheck = false;
      Logging.log("ListenerThread: " + ident + ":Duplicate Check: " + duplicateCheck);
   }


   public void stopRequest()
   {
      stopflag = true;
      Logging.log("ListenerThread: " + ident + ":Stop Requested");
   }

   public boolean gotDuplicate(Record record) throws Exception
   {
      if (duplicateCheck == false)
         return false;

      String md5key = record.computemd5();
      record.setmd5(md5key);

      return gotDuplicate(record,md5key);
   }

   public boolean gotDuplicate(Record current, String md5key) throws Exception
   {
      if (duplicateCheck == false)
         return false;

      boolean status = false;
      String dq = "'";

      String table = current.getTableName();

      String sql = "SELECT dbid from "+table+"_Meta"+" where md5 = " + dq + md5key + dq;

      org.hibernate.Session session2 = HibernateWrapper.getSession();
      dupdbid = 0;

      try
      {
         List list = session2.createSQLQuery(sql).list();
         if (list.size() > 0)
         {
            status = true;
            Integer value = (Integer)list.get(0);
            dupdbid = value.intValue();
         }
      }
      catch (Exception e)
      {
         // Logging.log("ListenerThread: " + ident + ":Error During Dup Check");
         throw e;
      }
      finally
      {
         try
         {
            session2.close();
         }
         catch (Exception ignore)
         {
         }
      }

      return status;
   }

   public void run()
   {
      while (true)
      {
         if (stopflag)
         {
            Logging.log("ListenerThread: " + ident + ":Exiting");
            return;
         }

         if (!HibernateWrapper.databaseUp())
         {
            HibernateWrapper.start();
            if (HibernateWrapper.databaseDown)
            {
               Logging.log("ListenerThread: " + ident + ":Hibernate Down: Sleeping");
               try
               {
                  Thread.sleep(30 * 1000);
               }
               catch (Exception ignore)
               {
               }
               continue;
            }
         }
         if (stopflag)
         {
            Logging.log("ListenerThread: " + ident + ":Exiting");
            return;
         }
         loop();
         if (stopflag)
         {
            Logging.log("ListenerThread: " + ident + ":Exiting");
            return;
         }
         try
         {
            Thread.sleep(30 * 1000);
         }
         catch (Exception ignore)
         {
         }
      }
   }

   public void loop()
   {
      if (!HibernateWrapper.databaseUp())
         return;

      String files[] = xp.getFileList(directory);

      if (files.length == 0)
         return;

      statusUpdater = new StatusUpdater();
      newProbeUpdate = new NewProbeUpdate();
      newVOUpdate = new NewVOUpdate();

      for (int i = 0; i < files.length; i++)
      {
         global.put("listener", new java.util.Date());

         if (stopflag)
         {
            Logging.log("ListenerThread: " + ident + ":Exiting");
            return;
         }

         file = files[i];
         blob = xp.get(files[i]);
         xml = null;
         rawxml = null;
         extraxml = null;
         md5key = null;
         gotreplication = gothistory = gotduplicate = goterror = false;
         saveBlob();

         Record current = null;

         //
         // see if trace requested
         //

         if (p.getProperty("service.datapump.trace").equals("1"))
         {
            Logging.log("ListenerThread: " + ident + ":XML Trace:" + "\n\n" + blob + "\n\n");
         }

         //
         // see if we got a normal update or a replicated one
         //

         String historydate = null;

         try
         {
            if (blob.startsWith("replication"))
            {
               StringTokenizer st = new StringTokenizer(blob, "|");
               if (st.hasMoreTokens())
                  st.nextToken();
               if (st.hasMoreTokens())
                  xml = st.nextToken();
               if (st.hasMoreTokens())
                  rawxml = st.nextToken();
               if (st.hasMoreTokens())
                  extraxml = st.nextToken();
               gotreplication = true;
            }
            else if (blob.startsWith("history"))
            {
               StringTokenizer st = new StringTokenizer(blob, "|");
               if (st.hasMoreTokens())
                  st.nextToken();
               if (st.hasMoreTokens())
                  historydate = st.nextToken();
               if (st.hasMoreTokens())
                  xml = st.nextToken();
               if (st.hasMoreTokens())
                  rawxml = st.nextToken();
               if (st.hasMoreTokens())
                  extraxml = st.nextToken();
               gothistory = true;
            }
            else if (blob.startsWith("historymd5"))
            {
               StringTokenizer st = new StringTokenizer(blob, "|");
               if (st.hasMoreTokens())
                  st.nextToken();
               if (st.hasMoreTokens())
                  historydate = st.nextToken();
               if (st.hasMoreTokens())
                  xml = st.nextToken();
               if (st.hasMoreTokens())
                  md5key = st.nextToken();
               gothistory = true;
            }
            else
               xml = blob;
         }
         catch (Exception e)
         {
            Logging.log("ListenerThread: " + ident + ":Error:Processing File: " + file);
            Logging.log("ListenerThread: " + ident + ":Blob: " + blob);
            try
            {
               File temp = new File(file);
               temp.delete();
            }
            catch (Exception ignore)
            {
            }
            continue;
         }

         if (xml == null)
         {
            Logging.log("ListenerThread: " + ident + ":Error:No Data To Process: " + file);
            try
            {
               File temp = new File(file);
               temp.delete();
            }
            catch (Exception ignore)
            {
            }
            continue;
         }

         Logging.log("ListenerThread: " + ident + ":Processing: " + file);

         try
         {
            ArrayList records = new ArrayList();

            try
            {
               records = convert(xml);
            }
            catch (Exception e)
            {
               goterror = true;
               if (gotreplication)
                  saveParse("Replication", "Parse", xml);
               else if (gothistory)
                  saveParse("History", "Parse", xml);
               else
                  saveParse("Probe", "Parse", xml);
            }

            for (int j = 0; j < records.size(); j++)
            {
               // Logging.log("ListenerThread: " + ident + ":Before Begin Transaction");
               session = HibernateWrapper.getSession();
               tx = session.beginTransaction();
               // Logging.log("ListenerThread: " + ident + ":After Begin Transaction");

               current = (Record)records.get(j);
               statusUpdater.update(current, xml);

               //Logging.log("ListenerThread: " + ident + ":Before Duplicate Check");
               if (gothistory && (md5key != null))
                  gotduplicate = gotDuplicate(current,current.getmd5());
               else
                  gotduplicate = gotDuplicate(current);
               //Logging.log("ListenerThread: " + ident + ":After Duplicate Check");

               if (gotduplicate)
               {
                  goterror = true;
                  //Logging.log("ListenerThread: " + ident + ":Before Save Duplicate");
                  if (gotreplication)
                     saveDuplicate("Replication", "Duplicate", dupdbid, current);
                  else if (gothistory)
                     ;
                  else
                     saveDuplicate("Probe", "Duplicate", dupdbid, current);
                  //Logging.log("ListenerThread: " + ident + ":After Save Duplicate");
               }
               else
               {
                  // Logging.log("ListenerThread: " + ident + ":Before New Probe Update");
                  synchronized (lock)
                  {
                     newProbeUpdate.check(current);
                  }
                  synchronized (lock)
                  {
                     newVOUpdate.check(current);
                  }
                  // Logging.log("ListenerThread: " + ident + ":After New Probe Update");
                  updater.Update(current);
                  if (rawxml != null)
                     current.setRawXml(rawxml);
                  if (extraxml != null)
                     current.setExtraXml(extraxml);
                  Logging.log("ListenerThread: " + ident + ":Before Hibernate Save");
                  try
                  {
                     if (gothistory)
                     {
                        Date serverDate = new Date(Long.parseLong(historydate));
                        current.setServerDate(serverDate);
                     }
                     session.save(current);
                     //
                     // now - save history
                     //
                     if (!gothistory)
                     {
                        Date serverDate = current.getServerDate();
                        synchronized (lock)
                        {
                           Date now = new Date();
                           SimpleDateFormat format = new SimpleDateFormat("yyyyMMddkk");
                           String path = historypath + "history" + format.format(now);
                           File directory = new File(path);
                           if (!directory.exists())
                           {
                              directory.mkdir();
                           }
                           File historyfile = File.createTempFile("history", "xml", new File(path));
                           String filename = historyfile.getPath();
                           if (gotreplication && (extraxml != null))
                              xp.save(filename, "history" + "|" + serverDate.getTime() +
                                          "|" + xml + "|" + rawxml + "|" + extraxml);
                           else if (gotreplication)
                              xp.save(filename, "history" + "|" + serverDate.getTime() + "|" + xml + "|" + rawxml);
                           else
                              xp.save(filename, "historymd5" + "|" + serverDate.getTime() + "|" + xml + "|" + current.getmd5());
                        }
                     }
                  }
                  catch (Exception e)
                  {
                     goterror = true;
		     e.printStackTrace();
                     if (HibernateWrapper.databaseUp())
                     {
                        if (gotreplication)
                           saveSQL("Replication", "SQLError", current);
                        else
                           saveSQL("Probe", "SQLError", current);
                     }
                     else
                        throw e;
                  }

                  // Logging.log("ListenerThread: " + ident + ":After Hibernate Save");
               }
               // Logging.log("ListenerThread: " + ident + ":Before Transaction Commit");
               tx.commit();
               session.close();
               // Logging.log("ListenerThread: " + ident + ":After Transaction Commit");
            }
         }
         catch (Exception exception)
         {
            goterror = true;
            if (!HibernateWrapper.databaseUp())
            {
               Logging.log("ListenerThread: " + ident + ":Communications Error:Shutting Down");
               return;
            }
            Logging.log("");
            Logging.log("ListenerThread: " + ident + ":Error In Process: " + exception);
            Logging.log("ListenerThread: " + ident + ":Current: " + current);
         }
         // Logging.log("ListenerThread: " + ident + ":Before File Delete: " + file);
         try
         {
            File temp = new File(file);
            temp.delete();
         }
         catch (Exception ignore)
         {
            // Logging.log("ListenerThread: " + ident + ":File Delete Failed: " + file + " Error: " + ignore);
         }
         // Logging.log("ListenerThread: " + ident + ":After File Delete: " + file);
         itotal++;
         Logging.log("ListenerThread: " + ident + ":Total Records: " + itotal);
      }
   }

   public void saveBlob()
   {
      synchronized (lock)
      {
         try
         {
            Date now = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddkk");
            String path = historypath + "old" + format.format(now);
            File directory = new File(path);
            if (!directory.exists())
            {
               directory.mkdir();
            }
            File errorfile = File.createTempFile("old", "xml", new File(path));
            String filename = errorfile.getPath();
            xp.save(filename, blob);
         }
         catch (Exception ignore)
         {
         }
      }
   }

   public ArrayList convert(String xml) throws Exception
   {
      ArrayList records = null;

      try {
         records = converter.convert(xml);
      }
      catch (Exception e)
      {
         Logging.log("ListenerThread: " + ident + ":Parse error:  " + e.getMessage());
         Logging.log("ListenerThread: " + ident + ":XML:  " + "\n" + xml);
         throw e;
      }

      // The usage records array list is now populated with all the job usage records found in the given XML file
      //  return it to the caller.
      return records;
   }

   public void saveDuplicate(String source, String error, int dupdbid, Record current)
   {
      DupRecord record = new DupRecord();

      record.seteventdate(new java.util.Date());
      record.setrawxml(current.asXML());
      record.setsource(source);
      record.seterror(error);
      record.setdbid(dupdbid);
      record.setRecordType(current.getTableName());

      try
      {
         org.hibernate.Session session2 = HibernateWrapper.getSession();
         Transaction tx2 = session2.beginTransaction();
         session2.save(record);
         tx2.commit();
         session2.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   public void saveParse(String source, String error, String xml)
   {
      DupRecord record = new DupRecord();

      record.seteventdate(new java.util.Date());
      record.setrawxml(xml);
      record.setsource(source);
      record.seterror(error);

      try
      {
         org.hibernate.Session session2 = HibernateWrapper.getSession();
         Transaction tx2 = session2.beginTransaction();
         session2.save(record);
         tx2.commit();
         session2.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   public void saveSQL(String source, String error, Record current)
   {
      DupRecord record = new DupRecord();

      record.seteventdate(new java.util.Date());
      record.setrawxml(current.asXML());
      record.setsource(source);
      record.seterror(error);
      record.setRecordType(current.getTableName());

      try
      {
         org.hibernate.Session session2 = HibernateWrapper.getSession();
         Transaction tx2 = session2.beginTransaction();
         session2.save(record);
         tx2.commit();
         session2.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}

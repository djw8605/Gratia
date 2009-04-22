package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

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

//
// Note - this is a stripped down version of the real listener thread
// and is used for various performance testing scenarios
//

public class PerformanceThread extends Thread
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
   boolean gotreplication = false;
   boolean gothistory = false;
   boolean gotduplicate = false;
   boolean goterror = false;

   boolean stopflag = false;

   public PerformanceThread(String ident,
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
         Logging.log("PerformanceThread: " + ident + ":" + directory + ": Started");
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
      Logging.log("PerformanceThread: " + ident + ":Duplicate Check: " + duplicateCheck);
   }


   public void stopRequest()
   {
      stopflag = true;
      Logging.log("PerformanceThread: " + ident + ":Stop Requested");
   }

   public boolean gotDuplicate(Record record) throws Exception
   {
      boolean status = false;
      String dq = "'";

      if (duplicateCheck == false)
         return false;

      String md5key = record.computemd5(DatabaseMaintenance.UseJobUsageSiteName());
      record.setmd5(md5key);

      String table = record.getTableName();
      
      String sql = "SELECT dbid from "+table+"_Meta where md5 = " + dq + md5key + dq;

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
         // Logging.log("PerformanceThread: " + ident + ":Error During Dup Check");
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
            Logging.log("PerformanceThread: " + ident + ":Exiting");
            return;
         }

         if (!HibernateWrapper.databaseUp())
         {
             try {
                 HibernateWrapper.start();
             }
             catch (Exception e) { // Ignore
             }
            if (HibernateWrapper.databaseDown)
            {
               Logging.log("PerformanceThread: " + ident + ":Hibernate Down: Sleeping");
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
            Logging.log("PerformanceThread: " + ident + ":Exiting");
            return;
         }
         loop();
         if (stopflag)
         {
            Logging.log("PerformanceThread: " + ident + ":Exiting");
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

      for (int i = 0; i < files.length; i++)
      {
         global.put("listener", new java.util.Date());

         if (stopflag)
         {
            Logging.log("PerformanceThread: " + ident + ":Exiting");
            return;
         }

         file = files[i];
         blob = xp.get(files[i]);
         xml = null;
         rawxml = null;
         extraxml = null;
         gotreplication = gothistory = gotduplicate = goterror = false;
         saveBlob();

         Record current = null;

         //
         // see if trace requested
         //

         if (p.getProperty("service.datapump.trace").equals("1"))
         {
            Logging.log("PerformanceThread: " + ident + ":XML Trace:" + "\n\n" + blob + "\n\n");
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
            else
               xml = blob;
         }
         catch (Exception e)
         {
            Logging.log("PerformanceThread: " + ident + ":Error:Processing File: " + file);
            Logging.log("PerformanceThread: " + ident + ":Blob: " + blob);
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
            Logging.log("PerformanceThread: " + ident + ":Error:No Data To Process: " + file);
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

         Logging.log("PerformanceThread: " + ident + ":Processing: " + file);

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
               // Logging.log("PerformanceThread: " + ident + ":Before Begin Transaction");
               session = HibernateWrapper.getSession();
               tx = session.beginTransaction();

               // Logging.log("PerformanceThread: " + ident + ":After Begin Transaction");


               current = (Record)records.get(j);
               Probe probe = statusUpdater.update(session,current, xml);
               current.setProbe(probe);

               // Logging.log("PerformanceThread: " + ident + ":Before Duplicate Check");
               gotduplicate = gotDuplicate(current);
               // Logging.log("PerformanceThread: " + ident + ":After Duplicate Check");

               if (gotduplicate)
               {
                  // setDuplicate will increase the count (nRecords,nConnections,nDuplicates) for the probe
                  // and will return true if the duplicate needs to be recorded as a potential error.
                  if (current.setDuplicate(true)) {
                      goterror = true;
                      // Logging.log("PerformanceThread: " + ident + ":Before Save Duplicate");
                      if (gotreplication)
                          saveDuplicate("Replication", "Duplicate", dupdbid, current);
                      else if (gothistory)
                          ;
                      else
                          saveDuplicate("Probe", "Duplicate", dupdbid, current);
                      // Logging.log("PerformanceThread: " + ident + ":After Save Duplicate");
                  }
               }
               else
               {
                  // Logging.log("ListenerThread: " + ident + ":Before New Probe Update");
                  // setDuplicate will increase the count (nRecords,nConnections,nDuplicates) for the probe
                  current.setDuplicate(false);

                  // Logging.log("PerformanceThread: " + ident + ":Before New Probe Update");
                  // Logging.log("PerformanceThread: " + ident + ":After New Probe Update");
                  updater.Update(current);
                  synchronized (lock)
                  {
                      current.AttachContent(session);
                  }
                  if (rawxml != null)
                     current.setRawXml(rawxml);
                  if (extraxml != null)
                     current.setExtraXml(extraxml);
                  // Logging.log("PerformanceThread: " + ident + ":Before Hibernate Save");
                  try
                  {
                     if (gothistory)
                     {
                        Date serverDate = new Date(Long.parseLong(historydate));
                        current.setServerDate(serverDate);
                     }
                     /*
                        session.save(current);
                     */
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
                              xp.save(filename, "history" + "|" + serverDate.getTime() + "|" + xml);
                        }
                     }
                  }
                  catch (Exception e)
                  {
                     goterror = true;
                     if (HibernateWrapper.databaseUp())
                     {
                        /*
                           if (gotreplication)
                           saveSQL("Replication","SQLError",current);
                           else
                           saveSQL("Probe","SQLError",current);
                        */
                     }
                     else
                        throw e;
                  }

                  // Logging.log("PerformanceThread: " + ident + ":After Hibernate Save");
               }
               // Logging.log("PerformanceThread: " + ident + ":Before Transaction Commit");
               session.flush();
               tx.commit();
               session.close();
               // Logging.log("PerformanceThread: " + ident + ":After Transaction Commit");
            }
         }
         catch (Exception exception)
         {
            goterror = true;
            if (!HibernateWrapper.databaseUp())
            {
               Logging.log("PerformanceThread: " + ident + ":Communications Error:Shutting Down");
               return;
            }
            Logging.log("");
            Logging.log("PerformanceThread: " + ident + ":Error In Process: " + exception);
            Logging.log("PerformanceThread: " + ident + ":Current: " + current);
         }
         // Logging.log("PerformanceThread: " + ident + ":Before File Delete: " + file);
         try
         {
            File temp = new File(file);
            temp.delete();
         }
         catch (Exception ignore)
         {
            // Logging.log("PerformanceThread: " + ident + ":File Delete Failed: " + file + " Error: " + ignore);
         }
         // Logging.log("PerformanceThread: " + ident + ":After File Delete: " + file);
         itotal++;
         Logging.log("PerformanceThread: " + ident + ":Total Records: " + itotal);
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

      try
      {
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

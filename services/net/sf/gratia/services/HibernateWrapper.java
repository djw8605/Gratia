package net.sf.gratia.services;

import java.util.*;
import java.io.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.sql.*;
import net.sf.gratia.storage.*;

public class HibernateWrapper
{
   static Properties p;

   static org.hibernate.cfg.Configuration hibernateConfiguration;
   static org.hibernate.SessionFactory hibernateFactory;
   static org.hibernate.Session session;

   public static boolean databaseDown = true;


   public static synchronized void start()
   {
      if (systemDatabaseUp())
         return;

      p = net.sf.gratia.services.Configuration.getProperties();

      String configurationPath = net.sf.gratia.services.Configuration.getConfigurationPath();

      try
      {
         hibernateFactory.close();
      }
      catch (Exception ignore)
      {
      }

      try
      {
         hibernateConfiguration = new org.hibernate.cfg.Configuration();
         hibernateConfiguration.addFile(new File(net.sf.gratia.services.Configuration.getGratiaHbmPath()));
         hibernateConfiguration.addFile(new File(net.sf.gratia.services.Configuration.getJobUsagePath()));
         hibernateConfiguration.addFile(new File(net.sf.gratia.services.Configuration.getMetricRecordPath()));
         hibernateConfiguration.configure(new File(net.sf.gratia.services.Configuration.getHibernatePath()));

         Properties hp = new Properties();
         hp.setProperty("hibernate.connection.driver_class", p.getProperty("service.mysql.driver"));
         hp.setProperty("hibernate.connection.url", p.getProperty("service.mysql.url"));
         hp.setProperty("hibernate.connection.username", p.getProperty("service.mysql.user"));
         hp.setProperty("hibernate.connection.password", p.getProperty("service.mysql.password"));
         hibernateConfiguration.addProperties(hp);

         hibernateFactory = hibernateConfiguration.buildSessionFactory();

         Logging.log("");
         Logging.log("HibernateWrapper: Hibernate Services Started");
         Logging.log("");

         databaseDown = false;
      }
      catch (Exception databaseError)
      {
         Logging.log("");
         Logging.log("HibernateWrapper: Error Starting Hibernate");
         Logging.log("");
         databaseError.printStackTrace();
         databaseDown = true;
      }
   }

   public static boolean systemDatabaseUp()
   {
      int i = 0;

      try
      {
         org.hibernate.Session session = hibernateFactory.openSession();
         String command = "from JobUsageRecord where dbid = 1";
         List result = session.createQuery(command).list();
         for (i = 0; i < result.size(); i++)
         {
            JobUsageRecord record = (JobUsageRecord)result.get(i);
         }
         session.close();
         databaseDown = false;
         return true;
      }
      catch (Exception e)
      {
         databaseDown = true;
         return false;
      }
   }

   public static synchronized boolean databaseUp()
   {
      int i = 0;

      try
      {
         org.hibernate.Session session = hibernateFactory.openSession();
         String command = "from JobUsageRecord where dbid = 1";
         List result = session.createQuery(command).list();
         for (i = 0; i < result.size(); i++)
         {
            JobUsageRecord record = (JobUsageRecord)result.get(i);
         }
         session.close();
         databaseDown = false;
         return true;
      }
      catch (Exception e)
      {
         databaseDown = true;
         Logging.log("HibernateWrapper: Database Check: Database Down");
         e.printStackTrace();
         return false;
      }
   }

   public static synchronized org.hibernate.Session getSession()
   {
      int i = 0;

      try
      {
         org.hibernate.Session session = hibernateFactory.openSession();
         return session;
      }
      catch (Exception e)
      {
         databaseDown = true;
         Logging.log("HibernateWrapper: Get Session: Database Down");
         return null;
      }
   }
}

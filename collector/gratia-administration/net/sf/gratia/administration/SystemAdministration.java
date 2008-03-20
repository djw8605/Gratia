package net.sf.gratia.administration;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import net.sf.gratia.services.*;

import java.io.*;
import java.net.*;

import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;

import java.util.regex.*;
import java.text.*;

import java.rmi.*;

import org.apache.tools.bzip2.*;
import com.ice.tar.*;

public class SystemAdministration extends HttpServlet 
{
      XP xp = new XP();
      //
      // database related
      //
      String driver = "";
      String url = "";
      String user = "";
      String password = "";
      Connection connection;
      Statement statement;
      ResultSet resultSet;
      //
      // processing related
      //
      String html = "";
      String row = "";
      StringBuffer buffer = new StringBuffer();
      //
      // globals
      //
      HttpServletRequest request;
      HttpServletResponse response;
      boolean initialized = false;
      Properties props;
      Properties p;
      String message = null;
      //
      // support
      //
      String dq = "\"";
      String comma = ",";
      String cr = "\n";

      public JMSProxy proxy = null;

      //
      // statics for recovery thread
      //

      public static RecoveryService recoveryService = null;
      public static String status = "";
      public static long skipped = 0;
      public static long processed = 0;
      public static long errors = 0;
      public static boolean replayall = false;

    public void initialize()
      {
            p = net.sf.gratia.util.Configuration.getProperties();
            try
                  {
                        proxy = (JMSProxy) Naming.lookup(p.getProperty("service.rmi.rmilookup") +
                                                                         p.getProperty("service.rmi.service"));
                  }
            catch (Exception e)
                  {
                        Logging.warning(xp.parseException(e));
                  }
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
      {
            initialize();
            this.request = request;
            this.response = response;
            if (request.getParameter("action") != null)
                  {
                        if (request.getParameter("action").equals("replay"))
                              replay();
                        else if (request.getParameter("action").equals("replayAll"))
                              replayAll();
                        else if (request.getParameter("action").equals("stopDatabaseUpdateThreads"))
                              stopDatabaseUpdateThreads();
                        else if (request.getParameter("action").equals("startDatabaseUpdateThreads"))
                              startDatabaseUpdateThreads();
                  }
            setup();
            process();
            response.setContentType("text/html");
            response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0
            PrintWriter writer = response.getWriter();
            writer.write(html);
            writer.flush();
            writer.close();
      }

      public void setup()
      {
            html = xp.get(request.getRealPath("/") + "systemadministration.html");
      }

      public void process()
      {
            String status = "Active";

            html = xp.replaceAll(html,"#status#",SystemAdministration.status);
            html = xp.replaceAll(html,"#processed#","" + SystemAdministration.processed);
            html = xp.replaceAll(html,"#skipped#","" + SystemAdministration.skipped);

            try
                  {
                        boolean flag = proxy.databaseUpdateThreadsActive();
                        if (flag)
                              status = "Alive";
                        else
                              status = "Stopped";
                  }
            catch (Exception e)
                  {
                        e.printStackTrace();
                  }
            html = xp.replaceAll(html,"#threadstatus#",status);
      }

      public void replay()
      {
            if (SystemAdministration.recoveryService != null)
                  if (SystemAdministration.recoveryService.isAlive())
                        return;
            SystemAdministration.status = "Starting";
            SystemAdministration.skipped = 0;
            SystemAdministration.processed = 0;
            SystemAdministration.replayall = false;
            SystemAdministration.recoveryService = new RecoveryService();
            SystemAdministration.recoveryService.start();
      }

      public void replayAll()
      {
            if (SystemAdministration.recoveryService != null)
                  if (SystemAdministration.recoveryService.isAlive())
                        return;
            SystemAdministration.status = "Starting";
            SystemAdministration.skipped = 0;
            SystemAdministration.processed = 0;
            SystemAdministration.replayall = true;
            SystemAdministration.recoveryService = new RecoveryService();
            SystemAdministration.recoveryService.start();
      }

      public void stopDatabaseUpdateThreads()
      {
            try
                  {
                        proxy.stopDatabaseUpdateThreads();
                  }
            catch (Exception e)
                  {
                        e.printStackTrace();
                  }
      }

      public void startDatabaseUpdateThreads()
      {
            try
                  {
                        proxy.startDatabaseUpdateThreads();
                  }
            catch (Exception e)
                  {
                        e.printStackTrace();
                  }
      }

      public class RecoveryService extends Thread
      {
            public String driver;
            public String url;
            public String user;
            public String password;

            Connection connection;
            Statement statement;
            ResultSet resultSet;

            String command;

            Properties p;
      
            XP xp = new XP();

            Vector history = new Vector();
            String filenames[] = new String[0];
            java.util.Date databaseDate = null;

            int irecords = 0;

            public RecoveryService()
            {
                  System.out.println("RecoveryService: Starting");
                  SystemAdministration.status = "RecoveryService: Starting";

                  p = net.sf.gratia.util.Configuration.getProperties();
                  driver = p.getProperty("service.mysql.driver");
                  url = p.getProperty("service.mysql.url");
                  user = p.getProperty("service.mysql.user");
                  password = p.getProperty("service.mysql.password");
                  openConnection();
                  getDirectories();
                  getDatabaseDate();
            }

            public void openConnection()
            {
                  try
                        {
                              Class.forName(driver).newInstance();
                              connection = null;
                              connection = DriverManager.getConnection(url,user,password);
                        }
                  catch (Exception e)
                        {
                        }
            }

          public void getDirectories()
          {
              int i = 0;
              Vector vector = new Vector();
              String path = System.getProperties().getProperty("catalina.home") + "/gratia/data";
              path = xp.replaceAll(path,"\\","/");
              System.out.println("RecoveryService: Path: " + path);
              String temp[] = xp.getDirectoryList(path);
              for (i = 0; i < temp.length; i++)
                  if (temp[i].indexOf("history") > -1)
                      history.add(temp[i]);
              System.out.println("RecoveryService: Directories To Process: " + history.size());
          }

          public void getDatabaseDate()
          {
              long days = Long.parseLong(p.getProperty("maintain.history.log"));
              long now = (new java.util.Date()).getTime();
              databaseDate = new java.util.Date(now - (days * 24 * 60 * 1000));
              
              command = "select max(ServerDate) from JobUsageRecord_Meta";
              try
                  {
                      statement = connection.prepareStatement(command);
                      resultSet = statement.executeQuery(command);
                      while(resultSet.next())
                          {
                              Timestamp timestamp = resultSet.getTimestamp(1);
                              if (timestamp != null)
                                  databaseDate = new java.util.Date(timestamp.getTime());
                          }
                      resultSet.close();
                      statement.close();
                  }
              catch (Exception e)
                  {
                      e.printStackTrace();
                  }
              
              long temp = databaseDate.getTime() - (5 * 60 * 1000);
              databaseDate = new java.util.Date(temp);
              System.out.println("RecoveryService: Recovering From: " + databaseDate);
          }

          public void recoverRecord(String data, String filename) 
          {
              String connection = p.getProperty("service.open.connection");
              Post post = null;

              StringTokenizer st = new StringTokenizer(data,"|");
              st.nextToken();
              String timestamp = st.nextToken();
              java.util.Date recordDate = new java.util.Date(Long.parseLong(timestamp));
              
              if (SystemAdministration.replayall || recordDate.after(databaseDate))
                  {
                      post = new Post(connection + "/gratia-servlets/rmi","update",data);
                      try
                          {
                              irecords++;
                              post.send();
                              SystemAdministration.processed++;
                              System.out.println("RecoveryService: Sent: " + irecords + ":" + filename + " :Timestamp: " + recordDate);
                              SystemAdministration.status = "RecoveryService: Sent: " + irecords + ":" + filename + " :Timestamp: " + recordDate;
                          }
                      catch (Exception e)
                          {
                              System.out.println("RecoveryService: Error Sending: " + filename + " Error: " + e);
                              return;
                          }
                  }
              else                  
                  {
                      SystemAdministration.skipped++;
                      System.out.println("RecoveryService: Skipping: " + filename + " :Timestamp: " + recordDate);
                      SystemAdministration.status = "RecoveryService: Skipping: " + filename + " :Timestamp: " + recordDate;
                  }         
          }

          public void run()
          {
              String directory = "";
              System.out.println("RecoveryService: Started");
              for (int i = 0; i < history.size(); i++)
                  {
                      directory = (String) history.elementAt(i);
                      recover(directory);
                  }
              System.out.println("RecoveryService: Exiting");
              SystemAdministration.status = "Finished";
          }
          
          public void recoverArchiveEntry(TarInputStream tin, TarEntry tarEntry) throws java.io.IOException
          {
              if (tarEntry.isDirectory()) {

                  // Ignore directories
                      
              } else {
                  
                  System.out.println("RecoveryService: Processing Entry: " + tarEntry.getName());
                  //create a file with the same name as the tarEntry
                  int size=(int)tarEntry.getSize();
                  // -1 means unknown size. 
                  if (size==-1) {
                      size=1000; // ((Integer)htSizes.get(ze.getName())).intValue();
                  }
                  byte[] b=new byte[(int)size];
                  int rb=0;
                  int chunk=0;
                  while (((int)size - rb) > 0) {
                      chunk=tin.read(b,rb,(int)size - rb);
                      if (chunk==-1) {
                          break;
                      }
                      rb+=chunk;
                  }
                  String data = new String(b);
                  recoverRecord(data,tarEntry.getName());
              }
          } 

          public void recoverArchive(File archive)
          {
              System.out.println("RecoveryService: Processing Archive: " + archive.getAbsolutePath());
              try
                  {
                      FileInputStream fis = new FileInputStream(archive); 
                      System.out.println("RecoveryService: Processing Archive 2: " + archive.getName());
                      
                      byte skip[] = new byte[2];
                      fis.read(skip,0,2);
                     
                      CBZip2InputStream gzipInputStream = new CBZip2InputStream(fis);
                       
                      System.out.println("RecoveryService: Processing Archive 3: " + archive.getName());

                      // Byte skip[] = new Byte[2];
                      // gzipInputStream.read(skip,0,2);

                      TarInputStream tin = new TarInputStream( gzipInputStream );
                      
                      System.out.println("RecoveryService: Processing Archive 4: " + archive.getName());
                      
                      TarEntry tarEntry = tin.getNextEntry();
                      
                      System.out.println("RecoveryService: Processing Archive 5: " + archive.getName());

                      while (tarEntry != null)
                          {
                              recoverArchiveEntry(tin,tarEntry);
                              tarEntry = tin.getNextEntry();                             
                          }
                      
                      // Close the file and stream
                      tin.close();
                  } 
              catch (Exception e) 
                  {
                      System.out.println("recoverArchive: failed to processed file "+archive.getName()+"\nError: "+e.getMessage());
                      e.printStackTrace();
                  }
          }

          public void recover(String directory)
          {
              recoverDirectory(new File(directory));
          }

          public void recoverFile(File file) 
          {
              String blob = xp.get(file);
              recoverRecord(blob,file.getName());
          }

          public void recoverDirectory(File directory)
          {
              System.out.println("RecoveryService: Processing Directory: " + directory.getName());

              int i = 0;

              File filelist[] = directory.listFiles();

              for (i = 0; i < filelist.length; i++)
                  {
                      File current = filelist[i];
                      System.out.println("RecoveryService: sub-processing "+current.getName());
                      if (current.isDirectory()) {
                          recoverDirectory(current);
                      } else if (current.getName().endsWith(".tar.bz2")) {
                          recoverArchive(current);
                      } else {
                          recoverFile(current);
                      }
                  }
          }
      }
}

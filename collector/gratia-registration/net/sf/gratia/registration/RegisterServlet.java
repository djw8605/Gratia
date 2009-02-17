package net.sf.gratia.registration;

import net.sf.gratia.util.Logging;

import net.sf.gratia.util.Execute;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.XP;

import net.sf.gratia.services.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import java.rmi.*;

import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;

import java.sql.*;

public class RegisterServlet extends HttpServlet 
   {
      public Properties p;
      //public JMSProxy proxy = null;
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
      // other globals
      //
      String dq = "\"";
      String comma = ",";
      boolean autoregister = false;
      Properties props;
      
      public void init(ServletConfig config) throws ServletException 
      {
         super.init(config);
         
         System.err.println("Registration is starting");
         
         p = Configuration.getProperties();
         
         //
         // initialize logging
         //
         
         Logging.initialize("registration");
         Logging.warning("--RegisterServlet: I am here");
         try
         {
            props = Configuration.getProperties();
            driver = props.getProperty("service.mysql.driver");
            url = props.getProperty("service.mysql.url");
            user = props.getProperty("service.mysql.user");
            password = props.getProperty("service.mysql.password");
            if (props.getProperty("service.autoregister.pem") != null)
               if (props.getProperty("service.autoregister.pem").equals("1"))
                  autoregister = true;

            Logging.info("RegisterServlet: AutoRegister is " + (autoregister ? "enabled" : "disabled") );
            Logging.warning("--RegisterServlet: AutoRegister is " + (autoregister ? "enabled" : "disabled") );
         }
         catch (Exception ignore)
         {
         }
         try
         {
            Class.forName(driver).newInstance();
            connection = DriverManager.getConnection(url,user,password);
            Logging.info("Connection Opened");
         }
         catch (Exception e)
         {
            e.printStackTrace();
            return;
         }
         System.err.println("Registration is allsetup");
     }
      
      
      
      public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException 
      {
         String command = null;
         String from = null;
         String to = null;
         String rmi = null;
         String arg1 = null;
         String arg2 = null;
         String arg3 = null;
         String arg4 = null;
         String output = "";
         
         int argcount = 0;
         
         try
         {
            //proxy = (JMSProxy) Naming.lookup(p.getProperty("service.rmi.rmilookup") +
            //                                 p.getProperty("service.rmi.service"));
         }
         catch (Exception e)
         {
            Logging.warning(xp.parseException(e));
         }
         
         try 
         {
            command = req.getParameter("command");
            from = req.getParameter("from");
            to = req.getParameter("to");
            rmi = req.getParameter("rmi");
            arg1 = req.getParameter("arg1");
            arg2 = req.getParameter("arg2");
            arg3 = req.getParameter("arg3");
            arg4 = req.getParameter("arg4");
            
            if (command != null)
               command = command.toLowerCase();
            else
            {
               //
               // the following is a hack to get around a python post issue
               //
               ServletInputStream input = req.getInputStream();
               byte buffer[] = new byte[4 * 4096];
               int icount = 0;
               int istatus = 0;
               for (icount = 0; icount < buffer.length; icount++)
               {
                  istatus = input.read(buffer,icount,1);
                  if (istatus == -1)
                     break;
               }
               String body = new String(buffer,0,icount);
               StringTokenizer st1 = new StringTokenizer(body,"&");
               while(st1.hasMoreTokens())
               {
                  String token = st1.nextToken();
                  int index = token.indexOf("=");
                  String key = token.substring(0,index);
                  String value = token.substring(index + 1);
                  key = key.toLowerCase();
                  if (key.equals("command"))
                     command = value;
                  else if (key.equals("from"))
                     from = value;
                  else if (key.equals("to"))
                     to = value;
                  else if (key.equals("rmi"))
                     rmi = value;
                  else if (key.equals("arg1"))
                     arg1 = value;
                  else if (key.equals("arg2"))
                     arg2 = value;
                  else if (key.equals("arg3"))
                     arg3 = value;
                  else if (key.equals("arg4"))
                     arg4 = value;
               }
            }
            
            if (arg1 != null)
               argcount++;
            if (arg2 != null)
               argcount++;
            if (arg3 != null)
               argcount++;
            if (arg4 != null)
               argcount++;
            
            Logging.log("RegisterServlet: From: " + from);
            Logging.log("RegisterServlet: To: " + to);
            Logging.log("RegisterServlet: RMI: " + rmi);
            Logging.log("RegisterServlet: Command: " + command);
            Logging.log("RegisterServlet: Argcount: " + argcount);
            Logging.log("RegisterServlet: Arg1: " + arg1);
            Logging.log("RegisterServlet: Arg2: " + arg2);
            Logging.log("RegisterServlet: Arg3: " + arg3);
            Logging.log("RegisterServlet: Arg4: " + arg4);
            
            PrintWriter writer = res.getWriter();
            
            //
            // if registerprobe: arg1 = probename
            // if the probename doesn't exist in the security table add it and return a self signed cert
            //
            // otherwise return two error strings
            //
            
            if (props.getProperty("service.security.level").equals("0"))
            {
               writer.write("error:Security Not Supported");
               writer.flush();
               writer.close();
               return;
            }
            
            if ((command.equals("request")) && (argcount == 1))
            {
               arg1 = arg1 + ":" + req.getRemoteAddr();
               Logging.debug("Recieved Request Request");
               Logging.debug("Arg1: " + arg1);
               if (probeAlreadyRegistered(arg1))
               {
                  output = "error:Error - Already Registered";
                  writer.write(output);
                  writer.flush();
                  writer.close();
                  return;
               }
               output = requestCertificate(arg1);
               writer.write(output);
               writer.flush();
               writer.close();
               return;
            }
            else if ((command.equals("exchange")) && (argcount == 3))
            {
               arg1 = arg1 + ":" + req.getRemoteAddr();
               Logging.debug("Recieved Exchange Request");
               Logging.debug("Arg1: " + arg1);
               Logging.debug("Arg2: " + arg2);
               Logging.debug("Arg3: " + arg3);
               
               if (probeAlreadyRegistered(arg1))
               {
                  output = "error:Error - Already Registered";
                  writer.write(output);
                  writer.flush();
                  writer.close();
                  return;
               }
               output = exchangeCertificate(arg1,arg2,arg3);
               writer.write(output);
               writer.flush();
               writer.close();
               return;
            }
            else if ((command.equals("register")) && (argcount == 3))
            {
               arg1 = arg1 + ":" + req.getRemoteAddr();
               Logging.debug("Recieved Register Request");
               Logging.debug("Arg1: " + arg1);
               Logging.debug("Arg2: " + arg2);
               Logging.debug("Arg3: " + arg3);
               
               if (probeAlreadyRegistered(arg1))
               {
                  output = "error:Error - Already Registered";
                  writer.write(output);
                  writer.flush();
                  writer.close();
                  return;
               }
               output = registerCertificate(arg1,arg2,arg3);
               writer.write(output);
               writer.flush();
               writer.close();
               return;
            }
            else if (command.equals("get"))
            {
               Logging.debug("Recieved Get Request");
               Logging.debug("Arg1: " + arg1);
               Logging.debug("Arg2: " + arg2);
               Logging.debug("Arg3: " + arg3);
               
               output = get();
               writer.write(output);
               writer.flush();
               writer.close();
               return;
            }
            else if ((command.equals("put")) && (argcount == 3))
            {
               arg1 = arg1 + ":" + req.getRemoteAddr();
               Logging.debug("Recieved Put Request");
               Logging.debug("Arg1: " + arg1);
               Logging.debug("Arg2: " + arg2);
               Logging.debug("Arg3: " + arg3);
               
               if (probeAlreadyRegistered(arg1))
               {
                  output = "error:Error - Already Registered";
                  writer.write(output);
                  writer.flush();
                  writer.close();
                  return;
               }
               output = put(arg1,arg2,arg3);
               writer.write(output);
               writer.flush();
               writer.close();
               return;
            }
            else
            {
               Logging.info("RegisterServlet: Error: Unknown Command: " + 
                            command + " Or Invalid Arg Count: " + argcount);
               writer.write("Error: Unknown Command: " + command + " Or Invalid Arg Count: " + argcount);
            }
            writer.flush();
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
      
      public boolean probeAlreadyRegistered(String probename)
      {
         String command = "select count(*) from Security where alias = " + dq + probename + dq;
         int result = 0;
         try
         {
            statement = connection.prepareStatement(command);
            resultSet = statement.executeQuery(command);
            
            while(resultSet.next())
            {
               result = resultSet.getInt(1);
            }
            resultSet.close();
            statement.close();
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
         if (result == 0)
            return false;
         return true;
      }
      
      public String requestCertificate(String probename)
      {
         String dname = "cn=xxx, ou=Fermi-GridAccounting, o=Fermi, c=US";
         dname = xp.replace(dname,"xxx",probename);
         String keystore = Configuration.getConfigurationPath() + "/truststore";
         String tempstore = Configuration.getConfigurationPath() + "/tempstore";
         String  keycert = Configuration.getConfigurationPath() + "/key.cert";
         String output = "";
         //
         // first - build self certified certs and add to temp keystore
         //
         try
         {
            String command = "";
            try
            {
               File file = new File(tempstore);
               if (file.exists())
                  file.delete();
            }
            catch (Exception ignore)
            {
            }
            
            command = "keytool -genkey -dname " + dq + dname + dq + " -alias " + probename + 
            " -keypass server -keystore tempstore -storepass server";
            String command1[] =
            {"keytool",
               "-genkey",
               "-dname",
               dname,
               "-alias",
               probename,
               "-keypass",
               "server",
               "-keystore",
               tempstore,
               "-storepass",
            "server"};
            
            Execute.execute(command1);
            
            command = "keytool -selfcert -alias " + probename + " -keypass server" +
            " -keystore tempstore -storepass server";
            String command2[] =
            {"keytool",
               "-selfcert",
               "-alias",
               probename,
               "-keypass",
               "server",
               "-keystore",
               tempstore,
               "-storepass",
            "server"};
            Execute.execute(command2);
            
            command = "keytool -export -alias " + probename +
            " -file host.cert -keypass server -keystore tempstore -storepass server";
            String command3[] =
            {"keytool",
               "-export",
               "-alias",
               probename,
               "-file",
               "host.cert",
               "-keypass",
               "server",
               "-keystore",
               tempstore,
               "-storepass",
            "server"};
            Execute.execute(command3);
            
            Export export = new Export();
            export.export(tempstore,probename,"server",keycert);
            
            command = "openssl x509 -out gratia.hostcert.pem -outform pem -in host.cert -inform der";
            String command4[] =
            {"openssl",
               "x509",
               "-out",
               "gratia.hostcert.pem",
               "-outform",
               "pem",
               "-in",
               "host.cert",
               "-inform",
            "der"};
            Execute.execute(command4);
            
            command = "openssl pkcs8 -out gratia.hostkey.pem -in key.cert -inform pem -nocrypt";
            String command5[] =
            {"openssl",
               "pkcs8",
               "-out",
               "gratia.hostkey.pem",
               "-in",
               keycert,
               "-inform",
               "pem",
            "-nocrypt"};
            Execute.execute(command5);
            
            try
            {
               File file = new File(tempstore);
               if (file.exists())
                  file.delete();
            }
            catch (Exception ignore)
            {
            }
            
            String host = xp.get("gratia.hostcert.pem");
            String key = xp.get("gratia.hostkey.pem");
            return host + ":" + key;
         }
         catch (Exception e)
         {
            e.printStackTrace();
            return "error:" + xp.parseException(e);
         }
      }
      
      
      public String registerCertificate(String probename,String type,String pem)
      {
         String dname = "cn=xxx, ou=Fermi-GridAccounting, o=Fermi, c=US";
         dname = xp.replace(dname,"xxx",probename);
         String configurationPath = Configuration.getConfigurationPath();
         String keystore = configurationPath + "/truststore";
         String output = "";
         String active = "Inactive";
         String command = "";
         
         if (autoregister)
            active = "Active";
         
         //
         // save cert and import
         //
         try
         {
            xp.save(configurationPath + "/host.cert",pem);
            
            String command1[] =
            {"keytool",
               "-import",
               "-alias",
               probename,
               "-file",
               configurationPath + "/host.cert",
               "-keystore",
               keystore,
               "-storepass",
               "server",
               "-trustcacerts",
               "-noprompt",
               "-keypass",
            "server"};
            if (autoregister)
               Execute.execute(command1);
            
            command = "insert Security(source,alias,hostpem,state) values(" +
            dq + "Probe" + dq + comma +
            dq + probename + dq + comma +
            dq + pem + dq + comma + dq + active + dq + ")";
            statement = connection.createStatement();
            statement.executeUpdate(command);
            statement.close();
            // FlipSSL.flip();
            return "ok:ok";
         }
         catch (Exception e)
         {
            e.printStackTrace();
            return "error:" + xp.parseException(e);
         }
      }
      
      public String exchangeCertificate(String probename,String type,String pem)
      {
         String dname = "cn=xxx, ou=Fermi-GridAccounting, o=Fermi, c=US";
         dname = xp.replace(dname,"xxx",probename);
         String truststore = Configuration.getConfigurationPath() + "/truststore";
         String keystore = Configuration.getConfigurationPath() + "/keystore";
         String output = "";
         String active = "Inactive";
         
         if (autoregister)
            active = "Active";
         
         //
         // save cert and import
         //
         try
         {
            xp.save("host.cert",pem);
            
            String command = "keytool -import -alias " + probename +
            " -file host.cert -keystore " + truststore + 
            " -storepass server -trustcacerts -noprompt -keypass server";
            String command1[] =
            {"keytool",
               "-import",
               "-alias",
               probename,
               "-file",
               "host.cert",
               "-keystore",
               truststore,
               "-storepass",
               "server",
               "-trustcacerts",
               "-noprompt",
               "-keypass",
            "server"};
            if (autoregister)
               Execute.execute(command1);
            
            if (! probeAlreadyRegistered(probename))
            {
               command = "insert Security(source,alias,hostpem,state) values(" +
               dq + "Probe" + dq + comma +
               dq + probename + dq + comma +
               dq + pem + dq + comma + dq + active + dq + ")";
               
               statement = connection.createStatement();
               statement.executeUpdate(command);
               statement.close();
            }
            
            command = "keytool -export -alias server -file host.pem -keystore " + keystore +
            " -keypass server -storepass server";
            String command2[] =
            {"keytool",
               "-export",
               "-alias",
               "server",
               "-file",
               "host.pem",
               "-keystore",
               keystore,
               "-keypass",
               "server",
               "-storepass",
            "server"};
            Execute.execute(command2);
            
            command = "openssl x509 -out gratia.hostcert.pem -outform pem -in host.cert -inform der";
            String command3[] =
            {"openssl",
               "x509",
               "-out",
               "gratia.hostcert.pem",
               "-outform",
               "pem",
               "-in",
               "host.cert",
               "-inform",
            "der"};
            Execute.execute(command3);
            
            String mypem = xp.get("gratia.hostcert.pem");
            
            // FlipSSL.flip();
            
            return "ok:" + mypem + ":" + URLEncoder.encode(p.getProperty("service.secure.connection","UTF-8"));
         }
         catch (Exception e)
         {
            e.printStackTrace();
            return "error:" + xp.parseException(e);
         }
      }
      
      public String get()
      {
         String truststore = Configuration.getConfigurationPath() + "/truststore";
         String keystore = Configuration.getConfigurationPath() + "/keystore";
         String configurationPath = Configuration.getConfigurationPath();
         String output1 = configurationPath + "/get1";
         String output2 = configurationPath + "/get2";
         String output = "";
         //
         // save cert and import
         //
         try
         {
            String command2[] =
            {"keytool",
               "-export",
               "-alias",
               "server",
               "-file",
               output1,
               "-keystore",
               keystore,
               "-keypass",
               "server",
               "-storepass",
            "server"};
            Execute.execute(command2);
            
            String command3[] =
            {"openssl",
               "x509",
               "-out",
               output2,
               "-outform",
               "pem",
               "-in",
               output1,
               "-inform",
            "der"};
            Execute.execute(command3);
            
            String mypem = xp.get(output2);
            
            return "ok:" + 
            URLEncoder.encode(mypem,"UTF-8") + ":" + 
            URLEncoder.encode(p.getProperty("service.secure.connection","UTF-8"));
         }
         catch (Exception e)
         {
            e.printStackTrace();
            return "error:" + xp.parseException(e);
         }
      }
      
      public String put(String probename,String type,String pem)
      {
         String configurationPath = Configuration.getConfigurationPath();
         String keystore = configurationPath + "/truststore";
         String output1 = configurationPath + "/put1";
         String output2 = configurationPath + "/put2";
         String output = "";
         String active = "Inactive";
         String command = "";
         
         if (autoregister)
            active = "Active";
         
         //
         // save cert and import
         //
         try
         {
            pem = URLDecoder.decode(pem,"UTF-8");
            xp.save(output1,pem);
            
            String command1[] =
            {"openssl",
               "x509",
               "-out",
               output2,
               "-outform",
               "der",
               "-in",
               output1,
               "-inform",
            "pem"};
            Execute.execute(command1);
            
            String command2[] =
            {"keytool",
               "-import",
               "-alias",
               probename,
               "-file",
               output2,
               "-keystore",
               keystore,
               "-storepass",
               "server",
               "-trustcacerts",
               "-noprompt",
               "-keypass",
            "server"};
            
            if (autoregister)
               Execute.execute(command2);
            
            command = "insert Security(source,alias,hostpem,state) values(" +
            dq + type + dq + comma +
            dq + probename + dq + comma +
            dq + pem + dq + comma + dq + active + dq + ")";
            statement = connection.createStatement();
            statement.executeUpdate(command);
            statement.close();
            // FlipSSL.flip();
            return "ok:ok";
         }
         catch (Exception e)
         {
            e.printStackTrace();
            return "error:" + xp.parseException(e);
         }
      }
   }

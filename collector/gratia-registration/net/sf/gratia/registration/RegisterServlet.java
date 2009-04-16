package net.sf.gratia.registration;

import net.sf.gratia.util.Logging;

import net.sf.gratia.util.Execute;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.XP;
import net.sf.gratia.util.Base64;

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
      
      JMSProxy fCollectorProxy = null;
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
      
      // Cache configuration info
      String fSecureUrl;
      
      
      // This is a common routine, it should be 'shared'
      private synchronized boolean lookupProxy() throws java.rmi.UnmarshalException {
         int counter = 0;
         final int maxloop = 20;
         
         while (fCollectorProxy == null && counter < 20 ) {
            try {
               fCollectorProxy = (JMSProxy) Naming.lookup(p.getProperty("service.rmi.rmilookup") +
                                                          p.getProperty("service.rmi.service"));
            }
            catch (java.rmi.UnmarshalException e) {
               Logging.warning("RMIHandlerServlet caught exception doing RMI lookup: ", e);
               // We will not recover from this error, so let's give up now.
               throw e;
            }
            catch (Exception e) {
               Logging.warning("RMIHandlerServlet caught exception doing RMI lookup: ", e);
               try {
                  Thread.sleep(5000);
               } catch (Exception ignore) {
               }
            }
            counter = counter + 1;
         }
         return (fCollectorProxy != null);
      }
      
      public void init(ServletConfig config) throws ServletException 
      {
         super.init(config);
         
         p = Configuration.getProperties();
         
         //
         // initialize logging
         //
         
         Logging.initialize("registration");
         Logging.info("RegisterServlet is started");
         try
         {
            props = Configuration.getProperties();
            fSecureUrl = props.getProperty("service.secure.connection");

            driver = props.getProperty("service.mysql.driver");
            url = props.getProperty("service.mysql.url");
            user = props.getProperty("service.mysql.user");
            password = props.getProperty("service.mysql.password");
            if (props.getProperty("service.autoregister.pem") != null)
               if (props.getProperty("service.autoregister.pem").equals("1"))
                  autoregister = true;

            Logging.info("RegisterServlet: AutoRegister is " + (autoregister ? "enabled" : "disabled") );
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
     }
      
      
      
      public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException 
      {
         
         try {
            lookupProxy();
         } catch (Exception e) {
            // lookupProxy already insert the error in the logs.
            PrintWriter qwriter = res.getWriter();
            qwriter.write("Error: service is not functional.  Please report the issue to the system adminsitrator.");               
            qwriter.flush();
            return;
         }
            
         if ( fCollectorProxy == null) {
            PrintWriter qwriter = res.getWriter();
            qwriter.write("Error: service not ready.");
            qwriter.flush();
            return;
         } else if (!fCollectorProxy.servletEnabled()) {
            PrintWriter qwriter = res.getWriter();
            qwriter.write("Error: service currently disabled.");
            qwriter.flush();
            return;
         }
                  
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
            
            if (command != null) {
               command = command.toLowerCase();
            }
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
            
            if (command==null) {
               Logging.info("RegisterServlet: Error: no Command");
               writer.write("Error: no command");
               
            } 
            else if (command.equals("getsecureurl")) 
            {
               if (argcount == 1) {
                  // The first argument is the pem of the certificate, let's check it (this will 'register' it).
                  String origin = null;
                  try {
                     origin = fCollectorProxy.checkConnection(arg1, req.getRemoteAddr(), from);
                  } catch (net.sf.gratia.services.AccessException e) {
                     
                     Logging.info("Rejected the certificate(s)");                  
                     Logging.debug("Exception detail:", e);
                     writer.write("Error: Certificate rejected by the Gratia Collector.");
                     writer.flush();
                     return;
                     
                  } catch (Exception e) {
                     Logging.warning("Proxy communication failure: " + e);
                     Logging.warning("Error: For req: " +
                                     req +
                                     ", originating server: " +
                                     req.getRemoteHost());
                     Logging.debug("RMIHandlerServlet error diagnostic for req: " +
                                   req +
                                   ", headers: \n" + requestDiagnostics(req));
                     Logging.debug("Exception detail:", e);
                     writer.write("Error: issue during certificate check"+ xp.parseException(e));
                     writer.flush();
                     return;
                  } 
                  if (origin != null && origin.length() > 0) {
                     Logging.debug("Crudentials accepted.");
                  } else {
                     Logging.info("rejected the certificate(s)");
                     writer.write("Error: The certificate has been rejected by the Gratia Collector!");
                     writer.flush();
                     // If we can't check the validity of the certificate, we quit.
                     return;
                  }
               }
               Logging.debug("Received request for the secure connection string");
               Logging.debug("Arg1: " + arg1);
               writer.write("secureconnection:"+URLEncoder.encode(fSecureUrl));
               writer.flush();
               writer.close();
               Logging.info("Registered: "+from);
               return;
            } 
            else if ((command.equals("request")) && (argcount == 1))
            {
               Logging.debug("Received Request Request");
               Logging.debug("Arg1: " + arg1);

               output = requestCertificate(writer,from,req.getRemoteAddr());
               writer.write(output);
               writer.flush();
               writer.close();
               return;
            }
            else if ((command.equals("exchange")) && (argcount == 3))
            {
               String alias = arg1 + ":" + req.getRemoteAddr();
               Logging.debug("Received Exchange Request");
               Logging.debug("Arg1: " + arg1);
               Logging.debug("Arg2: " + arg2);
               Logging.debug("Arg3: " + arg3);

               output = exchangeCertificate(alias,from,req.getRemoteAddr(),arg1);
               writer.write(output);
               writer.flush();
               writer.close();
               return;
            }
            else if ((command.equals("register")) && (argcount == 3))
            {
               Logging.debug("Received Register Request");
               Logging.debug("Arg1: " + arg1);
               Logging.debug("Arg2: " + arg2);
               Logging.debug("Arg3: " + arg3);
               
               output = registerCertificate(from,req.getRemoteAddr(),arg1);
               writer.write(output);
               writer.flush();
               writer.close();
               return;
            }
            else if (command.equals("getpublickey"))
            {
               Logging.debug("Received Get Request");
               Logging.debug("Arg1: " + arg1);
               Logging.debug("Arg2: " + arg2);
               Logging.debug("Arg3: " + arg3);
               
               output = getCollectorPublicKey();
               writer.write(output);
               writer.flush();
               writer.close();
               return;
            }
            else if ((command.equals("putpublickey")) && (argcount == 1))
            {
               String alias = arg1 + ":" + req.getRemoteAddr();
               Logging.debug("Received Put Request");
               Logging.debug("Arg1: " + arg1);
               Logging.debug("Arg2: " + arg2);
               Logging.debug("Arg3: " + arg3);
               
               output = putPublicKey(alias,from,req.getRemoteAddr(),arg1);
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
      
      public String requestCertificate(PrintWriter writer, String from, String remoteAddr)
      {
         String dname = "cn=xxx, ou=GratiaAccounting, o=Gratia, c=US";
         String keystore = Configuration.getConfigurationPath() + "/keystore";
         dname = xp.replace(dname,"xxx",from);
         String alias = from + ":" + remoteAddr;
         String output = "";

         // 
         // first - build self certified certs and add to temp keystore
         //
         try
         {
            // First let's make sure the incoming probe is allowed:
            checkConnection(remoteAddr,from);
            
            String command = "keytool -genkey -dname " + dq + dname + dq + " -alias " + alias + 
                      " -keypass server -keystore " + keystore + " -storepass server";
            String command1[] =
            {"keytool",
               "-genkey",
               "-dname",
               dname,
               "-alias",
               alias,
               "-keypass",
               "server",
               "-keystore",
               keystore,
               "-storepass",
            "server"};
            
            Execute.execute(command1);
            
            command = "keytool -selfcert -dname " + dq + dname + dq + " -alias " + alias + 
            " -keypass server -keystore " + keystore + " -storepass server";
            String command2[] =
            {"keytool",
               "-selfcert",
               "-dname",
               dname,
               "-alias",
               alias,
               "-keypass",
               "server",
               "-keystore",
               keystore,
               "-storepass",
               "server"};
            //Execute.execute(command2);
           
 
            // If it is stored in the keystore:         
            java.security.KeyStore ks = java.security.KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(keystore),"server".toCharArray());
            
            java.security.Key key = ks.getKey(alias,"server".toCharArray());
            //java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            //key = kf.translateKey(key);
            //This should have led to key.getFormat().equals("RSA") 
            //but instead it is still PKCS#8

            String keypemfile = "/tmp/key.pem"; // need to add pid
            String keypkcs8 = "/tmp/keypcks8";
            StringBuffer buffer = new StringBuffer();
            buffer.append("-----BEGIN PRIVATE KEY-----" + "\n");
            buffer.append(Base64.encodeBytes(key.getEncoded()) + "\n");
            buffer.append("-----END PRIVATE KEY-----" + "\n");
            xp.save(keypkcs8,buffer.toString());

            command = "openssl pkcs8 -out "+keypemfile+" -in "+keypkcs8+" -inform pem -nocrypt";
            String command5[] =
            {"openssl",
               "pkcs8",
               "-out",
               keypemfile, 
               "-in", 
               keypkcs8,
               "-inform",
               "pem",
            "-nocrypt"};
            Execute.execute(command5);
 
            //String keypem = "-----BEGIN RSA PRIVATE KEY-----\n"+Base64.encodeBytes(key.getEncoded())+"\n-----END RSA PRIVATE KEY-----\n";
            String certpem = new String("-----BEGIN CERTIFICATE-----\n" + Base64.encodeBytes(ks.getCertificate(alias) .getEncoded()) + "\n-----END CERTIFICATE-----\n");
            String keypem = xp.get(keypemfile);
            new File(keypemfile).delete();
            new File(keypkcs8).delete();
            return "ok:" + certpem + ":" + keypem;
         }
         catch (net.sf.gratia.services.AccessException e) {
            return "error:"+e.getMessage();
         }
         catch (Exception e)
         {
            e.printStackTrace();
            return "error:" + xp.parseException(e);
         }
      }
      
      
      public String registerCertificate(String from, String remoteAddr, String pem)
      {
         String origin = null;
         try {
            origin = fCollectorProxy.checkConnection(pem, remoteAddr, from);
         } catch (net.sf.gratia.services.AccessException e) {
            
            Logging.info("Rejected the certificate(s)");                  
            Logging.debug("Exception detail:", e);
            return "Error: Certificate rejected by the Gratia Collector. " +  xp.parseException(e);
            
         } catch (Exception e) {
            Logging.warning("Proxy communication failure: ",e);
            return "Error: issue during certificate check"+ xp.parseException(e);
         }
         return "ok:ok";
      }
      
      public String exchangeCertificate(String alias,String from,String remoteAddr,String pem)
      {
         String putoutput = putPublicKey(alias,from,remoteAddr,pem);
         if (!putoutput.equals("ok:ok")) {
            return putoutput;
         } else {
            return getCollectorPublicKey();
         }
      }
      
      public String getCollectorPublicKey()
      {
         // Return this server public key.
         
         String certfile =  net.sf.gratia.util.Configuration.getProperties().getProperty("service.vdt.cert.file");
         String collectorPem = net.sf.gratia.util.XP.get(certfile);
         
         String mypem = collectorPem;
         
         //
         // save cert and import
         //
         if (mypem == null || mypem.length()==0) {
            try
            {
               // If it is stored in the keystore:         
               String keystore = Configuration.getConfigurationPath() + "/keystore";
               java.security.KeyStore ks = java.security.KeyStore.getInstance("JKS");
               ks.load(new FileInputStream(keystore),"server".toCharArray());
               
               java.security.Key key = ks.getKey("server","server".toCharArray());
               mypem = "-----BEGIN RSA PRIVATE KEY-----\n"+Base64.encodeBytes(key.getEncoded())+"\n-----END RSA PRIVATE KEY-----\n";
               
            }
            catch (Exception e)
            {
               Logging.warning("Error during private key retrieval",e);
               return "error:" + xp.parseException(e);
            }
         }
         try
         {
            return "new:"+collectorPem+"\n"+"ok:" + 
                   URLEncoder.encode(mypem,"UTF-8") + ":" + 
                   URLEncoder.encode(p.getProperty("service.secure.connection","UTF-8"));
         }
         catch (Exception e)
         {
            Logging.warning("Error during pem encoding",e);
            return "error:" + xp.parseException(e);
         }
      }
      
      public String putPublicKey(String alias, String from, String remoteAddr, String pem)
      {
         String origin = null;
         try {
            origin = fCollectorProxy.checkConnection(pem, remoteAddr, from);
         } catch (net.sf.gratia.services.AccessException e) {
            
            Logging.info("Rejected the certificate(s)");                  
            Logging.debug("Exception detail:", e);
            return "Error: Certificate rejected by the Gratia Collector. " +  xp.parseException(e);
            
         } catch (Exception e) {
            Logging.warning("Proxy communication failure: ",e);
           return "Error: issue during certificate check"+ xp.parseException(e);
         }
         
         try
         {
            // If it is stored in the keystore:         
            String keystore = Configuration.getConfigurationPath() + "/keystore";
            java.security.KeyStore ks = java.security.KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(keystore),"server".toCharArray());

            // Remove leading and trailing lines.
            String keyPem = pem.substring(pem.indexOf('\n')+1,
                                          pem.lastIndexOf('\n',pem.length()-2));
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            java.security.Key key = keyFactory.generatePublic(new java.security.spec.PKCS8EncodedKeySpec(net.sf.gratia.util.Base64.decode(keyPem)));
            
            ks.setKeyEntry(alias,key,"server".toCharArray(),null);

            ks.store(new FileOutputStream(keystore),"server".toCharArray());
            
         }
         catch (Exception e)
         {
            Logging.warning("Error during key storage",e);
            return "error:" + xp.parseException(e);
         }
         

         return "ok:ok";
      }
      
      public void checkConnection(String remoteAddr, String sender) throws Exception {
         
         // First let's make sure the connection is 'allowed'
         String origin = null;
         try {
            java.security.cert.X509Certificate certs[] = null;
            origin = fCollectorProxy.checkConnection(certs, remoteAddr, sender);
         } catch (net.sf.gratia.services.AccessException e) {
            
            Logging.info("Rejected the connection.");                  
            Logging.debug("Exception detail:", e);
            throw new net.sf.gratia.services.AccessException("Connection rejected by the Gratia Collector.");
            
         } catch (Exception e) {
            Logging.warning("Proxy communication failure: " + e);
            Logging.warning("Error: originating server: " + remoteAddr);
            Logging.debug("Exception detail:", e);
            throw new Exception("Error: issue during certificate check"+ xp.parseException(e));
         } 
         if (origin != null && origin.length() > 0) {
            Logging.debug("Crudentials accepted.");
         } else {
            Logging.info("rejected the connection.");
            throw new Exception("The connection has been rejected by the Gratia Collector!");
         }
      }         

      // Actually same as in RMIhandlerServlet.
      private String requestDiagnostics(HttpServletRequest req) {
         Enumeration hNameList = req.getHeaderNames();
         String hList = new String("");
         while (hNameList.hasMoreElements()) {
            if (hList.length() != 0) {
               hList += ", ";
            }
            String hName = (String) hNameList.nextElement();
            hList += hName + ": " + req.getHeader(hName);
         }
         return hList;
      }
      
   }

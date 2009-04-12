package net.sf.gratia.services;

import net.sf.gratia.util.Logging;

import java.io.*;
import java.net.*;

import java.security.*;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

public class Post
   {
      public StringBuffer buffer = new StringBuffer();
      public String destination;
      public Boolean success;
      public String errorMsg;
      public Exception exception;
      
      public Post(String destination,String command)
      {
         this.destination = destination;
         success = true;
         try
         {
            buffer.append(URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command,"UTF-8"));
         }
         catch (Exception e)
         {
            success = false;
            errorMsg = e.toString();
            exception = e;
         }
      }
      
      public Post(String destination,String command,String arg1)
      {
         this.destination = destination;
         success = true;
         try
         {
            buffer.append(URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("from", "UTF-8") + "=" + URLEncoder.encode(net.sf.gratia.services.CollectorService.getName(),"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg1", "UTF-8") + "=" + URLEncoder.encode(arg1,"UTF-8"));
         }
         catch (Exception e)
         {
            success = false;
            errorMsg = e.toString();
            exception = e;
         }
      }
      
      public Post(String destination,String command,String arg1,String arg2)
      {
         this.destination = destination;
         success = true;
         try
         {
            buffer.append(URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("from", "UTF-8") + "=" + URLEncoder.encode(net.sf.gratia.services.CollectorService.getName(),"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg1", "UTF-8") + "=" + URLEncoder.encode(arg1,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg2", "UTF-8") + "=" + URLEncoder.encode(arg2,"UTF-8"));
         }
         catch (Exception e)
         {
            success = false;
            errorMsg = e.toString();
            exception = e;
         }
      }
      
      public Post(String destination,String command,String arg1,String arg2,String arg3)
      {
         this.destination = destination;
         success = true;
         try
         {
            buffer.append(URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("from", "UTF-8") + "=" + URLEncoder.encode(net.sf.gratia.services.CollectorService.getName(),"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg1", "UTF-8") + "=" + URLEncoder.encode(arg1,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg2", "UTF-8") + "=" + URLEncoder.encode(arg2,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg3", "UTF-8") + "=" + URLEncoder.encode(arg3,"UTF-8"));
         }
         catch (Exception e)
         {
            success = false;
            errorMsg = e.toString();
            exception = e;
         }
      }
      
      public Post(String destination,String command,String arg1,String arg2,String arg3,String arg4)
      {
         this.destination = destination;
         success = true;
         try
         {
            buffer.append(URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("from", "UTF-8") + "=" + URLEncoder.encode(net.sf.gratia.services.CollectorService.getName(),"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg1", "UTF-8") + "=" + URLEncoder.encode(arg1,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg2", "UTF-8") + "=" + URLEncoder.encode(arg2,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg3", "UTF-8") + "=" + URLEncoder.encode(arg3,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg4", "UTF-8") + "=" + URLEncoder.encode(arg4,"UTF-8"));
         }
         catch (Exception e)
         {
            success = false;
            errorMsg = e.toString();
            exception = e;
         }
      }
      
      public Post(String destination,String command,String arg1,String arg2,String arg3,String arg4,String arg5)
      {
         this.destination = destination;
         success = true;
         try
         {
            buffer.append(URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("from", "UTF-8") + "=" + URLEncoder.encode(net.sf.gratia.services.CollectorService.getName(),"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg1", "UTF-8") + "=" + URLEncoder.encode(arg1,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg2", "UTF-8") + "=" + URLEncoder.encode(arg2,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg3", "UTF-8") + "=" + URLEncoder.encode(arg3,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg4", "UTF-8") + "=" + URLEncoder.encode(arg4,"UTF-8"));
            buffer.append("&");
            buffer.append(URLEncoder.encode("arg5", "UTF-8") + "=" + URLEncoder.encode(arg5,"UTF-8"));
         }
         catch (Exception e)
         {
            success = false;
            errorMsg = e.toString();
            exception = e;
         }
      }
      
      public void add(String key,String value)
      {
         if (!success) return;
         
         success = true;
         try
         {
            buffer.append("&");
            buffer.append(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value,"UTF-8"));
         }
         catch (Exception e)
         {
            success = false;
            errorMsg = e.toString();
            exception = e;
         }
      }
      
      class CustomTrustManager implements X509TrustManager {
         
         private KeyStore fKeyStore;

         /*
          * The default PKIX X509TrustManager9.  We'll delegate
          * decisions to it, and fall back to the logic in this class if the
          * default X509TrustManager doesn't trust it.
          */
         X509TrustManager pkixTrustManager;
         String fName;
         
         CustomTrustManager(String name) throws Exception {
            // create a "default" JSSE X509TrustManager.
            
            fName = name;
            
            fKeyStore = KeyStore.getInstance("JKS");
            fKeyStore.load(new FileInputStream(net.sf.gratia.util.Configuration.getConfigurationPath() + "/truststore"),
                           "server".toCharArray());
            
            TrustManagerFactory tmf =
            TrustManagerFactory.getInstance("PKIX");
            tmf.init(fKeyStore);
            
            TrustManager tms [] = tmf.getTrustManagers();
            
            /*
             * Iterate over the returned trustmanagers, look
             * for an instance of X509TrustManager.  If found,
             * use that as our "default" trust manager.
             */
            for (int i = 0; i < tms.length; i++) {
               if (tms[i] instanceof X509TrustManager) {
                  pkixTrustManager = (X509TrustManager) tms[i];
                  return;
               }
            }
            
            /*
             * Find some other way to initialize, or else we have to fail the
             * constructor.
             */
            throw new Exception("Couldn't initialize");
         }
         
         
         private void AddChain(String aliasPrefix, X509Certificate[] chain) 
         throws java.security.KeyStoreException, java.io.FileNotFoundException, java.io.IOException, java.security.NoSuchAlgorithmException, java.security.cert.CertificateException {
            
            int index = 0;
            while( fKeyStore.containsAlias(aliasPrefix + "-" + index) ) {
               index = index + 1;
            }
            fKeyStore.setCertificateEntry( aliasPrefix + "-" + index, chain[0] );
            fKeyStore.store(new FileOutputStream(net.sf.gratia.util.Configuration.getConfigurationPath() + "/truststore"),
                           "server".toCharArray());
         }
         
         /*
          * Delegate to the default trust manager.
          */
         public void checkClientTrusted(X509Certificate[] chain, String authType)
         throws CertificateException {
            Logging.debug("checkClient:"+authType+" "+chain);
            try {
               pkixTrustManager.checkClientTrusted(chain, authType);
               return;
            } catch (CertificateException excep) {
               // do any special handling here, or rethrow exception.
               Logging.info("checkClient certificate probably not found in truststore");
            } catch(java.lang.RuntimeException re) {
               if (re.getCause() instanceof java.security.InvalidAlgorithmParameterException) {
                  // No certificate registered at all.
               } else {
                  Logging.warning("checkClient unexpected runtime exception:",re);
               }               
            } catch (Exception e) {
               if (e instanceof java.security.InvalidAlgorithmParameterException) {
                  // No certificate registered at all.
               } else {
                  Logging.warning("checkClient unexpected exception:",e);
               }
            }
            try {
               AddChain(net.sf.gratia.services.CollectorService.getName(),chain);
            } catch (Exception e) {
               Logging.warning("checkClientTrusted had a problem when storing a key",e);
            }
         }
         
         /*
          * Delegate to the default trust manager.
          */
         public void checkServerTrusted(X509Certificate[] chain, String authType)
         throws CertificateException {
            Logging.debug("checkServer:"+authType+" "+chain[0]);
            try {
               pkixTrustManager.checkServerTrusted(chain, authType);
               return;
               
            } catch (CertificateException excep) {
               /*
                * Possibly pop up a dialog box asking whether to trust the
                * cert chain.
                */
               Logging.info("checkServer certificate probably not found in truststore.");
            } catch(java.lang.RuntimeException re) {
               if (re.getCause() instanceof java.security.InvalidAlgorithmParameterException) {
                  // No certificate registered at all.
               } else {
                  Logging.warning("checkClient unexpected runtime exception:",re);
               }               
            } catch (Exception e) {
               if (e instanceof java.security.InvalidAlgorithmParameterException) {
                  // No certificate registered at all.
               } else {
                  Logging.warning("checkClient unexpected exception:",e);
               }
            }
            try {
               AddChain(fName,chain);
            } catch (Exception e) {
               Logging.warning("checkClientTrusted had a problem when storing a key",e);
            }
         }
         
         /*
          * Merely pass this through.
          */
         public X509Certificate[] getAcceptedIssuers() {
            //Logging.warning("getAccptedIssuers");
            return pkixTrustManager.getAcceptedIssuers();
         }
      }
      
      public final class CustomKeyManager 
         extends javax.net.ssl.X509ExtendedKeyManager
         {
            private java.util.Properties props = null;
            private java.security.KeyStore ks = null;
            private javax.net.ssl.X509KeyManager km = null;
            private java.util.Properties sslConfig = null;
            private String clientAlias = null;
            private String serverAlias = null;
            private int clientslotnum = 0;
            private int serverslotnum = 0;
            private String fCollectorPem;
            private net.sf.gratia.storage.Certificate fCertificate;
            private String fCollectorKeyPem;
            private PrivateKey fPrivateKey;
           
            public CustomKeyManager()  throws Exception 
            {
               ks = KeyStore.getInstance("JKS");
               ks.load(new FileInputStream(net.sf.gratia.util.Configuration.getConfigurationPath() + "/keystore"),
                       "server".toCharArray());
               
               KeyManagerFactory kmf =
               KeyManagerFactory.getInstance("SunX509", "SunJSSE");
               char passwd[] = { 's','e','r','v','e','r' };
               kmf.init(ks,passwd);
               
               km = (X509KeyManager) kmf.getKeyManagers()[0];
               
               String certfile =  net.sf.gratia.util.Configuration.getProperties().getProperty("service.vdt.cert.file");
               fCollectorPem = net.sf.gratia.util.XP.get(certfile);
               fCertificate = new net.sf.gratia.storage.Certificate( fCollectorPem );

               String keyfile =  net.sf.gratia.util.Configuration.getProperties().getProperty("service.vdt.key.file");
               fCollectorKeyPem = net.sf.gratia.util.XP.get(keyfile);               
                // Remove leading and trailing lines.
               fCollectorKeyPem = fCollectorKeyPem.substring(fCollectorKeyPem.indexOf('\n')+1,
                                                             fCollectorKeyPem.lastIndexOf('\n',fCollectorKeyPem.length()-2));
               KeyFactory keyFactory = KeyFactory.getInstance("RSA");
               fPrivateKey = keyFactory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(net.sf.gratia.util.Base64.decode(fCollectorKeyPem)));
            }
            
            /**
             * Method called by WebSphere Application Server runtime to set the custom
             * properties.
             * 
             * @param java.util.Properties - custom props
             */
            public void setCustomProperties(java.util.Properties customProps)
            {
               props = customProps;
            }
            
            private java.util.Properties getCustomProperties()
            {
               return props;
            }
            
            /**
             * Method called by WebSphere Application Server runtime to set the SSL
             * configuration properties being used for this connection.
             * 
             * @param java.util.Properties - contains a property for the SSL configuration.
             */
            public void setSSLConfig(java.util.Properties config)
            {
               sslConfig = config;                                   
            }
            
            private java.util.Properties getSSLConfig()
            {
               return sslConfig;
            }
            
            /**
             * Method called by WebSphere Application Server runtime to set the default
             * X509KeyManager created by the IbmX509 KeyManagerFactory using the KeyStore
             * information present in this SSL configuration.  This allows some delegation
             * to the default IbmX509 KeyManager to occur.
             * 
             * @param javax.net.ssl.KeyManager defaultX509KeyManager - default key manager for IbmX509
             */
            public void setDefaultX509KeyManager(javax.net.ssl.X509KeyManager defaultX509KeyManager)
            {
               km = defaultX509KeyManager;
            }
            
            public javax.net.ssl.X509KeyManager getDefaultX509KeyManager()
            {
               return km;
            }
            
            /**
             * Method called by WebSphere Application Server runtime to set the SSL
             * KeyStore used for this connection.
             * 
             * @param java.security.KeyStore - the KeyStore currently configured
             */
            public void setKeyStore(java.security.KeyStore keyStore)
            {
               ks = keyStore;
            }
            
            public java.security.KeyStore getKeyStore()
            {
               return ks;
            }
            
            /**
             * Method called by custom code to set the server alias.
             * 
             * @param String - the server alias to use
             */
            public void setKeyStoreServerAlias(String alias)
            {
               serverAlias = alias;
            }
            
            private String getKeyStoreServerAlias()
            {
               return serverAlias;
            }
            
            /**
             * Method called by custom code to set the client alias.
             * 
             * @param String - the client alias to use
             */
            public void setKeyStoreClientAlias(String alias)
            {
               clientAlias = alias;
            }
            
            private String getKeyStoreClientAlias()
            {
               return clientAlias;
            }
            
            /**
             * Method called by custom code to set the client alias and slot (if necessary).
             * 
             * @param String - the client alias to use
             * @param int - the slot to use (for hardware)
             */
            public void setClientAlias(String alias, int slotnum) throws Exception
            {
               //Logging.warning("setClientAlias: "+alias);
               if ( !alias.equals("currentKey") && !ks.containsAlias(alias))
               {
                  throw new IllegalArgumentException ( "Client alias " + alias + "not found in keystore." );
               }
               this.clientAlias = alias;
               this.clientslotnum = slotnum;
            }
            
            /**
             * Method called by custom code to set the server alias and slot (if necessary).
             * 
             * @param String - the server alias to use
             * @param int - the slot to use (for hardware)
             */
            public void setServerAlias(String alias, int slotnum) throws Exception
            {
               //Logging.warning("setServerAlias: "+alias);
               if ( ! ks.containsAlias(alias))
               {
                  throw new IllegalArgumentException ( "Server alias " + alias + "not found in keystore." );
               }
               this.serverAlias = alias;
               this.serverslotnum = slotnum;
            }
            
            
            /**
             * Method called by JSSE runtime to when an alias is needed for a client
             * connection where a client certificate is required.
             * 
             * @param String keyType
             * @param Principal[] issuers
             * @param java.net.Socket socket (not always present)
             */
            public String chooseClientAlias(String[] keyType, java.security.Principal[] issuers, java.net.Socket socket)
            {
               //Logging.warning("chooseClientAlias: "+keyType);
               if (clientAlias != null && !clientAlias.equals(""))
               {
                  String[] list = km.getClientAliases(keyType[0], issuers);
                  String aliases = "";
                  
                  if (list != null)
                  {
                     boolean found=false;
                     for (int i=0; i<list.length; i++)
                     {
                        aliases += list[i] + " ";
                        if (clientAlias.equalsIgnoreCase(list[i]))
                           found=true;
                     }
                     
                     if (found)
                     {
                        return clientAlias;
                     }
                     
                  }
               }
               
               // client alias not found, let the default key manager choose.
               String[] keyArray = new String [] {keyType[0]};
               String alias = km.chooseClientAlias(keyArray, issuers, null);
               if (alias == null) {
                  return "currentKey";
               } else {
                  return alias.toLowerCase();
               }
            }
            
            /**
             * Method called by JSSE runtime to when an alias is needed for a server
             * connection to provide the server identity.
             * 
             * @param String[] keyType
             * @param Principal[] issuers
             * @param java.net.Socket socket (not always present)
             */
            public String chooseServerAlias(String keyType, java.security.Principal[] 
                                            issuers, java.net.Socket socket)
            {
               //Logging.warning("chooseServerAlias: "+keyType);
               if (serverAlias != null && !serverAlias.equals(""))
               {
                  // get the list of aliases in the keystore from the default key manager
                  String[] list = km.getServerAliases(keyType, issuers);
                  String aliases = "";
                  
                  if (list != null)
                  {
                     boolean found=false;
                     for (int i=0; i<list.length; i++)
                     {
                        aliases += list[i] + " ";
                        if (serverAlias.equalsIgnoreCase(list[i]))
                           found = true;
                     }
                     
                     if (found)
                     {
                        return serverAlias;
                     }
                  }
               }
               
               // specified alias not found, let the default key manager choose.
               String alias = km.chooseServerAlias(keyType, issuers, null);
               return alias.toLowerCase();
            }
            
            public String[] getClientAliases(String keyType, java.security.Principal[] issuers)
            {
               //Logging.warning("getClientAliases: "+keyType);
               String alias[] = { "currentKey" };
               return alias;
               // return km.getClientAliases(keyType, issuers);
            }
            
            public String[] getServerAliases(String keyType, java.security.Principal[] issuers)
            {
               //Logging.warning("getServerAliases: "+keyType);
               return km.getServerAliases(keyType, issuers);
            }
            
            public java.security.PrivateKey getPrivateKey(String s)
            {
               //Logging.warning("getPrivateKey: "+s);
               if (s.equals("currentKey")) {
                  try {
                     return fPrivateKey; 
                  } catch (Exception e) {
                     Logging.warning("Unable to create the X509 private key object for "+s,e);
                  }
               }
               return km.getPrivateKey(s);
            }
            
            public java.security.cert.X509Certificate[] getCertificateChain(String s)
            {
               //Logging.warning("getCertificateChain: "+s);
               try {
                  if (s.equals("currentKey")) {
                     java.security.cert.X509Certificate[] tmp = new  java.security.cert.X509Certificate[] {fCertificate.getCert() }; 
                     return tmp;
                  }
               } catch (java.security.cert.CertificateException e) {
                  Logging.warning("Unable to create the X509 object for "+s,e);
               }
               return km.getCertificateChain(s);
            }
            
            public javax.net.ssl.X509KeyManager getX509KeyManager()
            {
               return km;
            }
            
         }
      public String send(boolean printError)
      {
         if (!success) {
            if (printError) exception.printStackTrace();
            return null;
         }
         
         StringBuffer received = new StringBuffer();
         success = true;
         try
         {
            URL url = new URL(destination);
            URLConnection connection = url.openConnection();
            if (url.getProtocol().equals("https")) {
               Logging.debug("Setting up the special manager");

               TrustManager[] tm = new TrustManager[ 1 ];
               tm[ 0 ] = new CustomTrustManager(destination);
               KeyManager[] km = new KeyManager[ 1 ];
               km[ 0 ] = new CustomKeyManager();
               
               SSLContext ctx = SSLContext.getInstance("TLS");
               ctx.init(km, tm, null); // last argument could be set to new java.security.SecureRandom()
               SSLSocketFactory sslSocketFactory = ctx.getSocketFactory();
                
               if (connection instanceof HttpsURLConnection) {
                  ((HttpsURLConnection)connection).setSSLSocketFactory(sslSocketFactory);
               } else {
                  ((com.sun.net.ssl.internal.www.protocol.https.HttpsURLConnectionOldImpl)connection).setSSLSocketFactory(sslSocketFactory);
               }
            }
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            String temp = buffer.toString();
            for (int i = 0; i < temp.length(); i++)
               writer.write(temp,i,1);
            // writer.write(buffer.toString());
            writer.flush();
            
            // Get the response
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null)
               received.append(line);
            writer.close();
            reader.close();
            return received.toString();
         }
         catch (Exception e)
         {
            success = false;
            errorMsg = e.toString();
            exception = e;
            if (printError) e.printStackTrace();
            return null;
         }
      }
      
      public String send() {
         return send(false);
      }
      
      public static void main(String args[])
      {
         Post post = new Post("http://localhost:8080/gratia/rmi","xxxxxxx");
         String response = post.send();
         Logging.log("R: " + response);
      }
   }

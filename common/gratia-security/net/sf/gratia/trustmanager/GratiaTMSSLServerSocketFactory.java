/*
 * Copyright (c) Fermilab 2009
 *
 */

package net.sf.gratia.trustmanager;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.glite.security.trustmanager.ContextWrapper;

import java.security.GeneralSecurityException;

import java.util.Properties;

import java.io.IOException;

/**
 * @author Philippe Canal
 *
 * Adaptater of glite's TMSSLServerSocketFactory.
 **/
class GratiaTMSSLServerSocketFactory extends org.glite.security.trustmanager.tomcat.TMSSLServerSocketFactory {
   static org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(org.glite.security.trustmanager.tomcat.TMSSLServerSocketFactory.class);
   static String defaultAlgorithm = "SunX509";

   GratiaTMSSLServerSocketFactory() {
      super();
      logger.debug("GratiaTMSSLServerSocketFactory.constructor:");
   }
   
   /*
    * (non-Javadoc)
    *
    * @see org.apache.tomcat.util.net.ServerSocketFactory#createSocket(int,
    *      int, java.net.InetAddress)
    */
   public ServerSocket createSocket(int port, int backlog, InetAddress ifAddress) throws IOException, InstantiationException {
      logger.debug("GratiaTMSSLServerSocketFactory.createSocket3:");
      
      if (sslProxy == null) {
         init();
      }
      
      return super.createSocket(port,backlog,ifAddress);
//      ServerSocket socket = sslProxy.createServerSocket(port, backlog, ifAddress);
//      super.initServerSocket(socket);
//      return socket;
   }
   
   /*
    * (non-Javadoc)
    *
    * @see org.apache.tomcat.util.net.ServerSocketFactory#createSocket(int,
    *      int)
    */
   public ServerSocket createSocket(int port, int backlog) throws IOException, InstantiationException {
      logger.debug("GratiaTMSSLServerSocketFactory.createSocket2:");
      
      if (sslProxy == null) {
         init();
      }
      
      return super.createSocket(port,backlog);
//      ServerSocket socket = sslProxy.createServerSocket(port, backlog);
//      super.initServerSocket(socket);
//      return socket;
   }
   
   /*
    * (non-Javadoc)
    *
    * @see org.apache.tomcat.util.net.ServerSocketFactory#createSocket(int)
    */
   public ServerSocket createSocket(int port) throws IOException, InstantiationException {
      logger.debug("GratiaTMSSLServerSocketFactory.createSocket1:");
      
      if (sslProxy == null) {
         init();
      }
      
      return super.createSocket(port);
//      ServerSocket socket = sslProxy.createServerSocket(port);
//      super.initServerSocket(socket);      
//      return socket;
   }

   /**
    * Reads the keystore and initializes the SSL socket factory.
    */
   void init() throws IOException {
      logger.debug("GratiaTMSSLServerSocketFactory.init:");
      
      try {
         String clientAuthStr = (String) attributes.get("clientauth");
         
         if ("true".equalsIgnoreCase(clientAuthStr) || "yes".equalsIgnoreCase(clientAuthStr)) {
            requireClientAuth = true;
         } else if ("want".equalsIgnoreCase(clientAuthStr)) {
            wantClientAuth = true;
         }
         
         // SSL protocol variant (e.g., TLS, SSL v3, etc.)
         String protocol = (String) attributes.get("protocol");
         
         if (protocol == null) {
            protocol = ContextWrapper.SSL_PROTOCOL_DEFAULT;
         }
         
         // Certificate encoding algorithm (e.g., SunX509)
         String algorithm = (String) attributes.get("algorithm");
         
         if (algorithm == null) {
            algorithm = defaultAlgorithm;
         }
         
         String keystoreType = (String) attributes.get("keystoreType");
         
         if (keystoreType == null) {
            keystoreType = ContextWrapper.KEYSTORE_TYPE_DEFAULT;
         }
         
         String trustAlgorithm = (String) attributes.get("truststoreAlgorithm");
         
         if (trustAlgorithm == null) {
            trustAlgorithm = algorithm;
         }
         
         // Create and init SSLContext
         initProxy();
         
         // Determine which cipher suites to enable
         String requestedCiphers = (String) attributes.get("ciphers");
         
         if (requestedCiphers != null) {
            enabledCiphers = getEnabledCiphers(requestedCiphers, sslProxy.getSupportedCipherSuites());
         }
      } catch (Exception e) {
         if (e instanceof IOException) {
            throw (IOException) e;
         }
         
         throw new IOException(e.getMessage());
      }
   }

   /**
    * Initialize the SSL socket factory.
    *
    * @exception IOException
    *                if an input/output error occurs
    */
   private void initProxy() throws IOException {
      logger.debug("GratiaTMSSLServerSocketFactory.initProxy:");
      
      try {
         Properties props = new Properties();
         @SuppressWarnings("unchecked") java.util.Hashtable<String,String> tmp = attributes;
         
         props.putAll(tmp);
         logger.debug(props);
         
         contextWrapper = new org.glite.security.trustmanager.GratiaContextAdaptater(props);
         
         // Create the proxy and return
         sslProxy = contextWrapper.getServerSocketFactory();
      } catch (Exception e) {
         logger.fatal("Server socket factory creation failed:  "+e);
         throw new IOException(e.toString());
      }
   }
   
}

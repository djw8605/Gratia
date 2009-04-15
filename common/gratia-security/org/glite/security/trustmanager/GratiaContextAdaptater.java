/*
 * Copyright (c) Fermilab 2009
 *
 */

package org.glite.security.trustmanager;

import org.bouncycastle.openssl.PasswordFinder;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import java.io.IOException;

import java.util.Properties;

import javax.net.ssl.SSLContext;

/**
 * @author Philippe Canal
 *
 * Adaptater of glite's Context Wrapper.
 **/
public class GratiaContextAdaptater extends org.glite.security.trustmanager.ContextWrapper
{
   static org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(org.glite.security.trustmanager.GratiaContextAdaptater.class);

   /**
    * Creates a new ContextWrapper object.
    *
    * @param inputConfig the configuration to use.
    *
    * @throws IOException in case there is a problem reading config file, certificates, key or CRLs.
    * @throws GeneralSecurityException if there is a problem initializing the SSLContext.
    */
   public GratiaContextAdaptater(Properties inputConfig) throws IOException, GeneralSecurityException, Exception {
      super(inputConfig);
   }
   
   
   /**
    * Initializes the key manager.
    *
    * @param finder the PasswordFinder implementation to use to ask the user for password to access the private key.
    * @param chain the certificate chain to be used as credentials.
    * @param key the private key to be used as credential.
    *
    * @throws CertificateException if certificate reading failed.
    * @throws NoSuchAlgorithmException if certificate or key uses unsupported algorithm.
    * @throws IOException if certificate reading failed.
    */
   public void init(PasswordFinder finder, X509Certificate[] chain, PrivateKey key)
   throws CertificateException, GeneralSecurityException, IOException, Exception {
      logger.debug("Gratia TrustManager's context");
      
      certReader = new org.glite.security.util.FileCertReader();
      
      try {
         if ((chain == null) && (key == null)) {
            initKeyManagers(finder);
         } else {
            if ((chain == null) || (key == null)) {
               logger.fatal(
                            "Internal error: either certificate chain or private key of credentials is not defined");
               throw new CertificateException(
                                              "Internal error: either certificate chain or private key of credentials is not defined");
            }
            
            initKeyManagers(chain, key);
         }
         
         super.initTrustAnchors();
         
         trustManager = new net.sf.gratia.trustmanager.TrustManager(trustAnchors);
         
         javax.net.ssl.TrustManager[] managerArray = new javax.net.ssl.TrustManager[] { trustManager };
         
         String protocol = config.getProperty(SSL_PROTOCOL, SSL_PROTOCOL_DEFAULT);
         
         logger.debug("Using transport protocol: " + protocol);
         
         // Create an SSL context used to create an SSL socket factory
         sslContext = SSLContext.getInstance(protocol);
         
         logger.debug("Actually using transport protocol: " + sslContext.getProtocol());
         
         // Initialize the context with the key managers
         sslContext.init(identityKeyManagers, managerArray, new java.security.SecureRandom());
         
         super.startCRLLoop();
      } catch (GeneralSecurityException e) {
         logger.fatal("ContextWrapper initialization failed: " + e.getMessage());
         throw e;
      } catch (IOException e) {
         logger.fatal("ContextWrapper initializatoin failed: " + e.getMessage());
         throw e;
      }
   }
}
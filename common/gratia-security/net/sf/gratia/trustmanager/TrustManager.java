/*
 * Copyright (c) Fermilab 2009
 *
 */

package net.sf.gratia.trustmanager;

import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import java.util.Vector;
import java.io.FileInputStream;


/**
 * @author Philippe Canal
 *
 * Adaptater of glite's CRLFileTrustManager.
 **/
public class TrustManager extends org.glite.security.trustmanager.CRLFileTrustManager {
   static org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(org.glite.security.trustmanager.CRLFileTrustManager.class);
   X509TrustManager pkixTrustManager;
   private KeyStore fKeyStore;
   
   /** Creates new CRLTrustManager
    */
   public TrustManager(Vector trustAnchors) throws CertificateException, NoSuchProviderException, java.security.KeyStoreException, 
                                                   java.io.FileNotFoundException, java.security.NoSuchAlgorithmException, java.io.IOException 
   {
      super(trustAnchors);
      
      fKeyStore = KeyStore.getInstance("JKS");
      fKeyStore.load(new FileInputStream(net.sf.gratia.util.Configuration.getConfigurationPath() + "/keystore"),
                     "server".toCharArray());
      
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
      tmf.init(fKeyStore);
      
      javax.net.ssl.TrustManager tms [] = tmf.getTrustManagers();
      
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
      
   }
   
   /** This method checks that the certificate path is a valid
    * client certificate path. Currently the signatures and
    * subject lines are checked so that the path is valid and leads
    * to one of the CA certs given in init method. The certs
    * are also checked against the CRLs and that they have not
    * expired. This method behaves identically to the server
    * version of this method. No checks are made that this is
    * a client cert. If the cert path fails the check an
    * exception is thrown.
    *
    * @param x509Certificate The certificate path to check. It may contain
    * the CA cert or not. If it contains the CA cert,
    * the CA cert is discarded and the one given in init
    * is used. The array has the actual certificate in
    * the index 0 and the CA or the CA signed cert as
    * the last cert.
    * @param authType Defines the authentication type, but is not used.
    * @throws CertificateException Thrown if the certificate path is invalid.
    */
   public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificate, String authType) throws java.security.cert.CertificateException {
      logger.debug("Gratia Trustmanager is validating a client");
      
      if (x509Certificate == null) return;
      
      logger.debug("Gratia Trustmanager subject:"+x509Certificate[0].getSubjectDN());
      logger.debug("Gratia Trustmanager issuer:"+x509Certificate[0].getIssuerDN());

      if ( x509Certificate[0].getIssuerDN().equals( x509Certificate[0].getSubjectDN() ) ) 
      {
         logger.debug("Gratia Trustmanager has matching cert");
         pkixTrustManager.checkClientTrusted(x509Certificate, authType);
      } else {         
         logger.debug("Gratia Trustmanager a non matching cert");
         super.checkClientTrusted(x509Certificate, authType);
      }
   }
   
   /** This method checks that the certificate path is a valid
    * server certificate path. Currently the signatures and
    * subject lines are checked so that the path is valid and leads
    * to one of the CA certs given in init method. The certs
    * are also checked against the CRLs and that they have not
    * expired. This method behaves identically to the client
    * version of this method. No checks are made that this is
    * a server cert. If the cert path fails the check an
    * exception is thrown.
    *
    * @param x509Certificate The certificate path to check. It may contain
    * the CA cert or not. If it contains the CA cert,
    * the CA cert is discarded and the one given in init
    * is used. The array has the actual certificate in
    * the index 0 and the CA or the CA signed cert as
    * the last cert.
    * @param authType Defines the authentication type, but is not used.
    * @throws CertificateException Thrown if the certificate path is invalid.
    */
   public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificate, String authType) throws CertificateException {
      logger.debug("Gratia Trustmanager is validating a server");
      checkClientTrusted(x509Certificate, authType);
   }
   
   /** This method returns an array containing all the CA
    * certs.
    * @return An array containig all the CA certs is reaurned.
    */
   public java.security.cert.X509Certificate[] getAcceptedIssuers() {
      logger.debug("Gratia Trustmanager getAcceptedIssuers");
      
      return super.getAcceptedIssuers();
   }
   
}
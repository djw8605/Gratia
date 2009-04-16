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
import java.io.FileOutputStream;

/**
 * @author Philippe Canal
 *
 * Adaptater of glite's CRLFileTrustManager.
 **/
public class TrustManager extends org.glite.security.trustmanager.CRLFileTrustManager {
   static org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(org.glite.security.trustmanager.CRLFileTrustManager.class);
   
   /** Creates new CRLTrustManager
    */
   public TrustManager(Vector trustAnchors) throws CertificateException, NoSuchProviderException
   {
      super(trustAnchors);
   }
      
   private X509TrustManager getPkixTrustManager() throws CertificateException, java.security.KeyStoreException, 
                java.io.FileNotFoundException, java.security.NoSuchAlgorithmException, java.io.IOException {

      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(new FileInputStream(net.sf.gratia.util.Configuration.getConfigurationPath() + "/keystore"),
                     "server".toCharArray());
      
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
      tmf.init(keyStore);
      
      javax.net.ssl.TrustManager tms [] = tmf.getTrustManagers();
      
      /*
       * Iterate over the returned trustmanagers, look
       * for an instance of X509TrustManager.  If found,
       * use that as our "default" trust manager.
       */
      for (int i = 0; i < tms.length; i++) {
         if (tms[i] instanceof X509TrustManager) {
            return (X509TrustManager) tms[i];
         }
      }
      return null;
   }

   private boolean addCertificate(java.security.cert.X509Certificate cert[]) {
      
      // Returns true if we added the cert (i.e. if its DN looks like one of ours.
      
      // Our DN pattern:
      // String dname = "cn=xxx, ou=Fermi-GridAccounting, o=Fermi, c=US";
      
      if (cert == null || cert[0] == null) return false;
      
      String dn = cert[0].getSubjectDN().toString();
      int first = dn.indexOf(',');
      if (first == -1) return false;
      
      String cn = dn.substring(0,first);
      String sn = dn.substring(first+1);
      
      if (!sn.toLowerCase().equals(" ou=gratiaaccounting, o=gratia, c=us")) {
         logger.debug("Gratia Trustmanager failed to add client certificate '"+dn+"' because of:"+sn);
         return false;
      }
          
      try {
         KeyStore keyStore = KeyStore.getInstance("JKS");
         keyStore.load(new FileInputStream(net.sf.gratia.util.Configuration.getConfigurationPath() + "/keystore"),
                       "server".toCharArray());
         
         keyStore.setCertificateEntry( sn, cert[0] );
         keyStore.store(new FileOutputStream(net.sf.gratia.util.Configuration.getConfigurationPath() + "/keystore"),
                        "server".toCharArray());
      } catch (Exception e) {
         logger.debug("Gratia Trustmanager failed to add client certificate "+dn+" because of:"+e.getMessage());
         return false;
      }
      logger.debug("Gratia Trustmanager added client certificate: "+dn);
      return true;
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
         X509TrustManager manager = null; 
         try {
            manager = getPkixTrustManager();
         } catch (Exception e) {
            logger.fatal("Gratia Trustmanager can't red the key store"+e.getMessage());
            super.checkClientTrusted(x509Certificate, authType);
         } 
         if (manager != null) {
            try {
               manager.checkClientTrusted(x509Certificate, authType);
            }
            catch (java.security.cert.CertificateException e) {
               logger.debug("Gratia Trustmanager rejecting certificate because: "+e.getMessage());
               if (!addCertificate(x509Certificate)) {
                  throw e;
               }
            } catch(java.lang.RuntimeException re) {
               Throwable e = re.getCause();
               if (e instanceof java.security.InvalidAlgorithmParameterException) {
                  if (e.getMessage().equals("the trustAnchors parameter must be non-empty")) {
                     logger.debug("Gratia Trustmanager has noticed an empty keystore");
                     if (!addCertificate(x509Certificate)) {
                        throw new java.security.cert.CertificateException("empty keystore");
                     }
                  } else {
                     logger.debug("Gratia Trustmanager Unexpected exception message: "+e.getMessage());
                     throw new java.security.cert.CertificateException(e.getMessage());
                  }
               } else {                  
                  logger.debug("Gratia Trustmanager Unexpected exception type: "+e.getMessage());
                  throw new java.security.cert.CertificateException("Unexpected exception type: "+e.getMessage());
               }
            } catch (Exception e) {
               logger.debug("Gratia Trustmanager Unexpected exception type: "+e.getMessage());
               throw new java.security.cert.CertificateException("Unexpected exception type: "+e.getMessage());
            }
         }
         logger.debug("Gratia Trustmanager has matching cert that was accepted");
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
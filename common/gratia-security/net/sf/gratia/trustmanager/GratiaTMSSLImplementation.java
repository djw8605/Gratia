/*
 * Copyright (c) Members of the EGEE Collaboration. 2004.
 * See http://eu-egee.org/partners/ for details on the copyright holders.
 * For license conditions see the license file or http://eu-egee.org/license.html
 */

/*
 * Created on Sep 15, 2004
 *
 * Adapted by Philippe Canal from org.glite.security.trustmanager.tomcat.TMSSLImplementation
 */
package net.sf.gratia.trustmanager;

import org.apache.tomcat.util.net.SSLImplementation;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.ServerSocketFactory;
import org.apache.tomcat.util.net.jsse.JSSEImplementation;

import java.net.Socket;


/**
 * @author Joni Hahkala and Philippe Canal
 *
 * The main Tomcat 5 (+?) glue class
 */
public class GratiaTMSSLImplementation extends SSLImplementation {
   static org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(GratiaTMSSLImplementation.class);
   
   /*
    * The constructor for the class, does nothing except checks that the actual
    * ssl implementation TrustManager is present.
    *
    */
   public GratiaTMSSLImplementation() throws ClassNotFoundException {
      // Check to see if glite-security-trustmanager is floating around somewhere
      Class.forName("org.glite.security.trustmanager.ContextWrapper");
      Class.forName("org.glite.security.trustmanager.GratiaContextAdaptater");
   }
   
   /*
    * The Method that returns the name of the SSL implementation
    *
    * The string "TM-SSL" is returned (shorthand for TrustManager SSL)
    *
    * @see org.apache.tomcat.util.net.SSLImplementation#getImplementationName()
    */
   public String getImplementationName() {
      return "GTM-SSL";
   }
   
   /*
    * The method used by Tomcat to get the actual SSLServerSocketFactory to use
    * to create the ServerSockets.
    *
    * @see org.apache.tomcat.util.net.SSLImplementation#getServerSocketFactory()
    */
   public ServerSocketFactory getServerSocketFactory() {
      return new GratiaTMSSLServerSocketFactory();
   }
   
   /*
    * The method used to get the class that provides the SSL support functions.
    * Current implementation reuses Tomcat's own JSSE SSLSupport class as we
    * use JSSE internally too (with modifications to the certificate path
    * checking of course.
    *
    * @see org.apache.tomcat.util.net.SSLImplementation#getSSLSupport(java.net.Socket)
    */
   public SSLSupport getSSLSupport(Socket arg0) {
      try {
         JSSEImplementation impl = new JSSEImplementation();
         
         return impl.getSSLSupport(arg0);
      } catch (ClassNotFoundException e) {
         logger.fatal("Internal server error, JSSEImplementation class creation failed:", e);
         
         return null;
      }
   }
}

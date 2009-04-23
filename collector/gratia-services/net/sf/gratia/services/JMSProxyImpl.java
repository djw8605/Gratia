package net.sf.gratia.services;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.XP;

import java.rmi.*;
import java.rmi.server.*;
import java.util.Properties;
import java.io.*;

public class JMSProxyImpl extends UnicastRemoteObject implements JMSProxy {
   
   static final long serialVersionUID = 1;
   Properties p;
   String queues[] = null;
   int iq = 0;
   int irecords = 0;
   CollectorService collectorService = null;
   
   public JMSProxyImpl(CollectorService collectorService)
   throws RemoteException {
      super();
      
      this.collectorService = collectorService;
      
      loadProperties();
      
      int maxthreads = Integer.parseInt(p
                                        .getProperty("service.listener.threads"));
      queues = new String[maxthreads];
      for (int i = 0; i < maxthreads; i++)
         queues[i] = System.getProperties().getProperty("catalina.home")
         + "/gratia/data/thread" + i;
   }
   
   public void loadProperties() {
      try {
         p = Configuration.getProperties();
      } catch (Exception ignore) {
      }
   }
   
   public Boolean update(String xml) throws RemoteException {
      ++irecords;
      
      if ((irecords % 100) == 0) {
         ++iq;
         if (iq > (queues.length - 1)) {
            iq = 0;
         }
         try {
            Thread.yield();
         } catch (Exception ignore) {
         }
      }
      
      try {
         File file = File.createTempFile("job", ".xml", new File(queues[iq]));
         String filename = file.getPath();
         XP.save(filename, xml);
         return true;
      } catch (Exception e) {
         e.printStackTrace();
      }
      return false;
   }
   
   public void stopDatabaseUpdateThreads() throws RemoteException {
      collectorService.stopDatabaseUpdateThreads();
   }
   
   public void startDatabaseUpdateThreads() throws RemoteException {
      collectorService.startDatabaseUpdateThreads();
   }
   
   public Boolean reaperActive() throws RemoteException {
      return collectorService.reaperActive();
   }
   
   public void runReaper() throws RemoteException {
      collectorService.runReaper();
   }   
   
   public Boolean databaseUpdateThreadsActive() throws RemoteException {
      return collectorService.databaseUpdateThreadsActive();
   }
   
   public Boolean operationsDisabled() throws RemoteException {
      return collectorService.operationsDisabled();
   }
   
   public void enableOperations() throws RemoteException {
      collectorService.enableOperations();
   }
   
   public Boolean replicationServiceActive() throws RemoteException {
      return collectorService.replicationServiceActive();
   }
   
   public void startReplicationService() throws RemoteException {
      collectorService.startReplicationService();
   }
   
   public void stopReplicationService() throws RemoteException {
      collectorService.stopReplicationService();
   }
   
   public Boolean servletEnabled() throws RemoteException {
      return collectorService.servletEnabled();
   }
   
   public void enableServlet() throws RemoteException {
      collectorService.enableServlet();
   }
   
   public void disableServlet() throws RemoteException {
      collectorService.disableServlet();
   }
   
   public String housekeepingServiceStatus() throws RemoteException {
      return collectorService.housekeepingServiceStatus();
   }
   
   public void startHousekeepingService() throws RemoteException {
      collectorService.startHousekeepingService();
   }
   
   public Boolean startHousekeepingActionNow() throws RemoteException {
      return collectorService.startHousekeepingActionNow();
   }
   
   public void stopHousekeepingService() throws RemoteException {
      collectorService.stopHousekeepingService();
   }
   
   public void disableHousekeepingService() throws RemoteException {
      collectorService.disableHousekeepingService();
   }

   public void setConnectionCaching(boolean enable) throws RemoteException {
      collectorService.setConnectionCaching(enable);
   }
   
   public String checkConnection(java.security.cert.X509Certificate certs[], String senderHost, String sender) throws RemoteException, AccessException {
      return collectorService.checkConnection(certs,senderHost,sender);
   }

   public String checkConnection(String certpem, String senderHost, String sender) throws RemoteException, AccessException {
      return collectorService.checkConnection(certpem,senderHost,sender);
   }
   
}

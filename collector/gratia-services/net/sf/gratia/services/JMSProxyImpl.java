package net.sf.gratia.services;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.XP;
import net.sf.gratia.util.Logging;
import net.sf.gratia.util.LogLevel;

import java.rmi.*;
import java.rmi.server.*;
import java.util.Properties;
import java.io.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JMSProxyImpl extends UnicastRemoteObject implements JMSProxy 
{
   // For UnicastRemoteObject.
   static final long serialVersionUID = 1;

   File stageDir = null;
   int iq = 0;
   int irecords = 0;
   CollectorService collectorService = null;
   
   public JMSProxyImpl(CollectorService collectorService)
   throws RemoteException {
      super();
      
      this.collectorService = collectorService;

      QueueManager.initialize();
      stageDir = QueueManager.getStageDir();
   }
   
   public Boolean update(String from, String xml) throws RemoteException 
   {
      // First attempt to get the xml stored on disk on the stage directory
      try {
         File tmpFile = File.createTempFile("stage-", ".xml", stageDir);
         Boolean tmpResult = XP.save(tmpFile, xml);
         if (!tmpResult) {
            return tmpResult;
         }
         // Successful save
         ++irecords;
         if ((irecords % 100) == 0) {
            ++iq;
            if (iq > (QueueManager.getNumberOfQueues() - 1)) {
               iq = 0;
            }
            try {
               Thread.yield();
            } catch (Exception ignore) {
            }
         }
         return QueueManager.update(iq,tmpFile,from,xml);
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }

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
   
   public void pauseHousekeepingService() throws RemoteException {
      collectorService.pauseHousekeepingService();
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
   
   public String refreshStatus() throws RemoteException {
      QueueManager.refreshStatus();
      return "";
   }
   
   public String queueManagerStatus() throws RemoteException {
      return QueueManager.getStatus();
   }

}

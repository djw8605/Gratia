package net.sf.gratia.services;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;

public interface JMSProxy extends java.rmi.Remote {
    public Boolean update(String xml) throws RemoteException;
    public void stopDatabaseUpdateThreads() throws RemoteException;
    public void startDatabaseUpdateThreads() throws RemoteException;
    public Boolean databaseUpdateThreadsActive() throws RemoteException;
    public void stopReplicationService() throws RemoteException;
    public void startReplicationService() throws RemoteException;
    public Boolean reaperActive() throws RemoteException;
    public void runReaper() throws RemoteException;
    public Boolean replicationServiceActive() throws RemoteException;
    public Boolean servletEnabled() throws RemoteException;
    public Boolean operationsDisabled() throws RemoteException;
    public void enableOperations() throws RemoteException;
    public void enableServlet() throws RemoteException;
    public void disableServlet() throws RemoteException;
    public String housekeepingServiceStatus() throws RemoteException;
    public void startHousekeepingService() throws RemoteException;
    public void stopHousekeepingService() throws RemoteException;
    public void disableHousekeepingService() throws RemoteException;
    public Boolean startHousekeepingActionNow() throws RemoteException;
    public void setConnectionCaching(boolean enable) throws RemoteException;
    public String checkConnection(X509Certificate certs[], String senderHost, String sender) throws RemoteException, AccessException;
    public String checkConnection(String certspem, String senderHost, String sender) throws RemoteException, AccessException;
}

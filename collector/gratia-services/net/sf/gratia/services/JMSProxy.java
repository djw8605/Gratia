package net.sf.gratia.services;

import java.rmi.RemoteException;

public interface JMSProxy extends java.rmi.Remote {
    public Boolean update(String xml) throws RemoteException;
    public void stopDatabaseUpdateThreads() throws RemoteException;
    public void startDatabaseUpdateThreads() throws RemoteException;
    public Boolean databaseUpdateThreadsActive() throws RemoteException;
    public void stopReplicationService() throws RemoteException;
    public void startReplicationService() throws RemoteException;
    public Boolean replicationServiceActive() throws RemoteException;
    public Boolean servletEnabled() throws RemoteException;
    public Boolean operationsDisabled() throws RemoteException;
    public void enableOperations() throws RemoteException;
    public void enableServlet() throws RemoteException;
    public void disableServlet() throws RemoteException;
}

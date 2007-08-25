package net.sf.gratia.services;

import java.rmi.RemoteException;

public interface JMSProxy extends java.rmi.Remote {
    public boolean update(String xml) throws RemoteException;
    public boolean handshake(String xml) throws RemoteException;
    public void stopDatabaseUpdateThreads() throws RemoteException;
    public void startDatabaseUpdateThreads() throws RemoteException;
    public boolean databaseUpdateThreadsActive() throws RemoteException;
}


package net.sf.gratia.services;

import java.util.*;
import java.rmi.*;

public interface JMSProxy extends java.rmi.Remote
{
		public boolean update(String xml) throws RemoteException;
		public boolean statusUpdate(String xml) throws RemoteException;
		public void stopDatabaseUpdateThreads() throws RemoteException;
		public void startDatabaseUpdateThreads() throws RemoteException;
		public boolean databaseUpdateThreadsActive() throws RemoteException;
}


package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import java.rmi.*;
import java.rmi.server.*;
import java.util.Hashtable;
import java.util.Properties;
import java.io.*;

public class JMSProxyImpl extends UnicastRemoteObject implements JMSProxy {

    static final long serialVersionUID = 1;
    private String rmilookup;
    private String service;
    private String driver;
    private String url;
    private String user;
    private String password;
    Hashtable pumps = new Hashtable();
    Properties p;
    String queues[] = null;
    int iq = 0;
    XP xp = new XP();
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
            rmilookup = p.getProperty("service.rmi.rmilookup");
            service = p.getProperty("service.rmi.service");
            driver = p.getProperty("service.mysql.driver");
            url = p.getProperty("service.mysql.url");
            user = p.getProperty("service.mysql.user");
            password = p.getProperty("service.mysql.password");
        } catch (Exception ignore) {
        }
    }

    public Boolean update(String xml) throws RemoteException {
        irecords++;

        if ((irecords % 100) == 0) {
            iq++;
            if (iq > (queues.length - 1))
                iq = 0;

            try {
                Thread.yield();
            } catch (Exception ignore) {
            }
        }

        try {
            File file = File.createTempFile("job", "xml", new File(queues[iq]));
            String filename = file.getPath();
            xp.save(filename, xml);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Boolean handshake(String xml) throws RemoteException {
        ProbeStatusUpdate update = new ProbeStatusUpdate();

        update.update(xml);

        return true;
    }

    public void stopDatabaseUpdateThreads() throws RemoteException {
        collectorService.stopDatabaseUpdateThreads();
    }

    public void startDatabaseUpdateThreads() throws RemoteException {
        collectorService.startDatabaseUpdateThreads();
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

}

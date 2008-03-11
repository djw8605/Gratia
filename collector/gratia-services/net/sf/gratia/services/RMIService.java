package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.rmi.registry.*;
import java.util.*;

public class RMIService extends Thread
{
    int port;
    XP xp = new XP();
    
    public RMIService()
    {
	try
	    {
		Properties p = Configuration.getProperties();
		java.net.InetAddress local = java.net.InetAddress.getLocalHost();
		System.setProperty("java.rmi.server.hostname",local.getHostName());
		System.setProperty("java.rmi.useLocalHostname","true");
		port = Integer.parseInt(p.getProperty("service.rmi.port"));
		Registry registry = LocateRegistry.createRegistry(port);
		Logging.info("RMI Registry Created: " + local.getHostName() + ":" + port);
	    }
	catch (Exception e)
	    {
		Logging.warning("Problems Creating Registry");
		Logging.warning(xp.parseException(e));
	    }
    }
    
    public RMIService(int port)
    {
	this.port = port;
	try
	    {
		java.net.InetAddress local = java.net.InetAddress.getLocalHost();
		System.setProperty("java.rmi.server.hostname",local.getHostName());
		System.setProperty("java.rmi.useLocalHostname","true");
		Registry registry = LocateRegistry.createRegistry(port);
		Logging.info("RMI Registry Created: " + local.getHostName() + ":" + port);
	    }
	catch (Exception e)
	    {
		Logging.warning("Problems Creating Registry");
		Logging.warning(xp.parseException(e));
	    }
    }
    
    public void run()
    {
	while(true)
	    {
		try
		    {
			Thread.sleep(60 * 60 * 1000);
		    }
		catch (Exception ignore)
		    {
		    }
	    }
    }
}

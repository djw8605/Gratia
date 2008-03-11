package net.sf.gratia.services;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.io.*;
import java.util.*;

import java.rmi.*;
import java.net.*;

public class Slave extends Thread
{
    public static int count = 0;
    public int mycount = 0;
    
    //
    // internal stuff
    //
    
    Properties p;
    Socket socket;
    byte[] buffer = new byte[4096];
    
    
    public Slave(Socket socket)
    {
	p = Configuration.getProperties();
	this.socket = socket;
    }
    
    public void mysleep(long time)
    {
	try
	    {
		Thread.sleep(time);
	    }
	catch (Exception ignore)
	    {
	    }
    }
    
    public void run()
    {
	try
	    {
		String xml = readSocket();
		if (xml == null)
		    {
			writeSocket("Error: No Data");
			closeSocket();
			return;
		    }
		if (xml.length() == 0)
		    {
			writeSocket("Error: No Data");
			closeSocket();
			return;
		    }
		JMSProxy proxy = (JMSProxy) Naming.lookup(p.getProperty("service.rmi.rmilookup") +
							  p.getProperty("service.rmi.service"));
		proxy.update(xml);
		writeSocket("OK");
		closeSocket();
		Slave.count++;
		Logging.info("Slave: Record: " + Slave.count);
	    }
	catch (Exception e)
	    {
		e.printStackTrace();
	    }
    }
    
    //
    // socket handling
    //
    
    public String readSocket()
    {
	try
	    {
		DataInputStream input = new DataInputStream(socket.getInputStream());
		
		int count = input.read(buffer);
		String request = new String(buffer,0,count);
		return request;
	    }
	catch (Exception e)
	    {
		return null;
	    }
    }
    
    public void writeSocket(String message) throws Exception
    {
	if (message == null)
	    return;
	DataOutputStream output = new DataOutputStream(socket.getOutputStream());
	output.write(message.getBytes(),0,message.getBytes().length);
    }
    
    public void closeSocket()
    {
	try
	    {
		socket.close();
	    }
	catch (Exception ignore)
	    {
	    }
    }
    
}

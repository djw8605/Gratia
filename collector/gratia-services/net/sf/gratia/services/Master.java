package net.sf.gratia.services;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.util.*;
import java.net.*;

public class Master extends Thread
{
		Hashtable global;
		ServerSocket master;
		int port;

		public void sleep(int milliseconds)
		{
				try
						{
								Thread.sleep(milliseconds);
						}
				catch (Exception e)
						{
						}
		}
		
		public Master()
		{
				try
						{
								//
								// get configuration properties
								//

								Properties p = Configuration.getProperties();
								port = Integer.parseInt(p.getProperty("service.master.port"));
								master = new ServerSocket(this.port);
								Logging.log("");
								Logging.log("Master Started On Port: " + port);
								Logging.log("");
						}
				catch (Exception e)
						{
								Logging.log("XMasterSocket: Error Opening ServerSocket");
								e.printStackTrace();
						}

		}

		public void run()
		{
				while (true)
						{
								try
										{
												Socket socket = master.accept();
												Slave slave = new Slave(socket);
												slave.start();
										}
								catch (Exception ignore)
										{
										}

						}
		}
		
}

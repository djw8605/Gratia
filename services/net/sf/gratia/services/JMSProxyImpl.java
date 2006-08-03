package net.sf.gratia.services;

import java.rmi.*;
import java.rmi.server.*;
import javax.jms.*;
import java.util.Hashtable;
import java.util.Properties;
import java.sql.*;
import java.io.*;

import org.hibernate.SessionFactory;

public class JMSProxyImpl extends UnicastRemoteObject implements JMSProxy
{
		private Queue queue1 = null;
		private Queue queue2 = null;

		private QueueConnectionFactory factory = null;

		private SessionFactory hibernateFactory;

		private String rmilookup;
		private String service;
		private String driver;
		private String url;
		private String user;
		private String password;

		Hashtable pumps = new Hashtable();

		public JMSProxyImpl(QueueConnectionFactory factory,Queue queue1,Queue queue2,SessionFactory hibernateFactory) throws RemoteException
		{
				super();
				this.factory = factory;
				this.queue1 = queue1;
				this.queue2 = queue2;
				this.hibernateFactory = hibernateFactory;
				loadProperties();
		}

		public void loadProperties()
		{
				try
						{
								Properties p = Configuration.getProperties();
								rmilookup = p.getProperty("service.rmi.rmilookup");
								service = p.getProperty("service.rmi.service");
								driver = p.getProperty("service.mysql.driver");
								url = p.getProperty("service.mysql.url");
								user = p.getProperty("service.mysql.user");
								password = p.getProperty("service.mysql.password");
						}
				catch (Exception ignore)
						{
						}
		}

		public boolean update(String xml) throws RemoteException
		{
				try
						{
								QueueConnection queueConnection = factory.createQueueConnection();
								QueueSession queueSession = (QueueSession) queueConnection.createQueueSession(true, 0);
								MessageProducer messageProducer = (MessageProducer) queueSession.createProducer(null);
								TextMessage msg = (TextMessage) queueSession.createTextMessage();
								msg.setText(xml);
								messageProducer.send(queue1,msg);
								queueSession.commit();
								queueConnection.close();
								return true;
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
				return false;
		}

		public boolean remoteUpdate(String from,long dbid,String xml,String rawxml,String extraxml) throws RemoteException
		{
				try
						{
								Logging.log("RemoteUpdate: From: " + from + " DBID: " + dbid);
								Hashtable accountingTable = Configuration.getAccountingTable();
								QueueConnection queueConnection = factory.createQueueConnection();
								QueueSession queueSession = (QueueSession) queueConnection.createQueueSession(true, 0);
								MessageProducer messageProducer = (MessageProducer) queueSession.createProducer(null);
								TextMessage msg = (TextMessage) queueSession.createTextMessage();
								msg.setText(xml);
								msg.setStringProperty("rawxml",rawxml);
								msg.setStringProperty("extraxml",extraxml);
								msg.setStringProperty("dbid","" + dbid);
								messageProducer.send(queue1,msg);
								queueSession.commit();
								queueConnection.close();
								accountingTable.put(from,new Long(dbid));
								Configuration.saveAccountingTable(accountingTable);
								Logging.log("After: " + accountingTable);
								return true;
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
				return false;
		}

		public boolean statusUpdate(String xml) throws RemoteException
		{
				try
						{
								QueueConnection queueConnection = factory.createQueueConnection();
								QueueSession queueSession = (QueueSession) queueConnection.createQueueSession(true, 0);
								MessageProducer messageProducer = (MessageProducer) queueSession.createProducer(null);
								TextMessage msg = (TextMessage) queueSession.createTextMessage();
								msg.setStringProperty("xml",xml);
								messageProducer.send(queue2,msg);
								queueSession.commit();
								queueConnection.close();
								return true;
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
				return false;
		}

}

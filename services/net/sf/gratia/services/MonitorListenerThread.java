package net.sf.gratia.services;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public class MonitorListenerThread extends Thread
{
		Hashtable global;
		Properties p;

		public MonitorListenerThread(Hashtable global)
		{
				this.global = global;
				loadProperties();
				
				Logging.log("");
				Logging.log("MonitorListenerThread: Started");
				Logging.log("");
		}

		public void loadProperties()
		{
				p = Configuration.getProperties();
		}	

		public void run()
		{
				while (true)
						loop();
		}

		public void loop()
		{
				String waitPeriod = p.getProperty("monitor.listener.wait");
				long sleep = Long.parseLong(waitPeriod);
				sleep = sleep * (60 * 1000);
				try
						{
								Thread.sleep(sleep);
						}
				catch (Exception ignore)
						{
						}
				Date now = new Date();
				Date listener = (Date) global.get("listener");
				//
				// if listener == null we haven't updated anything
				//
				if (listener == null)
						{
								sendMessage();
								return;
						}
				//
				// if (listener + sleep) < now we haven't updated within time period
				//
				if ((listener.getTime() + sleep) < now.getTime())
						{
								sendMessage();
								return;
						}
		}

		public void sendMessage()
		{
				Logging.log("");
				Logging.log("MonitorListenerThread: Possible Listener Wedge");
				Logging.log("");
				try
						{
								Properties props = new Properties();
								if (p.getProperty("monitor.smtp.authentication.required").equals("true"))
										{
												props.put("mail.smtp.auth", "true");
												props.put("mail.user",p.getProperty("monitor.smtp.user"));
												props.put("mail.password",p.getProperty("monitor.smtp.password"));
										}
								Session session = Session.getDefaultInstance(props, null);
								MimeMessage message = new MimeMessage(session);
								message.setText("Possible Listener Error: Wedged");
								message.setSubject(p.getProperty("monitor.subject"));
								Address fromAddress = new InternetAddress(p.getProperty("monitor.from.address"));
								Address toAddress = new InternetAddress(p.getProperty("monitor.to.address.0"));
								message.setFrom(fromAddress);
								message.addRecipient(Message.RecipientType.TO,toAddress);
								Transport transport = session.getTransport("smtp");
								transport.connect(p.getProperty("monitor.smtp.server"),"glr","lisp01");
								transport.sendMessage(message, message.getAllRecipients());
								transport.close();
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}
}

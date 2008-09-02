package net.sf.gratia.services;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

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
	
        Logging.info("MonitorListenerThread: Started");
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
        String cr = "\n";
        
        Logging.warning("MonitorListenerThread: no records processed for last " +
                        p.getProperty("monitor.listener.wait") +
                        " minutes." );
        p = Configuration.getProperties();
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
                String textMessage = cr +
                    "The MonitorListenerThread has detected a possible problem. There have been" + cr +
                    "no input records processed in the past " + p.getProperty("monitor.listener.wait") +
                    " minutes. Please check the service." + cr;
                message.setText(textMessage);
                message.setSubject(p.getProperty("monitor.subject", "Gratia collector problems"));
                String mailUsername = null;
                if (p.getProperty("monitor.from.address") == null) {
                    Logging.warning("MonitorListenerThread unable to send " +
                                    "inactivity warning via email because " +
                                    "monitor.from.address property is not set!");
                    return;
                } else if (p.getProperty("monitor.to.address.0") == null) {
                    Logging.warning("MonitorListenerThread unable to send " +
                                    "inactivity warning via email because " +
                                    "monitor.to.address.0 property is not set!");
                    return;
                } else {
                    mailUsername = p.getProperty("monitor.from.address");
                    mailUsername = mailUsername.split("\\.", 1)[0];
                }
                Address fromAddress = new InternetAddress(p.getProperty("monitor.from.address"));
                Address toAddress = new InternetAddress(p.getProperty("monitor.to.address.0"));
                message.setFrom(fromAddress);
                message.addRecipient(Message.RecipientType.TO,toAddress);
                for (int i = 1; i < 100; i++)
                    {
                        String temp = p.getProperty("monitor.to.address." + i);
                        if (temp != null)
                            {
                                Address address = new InternetAddress(temp);
                                message.addRecipient(Message.RecipientType.TO,address);
                            }
                    }
                Transport transport = session.getTransport("smtp");
                transport.connect(p.getProperty("monitor.smtp.server"), mailUsername, null);
                transport.sendMessage(message, message.getAllRecipients());
                transport.close();
            }
        catch (Exception e)
            {
                e.printStackTrace();
            }
    }
}

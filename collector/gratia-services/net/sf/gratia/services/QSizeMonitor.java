package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.util.*;

//import java.lang.management.*; 
import javax.management.*; 
import javax.management.remote.*;

//
// notes and asides - this class will check each of the defined q's and stop the input service if the qsize > max.q.size
//
// in order to work the following options must be passed to the jvm prior to starting tomcat
//
// -Dcom.sun.management.jmxremote -> turns on jmx management
// -Dcom.sun.management.jmxremote.port=8004 -> where I can connect - must be unique per tomcat instance
// -Dcom.sun.management.jmxremote.authenticate=false -> no headaches
// -Dcom.sun.management.jmxremote.ssl=false -> no headahces
// -Dssl.port=8443 -> the ssl port to manage - must match that in server.xml
//
// note that this will only work with the 5.x series of tomcat
//

public class QSizeMonitor extends Thread {

    boolean running = true;
    Properties p;
    String path = "";
    int maxthreads = 0;
    int maxqsize = 0;
    static final double restartThreshold = 0.8;

    public QSizeMonitor() {
        p = Configuration.getProperties();
        maxthreads = Integer.parseInt(p.getProperty("service.listener.threads"));
        maxqsize = Integer.parseInt(p.getProperty("max.q.size"));
        path = System.getProperties().getProperty("catalina.home");
        path = path.replaceAll("\\\\", "/");
    }

    public void run() {
        Logging.fine("QSizeMonitor: Started");
        while (true) {
            try {
                Thread.sleep(60 * 1000);
            }
            catch (Exception e) {
            }
            check();
        }
    }

    public void check() {
        boolean toobig = false;
        int maxfound = 0;

        Logging.log("QSizeMonitor: Checking");
        for (int i = 0; i < maxthreads; i++) {
            String xpath = path + "/gratia/data/thread" + i;
            String filelist[] = XP.getFileList(xpath);
            if (filelist.length > maxqsize)
                toobig = true;
            if (filelist.length > maxfound)
                maxfound = filelist.length;
        }
        if (toobig && running) {
            Logging.info("QSizeMonitor: Q Size Exceeded: " + maxfound);
            Logging.info("QSizeMonitor: Shuttinng Down Input");
            stopServlet();
            running = false;
            return;
        }
        if ((! toobig) && (! running)) {
            if (maxfound < (maxqsize * restartThreshold)){
                Logging.info("QSizeMonitor: Restarting Input: " + maxfound);
                startServlet();
                running = true;
            }
        }
    }

    void startServlet() {
        servletCommand("start");
    }

    void stopServlet() {
        servletCommand("stop");
    }

    void servletCommand(String cmd) {
        String bean1 = "Catalina:j2eeType=WebModule,name=//localhost/gratia-servlets,J2EEApplication=none,J2EEServer=none";
        String urlstring = "service:jmx:rmi:///jndi/rmi://localhost:xxxx/jmxrmi";

        if (System.getProperty("com.sun.management.jmxremote.port") == null) {
            return;
        }

        urlstring = urlstring.replace("xxxx", System.getProperty("com.sun.management.jmxremote.port"));
				
        JMXConnector jmxc = null;

        try {						
            JMXServiceURL url = new JMXServiceURL(urlstring); 
            jmxc = JMXConnectorFactory.connect(url,null); 
            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection(); 
            ObjectName objectName = new ObjectName(bean1);
            //
            // now call
            //
            mbsc.invoke(objectName, cmd, null, null);
        }
        catch (Exception e) {
            Logging.warning("CollectorService: ServletCommand(\"" +
                            cmd + "\") caught exception ", e);
        }
        finally {
            try {
                jmxc.close();
            }
            catch (Exception ignore) {
            }
        }
    }

}


 

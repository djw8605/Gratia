package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;
import net.sf.gratia.util.LogLevel;

import java.util.*;

import java.lang.management.ManagementFactory; 
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
        long maxfound = 0;

        Logging.log("QSizeMonitor: Checking");
        for (int i = 0; i < maxthreads; i++) {
            String xpath = path + "/gratia/data/thread" + i;
            long nfiles = XP.getFileNumber(xpath);
            if (nfiles > maxqsize)
                toobig = true;
            if (nfiles > maxfound)
                maxfound = nfiles;
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

    final String bean_servlets = "Catalina:j2eeType=WebModule," +
        "name=//localhost/gratia-servlets,J2EEApplication=none,J2EEServer=none";

    void startServlet() {
        servletCommand(bean_servlets,"start",false);
    }

    void stopServlet() {
        servletCommand(bean_servlets,"stop",false);
    }

    static Boolean servletCommand(String bean_name, String cmd, boolean ignore_missing) {
        Boolean result = false;

        try {
            MBeanServerConnection mbsc = null;
            if (System.getProperty("com.sun.management.jmxremote") == null) {
                Logging.log(LogLevel.SEVERE, "CollectorService: internal servlet " +
                            "control is not available." +
                            " Please ensure that the system property " +
                            "com.sun.management.jmxremote" +
                            " is set to allow required control of " +
                            "servlets and reporting service.");
                return result; // No point in continuing
            }
            Logging.log("CollectorService: attempting to obtain local MBeanServer");
            mbsc = ManagementFactory.getPlatformMBeanServer();

            ObjectName objectName = new ObjectName(bean_name);

            mbsc.invoke(objectName, cmd, null, null);
            Logging.log("CollectorService: successfully executed MBean control command " +
                        cmd + " on MBean " + bean_name);
            result = true;
        }
        catch (javax.management.InstanceNotFoundException missing) {
            // We might want to ignore this error.
            if (ignore_missing) {
                result = true; // Didn't care whether it was there or not.
            } else {
                Logging.warning("CollectorService: ServletCommand(\"" +
                                cmd + "\") caught exception " + missing);
                Logging.debug("Exception details: ", missing);
            }
        }
        catch (Exception e) {
            Logging.warning("CollectorService: ServletCommand(\"" +
                            cmd + "\") caught exception " + e);
            Logging.debug("Exception details: ", e);
        }
        return result;
    }
}


 

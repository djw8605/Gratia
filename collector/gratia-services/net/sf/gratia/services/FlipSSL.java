package net.sf.gratia.services;

import net.sf.gratia.util.Logging;

import java.util.*;

import javax.management.*;
import javax.management.remote.*;

//
// notes and asides - this class will reset the tomcat https connection after updates are made to either keystore or truststore
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

public class FlipSSL {

    public static boolean flip() {
        boolean result = false;

        if (System.getProperty("com.sun.management.jmxremote.port") == null)
            return result;

        String urlstring = "service:jmx:rmi:///jndi/rmi://localhost:xxxx/jmxrmi";
        String mbeanName = "Catalina:type=Connector,port=xxxx";

        urlstring = urlstring.replace("xxxx", System
                .getProperty("com.sun.management.jmxremote.port"));
        mbeanName = mbeanName.replace("xxxx", System.getProperty("ssl.port"));

        JMXConnector jmxc = null;

        try {

            JMXServiceURL url = new JMXServiceURL(urlstring);
            jmxc = JMXConnectorFactory.connect(url, null);
            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

            Set mBeanSet = mbsc.queryMBeans(null, null);
            Iterator mBeanSetIterator = mBeanSet.iterator();
            while (mBeanSetIterator.hasNext()) {
                ObjectInstance objectInstance = (ObjectInstance) mBeanSetIterator
                        .next();
                ObjectName objectName = objectInstance.getObjectName();
                if (objectName.toString().equals(mbeanName)) {
                    //
                    // now call
                    //
                    Logging.log("Flipping");
                    mbsc.invoke(objectName, "stop", null, null);
                    mbsc.invoke(objectName, "start", null, null);
                    jmxc.close();
                    return true;
                }
            }
            Logging.log("Couldn't Find: " + mbeanName);
            jmxc.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                jmxc.close();
            } catch (Exception ignore) {
            }
            return result;
        }
    }
}

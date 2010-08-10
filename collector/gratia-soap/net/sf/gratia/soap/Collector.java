package net.sf.gratia.soap;

import net.sf.gratia.util.Configuration;

import java.util.*;
import java.io.*;
import java.rmi.*;
import net.sf.gratia.services.*;

/**
 * @author Tim Byrne
 *
 * Collector
 * 
 * The Collector class represents the logic required for a Collector web service to receive usage xml
 *  and persist it both to file and to a data source.
 */

public class Collector {	
    public String rmilookup;

    /**
     * collectUsageXml
     * 
     * Exposed as a web service to take an 'Event' parameter, which is made up of a meter id and usage xml.
     *  The usage xml is parsed, validated and saved to a data file.  The data file is then loaded into a 
     *  storage structure and persisted to a data source.  If the persist succeeds, then the data file is 
     *  removed from the system.
     *  
     * @param event
     *  A fully populated Event object must be provided to specify the source of the data (meter id) and the
     *   usage xml to persist.
     * @return
     *  A fully populated Response object is returned to the caller to specify success or failure, and any 
     *   messages for the caller to display or log.
     */

    public Collector()
    {
        Properties p = Configuration.getProperties();
        rmilookup = p.getProperty("service.rmi.rmilookup") + p.getProperty("service.rmi.service");
    }

    public Response collectUsageXml(Event event)
    {	
        Response response = new Response();

        String xml = event.get_xml();
        String id = event.get_id();

        String host = "localhost";
        JMSProxy proxy = null;
        boolean status = false;

        if (xml.length() == 0)
            {
                response.set_code(1);
                response.set_message("Error: No data!");
                return response;
            }

        try
            {
                proxy = (JMSProxy) Naming.lookup(rmilookup);
            }
        catch (Exception e)
            {
                e.printStackTrace();
                response.set_code(1);
                response.set_message("Error: Unable to contact JMS");
                return response;
            }

        try
            {
								
                status = proxy.update("viasoap",xml);
                if (! status)
                    {
                        response.set_code(1);
                        response.set_message("Error: Check Server Logs");
                        return response;
                    }
            }
        catch (Exception e)
            {
                e.printStackTrace();
                response.set_code(1);
                response.set_message("Error: " + e);
                return response;
            }
        response.set_code(0);
        response.set_message("OK");
        return response;
    }
}		

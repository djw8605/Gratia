package net.sf.gratia.servlets;

import net.sf.gratia.services.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import java.rmi.*;

public class RMIHandlerServlet extends HttpServlet 
{
    public Properties p;
    public JMSProxy proxy = null;
    XP xp = new XP();
    static URLDecoder D;

    public void init(ServletConfig config) throws ServletException 
    {
        super.init(config);
        p = Configuration.getProperties();

        //
        // initialize logging
        //

        Logging.initialize(p.getProperty("service.rmiservlet.logfile"),
                           p.getProperty("service.rmiservlet.maxlog"),
                           p.getProperty("service.rmiservlet.console"),
                           p.getProperty("service.rmiservlet.level"));
    }
    
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException 
    {
        String command = null;
        String from = null;
        String to = null;
        String rmi = null;
        String arg1 = null;
        String arg2 = null;
        String arg3 = null;
        String arg4 = null;

        int argcount = 0;

        try
            {
                proxy = (JMSProxy) Naming.lookup(p.getProperty("service.rmi.rmilookup") +
                                                 p.getProperty("service.rmi.service"));
            }
        catch (Exception e)
            {
                Logging.warning(xp.parseException(e));
            }

        try 
            {
                command = req.getParameter("command");
                
                if (command == null) {
                    Logging.log("RMIHandlerServlet got buggy POST: remediating");
                    //
                    // the following is a hack to get around a python post issue
                    //
                    ServletInputStream input = req.getInputStream();
                    byte buffer[] = new byte[4 * 4096];
                    int icount = 0;
                    int istatus = 0;
                    for (icount = 0; icount < buffer.length; icount++)
                        {
                            istatus = input.read(buffer,icount,1);
                            if (istatus == -1)
                                break;
                        }
                    String body = new String(buffer,0,icount);
                    Logging.debug("RMIHandlerServlet: body = " + body);

                    StringTokenizer st1 = new StringTokenizer(body,"&");
                    boolean swallowAll = false;
                    while(st1.hasMoreTokens())
                        {
                            String token = st1.nextToken();
                            if (swallowAll) { // Swallow everything from here on in
                                arg1 += "&" + token;
                                continue;
                            }
                            int index = token.indexOf("=");
                            if (index < 0) {
                                Logging.info("RMIHandlerServlet: warning: token = " + token);
                            }
                            String key = token.substring(0,index);
                            String value = token.substring(index + 1);
                            key = key.toLowerCase();
                            if ((command == null) && key.equals("command")) {
                                // Only if command is still null
                                Logging.debug("RMIHandlerServlet: setting command = " + value);
                                command = value.toLowerCase();
                            } else if (key.equals("from")) {
                                Logging.debug("RMIHandlerServlet: setting from = " + value);
                                from = maybeURLDecode(command, value);
                            } else if (key.equals("to")) {
                                Logging.debug("RMIHandlerServlet: setting to = " + value);
                                to = maybeURLDecode(command, value);
                            } else if (key.equals("rmi")) {
                                Logging.debug("RMIHandlerServlet: setting rmi = " + value);
                                rmi = maybeURLDecode(command, value);
                            } else if (key.equals("arg1")) {
                                Logging.debug("RMIHandlerServlet: setting arg1 = " + value);
                                arg1 = value;
                                // Check for old command construction and rescue
                                if ((command != null) && command.equals("update")) {
                                    Logging.log("RMIHandlerServlet: setting swallowAll to true");
                                    swallowAll = true;
                                }
                            } else if (key.equals("arg2")) {
                                arg2 = maybeURLDecode(command, value);
                                Logging.debug("RMIHandlerServlet: setting arg2 = " + arg2);
                            } else if (key.equals("arg3")) {
                                arg3 = maybeURLDecode(command, value);
                                Logging.debug("RMIHandlerServlet: setting arg3 = " + arg3);
                            } else if (key.equals("arg4")) {
                                arg4 = maybeURLDecode(command, value);
                                Logging.debug("RMIHandlerServlet: setting arg4 = " + arg4);
                            }
                        }
                    arg1 = maybeURLDecode(command, arg1);
                } else {
                    // getParameter already handles URLEncoded data.
                    command = command.toLowerCase();
                    from = req.getParameter("from");
                    to = req.getParameter("to");
                    rmi = req.getParameter("rmi");
                    arg1 = req.getParameter("arg1");
                    arg2 = req.getParameter("arg2");
                    arg3 = req.getParameter("arg3");
                    arg4 = req.getParameter("arg4");
                }

                if (arg1 != null)
                    argcount++;
                if (arg2 != null)
                    argcount++;
                if (arg3 != null)
                    argcount++;
                if (arg4 != null)
                    argcount++;

                Logging.log("RMIHandlerServlet: From: " + from);
                Logging.log("RMIHandlerServlet: To: " + to);
                Logging.log("RMIHandlerServlet: RMI: " + rmi);
                Logging.log("RMIHandlerServlet: Command: " + command);
                Logging.log("RMIHandlerServlet: Argcount: " + argcount);
                Logging.log("RMIHandlerServlet: Arg1: " + arg1);
                Logging.log("RMIHandlerServlet: Arg2: " + arg2);
                Logging.log("RMIHandlerServlet: Arg3: " + arg3);
                Logging.log("RMIHandlerServlet: Arg4: " + arg4);

                //
                // the - connect to rmi
                //

                PrintWriter writer = res.getWriter();

                if ((command.equals("update") ||
                     command.equals("urlencodedupdate") ||
                     command.endsWith("handshake")) && (argcount == 1))
                    {
                        boolean status = proxy.update(arg1);
                        if (status)
                            writer.write("OK");
                        else
                            writer.write("Error");
                    }
                else if ((command.equals("statusupdate")) && (argcount == 2))
                    {
                        if (arg1.equals("status"))
                            {
                                boolean status = proxy.handshake(arg2);
                                if (status)
                                    writer.write("OK");
                                else
                                    writer.write("Error");
                            }
                    }
                else
                    {
                        Logging.info("RMIHandlerServlet: Error: Unknown Command: " + command + " Or Invalid Arg Count: " + argcount);
                        writer.write("Error: Unknown Command: " + command + " Or Invalid Arg Count: " + argcount);
                    }
                writer.flush();
            }
        catch (Exception e)
            {
                e.printStackTrace();
            }
    }

    private String maybeURLDecode(String command, String value) throws Exception {
        if ((command == null) || command.startsWith("urlencoded")) {
            return D.decode(value, "UTF-8");
        } else {
            return value;
        }
    }

}

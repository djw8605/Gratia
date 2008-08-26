package net.sf.gratia.servlets;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import net.sf.gratia.services.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import java.rmi.*;

public class RMIHandlerServlet extends HttpServlet 
{
    Properties p;
    static JMSProxy proxy = null;
    static URLDecoder D;

    private synchronized void lookupProxy() {
        while (proxy == null) {
            try {
                proxy = (JMSProxy) Naming.lookup(p.getProperty("service.rmi.rmilookup") +
                                                 p.getProperty("service.rmi.service"));
            }
            catch (Exception e) {
                Logging.warning("RMIHandlerServlet caught exception doing RMI lookup: ", e);
                try {
                    Thread.sleep(5000);
                } catch (Exception ignore) {
                }
            }
        }
    }

    public void init(ServletConfig config)
        throws ServletException {
        TimeZone.setDefault(null);
        super.init(config);
        p = Configuration.getProperties();

        //
        // initialize logging
        //

        Logging.initialize("rmiservlet");
    }
    
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        String command = null;
        String from = null;
        String to = null;
        String rmi = null;
        String arg1 = null;
        String arg2 = null;
        String arg3 = null;
        String arg4 = null;

        int argcount = 0;

        lookupProxy();

        if (!proxy.servletEnabled()) {
            PrintWriter writer = res.getWriter();
            writer.write("Error: service not ready.");
            writer.flush();
            return;
        }

        try {
            Logging.debug("RMIHandlerServlet debug diagnostics for req: " +
                          req +
                          ", originating server: " +
                          req.getRemoteHost() +
                          ", headers: \n" + requestDiagnostics(req));
            command = req.getParameter("command");

            if (command == null) {
                Logging.fine("RMIHandlerServlet got buggy POST from " +
                            req.getRemoteHost() +
                            ": remediating");
                //
                // the following is a hack to get around a python post issue
                //
                // As new probes get out into the wild, this code will
                // be called less and less often.
                ServletInputStream input = req.getInputStream();
                int icount = 0;
                int loopcount = 0;
                int maxloops = 10;
                String body = new String("");
                int bcount = 0;
                byte buffer[];
                try {
                    do {
                        bcount = 0;
                        buffer = new byte[4 * 4096];
                        int istatus = 0;
                        for (bcount = 0; bcount < buffer.length; ++bcount, ++icount) {
                            istatus = input.read(buffer, bcount, 1);
                            if (istatus == -1) {
                                break;
                            }
                        }
                        body += new String(buffer, 0, bcount);
                    } while ((bcount == buffer.length) && (++loopcount < maxloops));
                    if (loopcount == maxloops) {
                        Logging.warning("RMIHanderservlet: record exceeds maximum buffer size of " +
                                        loopcount * buffer.length + " bytes!");
                        return;
                    }
                    Logging.debug("RMIHandlerServlet: body = " + body);
                }
                catch (Exception e) {
                    Logging.warning("RMIHandlerServlet: Error: Problematic req: " +
                                    req +
                                    ", originating server: " +
                                    req.getRemoteHost() +
                                    "\nData received so far:\n" +
                                    body);
                    Logging.debug("RMIHandlerServlet error diagnostic for req: " +
                                req +
                                ": read parameters: iteration " +
                                loopcount +
                                ", buffer position " +
                                bcount + 
                                ", input position "
                                + icount);
                    Logging.debug("RMIHandlerServlet error diagnostic for req: " +
                                req +
                                ", headers: \n" + requestDiagnostics(req));
                    Logging.debug("Exception detail:", e);
                    PrintWriter writer = res.getWriter();
                    writer.write("Error: RMIHandlerServlet: Error: Problematic req: " + req);
                    writer.flush();
                    return;
                }
                StringTokenizer st1 = new StringTokenizer(body,"&");
                boolean swallowAll = false;
                while(st1.hasMoreTokens()) {
                    String token = st1.nextToken();
                    if (swallowAll) { // Swallow everything from here on in
                        arg1 += "&" + token;
                        continue;
                    }
                    int index = token.indexOf("=");
                    if (index < 0) {
                        Logging.warning("RMIHandlerServlet: warning: token = " + token);
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

            Logging.debug("RMIHandlerServlet: From: " + from);
            Logging.debug("RMIHandlerServlet: To: " + to);
            Logging.debug("RMIHandlerServlet: RMI: " + rmi);
            Logging.debug("RMIHandlerServlet: Command: " + command);
            Logging.debug("RMIHandlerServlet: Argcount: " + argcount);
            Logging.debug("RMIHandlerServlet: Arg1: " + arg1);
            Logging.debug("RMIHandlerServlet: Arg2: " + arg2);
            Logging.debug("RMIHandlerServlet: Arg3: " + arg3);
            Logging.debug("RMIHandlerServlet: Arg4: " + arg4);

            //
            // the - connect to rmi
            //

            PrintWriter writer = res.getWriter();
            boolean parse_error = true;
            if (argcount == 1) {
                if (command.equals("update") ||
                    command.equals("urlencodedupdate")) {
                    parse_error = false;
                    boolean status = true;
                    if (arg1.equals("xxx")) {
                        Logging.info("RMIHandlerServlet: received test message from " +
                                     req.getRemoteHost());
                    } else { // Process normally
                        // First attempt to fix an error in data
                        // replicated from old collectors.
                        arg1 = arg1.ReplaceAll("<LocalJobId ><undefined></LocalJobId>",
                                               "<LocalJobId >&lt;undefined&gt;</LocalJobId>");
                        status = proxy.update(arg1);
                    }
                    if (status) {
                        writer.write("OK");
                    } else {
                        writer.write("Error"); 
                    }
                }
            }
            if (parse_error) {
                Logging.warning("RMIHandlerServlet: Error: Unknown Command: " +
                                command + " Or Invalid Arg Count: " + argcount);
                writer.write("Error: Unknown Command: " + command +
                             " or Invalid Arg Count: " + argcount);
            }
            writer.flush();
        }
        catch (Exception e) {
            Logging.warning("RMIHandlerServlet: Error: Problematic req: " +
                            req +
                            ", originating server: " +
                            req.getRemoteHost());
            Logging.debug("RMIHandlerServlet error diagnostic for req: " +
                          req +
                          ", headers: \n" + requestDiagnostics(req));
            Logging.debug("Exception detail:", e);
            PrintWriter writer = res.getWriter();
            writer.write("Error: RMIHandlerServlet: Error: Problematic req: " + req);
            writer.flush();
        }
    }

    private String maybeURLDecode(String command, String value) throws Exception {
        if ((command == null) || command.startsWith("urlencoded")) {
            return D.decode(value, "UTF-8");
        } else {
            return value;
        }
    }

    private String requestDiagnostics(HttpServletRequest req) {
        Enumeration hNameList = req.getHeaderNames();
        String hList = new String("");
        while (hNameList.hasMoreElements()) {
            if (hList.length() != 0) {
                hList += ", ";
            }
            String hName = (String) hNameList.nextElement();
            hList += hName + ": " + req.getHeader(hName);
        }
        return hList;
    }
}

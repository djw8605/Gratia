package net.sf.gratia.services;

import net.sf.gratia.util.Logging;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

import javax.naming.*;

public class TestServlet extends HttpServlet {

        static final long serialVersionUID = 1;
        
		static
		{
				Logging.log("");
				Logging.log("Test Loaded");
				Logging.log("");
				try
						{
								Process p = Runtime.getRuntime().exec("java bsh.Console");
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}

				Enumeration e = System.getProperties().propertyNames();
				Logging.log("");
				while(e.hasMoreElements())
						{
								String key = (String) e.nextElement();
                if (key.endsWith(".password")) continue;
								String value = (String) System.getProperty(key);
								Logging.log("Key: " + key + " value: " + value);
						}
				Logging.log("");

				Logging.log("");
				Logging.log("Testing Context");
				Logging.log("");

				try
						{
								Context context = new InitialContext();

								context.bind("key1","value1");
								context.bind("key2","value2");
								context.close();
						}
				catch (Exception error)
						{
						}

				try
						{
								Context context = new InitialContext();
								String value1 = (String) context.lookup("key1");
								Logging.log("value1: " + value1);
								context.close();
						}
				catch (Exception error)
						{
								error.printStackTrace();
						}

				Logging.log("");
				Logging.log("Starting CollectorService");
				Logging.log("");

				CollectorService service = new CollectorService();
				service.contextInitialized(null);

				Logging.log("");
				Logging.log("CollectorService Started");
				Logging.log("");

		}

    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Hello World!</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>Hello World!</h1>");
        out.println("</body>");
        out.println("</html>");
    }
}

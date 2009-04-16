package net.sf.gratia.administration;

import net.sf.gratia.util.XP;
import net.sf.gratia.util.Logging;
import net.sf.gratia.util.Execute;
import net.sf.gratia.util.Configuration;

import net.sf.gratia.services.*;
import net.sf.gratia.storage.Connection;

import java.io.*;

import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;

import java.util.regex.*;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.*;

public class ConnectionTable extends HttpServlet 
   {
      XP xp = new XP();
      
      //
      // processing related
      //
      String fHtml = "";
      String fRow = "";
      String fError = "";
      static Pattern gRowPattern = Pattern.compile("<tr><form .*?</tr>",Pattern.MULTILINE + Pattern.DOTALL);
      StringBuffer buffer = new StringBuffer();
      Hashtable<Long,Connection> fRepTable = null;
      //
      // globals
      //
      HttpServletRequest fRequest;
      HttpServletResponse fResponse;
      boolean fInitialized = false;
      
      JMSProxy fCollectorProxy = null;
      
      JMSProxy getCollectorProxy() throws Exception
      {
         if (fCollectorProxy!=null) return fCollectorProxy;
         
         int loop = 0;
         Properties p = Configuration.getProperties();
         while (fCollectorProxy == null) {
            // Wait until JMS service is up
            try {
               fCollectorProxy = (JMSProxy) java.rmi.Naming.lookup(p.getProperty("service.rmi.rmilookup") +
                                                                   p.getProperty("service.rmi.service"));
            }
            catch (Exception e) {
               try {
                  Thread.sleep(5000);
               } catch (Exception ignore) {
                  if (loop > 5) {
                     Logging.warning("SystemAdministration: Caught exception during RMI lookup", e);
                     throw e;
                  }
               }
            }
         }
         return fCollectorProxy;
      }
      
      public void init(ServletConfig config) throws ServletException 
      {
         try {
            HibernateWrapper.start();
         }
         catch (Exception e) {
            Logging.warning("SystemAdministration: Caught exception during hibernate init", e);
         }
         fInitialized = true;
      }
      
      boolean checkLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
      {
         String fqan = (String) request.getSession().getAttribute("FQAN");
         boolean login = true;
         if (fqan == null)
            login = false;
         else if (fqan.indexOf("NoPrivileges") > -1)
            login = false;
         
         String uriPart = request.getRequestURI();
         int slash2 = uriPart.substring(1).indexOf("/") + 1;
         uriPart = uriPart.substring(slash2);
         String queryPart = request.getQueryString();
         if (queryPart == null)
            queryPart = "";
         else
            queryPart = "?" + queryPart;
         
         request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);
         
         if (login) {
            
            return true;
            
         } else {
            Properties p = Configuration.getProperties();
            String loginLink = p.getProperty("service.secure.connection") + request.getContextPath() + "/gratia-login.jsp";
            String redirectLocation = response.encodeRedirectURL(loginLink);
            response.sendRedirect(redirectLocation);
            request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);

            return false;
         }
      }

      void displayPage() throws IOException 
      {

         process();
         
         fResponse.setContentType("text/html");
         fResponse.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
         fResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0
         PrintWriter writer = fResponse.getWriter();
         writer.write(fHtml);
         writer.flush();
         writer.close();
         
      }
      
      public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
      {
         if (checkLogin(request,response)) {
             
            Logging.debug("ConnectionTable: doGet");
            
            setup(request,response);
            if (request.getParameter("action") != null) {
               update();
            }
            displayPage();
          }
      }
      
      public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
      {
         if (checkLogin(request,response)) {
            Logging.debug("ConnectionTable: doPost");
            
            setup(request,response);
            update();
            
            displayPage();
         }
      }

      public void setup(HttpServletRequest request, HttpServletResponse response)
      {
         this.fRequest = request;
         this.fResponse = response;
         fHtml = xp.get(fRequest.getRealPath("/") + "connectiontable.html");
         Matcher m = gRowPattern.matcher(fHtml);
         while (m.find())
         {
            String temp = m.group();
            if (temp.indexOf("#index#") > 0)
            {
               fRow = temp;
               break;
            }
         }
         loadConnections();
      }
      
      void loadConnections() {
         // Load replication entries from DB
         Session session;
         session = HibernateWrapper.getSession();
         Query rq = session.createQuery("from Connection");
         List records = rq.list();
         session.close();
         
         // Load hash table with entries
         fRepTable = new Hashtable<Long, Connection>();
         for ( Object listEntry : records ) {
            Connection repEntry = (Connection) listEntry;
            Logging.debug("ConnectionTable: loaded entry " + repEntry.getcid());
            fRepTable.put(new Long(repEntry.getcid()),
                          repEntry);
         }
      }
      
      void reportError(String summary, String details) {
         
         fHtml = fHtml.replaceAll("<!--#Error#-->",
                                  "<table border=\"0\"><tr> <td align=\"left\" ><hr color=\"#FF8330\"></td></tr>"+                          
                                  "<tr><td><font color=\"#FF8330\">"+summary+"<br>&nbsp;&nbsp;&nbsp;&nbsp;<strong>"+
                                  details+"</strong> <br></font></td></tr><tr> <td align=\"left\" ><hr color=\"#FF8330\"></td></tr></table>");
         
      }
      
      public void process()
      {
         // Loop through replication table entries.
         Enumeration<Connection> certEntries = fRepTable.elements();
         Vector<Connection> vec = new Vector<Connection>();
         while (certEntries.hasMoreElements()) {
            vec.add((Connection) certEntries.nextElement());
         }
         Collections.sort(vec); // Sort according to Replication.CompareTo().
         
         Logging.debug("ConnectionTable: processing " + vec.size() + " connections");
         buffer = new StringBuffer();
         int index = 0;
         for ( Connection entry : vec ) {
            String Sender = "N/A";
            String SenderHost = "N/A";
            String CollectorName = "N/A";
            String FirstSeen = "N/A";
            String LastSeen = "N/A";
            try {
               if (entry.getSender() != null) {
                  Sender = entry.getSender();
               }
               SenderHost = entry.getSenderHost();
               CollectorName = entry.getCollectorName();
               FirstSeen = entry.getFirstSeen().toString();
               LastSeen = entry.getLastSeen().toString();
            } catch (Exception e) {
               Logging.warning("ConnectionTable: Problem " + e);
            }

            String newrow = fRow.replaceAll("#index#","" + index)
               .replaceAll("#cid#","" + entry.getcid())
               .replaceAll("#Sender#",Sender)
               .replaceAll("#SenderHost#",SenderHost)
               .replaceAll("#CollectorName#",CollectorName)
               .replaceAll("#FirstSeen#",FirstSeen)
               .replaceAll("#LastSeen#",LastSeen)
               .replaceAll("#Status#", entry.isValid() ? "Allowed" : "Banned")
               .replaceAll("#Change#", entry.isValid() ? 
                           "<input type=\"submit\" name=\"action\" value=\"Ban\"/>" : 
                           "<input type=\"submit\" name=\"action\" value=\"Allow\"/>");
            index++;
            buffer.append(newrow);
         }
         
         fHtml = fHtml.replaceFirst(fRow,buffer.toString());
      }
      
      public void update()
      {
         String action = fRequest.getParameter("action");
         try {
                        
            if (action.equals("Ban")) {
               Long cid = Long.decode(fRequest.getParameter("cid"));
               Logging.debug("ConnectionTable: Banning :" + cid);
               setState(cid,false);
            } else if (action.equals("Allow")) {
               Long cid = Long.decode(fRequest.getParameter("cid"));
               Logging.debug("ConnectionTable: Allowing :" + cid);
               setState(cid,true);
            } else if (action.equals("BanAll")) {
               Logging.debug("ConnectionTable: Banning All");
               setAllState(false);
            } else if (action.equals("AllowAll")) {
               Logging.debug("ConnectionTable: Allowing All");
               setAllState(true);
            }
         } catch (NumberFormatException e) {
            Logging.warning("ConnectionTable: Problem when parsing cid in post: "+fRequest.getParameter("cid"));
            reportError("Internal Error",e.toString());            
         } catch (Exception e) {
            Logging.warning("ConnectionTable: Problem when handling connection: "+fRequest.getParameter("cid")+": ",e);
            reportError("Internal Error",e.toString());
         }
         
      }
      
      public void setState(Long cid, boolean isValid) throws Exception
      {
         Logging.debug("ConnectionTable: changing state of cid" + cid + " to " + (isValid ? "Allowed" : "Banned"));
         
         try {
            getCollectorProxy().setConnectionCaching(false);
            Connection cert = fRepTable.get(cid);
            Connection updated = new Connection(cert);
            updated.setValid(isValid);
         
            if (cert != null) {
               Session session = HibernateWrapper.getSession();
               Transaction tx = session.beginTransaction();
               session.saveOrUpdate( updated );
               session.flush();
               tx.commit();
               session.close();
               fRepTable.put( new Long(updated.getcid()), updated );
            }
            fCollectorProxy.setConnectionCaching(true);
         } catch (Exception e) {
            if (fCollectorProxy!=null) fCollectorProxy.setConnectionCaching(true);
            throw e;
         }
      }
      
      public void setAllState(boolean isValid) throws Exception
      {
         Logging.debug("ConnectionTable: changing state of all connections to " + (isValid ? "Allowed" : "Banned"));

         try {
            getCollectorProxy().setConnectionCaching(false);
            Session session = HibernateWrapper.getSession();
            Transaction tx = session.beginTransaction();
         
            Hashtable<Long,Connection> updatedTable = new Hashtable<Long,Connection>();
            Enumeration<Connection> certEntries    = fRepTable.elements();
            while (certEntries.hasMoreElements()) {
               Connection cert = certEntries.nextElement();
               Connection updated = new Connection(cert);
               updated.setValid(isValid);
               updatedTable.put( new Long(updated.getcid()), updated );
               session.saveOrUpdate( updated );
            }
            session.flush();
            tx.commit();
            session.close();
            fRepTable = updatedTable;
            getCollectorProxy().setConnectionCaching(true);
         } catch (Exception e) {
            if (fCollectorProxy!=null) fCollectorProxy.setConnectionCaching(true);
            throw e;
         }
      }
      
   }

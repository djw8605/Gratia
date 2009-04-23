package net.sf.gratia.administration;

import net.sf.gratia.util.XP;
import net.sf.gratia.util.Logging;
import net.sf.gratia.util.Configuration;

import net.sf.gratia.services.*;
import net.sf.gratia.storage.Certificate;

import java.io.*;

import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.servlet.*;
import javax.servlet.http.*;

import java.util.regex.*;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class CertificateTable extends HttpServlet 
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
      Hashtable<Long,Certificate> fRepTable = null;
      //
      // globals
      //
      HttpServletRequest fRequest;
      HttpServletResponse fResponse;
      boolean fInitialized = false;
      
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
             
            Logging.debug("CertificateTable: doGet");
            
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
            Logging.debug("CertificateTable: doPost");
            
            setup(request,response);
            update();
            
            displayPage();
         }
      }

      public void setup(HttpServletRequest request, HttpServletResponse response)
      {
         this.fRequest = request;
         this.fResponse = response;
         fHtml = xp.get(fRequest.getRealPath("/") + "certificatetable.html");
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
         loadCertificates();
      }
      
      void loadCertificates() {
         // Load replication entries from DB
         Session session;
         session = HibernateWrapper.getSession();
         Query rq = session.createQuery("from Certificate");
         List records = rq.list();
         session.close();
         
         // Load hash table with entries
         fRepTable = new Hashtable<Long, Certificate>();
         for ( Object listEntry : records ) {
            Certificate repEntry = (Certificate) listEntry;
            Logging.debug("CertificateTable: loaded entry " + repEntry.getCertid());
            fRepTable.put(new Long(repEntry.getCertid()),
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
         Enumeration<Certificate> certEntries = fRepTable.elements();
         Vector<Certificate> vec = new Vector<Certificate>();
         while (certEntries.hasMoreElements()) {
            vec.add((Certificate) certEntries.nextElement());
         }
         Collections.sort(vec); // Sort according to Replication.CompareTo().
         
         Logging.debug("CertificateTable: processing " + vec.size() + " certificates");
         buffer = new StringBuffer();
         int index = 0;
         for ( Certificate certEntry : vec ) {
            String issuer = "N/A";
            String name = "N/A";
            String serial = "N/A";
            String date = "N/A";
            try {
               issuer = certEntry.getCert().getIssuerDN().toString();
               name =  certEntry.getCert().getSubjectDN().toString();
               serial = certEntry.getCert().getSerialNumber().toString(16);
               date = certEntry.getCert().getNotAfter().toString();
            } catch (Exception e) {
               Logging.warning("CertificateTable: Problem " + e);
            }
            
            String newrow = fRow.replaceAll("#index#","" + index)
               .replaceAll("#certid#","" + certEntry.getCertid())
               .replaceAll("#Issuer#",issuer)
               .replaceAll("#Name#",name)
               .replaceAll("#Serial#",serial)
               .replaceAll("#ExpirationDate#",date)
               .replaceAll("#Status#", certEntry.isValid() ? "Allowed" : "Banned")
               .replaceAll("#Change#", certEntry.isValid() ? 
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
               Long certid = Long.decode(fRequest.getParameter("certid"));
               Logging.debug("CertificateTable: Banning :" + certid);
               setState(certid,false);
            } else if (action.equals("Allow")) {
               Long certid = Long.decode(fRequest.getParameter("certid"));
               Logging.debug("CertificateTable: Allowing :" + certid);
               setState(certid,true);
            } else if (action.equals("BanAll")) {
               Logging.debug("CertificateTable: Banning All");
               setAllState(false);
            } else if (action.equals("AllowAll")) {
               Logging.debug("CertificateTable: Allowing All");
               setAllState(true);
            }
         } catch (NumberFormatException e) {
            Logging.warning("CertificateTable: Problem when parsing certid in post: "+fRequest.getParameter("certid"));
            reportError("Internal Error",e.toString());            
         } catch (Exception e) {
            Logging.warning("CertificateTable: Problem when handling certificate: "+fRequest.getParameter("certid")+": ",e);
            reportError("Internal Error",e.toString());
         }
         
      }
      
      public void setState(Long certid, boolean isValid) throws Exception
      {
         Logging.debug("CertificateTable: changing state of certid" + certid + " to " + (isValid ? "Allowed" : "Banned"));
         
         try {
            Certificate cert = fRepTable.get(certid);
            Certificate updated = new Certificate(cert);
            updated.setValid(isValid);
         
            if (cert != null) {
               Session session = HibernateWrapper.getSession();
               Transaction tx = session.beginTransaction();
               session.saveOrUpdate( updated );
               session.flush();
               tx.commit();
               session.close();
               fRepTable.put( new Long(updated.getCertid()), updated );
            }
         } catch (Exception e) {
            throw e;
         }
      }
      
      public void setAllState(boolean isValid) throws Exception
      {
         Logging.debug("CertificateTable: changing state of all certificates to " + (isValid ? "Allowed" : "Banned"));

         try {
            Session session = HibernateWrapper.getSession();
            Transaction tx = session.beginTransaction();
         
            Hashtable<Long,Certificate> updatedTable = new Hashtable<Long,Certificate>();
            Enumeration<Certificate> certEntries    = fRepTable.elements();
            while (certEntries.hasMoreElements()) {
               Certificate cert = certEntries.nextElement();
               Certificate updated = new Certificate(cert);
               updated.setValid(isValid);
               updatedTable.put( new Long(updated.getCertid()), updated );
               session.saveOrUpdate( updated );
            }
            session.flush();
            tx.commit();
            session.close();
            fRepTable = updatedTable;
         } catch (Exception e) {
            throw e;
         }
      }
      
   }

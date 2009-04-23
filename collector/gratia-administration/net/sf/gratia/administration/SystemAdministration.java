package net.sf.gratia.administration;

import net.sf.gratia.util.XP;
import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Logging;
import net.sf.gratia.services.*;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Vector;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import java.text.*;
import java.rmi.*;
import org.apache.tools.bzip2.*;
import com.ice.tar.*;

public class SystemAdministration extends HttpServlet {
    //
    // database related
    //
    String driver = "";
    String url = "";
    String user = "";
    String password = "";
    Connection connection;
    Statement statement;
    ResultSet resultSet;
    //
    // processing related
    //
    String html = "";
    String row = "";
    StringBuffer buffer = new StringBuffer();
    //
    // globals
    //
    HttpServletRequest request;
    HttpServletResponse response;
    boolean initialized = false;
    Properties props;
    Properties p;
    String message = null;
    //
    // support
    //
    String dq = "\"";
    String comma = ",";
    String cr = "\n";

    JMSProxy proxy = null;

    //
    // statics for recovery thread
    //

    static RecoveryService recoveryService = null;
    static String replayStatus = "";
    static long replayRecordsSkipped = 0;
    static long replayRecordsProcessed = 0;
    static long errors = 0;
    static boolean replayall = false;

    public void initialize() {
        p = net.sf.gratia.util.Configuration.getProperties();
        try {
            proxy = (JMSProxy) Naming.lookup(p.getProperty("service.rmi.rmilookup") +
                                             p.getProperty("service.rmi.service"));
        }
        catch (Exception e) {
            Logging.warning("SystemAdministration: Caught exception during RMI lookup", e);
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
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

        // This could be use to avoid using redirection altogether in non-interactive browsing (wget)
        //         String agent = request.getHeader("User-Agent");
        //         Logging.warning("SystemAdministration: request coming from : "+ agent);
        //         boolean noninterative = agent.startsWith("Wget/");

        if (!login) {
            Properties p = Configuration.getProperties();
            String loginLink = p.getProperty("service.secure.connection") + request.getContextPath() + "/gratia-login.jsp";
            String redirectLocation = response.encodeRedirectURL(loginLink);
            response.sendRedirect(redirectLocation);
            request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);
        } else {
            initialize();
            this.request = request;
            this.response = response;
            if (request.getParameter("action") != null) {
                String action = request.getParameter("action");
                if (action.equals("replay")) {
                    replay();
                } else if (action.equals("replayAll")) {
                    replayAll();
                } else { // Proxy operation
                    executeProxyAction(request.getParameter("action"));
                }
                try {
                    Thread.sleep(2000); // Time for state to change
                }
                catch (Exception ignore) {
                }
            }
            setup();
            process();
            response.setContentType("text/html");
            response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0
            PrintWriter writer = response.getWriter();
            writer.write(html);
            writer.flush();
            writer.close();
        }
    }

    public void setup() {
        html = XP.get(request.getRealPath("/") + "systemadministration.html");
    }

    public void process() {
        String operationsStatus = "UNKNOWN";
        String listenerStatus = "UNKNOWN";
        String replicationStatus = "UNKNOWN";
        String servletStatus = "UNKNOWN";
        String housekeepingStatus = "UNKNOWN";
        String reaperStatus = "UNKNOWN";

        if (proxy == null) {
            initialize();
        }

        if (proxy != null ) {
            try {
                Boolean flag = proxy.operationsDisabled();
                if (flag) {
                    operationsStatus = "SAFE";
                } else {
                    operationsStatus = "Active";
                    flag = proxy.databaseUpdateThreadsActive();
                    if (flag) {
                        listenerStatus = "Active";
                    } else {
                        listenerStatus = "Stopped";
                    }
                    flag = proxy.replicationServiceActive();
                    if (flag) {
                        replicationStatus = "Active";
                    } else {
                        replicationStatus = "Stopped";
                    }
                    flag = proxy.servletEnabled();
                    if (flag) {
                        servletStatus = "Active";
                    } else {
                        servletStatus = "Stopped";
                    }
                    flag = proxy.reaperActive();
                    if (flag) {
                        reaperStatus = "RUNNING";
                    } else {
                        reaperStatus = "SLEEPING";
                    }
                    housekeepingStatus = proxy.housekeepingServiceStatus();
                }
            }
            catch (Exception e) {
                Logging.warning("SystemAdministration.process: Caught exception assessing operational status via proxy", e);
            }
        } 

        html = html.replaceAll("#date#", DateFormat.getDateTimeInstance().format(new Date()) + " UTC");

        if (operationsStatus.equals("SAFE")) {
            html = html.replaceAll("#opstatus#",
                                   "<font color=\"fuchsia\"><strong>DISABLED</strong></font>").
                replaceAll("#opcomment#", 
                           "<a href=\"systemadministration.html?action=enableOperations\"><strong>Enable</strong></a><br />To disable safe mode start, set <tt>gratia.service.safeStart = 0</tt> in service-configuration.properties.");
            listenerStatus = "SAFE";
            replicationStatus = "SAFE";
            servletStatus = "SAFE";
            housekeepingStatus = "SAFE";
        } else if (operationsStatus.equals("Active")) {
            html = html.replaceAll("#opstatus#",
                                   "<font color=\"green\"><strong>ENABLED</strong></font>").
                replaceAll("#opcomment#",
                           "To start in safe mode, set <strong><tt>gratia.service.safeStart = 1</tt></strong> in service-configuration.properties.");
        } else {
            html = html.replaceAll("#opstatus#",
                                   "<font color=\"red\"><strong>UNKNOWN</strong></font>").
                replaceAll("#opcomment#",
                           "Check logs for errors");
        }

        html = html.replaceAll("#replaystatus#", replayStatus).
            replaceAll("#replayrecordsprocessed#","" + replayRecordsProcessed).
            replaceAll("#replayrecordsskipped#","" + replayRecordsSkipped);

        if (listenerStatus.equals("Active")) {
            html = html.replaceAll("#listenerstatus#",
                                   "<font color=\"green\"><strong>ACTIVE</strong></font>").
                replaceAll("#listenercomment#",
                           "<a href=\"systemadministration.html?action=stopDatabaseUpdateThreads\"><strong>Stop</strong></a>.");
        } else if (listenerStatus.equals("SAFE")) {
            html = html.replaceAll("#listenerstatus#",
                                   "<font color=\"fuchsia\"><strong>SAFE</strong></font>").
                replaceAll("#listenercomment#",
                           "See <strong>Global Operational Status</strong>, above.");
        } else if (listenerStatus.equals("Stopped")) {
            html = html.replaceAll("#listenerstatus#",
                                   "<font color=\"red\"><strong>STOPPED</strong></font>").
                replaceAll("#listenercomment#",
                           "<a href=\"systemadministration.html?action=startDatabaseUpdateThreads\"><strong>Start</strong></a>.");
        } else {
            html = html.replaceAll("#listenerstatus#",
                                   "<font color=\"red\"><strong>UNKNOWN</strong></font>").
                replaceAll("#listenercomment#",
                           "Check log for errors.");
        }

        if (replicationStatus.equals("Active")) {
            html = html.replaceAll("#replicationstatus#",
                                   "<font color=\"green\"><strong>ACTIVE</strong></font>").
                replaceAll("#replicationcomment#",
                           "<a href=\"systemadministration.html?action=stopReplication\"><strong>Stop</strong></a>.");
        } else if (replicationStatus.equals("SAFE")) {
            html = html.replaceAll("#replicationstatus#",
                                   "<font color=\"fuchsia\"><strong>SAFE</strong></font>").
                replaceAll("#replicationcomment#",
                           "See <strong>Global Operational Status</strong>, above.");
        } else if (replicationStatus.equals("Stopped")) {
            html = html.replaceAll("#replicationstatus#",
                                   "<font color=\"red\"><strong>STOPPED</strong></font>").
                replaceAll("#replicationcomment#",
                           "<a href=\"systemadministration.html?action=startReplication\"><strong>Start</strong></a>.");
        } else {
            html = html.replaceAll("#replicationstatus#",
                                   "<font color=\"red\"><strong>UNKNOWN</strong></font>").
                replaceAll("#replicationcomment#",
                           "Check log for errors.");
        }

        if (servletStatus.equals("Active")) {
            html = html.replaceAll("#servletstatus#",
                                   "<font color=\"green\"><strong>ACTIVE</strong></font>").
                replaceAll("#servletcomment#",
                           "<a href=\"systemadministration.html?action=disableServlet\"><strong>Stop</strong></a>.");
        } else if (servletStatus.equals("SAFE")) {
            html = html.replaceAll("#servletstatus#",
                                   "<font color=\"fuchsia\"><strong>SAFE</strong></font>").
                replaceAll("#servletcomment#",
                           "See <strong>Global Operational Status</strong>, above.");
        } else if (servletStatus.equals("Stopped")) {
            html = html.replaceAll("#servletstatus#",
                                   "<font color=\"red\"><strong>STOPPED</strong></font>").
                replaceAll("#servletcomment#",
                           "<a href=\"systemadministration.html?action=enableServlet\"><strong>Start</strong></a>.");
        } else {
            html = html.replaceAll("#servletstatus#",
                                   "<font color=\"red\"><strong>UNKNOWN</strong></font>").
                replaceAll("#servletcomment#",
                           "Check log for errors.");
        }

        if (reaperStatus.equals("RUNNING")) {
            html = html.replaceAll("#reaperstatus#",
                                   "<font color=\"green\"><strong>RUNNING</strong></font>").
                replaceAll("#reapercomment#", "");
        } else {
            html = html.replaceAll("#reaperstatus#",
                                   "<font color=\"green\"><strong>SLEEPING</strong></font>").
                replaceAll("#reapercomment#",
                           "<a href=\"systemadministration.html?action=runReaper\"><strong>Run now</strong></a>.");
        }

        String color;

        if (housekeepingStatus.equals("SAFE")) {
            color = "fuchsia";
            html = html.replaceAll("#housekeepingcomment#",
                                   "See <strong>Global Operational Status</strong>, above.");
        } else if (housekeepingStatus.equals("DISABLED")) {
            color = "fuchsia";
            html = html.replaceAll("#housekeepingcomment#",
                                   "Old records are <strong>accepted</strong>. <a href=\"systemadministration.html?action=startHousekeeping\"><strong>Start normally</strong></a> or <a href=\"systemadministration.html?action=startHousekeepingNow\"><strong>Start run now</strong></a>.");
        } else if (housekeepingStatus.equalsIgnoreCase("STOPPED")) {
            color = "red";
            html = html.replaceAll("#housekeepingcomment#",
                                   "Old records are <strong>rejected</strong> as normal. <a href=\"systemadministration.html?action=startHousekeeping\"><strong>Start normally</strong></a>; <a href=\"systemadministration.html?action=startHousekeepingNow\"><strong>Start run now</strong></a>; or <a href=\"systemadministration.html?action=disableHousekeeping\"><strong>Disable</strong></a> housekeeping service and the rejection of incoming old records.");
        } else if (housekeepingStatus.equalsIgnoreCase("SLEEPING")) {
            color = "green";
            html = html.replaceAll("#housekeepingcomment#",
                                   "<a href=\"systemadministration.html?action=startHousekeepingNow\"><strong>Run now</strong></a>, <a href=\"systemadministration.html?action=stopHousekeeping\"><strong>Stop</strong></a> after current run or <a href=\"systemadministration.html?action=disableHousekeeping\"><strong>Stop and disable</strong></a> housekeeping service and the rejection of incoming old records after current run.");
        } else if (housekeepingStatus.equalsIgnoreCase("UNKNOWN")) {
            color = "red";
            html = html.replaceAll("#housekeepingcomment#",
                                   "Check log for errors.");
        } else {
            color = "green";
            html = html.replaceAll("#housekeepingcomment#",
                                   "<a href=\"systemadministration.html?action=stopHousekeeping\"><strong>Stop</strong></a> or " +
                                   "<a href=\"systemadministration.html?action=disableHousekeeping\"><strong>Stop and disable</strong></a> housekeeping service and the rejection of incoming old records after current run.");
        }
        html = html.replaceAll("#housekeepingstatus#",
                               "<font color=\"" +
                               color +
                               "\"><strong>" +
                               housekeepingStatus +
                               "</strong></font>");
    }

    public void replay() {
        if ((SystemAdministration.recoveryService != null) &&
            SystemAdministration.recoveryService.isAlive()) {
            return;
        }
        SystemAdministration.replayStatus = "Starting";
        SystemAdministration.replayRecordsSkipped = 0;
        SystemAdministration.replayRecordsProcessed = 0;
        SystemAdministration.replayall = false;
        SystemAdministration.recoveryService = new RecoveryService();
        SystemAdministration.recoveryService.start();
    }

    public void replayAll() {
        if ((SystemAdministration.recoveryService != null) &&
            SystemAdministration.recoveryService.isAlive()) {
            return;
        }
        SystemAdministration.replayStatus = "Starting";
        SystemAdministration.replayRecordsSkipped = 0;
        SystemAdministration.replayRecordsProcessed = 0;
        SystemAdministration.replayall = true;
        SystemAdministration.recoveryService = new RecoveryService();
        SystemAdministration.recoveryService.start();
    }

    public void executeProxyAction(String action) {
        try {
            if (action.equals("stopDatabaseUpdateThreads")) {
                proxy.stopDatabaseUpdateThreads();
            } else if (action.equals("startDatabaseUpdateThreads")) {
                proxy.startDatabaseUpdateThreads();
            } else if (action.equals("enableOperations")) {
                proxy.enableOperations();
            } else if (action.equals("startReplication")) {
                proxy.startReplicationService();
            } else if (action.equals("stopReplication")) {
                proxy.stopReplicationService();
            } else if (action.equals("enableServlet")) {
                proxy.enableServlet();
            } else if (action.equals("disableServlet")) {
                proxy.disableServlet();
            } else if (action.equals("startHousekeeping")) {
                proxy.startHousekeepingService();
            } else if (action.equals("stopHousekeeping")) {
                proxy.stopHousekeepingService();
            } else if (action.equals("disableHousekeeping")) {
                proxy.disableHousekeepingService();
            } else if (action.equals("startHousekeepingNow")) {
                proxy.startHousekeepingActionNow();
            } else if (action.equals("runReaper")) {
                proxy.runReaper();
            } else {
                Logging.warning("SystemAdministration.executeProxyAction called with unknown action " + action);
            }
            
        }
        catch (Exception e) {
            Logging.warning("SystemAdministration.executeProxyAction: Caught exception during proxy operation", e);
        }
    }

    private class RecoveryService extends Thread {
        String driver;
        String url;
        String user;
        String password;

        Connection connection;
        Statement statement;
        ResultSet resultSet;

        String command;

        Properties p;

        Vector history = new Vector();
        String filenames[] = new String[0];
        java.util.Date databaseDate = null;

        int irecords = 0;

        public RecoveryService() {
            Logging.info("RecoveryService: Starting");
            SystemAdministration.replayStatus = "RecoveryService: Starting";

            p = net.sf.gratia.util.Configuration.getProperties();
            driver = p.getProperty("service.mysql.driver");
            url = p.getProperty("service.mysql.url");
            user = p.getProperty("service.mysql.user");
            password = p.getProperty("service.mysql.password");
            openConnection();
            getDirectories();
            getDatabaseDate();
        }

        public void openConnection() {
            try {
                Class.forName(driver).newInstance();
                connection = null;
                connection = DriverManager.getConnection(url,user,password);
            }
            catch (Exception e) {
            }
        }

        public void getDirectories() {
            int i = 0;
            Vector vector = new Vector();
            String path = System.getProperties().getProperty("catalina.home") + "/gratia/data";
            path = path.replaceAll("\\\\","/");
            Logging.debug("RecoveryService: Path: " + path);
            String temp[] = XP.getDirectoryList(path);
            for (i = 0; i < temp.length; i++)
                if (temp[i].indexOf("history") > -1)
                    history.add(temp[i]);
            Logging.log("RecoveryService: Directories To Process: " + history.size());
        }

        public void getDatabaseDate() {
            long days = Long.parseLong(p.getProperty("maintain.history.log"));
            long now = (new java.util.Date()).getTime();
            databaseDate = new java.util.Date(now - (days * 24 * 60 * 1000));

            command = "select max(ServerDate) from JobUsageRecord_Meta";
            try {
                statement = connection.prepareStatement(command);
                resultSet = statement.executeQuery(command);
                while(resultSet.next()) {
                    Timestamp timestamp = resultSet.getTimestamp(1);
                    if (timestamp != null)
                        databaseDate = new java.util.Date(timestamp.getTime());
                }
                resultSet.close();
                statement.close();
            }
            catch (Exception e) {
                Logging.warning("RecoveryService: received exception getting database date.", e);
            }

            long temp = databaseDate.getTime() - (5 * 60 * 1000);
            databaseDate = new java.util.Date(temp);
            Logging.info("RecoveryService: Recovering From: " + databaseDate);
        }

        public void recoverRecord(String data, String filename) {
            String connection = p.getProperty("service.open.connection");
            Post post = null;

            StringTokenizer st = new StringTokenizer(data,"|");
            st.nextToken();
            String timestamp = st.nextToken();
            java.util.Date recordDate = new java.util.Date(Long.parseLong(timestamp));

            if (SystemAdministration.replayall || recordDate.after(databaseDate)) {
                post = new Post(connection + "/gratia-servlets/rmi","update",data);
                try {
                    irecords++;
                    post.send();
                    SystemAdministration.replayRecordsProcessed++;
                    Logging.debug("RecoveryService: Sent: " + irecords + ":" + filename + " :Timestamp: " + recordDate);
                    SystemAdministration.replayStatus = "RecoveryService: Sent: " + irecords + ":" + filename + " :Timestamp: " + recordDate;
                }
                catch (Exception e) {
                    Logging.warning("RecoveryService: Error Sending " + filename, e);
                    return;
                }
            } else {
                SystemAdministration.replayRecordsSkipped++;
                Logging.debug("RecoveryService: Skipping: " + filename + " :Timestamp: " + recordDate);
                SystemAdministration.replayStatus = "RecoveryService: Skipping: " + filename + " :Timestamp: " + recordDate;
            }
        }

        public void run() {
            String directory = "";
            Logging.info("RecoveryService: Started");
            for (int i = 0; i < history.size(); i++) {
                directory = (String) history.elementAt(i);
                recover(directory);
            }
            Logging.info("RecoveryService: Exiting");
            SystemAdministration.replayStatus = "Finished";
        }

        public void recoverArchiveEntry(TarInputStream tin, TarEntry tarEntry)
            throws java.io.IOException {
            if (tarEntry.isDirectory()) {
                // Ignore directories
            } else {
                Logging.debug("RecoveryService: Processing Entry: " + tarEntry.getName());
                //create a file with the same name as the tarEntry
                int size=(int)tarEntry.getSize();
                // -1 means unknown size.
                if (size==-1) {
                    size=1000; // ((Integer)htSizes.get(ze.getName())).intValue();
                }
                byte[] b=new byte[(int)size];
                int rb=0;
                int chunk=0;
                while (((int)size - rb) > 0) {
                    chunk=tin.read(b,rb,(int)size - rb);
                    if (chunk==-1) {
                        break;
                    }
                    rb+=chunk;
                }
                String data = new String(b);
                recoverRecord(data,tarEntry.getName());
            }
        }

        public void recoverArchive(File archive) {
            Logging.debug("RecoveryService: Processing Archive: " + archive.getAbsolutePath());
            try {
                FileInputStream fis = new FileInputStream(archive);
                Logging.debug("RecoveryService: Processing Archive 2: " + archive.getName());

                byte skip[] = new byte[2];
                fis.read(skip,0,2);

                CBZip2InputStream gzipInputStream = new CBZip2InputStream(fis);

                Logging.debug("RecoveryService: Processing Archive 3: " + archive.getName());

                // Byte skip[] = new Byte[2];
                // gzipInputStream.read(skip,0,2);

                TarInputStream tin = new TarInputStream( gzipInputStream );

                Logging.debug("RecoveryService: Processing Archive 4: " + archive.getName());

                TarEntry tarEntry = tin.getNextEntry();

                Logging.debug("RecoveryService: Processing Archive 5: " + archive.getName());

                while (tarEntry != null)
                    {
                        recoverArchiveEntry(tin,tarEntry);
                        tarEntry = tin.getNextEntry();
                    }

                // Close the file and stream
                tin.close();
            }
            catch (Exception e) {
                Logging.warning("recoverArchive: failed to processed file " +
                                archive.getName(), e);
            }
        }

        public void recover(String directory) {
            recoverDirectory(new File(directory));
        }

        public void recoverFile(File file) {
            String blob = XP.get(file);
            recoverRecord(blob,file.getName());
        }

        public void recoverDirectory(File directory) {
            Logging.log("RecoveryService: Processing Directory: " + directory.getName());

            int i = 0;

            File filelist[] = directory.listFiles();

            for (i = 0; i < filelist.length; i++) {
                File current = filelist[i];
                Logging.debug("RecoveryService: sub-processing "+current.getName());
                if (current.isDirectory()) {
                    recoverDirectory(current);
                } else if (current.getName().endsWith(".tar.bz2")) {
                    recoverArchive(current);
                } else {
                    recoverFile(current);
                }
            }
        }
    }
}

package net.sf.gratia.reporting;

import org.eclipse.birt.report.engine.api.script.eventadapter.ReportEventAdapter;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.engine.api.script.element.IReportDesign;
import java.io.*;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import net.sf.gratia.reporting.ReportingConfiguration;
//import net.sf.gratia.util.Logging;

/**
 * @author penelope
 *
 */

public class GratiaBirtReportEH extends ReportEventAdapter {
	private static String timeStampFile = "/gratia-logs/gratiaReportingLog.csv";
	private static String timeStampFolder ="/gratia-logs/";

	public void afterFactory(IReportContext rc) {
		long timeStamp = System.currentTimeMillis();

		try
		{
			File checkFile = new java.io.File(System.getProperty("catalina.home") + timeStampFile);
			if (checkFile.exists())
			{
				BufferedWriter out = new BufferedWriter(new FileWriter(System.getProperty("catalina.home") + timeStampFile, true));
				out.write(", afterFactory = ," + timeStamp);
				out.flush();
				out.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void afterRender(IReportContext rc) {
		// Get current time
		long timeStamp = System.currentTimeMillis();

		try
		{
			File checkFile = new java.io.File(System.getProperty("catalina.home") + timeStampFile);
			if (checkFile.exists())
			{
				BufferedWriter out = new BufferedWriter(new FileWriter(System.getProperty("catalina.home") + timeStampFile, true));
				out.write(", afterRender = ," + timeStamp + "\n");
				out.flush();
				out.close();
			}
		}
		catch  (Exception e) {
			e.printStackTrace();
		}
	}

	public String formatParam (String param)
	{
		if (param == null)
			return "";

		// out.write("Param = " + param +"\n");
		// out.flush();

		String[] words = param.split (";");
		
		// If the first value is " ALL" then return an empty string
		if (words[0].trim().compareToIgnoreCase("ALL") == 0)
			return "";
		
		String outParam = "(";
		for (int i=0; i < words.length; i++)
		{
			if (outParam != "(")
				outParam += "," + "'"+ words[i] + "'";
			else
				outParam += "'"+ words[i] + "'";
		}
		outParam += ")";
		return outParam;
	}

	public void beforeFactory(IReportDesign design, IReportContext rc) {
		// Get current time
		long timeStamp = System.currentTimeMillis();

		try
		{
			HttpServletRequest request = (HttpServletRequest) rc.getHttpServletRequest();
			String outReportURL = request.getRequestURL().toString().replace("frameset", "checkDateParameters.jsp") + "?" + request.getQueryString();

			// Check if there is a VOs or Sites parameter. If so, format it for SQL input
			Object inVOsObj = rc.getParameterValue("SelectVOs");
			if (inVOsObj != null)
			{
				rc.setParameterValue("SelectVOs", formatParam(inVOsObj.toString()));
			}

			Object inSitesObj = rc.getParameterValue("SelectSites");
			if (inSitesObj != null)
			{
				rc.setParameterValue("SelectSites", formatParam(inSitesObj.toString()));
			}

			Object inProbesObj = rc.getParameterValue("SelectProbes");
			if (inProbesObj != null)
			{
				rc.setParameterValue("SelectProbes", formatParam(inProbesObj.toString()));
			}

			// Set parameter "ReportURL" to the called URL.

			Object inReportURLObj = rc.getParameterValue("ReportURL");

			if (inReportURLObj != null)
			{
				String inReportURL = inReportURLObj.toString();
				//out.write("Object is not null= " + inReportURL + "\n");
				//out.flush();
				if (inReportURL.length() == 0 || inReportURL == "")
				{
					//out.write("OUTPUT URL = " + outReportURL);
					//out.flush();

					rc.setParameterValue("ReportURL", outReportURL);
				}
			}
			File checkFile = new java.io.File(System.getProperty("catalina.home") + timeStampFile);
			if (checkFile.exists())
			{
				BufferedWriter out = new BufferedWriter(new FileWriter(System.getProperty("catalina.home") + timeStampFile, true));
				out.write(", beforeFactory = ," + timeStamp);
				out.flush();
				out.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void beforeRender(IReportContext rc) {
		// Get current time
		long timeStamp = System.currentTimeMillis();

		try
		{
			File checkFile = new java.io.File(System.getProperty("catalina.home") + timeStampFile);
			if (checkFile.exists())
			{
				BufferedWriter out = new BufferedWriter(new FileWriter(System.getProperty("catalina.home") + timeStampFile, true));
				out.write(", beforeRender = ," + timeStamp);
				out.flush();
				out.close();
			}
		}
		catch  (Exception e) {
			e.printStackTrace();
		}
	}

	public void initialize(IReportContext inReport) {

		// Get current time
		long timeStamp = System.currentTimeMillis();

		try
		{
			// Debugging ...
			//BufferedWriter out1 = new BufferedWriter(new FileWriter("./GratiaBirtEH.log", true));
			//out1.write("\n+++++++++ INTIALIZE REPORT ++++++++++++++++\n");
			//out1.flush();

			HttpServletRequest request = (HttpServletRequest) inReport.getHttpServletRequest();

			if (request != null)
			{
				// DEBUG
				//String ReportURL = request.getRequestURL().toString() + "?" + request.getQueryString();


				ReportingConfiguration reportingConfig = new ReportingConfiguration();
				reportingConfig.loadReportingConfiguration(request);

				//*CertificateHandler certificateHandler = new CertificateHandler();
				//*certificateHandler.loadCertificateHandler(request);
				//*certificateHandler.dump();

				String userName = "GratiaUser"; //*certificateHandler.getName();
				String userRole = "GratiaUser"; //*certificateHandler.getRole();
				String VO = "Unknown"; //*certificateHandler.getVO();
				//String subtitle = "A Subtitle"; //*certificateHandler.getSubtitle();

				String userKey = "" + System.currentTimeMillis();
				userName = userName + "|" + userKey + "|" + VO;
				String databaseURL =  reportingConfig.getDatabaseURL();
				String databaseUser = reportingConfig.getDatabaseUser();
				String databasePassword = reportingConfig.getDatabasePassword();
				//String reportsMenuConfig = reportingConfig.getReportsMenuConfig();
				//String reportsFolder = reportingConfig.getReportsFolder();
				//String varConfigLoaded = reportingConfig.getConfigLoaded();

				// set parameter values
				inReport.setParameterValue("DatabaseURL", databaseURL);
				inReport.setParameterValue("DatabasePassword", databasePassword);
				inReport.setParameterValue("DatabaseUser", databaseUser);
				inReport.setParameterValue("UserName", userName);
				inReport.setParameterValue("UserRole", userRole);
				// inReport.setParameterValue("ReportSubtitle", subtitle);

				// out1.write("Time Stamp file = " + timeStampFile + " \tLogging is: " + logTimeStamps + "\n");
				// out1.flush();
				// out1.close();

				if (reportingConfig.getLogging())
				{
					File checkFolder = new java.io.File(System.getProperty("catalina.home") + timeStampFolder);
					if (!checkFolder.exists())
						checkFolder.mkdirs();
					checkFolder = null;
					BufferedWriter out = new BufferedWriter(new FileWriter(System.getProperty("catalina.home") + timeStampFile, true));
					String reportName = request.getParameter("__report");
					reportName = reportName.substring(reportName.lastIndexOf("/")+1, reportName.indexOf(".rptdesign"));
					out.write(reportName + " = ,"+ timeStamp);
					out.flush();
					out.close();
				}
				else
				{
					File checkFile = new java.io.File(System.getProperty("catalina.home") + timeStampFile);
					if (checkFile.exists())
					{
						File dest = new java.io.File (System.getProperty("catalina.home") + timeStampFile + userKey);
						checkFile.renameTo(dest);
					}
				}

				// out.write("\tParameters are set\n");
				// out.write("\tDatabaseURL= " + inReport.getParameterValue("DatabaseURL")+"\n");
				// out.write("\tDatabasePassword= " + inReport.getParameterValue("DatabasePassword")+"\n");
				// out.write("\tDatabaseUser= " + inReport.getParameterValue("DatabaseUser")+"\n");
				// out.write("\tUserName= " + inReport.getParameterValue("UserName")+"\n");
				// out.write("\tUserRole= " + inReport.getParameterValue("UserRole")+"\n");
				// out.write("\tSubtitle= " + inReport.getParameterValue("Subtitle")+"\n");
				// out.close();
			}
			else
			{
				File checkFile = new java.io.File(System.getProperty("catalina.home") + timeStampFile);
				if (checkFile.exists())
				{
					BufferedWriter out = new BufferedWriter(new FileWriter(System.getProperty("catalina.home") + timeStampFile, true));
					out.write(", initialize = ," + timeStamp);
					out.flush();
					out.close();
				}
				// out.write("\tHttpServletRequest is NULL");
				// out.flush();
			}
		}
		catch (Exception e)
		{
		e.printStackTrace();
		}
	}
}

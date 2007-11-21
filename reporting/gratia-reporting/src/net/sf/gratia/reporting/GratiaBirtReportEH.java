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

	public void afterFactory(IReportContext rc) {
	}

	public void afterRender(IReportContext rc) {
	}

	public void beforeFactory(IReportDesign design, IReportContext rc) {
		try 
		{			
			HttpServletRequest request = (HttpServletRequest) rc.getHttpServletRequest();
			
			String outReportURL = request.getRequestURL().toString().replace("frameset", "checkDateParameters.jsp") + "?" + request.getQueryString();

			//  Debugging -- Start 1 of 5
			// String gratiaEHLog =  System.getProperty("catalina.home") + File.separatorChar+ "webapps" + File.separatorChar + "GratiaBirtEH.log";
			// BufferedWriter out = new BufferedWriter(new FileWriter(gratiaEHLog, true));
		    // out.write("\n+++++++++ EH: BEFORE FACTORY ++++++++++++++++\n");
			// out.write("ReportURL = " + ReportURL + "\n");
			// out.flush();
			//  Debugging -- End 1 of 5
			
			// Check if there is a VOs parameter. If so, format it for SQL input
			Object inVOsObj = rc.getParameterValue("SelectVOs");
	        if (inVOsObj != null)
	        {
	        	String inVOs = inVOsObj.toString();
	        	
				//   Debugging -- Start 2 of 5
	        	// out.write("VOs = " + inVOs +"\n");
	        	// out.flush();
				//  Debugging -- End 2 of 5
		        
	        	String[] words = inVOs.split (";");
	        	String outVOs = "(";
	        	for (int i=0; i < words.length; i++)
	        	{
	        		if (outVOs != "(") 
	        			outVOs += "," + "'"+ words[i] + "'";
	        		else
	        			outVOs += "'"+ words[i] + "'";
	        	}
	    	
	        outVOs += ")";
	        rc.setParameterValue("SelectVOs", outVOs); 
	        }
	        
	        // Check if there is the parameter "ReportURL" is blank. 
	        // If so set it to the called URL  

			Object inReportURLObj = rc.getParameterValue("ReportURL");
			
			if (inReportURLObj != null)
			{
				String inReportURL = inReportURLObj.toString();
				
				//  Debugging -- Start 3 of 5
				//out.write("Object is not null= " + inReportURL + "\n");
				//out.flush();
				//  Debugging -- End 3 of 5
				
				if (inReportURL.length() == 0 || inReportURL == "")
				{
					//  Debugging -- Start 4 of 5
					//out.write("OUTPUT URL = " + outReportURL);
					//out.flush();
					//  Debugging -- End 4 of 5
					
					rc.setParameterValue("ReportURL", outReportURL); 
				}
			}

			//  Debugging -- Start 5 of 5
	        // out.close();
			//  Debugging -- End 5 of 5
		}
		catch (Exception e) {
			e.printStackTrace();
		 }
	}

	public void beforeRender(IReportContext rc) {
	}

	public void initialize(IReportContext inReport) {
	
	   try {

			//  Debugging -- Start 1 of 2
			// String gratiaEHLog =  System.getProperty("catalina.home") + File.separatorChar+ "webapps" + File.separatorChar + "GratiaBirtEH.log";
			// BufferedWriter out = new BufferedWriter(new FileWriter(gratiaEHLog, true));
		    // out.write("\n+++++++++ EH: INTIALIZE REPORT PARAMETERS ++++++++++++++++\n");
	    	// out.flush();
		   
		   HttpServletRequest request = (HttpServletRequest) inReport.getHttpServletRequest();
		   if (request != null)
		   {
			   String ReportURL = request.getRequestURL().toString() + "?" + request.getQueryString();
		
			   // out.write("ReportURL = " + ReportURL + "\n");
			   // out.flush();
			   //  Debugging -- End 1 of 2
		
			   ReportingConfiguration reportingConfig = new ReportingConfiguration();
			   reportingConfig.loadReportingConfiguration(request);
		
			   //*CertificateHandler certificateHandler = new CertificateHandler();
			   //*certificateHandler.loadCertificateHandler(request);
		
		
			   //*certificateHandler.dump();
			   String userName = "GratiaUser"; //*certificateHandler.getName();
			   String userRole = "GratiaUser"; //*certificateHandler.getRole();
			   //String subtitle = "A Subtitle"; //*certificateHandler.getSubtitle();
			   String VO = "Unknown"; //*certificateHandler.getVO();
		
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
		
			   //  Debugging -- Start 2 of 2
			   // out.write("\tParameters are set\n");
			   // out.write("\tDatabaseURL= " + inReport.getParameterValue("DatabaseURL")+"\n");
			   // out.write("\tDatabasePassword= " + inReport.getParameterValue("DatabasePassword")+"\n");
			   // out.write("\tDatabaseUser= " + inReport.getParameterValue("DatabaseUser")+"\n");
			   // out.write("\tUserName= " + inReport.getParameterValue("UserName")+"\n");
			   // out.write("\tUserRole= " + inReport.getParameterValue("UserRole")+"\n");
			   // out.flush();
		   }
		   else
		   {
				// out.write("\tHttpServletRequest is NULL");
	    		// out.flush();
		   }    
	 
		   // out.close();
		   //  Debugging -- End 2 of 2
		
	   } catch (Exception e) {
			e.printStackTrace();
	   }
	}

}

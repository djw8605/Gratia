package net.sf.gratia.reporting;

import org.eclipse.birt.report.engine.api.script.eventadapter.ReportEventAdapter;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.engine.api.script.element.IReportDesign;
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

	}

	public void beforeRender(IReportContext rc) {
	}

	public void initialize(IReportContext inReport) {
	
	   try {
		    //  Debugging ...
			// BufferedWriter out = new BufferedWriter(new FileWriter("./GratiaBirtEH.log", true));
	        // out.write("\n+++++++++ INTIALIZE REPORT PARAMETERS ++++++++++++++++\n");
	
		HttpServletRequest request = (HttpServletRequest) inReport.getHttpServletRequest();
				
		ReportingConfiguration reportingConfig = new ReportingConfiguration();
		reportingConfig.loadReportingConfiguration(request);
		
		//*CertificateHandler certificateHandler = new CertificateHandler();
		//*certificateHandler.loadCertificateHandler(request);
		
		
		//*certificateHandler.dump();
		String userName = "GratiaUser"; //*certificateHandler.getName();
		String userRole = "GratiaUser"; //*certificateHandler.getRole();
		String subtitle = "A Subtitle"; //*certificateHandler.getSubtitle();
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
		inReport.setParameterValue("Subtitle", subtitle);
		
		// out.write("\tParameters are set\n");
		// out.write("\tDatabaseURL= " + inReport.getParameterValue("DatabaseURL")+"\n");
		// out.write("\tDatabasePassword= " + inReport.getParameterValue("DatabasePassword")+"\n");
		// out.write("\tDatabaseUser= " + inReport.getParameterValue("DatabaseUser")+"\n");
		// out.write("\tUserName= " + inReport.getParameterValue("UserName")+"\n");
		// out.write("\tUserRole= " + inReport.getParameterValue("UserRole")+"\n");
		// out.write("\tSubtitle= " + inReport.getParameterValue("Subtitle")+"\n");
	        
		// out.close();
		
	   } catch (Exception e) {
			e.printStackTrace();
	   }
	}

}

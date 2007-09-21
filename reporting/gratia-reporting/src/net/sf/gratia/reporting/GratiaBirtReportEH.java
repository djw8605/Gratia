/**
 * 
 */
package net.sf.gratia.reporting;

import org.eclipse.birt.report.engine.api.script.eventadapter.ReportEventAdapter;
//import net.sf.gratia.reporting.BirtEventFormatter;

//import java.io.File;
//import java.io.FileInputStream;
//import java.util.Properties;
//import java.io.File;
//import java.util.logging.ConsoleHandler;
//import java.util.logging.FileHandler;
//import java.util.logging.Handler;
//import java.util.logging.Logger;
//import java.util.*;

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
		inReport.setParameterValue("TraceTableKey", userKey);
		
	   } catch (Exception e) {
			e.printStackTrace();
	   }
	}

}

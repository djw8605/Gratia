<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="net.sf.gratia.reporting.*"%>

<%
	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");
	if(reportingConfiguration == null)
	{
		reportingConfiguration = new ReportingConfiguration();
		reportingConfiguration.loadReportingConfiguration(request);
		session.setAttribute("reportingConfiguration", reportingConfiguration);
	}

	UserConfiguration userConfiguration = (UserConfiguration)session.getAttribute("userConfiguration");
	if(userConfiguration == null)
	{
		userConfiguration = new UserConfiguration();
		userConfiguration.loadUserConfiguration(request);
		session.setAttribute("userConfiguration", userConfiguration);
	}
	

	StaticReportConfig staticReportConfig = (StaticReportConfig)session.getAttribute("staticReportConfig");
	if(staticReportConfig == null)
	{
		staticReportConfig = new StaticReportConfig();
		staticReportConfig.loadStaticReportConfig(request);
		session.setAttribute("staticReportConfig", staticReportConfig);
	}
%>

package net.sf.gratia.vomsSecurity;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.servlet.http.*;

import java.security.cert.X509Certificate;
import org.glite.security.SecurityContext;
import org.glite.security.util.DNHandler;
import org.glite.security.util.DNImpl;
import org.glite.security.voms.service.admin.VOMSAdmin;
import org.glite.security.voms.service.admin.VOMSAdminServiceLocator;

//@SuppressWarnings("unchecked")
public class CertificateHandler
{
	/*
	 *	Only VOs listed in the service-configuration.properties have administrative access.
	 *	The user is prompt to select an appropriate VO from this list.
	 *	Upon selection, the voms server for the specified VO is contacted to get the Roles
	 *	the user roles.
	 *	The voms server link is listed in the "voms-servers" file, which is setup
	 *	by the administrator of the gratia system.
	 *	To grant access, the user selected role must match one of the roles listed in the
	 *	service-configuration.properties file.
	 */

	private String _vomsServerFile;

	//---- certificate information ----
	private String userDN   = null;
	private String userCA   = null;
	private String voServer = null;

	//---- VOMS information ----
	private String[] voFQANlist = null;		// list of FQAN with admin access to compare with
	private String[] voList     = null;		// list of VOs to display for user selection
	private String[] rolesVOMS  = null;		// list of VOMS roles to display for user selection
	private String[] groupsVOMS = null;		// list of VOMS groups to display for user selection

	private String voSelect  = null;		// User selected VO
	private String voConnect = null;		// User selected VO connection

	private String[] vomsNameList = null;	// list of VOs from voms-serevrs
	private String[] vomsUrlList  = null;	// list of VO URLs from voms-serevrs

	private String secureConnection = null;	// form: https://gratia.osg.org:8899
	private String openConnection   = null;	// form: http://gratia.osg.org:8801
	private String dbConnection     = null;	// feature use
	private String servicesVersion     = null;	// feature use

	//---------------------------------------------------
	public CertificateHandler(HttpServletRequest request)
	{
		// ---- load the service-configuration.properties ----
		loadServiceConfigurationProperties();

		// ---- reading certificate for dn/ca for VOMS call ----
		SecurityContext sc = new SecurityContext();
		SecurityContext.setCurrentContext(sc);

		X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

		if (certs == null)
			return;

		sc.setClientCertChain(certs);

		if (sc.getClientName () != null)
			sc.setClientName (new DNImpl(sc.getClientName ()).getX500());

		if (sc.getIssuerName () != null)
			sc.setIssuerName (new DNImpl(sc.getIssuerName ()).getX500());

		X509Certificate cs = sc.getClientCert();
		this.userDN = ((DNImpl) DNHandler.getSubject(cs)).getX500();
		this.userCA = ((DNImpl) DNHandler.getIssuer(cs)).getX500();

		// ---- setting up to call VOMS (not sure how much is needed)
		//		this needs to be set some other way (TRUSTED_CA on VDT dist ----
		System.setProperty("sslCAFiles","/etc/grid-security/certificates/*.0");
		// this may be ok
		System.setProperty("axis.socketSecureFactory","org.glite.security.trustmanager.axis.AXISSocketFactory");
		// these needs to be set some other way (I think there is a properties file)
		System.setProperty("sslKey","/etc/grid-security/http/httpkey.pem");
		System.setProperty("sslCertfile","/etc/grid-security/http/httpcert.pem");

	}

	// ----------------------------------
	public void loadServiceConfigurationProperties()
	{
		Properties p = net.sf.gratia.util.Configuration.getProperties();
		String vos;
		String voName = "";
		TreeSet tempVOs = new TreeSet();	// sorted arrayList with unique names for the voList
		List tempFQANs  = new ArrayList();	// FQAN list from service properties file

		try
		{
			_vomsServerFile = net.sf.gratia.util.Configuration.getConfigurationPath() + "/" + p.getProperty("service.voms.connections");
			secureConnection = p.getProperty("service.secure.connection");
			openConnection   = p.getProperty("service.open.connection");
			dbConnection     = p.getProperty("service.mysql.url");
			servicesVersion         = p.getProperty("gratia.services.version");

			boolean foundAdmin = true;
			int admid = 0;
			String admin_id = "service.admin.identity.";
			while(foundAdmin)
			{
				vos = p.getProperty(admin_id + admid);
				if (vos != null)
				{
					vos = vos.trim();
					tempFQANs.add(vos);

					// Extract the node name from the identity, example of identity:
					// 	     /gratia-vo1/Role=VO-Admin
					// we want to extract gratia-vo1 to populate the pull down menu.

					String[] VoFullStr = vos.split ("/");
					voName = VoFullStr[1];
					if (voName != null)
						tempVOs.add(voName);

					admid += 1;
				}
				else
				{
					foundAdmin = false;
				}
			}
			// Convert the ArrayLists to string arrays
			voList     = (String[])tempVOs.toArray(new String[tempVOs.size()]);
			voFQANlist = (String[])tempFQANs.toArray(new String[tempFQANs.size()]);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	// ----------------------------------
	public String loadVOServers(String voFile)
	{
		List voVOMSNam = new ArrayList();	// list of VO names from the voms-server file
		List voVOMSUrl = new ArrayList();	// list of URLs for VO from the voms-server file

		String InStrLine;	// Input line
		String StrLine;		// Line to parse after trimming

		try
		{
			BufferedReader inFile = new BufferedReader (new FileReader(voFile) );

			while ( (InStrLine = inFile.readLine()) != null)
			{
				StrLine = InStrLine.trim();
				if (StrLine.length() == 0) continue;	// Skip empty lines
				if (StrLine.startsWith("#")) continue;

				// Line is not a comment, parsing it using token to break it into fields
				String[] VoInfo = StrLine.split ("=");

				String  voNam = VoInfo[0].trim();
				String  voUrl = VoInfo[1].trim();
				voVOMSNam.add(voNam);
				voVOMSUrl.add(voUrl);
			}
		}
		catch (IOException e)
		{
			String msg = "CERTIFICATE HANDLER message: <br>" + e.getMessage();
			//System.out.println(msg);
			return msg;
		}

		// Convert now to arrays
		vomsNameList = (String[])voVOMSNam.toArray(new String[voVOMSNam.size()]);
		vomsUrlList  = (String[])voVOMSUrl.toArray(new String[voVOMSUrl.size()]);
		return "";
	}

	// ----------------------------------
	public String connectVOname(String voname)
	{
//		Read voms-servers file: the file resides in the tomcat/gratia area
		String fileStatus = loadVOServers(_vomsServerFile);
		if (fileStatus.length() > 1 )
			return fileStatus;

		voConnect = voname.trim();

		String VomsLocation = getVoVOMSserver(voConnect);

		// ---- calling VOMS ---
		final VOMSAdmin stub;
		try
		{
			// other test VOMS servers
			// String VomsLocation = "https://gratiax34.fnal.gov:8443/voms/" + voConnect + "/services/VOMSAdmin";

			final VOMSAdminServiceLocator locator = new VOMSAdminServiceLocator();
			stub = locator.getVOMSAdmin(new URL(VomsLocation));

			this.rolesVOMS  = stub.listRoles(userDN, userCA);
			this.groupsVOMS = stub.listGroups(userDN,userCA);
		}
		catch (Exception e)
		{
			String msg = "CERTIFICATE HANDLER message: <br>" + e.getMessage();
			//System.out.println(msg);
			return msg;
		}

		return "";
	}

	// ----------------------------------
	public String getSecureConnection() {
		return this.secureConnection;
	}

	// ----------------------------------
	public String getOpenConnection() {
		return this.openConnection;
	}

	// ----------------------------------
	public String getDBconnection() {
		return this.dbConnection;
	}

	// ----------------------------------
	public String getServicesVersion() {
		return this.servicesVersion;
	}

	//---------------------------------------------------
	// The information comes from the voms-servers configuration file under
	//	the tomcat/gratia area

	public String getVoVOMSserver(String voCon)
	{
		String  conUrl = null;

		for (int j=0; j < vomsNameList.length; j++)
		{
			if (vomsNameList[j].trim().equalsIgnoreCase(voCon.trim()))
			{
				conUrl = vomsUrlList[j].toString().trim();
			}
		}
		this.voServer = conUrl + "/services/VOMSAdmin";
		return this.voServer;
	}

	//---------------------------------------------------
	public String[] getVOlist()
	{
		return this.voList;
	}

	//---------------------------------------------------

	public String[] getFQANlist()
	{
		return this.voFQANlist;
	}

	//---------------------------------------------------
	public String getDN()
	{
		if (this.userDN == null)
			return "UnknownDn";
		return this.userDN;
	}

	//---------------------------------------------------
	public String getDNname()
	{
		String dnString = "";
		String[] subwords;

		if (this.userDN == null)
		{
			return "GratiaUserName";
		}
		else
		{
			dnString = this.userDN;
			String[] dnWords = dnString.split ("/");

			for (int i=0; i < dnWords.length; i++)
			{
				subwords = dnWords[i].split ("=");
				for (int ij=0; ij < subwords.length; ij++)
				{
					if (subwords[ij].equals("UID"))
					{
						String userName = subwords[ij+1];
						return userName;
					}
				}
			}
		}
		return "GratiaUserName";
	}

	// ----------------------------------
	public String[] getVOMSroles()
	{
		String[] initRoles = {"GratiaUserRole"};
		String[] userRoles = {""};
		String[] RolesStr = {"NoRoles"};

		if (this.rolesVOMS == null)
			return initRoles;

		if (this.rolesVOMS.length <= 0)
		{
			// System.out.println("CERTIFICATE HANDLER Message:  NO roles found ");
			return RolesStr;
		}

		return this.rolesVOMS;
	}

	// ----------------------------------
	public String [] getVOMSgroups()
	{
		String[] initGroup = {"GratiaUserGrp"};

		if (this.groupsVOMS == null)
			return initGroup;
		return this.groupsVOMS;
	}

	// ----------------------------------
	public String setVOname(String voname)
	{
		voSelect = voname;
		return voSelect;
	}

	// ----------------------------------
	public String getVOname()
	{
		return this.voSelect;
	}
}

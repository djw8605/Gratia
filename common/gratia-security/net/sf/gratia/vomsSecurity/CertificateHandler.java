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
	private String _configPath;

	//---- ssl information ----
	private String sslCAFiles  = null;
	private String sslKey      = null;
	private String sslCertfile = null;

	//---- certificate information ----
	private String userDN   = null;
	private String userCA   = null;
	private String voServer = null;

	//---- VOMS information ----
	private String[] voFQANlist = null;		// list of FQAN with admin access to compare with
	private String[] DNlist     = null;		// list of FQAN with admin access to compare with
	private String[] voList     = null;		// list of VOs to display for user selection
	private String[] rolesVOMS  = null;		// list of VOMS roles to display for user selection
	private String[] groupsVOMS = null;		// list of VOMS groups to display for user selection

	private String voSelect  = null;		// User selected VO
	private String voConnect = null;		// User selected VO connection

	private String[] vomsNameList = null;	// list of VOs from voms-serevrs
	private String[] vomsUrlList  = null;	// list of VO URLs from voms-serevrs

	private String secureConnection = null;	// form: https://gratia.osg.org:8899
	private String openConnection   = null;	// form: http://gratia.osg.org:8801
	private String dbConnection     = null;	// connected database
	private String servicesVersion  = null;	// services version

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
		System.setProperty("sslCAFiles", sslCAFiles);
		System.setProperty("axis.socketSecureFactory","org.glite.security.trustmanager.axis.AXISSocketFactory");
		System.setProperty("sslKey", sslKey);
		System.setProperty("sslCertfile", sslCertfile);

	}

	// ----------------------------------
	public void loadServiceConfigurationProperties()
	{
		Properties p = net.sf.gratia.util.Configuration.getProperties();
		String vos;
		String voName = "";
		TreeSet tempVOs = new TreeSet();	// sorted arrayList with unique names for the voList
		List tempFQANs  = new ArrayList();	// FQAN list from service properties file
		List tempDNs    = new ArrayList();	// DN list from service properties file

		try
		{
			_configPath      = net.sf.gratia.util.Configuration.getConfigurationPath() + "/";
			_vomsServerFile  = p.getProperty("service.voms.connections");
			secureConnection = p.getProperty("service.secure.connection");
			if (secureConnection == null)
				secureConnection = "";
			openConnection   = p.getProperty("service.open.connection");
			if (openConnection == null)
				openConnection = "";
			dbConnection     = p.getProperty("service.mysql.url");
			if (dbConnection == null)
				dbConnection = "";
			servicesVersion  = p.getProperty("gratia.services.version");
			if (servicesVersion == null)
				servicesVersion = "";
			sslCAFiles       = p.getProperty("service.ca.certificates");
			if (sslCAFiles == null)
				sslCAFiles = "/etc/grid-security/certificates/";
			sslCAFiles = sslCAFiles + "*.0";
			sslKey           = p.getProperty("service.vdt.key.file");
			if (sslKey == null)
				sslKey = "/etc/grid-security/http/httpkey.pem";
			sslCertfile      = p.getProperty("service.vdt.cert.file");
			if (sslCertfile == null)
				sslCertfile = "/etc/grid-security/http/httpcert.pem";

			Enumeration e = p.propertyNames ();
			String key = "";
			String value = "";
			while ( e.hasMoreElements() )
			{
				key =  (String) e.nextElement();
				if (key.indexOf("service.admin.FQAN") >= 0)
				{
					value = p.getProperty (key);

					if (value != null)
					{
						value = value.trim();
						tempFQANs.add(value);

					// Extract the node name from the identity, example of identity:
					//       /gratia-vo1/Role=VO-Admin
					// we want to extract gratia-vo1 to populate the pull down menu.

						String[] VoFullStr = value.split ("/");
						voName = VoFullStr[1];
						if (voName != null)
							tempVOs.add(voName);
					}
				}
				else if (key.indexOf("service.admin.DN") >= 0)
				{
					value =  p.getProperty (key);

					if (value != null)
					{
						value = value.trim();
						tempDNs.add(value);
					}
				}
			}
			// Convert the ArrayLists to string arrays
			voList     = (String[])tempVOs.toArray(new String[tempVOs.size()]);
			voFQANlist = (String[])tempFQANs.toArray(new String[tempFQANs.size()]);
			DNlist     = (String[])tempDNs.toArray(new String[tempDNs.size()]);
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
			String msg = "ERROR reading file: " + voFile + "<br>" + e.toString();
			return msg;
		}

		// Convert now to arrays
		vomsNameList = (String[])voVOMSNam.toArray(new String[voVOMSNam.size()]);
		vomsUrlList  = (String[])voVOMSUrl.toArray(new String[voVOMSUrl.size()]);
		return "";
	}

	// ----------------------------------
	public String checkVOMSFile ()
	{

		File vomsFile = new File(_configPath + _vomsServerFile);
		String msg1 = "<hr><p class='txterror'>File of VOMS servers "; 
		String msg2 = _configPath + _vomsServerFile + "</em> <br>Please contact your administrator to check your installation.<br>Additional administrative services are not available until this is resolved.";
		String msg3 = "<br>Please contact your administrator to check your installation.<br>Additional administrative services are not available until this is resolved.";
		msg2 += "</p><hr>";
		msg3 += "</p><hr>";
		
		if (_vomsServerFile == null)
			return msg1 + " was NOT specified in the gratia service properties." + msg3;

		if (!vomsFile.exists()) 
			return  msg1 + " does not exist: <em>" + msg2;

		if (!vomsFile.isFile())
			return  msg1 + " is not a file: <em>" + msg2;

		if (!vomsFile.canRead())
			return  msg1 + " is not readable: <em>" + msg2;

		return "";
	}

	// ----------------------------------
	public String connectVOname(String voname)
	{
		if (_vomsServerFile != null)
		{
			// check if the file exists etc
			String msg = checkVOMSFile();
			if ( msg.length() > 1)
				return msg;

			// Read voms-servers file: the file resides in the tomcat/gratia area
			String fileStatus = loadVOServers(_configPath + _vomsServerFile);
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
				this.groupsVOMS = stub.listGroups(userDN, userCA);
			}
			catch (Exception e)
			{
				return "ERROR connecting to VOMS location: <br>" + VomsLocation + "<br>" + e.toString();
			}
		}
		else
		{
			String err = "<hr><p class='txterror'>";
			err += "File of VOMS servers was NOT specified in the gratia service properties. ";
			err += "<br>Please contact your administrator to check your installation.";
			err += "<br>Additional administrative services are not available until this is resolved.</p><hr>";
			return err;
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

	public String[] getDNlist()
	{
		return this.DNlist;
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

	// ----------------------------------
	public String getVomsFile()
	{
		return this._vomsServerFile;
	}

}

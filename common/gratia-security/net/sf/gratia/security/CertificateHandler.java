package net.sf.gratia.security;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.text.*;
import javax.servlet.*;
import javax.servlet.http.*;
//import java.security.cert.*;

//import org.bouncycastle.asn1.*;
//import org.bouncycastle.asn1.util.*;

import java.security.cert.X509Certificate;
import org.glite.security.SecurityContext;
import org.glite.security.util.DNHandler;
import org.glite.security.util.DNImpl;
import org.glite.security.voms.service.admin.VOMSAdmin;
import org.glite.security.voms.service.admin.VOMSAdminServiceLocator;

import net.sf.gratia.util.*;
import java.sql.*;

public class CertificateHandler
{
	HttpServletRequest request = null;
	X509Certificate[] certs;
	//
	// database related
	//
	String driver = "";
	String url    = "";

	String password = "";
	Connection connection;
	Statement statement;
	ResultSet resultSet;

	//---- certificate information -----
	String user   = null;
	String dn     = null;
	String ca     = null;
	String vo     = null;
	String voServer = null;
	String[] VoNodes = {"VDT"};

	// --- VOMS information ---
	String[] roles  = null;
	String[] rolesFound  = null;
	String[] groups = null;

	String voSelect = null;
	String voConnect = null;

	//---------------------------------------------------
	public CertificateHandler(HttpServletRequest request)
	{
		this.request = request;

		System.out.println("### CertificateHandler### In CertificateHandler class");
		// ---- reading certificate for dn/ca for VOMS call ---
		SecurityContext sc = new SecurityContext();
		SecurityContext.setCurrentContext(sc);

		System.out.println("### CertificateHandler### reading certificate");
		certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

		if (certs==null) {
			return;
		} else {
			System.out.println("### CertificateHandler### got certificate");
		}

		sc.setClientCertChain(certs);

		if (sc.getClientName () != null)
			sc.setClientName (new DNImpl (sc.getClientName ()).getX500());

		if (sc.getIssuerName () != null)
			sc.setIssuerName (new DNImpl(sc.getIssuerName ()).getX500());

		System.out.println("### CertificateHandler### getting names ");
		X509Certificate cs=sc.getClientCert();
		this.dn=((DNImpl) DNHandler.getSubject(cs)).getX500();
		this.ca=((DNImpl) DNHandler.getIssuer(cs)).getX500();

		System.out.println("### CertificateHandler### DN: "+dn);
		System.out.println("### CertificateHandler### CA: "+ca);

		// ---- setting up to call VOMS (not sure how much is needed ---
		// this needs to be set some other way (TRUSTED_CA on VDT dist /
				System.setProperty("sslCAFiles","/etc/grid-security/certificates/*.0");
		// this may be ok
				System.setProperty("axis.socketSecureFactory","org.glite.security.trustmanager.axis.AXISSocketFactory");
		// these needs to be set some other way (I think there is a properties file)
				System.setProperty("sslKey","/etc/grid-security/http/httpkey.pem");
				System.setProperty("sslCertfile","/etc/grid-security/http/httpcert.pem");

		// ---- calling VOMS ---

		/**
		System.out.println("### CertificateHandler:  calling connect to vo ### ");
		String vonam = "oiv_test1";
		voConnect = vonam.trim();


		String retcode = connectVOname(voConnect);
		System.out.println("### CertificateHandler:  after calling connect to vo, code =  " + retcode +  "### ");
		**/

	}
	// ----------------------------------

	public String connectVOname(String voname)
	{
		voConnect = voname.trim();
		//String VomsLocation = "https://gratiax34.fnal.gov:8443/voms/" + voConnect + "/services/VOMSAdmin";

		String VomsLocation = getVoServer(voConnect);	// still hardcoded for now

		System.out.println("### CertificateHandler:connectVOname ###Locating SELECTED VOMS Server"+VomsLocation);
		System.out.println("### CertificateHandler:connectVOname, dn " + this.dn + " ==== ");
		System.out.println("### CertificateHandler:connectVOname, ca " + this.ca + " ==== ");
		//return "Connected";
		// ---- calling VOMS ---
		/** **/
		final VOMSAdmin stub;
		try {
			// other test VOMS servers
			//String VomsLocation = "https://gratiax34.fnal.gov:8443/voms/" + voConnect + "/services/VOMSAdmin";

			System.out.println("### CertificateHandler: connectVOname : ###Locating VOMS Server"+VomsLocation);
			final VOMSAdminServiceLocator locator = new VOMSAdminServiceLocator();
			stub = locator.getVOMSAdmin(new URL(VomsLocation));

			System.out.println("### CertificateHandler: connectVOname : ###  Getting roles from  VOMS Server");
			roles  = stub.listRoles( dn, ca);
			this.rolesFound = roles;
			System.out.println("### CertificateHandler: connectVOname : ###  Getting groups from  VOMS Server");
			groups = stub.listGroups(dn,ca);
		} catch (Exception e) {
			System.out.println("### CertificateHandler: connectVOname : ###  Unable to contact VOMS service");
			//e.printStackTrace();
			return "Connection Error";
		}
		System.out.println("### CertificateHandler: connectVOname : ### Roles:");
		for( int i=0; i < roles.length; i++) {
			System.out.println("### ====== role =    "+roles[i]);
			System.out.println("### ====== roleFound =    "+rolesFound[i]);
		}
		System.out.println("### CertificateHandler: connectVOname : ### Groups:");
		for( int i=0; i < groups.length; i++) {
			System.out.println("### ====== group =     "+groups[i]);
		}
		return "Connected";   /** **/
	}

	//---------------------------------------------------
	public String getVoServer(String voCon)
	{
		this.voServer = "https://gratiax34.fnal.gov:8443/voms/" + voCon + "/services/VOMSAdmin";
		return this.voServer;
	}

    //---------------------------------------------------
	public String[] getVoNodes()
	{
		String[] VoNodesInit = {"VDT","oiv_test1","oiv_test2","cms","cdf"};
		this.VoNodes = VoNodesInit;
		return this.VoNodes;
	}
	//---------------------------------------------------
	public String getDN()
	{
		if (this.dn == null)
			return "UnknownDn";
		return this.dn;
	}

	//---------------------------------------------------
	public String getName()
	{

		String dnString="";
		String[] subwords;

		if (this.dn == null){
			return "GratiaUserName";
		} else {
			dnString = this.dn;
			//System.out.println("### CertificateHandler:getName: Extracting name from dn " + dnString);
			String[] dnWords = dnString.split ("/");
			for (int i=0; i < dnWords.length; i++){
			//System.out.println (dnWords[i]);
				subwords = dnWords[i].split ("=");
				for (int ij=0; ij < subwords.length; ij++){
					//System.out.println ("sub= " + subwords[ij]);
					if (subwords[ij].equals("UID") ) {
						String userName=subwords[ij+1];
						//System.out.println("----- got userName = " + userName);
						this.user = userName;
						return this.user;
					}
				}
			}
		}
		return "GratiaUserName";
	}

    // ----------------------------------
	public String[] getRole()
	{
		String[] initRoles ={"GratiaUserRole"};

		if (this.rolesFound== null)
			return initRoles;
		return this.rolesFound;
		/*if (this.roles == null)
				return "GratiaUserRole";
		return "Test";*/
	}

	// ----------------------------------
	public String getGroups()
	{
		if (this.groups == null)
			return "GratiaUserGrp";
		return "Test";
	}

	// ----------------------------------
	public String getVO()
	{
		if (this.vo == null)
			 return "Unknown";
		return vo;
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

		return voSelect;
	}

	// ----------------------------------
	public String getSubtitle()
	{
		String dq = "'";
		String command = "select * from Role where role = " + dq + getRole() + dq;
		String subtitle = "Test Subtitle";
		openConnection();
		try
		{
			statement = connection.prepareStatement(command);
			resultSet = statement.executeQuery(command);
			while(resultSet.next())
			{
				subtitle = resultSet.getString("subtitle");
			}
			resultSet.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			closeConnection();
			return subtitle;
		}

		return subtitle;
	}

	public void dump()
	{
		StringBuffer buffer = new StringBuffer();
		String cr = "\n";
		int i;
		return; // wanted to terminate as this whole thing servers no purpose

	}

	public void append(String string)
	{
		String path = System.getProperties().getProperty("catalina.home");
		path = path + "/logs/gratia-certificates.log";
		try
		{
			RandomAccessFile output = new RandomAccessFile(path,"rw");
			output.seek(output.length());
			output.write(string.getBytes());
			output.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void openConnection()
	{
		try
		{
			Properties p = Configuration.getProperties();
			driver = p.getProperty("service.mysql.driver");
			url = p.getProperty("service.mysql.url");
			user = p.getProperty("service.mysql.user");
			password = p.getProperty("service.mysql.password");
		}
		catch (Exception ignore)
		{
		}
		try
		{
			Class.forName(driver).newInstance();
			connection = DriverManager.getConnection(url,user,password);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void closeConnection()
	{
		try
		{
			connection.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
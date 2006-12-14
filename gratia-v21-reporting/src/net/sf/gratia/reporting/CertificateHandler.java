package net.sf.gratia.reporting;

import java.io.*;
import java.util.*;
import java.text.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.security.cert.*;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.util.*;
import org.glite.security.voms.*;

import net.sf.gratia.services.*;
import java.sql.*;

public class CertificateHandler
{
		HttpServletRequest request = null;
		X509Certificate certs[];
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


		public CertificateHandler(HttpServletRequest request)
		{
				this.request = request;
				certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
		}

		public String getName()
		{
				if (certs == null)
						return "GratiaUser";
				if (certs.length == 0)
						return "GratiaUser";
				return certs[0].getSubjectX500Principal().toString().toLowerCase();
		}

		public String getRole()
		{
				if (certs == null)
						return "GratiaUser";
				if (certs.length == 0)
						return "GratiaUser";
				return "Test";
		}

		public String getVO()
		{
				if (certs == null)
						return "Unknown";
				if (certs.length == 0)
						return "Unknown";
				return "Test";
		}

		public String getSubtitle()
		{
				String dq = "'";
				String command = "select * from RolesTable where role = " + dq + getRole() + dq;
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

		public void print1(String oid,byte[] value)
		{
				try
						{
								ASN1InputStream eIn = new ASN1InputStream(new ByteArrayInputStream(value));
								ASN1OctetString ext = (ASN1OctetString)eIn.readObject();
								ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(ext.getOctets()));
								Object obj = aIn.readObject();
								System.out.println("print1: oid: " + oid + " value: " +  ASN1Dump.dumpAsString(obj));
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}

		public void print2()
		{
				VOMSValidator validator = new VOMSValidator(certs).parse();
				List attributeList = validator.getVOMSAttributes();
				for (Iterator x = attributeList.iterator(); x.hasNext();)
						{
								VOMSAttribute attr = (VOMSAttribute) x.next();
								String fqan = attr.getFullyQualifiedAttributes().toString();
								System.out.println("print2: " + fqan);
						}
		}

		public void dump()
		{
				StringBuffer buffer = new StringBuffer();
				String cr = "\n";
				int i;

				if (certs == null)
						return;
				if (certs.length == 0)
						return;
				
				print2();

				for (i = 0; i < certs.length; i++)
						{
								String dn = certs[i].getSubjectX500Principal().toString();
								buffer.append("" + new java.util.Date() + ": " + dn + cr);
								System.out.println("dn: " + dn);
								Set critical = certs[i].getCriticalExtensionOIDs();
								Set noncritical = certs[i].getNonCriticalExtensionOIDs();
								if ((critical != null) && (! critical.isEmpty()))
										for (Iterator iterator = critical.iterator(); iterator.hasNext();)
												{
														String oid = (String) iterator.next();
														byte[] value = certs[i].getExtensionValue(oid);
														print1(oid,value);
												}
								if ((noncritical != null) && (! noncritical.isEmpty()))
										for (Iterator iterator = noncritical.iterator(); iterator.hasNext();)
												{
														String oid = (String) iterator.next();
														byte[] value = certs[i].getExtensionValue(oid);
														print1(oid,value);
												}
						}
				append(buffer.toString());
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

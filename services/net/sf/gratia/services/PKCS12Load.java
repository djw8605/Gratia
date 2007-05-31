package net.sf.gratia.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
//import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class PKCS12Load
{
		public void load(String inputFile,String outputFile) throws Exception
		{
				File fileIn = new File(inputFile);
				File fileOut = new File(outputFile);

				if (!fileIn.canRead()) 
						{
								System.err.println("Unable to access input keystore: " + fileIn.getPath());
								System.exit(2);
						}

				if (fileOut.exists() && ! fileOut.canWrite()) 
						{
								System.err.println("Output file is not writable: " + fileOut.getPath());
								System.exit(2);
						}

				KeyStore kspkcs12 = KeyStore.getInstance("pkcs12");
				KeyStore ksjks = KeyStore.getInstance("jks");

				LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
				
				char[] inpassword = "server".toCharArray();
				char[] outpassword = "server".toCharArray();

				kspkcs12.load(new FileInputStream(fileIn), inpassword);

				ksjks.load(
									 (fileOut.exists())
									 ? new FileInputStream(fileOut) : null, outpassword);

				Enumeration eAliases = kspkcs12.aliases();
				int n = 0;
				while (eAliases.hasMoreElements()) 
						{
								String strAlias = (String) eAliases.nextElement();
								System.err.println("Alias " + n++ + ": " + strAlias);

								if (kspkcs12.isKeyEntry(strAlias)) 
										{
												System.err.println("Adding key for alias " + strAlias);
												Key key = kspkcs12.getKey(strAlias, inpassword);

												Certificate[] chain = kspkcs12.getCertificateChain(strAlias);

												ksjks.setKeyEntry(strAlias, key, outpassword, chain);
										}
						}

				OutputStream out = new FileOutputStream(fileOut);
				ksjks.store(out, outpassword);
				out.close();
		}

}


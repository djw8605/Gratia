package net.sf.gratia.security;

import net.sf.gratia.services.*;

import sun.misc.BASE64Encoder;
import java.security.cert.Certificate;
import java.security.*;
import java.io.File;
import java.io.FileInputStream;

public class Export 
{
		public Export()
		{
		}

    public void export(String fileName, String aliasName, String pass,String outputfile) throws Exception
		{
				XP xp = new XP();
				KeyStore ks = KeyStore.getInstance("JKS");

				char[] passPhrase = pass.toCharArray();
				BASE64Encoder myB64 = new BASE64Encoder();
	

				File certificateFile = new File(fileName);
				ks.load(new FileInputStream(certificateFile), passPhrase);

				KeyPair kp = getPrivateKey(ks, aliasName, passPhrase);
		
				PrivateKey privKey = kp.getPrivate();
	

				String b64 = myB64.encode(privKey.getEncoded());

				if (outputfile != null)
						{
								StringBuffer buffer = new StringBuffer();

								buffer.append("-----BEGIN PRIVATE KEY-----" + "\n");
								buffer.append(b64 + "\n");
								buffer.append("-----END PRIVATE KEY-----" + "\n");
								xp.save(outputfile,buffer.toString());
						}
				else
						{
								System.out.println("-----BEGIN PRIVATE KEY-----");
								System.out.println(b64);
								System.out.println("-----END PRIVATE KEY-----");
						}
		}

		public KeyPair getPrivateKey(KeyStore keystore, String alias, char[] password) 
		{
        try 
						{
								// Get private key
								Key key = keystore.getKey(alias, password);
								System.out.println("Got Key: " + key);
								if (key instanceof PrivateKey) 
										{
												// Get certificate of public key
												Certificate cert = keystore.getCertificate(alias);
    
												// Get public key
												PublicKey publicKey = cert.getPublicKey();
    
												// Return a key pair
												return new KeyPair(publicKey, (PrivateKey)key);
										}
						} 
				catch (UnrecoverableKeyException e) 
						{
								e.printStackTrace();
						} 
				catch (NoSuchAlgorithmException e) 
						{
								e.printStackTrace();
						} 
				catch (KeyStoreException e) 
						{
								e.printStackTrace();
						}
        return null;
    }

    public static void main(String args[]) throws Exception
		{
				for (int i = 0; i < args.length; i++) {

						System.out.println(i + ": "+ args[i]);
				}
				if (args.length < 2) 
						{
								//Yes I know this sucks (the password is visible to other users via ps
								// but this was a quick-n-dirty fix to export from a keystore to pkcs12
								// someday I may fix, but for now it'll have to do.
								System.err.println("Usage: java ExportPriv <keystore> <alias> <password>");
								System.exit(1);
						}
				Export myep = new Export();
				myep.export(args[0], args[1], args[2],null);
    }

}




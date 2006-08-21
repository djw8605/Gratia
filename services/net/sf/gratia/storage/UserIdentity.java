package net.sf.gratia.storage;


/**
 * <p>Title: UserIdentity</p>
 *
 * <p>Description: Contains all the known information about the user.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 */
public class UserIdentity {
    private String LocalUserId;
	private String GlobalUsername;
	private KeyInfoType KeyInfo;
	private String VOName;
	private String CommonName;

    public UserIdentity() {
    }

	public void setLocalUserId(String LocalUserId)
	{
		this.LocalUserId = LocalUserId;
	}

	public String getLocalUserId()
	{
		return LocalUserId;
	}

	public void setGlobalUsername(String name)
	{
		this.GlobalUsername = name;
	}

	public String getGlobalUsername()
	{
		return GlobalUsername;
	}

	public void setKeyInfo(KeyInfoType KeyInfo) {
        this.KeyInfo = KeyInfo;
    }

    public KeyInfoType getKeyInfo() {
        return KeyInfo;
    }
    
    public void setVOName(String name)
	{
		this.VOName = name;
	}

	public String getVOName()
	{
		return VOName;
	}
	
	public void setCommonName(String name)
	{
		this.CommonName = name;
	}

	public String getCommonName()
	{
		return CommonName;
	}

    public String toString() {
        return "(LocalId: "+GlobalUsername+" "+LocalUserId+" "+KeyInfo+" "+VOName+" "+CommonName+")";
    }
    public String asXml() {
        String output = "<UserIdentity>\n";
		if (GlobalUsername != null) output = output + "\t<GlobalUsername>" + GlobalUsername + "</GlobalUsername>\n";
		if (LocalUserId != null) output = output + "\t<LocalUserId>" + LocalUserId + "</LocalUserId>\n";
		if (KeyInfo != null) output = output + "\t" + KeyInfo.asXML();
		if (VOName != null) output = output + "\t<VOName>" + VOName + "</VOName>\n";
		if (CommonName != null) output = output + "\t<CommonName>" + CommonName + "</CommonName>\n";
        output = output + "</UserIdentity>\n";
        return output;
    }
}

package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

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
	private String ReportableVOName;
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
	
    public void setReportableVOName(String name)
	{
		this.ReportableVOName = name;
	}

	public String getReportableVOName()
	{
		return ReportableVOName;
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
      StringBuilder output = new StringBuilder();
      asXml(output);
      return output.toString();
   }
   
   public void asXml(StringBuilder output) {
      output.append("<UserIdentity>\n");
      if (GlobalUsername != null) output.append("\t<GlobalUsername>" +
                                                StringEscapeUtils.escapeXml(GlobalUsername) + "</GlobalUsername>\n");
      if (LocalUserId != null) output.append("\t<LocalUserId>" +
                                             StringEscapeUtils.escapeXml(LocalUserId) + "</LocalUserId>\n");
      if (KeyInfo != null) {
         output.append("\t");
         KeyInfo.asXml(output);
      }
      if (VOName != null) output.append("\t<VOName>" +
                                        StringEscapeUtils.escapeXml(VOName) +
                                        "</VOName>\n");
      if (ReportableVOName != null) output.append("\t<ReportableVOName>" +
                                                  StringEscapeUtils.escapeXml(ReportableVOName) + "</ReportableVOName>\n");
      if (CommonName != null) output.append("\t<CommonName>" +
                                            StringEscapeUtils.escapeXml(CommonName) +
                                            "</CommonName>\n");
      output.append("</UserIdentity>\n");
    }
}

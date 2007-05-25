package net.sf.gratia.storage;
import java.util.Date;

/**
 * <p>Title: Record </p>
 *
 * <p>Description: Interface of the Gratia Records
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 */
public interface Record
{
   public void addRawXml(String RawXml);
   public void setRawXml(String RawXml);
   public String getRawXml();
   public void addExtraXml(String ExtraXml);
   public void setExtraXml(String ExtraXml);
   public String getExtraXml();
   public String asXML();

   public StringElement getSiteName();
   public StringElement getProbeName();
   public Date getServerDate();
   public void setServerDate(Date value);
   public String computemd5() throws Exception;
   public String getmd5();
   public void setmd5(String md5set);

}
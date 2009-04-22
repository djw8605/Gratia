package net.sf.gratia.storage;
import java.util.Date;

/**
 * <p>Title: RecordInterface </p>
 *
 * <p>Description: Interface of the Gratia Records</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 */
public interface RecordInterface {
    public void addRawXml(String RawXml);
    public void setRawXml(String RawXml);
    public String getRawXml();
    public void addExtraXml(String ExtraXml);
    public void setExtraXml(String ExtraXml);
    public String getExtraXml();
    public String asXML();
    public String asXML(boolean formd5, boolean optional);

    public StringElement getSiteName();
    public StringElement getProbeName();
    public Date getServerDate();
    public void setServerDate(Date value);
    public String computemd5(boolean optional) throws Exception;
    public String getmd5();
    public void setmd5(String md5set);
    public int getRecordId();
    public void setRecordId(int RecordId);

    public Probe getProbe();
    public void setProbe(Probe p);
    public boolean setDuplicate(boolean b);

    public String getTableName();

    public Date getDate();  // Returns the date this records is reporting about.
    public Date getExpirationDate(); // Returns the date of the oldest raw records we keep

    // In case anything special needs to be done after saving.
    public void executeTrigger(org.hibernate.Session session) throws Exception;

    public void AttachContent( org.hibernate.Session session ) throws Exception;
}

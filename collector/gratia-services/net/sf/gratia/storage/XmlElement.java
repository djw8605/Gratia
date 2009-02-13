package net.sf.gratia.storage;

/**
 * <p>Title: XmlElement</p>
 *
 * <p>Description: Interface to allow print as Xml of the JobUsageRecord objects</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author Philippe Canal
 * @version 1.0
 */
public interface XmlElement {
    /**
     * asXml
     *
     * @param elementName String
     * @return String
     */
    public String asXml(String elementName);
    public void asXml(StringBuilder output, String elementName);
}

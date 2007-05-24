package net.sf.gratia.storage;

import java.util.ArrayList;
import org.dom4j.Element;

/**
 * <p>Title: RecordLoader</p>
 *
 * <p>Description: Defines the interface for the parsing and transformation of the XML Usage Record 
 * (via a sax Element).</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 */
public interface RecordLoader
{
   public ArrayList ReadRecords(Element eroot) throws Exception;
   public Record ReadRecord(Element element) throws Exception;

}
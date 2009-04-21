package net.sf.gratia.storage;

import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Logging;

import java.text.*;
import java.util.Date;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.datatype.*;

import org.apache.commons.lang.StringEscapeUtils;


/**
 * <p>Title: DateElement</p>
 *
 * <p>Description: Correspond to a specific date.  Handles conversion to and 
 * from the xml format</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 */
public class DateElement implements XmlElement {
    private Date Value;
    private String Description;
    private String Type;
    private static Pattern wrongDateFixer =
        Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s+(\\d+:\\d+:[\\d\\.]+)");

    public DateElement() {
    }

    static javax.xml.datatype.DatatypeFactory gFactory = null;

    public void setValue(String str) throws Exception {
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        XMLGregorianCalendar cal;
        try {
           if (gFactory == null) {
               gFactory = javax.xml.datatype.DatatypeFactory.newInstance();
           }
           cal = gFactory.newXMLGregorianCalendar(str.trim());
        } catch (Exception e) {
            Logging.debug("DateElement: caught bad date element, \"" +
                          str.trim() + "\" -- attempting to fix\"");
            // Mitigation
            Matcher m = wrongDateFixer.matcher(str.trim());
            if (m.lookingAt()) {
                String newStr = m.group(1) + "T" + m.group(2) + "Z";
                cal = gFactory.newXMLGregorianCalendar(newStr);
                Logging.fine("DateElement: caught problem with date element, \"" +
                             str.trim() + "\", fixed to \"" + newStr + "\"");
            } else {
                throw e;
            }
        }
        GregorianCalendar jcal = cal.toGregorianCalendar();
        this.Value = new Date( jcal.getTimeInMillis() );
    }

    public void setValue(Date Value) {
        this.Value = Value;
    }

    public Date getValue() {
        return Value;
    }

    public void setDescription(String Description) {
        this.Description = Description;
    }

    public String getDescription() {
        return Description;
    }

    public String toString() {
        DateFormat f = new java.text.SimpleDateFormat("dd MMMM yyyy 'at' HH:mm:ss z" );
        String output = f.format(Value);
        if (Type != null) output = output + " type: " + Type;
        if (Description != null) output = output + " (" + Description + ")";
        return output;
    }

    public void setType(String Type) {
        this.Type = Type;
    }

    public String getType() {
        return Type;
    }

    public String asXml(String elementName) {
       StringBuilder output = new StringBuilder();
       asXml(output,elementName);
       return output.toString();
    }
     
   public void asXml(StringBuilder output, String elementName) {
        output.append("<"+elementName+" ");
        if (Description != null) output.append("urwg:description=\"" + Description + "\" ");
        if (Type != null) output.append("urwg:type=\"" + Type + "\" ");
        output.append(">");

        //
        // glr: a hack to get around conversion problems switching from date <-> xml
        //
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        output.append(format.format(Value));

        output.append("</" + elementName + ">\n");
    }
}

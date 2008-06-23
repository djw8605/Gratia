package net.sf.gratia.storage;

import net.sf.gratia.util.Configuration;

import java.text.*;
import java.util.Date;
import java.util.TimeZone;
import java.util.GregorianCalendar;

import javax.xml.datatype.*;


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
    public DateElement() {
    }

    public void setValue(String str) throws DatatypeConfigurationException {
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        javax.xml.datatype.DatatypeFactory fac = javax.xml.datatype.
                                                 DatatypeFactory.
                                                 newInstance();
        XMLGregorianCalendar cal = fac.newXMLGregorianCalendar(str.trim());
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
        String output = "<"+elementName+" ";
        if (Description != null) output = output + "urwg:description=\"" + Description + "\" ";
        if (Type != null) output = output + "urwg:type=\"" + Type + "\" ";
        output = output + ">";

        //
        // glr: a hack to get around conversion problems switching from date <-> xml
        //
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        output = output + format.format(Value);

        output = output + "</" + elementName + ">\n";
        return output;
    }
}

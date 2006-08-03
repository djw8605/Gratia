package net.sf.gratia.storage;

import javax.xml.datatype.DatatypeConfigurationException;

/**
 * <p>Title: DurationElement</p>
 *
 * <p>Description: Correspond to a length of time.  Handles the conversion
 * to and from the xml format.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 */
public class DurationElement implements XmlElement {
    private double Value;
    private String Description;
    private String Type;
    public DurationElement() {
    }

    public void setValue(String str) throws DatatypeConfigurationException {
        this.Value = Utils.StringToDuration(str);
    }

   public void setValue(double Value) {
        this.Value = Value;
    }

    public double getValue() {
        return Value;
    }

    public void setDescription(String Description) {
        this.Description = Description;
    }

    public String getDescription() {
        return Description;
    }

    public void setType(String Type) {
        this.Type = Type;
    }

    public String getType() {
        return Type;
    }

    public String toString() {
        String output = "" + Value;
        if (Type != null) output = output + " type: " + Type;
        if (Description != null) output = output + " (" + Description + ")";

        return output;
    }

    public String asXml(String elementName, String default_type) {
        String output = "<"+elementName+" ";
        if (Description != null) output = output + "urwg:description=\"" + Description + "\" ";
        if (Type != null) output = output + "urwg:type=\"" + Type + "\" ";
        else if (default_type != null) output = output + "urwg:type=\"" + default_type + "\" ";
        output = output + ">";
        try {
            output = output + Utils.DurationToXml(Value);
        } catch (DatatypeConfigurationException ex) {
            output = output + Value;
        }
        output = output + "</" + elementName + ">\n"; //FIXME: I need the right format
        return output;
    }

    public String asXml(String elementName) {
        return asXml(elementName,null);
    }    
}

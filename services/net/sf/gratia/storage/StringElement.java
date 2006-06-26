package net.sf.gratia.storage;



/**
 * <p>Title: StringElement</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 */

public class StringElement implements XmlElement {
    private String Value;
    private String Description;
    private String Type;

    public StringElement() {
    }

    public void setValue(String Value) {
        this.Value = Value;
    }

    public String getValue() {
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
        String output = Value;
        if (Type != null) output = output + " type: " + Type;
        if (Description != null) output = output + " (" + Description + ")";
        return output;
    }

    public String asXml(String elementName) {
        String output = "<"+elementName+" ";
        if (Description != null) output = output + "urwg:description=\"" + Description + "\" ";
        if (Type != null) output = output + "urwg:type=\"" + Type + "\" ";
        output = output + ">" + Value + "</" + elementName + ">\n";
        return output;
    }
}

package net.sf.gratia.storage;

import net.sf.gratia.util.Configuration;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;

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

   public String asXml(String elementName, String type_name, String duration_type) {
      StringBuilder output = new StringBuilder();
      asXml(output,elementName);
      return output.toString();
   }
   
   public void asXml(StringBuilder output, String elementName, String type_name, String duration_type) {
      output.append("<"+elementName+" ");
      if (Description != null) output.append("urwg:description=\"" + StringEscapeUtils.escapeXml(Description) + "\" ");
      if (Type != null) output.append("urwg:" +
         StringEscapeUtils.escapeXml(type_name) +
         "=\"" + StringEscapeUtils.escapeXml(Type) + "\" ");
      else if (duration_type != null) output.append("urwg:" +
         StringEscapeUtils.escapeXml(type_name) +
         "=\"" + StringEscapeUtils.escapeXml(duration_type) + "\" ");
      output.append(">");
      try {
         output.append(Utils.DurationToXml(Value));
      } catch (DatatypeConfigurationException ex) {
         output.append(Value);
      }
      output.append("</" + elementName + ">\n"); //FIXME: I need the right format
   }

   public String asXml(String elementName) {
      return asXml(elementName, "type", null);
   }    
   
   public void asXml(StringBuilder output, String elementName) {
      asXml(output, elementName, "type", null);
   }    
}

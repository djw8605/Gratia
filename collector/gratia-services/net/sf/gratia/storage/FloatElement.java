package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * <p>Title: FloatElement</p>
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
public class FloatElement implements XmlElement {
    private double Value;
    private String Description;
    private String Unit;
    private String Formula;

    public FloatElement() {
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

    public void setUnit(String Unit) {
        this.Unit = Unit;
    }

    public String getUnit() {
        return Unit;
    }

    public void setFormula(String Formula) {
        this.Formula = Formula;
    }

    public String getFormula() {
        return Formula;
    }

    public String toString() {
        String output = "" + Value;
        if (Unit != null) output = output + " " + Unit;
        if (Formula != null) output = output + " [" + Formula + "]";
        output = output + " (" + Description + ")";
        return output;
    }

   public String asXml(String elementName) {
      StringBuilder output = new StringBuilder();
      asXml(output,elementName);
      return output.toString();
   }
   
   public void asXml(StringBuilder output, String elementName) {
      output.append("<"+elementName+" ");
      if (Description != null) output.append("urwg:description=\"" +
                                             StringEscapeUtils.escapeXml(Description) + "\" ");
      if (Unit != null) output.append("urwg:unit=\"" + StringEscapeUtils.escapeXml(Unit) + "\" ");
      if (Formula != null) output.append("urwg:formula=\"" + StringEscapeUtils.escapeXml(Formula) + "\" ");
      output.append(">" + Value + "</" + elementName + ">\n");
   }
}

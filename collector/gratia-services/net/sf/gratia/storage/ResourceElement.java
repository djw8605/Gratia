package net.sf.gratia.storage;

import net.sf.gratia.util.Configuration;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * <p>Title: ResourceElement</p>
 *
 * <p>Description: Generic resource information</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 */

public class ResourceElement implements XmlElement {
    private double Value;
    private String Metrics;
    private String Description;
    private String Unit;
    private String StorageUnit;
    private Double PhaseUnit;
    private String Type;

    public ResourceElement() {
    }

    public void setValue(double Value) {
        this.Value = Value;
    }

    public double getValue() {
        return Value;
    }

    public void setMetrics(String Metrics) {
        this.Metrics = Metrics;
    }

    public String getMetrics() {
        return Metrics;
    }

    public void setDescription(String Description) {
        this.Description = Description;
    }

    public String getDescription() {
        return Description;
    }

    public void setStorageUnit(String StorageUnit) {
        this.StorageUnit = StorageUnit;
    }

    public String getStorageUnit() {
        return StorageUnit;
    }

    public void setPhaseUnit(String PhaseUnit) throws
            DatatypeConfigurationException {
       this.PhaseUnit = new Double(Utils.StringToDuration(PhaseUnit));
   }

    public void setPhaseUnit(double PhaseUnit) {
        this.PhaseUnit = new Double(PhaseUnit);
    }

    public double getPhaseUnit() {
        if (PhaseUnit == null) return 0.0;
        return PhaseUnit.doubleValue();
    }

    public String toString() {
        String output = "" + Value;
        if (Unit != null) output = output + " " + Unit;
        if (StorageUnit != null) output = output + " " + StorageUnit;
        if (PhaseUnit != null) output = output + " by " + PhaseUnit + "s";
        if (Type != null) output = output + " type:" + Type;
        if (Metrics != null) output = output + " metric:" + Metrics;
        if (Description != null) output = output + " (" + Description + ")";
        return output;
    }

   public String asXml(String elementName) {
      StringBuilder output = new StringBuilder();
      asXml(output,elementName);
      return output.toString();
   }
   
   public void asXml(StringBuilder output, String elementName) {
      output.append("<"+elementName+" ");
      if (Description != null) output.append("urwg:description=\"" + StringEscapeUtils.escapeXml(Description) + "\" ");
      if (Type != null) output.append("urwg:type=\"" + StringEscapeUtils.escapeXml(Type) + "\" ");
      if (Unit != null) output.append("urwg:unit=\"" + StringEscapeUtils.escapeXml(Unit) + "\" ");
      try
      {
         if (PhaseUnit != null) output.append("urwg:phaseUnit=\"" + Utils.DurationToXml(PhaseUnit) + "\" ");
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      if (StorageUnit != null) output.append("urwg:storageUnit=\"" + StringEscapeUtils.escapeXml(StorageUnit) + "\" ");
      if (Metrics != null) output.append("urwg:metrics=\"" + StringEscapeUtils.escapeXml(Metrics) + "\" ");
      output.append(">" + Value + "</" + elementName + ">\n");
   }

    public void setType(String Type) {
        this.Type = Type;
    }

    public String getType() {
        return Type;
    }

    public void setUnit(String Unit) {
        this.Unit = Unit;
    }

    public String getUnit() {
        return Unit;
    }


}

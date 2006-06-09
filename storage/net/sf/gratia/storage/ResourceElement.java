package net.sf.gratia.storage;

import javax.xml.datatype.DatatypeConfigurationException;



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
        String output = "<"+elementName+" ";
        if (Description != null) output = output + "urwg:description=\"" + Description + "\" ";
        if (Type != null) output = output + "urwg:type=\"" + Type + "\" ";
        if (Unit != null) output = output + "urwg:unit=\"" + Unit + "\" ";
        if (PhaseUnit != null) output = output + "urwg:phaseUnit=\"" + PhaseUnit + "\" ";
        if (StorageUnit != null) output = output + "urwg:storageUnit=\"" + StorageUnit + "\" ";
        if (Metrics != null) output = output + "urwg:metrics=\"" + Metrics + "\" ";
        output = output + ">" + Value + "</" + elementName + ">\n";
        return output;
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

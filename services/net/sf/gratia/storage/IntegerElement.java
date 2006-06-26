package net.sf.gratia.storage;

/**
 * <p>Title: IntegerElement</p>
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
public class IntegerElement implements XmlElement {
    private long Value;
    private String Description;
    private String Metric;
    private double ConsumptionRate;

    public IntegerElement() {
        ConsumptionRate = 1;
    }

    public void setValue(long Value) {
        this.Value = Value;
    }

    public long getValue() {
        return Value;
    }

    public void setDescription(String Description) {
        this.Description = Description;
    }

    public String getDescription() {
        return Description;
    }

    public void setMetric(String Metric) {
        this.Metric = Metric;
    }

    public String getMetric() {
        return Metric;
    }

    public String toString() {
        String output = "" + Value;
        if (Metric != null)       output = output + " " + Metric;
        if (ConsumptionRate != 1) output = output + " rate:" + ConsumptionRate;
        if (Description != null)  output = output + " (" + Description + ")";
        return output;
    }

    public String asXml(String elementName) {
        String output = "<"+elementName+" ";
        if (Description != null) output = output + "urwg:description=\"" + Description + "\" ";
        if (Metric != null) output = output + "urwg:metric=\"" + Metric + "\" ";
        if (ConsumptionRate != 1) output = output + "urwg:consumptionRate=\"" + ConsumptionRate + "\" ";
        output = output + ">" + Value + "</" + elementName + ">\n";
        return output;
    }

    public void setConsumptionRate(double ConsumptionRate) {
        this.ConsumptionRate = ConsumptionRate;
    }

    public double getConsumptionRate() {
        return ConsumptionRate;
    }

}

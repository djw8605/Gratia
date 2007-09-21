package net.sf.gratia.reporting;

public class ParameterProperty  // MenuItem
{
	private String _propertyName; // _name
	private String _propertyValue; // _link

	public ParameterProperty(String propertyName, String propertyValue) // MenuItem
	{
		_propertyName = propertyName;
		_propertyValue = propertyValue;
	}

	public String getPropertyName(){ // getName
		return _propertyName;
	}

	public String getPropertyValue(){ // getLink
		return _propertyValue;
	}
}

package net.sf.gratia.reporting;

import java.util.ArrayList;

public class ParameterGroup //MenuGroup
{

	private String _parameterName; //_name;
	private ArrayList _parameterProperties; //_menuItems;
	private ArrayList _parameterListSelection;

	public ParameterGroup(String parameterName) // MenuGroup
	{
		_parameterName = parameterName;
		_parameterProperties = new ArrayList();
		_parameterListSelection = new ArrayList();
	}

	public String getParameterName(){  //getName
		return _parameterName;
	}

	public ArrayList getParameterProperties() { //getMenuItems
		return _parameterProperties;
	}

	public ArrayList getParameterListSelection() {
			return _parameterListSelection;
	}


}

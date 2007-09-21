package net.sf.gratia.reporting;

public class ParameterListSelection
{
	private String _selectionName;
	private String _selectionValue;

	public ParameterListSelection(String selectionName, String selectionValue)
	{
		_selectionName = selectionName;
		_selectionValue = selectionValue;
	}

	public String getSelectionName(){
		return _selectionName;
	}

	public String getSelectionValue(){
		return _selectionValue;
	}
}

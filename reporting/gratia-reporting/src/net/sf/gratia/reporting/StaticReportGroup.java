package net.sf.gratia.reporting;

import java.util.ArrayList;

public class StaticReportGroup
{

	private String _group;
	private ArrayList _statItems;

	public StaticReportGroup(String group)
	{
		_group = group;
		_statItems = new ArrayList();
	}

	public String getGroup() {
		return _group;
	}

	public ArrayList getStatItems (){
		return _statItems;
	}

}

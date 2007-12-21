package net.sf.gratia.reporting;

import java.util.ArrayList;

public class MenuGroup 
{
	
	private String _group;
	private ArrayList _menuItems;
	
	public MenuGroup(String group)
	{
		_group = group;
		_menuItems = new ArrayList();
	}
	
	public String getGroup(){
		return _group;
	}
	
	public ArrayList getMenuItems() {
		return _menuItems;
	}
}

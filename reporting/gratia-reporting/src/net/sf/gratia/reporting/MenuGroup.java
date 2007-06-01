package net.sf.gratia.reporting;

import java.util.ArrayList;

public class MenuGroup 
{
	
	private String _name;
	private ArrayList _menuItems;
	
	public MenuGroup(String name)
	{
		_name = name;
		_menuItems = new ArrayList();
	}
	
	public String getName(){
		return _name;
	}
	
	public ArrayList getMenuItems() {
		return _menuItems;
	}
	
	
}

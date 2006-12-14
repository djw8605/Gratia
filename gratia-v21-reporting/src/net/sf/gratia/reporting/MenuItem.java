package net.sf.gratia.reporting;

public class MenuItem 
{
	private String _name;
	private String _link;
	
	public MenuItem(String name, String link)
	{
		_name = name;
		_link = link;
	}
	
	public String getName(){
		return _name;
	}
	
	public String getLink(){
		return _link;
	}
}

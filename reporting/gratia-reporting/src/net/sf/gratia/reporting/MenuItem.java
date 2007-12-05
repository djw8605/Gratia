package net.sf.gratia.reporting;

public class MenuItem 
{
	private String _name;
	private String _link;
	private String _display;
	
	public MenuItem(String name, String link, String display)
	{
		_name = name;
		_link = link;
		_display = display;
	}
	
	public String getName(){
		return _name;
	}
	
	public String getLink(){
		return _link;
	}
	
	public String getDisplay(){
		return _display;
	}
	
}

package net.sf.gratia.reporting;

public class StaticReportItem
{
	private String _name;
	private String _report;
	private String _link;

	public StaticReportItem(String name, String report, String link)
	{
		_name = name;
		_report = report;
		_link = link;
	}

	public String getName(){
		return _name;
	}

	public String getReport(){
		return _report;
	}

	public String getLink(){
		return _link;
	}

}

package net.sf.gratia.reporting;

import java.util.*;
import java.text.*;

public class DashboardItem 
{
		private String _link;
	
		public DashboardItem(String link)
		{
				_link = link;
		}
	
		public String getLink()
		{
				//
				// replace Empty date parameters
				//
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				Date to = new Date();
				String endDate = "EndDate=" + format.format(to);
				Date from = new Date(to.getTime() - (7 * 24 * 60 * 60 * 1000));
				String startDate = "StartDate=" + format.format(from);
				String newlink = _link.replace("StartDate",startDate);
				newlink = newlink.replaceAll("EndDate",endDate);
				
				return newlink;
		}
}

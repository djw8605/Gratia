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
				// glr: replace date stamps
				//
				SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
				Date to = new Date();
				Date from = new Date(to.getTime() - (7 * 24 * 60 * 60 * 1000));
				System.out.println("link: " + _link);
				String newlink = _link.replaceAll("01/01/1990",format.format(from));
				newlink = newlink.replaceAll("12/31/2999",format.format(to));
				System.out.println("newlink: " + newlink);
				return newlink;
		}
}

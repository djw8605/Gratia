package net.sf.gratia.reporting;

import java.util.ArrayList;

public class DashboardRow 
{
		private ArrayList dashboardItems;
	
		public DashboardRow()
		{
				dashboardItems = new ArrayList();
		}
	
		public ArrayList getDashboardItems() 
		{
				int i;
				for (i = 0; i < dashboardItems.size(); i++)
						System.out.println("dashboard item: " + dashboardItems.get(i));

				return dashboardItems;
		}
}

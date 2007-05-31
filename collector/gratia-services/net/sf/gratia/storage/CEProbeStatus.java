package net.sf.gratia.storage;

import java.util.*;

public class CEProbeStatus
{
		private int statusid;
		private String probename;
		private DateElement currenttime;
		private String probestatus;
		private int jobs;
		private int lostjobs;

		public CEProbeStatus()
		{
		}

		public int getstatusid()
		{
				return statusid;
		}

		public void setstatusid(int value)
		{
				this.statusid = value;
		}

		public String getprobename() 
		{
				return probename;
		}

		public void setprobename(String name) 
		{
				probename = name;
		}
	
		public DateElement getcurrenttime()
		{
				return currenttime;
		}

		public void setcurrenttime(DateElement time)
		{
				this.currenttime = time;
		}

		public String getprobestatus()
		{
				return probestatus;
		}

		public void setprobestatus(String value)
		{
				this.probestatus = value;
		}

		public void setjobs(int value)
		{
				jobs = value;
		}

		public int getjobs()
		{
				return jobs;
		}

		public void setlostjobs(int value)
		{
				lostjobs = value;
		}

		public int getlostjobs()
		{
				return lostjobs;
		}

		public String toString() 
		{
				String output = 
						"CEProbeStatus: " + 
						" probename: " + probename +
						" currenttime: " + currenttime +
						" probestatus: " + probestatus;
        return output;
    }
}

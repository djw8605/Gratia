package net.sf.gratia.storage;

import java.util.*;

public class CEProbes
{
		private int _facility_id;
		private int probeid;
		private String probename;
		private DateElement currenttime;
		private int active;
		private int reporthh;
		private int reportmm;
		private String status;
		private int jobs;

		public CEProbes()
		{
		}

		public int get_facility_id() 
		{
				return _facility_id;
		}

		public void set_facility_id(int _facility_id) 
		{
				this._facility_id = _facility_id;
		}

		public int getprobeid() 
		{
				return probeid;
		}

		public void setprobeid(int probeid) 
		{
				this.probeid = probeid;
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

		public int getreporthh()
		{
				return reporthh;
		}

		public void setreporthh(int value)
		{
				this.reporthh = value;
		}

		public int getreportmm()
		{
				return reportmm;
		}

		public void setreportmm(int value)
		{
				this.reportmm = value;
		}

		public int getactive()
		{
				return active;
		}

		public void setactive(int value)
		{
				this.active = value;
		}

		public void setstatus(String value)
		{
				this.status = value;
		}

		public String getstatus()
		{
				return status;
		}

		public int getjobs()
		{
				return jobs;
		}

		public void setjobs(int value)
		{
				this.jobs = value;
		}

		public String toString() 
		{
				String output = 
						"CEProbes: " + 
						"_facility_id: " + _facility_id +
						" probeid: " + probeid +
						" probename: " + probename +
						" currenttime: " + currenttime +
						" active: " + active +
						" reporthh: " + reporthh +
						" reportmm: " + reportmm +
						" status: " + status;
        return output;
    }
}

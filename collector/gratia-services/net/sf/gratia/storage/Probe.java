package net.sf.gratia.storage;

public class Probe
{
        private int probeid;
		private int siteid;
		private String probename;
		private DateElement currenttime;
		private int active;
		private int reporthh;
		private int reportmm;
		private String status;
		private int nRecords;

		public Probe()
		{
		}

		public int getsiteid() 
		{
				return siteid;
		}

		public void setsiteid(int siteid) 
		{
				this.siteid = siteid;
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

		public int getnRecords()
		{
				return nRecords;
		}

		public void setnRecords(int value)
		{
				this.nRecords = value;
		}

		public String toString() 
		{
				String output = 
						"Probe: " + 
						" siteid: " + siteid +
						" probeid: " + probeid +
						" probename: " + probename +
						" currenttime: " + currenttime +
						" active: " + active +
						" reporthh: " + reporthh +
						" reportmm: " + reportmm +
						" status: " + status +
						" nRecords: " + nRecords;
        return output;
    }
}

package net.sf.gratia.storage;

public class Replication
{
		private int replicationid;
		private int registered;
		private int running;
		private int security;
		private String openconnection;
		private String secureconnection;
		private int frequency;
		private int dbid;
		private int rowcount;
		private String probename;

		public int getreplicationid() 
		{
				return replicationid;
		}

		public void setreplicationid(int value) 
		{
				replicationid = value;
		}

		public int getregistered() 
		{
				return registered;
		}

		public void setregistered(int value) 
		{
				registered = value;
		}

		public int getrunning() 
		{
				return running;
		}

		public void setrunning(int value) 
		{
				running = value;
		}

		public int getsecurity() 
		{
				return security;
		}

		public void setsecurity(int value) 
		{
				security = value;
		}

		public String getopenconnection() 
		{
				return openconnection;
		}

		public void setopenconnection(String value) 
		{
				openconnection = value;
		}

		public String getsecureconnection() 
		{
				return secureconnection;
		}

		public void setsecureconnection(String value) 
		{
				secureconnection = value;
		}
	
		public void setdbid(int value) 
		{
				dbid = value;
		}

		public int getdbid() 
		{
				return dbid;
		}

		public void setfrequency(int value) 
		{
				frequency = value;
		}

		public int getfrequency() 
		{
				return frequency;
		}

		public void setrowcount(int value) 
		{
				rowcount = value;
		}

		public int getrowcount() 
		{
				return rowcount;
		}

		public String getprobename() 
		{
				return probename;
		}

		public void setprobename(String value) 
		{
				probename = value;
		}

}

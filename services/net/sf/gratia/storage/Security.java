package net.sf.gratia.storage;

public class Security
{
		private int securityid;
		private String source;
		private String alias;
		private String hostpem;
		private String state;

		public Security()
		{
		}

		public int getsecurityid() 
		{
				return securityid;
		}

		public void setsecurityid(int value) 
		{
				securityid = value;
		}

		public String getsource() 
		{
				return source;
		}

		public void setsource(String value) 
		{
				source = value;
		}
	
		public String getalias() 
		{
				return alias;
		}

		public void setalias(String value) 
		{
				alias = value;
		}

		public String gethostpem() 
		{
				return hostpem;
		}

		public void sethostpem(String value) 
		{
				hostpem = value;
		}
	
		public String getstate() 
		{
				return state;
		}

		public void setstate(String value) 
		{
				state = value;
		}
	
}

package net.sf.gratia.storage;

public class SystemProplist
{
		private long propid;
		private String car;
		private String cdr;

		public SystemProplist()
		{
		}

		public long getpropid()
		{
				return propid;
		}

		public void setpropid(long value)
		{
				propid = value;
		}

		public String getcar() 
		{
				return car;
		}

		public void setcar(String value) 
		{
				car = value;
		}

		public String getcdr() 
		{
				return cdr;
		}

		public void setcdr(String value) 
		{
				cdr = value;
		}
}

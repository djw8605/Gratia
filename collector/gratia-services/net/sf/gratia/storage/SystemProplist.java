package net.sf.gratia.storage;

import java.util.*;

public class SystemProplist
{
		private int propid;
		private String car;
		private String cdr;

		public SystemProplist()
		{
		}

		public int getpropid()
		{
				return propid;
		}

		public void setpropid(int value)
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

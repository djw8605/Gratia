package net.sf.gratia.storage;

import java.util.*;

public class DupRecord
{
		private int dupid;
		private Date eventdate;
		private String rawxml;
		private String extraxml;

		public DupRecord()
		{
		}

		public int getdupid() 
		{
				return dupid;
		}

		public void setdupid(int value) 
		{
				dupid = value;
		}

		public Date geteventdate() 
		{
				return eventdate;
		}

		public void seteventdate(Date value) 
		{
				eventdate = value;
		}

		public String getrawxml() 
		{
				return rawxml;
		}

		public void setrawxml(String value) 
		{
				rawxml = value;
		}


		public String getextraxml() 
		{
				return extraxml;
		}

		public void setextraxml(String value) 
		{
				extraxml = value;
		}


}

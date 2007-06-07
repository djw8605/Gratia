package net.sf.gratia.storage;

public class Role
{
		public int roleid;
		public String role;
		public String subtitle;
		public String whereClause;

		public Role()
		{
		}

		public int getroleid()
		{
				return roleid;
		}

		public void setroleid(int value)
		{
				roleid = value;
		}

		public String getrole()
		{
				return role;
		}

		public void setrole(String value)
		{
				role = value;
		}

		public String getsubtitle()
		{
				return subtitle;
		}

		public void setsubtitle(String value)
		{
				subtitle = value;
		}

		public String getwhereClause()
		{
				return whereClause;
		}

		public void setwhereClause(String value)
		{
				whereClause = value;
		}
}

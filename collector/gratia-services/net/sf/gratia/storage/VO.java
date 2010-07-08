package net.sf.gratia.storage;

public class VO
{
		private long VOid;
		private String VOName;
	
		public VO()
		{
		}

		public long getVOid() {
				return VOid;
		}

		public void setVOid(long id) {
				this.VOid = id;
		}

		public String getVOName() {
				return VOName;
		}

		public void setVOName(String name) {
            VOName = name;
		}
	
    	public String toString() {
    	    String output = "Site: " + "VOid: " + VOid + " VOName: " + VOName;
    	    return output;
    	}
}

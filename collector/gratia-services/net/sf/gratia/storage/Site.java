package net.sf.gratia.storage;

public class Site
{
		private int siteId;
		private String siteName;
	
		public Site()
		{
		}

		public int getsiteId() {
				return siteId;
		}

		public void setsiteId(int id) {
				this.siteId = id;
		}

		public String getsiteName() {
				return siteName;
		}

		public void setsiteName(String name) {
            siteName = name;
		}
	
		public String toString() {
				String output = "Site: " + "_facility_id: " + siteId + " _facility_name: " + siteName;
        return output;
    }
}

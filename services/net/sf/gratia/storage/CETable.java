package net.sf.gratia.storage;

public class CETable
{
		private int _facility_id;
		private String _facility_name;
	
		public CETable()
		{
		}

		public int get_facility_id() {
				return _facility_id;
		}

		public void set_facility_id(int _facility_id) {
				this._facility_id = _facility_id;
		}

		public String get_facility_name() {
				return _facility_name;
		}

		public void set_facility_name(String name) {
				_facility_name = name;
		}
	
		public String toString() {
				String output = "CETable: " + "_facility_id: " + _facility_id + " _facility_name: " + _facility_name;
        return output;
    }
}

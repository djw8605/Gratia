package net.sf.gratia.storage;

public class FacilityProbes
{
		private int _facility_id;
		private int _probe_id;
		private String _probe_name;
	
		public int get_facility_id() {
				return _facility_id;
		}

		public void set_facility_id(int _facility_id) {
				this._facility_id = _facility_id;
		}

		public int get_probe_id() {
				return _probe_id;
		}

		public void set_probe_id(int _probe_id) {
				this._probe_id = _probe_id;
		}

		public String get_probe_name() {
				return _probe_name;
		}
		public void set_probe_name(String name) {
				_probe_name = name;
		}
	
		public String toString() {
				String output = 
						"FacilityProbes: " + "_facility_id: " + _facility_id +
						" _probe_id: " + _probe_id +
						" _probe_name: " + _probe_name;
        return output;
    }
}

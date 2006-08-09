package net.sf.gratia.storage;

public class CPUInfo 
{
		private int _id;
		private String _hostDescription;
		private String _benchmarkScore;
		private String _CPUCount;
		private String _OS;
		private String _OSVersion;
		private String _CPUType;
	
		public CPUInfo()
		{
		}

		public int get_id() {
				return _id;
		}
		public void set_id(int _id) {
				this._id = _id;
		}
		public String get_benchmarkScore() {
				return _benchmarkScore;
		}
		public void set_benchmarkScore(String score) {
				_benchmarkScore = score;
		}
		public String get_CPUCount() {
				return _CPUCount;
		}
		public void set_CPUCount(String count) {
				_CPUCount = count;
		}
		public String get_CPUType() {
				return _CPUType;
		}
		public void set_CPUType(String type) {
				_CPUType = type;
		}
		public String get_hostDescription() {
				return _hostDescription;
		}
		public void set_hostDescription(String name) {
				_hostDescription = name;
		}
		public String get_OS() {
				return _OS;
		}
		public void set_OS(String _os) {
				_OS = _os;
		}
		public String get_OSVersion() {
				return _OSVersion;
		}
		public void set_OSVersion(String version) {
				_OSVersion = version;
		}
	
		public String toString() {
        String output = "CPUInfo: ";
        if (_hostDescription != null) output = output + " Host Description:  " + _hostDescription;
        if (_benchmarkScore != null) output = output + " Benchmark Score: " + _benchmarkScore;
        if (_CPUCount != null) output = output + " CPU Count: " + _CPUCount;
        if (_OS != null) output = output + " OS: " + _OS;
        if (_OSVersion != null) output = output + " OS Version: " + _OSVersion;
        if (_CPUType != null) output = output + " CPU Type: " + _CPUType;
        
        return output;
    }
}

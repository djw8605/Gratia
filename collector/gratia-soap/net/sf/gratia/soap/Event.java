package net.sf.gratia.soap;

/**
 * @author Tim Byrne
 *
 * Event
 * 
 * The Event class represents an event that the collector service needs to act on.  At this time, the only
 *  event is to 'collect usage xml', but this object will be flexible enough to allow other events in the
 *  future.
 *  
 */
public class Event {
	private String _id;
	private String _xml;
	
	/**
	 * Constructor
	 * 
	 * A default constructor, required by any class that is passed to or returned from a web service.
	 * 
	 */
	public Event()
	{
		_id="";
		_xml="";
	}
	
	/**
	 * Constructor
	 * 
	 * A full-value constructor, used to create a fully populated Event object in one line of code.
	 * 
	 * @param id
	 * 	Specify the meter id that is the source of this Event (who is sending the event).
	 * @param xml
	 *  Specify the xml data associated with the event, at this time this field represents the usage xml.
	 *  
	 */
	public Event(String id, String xml)
	{
		_id = id;
		_xml = xml;
	}

	/**
	 * Getters and setters section.
	 * 
	 */
	public String get_id() {
		return _id;
	}
	public String get_xml() {
		return _xml;
	}

	public void set_id(String _id) {
		this._id = _id;
	}
	public void set_xml(String _xml) {
		this._xml = _xml;
	}
}


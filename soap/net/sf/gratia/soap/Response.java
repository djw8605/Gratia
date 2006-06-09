package net.sf.gratia.soap;

/**
 * @author Tim Byrne
 *
 * Response
 * 
 * The Response class represents a web service call response, which is returned to the consumer to indicate
 *  the success/failure of the call (code) and the success/failure message (message).
 *  
 */
public class Response 
{
	private int _code;
	private String _message;
	
	/**
	 * Constructor
	 * 
	 * A default constructor, required for any object passed to or returned from a web service.
	 * 
	 */
	public Response()
	{
		_code = -1;
		_message = "";
	}
	
	
	/**
	 * Constructor
	 * 
	 * A full-valued constructor, used to create a fully populated Response object in one line of code.
	 * 
	 * @param code
	 *  Specify the response code where 0 indicates success and any other value will be interpreted by the caller
	 * @param message
	 *  Specify the response message
	 *  
	 */
	public Response(int code, String message)
	{
		_code = code;
		_message = message;
	}
	
	
	/**
	 * Getters and setters section.
	 * 
	 */
	public int get_code() 
	{
		return _code;
	}
	public void set_code(int _code) 
	{
		this._code = _code;
	}

	public String get_message() 
	{
		return _message;
	}
	public void set_message(String _message) 
	{
		this._message = _message;
	}
}


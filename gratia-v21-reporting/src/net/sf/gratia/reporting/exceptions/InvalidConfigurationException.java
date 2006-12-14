package net.sf.gratia.reporting.exceptions;

/**
 * InvalidConfigurationException
 * 
 * Thrown when trying to load and parse the configuration file.
 * 
 * @author Tim Byrne
 *
 */
public final class InvalidConfigurationException 
	extends Exception 
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6396111358608689892L;
	
	public InvalidConfigurationException(String message)
	{
		super(message);		
	}
	
	public InvalidConfigurationException(String message, Exception source)
	{
		super(message, source);		
	}

}

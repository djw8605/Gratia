//package net.sf.gratia.storage.standAloneParser;

import java.io.*;
import java.util.*;

/**
	Class representing the ParseResult object.
	This class encapsulates the actual results of parsing. When the parsing
	is done (in ParseTester.java -> Parser.java), we have the results of parsing
	that is stored in an instance of the ParseResult object. 
	Then we take this ParseResult object and compare the results obtained to 
	the expected result in the ParseResult object contained in the TestCase object. 

        @author Karthik Arunachalam
	Date: Apr 2011
*/
class ParseResult
{
	private ParseStatus parseStatus = ParseStatus.FAIL; //Actual or expected result of parsing
	private String errorMessage = ""; //Error message thrown by any exception encountered during the parsing
	private String stackTrace = ""; //Detailed stack trace resulting from the exception if any
        private String additionalDetails = ""; //additional details containing additional information. For example this could include the
					       //difference between the xml in the correctly parsed results and the actual parsed results

	/**
		Overloaded constructors
	*/
	ParseResult()
	{
		parseStatus = ParseStatus.FAIL;
		errorMessage = "";
	}
	ParseResult(ParseStatus parseStatus)
	{
		this.parseStatus = parseStatus;
	}
	ParseResult(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}
	ParseResult(ParseStatus parseStatus, String errorMessage)
	{
		this.parseStatus = parseStatus;
		this.errorMessage = errorMessage;
	}

	/**
		Setter and getter methods
	*/
	public ParseStatus getParseStatus()
	{
		return parseStatus;
	}
	public void setParseStatus(ParseStatus parseStatus)
	{
		this.parseStatus = parseStatus;
	}
	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}
	public String getErrorMessage()
	{
		return errorMessage;
	}
	public void setStackTrace(String stackTrace)
	{
		this.stackTrace = stackTrace;
	}
	public String getStackTrace()
	{
		return stackTrace;
	}
        public void setAdditionalDetails(String additionalDetails)
	{
		this.additionalDetails = additionalDetails;
	}
	public String getAdditionalDetails()
	{
		return additionalDetails;
	}

	/**
		Method to determine if two given ParseResult objects are equal.	
	*/
        @Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof ParseResult))
			return false;
		ParseResult parseResult = (ParseResult)obj;

		//If the parseStatus and errorMessage are the same then the two 
		//objects are considered equal
		if(parseStatus == parseResult.parseStatus && errorMessage.equals(parseResult.errorMessage))
			return true;

		//Otherwise they are not equal
		return false;
	}

	/**
		To enable this object to be stored in a Hash data structure	
	*/
	@Override
	public int hashCode()
	{
		int val = 1121 + errorMessage.length();
		if(parseStatus == ParseStatus.PASS)
			val++;
		else
			val--;
		return val;
	}

	public String toString()
	{
		String ret = "ParseStatus: " + parseStatus;
		if(!errorMessage.equals("") && errorMessage != null)
			ret += "\n" + "Error message from exception: " + errorMessage;
		if(!stackTrace.equals("") && stackTrace != null)
			ret += "\n" + "Stack trace of the exception: " + stackTrace;
                ret += "\n" + additionalDetails;
		return ret;
	}
}

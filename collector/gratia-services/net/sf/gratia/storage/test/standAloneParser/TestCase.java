//package net.sf.gratia.storage.standAloneParser;

import java.io.*;
import java.util.*;
import net.sf.gratia.util.Logging;

/**
	Class to represent the test case
	This test case encapsulates information about the files that are
	used as sample test cases and it also sets the expectations about
	the expected result for a particular test. The actual results of 
	the test should match with the expected results in order to declare 
	that the parsing worked as expected. If not, the parsing did not 
	work as expected. 

        @author Karthik
        Date: Apr 2011

*/
class TestCase
{
	private String testDir; //Directory under which the reference files to be parsed are located
	private String fileToParse = "JobUsage.xml"; //The xml file to be parsed for testing
	private String rawXmlFile = "raw.xml"; //This file contains the expected raw xml data
	private String extraXmlFile = "extra.xml"; //This file contains the expected extra xml data
	private ParseResult expectedResult; //Set the expected result in this variable
	private ParseResult actualResult; //Set the actual result in this variable

        /**
		Constructor to set the test directory and the expected parsing result for this test case
        */
        public TestCase(String testDir, ParseResult expectedResult)
	{
		setTestDir(testDir);
		setExpectedResult(expectedResult);
	}

	/**
		The rest of the methods are setter and getter methods 
	*/
	public void setTestDir(String testDir)
	{
		this.testDir = testDir;
	}

	public void setExpectedResult(ParseResult expectedResult)
	{
		this.expectedResult = expectedResult;
	}

	public void setActualResult(ParseResult actualResult)
	{
		this.actualResult = actualResult;
	}

	public ParseResult getExpectedResult()
	{
		return expectedResult;
	}

	public ParseResult getActualResult()
	{
		return actualResult;
	}

	public String getFileToParse()
	{
		return testDir + "/" + fileToParse;
	}

	public String getRawXmlFile()
	{
		return testDir + "/" + rawXmlFile;
	}

	public String getExtraXmlFile()
	{
		return testDir + "/" + extraXmlFile;
	}

        public String toString()
	{
		String ret = "";
        	ret += "Test directory containing xml files: " + testDir + "\n";
        	ret += "File to parse: " + fileToParse + "\n";
        	ret += "Raw xml expected from parsing: " + rawXmlFile  + "\n";
        	ret += "Extra xml expected from parsing: " + extraXmlFile + "\n"; 
                ret += resultWrapper(expectedResult, "Expected");
                ret += resultWrapper(actualResult, "Actual");
		if(expectedResult.equals(actualResult))
		{
		    ret += "Parsing *WORKED* as expected.\n";
		}
		else
		{
		    ret += "Parsing *DID NOT WORK* as expected.\n";
		}
		return ret;
        }
	
        public String resultWrapper(ParseResult result, String resultType)
	{
		String ret = "";
		ret += "\n";
        	ret += resultType + " result of parsing \n";
        	ret += "---------------------------\n";
        	ret += result + "\n";
		return ret;
	}
}

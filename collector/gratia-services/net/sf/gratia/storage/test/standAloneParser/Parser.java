//package net.sf.gratia.storage.standAloneParser;

import java.io.*;
import java.util.*;
import net.sf.gratia.util.Logging;
import net.sf.gratia.storage.*;

/**
	Class to verify if the parsing code works as expected i.e. it passes when it should pass and 
	it fails when it should fail.

	Logic:
		We use a set of xml files for which the parsing result is already known.
		i.e. they have been parsed already and we know if they could be parsed 
		successfully and if so what are the raw xml part and the extra xml part.
		Look under the testFiles/pass director for an example of this. 
		Now we do the actual parsing using this class which in turn uses the parsing code.
		If the file is expected to parse successfully, then it should be parsed successfully.
		In addition the raw xml part and the extra xml part should be as expected. 
		If this is the case then it means that parsing was done as expected.

		In case the parsing is expected to fail (because of a badly formed XML data), then the 
		parsing should fail. In addition the reason for failure (badly formed XML data) should match. 

		We could perform more test cases like some kind of an exception is encountered in the
		parsing code itself and it is as expected etc. 

	@author Karthik
	Date: April 2011

*/
public class Parser
{
   {Logging.initialize("Parser");}

   /**
	Method that uses the parsing code to parse a xml file belonging to 
	a test case and provide the parsing result to the caller. 

	@param
		testCase - the TestCase to be parsed. This test case contains
			   the information about the file to be parsed and the 
			   results to be expected from the parsing.

	@return
		ParseResult - the actual result of the parsing. This is expected to 
			      match with the expected result of parsing. If so the parsing
			      passed. If not it failed. 

   */
   public ParseResult parse(TestCase testCase) throws Exception
   {
      //To start with assume that the parsing failed
      ParseResult parseResult = new ParseResult();
      String errorMessage;

      ArrayList records = new ArrayList();

      //Try parsing the file from this test case
      try
      {
            records = convert(readFile(testCase.getFileToParse()));
      }
      catch(Exception exception) 
      { 
	//If an exception is thrown, store the error message in the parseResult
	parseResult.setErrorMessage(exception.getMessage());
	parseResult.setStackTrace(StackTrace.getStackTrace(exception));
      }

      for(Object object : records)
      {
	   //set the parse result to PASS (expected result) if the contents of the raw xml file and the extra xml file match
	   //(Note: The default parse result is set to FAIL at the very beginning)
           Record record = (Record)object;

           String actualRawXml = record.getRawXml();
	   String expectedRawXml = readFile(testCase.getRawXmlFile());

           String actualExtraXml = record.getExtraXml();
	   String expectedExtraXml = readFile(testCase.getExtraXmlFile());


           if(diff(actualRawXml, expectedRawXml) && diff(actualExtraXml, expectedExtraXml))
	        parseResult.setParseStatus(ParseStatus.PASS);
           else 
           {
                String additionalDetails = "";

                if(diff(actualRawXml, expectedRawXml))
                {
                    additionalDetails += "There are differences between the raw xml resulting from actual parsing vs the expected raw xml. The actual and expected raw xml data and the difference between them is given below. Analyzing the difference could help you to figure out what might be wrong in the parsing code.\n\n";
                    additionalDetails += "Actual raw xml:\n";
                    additionalDetails += actualRawXml + "\n\n";
                    additionalDetails += "Expected raw xml:\n";
                    additionalDetails += expectedRawXml + "\n\n";
                    additionalDetails += "Difference:\n";
                    additionalDetails += Diff.difference(actualRawXml, expectedRawXml) + "\n\n";
                }
                if(diff(actualExtraXml, expectedExtraXml))
                {
                    additionalDetails += "There are differences between the extra xml resulting from actual parsing vs the expected extra xml. The actual and expected extra xml data and the difference between them is given below. Analyzing the difference could help you to figure out what might be wrong in the parsing code.\n\n";
                    additionalDetails += "Actual extra xml:\n";
                    additionalDetails += actualExtraXml + "\n\n";
                    additionalDetails += "Expected extra xml:\n";
                    additionalDetails += expectedExtraXml + "\n\n";
                    additionalDetails += "Difference:\n";
                    additionalDetails += Diff.difference(actualExtraXml, expectedExtraXml) + "\n\n";
                }
                parseResult.setAdditionalDetails("\nAdditional details: " + additionalDetails);
           }
      }
      return parseResult;
   }

   /**
	Method to read a file and then clean it off tabs and end lines and adjust spaces.

	@param
		file - the input file name as a String

	@return
		the file contents as a String
   */   
   public String readFile(String file) throws IOException
   {
	 String line = null;
         StringBuilder xml = new StringBuilder();
         BufferedReader br = new BufferedReader(new FileReader(new File(file)));
         while((line = br.readLine()) != null)
            xml.append(line);
         return xml.toString().replaceAll("(\\t|\\n)","").replaceAll(" +"," ");
   }

   /**
	Method to convert a given xml string into gratia collector records
	@param
		xml - input xml string
	@return
		The ArrayList containing the gratia records generated from the xml
   */
   public ArrayList convert(String xml) throws Exception 
   {
         return (new RecordConverter().convert(xml));
   }

   /**
      Method to remove all endline and tab character from input string

      @param
	text - input String

      @return 
        Input String with tabs and endlines removed
   */
   public String cleanUp(String text)
   {
      //return Pattern.compile("(\\n|\\t)").matcher(text).replaceAll("");
      return text.replaceAll("(\\n|\\t)","");
   }

   public boolean diff(String xml1, String xml2)
   {
       return !cleanUp(xml1).equals(cleanUp(xml2));
   }
}

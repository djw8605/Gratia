//package net.sf.gratia.storage.standAloneParser;

import java.io.*;
import java.util.*;

/**
	Test driver for parser test

        @author Karthik
        Date: Apr 2011

*/
public class ParseTester
{
   /**
        main method to drive the testing process
   */
   public static void main(String[] args) throws Exception
   {
	//ParseStatus parseStatus = ParseStatus.PASS;

        ArrayList<TestCase> testCases = createTestCases();
	performTest(testCases);
   	printTestResults(testCases);
   }

   public static ArrayList<TestCase> createTestCases()
   {
        ArrayList<TestCase> testCases = new ArrayList<TestCase>();
        //Set the directory location under which the xml files are found and the test results expected from the test case
        testCases.add(new TestCase("testFiles/pass", new ParseResult(ParseStatus.PASS)));
        testCases.add(new TestCase("testFiles/fail", new ParseResult("Badly formed xml file")));
	return testCases;
   }

   public static void performTest(ArrayList<TestCase> testCases) throws Exception
   {
        Parser parser = new Parser();
        //Iterate through each test case and set the actual result
        for(TestCase testCase : testCases)
                testCase.setActualResult(parser.parse(testCase));
   }

   public static void printTestResults(ArrayList<TestCase> testCases)
   { 
	int count = 1;
    	System.out.println();
	System.out.println("=====================");
	System.out.println("Parsing test results");
	System.out.println("=====================");
        for(TestCase testCase : testCases)
	{
	    System.out.println("Test case " + count++);
	    System.out.println("~~~~~~~~~~~~~");
	    System.out.println(testCase);
	    System.out.println();
        }
   }
}

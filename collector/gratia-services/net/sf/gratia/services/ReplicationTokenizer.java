package net.sf.gratia.services;

/**
 * <p>Title: ReplicationTokenizer </p>
 *
 * <p>Description: ReplicationTokenizer implenting a subset of Java's Tokenizer
 * which ignores delimiters that are with <> or ""
 *
 * <p>Copyright: Copyright (c) 2009</p>
 *
 * <p>Company: Fermilab </p>
 *
 * @Philippe Canal
 * @version 1.0
 *
 * Updated by Brian Bockelman, University of Nebraska-Lincoln (http://rcf.unl.edu)
 *
 */

class ReplicationTokenizer {
   private final char   fDelimiter;  // The delimiter. 
   private final String fData;       // The string we are looking at.
   private int          fPos;        // Current position of the parsing.

   public ReplicationTokenizer(String data, String delimiters) 
   {
      // Constructs a string tokenizer for the specified string. The characters in the delim argument are the delimiters for separating tokens. Delimiter characters themselves will not be treated as tokens.
      // Parameters:
      //   str - a string to be parsed.
      //   delim - the delimiter.  This class only supports one delimiter.
      fData = data;
      fPos = 0;
      
      if (delimiters.length()!=1) {
         throw new java.lang.RuntimeException("ReplicationTokenizer requires equatly one delimiter");
      }
      fDelimiter = delimiters.charAt(0);
   }
   
   public boolean hasMoreTokens() 
   {
      // Tests if there are more tokens available from this tokenizer's string. If this method returns true, then a subsequent call to nextToken with no argument will successfully return a token.
      // Returns:
      //    true if and only if there is at least one token in the string after the current position; false otherwise.
      
      return fPos >= 0;
   }
   
   public String nextToken() throws java.util.NoSuchElementException
   {
      // Returns the next token from this string tokenizer.
      // Returns:
      //    the next token from this string tokenizer.
      // Throws:
      //    NoSuchElementException - if there are no more tokens in this tokenizer's string.
      
      if (fPos<0) throw new java.util.NoSuchElementException("no more tokens");
      
      int nextpos = fPos;
      int end = fData.length();

      boolean instring = false;
      boolean prev_was_escape = false;
      char stringtype = 0;
      while( nextpos < end ) {
         char what = fData.charAt(nextpos);
         if (!instring && what == fDelimiter ) {
            break;
         }
         if (!prev_was_escape) {
            if (instring) {
               if (what == stringtype) {
                  instring = false;
               }
            } else {
               if (what == '"' || what == '\'') {
                  instring = true;
                  stringtype = what;
               }
            }
         }
         prev_was_escape = ( what == '\\' );
         ++nextpos;
      }

      String result = fData.substring( fPos, nextpos );
      if (nextpos >= end) {
         fPos = -1;
      } else {
         fPos = nextpos + 1;
      }
      return result;
   }
   
}

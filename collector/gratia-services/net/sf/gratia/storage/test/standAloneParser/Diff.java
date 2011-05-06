/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class Diff {
  // Difference
  //-----------------------------------------------------------------------
  /**
   * <p>Compares two Strings, and returns the portion where they differ.
   * (More precisely, return the remainder of the second String,
   * starting from where it's different from the first.)</p>
   *
   * <p>For example,
   * <code>difference("i am a machine", "i am a robot") -> "robot"</code>.</p>
   *
   * <pre>
   * StringUtils.difference(null, null) = null
   * StringUtils.difference("", "") = ""
   * StringUtils.difference("", "abc") = "abc"
   * StringUtils.difference("abc", "") = ""
   * StringUtils.difference("abc", "abc") = ""
   * StringUtils.difference("ab", "abxyz") = "xyz"
   * StringUtils.difference("abcde", "abxyz") = "xyz"
   * StringUtils.difference("abcde", "xyz") = "xyz"
   * </pre>
   *
   * @param str1  the first String, may be null
   * @param str2  the second String, may be null
   * @return the portion of str2 where it differs from str1; returns the
   * empty String if they are equal
   * @since 2.0
   */
  public static String difference(String str1, String str2) {
      if (str1 == null) {
          return str2;
      }
      if (str2 == null) {
          return str1;
      }
      int at = indexOfDifference(str1, str2);
      if (at == -1) {
          return "";
      }
      return str2.substring(at);
  }
  /**
   * <p>Compares two Strings, and returns the index at which the
   * Strings begin to differ.</p>
   *
   * <p>For example,
   * <code>indexOfDifference("i am a machine", "i am a robot") -> 7</code></p>
   *
   * <pre>
   * StringUtils.indexOfDifference(null, null) = -1
   * StringUtils.indexOfDifference("", "") = -1
   * StringUtils.indexOfDifference("", "abc") = 0
   * StringUtils.indexOfDifference("abc", "") = 0
   * StringUtils.indexOfDifference("abc", "abc") = -1
   * StringUtils.indexOfDifference("ab", "abxyz") = 2
   * StringUtils.indexOfDifference("abcde", "abxyz") = 2
   * StringUtils.indexOfDifference("abcde", "xyz") = 0
   * </pre>
   *
   * @param str1  the first String, may be null
   * @param str2  the second String, may be null
   * @return the index where str2 and str1 begin to differ; -1 if they are equal
   * @since 2.0
   */
  public static int indexOfDifference(String str1, String str2) {
      if (str1 == str2) {
          return -1;
      }
      if (str1 == null || str2 == null) {
          return 0;
      }
      int i;
      for (i = 0; i < str1.length() && i < str2.length(); ++i) {
          if (str1.charAt(i) != str2.charAt(i)) {
              break;
          }
      }
      if (i < str2.length() || i < str1.length()) {
          return i;
      }
      return -1;
  }

  //Example standalone usage
  public static void main(String[] args)
  {
     String s1="<Extra1 xmlns=\"http://www.gridforum.org/2003/ur-wg\">1. This is extra xml. Let us see what happens now.</Extra1><Extra3 xmlns=\"http://www.gridforum.org/2003/ur-wg\" sfjslfs=\"slfjslfjs\">3. This is extra xml. Let us see what happens now.</Extra3><Extra2 xmlns=\"http://www.gridforum.org/2003/ur-wg\">2. This is extra xml. Let us see what happens now.</Extra2><ExtraAttribute xmlns=\"http://www.gridforum.org/2003/ur-wg\"><JobName fakeAttribute1=\"This is a fake attribute\" fakeAttributeAno=\"2nd\"/><Status fakeAttribute2=\"This is a 2nd fake attribute\"/><TimeDuration timeextra1=\"time extra 1\"/><SubmitHost fake1SubmitHost=\"111111111111111\" fake2SubmitHost=\"22222222222222222222222\"/><Host secondary=\"false\"/><ProbeName pnExtra=\"----------------------------\"/><SiteName blaName=\"Site blah\"/><Grid extraGridAttribute=\"this is a extra grid attribute 1\" gridAttr2=\"grid attr 2\"/><Njobs njobsextra=\"njobs extra attribute\"></ExtraAttribute>";

    String s2="<Extra1 xmlns=\"http://www.gridforum.org/2003/ur-wg\">1. This is extra xml. Let us see what happens now.</Extra1><Extra3 xmlns=\"http://www.gridforum.org/2003/ur-wg\" sfjslfs=\"slfjslfjs\">3. This is extra xml. Let us see what happens now.</Extra3><Extra2 xmlns=\"http://www.gridforum.org/2003/ur-wg\">2. This is extra xml. Let us see what happens now.</Extra2><ExtraAttribute xmlns=\"http://www.gridforum.org/2003/ur-wg\"><JobName fakeAttribute1=\"This is a fake attribute\" fakeAttributeAno=\"2nd\"/><Status fakeAttribute2=\"This is a 2nd fake attribute\"/><WallDuration fakeAttribute3=\"This is fake attribute 3\"/><TimeDuration timeextra1=\"time extra 1\"/><CpuDuration fakeAttribute2=\"cpuDuration fake attribute\"/><SubmitHost fake1SubmitHost=\"111111111111111\" fake2SubmitHost=\"22222222222222222222222\"/><Host secondary=\"false\"/><ProbeName pnExtra=\"----------------------------\"/><SiteName blaName=\"Site blah\"/><Grid extraGridAttribute=\"this is a extra grid attribute 1\" gridAttr2=\"grid attr 2\"/><Njobs njobsextra=\"njobs extra attribute\"></ExtraAttribute>";
     System.out.println(difference(s1, s2));
    //This would yield the following, which clearly shows that the tags corresponding to WallDuration and 
    /*
WallDuration fakeAttribute3="This is fake attribute 3"/><TimeDuration timeextra1="time extra 1"/><CpuDuration fakeAttribute2="cpuDuration fake attribute"/><SubmitHost fake1SubmitHost="111111111111111" fake2SubmitHost="22222222222222222222222"/><Host secondary="false"/><ProbeName pnExtra="----------------------------"/><SiteName blaName="Site blah"/><Grid extraGridAttribute="this is a extra grid attribute 1" gridAttr2="grid attr 2"/><Njobs njobsextra="njobs extra attribute"></ExtraAttribute>	
    */
  }

}

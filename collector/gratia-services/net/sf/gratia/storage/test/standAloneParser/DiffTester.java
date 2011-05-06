public class DiffTester {

  //Example of usage of Diff.java
 
  public static void main(String[] args)
  {

     //This is the actual xml obtained as a result exercising the current parsing code
     String actualXml = "<Extra1 xmlns=\"http://www.gridforum.org/2003/ur-wg\">1. This is extra xml. Let us see what happens now.</Extra1><Extra3 xmlns=\"http://www.gridforum.org/2003/ur-wg\" sfjslfs=\"slfjslfjs\">3. This is extra xml. Let us see what happens now.</Extra3><Extra2 xmlns=\"http://www.gridforum.org/2003/ur-wg\">2. This is extra xml. Let us see what happens now.</Extra2><ExtraAttribute xmlns=\"http://www.gridforum.org/2003/ur-wg\"><JobName fakeAttribute1=\"This is a fake attribute\" fakeAttributeAno=\"2nd\"/><Status fakeAttribute2=\"This is a 2nd fake attribute\"/><TimeDuration timeextra1=\"time extra 1\"/><SubmitHost fake1SubmitHost=\"111111111111111\" fake2SubmitHost=\"22222222222222222222222\"/><Host secondary=\"false\"/><ProbeName pnExtra=\"----------------------------\"/><SiteName blaName=\"Site blah\"/><Grid extraGridAttribute=\"this is a extra grid attribute 1\" gridAttr2=\"grid attr 2\"/><Njobs njobsextra=\"njobs extra attribute\"></ExtraAttribute>";

     //This is the expeceted/correct xml to be obtained as a result exercising the current parsing code
    String expectedXml = "<Extra1 xmlns=\"http://www.gridforum.org/2003/ur-wg\">1. This is extra xml. Let us see what happens now.</Extra1><Extra3 xmlns=\"http://www.gridforum.org/2003/ur-wg\" sfjslfs=\"slfjslfjs\">3. This is extra xml. Let us see what happens now.</Extra3><Extra2 xmlns=\"http://www.gridforum.org/2003/ur-wg\">2. This is extra xml. Let us see what happens now.</Extra2><ExtraAttribute xmlns=\"http://www.gridforum.org/2003/ur-wg\"><JobName fakeAttribute1=\"This is a fake attribute\" fakeAttributeAno=\"2nd\"/><Status fakeAttribute2=\"This is a 2nd fake attribute\"/><WallDuration fakeAttribute3=\"This is fake attribute 3\"/><TimeDuration timeextra1=\"time extra 1\"/><CpuDuration fakeAttribute2=\"cpuDuration fake attribute\"/><SubmitHost fake1SubmitHost=\"111111111111111\" fake2SubmitHost=\"22222222222222222222222\"/><Host secondary=\"false\"/><ProbeName pnExtra=\"----------------------------\"/><SiteName blaName=\"Site blah\"/><Grid extraGridAttribute=\"this is a extra grid attribute 1\" gridAttr2=\"grid attr 2\"/><Njobs njobsextra=\"njobs extra attribute\"></ExtraAttribute>";

    //If there is a difference between the actual and extra xml then capture the difference
    String diff = Diff.difference(actualXml, expectedXml);
    System.out.println(diff);
    //This would yield the following, which clearly shows that the tags corresponding to WallDuration and CpuDuration are missing in the actualXml. 
    /*
WallDuration fakeAttribute3="This is fake attribute 3"/><TimeDuration timeextra1="time extra 1"/><CpuDuration fakeAttribute2="cpuDuration fake attribute"/><SubmitHost fake1SubmitHost="111111111111111" fake2SubmitHost="22222222222222222222222"/><Host secondary="false"/><ProbeName pnExtra="----------------------------"/><SiteName blaName="Site blah"/><Grid extraGridAttribute="this is a extra grid attribute 1" gridAttr2="grid attr 2"/><Njobs njobsextra="njobs extra attribute"></ExtraAttribute>	

    Now looking at the corresponding code in JobUsageRecordLoader.java, we see that the corresponding code has been commented out (in order to introduce an intentional bug in the code)
    So this needs to be fixed. 

                } else if (sub.getName().equalsIgnoreCase("WallDuration")) {
                    //SetWallDuration(job, sub);
                } else if (sub.getName().equalsIgnoreCase("CpuDuration")) {
                    //SetCpuDuration(job, sub);
    */
  }

}

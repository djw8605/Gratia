/**
 * 
 */
package net.sf.gratia.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Hashtable;

/**
 * @author pcanal
 *
 */
public class VONameUpdater extends JobUsageRecordUpdater {

      java.util.Hashtable CEtoMap;
   
      private class CEmap {
            String gumsServer;
            java.util.Date lastUpdate;
            java.util.Hashtable localIdToVOname;
      
            CEmap(String server) {
                  gumsServer = server;
                  lastUpdate = new java.util.Date();
                  localIdToVOname = new Hashtable();
            }
      
            public void Add(String localid, String voname) {
                  localIdToVOname.put(localid, voname);
            }
      
            public String getVOFromLocalId(String localid) {
                  if (localid==null) return null;
                  String result = (String)localIdToVOname.get(localid);
                  //TODO loookup in the table
                  if (result==null) return "Unknown";
                  return result;
            }
      
      }

    public VONameUpdater() {
        CEtoMap = new java.util.Hashtable();
        //Add("flxi02.fnal.gov","cmsuser001","one");
        //Add("flxi02.fnal.gov","cmsuser002","two");
    }

    /**
     * For the Compute Element CE, add a pair
     * local user id <-> name of the VO.
     * 
     * @param ce
     * @param localid
     * @param voname
     */
    void Add(String ce, String localid, String voname) {
        CEmap cemap = (CEmap)CEtoMap.get(ce);
        if (cemap == null) {
            cemap = new CEmap("Unknown Gums");
            CEtoMap.put(ce,cemap);
        }
        cemap.Add(localid,voname);
    }
    
    
    /**
     * Load in an existing Grid3UserVOMap
     * for the Compute Element 'ce'.
     * 
     * @param ce
     * @param filename
     */
    void AddFile(String ce,String filename) 
    {
            File f = new File(filename);
            if (f.exists() && f.canRead()) 
                  AddFile(ce, f);
    }
 
    /**
     * Load in an existing Grid3UserVOMap
     * for the Compute Element 'ce'.
     * 
     * @param ce
     * @param filename
     */
    void AddFile(String ce, File f) 
    {
        BufferedReader in;
            try {
                  in = new BufferedReader(new FileReader(f));
                  if (!in.ready()) return;
           
                  String line;
                  while ((line = in.readLine()) != null) {
                        // Parse the UserVoMap file
                        // Comment start with a '#'
                        // Some comment actually have content (but we ignore it for now):
                        // #---- accounts for vo: i2u2 ----#
               
                        if (!line.matches("\\s*#")) {
                              String[] values = line.split("\\s");
                              if (values.length==2) {
                                    Add(ce,values[0],values[1]);
                              }
                        }
                  }
                  in.close();
            } catch (FileNotFoundException e) {
                  // TODO Auto-generated catch block
                  Utils.GratiaError(e);
            } catch (IOException e) {
                  // TODO Auto-generated catch block
                  Utils.GratiaError(e);
            }
    
    }

    public void LoadFiles(String dir) 
    {
            final String start = "UserVoMap.";

            File f = new File(dir);
            if (f.exists()) {
                  Utils.GratiaDebug("Looking at " + f);
                  File[] files = f.listFiles();
                  for (int j = 0; j < files.length; j = j + 1) {
                        File rec = files[j];
                        String name = rec.getName();
                        if (rec.canRead() && name.startsWith(start)) {
                              String ce = name.substring(start.length(),name.length());
                              AddFile(ce,rec);
                        }
                  }
            }
    }


      /* (non-Javadoc)
       * @see net.sf.gratia.storage.JobUsageRecordUpdater#Update(net.sf.gratia.storage.JobUsageRecord)
       */
      public void Update(JobUsageRecord rec) {
            UserIdentity user = rec.getUserIdentity();
            if( user == null) {
                  // TODO lookup when there is no user identity at all.
                  // user = new  UserIdentity();

            } else {
                  if (user.getVOName() == null
                        || user.getVOName().length() == 0
                        || user.getVOName().equals("Unknown")  )
                        {
                              //   TODO find the VO and update it.
                              String ce = "Unknown";
                              if (rec.getMachineName()!=null)
                                    ce = rec.getMachineName().getValue();
            
                              CEmap cemap = (CEmap)CEtoMap.get(ce);
                              if (cemap == null) {
                                    user.setVOName("Unknown");
                              } else {
                                    String localid = user.getLocalUserId();
                                    String voname = cemap.getVOFromLocalId(localid);
                                    user.setVOName(voname);
                              }
                        }
            }
      }

}

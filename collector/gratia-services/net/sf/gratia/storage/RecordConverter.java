package net.sf.gratia.storage;

import java.io.StringReader;
import java.util.ArrayList;
import net.sf.gratia.util.Logging;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class RecordConverter {
    private ArrayList<RecordLoader> loaderList = new ArrayList<RecordLoader>();

    public RecordConverter() {
        // Load up the list with loaders
        loaderList.add(new UsageRecordLoader());
        loaderList.add(new MetricRecordLoader());
        loaderList.add(new ComputeElementRecordLoader());
        loaderList.add(new StorageElementRecordLoader());
        loaderList.add(new ComputeElementLoader());
        loaderList.add(new StorageElementLoader());
        loaderList.add(new SubclusterLoader());
        loaderList.add(new ProbeDetailsLoader());
    }

   public Origin convertOrigin(String xml) throws Exception {
      SAXReader saxReader = new SAXReader();
      Document doc = null;
      Element eroot = null;
      Origin found = null;
      
      // Read the XML into a document for parsing
      try {
         doc = saxReader.read(new StringReader(xml));
      }
      catch (Exception e) {
         Utils.GratiaError(e,"XML:" + "\n\n" + xml + "\n\n");
         throw new Exception("Badly formed xml file");
      }
      try {
         eroot = doc.getRootElement();
         
         found = OriginLoader.Read(eroot);
         
      }
      catch (Exception e) {
         Utils.GratiaError(e);
         throw e;
         // throw new Exception("loadURXmlFile saw an error at 2:" + e);
      }
      finally {
         // Cleanup object instantiations
         saxReader = null;
         doc = null;
         eroot = null;
      }
      
      // The records array list is now populated with all the records
      // found in the given XML file: return it to the caller.
      return found;
   }
   
   
   public ArrayList convert(String xml) throws Exception {
        ArrayList foundRecords = new ArrayList(); 
        SAXReader saxReader = new SAXReader();
        Document doc = null;
        Element eroot = null;

        // Read the XML into a document for parsing
        try {
            doc = saxReader.read(new StringReader(xml));
        }
        catch (Exception e) {
            Utils.GratiaError(e,"XML:" + "\n\n" + xml + "\n\n");
            throw new Exception("Badly formed xml file");
        }
        try {
            eroot = doc.getRootElement();

            int expectedRecords = -1;

            if (eroot.getName().equals("RecordEnvelope")) {
                expectedRecords = eroot.elements().size();
            }

            ArrayList recordsThisLoader = null;
         
            for (RecordLoader loader : loaderList) {
                recordsThisLoader = loader.ReadRecords(eroot);
                if (recordsThisLoader != null) {
                    foundRecords.addAll(recordsThisLoader);
                    if ((expectedRecords == -1) || // Only expected one record or type of record
                        (expectedRecords <= foundRecords.size())) { // Found what we're looking for
                        break; // done
                    }
                }
            }
            if (foundRecords.size() == 0) {
                // Unexpected root element
                throw new Exception("Found problem parsing document with root name " + eroot.getName());
            } else if ((expectedRecords > -1) &&
                       (expectedRecords != foundRecords.size())) {
                Logging.info("Expected an envelope with " + expectedRecords +
                             " records but found " + foundRecords.size());
            }
        }
        catch (Exception e) {
            Utils.GratiaError(e);
            throw e;
            // throw new Exception("loadURXmlFile saw an error at 2:" + e);
        }
        finally {
            // Cleanup object instantiations
            saxReader = null;
            doc = null;
            eroot = null;
        }

        // The records array list is now populated with all the records
        // found in the given XML file: return it to the caller.
        return foundRecords;
    }

}

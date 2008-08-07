package net.sf.gratia.storage;

import net.sf.gratia.util.XP;

import net.sf.gratia.services.*;
import java.util.*;
import org.dom4j.*;
import org.dom4j.io.*;
import java.io.*;

import net.sf.gratia.util.Logging;

public class RecordConverter {
    private ArrayList<RecordLoader> loaderList = new ArrayList<RecordLoader>();

    public RecordConverter() {
        // Load up the list with loaders
        loaderList.add(new UsageRecordLoader());
        loaderList.add(new MetricRecordLoader());
        loaderList.add(new ProbeDetailsLoader());
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
                        (expectedRecords >= foundRecords.size())) { // Found what we're looking for
                        break; // done
                    }
                }
            }
            if (foundRecords.size() == 0) {
                // Unexpected root element
                throw new Exception("Found problem parsing document with root name " + eroot.getName());
            } else if ((expectedRecords > -1) &&
                       (expectedRecords != foundRecords.size())) {
                Logging.log("Expected an envelope with " + expectedRecords +
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

/**
 * 
 */
package net.sf.gratia.storage;

import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Logging;

import java.io.StringReader;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.Element;
import org.xml.sax.InputSource;

/**
 * @author pcanal
 *
 */
public abstract class JobUsageRecordUpdater implements RecordUpdater {

   public void Update(Record rec) {
      if (rec.getClass() != JobUsageRecord.class) {
         return;
      }
      Update((JobUsageRecord) rec);
   }

   public abstract void Update(JobUsageRecord rec);

   public static class CheckResourceType extends JobUsageRecordUpdater {
      // Set the ResourceType (if needed) according to the ProbeName (if any).

      public void Update(JobUsageRecord current) {

         StringElement type = current.getResourceType();
         if (current.getProbeName() == null) {
            return;
         }

         String probeName = current.getProbeName().getValue();
         if (probeName == null || probeName.length() == 0) {
            return;
         }

         String[] splits = probeName.toLowerCase().split(":");

         if (type != null) {
            if (splits[0].equals("glexec")) {
               type.setValue("Glexec");
            }
            return;
         }

         if (splits[0].equals("psacct")) {
            type = new StringElement();
            type.setValue("RawCPU");
         } else if (splits[0].equals("condor") || splits[0].equals("pbs") ||
               splits[0].equals("pbs-lsf") || splits[0].equals("lsf") ||
               splits[0].equals("daily")) {
            type = new StringElement();
            type.setValue("Batch");
         } else if (splits[0].equals("glexec")) {
            type = new StringElement();
            type.setValue("Glexec");
         } else if (splits[0].equals("dcache")) {
            type = new StringElement();
            type.setValue("Storage");
         }
         if (type != null) {
            current.setResourceType(type);
         }
      }
   }

   public static class CheckWallDuration extends JobUsageRecordUpdater {

      public void Update(JobUsageRecord current) {
//             Logging.debug("CheckWallDuration: StartTime, EndTime, WallDuration = " +
//                           ((current.getStartTime() == null)?"NULL":current.getStartTime().toString()) + ", " +
//                           ((current.getEndTime() == null)?"NULL":current.getEndTime().toString()) + ", " +
//                           ((current.getWallDuration() == null)?"NULL":current.getWallDuration().getValue()));

         if (current.getStartTime() != null &&
               current.getWallDuration() == null &&
               current.getEndTime() != null &&
               (!current.getEndTime().getValue().before(current.getStartTime().getValue()))) {
            DurationElement wallDuration = new DurationElement();
            wallDuration.setValue((current.getEndTime().getValue().getTime() -
                  current.getStartTime().getValue().getTime()) / 1000.0);
            Logging.debug("CheckWallDuration: null WallDuration set to " + wallDuration.getValue());
            wallDuration.setDescription("calculated");
            current.setWallDuration(wallDuration);
         }
      }
   }

   public static class CheckEndTime extends JobUsageRecordUpdater {

      public void Update(JobUsageRecord current) {

         if (current.getStartTime() != null &&
               current.getWallDuration() != null &&
               (current.getEndTime() == null ||
               (current.getEndTime().getValue().before(current.getStartTime().getValue())))) {
            Utils.GratiaDebug(current.getStartTime().getValue().toLocaleString() + " + " + current.getWallDuration().getValue());
            DateElement newEndTime = new DateElement();
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(current.getStartTime().getValue());
            cal.add(GregorianCalendar.SECOND, (int) current.getWallDuration().getValue());

            Utils.GratiaDebug(cal.getTime().toLocaleString());
            newEndTime.setValue(cal.getTime());
            if (current.getEndTime() != null) {
               newEndTime.setDescription("Original end time was less than start time.  It was:  " + current.getEndTime().getValue().toLocaleString());
            } else {
               newEndTime.setDescription("calculated");
            }
            current.setEndTime(newEndTime);
         }
      }
   }

   public static class CheckStartTime extends JobUsageRecordUpdater {

      public void Update(JobUsageRecord current) {
         if (current.getEndTime() != null && current.getWallDuration() != null &&
               (current.getStartTime() == null)) {
            Utils.GratiaDebug(current.getEndTime().toString() + " - " + current.getWallDuration().getValue());
            DateElement newStartTime = new DateElement();
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(current.getEndTime().getValue());
            cal.add(GregorianCalendar.SECOND, -(int) current.getWallDuration().getValue());

            Utils.GratiaDebug(cal.getTime().toLocaleString());
            newStartTime.setValue(cal.getTime());
            newStartTime.setDescription("calculated");
            current.setStartTime(newStartTime);
         }
      }
   }

   public static class CheckIsNew extends JobUsageRecordUpdater {

      public void Update(JobUsageRecord current) {
         // All we need to do here is rip Source, Destination,
         // Protocol and IsNew out of the Resource list, create a new
         // TransferDetails object and set the bi-directional links.
         StringElement Protocol = findResource(current, "Protocol");
         if (current.getResourceType().getValue().equals("Storage") &&
               (current.getStartTime() != null) &&
               (Protocol != null)) { // Transfer record
            Vector v = new Vector();

            // Protocol
            v.add(Protocol);

            // Source
            StringElement Source = findResource(current, "Source");
            if (Source == null) {
               Logging.warning("JobUsageRecordUpdater.CheckIsNew.Update(): could not find Source resource for transfer record");
               return;
            }
            v.add(Source);

            // Destination
            StringElement Destination = findResource(current, "Destination");
            if (Destination == null) {
               Logging.warning("JobUsageRecordUpdater.CheckIsNew.Update(): could not find Destination resource for transfer record");
               return;
            }
            v.add(Destination);

            TransferDetails td = new TransferDetails();
            td.setProtocol(Protocol.getValue());
            td.setSource(Source.getValue());
            td.setDestination(Destination.getValue());

            // IsNew
            StringElement IsNew = findResource(current, "IsNew");
            if (IsNew == null) {
               Logging.log("JobUsageRecordUpdater.CheckIsNew.Update(): setting isNew based on Destination resource");
               if (Destination.getValue().contains("@")) {
                  td.setIsNew(1);
               } else {
                  td.setIsNew(0);
               }
            } else {
               try {
                  td.setIsNew(Integer.parseInt(IsNew.getValue()));
                  v.add(IsNew);
               } catch (Exception e) {
                  Logging.warning("JobUsageRecordUpdaterCheckIsNew.Update(): unable to parse IsNew resource " + IsNew.getValue());
                  td.setIsNew(0);
               }
            }
            td.setJobUsageRecord(current); // Set link
            current.setTransferDetails(td); // Set other link
            current.getResource().removeAll(v); // Remove resource entries
         }
      }

      private StringElement findResource(JobUsageRecord current,
            String description) {
         List resource = current.getResource();
         if (resource == null) {
            return null;
         }
         try {
            for (Object rObj : resource) {
               StringElement se = (StringElement) rObj;
               if (se.getDescription().equalsIgnoreCase(description)) {
                  return se;
               }
            }
         } catch (Exception ignore) {
         }
         return null;
      }
   }

   public static class CheckCpuDuration extends JobUsageRecordUpdater {

      public void Update(JobUsageRecord current) {
         // Insure that CpuUserDuration and CpuSystemDuration are either
         // both invalid or both initialized.
         DurationElement el = new DurationElement();
         el.setValue(0.0);
         el.setDescription("Default value");
         if (current.getCpuUserDuration() != null) {
            if (current.getCpuSystemDuration() == null) {
               current.setCpuSystemDuration(el);
            }
         } else if (current.getCpuSystemDuration() != null) {
            current.setCpuUserDuration(el);
         }
      }
   }

   /**
    * @author Tim Byrne
    *
    * ExtractKeyInfoContent
    *
    *    Attempts to extract the VO and Username from KeyInfoContent (it is currently the portion that reads 'o=...'
    *  and 'CN=...' repsectively)
    *  If KeyInfoContent is null or the VO portion does not exist, this method should then determine the
    *  VO based off of a user info lookup (TBD)
    */
   public static class ExtractKeyInfoContent extends JobUsageRecordUpdater {

      public void Update(JobUsageRecord current) {

         if (current.getUserIdentity() == null) {
            current.setUserIdentity(new UserIdentity());
         }
         boolean populatedVOFromKeyInfoContent = false;
         boolean populatedUserNameFromKeyInfoContent = false;
         String keyInfoContent = "";
         String VO = current.getUserIdentity().getVOName();
         if (VO == null) {
            VO = "Unknown";
         }
         String userName = current.getUserIdentity().getCommonName();
         if (userName == null) {
            userName = "Unknown";
         }

         // Get the value for KeyInfoContent
         if (current.getUserIdentity() != null && current.getUserIdentity().getKeyInfo() != null && current.getUserIdentity().getKeyInfo().getContent() != null) {
            keyInfoContent = current.getUserIdentity().getKeyInfo().getContent();

            // Check if KeyInfoContent is not null
            if (keyInfoContent != null && keyInfoContent.length() != 0) {
               if (keyInfoContent.startsWith("/")) {
                  String cName = getCNFromDN(keyInfoContent);
                  if ((cName != null) && (cName.length() != 0)) {
                     userName = cName;
                     populatedUserNameFromKeyInfoContent = true;
                  }
               } else {
                  // Attempt to parse the KeyInfoContent into an XML document.
                  try {
                     SAXReader saxReader = new SAXReader();
                     Document doc = null;
                     doc = saxReader.read(new InputSource(new StringReader(keyInfoContent)));

                     // Try to xpath to the 'ds:X509SubjectName' node
                     java.util.List subjectNames = doc.selectNodes("//ds:X509SubjectName");

                     // Check that the xpath returned a node
                     if (subjectNames.size() > 0) {
                        String subjectName = ((Element) subjectNames.get(0)).getText();

                        String cName = getCNFromDN(subjectName);
                        if ((cName != null) && (cName.length() != 0)) {
                           userName = cName;
                           populatedUserNameFromKeyInfoContent = true;
                           Utils.GratiaDebug("Extracted a Username from X509SubjectNameNode: " + userName);
                        }
                        String[] subjectNameFields = subjectName.split("[,/]");

                        for (int i = 0; i < subjectNameFields.length; ++i) {
                           String caseFieldValue = subjectNameFields[i].trim();
                           String fieldValue = subjectNameFields[i].toLowerCase().trim();

                           if (fieldValue.startsWith("o=") && VO.equals("Unknown")) {
                              VO = fieldValue.substring(2);
                              populatedVOFromKeyInfoContent = true;
                              Utils.GratiaDebug("Extracted a VO from X509SubjectNameNode:  " + VO);
                           }
                        }

                        if (populatedVOFromKeyInfoContent == false) {
                           Utils.GratiaDebug("'o=' was not found in key info content.  Defaulting VO Name to " + VO);
                        }
                        if (populatedUserNameFromKeyInfoContent == false) {
                           Utils.GratiaDebug("'CN=' was not found in key info content.  Defaulting Common name Name to " + userName);
                        }
                     } else {
                        Utils.GratiaDebug("No X509SubjectName node.  Will have to search for VO and user by something else");
                     }

                     subjectNames = null;
                     doc = null;
                     saxReader = null;
                  } // End check for xpath returning a node
                  catch (DocumentException ex) {
                     // Failing to parse the KeyInfo shouldn't be a terrible failure, we'll just fall through
                     //  to the 'search elsewhere' portion
                     Utils.GratiaDebug("Exception in parsing for VO.  Will have to search for VO and user by something else\n" + ex.getMessage());
                  }
               }
            } else {
               Utils.GratiaDebug("No KeyInfo Content.  Will have to search for VO and user by something else");
            } // End check for KeyInfoContent not null
         } else {
            Utils.GratiaDebug("No KeyInfo Node.  Will have to search for VO and user by something else");
         } // End check for null path to key info content

         // If VO was not populated from KeyInfoContent
         if (populatedVOFromKeyInfoContent == false) {
            // TODO:  Look up the VO from some other location (TBD)
            } // End check for VO not populated from KeyInfoContent

         //          If UserName was not populated from KeyInfoContent
         if (populatedUserNameFromKeyInfoContent == false) {
            // TODO:  Look up the UserName from some other location (TBD)
            } // End check for UserName not populated from KeyInfoContent

         // Set the value for the current record's VO and username
         if (userName.equals("Unknown") && current.getUserIdentity().getGlobalUsername() != null) {
            if (0 > current.getUserIdentity().getGlobalUsername().indexOf("@")) {
               userName = current.getUserIdentity().getGlobalUsername();
            }
         }
         if (userName.equals("Unknown") && !VO.equals("Unknown")) {
            userName = "Generic " + VO + " user";
         }
         current.getUserIdentity().setVOName(VO);
         current.getUserIdentity().setCommonName(userName);
      }

      static final java.util.regex.Pattern gCertFieldPat = java.util.regex.Pattern.compile("\\s*[a-zA-Z]+=");
 
      private boolean isCertField(String text) {
          // return subjectNameFields[i].length() < 3 || subjectNameFields[i].charAt(2) != '=');
          java.util.regex.Matcher matcher = gCertFieldPat.matcher(text);
          Utils.GratiaInfo("Checking "+text+" result= "+matcher.lookingAt());
          return matcher.lookingAt();
      }

      private String getCNFromDN(String subjectName) {
         String[] subjectNameFields = subjectName.split("[,/]");
         String userName = null;
         boolean prevCN = false;
         for (int i = 0; i < subjectNameFields.length; ++i) {
            String caseFieldValue = subjectNameFields[i].trim();
            String fieldValue = subjectNameFields[i].toLowerCase().trim();
            if (fieldValue.startsWith("cn=")) {
               if (userName == null) {
                  userName = "/" + caseFieldValue;
               } else {
                  userName = userName + "/" + caseFieldValue;
               }
               prevCN = true;
            } else {
               // Deal with a CN like CN=http/hepcms-0.umd.edu
               if (prevCN && userName != null && !isCertField(subjectNameFields[i])) {
                   userName = userName + "/" + caseFieldValue;
               } else {
                   prevCN = false;
               }
            }
         }
         if (userName != null) {
            Utils.GratiaDebug("Extracted username(s) from subject:  " + userName);
         }
         return userName;
      }
   }

   public static void AddDefaults(RecordUpdaterManager man) {
      // Add the default Updaters:

      //
      // glr - change to prepend initialized VONameUpdater
      //

      VONameUpdater vopatch = new VONameUpdater();
      vopatch.LoadFiles(Configuration.getConfigurationPath());

      man.AddUpdater(vopatch);
      man.AddUpdater(new CheckStartTime());
      man.AddUpdater(new CheckEndTime());
      man.AddUpdater(new CheckWallDuration());
      man.AddUpdater(new CheckCpuDuration());
      man.AddUpdater(new ExtractKeyInfoContent());
      man.AddUpdater(new CheckResourceType());
      man.AddUpdater(new CheckIsNew());
   }
}

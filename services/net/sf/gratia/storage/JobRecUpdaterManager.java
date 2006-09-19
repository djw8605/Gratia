package net.sf.gratia.storage;

import java.util.Iterator;
import java.util.GregorianCalendar;

import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.Element;
import org.xml.sax.InputSource;

import net.sf.gratia.services.Configuration;

public class JobRecUpdaterManager implements JobUsageRecordUpdater {

   java.util.List Updaters;
   
   public class CheckEndTime implements JobUsageRecordUpdater 
   {
      public void Update(JobUsageRecord current) 
      {
         if(current.getStartTime() != null &&
            current.getWallDuration() != null && 
            (current.getEndTime() == null || 
            (current.getEndTime().getValue().before(current.getStartTime().getValue()))
            )
           )
         {
            Utils.GratiaDebug(current.getStartTime().getValue().toLocaleString() + " + " + current.getWallDuration().getValue());
            DateElement newEndTime = new DateElement();
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(current.getStartTime().getValue());
            cal.add(GregorianCalendar.SECOND, (int)current.getWallDuration().getValue());
               
            Utils.GratiaDebug(cal.getTime().toLocaleString());
            newEndTime.setValue(cal.getTime());
            if(current.getEndTime() != null)
               newEndTime.setDescription("Original end time was less than start time.  It was:  " + current.getEndTime().getValue().toLocaleString());
            else 
               newEndTime.setDescription("calculated");
            current.setEndTime(newEndTime);                  
         }
      }
   }

   public class CheckStartTime implements JobUsageRecordUpdater 
   {
      public void Update(JobUsageRecord current) 
      {
         if (current.getEndTime() != null && current.getWallDuration() != null &&
              (current.getStartTime() == null)
            )
         {
            Utils.GratiaDebug(current.getEndTime().toString() + " - " + current.getWallDuration().getValue());
            DateElement newStartTime = new DateElement();
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(current.getEndTime().getValue());
            cal.add(GregorianCalendar.SECOND, -(int)current.getWallDuration().getValue());
            
            Utils.GratiaDebug(cal.getTime().toLocaleString());
            newStartTime.setValue(cal.getTime());
            newStartTime.setDescription("calculated");
            current.setStartTime(newStartTime);
         }
      }
   }

   public class CheckCpuDuration implements JobUsageRecordUpdater 
   {
      public void Update(JobUsageRecord current) 
      {
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
   public class ExtractKeyInfoContent implements JobUsageRecordUpdater
   {
      public void Update(JobUsageRecord current)
      {
         if (current.getUserIdentity() == null) {
            current.setUserIdentity(new UserIdentity());
         }
         boolean populatedVOFromKeyInfoContent = false;
         boolean populatedUserNameFromKeyInfoContent = false;
         String keyInfoContent = "";
         String VO = current.getUserIdentity().getVOName();
         if (VO == null) VO = "Unknown";
         String userName = current.getUserIdentity().getCommonName();
         if (userName == null) userName = "Unknown";
         
         // Get the value for KeyInfoContent
         if(current.getUserIdentity() != null 
               && current.getUserIdentity().getKeyInfo() != null 
               && current.getUserIdentity().getKeyInfo().getContent() != null)
         {
            keyInfoContent = current.getUserIdentity().getKeyInfo().getContent();
         
            // Check if KeyInfoContent is not null
            if(keyInfoContent != null && keyInfoContent.length() != 0)
            {
               // Attempt to parse the KeyInfoContent into an XML document.
               try
               {
                  SAXReader saxReader = new SAXReader();        
                    Document doc = null;
                    doc = saxReader.read( new InputSource(new StringReader(keyInfoContent)));  
                    
                    // Try to xpath to the 'ds:X509SubjectName' node
                    java.util.List subjectNames = doc.selectNodes("//ds:X509SubjectName");
                    
                    // Check that the xpath returned a node
                    if(subjectNames.size() > 0)
                    {
                       String[] subjectNameFields = ((Element)subjectNames.get(0)).getText().split(",");
                       
                       for(int i=0; i<subjectNameFields.length; i++)
                       {
                          String fieldValue=subjectNameFields[i].toLowerCase().trim();
                          
                          if(fieldValue.startsWith("o=") && VO.equals("Unknown"))
                          {
                             VO = fieldValue.substring(2);
                             populatedVOFromKeyInfoContent = true;
                             Utils.GratiaDebug("Extracted a VO from X509SubjectNameNode:  " + VO);
                          }
                          else 
                          if(fieldValue.startsWith("cn="))
                          {
                             userName = fieldValue.substring(3);
                             populatedUserNameFromKeyInfoContent = true;
                             Utils.GratiaDebug("Extracted a Username from X509SubjectNameNode:  " + userName);
                          }
                       }      
                       
                       if(populatedVOFromKeyInfoContent == false)
                     {
                        Utils.GratiaDebug("'o=' was not found in key info content.  Defaulting VO Name to " + VO);
                     }                      
                     if(populatedUserNameFromKeyInfoContent == false)
                     {
                        Utils.GratiaDebug("'CN=' was not found in key info content.  Defaulting Common name Name to " + userName);
                     } 
                    } // End check for xpath returning a node
                    else
                    {
                       Utils.GratiaDebug("No X509SubjectName node.  Will have to search for VO and user by something else");
                    }
                    
                    subjectNames = null;
                    doc = null;
                    saxReader = null;                    
               }
               catch(DocumentException ex)
               {
                  // Failing to parse the KeyInfo shouldn't be a terrible failure, we'll just fall through
                  //  to the 'search elsewhere' portion
                  Utils.GratiaDebug("Exception in parsing for VO.  Will have to search for VO and user by something else\n" + ex.getMessage());
               }
               
            } // End check for KeyInfoContent not null
            else
            {
               Utils.GratiaDebug("No KeyInfo Content.  Will have to search for VO and user by something else");
            }
         }  // End check for null path to key info content
         else
         {
            Utils.GratiaDebug("No KeyInfo Node.  Will have to search for VO and user by something else");
         }
         
         // If VO was not populated from KeyInfoContent
         if(populatedVOFromKeyInfoContent == false)
         {
            // TODO:  Look up the VO from some other location (TBD)
         } // End check for VO not populated from KeyInfoContent
         
//          If UserName was not populated from KeyInfoContent
         if(populatedUserNameFromKeyInfoContent == false)
         {
            // TODO:  Look up the UserName from some other location (TBD)
         } // End check for UserName not populated from KeyInfoContent
         
         // Set the value for the current record's VO and username
         if (userName.equals("Unknown") && !VO.equals("Unknown")) {
            userName = "Generic " + VO + " user";
         }
         current.getUserIdentity().setVOName(VO);
         current.getUserIdentity().setCommonName(userName);
      }
   }
   
   public JobRecUpdaterManager() 
   {
      // Create an Updater Managers and add the default Updaters:
      //     CheckEndTime
      
      Updaters = new java.util.LinkedList();

      //
      // glr - change to prepend initialized VONameUpdater
      //

      VONameUpdater vopatch = new VONameUpdater();
      vopatch.LoadFiles(Configuration.getConfigurationPath());

                Updaters.add(vopatch);
      Updaters.add(new CheckStartTime());
      Updaters.add(new CheckEndTime());
      Updaters.add(new CheckCpuDuration());
      Updaters.add(new ExtractKeyInfoContent());

   }
   
   public JobUsageRecordUpdater AddUpdater(JobUsageRecordUpdater upd) 
   {
      Updaters.add(upd);
      return upd;
   }
   
   public JobUsageRecordUpdater PrependUpdater(JobUsageRecordUpdater upd) 
   {
      Updaters.add(0,upd);
      return upd;
   }
   
   public void Update(JobUsageRecord rec) 
   {
       for (Iterator i = Updaters.iterator(); i.hasNext(); ) {
          JobUsageRecordUpdater el = (JobUsageRecordUpdater)i.next();
          el.Update(rec);
       }
   }
}

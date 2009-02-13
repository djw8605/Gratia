package net.sf.gratia.storage;

import java.util.*;
import java.text.*;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * <p>Title: RecordIdentity</p>
 *
 * <p>Description: Contains all the known information about this specific Record.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 */

public class RecordIdentity 
{
    private String RecordId;
      private DateElement CreateTime;
    private KeyInfoType KeyInfo;

    void setRecordId(String n) 
      { 
            RecordId = n; 
      }

      String getRecordId() 
      { 
            return RecordId; 
      }

      public void setCreateTime(DateElement value)
      { 
            CreateTime = value;
      }

      public DateElement getCreateTime()
      { 
            return CreateTime;
      }

      public RecordIdentity() 
      {
    }

    /**
     * toString
     *
     * @return String
     */
    public String toString() 
      {
        return " Record (Id: "+RecordId+" CreateTime: "+CreateTime+" KeyInfo: "+KeyInfo+") ";
    }

   public String asXml() {
      StringBuilder output = new StringBuilder();
      asXml(output);
      return output.toString();
   }
   
   public void asXml(StringBuilder output) 
      {
         TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
         SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
         String timestamp = format.format(CreateTime.getValue());
         String dq = "\"";

         output.append("<RecordIdentity urwg:recordId=" + dq +
             StringEscapeUtils.escapeXml(RecordId) + dq +
             " urwg:createTime=" + dq + StringEscapeUtils.escapeXml(timestamp) + dq);
         if (KeyInfo != null) {
            output.append(">");
            KeyInfo.asXml(output);
            output.append("</RecordIdentity>\n");
         } else {
            output.append(" />\n");
         }
    }

    public void setKeyInfo(KeyInfoType KeyInfo) 
      {
        this.KeyInfo = KeyInfo;
    }

    public KeyInfoType getKeyInfo() 
      {
        return KeyInfo;
    }
}

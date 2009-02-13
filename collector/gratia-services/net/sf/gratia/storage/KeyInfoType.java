package net.sf.gratia.storage;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * <p>Title: KeyInfoType</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Fermilab</p>
 *
 * @author Philippe Canal
 * @version 1.0
 */
public class KeyInfoType {
    private String Id;
    private String Content; // Could be a list of elements.

    public KeyInfoType() {
    }

    public void setContent(String Content) {
        this.Content = Content;
    }

    public String getContent() {
        return Content;
    }

    public void setId(String Id) {
        this.Id = Id;
    }

    public String getId() {
        return Id;
    }

    public String toString() {
        String output = "KeyInfo: (";
        if (Id != null) output = output + "id: " + Id;
        if (Content != null) output = output + "content: " + Content;
        output = output + ")";
        return output;
    }

   public String asXml() {
      StringBuilder output = new StringBuilder();
      asXml(output);
      return output.toString();
   }
   
   public void asXml(StringBuilder output) {
      if (Id == null && Content == null) return;
      
      if (Content != null && Content.startsWith("/")) { // Straight DN
         output.append("<DN>" + StringEscapeUtils.escapeXml(Content) + "</DN>");
      } else {
         output.append("<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" ");
         if (Id != null) output.append("Id=\""+
                                       StringEscapeUtils.escapeXml(Id)+"\" ");
         output.append(">\n");
         if (Content != null) output.append(Content);
         output.append("\n</ds:KeyInfo>\n");
      }
   }
}
